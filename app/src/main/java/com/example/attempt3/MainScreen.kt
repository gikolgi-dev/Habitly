@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ExpressiveMainScreen(viewModel: HabitViewModel, habitDao: HabitDao, db: HabitDatabase, settingsDataStore: SettingsDataStore) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val archivedHabitsUiState by viewModel.archivedHabitsUiState.collectAsState()

    val now = Calendar.getInstance()
    val timezoneOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(now.timeZone.rawOffset.toLong()).toInt()
    val startOfDay = now.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfDay = now.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    val completionsToday by habitDao.getCompletionsForDay(startOfDay, endOfDay)
        .collectAsState(initial = emptyList())
    val completedHabitIds = remember(completionsToday) {
        completionsToday.map { it.habitId }.toSet()
    }

    var optimisticCompletionChanges by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var showHabitSheet by remember { mutableStateOf(false) }
    var habitToView by remember { mutableStateOf<Habit?>(null) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var customColor by remember { mutableStateOf<Color?>(null) }
    var tempColor by remember { mutableStateOf<Color?>(null) }
    var pickerInitialColor by remember { mutableStateOf<Color?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showArchiveSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

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

    fun validate(completionsText: String) {
        if (intervalUnit == "day") {
            completionsError = null
            return
        }
        val completions = completionsText.toIntOrNull()
        if (completions == null) {
            completionsError = "Must be a number"
        } else if (completions <= 0) {
            completionsError = "Must be > 0"
        } else if (completions > 7 && intervalUnit == "week") {
            completionsError = "Must be ≤ 7"
        } else if (completions > 28 && intervalUnit == "month") {
            completionsError = "Must be ≤ 28"
        }else {
            completionsError = null
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
            } else {
                habitName = ""
                habitDescription = ""
                habitColor = habitColors.first()
                habitIconKey = defaultHabitIconKey
                completionsPerInterval = "1"
                intervalUnit = "day"
            }
        }
    }

    if (showHabitSheet || showSettingsScreen || habitToView != null || showArchiveSheet || isFabMenuExpanded || showReorderSheet) {
        BackHandler {
            showHabitSheet = false
            showSettingsScreen = false
            habitToView = null
            showArchiveSheet = false
            isFabMenuExpanded = false
            showReorderSheet = false
        }
    }

    val isColorPickerVisible = showColorPicker
    if (isColorPickerVisible) {
        val controller = rememberColorPickerController()
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
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
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showColorPicker = false }) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        customColor = tempColor
                        showColorPicker = false
                    }) { Text("OK") }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    SharedTransitionLayout {
        val mainContentModifier = if ((habitToView != null || habitToEdit != null || showArchiveSheet || isFabMenuExpanded || showReorderSheet) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(16.dp)
        } else {
            Modifier
        }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = habitsUiState is HabitsUiState.Success,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FabMenu(
                            modifier = Modifier.offset(x = 8.dp, y = 20.dp),
                            expanded = isFabMenuExpanded,
                            onExpandedChange = { isFabMenuExpanded = it },
                            onAddHabit = {
                                habitToEdit = null
                                showHabitSheet = true
                            },
                            onShowArchived = { showArchiveSheet = true },
                            onShowSettings = { showSettingsScreen = true },
                            onShowReorder = { showReorderSheet = true }
                        )
                    }
                },
                floatingActionButtonPosition = FabPosition.End,
                content = { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(modifier = mainContentModifier.fillMaxSize(),color = MaterialTheme.colorScheme.background) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AnimatedVisibility(
                                    visible = habitsUiState is HabitsUiState.Loading,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 500))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        ContainedLoadingIndicator()
                                    }
                                }
                                AnimatedVisibility(
                                    visible = habitsUiState is HabitsUiState.Success,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val habitsWithCompletions = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()

                                    val optimisticallyUpdatedHabitsWithCompletions = remember(habitsWithCompletions, optimisticCompletionChanges) {
                                        if (optimisticCompletionChanges.isEmpty()) {
                                            habitsWithCompletions
                                        } else {
                                            habitsWithCompletions.map { habitWithCompletions ->
                                                val habitId = habitWithCompletions.habit.id
                                                val change = optimisticCompletionChanges[habitId]
                                                if (change == null) {
                                                    habitWithCompletions
                                                } else if (change) { // Completed
                                                    val alreadyCompletedToday = habitWithCompletions.completions.any { it.date in startOfDay..endOfDay }
                                                    if (alreadyCompletedToday) habitWithCompletions else {
                                                        val newCompletion = Completion(
                                                            id = UUID.randomUUID().toString(),
                                                            habitId = habitId,
                                                            date = System.currentTimeMillis(),
                                                            timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                                                            amountOfCompletions = 1
                                                        )
                                                        habitWithCompletions.copy(completions = habitWithCompletions.completions + newCompletion)
                                                    }
                                                } else { // Un-completed
                                                    habitWithCompletions.copy(completions = habitWithCompletions.completions.filterNot { it.date in startOfDay..endOfDay })
                                                }
                                            }
                                        }
                                    }

                                    val scrollState = rememberScrollState()

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState, enabled = habitToView == null && habitToEdit == null)
                                            .padding(top = paddingValues.calculateTopPadding()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        ElevatedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.elevatedCardElevation()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface),
                                                            start = Offset.Zero, end = Offset.Infinite
                                                        )
                                                    )
                                                    .padding(16.dp)
                                            ) {
                                                Column {
                                                    Text("Welcome back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Track your habits, build your future.", style = MaterialTheme.typography.bodyMedium)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .align(Alignment.CenterEnd)
                                                        .clip(CircleShape)
                                                        .background(
                                                            brush = Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent))
                                                        )
                                                )
                                            }
                                        }
                                        optimisticallyUpdatedHabitsWithCompletions.forEach { habitWithCompletions ->
                                            key(habitWithCompletions.habit.id) {
                                                HabitItemCard(
                                                    habit = habitWithCompletions.habit,
                                                    isCompleted = optimisticCompletionChanges[habitWithCompletions.habit.id] ?: completedHabitIds.contains(habitWithCompletions.habit.id),
                                                    completions = habitWithCompletions.completions,
                                                    showCheckbox = true,
                                                    monthLabelsFlow = settingsDataStore.monthLabels,
                                                    onComplete = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        optimisticCompletionChanges = optimisticCompletionChanges + (habitWithCompletions.habit.id to !(optimisticCompletionChanges[habitWithCompletions.habit.id] ?: completedHabitIds.contains(habitWithCompletions.habit.id)))
                                                        scope.launch {
                                                            if (!(optimisticCompletionChanges[habitWithCompletions.habit.id] ?: completedHabitIds.contains(habitWithCompletions.habit.id))) {
                                                                habitDao.insertCompletion(
                                                                    Completion(id = UUID.randomUUID().toString(), habitId = habitWithCompletions.habit.id, date = System.currentTimeMillis(), timezoneOffsetInMinutes = timezoneOffsetInMinutes, amountOfCompletions = 1)
                                                                )
                                                            } else {
                                                                habitDao.deleteCompletionsForHabitOnDay(habitWithCompletions.habit.id, startOfDay, endOfDay)
                                                            }
                                                        }
                                                    },
                                                    onClick = { habitToView = habitWithCompletions.habit }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(80.dp))
                                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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
                ReorderScreen(onBack = { showReorderSheet = false })
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
                enter = slideInVertically(animationSpec = tween(durationMillis = 250)) { it },
                exit = slideOutVertically(animationSpec = tween(durationMillis = 250)) { it }
            ) {
                habitToView?.let { habit ->
                    HabitDetailScreen(
                        habit = habit,
                        habitDao = habitDao,
                        isArchivedView = false,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        onDismiss = { habitToView = null },
                        onEditHabit = {
                            habitToEdit = it
                            showHabitSheet = true
                        },
                        settingsDataStore = settingsDataStore
                    )
                }
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
                SettingsScreen(onDismiss = { showSettingsScreen = false }, db = db, settingsDataStore = settingsDataStore)
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
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            val delta = available.y
                            return if (delta > 0 && sheetOffsetY.value > 0) {
                                scope.launch { sheetOffsetY.snapTo(sheetOffsetY.value + delta) }
                                Offset(0f, delta)
                            } else Offset.Zero
                        }

                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
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
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), shape = CircleShape
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
                                    pickerInitialColor = color
                                    tempColor = color
                                }
                            },
                            onClearCustomColor = { customColor = null },
                            livePreviewColor = if (showColorPicker) { if (pickerInitialColor == null) tempColor else pickerInitialColor } else customColor,
                            scrollState = scrollState
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
                    isEnabled = habitName.trim().isNotBlank() && completionsError == null
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
                                    completionsPerInterval = completionsPerInterval.toIntOrNull() ?: 1,
                                    intervalUnit = intervalUnit
                                )
                                habitDao.updateHabit(updatedHabit)
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
                                    completionsPerInterval = completionsPerInterval.toIntOrNull() ?: 1,
                                    intervalUnit = intervalUnit
                                )
                                habitDao.insertHabit(newHabit)
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
}