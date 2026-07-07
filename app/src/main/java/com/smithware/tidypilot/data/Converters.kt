package com.smithware.tidypilot.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {
    @TypeConverter fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun fromDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
    @TypeConverter fun fromTime(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun toDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)
    @TypeConverter fun fromDateTime(value: LocalDateTime?): String? = value?.toString()
}
