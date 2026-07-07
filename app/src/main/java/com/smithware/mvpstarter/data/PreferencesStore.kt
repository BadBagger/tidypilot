package com.smithware.mvpstarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("mvpstarter_preferences")

class PreferencesStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val compactCardsKey = booleanPreferencesKey("compact_cards")

    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[darkModeKey] ?: false }
    val compactCards: Flow<Boolean> = context.dataStore.data.map { it[compactCardsKey] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setCompactCards(enabled: Boolean) {
        context.dataStore.edit { it[compactCardsKey] = enabled }
    }
}
