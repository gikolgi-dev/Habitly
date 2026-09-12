/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.colors

import androidx.compose.ui.graphics.Color

/**
 * A data class to hold a color's names and its corresponding Color object.
 * A color can have multiple names (aliases). The first name in the list is considered the primary name.
 */
data class NamedColor(
    val names: List<String>,
    val color: Color,
    val lightColor: Color = color
)

/**
 * A list of predefined named colors.
 * In dark mode, interpolated between Material 300 and 400 for vibrant contrast on dark backgrounds.
 * In light mode, uses deeper, richer tones (Material 600-800) to ensure strong contrast against bright backgrounds.
 * Includes aliases for some colors for reverse compatibility.
 */
val predefinedColors = listOf(
    // Rainbow Colors
    NamedColor(listOf("Red"), Color(0xFFEA6361), Color(0xFFD32F2F)),
    NamedColor(listOf("Pink"), Color(0xFFEE5186), Color(0xFFC2185B)),
    NamedColor(listOf("Purple"), Color(0xFFB257C2), Color(0xFF7B1FA2)),
    NamedColor(listOf("Deep Purple", "violet"), Color(0xFF8966C7), Color(0xFF512DA8)),
    NamedColor(listOf("Indigo"), Color(0xFF6A78C5), Color(0xFF303F9F)),
    NamedColor(listOf("Blue"), Color(0xFF53ADF5), Color(0xFF1976D2)),
    NamedColor(listOf("Light Blue", "sky"), Color(0xFF3CBCF6), Color(0xFF0288D1)),
    NamedColor(listOf("Cyan"), Color(0xFF39CBDD), Color(0xFF0097A7)),
    NamedColor(listOf("Teal"), Color(0xFF39AEA3), Color(0xFF00796B)),
    NamedColor(listOf("Green", "emerald"), Color(0xFF73C177), Color(0xFF388E3C)),
    NamedColor(listOf("Forest"), Color(0xFF246D29), Color(0xFF246D29)),
    NamedColor(listOf("Light Green"), Color(0xFF8BC34A), Color(0xFF558B2F)),
    NamedColor(listOf("Lime"), Color(0xFFCDDC39), Color(0xFF827717)),
    NamedColor(listOf("Yellow"), Color(0xFFFFEB3B), Color(0xFFC47700)),
    NamedColor(listOf("Amber"), Color(0xFFFFC107), Color(0xFFD97706)),
    NamedColor(listOf("Orange"), Color(0xFFFFAF39), Color(0xFFE65100)),
    NamedColor(listOf("Deep Orange"), Color(0xFFFF7D54), Color(0xFFD84315)),
    NamedColor(listOf("Brown"), Color(0xFF977B71), Color(0xFF6D4C41)),
    // Monochrome & Earthy Tones
    NamedColor(listOf("Grey"), Color(0xFF9E9E9E), Color(0xFF616161)),
    NamedColor(listOf("Beige"), Color(0xFFB0B09B), Color(0xFF757563)),
    NamedColor(listOf("Blue Grey"), Color(0xFF849AA5), Color(0xFF455A64)),
    NamedColor(listOf("Slate"), Color(0xFF3E515A), Color(0xFF3E515A)),
    NamedColor(listOf("Charcoal"), Color(0xFF70767A), Color(0xFF45494C))
)

/**
 * A simple list of the color values for easy access.
 */
val habitColors: List<Color> = predefinedColors.map { it.color }

/**
 * Fast lookup map from canonical/dark color to light mode color.
 */
val habitColorLightMap: Map<Color, Color> = predefinedColors.associate { it.color to it.lightColor }


