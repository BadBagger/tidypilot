package com.smithware.tidypilot.data

import java.time.DayOfWeek
import java.time.Duration
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
    val confidenceLabel: String,
    val isDayOff: Boolean = false
)

enum class ScheduleImportIssue(
    val title: String,
    val body: String,
    val tips: List<String>
) {
    NoTextDetected(
        title = "No text detected",
        body = "This screenshot was hard to read.",
        tips = listOf("Try cropping closer to the schedule list.", "Make sure dates and shift times are visible.")
    ),
    ImageTooBlurry(
        title = "Image may be hard to read",
        body = "The scanner found text, but the result looks noisy.",
        tips = listOf("Try a clearer screenshot if the preview looks wrong.", "You can edit the text before saving.")
    ),
    DatesDetectedTimesMissing(
        title = "Dates found, times missing",
        body = "The scanner can see dates, but shift times are not clear yet.",
        tips = listOf("Check that start and end times are visible.", "You can type missing times into the text box.")
    ),
    TimesDetectedDatesMissing(
        title = "Times found, dates missing",
        body = "The scanner can see shift times, but not the matching dates.",
        tips = listOf("Crop so day names or dates are visible.", "Review before saving.")
    ),
    ScheduleFormatUnclear(
        title = "Schedule format needs review",
        body = "The scanner found text, but could not confidently build a schedule.",
        tips = listOf("Try a tighter crop.", "You can still add shifts manually below.")
    )
}

data class ScheduleImportGuidance(
    val issue: ScheduleImportIssue,
    val detail: String? = null
)

object ScheduleImportGuidanceClassifier {
    private val dateRegex = Regex("""\b(?:\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?|(?:mon|tue|wed|thu|fri|sat|sun)\w*)\b""", RegexOption.IGNORE_CASE)
    private val timeRegex = Regex("""\b\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)\b|(?:\d{1,2}(?::\d{2})?\s*(?:-|\u2013|\u2014|to)\s*\d{1,2}(?::\d{2})?)""", RegexOption.IGNORE_CASE)

    fun fromText(rawText: String, candidates: List<ScheduleImportCandidate>): ScheduleImportGuidance? {
        if (rawText.isBlank()) return ScheduleImportGuidance(ScheduleImportIssue.NoTextDetected)
        if (candidates.isNotEmpty() && !looksNoisy(rawText)) return null

        val hasDate = dateRegex.containsMatchIn(rawText)
        val hasTime = timeRegex.containsMatchIn(rawText)
        return when {
            candidates.isNotEmpty() && looksNoisy(rawText) -> ScheduleImportGuidance(ScheduleImportIssue.ImageTooBlurry)
            hasDate && !hasTime -> ScheduleImportGuidance(ScheduleImportIssue.DatesDetectedTimesMissing)
            hasTime && !hasDate -> ScheduleImportGuidance(ScheduleImportIssue.TimesDetectedDatesMissing)
            looksNoisy(rawText) -> ScheduleImportGuidance(ScheduleImportIssue.ImageTooBlurry)
            else -> ScheduleImportGuidance(ScheduleImportIssue.ScheduleFormatUnclear)
        }
    }

    private fun looksNoisy(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 18) return true
        val lines = trimmed.lines().filter { it.isNotBlank() }
        val shortLineRatio = lines.count { it.trim().length <= 2 }.toDouble() / lines.size.coerceAtLeast(1)
        val readableRatio = trimmed.count { it.isLetterOrDigit() || it.isWhitespace() || it in "/:-." }.toDouble() / trimmed.length.coerceAtLeast(1)
        return shortLineRatio > 0.45 || readableRatio < 0.62
    }
}

object ScheduleImportParser {
    private data class DateSlot(
        val date: LocalDate,
        val lineIndex: Int,
        val label: String
    )

