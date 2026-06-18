/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class HeatmapWeekData(
    val weekStartMillis: Long,
    val completedDays: List<Boolean>, // 7 booleans
    val futureDays: List<Boolean>,    // 7 booleans
    val todayIndex: Int,              // 0-6 or -1
    val monthLabel: String?,
    val isStartOfYear: Boolean,
    val yearDigits: String?,
    val notificationDots: List<Boolean>
)

@Composable
fun HeatmapWeekColumn(
    weekData: HeatmapWeekData,
    habitColor: Color,
    cellSize: Dp,
    verticalSpacing: Dp,
    horizontalSpacing: Dp,
    showMonthLabels: Boolean,
    showYearDivider: Boolean,
    showYearLabels: Boolean
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val lineColor = onSurface.copy(alpha = 0.5f)
    val density = LocalDensity.current
    
    val cellSizePx = with(density) { cellSize.toPx() }
    val verticalSpacingPx = with(density) { verticalSpacing.toPx() }
    val horizontalSpacingPx = with(density) { horizontalSpacing.toPx() }
    val cornerRadiusPx = with(density) { 2.dp.toPx() }

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
                    val xOffset = if (showMonthLabels) (horizontalSpacingPx / 2) - 1 else (horizontalSpacingPx / 2) - 12

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
                        color = onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Box(contentAlignment = Alignment.TopStart) {
            // Draw all cells in a single Canvas for performance
            Canvas(
                modifier = Modifier.size(
                    width = cellSize,
                    height = (cellSize * 7) + (verticalSpacing * 6)
                )
            ) {
                for (i in 0..6) {
                    val isCompleted = weekData.completedDays[i]
                    val isFuture = weekData.futureDays[i]
                    val isToday = weekData.todayIndex == i

                    val color = when {
                        isCompleted -> habitColor
                        isFuture -> onSurface.copy(alpha = 0.05f)
                        else -> habitColor.copy(alpha = 0.15f)
                    }

                    val top = i * (cellSizePx + verticalSpacingPx)
                    
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, top),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Fill
                    )

                    if (isToday) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(0.5.dp.toPx(), top + 0.5.dp.toPx()),
                            size = Size(cellSizePx - 1.dp.toPx(), cellSizePx - 1.dp.toPx()),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    if (weekData.notificationDots[i]) {
                        drawCircle(
                            color = Color.White,
                            radius = 1.dp.toPx(),
                            center = Offset(cellSizePx / 2f, top + cellSizePx / 2f)
                        )
                    }
                }
            }

            // Overlay Year Labels if needed
            if (showYearLabels && weekData.isStartOfYear) {
                Column(
                    modifier = Modifier.height((cellSize * 7) + (verticalSpacing * 6)),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    for (i in 0..3) {
                        Box(
                            modifier = Modifier.size(cellSize),
                            contentAlignment = Alignment.Center
                        ) {
                            weekData.yearDigits?.getOrNull(i)?.let { digit ->
                                Text(
                                    text = digit.toString(),
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = onSurface,
                                        shadow = Shadow(
                                            color = surface,
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
}
