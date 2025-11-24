package com.example.attempt3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsScreen(
    modifier: Modifier = Modifier,
    settingsDataStore: SettingsDataStore,
    showTimePicker: Boolean,
    onShowTimePickerChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val globalNotificationsEnabled by settingsDataStore.globalNotificationsEnabled.collectAsState(initial = false)
    val globalNotificationTime by settingsDataStore.globalNotificationTime.collectAsState(initial = "09:00")
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val haptic = LocalHapticFeedback.current
    val notificationScheduler = remember { NotificationScheduler(context) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                scope.launch {
                    settingsDataStore.setGlobalNotificationsEnabled(true)
                    notificationScheduler.scheduleGeneralNotification(globalNotificationTime)
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            }
        }
    )

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun handleNotificationToggle(enable: Boolean) {
        if (enable) {
            if (hasNotificationPermission) {
                scope.launch {
                    settingsDataStore.setGlobalNotificationsEnabled(true)
                    notificationScheduler.scheduleGeneralNotification(globalNotificationTime)
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            } else {
                requestPermission()
            }
        } else {
            scope.launch {
                settingsDataStore.setGlobalNotificationsEnabled(false)
                notificationScheduler.cancelGeneralNotification()
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
            }
        }
    }


    if (showTimePicker) {
        val initialHour = globalNotificationTime.split(":")[0].toIntOrNull() ?: 9
        val initialMinute = globalNotificationTime.split(":")[1].toIntOrNull() ?: 0
        CustomTimePickerDialog(
            onDismissRequest = { onShowTimePickerChange(false) },
            onConfirm = { hour, minute ->
                scope.launch {
                    val newTime = String.format("%02d:%02d", hour, minute)
                    settingsDataStore.setGlobalNotificationTime(newTime)
                    if (globalNotificationsEnabled) {
                        notificationScheduler.scheduleGeneralNotification(newTime)
                    }
                }
                onShowTimePickerChange(false)
            },
            initialHour = initialHour,
            initialMinute = initialMinute,
            borderContrast = borderContrast
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = borderContrast),
                        RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = vibrationsEnabled,
                                onClick = {
                                    scope.launch {
                                        settingsDataStore.setVibrations(!vibrationsEnabled)
                                    }
                                    haptic.performHapticFeedback(if (!vibrationsEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = vibrationsEnabled,
                            modifier = Modifier.padding(start = 16.dp),
                            onCheckedChange = {
                                scope.launch {
                                    settingsDataStore.setVibrations(it)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        Text(
                            text = "Enable vibrations",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (hasNotificationPermission) 1f else 0.5f)
                            .clickable(onClick = { handleNotificationToggle(!globalNotificationsEnabled) })
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = globalNotificationsEnabled && hasNotificationPermission,
                            onCheckedChange = { handleNotificationToggle(it) },
                            enabled = true,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Text(
                            text = "Enable daily reminder",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    AnimatedVisibility(visible = globalNotificationsEnabled && hasNotificationPermission) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onShowTimePickerChange(true) })
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reminder time",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            Text(
                                text = globalNotificationTime,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}