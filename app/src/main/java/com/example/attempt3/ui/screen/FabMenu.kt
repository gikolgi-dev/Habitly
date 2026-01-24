@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.attempt3.data.settings.SettingsDataStore

@Composable
fun FabMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddHabit: () -> Unit,
    onShowArchived: () -> Unit,
    onShowSettings: () -> Unit,
    onShowReorder: () -> Unit,
    onShowStatistics: () -> Unit,
    settingsDataStore: SettingsDataStore
) {
    val haptic = LocalHapticFeedback.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val items = listOf(
        FabItem(
            text = "Settings",
            icon = Icons.Default.Settings,
            onClick = onShowSettings
        ),
        FabItem(
            text = "Statistics",
            icon = Icons.AutoMirrored.Filled.ShowChart,
            onClick = onShowStatistics
        ),
        FabItem(
            text = "Reorder",
            icon = Icons.Default.Menu,
            onClick = onShowReorder
        ),
        FabItem(
            text = "Archived",
            icon = Icons.Default.Archive,
            onClick = onShowArchived
        ),
        FabItem(
            text = "New habit",
            icon = Icons.Default.Add,
            onClick = onAddHabit
        ),

        )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButtonMenu(
            expanded = expanded,
            button = {
                ToggleFloatingActionButton(
                    checked = expanded,
                    onCheckedChange = {
                        onExpandedChange(it)
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(if(expanded) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                        }
                    }

                ) {
                    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "fab_icon_rotation")
                    Crossfade(targetState = expanded, label = "fab_icon_crossfade") { isExpanded ->
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = if (isExpanded) "Close menu" else "Open menu",
                            modifier = Modifier.rotate(rotation),
                            tint = if (isExpanded) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
            },
        ) {
            items.forEach { item ->
                FloatingActionButtonMenuItem(
                    onClick = {
                        item.onClick()
                        onExpandedChange(false)
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        }
                    },
                    text = { Text(item.text) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.text
                        )
                    }
                )
            }
        }
    }
}

data class FabItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)