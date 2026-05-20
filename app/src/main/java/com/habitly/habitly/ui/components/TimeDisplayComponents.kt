/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitly.habitly.ui.components.TimeDisplayConstants.Base12hAmPmSize
import com.habitly.habitly.ui.components.TimeDisplayConstants.Base12hCropAmount
import com.habitly.habitly.ui.components.TimeDisplayConstants.Base12hHourSize
import com.habitly.habitly.ui.components.TimeDisplayConstants.Base12hMinuteSize
import com.habitly.habitly.ui.components.TimeDisplayConstants.Base24hCropAmount
import com.habitly.habitly.ui.components.TimeDisplayConstants.Base24hFontSize
import com.habitly.habitly.ui.components.TimeDisplayConstants.ColonOffsetYRatio
import com.habitly.habitly.ui.components.TimeDisplayConstants.ReferenceWidth
import com.habitly.habitly.ui.components.TimeDisplayConstants.StackOffsetYRatio

/**
 * A centered 24-hour time display that scales uniformly to fill the available width.
 */
@Composable
fun TimeDisplay24h(
    hour: Int,
    minute: Int,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val scale = maxWidth / ReferenceWidth
        val fontSize = (Base24hFontSize.value * scale).sp
        val colonOffset = (ColonOffsetYRatio * scale).dp
        val cropAmount = (Base24hCropAmount.value * scale).dp

        Row(
            modifier = Modifier.negativeVerticalPadding(cropAmount),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePart(hour, fontSize, textStyle)
            Text(
                text = ":",
                style = textStyle.copy(
                    fontSize = fontSize,
                    lineHeight = fontSize
                ),
                modifier = Modifier.offset(y = colonOffset),
                maxLines = 1,
                softWrap = false
            )
            TimePart(minute, fontSize, textStyle)
        }
    }
}

/**
 * A centered 12-hour time display with AM/PM indicator that scales uniformly to fill the available width.
 */
@Composable
fun TimeDisplay12h(
    hour: Int,
    minute: Int,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val scale = maxWidth / ReferenceWidth
        
        val hourSize = (Base12hHourSize.value * scale).sp
        val minuteSize = (Base12hMinuteSize.value * scale).sp
        val amPmSize = (Base12hAmPmSize.value * scale).sp
        val stackOffsetY = (StackOffsetYRatio * scale).dp
        val cropAmount = (Base12hCropAmount.value * scale).dp

        Row(
            modifier = Modifier.negativeVerticalPadding(cropAmount),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimePart(displayHour, hourSize, textStyle)
            TimeStack(
                minute = minute,
                amPm = amPm,
                minuteFontSize = minuteSize,
                amPmFontSize = amPmSize,
                style = textStyle,
                offsetY = stackOffsetY
            )
        }
    }
}

private fun Modifier.negativeVerticalPadding(padding: Dp): Modifier = this.layout { measurable, constraints ->
    val paddingPx = padding.roundToPx()
    val placeable = measurable.measure(constraints)
    val newHeight = (placeable.height - paddingPx * 2).coerceAtLeast(0)
    layout(
        width = placeable.width,
        height = newHeight
    ) {
        placeable.placeRelative(0, -paddingPx)
    }
}
