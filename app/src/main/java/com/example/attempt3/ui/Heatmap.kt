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
import androidx.compose.foundation.lazy.itemsIndexed
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
    val verticalSpacing = 4.dp
    val minHorizontalSpacing = 4.dp

    // 2. Prepare Data State
    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
        if (visibleDayLabels.isNotEmpty() && !dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(
                labels = dayOfWeekLabels,
                visibleDayLabels = visibleDayLabels,
                showMonthLabels = showMonthLabels,
                cellSize = cellSize,
                minSpacing = verticalSpacing
            )
        }

        // Main Heatmap Grid
        BoxWithConstraints(modifier = Modifier.weight(1f).graphicsLayer(clip = false)) {
            val density = LocalDensity.current
            val availableWidthPx = constraints.maxWidth
            
            val cellSizePx = with(density) { cellSize.roundToPx() }
            val minSpacingPx = with(density) { minHorizontalSpacing.roundToPx() }

            // Calculate how many weeks fit on screen with at least minHorizontalSpacing
            val numWeeksOnScreen = floor((availableWidthPx + minSpacingPx).toFloat() / (cellSizePx + minSpacingPx)).toInt().coerceAtLeast(1)

            // Calculate precise horizontal spacing to be flush with edges.
            val horizontalSpacing = if (numWeeksOnScreen > 1) {
                val totalCellWidthPx = numWeeksOnScreen * cellSizePx
                val remainingSpacePx = availableWidthPx - totalCellWidthPx
                with(density) { (remainingSpacePx.toFloat() / (numWeeksOnScreen - 1)).toDp() }
            } else {
                minHorizontalSpacing
            }

            val totalWeeks = remember(completions, numWeeksOnScreen, isScrollable, minWeeks) {
                val oldestCompletion = completions.minByOrNull { it.date }
                val weeksDiff = if (oldestCompletion == null) {
                    0
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
                        if (timeInMillis > System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, -7)
                        }
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val diff = todayCal.timeInMillis - oldestCal.timeInMillis
                    (diff / (1000L * 60 * 60 * 24 * 7)).toInt() + 1
                }
                
                if (isScrollable) {
                    maxOf(weeksDiff, numWeeksOnScreen, minWeeks)
                } else {
                    numWeeksOnScreen
                }
            }

            val weeksData = remember(totalWeeks, showMonthLabels, isScrollable) {
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                
                fun getFirstOfMonthLabel(cal: Calendar): String? {
                    val checkCal = cal.clone() as Calendar
                    for (i in 0..6) {
                        if (checkCal.get(Calendar.DAY_OF_MONTH) == 1) {
                            return monthFormat.format(checkCal.time)
                        }
                        checkCal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return null
                }

                // 1. Pre-calculate week objects with raw data
                val rawWeeks = List(totalWeeks) { weekIndex ->
                    val cal = Calendar.getInstance()
                    cal.firstDayOfWeek = Calendar.MONDAY
                    cal.add(Calendar.WEEK_OF_YEAR, -weekIndex)
                    val todayDate = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    if (cal.timeInMillis > todayDate) {
                        cal.add(Calendar.DAY_OF_YEAR, -7)
                    }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val weekStartDate = cal.clone() as Calendar

                    val dayMillis = (0..6).map { dayIndex ->
                        val d = cal.clone() as Calendar
                        d.add(Calendar.DAY_OF_YEAR, dayIndex)
                        d.timeInMillis
                    }

                    val checkSunCal = weekStartDate.clone() as Calendar
                    checkSunCal.add(Calendar.DAY_OF_YEAR, 6)
                    val currentYear = checkSunCal.get(Calendar.YEAR)
                    checkSunCal.add(Calendar.WEEK_OF_YEAR, -1)
                    val prevYear = checkSunCal.get(Calendar.YEAR)
                    val isStartOfYear = currentYear != prevYear

                    val yearDigits = if (isStartOfYear) {
                        val yearCal = weekStartDate.clone() as Calendar
                        yearCal.add(Calendar.DAY_OF_YEAR, 6)
                        yearCal.get(Calendar.YEAR).toString()
                    } else null

                    Triple(weekStartDate, dayMillis, Pair(isStartOfYear, yearDigits))
                }

                // 2. Determine and Shift Month Labels to avoid edges
                val monthLabels = MutableList<String?>(totalWeeks) { null }
                for (i in 0 until totalWeeks) {
                    val label = getFirstOfMonthLabel(rawWeeks[i].first)
                    if (label != null) {
                        // reverseLayout = true, so index 0 is right edge, totalWeeks-1 is left edge.
                        val targetIndex = when {
                            // If it's the rightmost column, move label one column to the left (index 1)
                            i == 0 && totalWeeks > 1 -> 1
                            // If it's the leftmost column, move label one column to the right (index totalWeeks-2)
                            i == totalWeeks - 1 && totalWeeks > 1 -> totalWeeks - 2
                            else -> i
                        }
                        if (monthLabels[targetIndex] == null) {
                            monthLabels[targetIndex] = label
                        }
                    }
                }

                // 3. Construct final HeatmapWeekData
                rawWeeks.mapIndexed { index, (weekStartDate, dayMillis, yearData) ->
                    HeatmapWeekData(
                        dayMillis = dayMillis,
                        monthLabel = monthLabels[index],
                        isStartOfYear = yearData.first,
                        yearDigits = yearData.second
                    )
                }
            }

            val lazyListState = rememberLazyListState()
            val maxSpeed = with(density) { 3000.dp.toPx() }
            val flingBehavior = rememberMaxSpeedFlingBehavior(maxVelocity = maxSpeed)

            LaunchedEffect(isScrollable) {
                lazyListState.scrollToItem(0)
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(clip = false)
                    .fadingEdge(lazyListState, enabled = isScrollable && showScrollBlur),
                state = lazyListState,
                reverseLayout = true,
                flingBehavior = flingBehavior,
                horizontalArrangement = if (!isScrollable) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.spacedBy(horizontalSpacing)
                },
                userScrollEnabled = isScrollable
            ) {
                itemsIndexed(weeksData) { _, weekData ->
                    HeatmapWeekColumn(
                        weekData = weekData,
                        todayMillis = todayMillis,
                        completionDates = completionDates,
                        habitColor = habitColor,
                        cellSize = cellSize,
                        verticalSpacing = verticalSpacing,
                        horizontalSpacing = horizontalSpacing,
                        showMonthLabels = showMonthLabels,
                        showYearDivider = showYearDivider,
                        showYearLabels = showYearLabels
                    )
                }
            }
        }

        // Right Labels
        if (visibleDayLabels.isNotEmpty() && dayOfWeekLabelsOnRight) {
            DayOfWeekLabels(
                labels = dayOfWeekLabels,
                visibleDayLabels = visibleDayLabels,
                showMonthLabels = showMonthLabels,
                cellSize = cellSize,
                minSpacing = verticalSpacing
            )
        }
    }
}

@Composable
fun DayOfWeekLabels(
    labels: List<String>,
    visibleDayLabels: Set<String>,
    showMonthLabels: Boolean,
    cellSize: Dp,
    minSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val dayValues = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
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
                val dayValue = dayValues[index]
                val isVisible = dayValue in visibleDayLabels

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
