package com.smithware.tidypilot.data

import java.time.LocalDate

fun selectOneThingTask(
    tasks: List<CleaningTaskEntity>,
    rooms: List<RoomEntity>,
    completions: List<TaskCompletionEntity>,
    today: LocalDate,
    availableMinutes: Int?,
    energyLevel: String,
    excludedTaskIds: Set<String> = emptySet()
): CleaningTaskEntity? {
    val roomById = rooms.associateBy { it.id }
    val energyRank = mapOf("very low" to 1, "low" to 1, "medium" to 2, "high" to 3)
    val maxEnergy = energyRank[energyLevel] ?: 2
    val minuteLimit = availableMinutes ?: Int.MAX_VALUE
    return tasks
        .asSequence()
        .filter { !it.isArchived }
        .filter { it.id !in excludedTaskIds }
        .filter { it.estimatedMinutes <= minuteLimit }
        .filter { (energyRank[it.energyRequired] ?: 1) <= maxEnergy || energyLevel == "high" }
        .map { task ->
            val room = roomById[task.roomId]
            val need = calculateTaskNeedScore(task, room, completions, today)
            task to oneThingScore(task, room, need, today, availableMinutes)
        }
        .sortedWith(
            compareByDescending<Pair<CleaningTaskEntity, Int>> { it.second }
                .thenBy { it.first.estimatedMinutes }
                .thenBy { it.first.name }
        )
        .firstOrNull()
        ?.first
}

fun oneThingWhy(task: CleaningTaskEntity, room: RoomEntity?, need: TaskNeedScore): String {
    val roomName = room?.name ?: "this room"
    val text = "${task.name} ${task.description} ${task.photoDetectableCategory}".lowercase()
    return when {
        "trash" in text || "garbage" in text -> "This can prevent smells and make $roomName feel better fast."
        "dishes" in text || "sink" in text -> "This clears a high-impact kitchen reset and makes the next task easier."
        "laundry" in text || "clothes" in text || "towels" in text -> "This keeps laundry from piling up and gives the room a quick reset."
        "counter" in text || "surface" in text || "clutter" in text -> "This will make $roomName feel cleaner fast."
        "floor" in text || "vacuum" in text || "sweep" in text -> "This opens up the room so the space feels easier to use."
        need.status == "Overdue" -> "This is the most useful overdue reset to handle first."
        need.status == "Needs attention" -> "This is worth doing soon and should make the room feel more under control."
        else -> "This is a small useful reset that fits what you picked."
    }
}

private fun oneThingScore(
    task: CleaningTaskEntity,
    room: RoomEntity?,
    need: TaskNeedScore,
    today: LocalDate,
    availableMinutes: Int?
): Int {
    val roomPriority = when (room?.priority) {
        "urgent" -> 16
        "high" -> 12
        "normal" -> 6
        else -> 2
    }
    val overdue = if (task.nextDueAt?.isBefore(today) == true) 18 else 0
    val quickWin = if (task.isQuickResetTask || task.estimatedMinutes <= 5) 14 else 0
    val fitBonus = when {
        availableMinutes == null -> if (task.isDeepCleanTask) 8 else 2
        task.estimatedMinutes <= availableMinutes.coerceAtMost(5) -> 12
        task.estimatedMinutes <= availableMinutes -> 6
        else -> -50
    }
    return need.score + roomPriority + overdue + quickWin + fitBonus
}
