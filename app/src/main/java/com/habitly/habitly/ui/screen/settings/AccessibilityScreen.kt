/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AccessibilityScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore,
    onNavigateToScrollBlur: () -> Unit,
    onNavigateToReduceMovement: () -> Unit,
    onNavigateToAutoScroll: () -> Unit
) {
    val currentBorderContrast by settingsDataStore.borders.collectAsState(initial = DefaultSettings.BORDERS)
    val currentShowScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = DefaultSettings.SHOW_SCROLL_BLUR)
    val currentReduceMovement by settingsDataStore.reduceMovement.collectAsState(initial = DefaultSettings.REDUCE_MOVEMENT)
    val currentAutoScrollText by settingsDataStore.autoScrollText.collectAsState(initial = DefaultSettings.AUTO_SCROLL_TEXT)
    val vibr by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccessibilitySection(
            borderContrast = currentBorderContrast,
            showScrollBlur = currentShowScrollBlur,
            reduceMovement = currentReduceMovement,
            vibrationsEnabled = vibr,
            settingsDataStore = settingsDataStore,
            scope = scope,
            haptic = haptic,
            onNavigateToScrollBlur = onNavigateToScrollBlur,
            onNavigateToReduceMovement = onNavigateToReduceMovement,
            autoScrollText = currentAutoScrollText,
            onNavigateToAutoScroll = onNavigateToAutoScroll
        )

        Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
    }
}

@Composable
fun AccessibilitySection(
    borderContrast: Float,
    showScrollBlur: Boolean,
    reduceMovement: Boolean,
    vibrationsEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onNavigateToScrollBlur: () -> Unit,
    onNavigateToReduceMovement: () -> Unit,
    autoScrollText: Boolean,
    onNavigateToAutoScroll: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = interactionSource.collectIsDraggedAsState().value

    SettingsGroup(title = "", settingsDataStore = settingsDataStore) {
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

        SettingsSwitchNavigationItem(
            text = "Reduce movement",
            description = "Minimize the amount of animation and movement in the app",
            checked = reduceMovement,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Middle,
            onCheckedChange = {
                scope.launch { settingsDataStore.setReduceMovement(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            },
            onClick = onNavigateToReduceMovement
        )

        SettingsSwitchNavigationItem(
            text = "Auto-scroll text",
            description = "Scroll overflowing titles and descriptions horizontally",
            checked = autoScrollText,
            settingsDataStore = settingsDataStore,
            position = SettingsItemPosition.Bottom,
            onCheckedChange = {
                scope.launch { settingsDataStore.setAutoScrollText(it) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            },
            onClick = onNavigateToAutoScroll
        )
    }
}
