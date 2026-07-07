package com.smithware.tidypilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CleaningTaskEntity::class,
        RoomEntity::class,
        WorkShiftEntity::class,
        EnergyCheckInEntity::class,
        DailyCleaningPlanEntity::class,
        TaskCompletionEntity::class,
        RoomPhotoScanEntity::class,
        ScanIssueEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TidyPilotDatabase : RoomDatabase() {
    abstract fun dao(): TidyPilotDao

    companion object {
        @Volatile private var instance: TidyPilotDatabase? = null

        fun get(context: Context): TidyPilotDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TidyPilotDatabase::class.java,
                    "tidypilot.db"
                ).build().also { instance = it }
            }
    }
}
