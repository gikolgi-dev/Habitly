/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitly.habitly.ui.colors.isBright
import com.habitly.habitly.ui.colors.toThemeHabitColor

@Immutable
data class HeatmapWeekData(
    val weekStartMillis: Long,
    val completedDays: List<Boolean>, // 7 booleans
    val futureDays: List<Boolean>,    // 7 booleans
    val todayIndex: Int,              // 0-6 or -1
    val monthLabel: String?,
    val isStartOfYear: Boolean,
    val yearDigits: String?,
    val notificationDots: List<Boolean>,
    val completionRatios: List<Float> = completedDays.map { if (it) 1f else 0f }
)

@Composable
fun HeatmapWeekColumn(
    weekData: HeatmapWeekData,
    habitColor: Color,
    cellSize: Dp,
    verticalSpacing: Dp,
    horizontalSpacing: Dp,
    monthLabelAlpha: Float,
    monthTopSpacerHeight: Dp = 0.dp,
    monthRowHeight: Dp = 14.dp,
    monthSpacerHeight: Dp = 2.dp,
    yearDividerAlpha: Float,
    yearLabelsAlpha: Float,
    notificationDotAlpha: Float = 1f,
    animateTileChanges: Boolean = false,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val isDark = !surface.isBright()
    val effectiveHabitColor = habitColor.toThemeHabitColor(isDark)
    val density = LocalDensity.current
    
    val cellSizePx = with(density) { cellSize.toPx() }
    val verticalSpacingPx = with(density) { verticalSpacing.toPx() }
    val horizontalSpacingPx = with(density) { horizontalSpacing.toPx() }
    val cornerRadiusPx = with(density) { 2.dp.toPx() }

    val hasDots = weekData.notificationDots.any { it }
    val animatedDotAlphas = if (hasDots) {
        (0..6).map { i ->
            val targetDotAlpha = if (weekData.notificationDots.getOrElse(i) { false }) notificationDotAlpha else 0f
            androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetDotAlpha,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "notificationDotAlpha_$i"
            ).value
        }
    } else emptyList()

    val animatedRatios = (0..6).map { i ->
        val targetRatio = weekData.completionRatios.getOrNull(i) ?: if (weekData.completedDays[i]) 1f else 0f
        if (animateTileChanges) {
            androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
                label = "tileRatio_$i"
            ).value
        } else {
            targetRatio
        }
    }

    Column(
        modifier = modifier.width(cellSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (monthTopSpacerHeight > 0.dp) {
            Spacer(modifier = Modifier.height(monthTopSpacerHeight))
        }
        if (monthRowHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .width(cellSize)
                    .height(monthRowHeight),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (monthLabelAlpha > 0f) {
                    weekData.monthLabel?.let {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            color = onSurface.copy(alpha = 0.6f * monthLabelAlpha),
                            maxLines = 1,
                            softWrap = false,
                            style = LocalTextStyle.current.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
                            ),
                            modifier = Modifier.wrapContentSize(unbounded = true)
                        )
                    }
                }
            }
        }
        if (monthSpacerHeight > 0.dp) {
            Spacer(modifier = Modifier.height(monthSpacerHeight))
        }

        Box(
            modifier = Modifier
                .size(width = cellSize, height = (cellSize * 7) + (verticalSpacing * 6))
                .drawBehind {
                    if (weekData.isStartOfYear && yearDividerAlpha > 0f) {
                        val x = -(horizontalSpacingPx / 2f)
                        drawLine(
                            color = onSurface.copy(alpha = yearDividerAlpha),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 0.75.dp.toPx()
                        )
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
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

                    val ratio = animatedRatios.getOrElse(i) { weekData.completionRatios.getOrNull(i) ?: if (isCompleted) 1f else 0f }
                    val color = when {
                        isFuture -> onSurface.copy(alpha = 0.05f)
                        ratio >= 1f -> effectiveHabitColor
                        ratio > 0f -> androidx.compose.ui.graphics.lerp(
                            effectiveHabitColor.copy(alpha = if (isDark) 0.35f else 0.42f),
                            effectiveHabitColor,
                            ratio
                        )
                        else -> effectiveHabitColor.copy(alpha = if (isDark) 0.15f else 0.24f)
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

                    val dotAlpha = animatedDotAlphas.getOrElse(i) { 0f }
                    if (dotAlpha > 0f) {
                        drawCircle(
                            color = Color.White.copy(alpha = dotAlpha),
                            radius = 1.dp.toPx(),
                            center = Offset(cellSizePx / 2f, top + cellSizePx / 2f)
                        )
                    }
                }
            }

            // Overlay Year Labels if needed
            if (weekData.isStartOfYear && yearLabelsAlpha > 0f) {
                Column(
                    modifier = Modifier
                        .height((cellSize * 7) + (verticalSpacing * 6))
                        .graphicsLayer { alpha = yearLabelsAlpha },
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
