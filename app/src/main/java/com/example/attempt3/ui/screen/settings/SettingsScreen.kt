@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui.screen.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attempt3.R
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.notifications.NotificationScheduler
import com.example.attempt3.ui.CustomTimePickerDialog
import com.example.attempt3.ui.NotificationTimeSelectors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit, db: HabitDatabase, settingsDataStore: SettingsDataStore) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val showConfirmationDialog = remember { mutableStateOf(false) }
    val showSecondConfirmationDialog = remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val globalNotificationsEnabled by settingsDataStore.globalNotificationsEnabled.collectAsState(initial = false)
    val globalNotificationTime by settingsDataStore.globalNotificationTime.collectAsState(initial = "09:00")
    val globalNotificationDays by settingsDataStore.globalNotificationDays.collectAsState(initial = setOf())
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = false)
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
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
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
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
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    }
                }
            } else {
                requestPermission()
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

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text("Clear all data?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This action is irreversible and will delete all your habits and completions.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showConfirmationDialog.value = false },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showConfirmationDialog.value = false
                            showSecondConfirmationDialog.value = true
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Clear Data", color = Color.White)
                    }
                }
            },
            dismissButton = null,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSecondConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showSecondConfirmationDialog.value = false },
            title = { Text("Are you absolutely sure?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This is your final warning. All data will be lost.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showSecondConfirmationDialog.value = false },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                db.clearAllTables()
                                withContext(Dispatchers.Main) {
                                    onDismiss()
                                 }
                            }
                            showSecondConfirmationDialog.value = false
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Yes, Delete Everything", color = Color.White)
                    }
                }
            },
            dismissButton = null,
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

    val blurModifier = if (showTimePicker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else {
        Modifier
    }

    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
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
                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                val isEnabled = globalNotificationsEnabled && hasNotificationPermission
                NotificationTimeSelectors(
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
                                notificationScheduler.scheduleGeneralNotification(
                                    globalNotificationTime,
                                    newDays
                                )
                            }
                        }
                    },
                    isEnabled = isEnabled,
                    borderAlpha = borderContrast,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = blurModifier.fillMaxSize()
    ) {
        // Main Screen
        composable(
            route = "main",
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
        ) {
            BackHandler { onDismiss() }
            SettingsScaffold(
                title = "Settings",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    onDismiss()
                },
                settingsDataStore = settingsDataStore,
                isRoot = true
            ) { paddingValues ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(paddingValues).padding(top = 8.dp)
                ) {
                    item {
                        SettingsGroup(settingsDataStore = settingsDataStore) {
                            GroupedSettingsItem(
                                title = "General",
                                subtitle = "Toggle vibrations",
                                icon = Icons.Default.Tune,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Top
                            ) { navController.navigate("general") }
                            GroupedSettingsItem(
                                title = "Appearance",
                                subtitle = "Change the look and feel of the app",
                                icon = Icons.Default.Palette,
                                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Bottom
                            ) { navController.navigate("appearance") }
                        }
                    }
                    item {
                        ModernSettingsItem(
                            title = "Daily Notifications",
                            subtitle = "Set daily notification completion reminder",
                            icon = Icons.Default.Notifications,
                            iconBackgroundColor = MaterialTheme.colorScheme.tertiary,
                            iconColor = MaterialTheme.colorScheme.onTertiary,
                            settingsDataStore = settingsDataStore,
                            position = SettingsItemPosition.Alone
                        ) { showNotificationSheet = true }
                    }
                    item {
                        SettingsGroup(settingsDataStore = settingsDataStore) {
                            GroupedSettingsItem(
                                title = "Data management",
                                subtitle = "Import and export habit data",
                                icon = Icons.Default.ImportExport,
                                iconBackgroundColor = Color(0xFF73C177).copy(alpha = if (useDarkTheme) 1f else 0.35f),
                                iconColor = Color(0xFF246D29),
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Top
                            ) { navController.navigate("import") }
                            GroupedSettingsItem(
                                title = "Clear all data",
                                subtitle = "Delete all habits and their completions",
                                icon = Icons.Default.Delete,
                                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                                iconColor = MaterialTheme.colorScheme.onErrorContainer,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Bottom
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
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(borderContrast),
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
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "View on GitHub",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                text = "Made with ❤️",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Version 2.0.4",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Sub Screens
        composable(
            route = "appearance",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
        ) {
            SettingsScaffold(
                title = "Appearance",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore,
                actions = {
                    IconButton(onClick = { scope.launch { settingsDataStore.resetToDefault() } }) {
                        Icon(painter = painterResource(id = R.drawable.resetwrench), contentDescription = "Reset Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            ) { paddingValues ->
                AppearanceScreen(
                    modifier = Modifier.padding(paddingValues),
                    settingsDataStore = settingsDataStore,
                    onNavigateToScrollBlur = { navController.navigate("scroll_blur") }
                )
            }
        }

        composable(
            route = "general",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
        ) {
            SettingsScaffold(
                title = "General",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore
            ) { paddingValues ->
                GeneralSettingsScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable(
            route = "import",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
        ) {
            SettingsScaffold(
                title = "Data Management",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore
            ) { paddingValues ->
                ImportExportScreen(
                    db = db,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable(
            route = "scroll_blur",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(250)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(250)) }
        ) {
            SettingsScaffold(
                title = "Scroll Blur",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore
            ) { paddingValues ->
                ScrollBlurSubScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    settingsDataStore: SettingsDataStore,
    isRoot: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = title, 
                        fontWeight = FontWeight.SemiBold, 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp)
                    ) 
                },
                navigationIcon = {
                    SettingsBackButton(onBack = onBack, settingsDataStore = settingsDataStore, isRoot = isRoot)
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        content = content
    )
}