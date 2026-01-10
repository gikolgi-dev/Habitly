@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HabitDetailScreen(
    habitWithCompletions: HabitWithCompletions,
    viewModel: HabitViewModel,
    isArchivedView: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismiss: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    settingsDataStore: SettingsDataStore,
    borderContrast: Float
) {
    val haptic = LocalHapticFeedback.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = false)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = true)
    val dayOfWeekLabelsVisible by settingsDataStore.dayOfWeekLabelsVisible.collectAsState(initial = false)
    val dayOfWeekLabelsOnRight by settingsDataStore.dayOfWeekLabelsOnRight.collectAsState(initial = false)
    val showAllDayOfWeekLabels by settingsDataStore.showAllDayOfWeekLabels.collectAsState(initial = false)
    var showDeleteConfirmation by remember { mutableStateOf(false) } // State for delete confirmation dialog
    val habit = habitWithCompletions.habit
    val completions = habitWithCompletions.completions
    val animatedColor by animateColorAsState(targetValue = Color(habit.color), animationSpec = tween(durationMillis = 500))
    val streak = remember(habit, completions) { calculateStreak(habit, completions) }


    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Are you sure?") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
            confirmButton = {}, // Keep this empty, we will handle buttons manually below
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirmation = false }
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onDismiss()
                            showDeleteConfirmation = false
                            viewModel.deleteHabit(habit)
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .sharedElement(
                    rememberSharedContentState(key = "card-${habit.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxWidth(0.9f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {/* Prevents click from dismissing the dialog if clicked inside the card */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                Color.Gray.copy(alpha = borderContrast)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = habitIconMap[habit.icon] ?: Icons.Default.Refresh // Fallback icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(animatedColor.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                animatedColor.copy(alpha = borderContrast),
                                MaterialShapes.Cookie12Sided.toShape()
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = habit.icon,
                            modifier = Modifier.size(40.dp),
                            tint = animatedColor.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                    HabitTitleAndDescription(habit = habit, isDetailView = true, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                            .border(
                                1.dp,
                                Color.Gray.copy(alpha = borderContrast*2),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                
                Heatmap(
                    completions = completions,
                    habitColor = animatedColor,
                    modifier = Modifier.fillMaxWidth().padding(top = if (showMonthLabels) 0.dp else 8.dp),
                    showMonthLabels = showMonthLabels,
                    dayOfWeekLabelsVisible = dayOfWeekLabelsVisible,
                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                    showAllDayOfWeekLabels = showAllDayOfWeekLabels,
                    showYearDivider = showYearDivider
                )

                //Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) { // Group left-aligned buttons
                        // Edit button in a box
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = borderContrast*2),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onEditHabit(habit)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Habit",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.size(6.dp)) // Space between edit and archive/unarchive

                        // Archive/Unarchive button in a box
                        val archiveIcon = if (isArchivedView) Icons.Default.Restore else Icons.Default.Archive
                        val archiveContentDescription = if (isArchivedView) "Un-archive Habit" else "Archive Habit"
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = borderContrast*2),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    viewModel.updateHabit(habit.copy(archived = !isArchivedView))
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = archiveIcon,
                                contentDescription = archiveContentDescription,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val intervalText = when (habit.intervalUnit) {
                            "day" -> "Daily"
                            else -> "${habit.completionsPerInterval}/${habit.intervalUnit.replaceFirstChar { it.uppercase() }}"
                        }
                        Box(
                            modifier = Modifier
                                .height(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = borderContrast * 2),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = intervalText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Box(
                            modifier = Modifier
                                .size(height = 35.dp, width = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFC9920).copy(alpha = 0.4f))
                                .border(
                                    1.dp,
                                    Color(0xFFFC9920).copy(alpha = borderContrast * 2),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("$streak")
                                Spacer(modifier = Modifier.size(2.dp))
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFC9920)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider( thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                MonthCalendar(
                    //modifier = Modifier.padding(horizontal = 8.dp),
                    completions = completions,
                    habitColor = animatedColor,
                    onDateClick = { date, isCompleted ->
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.toggleCompletion(habit, date, isCompleted)
                    }
                )
            }
        }
    }
}

