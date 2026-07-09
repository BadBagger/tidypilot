package com.smithware.tidypilot.data

import java.time.LocalDate
import kotlin.math.roundToInt

data class TaskNeedScore(
    val score: Int,
    val status: String,
    val explanation: String
)

fun calculateTaskNeedScore(
    task: CleaningTaskEntity,
    room: RoomEntity?,
    completions: List<TaskCompletionEntity>,
    today: LocalDate
): TaskNeedScore {
    val lastCompletedDate = task.lastCompletedAt?.toLocalDate()
        ?: completions
            .filter { it.taskId == task.id }
            .maxByOrNull { it.completedAt }
            ?.completedAt
            ?.toLocalDate()
    val daysSinceLastDone = lastCompletedDate?.let { (today.toEpochDay() - it.toEpochDay()).coerceAtLeast(0).toInt() }
    val frequencyDays = recommendedFrequencyDays(task.frequencyType)
    val dueRatio = when {
        frequencyDays == null && task.nextDueAt == null -> 0.35
        frequencyDays == null -> if (task.nextDueAt?.isAfter(today) == true) 0.25 else 1.0
        daysSinceLastDone != null -> daysSinceLastDone.toDouble() / frequencyDays
        task.nextDueAt != null -> dueRatioFromDueDate(task.nextDueAt, today, frequencyDays)
        else -> 0.7
    }
    val baseNeed = (dueRatio * 55).roundToInt()
    val hygieneBoost = needCategoryWeight(task)
    val roomBoost = when (room?.priority) {
        "urgent" -> 12
        "high" -> 9
        "normal" -> 5
        else -> 2
    }
    val taskBoost = when (task.priority) {
        "urgent" -> 16
        "high" -> 12
        "normal" -> 6
        else -> 2
    }
    val skipBoost = (task.skippedCount * 6).coerceAtMost(18)
    val effortAdjustment = when {
        task.estimatedMinutes <= 5 -> 6
        task.estimatedMinutes <= 15 -> 3
        task.estimatedMinutes >= 60 -> -10
        task.estimatedMinutes >= 30 -> -6
        else -> 0
    }
    val seasonalAdjustment = if (frequencyDays != null && frequencyDays >= 60 && task.priority == "low") -12 else 0
    val score = (baseNeed + hygieneBoost + roomBoost + taskBoost + skipBoost + effortAdjustment + seasonalAdjustment).coerceIn(0, 100)
    val status = when {
        score < 20 -> "Fresh"
        score < 40 -> "Fine"
        score < 60 -> "Due soon"
        score < 80 -> "Needs attention"
        else -> "Overdue"
    }
    val explanation = needExplanation(task, frequencyDays, daysSinceLastDone, today, status)
    return TaskNeedScore(score, status, explanation)
}

fun recommendedFrequencyDays(frequencyType: String): Int? = when (frequencyType.lowercase()) {
    "daily" -> 1
    "every few days" -> 3
    "weekly" -> 7
    "monthly" -> 30
    "seasonal" -> 90
    "quarterly" -> 90
    "one-time" -> null
    else -> parseFrequencyDays(frequencyType)
}

private fun parseFrequencyDays(value: String): Int? {
    val lower = value.lowercase()
    val number = Regex("""\d+""").find(lower)?.value?.toIntOrNull() ?: return null
    return when {
        "day" in lower -> number
        "week" in lower -> number * 7
        "month" in lower -> number * 30
        else -> null
    }
}

private fun dueRatioFromDueDate(nextDueAt: LocalDate?, today: LocalDate, frequencyDays: Int): Double {
    if (nextDueAt == null) return 0.7
    val daysUntilDue = (nextDueAt.toEpochDay() - today.toEpochDay()).toInt()
    return when {
        daysUntilDue < 0 -> 1.0 + (-daysUntilDue.toDouble() / frequencyDays).coerceAtMost(1.0)
        daysUntilDue == 0 -> 1.0
        daysUntilDue <= 2 -> 0.75
        daysUntilDue <= frequencyDays / 2 -> 0.45
        else -> 0.2
    }
}

private fun needCategoryWeight(task: CleaningTaskEntity): Int {
    val searchable = "${task.name} ${task.description} ${task.photoDetectableCategory}".lowercase()
    return when {
        listOf("trash", "garbage", "smell", "odor").any { it in searchable } -> 18
        listOf("dishes", "dishwasher", "sink").any { it in searchable } -> 16
        listOf("toilet", "bathroom", "hygiene", "shower").any { it in searchable } -> 15
        listOf("laundry", "clothes", "towels", "sheets").any { it in searchable } -> 12
        listOf("clutter", "surface", "floor", "visual", "bed").any { it in searchable } -> 8
        else -> 0
    }
}

private fun needExplanation(
    task: CleaningTaskEntity,
    frequencyDays: Int?,
    daysSinceLastDone: Int?,
    today: LocalDate,
    status: String
): String = when {
    daysSinceLastDone != null && frequencyDays != null && daysSinceLastDone >= frequencyDays ->
        "Due because it has been $daysSinceLastDone days since this was last done."
    daysSinceLastDone != null && frequencyDays != null ->
        "Still fine: $daysSinceLastDone of about $frequencyDays days since the last reset."
    task.nextDueAt?.isBefore(today) == true ->
        "Due because it was scheduled for ${task.nextDueAt}."
    task.nextDueAt == today ->
        "Worth doing soon because it is due today."
    task.skippedCount > 0 ->
        "Worth doing soon because it has been skipped ${task.skippedCount} time${if (task.skippedCount == 1) "" else "s"}."
    status == "Fresh" -> "Still fine after the last reset."
    status == "Due soon" -> "Coming up based on its usual cleaning rhythm."
    else -> "Calculated from frequency, room priority, task priority, effort, and visible-need category."
}
