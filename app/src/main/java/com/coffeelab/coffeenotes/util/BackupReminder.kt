package com.coffeelab.coffeenotes.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.coffeelab.coffeenotes.MainActivity

/**
 * 备份提醒：
 * - [schedule] 根据 SharedPreferences 里的周期调度 AlarmManager 重复提醒
 * - [cancel] 关闭提醒
 * - [ReminderReceiver] 到点发一条本地通知
 *
 * 周期存储：`backup_reminder_interval_days`（0 = 关闭；7/14/30 = 每周/每两周/每月）
 * 不引 WorkManager，用 AlarmManager.setRepeating 轻量实现。
 */
object BackupReminder {

    const val PREFS_NAME = "backup_reminder_prefs"
    const val KEY_INTERVAL_DAYS = "backup_reminder_interval_days"
    const val KEY_NEXT_TRIGGER = "backup_reminder_next_trigger"
    const val CHANNEL_ID = "backup_reminder"
    private const val REQUEST_CODE = 1001
    private const val ACTION_REMIND = "com.coffeelab.coffeenotes.ACTION_BACKUP_REMIND"

    /** 读取当前提醒周期（天），0 = 关闭 */
    fun getIntervalDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INTERVAL_DAYS, 0)
    }

    /** 设置周期（天）并重新调度；0 = 关闭 */
    fun setIntervalDays(context: Context, days: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INTERVAL_DAYS, days).apply()
        if (days > 0) {
            schedule(context, days, anchorTime = System.currentTimeMillis())
        } else {
            cancel(context)
        }
    }

    /**
     * 启动时恢复闹钟：AlarmManager 的 alarm 在 `adb install -r` 覆盖安装或系统清理后
     * 会被取消（PendingIntent 失效），而设置存在 SharedPreferences 里不会丢——
     * 所以每次 app 启动检查设置，有提醒就按持久化的锚点时间重新注册。
     *
     * 锚点逻辑（关键）：不重新按"当前时间+间隔"计算，而是沿用上次存的下次触发时间。
     * 否则用户每次打开 app 都会把周期往后推，永远不触发。若锚点已过期（比如装机后
     * 错过了触发点），则立即补发一条提醒并重新锚定。
     */
    fun rescheduleIfNeeded(context: Context) {
        val days = getIntervalDays(context)
        if (days <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextTrigger = prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        val now = System.currentTimeMillis()
        if (nextTrigger <= 0 || nextTrigger <= now) {
            // 无锚点或已错过：补发一次 + 从此刻重新锚定
            createChannel(context)
            postNotification(context, days)
            schedule(context, days, anchorTime = now)
        } else {
            // 锚点在未来：原计划注册
            schedule(context, days, anchorTime = nextTrigger - days * 24L * 60 * 60 * 1000)
        }
    }

    private fun schedule(context: Context, days: Int, anchorTime: Long) {
        createChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervalMs = days * 24L * 60 * 60 * 1000
        val triggerAt = anchorTime + intervalMs
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_NEXT_TRIGGER, triggerAt).apply()
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intervalMs,
            buildPendingIntent(context)
        )
    }

    private fun postNotification(context: Context, days: Int) {
        // 点击通知 → 打开 app 并进入「备份与恢复」页
        val openIntent = Intent(context, com.coffeelab.coffeenotes.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO_BACKUP, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("☕ 该备份咖啡笔记了")
            .setContentText("距离上次提醒已 ${days} 天，点击去备份，防止数据丢失")
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(REQUEST_CODE, notification)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "备份提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "定期提醒导出备份" }
            manager.createNotificationChannel(channel)
        }
    }

    /** AlarmManager 触发：发一条"该备份了"通知 */
    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_REMIND) return
            val days = getIntervalDays(context)
            if (days <= 0) return
            postNotification(context, days)
        }
    }
}
