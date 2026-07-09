package com.smithware.tidypilot.data

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleImportParserTest {
    @Test
    fun parsesDatedShiftWithAmPm() {
        val parsed = ScheduleImportParser.parse(
            rawText = "7/8 9:00 AM - 5:30 PM Work",
            anchorDate = LocalDate.of(2026, 7, 7)
        )

        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026, 7, 8), parsed.single().date)
        assertEquals(LocalTime.of(9, 0), parsed.single().startTime)
        assertEquals(LocalTime.of(17, 30), parsed.single().endTime)
        assertEquals("high", parsed.single().confidenceLabel)
    }

    @Test
    fun parsesWeekdayShiftNearCurrentWeek() {
        val parsed = ScheduleImportParser.parse(
            rawText = "Wed 12-8:30pm Closing",
            anchorDate = LocalDate.of(2026, 7, 7)
        )

        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026, 7, 8), parsed.single().date)
        assertEquals(LocalTime.of(12, 0), parsed.single().startTime)
        assertEquals(LocalTime.of(20, 30), parsed.single().endTime)
        assertEquals("Closing shift", parsed.single().label)
    }

    @Test
    fun parsesDaysOffAndUnclearLines() {
        val parsed = ScheduleImportParser.parse(
            rawText = """
                Monday OFF
                Random schedule header
                Friday 8am to 2pm
            """.trimIndent(),
            anchorDate = LocalDate.of(2026, 7, 7)
        )

        assertEquals(2, parsed.size)
        assertEquals(LocalDate.of(2026, 7, 6), parsed.first().date)
        assertEquals(true, parsed.first().isDayOff)
        assertEquals(LocalDate.of(2026, 7, 10), parsed.last().date)
        assertEquals(LocalTime.of(8, 0), parsed.last().startTime)
        assertEquals(LocalTime.of(14, 0), parsed.last().endTime)
    }

    @Test
    fun combinesDayHeaderWithFollowingTimeLine() {
        val parsed = ScheduleImportParser.parse(
            rawText = """
                Wed
                12 PM - 8:30 PM Closing
            """.trimIndent(),
            anchorDate = LocalDate.of(2026, 7, 7)
        )

        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026, 7, 8), parsed.single().date)
        assertEquals(LocalTime.of(12, 0), parsed.single().startTime)
        assertEquals(LocalTime.of(20, 30), parsed.single().endTime)
    }

    @Test
    fun pairsWeekListEntriesWithTheirActualDays() {
        val parsed = ScheduleImportParser.parse(
            rawText = """
                6:05 G O
                Net hours: 45.25
                Sat
                4
                Sun
                5
                Mon
                6
                Tue
                7
                Wed
                8
                Thu
                Fri
                2o publix.org/passpoi +
                10
                11 a.m.-7:30 p.m.
                Asst. Deli Manager
                Store #1640
                8 hours
                Not Scheduled
                6 a.m. - 3 p.m.
                Asst. Deli Manager
                Store #1640
                8.5 hours
                Not Scheduled
                6 a.m. - 5 p.m.
                Asst. Deli Manager
                Store #1640
                10.25 hours
                1 p.m. - 10:30 p.m.
                9 a.m. - 7 p.m.
            """.trimIndent(),
            anchorDate = LocalDate.of(2026, 7, 8)
        )

        assertEquals(7, parsed.size)
        assertEquals(LocalDate.of(2026, 7, 4), parsed[0].date)
        assertEquals(LocalTime.of(11, 0), parsed[0].startTime)
        assertEquals(LocalTime.of(19, 30), parsed[0].endTime)
        assertEquals(LocalDate.of(2026, 7, 5), parsed[1].date)
        assertEquals(true, parsed[1].isDayOff)
        assertEquals(LocalDate.of(2026, 7, 6), parsed[2].date)
        assertEquals(LocalTime.of(6, 0), parsed[2].startTime)
        assertEquals(LocalTime.of(15, 0), parsed[2].endTime)
        assertEquals(LocalDate.of(2026, 7, 7), parsed[3].date)
        assertEquals(true, parsed[3].isDayOff)
        assertEquals(LocalDate.of(2026, 7, 8), parsed[4].date)
        assertEquals(LocalTime.of(6, 0), parsed[4].startTime)
        assertEquals(LocalTime.of(17, 0), parsed[4].endTime)
        assertEquals(LocalDate.of(2026, 7, 9), parsed[5].date)
        assertEquals(LocalTime.of(13, 0), parsed[5].startTime)
        assertEquals(LocalDate.of(2026, 7, 10), parsed[6].date)
        assertEquals(LocalTime.of(9, 0), parsed[6].startTime)
    }

    @Test
    fun guidanceFlagsTimesWithoutDates() {
        val parsed = ScheduleImportParser.parse(
            rawText = "9:00 AM - 5:00 PM",
            anchorDate = LocalDate.of(2026, 7, 7)
        )

        val guidance = ScheduleImportGuidanceClassifier.fromText("9:00 AM - 5:00 PM", parsed)
        assertEquals(ScheduleImportIssue.TimesDetectedDatesMissing, guidance?.issue)
    }
}
