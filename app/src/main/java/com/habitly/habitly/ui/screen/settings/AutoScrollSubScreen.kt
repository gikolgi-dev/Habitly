/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

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
    val (scrollState, scrollEnabled) = rememberSettingsScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled)
    ) {
        Text(
            text = "Automatically scroll overflowing titles and descriptions horizontally from left to right.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        MainSettingsToggle(
            text = "Auto-scroll text",
            checked = autoScrollText,
            onCheckedChange = { isChecked ->
                scope.launch {
                    settingsDataStore.setAutoScrollText(isChecked)
                }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = "Elements",
            settingsDataStore = settingsDataStore
        ) {
            val elements = listOf("Title", "Description")
            elements.forEachIndexed { index, element ->
                val position = when (index) {
                    0 -> SettingsItemPosition.Top
                    elements.size - 1 -> SettingsItemPosition.Bottom
                    else -> SettingsItemPosition.Middle
                }

                SettingsCheckboxItem(
                    text = element,
                    checked = element in autoScrollTextElements,
                    enabled = autoScrollText,
                    settingsDataStore = settingsDataStore,
                    position = position,
                    showDivider = index < elements.size - 1,
                    onCheckedChange = { isChecked ->
                        val newElements = if (isChecked) {
                            autoScrollTextElements + element
                        } else {
                            autoScrollTextElements - element
                        }
                        scope.launch {
                            settingsDataStore.setAutoScrollTextElements(newElements)
                        }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = "Screens",
            settingsDataStore = settingsDataStore
        ) {
            val screens = listOf("Main Screen", "Detail Screen")
            screens.forEachIndexed { index, screen ->
                val position = when (index) {
                    0 -> SettingsItemPosition.Top
                    screens.size - 1 -> SettingsItemPosition.Bottom
                    else -> SettingsItemPosition.Middle
                }

                SettingsCheckboxItem(
                    text = screen,
                    checked = screen in autoScrollTextScreens,
                    enabled = autoScrollText,
                    settingsDataStore = settingsDataStore,
                    position = position,
                    showDivider = index < screens.size - 1,
                    onCheckedChange = { isChecked ->
                        val newScreens = if (isChecked) {
                            autoScrollTextScreens + screen
                        } else {
                            autoScrollTextScreens - screen
                        }
                        scope.launch {
                            settingsDataStore.setAutoScrollTextScreens(newScreens)
                        }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
