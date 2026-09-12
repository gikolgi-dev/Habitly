/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val (scrollState, scrollEnabled) = rememberSettingsScrollState()

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = interactionSource.collectIsDraggedAsState().value

    val sliderState = remember {
        SliderState(
            value = currentBorderContrast,
            steps = 19,
            trackRange = 0f..1f
        )
    }

    LaunchedEffect(currentBorderContrast) {
        if (!isDragged) {
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
                            sliderState.value = newValue
                            scope.launch {
                                settingsDataStore.setBorders(newValue)
                            }
                        },
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(
                            activeTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                            inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        thumb = { thumbState ->
                            Layout(
                                content = {
                                    if (isDragged) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = "%.2f".format(thumbState.value),
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
                                        val indicatorX = if (thumbState.value > 0.5f) {
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
                checked = currentShowScrollBlur,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Middle,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setshowScrollBlur(it) }
                    if (vibr) {
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                },
                onClick = onNavigateToScrollBlur
            )

            SettingsSwitchNavigationItem(
                text = "Reduce movement",
                description = "Minimize the amount of animation and movement in the app",
                checked = currentReduceMovement,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Middle,
                onCheckedChange = {
                    scope.launch { settingsDataStore.setReduceMovement(it) }
                    if (vibr) {
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
                    if (vibr) {
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
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = DefaultSettings.SHOW_SCROLL_BLUR)
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(
        initial = DefaultSettings.SCROLL_BLUR_TARGETS.split(',').filter { it.isNotEmpty() }.toSet()
    )
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
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
    }
}

@Composable
fun ReduceMovementSubScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val reduceMovement by settingsDataStore.reduceMovement.collectAsState(initial = DefaultSettings.REDUCE_MOVEMENT)
    val reduceMovementTargets by settingsDataStore.reduceMovementTargets.collectAsState(
        initial = DefaultSettings.REDUCE_MOVEMENT_TARGETS.split(',').filter { it.isNotEmpty() }.toSet()
    )
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
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
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val autoScrollText by settingsDataStore.autoScrollText.collectAsState(initial = DefaultSettings.AUTO_SCROLL_TEXT)
    val autoScrollTextElements by settingsDataStore.autoScrollTextElements.collectAsState(
        initial = DefaultSettings.AUTO_SCROLL_TEXT_ELEMENTS.split(',').filter { it.isNotEmpty() }.toSet()
    )
    val autoScrollTextScreens by settingsDataStore.autoScrollTextScreens.collectAsState(
        initial = DefaultSettings.AUTO_SCROLL_TEXT_SCREENS.split(',').filter { it.isNotEmpty() }.toSet()
    )
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
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

