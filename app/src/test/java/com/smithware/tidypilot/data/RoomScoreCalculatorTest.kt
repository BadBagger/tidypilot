package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomScoreCalculatorTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 7)
    private val kitchen = RoomEntity(id = "kitchen", name = "Kitchen", roomType = "Kitchen", priority = "high")

    @Test
    fun roomScoreCountsOpenTasksOverdueTasksAndScanIssues() {
        val overdue = task("overdue", today.minusDays(2))
        val current = task("current", today)
        val scan = RoomPhotoScanEntity(
            id = "scan",
            roomId = kitchen.id,
            imageUri = "demo://kitchen",
            tidyScore = 55,
            messScore = 45,
            detectedIssueTags = "dishes_visible",
            estimatedCleanupMinutes = 10,
            confidenceSummary = "local estimate"
        )
        val issue = ScanIssueEntity(
            scanId = scan.id,
            tag = "dishes_visible",
            label = "Dishes visible",
            confidence = 0.7f,
            suggestedAction = "Load dishwasher",
            estimatedMinutes = 10,
            energyLevel = "medium"
        )

        val score = calculateRoomScore(
            room = kitchen,
            tasks = listOf(overdue, current),
            scans = listOf(scan),
            issues = listOf(issue),
            completions = emptyList(),
            today = today
        )

        assertEquals(2, score.openTasks)
        assertEquals(1, score.overdueTasks)
        assertEquals(1, score.openIssues)
        assertEquals("Priority room", score.label)
        assertTrue(score.reason.contains("overdue"))
    }

    @Test
    fun recentCompletionImprovesScoreAndLastCompletedLabel() {
        val task = task("counter", today.plusDays(1))
        val score = calculateRoomScore(
            room = kitchen.copy(priority = "normal"),
            tasks = listOf(task),
            scans = emptyList(),
            issues = emptyList(),
            completions = listOf(TaskCompletionEntity(taskId = task.id, completedAt = LocalDateTime.of(today, LocalTime.of(10, 0)))),
            today = today
        )

        assertEquals("Today", score.lastCompletedLabel)
        assertEquals(0, score.overdueTasks)
        assertTrue(score.score >= 80)
        assertEquals("Good", score.label)
    }

    @Test
    fun heavyRecentScanLowersRoomScore() {
        val scan = RoomPhotoScanEntity(
            id = "heavy-scan",
            roomId = kitchen.id,
            imageUri = "demo://kitchen",
            tidyScore = 18,
            messScore = 82,
            messLevel = "heavy_reset",
            confidence = "medium",
            summary = "Bigger reset suggested.",
            detectedIssueTags = "floor_clutter|trash_visible",
            estimatedCleanupMinutes = 30,
            confidenceSummary = "Estimated from scan review."
        )

        val score = calculateRoomScore(
            room = kitchen.copy(priority = "normal"),
            tasks = emptyList(),
            scans = listOf(scan),
            issues = listOf(
                ScanIssueEntity(
                    scanId = scan.id,
                    roomId = kitchen.id,
                    tag = "floor_clutter",
                    label = "Floor clutter",
                    confidence = 0.7f,
                    suggestedAction = "Clear floor path",
                    estimatedMinutes = 8,
                    energyLevel = "low"
                )
            ),
            completions = emptyList(),
            today = today
        )

        assertTrue(score.score < 75)
        assertEquals("Needs attention", score.label)
    }

    private fun task(id: String, due: LocalDate) = CleaningTaskEntity(
        id = id,
        name = id,
        roomId = kitchen.id,
        estimatedMinutes = 5,
        energyRequired = "low",
        nextDueAt = due
    )
}
