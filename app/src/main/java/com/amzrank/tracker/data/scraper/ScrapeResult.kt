package com.amzrank.tracker.data.scraper

sealed class ScrapeResult {
    data class Success(
        val asin: String,
        val title: String?,
        val imageUrl: String?,
        val mainRank: Int,
        val mainCategory: String,
        val subRank: Int? = null,
        val subCategory: String? = null,
        val rawSnippet: String? = null
    ) : ScrapeResult()

    data class CaptchaBlocked(
        val message: String = "亚马逊返回人机验证(Robot Check)，请点击下方【验证会话】"
    ) : ScrapeResult()

    data class Error(
        val message: String,
        val statusCode: Int? = null
    ) : ScrapeResult()

    data class NotFound(
        val message: String = "未在 Amazon.com 找到该 ASIN 商品"
    ) : ScrapeResult()
}
