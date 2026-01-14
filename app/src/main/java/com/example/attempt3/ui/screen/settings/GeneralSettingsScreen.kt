package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val heatmapScrolling by settingsDataStore.heatmapScrolling.collectAsState(initial = false)
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
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = vibrationsEnabled,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setVibrations(!vibrationsEnabled)
                                    }
                                    haptic.performHapticFeedback(if (!vibrationsEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
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
                    HorizontalDivider(color = Color.Gray.copy(0.1f), modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = heroCardVisible,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setHeroCardVisible(!heroCardVisible)
                                    }
                                    haptic.performHapticFeedback(if (!heroCardVisible) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = heroCardVisible,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setHeroCardVisible(it)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        Text(
                            text = "Show welcome card",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    HorizontalDivider(color = Color.Gray.copy(0.1f), modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = heatmapScrolling,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setHeatmapScrolling(!heatmapScrolling)
                                    }
                                    haptic.performHapticFeedback(if (!heatmapScrolling) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = heatmapScrolling,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setHeatmapScrolling(it)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        Text(
                            text = "Enable heatmap scrolling",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
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
                        HourFormatSelector(
                            is24Hour = is24Hour,
                            onSelectionChange = {
                                if (is24Hour != it) {
                                    scope.launch {
                                        settingsDataStore.setIs24Hour(it)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourFormatSelector(
    is24Hour: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("12-h", "24-h")
    val selectedIndex = if (is24Hour) 1 else 0

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedIndex])
                    .fillMaxHeight()
                    .padding(all = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .zIndex(-1f)
            )
        },
        divider = {}
    ) {
        options.forEachIndexed { index, text ->
            val selected = selectedIndex == index
            Tab(
                selected = selected,
                onClick = { onSelectionChange(index == 1) },
                text = { Text(text = text) },
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}