package com.smithware.mvpstarter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val idea: String,
    val audience: String,
    val coreProblem: String,
    val status: String,
    val notes: String,
    val checklist: String,
    val progress: Int,
    val archived: Boolean = false,
    val lastEdited: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: String = "local_settings",
    val title: String = "MVP Starter Studio",
    val notes: String = "Your app ideas stay on this device.",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

val defaultChecklist = listOf(
    "Lock MVP",
    "Confirm the core user problem",
    "Define local data models",
    "Write launch copy",
    "Generate Codex build prompt",
    "Test release APK"
)
