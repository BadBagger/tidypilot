package com.smithware.tidypilot.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedCleaningPlansTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 8)

    @Test
    fun includesRequiredGuidedPlans() {
        val titles = guidedCleaningPlans.map { it.title }.toSet()

        listOf(
            "Daily reset",
            "Weekly reset",
            "Monthly deep clean",
            "Spring cleaning",
            "Fall cleaning",
            "Move-in / move-out cleaning",
            "Guest coming over",
            "New baby / pet / roommate setup",
            "Holiday hosting reset"
        ).forEach { assertTrue("Missing plan $it", it in titles) }
    }

    @Test
    fun guestPlanIncludesRequestedChecklistItems() {
        val guest = guidedCleaningPlans.first { it.id == "guest_coming" }
        val stepNames = guest.steps.map { it.title }.toSet()

        listOf(
            "Clear entryway",
            "Wipe bathroom sink",
            "Clean toilet",
            "Take out trash",
            "Wipe kitchen counters",
            "Vacuum visible areas",
            "Hide clutter basket",
            "Light reset task"
        ).forEach { assertTrue("Missing guest step $it", it in stepNames) }
    }

    @Test
    fun thirtyMinuteModeKeepsChecklistSmall() {
        val guest = guidedCleaningPlans.first { it.id == "guest_coming" }

        val generated = generateGuidedPlan(
            plan = guest,
            existingRooms = emptyList(),
            includedRoomCategories = guest.suggestedRooms.toSet(),
            onlyThirtyMinutes = true,
            spreadDays = 1,
            today = today
        )

        assertTrue(generated.totalMinutes <= 30 || generated.tasks.size == 1)
    }

    @Test
    fun canSkipIrrelevantRooms() {
        val guest = guidedCleaningPlans.first { it.id == "guest_coming" }

        val generated = generateGuidedPlan(
            plan = guest,
            existingRooms = emptyList(),
            includedRoomCategories = setOf("Bathroom"),
            onlyThirtyMinutes = false,
            spreadDays = 1,
            today = today
        )

        assertTrue(generated.tasks.all { it.name.contains("bathroom", ignoreCase = true) || it.name.contains("toilet", ignoreCase = true) })
    }

    @Test
    fun spreadOverThreeDaysDistributesDueDates() {
        val weekly = guidedCleaningPlans.first { it.id == "weekly_reset" }

        val generated = generateGuidedPlan(
            plan = weekly,
            existingRooms = emptyList(),
            includedRoomCategories = weekly.suggestedRooms.toSet(),
            onlyThirtyMinutes = false,
            spreadDays = 3,
            today = today
        )

        assertEquals(3, generated.spreadDays)
        assertTrue(generated.tasks.any { it.nextDueAt == today.plusDays(1) })
        assertTrue(generated.tasks.any { it.nextDueAt == today.plusDays(2) })
    }
}
