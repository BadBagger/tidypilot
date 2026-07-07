package com.smithware.tidypilot.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity(tableName = "cleaning_tasks")
data class CleaningTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roomId: String,
    val description: String = "",
    val priority: String = "normal",
    val estimatedMinutes: Int = 10,
    val difficulty: String = "easy",
    val energyRequired: String = "low",
    val frequencyType: String = "weekly",
    val preferredTime: String = "anytime",
    val isQuickResetTask: Boolean = false,
    val isDeepCleanTask: Boolean = false,
    val photoDetectableCategory: String = "other",
    val lastCompletedAt: LocalDateTime? = null,
    val nextDueAt: LocalDate? = LocalDate.now(),
    val isArchived: Boolean = false,
    val skippedCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roomType: String,
    val iconName: String = "room",
    val tidyScore: Int = 70,
    val priority: String = "normal",
    val defaultTaskIntensity: String = "medium",
    val defaultTaskFrequency: String = "weekly",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "work_shifts")
data class WorkShiftEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val label: String = "Work",
    val expectedExhaustionLevel: String = "medium",
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "energy_check_ins")
data class EnergyCheckInEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val energyLevel: String = "medium",
    val moodLabel: String = "steady",
    val availableMinutes: Int = 15,
    val afterWorkExhaustion: Boolean = false,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "daily_cleaning_plans")
data class DailyCleaningPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate = LocalDate.now(),
    val workStatus: String = "free day",
    val energyLevel: String = "medium",
    val availableMinutes: Int = 15,
    val planType: String = "balanced reset",
    val suggestedTaskIds: String = "",
    val completedTaskIds: String = "",
    val skippedTaskIds: String = "",
    val adaptedReason: String = "Built from today's schedule and energy check-in.",
    val sourceType: String = "planning_engine",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "task_completions")
data class TaskCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val completedAt: LocalDateTime = LocalDateTime.now(),
    val durationMinutes: Int = 0,
    val energyLevelAtCompletion: String = "medium",
    val notes: String = ""
)

@Entity(tableName = "room_photo_scans")
data class RoomPhotoScanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val roomId: String,
    val imageUri: String,
    val scanDate: LocalDateTime = LocalDateTime.now(),
    val tidyScore: Int,
    val messScore: Int,
    val detectedIssueTags: String,
    val suggestedTaskIds: String = "",
    val estimatedCleanupMinutes: Int,
    val confidenceSummary: String,
    val userFeedback: String = "",
    val note: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "scan_issues")
data class ScanIssueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scanId: String,
    val tag: String,
    val label: String,
    val confidence: Float,
    val suggestedAction: String,
    val estimatedMinutes: Int,
    val energyLevel: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String = "local_settings",
    val defaultCleaningIntensity: String = "gentle",
    val accentStyle: String = "warm orange",
    val defaultEnergyLevel: String = "medium",
    val defaultTaskDurationMinutes: Int = 10,
    val workdayPlanningBehavior: String = "keep it light",
    val dayOffPlanningBehavior: String = "allow bigger resets",
    val defaultRecoveryMinutesAfterWork: Int = 45,
    val reminderEnabled: Boolean = false,
    val preferredReminderTime: String = "18:30",
    val quietHoursStart: String = "21:00",
    val quietHoursEnd: String = "08:00",
    val lowEnergyReminderMode: String = "gentle",
    val workdayReminderBehavior: String = "after shift",
    val dayOffReminderBehavior: String = "morning reset",
    val minimumExhaustedTaskMinutes: Int = 5,
    val savePhotosLocally: Boolean = true,
    val themeMode: String = "system"
)

fun pipe(items: List<String>): String = items.filter { it.isNotBlank() }.joinToString("|")
fun String.unpipe(): List<String> = split("|").map { it.trim() }.filter { it.isNotBlank() }
