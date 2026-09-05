package com.amzrank.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asin_items")
data class AsinItem(
    @PrimaryKey
    val asin: String,
    val alias: String,
    val productTitle: String? = null,
    val imageUrl: String? = null,
    val latestMainRank: Int? = null,
    val latestMainCategory: String? = null,
    val latestSubRank: Int? = null,
    val latestSubCategory: String? = null,
    val previousMainRank: Int? = null,
    val lastUpdated: Long = 0L,
    val lastStatus: String = "PENDING", // PENDING, OK, CAPTCHA, ERROR
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 计算相比上一次排名的变化。
     * 在排名中，数值越小越靠前。
     * 例如：从 #100 变成 #80，排名上升 (rankDelta = +20)
     */
    val rankDelta: Int?
        get() {
            val curr = latestMainRank ?: return null
            val prev = previousMainRank ?: return null
            return prev - curr // 正数表示上升，负数表示下降
        }
}
