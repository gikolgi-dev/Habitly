/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data.settings

object DefaultSettings {
    // General
    const val VIBRATIONS = true
    const val HERO_CARD_VISIBLE = true
    const val IGNORE_WELCOME_ENGINE = true
    const val HEATMAP_WEEKS = 100
    const val HEATMAP_INFINITE = false
    const val IS_24_HOUR = true
    const val FIRST_DAY_OF_WEEK = "monday"

    // Appearance & Heatmap
    const val THEME = "system"
    const val USE_MATERIAL_THEMING = true
    const val USE_HABIT_COLOR_FOR_CARD = false
    const val HABIT_COLOR_TARGETS = "Habit Cards"
    const val LINE_CHART_YEAR_DIVIDER = false
    const val MONTH_LABELS = true
    const val YEAR_DIVIDER = false
    const val YEAR_LABELS = true
    const val HEATMAP_NOTIFICATION_DOT = false
    const val HEATMAP_NOTIFICATION_DOT_RANGE = "today_and_future"
    const val HEATMAP_NOTIFICATION_DOT_DETAIL_ONLY = false
    const val HEATMAP_SCROLLING = false
    const val HEATMAP_VISIBLE_DAYS = ""
    const val DAY_OF_WEEK_LABELS_VISIBLE = false
    const val DAY_OF_WEEK_LABELS_ON_RIGHT = false
    const val SHOW_ALL_DAY_OF_WEEK_LABELS = true

    // Accessibility
    const val BORDERS = 0.05f
    const val SHOW_SCROLL_BLUR = true
    const val SCROLL_BLUR_TARGETS = "Line Chart,Heatmap"
    const val DISABLE_ANIMATIONS = false
    const val REDUCE_MOVEMENT = false
    const val REDUCE_MOVEMENT_TARGETS = "Rotation,Grid Reactions"
    const val AUTO_SCROLL_TEXT = true
    const val AUTO_SCROLL_TEXT_ELEMENTS = "Title,Description"
    const val AUTO_SCROLL_TEXT_SCREENS = "Main Screen,Detail Screen"

    // Notifications
    const val GLOBAL_NOTIFICATIONS_ENABLED = false
    const val GLOBAL_NOTIFICATION_TIME = "09:00"
    const val GLOBAL_NOTIFICATION_DAYS = "MON,TUE,WED,THU,FRI,SAT,SUN"
    const val SKIP_COMPLETED_HABIT_NOTIFICATIONS = true
    const val EXACT_ALARMS = true
    const val SNOOZE_ENABLED = true
    const val SNOOZE_DURATION_MINUTES = 60
    const val HAS_ASKED_NOTIFICATION_PERMISSION = false
    const val WELCOME_CARD_NOTIFICATION_ENABLED = false
    const val WELCOME_CARD_NOTIFICATION_TIME = "09:00"
    const val WELCOME_CARD_NOTIFICATION_DAYS = "MON,TUE,WED,THU,FRI,SAT,SUN"
}
