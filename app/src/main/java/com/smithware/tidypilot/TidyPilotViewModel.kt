package com.smithware.tidypilot

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smithware.tidypilot.data.AppSettingsEntity
import com.smithware.tidypilot.data.CleaningTaskEntity
import com.smithware.tidypilot.data.DailyCleaningPlanEntity
import com.smithware.tidypilot.data.EnergyCheckInEntity
import com.smithware.tidypilot.data.PlanningEngine
import com.smithware.tidypilot.data.PreferencesStore
import com.smithware.tidypilot.data.RoomEntity
import com.smithware.tidypilot.data.RoomPhotoAnalyzer
import com.smithware.tidypilot.data.RoomPhotoScanEntity
import com.smithware.tidypilot.data.ScanIssueEntity
import com.smithware.tidypilot.data.TaskCompletionEntity
import com.smithware.tidypilot.data.TidyPilotDatabase
import com.smithware.tidypilot.data.TidyPilotRepository
import com.smithware.tidypilot.data.WorkShiftEntity
import com.smithware.tidypilot.data.pipe
import com.smithware.tidypilot.data.unpipe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TidyPilotState(
    val tasks: List<CleaningTaskEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val shifts: List<WorkShiftEntity> = emptyList(),
    val energy: List<EnergyCheckInEntity> = emptyList(),
    val plans: List<DailyCleaningPlanEntity> = emptyList(),
    val completions: List<TaskCompletionEntity> = emptyList(),
    val scans: List<RoomPhotoScanEntity> = emptyList(),
    val issues: List<ScanIssueEntity> = emptyList(),
    val settings: AppSettingsEntity = AppSettingsEntity(),
    val themeMode: String = "system",
    val remindersEnabled: Boolean = false,
    val savePhotosLocally: Boolean = true,
    val onboardingComplete: Boolean = false
) {
    val today: LocalDate = LocalDate.now()
    val latestCheckIn: EnergyCheckInEntity? = energy.firstOrNull { it.date == today } ?: energy.firstOrNull()
    val todayShift: WorkShiftEntity? = shifts.firstOrNull { it.date == today }
    val todayPlan: DailyCleaningPlanEntity? = plans.firstOrNull { it.date == today }
    val todayCompletions: List<TaskCompletionEntity> = completions.filter { it.completedAt.toLocalDate() == today }
    val suggestedTasks: List<CleaningTaskEntity> = todayPlan?.suggestedTaskIds?.unpipe()?.mapNotNull { id -> tasks.firstOrNull { it.id == id } } ?: emptyList()
    val completedTodayCount: Int = todayCompletions.size
    val weeklyCompletedCount: Int = completions.count { it.completedAt.toLocalDate() >= today.minusDays(6) }
    val streak: Int = calculateStreak(completions)
    val averageTidyScore: Int = if (rooms.isEmpty()) 0 else rooms.map { it.tidyScore }.average().toInt()
    val lowEnergyTask: CleaningTaskEntity? = tasks.filter { it.energyRequired == "low" && it.estimatedMinutes <= settings.minimumExhaustedTaskMinutes.coerceAtLeast(5) }.minByOrNull { it.estimatedMinutes }
}

private fun calculateStreak(completions: List<TaskCompletionEntity>): Int {
    val dates = completions.map { it.completedAt.toLocalDate() }.toSet()
    var day = LocalDate.now()
    var count = 0
    while (day in dates) {
        count++
        day = day.minusDays(1)
    }
    return count
}

class TidyPilotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TidyPilotRepository(TidyPilotDatabase.get(application).dao())
    private val preferences = PreferencesStore(application)
    private val planner = PlanningEngine()
    private val analyzer = RoomPhotoAnalyzer()

    val state: StateFlow<TidyPilotState> = combine(
        repository.tasks,
        repository.rooms,
        repository.shifts,
        repository.energy,
        repository.plans,
        repository.completions,
        repository.scans,
        repository.issues,
        repository.settings,
        preferences.themeMode,
        preferences.remindersEnabled,
        preferences.savePhotosLocally,
        preferences.onboardingComplete
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        TidyPilotState(
            tasks = values[0] as List<CleaningTaskEntity>,
            rooms = values[1] as List<RoomEntity>,
            shifts = values[2] as List<WorkShiftEntity>,
            energy = values[3] as List<EnergyCheckInEntity>,
            plans = values[4] as List<DailyCleaningPlanEntity>,
            completions = values[5] as List<TaskCompletionEntity>,
            scans = values[6] as List<RoomPhotoScanEntity>,
            issues = values[7] as List<ScanIssueEntity>,
            settings = values[8] as? AppSettingsEntity ?: AppSettingsEntity(),
            themeMode = values[9] as String,
            remindersEnabled = values[10] as Boolean,
            savePhotosLocally = values[11] as Boolean,
            onboardingComplete = values[12] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TidyPilotState())

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            replan()
        }
    }

    fun replan(exhausted: Boolean = false, availableMinutes: Int? = null, energyLevel: String? = null) {
        viewModelScope.launch {
            val current = state.value
            val checkIn = if (exhausted || availableMinutes != null || energyLevel != null) {
                EnergyCheckInEntity(
                    energyLevel = energyLevel ?: if (exhausted) "very low" else current.latestCheckIn?.energyLevel ?: "medium",
                    moodLabel = if (exhausted) "exhausted" else current.latestCheckIn?.moodLabel ?: "steady",
                    availableMinutes = availableMinutes ?: if (exhausted) current.settings.minimumExhaustedTaskMinutes else current.latestCheckIn?.availableMinutes ?: 15,
                    afterWorkExhaustion = exhausted || current.latestCheckIn?.afterWorkExhaustion == true,
                    notes = if (exhausted) "Minimum reset requested." else current.latestCheckIn?.notes ?: ""
                ).also { repository.saveEnergy(it) }
            } else {
                current.latestCheckIn
            }
            val result = planner.buildPlan(
                now = LocalDateTime.now(),
                rooms = current.rooms,
                tasks = current.tasks,
                shifts = current.shifts,
                checkIn = checkIn,
                scans = current.scans,
                minimumExhaustedMinutes = current.settings.minimumExhaustedTaskMinutes,
                completions = current.completions
            )
            repository.savePlan(
                DailyCleaningPlanEntity(
                    date = LocalDate.now(),
                    workStatus = result.workStatus,
                    energyLevel = checkIn?.energyLevel ?: "medium",
                    availableMinutes = checkIn?.availableMinutes ?: 15,
                    planType = result.planType,
                    suggestedTaskIds = pipe(result.suggestedTasks.map { it.id }),
                    adaptedReason = result.adaptedReason,
                    sourceType = result.sourceType
                )
            )
        }
    }

    fun quickClean(minutes: Int?, energyLevel: String? = null) {
        viewModelScope.launch {
            val current = state.value
            val energy = energyLevel ?: current.latestCheckIn?.energyLevel ?: "medium"
            val limit = minutes ?: 60
            val tasks = buildQuickCleanTasks(current, limit, energy, fullReset = minutes == null)
            val checkIn = EnergyCheckInEntity(
                energyLevel = energy,
                moodLabel = if (minutes == null) "full reset" else "quick clean",
                availableMinutes = limit,
                afterWorkExhaustion = energy == "very low" || energy == "low",
                notes = if (minutes == null) "Quick Clean full reset requested." else "Quick Clean ${limit}-minute plan requested."
            )
            repository.saveEnergy(checkIn)
            repository.savePlan(
                DailyCleaningPlanEntity(
                    date = LocalDate.now(),
                    workStatus = current.todayShift?.let { "quick clean around work" } ?: "quick clean",
                    energyLevel = energy,
                    availableMinutes = limit,
                    planType = if (minutes == null) "Quick Clean full reset" else "Quick Clean $limit min",
                    suggestedTaskIds = pipe(tasks.map { it.id }),
                    adaptedReason = quickCleanReason(limit, tasks.size, energy, minutes == null),
                    sourceType = "quick_clean"
                )
            )
        }
    }

    fun saveEnergy(level: String, mood: String, minutes: Int, exhausted: Boolean, notes: String) {
        viewModelScope.launch {
            repository.saveEnergy(EnergyCheckInEntity(energyLevel = level, moodLabel = mood, availableMinutes = minutes, afterWorkExhaustion = exhausted, notes = notes))
            replan(energyLevel = level, availableMinutes = minutes)
        }
    }

    fun saveTask(existing: CleaningTaskEntity?, task: CleaningTaskEntity): String? {
        if (task.name.isBlank()) return "Task name is required."
        if (task.roomId.isBlank()) return "Choose a room before saving."
        viewModelScope.launch { repository.saveTask((existing ?: task).copy(
            name = task.name.trim(),
            roomId = task.roomId,
            description = task.description.trim(),
            priority = task.priority,
            estimatedMinutes = task.estimatedMinutes.coerceAtLeast(1),
            difficulty = task.difficulty.ifBlank { "easy" },
            energyRequired = task.energyRequired,
            frequencyType = task.frequencyType,
            preferredTime = task.preferredTime,
            isQuickResetTask = task.isQuickResetTask,
            isDeepCleanTask = task.isDeepCleanTask,
            photoDetectableCategory = task.photoDetectableCategory,
            nextDueAt = task.nextDueAt
        )) }
        return null
    }

    fun saveRoom(room: RoomEntity): String? {
        if (room.name.isBlank()) return "Room name is required."
        viewModelScope.launch {
            repository.saveRoom(
                room.copy(
                    name = room.name.trim(),
                    roomType = room.roomType.trim().ifBlank { "Room" },
                    priority = room.priority.ifBlank { "normal" },
                    defaultTaskFrequency = room.defaultTaskFrequency.ifBlank { "weekly" },
                    notes = room.notes.trim()
                )
            )
        }
        return null
    }

    fun saveShift(shift: WorkShiftEntity): String? {
        if (!shift.endTime.isAfter(shift.startTime)) return "End time must be after start time."
        viewModelScope.launch {
            repository.saveShift(shift)
            replan()
        }
        return null
    }

    fun saveShifts(shifts: List<WorkShiftEntity>) {
        viewModelScope.launch {
            shifts.forEach { repository.saveShift(it) }
            replan()
        }
    }

    fun markComplete(task: CleaningTaskEntity) {
        viewModelScope.launch {
            repository.markTaskComplete(task, state.value.latestCheckIn?.energyLevel ?: "medium")
            replan()
        }
    }

    fun skipTask(task: CleaningTaskEntity) {
        viewModelScope.launch {
            repository.skipTask(task)
            replan()
        }
    }

    fun snoozeTask(task: CleaningTaskEntity) {
        viewModelScope.launch {
            repository.snoozeTask(task)
            replan()
        }
    }

    fun deleteTask(task: CleaningTaskEntity) { viewModelScope.launch { repository.deleteTask(task) } }
    fun deleteRoom(room: RoomEntity) { viewModelScope.launch { repository.deleteRoom(room) } }
    fun archiveRoom(room: RoomEntity) { viewModelScope.launch { repository.archiveRoom(room) } }
    fun deleteShift(shift: WorkShiftEntity) {
        viewModelScope.launch {
            repository.deleteShift(shift)
            replan()
        }
    }

    fun markDayOff(date: LocalDate) {
        viewModelScope.launch {
            state.value.shifts.filter { it.date == date }.forEach { repository.deleteShift(it) }
            replan()
        }
    }

    fun analyzePhoto(room: RoomEntity, imageUri: Uri, note: String) {
        viewModelScope.launch {
            repository.saveScanAnalysis(room, imageUri.toString(), note, analyzer.analyze(room, note, imageUri.toString()))
            replan()
        }
    }

    fun addTasksFromScan(scan: RoomPhotoScanEntity) {
        val room = state.value.rooms.firstOrNull { it.id == scan.roomId } ?: return
        val related = state.value.issues.filter { it.scanId == scan.id }
        viewModelScope.launch {
            related.forEach {
                repository.saveTask(
                    CleaningTaskEntity(
                        name = it.suggestedAction,
                        roomId = room.id,
                        description = "Suggested from room scan: ${it.label}",
                        priority = if (it.energyLevel == "low") "normal" else "high",
                        estimatedMinutes = it.estimatedMinutes,
                        energyRequired = it.energyLevel,
                        frequencyType = "one-time",
                        preferredTime = "anytime",
                        isQuickResetTask = it.estimatedMinutes <= 10,
                        photoDetectableCategory = it.tag,
                        nextDueAt = LocalDate.now()
                    )
                )
            }
            replan()
        }
    }

    fun setScanFeedback(scanId: String, feedback: String) {
        viewModelScope.launch { repository.setScanFeedback(scanId, feedback) }
    }

    fun markScanReviewed(scanId: String) {
        viewModelScope.launch { repository.markScanReviewed(scanId) }
    }

    fun updateScanIssueStatus(issueId: String, status: String, createdTaskId: String? = null) {
        if (issueId.startsWith("manual-")) return
        viewModelScope.launch { repository.updateScanIssueStatus(issueId, status, createdTaskId) }
    }

    fun updateSettings(settings: AppSettingsEntity, themeMode: String, reminders: Boolean, savePhotos: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.copy(themeMode = themeMode, reminderEnabled = reminders, savePhotosLocally = savePhotos))
            preferences.setThemeMode(themeMode)
            preferences.setRemindersEnabled(reminders)
            preferences.setSavePhotosLocally(savePhotos)
        }
    }

    fun completeOnboarding(starterRooms: Set<String>, reminders: Boolean) {
        viewModelScope.launch {
            val selected = starterRooms.map { it.lowercase() }.toSet()
            state.value.rooms.forEach { room ->
                val isSelected = room.name.lowercase() in selected
                repository.saveRoom(
                    room.copy(
                        priority = when {
                            isSelected && room.priority == "low" -> "normal"
                            isSelected -> room.priority
                            else -> "low"
                        }
                    )
                )
            }
            repository.updateSettings(state.value.settings.copy(reminderEnabled = reminders))
            preferences.setRemindersEnabled(reminders)
            preferences.setOnboardingComplete(true)
            replan()
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetDemoData()
            replan()
        }
    }

    fun clearScanData() {
        viewModelScope.launch {
            repository.clearScanData()
            replan()
        }
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            repository.deleteAllLocalData()
            preferences.setOnboardingComplete(false)
            preferences.setRemindersEnabled(false)
            preferences.setThemeMode("system")
            preferences.setSavePhotosLocally(true)
        }
    }
}

