package com.smithware.mvpstarter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MvpStarterDao {
    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY lastEdited DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeProject(id: String): Flow<ProjectEntity?>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("UPDATE projects SET archived = 1, lastEdited = :editedAt WHERE id = :id")
    suspend fun archiveProject(id: String, editedAt: java.time.LocalDateTime)

    @Query("SELECT * FROM settings WHERE id = 'local_settings' LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)
}
