/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.components.DayOfWeekSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ThemeSection(
    currentTheme: String,
    useMaterialTheming: Boolean,
    useHabitColorForCard: Boolean,
    borderContrast: Float,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onNavigateToHabitColor: () -> Unit
) {
    SettingsGroup(
        title = "Theme",
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
    borderContrast: Float,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback
) {
    val heatmapVisibleDays by settingsDataStore.heatmapVisibleDays.collectAsState(initial = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))

    SettingsGroup(title = "Heatmap", settingsDataStore = settingsDataStore) {
        SettingsSwitchItem(
            text = "Month labels",
            description = "Show the names of months above the heatmap",
            checked = showMonthLabels,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Top
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

        SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Bottom) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Visible day labels",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Toggle which days are shown on the heatmap's side labels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
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
                    borderAlpha = borderContrast
                )
            }
        }
    }
}

@Composable
fun AccessibilitySection(
    borderContrast: Float,
    showScrollBlur: Boolean,
    scrollBlurTargets: Set<String>,
    disableAnimations: Boolean,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onNavigateToScrollBlur: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = interactionSource.collectIsDraggedAsState().value

    SettingsGroup(title = "Accessibility", settingsDataStore = settingsDataStore) {
        SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Top) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Set border contrast",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = borderContrast,
                    onValueChange = {
                        scope.launch {
                            settingsDataStore.setBorders(it)
                        }
                    },
                    valueRange = 0f..1f,
                    steps = 19,
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(
                        activeTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                        inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    thumb = {
                        Layout(
                            content = {
                                if (isDragged) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Text(
                                            text = "%.2f".format(borderContrast),
                                            modifier = Modifier.padding(4.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                SliderDefaults.Thumb(
                                    interactionSource = interactionSource,
                                    colors = SliderDefaults.colors(),
                                    enabled = true
                                )
                            }
                        ) { measurables, constraints ->
                            val thumbPlaceable = measurables.last().measure(constraints)
                            val indicatorPlaceable = if (isDragged) {
                                measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                            } else {
                                null
                            }

                            layout(thumbPlaceable.width, thumbPlaceable.height) {
                                thumbPlaceable.placeRelative(0, 0)
                                indicatorPlaceable?.let {
                                    val indicatorY = (thumbPlaceable.height - it.height) / 2
                                    val indicatorX = if (borderContrast > 0.5f) {
                                        -it.width - 8.dp.roundToPx()
                                    } else {
                                        thumbPlaceable.width + 8.dp.roundToPx()
                                    }
                                    it.placeRelative(indicatorX, indicatorY)
                                }
                            }
                        }
                    }
                )
            }
        }
        
        SettingsSwitchNavigationItem(
            text = "Scroll blur",
            description = "Apply a blur effect to the top and bottom of scrolling lists",
            checked = showScrollBlur,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle,
            onCheckedChange = {
                scope.launch { settingsDataStore.setshowScrollBlur(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            },
            onClick = onNavigateToScrollBlur
        )

        SettingsSwitchItem(
            text = "Disable icon rotation",
            description = "Stop habit icons from rotating slowly",
            checked = disableAnimations,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Bottom
        ) {
            scope.launch { settingsDataStore.setDisableAnimations(it) }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
        }
    }
}