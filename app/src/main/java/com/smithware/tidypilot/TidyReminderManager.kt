package com.smithware.tidypilot

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smithware.tidypilot.data.AppSettingsEntity
import com.smithware.tidypilot.data.PlannedReminder
import com.smithware.tidypilot.data.ReminderPlanner
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object TidyReminderManager {
    private const val CHANNEL_ID = "tidypilot_cleaning_reminders"
    private const val CHANNEL_NAME = "TidyPilot reminders"
    private const val REMINDER_REQUEST_CODE = 7148

    fun showTestReminder(context: Context, state: TidyPilotState): Boolean {
        if (!canNotify(context)) return false
        ensureChannel(context)
        val reminder = ReminderPlanner
            .plan(state.settings.copy(reminderEnabled = true), state.tasks, state.rooms, state.completions)
            .firstOrNull()
            ?: PlannedReminder(
                type = "daily",
                key = "test",
                title = "Tiny reset?",
                body = "One quick task can make your home feel better."
            )
        showReminder(context, reminder.copy(key = "test-${reminder.key}"))
        return true
    }

    fun showReminder(context: Context, reminder: PlannedReminder): Boolean {
        if (!canNotify(context)) return false
        ensureChannel(context)
        show(context, reminder)
        return true
    }

    fun scheduleNext(context: Context, settings: AppSettingsEntity) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = reminderPendingIntent(context)
        if (!settings.reminderEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            nextReminderMillis(settings.preferredReminderTime),
            30 * 60 * 1000L,
            pendingIntent
        )
    }

    fun cancelSchedule(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(reminderPendingIntent(context))
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId("task-$taskId"))
        NotificationManagerCompat.from(context).cancel(notificationId("quick-win-$taskId"))
        NotificationManagerCompat.from(context).cancel(notificationId("seasonal-$taskId"))
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
        cancelSchedule(context)
    }

    private fun show(context: Context, reminder: PlannedReminder) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId("open-tidypilot"),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tidy_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(reminder.key), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Gentle local reminders for cleaning tasks and room resets."
        }
        manager.createNotificationChannel(channel)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun reminderPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).apply { action = "com.smithware.tidypilot.REMINDER_TICK" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextReminderMillis(time: String): Long {
        val reminderTime = runCatching { LocalTime.parse(time.trim()) }.getOrDefault(LocalTime.of(18, 30))
        val now = LocalDateTime.now()
        var next = LocalDate.now().atTime(reminderTime)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun notificationId(key: String): Int = key.hashCode() and Int.MAX_VALUE
}