private fun calculateStreak(habit: Habit, completions: List<Completion>): Int {
    if (completions.isEmpty()) return 0

    val sortedDates = completions.map { it.date }.sortedDescending()
    
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val completedDays = sortedDates.map { date ->
        val c = Calendar.getInstance().apply { timeInMillis = date }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }.toSet()

    return when (habit.intervalUnit) {
        "day" -> {
            var currentStreak = 0
            val checkC = today.clone() as Calendar

            // Check today
            if (completedDays.contains(checkC.timeInMillis)) {
                currentStreak++
            }

            // Move to yesterday
            checkC.add(Calendar.DAY_OF_YEAR, -1)

            if (currentStreak == 0) {
                // If today was not completed, we give a chance to yesterday
                if (completedDays.contains(checkC.timeInMillis)) {
                    currentStreak++
                    checkC.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    return 0
                }
            }

            // Count consecutive past days
            while (completedDays.contains(checkC.timeInMillis)) {
                currentStreak++
                checkC.add(Calendar.DAY_OF_YEAR, -1)
            }
            currentStreak
        }
        "week" -> {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            
            val firstDayOfWeek = c.firstDayOfWeek
            val startOfCurrentWeek = c.clone() as Calendar
            while (startOfCurrentWeek.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                startOfCurrentWeek.add(Calendar.DAY_OF_YEAR, -1)
            }
            
            val diffInMillis = c.timeInMillis - startOfCurrentWeek.timeInMillis
            val daysPassed = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt() + 1
            
            var completionsInCurrentWeek = 0
            val tempC = startOfCurrentWeek.clone() as Calendar
            for (i in 0 until daysPassed) {
                if (completedDays.contains(tempC.timeInMillis)) {
                    completionsInCurrentWeek++
                }
                tempC.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val target = habit.completionsPerInterval
            val allowedMisses = 7 - target
            val currentMisses = daysPassed - completionsInCurrentWeek
            
            if (currentMisses <= allowedMisses) {
                var streak = completionsInCurrentWeek
                val checkWeekStart = startOfCurrentWeek.clone() as Calendar
                checkWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
                
                while (true) {
                    var weekCompletions = 0
                    val dayIterator = checkWeekStart.clone() as Calendar
                    for (i in 0 until 7) {
                        if (completedDays.contains(dayIterator.timeInMillis)) {
                            weekCompletions++
                        }
                        dayIterator.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    if (weekCompletions >= target) {
                        streak += weekCompletions
                        checkWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
                streak
            } else {
                var tail = 0
                val scanC = c.clone() as Calendar
                var counting = false
                while (scanC.timeInMillis >= startOfCurrentWeek.timeInMillis) {
                    if (completedDays.contains(scanC.timeInMillis)) {
                        counting = true
                        tail++
                    } else {
                        if (counting) break
                    }
                    scanC.add(Calendar.DAY_OF_YEAR, -1)
                }
                tail
            }
        }
        "month" -> {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            
            val startOfCurrentMonth = c.clone() as Calendar
            startOfCurrentMonth.set(Calendar.DAY_OF_MONTH, 1)
            
            val daysPassed = c.get(Calendar.DAY_OF_MONTH)

            var completionsInCurrentMonth = 0
            val tempC = startOfCurrentMonth.clone() as Calendar
            for (i in 0 until daysPassed) {
                if (completedDays.contains(tempC.timeInMillis)) {
                    completionsInCurrentMonth++
                }
                tempC.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            val target = habit.completionsPerInterval
            val allowedMisses = daysInMonth - target
            val currentMisses = daysPassed - completionsInCurrentMonth
            
            if (currentMisses <= allowedMisses) {
                var streak = completionsInCurrentMonth
                val checkMonthStart = startOfCurrentMonth.clone() as Calendar
                checkMonthStart.add(Calendar.MONTH, -1)
                
                while (true) {
                    val prevMonthDays = checkMonthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                    var monthCompletions = 0
                    val dayIterator = checkMonthStart.clone() as Calendar
                    for (i in 0 until prevMonthDays) {
                        if (completedDays.contains(dayIterator.timeInMillis)) {
                            monthCompletions++
                        }
                        dayIterator.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    if (monthCompletions >= target) {
                        streak += monthCompletions
                        checkMonthStart.add(Calendar.MONTH, -1)
                    } else {
                        break
                    }
                }
                streak
            } else {
                var tail = 0
                val scanC = c.clone() as Calendar
                var counting = false
                while (scanC.timeInMillis >= startOfCurrentMonth.timeInMillis) {
                    if (completedDays.contains(scanC.timeInMillis)) {
                        counting = true
                        tail++
                    } else {
                        if (counting) break
                    }
                    scanC.add(Calendar.DAY_OF_YEAR, -1)
                }
                tail
            }
        }
        else -> 0
    }
}
