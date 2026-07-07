package com.smithware.mvpstarter.data

import androidx.room.TypeConverter
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromText(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun toText(value: LocalDateTime?): String? = value?.toString()
}
