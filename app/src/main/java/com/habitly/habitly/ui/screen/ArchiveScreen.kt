/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitDao
import com.habitly.habitly.data.Database.HabitsUiState
import com.habitly.habitly.notifications.NotificationScheduler
import com.habitly.habitly.ui.AppBackButton
import com.habitly.habitly.ui.HabitItemCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    uiState: HabitsUiState,
    habitDao: HabitDao,
    onBack: () -> Unit,
    borderContrast: Float,
    showMonthLabels: Boolean,
    showYearDivider: Boolean,
    showYearLabels: Boolean,
    heatmapNotificationDot: Boolean,
    heatmapNotificationDotRange: String,
    heatmapVisibleDays: Set<String>,
    dayOfWeekLabelsOnRight: Boolean,
    vibrationsEnabled: Boolean,
    useHabitColor: Boolean,
    disableAnimations: Boolean,
    heatmapWeeks: Int = 0,
    heatmapInfinite: Boolean = false,
    currentDateMillis: Long = System.currentTimeMillis()
) {
    val scope = rememberCoroutineScope()
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val notificationScheduler = remember { NotificationScheduler(context) }

    if (habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Are you sure?") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.",color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { habitToDelete = null },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                habitToDelete?.let {
                                    habitDao.deleteHabit(it)
                                    notificationScheduler.cancelNotification(it)
                                }
                                habitToDelete = null
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = null,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Archived Habits", fontWeight = FontWeight.SemiBold) },
                actions = {
                    AppBackButton(
                        onBack = {
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            }
                            onBack()
                        },
                        borderContrast = borderContrast,
                        icon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AnimatedVisibility(
                    visible = uiState is HabitsUiState.Success,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val habitsWithCompletions = (uiState as? HabitsUiState.Success)?.habits ?: emptyList()

                    if (habitsWithCompletions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No archived habits.")
                        }
                    } else {
                        val lazyListState = rememberLazyListState()

                        // Add haptic feedback when scrolling between habits
                        LaunchedEffect(lazyListState) {
                            snapshotFlow { lazyListState.firstVisibleItemIndex }.collect {
                                if (vibrationsEnabled && habitsWithCompletions.isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(habitsWithCompletions, key = { it.habit.id }) { habitWithCompletions ->
                                HabitItemCard(
                                    habit = habitWithCompletions.habit,
                                    isCompleted = false, // Not relevant for archived habits
                                    completions = habitWithCompletions.completions,
                                    showCheckbox = false,
                                    showMonthLabels = showMonthLabels,
                                    visibleDayLabels = heatmapVisibleDays,
                                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                                    showYearDivider = showYearDivider,
                                    showYearLabels = showYearLabels,
                                    heatmapNotificationDot = heatmapNotificationDot,
                                    heatmapNotificationDotRange = heatmapNotificationDotRange,
                                    borderContrast = borderContrast,
                                    useHabitColor = useHabitColor,
                                    disableAnimations = disableAnimations,
                                    heatmapWeeks = heatmapWeeks,
                                    heatmapInfinite = heatmapInfinite,
                                    currentDateMillis = currentDateMillis,
                                    onComplete = { },
                                    onClick = { },
                                    onUnarchive = {
                                        scope.launch {
                                            val restoredHabit = habitWithCompletions.habit.copy(archived = false)
                                            habitDao.updateHabit(restoredHabit)
                                            notificationScheduler.scheduleNotification(restoredHabit)
                                        }
                                    },
                                    onDelete = {
                                        habitToDelete = habitWithCompletions.habit
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
