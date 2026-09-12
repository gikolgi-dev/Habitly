/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.colors

import androidx.compose.ui.graphics.Color

fun Color.isBright(): Boolean {
    val red = this.red * 255
    val green = this.green * 255
    val blue = this.blue * 255
    return (red * 0.299 + green * 0.587 + blue * 0.114) > 186
}

/**
 * Returns the theme-adapted version of this habit color.
 * In dark mode, returns the base color untouched ("dark modes perfect").
 * In light mode, returns the curated light-mode variant if it is a predefined color,
 * or an automatically contrast-adjusted shade if it's a bright custom color.
 */
fun Color.toThemeHabitColor(isDark: Boolean): Color {
    if (isDark) return this
    habitColorLightMap[this]?.let { return it }
    if (!this.isBright()) return this
    val factor = 0.72f
    return Color(
        red = (this.red * factor).coerceIn(0f, 1f),
        green = (this.green * factor).coerceIn(0f, 1f),
        blue = (this.blue * factor).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}

