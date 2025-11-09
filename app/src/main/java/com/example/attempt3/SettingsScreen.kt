package com.example.attempt3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit, db: HabitDatabase) {
    val scope = rememberCoroutineScope()
    val showConfirmationDialog = remember { mutableStateOf(false) }
    val showSecondConfirmationDialog = remember { mutableStateOf(false) }

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
                        Text("Clear Data",color = Color.Red)
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
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
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
}