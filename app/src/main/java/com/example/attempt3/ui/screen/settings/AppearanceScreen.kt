/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore

@Composable
fun AppearanceScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore,
    onNavigateToScrollBlur: () -> Unit,
    onNavigateToHabitColor: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsDataStore.theme.collectAsState(initial = "system")
    val useHabitColorForCard by settingsDataStore.useHabitColorForCard.collectAsState(initial = true)
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = true)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = true)
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = true)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = true)
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(initial = setOf("Heatmap", "Line Chart"))
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0f)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val disableAnimations by settingsDataStore.disableAnimations.collectAsState(initial = false)

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ThemeSection(
                currentTheme = currentTheme,
                useHabitColorForCard = useHabitColorForCard,
                borderContrast = borderContrast,
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = scope,
                haptic = haptic,
                onNavigateToHabitColor = onNavigateToHabitColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            HeatmapSection(
                showMonthLabels = showMonthLabels,
                showYearDivider = showYearDivider,
                showYearLabels = showYearLabels,
                borderContrast = borderContrast,
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = scope,
                haptic = haptic
            )

            Spacer(modifier = Modifier.height(8.dp))

            AccessibilitySection(
                borderContrast = borderContrast,
                showScrollBlur = showScrollBlur,
                scrollBlurTargets = scrollBlurTargets,
                disableAnimations = disableAnimations,
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = scope,
                haptic = haptic,
                onNavigateToScrollBlur = onNavigateToScrollBlur
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
