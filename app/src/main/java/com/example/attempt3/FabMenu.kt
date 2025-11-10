@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun FabMenu(
    modifier: Modifier = Modifier,
    onAddHabit: () -> Unit,
    onShowArchived: () -> Unit,
    onShowSettings: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val items = listOf(
        FabItem(
            text = "New habit",
            icon = Icons.Default.Add,
            onClick = onAddHabit
        ),
        FabItem(
            text = "Archived",
            icon = Icons.Default.Archive,
            onClick = onShowArchived
        ),
        FabItem(
            text = "Settings",
            icon = Icons.Default.Settings,
            onClick = onShowSettings
        )
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it }
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
                    expanded = false
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

data class FabItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)