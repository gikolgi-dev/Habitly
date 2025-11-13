package com.example.attempt3

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val MONTH_LABELS_KEY = booleanPreferencesKey("month_labels")
        val VIBRATIONS_KEY = booleanPreferencesKey("vibrations")
        val BORDERS_KEY = floatPreferencesKey("borders_float") // Renamed key
        val OLD_BORDERS_KEY = booleanPreferencesKey("borders") // Old key
    }

    val theme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = theme
        }
    }

    val monthLabels: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[MONTH_LABELS_KEY] ?: true
        }

    suspend fun setMonthLabels(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[MONTH_LABELS_KEY] = show
        }
    }

    val vibrations: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATIONS_KEY] ?: true
        }

    suspend fun setVibrations(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[VIBRATIONS_KEY] = enabled
        }
    }
    val borders: Flow<Float> = context.dataStore.data
        .map { preferences ->
            val floatValue = preferences[BORDERS_KEY]
            if (floatValue != null) {
                floatValue
            } else {
                val oldValue = preferences[OLD_BORDERS_KEY]
                val newValue = if (oldValue == true) 1.0f else if (oldValue == false) 0.0f else 0.25f
                
                // Migrate to the new key and remove the old one
                context.dataStore.edit {
                    it[BORDERS_KEY] = newValue
                    it.remove(OLD_BORDERS_KEY)
                }
                newValue
            }
        }

    suspend fun setBorders(alpha: Float) {
        context.dataStore.edit { settings ->
            settings[BORDERS_KEY] = alpha
        }
    }
}
