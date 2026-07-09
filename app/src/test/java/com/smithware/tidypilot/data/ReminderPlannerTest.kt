package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {
    private val today = LocalDate.of(2026, 7, 8)
    private val room = RoomEntity(id = "kitchen", name = "Kitchen", roomType = "Kitchen", tidyScore = 58, priority = "high")
    private val task = CleaningTaskEntity(
        id = "counters",
        name = "Wipe kitchen counters",
        roomId = room.id,
        priority = "high",
        estimatedMinutes = 5,
        frequencyType = "daily",
        photoDetectableCategory = "surface wipe",
        lastCompletedAt = today.minusDays(3).atTime(9, 0),
        nextDueAt = today
    )

    @Test
    fun gentleToneUsesSupportiveCopy() {
        val copy = ReminderPlanner.copyForTone("Gentle", "quick_win", task.name, room.name)

        assertEquals("One quick win?", copy.first)
        assertTrue(copy.second.contains("small reset"))
    }

    @Test
    fun directToneNamesTheDueTask() {
        val copy = ReminderPlanner.copyForTone("Direct", "task", task.name, room.name)

        assertEquals("Wipe kitchen counters is due today.", copy.first)
    }

    @Test
    fun quietHoursHandleOvernightWindow() {
        assertTrue(ReminderPlanner.isQuietHour(LocalTime.of(22, 30), "21:00", "08:00"))
        assertTrue(ReminderPlanner.isQuietHour(LocalTime.of(7, 30), "21:00", "08:00"))
        assertFalse(ReminderPlanner.isQuietHour(LocalTime.of(12, 0), "21:00", "08:00"))
    }

    @Test
    fun plannerRespectsMaxRemindersPerDay() {
        val settings = AppSettingsEntity(
            reminderEnabled = true,
            maxRemindersPerDay = 2,
            reminderTone = "Minimal",
            quietHoursStart = "23:00",
            quietHoursEnd = "06:00"
        )

        val reminders = ReminderPlanner.plan(settings, listOf(task), listOf(room), emptyList(), today, LocalTime.NOON)

        assertEquals(2, reminders.size)
    }

    @Test
    fun quietDaySuppressesReminders() {
        val settings = AppSettingsEntity(
            reminderEnabled = true,
            quietDays = "Wednesday",
            quietHoursStart = "23:00",
            quietHoursEnd = "06:00"
        )

        val reminders = ReminderPlanner.plan(settings, listOf(task), listOf(room), emptyList(), today, LocalTime.NOON)

        assertTrue(reminders.isEmpty())
    }
}
