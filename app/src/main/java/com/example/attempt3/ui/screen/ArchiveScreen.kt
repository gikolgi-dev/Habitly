package com.example.attempt3.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.Database.Habit
import com.example.attempt3.data.Database.HabitDao
import com.example.attempt3.data.Database.HabitsUiState
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.HabitItemCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(uiState: HabitsUiState, habitDao: HabitDao, onBack: () -> Unit, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = false)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = true)
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = true)
    val heatmapVisibleDays by settingsDataStore.heatmapVisibleDays.collectAsState(initial = emptySet())
    val dayOfWeekLabelsOnRight by settingsDataStore.dayOfWeekLabelsOnRight.collectAsState(initial = false)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val haptic = LocalHapticFeedback.current

    if (habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Are you sure?") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.",color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { habitToDelete = null },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                habitToDelete?.let {
                                    habitDao.deleteHabit(it)
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Archived Habits", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = {
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            }
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
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
                                        borderContrast = borderContrast,
                                        onComplete = { },
                                        onClick = { },
                                        onUnarchive = {
                                            scope.launch {
                                                habitDao.updateHabit(
                                                    habitWithCompletions.habit.copy(
                                                        archived = false
                                                    )
                                                )
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
}