@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit, db: HabitDatabase, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val showConfirmationDialog = remember { mutableStateOf(false) }
    val showSecondConfirmationDialog = remember { mutableStateOf(false) }
    var showAppearanceScreen by remember { mutableStateOf(false) }
    var showGeneralScreen by remember { mutableStateOf(false) }
    var showImportScreen by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val globalNotificationsEnabled by settingsDataStore.globalNotificationsEnabled.collectAsState(initial = false)
    val globalNotificationTime by settingsDataStore.globalNotificationTime.collectAsState(initial = "09:00")
    val globalNotificationDays by settingsDataStore.globalNotificationDays.collectAsState(initial = setOf())
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = false)
    val haptic = LocalHapticFeedback.current
    val notificationScheduler = remember { NotificationScheduler(context) }
    val theme by settingsDataStore.theme.collectAsState(initial = "system")
    val uriHandler = LocalUriHandler.current
    val useDarkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

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
                    notificationScheduler.scheduleGeneralNotification(globalNotificationTime, globalNotificationDays)
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
                    notificationScheduler.scheduleGeneralNotification(globalNotificationTime, globalNotificationDays)
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
    BackHandler(enabled = showAppearanceScreen || showGeneralScreen || showImportScreen) {
        if (showAppearanceScreen) {
            showAppearanceScreen = false
        }
        if (showGeneralScreen) {
            showGeneralScreen = false
        }
        if (showImportScreen) {
            showImportScreen = false
        }
    }

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text("Clear all data?") },
            text = { Text("This action is irreversible and will delete all your habits and completions.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showConfirmationDialog.value = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            showConfirmationDialog.value = false
                            showSecondConfirmationDialog.value = true
                        }
                    ) {
                        Text("Clear Data", color = Color.Red)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSecondConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showSecondConfirmationDialog.value = false },
            title = { Text("Are you absolutely sure?") },
            text = { Text("This is your final warning. All data will be lost.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showSecondConfirmationDialog.value = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                db.clearAllTables()
                                withContext(Dispatchers.Main) {
                                    onDismiss()
                                 }
                            }
                            showSecondConfirmationDialog.value = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Yes, Delete Everything", color = Color.Red)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

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
                        notificationScheduler.scheduleGeneralNotification(newTime, globalNotificationDays)
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

    val blurModifier = if (showTimePicker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else {
        Modifier
    }

    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { },
            dragHandle = { BottomSheetDefaults.DragHandle(Modifier.fillMaxWidth(0.15f)) }
        ) {
            Column(modifier = blurModifier) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween

                ) {
                    Text(
                        fontSize = 32.sp,
                        text = "Daily notifications",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = globalNotificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { handleNotificationToggle(it) }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
                    Text(
                        fontSize = 14.sp,
                        text = "Create a daily notification to remind you of adding completions.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                val isEnabled = globalNotificationsEnabled && hasNotificationPermission
                NotificationSelectors(
                    notificationTime = globalNotificationTime,
                    selectedDays = globalNotificationDays,
                    onTimeClick = { },
                    onDaySelected = { day ->
                        scope.launch {
                            val newDays = if (globalNotificationDays.contains(day)) {
                                globalNotificationDays - day
                            } else {
                                globalNotificationDays + day
                            }
                            settingsDataStore.setGlobalNotificationDays(newDays)
                            if (globalNotificationsEnabled) {
                                notificationScheduler.scheduleGeneralNotification(globalNotificationTime, newDays)
                            }
                        }
                    },
                    isEnabled = isEnabled,
                    borderAlpha = borderContrast
                )
            }
        }
    }

    Scaffold(
        modifier = blurModifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (showAppearanceScreen) "Appearance" else if (showGeneralScreen) "General" else if (showImportScreen) "Data Management" else "Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (showAppearanceScreen || showGeneralScreen || showImportScreen) {
                        IconButton(onClick = {
                            if (showAppearanceScreen) showAppearanceScreen = false
                            if (showGeneralScreen) showGeneralScreen = false
                            if (showImportScreen) showImportScreen = false
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                actions = {
                    if (showAppearanceScreen) {
                        IconButton(onClick = {
                            scope.launch {
                                settingsDataStore.resetToDefault()
                            }
                        }) {
                            Icon(painter = painterResource(id = R.drawable.resetwrench), contentDescription = "Reset Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = !showAppearanceScreen && !showGeneralScreen && !showImportScreen,
                exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)),
                enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300))
            ) {
                LazyColumn {
                    item {
                        SettingsGroup(settingsDataStore = settingsDataStore) {
                            GroupedSettingsItem(
                                title = "General",
                                subtitle = "Toggle vibrations",
                                icon = Icons.Default.Tune,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) { showGeneralScreen = true }
                            HorizontalDivider(color = Color.Gray.copy(0.1f), modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally))
                            GroupedSettingsItem(
                                title = "Appearance",
                                subtitle = "Change the look and feel of the app",
                                icon = Icons.Default.Palette,
                                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) { showAppearanceScreen = true }
                        }
                    }
                    item {
                        ModernSettingsItem(
                            title = "Daily Notifications",
                            subtitle = "Set daily notification completion reminder",
                            icon = Icons.Default.Notifications,
                            iconBackgroundColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { },
                            iconColor = MaterialTheme.colorScheme.onTertiary,
                            settingsDataStore = settingsDataStore
                        )
                    }
                    item {
                        SettingsGroup(settingsDataStore = settingsDataStore) {
                            GroupedSettingsItem(
                                title = "Data management",
                                subtitle = "Import and export habit data",
                                icon = Icons.Default.ImportExport,
                                iconBackgroundColor = Color(0xFF73C177).copy(alpha = if (useDarkTheme) 1f else 0.35f),
                                iconColor = Color(0xFF246D29)
                            ) { showImportScreen = true }
                            HorizontalDivider(color = Color.Gray.copy(0.1f), modifier = Modifier.fillMaxWidth(0.90f).align(Alignment.CenterHorizontally))
                            GroupedSettingsItem(
                                title = "Clear all data",
                                subtitle = "Delete all habits and their completions",
                                icon = Icons.Default.Delete,
                                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                                iconColor = MaterialTheme.colorScheme.onErrorContainer
                            ) { showConfirmationDialog.value = true }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = Color.Gray.copy(borderContrast),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .clickable { uriHandler.openUri("https://github.com/gikolgi-dev/Habitly") }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (useDarkTheme) {
                                            R.drawable.github_mark_white
                                        } else {
                                            R.drawable.github_mark
                                        }
                                    ),
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "View on GitHub",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Made with ❤\uFE0F",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Version 1.1.2",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showAppearanceScreen,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                AppearanceScreen(
                    settingsDataStore = settingsDataStore
                )
            }

            AnimatedVisibility(
                visible = showImportScreen,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                ImportExportScreen(
                    db = db
                )
            }

            AnimatedVisibility(
                visible = showGeneralScreen,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
            ) {
                GeneralSettingsScreen(
                    settingsDataStore = settingsDataStore
                )
            }
        }
    }
}
