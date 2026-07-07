package com.smithware.tidypilot.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("tidypilot_preferences")

class PreferencesStore(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val remindersKey = booleanPreferencesKey("reminders_enabled")
    private val savePhotosKey = booleanPreferencesKey("save_photos_locally")

    val themeMode: Flow<String> = context.dataStore.data.map { it[themeModeKey] ?: "system" }
    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[remindersKey] ?: true }
    val savePhotosLocally: Flow<Boolean> = context.dataStore.data.map { it[savePhotosKey] ?: true }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[remindersKey] = enabled }
    }

    suspend fun setSavePhotosLocally(enabled: Boolean) {
        context.dataStore.edit { it[savePhotosKey] = enabled }
    }
}
