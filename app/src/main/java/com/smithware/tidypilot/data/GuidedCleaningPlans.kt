package com.smithware.tidypilot.data

import java.time.LocalDate

data class GuidedCleaningPlan(
    val id: String,
    val title: String,
    val description: String,
    val suggestedRooms: List<String>,
    val steps: List<GuidedPlanStep>
)

data class GuidedPlanStep(
    val title: String,
    val roomCategory: String,
    val minutes: Int,
    val effort: String = "low",
    val priority: String = "normal",
    val category: String = "other"
)

data class GeneratedGuidedPlan(
    val plan: GuidedCleaningPlan,
    val rooms: List<RoomEntity>,
    val tasks: List<CleaningTaskEntity>,
    val totalMinutes: Int,
    val spreadDays: Int
)

val guidedCleaningPlans: List<GuidedCleaningPlan> = listOf(
    GuidedCleaningPlan(
        id = "daily_reset",
        title = "Daily reset",
        description = "A small daily pass for visible mess and basic home control.",
        suggestedRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Entryway"),
        steps = listOf(
            GuidedPlanStep("Put dishes in sink or dishwasher", "Kitchen", 5, "low", "high", "dishes"),
            GuidedPlanStep("Wipe kitchen counters", "Kitchen", 5, "low", "high", "surface wipe"),
            GuidedPlanStep("Take out trash if full", "Kitchen", 3, "low", "high", "trash"),
            GuidedPlanStep("Make bed", "Bedroom", 3, "low", "normal", "bed reset"),
            GuidedPlanStep("Clear one living room surface", "Living room", 5, "low", "normal", "clutter")
        )
    ),
    GuidedCleaningPlan(
        id = "weekly_reset",
        title = "Weekly reset",
        description = "A calm weekly reset for floors, bathroom basics, laundry, and visible clutter.",
        suggestedRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Laundry", "Entryway"),
        steps = listOf(
            GuidedPlanStep("Clean toilet", "Bathroom", 10, "medium", "high", "bathroom reset"),
            GuidedPlanStep("Wipe bathroom sink and mirror", "Bathroom", 8, "low", "normal", "surface wipe"),
            GuidedPlanStep("Change bed sheets", "Bedroom", 10, "medium", "normal", "laundry"),
            GuidedPlanStep("Vacuum visible areas", "Living room", 15, "medium", "normal", "floor clutter"),
            GuidedPlanStep("Fold one laundry basket", "Laundry", 12, "medium", "normal", "laundry"),
            GuidedPlanStep("Sweep kitchen floor", "Kitchen", 8, "low", "normal", "floor clutter")
        )
    ),
    GuidedCleaningPlan(
        id = "monthly_deep_clean",
        title = "Monthly deep clean",
        description = "A bigger but still chunked plan for monthly build-up.",
        suggestedRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Whole home"),
        steps = listOf(
            GuidedPlanStep("Clean fridge shelves one section at a time", "Kitchen", 30, "medium", "normal", "other"),
            GuidedPlanStep("Scrub shower or tub", "Bathroom", 20, "medium", "high", "bathroom reset"),
            GuidedPlanStep("Dust bedroom surfaces", "Bedroom", 10, "low", "normal", "surface wipe"),
            GuidedPlanStep("Vacuum under main furniture edges", "Living room", 20, "medium", "normal", "floor clutter"),
            GuidedPlanStep("Wipe baseboards in one zone", "Whole home", 20, "medium", "low", "surface wipe")
        )
    ),
    GuidedCleaningPlan(
        id = "spring_cleaning",
        title = "Spring cleaning",
        description = "Seasonal refresh with windows, storage, floors, and forgotten corners.",
        suggestedRooms = listOf("Kitchen", "Bedroom", "Living room", "Garage", "Basement", "Whole home"),
        steps = listOf(
            GuidedPlanStep("Wash windows in one area", "Whole home", 45, "high", "normal", "surface wipe"),
            GuidedPlanStep("Declutter one closet", "Bedroom", 60, "high", "normal", "clutter"),
            GuidedPlanStep("Sort one storage shelf", "Garage", 20, "medium", "normal", "clutter"),
            GuidedPlanStep("Clear one basement path", "Basement", 15, "medium", "high", "floor clutter"),
            GuidedPlanStep("Wipe cabinet fronts", "Kitchen", 20, "medium", "normal", "surface wipe")
        )
    ),
    GuidedCleaningPlan(
        id = "fall_cleaning",
        title = "Fall cleaning",
        description = "Prep the home for colder months and busier indoor routines.",
        suggestedRooms = listOf("Entryway", "Bedroom", "Garage", "Basement", "Whole home"),
        steps = listOf(
            GuidedPlanStep("Reset entryway shoes and coats", "Entryway", 15, "medium", "normal", "floor clutter"),
            GuidedPlanStep("Wash extra blankets or linens", "Bedroom", 20, "medium", "normal", "laundry"),
            GuidedPlanStep("Replace HVAC filter", "Whole home", 5, "low", "high", "other"),
            GuidedPlanStep("Sort one garage seasonal area", "Garage", 25, "medium", "normal", "clutter"),
            GuidedPlanStep("Check basement trash and loose items", "Basement", 15, "medium", "normal", "trash")
        )
    ),
    GuidedCleaningPlan(
        id = "move_cleaning",
        title = "Move-in / move-out cleaning",
        description = "Room-by-room reset for emptying, entering, or handing off a home.",
        suggestedRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Entryway", "Whole home"),
        steps = listOf(
            GuidedPlanStep("Clear and wipe kitchen cabinets", "Kitchen", 30, "high", "high", "surface wipe"),
            GuidedPlanStep("Clean toilet, sink, and mirror", "Bathroom", 20, "medium", "high", "bathroom reset"),
            GuidedPlanStep("Vacuum bedroom edges", "Bedroom", 15, "medium", "normal", "floor clutter"),
            GuidedPlanStep("Wipe living room surfaces", "Living room", 15, "low", "normal", "surface wipe"),
            GuidedPlanStep("Sweep entryway", "Entryway", 8, "low", "normal", "floor clutter"),
            GuidedPlanStep("Take out final trash bags", "Whole home", 10, "medium", "high", "trash")
        )
    ),
    GuidedCleaningPlan(
        id = "guest_coming",
        title = "Guest coming over",
        description = "A visible-area reset that makes the home feel ready fast.",
        suggestedRooms = listOf("Entryway", "Bathroom", "Kitchen", "Living room"),
        steps = listOf(
            GuidedPlanStep("Clear entryway", "Entryway", 5, "low", "normal", "floor clutter"),
            GuidedPlanStep("Wipe bathroom sink", "Bathroom", 5, "low", "high", "surface wipe"),
            GuidedPlanStep("Clean toilet", "Bathroom", 10, "medium", "high", "bathroom reset"),
            GuidedPlanStep("Take out trash", "Kitchen", 3, "low", "high", "trash"),
            GuidedPlanStep("Wipe kitchen counters", "Kitchen", 5, "low", "high", "surface wipe"),
            GuidedPlanStep("Vacuum visible areas", "Living room", 15, "medium", "normal", "floor clutter"),
            GuidedPlanStep("Hide clutter basket", "Living room", 5, "low", "normal", "clutter"),
            GuidedPlanStep("Light reset task", "Whole home", 5, "low", "normal", "clutter")
        )
    ),
    GuidedCleaningPlan(
        id = "new_baby_pet_roommate",
        title = "New baby / pet / roommate setup",
        description = "A practical setup reset for shared routines and high-use zones.",
        suggestedRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living room", "Pet area", "Whole home"),
        steps = listOf(
            GuidedPlanStep("Clear one shared surface", "Living room", 5, "low", "normal", "clutter"),
            GuidedPlanStep("Set up a laundry drop zone", "Bedroom", 10, "low", "normal", "laundry"),
            GuidedPlanStep("Refresh bathroom towels", "Bathroom", 5, "low", "normal", "laundry"),
            GuidedPlanStep("Wipe kitchen prep area", "Kitchen", 5, "low", "high", "surface wipe"),
            GuidedPlanStep("Refresh pet station", "Pet area", 5, "low", "high", "other"),
            GuidedPlanStep("Take out trash", "Whole home", 5, "low", "high", "trash")
        )
    ),
    GuidedCleaningPlan(
        id = "holiday_hosting",
        title = "Holiday hosting reset",
        description = "A hosting-focused reset for guest-facing spaces and food areas.",
        suggestedRooms = listOf("Kitchen", "Bathroom", "Living room", "Dining room", "Entryway"),
        steps = listOf(
            GuidedPlanStep("Clear dining table", "Dining room", 8, "low", "high", "clutter"),
            GuidedPlanStep("Wipe kitchen counters", "Kitchen", 5, "low", "high", "surface wipe"),
            GuidedPlanStep("Clean bathroom sink and toilet", "Bathroom", 15, "medium", "high", "bathroom reset"),
            GuidedPlanStep("Vacuum visible areas", "Living room", 15, "medium", "normal", "floor clutter"),
            GuidedPlanStep("Reset entryway", "Entryway", 8, "low", "normal", "floor clutter"),
            GuidedPlanStep("Take out kitchen trash", "Kitchen", 3, "low", "high", "trash")
        )
    )
)

