package com.amzrank.tracker.data.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object AmazonScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.113 Mobile Safari/537.36"

    /**
     * 抓取指定 ASIN 的亚马逊详情页，并解析出大类排名与子类排名
     */
    suspend fun fetchAsinRank(asin: String): ScrapeResult = withContext(Dispatchers.IO) {
        val cleanAsin = asin.trim().uppercase()
        val url = "https://www.amazon.com/dp/$cleanAsin"

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", MOBILE_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Android Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                .header("sec-ch-ua-mobile", "?1")
                .header("sec-ch-ua-platform", "\"Android\"")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")

            val cookieHeader = CookieManagerHelper.getAmazonCookieHeader()
            if (cookieHeader.isNotEmpty()) {
                requestBuilder.header("Cookie", cookieHeader)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            // 1. 判断是否为人机验证或反爬拦截
            if (isCaptchaOrBotBlock(code, body)) {
                return@withContext ScrapeResult.CaptchaBlocked()
            }

            // 2. 判断是否 404 或未找到
            if (code == 404 || body.contains("looking for doesn't exist") || body.contains("dogs of Amazon")) {
                return@withContext ScrapeResult.NotFound()
            }

            if (!response.isSuccessful) {
                return@withContext ScrapeResult.Error("HTTP 状态异常: $code", code)
            }

            // 3. Jsoup 解析文档
            val doc = Jsoup.parse(body, url)
            parseDocument(cleanAsin, doc, body)
        } catch (e: Exception) {
            ScrapeResult.Error(e.message ?: "网络请求失败")
        }
    }

    private fun isCaptchaOrBotBlock(code: Int, html: String): Boolean {
        if (code == 503) return true
        if (html.contains("Robot Check", ignoreCase = true)) return true
        if (html.contains("Type the characters you see in this image", ignoreCase = true)) return true
        if (html.contains("api-services-support@amazon.com", ignoreCase = true)) return true
        if (html.contains("opfcaptcha", ignoreCase = true)) return true
        if (html.contains("validateCaptcha", ignoreCase = true)) return true
        return false
    }

    private fun parseDocument(asin: String, doc: Document, rawHtml: String): ScrapeResult {
        // 提取标题
        val title = doc.select("#productTitle").text().ifBlank {
            doc.select("h1#title").text().ifBlank {
                doc.select("meta[property=og:title]").attr("content").ifBlank {
                    doc.title()
                }
            }
        }.trim()

        // 提取主图
        val imageUrl = doc.select("#landingImage").attr("src").ifBlank {
            doc.select("#main-image").attr("src").ifBlank {
                doc.select("meta[property=og:image]").attr("content")
            }
        }.trim()

        // 提取 BSR (Best Sellers Rank)
        val bsrResult = extractBSR(doc, rawHtml)

        return if (bsrResult != null) {
            ScrapeResult.Success(
                asin = asin,
                title = title.takeIf { it.isNotBlank() },
                imageUrl = imageUrl.takeIf { it.isNotBlank() },
                mainRank = bsrResult.mainRank,
                mainCategory = bsrResult.mainCategory,
                subRank = bsrResult.subRank,
                subCategory = bsrResult.subCategory,
                rawSnippet = bsrResult.rawText
            )
        } else {
            // 如果成功加载页面但尚未获取到明确的 BSR（某些新商品或极冷门商品暂无排名）
            ScrapeResult.Error("页面已加载，但未检测到该商品的 Best Sellers Rank 排名信息")
        }
    }

    data class BsrInfo(
        val mainRank: Int,
        val mainCategory: String,
        val subRank: Int?,
        val subCategory: String?,
        val rawText: String
    )

    private fun extractBSR(doc: Document, html: String): BsrInfo? {
        // 尝试从不同的排版容器中抓取 BSR
        // 方式 1: 详情表格 (#productDetails_db_sections)
        val tableRow = doc.select("tr:contains(Best Sellers Rank)").first()
        val tableText = tableRow?.select("td")?.text()

        // 方式 2: 详情列表 (#detailBullets_feature_div)
        val bulletItem = doc.select("#detailBullets_feature_div li:contains(Best Sellers Rank)").first()
        val bulletText = bulletItem?.text()

        // 方式 3: 通用选择器与 #SalesRank
        val salesRankEl = doc.select("#SalesRank").first()
        val salesRankText = salesRankEl?.text()

        val textToSearch = listOfNotNull(tableText, bulletText, salesRankText).firstOrNull()
            ?: doc.select("*:containsOwn(Best Sellers Rank)").firstOrNull()?.parent()?.text()
            ?: html

        return parseBsrFromText(textToSearch)
    }

    /**
     * 正则解析 BSR 排名文本
     * 常见格式样例:
     * "#1,234 in Electronics (See Top 100 in Electronics)"
     * "Best Sellers Rank: #35 in Kitchen & Dining"
     * "#3 in Over-Ear Headphones"
     */
    fun parseBsrFromText(text: String): BsrInfo? {
        // 匹配 "#数字 in 类目"
        val mainPattern = Pattern.compile("#([0-9,]+)\\s+(?:in|within)\\s+([^(\\n<\\|]+)", Pattern.CASE_INSENSITIVE)
        val matcher = mainPattern.matcher(text)

        if (matcher.find()) {
            val rankStr = matcher.group(1)?.replace(",", "") ?: return null
            val mainRank = rankStr.toIntOrNull() ?: return null
            val mainCategory = matcher.group(2)?.trim()?.replace("&#38;", "&") ?: "General"

            // 尝试匹配第二项（子分类排名），例如 " #5 in Over-Ear Headphones"
            var subRank: Int? = null
            var subCategory: String? = null
            if (matcher.find()) {
                val subRankStr = matcher.group(1)?.replace(",", "")
                subRank = subRankStr?.toIntOrNull()
                subCategory = matcher.group(2)?.trim()?.replace("&#38;", "&")
            }

            return BsrInfo(
                mainRank = mainRank,
                mainCategory = mainCategory,
                subRank = subRank,
                subCategory = subCategory,
                rawText = text.take(300)
            )
        }
        return null
    }

    /**
     * 辅助工具：从 Amazon 任意 URL 中提取 10 位 ASIN 码
     */
    fun extractAsinFromUrl(input: String): String? {
        val trimmed = input.trim()
        // 如果用户直接输入了标准的 10 位 ASIN（例如 B09V3HN1KC）
        if (trimmed.matches(Regex("^[A-Z0-9]{10}$", RegexOption.IGNORE_CASE))) {
            return trimmed.uppercase()
        }

        // 从 URL 中提取：/dp/ASIN 或 /product/ASIN 或 /gp/product/ASIN
        val patterns = listOf(
            Regex("/(?:dp|gp/product|product)/([A-Z0-9]{10})", RegexOption.IGNORE_CASE),
            Regex("[?&]asin=([A-Z0-9]{10})", RegexOption.IGNORE_CASE),
            Regex("/([A-Z0-9]{10})(?:[/?]|$)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1].uppercase()
            }
        }
        return null
    }
}
