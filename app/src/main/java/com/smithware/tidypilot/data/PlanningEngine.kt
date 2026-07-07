package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.temporal.ChronoUnit

data class PlanningResult(
    val workStatus: String,
    val planType: String,
    val suggestedTasks: List<CleaningTaskEntity>,
    val adaptedReason: String,
    val sourceType: String = "planning_engine",
    val recommendations: List<PlanRecommendation> = emptyList()
)

data class PlanRecommendation(
    val task: CleaningTaskEntity,
    val score: Int,
    val label: String,
    val reasons: List<String>
)

class PlanningEngine {
    fun buildPlan(
        now: LocalDateTime,
        rooms: List<RoomEntity>,
        tasks: List<CleaningTaskEntity>,
        shifts: List<WorkShiftEntity>,
        checkIn: EnergyCheckInEntity?,
        scans: List<RoomPhotoScanEntity>,
        minimumExhaustedMinutes: Int,
        completions: List<TaskCompletionEntity> = emptyList()
    ): PlanningResult {
        val today = now.toLocalDate()
        val shift = shifts.firstOrNull { it.date == today }
        val workStatus = workStatusFor(now.toLocalTime(), shift)
        val longShift = shift?.let { shiftHours(it) >= 8 } == true
        val minutesUntilShift = shift?.let { Duration.between(now.toLocalTime(), it.startTime).toMinutes().toInt() } ?: Int.MAX_VALUE
        val energy = checkIn?.energyLevel ?: if (workStatus == "after work" && longShift) "low" else shift?.expectedExhaustionLevel ?: "medium"
        val minutes = (checkIn?.availableMinutes ?: defaultMinutes(workStatus, energy, shift, minutesUntilShift)).coerceAtLeast(0)
        val roughRoomIds = scans
            .filter { it.scanDate.toLocalDate() >= today.minusDays(2) && it.messScore >= 45 }
            .map { it.roomId }
            .toSet()
        val roomPriority = rooms.associate { room ->
            room.id to priorityWeight(room.priority) + if (room.id in roughRoomIds) 3 else 0
        }
        val roomScores = rooms.associate { it.id to it.tidyScore }
        val recentCompletionByTask = completions
            .filter { it.completedAt >= now.minusHours(36) }
            .groupBy { it.taskId }
            .mapValues { entry -> entry.value.maxOf { it.completedAt } }
        val exhausted = isLowEnergy(energy) ||
            checkIn?.afterWorkExhaustion == true ||
            workStatus == "after work" && (shift?.expectedExhaustionLevel == "high" || longShift)
        val shiftSoon = shift != null && workStatus == "before work"
        val tooTight = workStatus == "too tight today" || minutes <= 5

        val scored = tasks
            .filter { !it.isArchived }
            .mapNotNull { task ->
                scoreTask(
                    task = task,
                    today = today,
                    workStatus = workStatus,
                    energy = energy,
                    availableMinutes = minutes,
                    exhausted = exhausted,
                    shiftSoon = shiftSoon,
                    tooTight = tooTight,
                    roughRoomIds = roughRoomIds,
                    roomPriority = roomPriority[task.roomId] ?: 0,
                    roomScore = roomScores[task.roomId] ?: 75,
                    recentlyCompletedAt = recentCompletionByTask[task.id],
                    minimumExhaustedMinutes = minimumExhaustedMinutes
                )
            }
            .sortedWith(compareByDescending<PlanRecommendation> { it.score }.thenBy { it.task.estimatedMinutes })

        val limit = when {
            tooTight -> 1
            exhausted -> 1
            energy == "medium" -> 3
            shift == null -> 5
            else -> 4
        }
        val selectedRecommendations = scored.take(limit).fitWithin(minutes).ifEmpty {
            minimumResetTasks(tasks, minimumExhaustedMinutes).map {
                PlanRecommendation(it, 20, LOW_ENERGY_TASK, listOf("Small reset fallback"))
            }
        }
        val reason = when {
            tooTight -> "Too tight today. Suggested only the smallest task that fits before work."
            exhausted -> "Built a minimum reset plan with the smallest useful task."
            shiftSoon -> "You have a shift soon, so the plan only uses quick tasks that fit before work."
            workStatus == "after work" && longShift -> "After a long shift, the plan favors low-energy reset tasks."
            roughRoomIds.isNotEmpty() && isLowEnergy(energy) -> "Recent scan results promoted small visible wins without adding a heavy plan."
            roughRoomIds.isNotEmpty() -> "Recent room scan results promoted cleanup actions for the rooms that need attention."
            shift != null -> "Let's keep it light around your shift."
            else -> "Good day for a deeper reset if you feel up to it."
        }
        return PlanningResult(
            workStatus = workStatus,
            planType = when {
                tooTight -> TOO_TIGHT_TODAY
                workStatus == "day off" -> DAY_OFF_RESET
                workStatus == "before work" -> BEFORE_WORK
                workStatus == "after work" -> AFTER_WORK
                exhausted -> "minimum reset"
                else -> "adaptive daily plan"
            },
            suggestedTasks = selectedRecommendations.map { it.task },
            adaptedReason = reason,
            recommendations = selectedRecommendations
        )
    }

