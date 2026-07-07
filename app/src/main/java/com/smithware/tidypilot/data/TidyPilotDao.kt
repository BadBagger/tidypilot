package com.smithware.tidypilot.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface TidyPilotDao {
    @Query("SELECT * FROM cleaning_tasks WHERE isArchived = 0 ORDER BY priority DESC, estimatedMinutes ASC")
    fun observeTasks(): Flow<List<CleaningTaskEntity>>

    @Query("SELECT * FROM rooms WHERE isArchived = 0 ORDER BY priority DESC, name ASC")
    fun observeRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM work_shifts ORDER BY date ASC, startTime ASC")
    fun observeShifts(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM energy_check_ins ORDER BY date DESC, createdAt DESC")
    fun observeEnergy(): Flow<List<EnergyCheckInEntity>>

    @Query("SELECT * FROM daily_cleaning_plans ORDER BY date DESC, updatedAt DESC")
    fun observePlans(): Flow<List<DailyCleaningPlanEntity>>

    @Query("SELECT * FROM task_completions ORDER BY completedAt DESC")
    fun observeCompletions(): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM room_photo_scans ORDER BY scanDate DESC")
    fun observeScans(): Flow<List<RoomPhotoScanEntity>>

    @Query("SELECT * FROM scan_issues ORDER BY confidence DESC")
    fun observeIssues(): Flow<List<ScanIssueEntity>>

    @Query("SELECT * FROM app_settings WHERE id = 'local_settings' LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun roomCount(): Int

    @Query("SELECT name FROM rooms")
    suspend fun roomNames(): List<String>

    @Query("SELECT COUNT(*) FROM cleaning_tasks WHERE isArchived = 0")
    suspend fun activeTaskCount(): Int

    @Query("SELECT COUNT(*) FROM cleaning_tasks WHERE roomId = :roomId")
    suspend fun taskCountForRoom(roomId: String): Int

    @Query("SELECT COUNT(*) FROM room_photo_scans WHERE roomId = :roomId")
    suspend fun scanCountForRoom(roomId: String): Int

    @Query("SELECT COUNT(*) FROM cleaning_tasks WHERE isArchived = 0 AND (nextDueAt IS NULL OR nextDueAt <= :today)")
    suspend fun dueTaskCount(today: LocalDate): Int

    @Query("SELECT COUNT(*) FROM cleaning_tasks WHERE isArchived = 0 AND skippedCount > 0")
    suspend fun skippedTaskCount(): Int

    @Query("SELECT name FROM cleaning_tasks WHERE isArchived = 0 AND (nextDueAt IS NULL OR nextDueAt <= :today) ORDER BY CASE priority WHEN 'urgent' THEN 4 WHEN 'high' THEN 3 WHEN 'normal' THEN 2 ELSE 1 END DESC, estimatedMinutes ASC LIMIT 3")
    suspend fun dueTaskNames(today: LocalDate): List<String>

    @Query("SELECT AVG(tidyScore) FROM rooms WHERE isArchived = 0")
    suspend fun averageRoomScore(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTask(task: CleaningTaskEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveRoom(room: RoomEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveShift(shift: WorkShiftEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveEnergy(checkIn: EnergyCheckInEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlan(plan: DailyCleaningPlanEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCompletion(completion: TaskCompletionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveScan(scan: RoomPhotoScanEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveIssue(issue: ScanIssueEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(settings: AppSettingsEntity)

    @Delete suspend fun deleteTask(task: CleaningTaskEntity)
    @Delete suspend fun deleteRoom(room: RoomEntity)
    @Delete suspend fun deleteShift(shift: WorkShiftEntity)

    @Query("UPDATE cleaning_tasks SET lastCompletedAt = :completedAt, nextDueAt = :nextDueAt, skippedCount = 0, updatedAt = :completedAt WHERE id = :id")
    suspend fun markTaskComplete(id: String, completedAt: LocalDateTime, nextDueAt: LocalDate?)

    @Query("UPDATE cleaning_tasks SET skippedCount = skippedCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun skipTask(id: String, updatedAt: LocalDateTime)

    @Query("UPDATE cleaning_tasks SET nextDueAt = :nextDueAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun snoozeTask(id: String, nextDueAt: LocalDate, updatedAt: LocalDateTime)

    @Query("UPDATE rooms SET tidyScore = :score, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRoomScore(id: String, score: Int, updatedAt: LocalDateTime)

    @Query("UPDATE rooms SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveRoom(id: String, updatedAt: LocalDateTime)

    @Query("UPDATE room_photo_scans SET userFeedback = :feedback WHERE id = :scanId")
    suspend fun setScanFeedback(scanId: String, feedback: String)

    @Query("DELETE FROM scan_issues")
    suspend fun clearScanIssues()

    @Query("DELETE FROM room_photo_scans")
    suspend fun clearScans()

    @Query("DELETE FROM task_completions")
    suspend fun clearCompletions()

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun clearCompletionsForTask(taskId: String)

    @Query("DELETE FROM daily_cleaning_plans")
    suspend fun clearPlans()

    @Query("DELETE FROM energy_check_ins")
    suspend fun clearEnergy()

    @Query("DELETE FROM work_shifts")
    suspend fun clearShifts()

    @Query("DELETE FROM cleaning_tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM rooms")
    suspend fun clearRooms()

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()
}
