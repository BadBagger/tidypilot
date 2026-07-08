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
    fun ignoresDaysOffAndUnclearLines() {
        val parsed = ScheduleImportParser.parse(
            rawText = """
                Monday OFF
                Random schedule header
                Friday 8am to 2pm
            """.trimIndent(),
            anchorDate = LocalDate.of(2026, 7, 7)
        )

        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026, 7, 10), parsed.single().date)
        assertEquals(LocalTime.of(8, 0), parsed.single().startTime)
        assertEquals(LocalTime.of(14, 0), parsed.single().endTime)
    }
}
