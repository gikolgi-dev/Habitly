@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun FabMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddHabit: () -> Unit,
    onShowArchived: () -> Unit,
    onShowSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val items = listOf(
        FabItem(
            text = "Settings",
            icon = Icons.Default.Settings,
            onClick = onShowSettings
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
                        haptic.performHapticFeedback(if(expanded) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    }

                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                        contentDescription = if (expanded) "Close menu" else "Open menu",
                        tint = if (expanded) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            },
        ) {
            items.forEach { item ->
                FloatingActionButtonMenuItem(
                    onClick = {
                        item.onClick()
                        onExpandedChange(false)
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
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