package com.smithware.tidypilot.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.util.Locale

data class ScheduleImportCandidate(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val label: String,
    val expectedExhaustionLevel: String,
    val sourceLine: String,
    val confidenceLabel: String
)

object ScheduleImportParser {
    private val timeRangeRegex = Regex(
        """(?i)\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\s*(?:-|–|to)\s*(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\b"""
    )
    private val numericDateRegex = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""")
    private val dayRegex = Regex("""(?i)\b(mon(?:day)?|tue(?:s|sday)?|wed(?:nesday)?|thu(?:rs|rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\b""")
    private val offRegex = Regex("""(?i)\b(off|pto|vacation|day\s*off|not\s*scheduled)\b""")

    fun parse(rawText: String, anchorDate: LocalDate = LocalDate.now()): List<ScheduleImportCandidate> {
        return rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { offRegex.containsMatchIn(it) }
            .mapNotNull { line -> parseLine(line, anchorDate) }
            .distinctBy { "${it.date}-${it.startTime}-${it.endTime}" }
            .sortedWith(compareBy<ScheduleImportCandidate> { it.date }.thenBy { it.startTime })
            .toList()
    }

    private fun parseLine(line: String, anchorDate: LocalDate): ScheduleImportCandidate? {
        val range = timeRangeRegex.find(line) ?: return null
        val date = parseDate(line, anchorDate) ?: return null
        val endPeriod = range.groupValues[6].normalizePeriod()
        val startPeriod = range.groupValues[3].normalizePeriod() ?: inferStartPeriod(range.groupValues[1].toInt(), range.groupValues[4].toInt(), endPeriod)
        val start = parseTime(range.groupValues[1], range.groupValues[2], startPeriod)
        val end = parseTime(range.groupValues[4], range.groupValues[5], endPeriod ?: startPeriod)
        val adjustedEnd = if (end <= start) end.plusHours(12) else end
        val durationHours = java.time.Duration.between(start, adjustedEnd).toHours()
        val label = when {
            line.contains("open", ignoreCase = true) -> "Opening shift"
            line.contains("clos", ignoreCase = true) -> "Closing shift"
            line.contains("training", ignoreCase = true) -> "Training shift"
            else -> "Imported shift"
        }
        return ScheduleImportCandidate(
            date = date,
            startTime = start,
            endTime = adjustedEnd,
            label = label,
            expectedExhaustionLevel = if (durationHours >= 8) "high" else if (durationHours >= 6) "medium" else "low",
            sourceLine = line,
            confidenceLabel = if (numericDateRegex.containsMatchIn(line) && endPeriod != null) "high" else "medium"
        )
    }

    private fun parseDate(line: String, anchorDate: LocalDate): LocalDate? {
        numericDateRegex.find(line)?.let { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return null
            val day = match.groupValues[2].toIntOrNull() ?: return null
            val rawYear = match.groupValues[3]
            val year = when {
                rawYear.length == 2 -> 2000 + rawYear.toInt()
                rawYear.length == 4 -> rawYear.toInt()
                else -> Year.now().value
            }
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
        val dayName = dayRegex.find(line)?.value ?: return null
        val wanted = dayOfWeek(dayName) ?: return null
        val monday = anchorDate.minusDays((anchorDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        return monday.plusDays((wanted.value - DayOfWeek.MONDAY.value).toLong())
    }

    private fun parseTime(hourText: String, minuteText: String, period: String?): LocalTime {
        var hour = hourText.toInt()
        val minute = minuteText.toIntOrNull() ?: 0
        if (period == "pm" && hour < 12) hour += 12
        if (period == "am" && hour == 12) hour = 0
        return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun inferStartPeriod(startHour: Int, endHour: Int, endPeriod: String?): String? {
        if (endPeriod == null) return null
        if (endPeriod == "pm" && startHour in 1..7 && endHour <= 11) return "pm"
        if (endPeriod == "pm" && startHour == 12) return "pm"
        if (endPeriod == "am" && startHour == 12) return "am"
        return if (endPeriod == "pm" && startHour > endHour) "am" else endPeriod
    }

    private fun String.normalizePeriod(): String? = lowercase(Locale.US)
        .replace(".", "")
        .takeIf { it == "am" || it == "pm" }

    private fun dayOfWeek(value: String): DayOfWeek? = when (value.take(3).lowercase(Locale.US)) {
        "mon" -> DayOfWeek.MONDAY
        "tue" -> DayOfWeek.TUESDAY
        "wed" -> DayOfWeek.WEDNESDAY
        "thu" -> DayOfWeek.THURSDAY
        "fri" -> DayOfWeek.FRIDAY
        "sat" -> DayOfWeek.SATURDAY
        "sun" -> DayOfWeek.SUNDAY
        else -> null
    }
}
