package com.smithware.tidypilot

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.smithware.tidypilot.data.CleaningTaskEntity
import com.smithware.tidypilot.data.TidyPilotDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TidyPilotTodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TidyPilotWidgetUpdater.updateToday(context, appWidgetManager, appWidgetIds)
    }
}

class TidyPilotQuickResetWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TidyPilotWidgetUpdater.updateQuickReset(context, appWidgetManager, appWidgetIds)
    }
}

object TidyPilotWidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        updateToday(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, TidyPilotTodayWidgetProvider::class.java))
        )
        updateQuickReset(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, TidyPilotQuickResetWidgetProvider::class.java))
        )
    }

    fun updateToday(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = loadSnapshot(context)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, todayViews(context, snapshot))
                }
            }
        }
    }

    fun updateQuickReset(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = loadSnapshot(context)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, quickResetViews(context, snapshot))
                }
            }
        }
    }

    private suspend fun loadSnapshot(context: Context): WidgetSnapshot {
        val dao = TidyPilotDatabase.get(context).dao()
        val today = LocalDate.now()
        val due = dao.dueTasksOnce(today, 5)
        val active = dao.activeTasksOnce()
        val energy = dao.latestEnergyOnce()
        val averageScore = dao.averageRoomScore()?.toInt() ?: 0
        val completed = dao.completionCountBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
        val recommended = due.ifEmpty { active }
        val quickReset = recommended
            .filter { it.energyRequired == "low" || it.estimatedMinutes <= 10 || it.isQuickResetTask }
            .minWithOrNull(compareBy<CleaningTaskEntity> { it.estimatedMinutes }.thenBy { priorityRank(it.priority) })
            ?: recommended.minByOrNull { it.estimatedMinutes }
        return WidgetSnapshot(
            tasks = recommended.take(3),
            quickReset = quickReset,
            energy = energy?.energyLevel ?: "medium",
            minutes = energy?.availableMinutes ?: 15,
            averageRoomScore = averageScore,
            completedToday = completed
        )
    }

    private fun todayViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_today_plan).apply {
            setOnClickPendingIntent(R.id.widget_today_root, openAppIntent(context))
            setTextViewText(R.id.widget_today_title, "TidyPilot")
            setTextViewText(R.id.widget_today_subtitle, todaySubtitle(snapshot))
            setTextViewText(R.id.widget_today_energy, "${snapshot.energy} energy")
            setTextViewText(R.id.widget_today_time, "${snapshot.minutes} min")
            setTextViewText(R.id.widget_today_score, "Home ${snapshot.averageRoomScore}/100")
            val taskLines = snapshot.tasks.mapIndexed { index, task -> "${index + 1}. ${task.name} - ${task.estimatedMinutes} min" }
            setTextViewText(
                R.id.widget_today_tasks,
                taskLines.ifEmpty { listOf("No chores queued. Add a task or scan a room.") }.joinToString("\n")
            )
            setTextViewText(R.id.widget_today_action, "Open Today's Plan")
        }

    private fun quickResetViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_quick_reset).apply {
            setOnClickPendingIntent(R.id.widget_quick_root, openAppIntent(context))
            setTextViewText(R.id.widget_quick_title, "Quick reset")
            setTextViewText(
                R.id.widget_quick_task,
                snapshot.quickReset?.let { "${it.name}\n${it.estimatedMinutes} min - ${it.energyRequired} energy" }
                    ?: "No chores queued.\nOpen TidyPilot to add one."
            )
            setTextViewText(R.id.widget_quick_footer, if (snapshot.completedToday > 0) "${snapshot.completedToday} done today" else "Small reset still counts")
        }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun todaySubtitle(snapshot: WidgetSnapshot): String =
        when {
            snapshot.completedToday > 0 -> "${snapshot.completedToday} reset${if (snapshot.completedToday == 1) "" else "s"} done today"
            snapshot.energy == "very low" || snapshot.energy == "low" -> "Pick one small reset"
            snapshot.tasks.isEmpty() -> "Ready when you are"
            else -> "Next useful chores"
        }

    private fun priorityRank(priority: String): Int = when (priority) {
        "urgent" -> 0
        "high" -> 1
        "normal" -> 2
        else -> 3
    }
}

private data class WidgetSnapshot(
    val tasks: List<CleaningTaskEntity>,
    val quickReset: CleaningTaskEntity?,
    val energy: String,
    val minutes: Int,
    val averageRoomScore: Int,
    val completedToday: Int
)
