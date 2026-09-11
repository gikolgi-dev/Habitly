/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen.settings

import android.content.Intent
import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.habitly.habitly.BuildConfig
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.notifications.GENERAL_NOTIFICATION_ID
import com.habitly.habitly.notifications.NotificationReceiver
import com.habitly.habitly.notifications.NotificationScheduler
import com.habitly.habitly.ui.components.CustomTimePickerDialog
import com.habitly.habitly.ui.components.NotificationTimeSelectors
import com.habitly.habitly.ui.components.rememberNotificationPermissionHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

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
    val firstDayOfWeekCalendar by settingsDataStore.firstDayOfWeekCalendar.collectAsState(initial = Calendar.MONDAY)
    val notificationScheduler = remember { NotificationScheduler(context) }

    val vibrationsEnabledState = settingsDataStore.vibrations.collectAsState(initial = null)
    val skipCompletedState = settingsDataStore.skipCompletedHabitNotifications.collectAsState(initial = null)
    val snoozeEnabledState = settingsDataStore.snoozeEnabled.collectAsState(initial = null)
    val snoozeDurationMinutesState = settingsDataStore.snoozeDurationMinutes.collectAsState(initial = null)
    val globalNotificationsEnabledState = settingsDataStore.globalNotificationsEnabled.collectAsState(initial = null)
    val globalNotificationTimeState = settingsDataStore.globalNotificationTime.collectAsState(initial = null)
    val globalNotificationDaysState = settingsDataStore.globalNotificationDays.collectAsState(initial = null)

    val exactAlarmsState = settingsDataStore.exactAlarms.collectAsState(initial = null)
    val vibrationsEnabled = vibrationsEnabledState.value ?: return
    val skipCompleted = skipCompletedState.value ?: return
    val snoozeEnabled = snoozeEnabledState.value ?: return
    val snoozeDurationMinutes = snoozeDurationMinutesState.value ?: return
    val globalNotificationsEnabled = globalNotificationsEnabledState.value ?: return
    val globalNotificationTime = globalNotificationTimeState.value ?: return
    val globalNotificationDays = globalNotificationDaysState.value ?: return

    val exactAlarms = exactAlarmsState.value ?: return

    val lifecycleOwner = LocalLifecycleOwner.current
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (exactAlarms) {
                    scope.launch {
                        notificationScheduler.rescheduleAll()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun handleExactAlarmsToggle(enabled: Boolean) {
        if (enabled) {
            val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            scope.launch {
                settingsDataStore.setExactAlarms(true)
                notificationScheduler.rescheduleAll()
            }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            }
            if (!canSchedule) {
                showExactAlarmPermissionDialog = true
            }
        } else {
            scope.launch {
                settingsDataStore.setExactAlarms(false)
                notificationScheduler.rescheduleAll()
            }
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
            }
        }
    }

    if (showExactAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmPermissionDialog = false },
            title = {
                Text(
                    text = "Exact Alarm Permission",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "To deliver notifications at the exact specified time even when the device is in sleep or battery saver mode, Habitly needs the 'Alarms & reminders' permission. Please enable it in system settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExactAlarmPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    e2.printStackTrace()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showExactAlarmPermissionDialog = false
                        scope.launch {
                            settingsDataStore.setExactAlarms(false)
                            notificationScheduler.rescheduleAll()
                        }
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        }
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }
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
            title = "General",
            settingsDataStore = settingsDataStore
        ) {
            SettingsSwitchItem(
                text = "Skip completed",
                description = "Don't notify if the habit is already completed today",
                checked = skipCompleted,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Top
            ) {
                scope.launch {
                    settingsDataStore.setSkipCompletedHabitNotifications(it)
                }
                if (vibrationsEnabled) haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            }
            /* SettingsSwitchItem(
                text = "Snooze enabled",
                description = "Allow snoozing notifications",
                checked = snoozeEnabled,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Middle
            ) {
                scope.launch {
                    settingsDataStore.setSnoozeEnabled(it)
                }
                if (vibrationsEnabled) haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            } */
            SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Middle) {
                val snoozeDurationAlpha by animateFloatAsState(
                    targetValue = if (snoozeEnabled) 1f else 0.5f,
                    label = "SnoozeDurationAlpha"
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .alpha(snoozeDurationAlpha)
                ) {
                    Text(
                        text = "Snooze duration",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val options = if (BuildConfig.IS_DEVELOPER_MODE) {
                        listOf("2m", "15m", "30m", "1h", "2h")
                    } else {
                        listOf("15m", "30m", "1h", "2h")
                    }

                    val selectedIndex = if (BuildConfig.IS_DEVELOPER_MODE) {
                        when (snoozeDurationMinutes) {
                            2 -> 0
                            15 -> 1
                            30 -> 2
                            120 -> 4
                            else -> 3 // 1h
                        }
                    } else {
                        when (snoozeDurationMinutes) {
                            15 -> 0
                            30 -> 1
                            120 -> 3
                            else -> 2 // 1h
                        }
                    }

                    SettingsSegmentedSelector(
                        options = options,
                        selectedIndex = selectedIndex,
                        enabled = snoozeEnabled,
                        onSelectionChange = { index ->
                            val newValue = if (BuildConfig.IS_DEVELOPER_MODE) {
                                when (index) {
                                    0 -> 2
                                    1 -> 15
                                    2 -> 30
                                    4 -> 120
                                    else -> 60
                                }
                            } else {
                                when (index) {
                                    0 -> 15
                                    1 -> 30
                                    3 -> 120
                                    else -> 60
                                }
                            }
                            if (snoozeDurationMinutes != newValue) {
                                scope.launch {
                                    settingsDataStore.setSnoozeDurationMinutes(newValue)
                                }
                                if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                }
            }
            SettingsSwitchItem(
                text = "Exact alarms & reminders",
                description = "Improve notification accuracy. Consumes more battery.",
                checked = exactAlarms,
                settingsDataStore = settingsDataStore,
                position = SettingsItemPosition.Bottom
            ) {
                handleExactAlarmsToggle(it)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                        onTimeClick = { showTimePicker = true },
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
                        onDisabledClick = {
                            if (!notificationPermissionHandler.hasPermission) {
                                notificationPermissionHandler.requestPermission()
                            } else {
                                handleNotificationToggle(true)
                            }
                        },
                        isEnabled = isEnabled,
                        borderAlpha = borderContrast,
                        is24Hour = is24Hour,
                        vibrationsEnabled = vibrationsEnabled,
                        firstDayOfWeek = firstDayOfWeekCalendar,
                        modifier = Modifier
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (BuildConfig.IS_DEVELOPER_MODE) {
            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup(
                title = "Developer Debug",
                settingsDataStore = settingsDataStore
            ) {
                SettingsItemBox(settingsDataStore = settingsDataStore, position = SettingsItemPosition.Alone) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Send Test Notification",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, NotificationReceiver::class.java).apply {
                                        putExtra("habitId", GENERAL_NOTIFICATION_ID)
                                        putExtra("notificationTime", "00:00")
                                        putExtra("notificationDays", arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                                    }
                                    context.sendBroadcast(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Daily")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val dao = HabitDatabase.getDatabase(context).habitDao()
                                        val firstHabit = dao.getAllHabits().first().firstOrNull()

                                        if (firstHabit != null) {
                                            val intent = Intent(context, NotificationReceiver::class.java).apply {
                                                putExtra("habitId", firstHabit.id)
                                                putExtra("habitName", firstHabit.name)
                                                putExtra("notificationTime", "00:00")
                                                putExtra("notificationDays", arrayOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                                            }
                                            context.sendBroadcast(intent)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Habit")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
    }
}
