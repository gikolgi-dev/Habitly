package com.example.attempt3

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(if (from < to) to - 1 else to, item)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ExpressiveMainScreen(viewModel: HabitViewModel, habitDao: HabitDao, db: HabitDatabase) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 1 })


    // --- Navigation State ---
    val navIndex = remember { mutableIntStateOf(0) }
    val uiState by if (navIndex.intValue == 0) {
        viewModel.habitsUiState.collectAsState()
    } else {
        viewModel.archivedHabitsUiState.collectAsState()
    }

    // --- Completions State ---
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
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }


    val isColorPickerVisible = showColorPicker
    if (isColorPickerVisible) {
        val controller = rememberColorPickerController()
        AlertDialog(
            onDismissRequest = {
                showColorPicker = false
            },
            title = { Text("Choose a color") },
            text = {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    controller = controller,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        tempColor = colorEnvelope.color
                    }
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        customColor = tempColor
                        showColorPicker = false
                    }) {
                        Text("OK")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showHabitSheet) {
        val habits = (uiState as? HabitsUiState.Success)?.habits ?: emptyList()
        HabitSheet(
            habit = habitToEdit,
            habitDao = habitDao,
            onDismiss = {
                showHabitSheet = false
                habitToEdit = null
                customColor = null
            },
            onShowColorPicker = { show, color ->
                showColorPicker = show
                if (show) {
                    pickerInitialColor = color
                    tempColor = color
                }
            },
            onClearCustomColor = { customColor = null },
            customColor = customColor,
            livePreviewColor = if (showColorPicker) {
                if (pickerInitialColor == null) { // new custom color
                    tempColor
                } else { // editing existing custom color
                    pickerInitialColor // sticky, no live preview
                }
            } else {
                customColor
            },
            habits = habits
        )
    }


    SharedTransitionLayout {
        val mainContentModifier = if ((habitToView != null || habitToEdit != null) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(16.dp)
        } else {
            Modifier
        }

        Box(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = habitToView == null && habitToEdit == null
            ) { page ->
                when (page) {
                    0 -> Scaffold(
                        modifier = mainContentModifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Habitly", fontWeight = FontWeight.SemiBold) },
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        floatingActionButton = {
                            AnimatedVisibility(
                                visible = uiState is HabitsUiState.Success,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                FabMenu(
                                    isArchivedScreen = navIndex.intValue == 1,
                                    onAddNewHabit = {
                                        habitToEdit = null
                                        showHabitSheet = true
                                    },
                                    onShowArchived = { navIndex.intValue = 1 },
                                    onShowHome = { navIndex.intValue = 0 },
                                    onShowSettings = { showSettingsScreen = true },
                                    isExpanded = isFabMenuExpanded,
                                    onToggle = { isFabMenuExpanded = it }
                                )
                            }
                        },
                        content = { paddingValues ->
                            Box(modifier = Modifier.padding(paddingValues)) {
                                AnimatedVisibility(
                                    visible = uiState is HabitsUiState.Loading,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Loading habits...")
                                    }
                                }
                                AnimatedVisibility(
                                    visible = uiState is HabitsUiState.Success,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val habitsWithCompletions = (uiState as? HabitsUiState.Success)?.habits ?: emptyList()

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
                                                    if (alreadyCompletedToday) {
                                                        habitWithCompletions
                                                    } else {
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
                                                    habitWithCompletions.copy(
                                                        completions = habitWithCompletions.completions.filterNot {
                                                            it.date in startOfDay..endOfDay
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val scrollState = rememberScrollState()

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(
                                                scrollState,
                                                enabled = habitToView == null && habitToEdit == null
                                            )
                                            .padding(PaddingValues(bottom = 80.dp)),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // expressive hero card with gradient background
                                        ElevatedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 12.dp, vertical = 2.dp
                                                ),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.elevatedCardElevation()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.surface
                                                            ),
                                                            start = Offset.Zero,
                                                            end = Offset.Infinite
                                                        )
                                                    )
                                                    .padding(16.dp)
                                            ) {
                                                Column {
                                                    Text("Welcome back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Track your habits, build your future.", style = MaterialTheme.typography.bodyMedium)
                                                }
                                                // subtle right-side decorative circle
                                                Box(
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .align(Alignment.CenterEnd)

                                                        .clip(CircleShape)
                                                        .background(
                                                            brush = Brush.radialGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = 0.06f),
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                        optimisticallyUpdatedHabitsWithCompletions.forEach { habitWithCompletions ->
                                            key(habitWithCompletions.habit.id) {
                                                HabitItemCard(
                                                    habit = habitWithCompletions.habit,
                                                    isCompleted = optimisticCompletionChanges[habitWithCompletions.habit.id]
                                                        ?: completedHabitIds.contains(
                                                            habitWithCompletions.habit.id
                                                        ),
                                                    completions = habitWithCompletions.completions,
                                                    showCheckbox = navIndex.intValue == 0,
                                                    onComplete = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        optimisticCompletionChanges =
                                                            optimisticCompletionChanges + (habitWithCompletions.habit.id to !(optimisticCompletionChanges[habitWithCompletions.habit.id]
                                                                ?: completedHabitIds.contains(
                                                                    habitWithCompletions.habit.id
                                                                )))
                                                        scope.launch {
                                                            if (!(optimisticCompletionChanges[habitWithCompletions.habit.id]
                                                                    ?: completedHabitIds.contains(
                                                                        habitWithCompletions.habit.id
                                                                    ))
                                                            ) {
                                                                habitDao.insertCompletion(
                                                                    Completion(
                                                                        id = UUID
                                                                            .randomUUID()
                                                                            .toString(),
                                                                        habitId = habitWithCompletions.habit.id,
                                                                        date = System.currentTimeMillis(),
                                                                        timezoneOffsetInMinutes = timezoneOffsetInMinutes,
                                                                        amountOfCompletions = 1
                                                                    )
                                                                )
                                                            } else {
                                                                habitDao.deleteCompletionsForHabitOnDay(
                                                                    habitWithCompletions.habit.id,
                                                                    startOfDay,
                                                                    endOfDay
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    habitToView = habitWithCompletions.habit
                                                }
                                            }
                                        }
                                    }
                                 }
                            }
                        }
                    )
                }
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
                        isArchivedView = navIndex.intValue == 1,
                        animatedVisibilityScope = this@AnimatedVisibility,
                        onDismiss = { habitToView = null },
                        onEditHabit = {
                            habitToEdit = it
                            showHabitSheet = true
                        }
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
                enter = slideInVertically(animationSpec = tween(durationMillis = 250)) { it },
                exit = slideOutVertically(animationSpec = tween(durationMillis = 250)) { it }
            ) {
                SettingsScreen(
                    onDismiss = { showSettingsScreen = false },
                    db = db
                )
            }
        }
    }
}