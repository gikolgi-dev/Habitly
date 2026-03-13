/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui.components

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Constants used for the uniform scaling of time display components.
 * These values are calibrated against a reference width of 320dp.
 */
object TimeDisplayConstants {
    val ReferenceWidth = 320.dp
    
    // 24h Display Constants
    // Reduced from 150.sp to 110.sp to fit "HH:mm" within the reference width
    val Base24hFontSize = 110.sp
    const val ColonOffsetYRatio = -8f
    
    // 12h Display Constants
    val Base12hHourSize = 180.sp
    val Base12hMinuteSize = 90.sp
    val Base12hAmPmSize = 60.sp
    const val StackOffsetYRatio = -16f
}
