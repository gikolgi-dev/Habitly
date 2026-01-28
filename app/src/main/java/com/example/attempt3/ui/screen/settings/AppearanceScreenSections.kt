@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.DayOfWeekSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ThemeSection(
    currentTheme: String,
    appearanceTint: Float,
    borderContrast: Float,
    vibrationsEnabled: Boolean,
    showTintDialogPref: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onShowTintDialog: (Float) -> Unit
) {
    val tintInteractionSource = remember { MutableInteractionSource() }
    val isTintDragged = tintInteractionSource.collectIsDraggedAsState().value

    SettingsGroup(
        title = "Theme",
        settingsDataStore = settingsDataStore,
        modifier = Modifier.combinedClickable(
            onLongClick = {
                scope.launch {
                    settingsDataStore.setShowTintDialog(true)
                }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                }
            },
            onClick = {}
        )
    ) {
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
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Set background tint",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = appearanceTint,
                onValueChange = { newValue ->
                    val isFirstTinting = (appearanceTint == 0f && newValue > 0f)
                    if (isFirstTinting && showTintDialogPref) {
                        onShowTintDialog(newValue)
                    } else {
                        scope.launch {
                            settingsDataStore.setAppearanceTint(newValue)
                        }
                    }
                },
                valueRange = 0f..1f,
                steps = 19,
                interactionSource = tintInteractionSource,
                colors = SliderDefaults.colors(
                    activeTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                    inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                thumb = {
                    Layout(
                        content = {
                            if (isTintDragged) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = "%.2f".format(appearanceTint),
                                        modifier = Modifier.padding(4.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            SliderDefaults.Thumb(
                                interactionSource = tintInteractionSource,
                                colors = SliderDefaults.colors(),
                                enabled = true
                            )
                        }
                    ) { measurables, constraints ->
                        val thumbPlaceable = measurables.last().measure(constraints)
                        val indicatorPlaceable = if (isTintDragged) {
                            measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                        } else {
                            null
                        }

                        layout(thumbPlaceable.width, thumbPlaceable.height) {
                            thumbPlaceable.placeRelative(0, 0)
                            indicatorPlaceable?.let {
                                val indicatorY = (thumbPlaceable.height - it.height) / 2
                                val indicatorX = if (appearanceTint > 0.5f) {
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
            text = "Toggle month labels",
            checked = showMonthLabels,
            onCheckedChange = {
                scope.launch { settingsDataStore.setMonthLabels(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        SettingsSwitchItem(
            text = "Toggle year divider",
            checked = showYearDivider,
            onCheckedChange = {
                scope.launch { settingsDataStore.setYearDivider(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        SettingsSwitchItem(
            text = "Toggle year labels",
            checked = showYearLabels,
            onCheckedChange = {
                scope.launch { settingsDataStore.setYearLabels(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "Visible day labels",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Toggle which days are shown on the heatmap's side labels.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
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
                borderAlpha = if (borderContrast>0.05f) borderContrast else 0.05f
            )
        }
    }
}

@Composable
fun AccessibilitySection(
    borderContrast: Float,
    showScrollBlur: Boolean,
    scrollBlurTargets: Set<String>,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = interactionSource.collectIsDraggedAsState().value

    SettingsGroup(title = "Accessibility", settingsDataStore = settingsDataStore) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Set border contrast",
                style = MaterialTheme.typography.bodyLarge
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
        val switchPadding by animateDpAsState(if(showScrollBlur) 0.dp else 4.dp, label = "switchPadding")
        SettingsSwitchItem(
            text = "Toggle scroll blur",
            checked = showScrollBlur,
            onCheckedChange = {
                scope.launch { settingsDataStore.setshowScrollBlur(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            },
            modifier = Modifier.padding(horizontal = 0.dp, vertical = switchPadding)
        )

        AnimatedVisibility(
            visible = showScrollBlur,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                val targets = listOf("Heatmap", "Line Chart")
                targets.forEach { target ->
                    SettingsChildCheckboxItem(
                        text = target,
                        checked = target in scrollBlurTargets,
                        onCheckedChange = { isChecked ->
                            val newTargets = if (isChecked) {
                                scrollBlurTargets + target
                            } else {
                                scrollBlurTargets - target
                            }
                            scope.launch {
                                settingsDataStore.setScrollBlurTargets(newTargets)
                            }
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            }
        }
    }
}