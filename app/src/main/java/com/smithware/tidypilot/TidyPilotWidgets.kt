package com.smithware.tidypilot

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.smithware.tidypilot.data.CleaningTaskEntity
import com.smithware.tidypilot.data.RoomEntity
import com.smithware.tidypilot.data.TaskCompletionEntity
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

class TidyPilotRoomStatusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TidyPilotWidgetUpdater.updateRoomStatus(context, appWidgetManager, appWidgetIds)
    }
}

class TidyPilotWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                when (action) {
                    ACTION_COMPLETE_TASK -> {
                        val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
                        if (taskId.isNotBlank()) TidyPilotWidgetUpdater.completeTask(context.applicationContext, taskId)
                    }
                    ACTION_ANOTHER_TASK -> {
                        val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
                        if (taskId.isNotBlank()) TidyPilotWidgetUpdater.pickAnotherQuickTask(context.applicationContext, taskId)
                    }
                }
            }
            pendingResult.finish()
        }
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
        updateRoomStatus(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, TidyPilotRoomStatusWidgetProvider::class.java))
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

    fun updateRoomStatus(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = loadSnapshot(context)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, roomStatusViews(context, snapshot))
                }
            }
        }
    }

    private suspend fun loadSnapshot(context: Context): WidgetSnapshot {
        val dao = TidyPilotDatabase.get(context).dao()
        val today = LocalDate.now()
        val due = dao.dueTasksOnce(today, 8)
        val active = dao.activeTasksOnce()
        val rooms = dao.activeRoomsOnce()
        val energy = dao.latestEnergyOnce()
        val averageScore = dao.averageRoomScore()?.toInt() ?: 0
        val completed = dao.completionCountBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
        val recommended = due.ifEmpty { active }.sortedWith(widgetTaskComparator(today))
        val excludedQuickTask = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).getString(KEY_EXCLUDED_QUICK_TASK, null)
        val quickReset = recommended
            .filter { it.energyRequired == "low" || it.estimatedMinutes <= 10 || it.isQuickResetTask }
            .filter { it.id != excludedQuickTask }
            .minWithOrNull(compareBy<CleaningTaskEntity> { it.estimatedMinutes }.thenBy { widgetScore(it, today) })
            ?: recommended.filter { it.id != excludedQuickTask }.minByOrNull { it.estimatedMinutes }
            ?: recommended.minByOrNull { it.estimatedMinutes }
        return WidgetSnapshot(
            tasks = recommended.take(3),
            allTasks = active,
            quickReset = quickReset,
            rooms = rooms.sortedWith(compareBy<RoomEntity> { roomAttentionScore(it, active, today) }.thenBy { it.name }).take(3),
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
            bindTaskRow(context, this, 0, snapshot.tasks.getOrNull(0))
            bindTaskRow(context, this, 1, snapshot.tasks.getOrNull(1))
            bindTaskRow(context, this, 2, snapshot.tasks.getOrNull(2))
            bindTaskRow(context, this, 3, null)
            setViewVisibility(R.id.widget_today_empty, if (snapshot.tasks.isEmpty()) View.VISIBLE else View.GONE)
            setTextViewText(R.id.widget_today_empty, "Set up TidyPilot to see today's cleaning tasks.")
            setTextViewText(R.id.widget_today_action, "Open Today")
        }

    private fun quickResetViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_quick_reset).apply {
            setOnClickPendingIntent(R.id.widget_quick_root, openAppIntent(context))
            setTextViewText(R.id.widget_quick_title, "One Thing")
            val task = snapshot.quickReset
            setTextViewText(R.id.widget_quick_task, task?.name ?: "Set up TidyPilot")
            setTextViewText(R.id.widget_quick_meta, task?.let { "${importanceLabel(it, LocalDate.now())} - ${it.estimatedMinutes} min - ${it.energyRequired}" } ?: "See today's cleaning tasks.")
            setOnClickPendingIntent(R.id.widget_quick_done, task?.let { completeTaskIntent(context, it.id) } ?: openAppIntent(context))
            setOnClickPendingIntent(R.id.widget_quick_another, task?.let { anotherTaskIntent(context, it.id) } ?: openAppIntent(context))
            setTextViewText(R.id.widget_quick_footer, if (snapshot.completedToday > 0) "${snapshot.completedToday} done today" else "Small reset still counts")
        }

    private fun roomStatusViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_room_status).apply {
            setOnClickPendingIntent(R.id.widget_room_root, openAppIntent(context))
            setTextViewText(R.id.widget_room_title, "Room status")
            setTextViewText(R.id.widget_room_subtitle, if (snapshot.rooms.isEmpty()) "Set up TidyPilot to see rooms." else "Needs attention")
            bindRoomRow(this, 0, snapshot.rooms.getOrNull(0), snapshot)
            bindRoomRow(this, 1, snapshot.rooms.getOrNull(1), snapshot)
            bindRoomRow(this, 2, snapshot.rooms.getOrNull(2), snapshot)
            setViewVisibility(R.id.widget_room_empty, if (snapshot.rooms.isEmpty()) View.VISIBLE else View.GONE)
            setTextViewText(R.id.widget_room_empty, "Set up TidyPilot to see today's room status.")
        }

    suspend fun completeTask(context: Context, taskId: String) {
        val dao = TidyPilotDatabase.get(context).dao()
        val task = dao.taskById(taskId) ?: return
        val now = LocalDateTime.now()
        dao.saveCompletion(
            TaskCompletionEntity(
                taskId = task.id,
                completedAt = now,
                durationMinutes = task.estimatedMinutes,
                energyLevelAtCompletion = task.energyRequired
            )
        )
        dao.markTaskComplete(task.id, now, nextDate(task.frequencyType, now.toLocalDate()))
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit().remove(KEY_EXCLUDED_QUICK_TASK).apply()
        updateAll(context)
    }

    fun pickAnotherQuickTask(context: Context, taskId: String) {
        context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit().putString(KEY_EXCLUDED_QUICK_TASK, taskId).apply()
        updateAll(context)
    }

    private fun bindTaskRow(context: Context, views: RemoteViews, index: Int, task: CleaningTaskEntity?) {
        val ids = widgetRowIds[index]
        views.setViewVisibility(ids.row, if (task == null) View.GONE else View.VISIBLE)
        if (task == null) return
        val today = LocalDate.now()
        views.setTextViewText(ids.title, task.name)
        views.setTextViewText(ids.meta, "${importanceLabel(task, today)} - ${task.estimatedMinutes} min - ${task.energyRequired}")
        views.setOnClickPendingIntent(ids.check, completeTaskIntent(context, task.id))
        views.setOnClickPendingIntent(ids.row, openAppIntent(context))
    }

    private fun bindRoomRow(views: RemoteViews, index: Int, room: RoomEntity?, snapshot: WidgetSnapshot) {
        val ids = widgetRoomRowIds[index]
        views.setViewVisibility(ids.row, if (room == null) View.GONE else View.VISIBLE)
        if (room == null) return
        val today = LocalDate.now()
        val roomTasks = snapshot.allTasks.filter { it.roomId == room.id }
        val overdue = roomTasks.count { it.nextDueAt?.isBefore(today) == true }
        views.setTextViewText(ids.name, room.name)
        views.setTextViewText(ids.score, "${room.tidyScore}/100")
        views.setTextViewText(ids.meta, roomStatusLabel(room, overdue, roomTasks.size))
    }

    private fun completeTaskIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, TidyPilotWidgetActionReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun anotherTaskIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, TidyPilotWidgetActionReceiver::class.java).apply {
            action = ACTION_ANOTHER_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            "another-$taskId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        "urgent" -> 4
        "high" -> 3
        "normal" -> 2
        else -> 1
    }

    private fun widgetTaskComparator(today: LocalDate): Comparator<CleaningTaskEntity> =
        compareByDescending<CleaningTaskEntity> { widgetScore(it, today) }
            .thenBy { it.estimatedMinutes }
            .thenBy { it.name }

    private fun widgetScore(task: CleaningTaskEntity, today: LocalDate): Int {
        val overdueBoost = task.nextDueAt?.let { due -> if (due.isBefore(today)) 6 else if (!due.isAfter(today)) 4 else 0 } ?: 3
        val energyBoost = if (task.energyRequired == "low" || task.estimatedMinutes <= 10) 2 else 0
        val resetBoost = if (task.isQuickResetTask) 2 else 0
        return priorityRank(task.priority) * 4 + overdueBoost + energyBoost + resetBoost + task.skippedCount.coerceAtMost(3)
    }

    private fun importanceLabel(task: CleaningTaskEntity, today: LocalDate): String = when {
        task.priority == "urgent" -> "Urgent"
        task.nextDueAt?.isBefore(today) == true -> "Overdue"
        task.priority == "high" -> "High impact"
        task.isQuickResetTask || task.estimatedMinutes <= 10 -> "Quick win"
        task.energyRequired == "low" -> "Low energy"
        else -> "Next up"
    }

    private fun roomAttentionScore(room: RoomEntity, tasks: List<CleaningTaskEntity>, today: LocalDate): Int {
        val roomTasks = tasks.filter { it.roomId == room.id }
        val overdue = roomTasks.count { it.nextDueAt?.isBefore(today) == true }
        val due = roomTasks.count { it.nextDueAt == null || it.nextDueAt?.isAfter(today) == false }
        val priority = priorityRank(room.priority) * 8
        return (100 - room.tidyScore) + overdue * 14 + due * 6 + priority
    }

    private fun roomStatusLabel(room: RoomEntity, overdue: Int, taskCount: Int): String = when {
        overdue > 0 -> "$overdue overdue - start small"
        room.tidyScore < 45 -> "Priority room"
        room.tidyScore < 65 -> "$taskCount task${if (taskCount == 1) "" else "s"} ready"
        else -> "Quick reset if useful"
    }

    private fun nextDate(frequency: String, today: LocalDate): LocalDate? = when (frequency) {
        "one-time" -> null
        "daily" -> today.plusDays(1)
        "every few days" -> today.plusDays(3)
        "weekly" -> today.plusWeeks(1)
        "monthly" -> today.plusMonths(1)
        else -> today.plusWeeks(1)
    }
}

