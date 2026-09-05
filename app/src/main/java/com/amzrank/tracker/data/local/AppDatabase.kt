package com.amzrank.tracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.amzrank.tracker.data.local.dao.AsinDao
import com.amzrank.tracker.data.local.dao.RankRecordDao
import com.amzrank.tracker.data.local.entity.AsinItem
import com.amzrank.tracker.data.local.entity.RankRecord

@Database(
    entities = [AsinItem::class, RankRecord::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun asinDao(): AsinDao
    abstract fun rankRecordDao(): RankRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amz_rank_tracker.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
