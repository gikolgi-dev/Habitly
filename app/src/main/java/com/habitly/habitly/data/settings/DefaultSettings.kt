/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.data.settings

// Make the reset to default button only affect and be visible in the appearence settings
object DefaultSettings {
    const val THEME = "system"
    const val USE_MATERIAL_THEMING = true
    const val MONTH_LABELS = false
    const val VIBRATIONS = true
    const val BORDERS = 0.0f
    const val HEATMAP_VISIBLE_DAYS = ""
    const val GLOBAL_NOTIFICATION_TIME = "09:00"
    const val GLOBAL_NOTIFICATION_DAYS = "MON,TUE,WED,THU,FRI,SAT,SUN"
    const val SKIP_COMPLETED_HABIT_NOTIFICATIONS = false
    const val SNOOZE_ENABLED = true
    const val SNOOZE_DURATION_MINUTES = 60
    const val IS_24_HOUR = false
    const val HERO_CARD_VISIBLE = true
    const val YEAR_DIVIDER = false
    const val YEAR_LABELS = false
    const val HEATMAP_NOTIFICATION_DOT = false
    const val HEATMAP_NOTIFICATION_DOT_RANGE = "today_and_future"
    const val HEATMAP_NOTIFICATION_DOT_DETAIL_ONLY = false
    const val HEATMAP_SCROLLING = false
    const val SHOW_SCROLL_BLUR = true
    const val SCROLL_BLUR_TARGETS = "Line Chart"
    const val DISABLE_ANIMATIONS = false
    const val REDUCE_MOVEMENT = false
    const val REDUCE_MOVEMENT_TARGETS = "Rotation,Grid Reactions"
    const val USE_HABIT_COLOR_FOR_CARD = false
    const val HABIT_COLOR_TARGETS = "Habit Cards,Statistic Screen"
    const val HEATMAP_WEEKS = 20
    const val HEATMAP_INFINITE = false
    const val HAS_ASKED_NOTIFICATION_PERMISSION = false
}