fun generateGuidedPlan(
    plan: GuidedCleaningPlan,
    existingRooms: List<RoomEntity>,
    includedRoomCategories: Set<String>,
    onlyThirtyMinutes: Boolean,
    spreadDays: Int,
    today: LocalDate
): GeneratedGuidedPlan {
    val filteredSteps = plan.steps.filter { it.roomCategory in includedRoomCategories }
    val selectedSteps = if (onlyThirtyMinutes) {
        val selected = mutableListOf<GuidedPlanStep>()
        var total = 0
        filteredSteps.sortedBy { it.minutes }.forEach { step ->
            if (total + step.minutes <= 30 || selected.isEmpty()) {
                selected += step
                total += step.minutes
            }
        }
        selected
    } else {
        filteredSteps
    }
    val rooms = selectedSteps
        .map { step ->
            existingRooms.firstOrNull { room -> room.name.equals(step.roomCategory, ignoreCase = true) }
                ?: RoomEntity(
                    name = step.roomCategory,
                    roomType = step.roomCategory,
                    iconName = step.roomCategory.lowercase().replace(" ", "_"),
                    priority = if (step.priority == "high") "high" else "normal",
                    defaultTaskIntensity = step.effort,
                    defaultTaskFrequency = "one-time",
                    notes = "Added from ${plan.title} guided plan."
                )
        }
        .distinctBy { it.name.lowercase() }
    val roomByName = rooms.associateBy { it.name.lowercase() }
    val safeSpread = spreadDays.coerceAtLeast(1)
    val tasks = selectedSteps.mapIndexedNotNull { index, step ->
        val room = roomByName[step.roomCategory.lowercase()] ?: roomByName["whole home"] ?: return@mapIndexedNotNull null
        CleaningTaskEntity(
            name = step.title,
            roomId = room.id,
            description = "Generated from ${plan.title}. Step ${index + 1} of ${selectedSteps.size}.",
            priority = step.priority,
            estimatedMinutes = step.minutes,
            difficulty = step.effort,
            energyRequired = step.effort,
            frequencyType = "one-time",
            preferredTime = if (spreadDays > 1) "day off" else "anytime",
            isQuickResetTask = step.minutes <= 5,
            isDeepCleanTask = step.minutes >= 20,
            photoDetectableCategory = step.category,
            nextDueAt = today.plusDays((index % safeSpread).toLong())
        )
    }
    return GeneratedGuidedPlan(plan, rooms, tasks, tasks.sumOf { it.estimatedMinutes }, safeSpread)
}
