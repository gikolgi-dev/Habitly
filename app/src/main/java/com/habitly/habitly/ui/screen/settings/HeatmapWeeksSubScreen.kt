/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun HeatmapWeeksSubScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val heatmapWeeks by settingsDataStore.heatmapWeeks.collectAsState(initial = DefaultSettings.HEATMAP_WEEKS)
    val heatmapInfinite by settingsDataStore.heatmapInfinite.collectAsState(initial = DefaultSettings.HEATMAP_INFINITE)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
    
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var textValue by remember(heatmapWeeks) { mutableStateOf(heatmapWeeks.toString()) }

    val alpha by animateFloatAsState(
        targetValue = if (heatmapInfinite) 0.38f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "deactivationAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0)
    ) {
        Text(
            text = "Adjust the number of weeks displayed in the habit heatmap. High values may impact performance.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        MainSettingsToggle(
            text = "Show all data",
            checked = heatmapInfinite,
            onCheckedChange = { isChecked ->
                scope.launch {
                    settingsDataStore.setHeatmapInfinite(isChecked)
                }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = "Week limit",
            settingsDataStore = settingsDataStore
        ) {
            SettingsItemBox(
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Alone
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .graphicsLayer { this.alpha = alpha }
                ) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                textValue = newValue
                                val weeks = newValue.toIntOrNull() ?: 0
                                scope.launch {
                                    settingsDataStore.setHeatmapWeeks(weeks)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !heatmapInfinite,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Number of weeks") }
                    )
                    if (heatmapWeeks > 52 && !heatmapInfinite) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Warning: Large values might cause lag.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
