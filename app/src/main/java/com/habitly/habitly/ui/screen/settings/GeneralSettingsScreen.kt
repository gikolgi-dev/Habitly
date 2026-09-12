/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsScreen(
    settingsDataStore: SettingsDataStore,
    onNavigateToHeatmapWeeks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = DefaultSettings.IS_24_HOUR)
    val heroCardVisible by settingsDataStore.heroCardVisible.collectAsState(initial = DefaultSettings.HERO_CARD_VISIBLE)
    val skipCompleted by settingsDataStore.skipCompletedHabitNotifications.collectAsState(initial = DefaultSettings.SKIP_COMPLETED_HABIT_NOTIFICATIONS)
<<<<<<< Updated upstream
    val firstDayOfWeek by settingsDataStore.firstDayOfWeek.collectAsState(initial = DefaultSettings.FIRST_DAY_OF_WEEK)
=======
<<<<<<< Updated upstream
=======
    val firstDayOfWeek by settingsDataStore.firstDayOfWeek.collectAsState(initial = DefaultSettings.FIRST_DAY_OF_WEEK)
    val ignoreWelcomeEngine by settingsDataStore.ignoreWelcomeEngine.collectAsState(initial = DefaultSettings.IGNORE_WELCOME_ENGINE)
>>>>>>> Stashed changes
>>>>>>> Stashed changes

    val haptic = LocalHapticFeedback.current
    val (scrollState, scrollEnabled) = rememberSettingsScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollEnabled),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsGroup(
            title = "General",
            settingsDataStore = settingsDataStore
        ) {
            SettingsSwitchItem(
                text = "Haptic feedback",
                description = "Enable subtle vibrations for interactions",
                checked = vibrationsEnabled,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Top
            ) {
                scope.launch {
                    settingsDataStore.setVibrations(it)
                }
                if (vibrationsEnabled) haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
            SettingsSwitchItem(
                text = "Welcome card",
                description = "Show the greeting card on the home screen",
                checked = heroCardVisible,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Middle
            ) {
                scope.launch {
                    settingsDataStore.setHeroCardVisible(it)
                }
                if (vibrationsEnabled) haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
<<<<<<< Updated upstream
=======
            SettingsSwitchItem(
<<<<<<< Updated upstream
                text = "Heatmap scrolling",
                description = "Allow horizontal scrolling on the main heatmap",
                checked = heatmapScrolling,
=======
                text = "Simple welcome descriptions",
                description = "Ignore the welcome engine and use the original simple descriptions",
                checked = ignoreWelcomeEngine,
>>>>>>> Stashed changes
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Middle
            ) {
                scope.launch {
<<<<<<< Updated upstream
                    settingsDataStore.setHeatmapScrolling(it)
=======
                    settingsDataStore.setIgnoreWelcomeEngine(it)
>>>>>>> Stashed changes
                }
                if (vibrationsEnabled) haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
>>>>>>> Stashed changes
            SettingsNavigationItem(
                text = "Week limit",
                description = "Limit the amount of weeks shown in the heatmap. High values may impact performance.",
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Middle,
                onClick = onNavigateToHeatmapWeeks
            )
            SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Middle) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Hour format",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedSelector(
                        options = listOf("12-h", "24-h"),
                        selectedIndex = if (is24Hour) 1 else 0,
                        onSelectionChange = { index ->
                            val newValue = index == 1
                            if (is24Hour != newValue) {
                                scope.launch {
                                    settingsDataStore.setIs24Hour(newValue)
                                }
                                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            }
            SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Bottom) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "First day of the week",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Affects weekly streak calculations and calendar week layout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedSelector(
                        options = listOf("Monday", "Sunday"),
                        selectedIndex = if (firstDayOfWeek.lowercase() == "sunday") 1 else 0,
                        onSelectionChange = { index ->
                            val newDay = if (index == 1) "sunday" else "monday"
                            if (firstDayOfWeek != newDay) {
                                scope.launch {
                                    settingsDataStore.setFirstDayOfWeek(newDay)
                                }
                                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}

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

    var textValue by remember(heatmapWeeks) { mutableStateOf(heatmapWeeks.toString()) }

    val alpha by animateFloatAsState(
        targetValue = if (heatmapInfinite) 0.38f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "deactivationAlpha"
    )

    SettingsSubScreenContainer(modifier = modifier) {
        SettingsSubScreenDescription("Adjust the number of weeks displayed in the habit heatmap. High values may impact performance.")

        MainSettingsToggle(
            text = "Show all data",
            checked = heatmapInfinite,
            onCheckedChange = { isChecked ->
                scope.launch { settingsDataStore.setHeatmapInfinite(isChecked) }
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
                                scope.launch { settingsDataStore.setHeatmapWeeks(weeks) }
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
    }
}

