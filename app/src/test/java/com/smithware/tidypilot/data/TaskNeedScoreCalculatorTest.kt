package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskNeedScoreCalculatorTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 8)
    private val lowRoom = RoomEntity(id = "office", name = "Office", roomType = "Office", priority = "low")
    private val kitchen = RoomEntity(id = "kitchen", name = "Kitchen", roomType = "Kitchen", priority = "high")

    @Test
    fun freshTaskStaysFresh() {
        val task = task(
            name = "Dust shelf",
            roomId = lowRoom.id,
            frequency = "monthly",
            priority = "low",
            minutes = 10
        )

        val score = calculateTaskNeedScore(
            task = task,
            room = lowRoom,
            completions = listOf(completion(task.id, today.minusDays(1))),
            today = today
        )

        assertEquals("Fresh", score.status)
        assertTrue(score.score < 20)
    }

    @Test
    fun dueSoonTaskUsesRecommendedFrequency() {
        val task = task(
            name = "Vacuum living room",
            roomId = lowRoom.id,
            frequency = "weekly",
            priority = "normal",
            minutes = 15
        )

        val score = calculateTaskNeedScore(
            task = task,
            room = lowRoom,
            completions = listOf(completion(task.id, today.minusDays(5))),
            today = today
        )

        assertEquals("Due soon", score.status)
        assertTrue(score.explanation.contains("5 of about 7 days"))
    }

    @Test
    fun overdueTaskGetsHighNeedWhenFrequencyIsExceeded() {
        val task = task(
            name = "Take out trash",
            roomId = kitchen.id,
            frequency = "every 2 days",
            priority = "high",
            minutes = 3,
            category = "trash"
        )

        val score = calculateTaskNeedScore(
            task = task,
            room = kitchen,
            completions = listOf(completion(task.id, today.minusDays(4))),
            today = today
        )

        assertEquals("Overdue", score.status)
        assertTrue(score.score >= 80)
        assertTrue(score.explanation.contains("4 days"))
    }

    @Test
    fun highPriorityHygieneTaskScoresAboveNormalClutter() {
        val hygiene = task(
            name = "Clean toilet",
            roomId = kitchen.id,
            frequency = "weekly",
            priority = "high",
            minutes = 8,
            category = "bathroom reset"
        )
        val clutter = task(
            name = "Sort one shelf",
            roomId = lowRoom.id,
            frequency = "weekly",
            priority = "normal",
            minutes = 8,
            category = "clutter"
        )

        val hygieneScore = calculateTaskNeedScore(hygiene, kitchen, listOf(completion(hygiene.id, today.minusDays(6))), today)
        val clutterScore = calculateTaskNeedScore(clutter, lowRoom, listOf(completion(clutter.id, today.minusDays(6))), today)

        assertTrue(hygieneScore.score > clutterScore.score)
        assertTrue(hygieneScore.status == "Needs attention" || hygieneScore.status == "Overdue")
    }

    @Test
    fun lowPrioritySeasonalTaskCanBeDueWithoutTakingOver() {
        val oven = task(
            name = "Clean oven",
            roomId = lowRoom.id,
            frequency = "seasonal",
            priority = "low",
            minutes = 60,
            category = "other"
        )
        val trash = task(
            name = "Take out trash",
            roomId = kitchen.id,
            frequency = "every 2 days",
            priority = "high",
            minutes = 3,
            category = "trash"
        )

        val ovenScore = calculateTaskNeedScore(oven, lowRoom, listOf(completion(oven.id, today.minusDays(100))), today)
        val trashScore = calculateTaskNeedScore(trash, kitchen, listOf(completion(trash.id, today.minusDays(4))), today)

        assertTrue(ovenScore.status == "Due soon" || ovenScore.status == "Needs attention")
        assertTrue(ovenScore.score < trashScore.score)
    }

    private fun task(
        name: String,
        roomId: String,
        frequency: String,
        priority: String,
        minutes: Int,
        category: String = "other"
    ) = CleaningTaskEntity(
        id = name.lowercase().replace(" ", "-"),
        name = name,
        roomId = roomId,
        frequencyType = frequency,
        priority = priority,
        estimatedMinutes = minutes,
        photoDetectableCategory = category,
        nextDueAt = today
    )

    private fun completion(taskId: String, date: LocalDate) = TaskCompletionEntity(
        taskId = taskId,
        completedAt = LocalDateTime.of(date, LocalTime.NOON)
    )
}
