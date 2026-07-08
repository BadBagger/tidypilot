package com.smithware.tidypilot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterRoutineProfileTest {
    @Test
    fun starterRoomsRespectBedroomAndBathroomCounts() {
        val rooms = starterRoomsFor(
            StarterRoutineProfile(
                selectedRooms = setOf("Kitchen", "Bedroom", "Bathroom", "Laundry"),
                bedroomCount = 3,
                bathroomCount = 2
            )
        ).map { it.name }

        assertEquals(
            listOf("Kitchen", "Laundry", "Bedroom", "Bedroom 2", "Bedroom 3", "Bathroom", "Bathroom 2"),
            rooms
        )
    }

    @Test
    fun starterTemplatesIncludeGoalSpecificTasks() {
        val templates = starterRoutineTemplates(
            StarterRoutineProfile(
                goals = setOf("Catch up from mess", "Guest ready", "Low energy maintenance"),
                delegationInterest = true
            )
        ).map { it.name }

        assertTrue("catch-up reset task should be added", "Sort one visible pile" in templates)
        assertTrue("guest-ready entryway task should be added", "Entryway reset" in templates)
        assertTrue("low-energy reset task should be added", "Five-minute reset basket" in templates)
        assertTrue("delegation candidate should be added", "Kid-friendly toy pickup" in templates)
    }
}
