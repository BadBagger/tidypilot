package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PlanningResult(
    val workStatus: String,
    val planType: String,
    val suggestedTasks: List<CleaningTaskEntity>,
    val adaptedReason: String,
    val sourceType: String = "planning_engine"
)

class PlanningEngine {
    fun buildPlan(
        now: LocalDateTime,
        rooms: List<RoomEntity>,
        tasks: List<CleaningTaskEntity>,
        shifts: List<WorkShiftEntity>,
        checkIn: EnergyCheckInEntity?,
        scans: List<RoomPhotoScanEntity>,
        minimumExhaustedMinutes: Int
    ): PlanningResult {
        val today = now.toLocalDate()
        val shift = shifts.firstOrNull { it.date == today }
        val workStatus = workStatusFor(now.toLocalTime(), shift)
        val energy = checkIn?.energyLevel ?: shift?.expectedExhaustionLevel ?: "medium"
        val minutes = checkIn?.availableMinutes ?: defaultMinutes(workStatus, energy)
        val roughRoomIds = scans.filter { it.scanDate.toLocalDate() >= today.minusDays(2) && it.messScore >= 45 }.map { it.roomId }.toSet()
        val roomPriority = rooms.associate { it.id to priorityWeight(it.priority) + if (it.id in roughRoomIds) 3 else 0 }
        val exhausted = energy == "low" || checkIn?.afterWorkExhaustion == true || workStatus == "after work" && shift?.expectedExhaustionLevel == "high"
        val soon = shift != null && workStatus == "before work"

        val candidates = tasks.filter { task ->
            val due = task.nextDueAt == null || !task.nextDueAt.isAfter(today)
            val fitsEnergy = !exhausted || task.energyRequired == "low"
            val fitsTime = when {
                exhausted -> task.estimatedMinutes <= minimumExhaustedMinutes.coerceAtLeast(5)
                soon -> task.estimatedMinutes <= minutes.coerceAtMost(10)
                energy == "medium" -> task.estimatedMinutes <= 15
                else -> true
            }
            due && fitsEnergy && fitsTime
        }.sortedWith(
            compareByDescending<CleaningTaskEntity> { if (it.roomId in roughRoomIds) 1 else 0 }
                .thenByDescending { priorityWeight(it.priority) + (roomPriority[it.roomId] ?: 0) }
                .thenByDescending { if (it.isQuickResetTask) 1 else 0 }
                .thenBy { it.estimatedMinutes }
        )

        val limit = when {
            exhausted -> 1
            energy == "medium" -> 3
            shift == null -> 5
            else -> 4
        }
        val selected = candidates.take(limit).fitWithin(minutes).ifEmpty { minimumResetTasks(tasks, minimumExhaustedMinutes) }
        val reason = when {
            exhausted -> "No guilt. Built a minimum reset plan with the smallest useful task."
            soon -> "You have a shift soon, so the plan only uses quick tasks that fit before work."
            roughRoomIds.isNotEmpty() && energy == "low" -> "Recent scan results promoted small visible wins without adding a heavy plan."
            roughRoomIds.isNotEmpty() -> "Recent room scan results promoted cleanup actions for the rooms that need attention."
            shift != null -> "Let’s keep it light around your shift."
            else -> "Good day for a deeper reset if you feel up to it."
        }
        return PlanningResult(workStatus, if (exhausted) "minimum reset" else "adaptive daily plan", selected, reason)
    }

    private fun defaultMinutes(workStatus: String, energy: String): Int = when {
        energy == "low" -> 5
        workStatus == "before work" -> 10
        workStatus == "after work" -> 12
        energy == "high" -> 35
        else -> 20
    }

    private fun workStatusFor(now: LocalTime, shift: WorkShiftEntity?): String = when {
        shift == null -> "day off"
        now.isBefore(shift.startTime) -> "before work"
        now.isAfter(shift.endTime) -> "after work"
        else -> "working today"
    }

