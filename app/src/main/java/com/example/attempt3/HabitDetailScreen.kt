package com.example.attempt3

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.HabitDetailScreen(habit: Habit, habitDao: HabitDao, isArchivedView: Boolean, animatedVisibilityScope: AnimatedVisibilityScope, onDismiss: () -> Unit, onEditHabit: (Habit) -> Unit, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val completions by habitDao.getCompletionsForHabit(habit.id).collectAsState(initial = emptyList())
    val haptic = LocalHapticFeedback.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = false)
    val dayOfWeekLabelsVisible by settingsDataStore.dayOfWeekLabelsVisible.collectAsState(initial = false)
    val dayOfWeekLabelsOnRight by settingsDataStore.dayOfWeekLabelsOnRight.collectAsState(initial = false)
    val showAllDayOfWeekLabels by settingsDataStore.showAllDayOfWeekLabels.collectAsState(initial = false)
    var showDeleteConfirmation by remember { mutableStateOf(false) } // State for delete confirmation dialog
    val animatedColor by animateColorAsState(targetValue = Color(habit.color), animationSpec = tween(durationMillis = 500))


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
                            scope.launch {
                                habitDao.deleteHabit(habit)
                            }
                            showDeleteConfirmation = false
                            onDismiss()
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
            .clickable { onDismiss() },
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(animatedColor.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                animatedColor.copy(borderContrast),
                                RoundedCornerShape(8.dp)
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
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = habit.name.replace(" ", " "),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold ,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                Color.Gray.copy(alpha = borderContrast),
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

                if (habit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    var isExpanded by remember { mutableStateOf(false) }
                    var isOverflowing by remember { mutableStateOf(false) }

                    val isClickable = (isOverflowing || isExpanded)
                    Column(
                        modifier = Modifier
                            .animateContentSize(animationSpec = tween(durationMillis = 300)) // Animate the size change of the Column
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = isClickable
                            ) { isExpanded = !isExpanded }
                    ) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            onTextLayout = { textLayoutResult ->
                                if (!isOverflowing && !isExpanded) {
                                    isOverflowing = textLayoutResult.hasVisualOverflow
                                }
                            }
                        )
                        if (isClickable) {
                            Text(
                                text = if (isExpanded) "Read less" else "Read more",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Heatmap(
                    completions = completions,
                    habitColor = animatedColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    showMonthLabels = showMonthLabels,
                    dayOfWeekLabelsVisible = dayOfWeekLabelsVisible,
                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                    showAllDayOfWeekLabels = showAllDayOfWeekLabels
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) { // Group left-aligned buttons
                        // Edit button in a box
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = borderContrast),
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
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = borderContrast),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    scope.launch {
                                        habitDao.updateHabit(habit.copy(archived = !isArchivedView))
                                    }
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

                    // Delete button (right-aligned), only visible in archived view
                    if (isArchivedView) {
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.8f))
                                .border(
                                    1.dp,
                                    Color.Red,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { showDeleteConfirmation = true }, // Set state to true on click
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Habit",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                HorizontalDivider(
                    //modifier = Modifier.padding(top = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                MonthCalendar(
                    //modifier = Modifier.padding(horizontal = 8.dp),
                    completions = completions,
                    habitColor = animatedColor,
                    onDateClick = { date, _ ->
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        scope.launch {
                            val startOfDay = (date.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endOfDay = (date.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }

                            val completionForDay = completions.find {
                                it.date >= startOfDay.timeInMillis && it.date <= endOfDay.timeInMillis
                            }

                            if (completionForDay != null) {
                                habitDao.deleteCompletionsForHabitOnDay(habit.id, startOfDay.timeInMillis, endOfDay.timeInMillis)
                            } else {
                                val now = Calendar.getInstance()
                                val timezoneOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(now.timeZone.rawOffset.toLong()).toInt()
                                date.set(Calendar.HOUR_OF_DAY, 12)
                                date.set(Calendar.MINUTE, 0)
                                date.set(Calendar.SECOND, 0)

                                val newCompletion = Completion(
                                    id = UUID.randomUUID().toString(),
                                    habitId = habit.id,
                                    date = date.timeInMillis,
                                    timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                                    amountOfCompletions = 1
                                )
                                habitDao.insertCompletion(newCompletion)
                            }
                        }
                    },
                    showMonthLabels = showMonthLabels
                )
            }
        }
    }
}