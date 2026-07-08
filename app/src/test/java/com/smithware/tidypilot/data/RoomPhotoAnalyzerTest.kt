package com.smithware.tidypilot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPhotoAnalyzerTest {
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
        assertTrue(result.estimatedCleanupMinutes >= 30)
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
}
