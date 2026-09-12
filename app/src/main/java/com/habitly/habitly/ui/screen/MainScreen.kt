/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.habitly.habitly.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.habitly.habitly.R
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitDao
import com.habitly.habitly.data.Database.HabitDatabase
import com.habitly.habitly.data.Database.HabitViewModel
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.data.Database.HabitsUiState
import com.habitly.habitly.data.Database.getEffectiveStartDateMillis
import com.habitly.habitly.data.Database.getDailyTarget
import com.habitly.habitly.data.Database.normalizeToStartOfDay
import com.habitly.habitly.data.Database.normalizeToEndOfDay
import com.habitly.habitly.data.settings.DefaultSettings
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.notifications.NotificationScheduler
import com.habitly.habitly.ui.HabitDetailScreen
import com.habitly.habitly.ui.HabitItemCard
import com.habitly.habitly.ui.HabitSheetContent
import com.habitly.habitly.ui.SaveHabitButton
import com.habitly.habitly.ui.colors.habitColors
import com.habitly.habitly.ui.components.CustomTimePickerDialog
import com.habitly.habitly.ui.components.rememberNotificationPermissionHandler
import com.habitly.habitly.ui.components.ProvideRotatingIconRotation
import com.habitly.habitly.ui.defaultHabitIconKey
import com.habitly.habitly.ui.screen.settings.SettingsScreen
import com.habitly.habitly.ui.screen.settings.settingsEnterTransition
import com.habitly.habitly.ui.screen.settings.settingsPopExitTransition
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalSharedTransitionApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ExpressiveMainScreen(viewModel: HabitViewModel, habitDao: HabitDao, db: HabitDatabase, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val notificationScheduler = remember { NotificationScheduler(context) }

    val notificationPermissionHandler = rememberNotificationPermissionHandler {
        // Optional logic when permission is granted
    }

    val hasAskedPermissionState = settingsDataStore.hasAskedNotificationPermission.collectAsState(initial = null)
    val hasAskedPermission = hasAskedPermissionState.value

    LaunchedEffect(hasAskedPermission, notificationPermissionHandler.hasPermission) {
        if (hasAskedPermission == false && !notificationPermissionHandler.hasPermission) {
            notificationPermissionHandler.requestPermission()
        }
    }

    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val archivedHabitsUiState by viewModel.archivedHabitsUiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var currentDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentDateMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val startOfDay by remember {
        derivedStateOf {
            Calendar.getInstance().apply {
                timeInMillis = currentDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    val endOfDay by remember {
        derivedStateOf {
            Calendar.getInstance().apply {
                timeInMillis = currentDateMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
    }

    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = null)
    val showMonthLabels by settingsDataStore.monthLabels.collectAsState(initial = null)
    val showYearDivider by settingsDataStore.yearDivider.collectAsState(initial = null)
    val lineChartYearDivider by settingsDataStore.lineChartYearDivider.collectAsState(initial = DefaultSettings.LINE_CHART_YEAR_DIVIDER)
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = null)
    val heatmapNotificationDot by settingsDataStore.heatmapNotificationDot.collectAsState(initial = null)
    val heatmapNotificationDotDetailOnly by settingsDataStore.heatmapNotificationDotDetailOnly.collectAsState(initial = null)
    val heatmapNotificationDotRange by settingsDataStore.heatmapNotificationDotRange.collectAsState(initial = null)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = true)
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(initial = setOf("Heatmap", "Line Chart"))
    val heatmapVisibleDays by settingsDataStore.heatmapVisibleDays.collectAsState(initial = null)
    val dayOfWeekLabelsOnRight by settingsDataStore.dayOfWeekLabelsOnRight.collectAsState(initial = null)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = false)
    val heroCardVisible by settingsDataStore.heroCardVisible.collectAsState(initial = true)
    val heatmapScrolling by settingsDataStore.heatmapScrolling.collectAsState(initial = false)
    val heatmapWeeks by settingsDataStore.heatmapWeeks.collectAsState(initial = DefaultSettings.HEATMAP_WEEKS)
    val heatmapInfinite by settingsDataStore.heatmapInfinite.collectAsState(initial = DefaultSettings.HEATMAP_INFINITE)
    val firstDayOfWeekCalendar by settingsDataStore.firstDayOfWeekCalendar.collectAsState(initial = java.util.Calendar.MONDAY)
    val autoScrollText by settingsDataStore.autoScrollText.collectAsState(initial = DefaultSettings.AUTO_SCROLL_TEXT)
    val autoScrollTextElements by settingsDataStore.autoScrollTextElements.collectAsState(
        initial = DefaultSettings.AUTO_SCROLL_TEXT_ELEMENTS.split(',').filter { it.isNotEmpty() }.toSet()
    )
    val autoScrollTextScreens by settingsDataStore.autoScrollTextScreens.collectAsState(
        initial = DefaultSettings.AUTO_SCROLL_TEXT_SCREENS.split(',').filter { it.isNotEmpty() }.toSet()
    )

    // Additional settings for consistent Shared Element Transition colors/animations
    val reduceMovement by settingsDataStore.reduceMovement.collectAsState(initial = false)
    val reduceMovementTargets by settingsDataStore.reduceMovementTargets.collectAsState(initial = emptySet())
    val disableAnimations = reduceMovement && "Rotation" in reduceMovementTargets

    val useHabitColorForCard by settingsDataStore.useHabitColorForCard.collectAsState(initial = true)
    val habitColorTargets by settingsDataStore.habitColorTargets.collectAsState(initial = setOf("Habit Cards", "Statistic Screen"))
    val theme by settingsDataStore.theme.collectAsState(initial = "system")

    val useHabitColorForItemCards = useHabitColorForCard && "Habit Cards" in habitColorTargets
    val useHabitColorForStatistics = useHabitColorForCard && "Statistic Screen" in habitColorTargets

    val areSettingsLoaded = borderContrast != null &&
            heatmapNotificationDot != null &&
            heatmapNotificationDotDetailOnly != null &&
            heatmapNotificationDotRange != null &&
            showMonthLabels != null &&
            showYearDivider != null &&
            showYearLabels != null &&
            heatmapVisibleDays != null &&
            dayOfWeekLabelsOnRight != null

    val greeting by remember {
        derivedStateOf {
            val hour = Calendar.getInstance().apply { timeInMillis = currentDateMillis }.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 1 .. 5 -> "It's a beautiful night!"
                in 6..14 -> "Good morning"
                in 15..19 -> "Good afternoon"
                in 20..24, 0 -> "Good evening"
                else -> "Welcome back"
            }
        }
    }

    val heroCardDescriptions = remember {
        listOf(
            "Track your habits, build your future.",
            "The secret of your future is hidden in your daily routine.",
            "Consistency is the key to success.",
            "Motivation is what gets you started. Habit is what keeps you going.",
            "A little progress each day adds up to big results."
        )
    }
    val heroCardDescription = remember { heroCardDescriptions.random() }

    var showHabitSheet by remember { mutableStateOf(false) }
    var habitToView by remember { mutableStateOf<HabitWithCompletions?>(null) }
    var lastNonNullHabitToView by remember { mutableStateOf<HabitWithCompletions?>(null) }
    if (habitToView != null) {
        lastNonNullHabitToView = habitToView
    }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf<Color?>(null) }
    var tempColor by remember { mutableStateOf<Color?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showArchiveSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }
    var showStatisticScreen by remember { mutableStateOf(false) }
    var initialHabitIdForStats by remember { mutableStateOf<String?>(null) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val isEditMode = habitToEdit != null
    val title = if (isEditMode) "Edit Habit" else "Add New Habit"
    val buttonText = if (isEditMode) "Save Changes" else "Save"
    var habitName by remember { mutableStateOf("") }
    var habitDescription by remember { mutableStateOf("") }
    var habitColor by remember { mutableStateOf(habitColors.first()) }
    var habitIconKey by remember { mutableStateOf(defaultHabitIconKey) }
    var completionsPerDay by remember { mutableStateOf("1") }
    var completionsPerInterval by remember { mutableStateOf("1") }
    var intervalUnit by remember { mutableStateOf("day") }
    var completionsError by remember { mutableStateOf<String?>(null) }
    var completionsPerDayError by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var notificationTime by remember { mutableStateOf<String?>("09:00") }

    val allDays = remember { setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
    var notificationDays by remember { mutableStateOf(allDays) }

    var isInverse by remember { mutableStateOf(false) }
    var invertCompletionsOnTypeChange by remember { mutableStateOf(true) }
    var targetConversionIsPercentage by remember { mutableStateOf(false) }
    var streakCountingDisabled by remember { mutableStateOf(false) }
    fun validateDaily(text: String) {
        val count = text.toIntOrNull()
        completionsPerDayError = if (count == null) {
            "Must be a number"
        } else if (count <= 0) {
            "Must be > 0"
        } else if (count > 14) {
            "Must be ≤ 14"
        } else {
            null
        }
    }

    fun validateInterval(intervalText: String, dailyText: String) {
        val daily = dailyText.toIntOrNull() ?: 1
        if (intervalUnit == "day") {
            if (daily > 1) {
                val completions = intervalText.toIntOrNull()
                completionsError = if (completions == null) {
                    "Must be a number"
                } else if (completions <= 0) {
                    "Must be > 0"
                } else if (completions > daily) {
                    "Must be ≤ $daily completions/day"
                } else {
                    null
                }
            } else {
                completionsError = null
            }
            return
        }
        val completions = intervalText.toIntOrNull()
        val maxTarget = if (intervalUnit == "week") 7 * daily else 31 * daily
        completionsError = if (completions == null) {
            "Must be a number"
        } else if (completions <= 0) {
            "Must be > 0"
        } else if (completions > maxTarget) {
            "Must be ≤ $maxTarget (${if (intervalUnit == "week") 7 else 31} days × $daily/day)"
        } else {
            null
        }
    }

    LaunchedEffect(completionsPerDay) {
        validateDaily(completionsPerDay)
        validateInterval(completionsPerInterval, completionsPerDay)
    }

    LaunchedEffect(completionsPerInterval, intervalUnit) {
        validateInterval(completionsPerInterval, completionsPerDay)
    }

    val isAnySheetOpen = showHabitSheet || showSettingsScreen || habitToView != null || showArchiveSheet || showReorderSheet || showStatisticScreen

    BackHandler(enabled = isAnySheetOpen || isFabMenuExpanded) {
        if (isFabMenuExpanded) { isFabMenuExpanded = false; return@BackHandler }
        if (showStatisticScreen) { showStatisticScreen = false; initialHabitIdForStats = null; return@BackHandler }
        if (showHabitSheet) {
            showHabitSheet = false
            return@BackHandler
        }
        if (showSettingsScreen) { showSettingsScreen = false; return@BackHandler }
        if (habitToView != null) { habitToView = null; return@BackHandler }
        if (showArchiveSheet) { showArchiveSheet = false; return@BackHandler }
        if (showReorderSheet) { showReorderSheet = false; return@BackHandler }
    }

    if (showTimePicker) {
        val initialHour = notificationTime?.split(":")?.get(0)?.toIntOrNull() ?: 9
        val initialMinute = notificationTime?.split(":")?.get(1)?.toIntOrNull() ?: 0
        CustomTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                notificationTime = String.format("%02d:%02d", hour, minute)
                showTimePicker = false
            },
            initialHour = initialHour,
            initialMinute = initialMinute,
            borderContrast = borderContrast!!,
            is24Hour = is24Hour,
            vibrationsEnabled = vibrationsEnabled
        )
    }

    val isColorPickerVisible = showColorPicker
    if (isColorPickerVisible) {
        val controller = rememberColorPickerController()
        AlertDialog(
            onDismissRequest = {
                showColorPicker = false
                tempColor = null
            },
            title = { Text("Choose a color") },
            text = {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    controller = controller,
                    onColorChanged = { colorEnvelope: ColorEnvelope -> tempColor = colorEnvelope.color }
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            showColorPicker = false
                            tempColor = null
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            customColor = tempColor
                            showColorPicker = false
                            tempColor = null
                        },
                        enabled = tempColor != Color.White,
                        shape = CircleShape
                    ) {
                        Text("OK")
                    }
                }
            },
            dismissButton = null,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }


    ProvideRotatingIconRotation {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SharedTransitionLayout {
            val sharedTransitionScope = this
            val lastViewedHabitId = remember { mutableStateOf<String?>(null) }
            if (habitToView != null) {
                lastViewedHabitId.value = habitToView?.habit?.id
            }
            val detailTransitionProgressState = animateFloatAsState(
                targetValue = if (habitToView != null) 1f else 0f,
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "detailTransitionProgress"
            )
            val editParallaxProgress by animateFloatAsState(
                targetValue = if (showHabitSheet) 1f else 0f,
                animationSpec = tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "editParallaxProgress"
            )
            val parallaxTranslationY = with(LocalDensity.current) { (-36).dp.toPx() } * editParallaxProgress
            val parallaxScale = 1f - 0.08f * editParallaxProgress
            val parallaxCornerRadius = 24.dp * editParallaxProgress
            val parallaxDim = 0.25f * editParallaxProgress

            val mainBlurRadius by animateDpAsState(
                targetValue = if (((habitToView != null || (isAnySheetOpen && !showHabitSheet)) || isFabMenuExpanded) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 16.dp else 0.dp,
                label = "mainBlurRadius"
            )

            val mainContentModifier = if (mainBlurRadius > 0.dp) {
                Modifier.blur(mainBlurRadius)
            } else {
                Modifier
            }

            val timePickerBlurRadius by animateDpAsState(
                targetValue = if (showTimePicker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 10.dp else 0.dp,
                label = "timePickerBlurRadius"
            )

            val timePickerBlurModifier = if (timePickerBlurRadius > 0.dp) {
                Modifier.blur(timePickerBlurRadius)
            } else {
                Modifier
            }

            Box(Modifier
                .fillMaxSize()
                .then(timePickerBlurModifier)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = parallaxTranslationY
                            scaleX = parallaxScale
                            scaleY = parallaxScale
                            shape = RoundedCornerShape(parallaxCornerRadius)
                            clip = editParallaxProgress > 0f
                        }
                        .drawWithContent {
                            drawContent()
                            if (parallaxDim > 0f) {
                                drawRect(Color.Black.copy(alpha = parallaxDim))
                            }
                        }
                ) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                    floatingActionButton = {
                        // Empty: FAB is hoisted to the parent Box to render on top of the shared element transition
                    },
                    floatingActionButtonPosition = FabPosition.End,
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            Surface(modifier = mainContentModifier.fillMaxSize(),color = MaterialTheme.colorScheme.surface) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AnimatedVisibility(
                                        visible = habitsUiState is HabitsUiState.Success && areSettingsLoaded,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        val habitsWithCompletions = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()

                                        if (habitsWithCompletions.isEmpty()) {
                                            BoxWithConstraints(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.BottomEnd
                                            ) {
                                                val scale = minOf(maxWidth.value / 400f, maxHeight.value / 750f).coerceAtMost(1f)
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 400.dp * scale, height = 750.dp * scale)
                                                        .offset(y = 32.dp * scale)
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.welcome_image),
                                                        contentDescription = null,
                                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Column(
                                                        modifier = Modifier
                                                            .offset(x = 25.dp * scale, y = 220.dp * scale)
                                                            .rotate(-3f),
                                                        horizontalAlignment = Alignment.Start
                                                    ) {
                                                        Text(
                                                            text = "Start by adding",
                                                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp * scale),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                        Text(
                                                            text = "a new habit",
                                                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp * scale),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                            modifier = Modifier.padding(start = 15.dp * scale)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        val lazyListState = rememberLazyListState()

                                        LazyColumn(
                                            state = lazyListState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            userScrollEnabled = !isAnySheetOpen && !isFabMenuExpanded
                                        ) {
                                            item {
                                                AnimatedVisibility(visible = heroCardVisible) {
                                                    HeroCard(greeting = greeting, description = heroCardDescription)
                                                }
                                            }
                                            items(
                                                items = habitsWithCompletions,
                                                key = { it.habit.id }
                                            ) { habitWithCompletions ->
                                                val isCompleted = com.habitly.habitly.data.Database.isDayCompleted(habitWithCompletions.habit, habitWithCompletions.completions, currentDateMillis, currentDateMillis)
                                                val isViewingThis = habitToView?.habit?.id == habitWithCompletions.habit.id

                                                val shadowColor = MaterialTheme.colorScheme.surfaceVariant.copy(/*alpha = 0.15f*/)

                                                Box {
                                                    if (isViewingThis) {
                                                        Box(
                                                            modifier = Modifier
                                                                .matchParentSize()
                                                                .padding(horizontal = 12.dp)
                                                                .background(shadowColor, MaterialTheme.shapes.medium)
                                                        )
                                                    }
                                                    HabitItemCard(
                                                        modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                            rememberSharedContentState(key = "card-${habitWithCompletions.habit.id}"),
                                                            visible = !isViewingThis,
                                                            boundsTransform = { _, _ -> tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing) }
                                                        ),
                                                        habit = habitWithCompletions.habit,
                                                        isCompleted = isCompleted,
                                                        completions = habitWithCompletions.completions,
                                                        showCheckbox = true,
                                                        showMonthLabels = showMonthLabels!!,
                                                        visibleDayLabels = heatmapVisibleDays!!,
                                                        dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight!!,
                                                        showYearDivider = showYearDivider!!,
                                                        showYearLabels = showYearLabels!!,
                                                        heatmapNotificationDot = heatmapNotificationDot!! && !heatmapNotificationDotDetailOnly!!,
                                                        heatmapNotificationDotRange = heatmapNotificationDotRange!!,
                                                        showScrollBlur = showScrollBlur && "Heatmap" in scrollBlurTargets,
                                                        borderContrast = borderContrast!!,
                                                        heatmapScrollEnabled = heatmapScrolling,
                                                        heatmapWeeks = heatmapWeeks,
                                                        heatmapInfinite = heatmapInfinite,
                                                        useHabitColor = useHabitColorForItemCards,
                                                        theme = theme,
                                                        disableAnimations = disableAnimations,
                                                        currentDateMillis = currentDateMillis,
                                                        firstDayOfWeek = firstDayOfWeekCalendar,
                                                        autoScrollText = autoScrollText,
                                                        autoScrollTextElements = autoScrollTextElements,
                                                        autoScrollTextScreens = autoScrollTextScreens,
                                                        vibrationsEnabled = vibrationsEnabled,
                                                        onComplete = {
                                                            if (vibrationsEnabled) {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.TextHandleMove
                                                                )
                                                            }
                                                            viewModel.toggleCompletion(
                                                                habitWithCompletions.habit,
                                                                Calendar.getInstance().apply { timeInMillis = currentDateMillis }
                                                            )
                                                        },
                                                        onDecrement = {
                                                            if (vibrationsEnabled) {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.TextHandleMove
                                                                )
                                                            }
                                                            viewModel.toggleCompletion(
                                                                habitWithCompletions.habit,
                                                                Calendar.getInstance().apply { timeInMillis = currentDateMillis },
                                                                decrement = true
                                                            )
                                                        },
                                                        onClick = {
                                                            habitToView = habitWithCompletions
                                                        },
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        visible = !isViewingThis,
                                                        transitionProgressProvider = {
                                                            if (isViewingThis || (habitToView == null && lastViewedHabitId.value == habitWithCompletions.habit.id))
                                                                detailTransitionProgressState.value
                                                            else 0f
                                                        }
                                                    )
                                                }
                                            }
                                            item {
                                                Spacer(modifier = Modifier.height(80.dp))
                                            }
                                            item {
                                                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                                            }
                                        }
                                    }
                                }
                            }
                            AnimatedVisibility(
                                visible = isFabMenuExpanded,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { isFabMenuExpanded = false }
                                )
                            }
                        }
                    }
                )

                AnimatedVisibility(
                    visible = showArchiveSheet,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showArchiveSheet = false }
                    )
                }

                AnimatedVisibility(
                    visible = showArchiveSheet,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it }
                ) {
                    ArchiveScreen(
                        uiState = archivedHabitsUiState,
                        habitDao = habitDao,
                        onBack = { showArchiveSheet = false },
                        borderContrast = borderContrast ?: 0f,
                        showMonthLabels = showMonthLabels ?: false,
                        showYearDivider = showYearDivider ?: false,
                        showYearLabels = showYearLabels ?: false,
                        heatmapNotificationDotDetailOnly = heatmapNotificationDotDetailOnly ?: false,
                        heatmapNotificationDot = if (heatmapNotificationDotDetailOnly == true) false else heatmapNotificationDot ?: false,
                        heatmapNotificationDotRange = heatmapNotificationDotRange ?: DefaultSettings.HEATMAP_NOTIFICATION_DOT_RANGE,
                        heatmapVisibleDays = heatmapVisibleDays ?: emptySet(),
                        dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight ?: false,
                        vibrationsEnabled = vibrationsEnabled,
                        useHabitColor = useHabitColorForItemCards,
                        disableAnimations = disableAnimations,
                        heatmapWeeks = heatmapWeeks,
                        heatmapInfinite = heatmapInfinite,
                        currentDateMillis = currentDateMillis,
                        autoScrollText = autoScrollText,
                        autoScrollTextElements = autoScrollTextElements,
                        autoScrollTextScreens = autoScrollTextScreens
                    )
                }

                AnimatedVisibility(
                    visible = showReorderSheet,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showReorderSheet = false }
                    )
                }

                AnimatedVisibility(
                    visible = showReorderSheet,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it }
                ) {
                    ReorderScreen(
                        habitViewModel = viewModel,
                        onBack = { showReorderSheet = false },
                        borderContrast = borderContrast ?: 0f,
                        vibrationsEnabled = vibrationsEnabled,
                        useHabitColor = useHabitColorForItemCards,
                        disableAnimations = disableAnimations
                    )
                }

                AnimatedVisibility(
                    visible = habitToView != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { habitToView = null }
                    )
                }

                AnimatedVisibility(
                    visible = habitToView != null,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
                    exit = ExitTransition.None
                ) {
                    lastNonNullHabitToView?.let { habitWithCompletions ->
                        val habitState by remember(habitsUiState, habitWithCompletions) {
                            derivedStateOf {
                                (habitsUiState as? HabitsUiState.Success)?.habits
                                    ?.find { it.habit.id == habitWithCompletions.habit.id } ?: habitWithCompletions
                            }
                        }

                        HabitDetailScreen(
                            habitWithCompletions = habitState,
                            viewModel = viewModel,
                            isArchivedView = false,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onDismiss = { habitToView = null },
                            onEditHabit = {
                                habitName = it.name
                                habitDescription = it.description
                                habitColor = Color(it.color)
                                habitIconKey = it.icon
                                completionsPerDay = it.getDailyTarget().toString()
                                completionsPerInterval = it.completionsPerInterval.toString()
                                intervalUnit = it.intervalUnit
                                notificationsEnabled = it.notificationsEnabled
                                notificationTime = it.notificationTime ?: "09:00"
                                notificationDays = it.notificationDays?.split(',')?.toSet() ?: allDays
                                customColor = null
                                isInverse = it.isInverse
                                invertCompletionsOnTypeChange = true
                                targetConversionIsPercentage = false
                                streakCountingDisabled = it.streakCountingDisabled
                                habitToEdit = it
                                showHabitSheet = true
                            },
                            onShowStatistics = { habit ->
                                initialHabitIdForStats = habit.id
                                showStatisticScreen = true
                            },
                            borderContrast = borderContrast!!,
                            showScrollBlur = showScrollBlur && "Heatmap" in scrollBlurTargets,
                            showYearLabels = showYearLabels!!,
                            heatmapNotificationDotDetailOnly = heatmapNotificationDotDetailOnly!!,
                            heatmapNotificationDot = heatmapNotificationDot!!,
                            heatmapNotificationDotRange = heatmapNotificationDotRange!!,
                            showYearDivider = showYearDivider!!,
                            vibrationsEnabled = vibrationsEnabled,
                            showMonthLabels = showMonthLabels!!,
                            dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight!!,
                            heatmapVisibleDays = heatmapVisibleDays!!,
                            disableAnimations = disableAnimations,
                            useHabitColor = useHabitColorForItemCards,
                            theme = theme,
                            heatmapWeeks = heatmapWeeks,
                            heatmapInfinite = heatmapInfinite,
                            currentDateMillis = currentDateMillis,
                            isEditSheetOpen = false,
                            transitionProgressProvider = { detailTransitionProgressState.value },
                            firstDayOfWeek = firstDayOfWeekCalendar,
                            is24Hour = is24Hour,
                            autoScrollText = autoScrollText,
                            autoScrollTextElements = autoScrollTextElements,
                            autoScrollTextScreens = autoScrollTextScreens
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showStatisticScreen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                showStatisticScreen = false
                                initialHabitIdForStats = null
                            }
                    )
                }

                AnimatedVisibility(
                    visible = showStatisticScreen,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInVertically(animationSpec = tween(durationMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it } + fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = slideOutVertically(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it } + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    StatisticScreen(
                        viewModel = viewModel,
                        onBack = {
                            showStatisticScreen = false
                            initialHabitIdForStats = null
                        },
                        initialHabitId = initialHabitIdForStats,
                        borderContrast = borderContrast ?: 0f,
                        vibrationsEnabled = vibrationsEnabled,
                        showScrollBlur = showScrollBlur,
                        scrollBlurTargets = scrollBlurTargets,
                        useHabitColor = useHabitColorForStatistics,
                        firstDayOfWeek = firstDayOfWeekCalendar,
                        showYearDivider = lineChartYearDivider
                    )
                }

                AnimatedVisibility(
                    visible = showSettingsScreen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showSettingsScreen = false }
                    )
                }

                AnimatedVisibility(
                    visible = showSettingsScreen,
                    modifier = Modifier.fillMaxSize(),
                    enter = settingsEnterTransition(),
                    exit = settingsPopExitTransition()
                ) {
                    SettingsScreen(
                        onDismiss = { showSettingsScreen = false },
                        db = HabitDatabase.getDatabase(context),
                        settingsDataStore = settingsDataStore,
                        vibrationsEnabled = vibrationsEnabled,
                        borderContrast = borderContrast ?: 0f,
                        is24Hour = is24Hour,
                        theme = theme
                    )
                }

                } // End of parallax Box

                AnimatedVisibility(
                    visible = showHabitSheet,
                    modifier = Modifier.fillMaxSize(),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
                ) {
                    val habitsForEdit = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()
                    val existingCompletionsForEdit = remember(habitToEdit, habitsForEdit) {
                        habitsForEdit.find { it.habit.id == habitToEdit?.id }?.completions ?: emptyList()
                    }
                    val previewCompletions = remember(
                        isEditMode,
                        existingCompletionsForEdit,
                        isInverse,
                        invertCompletionsOnTypeChange,
                        targetConversionIsPercentage,
                        completionsPerDay,
                        completionsPerInterval,
                        intervalUnit,
                        habitToEdit
                    ) {
                        val editingHabit = habitToEdit
                        if (isEditMode && editingHabit != null) {
                            val isTypeChanged = editingHabit.isInverse != isInverse
                            val oldTarget = editingHabit.getDailyTarget()
                            val newTarget = completionsPerDay.toIntOrNull()?.coerceIn(1, 14) ?: 1
                            val isTargetChanged = oldTarget != newTarget

                            if (isTypeChanged && !invertCompletionsOnTypeChange) {
                                // "Keep History": simulate converted completions in preview
                                val oldHabit = editingHabit
                                val startDate = normalizeToStartOfDay(oldHabit.getEffectiveStartDateMillis())
                                val todayEnd = normalizeToEndOfDay(System.currentTimeMillis())

                                val cal = Calendar.getInstance().apply { timeInMillis = startDate }
                                val converted = mutableListOf<Completion>()
                                while (cal.timeInMillis <= todayEnd) {
                                    val dayStart = normalizeToStartOfDay(cal.timeInMillis)
                                    val dayEnd = normalizeToEndOfDay(cal.timeInMillis)
                                    val dbAmount = existingCompletionsForEdit
                                        .filter { it.date in dayStart..dayEnd }
                                        .sumOf { it.amountOfCompletions }
                                    val oldEffective = if (oldHabit.isInverse) {
                                        (oldTarget - dbAmount).coerceAtLeast(0)
                                    } else {
                                        dbAmount
                                    }
                                    val scaledOldEffective = if (targetConversionIsPercentage && isTargetChanged) {
                                        Math.round(oldEffective.toFloat() * newTarget / oldTarget.toFloat())
                                            .toInt()
                                            .coerceIn(0, newTarget)
                                    } else {
                                        oldEffective
                                    }
                                    if (isInverse) {
                                        val slips = (newTarget - scaledOldEffective).coerceAtLeast(0)
                                        if (slips > 0) {
                                            converted.add(
                                                Completion(
                                                    id = UUID.randomUUID().toString(),
                                                    habitId = oldHabit.id,
                                                    date = dayStart + 12 * 3600 * 1000L,
                                                    timezoneOffsetInMinutes = 0,
                                                    amountOfCompletions = slips
                                                )
                                            )
                                        }
                                    } else {
                                        val completions = scaledOldEffective.coerceAtMost(newTarget)
                                        if (completions > 0) {
                                            converted.add(
                                                Completion(
                                                    id = UUID.randomUUID().toString(),
                                                    habitId = oldHabit.id,
                                                    date = dayStart + 12 * 3600 * 1000L,
                                                    timezoneOffsetInMinutes = 0,
                                                    amountOfCompletions = completions
                                                )
                                            )
                                        }
                                    }
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                converted
                            } else if (isTargetChanged && targetConversionIsPercentage) {
                                // Scale completions by percentage
                                existingCompletionsForEdit.mapNotNull { comp ->
                                    val newAmount = Math.round(comp.amountOfCompletions.toFloat() * newTarget / oldTarget.toFloat())
                                        .toInt()
                                        .coerceIn(0, newTarget)
                                    if (newAmount > 0) {
                                        comp.copy(amountOfCompletions = newAmount)
                                    } else null
                                }
                            } else {
                                // "Invert" or Absolute mode: preview uses the raw completions as-is
                                existingCompletionsForEdit
                            }
                        } else {
                            val list = mutableListOf<Completion>()
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -60)
                            val random = java.util.Random(42) // Fixed seed for stable preview
                            val dailyTarget = completionsPerDay.toIntOrNull()?.coerceIn(1, 14) ?: 1

                            for (i in 0..60) {
                                val dayMillis = cal.timeInMillis
                                val isToday = (i == 60)

                                val isActive = if (isToday) {
                                    false
                                } else {
                                    random.nextFloat() < 0.65f
                                }
                                val amount = if (!isActive) {
                                    0
                                } else if (dailyTarget == 1) {
                                    1
                                } else {
                                    if (random.nextFloat() < 0.60f) {
                                        dailyTarget
                                    } else {
                                        1 + random.nextInt(dailyTarget)
                                    }
                                }

                                if (amount > 0) {
                                    list.add(
                                        Completion(
                                            id = UUID.randomUUID().toString(),
                                            habitId = "preview",
                                            date = dayMillis,
                                            timezoneOffsetInMinutes = 0,
                                            amountOfCompletions = amount
                                        )
                                    )
                                }
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            list
                        }
                    }

                    val livePreviewColor = if (showColorPicker) tempColor else customColor
                    val dummyHabit = remember(habitName, habitDescription, habitColor, customColor, habitIconKey, completionsPerInterval, completionsPerDay, intervalUnit, notificationsEnabled, notificationTime, notificationDays, livePreviewColor, isEditMode, isInverse, habitToEdit, streakCountingDisabled) {
                        val defaultCreated = (System.currentTimeMillis() - 60L * 24 * 3600 * 1000).toString()
                        val created = habitToEdit?.createdAt ?: if (isEditMode) System.currentTimeMillis().toString() else defaultCreated
                        val start = habitToEdit?.startDate ?: created
                        val daily = completionsPerDay.toIntOrNull() ?: 1
                        val intervalTarget = if (intervalUnit == "day") {
                            if (daily > 1) (completionsPerInterval.toIntOrNull()?.coerceIn(1, daily) ?: daily) else 1
                        } else {
                            (completionsPerInterval.toIntOrNull() ?: 1)
                        }
                        Habit(
                            id = habitToEdit?.id ?: "preview",
                            name = habitName.ifBlank { "Habit Name" },
                            description = habitDescription.ifBlank { "Description" },
                            color = (livePreviewColor ?: habitColor).toArgb(),
                            icon = habitIconKey,
                            orderIndex = 0,
                            createdAt = created,
                            isInverse = isInverse,
                            startDate = start,
                            archived = false,
                            emoji = null,
                            completionsPerInterval = intervalTarget,
                            intervalUnit = intervalUnit,
                            notificationsEnabled = notificationsEnabled,
                            notificationTime = notificationTime,
                            notificationDays = notificationDays.joinToString(","),
                            completionsPerDay = daily,
                            streakCountingDisabled = streakCountingDisabled
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val scrollState = rememberScrollState()
                            HabitSheetContent(
                                title = title,
                                habitName = habitName,
                                onHabitNameChanged = { habitName = it },
                                habitDescription = habitDescription,
                                onHabitDescriptionChanged = { habitDescription = it },
                                completionsPerDay = completionsPerDay,
                                onCompletionsPerDayChanged = { completionsPerDay = it },
                                completionsPerDayError = completionsPerDayError,
                                completionsPerInterval = completionsPerInterval,
                                onCompletionsPerIntervalChanged = { completionsPerInterval = it },
                                intervalUnit = intervalUnit,
                                onIntervalUnitChanged = { intervalUnit = it },
                                completionsError = completionsError,
                                habitIconKey = habitIconKey,
                                onHabitIconKeyChanged = { habitIconKey = it },
                                habitColor = habitColor,
                                onHabitColorChanged = { habitColor = it },
                                customColor = customColor,
                                onShowColorPicker = { show, color ->
                                    showColorPicker = show
                                    if (show) {
                                        tempColor = color
                                    }
                                },
                                onClearCustomColor = { customColor = null },
                                livePreviewColor = if (showColorPicker) tempColor else customColor,
                                scrollState = scrollState,
                                settingsDataStore = settingsDataStore,
                                notificationsEnabled = notificationsEnabled,
                                onNotificationsEnabledChanged = {
                                    if (notificationPermissionHandler.hasPermission) {
                                        notificationsEnabled = it
                                    } else {
                                        notificationPermissionHandler.requestPermission()
                                    }
                                },
                                notificationTime = notificationTime,
                                onTimePickerClick = {
                                    if (notificationPermissionHandler.hasPermission) {
                                        showTimePicker = true
                                    } else {
                                        notificationPermissionHandler.requestPermission()
                                    }
                                },
                                notificationDays = notificationDays,
                                onNotificationDaySelected = { day ->
                                    notificationDays = if (notificationDays.contains(day)) {
                                        notificationDays - day
                                    } else {
                                        notificationDays + day
                                    }
                                },
                                hasNotificationPermission = notificationPermissionHandler.hasPermission,
                                isInverse = isInverse,
                                onIsInverseChanged = { isInverse = it },
                                showInvertOptions = isEditMode && habitToEdit?.isInverse != isInverse,
                                invertCompletions = invertCompletionsOnTypeChange,
                                onInvertCompletionsChanged = { invertCompletionsOnTypeChange = it },
                                showTargetConversionOptions = isEditMode && (habitToEdit?.getDailyTarget() != (completionsPerDay.toIntOrNull() ?: 1)),
                                targetConversionIsPercentage = targetConversionIsPercentage,
                                onTargetConversionChanged = { targetConversionIsPercentage = it },
                                onClose = {
                                    showHabitSheet = false
                                },
                                streakCountingDisabled = streakCountingDisabled,
                                onStreakCountingDisabledChanged = { streakCountingDisabled = it },
                                previewContent = {
                                    HabitItemCard(
                                        habit = dummyHabit,
                                        isCompleted = false,
                                        completions = previewCompletions,
                                        showCheckbox = true,
                                        showMonthLabels = showMonthLabels!!,
                                        visibleDayLabels = heatmapVisibleDays!!,
                                        dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight!!,
                                        showYearDivider = showYearDivider!!,
                                        showYearLabels = showYearLabels!!,
                                        heatmapNotificationDot = heatmapNotificationDot!!,
                                        heatmapNotificationDotRange = heatmapNotificationDotRange!!,
                                        showScrollBlur = false,
                                        borderContrast = borderContrast!!,
                                        heatmapScrollEnabled = false,
                                        heatmapWeeks = heatmapWeeks,
                                        heatmapInfinite = heatmapInfinite,
                                        useHabitColor = useHabitColorForItemCards,
                                        disableAnimations = disableAnimations,
                                        isPreview = true,
                                        onComplete = { /* Do nothing in preview */ },
                                        onClick = { /* Do nothing in preview */ },
                                        sharedTransitionScope = null,
                                        visible = true,
                                        detailBgColor = Color(dummyHabit.color).copy(alpha = 0.1f),
                                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
                                        currentDateMillis = currentDateMillis,
                                        animateTileChanges = true,
                                        firstDayOfWeek = firstDayOfWeekCalendar,
                                        autoScrollText = autoScrollText,
                                        autoScrollTextElements = autoScrollTextElements,
                                        autoScrollTextScreens = autoScrollTextScreens
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            )

                            val habits = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()
                            SaveHabitButton(
                                buttonText = buttonText,
                                isEnabled = habitName.trim().isNotBlank() && completionsError == null && completionsPerDayError == null,
                                settingsDataStore = settingsDataStore,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                val trimmedName = habitName.trim()
                                if (trimmedName.isNotBlank()) {
                                    val currentHabitToEdit = habitToEdit
                                    val daily = completionsPerDay.toIntOrNull() ?: 1
                                    val intervalTarget = if (intervalUnit == "day") {
                                        if (daily > 1) (completionsPerInterval.toIntOrNull()?.coerceIn(1, daily) ?: daily) else 1
                                    } else {
                                        (completionsPerInterval.toIntOrNull() ?: 1)
                                    }
                                    scope.launch {
                                        if (currentHabitToEdit != null) {
                                            val updatedHabit = currentHabitToEdit.copy(
                                                name = trimmedName,
                                                description = habitDescription,
                                                icon = habitIconKey,
                                                color = (customColor ?: habitColor).toArgb(),
                                                isInverse = isInverse,
                                                startDate = currentHabitToEdit.startDate ?: currentHabitToEdit.createdAt,
                                                completionsPerInterval = intervalTarget,
                                                intervalUnit = intervalUnit,
                                                completionsPerDay = daily,
                                                notificationsEnabled = notificationsEnabled,
                                                notificationTime = if (notificationsEnabled) notificationTime else null,
                                                notificationDays = if (notificationsEnabled) notificationDays.joinToString(
                                                    ","
                                                ) else null,
                                                streakCountingDisabled = streakCountingDisabled
                                            )
                                            viewModel.updateHabitWithConversion(
                                                currentHabitToEdit,
                                                updatedHabit,
                                                invertCompletions = invertCompletionsOnTypeChange,
                                                targetConversionMode = if (targetConversionIsPercentage)
                                                    com.habitly.habitly.data.Database.TargetConversionMode.PERCENTAGE
                                                else
                                                    com.habitly.habitly.data.Database.TargetConversionMode.ABSOLUTE
                                            )
                                            if (updatedHabit.notificationsEnabled) {
                                                notificationScheduler.scheduleNotification(updatedHabit)
                                            } else {
                                                notificationScheduler.cancelNotification(updatedHabit)
                                            }
                                            habitToView = habitToView?.copy(habit = updatedHabit)
                                                ?: habits.find { it.habit.id == updatedHabit.id }
                                        } else {
                                            val newHabit = Habit(
                                                id = UUID.randomUUID().toString(),
                                                name = trimmedName,
                                                description = habitDescription,
                                                icon = habitIconKey,
                                                color = (customColor ?: habitColor).toArgb(),
                                                archived = false,
                                                orderIndex = habits.size,
                                                createdAt = System.currentTimeMillis().toString(),
                                                startDate = System.currentTimeMillis().toString(),
                                                isInverse = isInverse,
                                                emoji = null,
                                                completionsPerInterval = intervalTarget,
                                                intervalUnit = intervalUnit,
                                                completionsPerDay = daily,
                                                notificationsEnabled = notificationsEnabled,
                                                notificationTime = if (notificationsEnabled) notificationTime else null,
                                                notificationDays = if (notificationsEnabled) notificationDays.joinToString(
                                                    ","
                                                ) else null,
                                                streakCountingDisabled = streakCountingDisabled
                                            )
                                            habitDao.insertHabit(newHabit)
                                            if (newHabit.notificationsEnabled) {
                                                notificationScheduler.scheduleNotification(newHabit)
                                            }
                                        }
                                        showHabitSheet = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = habitsUiState is HabitsUiState.Loading || !areSettingsLoaded,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        ) {
            Surface(modifier = Modifier.fillMaxSize(),color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomEnd),
            visible = habitsUiState is HabitsUiState.Success && !isAnySheetOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FabMenu(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(end = 16.dp, bottom = 16.dp)/*
                    .offset(x = 8.dp, y = 20.dp) */,
                expanded = isFabMenuExpanded,
                onExpandedChange = { isFabMenuExpanded = it },
                onAddHabit = {
                    habitName = ""
                    habitDescription = ""
                    habitColor = habitColors.first()
                    habitIconKey = defaultHabitIconKey
                    completionsPerDay = "1"
                    completionsPerInterval = "1"
                    intervalUnit = "day"
                    notificationsEnabled = false
                    notificationTime = "09:00"
                    notificationDays = allDays
                    customColor = null
                    isInverse = false
                    invertCompletionsOnTypeChange = true

                    streakCountingDisabled = false
                    habitToEdit = null
                    showHabitSheet = true
                },
                onShowArchived = { showArchiveSheet = true },
                onShowSettings = { showSettingsScreen = true },
                onShowReorder = { showReorderSheet = true },
                onShowStatistics = {
                    initialHabitIdForStats = null
                    showStatisticScreen = true
                },
                settingsDataStore = settingsDataStore
            )
        }
    }
}
}

@Composable
fun HeroCard(greeting: String, description: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(0.5f)
                        ),
                        start = Offset.Zero, end = Offset.Infinite
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
