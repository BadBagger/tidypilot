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
        CleaningSupplyEntity::class,
        TaskSupplyEntity::class,
        SupplyExpenseEntity::class,
        RoomEntity::class,
        WorkShiftEntity::class,
        EnergyCheckInEntity::class,
        DailyCleaningPlanEntity::class,
        TaskCompletionEntity::class,
        RoomPhotoScanEntity::class,
        ScanIssueEntity::class,
        AppSettingsEntity::class
    ],
    version = 10,
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

        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE room_photo_scans ADD COLUMN messLevel TEXT NOT NULL DEFAULT 'quick_reset'")
                db.execSQL("ALTER TABLE room_photo_scans ADD COLUMN confidence TEXT NOT NULL DEFAULT 'medium'")
                db.execSQL("ALTER TABLE room_photo_scans ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE room_photo_scans ADD COLUMN detectedZones TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE room_photo_scans ADD COLUMN reviewed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN roomId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN category TEXT NOT NULL DEFAULT 'general'")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'easy'")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN status TEXT NOT NULL DEFAULT 'suggested'")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN createdTaskId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE scan_issues ADD COLUMN createdAt TEXT NOT NULL DEFAULT '1970-01-01T00:00:00'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN saveProcessedScanImages INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN requireScanReview INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN defaultScanConfidenceThreshold TEXT NOT NULL DEFAULT 'medium'")
            }
        }

        private val migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cleaning_tasks ADD COLUMN assignedTo TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cleaning_tasks ADD COLUMN householdId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cleaning_tasks ADD COLUMN createdBy TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE task_completions ADD COLUMN householdId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE task_completions ADD COLUMN completedBy TEXT DEFAULT NULL")
            }
        }

        private val migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN quietDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN maxRemindersPerDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN reminderTone TEXT NOT NULL DEFAULT 'Gentle'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN enabledReminderTypes TEXT NOT NULL DEFAULT 'daily|task|room|weekly|seasonal|quick_win'")
            }
        }

        private val migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cleaning_supplies (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'general',
                        estimatedCostCents INTEGER NOT NULL DEFAULT 0,
                        isRunningLow INTEGER NOT NULL DEFAULT 0,
                        isOnShoppingList INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_supplies (
                        taskId TEXT NOT NULL,
                        supplyId TEXT NOT NULL,
                        PRIMARY KEY(taskId, supplyId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS supply_expenses (
                        id TEXT NOT NULL PRIMARY KEY,
                        supplyId TEXT,
                        name TEXT NOT NULL,
                        costCents INTEGER NOT NULL,
                        purchasedAt TEXT NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE app_settings ADD COLUMN supplyTrackingEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN monthlyCleaningBudgetCents INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN premiumEntitlement TEXT NOT NULL DEFAULT 'free'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN premiumPlan TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN premiumExpiresAt TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): TidyPilotDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TidyPilotDatabase::class.java,
                    "tidypilot.db"
                ).addMigrations(migration1To2, migration2To3, migration3To4, migration4To5, migration5To6, migration6To7, migration7To8, migration8To9, migration9To10).build().also { instance = it }
            }
    }
}
