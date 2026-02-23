package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
fun HabitColorSubScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val useHabitColor by settingsDataStore.useHabitColorForCard.collectAsState(initial = true)
    val habitColorTargets by settingsDataStore.habitColorTargets.collectAsState(initial = setOf("Habit Cards", "Statistic Screen"))
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Apply the habit's color to specific components to improve identification and aesthetics.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        MainSettingsToggle(
            text = "Use habit color",
            checked = useHabitColor,
            onCheckedChange = { isChecked ->
                scope.launch {
                    settingsDataStore.setUseHabitColorForCard(isChecked)
                }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = "Targets",
            settingsDataStore = settingsDataStore
        ) {
            val targets = listOf("Habit Cards", "Statistic Screen")
            targets.forEachIndexed { index, target ->
                val position = when {
                    targets.size == 1 -> SettingsItemPosition.Alone
                    index == 0 -> SettingsItemPosition.Top
                    index == targets.size - 1 -> SettingsItemPosition.Bottom
                    else -> SettingsItemPosition.Middle
                }

                SettingsCheckboxItem(
                    text = target,
                    checked = target in habitColorTargets,
                    enabled = useHabitColor,
                    settingsDataStore = settingsDataStore,
                    position = position,
                    showDivider = index < targets.size - 1,
                    onCheckedChange = { isChecked ->
                        val newTargets = if (isChecked) {
                            habitColorTargets + target
                        } else {
                            habitColorTargets - target
                        }
                        scope.launch {
                            settingsDataStore.setHabitColorTargets(newTargets)
                        }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    }
                )
            }
        }
    }
}
