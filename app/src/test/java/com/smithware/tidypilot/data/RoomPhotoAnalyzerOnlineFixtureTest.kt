package com.smithware.tidypilot.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPhotoAnalyzerOnlineFixtureTest {
    private val analyzer = RoomPhotoAnalyzer()

    @Test
    fun onlineReferenceFixturesProduceExpectedMessLevelsAndIssues() {
        onlineFixtures.forEach { fixture ->
            val result = analyzer.analyze(
                room = RoomEntity(
                    id = fixture.id,
                    name = fixture.roomName,
                    roomType = fixture.roomType,
                    tidyScore = fixture.startingRoomScore,
                    priority = fixture.priority
                ),
                note = fixture.visibleContext,
                imageUri = fixture.sourceUrl
            )

            val actualTags = result.issues.map { it.tag }.toSet()
            assertEquals("mess level for ${fixture.id}", fixture.expectedMessLevel, result.messLevel)
            assertTrue(
                "mess score for ${fixture.id} should be at least ${fixture.minimumMessScore}, was ${result.messScore}",
                result.messScore >= fixture.minimumMessScore
            )
            fixture.expectedTags.forEach { expected ->
                assertTrue("expected $expected for ${fixture.id}; actual=$actualTags", expected in actualTags)
            }
            assertTrue("summary should stay supportive for ${fixture.id}", result.summary.contains("No shame", ignoreCase = true) || result.summary.contains("Start", ignoreCase = true) || result.summary.contains("reset", ignoreCase = true))
        }
    }

    @Test
    fun clearReferenceFixtureDoesNotBecomeHeavyReset() {
        val fixture = OnlineScanFixture(
            id = "clear_living_room_reference",
            sourceUrl = "https://commons.wikimedia.org/wiki/File:Living_room.jpg",
            roomName = "Living Room",
            roomType = "Living Room",
            visibleContext = "mostly clear couch table open floor",
            expectedMessLevel = MessLevel.LIGHT_RESET,
            minimumMessScore = 30,
            expectedTags = setOf("couch_reset_needed"),
            startingRoomScore = 86,
            priority = "normal"
        )

        val result = analyzer.analyze(
            room = RoomEntity(
                id = fixture.id,
                name = fixture.roomName,
                roomType = fixture.roomType,
                tidyScore = fixture.startingRoomScore,
                priority = fixture.priority
            ),
            note = fixture.visibleContext,
            imageUri = fixture.sourceUrl
        )

        assertTrue(result.messLevel != MessLevel.HEAVY_RESET)
        assertTrue(result.messScore < 75)
    }

    private data class OnlineScanFixture(
        val id: String,
        val sourceUrl: String,
        val roomName: String,
        val roomType: String,
        val visibleContext: String,
        val expectedMessLevel: MessLevel,
        val minimumMessScore: Int,
        val expectedTags: Set<String>,
        val startingRoomScore: Int = 65,
        val priority: String = "normal"
    )

    private companion object {
        val onlineFixtures = listOf(
            OnlineScanFixture(
                id = "messy_kitchen_sink_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/File:Messy_kitchen_sink.jpg",
                roomName = "Kitchen",
                roomType = "Kitchen",
                visibleContext = "messy kitchen sink full dishes plates cups counter clutter",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("dishes_visible", "sink_full", "cluttered_surface")
            ),
            OnlineScanFixture(
                id = "gfp_messy_kitchen_sink_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/File:Gfp-messy-kitchen-sink.jpg",
                roomName = "Kitchen",
                roomType = "Kitchen",
                visibleContext = "messy pots pans plates cups dishes full sink counter clutter wipe needed",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("dishes_visible", "sink_full", "cluttered_surface", "wipe_needed")
            ),
            OnlineScanFixture(
                id = "messy_bedroom_bag_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/Category:Bedrooms_in_the_United_States",
                roomName = "Bedroom",
                roomType = "Bedroom",
                visibleContext = "messy bedroom rumpled bed blanket nightstand clutter trash bag clothes floor clutter",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("unmade_bed", "bedroom_surface_clutter", "trash_visible", "floor_clutter")
            ),
            OnlineScanFixture(
                id = "cluttered_bedroom_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/Category:Bedrooms_in_the_United_States",
                roomName = "Bedroom 2",
                roomType = "Bedroom",
                visibleContext = "cluttered bedroom crumpled bedspread floor bags dresser surface closet boxes",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("unmade_bed", "floor_clutter", "bedroom_surface_clutter", "closet_or_box_clutter")
            ),
            OnlineScanFixture(
                id = "laundry_room_pile_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/File:Energy_use_in_the_laundry_room_(36377510063).jpg",
                roomName = "Laundry",
                roomType = "Laundry",
                visibleContext = "laundry pile basket to fold clothes on floor washer dryer machine top clutter",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("laundry_machine_reset", "folding_needed", "laundry_visible")
            ),
            OnlineScanFixture(
                id = "cluttered_home_office_kitchen_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/File:DFC_3009_A_cozy_lived-in_home_office_and_kitchen_area_with_a_cluttered_desk_display_shelves_of_dishes_and_ornaments_a_refrigerator_and_stools_scattered_across_a_concrete_floor.jpg",
                roomName = "Office",
                roomType = "Office",
                visibleContext = "cluttered desk paper piles shelves dishes scattered stools concrete floor cords visible",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("office_desk_clutter", "floor_clutter", "electronics_clutter")
            ),
            OnlineScanFixture(
                id = "cluttered_kitchen_counter_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/Category:Microwave_ovens",
                roomName = "Kitchen",
                roomType = "Kitchen",
                visibleContext = "cluttered kitchen counter boxes microwave tools surface wipe needed",
                expectedMessLevel = MessLevel.MODERATE_MESS,
                minimumMessScore = 50,
                expectedTags = setOf("cluttered_surface", "wipe_needed")
            ),
            OnlineScanFixture(
                id = "cluttered_utility_room_commons",
                sourceUrl = "https://commons.wikimedia.org/wiki/Category:Fire_extinguishers_in_the_United_States",
                roomName = "Storage",
                roomType = "Storage",
                visibleContext = "cluttered storage utility room exposed wiring equipment on floor cords boxes floor path blocked",
                expectedMessLevel = MessLevel.HEAVY_RESET,
                minimumMessScore = 75,
                expectedTags = setOf("basement_floor_path", "storage_shelf_clutter", "cords_or_equipment_clutter")
            )
        )
    }
}
