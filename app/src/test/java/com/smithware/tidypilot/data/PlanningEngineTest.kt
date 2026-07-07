package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningEngineTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 7)
    private val kitchen = RoomEntity(id = "kitchen", name = "Kitchen", roomType = "Kitchen", tidyScore = 58, priority = "high")
    private val living = RoomEntity(id = "living", name = "Living Room", roomType = "Living", tidyScore = 78, priority = "normal")
    private val engine = PlanningEngine()

    @Test
    fun veryLowEnergyOnlySuggestsSmallLowEnergyTasks() {
        val quickTrash = task("trash", "Take out trash", kitchen.id, 3, "low", priority = "high", quick = true)
        val deepClean = task("shower", "Deep clean shower", kitchen.id, 30, "high", priority = "urgent", deep = true)

        val result = engine.buildPlan(
            now = at(18, 30),
            rooms = listOf(kitchen),
            tasks = listOf(deepClean, quickTrash),
            shifts = emptyList(),
            checkIn = EnergyCheckInEntity(date = today, energyLevel = "very low", availableMinutes = 5, afterWorkExhaustion = true),
            scans = emptyList(),
            minimumExhaustedMinutes = 5
        )

        assertEquals(listOf(quickTrash.id), result.suggestedTasks.map { it.id })
        assertEquals(PlanningEngine.LOW_ENERGY_TASK, result.recommendations.single().label)
    }

    @Test
    fun beforeLongShiftKeepsRecommendationsQuickAndBeforeWork() {
        val quickCounter = task("counter", "Clear counter", kitchen.id, 5, "low", priority = "normal", quick = true)
        val vacuum = task("vacuum", "Vacuum living room", living.id, 15, "medium")
        val shift = WorkShiftEntity(date = today, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(18, 0), expectedExhaustionLevel = "high")

        val result = engine.buildPlan(
            now = at(7, 45),
            rooms = listOf(kitchen, living),
            tasks = listOf(vacuum, quickCounter),
            shifts = listOf(shift),
            checkIn = EnergyCheckInEntity(date = today, energyLevel = "medium", availableMinutes = 10),
            scans = emptyList(),
            minimumExhaustedMinutes = 5
        )

        assertEquals("before work", result.workStatus)
        assertEquals(listOf(quickCounter.id), result.suggestedTasks.map { it.id })
        assertTrue(result.recommendations.single().reasons.contains(PlanningEngine.BEFORE_WORK))
    }

    @Test
    fun overdueHighPriorityTaskScoresAboveNormalDueTask() {
        val overdue = task("overdue", "Load dishwasher", kitchen.id, 10, "medium", priority = "high", due = today.minusDays(3))
        val due = task("due", "Reset nightstand", living.id, 10, "medium", priority = "normal", due = today)

        val result = engine.buildPlan(
            now = at(12, 0),
            rooms = listOf(kitchen, living),
            tasks = listOf(due, overdue),
            shifts = emptyList(),
            checkIn = EnergyCheckInEntity(date = today, energyLevel = "medium", availableMinutes = 30),
            scans = emptyList(),
            minimumExhaustedMinutes = 5
        )

        assertEquals(overdue.id, result.suggestedTasks.first().id)
        assertTrue(result.recommendations.first().score > result.recommendations.last().score)
        assertTrue(result.recommendations.first().reasons.contains("Overdue"))
    }

    @Test
    fun recentlyCompletedTaskIsNotRecommendedAgain() {
        val completed = task("completed", "Clear counter", kitchen.id, 5, "low", quick = true)
        val next = task("next", "Take out trash", kitchen.id, 3, "low", quick = true)

        val result = engine.buildPlan(
            now = at(19, 0),
            rooms = listOf(kitchen),
            tasks = listOf(completed, next),
            shifts = emptyList(),
            checkIn = EnergyCheckInEntity(date = today, energyLevel = "low", availableMinutes = 10),
            scans = emptyList(),
            minimumExhaustedMinutes = 5,
            completions = listOf(TaskCompletionEntity(taskId = completed.id, completedAt = at(18, 30)))
        )

        assertFalse(result.suggestedTasks.map { it.id }.contains(completed.id))
        assertEquals(next.id, result.suggestedTasks.single().id)
    }

    private fun at(hour: Int, minute: Int): LocalDateTime = LocalDateTime.of(today, LocalTime.of(hour, minute))

    private fun task(
        id: String,
        name: String,
        roomId: String,
        minutes: Int,
        energy: String,
        priority: String = "normal",
        due: LocalDate = today,
        quick: Boolean = false,
        deep: Boolean = false
    ) = CleaningTaskEntity(
        id = id,
        name = name,
        roomId = roomId,
        priority = priority,
        estimatedMinutes = minutes,
        energyRequired = energy,
        frequencyType = "daily",
        isQuickResetTask = quick,
        isDeepCleanTask = deep,
        nextDueAt = due
    )
}
