package com.smithware.tidypilot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPhotoAnalyzerTest {
    @Test
    fun messScoreMapsToFriendlyMessLevel() {
        assertEquals(MessLevel.CLEAR, messLevelForScore(12))
        assertEquals(MessLevel.LIGHT_RESET, messLevelForScore(38))
        assertEquals(MessLevel.MODERATE_MESS, messLevelForScore(62))
        assertEquals(MessLevel.HEAVY_RESET, messLevelForScore(82))
    }

    @Test
    fun taskSuggestionLimitsFollowMessLevel() {
        assertEquals(1, taskLimitForMessLevel(MessLevel.CLEAR))
        assertEquals(2, taskLimitForMessLevel(MessLevel.LIGHT_RESET))
        assertEquals(4, taskLimitForMessLevel(MessLevel.MODERATE_MESS))
        assertEquals(6, taskLimitForMessLevel(MessLevel.HEAVY_RESET))
    }

    @Test
    fun lowEnergyFilteringKeepsQuickEasySuggestions() {
        val issues = listOf(
            AnalysisIssue("trash_visible", "Trash", "Take out trash", 3, "low", 0.8f),
            AnalysisIssue("floor_clutter", "Floor", "Clear floor path", 8, "low", 0.7f),
            AnalysisIssue("shower_reset_needed", "Shower", "Reset shower", 20, "medium", 0.6f)
        )

        val filtered = filterScanTasksForEnergy(issues, "low", MessLevel.HEAVY_RESET)

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.energyLevel == "low" && it.estimatedMinutes <= 10 })
    }

    @Test
    fun basementScanSuggestsRelevantStorageAndFloorTasks() {
        val room = RoomEntity(
            id = "basement",
            name = "Basement",
            roomType = "Basement",
            tidyScore = 42,
            priority = "high"
        )

        val result = RoomPhotoAnalyzer().analyze(
            room = room,
            note = "super untidy basement with workout bench shelves cords and floor clutter",
            imageUri = "content://local/basement"
        )

        val tags = result.issues.map { it.tag }.toSet()
        assertTrue(result.tidyScore <= 25)
        assertTrue(result.messScore >= 75)
        assertEquals(MessLevel.HEAVY_RESET, result.messLevel)
        assertEquals("high", result.confidence)
        assertTrue(result.estimatedCleanupMinutes >= 30)
        assertTrue(result.summary.contains("Bigger reset", ignoreCase = true))
        assertTrue(result.detectedZones.isNotEmpty())
        assertTrue("basement_floor_path" in tags)
        assertTrue("storage_shelf_clutter" in tags)
        assertTrue("loose_gear_visible" in tags)
        assertTrue(result.issues.any { it.suggestedAction.contains("walking path", ignoreCase = true) })
    }

    @Test
    fun kitchenScanStillKeepsKitchenSpecificActions() {
        val room = RoomEntity(id = "kitchen", name = "Kitchen", roomType = "Kitchen")

        val result = RoomPhotoAnalyzer().analyze(
            room = room,
            note = "dishes and counters",
            imageUri = "content://local/kitchen"
        )

        assertEquals("dishes_visible", result.issues.first().tag)
        assertTrue(result.issues.any { it.suggestedAction.contains("dishes", ignoreCase = true) })
    }

    @Test
    fun bedroomScanUsesVisibleDetailsForSpecificTasks() {
        val room = RoomEntity(id = "bedroom2", name = "Bedroom 2", roomType = "Bedroom")

        val result = RoomPhotoAnalyzer().analyze(
            room = room,
            note = "laundry visible, floor clutter, nightstand clutter, closet or boxes",
            imageUri = "content://local/bedroom"
        )

        val tags = result.issues.map { it.tag }.toSet()
        assertTrue("laundry_visible" in tags)
        assertTrue("floor_clutter" in tags)
        assertTrue("bedroom_surface_clutter" in tags)
        assertTrue("closet_or_box_clutter" in tags)
    }

    @Test
    fun livingRoomScanUsesVisibleDetailsForSpecificTasks() {
        val room = RoomEntity(id = "living", name = "Living Room", roomType = "Living Room")

        val result = RoomPhotoAnalyzer().analyze(
            room = room,
            note = "coffee table clutter, couch blankets, electronics cords, vacuum needed",
            imageUri = "content://local/living"
        )

        val tags = result.issues.map { it.tag }.toSet()
        assertTrue("living_surface_clutter" in tags)
        assertTrue("couch_reset_needed" in tags)
        assertTrue("electronics_clutter" in tags)
        assertTrue("floor_clean_needed" in tags)
    }

    @Test
    fun blankBathroomScanDoesNotDefaultToHighTidyScore() {
        val room = RoomEntity(id = "bathroom", name = "Bathroom", roomType = "Bathroom", tidyScore = 72)

        val result = RoomPhotoAnalyzer().analyze(
            room = room,
            note = "",
            imageUri = "content://local/bathroom"
        )

        assertTrue(result.tidyScore <= 58)
        assertTrue(result.messScore >= 42)
        assertEquals("low", result.confidence)
        assertTrue(result.issues.size >= 3)
        assertTrue(result.confidenceSummary.contains("Review", ignoreCase = true))
    }

    @Test
    fun trashedBedroomScanGetsLowScoreAndEnoughTasks() {
        val room = RoomEntity(id = "bedroom", name = "Bedroom", roomType = "Bedroom", tidyScore = 63)

        val result = RoomPhotoAnalyzer().analyze(
            room = room,
            note = "trashed room with laundry visible floor clutter nightstand clutter boxes trash",
            imageUri = "content://local/bedroom"
        )

        val tags = result.issues.map { it.tag }.toSet()
        assertTrue(result.tidyScore <= 20)
        assertTrue(result.messScore >= 80)
        assertTrue(result.estimatedCleanupMinutes >= 25)
        assertTrue("laundry_visible" in tags)
        assertTrue("floor_clutter" in tags)
        assertTrue("bedroom_surface_clutter" in tags)
        assertTrue("trash_visible" in tags)
    }
}
