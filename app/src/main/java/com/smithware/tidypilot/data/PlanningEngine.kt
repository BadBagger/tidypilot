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
        val userProvidedContext = note.isNotBlank()
        val issues = mutableListOf<AnalysisIssue>()
        fun add(tag: String, label: String, action: String, minutes: Int, energy: String, confidence: Float) {
            issues += AnalysisIssue(tag, label, action, minutes, energy, confidence)
        }
        if ("kitchen" in lower) {
            if (lower.hasAny("dishes", "dishwasher", "plates", "cups")) add("dishes_visible", "Dishes visible", "Load or rinse dishes", 10, "medium", 0.74f)
            if (lower.hasAny("counter", "surface", "island", "clutter")) add("cluttered_surface", "Cluttered counter", "Clear one counter section", 5, "low", 0.7f)
            if (lower.hasAny("sink", "full sink")) add("sink_full", "Sink looks full", "Empty or stage the sink", 8, "medium", 0.66f)
            if (lower.hasAny("trash", "bag", "overflow")) add("trash_visible", "Trash visible", "Take out kitchen trash", 3, "low", 0.64f)
            if (lower.hasAny("floor", "crumbs", "sweep")) add("floor_clutter", "Kitchen floor needs attention", "Sweep one kitchen floor zone", 8, "medium", 0.58f)
            add("wipe_needed", "Wipe needed", "Wipe counter", 4, "low", 0.61f)
        }
        if ("bath" in lower) {
            if (lower.hasAny("counter", "sink", "toiletries", "surface", "clutter")) add("bathroom_counter_mess", "Bathroom counter mess", "Reset bathroom counter", 6, "low", 0.7f)
            if (lower.hasAny("mirror", "spots")) add("mirror_wipe_needed", "Mirror wipe needed", "Wipe bathroom mirror", 4, "low", 0.58f)
            if (lower.hasAny("towel", "towels", "laundry")) add("bathroom_towels_visible", "Towels visible", "Gather towels into laundry", 5, "low", 0.6f)
            if (lower.hasAny("shower", "tub")) add("shower_reset_needed", "Shower or tub reset", "Do one shower/tub reset pass", 12, "medium", 0.56f)
            add("wipe_needed", "Visible need for wiping", "Wipe sink and faucet", 5, "low", 0.63f)
            if (!userProvidedContext) {
                add("bathroom_counter_mess", "Possible bathroom counter reset", "Review the counter and gather loose items", 6, "low", 0.44f)
                add("floor_clutter", "Possible bathroom floor reset", "Clear one bathroom floor area", 5, "low", 0.42f)
            }
        }
        if ("bed" in lower) {
            if (lower.hasAny("bed", "unmade", "blanket", "sheets")) add("unmade_bed", "Bed reset needed", "Make the bed good enough", 4, "low", 0.67f)
            if (lower.hasAny("laundry", "clothes", "hamper")) add("laundry_visible", "Laundry visible", "Put clothes in one basket", 5, "low", 0.62f)
            if (lower.hasAny("nightstand", "dresser", "surface", "desk")) add("bedroom_surface_clutter", "Bedroom surface clutter", "Reset one bedside or dresser surface", 7, "low", 0.6f)
            if (lower.hasAny("floor", "shoes", "bags")) add("floor_clutter", "Bedroom floor clutter", "Clear one bedroom floor zone", 8, "medium", 0.59f)
            if (lower.hasAny("closet", "boxes", "storage")) add("closet_or_box_clutter", "Closet or box clutter", "Group bedroom storage items together", 10, "medium", 0.55f)
            if (!userProvidedContext) {
                add("laundry_visible", "Possible laundry visible", "Put visible clothes in one basket", 5, "low", 0.45f)
                add("floor_clutter", "Possible bedroom floor clutter", "Clear one bedroom floor zone", 8, "medium", 0.44f)
                add("bedroom_surface_clutter", "Possible bedroom surface clutter", "Reset one bedside or dresser surface", 7, "low", 0.42f)
            }
        }
        if ("living" in lower || "family room" in lower || "den" in lower) {
            if (lower.hasAny("floor", "toys", "clutter")) add("floor_clutter", "Living room floor clutter", "Pick up one living room floor zone", 8, "low", 0.62f)
            if (lower.hasAny("coffee table", "table", "surface", "clutter")) add("living_surface_clutter", "Living room surface clutter", "Clear one table or surface", 6, "low", 0.64f)
            if (lower.hasAny("couch", "sofa", "blanket", "pillow")) add("couch_reset_needed", "Couch reset needed", "Reset couch pillows and blankets", 4, "low", 0.6f)
            if (lower.hasAny("cord", "remote", "electronics")) add("electronics_clutter", "Electronics clutter", "Group remotes and cords", 5, "low", 0.55f)
            if (lower.hasAny("vacuum", "rug", "crumbs")) add("floor_clean_needed", "Floor clean needed", "Vacuum or sweep one visible zone", 12, "medium", 0.54f)
        }
        if ("entry" in lower || "mudroom" in lower || "hall" in lower) {
            if (lower.hasAny("shoes", "shoe")) add("shoes_visible", "Shoes visible", "Line up or bin loose shoes", 5, "low", 0.66f)
            if (lower.hasAny("bag", "bags", "backpack", "coat")) add("entry_bag_clutter", "Bags or coats visible", "Hang or group entryway bags and coats", 6, "low", 0.62f)
            if (lower.hasAny("mail", "paper", "keys")) add("mail_or_keys_clutter", "Mail or key drop clutter", "Reset the entry drop zone", 5, "low", 0.58f)
            add("entry_path_clutter", "Entry path clutter", "Clear the entry walking path", 6, "low", 0.56f)
        }
        if ("laundry" in lower) {
            if (lower.hasAny("washer", "dryer", "machine")) add("laundry_machine_reset", "Laundry machine reset", "Switch or start one laundry load", 7, "low", 0.68f)
            if (lower.hasAny("fold", "basket", "clean clothes")) add("folding_needed", "Folding needed", "Fold one small laundry stack", 12, "medium", 0.6f)
            if (lower.hasAny("floor", "clothes")) add("laundry_visible", "Laundry visible", "Put floor clothes in one basket", 6, "low", 0.62f)
            if (lower.hasAny("surface", "top", "shelf")) add("laundry_surface_clutter", "Laundry surface clutter", "Clear the washer or shelf top", 5, "low", 0.56f)
        }
        if ("office" in lower || "desk" in lower) {
            if (lower.hasAny("desk", "paper", "papers", "notebooks", "surface", "clutter")) add("office_desk_clutter", "Desk clutter", "Clear one desk section", 8, "low", 0.66f)
            if (lower.hasAny("cord", "cords", "electronics", "laptop", "charger")) add("electronics_clutter", "Electronics or cords visible", "Group cords and electronics", 6, "low", 0.58f)
            if (lower.hasAny("floor", "scattered", "stools", "boxes")) add("floor_clutter", "Office floor clutter", "Clear one floor path", 8, "low", 0.58f)
            if (lower.hasAny("trash", "packaging")) add("trash_visible", "Trash or packaging visible", "Collect trash into one bag", 5, "low", 0.55f)
        }
        if ("basement" in lower || "storage" in lower || "garage" in lower) {
            if (lower.hasAny("floor", "walking path", "path", "walkway", "open space")) {
                add("basement_floor_path", "Floor path clutter", "Clear one walking path around equipment", 10, "medium", 0.74f)
            }
            if (lower.hasAny("shelf", "shelves", "rack", "storage", "bins", "boxes")) {
                add("storage_shelf_clutter", "Storage shelf clutter", "Group loose shelf items into one bin", 12, "medium", 0.7f)
            }
            if (lower.hasAny("gear", "workout", "bench", "weights", "bike", "tools", "equipment")) {
                add("loose_gear_visible", "Loose gear visible", "Put loose gear in one zone", 8, "low", 0.66f)
            }
            if (lower.hasAny("cord", "cords", "cable", "cables", "charger", "power strip")) {
                add("cords_or_equipment_clutter", "Cords or equipment clutter", "Coil cords and move small items off the floor", 7, "low", 0.62f)
            }
            if (lower.hasAny("trash", "cardboard", "paper", "packaging")) {
                add("trash_visible", "Trash or cardboard pass", "Take one trash or cardboard pass", 5, "low", 0.6f)
            }
            if (issues.none { it.tag in basementTags }) {
                add("basement_floor_path", "Floor path clutter", "Clear one walking path around equipment", 10, "medium", 0.62f)
                add("storage_shelf_clutter", "Storage shelf clutter", "Group loose shelf items into one bin", 12, "medium", 0.58f)
                add("general_reset_needed", "Basement reset needed", "Set a 10-minute basement reset timer", 10, "medium", 0.55f)
            }
        }
        if ("laundry" in lower || "clothes" in lower) add("laundry_visible", "Laundry visible", "Start one laundry load", 7, "low", 0.66f)
        if ("trash" in lower) add("trash_visible", "Trash visible", "Take trash to one bag", 3, "low", 0.64f)
        if ("floor" in lower || issues.isEmpty()) {
            add("floor_clutter", "Floor clutter", "Pick up one visible area", 6, "low", 0.6f)
            add("general_reset_needed", "General room reset", "Set a 5-minute timer", 5, "low", 0.57f)
        }
        val distinct = issues.distinctBy { it.tag }.take(8)
        val estimated = distinct.sumOf { it.estimatedMinutes }.coerceAtMost(60)
        val weightedIssueMess = distinct.sumOf { it.messWeight() }
        val severityMess = when {
            lower.hasAny("trashed", "disaster", "super untidy", "very messy", "rough", "bad", "overwhelming") -> 42
            lower.hasAny("messy", "untidy", "cluttered", "needs bigger reset", "needs reset") -> 28
            lower.hasAny("floor clutter", "laundry visible", "trash visible", "sink full", "full sink", "overloaded sink", "pile", "blocked", "shelves cluttered") -> 18
            else -> 0
        }
        val existingRoomMess = (100 - room.tidyScore).coerceIn(0, 55)
        val unknownPhotoMessFloor = when {
            userProvidedContext -> 20
            lower.hasAny("bed", "basement", "storage", "garage", "laundry") -> 48
            else -> 42
        }
        val contextMess = when {
            "basement" in lower && ("super untidy" in lower || "rough" in lower || "bad" in lower || "needs reset" in lower) -> 38
            "basement" in lower || "storage" in lower || "garage" in lower -> 28
            "rough" in lower || "needs reset" in lower -> 15
            else -> 0
        }
        val rawMess = maxOf(weightedIssueMess + contextMess + severityMess, existingRoomMess, unknownPhotoMessFloor)
        val mess = rawMess.coerceIn(20, 92)
        val messLevel = messLevelForScore(mess)
        val confidence = scanConfidenceFor(userProvidedContext, distinct.size, lower)
        val detectedZones = detectedZonesFor(lower, distinct, messLevel)
        val summary = summaryForMessLevel(messLevel, distinct, confidence)
        return AnalysisOutput(
            imageUri = imageUri,
            tidyScore = 100 - mess,
            messScore = mess,
            messLevel = messLevel,
            confidence = confidence,
            summary = summary,
            detectedZones = detectedZones,
            estimatedCleanupMinutes = estimated,
            confidenceSummary = if (userProvidedContext) {
                "Local v1 estimate based on room type, your visible-photo notes, and practical household mess patterns."
            } else {
                "Local v1 estimate. Review or add visible-photo details before turning suggestions into chores."
            },
            issues = distinct
        )
    }
}

