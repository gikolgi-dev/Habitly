/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.ui.components.HeatmapWeekColumn
import com.habitly.habitly.ui.components.HeatmapWeekData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

@Composable
fun rememberMaxSpeedFlingBehavior(maxVelocity: Float): FlingBehavior {
    val defaultFlingBehavior = ScrollableDefaults.flingBehavior()
    return remember(maxVelocity, defaultFlingBehavior) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val limitedVelocity = initialVelocity.coerceIn(-maxVelocity, maxVelocity)
                return with(defaultFlingBehavior) {
                    performFling(limitedVelocity)
                }
            }
        }
    }
}

@Composable
fun Heatmap(
    completions: List<Completion>,
    habitColor: Color,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    showMonthLabels: Boolean,
    visibleDayLabels: Set<String>,
    dayOfWeekLabelsOnRight: Boolean,
    showYearDivider: Boolean = true,
    showYearLabels: Boolean = true,
    showScrollBlur: Boolean,
    minWeeks: Int = 0,
    isInfinite: Boolean = false,
    currentDateMillis: Long = System.currentTimeMillis(),
    habit: Habit? = null,
    showNotificationDot: Boolean = false,
    notificationDotRange: String = "today_and_future",
    notificationDotAlpha: Float = 1f
) {
    val density = LocalDensity.current

    val tz = remember { TimeZone.getDefault() }

    val todayDayIndex = remember(currentDateMillis, tz) {
        val offset = tz.getOffset(currentDateMillis)
        (currentDateMillis + offset) / 86400000L
    }

    // Normalized completed dates for fast O(1) lookup using pure math
    val completionDates = remember(completions, tz) {
        completions.map {
            val offset = tz.getOffset(it.date)
            (it.date + offset) / 86400000L
        }.toSet()
    }

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
    val verticalSpacing = 4.dp
    val minHorizontalSpacing = 4.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (visibleDayLabels.isNotEmpty() && !dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(dayOfWeekLabels, visibleDayLabels, showMonthLabels, cellSize, verticalSpacing)
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).graphicsLayer(clip = false)) {
            val availableWidthPx = constraints.maxWidth
            val cellSizePx = with(density) { cellSize.roundToPx() }
            val minSpacingPx = with(density) { minHorizontalSpacing.roundToPx() }

            val numWeeksOnScreen = floor((availableWidthPx + minSpacingPx).toFloat() / (cellSizePx + minSpacingPx)).toInt().coerceAtLeast(1)
            val horizontalSpacing = if (numWeeksOnScreen > 1) {
                val remainingSpacePx = availableWidthPx - (numWeeksOnScreen * cellSizePx)
                with(density) { (remainingSpacePx.toFloat() / (numWeeksOnScreen - 1)).toDp() }
            } else minHorizontalSpacing

            val totalWeeks = remember(completions, numWeeksOnScreen, isScrollable, minWeeks, isInfinite, currentDateMillis) {
                val oldestCompletion = if (completions.isNotEmpty()) completions.minOf { it.date } else null
                val weeksDiff = if (oldestCompletion == null) 0 else {
                    val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }

                    cal.timeInMillis = oldestCompletion
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val oldestMon = cal.timeInMillis

                    cal.timeInMillis = currentDateMillis
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    if (cal.timeInMillis > currentDateMillis) cal.add(Calendar.WEEK_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val currentMon = cal.timeInMillis

                    ((currentMon - oldestMon) / (1000L * 60 * 60 * 24 * 7)).toInt() + 1
                }
                if (isScrollable) {
                    if (isInfinite) {
                        maxOf(weeksDiff + 2, numWeeksOnScreen, minWeeks)
                    } else {
                        maxOf(numWeeksOnScreen, minWeeks)
                    }
                } else {
                    numWeeksOnScreen
                }
            }

            val lazyListState = rememberLazyListState()
            val flingBehavior = rememberMaxSpeedFlingBehavior(with(density) { 3000.dp.toPx() })

            LaunchedEffect(isScrollable) { lazyListState.scrollToItem(0) }

            val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
            val currentMondayMillis = remember(currentDateMillis) {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    timeInMillis = currentDateMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    if (timeInMillis > currentDateMillis) add(Calendar.WEEK_OF_YEAR, -1)
                }
                cal.timeInMillis
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false).fadingEdge(lazyListState, isScrollable && showScrollBlur),
                state = lazyListState,
                reverseLayout = true,
                flingBehavior = flingBehavior,
                horizontalArrangement = if (!isScrollable) Arrangement.SpaceBetween else Arrangement.spacedBy(horizontalSpacing),
                userScrollEnabled = isScrollable
            ) {
                items(count = totalWeeks, key = { it }) { weekIndex ->
                    val weekData = remember(weekIndex, currentMondayMillis, completionDates, todayDayIndex, showMonthLabels, tz, isScrollable, totalWeeks, habit, showNotificationDot, notificationDotRange) {
                        val cal = Calendar.getInstance()
                        cal.firstDayOfWeek = Calendar.MONDAY
                        cal.timeInMillis = currentMondayMillis
                        cal.add(Calendar.WEEK_OF_YEAR, -weekIndex)
                        val weekStartMillis = cal.timeInMillis

                        val offset = tz.getOffset(weekStartMillis)
                        val weekStartDayIndex = (weekStartMillis + offset) / 86400000L

                        val completed = BooleanArray(7)
                        val future = BooleanArray(7)
                        var todayIdx = -1

                        for (i in 0..6) {
                            val dayIndex = weekStartDayIndex + i
                            completed[i] = completionDates.contains(dayIndex)
                            future[i] = dayIndex > todayDayIndex
                            if (dayIndex == todayDayIndex) todayIdx = i
                        }

                        // Year transition check
                        cal.add(Calendar.DAY_OF_YEAR, 6)
                        val yearEnd = cal.get(Calendar.YEAR)
                        cal.add(Calendar.WEEK_OF_YEAR, -1)
                        val yearPrev = cal.get(Calendar.YEAR)
                        var isStartOfYear = yearEnd != yearPrev
                        var yearDigits = if (isStartOfYear) yearEnd.toString() else null

                        val dots = BooleanArray(7)
                        val cal0 = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
                        cal0.timeInMillis = currentMondayMillis
                        cal0.add(Calendar.DAY_OF_YEAR, 6)
                        val yEnd0 = cal0.get(Calendar.YEAR)
                        cal0.add(Calendar.WEEK_OF_YEAR, -1)
                        val yPrev0 = cal0.get(Calendar.YEAR)
                        val week0IsStartOfYear = yEnd0 != yPrev0
                
                        if (week0IsStartOfYear) {
                            if (weekIndex == 0) {
                                yearDigits = null
                                isStartOfYear = false
                            } else if (weekIndex == 1) {
                                yearDigits = yEnd0.toString()
                                isStartOfYear = true
                            }
                        }

                        if (showNotificationDot && habit?.notificationsEnabled == true) {
                            val habitDays = habit.notificationDays?.split(",")?.toSet() ?: emptySet()
                            
                            val currentWeekStartDayIndex = (currentMondayMillis + tz.getOffset(currentMondayMillis)) / 86400000L
                            for (i in 0..6) {
                                val dIndex = currentWeekStartDayIndex + i
                                val shouldShow = when (notificationDotRange) {
                                    "future" -> dIndex > todayDayIndex
                                    "today_and_future" -> dIndex >= todayDayIndex
                                    else -> true // "this_week"
                                }
                                
                                if (shouldShow) {
                                    val dayCal = Calendar.getInstance().apply { 
                                        timeInMillis = currentMondayMillis
                                        add(Calendar.DAY_OF_YEAR, i)
                                    }
                                    val dayName = SimpleDateFormat("EEE", Locale.ENGLISH).format(dayCal.time).uppercase()
                                    if (habitDays.contains(dayName)) {
                                        if (weekIndex == 0) {
                                            dots[i] = true
                                        }
                                    }
                                }
                            }
                        }

                        // Month label logic
                        var monthLabel: String? = null
                        if (showMonthLabels && !(!isScrollable && weekIndex == totalWeeks - 1)) {
                            val labelCal = Calendar.getInstance()
                            labelCal.timeInMillis = weekStartMillis
                            for (d in 0..6) {
                                if (labelCal.get(Calendar.DAY_OF_MONTH) == 1) {
                                    monthLabel = monthFormat.format(labelCal.time)
                                    break
                                }
                                labelCal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }

                        HeatmapWeekData(
                            weekStartMillis,
                            completed.toList(),
                            future.toList(),
                            todayIdx,
                            monthLabel,
                            isStartOfYear,
                            yearDigits,
                            dots.toList()
                        )
                    }

                    HeatmapWeekColumn(weekData, habitColor, cellSize, verticalSpacing, horizontalSpacing, showMonthLabels, showYearDivider, showYearLabels, notificationDotAlpha)
                }
            }
        }

        if (visibleDayLabels.isNotEmpty() && dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(dayOfWeekLabels, visibleDayLabels, showMonthLabels, cellSize, verticalSpacing)
        }
    }
}

@Composable
private fun DayOfWeekLabels(
    labels: List<String>,
    visibleDayLabels: Set<String>,
    showMonthLabels: Boolean,
    cellSize: Dp,
    minSpacing: Dp
) {
    val dayValues = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showMonthLabels) {
            Box(Modifier.height(20.dp))
            Spacer(Modifier.height(4.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(minSpacing)) {
            labels.forEachIndexed { index, label ->
                val isVisible = dayValues[index] in visibleDayLabels
                Box(Modifier.height(cellSize), contentAlignment = Alignment.Center) {
                    val alpha = if (isVisible) 0.6f else 0f
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
                        )
                    )
                }
            }
        }
    }
}
