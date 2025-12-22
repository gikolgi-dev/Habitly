@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(modifier: Modifier = Modifier, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsDataStore.theme.collectAsState(initial = "system")
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = true)
    val showDayLabels by settingsDataStore.dayOfWeekLabelsVisible.collectAsState(initial = true)
    val showAllDayOfWeekLabels by settingsDataStore.showAllDayOfWeekLabels.collectAsState(initial = true)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val appearanceTint by settingsDataStore.appearanceTint.collectAsState(initial = 0.08f)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val tintInteractionSource = remember { MutableInteractionSource() }
    val isTintDragged by tintInteractionSource.collectIsDraggedAsState()


    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = modifier) {
                    val themes = listOf("Light", "Dark", "System")
                    themes.forEach { theme ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (theme.lowercase() == currentTheme), // (1)
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
                            onValueChange = {
                                scope.launch {
                                    settingsDataStore.setAppearanceTint(it)
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Heatmap",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
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
                                selected = showMonthLabels,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setMonthLabels(!showMonthLabels)
                                    }
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(if (!showMonthLabels) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                    }
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showMonthLabels,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setMonthLabels(it)
                                }
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                        Text(
                            text = "Toggle month labels",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    val dayLabelDisplay = when {
                        !showDayLabels -> DayLabelDisplayOptions.Off
                        !showAllDayOfWeekLabels -> DayLabelDisplayOptions.Some
                        else -> DayLabelDisplayOptions.All
                    }

                    DayLabelSelector(
                        selectedOption = dayLabelDisplay,
                        onOptionSelected = {
                            scope.launch {
                                when (it) {
                                    DayLabelDisplayOptions.Off -> {
                                        settingsDataStore.setDayOfWeekLabelsVisible(false)
                                    }
                                    DayLabelDisplayOptions.Some -> {
                                        settingsDataStore.setDayOfWeekLabelsVisible(true)
                                        settingsDataStore.setShowAllDayOfWeekLabels(false)
                                    }
                                    DayLabelDisplayOptions.All -> {
                                        settingsDataStore.setDayOfWeekLabelsVisible(true)
                                        settingsDataStore.setShowAllDayOfWeekLabels(true)
                                    }
                                }
                            }
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Accessibility",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ){
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
            }
        }
    }
}