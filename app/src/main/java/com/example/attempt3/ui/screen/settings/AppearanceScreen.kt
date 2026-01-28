@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun AppearanceScreen(modifier: Modifier = Modifier, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsDataStore.theme.collectAsState(initial = "system")
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = true)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = true)
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = true)
    val showDayLabels by settingsDataStore.dayOfWeekLabelsVisible.collectAsState(initial = true)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = true)
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(initial = setOf("Heatmap", "Line Chart"))
    val showAllDayOfWeekLabels by settingsDataStore.showAllDayOfWeekLabels.collectAsState(initial = true)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0f)
    val appearanceTint by settingsDataStore.appearanceTint.collectAsState(initial = 0.1f)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val showTintDialogPref by settingsDataStore.showTintDialog.collectAsState(initial = true)
    
    var showTintDialogState by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var pendingTintValue by remember { mutableFloatStateOf(0f) }

    val haptic = LocalHapticFeedback.current

    if (showTintDialogState) {
        BasicAlertDialog(
            onDismissRequest = { showTintDialogState = false }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Background Tint",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "This setting looks best under specific combinations of Material theme and habit colors. You can adjust it to your preference, even if it might look unconventional.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineBreak = LineBreak.Paragraph,
                            hyphens = Hyphens.Auto
                        ),
                        textAlign = TextAlign.Justify
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = dontShowAgain,
                                onClick = { dontShowAgain = !dontShowAgain }
                            )
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            modifier = Modifier.offset(x = (-12).dp)
                        )
                        Text(
                            text = "Don't show again",
                            modifier = Modifier.offset(x = (-12).dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showTintDialogState = false },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    settingsDataStore.setAppearanceTint(pendingTintValue)
                                    if (dontShowAgain) {
                                        settingsDataStore.setShowTintDialog(false)
                                    }
                                }
                                showTintDialogState = false
                            },
                            shape = CircleShape
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ThemeSection(
                currentTheme = currentTheme,
                appearanceTint = appearanceTint,
                borderContrast = borderContrast,
                vibrationsEnabled = vibrationsEnabled,
                showTintDialogPref = showTintDialogPref,
                settingsDataStore = settingsDataStore,
                scope = scope,
                haptic = haptic,
                onShowTintDialog = { newValue ->
                    pendingTintValue = newValue
                    showTintDialogState = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            HeatmapSection(
                showMonthLabels = showMonthLabels,
                showYearDivider = showYearDivider,
                showYearLabels = showYearLabels,
                showDayLabels = showDayLabels,
                showAllDayOfWeekLabels = showAllDayOfWeekLabels,
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
                vibrationsEnabled = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                scope = scope,
                haptic = haptic
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}