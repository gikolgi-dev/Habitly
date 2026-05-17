/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.DefaultSettings
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.notifications.NotificationScheduler
import com.example.attempt3.ui.components.CustomTimePickerDialog
import com.example.attempt3.ui.components.NotificationTimeSelectors
import com.example.attempt3.ui.components.rememberNotificationPermissionHandler
import kotlinx.coroutines.launch

@Composable
fun NotificationSettingsScreen(
    settingsDataStore: SettingsDataStore,
    is24Hour: Boolean,
    borderContrast: Float,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val notificationScheduler = remember { NotificationScheduler(context) }

    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = DefaultSettings.VIBRATIONS)
    val skipCompleted by settingsDataStore.skipCompletedHabitNotifications.collectAsState(initial = DefaultSettings.SKIP_COMPLETED_HABIT_NOTIFICATIONS)
    val globalNotificationsEnabled by settingsDataStore.globalNotificationsEnabled.collectAsState(initial = false)
    val globalNotificationTime by settingsDataStore.globalNotificationTime.collectAsState(initial = "09:00")
    val globalNotificationDays by settingsDataStore.globalNotificationDays.collectAsState(initial = emptySet())

    val notificationPermissionHandler = rememberNotificationPermissionHandler {
        scope.launch {
            settingsDataStore.setGlobalNotificationsEnabled(true)
            notificationScheduler.scheduleGeneralNotification(globalNotificationTime, globalNotificationDays)
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val initialHour = globalNotificationTime.split(":")[0].toIntOrNull() ?: 9
        val initialMinute = globalNotificationTime.split(":")[1].toIntOrNull() ?: 0
        CustomTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                scope.launch {
                    val newTime = String.format("%02d:%02d", hour, minute)
                    settingsDataStore.setGlobalNotificationTime(newTime)
                    if (globalNotificationsEnabled) {
                        notificationScheduler.scheduleGeneralNotification(
                            newTime,
                            globalNotificationDays
                        )
                    }
                }
                showTimePicker = false
            },
            initialHour = initialHour,
            initialMinute = initialMinute,
            borderContrast = borderContrast,
            is24Hour = is24Hour
        )
    }

    fun handleNotificationToggle(enable: Boolean) {
        if (enable) {
            if (notificationPermissionHandler.hasPermission) {
                scope.launch {
                    settingsDataStore.setGlobalNotificationsEnabled(true)
                    notificationScheduler.scheduleGeneralNotification(globalNotificationTime, globalNotificationDays)
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                }
            } else {
                notificationPermissionHandler.requestPermission()
            }
        } else {
            scope.launch {
                settingsDataStore.setGlobalNotificationsEnabled(false)
                notificationScheduler.cancelGeneralNotification()
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = scrollState.maxValue > 0),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsGroup(
            title = "Daily Notifications",
            settingsDataStore = settingsDataStore
        ) {
            SettingsSwitchItem(
                text = "Daily reminder",
                description = "Remind you to add completions every day",
                checked = globalNotificationsEnabled && notificationPermissionHandler.hasPermission,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Top
            ) {
                handleNotificationToggle(it)
            }

            SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Bottom) {
                val isEnabled = globalNotificationsEnabled && notificationPermissionHandler.hasPermission
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    NotificationTimeSelectors(
                        notificationTime = globalNotificationTime,
                        selectedDays = globalNotificationDays,
                        onTimeClick = { if (isEnabled) showTimePicker = true else notificationPermissionHandler.requestPermission() },
                        onDaySelected = { day ->
                            scope.launch {
                                val newDays = if (globalNotificationDays.contains(day)) {
                                    globalNotificationDays - day
                                } else {
                                    globalNotificationDays + day
                                }
                                settingsDataStore.setGlobalNotificationDays(newDays)
                                if (globalNotificationsEnabled) {
                                    notificationScheduler.scheduleGeneralNotification(
                                        globalNotificationTime,
                                        newDays
                                    )
                                }
                            }
                        },
                        isEnabled = isEnabled,
                        borderAlpha = borderContrast,
                        is24Hour = is24Hour,
                        vibrationsEnabled = vibrationsEnabled,
                        modifier = Modifier
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsGroup(
            title = "Habit Notifications",
            settingsDataStore = settingsDataStore
        ) {
            SettingsSwitchItem(
                text = "Skip completed",
                description = "Don't notify if the habit is already completed today",
                checked = skipCompleted,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Alone
            ) {
                scope.launch {
                    settingsDataStore.setSkipCompletedHabitNotifications(it)
                }
                if (vibrationsEnabled) haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
        }

        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
