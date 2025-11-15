package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    showAllDayOfWeekLabels: Boolean
) {
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

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (dayOfWeekLabelsVisible && !dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(
                labels = dayOfWeekLabels,
                showAll = showAllDayOfWeekLabels,
                showMonthLabels = showMonthLabels
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val cellSize = 10.dp
            val minSpacing = 4.dp
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val cellSizePx = with(density) { cellSize.toPx() }
            val minSpacingPx = with(density) { minSpacing.toPx() }
            val numWeeksOnScreen = ((maxWidthPx + minSpacingPx) / (cellSizePx + minSpacingPx)).toInt()

            val oldestCompletion = remember(completions) { completions.minByOrNull { it.date } }

            val numWeeksSinceOldest = remember(oldestCompletion) {
                if (oldestCompletion == null) return@remember 0

                val oldestCal = Calendar.getInstance().apply {
                    timeInMillis = oldestCompletion.date
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val todayCal = Calendar.getInstance()
                val todayDate = todayCal.timeInMillis
                todayCal.firstDayOfWeek = Calendar.MONDAY
                todayCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                if (todayCal.timeInMillis > todayDate) {
                    todayCal.add(Calendar.DAY_OF_YEAR, -7)
                }
                todayCal.set(Calendar.HOUR_OF_DAY, 0)
                todayCal.set(Calendar.MINUTE, 0)
                todayCal.set(Calendar.SECOND, 0)
                todayCal.set(Calendar.MILLISECOND, 0)


                val diff = todayCal.timeInMillis - oldestCal.timeInMillis
                (diff / (1000L * 60 * 60 * 24 * 7)).toInt() + 1
            }

            val totalWeeks = if (isScrollable) {
                numWeeksSinceOldest.coerceAtLeast(numWeeksOnScreen)
            } else {
                numWeeksOnScreen
            }
            val scrollState = rememberScrollState()

            LaunchedEffect(totalWeeks) {
                if (isScrollable) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }

            // Define today here for this Composable's internal usage
            val today = remember {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal
            }
            val lazyRow = @Composable {
                LazyRow(
                    modifier = if (isScrollable) Modifier
                        .fillMaxWidth() else Modifier,
                    reverseLayout = true,
                    horizontalArrangement = Arrangement.spacedBy(minSpacing),
                    userScrollEnabled = isScrollable
                ) {
                    items(totalWeeks) { weekIndex ->
                        val weekStartDate = remember(weekIndex) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.WEEK_OF_YEAR, -weekIndex)
                            val todayDate = cal.timeInMillis
                            cal.firstDayOfWeek = Calendar.MONDAY
                            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            if (cal.timeInMillis > todayDate) {
                                cal.add(Calendar.DAY_OF_YEAR, -7)
                            }
                            cal
                        }

                        val monthLabel = remember(weekStartDate, weekIndex, totalWeeks) {
                            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                            val tempCal = weekStartDate.clone() as Calendar
                            // Show month label if the week contains the first day of a month
                            for (i in 0..6) {
                                if (tempCal.get(Calendar.DAY_OF_MONTH) == 1) {
                                    return@remember monthFormat.format(tempCal.time)
                                }
                                tempCal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            // Also show month label for the oldest week shown
                            if (weekIndex == totalWeeks - 1 && isScrollable) {
                                return@remember monthFormat.format(weekStartDate.time)
                            }
                            null
                        }

                        Column(
                            modifier = Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val cellWidthPx = cellSize.toPx().roundToInt()
                                layout(cellWidthPx, placeable.height) {
                                    val x = (cellWidthPx - placeable.width) / 2
                                    placeable.placeRelative(x, 0)
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier.height(16.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                monthLabel?.let {
                                    Text(
                                        text = it,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (showMonthLabels) 0.6f else 0f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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

                                    val isCompleted = completions.any {
                                        val completionCal = Calendar.getInstance().apply { timeInMillis = it.date }
                                        completionCal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                                                completionCal.get(
                                                    Calendar.DAY_OF_YEAR
                                                ) == day.get(Calendar.DAY_OF_YEAR)
                                    }

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
                                            .aspectRatio(1f) // Maintain aspect ratio
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
                }
            }
            if (isScrollable) {
                lazyRow()
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    lazyRow()
                }
            }
        }
        if (dayOfWeekLabelsVisible && dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(
                labels = dayOfWeekLabels,
                showAll = showAllDayOfWeekLabels,
                showMonthLabels = showMonthLabels
            )
        }
    }
}

@Composable
private fun DayOfWeekLabels(
    labels: List<String>,
    showAll: Boolean,
    showMonthLabels: Boolean,
    modifier: Modifier = Modifier
) {
    val minSpacing = 4.dp
    val cellSize = 10.dp

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showMonthLabels) {
            Box(modifier = Modifier.height(16.dp))
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
                    if (isVisible) {
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
}