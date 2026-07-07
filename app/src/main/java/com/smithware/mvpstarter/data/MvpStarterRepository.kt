package com.smithware.mvpstarter.data

import java.time.LocalDateTime

class MvpStarterRepository(private val dao: MvpStarterDao) {
    val projects = dao.observeProjects()
    val settings = dao.observeSettings()

    suspend fun seedIfEmpty() {
        if (dao.projectCount() == 0) {
            dao.saveSettings(SettingsEntity())
            dao.insertProject(
                ProjectEntity(
                    name = "Starter App Public Release",
                    idea = "Turn a rough app idea into a focused local-first Android MVP.",
                    audience = "Smithware builders who need fast, repeatable app delivery.",
                    coreProblem = "Every new app repeats the same scaffolding, storage, navigation, and release work.",
                    status = "MVP Locked",
                    notes = "Customize package names, copy, seed data, icon assets, and DevHub metadata before shipping.",
                    checklist = defaultChecklist.take(4).joinToString("|"),
                    progress = 67
                )
            )
            dao.insertProject(
                ProjectEntity(
                    name = "New Smithware App Brief",
                    idea = "Use the starter screens to capture the app's core workflow before adding advanced features.",
                    audience = "The target audience from the app spec.",
                    coreProblem = "The first release needs one clear problem, one useful workflow, and a clean APK handoff.",
                    status = "Draft",
                    notes = "Use this demo record to test edit, archive, detail, and export states.",
                    checklist = defaultChecklist.take(2).joinToString("|"),
                    progress = 34
                )
            )
        }
    }

    fun observeProject(id: String) = dao.observeProject(id)

    suspend fun saveProject(project: ProjectEntity) {
        dao.insertProject(project.copy(lastEdited = LocalDateTime.now()))
    }

    suspend fun deleteProject(project: ProjectEntity) = dao.deleteProject(project)

    suspend fun archiveProject(id: String) = dao.archiveProject(id, LocalDateTime.now())

    suspend fun saveSettings(settings: SettingsEntity) = dao.saveSettings(settings)
}
