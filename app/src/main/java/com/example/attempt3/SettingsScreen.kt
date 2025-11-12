package com.example.attempt3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit, db: HabitDatabase, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val showConfirmationDialog = remember { mutableStateOf(false) }
    val showSecondConfirmationDialog = remember { mutableStateOf(false) }
    var showAppearanceScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = showAppearanceScreen) {
        showAppearanceScreen = false
    }

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text("Clear all data?") },
            text = { Text("This action is irreversible and will delete all your habits and completions.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showConfirmationDialog.value = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            showConfirmationDialog.value = false
                            showSecondConfirmationDialog.value = true
                        }
                    ) {
                        Text("Clear Data", color = Color.Red)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSecondConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showSecondConfirmationDialog.value = false },
            title = { Text("Are you absolutely sure?") },
            text = { Text("This is your final warning. All data will be lost.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showSecondConfirmationDialog.value = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                db.clearAllTables()
                                withContext(Dispatchers.Main) {
                                    onDismiss()
                                }
                            }
                            showSecondConfirmationDialog.value = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Yes, Delete Everything", color = Color.Red)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showAppearanceScreen) {
                        IconButton(onClick = { showAppearanceScreen = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            AnimatedVisibility(
                visible = !showAppearanceScreen,
                exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)),
                enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300))
            ) {
                LazyColumn {
                    item {
                        ModernSettingsItem(
                            title = "Appearance",
                            subtitle = "Change the look and feel of the app",
                            icon = Icons.Default.Palette,
                            iconBackgroundColor = MaterialTheme.colorScheme.primary,
                            onClick = { showAppearanceScreen = true }
                        )
                    }
                    item {
                        ModernSettingsItem(
                            title = "Clear all data",
                            subtitle = "Delete all habits and their completions",
                            icon = Icons.Default.Delete,
                            iconBackgroundColor = Color.Red,
                            onClick = { showConfirmationDialog.value = true }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAppearanceScreen,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                AppearanceScreen(
                    settingsDataStore = settingsDataStore
                )
            }
        }
    }
}