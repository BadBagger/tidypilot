package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TidyPilotRepository(private val dao: TidyPilotDao) {
    val tasks = dao.observeTasks()
    val supplies = dao.observeSupplies()
    val taskSupplies = dao.observeTaskSupplies()
    val supplyExpenses = dao.observeSupplyExpenses()
    val rooms = dao.observeRooms()
    val shifts = dao.observeShifts()
    val energy = dao.observeEnergy()
    val plans = dao.observePlans()
    val completions = dao.observeCompletions()
    val scans = dao.observeScans()
    val issues = dao.observeIssues()
    val settings = dao.observeSettings()

    suspend fun seedIfEmpty() {
        if (dao.roomCount() != 0) {
            ensureDefaultRooms()
            ensureDefaultSupplies()
            return
        }
        dao.saveSettings(AppSettingsEntity())
        val defaults = defaultRooms()
        defaults.forEach { dao.saveRoom(it) }
        val kitchen = defaults.first { it.name == "Kitchen" }
        val bath = defaults.first { it.name == "Bathroom" }
        val bedroom = defaults.first { it.name == "Bedroom" }
        val living = defaults.first { it.name == "Living Room" }
        val laundry = defaults.first { it.name == "Laundry" }
        val basement = defaults.first { it.name == "Basement" }

        val today = LocalDate.now()
        val tasks = listOf(
            CleaningTaskEntity(name = "Load dishwasher", roomId = kitchen.id, description = "Put visible dishes into the dishwasher or sink.", priority = "high", estimatedMinutes = 10, difficulty = "medium", energyRequired = "medium", frequencyType = "daily", preferredTime = "after work", photoDetectableCategory = "dishes", nextDueAt = today),
            CleaningTaskEntity(name = "Clear kitchen counters", roomId = kitchen.id, description = "Clear one counter section first.", priority = "high", estimatedMinutes = 5, difficulty = "easy", energyRequired = "low", frequencyType = "daily", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "clutter", nextDueAt = today),
            CleaningTaskEntity(name = "Take out kitchen trash", roomId = kitchen.id, priority = "normal", estimatedMinutes = 3, difficulty = "easy", energyRequired = "low", frequencyType = "every few days", preferredTime = "before work", isQuickResetTask = true, photoDetectableCategory = "trash", nextDueAt = today),
            CleaningTaskEntity(name = "Wipe bathroom sink", roomId = bath.id, priority = "normal", estimatedMinutes = 5, difficulty = "easy", energyRequired = "low", frequencyType = "every few days", preferredTime = "before work", isQuickResetTask = true, photoDetectableCategory = "bathroom reset", nextDueAt = today.plusDays(1)),
            CleaningTaskEntity(name = "Wipe bathroom mirror", roomId = bath.id, priority = "low", estimatedMinutes = 4, difficulty = "easy", energyRequired = "low", frequencyType = "weekly", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "surface wipe", nextDueAt = today.plusDays(2)),
            CleaningTaskEntity(name = "Gather bathroom towels", roomId = bath.id, priority = "normal", estimatedMinutes = 6, difficulty = "easy", energyRequired = "low", frequencyType = "weekly", preferredTime = "day off", isQuickResetTask = true, photoDetectableCategory = "laundry", nextDueAt = today.plusDays(3)),
            CleaningTaskEntity(name = "Put laundry in hamper", roomId = bedroom.id, priority = "high", estimatedMinutes = 5, difficulty = "easy", energyRequired = "low", frequencyType = "daily", preferredTime = "after work", isQuickResetTask = true, photoDetectableCategory = "laundry", nextDueAt = today),
            CleaningTaskEntity(name = "Reset nightstand", roomId = bedroom.id, priority = "normal", estimatedMinutes = 5, difficulty = "easy", energyRequired = "low", frequencyType = "weekly", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "clutter", nextDueAt = today.plusDays(1)),
            CleaningTaskEntity(name = "Pick up bedroom floor", roomId = bedroom.id, priority = "normal", estimatedMinutes = 10, difficulty = "medium", energyRequired = "medium", frequencyType = "weekly", preferredTime = "day off", photoDetectableCategory = "floor clutter", nextDueAt = today.plusDays(2), skippedCount = 1),
            CleaningTaskEntity(name = "10-minute living room reset", roomId = living.id, priority = "normal", estimatedMinutes = 10, difficulty = "medium", energyRequired = "medium", frequencyType = "daily", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "floor clutter", nextDueAt = today),
            CleaningTaskEntity(name = "Vacuum living room rug", roomId = living.id, priority = "low", estimatedMinutes = 15, difficulty = "medium", energyRequired = "medium", frequencyType = "weekly", preferredTime = "day off", photoDetectableCategory = "floor clutter", nextDueAt = today.plusDays(4)),
            CleaningTaskEntity(name = "Switch laundry and fold one basket", roomId = laundry.id, priority = "normal", estimatedMinutes = 15, difficulty = "medium", energyRequired = "medium", frequencyType = "every few days", preferredTime = "day off", photoDetectableCategory = "laundry", nextDueAt = today.plusDays(1)),
            CleaningTaskEntity(name = "Clear one basement walking path", roomId = basement.id, description = "Move loose gear, cords, and floor items from one safe path first.", priority = "high", estimatedMinutes = 10, difficulty = "medium", energyRequired = "medium", frequencyType = "weekly", preferredTime = "day off", photoDetectableCategory = "floor clutter", nextDueAt = today),
            CleaningTaskEntity(name = "Sort one basement shelf", roomId = basement.id, description = "Group loose items on one shelf or rack without trying to fix the whole room.", priority = "normal", estimatedMinutes = 12, difficulty = "medium", energyRequired = "medium", frequencyType = "weekly", preferredTime = "day off", photoDetectableCategory = "clutter", nextDueAt = today.plusDays(2))
        )
        tasks.forEach { dao.saveTask(it) }
        seedStarterSupplies(tasks)

        dao.saveEnergy(EnergyCheckInEntity(date = today, energyLevel = "medium", moodLabel = "tired but okay", availableMinutes = 20, afterWorkExhaustion = false, notes = "Good enough for today."))
        dao.saveEnergy(EnergyCheckInEntity(date = today.minusDays(1), energyLevel = "low", moodLabel = "after shift tired", availableMinutes = 10, afterWorkExhaustion = true, notes = "Only wanted a tiny reset."))
        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
        listOf(0L, 2L, 4L).forEachIndexed { index, offset ->
            dao.saveShift(
                WorkShiftEntity(
                    date = startOfWeek.plusDays(offset),
                    startTime = if (index == 1) LocalTime.of(12, 0) else LocalTime.of(9, 0),
                    endTime = if (index == 1) LocalTime.of(20, 30) else LocalTime.of(17, 30),
                    label = if (index == 1) "Late shift" else "Work shift",
                    expectedExhaustionLevel = if (index == 1) "high" else "medium",
                    notes = "Work shift used to preview realistic planning."
                )
            )
        }

        val analyzer = RoomPhotoAnalyzer()
        saveScanAnalysis(kitchen, "sample://${kitchen.name}", "kitchen needs dishes trash reset", analyzer.analyze(kitchen, "kitchen needs dishes trash reset", "sample://${kitchen.name}"))
        listOf(
            tasks[1] to today.minusDays(4),
            tasks[3] to today.minusDays(3),
            tasks[9] to today.minusDays(2),
            tasks[2] to today.minusDays(1)
        ).forEach { (task, date) ->
            dao.saveCompletion(TaskCompletionEntity(taskId = task.id, completedAt = LocalDateTime.of(date, LocalTime.of(18, 15)), durationMinutes = task.estimatedMinutes, energyLevelAtCompletion = if (date == today.minusDays(1)) "low" else "medium"))
        }
        dao.savePlan(
            DailyCleaningPlanEntity(
                date = today.minusDays(1),
                workStatus = "after work",
                energyLevel = "low",
                availableMinutes = 10,
                planType = "weekly progress",
                suggestedTaskIds = pipe(listOf(tasks[2].id, tasks[7].id)),
                completedTaskIds = pipe(listOf(tasks[2].id)),
                skippedTaskIds = pipe(listOf(tasks[8].id)),
                adaptedReason = "After-shift plan stayed small and practical.",
                sourceType = "starter_report"
            )
        )
    }

    private suspend fun ensureDefaultRooms() {
        val existing = dao.roomNames().map { it.lowercase() }.toSet()
        defaultRooms().filter { it.name.lowercase() !in existing }.forEach { dao.saveRoom(it) }
    }

    private suspend fun ensureDefaultSupplies() {
        if (dao.supplyCount() == 0) seedStarterSupplies(dao.activeTasksOnce())
    }

    private fun defaultRooms(): List<RoomEntity> = listOf(
        RoomEntity(name = "Kitchen", roomType = "Kitchen", iconName = "kitchen", tidyScore = 58, priority = "high", defaultTaskFrequency = "daily", notes = "Dishes, counters, and trash are the main pressure points."),
        RoomEntity(name = "Bathroom", roomType = "Bathroom", iconName = "bath", tidyScore = 72, priority = "normal", notes = "Sink, mirror, and towels."),
        RoomEntity(name = "Bedroom", roomType = "Bedroom", iconName = "bed", tidyScore = 63, priority = "high", notes = "Laundry, nightstand, and floor resets."),
        RoomEntity(name = "Living Room", roomType = "Living Room", iconName = "sofa", tidyScore = 76, priority = "normal", notes = "Clutter reset and vacuuming."),
        RoomEntity(name = "Laundry", roomType = "Laundry", iconName = "laundry", tidyScore = 66, priority = "normal", defaultTaskFrequency = "every few days", notes = "Switch loads and fold clothes."),
        RoomEntity(name = "Basement", roomType = "Basement", iconName = "basement", tidyScore = 42, priority = "high", defaultTaskIntensity = "medium", defaultTaskFrequency = "weekly", notes = "Storage shelves, floor paths, cords, gear, and bigger reset projects.")
    )

    suspend fun applyStarterRoutine(profile: StarterRoutineProfile = StarterRoutineProfile()): Int {
        ensureStarterRooms(profile)
        val eligibleRoomNames = starterRoomsFor(profile).map { it.name.lowercase() }.toSet()
        val rooms = dao.activeRoomsOnce()
        val existingKeys = dao.activeTaskRoomKeys().toMutableSet()
        val today = LocalDate.now()
        var created = 0

        starterRoutineTemplates(profile).forEach { template ->
            val matchingRooms = rooms.filter { it.name.lowercase() in eligibleRoomNames && it.matchesStarterRoom(template.roomType) }
            matchingRooms.forEach { room ->
                val key = "${template.name.lowercase()}|${room.id}"
                if (key !in existingKeys) {
                    dao.saveTask(
                        CleaningTaskEntity(
                            name = template.name,
                            roomId = room.id,
                            description = template.description,
                            priority = template.priority,
                            estimatedMinutes = template.estimatedMinutes,
                            difficulty = template.difficulty,
                            energyRequired = template.energyRequired,
                            frequencyType = template.frequencyType,
                            preferredTime = template.preferredTime,
                            isQuickResetTask = template.quickReset,
                            isDeepCleanTask = template.deepClean,
                            photoDetectableCategory = template.category,
                            nextDueAt = today.plusDays(template.startOffsetDays)
                        )
                    )
                    existingKeys += key
                    created++
                }
            }
        }
        return created
    }

    private suspend fun ensureStarterRooms(profile: StarterRoutineProfile) {
        val existing = dao.roomNames().map { it.lowercase() }.toMutableSet()
        starterRoomsFor(profile).forEach { room ->
            if (room.name.lowercase() !in existing) {
                dao.saveRoom(room)
                existing += room.name.lowercase()
            }
        }
    }

    suspend fun resetDemoData() {
        dao.clearScanIssues()
        dao.clearScans()
        dao.clearCompletions()
        dao.clearPlans()
        dao.clearEnergy()
        dao.clearShifts()
        dao.clearSupplyExpenses()
        dao.clearTaskSupplies()
        dao.clearSupplies()
        dao.clearTasks()
        dao.clearRooms()
        dao.clearSettings()
        seedIfEmpty()
    }

    suspend fun saveTask(task: CleaningTaskEntity) = dao.saveTask(task.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveSupply(supply: CleaningSupplyEntity) = dao.saveSupply(supply.copy(updatedAt = LocalDateTime.now()))
    suspend fun deleteSupply(supply: CleaningSupplyEntity) = dao.deleteSupply(supply)
    suspend fun linkSupplyToTask(taskId: String, supplyId: String) = dao.saveTaskSupply(TaskSupplyEntity(taskId, supplyId))
    suspend fun unlinkSupplyFromTask(taskId: String, supplyId: String) = dao.unlinkSupplyFromTask(taskId, supplyId)
    suspend fun markSupplyRunningLow(supply: CleaningSupplyEntity, runningLow: Boolean) =
        dao.markSupplyRunningLow(supply.id, runningLow, LocalDateTime.now())
    suspend fun markSupplyOnShoppingList(supply: CleaningSupplyEntity, onList: Boolean) =
        dao.markSupplyOnShoppingList(supply.id, onList, LocalDateTime.now())
    suspend fun addSuppliesToShoppingList(ids: List<String>) {
        if (ids.isNotEmpty()) dao.addSuppliesToShoppingList(ids, LocalDateTime.now())
    }
    suspend fun saveSupplyExpense(expense: SupplyExpenseEntity) = dao.saveSupplyExpense(expense)
    suspend fun saveRoom(room: RoomEntity) = dao.saveRoom(room.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveShift(shift: WorkShiftEntity) = dao.saveShift(shift.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveEnergy(checkIn: EnergyCheckInEntity) = dao.saveEnergy(checkIn)
    suspend fun savePlan(plan: DailyCleaningPlanEntity) = dao.savePlan(plan.copy(updatedAt = LocalDateTime.now()))
    suspend fun deleteTask(task: CleaningTaskEntity) {
        dao.clearCompletionsForTask(task.id)
        dao.clearSuppliesForTask(task.id)
        dao.deleteTask(task)
    }

    suspend fun deleteRoom(room: RoomEntity) {
        val hasRelatedData = dao.taskCountForRoom(room.id) > 0 || dao.scanCountForRoom(room.id) > 0
        if (hasRelatedData) {
            archiveRoom(room)
        } else {
            dao.deleteRoom(room)
        }
    }
    suspend fun archiveRoom(room: RoomEntity) = dao.archiveRoom(room.id, LocalDateTime.now())
    suspend fun deleteShift(shift: WorkShiftEntity) = dao.deleteShift(shift)

    suspend fun markTaskComplete(task: CleaningTaskEntity, energy: String) {
        val now = LocalDateTime.now()
        dao.saveCompletion(
            TaskCompletionEntity(
                taskId = task.id,
                completedAt = now,
                durationMinutes = task.estimatedMinutes,
                energyLevelAtCompletion = energy,
                householdId = task.householdId,
                completedBy = task.assignedTo
            )
        )
        dao.markTaskComplete(task.id, now, nextDate(task.frequencyType, now.toLocalDate()))
    }

    suspend fun skipTask(task: CleaningTaskEntity) = dao.skipTask(task.id, LocalDateTime.now())
    suspend fun snoozeTask(task: CleaningTaskEntity) = dao.snoozeTask(task.id, LocalDate.now().plusDays(1), LocalDateTime.now())
    suspend fun setScanFeedback(scanId: String, feedback: String) = dao.setScanFeedback(scanId, feedback)
    suspend fun markScanReviewed(scanId: String) = dao.markScanReviewed(scanId)
    suspend fun updateScanIssueStatus(issueId: String, status: String, createdTaskId: String? = null) =
        dao.updateScanIssueStatus(issueId, status, createdTaskId)
    suspend fun updateSettings(settings: AppSettingsEntity) = dao.saveSettings(settings)

    suspend fun clearScanData() {
        dao.clearScanIssues()
        dao.clearScans()
    }

    suspend fun deleteAllLocalData() {
        dao.clearScanIssues()
        dao.clearScans()
        dao.clearCompletions()
        dao.clearPlans()
        dao.clearEnergy()
        dao.clearShifts()
        dao.clearSupplyExpenses()
        dao.clearTaskSupplies()
        dao.clearSupplies()
        dao.clearTasks()
        dao.clearRooms()
        dao.clearSettings()
    }

    private suspend fun seedStarterSupplies(tasks: List<CleaningTaskEntity>) {
        val supplies = listOf(
            CleaningSupplyEntity(name = "Dish soap", category = "kitchen", estimatedCostCents = 450),
            CleaningSupplyEntity(name = "Trash bags", category = "trash", estimatedCostCents = 850, isRunningLow = true, isOnShoppingList = true),
            CleaningSupplyEntity(name = "Microfiber cloths", category = "surface", estimatedCostCents = 700),
            CleaningSupplyEntity(name = "Bathroom cleaner", category = "bathroom", estimatedCostCents = 550),
            CleaningSupplyEntity(name = "Toilet cleaner", category = "bathroom", estimatedCostCents = 500),
            CleaningSupplyEntity(name = "Toilet brush", category = "bathroom", estimatedCostCents = 900),
            CleaningSupplyEntity(name = "Gloves", category = "general", estimatedCostCents = 600),
            CleaningSupplyEntity(name = "Laundry detergent", category = "laundry", estimatedCostCents = 1299),
            CleaningSupplyEntity(name = "Vacuum", category = "floors", estimatedCostCents = 0),
            CleaningSupplyEntity(name = "Mop", category = "floors", estimatedCostCents = 1500),
            CleaningSupplyEntity(name = "Floor cleaner", category = "floors", estimatedCostCents = 650),
            CleaningSupplyEntity(name = "Bucket", category = "floors", estimatedCostCents = 700)
        )
        supplies.forEach { dao.saveSupply(it) }
        tasks.forEach { task ->
            suggestedSupplyNames(task).mapNotNull { name -> supplies.firstOrNull { it.name == name } }.forEach { supply ->
                dao.saveTaskSupply(TaskSupplyEntity(task.id, supply.id))
            }
        }
        dao.saveSupplyExpense(SupplyExpenseEntity(name = "Trash bags", costCents = 850, purchasedAt = LocalDate.now().minusDays(4), notes = "Starter example purchase."))
        dao.saveSupplyExpense(SupplyExpenseEntity(name = "Bathroom cleaner", costCents = 550, purchasedAt = LocalDate.now().minusDays(2), notes = "Starter example purchase."))
    }

    suspend fun saveScanAnalysis(room: RoomEntity, imageUri: String, note: String, output: AnalysisOutput): RoomPhotoScanEntity {
        val scan = RoomPhotoScanEntity(
            roomId = room.id,
            imageUri = imageUri,
            tidyScore = output.tidyScore,
            messScore = output.messScore,
            messLevel = output.messLevel.key,
            confidence = output.confidence,
            summary = output.summary,
            detectedZones = pipe(output.detectedZones.map { "${it.type}:${it.name}:${it.clutterScore}:${it.notes}" }),
            detectedIssueTags = pipe(output.issues.map { it.tag }),
            estimatedCleanupMinutes = output.estimatedCleanupMinutes,
            confidenceSummary = output.confidenceSummary,
            note = note
        )
        dao.saveScan(scan)
        dao.updateRoomScore(room.id, output.tidyScore, LocalDateTime.now())
        output.issues.forEach { issue ->
            dao.saveIssue(
                ScanIssueEntity(
                    scanId = scan.id,
                    roomId = room.id,
                    tag = issue.tag,
                    title = issue.label,
                    description = issue.description,
                    category = issue.category,
                    label = issue.label,
                    confidence = issue.confidence,
                    suggestedAction = issue.suggestedAction,
                    estimatedMinutes = issue.estimatedMinutes,
                    difficulty = issue.difficulty,
                    energyLevel = issue.energyLevel
                )
            )
        }
        return scan
    }

    private fun nextDate(frequency: String, today: LocalDate): LocalDate? = when (frequency) {
        "one-time" -> null
        "daily" -> today.plusDays(1)
        "every few days" -> today.plusDays(3)
        "weekly" -> today.plusWeeks(1)
        "monthly" -> today.plusMonths(1)
        else -> today.plusWeeks(1)
    }
}

data class StarterRoutineProfile(
    val selectedRooms: Set<String> = setOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry"),
    val bedroomCount: Int = 1,
    val bathroomCount: Int = 1,
    val householdType: String = "just me",
    val goals: Set<String> = setOf("basic routine"),
    val delegationInterest: Boolean = false
)

internal data class StarterRoutineTemplate(
    val name: String,
    val roomType: String,
    val description: String,
    val priority: String,
    val estimatedMinutes: Int,
    val difficulty: String,
    val energyRequired: String,
    val frequencyType: String,
    val preferredTime: String,
    val category: String,
    val quickReset: Boolean = true,
    val deepClean: Boolean = false,
    val startOffsetDays: Long = 0
)

fun suggestedSupplyNames(task: CleaningTaskEntity): List<String> {
    val searchable = "${task.name} ${task.description} ${task.photoDetectableCategory}".lowercase()
    return when {
        listOf("mop", "floor cleaner", "floor", "sweep").any { it in searchable } ->
            listOf("Mop", "Floor cleaner", "Bucket")
        "toilet" in searchable ->
            listOf("Toilet cleaner", "Toilet brush", "Gloves")
        listOf("bathroom", "sink", "mirror", "shower").any { it in searchable } ->
            listOf("Bathroom cleaner", "Microfiber cloths", "Gloves")
        listOf("dish", "sink", "dishwasher").any { it in searchable } ->
            listOf("Dish soap", "Microfiber cloths")
        listOf("trash", "garbage").any { it in searchable } ->
            listOf("Trash bags", "Gloves")
        listOf("laundry", "clothes", "towels", "sheets").any { it in searchable } ->
            listOf("Laundry detergent")
        listOf("vacuum", "rug", "carpet").any { it in searchable } ->
            listOf("Vacuum")
        listOf("counter", "surface", "wipe", "table").any { it in searchable } ->
            listOf("Microfiber cloths", "Bathroom cleaner")
        else -> emptyList()
    }
}

internal fun starterRoutineTemplates(profile: StarterRoutineProfile): List<StarterRoutineTemplate> {
    val goals = profile.goals.map { it.lowercase() }.toSet()
    val wantsDelegation = profile.delegationInterest || profile.householdType.contains("family", ignoreCase = true)
    val base = mutableListOf(
    StarterRoutineTemplate("Load dishwasher", "Kitchen", "Put visible dishes into the dishwasher or sink. Good enough counts.", "high", 10, "medium", "medium", "daily", "after work", "dishes"),
    StarterRoutineTemplate("Clear kitchen counters", "Kitchen", "Clear one useful counter section first.", "high", 5, "easy", "low", "daily", "anytime", "clutter"),
    StarterRoutineTemplate("Take out kitchen trash", "Kitchen", "Tie one bag and move it out.", "normal", 3, "easy", "low", "every few days", "before work", "trash", startOffsetDays = 1),
    StarterRoutineTemplate("Sweep kitchen floor", "Kitchen", "Sweep the main walking path after counters or dishes feel handled.", "normal", 10, "medium", "medium", "weekly", "day off", "floor clutter", quickReset = false, startOffsetDays = 2),
    StarterRoutineTemplate("Wipe bathroom sink", "Bathroom", "Wipe the faucet and sink area.", "normal", 5, "easy", "low", "every few days", "before work", "bathroom reset", startOffsetDays = 1),
    StarterRoutineTemplate("Quick bathroom reset", "Bathroom", "Reset the sink, towel, and visible counter area.", "normal", 8, "medium", "medium", "weekly", "day off", "bathroom reset", startOffsetDays = 3),
    StarterRoutineTemplate("Gather bathroom towels", "Bathroom", "Collect towels into one hamper or laundry basket.", "normal", 6, "easy", "low", "weekly", "day off", "laundry", startOffsetDays = 3),
    StarterRoutineTemplate("Make bed", "Bedroom", "Straighten the bed enough to make the room feel reset.", "normal", 3, "easy", "low", "daily", "anytime", "bed reset"),
    StarterRoutineTemplate("Put laundry in hamper", "Bedroom", "Pick up clothes from one visible area.", "high", 5, "easy", "low", "daily", "after work", "laundry"),
    StarterRoutineTemplate("Reset nightstand", "Bedroom", "Clear cups, wrappers, or small loose items from the bedside table.", "normal", 5, "easy", "low", "weekly", "anytime", "clutter", startOffsetDays = 1),
    StarterRoutineTemplate("Pick up bedroom floor", "Bedroom", "Clear the main floor path first.", "normal", 10, "medium", "medium", "every few days", "day off", "floor clutter", quickReset = false, startOffsetDays = 2),
    StarterRoutineTemplate("10-minute living room reset", "Living Room", "Set a timer and put visible clutter back where it belongs.", "normal", 10, "medium", "medium", "daily", "anytime", "floor clutter"),
    StarterRoutineTemplate("Clear coffee table", "Living Room", "Clear one table or main surface.", "normal", 5, "easy", "low", "every few days", "anytime", "clutter", startOffsetDays = 1),
    StarterRoutineTemplate("Vacuum living room rug", "Living Room", "Vacuum after the floor path is mostly clear.", "low", 15, "medium", "medium", "weekly", "day off", "floor clutter", quickReset = false, startOffsetDays = 4),
    StarterRoutineTemplate("Start laundry", "Laundry", "Start one practical load. Do not sort the whole backlog first.", "normal", 7, "easy", "low", "every few days", "anytime", "laundry", startOffsetDays = 1),
    StarterRoutineTemplate("Switch laundry", "Laundry", "Move one load to the dryer or drying area.", "normal", 5, "easy", "low", "every few days", "anytime", "laundry", startOffsetDays = 1),
    StarterRoutineTemplate("Fold one basket", "Laundry", "Fold one basket or one small pile.", "normal", 15, "medium", "medium", "every few days", "day off", "laundry", startOffsetDays = 2)
    )

    if ("catch up from mess" in goals) {
        base += StarterRoutineTemplate("Sort one visible pile", "Living Room", "Pick one pile and sort only that pile.", "high", 10, "easy", "low", "every few days", "anytime", "clutter", startOffsetDays = 1)
        base += StarterRoutineTemplate("Clear one bedroom floor path", "Bedroom", "Make one walking path easier to use.", "high", 8, "easy", "low", "every few days", "anytime", "floor clutter", startOffsetDays = 1)
    }
    if ("guest ready" in goals) {
        base += StarterRoutineTemplate("Entryway reset", "Entryway", "Put shoes, bags, and mail into one calmer landing spot.", "normal", 8, "easy", "low", "every few days", "before work", "clutter", startOffsetDays = 1)
        base += StarterRoutineTemplate("Guest bathroom touch-up", "Bathroom", "Reset the sink, mirror, towel, and trash.", "normal", 10, "medium", "medium", "weekly", "day off", "bathroom reset", startOffsetDays = 2)
    }
    if ("low energy maintenance" in goals) {
        base += StarterRoutineTemplate("Five-minute reset basket", "Living Room", "Put loose items into one basket and stop there if that is enough.", "normal", 5, "easy", "low", "daily", "anytime", "clutter")
    }
    if (wantsDelegation) {
        base += StarterRoutineTemplate("Kid-friendly toy pickup", "Kids Room", "Put toys or loose items into one bin. Easy to assign later.", "normal", 5, "easy", "low", "daily", "anytime", "clutter")
        base += StarterRoutineTemplate("Shared table reset", "Living Room", "Clear one shared surface. Good household assignment candidate.", "normal", 5, "easy", "low", "every few days", "anytime", "clutter", startOffsetDays = 1)
    }
    return base
}

internal fun starterRoomsFor(profile: StarterRoutineProfile): List<RoomEntity> {
    val selected = profile.selectedRooms.ifEmpty { setOf("Kitchen", "Bathroom", "Bedroom", "Living Room", "Laundry") }
    val rooms = mutableListOf<RoomEntity>()
    selected.filterNot { it.equals("Bedroom", true) || it.equals("Bathroom", true) }.forEach { rooms += starterRoomForName(it) }
    val bedroomTotal = if (selected.any { it.equals("Bedroom", true) }) profile.bedroomCount.coerceIn(1, 6) else 0
    val bathroomTotal = if (selected.any { it.equals("Bathroom", true) }) profile.bathroomCount.coerceIn(1, 5) else 0
    repeat(bedroomTotal) { index -> rooms += starterRoomForName(if (index == 0) "Bedroom" else "Bedroom ${index + 1}") }
    repeat(bathroomTotal) { index -> rooms += starterRoomForName(if (index == 0) "Bathroom" else "Bathroom ${index + 1}") }
    return rooms.distinctBy { it.name.lowercase() }
}

private fun starterRoomForName(name: String): RoomEntity {
    val lower = name.lowercase()
    return when {
        lower.contains("kitchen") -> RoomEntity(name = name, roomType = "Kitchen", iconName = "kitchen", tidyScore = 62, priority = "high", defaultTaskFrequency = "daily", notes = "Dishes, counters, trash, and quick food-area resets.")
        lower.contains("bath") -> RoomEntity(name = name, roomType = "Bathroom", iconName = "bath", tidyScore = 68, priority = "normal", notes = "Sink, towels, mirror, trash, and small wipe-downs.")
        lower.contains("bed") -> RoomEntity(name = name, roomType = "Bedroom", iconName = "bed", tidyScore = 64, priority = "high", notes = "Laundry, bed reset, nightstand, and floor path.")
        lower.contains("living") -> RoomEntity(name = name, roomType = "Living Room", iconName = "sofa", tidyScore = 72, priority = "normal", notes = "Shared clutter reset, surfaces, and vacuuming.")
        lower.contains("laundry") -> RoomEntity(name = name, roomType = "Laundry", iconName = "laundry", tidyScore = 66, priority = "normal", defaultTaskFrequency = "every few days", notes = "Start, switch, and fold one practical load.")
        lower.contains("entry") -> RoomEntity(name = name, roomType = "Entryway", iconName = "entry", tidyScore = 70, priority = "normal", notes = "Shoes, bags, coats, and mail landing zone.")
        lower.contains("basement") -> RoomEntity(name = name, roomType = "Basement", iconName = "basement", tidyScore = 48, priority = "high", defaultTaskIntensity = "medium", notes = "Storage, floor paths, shelves, cords, and bigger reset projects.")
        lower.contains("garage") -> RoomEntity(name = name, roomType = "Garage", iconName = "garage", tidyScore = 55, priority = "normal", notes = "Floor paths, tools, boxes, and seasonal storage.")
        lower.contains("office") -> RoomEntity(name = name, roomType = "Office", iconName = "office", tidyScore = 68, priority = "normal", notes = "Desk surface, papers, cords, and floor clutter.")
        lower.contains("kid") || lower.contains("play") -> RoomEntity(name = name, roomType = "Kids Room", iconName = "kids", tidyScore = 62, priority = "normal", notes = "Toy bins, laundry, floor paths, and easy delegation candidates.")
        else -> RoomEntity(name = name, roomType = name, iconName = "room", tidyScore = 68, priority = "normal", notes = "Starter room added during setup.")
    }
}

private fun RoomEntity.matchesStarterRoom(templateRoomType: String): Boolean {
    val target = templateRoomType.lowercase()
    val roomText = "$name $roomType".lowercase()
    return when (target) {
        "bedroom" -> roomText.contains("bedroom") || roomText.contains("guest bedroom")
        "bathroom" -> roomText.contains("bathroom") || roomText.contains("bath")
        "living room" -> roomText.contains("living room") || roomText.contains("family room")
        "kids room" -> roomText.contains("kids room") || roomText.contains("playroom")
        else -> roomText.contains(target)
    }
}
