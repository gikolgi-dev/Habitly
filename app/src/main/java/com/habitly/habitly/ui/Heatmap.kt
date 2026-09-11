/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.getEffectiveStartDateMillis
import com.habitly.habitly.data.Database.getDailyTarget
import com.habitly.habitly.ui.components.HeatmapWeekColumn
import com.habitly.habitly.ui.components.HeatmapWeekData
import com.habitly.habitly.ui.components.getDaysAndDayValues
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
    notificationDotAlpha: Float = 1f,
    animateTileChanges: Boolean = false,
    firstDayOfWeek: Int = Calendar.MONDAY
) {
    val density = LocalDensity.current

    val tz = remember { TimeZone.getDefault() }

    val todayDayIndex = remember(currentDateMillis, tz) {
        val offset = tz.getOffset(currentDateMillis)
        (currentDateMillis + offset) / 86400000L
    }

    // Map of dayIndex to sum of amountOfCompletions
    val completionAmounts = remember(completions, tz) {
        val map = mutableMapOf<Long, Int>()
        completions.forEach {
            val offset = tz.getOffset(it.date)
            val dayIndex = (it.date + offset) / 86400000L
            map[dayIndex] = (map[dayIndex] ?: 0) + it.amountOfCompletions
        }
        map
    }

    val habitStartDayIndex = remember(habit, tz) {
        if (habit != null) {
            val startMillis = habit.getEffectiveStartDateMillis()
            val offset = tz.getOffset(startMillis)
            (startMillis + offset) / 86400000L
        } else {
            null
        }
    }

    val dayOfWeekLabels = remember(firstDayOfWeek) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = firstDayOfWeek
        cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        (0..6).map {
            val calDay = cal.get(Calendar.DAY_OF_WEEK)
            val label = getDayOfWeekShortLabel(calDay)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            label
        }
    }

    val cellSize = 10.dp
    val verticalSpacing = 4.dp
    val minHorizontalSpacing = 4.dp

    val animatedMonthLabelAlpha by animateFloatAsState(
        targetValue = if (showMonthLabels) 1f else 0f,
        animationSpec = tween(300),
        label = "monthLabelAlpha"
    )

    val animatedMonthTopSpacerHeight by animateDpAsState(
        targetValue = if (showMonthLabels) 2.dp else 0.dp,
        animationSpec = tween(300),
        label = "monthTopSpacerHeight"
    )

    val animatedMonthRowHeight by animateDpAsState(
        targetValue = if (showMonthLabels) 14.dp else 0.dp,
        animationSpec = tween(300),
        label = "monthRowHeight"
    )

    val animatedMonthSpacerHeight by animateDpAsState(
        targetValue = if (showMonthLabels) 6.dp else 8.dp,
        animationSpec = tween(300),
        label = "monthSpacerHeight"
    )

    val animatedYearDividerAlpha by animateFloatAsState(
        targetValue = if (showYearDivider) 0.5f else 0f,
        animationSpec = tween(300),
        label = "yearDividerAlpha"
    )

    val animatedYearLabelsAlpha by animateFloatAsState(
        targetValue = if (showYearLabels) 1f else 0f,
        animationSpec = tween(300),
        label = "yearLabelsAlpha"
    )

    val animatedNotificationDotAlpha by animateFloatAsState(
        targetValue = if (showNotificationDot) notificationDotAlpha else 0f,
        animationSpec = tween(300),
        label = "effectiveNotificationDotAlpha"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth().graphicsLayer(clip = false)) {
        val totalAvailableWidthPx = constraints.maxWidth
        val cellSizePx = with(density) { cellSize.roundToPx() }
        val minSpacingPx = with(density) { minHorizontalSpacing.roundToPx() }

        val totalSlots = floor((totalAvailableWidthPx + minSpacingPx).toFloat() / (cellSizePx + minSpacingPx)).toInt().coerceAtLeast(1)
        val horizontalSpacing = if (totalSlots > 1) {
            val remainingSpacePx = totalAvailableWidthPx - (totalSlots * cellSizePx)
            with(density) { (remainingSpacePx.toFloat() / (totalSlots - 1)).toDp() }
        } else minHorizontalSpacing

        val showDayLabels = visibleDayLabels.isNotEmpty()
        val numWeeksOnScreen = if (showDayLabels) {
            (totalSlots - 1).coerceAtLeast(1)
        } else {
            totalSlots
        }

        val animatedDayLabelsWidth by animateDpAsState(
            targetValue = if (showDayLabels) cellSize + horizontalSpacing else 0.dp,
            animationSpec = tween(300),
            label = "dayLabelsWidth"
        )
        val animatedDayLabelsAlpha by animateFloatAsState(
            targetValue = if (showDayLabels) 1f else 0f,
            animationSpec = tween(300),
            label = "dayLabelsAlpha"
        )

        val totalWeeks = remember(completions, numWeeksOnScreen, isScrollable, minWeeks, isInfinite, currentDateMillis, habit, firstDayOfWeek) {
            val oldestDate = listOfNotNull(
                habit?.getEffectiveStartDateMillis(),
                if (completions.isNotEmpty()) completions.minOf { it.date } else null
            ).minOrNull()
            val weeksDiff = if (oldestDate == null) 0 else {
                val cal = Calendar.getInstance().apply { this.firstDayOfWeek = firstDayOfWeek }

                cal.timeInMillis = oldestDate
                cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val oldestWeekStart = cal.timeInMillis

                cal.timeInMillis = currentDateMillis
                cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                if (cal.timeInMillis > currentDateMillis) cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val currentWeekStart = cal.timeInMillis

                ((currentWeekStart - oldestWeekStart) / (1000L * 60 * 60 * 24 * 7)).toInt() + 1
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
        val currentFirstDayOfWeekMillis = remember(currentDateMillis, firstDayOfWeek) {
            val cal = Calendar.getInstance().apply {
                this.firstDayOfWeek = firstDayOfWeek
                timeInMillis = currentDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                if (timeInMillis > currentDateMillis) add(Calendar.WEEK_OF_YEAR, -1)
            }
            cal.timeInMillis
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (!dayOfWeekLabelsOnRight) {
                if (animatedDayLabelsWidth > 0.dp || showDayLabels) {
                    Box(
                        modifier = Modifier
                            .width(animatedDayLabelsWidth)
                            .graphicsLayer(clip = false),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cellSize + horizontalSpacing)
                                .offset(x = (-5).dp)
                        ) {
                            DayOfWeekLabels(
                                labels = dayOfWeekLabels,
                                visibleDayLabels = visibleDayLabels,
                                cellSize = cellSize,
                                minSpacing = verticalSpacing,
                                monthTopSpacerHeight = animatedMonthTopSpacerHeight,
                                monthRowHeight = animatedMonthRowHeight,
                                monthSpacerHeight = animatedMonthSpacerHeight,
                                firstDayOfWeek = firstDayOfWeek,
                                columnAlpha = animatedDayLabelsAlpha,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(clip = false)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(clip = false)
                        .fadingEdge(lazyListState, isScrollable && showScrollBlur),
                    state = lazyListState,
                    reverseLayout = true,
                    flingBehavior = flingBehavior,
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    userScrollEnabled = isScrollable
                ) {
                    items(count = totalWeeks, key = { it }) { weekIndex ->
                        val weekData = remember(weekIndex, currentFirstDayOfWeekMillis, completionAmounts, todayDayIndex, tz, isScrollable, totalWeeks, habit, habitStartDayIndex, notificationDotRange, firstDayOfWeek) {
                            val cal = Calendar.getInstance()
                            cal.firstDayOfWeek = firstDayOfWeek
                            cal.timeInMillis = currentFirstDayOfWeekMillis
                            cal.add(Calendar.WEEK_OF_YEAR, -weekIndex)
                            val weekStartMillis = cal.timeInMillis

                            val offset = tz.getOffset(weekStartMillis)
                            val weekStartDayIndex = (weekStartMillis + offset) / 86400000L

                            val completed = BooleanArray(7)
                            val future = BooleanArray(7)
                            val ratios = FloatArray(7)
                            var todayIdx = -1

                            val target = habit?.getDailyTarget() ?: 1

                            for (i in 0..6) {
                                val dayIndex = weekStartDayIndex + i
                                val isFuture = dayIndex > todayDayIndex
                                future[i] = isFuture
                                if (dayIndex == todayDayIndex) todayIdx = i

                                if (isFuture || (habitStartDayIndex != null && dayIndex < habitStartDayIndex)) {
                                    completed[i] = false
                                    ratios[i] = 0f
                                } else {
                                    val dbAmount = completionAmounts[dayIndex] ?: 0
                                    val effective = if (habit?.isInverse == true) {
                                        (target - dbAmount).coerceAtLeast(0)
                                    } else {
                                        dbAmount
                                    }
                                    val ratio = (effective.toFloat() / target).coerceIn(0f, 1f)
                                    completed[i] = ratio >= 1f
                                    ratios[i] = ratio
                                }
                            }

                            // Year transition check
                            cal.add(Calendar.DAY_OF_YEAR, 6)
                            val yearEnd = cal.get(Calendar.YEAR)
                            cal.add(Calendar.WEEK_OF_YEAR, -1)
                            val yearPrev = cal.get(Calendar.YEAR)
                            var isStartOfYear = yearEnd != yearPrev
                            var yearDigits = if (isStartOfYear) yearEnd.toString() else null

                            val dots = BooleanArray(7)
                            val cal0 = Calendar.getInstance().apply { this.firstDayOfWeek = firstDayOfWeek }
                            cal0.timeInMillis = currentFirstDayOfWeekMillis
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

                            if (habit?.notificationsEnabled == true) {
                                val habitDays = habit.notificationDays?.split(",")?.toSet() ?: emptySet()

                                val currentWeekStartDayIndex = (currentFirstDayOfWeekMillis + tz.getOffset(currentFirstDayOfWeekMillis)) / 86400000L
                                for (i in 0..6) {
                                    val dIndex = currentWeekStartDayIndex + i
                                    val shouldShow = when (notificationDotRange) {
                                        "future" -> dIndex > todayDayIndex
                                        "today_and_future" -> dIndex >= todayDayIndex
                                        else -> true // "this_week"
                                    }

                                    if (shouldShow) {
                                        val dayCal = Calendar.getInstance().apply {
                                            timeInMillis = currentFirstDayOfWeekMillis
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
                            if (!(!isScrollable && weekIndex == totalWeeks - 1)) {
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
                                dots.toList(),
                                ratios.toList()
                            )
                        }

                        HeatmapWeekColumn(
                            weekData = weekData,
                            habitColor = habitColor,
                            cellSize = cellSize,
                            verticalSpacing = verticalSpacing,
                            horizontalSpacing = horizontalSpacing,
                            monthLabelAlpha = animatedMonthLabelAlpha,
                            monthTopSpacerHeight = animatedMonthTopSpacerHeight,
                            monthRowHeight = animatedMonthRowHeight,
                            monthSpacerHeight = animatedMonthSpacerHeight,
                            yearDividerAlpha = animatedYearDividerAlpha,
                            yearLabelsAlpha = animatedYearLabelsAlpha,
                            notificationDotAlpha = animatedNotificationDotAlpha,
                            animateTileChanges = animateTileChanges,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(300),
                                fadeOutSpec = tween(300),
                                placementSpec = tween(300)
                            )
                        )
                    }
                }
            }

            if (dayOfWeekLabelsOnRight) {
                if (animatedDayLabelsWidth > 0.dp || showDayLabels) {
                    Box(
                        modifier = Modifier
                            .width(animatedDayLabelsWidth)
                            .graphicsLayer(clip = false),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cellSize + horizontalSpacing)
                                .offset(x = 3.5.dp)
                        ) {
                            DayOfWeekLabels(
                                labels = dayOfWeekLabels,
                                visibleDayLabels = visibleDayLabels,
                                cellSize = cellSize,
                                minSpacing = verticalSpacing,
                                monthTopSpacerHeight = animatedMonthTopSpacerHeight,
                                monthRowHeight = animatedMonthRowHeight,
                                monthSpacerHeight = animatedMonthSpacerHeight,
                                firstDayOfWeek = firstDayOfWeek,
                                columnAlpha = animatedDayLabelsAlpha,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekLabels(
    labels: List<String>,
    visibleDayLabels: Set<String>,
    cellSize: Dp,
    minSpacing: Dp,
    monthTopSpacerHeight: Dp = 0.dp,
    monthRowHeight: Dp = 14.dp,
    monthSpacerHeight: Dp = 2.dp,
    firstDayOfWeek: Int = Calendar.MONDAY,
    columnAlpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    val dayValues = getDaysAndDayValues(firstDayOfWeek).second

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (monthTopSpacerHeight > 0.dp) {
            Spacer(Modifier.height(monthTopSpacerHeight))
        }
        if (monthRowHeight > 0.dp) {
            Box(Modifier.height(monthRowHeight))
        }
        if (monthSpacerHeight > 0.dp) {
            Spacer(Modifier.height(monthSpacerHeight))
        }
        Column(verticalArrangement = Arrangement.spacedBy(minSpacing)) {
            labels.forEachIndexed { index, label ->
                val isVisible = dayValues[index] in visibleDayLabels
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isVisible) 0.6f else 0f,
                    animationSpec = tween(300),
                    label = "dayLabelAlpha_$index"
                )
                Box(
                    modifier = Modifier
                        .height(cellSize)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = animatedAlpha * columnAlpha),
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

private fun getDayOfWeekShortLabel(calDay: Int): String {
    return when (calDay) {
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        Calendar.SUNDAY -> "Sun"
        else -> "Mon"
    }
}
