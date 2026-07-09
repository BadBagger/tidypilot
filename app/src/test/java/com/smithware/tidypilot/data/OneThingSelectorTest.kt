package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OneThingSelectorTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 8)
    private val kitchen = RoomEntity(id = "kitchen", name = "Kitchen", roomType = "Kitchen", priority = "high")
    private val bedroom = RoomEntity(id = "bedroom", name = "Bedroom", roomType = "Bedroom", priority = "normal")

    @Test
    fun twoMinuteModePicksTinyTaskThatFits() {
        val tiny = task("trash", "Take out trash", kitchen.id, 2, "low", "every few days", "trash", today.minusDays(1), quick = true)
        val bigger = task("dishes", "Wash dishes", kitchen.id, 10, "medium", "daily", "dishes", today.minusDays(1))

        val selected = selectOneThingTask(
            tasks = listOf(bigger, tiny),
            rooms = listOf(kitchen),
            completions = emptyList(),
            today = today,
            availableMinutes = 2,
            energyLevel = "low"
        )

        assertEquals(tiny.id, selected?.id)
    }

    @Test
    fun lowEnergyAvoidsHighEnergyDeepTask() {
        val quick = task("counter", "Wipe counter", kitchen.id, 5, "low", "daily", "surface wipe", today, quick = true)
        val deep = task("shower", "Scrub shower", kitchen.id, 15, "high", "weekly", "bathroom reset", today.minusDays(10), priority = "urgent")

        val selected = selectOneThingTask(
            tasks = listOf(deep, quick),
            rooms = listOf(kitchen),
            completions = emptyList(),
            today = today,
            availableMinutes = 15,
            energyLevel = "low"
        )

        assertEquals(quick.id, selected?.id)
    }

    @Test
    fun highNeedOverdueTaskWinsWhenItFits() {
        val trash = task("trash", "Take out trash", kitchen.id, 3, "low", "every 2 days", "trash", today.minusDays(4), priority = "high", quick = true)
        val bed = task("bed", "Make bed", bedroom.id, 3, "low", "daily", "bed reset", today.plusDays(1), priority = "normal", quick = true)

        val selected = selectOneThingTask(
            tasks = listOf(bed, trash),
            rooms = listOf(kitchen, bedroom),
            completions = listOf(completion(bed.id, today)),
            today = today,
            availableMinutes = 5,
            energyLevel = "low"
        )

        assertEquals(trash.id, selected?.id)
    }

    @Test
    fun excludedTaskIsSwappedOut() {
        val first = task("counter", "Wipe counter", kitchen.id, 5, "low", "daily", "surface wipe", today.minusDays(2), priority = "high", quick = true)
        val second = task("sink", "Clear sink", kitchen.id, 5, "low", "daily", "dishes", today.minusDays(1), priority = "normal", quick = true)

        val selected = selectOneThingTask(
            tasks = listOf(first, second),
            rooms = listOf(kitchen),
            completions = emptyList(),
            today = today,
            availableMinutes = 5,
            energyLevel = "low",
            excludedTaskIds = setOf(first.id)
        )

        assertEquals(second.id, selected?.id)
    }

    @Test
    fun returnsNullWhenNothingFits() {
        val deep = task("garage", "Deep reset garage", kitchen.id, 60, "high", "monthly", "clutter", today.minusDays(30))

        val selected = selectOneThingTask(
            tasks = listOf(deep),
            rooms = listOf(kitchen),
            completions = emptyList(),
            today = today,
            availableMinutes = 5,
            energyLevel = "low"
        )

        assertNull(selected)
    }

    private fun task(
        id: String,
        name: String,
        roomId: String,
        minutes: Int,
        energy: String,
        frequency: String,
        category: String,
        due: LocalDate,
        priority: String = "normal",
        quick: Boolean = false
    ) = CleaningTaskEntity(
        id = id,
        name = name,
        roomId = roomId,
        priority = priority,
        estimatedMinutes = minutes,
        energyRequired = energy,
        frequencyType = frequency,
        isQuickResetTask = quick,
        photoDetectableCategory = category,
        nextDueAt = due
    )

    private fun completion(taskId: String, date: LocalDate) = TaskCompletionEntity(
        taskId = taskId,
        completedAt = LocalDateTime.of(date, LocalTime.NOON)
    )
}
