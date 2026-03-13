/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A basic component to display a two-digit time unit (hour or minute).
 */
@Composable
fun TimePart(
    value: Int,
    fontSize: TextUnit,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Text(
        text = String.format(LocalLocale.current.platformLocale, "%02d", value),
        style = style.copy(
            fontSize = fontSize,
            lineHeight = fontSize
        ),
        modifier = modifier,
        maxLines = 1,
        softWrap = false
    )
}

/**
 * A stacked display for minutes and AM/PM indicator.
 */
@Composable
fun TimeStack(
    minute: Int,
    amPm: String,
    minuteFontSize: TextUnit,
    amPmFontSize: TextUnit,
    style: TextStyle,
    offsetY: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(LocalLocale.current.platformLocale, "%02d", minute),
            style = style.copy(
                fontSize = minuteFontSize,
                lineHeight = (minuteFontSize.value * 0.6f).sp
            ),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = amPm,
            style = style.copy(
                fontSize = amPmFontSize,
                lineHeight = (amPmFontSize.value * 0.8f).sp
            ),
            modifier = Modifier.offset(y = offsetY),
            maxLines = 1,
            softWrap = false
        )
    }
}
