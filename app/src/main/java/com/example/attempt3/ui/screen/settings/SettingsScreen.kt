/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)

package com.example.attempt3.ui.screen.settings

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attempt3.R
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.notifications.NotificationScheduler
import com.example.attempt3.ui.AppBackButton
import com.example.attempt3.ui.components.CustomTimePickerDialog
import com.example.attempt3.ui.components.NotificationTimeSelectors
import com.example.attempt3.ui.components.rememberNotificationPermissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SETTINGS_TRANSITION_DURATION = 300

// When pressing BACK: The Main Settings screen re-enters
fun AnimatedContentTransitionScope<NavBackStackEntry>.settingsPopEnterTransition() =
    slideInHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 3 } // Mimic coming from below/behind by starting like a third of the screen
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

// When pressing BACK: The Appearance Settings screen exits
fun AnimatedContentTransitionScope<NavBackStackEntry>.settingsPopExitTransition() =
    slideOutHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> fullWidth } // Slides entirely out to the right
    ) + scaleOut(
        targetScale = 0.8f, // Scaling down whilst having the shape of the screen
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

// When clicking FORWARD: The Appearance Settings screen enters
fun AnimatedContentTransitionScope<NavBackStackEntry>.settingsEnterTransition() =
    slideInHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> fullWidth } // Slides in from the right
    ) + scaleIn(
        initialScale = 0.8f, // Scaling up whilst having the shape of the screen
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

// When clicking FORWARD: The Main Settings screen exits
fun AnimatedContentTransitionScope<NavBackStackEntry>.settingsExitTransition() =
    slideOutHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> -fullWidth / 3 } // Slides slightly to the left
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

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
    val globalNotificationsEnabledState = settingsDataStore.globalNotificationsEnabled.collectAsState(initial = null)
    val globalNotificationTimeState = settingsDataStore.globalNotificationTime.collectAsState(initial = null)
    val globalNotificationDaysState = settingsDataStore.globalNotificationDays.collectAsState(initial = null)
    val borderContrastState = settingsDataStore.borders.collectAsState(initial = null)
    val is24HourState = settingsDataStore.is24Hour.collectAsState(initial = null)
    val vibrationsEnabledState = settingsDataStore.vibrations.collectAsState(initial = null)
    val themeState = settingsDataStore.theme.collectAsState(initial = null)

    val globalNotificationsEnabled = globalNotificationsEnabledState.value ?: return
    val globalNotificationTime = globalNotificationTimeState.value ?: return
    val globalNotificationDays = globalNotificationDaysState.value ?: return
    val borderContrast = borderContrastState.value ?: return
    val is24Hour = is24HourState.value ?: return
    val vibrationsEnabled = vibrationsEnabledState.value ?: return
    val theme = themeState.value ?: return

    val haptic = LocalHapticFeedback.current
    val notificationScheduler = remember { NotificationScheduler(context) }
    val uriHandler = LocalUriHandler.current
    val useDarkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
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

    if (showConfirmationDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text("Clear all data?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This action is irreversible and will delete all your habits and completions.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showConfirmationDialog.value = false },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        modifier = Modifier.weight(1f),
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showSecondConfirmationDialog.value = false },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        modifier = Modifier.weight(1f),
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
                        Text("Delete", color = Color.White)
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle(Modifier.fillMaxWidth(0.15f)) }
        ) {
            Column(
                modifier = blurModifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
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
                        checked = globalNotificationsEnabled && notificationPermissionHandler.hasPermission,
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
                val isEnabled = globalNotificationsEnabled && notificationPermissionHandler.hasPermission
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
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = blurModifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(Color.Black.copy(alpha = 0.5f)),
        enterTransition = { settingsEnterTransition() },
        exitTransition = { settingsExitTransition() },
        popEnterTransition = { settingsPopEnterTransition() },
        popExitTransition = { settingsPopExitTransition() }
    ) {
        // Main Screen
        composable(
            route = "main"
        ) {
            BackHandler { onDismiss() }
            SettingsScaffold(
                title = "Settings",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    onDismiss()
                },
                settingsDataStore = settingsDataStore,
                isRoot = true,
            ) { paddingValues ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SettingsGroup(settingsDataStore = settingsDataStore) {
                            GroupedSettingsItem(
                                title = "General",
                                subtitle = "Toggle app-wide features",
                                icon = Icons.Default.Tune,
                                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Top
                            ) { navController.navigate("general") { launchSingleTop = true } }
                            GroupedSettingsItem(
                                title = "Appearance",
                                subtitle = "Change the look and feel of the app",
                                icon = Icons.Default.Palette,
                                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Bottom
                            ) { navController.navigate("appearance") { launchSingleTop = true } }
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
                            ) { navController.navigate("import") { launchSingleTop = true } }
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
                        SettingsGroup(settingsDataStore = settingsDataStore) {
                            GroupedSettingsItem(
                                title = "View on GitHub",
                                subtitle = "Check out the source code",
                                painter = painterResource(
                                    if (useDarkTheme) {
                                        R.drawable.github_mark_white
                                    } else {
                                        R.drawable.github_mark
                                    }
                                ),
                                iconBackgroundColor = Color.Gray.copy(alpha = if (useDarkTheme) 0.5f else 0.15f),
                                iconColor = MaterialTheme.colorScheme.onSurface,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Top
                            ) { uriHandler.openUri("https://github.com/gikolgi-dev/Habitly") }
                            GroupedSettingsItem(
                                title = "License",
                                subtitle = "This app is licensed under GNU GPL v3.0",
                                icon = Icons.Default.Description,
                                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                settingsDataStore = settingsDataStore,
                                position = SettingsItemPosition.Bottom
                            ) { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html") }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
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
                                text = "Version $versionName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }

        // Sub Screens
        composable(
            route = "appearance"
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
                },
            ) { paddingValues ->
                AppearanceScreen(
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
                    settingsDataStore = settingsDataStore,
                    onNavigateToScrollBlur = { navController.navigate("scroll_blur") { launchSingleTop = true } },
                    onNavigateToHabitColor = { navController.navigate("habit_color") { launchSingleTop = true } }
                )
            }
        }

        composable(
            route = "general"
        ) {
            SettingsScaffold(
                title = "General",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore,
            ) { paddingValues ->
                GeneralSettingsScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }

        composable(
            route = "import"
        ) {
            SettingsScaffold(
                title = "Data Management",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore,
            ) { paddingValues ->
                ImportExportScreen(
                    db = db,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }

        composable(
            route = "scroll_blur"
        ) {
            SettingsScaffold(
                title = "Scroll Blur",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore,
            ) { paddingValues ->
                ScrollBlurSubScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }

        composable(
            route = "habit_color"
        ) {
            SettingsScaffold(
                title = "Habit Color",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                settingsDataStore = settingsDataStore,
            ) { paddingValues ->
                HabitColorSubScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedVisibilityScope.SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    settingsDataStore: SettingsDataStore,
    isRoot: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val density = LocalDensity.current
    
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    // Dynamic height based on whether the title has multiple words
    val hasSpace = title.contains(" ")
    val expandedMaxHeight = if (hasSpace) 210.dp else 160.dp
    val maxHeight = expandedMaxHeight + statusBarPadding
    val minHeight = 72.dp + statusBarPadding
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val minHeightPx = with(density) { minHeight.toPx() }
    
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != minHeightPx - maxHeightPx) {
            scrollBehavior.state.heightOffsetLimit = minHeightPx - maxHeightPx
        }
    }

    val collapsedFraction = scrollBehavior.state.collapsedFraction

    val cornerRadius by transition.animateDp(
        transitionSpec = { 
            if (targetState == EnterExitState.Visible) {
                tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
            } else {
                tween(50, easing = FastOutSlowInEasing)
            }
        },
        label = "settingsCornerRadius"
    ) { state ->
        if (state == EnterExitState.Visible) 0.dp else 32.dp
    }

    val dimAlpha by transition.animateFloat(
        transitionSpec = { tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing) },
        label = "settingsDimAlpha"
    ) { state ->
        if (state == EnterExitState.Visible) 0f else 0.2f
    }

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithContent {
                drawContent()
                if (dimAlpha > 0f) {
                    drawRect(Color.Black.copy(alpha = dimAlpha))
                }
            },
        topBar = {
            val currentHeight = maxHeight + with(density) { scrollBehavior.state.heightOffset.toDp() }
            
            Surface(
                color = if (collapsedFraction > 0.9f) MaterialTheme.colorScheme.background else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentHeight)
            ) {
                Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    // Back button stays at the top
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .height(72.dp)
                            .padding(start = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppBackButton(onBack = onBack, settingsDataStore = settingsDataStore, isRoot = isRoot)
                    }

                    // Actions stay at the top
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .height(72.dp)
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )

                    // Morphing Title
                    val expandedFontSize = remember(title) {
                        val words = title.split(" ")
                        val longestWord = words.maxByOrNull { it.length }?.length ?: 0
                        val baseSize = 64f
                        
                        // Aggressively scale down based on word length to ensure it fits width
                        if (longestWord > 6) {
                            (baseSize * (6.5f / longestWord)).coerceAtLeast(30f).sp
                        } else {
                            baseSize.sp
                        }
                    }
                    val collapsedFontSize = 22.sp
                    val fontSize = (expandedFontSize.value - (expandedFontSize.value - collapsedFontSize.value) * collapsedFraction).sp
                    
                    val titleStartPadding = (20 + (72 - 20) * collapsedFraction).dp
                    
                    // Use newlines to force stacking for multi-word titles when expanded
                    val displayTitle = remember(title, collapsedFraction) {
                        if (collapsedFraction < 0.5f && title.contains(" ")) {
                            title.replace(" ", "\n")
                        } else {
                            title
                        }
                    }
                    
                    Text(
                        text = displayTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = fontSize,
                        lineHeight = (fontSize.value * 0.95f).sp, // Tight stacking for multi-line
                        softWrap = false,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        fontFamily = FontFamily(
                            Font(
                                resId = R.font.gflex_variable,
                                variationSettings = FontVariation.Settings(
                                    FontVariation.weight((636 - 36 * collapsedFraction).toInt()),
                                    FontVariation.width(152f - 22f * collapsedFraction),
                                    FontVariation.Setting("ROND", 50f),
                                    FontVariation.Setting("XTRA", 520f - 70f * collapsedFraction),
                                    FontVariation.Setting("YOPQ", 90f),
                                    FontVariation.Setting("YTLC", 505f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart) // Anchor to bottom to avoid clipping and excessive gaps
                            .padding(
                                start = titleStartPadding, 
                                bottom = (4.dp * (1 - collapsedFraction)), // Very minimal gap above the list as requested
                                end = 16.dp
                            )
                            // center vertically in 72dp area when collapsed
                            .heightIn(min = 72.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        content = content
    )
}