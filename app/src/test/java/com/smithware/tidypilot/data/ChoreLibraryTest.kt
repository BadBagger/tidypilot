package com.smithware.tidypilot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoreLibraryTest {
    @Test
    fun libraryIncludesRequiredRoomsAndFrequencies() {
        val rooms = choreLibrary.map { it.roomCategory }.toSet()
        val frequencies = choreLibrary.map { it.suggestedFrequency }.toSet()

        listOf(
            "Kitchen",
            "Bathroom",
            "Bedroom",
            "Living room",
            "Laundry",
            "Entryway",
            "Office",
            "Garage",
            "Basement",
            "Pet area",
            "Whole home"
        ).forEach { assertTrue("Missing room category $it", it in rooms) }

        listOf(
            "Daily",
            "Every few days",
            "Weekly",
            "Monthly",
            "Seasonal",
            "Annual",
            "As needed"
        ).forEach { assertTrue("Missing frequency $it", it in frequencies) }
    }

    @Test
    fun examplesArePresentWithExpectedMetadata() {
        val counters = choreLibrary.first { it.name == "Wipe kitchen counters" }
        val toilet = choreLibrary.first { it.name == "Clean toilet" }
        val windows = choreLibrary.first { it.name == "Wash windows" }
        val hvac = choreLibrary.first { it.name == "Replace HVAC filter" }

        assertEquals("Daily", counters.suggestedFrequency)
        assertEquals(5, counters.estimatedMinutes)
        assertEquals("Weekly", toilet.suggestedFrequency)
        assertEquals("high", toilet.hygieneImportance)
        assertTrue(windows.seasonal)
        assertTrue(hvac.suppliesNeeded.contains("filter"))
    }

    @Test
    fun frequencyConversionMatchesTaskModel() {
        assertEquals("daily", libraryFrequencyToTaskFrequency("Daily"))
        assertEquals("every few days", libraryFrequencyToTaskFrequency("Every few days"))
        assertEquals("every 14 days", libraryFrequencyToTaskFrequency("Biweekly"))
        assertEquals("seasonal", libraryFrequencyToTaskFrequency("Seasonal"))
        assertEquals("one-time", libraryFrequencyToTaskFrequency("As needed"))
    }
}
