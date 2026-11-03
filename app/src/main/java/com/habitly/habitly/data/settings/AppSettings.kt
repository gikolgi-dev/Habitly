/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Calendar

@Immutable
data class AppSettings(
    // General
    val vibrations: Boolean = DefaultSettings.VIBRATIONS,
    val heroCardVisible: Boolean = DefaultSettings.HERO_CARD_VISIBLE,
    val ignoreWelcomeEngine: Boolean = DefaultSettings.IGNORE_WELCOME_ENGINE,
    val heatmapWeeks: Int = DefaultSettings.HEATMAP_WEEKS,
    val heatmapInfinite: Boolean = DefaultSettings.HEATMAP_INFINITE,
    val is24Hour: Boolean = DefaultSettings.IS_24_HOUR,
    val firstDayOfWeek: String = DefaultSettings.FIRST_DAY_OF_WEEK,

    // Appearance & Heatmap
    val theme: String = DefaultSettings.THEME,
    val useMaterialTheming: Boolean = DefaultSettings.USE_MATERIAL_THEMING,
    val useHabitColorForCard: Boolean = DefaultSettings.USE_HABIT_COLOR_FOR_CARD,
    val habitColorTargets: Set<String> = DefaultSettings.HABIT_COLOR_TARGETS.split(',').filter { it.isNotEmpty() }.toSet(),
    val lineChartYearDivider: Boolean = DefaultSettings.LINE_CHART_YEAR_DIVIDER,
    val monthLabels: Boolean = DefaultSettings.MONTH_LABELS,
    val yearDivider: Boolean = DefaultSettings.YEAR_DIVIDER,
    val yearLabels: Boolean = DefaultSettings.YEAR_LABELS,
    val heatmapNotificationDot: Boolean = DefaultSettings.HEATMAP_NOTIFICATION_DOT,
    val heatmapNotificationDotRange: String = DefaultSettings.HEATMAP_NOTIFICATION_DOT_RANGE,
    val heatmapNotificationDotDetailOnly: Boolean = DefaultSettings.HEATMAP_NOTIFICATION_DOT_DETAIL_ONLY,
    val heatmapScrolling: Boolean = DefaultSettings.HEATMAP_SCROLLING,
    val heatmapVisibleDays: Set<String> = DefaultSettings.HEATMAP_VISIBLE_DAYS.split(',').filter { it.isNotEmpty() }.toSet(),
    val dayOfWeekLabelsOnRight: Boolean = DefaultSettings.DAY_OF_WEEK_LABELS_ON_RIGHT,

    // Accessibility
    val borders: Float = DefaultSettings.BORDERS,
    val showScrollBlur: Boolean = DefaultSettings.SHOW_SCROLL_BLUR,
    val scrollBlurTargets: Set<String> = DefaultSettings.SCROLL_BLUR_TARGETS.split(',').filter { it.isNotEmpty() }.toSet(),
    val scrollBlurDetailOnly: Boolean = DefaultSettings.SCROLL_BLUR_DETAIL_ONLY,
    val reduceMovement: Boolean = DefaultSettings.REDUCE_MOVEMENT,
    val reduceMovementTargets: Set<String> = DefaultSettings.REDUCE_MOVEMENT_TARGETS.split(',').filter { it.isNotEmpty() }.toSet(),
    val autoScrollText: Boolean = DefaultSettings.AUTO_SCROLL_TEXT,
    val autoScrollTextElements: Set<String> = DefaultSettings.AUTO_SCROLL_TEXT_ELEMENTS.split(',').filter { it.isNotEmpty() }.toSet(),
    val autoScrollTextScreens: Set<String> = DefaultSettings.AUTO_SCROLL_TEXT_SCREENS.split(',').filter { it.isNotEmpty() }.toSet(),

    // Notifications & Welcome Card
    val globalNotificationsEnabled: Boolean = DefaultSettings.GLOBAL_NOTIFICATIONS_ENABLED,
    val globalNotificationTime: String = DefaultSettings.GLOBAL_NOTIFICATION_TIME,
    val globalNotificationDays: Set<String> = DefaultSettings.GLOBAL_NOTIFICATION_DAYS.split(',').filter { it.isNotEmpty() }.toSet(),
    val skipCompletedHabitNotifications: Boolean = DefaultSettings.SKIP_COMPLETED_HABIT_NOTIFICATIONS,
    val exactAlarms: Boolean = DefaultSettings.EXACT_ALARMS,
    val snoozeEnabled: Boolean = DefaultSettings.SNOOZE_ENABLED,
    val snoozeDurationMinutes: Int = DefaultSettings.SNOOZE_DURATION_MINUTES,
    val hasAskedNotificationPermission: Boolean = DefaultSettings.HAS_ASKED_NOTIFICATION_PERMISSION,
    val welcomeCardNotificationEnabled: Boolean = DefaultSettings.WELCOME_CARD_NOTIFICATION_ENABLED,
    val welcomeCardNotificationTime: String = DefaultSettings.WELCOME_CARD_NOTIFICATION_TIME,
    val welcomeCardNotificationDays: Set<String> = DefaultSettings.WELCOME_CARD_NOTIFICATION_DAYS.split(',').filter { it.isNotBlank() }.toSet(),
    val welcomeCardDate: String = "",
    val welcomeCardCategoryId: String = "",
    val welcomeCardHabitId: String = "",
    val welcomeCardTemplateIndex: Int = 0
) {
    // Computed convenience properties used across UI
    val firstDayOfWeekCalendar: Int
        get() = if (firstDayOfWeek.lowercase() == "sunday") Calendar.SUNDAY else Calendar.MONDAY

    val disableRotationAnimations: Boolean
        get() = reduceMovement && "Rotation" in reduceMovementTargets

    val useHabitColorForItemCards: Boolean
        get() = useHabitColorForCard && "Habit Cards" in habitColorTargets

    val useHabitColorForStatistics: Boolean
        get() = useHabitColorForCard && "Statistic Screen" in habitColorTargets
}

val LocalAppSettings = compositionLocalOf { AppSettings() }

val LocalSettingsDataStore = staticCompositionLocalOf<SettingsDataStore> {
    error("SettingsDataStore not provided")
}
