package com.smithware.tidypilot.data

import org.junit.Assert.assertTrue
import org.junit.Test

class SupplyTrackingTest {
    @Test
    fun mopTaskSuggestsFloorSupplies() {
        val task = CleaningTaskEntity(
            name = "Mop kitchen floor",
            roomId = "kitchen",
            photoDetectableCategory = "floor clutter"
        )

        val supplies = suggestedSupplyNames(task)

        assertTrue("Mop" in supplies)
        assertTrue("Floor cleaner" in supplies)
        assertTrue("Bucket" in supplies)
    }

    @Test
    fun toiletTaskSuggestsBathroomSupplies() {
        val task = CleaningTaskEntity(
            name = "Clean toilet",
            roomId = "bathroom",
            photoDetectableCategory = "bathroom reset"
        )

        val supplies = suggestedSupplyNames(task)

        assertTrue("Toilet cleaner" in supplies)
        assertTrue("Toilet brush" in supplies)
        assertTrue("Gloves" in supplies)
    }
}