    private fun priorityWeight(value: String): Int = when (value) {
        "urgent" -> 4
        "high" -> 3
        "normal" -> 2
        else -> 1
    }

    private fun List<CleaningTaskEntity>.fitWithin(minutes: Int): List<CleaningTaskEntity> {
        val selected = mutableListOf<CleaningTaskEntity>()
        var used = 0
        forEach {
            if (used + it.estimatedMinutes <= minutes || selected.isEmpty()) {
                selected += it
                used += it.estimatedMinutes
            }
        }
        return selected
    }

    private fun minimumResetTasks(tasks: List<CleaningTaskEntity>, minimumMinutes: Int): List<CleaningTaskEntity> =
        tasks.filter { it.isQuickResetTask && it.energyRequired == "low" && it.estimatedMinutes <= minimumMinutes.coerceAtLeast(5) }
            .sortedBy { it.estimatedMinutes }
            .take(1)
}

class RoomPhotoAnalyzer {
    fun analyze(room: RoomEntity, note: String, imageUri: String): AnalysisOutput {
        val lower = "${room.roomType} ${room.name} $note".lowercase()
        val issues = mutableListOf<AnalysisIssue>()
        fun add(tag: String, label: String, action: String, minutes: Int, energy: String, confidence: Float) {
            issues += AnalysisIssue(tag, label, action, minutes, energy, confidence)
        }
        if ("kitchen" in lower) {
            add("dishes_visible", "Dishes visible", "Load or rinse dishes", 10, "medium", 0.72f)
            add("cluttered_surface", "Cluttered counter", "Clear one counter section", 5, "low", 0.68f)
            add("wipe_needed", "Wipe needed", "Wipe counter", 4, "low", 0.61f)
        }
        if ("bath" in lower) {
            add("bathroom_counter_mess", "Bathroom counter mess", "Reset bathroom counter", 6, "low", 0.7f)
            add("wipe_needed", "Visible need for wiping", "Wipe sink and faucet", 5, "low", 0.63f)
        }
        if ("bed" in lower) {
            add("unmade_bed", "Bed reset needed", "Make the bed good enough", 4, "low", 0.67f)
            add("laundry_visible", "Laundry visible", "Put clothes in one basket", 5, "low", 0.58f)
        }
        if ("laundry" in lower || "clothes" in lower) add("laundry_visible", "Laundry visible", "Start one laundry load", 7, "low", 0.66f)
        if ("trash" in lower) add("trash_visible", "Trash visible", "Take trash to one bag", 3, "low", 0.64f)
        if ("floor" in lower || "living" in lower || issues.isEmpty()) {
            add("floor_clutter", "Floor clutter", "Pick up one visible area", 6, "low", 0.6f)
            add("general_reset_needed", "General room reset", "Set a 5-minute timer", 5, "low", 0.57f)
        }
        val distinct = issues.distinctBy { it.tag }.take(5)
        val estimated = distinct.sumOf { it.estimatedMinutes }.coerceAtMost(35)
        val mess = (distinct.size * 14 + if ("bad" in lower || "got bad" in lower) 15 else 0).coerceIn(20, 88)
        return AnalysisOutput(
            imageUri = imageUri,
            tidyScore = 100 - mess,
            messScore = mess,
            estimatedCleanupMinutes = estimated,
            confidenceSummary = "Local v1 estimate based on room type, note, and practical household mess patterns.",
            issues = distinct
        )
    }
}

data class AnalysisOutput(
    val imageUri: String,
    val tidyScore: Int,
    val messScore: Int,
    val estimatedCleanupMinutes: Int,
    val confidenceSummary: String,
    val issues: List<AnalysisIssue>
)

data class AnalysisIssue(
    val tag: String,
    val label: String,
    val suggestedAction: String,
    val estimatedMinutes: Int,
    val energyLevel: String,
    val confidence: Float
)