private fun buildQuickCleanTasks(state: TidyPilotState, minutes: Int, energy: String, fullReset: Boolean): List<CleaningTaskEntity> {
    val maxTasks = when {
        fullReset -> 5
        minutes <= 5 -> 2
        minutes <= 10 -> 2
        minutes <= 15 -> 3
        else -> 5
    }
    val energyRank = mapOf("very low" to 1, "low" to 1, "medium" to 2, "high" to 3)
    fun energyFits(task: CleaningTaskEntity): Boolean =
        fullReset || (energyRank[task.energyRequired] ?: 1) <= (energyRank[energy] ?: 2)
    fun quickScore(task: CleaningTaskEntity): Int {
        val room = state.rooms.firstOrNull { it.id == task.roomId }
        val due = task.nextDueAt?.let { !it.isAfter(state.today) } ?: true
        return listOf(
            if (task.isQuickResetTask) 24 else 0,
            if (due) 18 else 0,
            when (task.priority) {
                "urgent" -> 18
                "high" -> 14
                "normal" -> 8
                else -> 3
            },
            if ((room?.tidyScore ?: 100) < 70) 10 else 0,
            if (task.estimatedMinutes <= 5) 10 else if (task.estimatedMinutes <= 10) 6 else 0,
            if (task.photoDetectableCategory in listOf("trash", "dishes", "clutter", "surface wipe", "laundry", "floor clutter")) 6 else 0,
            -task.estimatedMinutes
        ).sum()
    }
    val candidates = state.tasks
        .filter { !it.isArchived && it.estimatedMinutes <= minutes && energyFits(it) }
        .sortedWith(compareByDescending<CleaningTaskEntity> { quickScore(it) }.thenBy { it.estimatedMinutes })
    val selected = mutableListOf<CleaningTaskEntity>()
    var used = 0
    for (task in candidates) {
        if (selected.size >= maxTasks) break
        if (used + task.estimatedMinutes <= minutes || selected.isEmpty()) {
            selected += task
            used += task.estimatedMinutes
        }
    }
    return selected
}

private fun quickCleanReason(minutes: Int, count: Int, energy: String, fullReset: Boolean): String = when {
    fullReset -> "Quick Clean built a full reset with up to five practical tasks."
    minutes <= 5 -> "Quick Clean found the smallest useful reset for $energy energy."
    minutes <= 10 -> "Quick Clean picked a short mini plan that fits into 10 minutes."
    minutes <= 15 -> "Quick Clean balanced a few manageable tasks for your current energy."
    else -> "Quick Clean built a room-reset style plan without overloading the dashboard."
}.let { "$it $count task${if (count == 1) "" else "s"} suggested." }

fun parseDate(value: String): LocalDate = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
fun parseTime(value: String): LocalTime = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(9, 0))
