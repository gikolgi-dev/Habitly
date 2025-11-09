package com.example.attempt3

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * A data class to hold a color's names and its corresponding Color object.
 * A color can have multiple names (aliases). The first name in the list is considered the primary name.
 */
data class NamedColor(val names: List<String>, val color: Color) {
    val primaryName: String
        get() = names.first()
}

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
    NamedColor(listOf("Light Green"), Color(0xFFA5D073)),
    NamedColor(listOf("Lime"), Color(0xFFD8E466)),
    NamedColor(listOf("Yellow"), Color(0xFFFFEF67)),
    NamedColor(listOf("Amber"), Color(0xFFFFCF3B)),
    NamedColor(listOf("Orange"), Color(0xFFFFAF39)),
    NamedColor(listOf("Deep Orange"), Color(0xFFFF7D54)),
    NamedColor(listOf("Brown"), Color(0xFF977B71)),
    // Monochrome & Earthy Tones
    NamedColor(listOf("Beige"), Color(0xFFF5F5DC)),
    NamedColor(listOf("Silver"), Color(0xFFE7E7E7)),
    NamedColor(listOf("Blue Grey"), Color(0xFF849AA5)),
    NamedColor(listOf("Slate"), Color(0xFF3E515A)),
    NamedColor(listOf("Charcoal"), Color(0xFF70767A))
)

/**
 * A simple list of the color values for easy access.
 */
val habitColors: List<Color> = predefinedColors.map { it.color }

/**
 * Converts a color name (or its alias) to its corresponding hex string representation.
 * This function is case-insensitive.
 * @param name The name or alias of the color.
 * @return The hex string (e.g., "#EA6361") or null if the name is not found.
 */
fun nameToHex(name: String): String? {
    return predefinedColors.find { namedColor -> namedColor.names.any { it.equals(name, ignoreCase = true) } }?.color?.let {
        "#${it.toArgb().toUInt().toString(16).substring(2).uppercase()}"
    }
}

/**
 * Converts a hex string to its corresponding primary color name.
 * @param hex The hex string representation of the color (e.g., "#EA6361").
 * @return The primary name of the color or null if the hex value is not found in the predefined list.
 */
fun hexToName(hex: String): String? {
    try {
        val colorInt = android.graphics.Color.parseColor(hex)
        return predefinedColors.find { it.color.toArgb() == colorInt }?.primaryName
    } catch (e: IllegalArgumentException) {
        return null // Invalid hex color format
    }
}
