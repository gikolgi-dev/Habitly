package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsScreen(modifier: Modifier = Modifier, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = 0.25f),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = vibrationsEnabled,
                            onClick = {
                                scope.launch {
                                    settingsDataStore.setVibrations(!vibrationsEnabled)
                                }
                                haptic.performHapticFeedback(if(!vibrationsEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = vibrationsEnabled,
                        modifier = Modifier.padding(start = 16.dp),
                        onCheckedChange = {
                            scope.launch {
                                settingsDataStore.setVibrations(it)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                    Text(
                        text = "Enable vibrations",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}