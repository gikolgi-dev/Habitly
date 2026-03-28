/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.example.attempt3.data.settings.DefaultSettings
import com.example.attempt3.data.settings.SettingsDataStore

@Composable
fun AppearanceScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore,
    onNavigateToScrollBlur: () -> Unit,
    onNavigateToHabitColor: () -> Unit,
    onNavigateToReduceMovement: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsDataStore.theme.collectAsState(initial = DefaultSettings.THEME)
    val useMaterialTheming by settingsDataStore.useMaterialTheming.collectAsState(initial = DefaultSettings.USE_MATERIAL_THEMING)
    val useHabitColorForCard by settingsDataStore.useHabitColorForCard.collectAsState(initial = DefaultSettings.USE_HABIT_COLOR_FOR_CARD)
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = DefaultSettings.MONTH_LABELS)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = DefaultSettings.YEAR_DIVIDER)
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = DefaultSettings.YEAR_LABELS)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = DefaultSettings.SHOW_SCROLL_BLUR)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = DefaultSettings.BORDERS)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
    val reduceMovement by settingsDataStore.reduceMovement.collectAsState(initial = DefaultSettings.REDUCE_MOVEMENT)

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
                reduceMovement = reduceMovement,
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = scope,
                haptic = haptic,
                onNavigateToScrollBlur = onNavigateToScrollBlur,
                onNavigateToReduceMovement = onNavigateToReduceMovement
            )

            Spacer(modifier = Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}
