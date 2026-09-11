/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore

@Composable
fun AppearanceScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore,
    onNavigateToHabitColor: () -> Unit,
    onNavigateToHeatmap: () -> Unit
) {
    val currentTheme by settingsDataStore.theme.collectAsState(initial = DefaultSettings.THEME)
    val useMaterialTheming by settingsDataStore.useMaterialTheming.collectAsState(initial = DefaultSettings.USE_MATERIAL_THEMING)
    val useHabitColorForCard by settingsDataStore.useHabitColorForCard.collectAsState(initial = DefaultSettings.USE_HABIT_COLOR_FOR_CARD)
    val lineChartYearDivider by settingsDataStore.lineChartYearDivider.collectAsState(initial = DefaultSettings.LINE_CHART_YEAR_DIVIDER)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)

    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ThemeSection(
                currentTheme = currentTheme,
                useMaterialTheming = useMaterialTheming,
                useHabitColorForCard = useHabitColorForCard,
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = rememberCoroutineScope(),
                haptic = haptic,
                onNavigateToHabitColor = onNavigateToHabitColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(
                settingsDataStore = settingsDataStore
            ) {
                SettingsNavigationItem(
                    text = "Heatmap",
                    description = "Customize heatmap appearance and visible labels",
                    settingsDataStore = settingsDataStore,
                    position = SettingsItemPosition.Alone,
                    onClick = onNavigateToHeatmap
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LineChartSection(
                lineChartYearDivider = lineChartYearDivider,
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = rememberCoroutineScope(),
                haptic = haptic
            )

            Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}
