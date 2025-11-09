package com.example.attempt3

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    // Define a key for a boolean preference. This is how you'll access the value.
    companion object {
        val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    }

    // A Flow representing the current value of the dark mode preference.
    // It will emit the latest value whenever it changes.
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            // If the key is not present, return a default value (e.g., false).
            preferences[IS_DARK_MODE_KEY] ?: false
        }

    // A function to update the dark mode setting.
    // 'edit' is a transactional function that ensures data consistency.
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_DARK_MODE_KEY] = isDarkMode
        }
    }
}
