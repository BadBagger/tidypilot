package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TidyPilotRepository(private val dao: TidyPilotDao) {
    val tasks = dao.observeTasks()
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

    private fun defaultRooms(): List<RoomEntity> = listOf(
        RoomEntity(name = "Kitchen", roomType = "Kitchen", iconName = "kitchen", tidyScore = 58, priority = "high", defaultTaskFrequency = "daily", notes = "Dishes, counters, and trash are the main pressure points."),
        RoomEntity(name = "Bathroom", roomType = "Bathroom", iconName = "bath", tidyScore = 72, priority = "normal", notes = "Sink, mirror, and towels."),
        RoomEntity(name = "Bedroom", roomType = "Bedroom", iconName = "bed", tidyScore = 63, priority = "high", notes = "Laundry, nightstand, and floor resets."),
        RoomEntity(name = "Living Room", roomType = "Living Room", iconName = "sofa", tidyScore = 76, priority = "normal", notes = "Clutter reset and vacuuming."),
        RoomEntity(name = "Laundry", roomType = "Laundry", iconName = "laundry", tidyScore = 66, priority = "normal", defaultTaskFrequency = "every few days", notes = "Switch loads and fold clothes."),
        RoomEntity(name = "Basement", roomType = "Basement", iconName = "basement", tidyScore = 42, priority = "high", defaultTaskIntensity = "medium", defaultTaskFrequency = "weekly", notes = "Storage shelves, floor paths, cords, gear, and bigger reset projects.")
    )

    suspend fun resetDemoData() {
        dao.clearScanIssues()
        dao.clearScans()
        dao.clearCompletions()
        dao.clearPlans()
        dao.clearEnergy()
        dao.clearShifts()
        dao.clearTasks()
        dao.clearRooms()
        dao.clearSettings()
        seedIfEmpty()
    }

    suspend fun saveTask(task: CleaningTaskEntity) = dao.saveTask(task.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveRoom(room: RoomEntity) = dao.saveRoom(room.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveShift(shift: WorkShiftEntity) = dao.saveShift(shift.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveEnergy(checkIn: EnergyCheckInEntity) = dao.saveEnergy(checkIn)
    suspend fun savePlan(plan: DailyCleaningPlanEntity) = dao.savePlan(plan.copy(updatedAt = LocalDateTime.now()))
    suspend fun deleteTask(task: CleaningTaskEntity) {
        dao.clearCompletionsForTask(task.id)
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
        dao.saveCompletion(TaskCompletionEntity(taskId = task.id, completedAt = now, durationMinutes = task.estimatedMinutes, energyLevelAtCompletion = energy))
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
        dao.clearTasks()
        dao.clearRooms()
        dao.clearSettings()
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
