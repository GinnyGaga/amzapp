package com.amzrank.tracker.data.scraper

import android.webkit.CookieManager

object CookieManagerHelper {
    private const val AMAZON_DOMAIN = "https://www.amazon.com"

    fun getAmazonCookieHeader(): String {
        return try {
            val cookieManager = CookieManager.getInstance()
            var cookies = cookieManager.getCookie(AMAZON_DOMAIN) ?: ""
            // 确保强制锁定英文语言 en_US 与美元偏好，避免国内手机网络自动切换中文导致排版歧义
            if (cookies.contains("lc-main=")) {
                cookies = cookies.replace(Regex("lc-main=[^;]+"), "lc-main=en_US")
            } else {
                cookies = if (cookies.isEmpty()) "lc-main=en_US; i18n-prefs=USD;" else "$cookies; lc-main=en_US; i18n-prefs=USD;"
            }
            cookies
        } catch (e: Exception) {
            "lc-main=en_US; i18n-prefs=USD;"
        }
    }

    fun hasValidSessionCookie(): Boolean {
        val cookies = getAmazonCookieHeader()
        return cookies.contains("session-id") || cookies.contains("ubid-main")
    }

    fun ensureEnglishCookies() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setCookie(AMAZON_DOMAIN, "lc-main=en_US; path=/; domain=.amazon.com")
            cookieManager.setCookie(AMAZON_DOMAIN, "i18n-prefs=USD; path=/; domain=.amazon.com")
            cookieManager.flush()
        } catch (_: Exception) {}
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
