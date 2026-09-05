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

    /**
     * 商品页面正常存在且提取到了标题和图片，但该商品在亚马逊上暂无销量/未进入销售排行榜 (如新品或特定变体)
     */
    data class NoRank(
        val asin: String,
        val title: String?,
        val imageUrl: String?,
        val message: String = "该商品暂无销售榜排名 (新上架或冷门款)"
    ) : ScrapeResult()

    data class CaptchaBlocked(
        val message: String = "亚马逊返回人机验证，请点击右上角【🛡️】验证会话"
    ) : ScrapeResult()

    data class Error(
        val message: String,
        val statusCode: Int? = null
    ) : ScrapeResult()

    data class NotFound(
        val message: String = "未在 Amazon.com 找到该 ASIN 商品"
    ) : ScrapeResult()
}
