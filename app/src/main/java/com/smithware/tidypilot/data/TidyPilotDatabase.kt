package com.smithware.tidypilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TidyPilotDatabase : RoomDatabase() {
    abstract fun dao(): TidyPilotDao

    companion object {
        @Volatile private var instance: TidyPilotDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rooms ADD COLUMN defaultTaskFrequency TEXT NOT NULL DEFAULT 'weekly'")
                db.execSQL("ALTER TABLE rooms ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cleaning_tasks ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'easy'")
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN quietHoursStart TEXT NOT NULL DEFAULT '21:00'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN quietHoursEnd TEXT NOT NULL DEFAULT '08:00'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN lowEnergyReminderMode TEXT NOT NULL DEFAULT 'gentle'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN workdayReminderBehavior TEXT NOT NULL DEFAULT 'after shift'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN dayOffReminderBehavior TEXT NOT NULL DEFAULT 'morning reset'")
            }
        }

        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN accentStyle TEXT NOT NULL DEFAULT 'warm orange'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN defaultEnergyLevel TEXT NOT NULL DEFAULT 'medium'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN defaultTaskDurationMinutes INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN workdayPlanningBehavior TEXT NOT NULL DEFAULT 'keep it light'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN dayOffPlanningBehavior TEXT NOT NULL DEFAULT 'allow bigger resets'")
            }
        }

        fun get(context: Context): TidyPilotDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TidyPilotDatabase::class.java,
                    "tidypilot.db"
                ).addMigrations(migration1To2, migration2To3, migration3To4, migration4To5).build().also { instance = it }
            }
    }
}
