package com.smithware.tidypilot.data

import java.time.LocalDate

data class TidyRoomScore(
    val score: Int,
    val label: String,
    val reason: String,
    val openTasks: Int,
    val overdueTasks: Int,
    val openIssues: Int,
    val lastCompletedLabel: String
)

fun calculateRoomScore(
    room: RoomEntity,
    tasks: List<CleaningTaskEntity>,
    scans: List<RoomPhotoScanEntity>,
    issues: List<ScanIssueEntity>,
    completions: List<TaskCompletionEntity>,
    today: LocalDate
): TidyRoomScore {
    val roomTasks = tasks.filter { it.roomId == room.id && !it.isArchived }
    val openTasks = roomTasks.size
    val overdueTasks = roomTasks.count { it.nextDueAt?.isBefore(today) == true }
    val roomScanIds = scans.filter { it.roomId == room.id }.map { it.id }.toSet()
    val latestScan = scans.filter { it.roomId == room.id }.maxByOrNull { it.scanDate }
    val openIssues = issues.count { it.scanId in roomScanIds }
    val taskIds = roomTasks.map { it.id }.toSet()
    val lastCompleted = completions
        .filter { it.taskId in taskIds }
        .maxByOrNull { it.completedAt }
        ?.completedAt
        ?.toLocalDate()
    val daysSinceCompletion = lastCompleted?.let { today.toEpochDay() - it.toEpochDay() }
    val lastCompletedLabel = when {
        lastCompleted == null -> "Not yet"
        daysSinceCompletion == 0L -> "Today"
        daysSinceCompletion == 1L -> "Yesterday"
        else -> "${daysSinceCompletion} days ago"
    }
    val priorityPenalty = when (room.priority) {
        "urgent" -> 12
        "high" -> 8
        "normal" -> 4
        else -> 0
    }
    val completionPenalty = when {
        lastCompleted == null -> 10
        daysSinceCompletion != null && daysSinceCompletion > 14 -> 15
        daysSinceCompletion != null && daysSinceCompletion > 7 -> 10
        daysSinceCompletion != null && daysSinceCompletion > 3 -> 5
        else -> 0
    }
    val score = (100 -
        (openTasks * 4).coerceAtMost(24) -
        (overdueTasks * 8).coerceAtMost(24) -
        (openIssues * 6).coerceAtMost(24) -
        ((latestScan?.messScore ?: 0) / 5).coerceAtMost(20) -
        completionPenalty -
        priorityPenalty
    ).coerceIn(0, 100)
    val label = when {
        (room.priority == "urgent" || room.priority == "high") && (overdueTasks > 0 || openIssues > 0 || score < 80) -> "Priority room"
        score >= 80 -> "Good"
        score >= 65 -> "Needs a quick reset"
        else -> "Needs attention"
    }
    val reason = when {
        overdueTasks > 0 -> "$overdueTasks overdue task${if (overdueTasks == 1) "" else "s"}"
        openIssues > 0 -> "$openIssues scan issue${if (openIssues == 1) "" else "s"} to review"
        openTasks > 0 -> "$openTasks open task${if (openTasks == 1) "" else "s"}"
        completionPenalty > 0 -> "No recent reset logged"
        else -> "Steady right now"
    }
    return TidyRoomScore(score, label, reason, openTasks, overdueTasks, openIssues, lastCompletedLabel)
}
