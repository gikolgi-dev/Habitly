/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)

package com.habitly.habitly.ui.screen.settings

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.habitly.habitly.R
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.notifications.NotificationScheduler
import com.habitly.habitly.ui.AppBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SETTINGS_TRANSITION_DURATION = 300

// When pressing BACK: The Main Settings screen re-enters
fun settingsPopEnterTransition() =
    slideInHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 3 } // Mimic coming from below/behind by starting like a third of the screen
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

// When pressing BACK: The Appearance Settings screen exits
fun settingsPopExitTransition() =
    slideOutHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> fullWidth } // Slides entirely out to the right
    ) + scaleOut(
        targetScale = 0.8f, // Scaling down whilst having the shape of the screen
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

// When clicking FORWARD: The Appearance Settings screen enters
fun settingsEnterTransition() =
    slideInHorizontally(
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> fullWidth } // Slides in from the right
    ) + scaleIn(
        initialScale = 0.8f, // Scaling up whilst having the shape of the screen
        animationSpec = tween(SETTINGS_TRANSITION_DURATION, easing = FastOutSlowInEasing)
    )

// When clicking FORWARD: The Main Settings screen exits
fun settingsExitTransition() =
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
fun SettingsScreen(
    onDismiss: () -> Unit,
    db: HabitDatabase,
    settingsDataStore: SettingsDataStore,
    vibrationsEnabled: Boolean,
    borderContrast: Float,
    is24Hour: Boolean,
    theme: String
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val haptic = LocalHapticFeedback.current
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

    val blurModifier = Modifier

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
                borderContrast = borderContrast,
                isRoot = true,
            ) { paddingValues ->
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
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
                            title = "Notifications",
                            subtitle = "Manage daily and habit notifications",
                            icon = Icons.Default.Notifications,
                            iconBackgroundColor = MaterialTheme.colorScheme.tertiary,
                            iconColor = MaterialTheme.colorScheme.onTertiary,
                            settingsDataStore = settingsDataStore,
                            position = SettingsItemPosition.Alone
                        ) { navController.navigate("notifications") { launchSingleTop = true } }
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
                            HoldToClearSettingsItem(
                                title = "Clear all data",
                                subtitle = "Delete all habits and their completions",
                                icon = Icons.Default.Delete,
                                iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                                iconColor = MaterialTheme.colorScheme.onErrorContainer,
                                settingsDataStore = settingsDataStore,
                                vibrationsEnabled = vibrationsEnabled,
                                position = SettingsItemPosition.Bottom
                            ) {
                                scope.launch(Dispatchers.IO) {
                                    val scheduler = NotificationScheduler(context)
                                    scheduler.cancelAllNotifications()
                                    db.habitDao().clearAllTables()
                                }
                            }
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
                borderContrast = borderContrast,
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
                    onNavigateToHabitColor = { navController.navigate("habit_color") { launchSingleTop = true } },
                    onNavigateToReduceMovement = { navController.navigate("reduce_movement") { launchSingleTop = true } }
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
                borderContrast = borderContrast,
            ) { paddingValues ->
                GeneralSettingsScreen(
                    settingsDataStore = settingsDataStore,
                    onNavigateToHeatmapWeeks = { navController.navigate("heatmap_weeks") { launchSingleTop = true } },
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }

        composable(
            route = "heatmap_weeks"
        ) {
            SettingsScaffold(
                title = "Week limit",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                borderContrast = borderContrast,
            ) { paddingValues ->
                HeatmapWeeksSubScreen(
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
                borderContrast = borderContrast,
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
                borderContrast = borderContrast,
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
                borderContrast = borderContrast,
            ) { paddingValues ->
                HabitColorSubScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }

        composable(
            route = "reduce_movement"
        ) {
            SettingsScaffold(
                title = "Reduce Movement",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                borderContrast = borderContrast,
            ) { paddingValues ->
                ReduceMovementSubScreen(
                    settingsDataStore = settingsDataStore,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }

        composable(
            route = "notifications"
        ) {
            SettingsScaffold(
                title = "Notifications",
                onBack = {
                    if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    navController.popBackStack()
                },
                borderContrast = borderContrast,
            ) { paddingValues ->
                NotificationSettingsScreen(
                    settingsDataStore = settingsDataStore,
                    is24Hour = is24Hour,
                    borderContrast = borderContrast,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedVisibilityScope.SettingsScaffold(
    modifier: Modifier = Modifier,
    title: String,
    onBack: () -> Unit,
    borderContrast: Float,
    isRoot: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val density = LocalDensity.current

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Use fixed base height, content list will scroll under it.
    // Reducing maxHeight for one word title.
    val hasSpace = title.contains(" ")
    val expandedMaxHeight = if (hasSpace) 220.dp else 180.dp
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
            .graphicsLayer {
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
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
                        AppBackButton(onBack = onBack, borderContrast = borderContrast, isRoot = isRoot)
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
                        val baseSize = 56
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
                        lineHeight = (fontSize.value * 0.95f).sp,
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
                            .align(Alignment.BottomStart)
                            .padding(
                                start = titleStartPadding,
                                top = 3.dp + (32.dp *(1-collapsedFraction)),
                                bottom = 0.dp,
                                end = 16.dp
                            )
                            .heightIn(min = 72.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content
    )
}

private enum class ClearState {
    Idle, Holding, HoldingComplete, Success
}

@Composable
private fun HoldToClearSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color,
    settingsDataStore: SettingsDataStore,
    vibrationsEnabled: Boolean,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    onHoldComplete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isPressing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var clearState by remember { mutableStateOf(ClearState.Idle) }

    LaunchedEffect(isPressing) {
        if (isPressing && clearState == ClearState.Idle) {
            clearState = ClearState.Holding
            val duration = 3000L
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            // Launch the ticking haptics in a child coroutine so it cancels automatically
            val hapticJob = launch {
                var nextDelay = 300L
                while (isPressing && progress.value < 1f) {
                    delay(nextDelay)
                    if (isPressing && progress.value < 1f && vibrationsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    val p = progress.value
                    nextDelay = when {
                        p > 0.9f -> 40L
                        p > 0.8f -> 60L
                        p > 0.6f -> 100L
                        p > 0.4f -> 150L
                        p > 0.2f -> 220L
                        else -> 300L
                    }
                }
            }

            // Animate progress to 1f. If this coroutine is cancelled (e.g. key changed), the animation cancels.
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration.toInt(), easing = LinearEasing)
            )

            hapticJob.cancel()

            if (progress.value >= 1f) {
                clearState = ClearState.HoldingComplete

                // Aggressive haptics pause (0.5s) runs inside this LaunchedEffect!
                // If the user releases, isPressing becomes false, cancelling this LaunchedEffect.
                if (vibrationsEnabled) {
                    for (i in 0..5) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(80L)
                    }
                } else {
                    delay(500L)
                }

                // If we get here, the user held for the full 3s + 0.5s pause without releasing!
                // Now we perform the deletion and display the success checkmark.
                // We launch this final phase in the external coroutineScope so that releasing
                // the touch AFTER success starts does not abort the 1-second success screen.
                coroutineScope.launch {
                    clearState = ClearState.Success
                    onHoldComplete()
                    delay(1000L)
                    clearState = ClearState.Idle
                    progress.snapTo(0f)
                }
            }
        } else {
            if (clearState == ClearState.Holding || clearState == ClearState.HoldingComplete) {
                clearState = ClearState.Idle
                if (progress.value > 0f) {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            }
        }
    }

    val successColor = MaterialTheme.colorScheme.error
    val progressColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)

    val animatedBgColor by animateColorAsState(
        targetValue = when (clearState) {
            ClearState.Success -> successColor
            ClearState.Holding, ClearState.HoldingComplete -> progressColor
            ClearState.Idle -> Color.Transparent
        },
        animationSpec = tween(
            durationMillis = when (clearState) {
                ClearState.Success -> 300
                ClearState.Idle -> 300
                else -> 150
            }
        )
    )

    val successAlpha by animateFloatAsState(
        targetValue = if (clearState == ClearState.Success) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (clearState == ClearState.Success) 0f else 1f,
        animationSpec = tween(durationMillis = 300)
    )
    val successRotation by animateFloatAsState(
        targetValue = if (clearState == ClearState.Success) 0f else -180f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    SettingsItemBox(settingsDataStore = settingsDataStore, position = position) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { layoutSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (clearState == ClearState.Success || clearState == ClearState.HoldingComplete) {
                            return@awaitEachGesture
                        }
                        isPressing = true

                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                break
                            }
                            val pointer = event.changes.firstOrNull { it.pressed }
                            if (pointer != null) {
                                val pos = pointer.position
                                if (pos.x < 0 || pos.x > layoutSize.width || pos.y < 0 || pos.y > layoutSize.height) {
                                    break
                                }
                            }
                        }
                        isPressing = false
                    }
                }
                .drawBehind {
                    val width = if (clearState == ClearState.Success) {
                        size.width
                    } else {
                        size.width * progress.value
                    }
                    if (width > 0f) {
                        drawRect(
                            color = animatedBgColor,
                            size = Size(width = width, height = size.height)
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .graphicsLayer {
                        alpha = contentAlpha
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RotatingCookie(
                    icon = icon,
                    iconBackgroundColor = iconBackgroundColor,
                    iconColor = iconColor,
                    settingsDataStore = settingsDataStore,
                    contentDescription = title
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = successAlpha
                        scaleX = 0.8f + 0.2f * successAlpha
                        scaleY = 0.8f + 0.2f * successAlpha
                        rotationZ = successRotation
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
