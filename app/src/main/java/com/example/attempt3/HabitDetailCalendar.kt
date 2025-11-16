package com.example.attempt3

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CompletionHeatmap(
    completions: List<Completion>,
    habitColor: Color,
    modifier: Modifier = Modifier,
    totalWeeks: Int? = null
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        if (maxWidth <= 0.dp && totalWeeks == null) {
            return@BoxWithConstraints
        }

        val cellSize = 10.dp
        val minSpacing = 4.dp

        val numWeeks = totalWeeks ?: run {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val cellSizePx = with(density) { cellSize.toPx() }
            val minSpacingPx = with(density) { minSpacing.toPx() }
            ((maxWidthPx + minSpacingPx) / (cellSizePx + minSpacingPx)).toInt()
        }

        if (numWeeks <= 0) return@BoxWithConstraints

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

        val today = remember { // Define today here for this Composable
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal
        }

        val daysToShow = remember(numWeeks) {
            val calendar = Calendar.getInstance()
            // Find the Monday of the current week.
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            // Go back the required number of weeks.
            calendar.add(Calendar.WEEK_OF_YEAR, -(numWeeks - 1))

            val days = mutableListOf<Calendar>()
            repeat(numWeeks * 7) {
                val day = calendar.clone() as Calendar
                day.set(Calendar.HOUR_OF_DAY, 0)
                day.set(Calendar.MINUTE, 0)
                day.set(Calendar.SECOND, 0)
                day.set(Calendar.MILLISECOND, 0)
                days.add(day)
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            days
        }

        val daysByWeekday = remember(daysToShow) {
            val weeks = daysToShow.chunked(7)
            // Transpose the list of lists
            if (weeks.isEmpty()) {
                emptyList()
            } else {
                (0..6).map { dayIndex ->
                    weeks.map { week -> week[dayIndex] }
                }
            }
        }

        val arrangement = if (totalWeeks != null) Arrangement.spacedBy(minSpacing) else Arrangement.SpaceBetween

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(minSpacing)
        ) {
            daysByWeekday.forEach { weekDays ->
                Row(
                    modifier = if (totalWeeks == null) Modifier.fillMaxWidth() else Modifier,
                    horizontalArrangement = arrangement
                ) {
                    weekDays.forEach { day ->
                        val isCompleted = completionDates.contains(day.timeInMillis)
                        val isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                        val cellColor = when {
                            isCompleted -> habitColor
                            day.after(today) -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            else -> habitColor.copy(alpha = 0.1f)
                        }

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .aspectRatio(1f)
                                .background(cellColor, RoundedCornerShape(2.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isToday) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCalendar(
    modifier: Modifier = Modifier,
    completions: List<Completion>,
    habitColor: Color,
    onDateClick: (Calendar, Boolean) -> Unit
) {
    var displayedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var dragAmount by remember { mutableFloatStateOf(0f) }

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

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                displayedMonth = (displayedMonth.clone() as Calendar).apply {
                    add(Calendar.MONTH, -1)
                }
            }) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month")
            }

            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = displayedMonth,
                transitionSpec = {
                    if (targetState.after(initialState)) {
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                    } else {
                        slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    }
                },
                label = "Month",
                contentAlignment = Alignment.Center
            ) { month ->
                Text(
                    text = "${
                        month.getDisplayName(
                            Calendar.MONTH,
                            Calendar.LONG,
                            Locale.getDefault()
                        )
                    } ${month.get(Calendar.YEAR)}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }


            val isFutureMonth = remember(displayedMonth) {
                val todayCal = Calendar.getInstance()
                displayedMonth.get(Calendar.YEAR) > todayCal.get(Calendar.YEAR) ||
                        (displayedMonth.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                displayedMonth.get(Calendar.MONTH) >= todayCal.get(Calendar.MONTH))
            }

            IconButton(
                onClick = {
                    if (!isFutureMonth) {
                        displayedMonth = (displayedMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    }
                },
                enabled = !isFutureMonth
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Next Month",
                    tint = if (isFutureMonth) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week header
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.forEach { day ->
                Text(
                    modifier = Modifier.weight(1f),
                    text = day,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center

                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        AnimatedContent(
            targetState = displayedMonth,
            modifier = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, drag ->
                        dragAmount += drag.x
                    },
                    onDragEnd = {
                        if (abs(dragAmount) > 50) { // Swipe threshold
                            if (dragAmount > 0) {
                                displayedMonth = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                            } else {
                                val todayCal = Calendar.getInstance()
                                val isFuture = displayedMonth.get(Calendar.YEAR) > todayCal.get(Calendar.YEAR) ||
                                        (displayedMonth.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                                displayedMonth.get(Calendar.MONTH) >= todayCal.get(Calendar.MONTH))
                                if (!isFuture) {
                                    displayedMonth = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                }
                            }
                        }
                        dragAmount = 0f
                    }
                )
            },
            transitionSpec = {
                if (targetState.after(initialState)) {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                }
            }, label = "Calendar"
        ) { month ->

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val currentMonth = month.get(Calendar.MONTH)
                val currentYear = month.get(Calendar.YEAR)

                val firstDayOfMonthOffset = (Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear); set(Calendar.MONTH, currentMonth); set(Calendar.DAY_OF_MONTH, 1)
                }.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

                val daysInMonth = Calendar.getInstance()
                    .apply { set(Calendar.YEAR, currentYear); set(Calendar.MONTH, currentMonth) }
                    .getActualMaximum(Calendar.DAY_OF_MONTH)

                val totalCells = firstDayOfMonthOffset + daysInMonth
                val numRows = (totalCells + 6) / 7

                for (week in 0 until numRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (dayOfWeek in 0..6) {
                            val dayOfMonth = week * 7 + dayOfWeek - firstDayOfMonthOffset + 1
                            val day = Calendar.getInstance().apply {
                                clear()
                                set(Calendar.YEAR, currentYear)
                                set(Calendar.MONTH, currentMonth)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }

                            val dayStartMillis = day.timeInMillis
                            val isCompleted = completionDates.contains(dayStartMillis)
                            val isInCurrentMonth = day.get(Calendar.MONTH) == currentMonth
                            val isToday = dayStartMillis == today.timeInMillis
                            val isAfterToday = day.after(today)

                            val targetCellColor = when {
                                isCompleted -> habitColor.copy(alpha = if (isInCurrentMonth) 1f else 0.6f)
                                else -> Color.Transparent
                            }
                            val cellColor by animateColorAsState(
                                targetValue = targetCellColor,
                                animationSpec = tween(durationMillis = 100),
                                label = "cell color"
                            )

                            val textColor = when {
                                isCompleted -> if (habitColor.isBright()) Color.Black else Color.White
                                isAfterToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                !isInCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellColor)
                                    .border(
                                        width = if (isToday && !isCompleted) 1.dp else 0.dp,
                                        color = if (isToday && !isCompleted) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isAfterToday) {
                                        onDateClick(day, isInCurrentMonth)
                                        if (!isInCurrentMonth) {
                                            displayedMonth = day
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${day.get(Calendar.DAY_OF_MONTH)}",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
