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
    onNavigateToHabitColor: () -> Unit,
    onNavigateToReduceMovement: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentThemeState = settingsDataStore.theme.collectAsState(initial = null)
    val useMaterialThemingState = settingsDataStore.useMaterialTheming.collectAsState(initial = null)
    val useHabitColorForCardState = settingsDataStore.useHabitColorForCard.collectAsState(initial = null)
    val showMonthLabelsState = settingsDataStore.monthLabels.collectAsState(initial = null)
    val showYearDividerState = settingsDataStore.yearDivider.collectAsState(initial = null)
    val showYearLabelsState = settingsDataStore.yearLabels.collectAsState(initial = null)
    val showScrollBlurState = settingsDataStore.showScrollBlur.collectAsState(initial = null)
    val borderContrastState = settingsDataStore.borders.collectAsState(initial = null)
    val vibrationsEnabledState = settingsDataStore.vibrations.collectAsState(initial = null)
    val reduceMovementState = settingsDataStore.reduceMovement.collectAsState(initial = null)

    val currentTheme = currentThemeState.value ?: return
    val useMaterialTheming = useMaterialThemingState.value ?: return
    val useHabitColorForCard = useHabitColorForCardState.value ?: return
    val showMonthLabels = showMonthLabelsState.value ?: return
    val showYearDivider = showYearDividerState.value ?: return
    val showYearLabels = showYearLabelsState.value ?: return
    val showScrollBlur = showScrollBlurState.value ?: return
    val borderContrast = borderContrastState.value ?: return
    val vibrationsEnabled = vibrationsEnabledState.value ?: return
    val reduceMovement = reduceMovementState.value ?: return

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
