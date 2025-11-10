package com.example.attempt3

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val MONTH_LABELS_KEY = booleanPreferencesKey("month_labels")
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
}
