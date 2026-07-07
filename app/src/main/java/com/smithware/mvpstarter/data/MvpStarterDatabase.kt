package com.smithware.mvpstarter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ProjectEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MvpStarterDatabase : RoomDatabase() {
    abstract fun dao(): MvpStarterDao

    companion object {
        @Volatile private var instance: MvpStarterDatabase? = null

        fun get(context: Context): MvpStarterDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MvpStarterDatabase::class.java,
                    "mvpstarter.db"
                ).build().also { instance = it }
            }
    }
}
