package com.amzrank.tracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amzrank.tracker.MainActivity
import com.amzrank.tracker.data.local.AppDatabase
import com.amzrank.tracker.data.repository.RankRepository

class DailySyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "amz_rank_tracker_sync_channel"
        const val NOTIFICATION_ID = 1001
        const val CAPTCHA_NOTIFICATION_ID = 1002
    }

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val repository = RankRepository(database)

        createNotificationChannel()

        val summary = repository.syncAllActiveAsins()

        // 发送通知提醒用户
        if (summary.total > 0) {
            if (summary.captchaCount > 0) {
                sendCaptchaNotification(summary.captchaCount)
            } else {
                sendCompletionNotification(summary)
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ASIN 排名更新提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日后台抓取亚马逊排名并提醒变动"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendCompletionNotification(summary: RankRepository.SyncSummary) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "共更新 ${summary.total} 个商品，成功 ${summary.successCount} 件" +
                if (summary.errorCount > 0) "，${summary.errorCount} 件异常" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("亚马逊排名每日更新完毕")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendCaptchaNotification(captchaCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "web_verify")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("亚马逊需人机验证")
            .setContentText("有 $captchaCount 个商品抓取遇到验证码，点击进入 App 验证")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(CAPTCHA_NOTIFICATION_ID, notification)
    }
}
