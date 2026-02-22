package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.attempt3.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun ScrollBlurSubScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(initial = setOf("Heatmap", "Line Chart"))
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(
            title = "Targets",
            settingsDataStore = settingsDataStore,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            val targets = listOf("Heatmap", "Line Chart")
            targets.forEachIndexed { index, target ->
                val position = when {
                    targets.size == 1 -> SettingsItemPosition.Alone
                    index == 0 -> SettingsItemPosition.Top
                    index == targets.size - 1 -> SettingsItemPosition.Bottom
                    else -> SettingsItemPosition.Middle
                }

                SettingsCheckboxItem(
                    text = target,
                    checked = target in scrollBlurTargets,
                    settingsDataStore = settingsDataStore,
                    position = position,
                    showDivider = index < targets.size - 1,
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
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    }
                )
            }
        }
        
        Text(
            text = "Select which components should have the blur effect applied when scrolling.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}