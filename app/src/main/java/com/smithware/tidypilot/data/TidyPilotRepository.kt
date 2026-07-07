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
        if (dao.roomCount() != 0) return
        dao.saveSettings(AppSettingsEntity())
        val kitchen = RoomEntity(name = "Kitchen", roomType = "Kitchen", iconName = "kitchen", tidyScore = 58, priority = "high", notes = "Counters and dishes are the biggest stress point.")
        val bath = RoomEntity(name = "Bathroom", roomType = "Bathroom", iconName = "bath", tidyScore = 72, priority = "normal")
        val bedroom = RoomEntity(name = "Bedroom", roomType = "Bedroom", iconName = "bed", tidyScore = 63, priority = "high")
        val living = RoomEntity(name = "Living Room", roomType = "Living Room", iconName = "sofa", tidyScore = 76, priority = "normal")
        val laundry = RoomEntity(name = "Laundry", roomType = "Laundry", iconName = "laundry", tidyScore = 54, priority = "normal")
        val entry = RoomEntity(name = "Entryway", roomType = "Entryway", iconName = "entry", tidyScore = 80, priority = "low")
        listOf(kitchen, bath, bedroom, living, laundry, entry).forEach { dao.saveRoom(it) }

        val today = LocalDate.now()
        val tasks = listOf(
            CleaningTaskEntity(name = "Clear kitchen counter", roomId = kitchen.id, priority = "high", estimatedMinutes = 5, energyRequired = "low", frequencyType = "daily", preferredTime = "after work", isQuickResetTask = true, photoDetectableCategory = "clutter", nextDueAt = today),
            CleaningTaskEntity(name = "Take out trash", roomId = kitchen.id, priority = "normal", estimatedMinutes = 3, energyRequired = "low", frequencyType = "every few days", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "trash", nextDueAt = today),
            CleaningTaskEntity(name = "Wipe bathroom sink", roomId = bath.id, priority = "normal", estimatedMinutes = 5, energyRequired = "low", frequencyType = "every few days", preferredTime = "before work", isQuickResetTask = true, photoDetectableCategory = "bathroom reset", nextDueAt = today.plusDays(1)),
            CleaningTaskEntity(name = "Start laundry", roomId = laundry.id, priority = "high", estimatedMinutes = 7, energyRequired = "low", frequencyType = "every few days", preferredTime = "after work", isQuickResetTask = true, photoDetectableCategory = "laundry", nextDueAt = today),
            CleaningTaskEntity(name = "Put away clean clothes", roomId = bedroom.id, priority = "normal", estimatedMinutes = 15, energyRequired = "medium", frequencyType = "weekly", preferredTime = "day off", photoDetectableCategory = "laundry", nextDueAt = today.plusDays(2), skippedCount = 1),
            CleaningTaskEntity(name = "Sweep kitchen floor", roomId = kitchen.id, priority = "normal", estimatedMinutes = 10, energyRequired = "medium", frequencyType = "weekly", preferredTime = "day off", photoDetectableCategory = "floor clutter", nextDueAt = today),
            CleaningTaskEntity(name = "Reset bedside table", roomId = bedroom.id, priority = "low", estimatedMinutes = 5, energyRequired = "low", frequencyType = "weekly", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "clutter", nextDueAt = today.plusDays(1)),
            CleaningTaskEntity(name = "Load dishwasher", roomId = kitchen.id, priority = "urgent", estimatedMinutes = 10, energyRequired = "medium", frequencyType = "daily", preferredTime = "after work", photoDetectableCategory = "dishes", nextDueAt = today),
            CleaningTaskEntity(name = "5-minute living room pickup", roomId = living.id, priority = "normal", estimatedMinutes = 5, energyRequired = "low", frequencyType = "daily", preferredTime = "anytime", isQuickResetTask = true, photoDetectableCategory = "floor clutter", nextDueAt = today),
            CleaningTaskEntity(name = "Deep clean shower", roomId = bath.id, priority = "low", estimatedMinutes = 30, energyRequired = "high", frequencyType = "monthly", preferredTime = "day off", isDeepCleanTask = true, photoDetectableCategory = "bathroom reset", nextDueAt = today.plusDays(4), skippedCount = 2)
        )
        tasks.forEach { dao.saveTask(it) }

        dao.saveEnergy(EnergyCheckInEntity(date = today, energyLevel = "medium", moodLabel = "tired but okay", availableMinutes = 20, afterWorkExhaustion = false, notes = "Good enough for today."))
        val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
        listOf(0L, 1L, 3L, 5L).forEachIndexed { index, offset ->
            dao.saveShift(
                WorkShiftEntity(
                    date = startOfWeek.plusDays(offset),
                    startTime = if (index == 3) LocalTime.of(7, 0) else LocalTime.of(9, 0),
                    endTime = if (index == 3) LocalTime.of(15, 30) else LocalTime.of(17, 30),
                    label = if (index == 3) "Early shift" else "Work shift",
                    expectedExhaustionLevel = if (index == 1) "high" else "medium",
                    notes = "Demo shift for adaptive planning."
                )
            )
        }

        val analyzer = RoomPhotoAnalyzer()
        listOf(
            kitchen to "kitchen got bad trash dishes",
            bedroom to "after work mess clothes on bed",
            living to "floor pickup needed"
        ).forEach { (room, note) ->
            saveScanAnalysis(room, "demo://${room.name}", note, analyzer.analyze(room, note, "demo://${room.name}"))
        }
    }

    suspend fun saveTask(task: CleaningTaskEntity) = dao.saveTask(task.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveRoom(room: RoomEntity) = dao.saveRoom(room.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveShift(shift: WorkShiftEntity) = dao.saveShift(shift.copy(updatedAt = LocalDateTime.now()))
    suspend fun saveEnergy(checkIn: EnergyCheckInEntity) = dao.saveEnergy(checkIn)
    suspend fun savePlan(plan: DailyCleaningPlanEntity) = dao.savePlan(plan.copy(updatedAt = LocalDateTime.now()))
    suspend fun deleteTask(task: CleaningTaskEntity) = dao.deleteTask(task)
    suspend fun deleteRoom(room: RoomEntity) = dao.deleteRoom(room)
    suspend fun deleteShift(shift: WorkShiftEntity) = dao.deleteShift(shift)

    suspend fun markTaskComplete(task: CleaningTaskEntity, energy: String) {
        val now = LocalDateTime.now()
        dao.saveCompletion(TaskCompletionEntity(taskId = task.id, completedAt = now, durationMinutes = task.estimatedMinutes, energyLevelAtCompletion = energy))
        dao.markTaskComplete(task.id, now, nextDate(task.frequencyType, now.toLocalDate()))
    }

    suspend fun skipTask(task: CleaningTaskEntity) = dao.skipTask(task.id, LocalDateTime.now())
    suspend fun snoozeTask(task: CleaningTaskEntity) = dao.snoozeTask(task.id, LocalDate.now().plusDays(1), LocalDateTime.now())
    suspend fun setScanFeedback(scanId: String, feedback: String) = dao.setScanFeedback(scanId, feedback)
    suspend fun updateSettings(settings: AppSettingsEntity) = dao.saveSettings(settings)

    suspend fun saveScanAnalysis(room: RoomEntity, imageUri: String, note: String, output: AnalysisOutput): RoomPhotoScanEntity {
        val scan = RoomPhotoScanEntity(
            roomId = room.id,
            imageUri = imageUri,
            tidyScore = output.tidyScore,
            messScore = output.messScore,
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
                    tag = issue.tag,
                    label = issue.label,
                    confidence = issue.confidence,
                    suggestedAction = issue.suggestedAction,
                    estimatedMinutes = issue.estimatedMinutes,
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