    private val timeRangeRegex = Regex(
        """(?i)\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\s*(?:-|\u2013|\u2014|to)\s*(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\b"""
    )
    private val numericDateRegex = Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""")
    private val dayRegex = Regex("""(?i)\b(mon(?:day)?|tue(?:s|sday)?|wed(?:nesday)?|thu(?:rs|rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\b""")
    private val offRegex = Regex("""(?i)\b(off|pto|vacation|unavailable|day\s*off|not\s*scheduled)\b""")

    fun parse(rawText: String, anchorDate: LocalDate = LocalDate.now()): List<ScheduleImportCandidate> {
        val weekListCandidates = parseWeekList(rawText, anchorDate)
        val candidates = if (weekListCandidates.size >= 3) {
            weekListCandidates
        } else {
            weekListCandidates + expandedLines(rawText)
            .mapNotNull { line -> parseLine(line, anchorDate) }
        }
        return candidates
            .distinctBy { "${it.date}-${it.startTime}-${it.endTime}-${it.isDayOff}" }
            .sortedWith(compareBy<ScheduleImportCandidate> { it.date }.thenBy { it.startTime })
            .toList()
    }

    private fun parseWeekList(rawText: String, anchorDate: LocalDate): List<ScheduleImportCandidate> {
        val lines = normalizedLines(rawText)
        val dateSlots = findDateSlots(lines, anchorDate)
        if (dateSlots.size < 3) return emptyList()

        val entryLines = lines
            .drop((dateSlots.maxOfOrNull { it.lineIndex } ?: 0) + 1)
            .filter { lineHasTime(it) || offRegex.containsMatchIn(it) }
        if (entryLines.size < 2) return emptyList()

        return entryLines
            .take(dateSlots.size)
            .mapIndexedNotNull { index, entry ->
                parseEntryForDate(dateSlots[index].date, entry, dateSlots[index].label)
            }
    }

    private fun findDateSlots(lines: List<String>, anchorDate: LocalDate): List<DateSlot> {
        val headerDate = lines.firstNotNullOfOrNull { line ->
            numericDateRegex.find(line)?.let { match ->
                parseNumericDate(match, anchorDate.year)
            }
        }
        val bareDays = lines.mapIndexedNotNull { index, line ->
            val dayName = dayRegex.find(line)?.value?.takeIf { line.trim().equals(it, ignoreCase = true) }
            dayName?.let { index to it }
        }

        var previousDate: LocalDate? = null
        return bareDays.mapNotNull { (index, dayName) ->
            val wanted = dayOfWeek(dayName) ?: return@mapNotNull null
            val resolved = if (headerDate != null) {
                val rawDelta = wanted.value - headerDate.dayOfWeek.value
                headerDate.plusDays((if (rawDelta < 0) rawDelta + 7 else rawDelta).toLong())
            } else {
                val explicitDayNumber = followingDayNumberBeforeNextHeader(lines, index)
                when {
                    explicitDayNumber != null -> resolveDayNumber(java.time.YearMonth.from(anchorDate), explicitDayNumber)
                    previousDate != null -> {
                        val rawDelta = wanted.value - previousDate!!.dayOfWeek.value
                        previousDate!!.plusDays((if (rawDelta <= 0) rawDelta + 7 else rawDelta).toLong())
                    }
                    else -> weekdayNearAnchor(wanted, anchorDate)
                }
            }
            previousDate = resolved
            DateSlot(resolved, index, dayName.take(3).replaceFirstChar { it.uppercase(Locale.US) })
        }.distinctBy { it.date }
    }

    private fun followingDayNumberBeforeNextHeader(lines: List<String>, dayIndex: Int): Int? {
        return ((dayIndex + 1)..(dayIndex + 4).coerceAtMost(lines.lastIndex))
            .asSequence()
            .takeWhile { !isBareDayHeader(lines[it]) }
            .mapNotNull { lines[it].trim().toIntOrNull() }
            .firstOrNull { it in 1..31 }
    }

    private fun weekdayNearAnchor(wanted: DayOfWeek, anchorDate: LocalDate): LocalDate {
        val monday = anchorDate.minusDays((anchorDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        return monday.plusDays((wanted.value - DayOfWeek.MONDAY.value).toLong())
    }

    private fun resolveDayNumber(yearMonth: java.time.YearMonth, day: Int): LocalDate {
        val maxDay = yearMonth.lengthOfMonth()
        return LocalDate.of(yearMonth.year, yearMonth.month, day.coerceIn(1, maxDay))
    }

    private fun parseNumericDate(match: MatchResult, fallbackYear: Int): LocalDate? {
        val month = match.groupValues[1].toIntOrNull() ?: return null
        val day = match.groupValues[2].toIntOrNull() ?: return null
        val rawYear = match.groupValues[3]
        val year = when {
            rawYear.length == 2 -> 2000 + rawYear.toInt()
            rawYear.length == 4 -> rawYear.toInt()
            else -> fallbackYear
        }
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun parseEntryForDate(date: LocalDate, line: String, dayLabel: String): ScheduleImportCandidate? {
        if (offRegex.containsMatchIn(line)) {
            return ScheduleImportCandidate(
                date = date,
                startTime = LocalTime.MIDNIGHT,
                endTime = LocalTime.MIDNIGHT,
                label = "Day off",
                expectedExhaustionLevel = "low",
                sourceLine = "$dayLabel ${date.dayOfMonth} $line",
                confidenceLabel = "medium",
                isDayOff = true
            )
        }

        return parseTimedEntry(date, "$dayLabel ${date.dayOfMonth} $line", line, hasExplicitDate = false)
    }

    private fun parseLine(line: String, anchorDate: LocalDate): ScheduleImportCandidate? {
        val date = parseDate(line, anchorDate) ?: return null
        if (offRegex.containsMatchIn(line)) {
            return ScheduleImportCandidate(
                date = date,
                startTime = LocalTime.MIDNIGHT,
                endTime = LocalTime.MIDNIGHT,
                label = "Day off",
                expectedExhaustionLevel = "low",
                sourceLine = line,
                confidenceLabel = if (numericDateRegex.containsMatchIn(line)) "high" else "medium",
                isDayOff = true
            )
        }

        return parseTimedEntry(date, line, line, hasExplicitDate = numericDateRegex.containsMatchIn(line))
    }

    private fun parseTimedEntry(
        date: LocalDate,
        sourceLine: String,
        timeLine: String,
        hasExplicitDate: Boolean
    ): ScheduleImportCandidate? {
        val range = timeRangeRegex.find(timeLine) ?: return null
        val endPeriod = range.groupValues[6].normalizePeriod()
        val startPeriod = range.groupValues[3].normalizePeriod()
            ?: inferStartPeriod(range.groupValues[1].toInt(), range.groupValues[4].toInt(), endPeriod)
        val start = parseTime(range.groupValues[1], range.groupValues[2], startPeriod)
        val end = parseTime(range.groupValues[4], range.groupValues[5], endPeriod ?: startPeriod)
        val adjustedEnd = if (end <= start) end.plusHours(12) else end
        val durationHours = Duration.between(start, adjustedEnd).toHours()
        val label = when {
            sourceLine.contains("open", ignoreCase = true) -> "Opening shift"
            sourceLine.contains("clos", ignoreCase = true) -> "Closing shift"
            sourceLine.contains("training", ignoreCase = true) -> "Training shift"
            else -> "Imported shift"
        }
        return ScheduleImportCandidate(
            date = date,
            startTime = start,
            endTime = adjustedEnd,
            label = label,
            expectedExhaustionLevel = if (durationHours >= 8) "high" else if (durationHours >= 6) "medium" else "low",
            sourceLine = sourceLine,
            confidenceLabel = if (hasExplicitDate && endPeriod != null) "high" else "medium"
        )
    }

    private fun expandedLines(rawText: String): List<String> {
        val lines = normalizedLines(rawText)
        val expanded = mutableListOf<String>()
        var lastDateHeader: String? = null
        lines.forEachIndexed { index, line ->
            expanded += line
            if (lineHasDate(line) && !isBareDayHeader(line)) lastDateHeader = line
            val next = lines.getOrNull(index + 1)
            if (lineHasDate(line) && next != null && !lineHasDate(next) && (lineHasTime(next) || offRegex.containsMatchIn(next))) {
                expanded += "$line $next"
            }
            if (!lineHasDate(line) && (lineHasTime(line) || offRegex.containsMatchIn(line))) {
                lastDateHeader?.let { expanded += "$it $line" }
            }
        }
        return expanded
    }

    private fun normalizedLines(rawText: String): List<String> {
        return rawText.lineSequence()
            .map(::normalizeLine)
            .filter { it.isNotBlank() }
            .filterNot(::isJunkLine)
            .toList()
    }

    private fun normalizeLine(line: String): String {
        return line
            .replace('\u2013', '-')
            .replace('\u2014', '-')
            .replace('\u00a0', ' ')
            .replace("a.m.", "AM", ignoreCase = true)
            .replace("p.m.", "PM", ignoreCase = true)
            .replace("a m", "AM", ignoreCase = true)
            .replace("p m", "PM", ignoreCase = true)
            .replace(Regex("""(?i)\b([ap])\s*\.?\s*m\.?\b""")) { "${it.groupValues[1].uppercase(Locale.US)}M" }
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun lineHasDate(line: String): Boolean = numericDateRegex.containsMatchIn(line) || dayRegex.containsMatchIn(line)

    private fun lineHasTime(line: String): Boolean = timeRangeRegex.containsMatchIn(line)

    private fun isBareDayHeader(line: String): Boolean = dayRegex.find(line)?.value?.let { line.trim().equals(it, ignoreCase = true) } == true

    private fun isJunkLine(line: String): Boolean {
        return line.equals("schedule", ignoreCase = true) ||
            line.equals("menu", ignoreCase = true) ||
            line.startsWith("net hours", ignoreCase = true) ||
            line.contains("publix.org", ignoreCase = true)
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
