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
fun HeatmapNotificationDotSubScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val heatmapNotificationDot by settingsDataStore.heatmapNotificationDot.collectAsState(initial = DefaultSettings.HEATMAP_NOTIFICATION_DOT)
    val heatmapNotificationDotRange by settingsDataStore.heatmapNotificationDotRange.collectAsState(initial = DefaultSettings.HEATMAP_NOTIFICATION_DOT_RANGE)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
    val heatmapNotificationDotDetailOnly by settingsDataStore.heatmapNotificationDotDetailOnly.collectAsState(initial = DefaultSettings.HEATMAP_NOTIFICATION_DOT_DETAIL_ONLY)

    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0)
    ) {
        Text(
            text = "Show a white dot on the heatmap to indicate days when a notification is scheduled.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        MainSettingsToggle(
            text = "Notification indicator",
            checked = heatmapNotificationDot,
            onCheckedChange = { isChecked ->
                scope.launch {
                    settingsDataStore.setHeatmapNotificationDot(isChecked)
                }
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(if (isChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(
            title = "Display Range",
            settingsDataStore = settingsDataStore
        ) {
            val ranges = listOf(
                "future" to "Only future notifications",
                "today_and_future" to "Today and future notifications",
                "this_week" to "Notifications this week"
            )

            ranges.forEachIndexed { index, (rangeKey, rangeName) ->
                val position = when (index) {
                    0 -> SettingsItemPosition.Top
                    ranges.size - 1 -> SettingsItemPosition.Bottom
                    else -> SettingsItemPosition.Middle
                }

                SettingsRadioButtonItem(
                    text = rangeName,
                    selected = rangeKey == heatmapNotificationDotRange,
                    enabled = heatmapNotificationDot,
                    settingsDataStore = settingsDataStore,
                    position = position,
                    showDivider = index < ranges.size - 1,
                    onClick = {
                        scope.launch {
                            settingsDataStore.setHeatmapNotificationDotRange(rangeKey)
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
            settingsDataStore = settingsDataStore
        ) {
            SettingsSwitchItem(
                text = "Only show on detail screen",
                checked = heatmapNotificationDotDetailOnly,
                enabled = heatmapNotificationDot,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Alone,
                onCheckedChange = { isChecked ->
                    scope.launch {
                        settingsDataStore.setHeatmapNotificationDotDetailOnly(isChecked)
                    }
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
