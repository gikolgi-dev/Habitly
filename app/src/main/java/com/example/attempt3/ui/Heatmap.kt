package com.example.attempt3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attempt3.data.Database.Completion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun Heatmap(
    completions: List<Completion>,
    habitColor: Color,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    showMonthLabels: Boolean,
    dayOfWeekLabelsVisible: Boolean,
    dayOfWeekLabelsOnRight: Boolean,
    showAllDayOfWeekLabels: Boolean,
    showYearDivider: Boolean = true
) {
    // 1. Prepare static configuration
    val dayOfWeekLabels = remember {
        val format = SimpleDateFormat("E", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        (0..6).map {
            val day = format.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            day
        }
    }

    val cellSize = 10.dp
    val minSpacing = 4.dp

    // 2. Prepare Data State
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Pre-calculate completed dates normalized to midnight for O(1) lookup
    val completionDates = remember(completions) {
        completions.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSet()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Left Labels
        if (dayOfWeekLabelsVisible && !dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(
                labels = dayOfWeekLabels,
                showAll = showAllDayOfWeekLabels,
                showMonthLabels = showMonthLabels,
                cellSize = cellSize,
                minSpacing = minSpacing
            )
        }

        // Main Heatmap Grid
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val cellSizePx = with(density) { cellSize.toPx() }
            val minSpacingPx = with(density) { minSpacing.toPx() }

            // Calculate how many weeks fit on screen and total history
            val numWeeksOnScreen = ((maxWidthPx + minSpacingPx) / (cellSizePx + minSpacingPx)).toInt()

            val totalWeeks = remember(completions, isScrollable, numWeeksOnScreen) {
                val oldestCompletion = completions.minByOrNull { it.date }
                if (oldestCompletion == null) {
                    numWeeksOnScreen
                } else {
                    val oldestCal = Calendar.getInstance().apply {
                        timeInMillis = oldestCompletion.date
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val todayCal = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        // Adjust if future
                        if (timeInMillis > System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, -7)
                        }
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val diff = todayCal.timeInMillis - oldestCal.timeInMillis
                    val weeksDiff = (diff / (1000L * 60 * 60 * 24 * 7)).toInt() + 1

                    if (isScrollable) weeksDiff.coerceAtLeast(numWeeksOnScreen) else numWeeksOnScreen
                }
            }

            val scrollState = rememberScrollState()

            LaunchedEffect(totalWeeks) {
                if (isScrollable) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    modifier = if (isScrollable) Modifier else Modifier.fillMaxWidth(),
                    reverseLayout = true,
                    horizontalArrangement = if (isScrollable) Arrangement.spacedBy(minSpacing) else Arrangement.SpaceBetween,
                    userScrollEnabled = isScrollable
                ) {
                    items(totalWeeks) { weekIndex ->
                        // Calculate start of this specific week
                        val weekStartDate = remember(weekIndex) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.WEEK_OF_YEAR, -weekIndex)
                            val todayDate = cal.timeInMillis
                            cal.firstDayOfWeek = Calendar.MONDAY
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            // Safety check for future dates causing issues
                            if (cal.timeInMillis > todayDate) {
                                cal.add(Calendar.DAY_OF_YEAR, -7)
                            }
                            cal
                        }

                        HeatmapWeekColumn(
                            weekStartDate = weekStartDate,
                            weekIndex = weekIndex,
                            totalWeeks = totalWeeks,
                            today = today,
                            completionDates = completionDates,
                            habitColor = habitColor,
                            cellSize = cellSize,
                            minSpacing = minSpacing,
                            showMonthLabels = showMonthLabels,
                            showYearDivider = showYearDivider,
                            isScrollable = isScrollable
                        )
                    }
                }
            }
        }

        // Right Labels
        if (dayOfWeekLabelsVisible && dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(
                labels = dayOfWeekLabels,
                showAll = showAllDayOfWeekLabels,
                showMonthLabels = showMonthLabels,
                cellSize = cellSize,
                minSpacing = minSpacing
            )
        }
    }
}

@Composable
private fun HeatmapWeekColumn(
    weekStartDate: Calendar,
    weekIndex: Int,
    totalWeeks: Int,
    today: Calendar,
    completionDates: Set<Long>,
    habitColor: Color,
    cellSize: Dp,
    minSpacing: Dp,
    showMonthLabels: Boolean,
    showYearDivider: Boolean,
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

    val isStartOfYear = remember(weekStartDate) {
        val cal = weekStartDate.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 6) // check Sunday
        val currentYear = cal.get(Calendar.YEAR)
        cal.add(Calendar.WEEK_OF_YEAR, -1) // check previous Sunday
        val prevYear = cal.get(Calendar.YEAR)
        currentYear != prevYear
    }

    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val minSpacingPx = with(LocalDensity.current) { minSpacing.toPx() }

    // 2. Render Column
    Column(
        modifier = Modifier
            .layout { measurable, constraints ->
                // Force fixed width layout
                val placeable = measurable.measure(constraints)
                val cellWidthPx = cellSize.toPx().roundToInt()
                layout(cellWidthPx, placeable.height) {
                    val x = (cellWidthPx - placeable.width) / 2
                    placeable.placeRelative(x, 0)
                }
            }
            .drawBehind {
                if (isStartOfYear && showYearDivider) {
                    val startY = if (showMonthLabels) 24.dp.toPx() else 0.dp.toPx()

                    // FIX: Draw to the left of the column (negative x).
                    // Since layout is reversed, the "gap" to the previous year is to the left.
                    // Center the line in the middle of the spacing.
                    val xOffset = -(minSpacingPx / 2)

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
            verticalArrangement = Arrangement.spacedBy(minSpacing)
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
                        )
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekLabels(
    labels: List<String>,
    showAll: Boolean,
    showMonthLabels: Boolean,
    cellSize: Dp,
    minSpacing: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showMonthLabels) {
            Box(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(minSpacing)
        ) {
            labels.forEachIndexed { index, label ->
                val isVisible = if (showAll) {
                    true
                } else {
                    index % 2 != 0 // Show Mon, Wed, Fri, Sun
                }

                Box(
                    modifier = Modifier.height(cellSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isVisible) 0.6f else 0f),
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            ),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
            }
        }
    }
}