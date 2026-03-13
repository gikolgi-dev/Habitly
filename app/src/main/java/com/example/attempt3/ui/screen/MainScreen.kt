/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui.screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.Database.Habit
import com.example.attempt3.data.Database.HabitDao
import com.example.attempt3.data.Database.HabitDatabase
import com.example.attempt3.data.Database.HabitViewModel
import com.example.attempt3.data.Database.HabitWithCompletions
import com.example.attempt3.data.Database.HabitsUiState
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

    val now = Calendar.getInstance()
    val startOfDay = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfDay = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

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

    // Additional settings for consistent Shared Element Transition colors/animations
    val disableAnimations by settingsDataStore.disableAnimations.collectAsState(initial = false)
    val useHabitColorForCard by settingsDataStore.useHabitColorForCard.collectAsState(initial = true)
    val habitColorTargets by settingsDataStore.habitColorTargets.collectAsState(initial = setOf("Habit Cards", "Statistic Screen"))
    val theme by settingsDataStore.theme.collectAsState(initial = "system")

    val useHabitColor = useHabitColorForCard && "Habit Cards" in habitColorTargets


    val areSettingsLoaded = borderContrast != null &&
            showMonthLabels != null &&
            showYearDivider != null &&
            showYearLabels != null &&
            heatmapVisibleDays != null &&
            dayOfWeekLabelsOnRight != null

    val greeting = remember {
        when (now.get(Calendar.HOUR_OF_DAY)) {
            in 1 .. 5 -> "It's a beautiful night!"
            in 6..14 -> "Good morning"
            in 15..19 -> "Good afternoon"
            in 20..24 -> "Good evening"
            else -> "Welcome back"
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
    var notificationDays by remember { mutableStateOf<Set<String>>(emptySet()) }

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

    LaunchedEffect(habitToEdit, showHabitSheet) {
        if (showHabitSheet) {
            if (habitToEdit != null) {
                habitName = habitToEdit!!.name
                habitDescription = habitToEdit!!.description
                habitColor = Color(habitToEdit!!.color)
                habitIconKey = habitToEdit!!.icon
                completionsPerInterval = habitToEdit!!.completionsPerInterval.toString()
                intervalUnit = habitToEdit!!.intervalUnit
                notificationsEnabled = habitToEdit!!.notificationsEnabled
                notificationTime = habitToEdit!!.notificationTime ?: "09:00"
                notificationDays = habitToEdit!!.notificationDays?.split(',')?.toSet() ?: emptySet()
                customColor = null
            } else {
                habitName = ""
                habitDescription = ""
                habitColor = habitColors.first()
                habitIconKey = defaultHabitIconKey
                completionsPerInterval = "1"
                intervalUnit = "day"
                notificationsEnabled = false
                notificationTime = "09:00"
                notificationDays = emptySet()
                customColor = null
            }
        }
    }

    val isAnySheetOpen = showHabitSheet || showSettingsScreen || habitToView != null || showArchiveSheet || showReorderSheet || showStatisticScreen

    BackHandler(enabled = isAnySheetOpen || isFabMenuExpanded) {
        if (isFabMenuExpanded) { isFabMenuExpanded = false; return@BackHandler }
        if (showStatisticScreen) { showStatisticScreen = false; initialHabitIdForStats = null; return@BackHandler }
        if (showHabitSheet) { showHabitSheet = false; return@BackHandler }
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
            val mainContentModifier = if ((isAnySheetOpen || isFabMenuExpanded) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(16.dp)
            } else {
                Modifier
            }

            val timePickerBlurModifier = if (showTimePicker && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(10.dp)
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
                                        val lazyListState = rememberLazyListState()

                                        LazyColumn(
                                            state = lazyListState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            userScrollEnabled = habitToView == null && habitToEdit == null
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
                                                HabitItemCard(
                                                    modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                        rememberSharedContentState(key = "card-${habitWithCompletions.habit.id}"),
                                                        visible = habitToView?.habit?.id != habitWithCompletions.habit.id
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
                                                    showScrollBlur = showScrollBlur!! && "Heatmap" in scrollBlurTargets,
                                                    borderContrast = borderContrast!!,
                                                    heatmapScrollEnabled = heatmapScrolling,
                                                    useHabitColor = useHabitColor,
                                                    disableAnimations = disableAnimations,
                                                    onComplete = {
                                                        if (vibrationsEnabled) {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.TextHandleMove
                                                            )
                                                        }
                                                        viewModel.toggleCompletion(
                                                            habitWithCompletions.habit,
                                                            Calendar.getInstance(),
                                                            isCompleted
                                                        )
                                                    },
                                                    onClick = {
                                                        habitToView = habitWithCompletions
                                                    }
                                                )
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
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 250)) { -it },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 250)) { -it }
                ) {
                    ArchiveScreen(
                        uiState = archivedHabitsUiState,
                        habitDao = habitDao,
                        onBack = { showArchiveSheet = false },
                        settingsDataStore = settingsDataStore
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
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 250)) { -it },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 250)) { -it }
                ) {
                    ReorderScreen(habitViewModel = viewModel, onBack = { showReorderSheet = false }, settingsDataStore = settingsDataStore)
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
                    enter = fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 250))
                ) {
                    habitToView?.let { habitWithCompletions ->
                        val habitState by remember(habitsUiState) {
                            derivedStateOf {
                                (habitsUiState as? HabitsUiState.Success)?.habits
                                    ?.find { it.habit.id == habitWithCompletions.habit.id }
                            }
                        }

                        habitState?.let { it ->
                            HabitDetailScreen(
                                habitWithCompletions = it,
                                viewModel = viewModel,
                                isArchivedView = false,
                                animatedVisibilityScope = this@AnimatedVisibility,
                                onDismiss = { habitToView = null },
                                onEditHabit = {
                                    habitToEdit = it
                                    showHabitSheet = true
                                },
                                onShowStatistics = { habit ->
                                    initialHabitIdForStats = habit.id
                                    showStatisticScreen = true
                                },
                                borderContrast = borderContrast!!,
                                showScrollBlur = showScrollBlur!! && "Heatmap" in scrollBlurTargets,
                                showYearLabels = showYearLabels!!,
                                showYearDivider = showYearDivider!!,
                                vibrationsEnabled = vibrationsEnabled,
                                showMonthLabels = showMonthLabels!!,
                                dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight!!,
                                heatmapVisibleDays = heatmapVisibleDays!!,
                                disableAnimations = disableAnimations,
                                useHabitColorForCard = useHabitColorForCard,
                                theme = theme
                            )
                        }
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
                    enter = slideInVertically(animationSpec = tween(durationMillis = 250)) { -it },
                    exit = slideOutVertically(animationSpec = tween(durationMillis = 250)) { -it }
                ) {
                    StatisticScreen(
                        viewModel = viewModel, 
                        onBack = { 
                            showStatisticScreen = false 
                            initialHabitIdForStats = null
                        },
                        initialHabitId = initialHabitIdForStats,
                        settingsDataStore = settingsDataStore
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
                    enter = slideInHorizontally(animationSpec = tween(durationMillis = 250)) { it },
                    exit = slideOutHorizontally(animationSpec = tween(durationMillis = 250)) { it }
                ) {
                    SettingsScreen(
                        onDismiss = { showSettingsScreen = false },
                        db = HabitDatabase.getDatabase(context), // Fallback if db is needed here
                        settingsDataStore = settingsDataStore
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
                            .clickable { showHabitSheet = false }
                    )
                }

                val sheetOffsetY = remember { Animatable(0f) }
                LaunchedEffect(showHabitSheet) {
                    if (showHabitSheet) sheetOffsetY.snapTo(0f)
                }

                AnimatedVisibility(
                    visible = showHabitSheet,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(animationSpec = tween(durationMillis = 250)) { it },
                    exit = slideOutVertically(animationSpec = tween(durationMillis = 250)) { it }
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
                                    if (sheetOffsetY.value > dismissThresholdPx) showHabitSheet = false
                                    else sheetOffsetY.animateTo(0f, spring())
                                    return available
                                }
                                return super.onPreFling(available)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .offset { IntOffset(0, sheetOffsetY.value.roundToInt()) }
                            .nestedScroll(nestedScrollConnection),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
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
                                hasNotificationPermission = notificationPermissionHandler.hasPermission
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showHabitSheet,
                    enter = fadeIn(),
                    exit = fadeOut(),
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
                                    // Reset the state for the next new habit
                                    habitName = ""
                                    habitDescription = ""
                                    habitColor = habitColors.first()
                                    habitIconKey = defaultHabitIconKey
                                    completionsPerInterval = "1"
                                    intervalUnit = "day"
                                    completionsError = null
                                }
                                showHabitSheet = false
                                habitToEdit = null
                                customColor = null
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = habitsUiState is HabitsUiState.Loading,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
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