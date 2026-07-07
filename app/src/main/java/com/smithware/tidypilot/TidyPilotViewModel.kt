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
    val remindersEnabled: Boolean = true,
    val savePhotosLocally: Boolean = true
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
        preferences.savePhotosLocally
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
            savePhotosLocally = values[11] as Boolean
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
                    energyLevel = energyLevel ?: if (exhausted) "low" else current.latestCheckIn?.energyLevel ?: "medium",
                    moodLabel = if (exhausted) "exhausted" else current.latestCheckIn?.moodLabel ?: "steady",
                    availableMinutes = availableMinutes ?: if (exhausted) current.settings.minimumExhaustedTaskMinutes else current.latestCheckIn?.availableMinutes ?: 15,
                    afterWorkExhaustion = exhausted || current.latestCheckIn?.afterWorkExhaustion == true,
                    notes = if (exhausted) "No guilt. Minimum reset requested." else current.latestCheckIn?.notes ?: ""
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
                minimumExhaustedMinutes = current.settings.minimumExhaustedTaskMinutes
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
        viewModelScope.launch { repository.saveRoom(room.copy(name = room.name.trim(), roomType = room.roomType.trim().ifBlank { "Room" }, notes = room.notes.trim())) }
        return null
    }

    fun saveShift(shift: WorkShiftEntity): String? {
        if (!shift.endTime.isAfter(shift.startTime)) return "End time must be after start time."
        viewModelScope.launch { repository.saveShift(shift) }
        return null
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
    fun deleteShift(shift: WorkShiftEntity) { viewModelScope.launch { repository.deleteShift(shift) } }

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

    fun updateSettings(settings: AppSettingsEntity, themeMode: String, reminders: Boolean, savePhotos: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.copy(themeMode = themeMode, reminderEnabled = reminders, savePhotosLocally = savePhotos))
            preferences.setThemeMode(themeMode)
            preferences.setRemindersEnabled(reminders)
            preferences.setSavePhotosLocally(savePhotos)
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.seedIfEmpty()
            replan()
        }
    }
}

fun parseDate(value: String): LocalDate = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
fun parseTime(value: String): LocalTime = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(9, 0))
