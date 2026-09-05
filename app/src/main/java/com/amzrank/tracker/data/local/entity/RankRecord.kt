package com.amzrank.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rank_records",
    indices = [
        Index(value = ["asin", "dateString"], unique = true),
        Index(value = ["asin", "timestamp"])
    ]
)
data class RankRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val asin: String,
    val dateString: String, // 格式: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val mainRank: Int? = null,
    val mainCategory: String? = null,
    val subRank: Int? = null,
    val subCategory: String? = null,
    val status: String = "SUCCESS", // SUCCESS, CAPTCHA_BLOCKED, NETWORK_ERROR
    val notes: String? = null
)