private data class WidgetSnapshot(
    val tasks: List<CleaningTaskEntity>,
    val allTasks: List<CleaningTaskEntity>,
    val quickReset: CleaningTaskEntity?,
    val rooms: List<RoomEntity>,
    val energy: String,
    val minutes: Int,
    val averageRoomScore: Int,
    val completedToday: Int
)

private data class WidgetTaskRowIds(
    val row: Int,
    val check: Int,
    val title: Int,
    val meta: Int
)

private val widgetRowIds = listOf(
    WidgetTaskRowIds(R.id.widget_task_row_1, R.id.widget_task_check_1, R.id.widget_task_title_1, R.id.widget_task_meta_1),
    WidgetTaskRowIds(R.id.widget_task_row_2, R.id.widget_task_check_2, R.id.widget_task_title_2, R.id.widget_task_meta_2),
    WidgetTaskRowIds(R.id.widget_task_row_3, R.id.widget_task_check_3, R.id.widget_task_title_3, R.id.widget_task_meta_3),
    WidgetTaskRowIds(R.id.widget_task_row_4, R.id.widget_task_check_4, R.id.widget_task_title_4, R.id.widget_task_meta_4)
)

private data class WidgetRoomRowIds(
    val row: Int,
    val name: Int,
    val meta: Int,
    val score: Int
)

private val widgetRoomRowIds = listOf(
    WidgetRoomRowIds(R.id.widget_room_row_1, R.id.widget_room_name_1, R.id.widget_room_meta_1, R.id.widget_room_score_1),
    WidgetRoomRowIds(R.id.widget_room_row_2, R.id.widget_room_name_2, R.id.widget_room_meta_2, R.id.widget_room_score_2),
    WidgetRoomRowIds(R.id.widget_room_row_3, R.id.widget_room_name_3, R.id.widget_room_meta_3, R.id.widget_room_score_3)
)

private const val ACTION_COMPLETE_TASK = "com.smithware.tidypilot.widget.COMPLETE_TASK"
private const val ACTION_ANOTHER_TASK = "com.smithware.tidypilot.widget.ANOTHER_TASK"
private const val EXTRA_TASK_ID = "task_id"
private const val WIDGET_PREFS = "tidypilot_widget_prefs"
private const val KEY_EXCLUDED_QUICK_TASK = "excluded_quick_task"
