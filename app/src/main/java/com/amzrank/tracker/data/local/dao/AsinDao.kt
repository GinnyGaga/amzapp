package com.amzrank.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.amzrank.tracker.data.local.entity.AsinItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AsinDao {
    @Query("SELECT * FROM asin_items ORDER BY createdAt DESC")
    fun getAllAsinsFlow(): Flow<List<AsinItem>>

    @Query("SELECT * FROM asin_items WHERE isActive = 1")
    suspend fun getActiveAsins(): List<AsinItem>

    @Query("SELECT * FROM asin_items WHERE asin = :asin LIMIT 1")
    suspend fun getAsin(asin: String): AsinItem?

    @Query("SELECT * FROM asin_items WHERE asin = :asin LIMIT 1")
    fun getAsinFlow(asin: String): Flow<AsinItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(asin: AsinItem)

    @Update
    suspend fun update(asin: AsinItem)

    @Delete
    suspend fun delete(asin: AsinItem)

    @Query("DELETE FROM asin_items WHERE asin = :asin")
    suspend fun deleteByAsin(asin: String)
}
