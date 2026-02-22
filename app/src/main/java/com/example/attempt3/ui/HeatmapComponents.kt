package com.example.attempt3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class HeatmapWeekData(
    val dayMillis: List<Long>,
    val monthLabel: String?,
    val isStartOfYear: Boolean,
    val yearDigits: String?
)

@Composable
fun HeatmapWeekColumn(
    weekData: HeatmapWeekData,
    todayMillis: Long,
    completionDates: Set<Long>,
    habitColor: Color,
    cellSize: Dp,
    verticalSpacing: Dp,
    horizontalSpacing: Dp,
    showMonthLabels: Boolean,
    showYearDivider: Boolean,
    showYearLabels: Boolean
) {
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val density = LocalDensity.current
    val horizontalSpacingPx = with(density) { horizontalSpacing.toPx() }

    Column(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val cellWidthPx = cellSize.roundToPx()
                layout(cellWidthPx, placeable.height) {
                    val x = (cellWidthPx - placeable.width) / 2
                    placeable.placeRelative(x, 0)
                }
            }
            .drawBehind {
                if (weekData.isStartOfYear && showYearDivider) {
                    val startY = if (showMonthLabels) 24.dp.toPx() else 0.dp.toPx()
                    val xOffset = (horizontalSpacingPx / 2)-1

                    drawLine(
                        color = lineColor,
                        start = Offset(xOffset, startY),
                        end = Offset(xOffset, size.height),
                        strokeWidth = 0.75.dp.toPx()
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showMonthLabels) {
            Box(
                modifier = Modifier.height(20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                weekData.monthLabel?.let {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            weekData.dayMillis.forEachIndexed { dayIndex, dayMillis ->
                val isCompleted = completionDates.contains(dayMillis)
                val isTodayCell = dayMillis == todayMillis

                val cellColor = when {
                    isCompleted -> habitColor
                    dayMillis > todayMillis -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    else -> habitColor.copy(alpha = 0.15f)
                }

                Box(
                    modifier = Modifier
                        .size(cellSize)
                        .background(cellColor, RoundedCornerShape(2.dp))
                        .aspectRatio(1f)
                        .border(
                            width = 1.dp,
                            color = if (isTodayCell) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(2.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (showYearLabels && weekData.isStartOfYear && dayIndex < 4) {
                        weekData.yearDigits?.getOrNull(dayIndex)?.let { digit ->
                            Text(
                                text = digit.toString(),
                                style = TextStyle(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shadow = Shadow(
                                        color = MaterialTheme.colorScheme.surface,
                                        blurRadius = 2f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
