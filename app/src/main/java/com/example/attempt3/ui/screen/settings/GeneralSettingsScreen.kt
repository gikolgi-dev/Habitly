package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.example.attempt3.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsScreen(
    settingsDataStore: SettingsDataStore
) {
    val scope = rememberCoroutineScope()
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = false)
    val heroCardVisible by settingsDataStore.heroCardVisible.collectAsState(initial = true)
    val heatmapScrolling by settingsDataStore.heatmapScrolling.collectAsState(initial = false)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsGroup(
            title = "General",
            settingsDataStore = settingsDataStore
        ) {
            SettingsSwitchItem(
                text = "Enable vibrations",
                checked = vibrationsEnabled,
                onCheckedChange = {
                    scope.launch {
                        settingsDataStore.setVibrations(it)
                    }
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            )
            HorizontalDivider(
                color = Color.Gray.copy(0.1f),
                modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally)
            )
            SettingsSwitchItem(
                text = "Show welcome card",
                checked = heroCardVisible,
                onCheckedChange = {
                    scope.launch {
                        settingsDataStore.setHeroCardVisible(it)
                    }
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            )
            HorizontalDivider(
                color = Color.Gray.copy(0.1f),
                modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally)
            )
            SettingsSwitchItem(
                text = "Enable heatmap scrolling",
                checked = heatmapScrolling,
                onCheckedChange = {
                    scope.launch {
                        settingsDataStore.setHeatmapScrolling(it)
                    }
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Hour format",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedSelector(
                    options = listOf("12-h", "24-h"),
                    selectedIndex = if (is24Hour) 1 else 0,
                    onSelectionChange = { index ->
                        val newValue = index == 1
                        if (is24Hour != newValue) {
                            scope.launch {
                                settingsDataStore.setIs24Hour(newValue)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}