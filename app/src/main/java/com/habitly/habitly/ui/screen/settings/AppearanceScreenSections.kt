/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.ui.components.DayOfWeekSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun ThemeSection(
    currentTheme: String,
    useMaterialTheming: Boolean,
    useHabitColorForCard: Boolean,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onNavigateToHabitColor: () -> Unit
) {
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
}

@Composable
fun HeatmapSection(
    showMonthLabels: Boolean,
    showYearDivider: Boolean,
    showYearLabels: Boolean,
    heatmapNotificationDot: Boolean,
    borderContrast: Float,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    useHabitColorForCard: Boolean,
    onNavigateToHeatmapNotificationDot: () -> Unit,
    onNavigateToHabitColor: () -> Unit,
    title: String? = "Heatmap",
    heatmapScrolling: Boolean = false
) {
    val heatmapVisibleDays by settingsDataStore.heatmapVisibleDays.collectAsState(initial = emptySet())
    val firstDayOfWeekCalendar by settingsDataStore.firstDayOfWeekCalendar.collectAsState(initial = Calendar.MONDAY)

    SettingsGroup(title = title, settingsDataStore = settingsDataStore) {
        SettingsSwitchNavigationItem(
            text = "Habit color accents",
            description = "Add color accents to habit related elements",
            checked = useHabitColorForCard,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Top,
            onCheckedChange = {
                scope.launch { settingsDataStore.setUseHabitColorForCard(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            },
            onClick = onNavigateToHabitColor
        )
        SettingsSwitchItem(
            text = "Month labels",
            description = "Show the names of months above the heatmap",
            checked = showMonthLabels,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle
        ) {
            scope.launch { settingsDataStore.setMonthLabels(it) }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
        }

        SettingsSwitchItem(
            text = "Year divider",
            description = "Add a visual gap between different years",
            checked = showYearDivider,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle
        ) {
            scope.launch { settingsDataStore.setYearDivider(it) }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
        }

        SettingsSwitchItem(
            text = "Year labels",
            description = "Display the year next to the heatmap",
            checked = showYearLabels,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle
        ) {
            scope.launch { settingsDataStore.setYearLabels(it) }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
        }

        SettingsSwitchNavigationItem(
            text = "Notification indicator",
            description = "Show a dot on days with scheduled notifications",
            checked = heatmapNotificationDot,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle,
            onCheckedChange = {
                scope.launch { settingsDataStore.setHeatmapNotificationDot(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            },
            onClick = onNavigateToHeatmapNotificationDot
        )

        SettingsSwitchItem(
            text = "Heatmap scrolling",
            description = "Allow horizontal scrolling on the heatmap",
            checked = heatmapScrolling,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle
        ) {
            scope.launch { settingsDataStore.setHeatmapScrolling(it) }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
        }

        SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Bottom) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = 16.dp
                )
            ) {
                Text(
                    text = "Visible day labels",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Toggle which days are shown on the heatmap's side labels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                DayOfWeekSelector(
                    selectedDays = heatmapVisibleDays,
                    onDaySelected = { day ->
                        val newDays = if (heatmapVisibleDays.contains(day)) heatmapVisibleDays - day else heatmapVisibleDays + day
                        scope.launch { settingsDataStore.setHeatmapVisibleDays(newDays) }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    },
                    firstDayOfWeek = firstDayOfWeekCalendar,
                    borderAlpha = borderContrast
                )
            }
        }
    }
}

@Composable
fun LineChartSection(
    lineChartYearDivider: Boolean,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback
) {
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
}
