/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.example.attempt3.R
import com.example.attempt3.data.Database.Completion
import com.example.attempt3.data.Database.Habit
import com.example.attempt3.data.Database.HabitDao
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.Database.HabitViewModel
import com.example.attempt3.data.Database.HabitWithCompletions
import com.example.attempt3.data.Database.HabitsUiState
import com.example.attempt3.data.settings.DefaultSettings
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.notifications.NotificationScheduler
import com.example.attempt3.ui.HabitDetailScreen
import com.example.attempt3.ui.HabitItemCard
import com.example.attempt3.ui.HabitSheetContent
import com.example.attempt3.ui.SaveHabitButton
import com.example.attempt3.ui.colors.habitColors
import com.example.attempt3.ui.components.CustomTimePickerDialog
import com.example.attempt3.ui.components.rememberNotificationPermissionHandler
import com.example.attempt3.ui.defaultHabitIconKey
import com.example.attempt3.ui.screen.settings.SettingsScreen
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import kotlin.math.roundToInt

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
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
    val showYearLabels by settingsDataStore.yearLabels.collectAsState(initial = null)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = true)
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(initial = setOf("Heatmap", "Line Chart"))
    val heatmapVisibleDays by settingsDataStore.heatmapVisibleDays.collectAsState(initial = null)
    val dayOfWeekLabelsOnRight by settingsDataStore.dayOfWeekLabelsOnRight.collectAsState(initial = null)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = false)
    val heroCardVisible by settingsDataStore.heroCardVisible.collectAsState(initial = true)
    val heatmapScrolling by settingsDataStore.heatmapScrolling.collectAsState(initial = false)
    val heatmapWeeks by settingsDataStore.heatmapWeeks.collectAsState(initial = DefaultSettings.HEATMAP_WEEKS)
    val heatmapInfinite by settingsDataStore.heatmapInfinite.collectAsState(initial = DefaultSettings.HEATMAP_INFINITE)

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
    var completionsPerInterval by remember { mutableStateOf("1") }
    var intervalUnit by remember { mutableStateOf("day") }
    var completionsError by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var notificationTime by remember { mutableStateOf<String?>("09:00") }

    val allDays = remember { setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
    var notificationDays by remember { mutableStateOf(allDays) }

    fun validate(completionsText: String) {
        if (intervalUnit == "day") {
            completionsError = null
            return
        }
        val completions = completionsText.toIntOrNull()
        completionsError = if (completions == null) {
            "Must be a number"
        } else if (completions <= 0) {
            "Must be > 0"
        } else if (completions > 7 && intervalUnit == "week") {
            "Must be ≤ 7"
        } else if (completions > 28 && intervalUnit == "month") {
            "Must be ≤ 28"
        }else {
            null
        }
    }

    LaunchedEffect(intervalUnit) {
        if (intervalUnit == "day") {
            completionsPerInterval = "1"
        }
        validate(completionsPerInterval)
    }

    LaunchedEffect(completionsPerInterval, intervalUnit) {
        validate(completionsPerInterval)
    }

    LaunchedEffect(showHabitSheet) {
        if (!showHabitSheet) {
            habitToEdit = null
        }
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
            is24Hour = is24Hour
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

    Box(Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            val mainBlurRadius by animateDpAsState(
                targetValue = if ((isAnySheetOpen || isFabMenuExpanded) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 16.dp else 0.dp,
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
                                                val isCompleted = habitWithCompletions.completions.any { it.date in startOfDay..endOfDay }
                                                val isEditingThis = isEditMode && habitToEdit?.id == habitWithCompletions.habit.id
                                                val isViewingThis = habitToView?.habit?.id == habitWithCompletions.habit.id

                                                val shadowColor = MaterialTheme.colorScheme.surfaceVariant.copy(/*alpha = 0.15f*/)

                                                Box {
                                                    if (isViewingThis || isEditingThis) {
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
                                                            visible = !isViewingThis && !isEditingThis,
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
                                                        showScrollBlur = showScrollBlur && "Heatmap" in scrollBlurTargets,
                                                        borderContrast = borderContrast!!,
                                                        heatmapScrollEnabled = heatmapScrolling,
                                                        heatmapWeeks = heatmapWeeks,
                                                        heatmapInfinite = heatmapInfinite,
                                                        useHabitColor = useHabitColorForItemCards,
                                                        disableAnimations = disableAnimations,
                                                        currentDateMillis = currentDateMillis,
                                                        onComplete = {
                                                            if (vibrationsEnabled) {
                                                                haptic.performHapticFeedback(
                                                                    HapticFeedbackType.TextHandleMove
                                                                )
                                                            }
                                                            viewModel.toggleCompletion(
                                                                habitWithCompletions.habit,
                                                                Calendar.getInstance().apply { timeInMillis = currentDateMillis },
                                                                isCompleted
                                                            )
                                                        },
                                                        onClick = {
                                                            habitToView = habitWithCompletions
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
                        heatmapVisibleDays = heatmapVisibleDays ?: emptySet(),
                        dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight ?: false,
                        vibrationsEnabled = vibrationsEnabled,
                        useHabitColor = useHabitColorForItemCards,
                        disableAnimations = disableAnimations,
                        heatmapWeeks = heatmapWeeks,
                        heatmapInfinite = heatmapInfinite,
                        currentDateMillis = currentDateMillis
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
                    habitToView?.let { habitWithCompletions ->
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
                                completionsPerInterval = it.completionsPerInterval.toString()
                                intervalUnit = it.intervalUnit
                                notificationsEnabled = it.notificationsEnabled
                                notificationTime = it.notificationTime ?: "09:00"
                                notificationDays = it.notificationDays?.split(',')?.toSet() ?: allDays
                                customColor = null

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
                            isEditSheetOpen = showHabitSheet
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
                    enter = slideInVertically(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it },
                    exit = slideOutVertically(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it }
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
                        currentDateMillis = currentDateMillis
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
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it }
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

                AnimatedVisibility(
                    visible = showHabitSheet,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                showHabitSheet = false
                            }
                    )
                }

                val sheetOffsetY = remember { Animatable(0f) }
                LaunchedEffect(showHabitSheet) {
                    if (showHabitSheet) sheetOffsetY.snapTo(0f)
                }

                AnimatedVisibility(
                    visible = showHabitSheet,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    val dismissThresholdPx = with(LocalDensity.current) { 175.dp.toPx() }
                    val scrollState = rememberScrollState()
                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            // Fixed signature: added 'source' parameter
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                val delta = available.y
                                return if (delta > 0 && sheetOffsetY.value > 0) {
                                    scope.launch { sheetOffsetY.snapTo(sheetOffsetY.value + delta) }
                                    Offset(0f, delta)
                                } else Offset.Zero
                            }

                            // Fixed signature: added 'consumed' and 'source' parameters
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                val delta = available.y
                                if (delta > 0) scope.launch { sheetOffsetY.snapTo(sheetOffsetY.value + delta) }
                                return Offset.Zero
                            }

                            override suspend fun onPreFling(available: Velocity): Velocity {
                                if (sheetOffsetY.value > 0) {
                                    if (sheetOffsetY.value > dismissThresholdPx) {
                                        showHabitSheet = false
                                    }
                                    else sheetOffsetY.animateTo(0f, spring())
                                    return available
                                }
                                return super.onPreFling(available)
                            }
                        }
                    }

                    val habitsForEdit = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()
                    val existingCompletionsForEdit = remember(habitToEdit, habitsForEdit) {
                        habitsForEdit.find { it.habit.id == habitToEdit?.id }?.completions ?: emptyList()
                    }
                    val previewCompletions = remember(isEditMode, existingCompletionsForEdit) {
                        if (isEditMode) {
                            existingCompletionsForEdit
                        } else {
                            val list = mutableListOf<Completion>()
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -60)
                            val random = java.util.Random(42) // Fixed seed for stable "random"
                            for (_i in 0..60) {
                                if (random.nextBoolean()) {
                                    list.add(
                                        Completion(
                                            id = UUID.randomUUID().toString(),
                                            habitId = "preview",
                                            date = cal.timeInMillis,
                                            timezoneOffsetInMinutes = 0,
                                            amountOfCompletions = 1
                                        )
                                    )
                                }
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            list
                        }
                    }

                    val livePreviewColor = if (showColorPicker) tempColor else customColor
                    val dummyHabit = remember(habitName, habitDescription, habitColor, customColor, habitIconKey, completionsPerInterval, intervalUnit, notificationsEnabled, notificationTime, notificationDays, livePreviewColor) {
                        Habit(
                            id = "preview",
                            name = habitName.ifBlank { "Habit Name" },
                            description = habitDescription.ifBlank { "Description" },
                            color = (livePreviewColor ?: habitColor).toArgb(),
                            icon = habitIconKey,
                            orderIndex = 0,
                            createdAt = System.currentTimeMillis().toString(),
                            isInverse = false,
                            archived = false,
                            emoji = null,
                            completionsPerInterval = completionsPerInterval.toIntOrNull() ?: 1,
                            intervalUnit = intervalUnit,
                            notificationsEnabled = notificationsEnabled,
                            notificationTime = notificationTime,
                            notificationDays = notificationDays.joinToString(",")
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.9f)
                            .offset { IntOffset(0, sheetOffsetY.value.roundToInt()) }
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        val previewKey = if (isEditMode) "card-${habitToEdit!!.id}" else "card-preview"
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
                            showScrollBlur = false,
                            borderContrast = borderContrast!!,
                            heatmapScrollEnabled = false,
                            heatmapWeeks = heatmapWeeks,
                            heatmapInfinite = heatmapInfinite,
                            useHabitColor = useHabitColorForItemCards,
                            disableAnimations = disableAnimations,
                            onComplete = { /* Do nothing in preview */ },
                            onClick = { /* Do nothing in preview */ },
                            modifier = Modifier
                                .sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = previewKey),
                                    visible = showHabitSheet,
                                    boundsTransform = { _, _ -> tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing) }
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            currentDateMillis = currentDateMillis
                        )

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            val headerModifier = Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        if (dragAmount > 0 || sheetOffsetY.value > 0) {
                                            scope.launch { sheetOffsetY.snapTo((sheetOffsetY.value + dragAmount).coerceAtLeast(0f)) }
                                        }
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            if (sheetOffsetY.value > dismissThresholdPx) {
                                                showHabitSheet = false
                                            } else {
                                                sheetOffsetY.animateTo(0f, spring())
                                            }
                                        }
                                    }
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = headerModifier
                                        .padding(vertical = 10.dp)
                                        .fillMaxWidth(0.15f)
                                        .height(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                )
                                HabitSheetContent(
                                    title = title,
                                    habitName = habitName,
                                    onHabitNameChanged = { habitName = it },
                                    habitDescription = habitDescription,
                                    onHabitDescriptionChanged = { habitDescription = it },
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
                                    headerModifier = headerModifier,
                                    onClose = {
                                        showHabitSheet = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showHabitSheet,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val habits = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()
                    SaveHabitButton(
                        buttonText = buttonText,
                        isEnabled = habitName.trim().isNotBlank() && completionsError == null,
                        settingsDataStore = settingsDataStore
                    ) {
                        val trimmedName = habitName.trim()
                        if (trimmedName.isNotBlank()) {
                            scope.launch {
                                if (isEditMode) {
                                    val updatedHabit = habitToEdit!!.copy(
                                        name = trimmedName,
                                        description = habitDescription,
                                        icon = habitIconKey,
                                        color = (customColor ?: habitColor).toArgb(),
                                        completionsPerInterval = completionsPerInterval.toIntOrNull()
                                            ?: 1,
                                        intervalUnit = intervalUnit,
                                        notificationsEnabled = notificationsEnabled,
                                        notificationTime = if (notificationsEnabled) notificationTime else null,
                                        notificationDays = if (notificationsEnabled) notificationDays.joinToString(
                                            ","
                                        ) else null
                                    )
                                    habitDao.updateHabit(updatedHabit)
                                    if (updatedHabit.notificationsEnabled) {
                                        notificationScheduler.scheduleNotification(updatedHabit)
                                    } else {
                                        notificationScheduler.cancelNotification(updatedHabit)
                                    }
                                    habitToView = habits.find { it.habit.id == updatedHabit.id }
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
                                        isInverse = false,
                                        emoji = null,
                                        completionsPerInterval = completionsPerInterval.toIntOrNull()
                                            ?: 1,
                                        intervalUnit = intervalUnit,
                                        notificationsEnabled = notificationsEnabled,
                                        notificationTime = if (notificationsEnabled) notificationTime else null,
                                        notificationDays = if (notificationsEnabled) notificationDays.joinToString(
                                            ","
                                        ) else null
                                    )
                                    habitDao.insertHabit(newHabit)
                                    if (newHabit.notificationsEnabled) {
                                        notificationScheduler.scheduleNotification(newHabit)
                                    }
                                }
                                showHabitSheet = false
                                // We purposefully do NOT clear habitToEdit instantly
                                // to ensure the exit animation transitions cleanly
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
                    .padding(end = 16.dp, bottom = 16.dp)
                    .offset(x = 8.dp, y = 20.dp),
                expanded = isFabMenuExpanded,
                onExpandedChange = { isFabMenuExpanded = it },
                onAddHabit = {
                    habitName = ""
                    habitDescription = ""
                    habitColor = habitColors.first()
                    habitIconKey = defaultHabitIconKey
                    completionsPerInterval = "1"
                    intervalUnit = "day"
                    notificationsEnabled = false
                    notificationTime = "09:00"
                    notificationDays = allDays
                    customColor = null

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
