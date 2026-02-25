/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui.colors

import androidx.compose.ui.graphics.Color

/**
 * A data class to hold a color's names and its corresponding Color object.
 * A color can have multiple names (aliases). The first name in the list is considered the primary name.
 */
data class NamedColor(val names: List<String>, val color: Color)

/**
 * A list of predefined named colors, interpolated to be between the Material 300 and 400 color palettes.
 * Includes aliases for some colors for reverse compatibility.
 */
val predefinedColors = listOf(
    // Rainbow Colors
    NamedColor(listOf("Red"), Color(0xFFEA6361)),
    NamedColor(listOf("Pink"), Color(0xFFEE5186)),
    NamedColor(listOf("Purple"), Color(0xFFB257C2)),
    NamedColor(listOf("Deep Purple", "violet"), Color(0xFF8966C7)),
    NamedColor(listOf("Indigo"), Color(0xFF6A78C5)),
    NamedColor(listOf("Blue"), Color(0xFF53ADF5)),
    NamedColor(listOf("Light Blue", "sky"), Color(0xFF3CBCF6)),
    NamedColor(listOf("Cyan"), Color(0xFF39CBDD)),
    NamedColor(listOf("Teal"), Color(0xFF39AEA3)),
    NamedColor(listOf("Green", "emerald"), Color(0xFF73C177)),
    NamedColor(listOf("Forest"), Color(0xFF246D29)),
    NamedColor(listOf("Light Green"), Color(0xFF8BC34A)),
    NamedColor(listOf("Lime"), Color(0xFFCDDC39)),
    NamedColor(listOf("Yellow"), Color(0xFFFFEB3B)),
    NamedColor(listOf("Amber"), Color(0xFFFFC107)),
    NamedColor(listOf("Orange"), Color(0xFFFFAF39)),
    NamedColor(listOf("Deep Orange"), Color(0xFFFF7D54)),
    NamedColor(listOf("Brown"), Color(0xFF977B71)),
    // Monochrome & Earthy Tones
    NamedColor(listOf("Grey"), Color(0xFF9E9E9E)),
    NamedColor(listOf("Beige"), Color(0xFFB0B09B)),
    NamedColor(listOf("Blue Grey"), Color(0xFF849AA5)),
    NamedColor(listOf("Slate"), Color(0xFF3E515A)),
    NamedColor(listOf("Charcoal"), Color(0xFF70767A))
)

/**
 * A simple list of the color values for easy access.
 */
val habitColors: List<Color> = predefinedColors.map { it.color }

