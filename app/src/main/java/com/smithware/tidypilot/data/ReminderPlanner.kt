package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalTime

data class PlannedReminder(
    val type: String,
    val key: String,
    val title: String,
    val body: String,
    val taskId: String? = null,
    val roomId: String? = null
)

object ReminderPlanner {
    private val defaultTypes = listOf("daily", "task", "room", "weekly", "seasonal", "quick_win")

    fun plan(
        settings: AppSettingsEntity,
        tasks: List<CleaningTaskEntity>,
        rooms: List<RoomEntity>,
        completions: List<TaskCompletionEntity>,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now()
    ): List<PlannedReminder> {
        if (!settings.reminderEnabled) return emptyList()
        if (today.dayOfWeek.name.lowercase() in settings.quietDays.unpipe().map { it.lowercase() }) return emptyList()
        if (isQuietHour(now, settings.quietHoursStart, settings.quietHoursEnd)) return emptyList()

        val enabled = settings.enabledReminderTypes.unpipe().ifEmpty { defaultTypes }.map { it.lowercase() }.toSet()
        val openTasks = tasks.filterNot { it.isArchived }
        val taskScores = openTasks.map { task ->
            val room = rooms.firstOrNull { it.id == task.roomId }
            task to calculateTaskNeedScore(task, room, completions, today)
        }
        val topTask = taskScores
            .filter { (_, score) -> score.score >= 45 }
            .maxByOrNull { (_, score) -> score.score }
        val quickWin = taskScores
            .filter { (task, _) -> task.estimatedMinutes <= 5 || task.isQuickResetTask }
            .maxByOrNull { (_, score) -> score.score }
        val roomNeedingReset = rooms.filterNot { it.isArchived }.minByOrNull { it.tidyScore }
        val seasonalTask = taskScores
            .filter { (task, score) -> task.frequencyType.equals("seasonal", true) && score.score >= 40 }
            .maxByOrNull { (_, score) -> score.score }

        val reminders = buildList {
            if ("daily" in enabled) add(dailyReminder(settings, openTasks, rooms))
            if ("task" in enabled && topTask != null) add(taskReminder(settings, topTask.first, topTask.second, rooms.firstOrNull { it.id == topTask.first.roomId }))
            if ("room" in enabled && roomNeedingReset != null && roomNeedingReset.tidyScore < 70) add(roomReminder(settings, roomNeedingReset))
            if ("weekly" in enabled) add(weeklyReminder(settings, taskScores))
            if ("seasonal" in enabled && seasonalTask != null) add(seasonalReminder(settings, seasonalTask.first, rooms.firstOrNull { it.id == seasonalTask.first.roomId }))
            if ("quick_win" in enabled && quickWin != null) add(quickWinReminder(settings, quickWin.first, rooms.firstOrNull { it.id == quickWin.first.roomId }))
        }

        return reminders
            .distinctBy { it.key }
            .take(settings.maxRemindersPerDay.coerceIn(1, 6))
    }

    fun copyForTone(tone: String, type: String, taskName: String?, roomName: String?, dueCount: Int = 1): Pair<String, String> {
        val task = taskName ?: "one quick task"
        val room = roomName ?: "your home"
        return when (tone.lowercase()) {
            "direct" -> when (type) {
                "task" -> "$task is due today." to "$room can use this reset when you have a minute."
                "room" -> "$room needs attention." to "Pick one small reset to bring it back under control."
                "weekly" -> "Weekly reset is ready." to "$dueCount cleaning task${if (dueCount == 1) "" else "s"} worth doing soon."
                "seasonal" -> "$task is coming up." to "Seasonal tasks stay easier when they are handled in small steps."
                "quick_win" -> "Quick win available." to "$task can help $room feel better fast."
                else -> "$dueCount cleaning task${if (dueCount == 1) "" else "s"} due." to "Open TidyPilot to pick the next reset."
            }
            "minimal" -> when (type) {
                "quick_win" -> "Quick reset available." to task
                "room" -> "$room reset." to "1 room needs attention."
                else -> "$dueCount cleaning task${if (dueCount == 1) "" else "s"} due." to task
            }
            else -> when (type) {
                "task" -> "Tiny reset?" to "$task can make $room feel better."
                "room" -> "A small reset would help." to "$room could use one manageable task."
                "weekly" -> "Want a weekly reset?" to "A few small chores are ready when you are."
                "seasonal" -> "Seasonal task coming up." to "$task can be handled as one calm step."
                "quick_win" -> "One quick win?" to "$task is a small reset that still counts."
                else -> "Tiny reset?" to "One quick task can make $room feel better."
            }
        }
    }

    fun isQuietHour(now: LocalTime, start: String, end: String): Boolean {
        val startTime = parseTime(start) ?: return false
        val endTime = parseTime(end) ?: return false
        return if (startTime <= endTime) {
            !now.isBefore(startTime) && now.isBefore(endTime)
        } else {
            !now.isBefore(startTime) || now.isBefore(endTime)
        }
    }

    private fun dailyReminder(settings: AppSettingsEntity, tasks: List<CleaningTaskEntity>, rooms: List<RoomEntity>): PlannedReminder {
        val room = rooms.minByOrNull { it.tidyScore }?.name ?: "your home"
        val (title, body) = copyForTone(settings.reminderTone, "daily", null, room, tasks.size.coerceAtLeast(1))
        return PlannedReminder("daily", "daily-${LocalDate.now()}", title, body)
    }

    private fun taskReminder(settings: AppSettingsEntity, task: CleaningTaskEntity, score: TaskNeedScore, room: RoomEntity?): PlannedReminder {
        val (title, body) = copyForTone(settings.reminderTone, "task", task.name, room?.name, 1)
        return PlannedReminder("task", "task-${task.id}", title, "$body ${score.status}: ${score.explanation}", taskId = task.id, roomId = task.roomId)
    }

    private fun roomReminder(settings: AppSettingsEntity, room: RoomEntity): PlannedReminder {
        val (title, body) = copyForTone(settings.reminderTone, "room", null, room.name, 1)
        return PlannedReminder("room", "room-${room.id}", title, body, roomId = room.id)
    }

    private fun weeklyReminder(settings: AppSettingsEntity, taskScores: List<Pair<CleaningTaskEntity, TaskNeedScore>>): PlannedReminder {
        val dueCount = taskScores.count { (_, score) -> score.score >= 50 }.coerceAtLeast(1)
        val (title, body) = copyForTone(settings.reminderTone, "weekly", null, null, dueCount)
        return PlannedReminder("weekly", "weekly-${LocalDate.now()}", title, body)
    }

    private fun seasonalReminder(settings: AppSettingsEntity, task: CleaningTaskEntity, room: RoomEntity?): PlannedReminder {
        val (title, body) = copyForTone(settings.reminderTone, "seasonal", task.name, room?.name, 1)
        return PlannedReminder("seasonal", "seasonal-${task.id}", title, body, taskId = task.id, roomId = task.roomId)
    }

    private fun quickWinReminder(settings: AppSettingsEntity, task: CleaningTaskEntity, room: RoomEntity?): PlannedReminder {
        val (title, body) = copyForTone(settings.reminderTone, "quick_win", task.name, room?.name, 1)
        return PlannedReminder("quick_win", "quick-win-${task.id}", title, body, taskId = task.id, roomId = task.roomId)
    }

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value.trim()) }.getOrNull()
}
