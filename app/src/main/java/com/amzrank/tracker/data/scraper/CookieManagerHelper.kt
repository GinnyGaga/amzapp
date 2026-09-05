package com.amzrank.tracker.data.scraper

import android.webkit.CookieManager

object CookieManagerHelper {
    private const val AMAZON_DOMAIN = "https://www.amazon.com"

    fun getAmazonCookieHeader(): String {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(AMAZON_DOMAIN)
            cookies ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun hasValidSessionCookie(): Boolean {
        val cookies = getAmazonCookieHeader()
        return cookies.contains("session-id") || cookies.contains("ubid-main")
    }

    fun setCookie(cookie: String) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setCookie(AMAZON_DOMAIN, cookie)
            cookieManager.flush()
        } catch (_: Exception) {}
    }
}
