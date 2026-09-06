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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.113 Mobile Safari/537.36"

    /**
     * 抓取指定 ASIN 的亚马逊详情页，并解析出大类排名、子类排名、标题与封面
     * 支持中英文双语详情页，自动适应海外与国内网络环境
     */
    suspend fun fetchAsinRank(asin: String): ScrapeResult = withContext(Dispatchers.IO) {
        val cleanAsin = asin.trim().uppercase()
        val url = "https://www.amazon.com/dp/$cleanAsin"

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", MOBILE_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
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
            if (code == 404 || body.contains("looking for doesn't exist", ignoreCase = true) || body.contains("dogs of Amazon", ignoreCase = true)) {
                return@withContext ScrapeResult.NotFound()
            }

            if (!response.isSuccessful) {
                return@withContext ScrapeResult.Error("HTTP 状态异常: $code", code)
            }

            // 3. Jsoup 解析文档
            val doc = Jsoup.parse(body, url)
            parseDocument(cleanAsin, doc, body)
        } catch (e: Exception) {
            val msg = e.message ?: "网络请求失败"
            val userFriendlyMsg = if (msg.contains("timeout", ignoreCase = true)) {
                "连接超时(可能受网络波动或亚马逊限流影响)"
            } else {
                msg
            }
            ScrapeResult.Error(userFriendlyMsg)
        }
    }

    private fun isCaptchaOrBotBlock(code: Int, html: String): Boolean {
        if (code == 503) return true
        if (html.length < 15000 && (
            html.contains("Robot Check", ignoreCase = true) ||
            html.contains("Type the characters you see in this image", ignoreCase = true) ||
            html.contains("api-services-support@amazon.com", ignoreCase = true) ||
            html.contains("opfcaptcha", ignoreCase = true) ||
            html.contains("validateCaptcha", ignoreCase = true)
        )) {
            return true
        }
        return false
    }

    private fun cleanTitle(title: String?): String? {
        if (title.isNullOrBlank()) return null
        var t = title.trim()
        t = t.replace(Regex("(?i)^Amazon\\.com:\\s*"), "")
        t = t.replace(Regex("(?i)\\s*:\\s*(?:Cell Phones|Electronics|Home|Kitchen|Clothing|Automotive|Tools).*$"), "")
        return t.trim().takeIf { it.isNotBlank() }
    }

    private fun parseDocument(asin: String, doc: Document, rawHtml: String): ScrapeResult {
        // 提取标题 (多级回退)
        val rawTitle = doc.select("#productTitle").text().ifBlank {
            doc.select("h1#title").text().ifBlank {
                doc.select("span#title").text().ifBlank {
                    doc.select("meta[property=og:title]").attr("content").ifBlank {
                        doc.title()
                    }
                }
            }
        }
        val title = cleanTitle(rawTitle)

        // 提取主图 (多级回退)
        val imageUrl = doc.select("#landingImage").attr("src").ifBlank {
            doc.select("#main-image").attr("src").ifBlank {
                doc.select("img[data-old-hires]").attr("data-old-hires").ifBlank {
                    doc.select("meta[property=og:image]").attr("content")
                }
            }
        }.trim().takeIf { it.isNotBlank() }

        // 提取 BSR (Best Sellers Rank / 热销商品排名)
        val bsrResult = extractBSR(doc, rawHtml)

        return when {
            bsrResult != null -> {
                ScrapeResult.Success(
                    asin = asin,
                    title = title,
                    imageUrl = imageUrl,
                    mainRank = bsrResult.mainRank,
                    mainCategory = cleanCategory(bsrResult.mainCategory),
                    subRank = bsrResult.subRank,
                    subCategory = bsrResult.subCategory?.let { cleanCategory(it) },
                    rawSnippet = bsrResult.rawText
                )
            }
            title != null || imageUrl != null -> {
                // 页面正常打开且识别到商品，但该商品在亚马逊上暂无任何销售排名 (新上架或特定变体)
                ScrapeResult.NoRank(
                    asin = asin,
                    title = title,
                    imageUrl = imageUrl
                )
            }
            else -> {
                ScrapeResult.Error("页面已加载，但未识别到有效的商品数据")
            }
        }
    }

    private fun cleanCategory(cat: String): String {
        return cat.replace(Regex("(?i)^See Top 100 in\\s+"), "")
            .replace(Regex("查看.*销售排行榜"), "")
            .replace("&#38;", "&")
            .replace("&amp;", "&")
            .replace("（", "")
            .replace("）", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
    }

    data class BsrInfo(
        val mainRank: Int,
        val mainCategory: String,
        val subRank: Int?,
        val subCategory: String?,
        val rawText: String
    )

    private fun extractBSR(doc: Document, html: String): BsrInfo? {
        val ranks = mutableListOf<Pair<Int, String>>()
        val keywords = listOf("Best Sellers Rank", "热销商品排名", "畅销商品排名", "销售排行榜", "SalesRank")

        // 1. 定位包含排名信息的容器（表格或列表）
        var targetText = ""

        for (kw in keywords) {
            val th = doc.select("*:matchesOwn((?i)$kw)").firstOrNull()
            if (th != null) {
                val tr = th.parents().firstOrNull { it.tagName() == "tr" }
                val td = tr?.select("td")?.firstOrNull()
                if (td != null) {
                    targetText = td.text()
                    break
                } else {
                    targetText = th.parent()?.text() ?: ""
                    if (targetText.isNotBlank()) break
                }
            }
        }

        if (targetText.isBlank()) {
            val bulletItems = doc.select("#detailBullets_feature_div li")
            for (li in bulletItems) {
                if (keywords.any { li.text().contains(it, ignoreCase = true) }) {
                    targetText = li.text()
                    break
                }
            }
        }

        // 清洗掉括号中的说明性文字（如查看排行榜等）
        val cleanedText = targetText.replace(Regex("[\uff08(][^\uff09)]*[\uff09)]"), " ")

        // 2. 匹配英文模式: #70,544 in Electronics
        val enPattern = Pattern.compile("#([0-9,]+)\\s+(?:in|within)\\s+([^(\\n<\\|]{2,35})", Pattern.CASE_INSENSITIVE)
        val enMatcher = enPattern.matcher(cleanedText)
        while (enMatcher.find()) {
            val r = enMatcher.group(1)?.replace(",", "")?.toIntOrNull()
            val cat = enMatcher.group(2)?.trim()
            if (r != null && !cat.isNullOrBlank() && ranks.none { it.first == r }) {
                ranks.add(r to cat)
            }
        }

        // 3. 匹配中文模式: 电子商品里排第70,544名 / 耳机收纳盒商品里排第3,050名
        val zhPattern = Pattern.compile("([^(\\n\\r<\\|]{2,30}?)(?:商品)?(?:里)?排第\\s*([0-9,]+)\\s*名")
        val zhMatcher = zhPattern.matcher(cleanedText)
        while (zhMatcher.find()) {
            val cat = zhMatcher.group(1)?.trim()
            val r = zhMatcher.group(2)?.replace(",", "")?.toIntOrNull()
            if (r != null && !cat.isNullOrBlank() && ranks.none { it.first == r }) {
                ranks.add(r to cat)
            }
        }

        // 4. HTML a 链接全局备选匹配
        if (ranks.isEmpty()) {
            val linkPattern = Pattern.compile("#([0-9,]+)\\s+(?:in|within)\\s+<a[^>]*>([^<]+)</a>", Pattern.CASE_INSENSITIVE)
            val matcher = linkPattern.matcher(html)
            while (matcher.find()) {
                val r = matcher.group(1)?.replace(",", "")?.toIntOrNull()
                val cat = matcher.group(2)?.trim()
                if (r != null && !cat.isNullOrBlank() && ranks.none { it.first == r }) {
                    ranks.add(r to cat)
                }
            }
        }

        // 5. 全局 fallback (如果容器内没找到，搜索全文文本)
        if (ranks.isEmpty()) {
            val fullText = doc.text().replace(Regex("[\uff08(][^\uff09)]*[\uff09)]"), " ")
            val fallbackEn = enPattern.matcher(fullText)
            while (fallbackEn.find() && ranks.size < 2) {
                val r = fallbackEn.group(1)?.replace(",", "")?.toIntOrNull()
                val cat = fallbackEn.group(2)?.trim()
                if (r != null && !cat.isNullOrBlank() && ranks.none { it.first == r }) {
                    ranks.add(r to cat)
                }
            }

            val fallbackZh = zhPattern.matcher(fullText)
            while (fallbackZh.find() && ranks.size < 2) {
                val cat = fallbackZh.group(1)?.trim()
                val r = fallbackZh.group(2)?.replace(",", "")?.toIntOrNull()
                if (r != null && !cat.isNullOrBlank() && ranks.none { it.first == r }) {
                    ranks.add(r to cat)
                }
            }
        }

        if (ranks.isNotEmpty()) {
            val (mainRank, mainCat) = ranks[0]
            val subPair = ranks.getOrNull(1)
            return BsrInfo(
                mainRank = mainRank,
                mainCategory = mainCat,
                subRank = subPair?.first,
                subCategory = subPair?.second,
                rawText = "#$mainRank in $mainCat"
            )
        }
        return null
    }

    /**
     * 辅助工具：从 Amazon 任意 URL 中提取 10 位 ASIN 码
     */
    fun extractAsinFromUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.matches(Regex("^[A-Z0-9]{10}$", RegexOption.IGNORE_CASE))) {
            return trimmed.uppercase()
        }

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