    private fun scoreTask(
        task: CleaningTaskEntity,
        today: LocalDate,
        workStatus: String,
        energy: String,
        availableMinutes: Int,
        exhausted: Boolean,
        shiftSoon: Boolean,
        tooTight: Boolean,
        roughRoomIds: Set<String>,
        roomPriority: Int,
        roomScore: Int,
        recentlyCompletedAt: LocalDateTime?,
        minimumExhaustedMinutes: Int
    ): PlanRecommendation? {
        val due = task.nextDueAt == null || !task.nextDueAt.isAfter(today)
        val overdueDays = task.nextDueAt?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) } ?: 0
        val scanPromoted = task.roomId in roughRoomIds
        val recentlyCompleted = recentlyCompletedAt != null && overdueDays == 0L
        val fitsAvailableTime = task.estimatedMinutes <= availableMinutes
        val fitsBeforeWork = !shiftSoon || task.estimatedMinutes <= availableMinutes.coerceAtMost(10)
        val fitsTightWindow = !tooTight || task.estimatedMinutes <= availableMinutes.coerceAtMost(5)
        val exhaustedLimit = if (isVeryLowEnergy(energy)) 5 else minimumExhaustedMinutes.coerceAtLeast(5)
        val fitsExhaustedDay = !exhausted ||
            task.energyRequired == "low" && task.estimatedMinutes <= exhaustedLimit

        if (!due && !scanPromoted && task.skippedCount == 0) return null
        if (recentlyCompleted && !scanPromoted) return null
        if (!fitsBeforeWork || !fitsTightWindow || !fitsExhaustedDay) return null

        var score = 0
        val reasons = mutableListOf<String>()

        // Due and overdue chores should surface, but the overdue bonus is capped
        // so one old chore cannot crowd out every realistic quick win.
        if (due) {
            score += 16
            reasons += "Due today"
        }
        if (overdueDays > 0) {
            score += (overdueDays * 4).coerceAtMost(20).toInt()
            reasons += "Overdue"
        }

        // Room priority and low tidy scores make the plan respond to the visible
        // state of the home instead of behaving like a static chore checklist.
        score += roomPriority * 5
        if (roomPriority >= 3) reasons += "Priority room"
        if (roomScore < 65) {
            score += 10
            reasons += "Room needs attention"
        }

        // Energy fit is intentionally stronger than raw task priority. Low energy
        // should pull small low-effort chores upward and push high-effort chores down.
        score += when {
            energy == task.energyRequired -> 14
            isVeryLowEnergy(energy) && task.energyRequired == "low" && task.estimatedMinutes <= 5 -> 24
            isLowEnergy(energy) && task.energyRequired == "low" -> 18
            energy == "high" && task.energyRequired == "high" -> 10
            energy == "high" && task.energyRequired == "medium" -> 8
            energy == "medium" && task.energyRequired == "low" -> 7
            else -> -14
        }

        // Duration fit rewards tasks the user can actually finish in the current
        // time window. Longer tasks are not forbidden unless the work/energy gate
        // above makes them impractical.
        score += when {
            task.estimatedMinutes <= 5 -> 12
            fitsAvailableTime -> 8
            else -> -20
        }
        if (!fitsAvailableTime) reasons += "Too long for current window"

        // Work timing changes both score and labels. Before work favors small
        // tasks; day off permits deep work; workdays defer deep cleaning.
        if (shiftSoon && task.estimatedMinutes <= 10 && task.energyRequired == "low") {
            score += 12
            reasons += BEFORE_WORK
        }
        if (workStatus == "day off" && task.isDeepCleanTask) {
            score += 18
            reasons += DAY_OFF_RESET
        }
        if (workStatus == "after work" && task.energyRequired == "low") {
            score += 10
            reasons += AFTER_WORK
        }
        if (workStatus != "day off" && task.isDeepCleanTask) {
            score -= 10
            reasons += SAVE_FOR_DAY_OFF
        }

        // Repeating chores rely on nextDueAt as source of truth. Skips add only a
        // small recovery boost so missed chores do not overload the next plan.
        if (task.frequencyType != "one-time") score += 4
        if (task.skippedCount > 0) {
            score += task.skippedCount.coerceAtMost(3) * 3
            reasons += "Skipped before"
        }

        // Recent room scans promote related rooms, but the hard time/energy gates
        // above still prevent unrealistic recommendations.
        if (scanPromoted) {
            score += 14
            reasons += "Recent scan"
        }

        // Difficulty is a deterministic penalty based on energy, duration, and
        // deep-clean status. It keeps "practical now" ahead of "important but huge."
        score -= difficultyPenalty(task)

        val label = labelFor(task, score, workStatus, energy, availableMinutes, overdueDays, scanPromoted, roomScore)
        return PlanRecommendation(task, score, label, reasons.distinct())
    }

    private fun defaultMinutes(workStatus: String, energy: String, shift: WorkShiftEntity?, minutesUntilShift: Int): Int = when {
        isVeryLowEnergy(energy) -> 5
        energy == "low" -> 10
        workStatus == "too tight today" -> 5
        workStatus == "before work" -> minutesUntilShift.coerceIn(5, 15)
        workStatus == "after work" -> 12
        shift == null -> if (energy == "high") 45 else 30
        energy == "high" -> 35
        else -> 20
    }

    private fun workStatusFor(now: LocalTime, shift: WorkShiftEntity?): String = when {
        shift == null -> "day off"
        now.isBefore(shift.startTime) && Duration.between(now, shift.startTime).toMinutes() <= 20 -> "too tight today"
        now.isBefore(shift.startTime) -> "before work"
        now.isAfter(shift.endTime) -> "after work"
        else -> "working today"
    }

    private fun shiftHours(shift: WorkShiftEntity): Long = Duration.between(shift.startTime, shift.endTime).toHours()

    private fun priorityWeight(value: String): Int = when (value) {
        "urgent" -> 4
        "high" -> 3
        "normal" -> 2
        else -> 1
    }

    private fun List<PlanRecommendation>.fitWithin(minutes: Int): List<PlanRecommendation> {
        val selected = mutableListOf<PlanRecommendation>()
        var used = 0
        forEach {
            if (used + it.task.estimatedMinutes <= minutes || selected.isEmpty()) {
                selected += it
                used += it.task.estimatedMinutes
            }
        }
        return selected
    }

    private fun minimumResetTasks(tasks: List<CleaningTaskEntity>, minimumMinutes: Int): List<CleaningTaskEntity> =
        tasks.filter { it.isQuickResetTask && it.energyRequired == "low" && it.estimatedMinutes <= minimumMinutes.coerceAtLeast(5) }
            .sortedBy { it.estimatedMinutes }
            .take(1)

    private fun difficultyPenalty(task: CleaningTaskEntity): Int =
        energyWeight(task.energyRequired) * 3 + (task.estimatedMinutes / 10) * 2 + if (task.isDeepCleanTask) 8 else 0

    private fun energyWeight(value: String): Int = when (value) {
        "high" -> 3
        "medium" -> 2
        else -> 1
    }

    private fun isVeryLowEnergy(value: String): Boolean = value == "very low"

    private fun isLowEnergy(value: String): Boolean = value == "very low" || value == "low"

    private fun labelFor(
        task: CleaningTaskEntity,
        score: Int,
        workStatus: String,
        energy: String,
        availableMinutes: Int,
        overdueDays: Long,
        scanPromoted: Boolean,
        roomScore: Int
    ): String = when {
        workStatus == "too tight today" -> TOO_TIGHT_TODAY
        workStatus == "after work" && task.energyRequired == "low" -> AFTER_WORK
        workStatus == "day off" && task.isDeepCleanTask -> DAY_OFF_RESET
        isLowEnergy(energy) && task.energyRequired == "low" -> LOW_ENERGY_TASK
        workStatus == "before work" && task.estimatedMinutes <= availableMinutes.coerceAtMost(10) -> BEFORE_WORK
        workStatus != "day off" && task.isDeepCleanTask -> SAVE_FOR_DAY_OFF
        task.estimatedMinutes <= 5 && score >= 20 -> QUICK_WIN
        overdueDays > 0 || roomScore < 65 || scanPromoted -> NEEDS_ATTENTION
        task.priority == "urgent" || task.priority == "high" || score >= 45 -> HIGH_IMPACT_TASK
        else -> QUICK_WIN
    }

    companion object {
        const val QUICK_WIN = "Quick win"
        const val NEEDS_ATTENTION = "Needs attention"
        const val BEST_BEFORE_WORK = "Best before work"
        const val BEFORE_WORK = "Before work"
        const val AFTER_WORK = "After work"
        const val DAY_OFF_RESET = "Day off reset"
        const val TOO_TIGHT_TODAY = "Too tight today"
        const val SAVE_FOR_DAY_OFF = "Save for day off"
        const val LOW_ENERGY_TASK = "Low energy task"
        const val HIGH_IMPACT_TASK = "High impact task"

        fun displayLabelFor(
            task: CleaningTaskEntity,
            room: RoomEntity?,
            workStatus: String?,
            energy: String?,
            availableMinutes: Int,
            today: LocalDate = LocalDate.now()
        ): String {
            val overdue = task.nextDueAt?.isBefore(today) == true
            return when {
                workStatus == "too tight today" -> TOO_TIGHT_TODAY
                workStatus == "after work" && task.energyRequired == "low" -> AFTER_WORK
                workStatus == "day off" && task.isDeepCleanTask -> DAY_OFF_RESET
                (energy == "very low" || energy == "low") && task.energyRequired == "low" -> LOW_ENERGY_TASK
                workStatus == "before work" && task.estimatedMinutes <= availableMinutes.coerceAtMost(10) -> BEFORE_WORK
                workStatus != "day off" && task.isDeepCleanTask -> SAVE_FOR_DAY_OFF
                task.estimatedMinutes <= 5 -> QUICK_WIN
                overdue || (room?.tidyScore ?: 100) < 65 -> NEEDS_ATTENTION
                task.priority == "urgent" || task.priority == "high" -> HIGH_IMPACT_TASK
                else -> QUICK_WIN
            }
        }
    }
}

interface RoomImageAnalyzer {
    fun analyze(room: RoomEntity, note: String, imageUri: String): AnalysisOutput
}

class RoomPhotoAnalyzer : RoomImageAnalyzer {
    // Local v1 analyzer: deterministic room/note rules create useful cleanup suggestions
    // while preserving a clean replacement point for future on-device ML or approved services.
    override fun analyze(room: RoomEntity, note: String, imageUri: String): AnalysisOutput {
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
        val mess = (distinct.size * 14 + if ("rough" in lower || "needs reset" in lower) 15 else 0).coerceIn(20, 88)
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
