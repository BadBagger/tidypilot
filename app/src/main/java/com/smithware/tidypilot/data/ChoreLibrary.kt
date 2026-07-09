package com.smithware.tidypilot.data

data class ChoreLibraryItem(
    val name: String,
    val roomCategory: String,
    val suggestedFrequency: String,
    val estimatedMinutes: Int,
    val effortLevel: String,
    val hygieneImportance: String,
    val clutterImpact: String,
    val seasonal: Boolean = false,
    val suppliesNeeded: String = "",
    val category: String = "other"
)

val choreLibrary: List<ChoreLibraryItem> = listOf(
    ChoreLibraryItem("Wipe kitchen counters", "Kitchen", "Daily", 5, "low", "medium", "high", suppliesNeeded = "cloth, all-purpose cleaner", category = "surface wipe"),
    ChoreLibraryItem("Wash dishes", "Kitchen", "Daily", 10, "medium", "high", "medium", suppliesNeeded = "dish soap", category = "dishes"),
    ChoreLibraryItem("Clean sink", "Kitchen", "Every few days", 5, "low", "high", "medium", suppliesNeeded = "sponge, cleaner", category = "sink_full"),
    ChoreLibraryItem("Sweep kitchen floor", "Kitchen", "Every few days", 8, "low", "medium", "high", suppliesNeeded = "broom", category = "floor clutter"),
    ChoreLibraryItem("Mop kitchen floor", "Kitchen", "Weekly", 15, "medium", "medium", "medium", suppliesNeeded = "mop, floor cleaner", category = "floor clutter"),
    ChoreLibraryItem("Clean fridge", "Kitchen", "Monthly", 30, "medium", "medium", "low", suppliesNeeded = "cloth, mild cleaner", category = "other"),
    ChoreLibraryItem("Take out trash", "Kitchen", "Every few days", 3, "low", "high", "medium", suppliesNeeded = "trash bag", category = "trash"),
    ChoreLibraryItem("Clean microwave", "Kitchen", "Weekly", 8, "low", "medium", "low", suppliesNeeded = "bowl, cloth", category = "surface wipe"),

    ChoreLibraryItem("Clean toilet", "Bathroom", "Weekly", 10, "medium", "high", "low", suppliesNeeded = "toilet cleaner, brush", category = "bathroom reset"),
    ChoreLibraryItem("Wipe bathroom sink", "Bathroom", "Every few days", 5, "low", "high", "medium", suppliesNeeded = "cloth, bathroom cleaner", category = "surface wipe"),
    ChoreLibraryItem("Clean mirror", "Bathroom", "Weekly", 4, "low", "low", "medium", suppliesNeeded = "glass cleaner, cloth", category = "surface wipe"),
    ChoreLibraryItem("Scrub shower/tub", "Bathroom", "Weekly", 20, "medium", "high", "low", suppliesNeeded = "bathroom cleaner, brush", category = "bathroom reset"),
    ChoreLibraryItem("Replace towels", "Bathroom", "Weekly", 5, "low", "medium", "medium", category = "laundry"),
    ChoreLibraryItem("Empty bathroom trash", "Bathroom", "Weekly", 3, "low", "medium", "medium", suppliesNeeded = "trash bag", category = "trash"),
    ChoreLibraryItem("Mop bathroom floor", "Bathroom", "Weekly", 12, "medium", "medium", "medium", suppliesNeeded = "mop, floor cleaner", category = "floor clutter"),

    ChoreLibraryItem("Make bed", "Bedroom", "Daily", 3, "low", "low", "high", category = "bed reset"),
    ChoreLibraryItem("Change bed sheets", "Bedroom", "Weekly", 10, "medium", "medium", "medium", category = "laundry"),
    ChoreLibraryItem("Wash mattress protector", "Bedroom", "Biweekly", 15, "medium", "medium", "low", category = "laundry"),
    ChoreLibraryItem("Put away clothes", "Bedroom", "Every few days", 10, "low", "low", "high", category = "laundry"),
    ChoreLibraryItem("Vacuum bedroom", "Bedroom", "Weekly", 12, "medium", "low", "medium", suppliesNeeded = "vacuum", category = "floor clutter"),
    ChoreLibraryItem("Dust bedroom surfaces", "Bedroom", "Weekly", 8, "low", "low", "medium", suppliesNeeded = "duster or cloth", category = "surface wipe"),
    ChoreLibraryItem("Declutter closet", "Bedroom", "Seasonal", 60, "high", "low", "high", seasonal = true, category = "clutter"),

    ChoreLibraryItem("10-minute living room reset", "Living room", "Daily", 10, "medium", "low", "high", category = "floor clutter"),
    ChoreLibraryItem("Clear coffee table", "Living room", "Daily", 5, "low", "low", "high", category = "clutter"),
    ChoreLibraryItem("Vacuum living room", "Living room", "Weekly", 15, "medium", "low", "medium", suppliesNeeded = "vacuum", category = "floor clutter"),
    ChoreLibraryItem("Dust living room surfaces", "Living room", "Weekly", 10, "low", "low", "medium", suppliesNeeded = "duster or cloth", category = "surface wipe"),

    ChoreLibraryItem("Start laundry", "Laundry", "Every few days", 5, "low", "medium", "medium", suppliesNeeded = "detergent", category = "laundry"),
    ChoreLibraryItem("Switch laundry", "Laundry", "Every few days", 3, "low", "medium", "medium", category = "laundry"),
    ChoreLibraryItem("Fold one basket", "Laundry", "Every few days", 12, "medium", "low", "high", category = "laundry"),
    ChoreLibraryItem("Clean lint trap", "Laundry", "Weekly", 2, "low", "medium", "low", category = "other"),

    ChoreLibraryItem("Clear shoes and bags", "Entryway", "Every few days", 5, "low", "low", "high", category = "floor clutter"),
    ChoreLibraryItem("Sort mail", "Entryway", "Weekly", 8, "low", "low", "medium", category = "clutter"),
    ChoreLibraryItem("Sweep entryway", "Entryway", "Weekly", 8, "low", "low", "medium", suppliesNeeded = "broom", category = "floor clutter"),

    ChoreLibraryItem("Clear desk surface", "Office", "Every few days", 5, "low", "low", "high", category = "clutter"),
    ChoreLibraryItem("Sort loose papers", "Office", "Weekly", 10, "medium", "low", "medium", category = "clutter"),
    ChoreLibraryItem("Empty office trash", "Office", "Weekly", 3, "low", "medium", "medium", suppliesNeeded = "trash bag", category = "trash"),

    ChoreLibraryItem("Clear garage walking path", "Garage", "Monthly", 15, "medium", "low", "high", category = "floor clutter"),
    ChoreLibraryItem("Sort one garage shelf", "Garage", "Monthly", 20, "medium", "low", "medium", category = "clutter"),
    ChoreLibraryItem("Sweep garage", "Garage", "Monthly", 20, "medium", "low", "medium", suppliesNeeded = "broom", category = "floor clutter"),

    ChoreLibraryItem("Clear basement walking path", "Basement", "Weekly", 10, "medium", "low", "high", category = "floor clutter"),
    ChoreLibraryItem("Sort one basement pile", "Basement", "Weekly", 12, "medium", "low", "high", category = "clutter"),
    ChoreLibraryItem("Check basement trash", "Basement", "Weekly", 5, "low", "medium", "medium", suppliesNeeded = "trash bag", category = "trash"),

    ChoreLibraryItem("Refresh pet station", "Pet area", "Daily", 5, "low", "high", "medium", category = "other"),
    ChoreLibraryItem("Wash pet bowls", "Pet area", "Every few days", 5, "low", "high", "low", suppliesNeeded = "dish soap", category = "dishes"),
    ChoreLibraryItem("Sweep pet area", "Pet area", "Every few days", 8, "low", "medium", "medium", suppliesNeeded = "broom", category = "floor clutter"),

    ChoreLibraryItem("Vacuum main floors", "Whole home", "Weekly", 25, "medium", "low", "medium", suppliesNeeded = "vacuum", category = "floor clutter"),
    ChoreLibraryItem("Wash windows", "Whole home", "Seasonal", 45, "high", "low", "medium", seasonal = true, suppliesNeeded = "glass cleaner, cloth", category = "surface wipe"),
    ChoreLibraryItem("Replace HVAC filter", "Whole home", "Seasonal", 5, "low", "medium", "low", seasonal = true, suppliesNeeded = "replacement filter", category = "other"),
    ChoreLibraryItem("Test smoke detectors", "Whole home", "Annual", 10, "low", "high", "low", seasonal = true, category = "other"),
    ChoreLibraryItem("Wipe baseboards", "Whole home", "As needed", 30, "medium", "low", "medium", suppliesNeeded = "cloth", category = "surface wipe")
)

fun libraryFrequencyToTaskFrequency(frequency: String): String = when (frequency.lowercase()) {
    "daily" -> "daily"
    "every few days" -> "every few days"
    "weekly" -> "weekly"
    "biweekly" -> "every 14 days"
    "monthly" -> "monthly"
    "seasonal" -> "seasonal"
    "annual" -> "annual"
    "as needed" -> "one-time"
    else -> frequency.lowercase()
}

fun libraryPriority(item: ChoreLibraryItem): String = when {
    item.hygieneImportance == "high" || item.clutterImpact == "high" && item.suggestedFrequency in listOf("Daily", "Every few days") -> "high"
    item.seasonal -> "low"
    else -> "normal"
}
