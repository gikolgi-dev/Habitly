/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore,
    onNavigateToHabitColor: () -> Unit,
    onNavigateToHeatmap: () -> Unit
) {
    val currentTheme by settingsDataStore.theme.collectAsState(initial = DefaultSettings.THEME)
    val useMaterialTheming by settingsDataStore.useMaterialTheming.collectAsState(initial = DefaultSettings.USE_MATERIAL_THEMING)
    val useHabitColorForCard by settingsDataStore.useHabitColorForCard.collectAsState(initial = DefaultSettings.USE_HABIT_COLOR_FOR_CARD)
    val lineChartYearDivider by settingsDataStore.lineChartYearDivider.collectAsState(initial = DefaultSettings.LINE_CHART_YEAR_DIVIDER)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroup(
                title = "Styling",
                settingsDataStore = settingsDataStore
            ) {
                SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Top) {
                    Column {
                        val themes = listOf("Light", "Dark", "System")
                        themes.forEach { theme ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (theme.lowercase() == currentTheme),
                                        onClick = {
                                            scope.launch {
                                                settingsDataStore.setTheme(theme.lowercase())
                                            }
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    )
                                    .padding(start = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (theme.lowercase() == currentTheme),
                                    onClick = {
                                        scope.launch {
                                            settingsDataStore.setTheme(theme.lowercase())
                                        }
                                        if (vibrationsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        }
                                    }
                                )
                                Text(
                                    text = theme,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                SettingsSwitchItem(
                    text = "Use material theming",
                    description = "Match the background colors with your system's dynamic color theme",
                    checked = useMaterialTheming,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Middle
                ) {
                    scope.launch { settingsDataStore.setUseMaterialTheming(it) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }

                SettingsSwitchNavigationItem(
                    text = "Habit color accents",
                    description = "Add color accents to habit related elements",
                    checked = useHabitColorForCard,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Bottom,
                    onCheckedChange = {
                        scope.launch { settingsDataStore.setUseHabitColorForCard(it) }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        }
                    },
                    onClick = onNavigateToHabitColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(
                settingsDataStore = settingsDataStore
            ) {
                SettingsNavigationItem(
                    text = "Heatmap",
                    description = "Customize heatmap appearance and visible labels",
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Alone,
                    onClick = onNavigateToHeatmap
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(title = "Line Chart", settingsDataStore = settingsDataStore) {
                SettingsSwitchItem(
                    text = "Year divider",
                    description = "Add a visual gap between different years on the line chart",
                    checked = lineChartYearDivider,
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Alone
                ) {
                    scope.launch { settingsDataStore.setLineChartYearDivider(it) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}