enum class MessLevel(val key: String, val friendlyLabel: String) {
    CLEAR("clear", "Looks mostly clear"),
    LIGHT_RESET("light_reset", "Quick reset"),
    MODERATE_MESS("moderate_mess", "Needs attention"),
    HEAVY_RESET("heavy_reset", "Bigger reset")
}

data class DetectedZone(
    val name: String,
    val type: String,
    val clutterScore: Int,
    val notes: String
)

data class PhotoAnalysisResult(
    val id: String,
    val roomId: String,
    val scanId: String,
    val messLevel: MessLevel,
    val messScore: Int,
    val confidence: String,
    val summary: String,
    val detectedZones: List<DetectedZone>,
    val suggestedIssues: List<AnalysisIssue>,
    val suggestedTasks: List<PhotoSuggestedTask>,
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

data class PhotoSuggestedTask(
    val title: String,
    val roomId: String,
    val estimatedMinutes: Int,
    val difficulty: String,
    val energyRequired: String,
    val priority: String,
    val source: String = "photo_scan",
    val notes: String = ""
)

fun messLevelForScore(messScore: Int): MessLevel = when {
    messScore < 25 -> MessLevel.CLEAR
    messScore < 50 -> MessLevel.LIGHT_RESET
    messScore < 75 -> MessLevel.MODERATE_MESS
    else -> MessLevel.HEAVY_RESET
}

fun taskLimitForMessLevel(level: MessLevel): Int = when (level) {
    MessLevel.CLEAR -> 1
    MessLevel.LIGHT_RESET -> 2
    MessLevel.MODERATE_MESS -> 4
    MessLevel.HEAVY_RESET -> 6
}

fun filterScanTasksForEnergy(issues: List<AnalysisIssue>, energyLevel: String, level: MessLevel): List<AnalysisIssue> {
    val limit = taskLimitForMessLevel(level)
    val filtered = when (energyLevel) {
        "very low", "low" -> issues.filter { it.energyLevel == "low" && it.estimatedMinutes <= 10 }
        "high" -> issues
        else -> issues.filter { it.estimatedMinutes <= 15 || it.energyLevel != "high" }
    }
    return filtered.take(limit).ifEmpty { issues.filter { it.estimatedMinutes <= 10 }.take(limit) }
}

private fun scanConfidenceFor(userProvidedContext: Boolean, issueCount: Int, lower: String): String = when {
    !userProvidedContext -> "low"
    lower.hasAny("blurry", "dark", "glare", "too close", "not enough room") -> "low"
    issueCount >= 4 -> "high"
    issueCount >= 2 -> "medium"
    else -> "low"
}

private fun detectedZonesFor(lower: String, issues: List<AnalysisIssue>, messLevel: MessLevel): List<DetectedZone> {
    val zones = mutableListOf<DetectedZone>()
    fun zone(name: String, type: String, score: Int, notes: String) {
        zones += DetectedZone(name, type, score.coerceIn(0, 100), notes)
    }
    if (issues.any { it.tag.contains("floor") || it.tag.contains("path") }) zone("Floor path", "floor", messLevel.zoneScore(), "Visible floor or walking-path reset may help.")
    if (issues.any { it.tag.contains("surface") || it.tag.contains("counter") || it.tag.contains("shelf") }) zone("Main surface", "general surface", messLevel.zoneScore() - 8, "Start with one surface instead of the whole room.")
    if (issues.any { it.tag.contains("laundry") || it.tag.contains("folding") }) zone("Laundry area", "laundry", messLevel.zoneScore() - 5, "Clothes or laundry-related items may be a quick visible win.")
    if (issues.any { it.tag.contains("sink") || it.tag.contains("dishes") }) zone("Sink or dishes", "sink", messLevel.zoneScore(), "Dishes or sink reset can anchor the room.")
    if ("bed" in lower) zone("Bed area", "bed", messLevel.zoneScore() - 10, "A good-enough bed reset can make the room feel calmer.")
    if (zones.isEmpty()) zone("Room view", "unknown", messLevel.zoneScore(), "Estimated from scan review and room type.")
    return zones.distinctBy { it.name }.take(4)
}

private fun MessLevel.zoneScore(): Int = when (this) {
    MessLevel.CLEAR -> 15
    MessLevel.LIGHT_RESET -> 38
    MessLevel.MODERATE_MESS -> 64
    MessLevel.HEAVY_RESET -> 88
}

private fun summaryForMessLevel(level: MessLevel, issues: List<AnalysisIssue>, confidence: String): String {
    val first = issues.firstOrNull()?.suggestedAction ?: "Start with one small visible reset"
    val confidenceCopy = if (confidence == "low") " Review before creating tasks." else ""
    return when (level) {
        MessLevel.CLEAR -> "Looks mostly clear. A small reset would help keep it easy.$confidenceCopy"
        MessLevel.LIGHT_RESET -> "Quick reset suggested. Start with one surface or a short visible win.$confidenceCopy"
        MessLevel.MODERATE_MESS -> "Moderate reset suggested. Start with ${first.lowercase()}, then handle floors or surfaces if you have time.$confidenceCopy"
        MessLevel.HEAVY_RESET -> "Bigger reset suggested. No shame — start with one small task like ${first.lowercase()} and stop there if that is enough.$confidenceCopy"
    }
}

private fun AnalysisIssue.messWeight(): Int = when (tag) {
    "basement_floor_path", "floor_clutter", "sink_full", "storage_shelf_clutter" -> 18
    "laundry_visible", "dishes_visible", "trash_visible", "bathroom_counter_mess" -> 16
    "cluttered_surface", "bedroom_surface_clutter", "living_surface_clutter", "closet_or_box_clutter" -> 14
    "office_desk_clutter" -> 14
    "loose_gear_visible", "cords_or_equipment_clutter", "electronics_clutter", "floor_clean_needed" -> 12
    "unmade_bed", "couch_reset_needed", "bathroom_towels_visible", "laundry_machine_reset", "folding_needed" -> 10
    else -> 8
}

private val basementTags = setOf(
    "basement_floor_path",
    "storage_shelf_clutter",
    "loose_gear_visible",
    "cords_or_equipment_clutter",
    "trash_visible"
)

private fun String.hasAny(vararg needles: String): Boolean = needles.any { it in this }

data class AnalysisOutput(
    val imageUri: String,
    val tidyScore: Int,
    val messScore: Int,
    val messLevel: MessLevel,
    val confidence: String,
    val summary: String,
    val detectedZones: List<DetectedZone>,
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
    val confidence: Float,
    val description: String = label,
    val category: String = tag,
    val difficulty: String = when {
        energyLevel == "high" || estimatedMinutes >= 30 -> "hard"
        energyLevel == "medium" || estimatedMinutes >= 15 -> "medium"
        else -> "easy"
    }
)
