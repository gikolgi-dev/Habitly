/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen

import java.util.Calendar
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import com.habitly.habitly.data.Database.HabitViewModel
import com.habitly.habitly.data.Database.HabitsUiState
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.ui.AppBackButton
import com.habitly.habitly.ui.colors.isBright
import com.habitly.habitly.ui.colors.toThemeHabitColor
import com.habitly.habitly.ui.components.*
import com.habitly.habitly.data.settings.LocalAppSettings
import kotlinx.coroutines.launch

private val ALL_STAT_MODULES = listOf(
    "longest_streak",
    "current_streak",
    "completion_ratio",
    "avg_completion_time",
    "days_since_creation",
    "total_completions",
    "best_day_of_week",
    "rate_last_30_days",
    "monthly_chart"
)

private val defaultLayout = listOf(
    "longest_streak",
    "completion_ratio",
    "avg_completion_time",
    "days_since_creation",
    "monthly_chart"
)

private fun isStreakModule(id: String): Boolean = when (getBaseModuleId(id)) {
    "longest_streak", "current_streak" -> true
    else -> false
}

private fun getBaseModuleId(id: String): String = if (id.endsWith("_full")) id.removeSuffix("_full") else id

private fun getModuleDisplayName(id: String): String = when (getBaseModuleId(id)) {
    "longest_streak" -> "Longest Streak"
    "current_streak" -> "Current Streak"
    "completion_ratio" -> "Completion Ratio"
    "avg_completion_time" -> "Average Completion Time"
    "days_since_creation" -> "Days Since Creation"
    "total_completions" -> "Total Completions"
    "best_day_of_week" -> "Best Day of the Week"
    "rate_last_30_days" -> "Rate Last 30 Days"
    "monthly_chart" -> "Monthly Completion Chart"
    else -> getBaseModuleId(id).replace("_", " ").replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    initialHabitId: String? = null
) {
    val settings = LocalAppSettings.current
    val borderContrast = settings.borders
    val vibrationsEnabled = settings.vibrations
    val showScrollBlur = settings.showScrollBlur
    val scrollBlurTargets = settings.scrollBlurTargets
    val useHabitColor = settings.useHabitColorForStatistics
    val firstDayOfWeek = settings.firstDayOfWeekCalendar
    val showYearDivider = settings.lineChartYearDivider
    val context = LocalContext.current
    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val habits = (habitsUiState as? HabitsUiState.Success)?.habits?.filterNot { it.habit.archived }
        ?: emptyList()
    val haptic = LocalHapticFeedback.current

    val actualCount = habits.size
    val pageCount = if (actualCount > 1) Int.MAX_VALUE else actualCount

    val pagerState = rememberPagerState(pageCount = { pageCount })

    var isEditMode by remember { mutableStateOf(false) }
    var isShelfExpanded by remember { mutableStateOf(false) }
    var localActiveModules by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastSavedLayouts by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    LaunchedEffect(habits, initialHabitId) {
        if (actualCount > 0) {
            val targetIndex = if (initialHabitId != null) {
                habits.indexOfFirst { it.habit.id == initialHabitId }.takeIf { it >= 0 } ?: 0
            } else {
                0
            }

            if (actualCount > 1) {
                if (pagerState.currentPage < 1000) {
                    val middle = Int.MAX_VALUE / 2
                    val startPage = middle - (middle % actualCount) + targetIndex
                    pagerState.scrollToPage(startPage)
                }
            } else {
                if (pagerState.currentPage != 0) {
                    pagerState.scrollToPage(0)
                }
            }
        }
    }

    // Add haptic feedback when scrolling between habits
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            if (vibrationsEnabled && actualCount > 1) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val currentHabitColor by remember(habits, actualCount, primaryColor) {
        derivedStateOf {
            val currentIndex =
                if (actualCount > 0) ((pagerState.currentPage % actualCount) + actualCount) % actualCount else 0
            habits.getOrNull(currentIndex)?.habit?.color?.let { Color(it) } ?: primaryColor
        }
    }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (useHabitColor && habits.isNotEmpty()) {
            lerp(currentHabitColor, MaterialTheme.colorScheme.surface, 0.95f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backgroundColor"
    )

    val backButtonBackgroundColor = if (useHabitColor && habits.isNotEmpty()) {
        lerp(currentHabitColor, MaterialTheme.colorScheme.surfaceContainerHigh, 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val topBarTint = if (useHabitColor) currentHabitColor else MaterialTheme.colorScheme.onSurface

    val currentIndex = if (actualCount > 0) pagerState.currentPage % actualCount else 0
    val currentHabit = habits.getOrNull(currentIndex)

    fun enterEditMode() {
        val statsLayout = currentHabit?.habit?.statsLayout
        val isStreakDisabled = currentHabit?.habit?.streakCountingDisabled == true
        val baseDefaultLayout = if (isStreakDisabled) defaultLayout.filter { !isStreakModule(it) } else defaultLayout
        val currentLayout = if (statsLayout == null) {
            baseDefaultLayout
        } else {
            val raw = statsLayout.split(",").filter { it.isNotEmpty() }
            if (isStreakDisabled) raw.filter { !isStreakModule(it) } else raw
        }
        val sanitized = sanitizeStaticModuleList(currentLayout)
        localActiveModules = sanitized
        isEditMode = true
        isShelfExpanded = false
    }

    fun resetToDefault() {
        val isStreakDisabled = currentHabit?.habit?.streakCountingDisabled == true
        val baseDefault = if (isStreakDisabled) defaultLayout.filter { !isStreakModule(it) } else defaultLayout
        localActiveModules = sanitizeStaticModuleList(baseDefault)
        if (vibrationsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun cancelEdits() {
        isEditMode = false
        isShelfExpanded = false
    }

    fun saveEdits(applyToAll: Boolean) {
        currentHabit?.let {
            val layoutStr = localActiveModules.joinToString(",")
            if (applyToAll) {
                viewModel.applyStatsLayoutToAll(layoutStr)
                lastSavedLayouts = habits.associate { h -> h.habit.id to localActiveModules }
            } else {
                viewModel.updateHabitStatsLayout(it.habit, layoutStr)
                lastSavedLayouts = lastSavedLayouts + (it.habit.id to localActiveModules)
            }
        }
        isEditMode = false
        isShelfExpanded = false
    }

    val inactiveModules = remember(localActiveModules, currentHabit) {
        val activeBases = localActiveModules.map { getBaseModuleId(it) }
        val isStreakDisabled = currentHabit?.habit?.streakCountingDisabled == true
        val available = if (isStreakDisabled) ALL_STAT_MODULES.filter { !isStreakModule(it) } else ALL_STAT_MODULES
        available.filter { it !in activeBases }.sortedBy { getModuleDisplayName(it) }
    }

    fun onRemoveModule(moduleId: String) {
        localActiveModules = sanitizeStaticModuleList(localActiveModules.filter { it != moduleId })
    }

    fun onAddModule(moduleId: String) {
        if (!localActiveModules.contains(moduleId)) {
            localActiveModules = sanitizeStaticModuleList(localActiveModules + moduleId)
        }
    }

    fun onReorderModules(newList: List<String>) {
        localActiveModules = sanitizeStaticModuleList(newList)
    }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val collapsedShelfHeightDp = 64.dp + navigationBarsPadding
    val expandedShelfHeightDp = 580.dp
    val collapsedShelfHeightPx = with(density) { collapsedShelfHeightDp.toPx() }
    val expandedShelfHeightPx = with(density) { expandedShelfHeightDp.toPx() }

    val shelfExpansionFraction = remember { Animatable(0f) }
    val shelfGridState = rememberLazyGridState()

    fun settleShelf(expand: Boolean, initialVelocityFraction: Float = 0f) {
        isShelfExpanded = expand
        coroutineScope.launch {
            shelfExpansionFraction.animateTo(
                targetValue = if (expand) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialVelocity = initialVelocityFraction
            )
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            isShelfExpanded = false
            shelfExpansionFraction.snapTo(0f)
        }
    }

    val currentShelfHeightPx = collapsedShelfHeightPx +
        (expandedShelfHeightPx - collapsedShelfHeightPx) * shelfExpansionFraction.value
    val currentShelfHeightDp = with(density) { currentShelfHeightPx.toDp() }

    val shelfRangePx = (expandedShelfHeightPx - collapsedShelfHeightPx).coerceAtLeast(1f)
    val currentShelfRangePx by rememberUpdatedState(shelfRangePx)

    val shelfNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < 0f && shelfExpansionFraction.value < 1f) {
                    val deltaFraction = -dy / currentShelfRangePx
                    val newFraction = (shelfExpansionFraction.value + deltaFraction).coerceIn(0f, 1f)
                    val consumedFraction = newFraction - shelfExpansionFraction.value
                    if (consumedFraction != 0f) {
                        coroutineScope.launch { shelfExpansionFraction.snapTo(newFraction) }
                        isShelfExpanded = newFraction > 0.5f
                        return Offset(0f, -consumedFraction * currentShelfRangePx)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val dy = available.y
                if (dy > 0f && source == NestedScrollSource.UserInput && shelfExpansionFraction.value > 0f) {
                    val deltaFraction = -dy / currentShelfRangePx
                    val newFraction = (shelfExpansionFraction.value + deltaFraction).coerceIn(0f, 1f)
                    val consumedFraction = newFraction - shelfExpansionFraction.value
                    if (consumedFraction != 0f) {
                        coroutineScope.launch { shelfExpansionFraction.snapTo(newFraction) }
                        isShelfExpanded = newFraction > 0.5f
                        return Offset(0f, -consumedFraction * currentShelfRangePx)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val fraction = shelfExpansionFraction.value
                if (fraction > 0f && fraction < 1f) {
                    val vy = available.y
                    val velFraction = -vy / currentShelfRangePx
                    val shouldExpand = when {
                        vy < -350f -> true
                        vy > 350f -> false
                        else -> fraction >= 0.5f
                    }
                    settleShelf(shouldExpand, velFraction)
                    return available
                }
                if (fraction >= 1f && available.y > 600f && !shelfGridState.canScrollBackward) {
                    val velFraction = -available.y / currentShelfRangePx
                    settleShelf(false, velFraction)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = isEditMode) {
        if (vibrationsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
        }
        if (isShelfExpanded || shelfExpansionFraction.value > 0.05f) {
            settleShelf(false)
        } else {
            cancelEdits()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val currentHabitName = currentHabit?.habit?.name ?: ""
                    Text(
                        text = currentHabitName,
                        fontWeight = FontWeight.SemiBold,
                        color = topBarTint
                    )
                },
                navigationIcon = {
                    Crossfade(targetState = isEditMode, label = "navigationIconTransition") { editing ->
                        if (editing) {
                            AppBackButton(
                                onBack = {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                    }
                                    cancelEdits()
                                },
                                borderContrast = borderContrast,
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                tint = topBarTint,
                                backgroundColor = backButtonBackgroundColor
                            )
                        } else {
                            AppBackButton(
                                onBack = {
                                    enterEditMode()
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                borderContrast = borderContrast,
                                icon = Icons.Default.Edit,
                                tint = topBarTint,
                                backgroundColor = backButtonBackgroundColor
                            )
                        }
                    }
                },
                actions = {
                    Crossfade(targetState = isEditMode, label = "actionsTransition") { editing ->
                        if (editing) {
                            AppBackButton(
                                onBack = {
                                    resetToDefault()
                                },
                                borderContrast = borderContrast,
                                icon = Icons.Default.Refresh,
                                tint = topBarTint,
                                backgroundColor = backButtonBackgroundColor
                            )
                        } else {
                            AppBackButton(
                                onBack = {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                    }
                                    onBack()
                                },
                                borderContrast = borderContrast,
                                icon = Icons.Default.Close,
                                tint = topBarTint,
                                backgroundColor = backButtonBackgroundColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = animatedBackgroundColor
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = 0.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (habits.isEmpty()) {
                    Text("No habits available")
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (isEditMode) 60.dp else 0.dp),
                        pageSpacing = 24.dp,
                        beyondViewportPageCount = 1,
                        userScrollEnabled = !isEditMode,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            snapAnimationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    ) { page ->
                        val index = page % actualCount
                        val habit = habits.getOrNull(index)
                        if (habit != null) {
                            val isDark = !MaterialTheme.colorScheme.surface.isBright()
                            val habitColor = Color(habit.habit.color).toThemeHabitColor(isDark)

                            val pageLayout = remember(habit.habit.statsLayout, habit.habit.streakCountingDisabled) {
                                val layout = habit.habit.statsLayout
                                val isStreakDisabled = habit.habit.streakCountingDisabled
                                val baseDefault = if (isStreakDisabled) defaultLayout.filter { !isStreakModule(it) } else defaultLayout
                                val rawList = if (layout == null) {
                                    baseDefault
                                } else {
                                    val raw = layout.split(",").filter { it.isNotEmpty() }
                                    if (isStreakDisabled) raw.filter { !isStreakModule(it) } else raw
                                }
                                sanitizeStaticModuleList(rawList)
                            }

                            val activeList =
                                if (isEditMode && habit.habit.id == currentHabit?.habit?.id) {
                                    if (habit.habit.streakCountingDisabled) localActiveModules.filter { !isStreakModule(it) } else localActiveModules
                                } else {
                                    val saved = lastSavedLayouts[habit.habit.id]
                                    val effectiveSaved = if (saved != null && habit.habit.streakCountingDisabled) {
                                        saved.filter { !isStreakModule(it) }
                                    } else saved
                                    if (effectiveSaved != null && pageLayout != effectiveSaved) {
                                        effectiveSaved
                                    } else {
                                        pageLayout
                                    }
                                }

                            HabitStatisticsContent(
                                habit = habit,
                                accentColor = habitColor,
                                vibrationsEnabled = vibrationsEnabled,
                                showScrollBlur = showScrollBlur && "Line Chart" in scrollBlurTargets,
                                borderContrast = borderContrast,
                                useHabitColorForCard = useHabitColor,
                                isEditMode = isEditMode && habit.habit.id == currentHabit?.habit?.id,
                                activeModules = activeList,
                                onRemoveModule = ::onRemoveModule,
                                onReorderModules = ::onReorderModules,
                                firstDayOfWeek = firstDayOfWeek,
                                showYearDivider = showYearDivider
                            )
                        }
                    }

                    if (actualCount > 1 && !isEditMode) {
                        PageIndicator(
                            habits = habits,
                            currentPage = pagerState.currentPage % actualCount,
                            borderContrast = borderContrast,
                            useHabitColorForCard = useHabitColor,
                            currentHabitColor = currentHabitColor,
                             modifier = Modifier
                                 .align(Alignment.BottomCenter)
                                 .navigationBarsPadding()
                                 .padding(bottom = 16.dp)
                        )
                    }

                    if (isEditMode && shelfExpansionFraction.value > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f * shelfExpansionFraction.value))
                                .then(
                                    if (shelfExpansionFraction.value > 0.15f) {
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            settleShelf(false)
                                        }
                                    } else Modifier
                                )
                        )
                    }

                    val shelfDraggableState = rememberDraggableState { delta ->
                        val deltaFraction = -delta / currentShelfRangePx
                        val target = (shelfExpansionFraction.value + deltaFraction).coerceIn(0f, 1f)
                        coroutineScope.launch {
                            shelfExpansionFraction.snapTo(target)
                        }
                        isShelfExpanded = target > 0.5f
                    }

                    val onShelfDragStopped: suspend kotlinx.coroutines.CoroutineScope.(Float) -> Unit = { velocity ->
                        val velFraction = -velocity / currentShelfRangePx
                        val currentFraction = shelfExpansionFraction.value
                        val shouldExpand = when {
                            velocity < -350f -> true
                            velocity > 350f -> false
                            else -> currentFraction >= 0.5f
                        }
                        if (vibrationsEnabled && shouldExpand != (currentFraction > 0.5f)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        settleShelf(shouldExpand, velFraction)
                    }

                    AnimatedVisibility(
                        visible = isEditMode,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(currentShelfHeightDp)
                                .nestedScroll(shelfNestedScrollConnection)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = shelfDraggableState,
                                    onDragStopped = onShelfDragStopped
                                ),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp,
                            border = if (borderContrast > 0f) {
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast)
                                )
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (vibrationsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            settleShelf(shelfExpansionFraction.value < 0.5f)
                                        }
                                        .padding(top = 10.dp, bottom = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(56.dp)
                                                .height(5.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.4f
                                                    ),
                                                    shape = CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (inactiveModules.isNotEmpty()) {
                                                "Add Modules (${inactiveModules.size})"
                                            } else {
                                                "All Modules Added"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (shelfExpansionFraction.value > 0.02f && currentHabit != null) {
                                    val contentAlpha = ((shelfExpansionFraction.value - 0.05f) / 0.35f).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .graphicsLayer { alpha = contentAlpha }
                                    ) {
                                        if (inactiveModules.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "All modules are active",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            LazyVerticalGrid(
                                                state = shelfGridState,
                                                columns = GridCells.Fixed(2),
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(
                                                    start = 12.dp,
                                                    end = 12.dp,
                                                    top = 8.dp,
                                                    bottom = 48.dp
                                                ),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                itemsIndexed(
                                                    items = inactiveModules,
                                                    key = { _, item -> item },
                                                    span = { _, item ->
                                                        GridItemSpan(if (item == "monthly_chart") 2 else 1)
                                                    }
                                                ) { _, moduleId ->
                                                    Box(
                                                        modifier = Modifier.animateItem(
                                                            placementSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMediumLow
                                                            ),
                                                            fadeInSpec = tween(200),
                                                            fadeOutSpec = tween(200)
                                                        )
                                                    ) {
                                                        InactiveModuleCard(
                                                            moduleId = moduleId,
                                                            habit = currentHabit,
                                                            accentColor = currentHabitColor,
                                                            vibrationsEnabled = vibrationsEnabled,
                                                            borderContrast = borderContrast,
                                                            useHabitColorForCard = useHabitColor,
                                                            firstDayOfWeek = firstDayOfWeek,
                                                            showYearDivider = showYearDivider,
                                                            onAdd = { onAddModule(moduleId) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var showSaveDropdown by remember { mutableStateOf(false) }

                    AnimatedVisibility(
                        visible = isEditMode,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset {
                                IntOffset(
                                    x = (-16.dp).roundToPx(),
                                    y = (-(currentShelfHeightDp + 16.dp)).roundToPx()
                                )
                            }
                    ) {
                        SplitButtonLayout(
                            modifier = Modifier.shadow(elevation = 6.dp, shape = CircleShape),
                            leadingButton = {
                                SplitButtonDefaults.LeadingButton(
                                    onClick = {
                                        saveEdits(applyToAll = false)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                        contentDescription = "Save Layout",
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Save")
                                }
                            },
                            trailingButton = {
                                val description = "Toggle Save Options"
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above
                                    ),
                                    tooltip = {
                                        PlainTooltip(
                                            modifier = Modifier.semantics {
                                                liveRegion = LiveRegionMode.Assertive
                                                paneTitle = description
                                            }
                                        ) {
                                            Text(description)
                                        }
                                    },
                                    state = rememberTooltipState(),
                                ) {
                                    SplitButtonDefaults.TrailingButton(
                                        checked = showSaveDropdown,
                                        onCheckedChange = { showSaveDropdown = it },
                                        modifier = Modifier.semantics {
                                            stateDescription = if (showSaveDropdown) "Expanded" else "Collapsed"
                                            contentDescription = description
                                        },
                                    ) {
                                        val rotation: Float by animateFloatAsState(
                                            targetValue = if (showSaveDropdown) 180f else 0f,
                                            label = "Trailing Icon Rotation",
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            modifier = Modifier
                                                .size(SplitButtonDefaults.TrailingIconSize)
                                                .graphicsLayer {
                                                    this.rotationZ = rotation
                                                },
                                            contentDescription = "Save Options Toggle",
                                        )
                                        androidx.compose.material3.DropdownMenu(
                                            expanded = showSaveDropdown,
                                            onDismissRequest = { showSaveDropdown = false },
                                            offset = DpOffset(0.dp, (-12).dp),
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .then(
                                                    if (borderContrast > 0f) {
                                                        Modifier.border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                    } else Modifier
                                                )
                                        ) {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "Save & Apply to All",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.CopyAll,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                onClick = {
                                                    showSaveDropdown = false
                                                    saveEdits(applyToAll = true)
                                                    Toast.makeText(
                                                        context,
                                                        "Applied layout to all habits",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InactiveModuleCard(
    moduleId: String,
    habit: HabitWithCompletions,
    accentColor: Color,
    vibrationsEnabled: Boolean,
    borderContrast: Float,
    useHabitColorForCard: Boolean,
    firstDayOfWeek: Int = Calendar.MONDAY,
    showYearDivider: Boolean = false,
    onAdd: () -> Unit
) {
            val statsState = androidx.compose.runtime.produceState<com.habitly.habitly.data.HabitStatistics?>(initialValue = null, key1 = habit, key2 = firstDayOfWeek) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { com.habitly.habitly.data.calculateStatistics(habit, firstDayOfWeek) }
            }
            val monthlyStatsState = androidx.compose.runtime.produceState<List<com.habitly.habitly.data.MonthlyCompletion>?>(initialValue = null, key1 = habit) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { com.habitly.habitly.data.calculateMonthlyStats(habit) }
            }
            val stats = statsState.value
            val monthlyStats = monthlyStatsState.value
            val onSurface = MaterialTheme.colorScheme.onSurface
            val displayAccentColor =
                remember(accentColor, onSurface) { lerp(accentColor, onSurface, 0.3f) }

            val haptic = LocalHapticFeedback.current

            val baseCardColor = lerp(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface, 0.06f)

            if (stats == null || monthlyStats == null) {
                val placeholderColor = if (useHabitColorForCard) {
                    lerp(accentColor, baseCardColor, 0.75f)
                } else {
                    baseCardColor
                }
                val placeholderBorderColor = if (useHabitColorForCard) {
                    lerp(accentColor, lerp(accentColor, baseCardColor, 0.75f), 1f - borderContrast)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast)
                }
                val height = if (moduleId == "monthly_chart") 236.dp else 96.dp
                Card(
                    modifier = Modifier.fillMaxWidth().height(height),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = placeholderColor),
                    border = BorderStroke(1.dp, placeholderBorderColor)
                ) {}
                return
            }

            val onCardClick = {
                if (vibrationsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onAdd()
            }

            Box {
                when (moduleId) {
                    "longest_streak" -> StatCard(
                        label = "Longest Streak",
                        value = "${stats.longestStreak} days",
                        secondaryValue = if (stats.daysSinceLongestStreak > 0) "${stats.daysSinceLongestStreak} days ago" else "Current",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "current_streak" -> StatCard(
                        label = "Current Streak",
                        value = "${stats.currentStreak} days",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "completion_ratio" -> StatCard(
                        label = "Completed Ratio",
                        value = "${stats.completionRatio}%",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "avg_completion_time" -> StatCard(
                        label = "Avg. Completion Time",
                        value = stats.averageCompletionTime,
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "days_since_creation" -> StatCard(
                        label = "Days since creation",
                        value = "${stats.timeSinceCreation}",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "total_completions" -> StatCard(
                        label = "Total Completions",
                        value = "${stats.totalCompletions}",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "best_day_of_week" -> StatCard(
                        label = "Best Day of the Week",
                        value = stats.bestDayOfWeek,
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "rate_last_30_days" -> StatCard(
                        label = "Rate Last 30 Days",
                        value = "${stats.rateLast30Days}%",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )

                    "monthly_chart" -> MonthlyCompletionGraph(
                        stats = monthlyStats,
                        accentColor = accentColor,
                        vibrationsEnabled = vibrationsEnabled,
                        showScrollBlur = false,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        showYearDivider = showYearDivider,
                        interactive = false,
                        baseContainerColor = baseCardColor,
                        onClick = onCardClick
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable {
                            if (vibrationsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onAdd()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
}
