package com.example.attempt3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(uiState: HabitsUiState, habitDao: HabitDao, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    if (habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Are you sure?") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            habitToDelete?.let {
                                habitDao.deleteHabit(it)
                            }
                            habitToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            },
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Back")
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
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(PaddingValues(bottom = 80.dp)),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                habitsWithCompletions.forEach { habitWithCompletions ->
                                    HabitItemCard(
                                        habit = habitWithCompletions.habit,
                                        isCompleted = false, // Not relevant for archived habits
                                        completions = habitWithCompletions.completions,
                                        showCheckbox = false,
                                        onComplete = { },
                                        onClick = { },
                                        onUnarchive = {
                                            scope.launch {
                                                habitDao.updateHabit(habitWithCompletions.habit.copy(archived = false))
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
