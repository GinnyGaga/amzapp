package com.amzrank.tracker.data.repository

import com.amzrank.tracker.data.local.AppDatabase
import com.amzrank.tracker.data.local.entity.AsinItem
import com.amzrank.tracker.data.local.entity.RankRecord
import com.amzrank.tracker.data.scraper.AmazonScraper
import com.amzrank.tracker.data.scraper.ScrapeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class RankRepository(private val database: AppDatabase) {
    private val asinDao = database.asinDao()
    private val rankRecordDao = database.rankRecordDao()

    val allAsinsFlow: Flow<List<AsinItem>> = asinDao.getAllAsinsFlow()

    fun getAsinFlow(asin: String): Flow<AsinItem?> = asinDao.getAsinFlow(asin)

    fun getRecordsForAsinFlow(asin: String): Flow<List<RankRecord>> =
        rankRecordDao.getRecordsForAsinFlow(asin)

    fun getRecentRecordsFlow(asin: String, days: Int): Flow<List<RankRecord>> {
        val since = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return rankRecordDao.getRecentRecordsFlow(asin, since)
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * 添加新的 ASIN，并立即发起一次首次抓取以初始化商品信息与今日排名
     */
    suspend fun addAsin(rawInput: String, alias: String): Result<String> = withContext(Dispatchers.IO) {
        val asin = AmazonScraper.extractAsinFromUrl(rawInput)
            ?: return@withContext Result.failure(IllegalArgumentException("无法识别的 ASIN 或链接格式"))

        val existing = asinDao.getAsin(asin)
        val finalAlias = alias.ifBlank { "商品 $asin" }

        val initialItem = existing?.copy(alias = finalAlias, isActive = true)
            ?: AsinItem(asin = asin, alias = finalAlias, lastStatus = "PENDING")

        asinDao.insertOrUpdate(initialItem)

        // 立即执行一次抓取更新信息
        syncSingleAsin(asin)
        Result.success(asin)
    }

    suspend fun deleteAsin(asin: String) = withContext(Dispatchers.IO) {
        asinDao.deleteByAsin(asin)
        rankRecordDao.deleteByAsin(asin)
    }

    suspend fun toggleAsinActive(asin: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        val item = asinDao.getAsin(asin) ?: return@withContext
        asinDao.insertOrUpdate(item.copy(isActive = isActive))
    }

    /**
     * 针对单个 ASIN 进行实时抓取并入库
     */
    suspend fun syncSingleAsin(asin: String): ScrapeResult = withContext(Dispatchers.IO) {
        val currentItem = asinDao.getAsin(asin) ?: return@withContext ScrapeResult.NotFound()
        val result = AmazonScraper.fetchAsinRank(asin)
        val todayStr = getTodayDateString()
        val now = System.currentTimeMillis()

        when (result) {
            is ScrapeResult.Success -> {
                // 1. 存入/更新每日排名记录
                val record = RankRecord(
                    asin = asin,
                    dateString = todayStr,
                    timestamp = now,
                    mainRank = result.mainRank,
                    mainCategory = result.mainCategory,
                    subRank = result.subRank,
                    subCategory = result.subCategory,
                    status = "SUCCESS",
                    notes = result.rawSnippet
                )
                rankRecordDao.insertOrUpdate(record)

                // 2. 更新 AsinItem 主表（设置前一日排名用于计算涨跌）
                val prevRank = if (currentItem.latestMainRank != null && currentItem.latestMainRank != result.mainRank) {
                    currentItem.latestMainRank
                } else {
                    currentItem.previousMainRank
                }

                val updatedItem = currentItem.copy(
                    productTitle = result.title ?: currentItem.productTitle,
                    imageUrl = result.imageUrl ?: currentItem.imageUrl,
                    latestMainRank = result.mainRank,
                    latestMainCategory = result.mainCategory,
                    latestSubRank = result.subRank,
                    latestSubCategory = result.subCategory,
                    previousMainRank = prevRank,
                    lastUpdated = now,
                    lastStatus = "OK",
                    errorMessage = null
                )
                asinDao.insertOrUpdate(updatedItem)
            }

            is ScrapeResult.NoRank -> {
                // 商品存在但尚无排名，正常保留标题和封面
                val updatedItem = currentItem.copy(
                    productTitle = result.title ?: currentItem.productTitle,
                    imageUrl = result.imageUrl ?: currentItem.imageUrl,
                    latestMainRank = null,
                    latestMainCategory = null,
                    lastUpdated = now,
                    lastStatus = "NO_RANK",
                    errorMessage = result.message
                )
                asinDao.insertOrUpdate(updatedItem)
            }

            is ScrapeResult.CaptchaBlocked -> {
                asinDao.insertOrUpdate(
                    currentItem.copy(
                        lastUpdated = now,
                        lastStatus = "CAPTCHA",
                        errorMessage = result.message
                    )
                )
            }

            is ScrapeResult.Error -> {
                asinDao.insertOrUpdate(
                    currentItem.copy(
                        lastUpdated = now,
                        lastStatus = "ERROR",
                        errorMessage = result.message
                    )
                )
            }

            is ScrapeResult.NotFound -> {
                asinDao.insertOrUpdate(
                    currentItem.copy(
                        lastUpdated = now,
                        lastStatus = "NOT_FOUND",
                        errorMessage = result.message
                    )
                )
            }
        }

        result
    }

    data class SyncSummary(
        val total: Int,
        val successCount: Int,
        val noRankCount: Int,
        val captchaCount: Int,
        val errorCount: Int
    )

    /**
     * 批量同步所有处于激活状态的 ASIN（每日定时调度任务调用此方法）
     */
    suspend fun syncAllActiveAsins(
        onProgress: ((current: Int, total: Int, asin: String) -> Unit)? = null
    ): SyncSummary = withContext(Dispatchers.IO) {
        val activeItems = asinDao.getActiveAsins()
        var success = 0
        var noRank = 0
        var captcha = 0
        var error = 0

        val total = activeItems.size
        for ((index, item) in activeItems.withIndex()) {
            onProgress?.invoke(index + 1, total, item.asin)

            when (syncSingleAsin(item.asin)) {
                is ScrapeResult.Success -> success++
                is ScrapeResult.NoRank -> noRank++
                is ScrapeResult.CaptchaBlocked -> captcha++
                else -> error++
            }

            // 在每次抓取之间随机休眠 3~6 秒，模拟真人操作防封
            if (index < total - 1) {
                delay(Random.nextLong(3000, 6000))
            }
        }

        SyncSummary(
            total = total,
            successCount = success,
            noRankCount = noRank,
            captchaCount = captcha,
            errorCount = error
        )
    }
}
