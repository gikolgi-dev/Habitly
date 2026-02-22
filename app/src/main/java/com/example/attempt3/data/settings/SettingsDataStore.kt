package com.example.attempt3.data.settings

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
        val DAY_OF_WEEK_LABELS_VISIBLE_KEY = booleanPreferencesKey("day_of_week_labels_visible")
        val DAY_OF_WEEK_LABELS_ON_RIGHT_KEY = booleanPreferencesKey("day_of_week_labels_on_right")
        val SHOW_ALL_DAY_OF_WEEK_LABELS_KEY = booleanPreferencesKey("show_all_day_of_week_labels")
        val HEATMAP_VISIBLE_DAYS_KEY = stringPreferencesKey("heatmap_visible_days")
        val GLOBAL_NOTIFICATIONS_KEY = booleanPreferencesKey("global_notifications")
        val GLOBAL_NOTIFICATION_TIME_KEY = stringPreferencesKey("global_notification_time")
        val GLOBAL_NOTIFICATION_DAYS_KEY = stringPreferencesKey("global_notification_days")
        val IS_24_HOUR_KEY = booleanPreferencesKey("is_24_hour")
        val HERO_CARD_VISIBLE_KEY = booleanPreferencesKey("hero_card_visible")
        val YEAR_DIVIDER_KEY = booleanPreferencesKey("year_divider")
        val YEAR_LABELS_KEY = booleanPreferencesKey("year_labels")
        val HEATMAP_SCROLLING_KEY = booleanPreferencesKey("heatmap_scrolling")
        val SHOW_SCROLL_BLUR_KEY = booleanPreferencesKey("show_scroll_blur")
        val SCROLL_BLUR_TARGETS_KEY = stringPreferencesKey("scroll_blur_targets")
        val DISABLE_ANIMATIONS_KEY = booleanPreferencesKey("disable_animations")
        val USE_HABIT_COLOR_FOR_CARD_KEY = booleanPreferencesKey("use_habit_color_for_card")
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

    val dayOfWeekLabelsVisible: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DAY_OF_WEEK_LABELS_VISIBLE_KEY] ?: true
        }

    suspend fun setDayOfWeekLabelsVisible(visible: Boolean) {
        context.dataStore.edit { settings ->
            settings[DAY_OF_WEEK_LABELS_VISIBLE_KEY] = visible
        }
    }

    val dayOfWeekLabelsOnRight: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DAY_OF_WEEK_LABELS_ON_RIGHT_KEY] ?: false // Default to left
        }

    val showAllDayOfWeekLabels: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_ALL_DAY_OF_WEEK_LABELS_KEY] ?: true // Default to all
        }

    suspend fun setShowAllDayOfWeekLabels(showAll: Boolean) {
        context.dataStore.edit { settings ->
            settings[SHOW_ALL_DAY_OF_WEEK_LABELS_KEY] = showAll
        }
    }
    
    val heatmapVisibleDays: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val saved = preferences[HEATMAP_VISIBLE_DAYS_KEY]
            if (saved != null) {
                saved.split(',').filter { it.isNotEmpty() }.toSet()
            } else {
                // Migration logic from old boolean settings
                val visible = preferences[DAY_OF_WEEK_LABELS_VISIBLE_KEY] ?: true
                val all = preferences[SHOW_ALL_DAY_OF_WEEK_LABELS_KEY] ?: true
                if (!visible) emptySet()
                else if (all) setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                else setOf("TUE", "THU", "SAT")
            }
        }

    suspend fun setHeatmapVisibleDays(days: Set<String>) {
        context.dataStore.edit { settings ->
            settings[HEATMAP_VISIBLE_DAYS_KEY] = days.joinToString(",")
        }
    }

    val globalNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[GLOBAL_NOTIFICATIONS_KEY] ?: false
        }
    
    suspend fun setGlobalNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[GLOBAL_NOTIFICATIONS_KEY] = enabled
        }
    }
    
    val globalNotificationTime: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[GLOBAL_NOTIFICATION_TIME_KEY] ?: "09:00"
        }
    
    suspend fun setGlobalNotificationTime(time: String) {
        context.dataStore.edit { settings ->
            settings[GLOBAL_NOTIFICATION_TIME_KEY] = time
        }
    }

    val globalNotificationDays: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[GLOBAL_NOTIFICATION_DAYS_KEY]?.split(',')?.toSet() ?: setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        }

    suspend fun setGlobalNotificationDays(days: Set<String>) {
        context.dataStore.edit { settings ->
            settings[GLOBAL_NOTIFICATION_DAYS_KEY] = days.joinToString(",")
        }
    }

    val is24Hour: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_24_HOUR_KEY] ?: false
        }

    suspend fun setIs24Hour(is24Hour: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_24_HOUR_KEY] = is24Hour
        }
    }

    val heroCardVisible: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HERO_CARD_VISIBLE_KEY] ?: true
        }

    suspend fun setHeroCardVisible(visible: Boolean) {
        context.dataStore.edit { settings ->
            settings[HERO_CARD_VISIBLE_KEY] = visible
        }
    }

    val yearDivider: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[YEAR_DIVIDER_KEY] ?: true
        }

    suspend fun setYearDivider(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[YEAR_DIVIDER_KEY] = show
        }
    }

    val yearLabels: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[YEAR_LABELS_KEY] ?: true
        }

    suspend fun setYearLabels(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[YEAR_LABELS_KEY] = show
        }
    }

    val heatmapScrolling: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HEATMAP_SCROLLING_KEY] ?: false
        }

    suspend fun setHeatmapScrolling(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[HEATMAP_SCROLLING_KEY] = enabled
        }
    }

    val showScrollBlur: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_SCROLL_BLUR_KEY] ?: true
        }

    suspend fun setshowScrollBlur(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[SHOW_SCROLL_BLUR_KEY] = show
        }
    }

    val scrollBlurTargets: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SCROLL_BLUR_TARGETS_KEY]?.split(',')?.filter { it.isNotEmpty() }?.toSet() 
                ?: setOf("Heatmap", "Line Chart")
        }

    suspend fun setScrollBlurTargets(targets: Set<String>) {
        context.dataStore.edit { settings ->
            settings[SCROLL_BLUR_TARGETS_KEY] = targets.joinToString(",")
        }
    }

    val disableAnimations: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DISABLE_ANIMATIONS_KEY] ?: false
        }

    suspend fun setDisableAnimations(disable: Boolean) {
        context.dataStore.edit { settings ->
            settings[DISABLE_ANIMATIONS_KEY] = disable
        }
    }

    val useHabitColorForCard: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_HABIT_COLOR_FOR_CARD_KEY] ?: true
        }

    suspend fun setUseHabitColorForCard(use: Boolean) {
        context.dataStore.edit { settings ->
            settings[USE_HABIT_COLOR_FOR_CARD_KEY] = use
        }
    }

    suspend fun resetToDefault() {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = "system"
            settings[MONTH_LABELS_KEY] = false
            settings[DAY_OF_WEEK_LABELS_VISIBLE_KEY] = false
            settings[BORDERS_KEY] = 0f
            settings[GLOBAL_NOTIFICATIONS_KEY] = false
            settings[GLOBAL_NOTIFICATION_TIME_KEY] = "09:00"
            settings[GLOBAL_NOTIFICATION_DAYS_KEY] = "MON,TUE,WED,THU,FRI,SAT,SUN"
            settings[IS_24_HOUR_KEY] = false
            settings[HERO_CARD_VISIBLE_KEY] = true
            settings[YEAR_DIVIDER_KEY] = false
            settings[YEAR_LABELS_KEY] = false
            settings[HEATMAP_SCROLLING_KEY] = false
            settings[SHOW_SCROLL_BLUR_KEY] = true
            settings[SCROLL_BLUR_TARGETS_KEY] = "Line Chart"
            settings[HEATMAP_VISIBLE_DAYS_KEY] = "MON,TUE,WED,THU,FRI,SAT,SUN"
            settings[DISABLE_ANIMATIONS_KEY] = false
            settings[USE_HABIT_COLOR_FOR_CARD_KEY] = true
        }
    }
}