/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun DayOfWeekSelector(
    selectedDays: Set<String>,
    onDaySelected: (String) -> Unit,
    enabled: Boolean = true,
    borderAlpha: Float = 0.1f,
    horizontalPadding: Dp = 0.dp
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val dayValues = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val effectiveBorderAlpha = if (borderAlpha > 0.1f) borderAlpha else 0.1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayValues.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(day)
            val animatedColorState = animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(durationMillis = 300), // Adjust speed here
                label = "ColorTransition"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .border(
                        width = if(isSelected) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = effectiveBorderAlpha),
                        shape = MaterialShapes.Square.toShape()
                    )
                    .clip(MaterialShapes.Square.toShape())
                    .background(
                        animatedColorState.value
                    )
                    .clickable(enabled = enabled) { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = days[index],
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}