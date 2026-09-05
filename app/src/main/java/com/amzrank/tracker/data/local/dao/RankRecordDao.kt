package com.amzrank.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.amzrank.tracker.data.local.entity.RankRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RankRecordDao {
    @Query("SELECT * FROM rank_records WHERE asin = :asin ORDER BY timestamp ASC")
    fun getRecordsForAsinFlow(asin: String): Flow<List<RankRecord>>

    @Query("SELECT * FROM rank_records WHERE asin = :asin ORDER BY timestamp ASC")
    suspend fun getRecordsForAsin(asin: String): List<RankRecord>

    @Query("SELECT * FROM rank_records WHERE asin = :asin AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getRecentRecordsFlow(asin: String, sinceTimestamp: Long): Flow<List<RankRecord>>

    @Query("SELECT * FROM rank_records WHERE asin = :asin AND dateString = :dateString LIMIT 1")
    suspend fun getRecordByDate(asin: String, dateString: String): RankRecord?

    @Query("SELECT * FROM rank_records WHERE asin = :asin ORDER BY timestamp DESC LIMIT 2")
    suspend fun getLatestTwoRecords(asin: String): List<RankRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: RankRecord): Long

    @Query("DELETE FROM rank_records WHERE asin = :asin")
    suspend fun deleteByAsin(asin: String)
}
