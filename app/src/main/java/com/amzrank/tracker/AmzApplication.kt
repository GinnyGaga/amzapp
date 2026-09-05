package com.amzrank.tracker

import android.app.Application
import com.amzrank.tracker.data.local.AppDatabase
import com.amzrank.tracker.data.repository.RankRepository
import com.amzrank.tracker.worker.WorkScheduler

class AmzApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: RankRepository by lazy { RankRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // 注册每日 24 小时后台定时同步任务
        WorkScheduler.scheduleDailySync(this)
    }
}
