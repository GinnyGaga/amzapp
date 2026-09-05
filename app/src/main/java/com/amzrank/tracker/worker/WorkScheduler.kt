package com.amzrank.tracker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val DAILY_WORK_TAG = "amz_daily_rank_sync"
    private const val IMMEDIATE_WORK_TAG = "amz_immediate_rank_sync"

    /**
     * 注册 24 小时周期的每日定时后台抓取任务
     */
    fun scheduleDailySync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<DailySyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(DAILY_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * 触发一次立即单次全量同步
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<DailySyncWorker>()
            .setConstraints(constraints)
            .addTag(IMMEDIATE_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }
}
