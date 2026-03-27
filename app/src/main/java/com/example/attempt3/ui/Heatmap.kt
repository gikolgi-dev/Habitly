/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui

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
import androidx.compose.foundation.lazy.items
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
import com.example.attempt3.data.Database.Completion
import com.example.attempt3.ui.components.HeatmapWeekColumn
import com.example.attempt3.ui.components.HeatmapWeekData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
    minWeeks: Int = 0
) {
    val density = LocalDensity.current
    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Normalized completed dates for fast O(1) lookup
    val completionDates = remember(completions) {
        val cal = Calendar.getInstance()
        completions.map {
            cal.timeInMillis = it.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
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

            val totalWeeks = remember(completions, numWeeksOnScreen, isScrollable, minWeeks) {
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

                    cal.timeInMillis = System.currentTimeMillis()
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    if (cal.timeInMillis > System.currentTimeMillis()) cal.add(Calendar.WEEK_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val currentMon = cal.timeInMillis

                    ((currentMon - oldestMon) / (1000L * 60 * 60 * 24 * 7)).toInt() + 1
                }
                if (isScrollable) maxOf(weeksDiff, numWeeksOnScreen, minWeeks) else numWeeksOnScreen
            }

            val weeksData = remember(totalWeeks, showMonthLabels, completionDates, todayStartMillis) {
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    if (timeInMillis > System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, -1)
                }
                val currentMonday = cal.timeInMillis
                val dayCal = cal.clone() as Calendar

                val rawWeeks = List(totalWeeks) { weekIndex ->
                    cal.timeInMillis = currentMonday
                    cal.add(Calendar.WEEK_OF_YEAR, -weekIndex)
                    val weekStart = cal.timeInMillis

                    val completed = BooleanArray(7)
                    val future = BooleanArray(7)
                    var todayIdx = -1
                    
                    dayCal.timeInMillis = weekStart
                    for (i in 0..6) {
                        val d = dayCal.timeInMillis
                        completed[i] = completionDates.contains(d)
                        future[i] = d > todayStartMillis
                        if (d == todayStartMillis) todayIdx = i
                        dayCal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    // Year transition check
                    dayCal.timeInMillis = weekStart
                    dayCal.add(Calendar.DAY_OF_YEAR, 6)
                    val yearEnd = dayCal.get(Calendar.YEAR)
                    dayCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val yearPrev = dayCal.get(Calendar.YEAR)
                    val isStartOfYear = yearEnd != yearPrev
                    val yearDigits = if (isStartOfYear) yearEnd.toString() else null

                    Triple(weekStart, Triple(completed.toList(), future.toList(), todayIdx), Pair(isStartOfYear, yearDigits))
                }

                val monthLabels = MutableList<String?>(totalWeeks) { null }
                if (showMonthLabels) {
                    val labelCal = Calendar.getInstance()
                    for (i in 0 until totalWeeks) {
                        labelCal.timeInMillis = rawWeeks[i].first
                        var found: String? = null
                        for (d in 0..6) {
                            if (labelCal.get(Calendar.DAY_OF_MONTH) == 1) {
                                found = monthFormat.format(labelCal.time)
                                break
                            }
                            labelCal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        if (found != null) {
                            val target = when {
                                i == 0 && totalWeeks > 1 -> 1
                                i == totalWeeks - 1 && totalWeeks > 1 -> totalWeeks - 2
                                else -> i
                            }
                            if (monthLabels[target] == null) monthLabels[target] = found
                        }
                    }
                }

                rawWeeks.mapIndexed { index, (start, status, year) ->
                    HeatmapWeekData(start, status.first, status.second, status.third, monthLabels[index], year.first, year.second)
                }
            }

            val lazyListState = rememberLazyListState()
            val flingBehavior = rememberMaxSpeedFlingBehavior(with(density) { 3000.dp.toPx() })

            LaunchedEffect(isScrollable) { lazyListState.scrollToItem(0) }

            LazyRow(
                modifier = Modifier.fillMaxWidth().graphicsLayer(clip = false).fadingEdge(lazyListState, isScrollable && showScrollBlur),
                state = lazyListState,
                reverseLayout = true,
                flingBehavior = flingBehavior,
                horizontalArrangement = if (!isScrollable) Arrangement.SpaceBetween else Arrangement.spacedBy(horizontalSpacing),
                userScrollEnabled = isScrollable
            ) {
                items(items = weeksData, key = { it.weekStartMillis }) { weekData ->
                    HeatmapWeekColumn(weekData, habitColor, cellSize, verticalSpacing, horizontalSpacing, showMonthLabels, showYearDivider, showYearLabels)
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
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isVisible) 0.6f else 0f),
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
