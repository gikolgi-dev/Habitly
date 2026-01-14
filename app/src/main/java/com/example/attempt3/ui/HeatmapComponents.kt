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
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HeatmapWeekColumn(
    weekStartDate: Calendar,
    weekIndex: Int,
    totalWeeks: Int,
    today: Calendar,
    completionDates: Set<Long>,
    habitColor: Color,
    cellSize: Dp,
    verticalSpacing: Dp, // Renamed from minSpacing
    horizontalSpacing: Dp, // Added to handle drawing divider correctly
    showMonthLabels: Boolean,
    showYearDivider: Boolean,
    showYearLabels: Boolean,
    isScrollable: Boolean
) {
    // 1. Calculate Metadata (Labels, Divider Flags)
    val monthLabel = remember(weekStartDate, weekIndex, totalWeeks) {
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        fun hasFirstOfMonth(cal: Calendar): String? {
            val checkCal = cal.clone() as Calendar
            for (i in 0..6) {
                if (checkCal.get(Calendar.DAY_OF_MONTH) == 1) {
                    return monthFormat.format(checkCal.time)
                }
                checkCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return null
        }

        val currentLabel = hasFirstOfMonth(weekStartDate)
        if (currentLabel != null) {
            if (weekIndex == 0) null else currentLabel
        } else if (weekIndex == 1) {
            val nextWeekCal = weekStartDate.clone() as Calendar
            nextWeekCal.add(Calendar.WEEK_OF_YEAR, 1)
            val shiftedLabel = hasFirstOfMonth(nextWeekCal)
            shiftedLabel ?: if (weekIndex == totalWeeks - 1 && isScrollable) {
                monthFormat.format(weekStartDate.time)
            } else null
        } else if (weekIndex == totalWeeks - 1 && isScrollable) {
            monthFormat.format(weekStartDate.time)
        } else {
            null
        }
    }

    // Logic for drawing the divider line (Start of New Year)
    val isStartOfYear = remember(weekStartDate) {
        val cal = weekStartDate.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 6) // check Sunday
        val currentYear = cal.get(Calendar.YEAR)
        cal.add(Calendar.WEEK_OF_YEAR, -1) // check previous Sunday
        val prevYear = cal.get(Calendar.YEAR)
        currentYear != prevYear
    }

    // Logic for displaying Text (Start of New Year)
    val yearDigits = remember(weekStartDate, isStartOfYear) {
        if (isStartOfYear) {
            val cal = weekStartDate.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, 6)
            cal.get(Calendar.YEAR).toString()
        } else null
    }

    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val density = LocalDensity.current
    val horizontalSpacingPx = with(density) { horizontalSpacing.toPx() }

    // 2. Render Column
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
                if (isStartOfYear && showYearDivider) {
                    val startY = if (showMonthLabels) 24.dp.toPx() else 0.dp.toPx()
                    // Draw to the left of the column
                    val xOffset = -(horizontalSpacingPx / 2)

                    drawLine(
                        color = lineColor,
                        start = Offset(xOffset, startY),
                        end = Offset(xOffset, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showMonthLabels) {
            Box(
                modifier = Modifier.height(20.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                monthLabel?.let {
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
            (0..6).forEach { dayIndex ->
                val day = remember(weekStartDate) {
                    val dayCal = weekStartDate.clone() as Calendar
                    dayCal.add(Calendar.DAY_OF_YEAR, dayIndex)
                    dayCal.set(Calendar.HOUR_OF_DAY, 0)
                    dayCal.set(Calendar.MINUTE, 0)
                    dayCal.set(Calendar.SECOND, 0)
                    dayCal.set(Calendar.MILLISECOND, 0)
                    dayCal
                }

                val isCompleted = completionDates.contains(day.timeInMillis)

                val isTodayCell = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                val cellColor = when {
                    isCompleted -> habitColor
                    day.after(today) -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
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
                    if (showYearLabels && isStartOfYear && dayIndex < 4) {
                        yearDigits?.getOrNull(dayIndex)?.let { digit ->
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