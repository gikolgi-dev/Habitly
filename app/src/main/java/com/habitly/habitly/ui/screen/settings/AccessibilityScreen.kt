/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.LocalAppSettings
import com.habitly.habitly.data.settings.LocalSettingsDataStore
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccessibilityScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore = LocalSettingsDataStore.current,
    onNavigateToReduceMovement: () -> Unit,
    onNavigateToAutoScroll: () -> Unit
) {
    val settings = LocalAppSettings.current
    val currentBorderContrast = settings.borders
    val currentReduceMovement = settings.reduceMovement
    val currentAutoScrollText = settings.autoScrollText
    val vibrationsEnabled = settings.vibrations

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val (scrollState, scrollEnabled) = rememberSettingsScrollState()

    val interactionSource = remember { MutableInteractionSource() }

    val sliderState = rememberSliderState(
        value = currentBorderContrast,
        steps = 19,
        trackRange = 0f..1f
    )

    LaunchedEffect(currentBorderContrast) {
        if (!sliderState.isDragging) {
            sliderState.value = currentBorderContrast
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                        state = sliderState,
                        onValueChange = { newValue ->
                            if (newValue != sliderState.value && vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            sliderState.value = newValue
                            scope.launch {
                                settingsDataStore.setBorders(newValue)
                            }
                        },
                        interactionSource = interactionSource,
                        thumb = {
                            Label(
                                label = {
                                    PlainTooltip(
                                        modifier = Modifier
                                            .sizeIn(45.dp, 25.dp)
                                            .wrapContentWidth()
                                    ) {
                                        Text("%.2f".format(sliderState.value))
                                    }
                                },
                                interactionSource = interactionSource
                            ) {
                                SliderDefaults.Thumb(
                                    interactionSource = interactionSource,
                                    enabled = true
                                )
                            }
                        },
                        track = { state ->
                            SliderDefaults.Track(
                                sliderState = state,
                                trackCornerSize = 8.dp,
                                enabled = true
                            )
                        }
                    )
                }
            }

            SettingsSwitchNavigationItem(
                text = "Reduce movement",
                description = "Minimize the amount of animation and movement in the app",
                checked = currentReduceMovement,
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
                checked = currentAutoScrollText,
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

        Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
    }
}

@Composable
fun ScrollBlurSubScreen(
    settingsDataStore: SettingsDataStore = LocalSettingsDataStore.current,
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val showScrollBlur = settings.showScrollBlur
    val scrollBlurTargets = settings.scrollBlurTargets
    val scrollBlurDetailOnly = settings.scrollBlurDetailOnly
    val vibrationsEnabled = settings.vibrations
    val haptic = LocalHapticFeedback.current

    SettingsSubScreenContainer(modifier = modifier) {
        SettingsSubScreenDescription("Apply a blur effect to specific components while scrolling to improve focus and aesthetics.")

        MainSettingsToggle(
            text = "Use scroll blur",
            checked = showScrollBlur,
            onCheckedChange = { isChecked ->
                scope.launch { settingsDataStore.setshowScrollBlur(isChecked) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCheckboxGroup(
            title = "Targets",
            items = listOf("Heatmap", "Line Chart"),
            selectedItems = scrollBlurTargets,
            enabled = showScrollBlur,
            settingsDataStore = settingsDataStore,
            onToggleItem = { target ->
                val newTargets = if (target in scrollBlurTargets) scrollBlurTargets - target else scrollBlurTargets + target
                scope.launch { settingsDataStore.setScrollBlurTargets(newTargets) }
                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(settingsDataStore = settingsDataStore) {
            SettingsSwitchItem(
                text = "Only show on detail screen",
                checked = scrollBlurDetailOnly,
                enabled = showScrollBlur && "Heatmap" in scrollBlurTargets,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Alone,
                onCheckedChange = { isChecked ->
                    scope.launch { settingsDataStore.setScrollBlurDetailOnly(isChecked) }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                }
            )
        }
    }
}

@Composable
fun ReduceMovementSubScreen(
    settingsDataStore: SettingsDataStore = LocalSettingsDataStore.current,
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val reduceMovement = settings.reduceMovement
    val reduceMovementTargets = settings.reduceMovementTargets
    val vibrationsEnabled = settings.vibrations
    val haptic = LocalHapticFeedback.current

    SettingsSubScreenContainer(modifier = modifier) {
        SettingsSubScreenDescription("Minimize the amount of animation and movement in the app. This can be helpful if you are sensitive to motion or want a more static experience.")

        MainSettingsToggle(
            text = "Reduce movement",
            checked = reduceMovement,
            onCheckedChange = { isChecked ->
                scope.launch { settingsDataStore.setReduceMovement(isChecked) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCheckboxGroup(
            title = "Targets",
            items = listOf("Rotation", "Grid Reactions"),
            selectedItems = reduceMovementTargets,
            enabled = reduceMovement,
            settingsDataStore = settingsDataStore,
            onToggleItem = { target ->
                val newTargets = if (target in reduceMovementTargets) reduceMovementTargets - target else reduceMovementTargets + target
                scope.launch { settingsDataStore.setReduceMovementTargets(newTargets) }
                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        )
    }
}

@Composable
fun AutoScrollSubScreen(
    settingsDataStore: SettingsDataStore = LocalSettingsDataStore.current,
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val autoScrollText = settings.autoScrollText
    val autoScrollTextElements = settings.autoScrollTextElements
    val autoScrollTextScreens = settings.autoScrollTextScreens
    val vibrationsEnabled = settings.vibrations
    val haptic = LocalHapticFeedback.current

    SettingsSubScreenContainer(modifier = modifier) {
        SettingsSubScreenDescription("Automatically scroll overflowing titles and descriptions horizontally from left to right.")

        MainSettingsToggle(
            text = "Auto-scroll text",
            checked = autoScrollText,
            onCheckedChange = { isChecked ->
                scope.launch { settingsDataStore.setAutoScrollText(isChecked) }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCheckboxGroup(
            title = "Elements",
            items = listOf("Title", "Description"),
            selectedItems = autoScrollTextElements,
            enabled = autoScrollText,
            settingsDataStore = settingsDataStore,
            onToggleItem = { element ->
                val newElements = if (element in autoScrollTextElements) autoScrollTextElements - element else autoScrollTextElements + element
                scope.launch { settingsDataStore.setAutoScrollTextElements(newElements) }
                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCheckboxGroup(
            title = "Screens",
            items = listOf("Main Screen", "Detail Screen"),
            selectedItems = autoScrollTextScreens,
            enabled = autoScrollText,
            settingsDataStore = settingsDataStore,
            onToggleItem = { screen ->
                val newScreens = if (screen in autoScrollTextScreens) autoScrollTextScreens - screen else autoScrollTextScreens + screen
                scope.launch { settingsDataStore.setAutoScrollTextScreens(newScreens) }
                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        )
    }
}
