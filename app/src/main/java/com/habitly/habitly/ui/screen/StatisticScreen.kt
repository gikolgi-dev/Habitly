/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.Database.HabitViewModel
import com.habitly.habitly.data.Database.HabitsUiState
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.ui.AppBackButton
import com.habitly.habitly.ui.components.*

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

private fun getModuleIcon(id: String): ImageVector = when (id) {
    "longest_streak" -> Icons.Default.Star
    "current_streak" -> Icons.Default.FlashOn
    "completion_ratio" -> Icons.Default.DonutLarge
    "avg_completion_time" -> Icons.Default.AccessTime
    "days_since_creation" -> Icons.Default.CalendarToday
    "total_completions" -> Icons.Default.CheckCircle
    "best_day_of_week" -> Icons.Default.WbSunny
    "rate_last_30_days" -> Icons.Default.Speed
    "monthly_chart" -> Icons.Default.ShowChart
    else -> Icons.Default.Star
}

private fun getModuleDisplayName(id: String): String = when (id) {
    "longest_streak" -> "Longest Streak"
    "current_streak" -> "Current Streak"
    "completion_ratio" -> "Completion Ratio"
    "avg_completion_time" -> "Average Completion Time"
    "days_since_creation" -> "Days Since Creation"
    "total_completions" -> "Total Completions"
    "best_day_of_week" -> "Best Day of the Week"
    "rate_last_30_days" -> "Rate Last 30 Days"
    "monthly_chart" -> "Monthly Completion Chart"
    else -> id.replace("_", " ").replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    initialHabitId: String? = null,
    borderContrast: Float,
    vibrationsEnabled: Boolean,
    showScrollBlur: Boolean,
    scrollBlurTargets: Set<String>,
    useHabitColor: Boolean
) {
    val context = LocalContext.current
    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val habits = (habitsUiState as? HabitsUiState.Success)?.habits?.filterNot { it.habit.archived }
        ?: emptyList()
    val haptic = LocalHapticFeedback.current

    val actualCount = habits.size
    val pageCount = if (actualCount > 1) Int.MAX_VALUE else actualCount

    val pagerState = rememberPagerState(pageCount = { pageCount })

    var isEditMode by remember { mutableStateOf(false) }
    var localActiveModules by remember { mutableStateOf<List<String>>(emptyList()) }
    var savedLayout by remember { mutableStateOf<List<String>>(emptyList()) }

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
        lerp(currentHabitColor, MaterialTheme.colorScheme.surfaceVariant, 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val topBarTint = if (useHabitColor) currentHabitColor else MaterialTheme.colorScheme.onSurface

    val currentIndex = if (actualCount > 0) pagerState.currentPage % actualCount else 0
    val currentHabit = habits.getOrNull(currentIndex)

    fun enterEditMode() {
        val statsLayout = currentHabit?.habit?.statsLayout
        val currentLayout = if (statsLayout == null) {
            defaultLayout
        } else {
            statsLayout.split(",").filter { it.isNotEmpty() }
        }
        val sanitized = sanitizeStaticModuleList(currentLayout)
        savedLayout = sanitized
        localActiveModules = sanitized
        isEditMode = true
    }

    fun resetToDefault() {
        localActiveModules = sanitizeStaticModuleList(defaultLayout)
        if (vibrationsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun cancelEdits() {
        isEditMode = false
    }

    fun saveEdits(applyToAll: Boolean) {
        currentHabit?.let {
            val layoutStr = localActiveModules.joinToString(",")
            if (applyToAll) {
                viewModel.applyStatsLayoutToAll(layoutStr)
            } else {
                viewModel.updateHabitStatsLayout(it.habit, layoutStr)
            }
        }
        isEditMode = false
    }

    val inactiveModules = remember(localActiveModules) {
        ALL_STAT_MODULES.filter { it !in localActiveModules }.sortedBy { getModuleDisplayName(it) }
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

    var isShelfExpanded by remember { mutableStateOf(false) }
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val animatedShelfHeight by animateDpAsState(
        targetValue = if (isShelfExpanded) 580.dp else (60.dp + navigationBarsPadding),
        animationSpec = tween(durationMillis = 300), // Smooth non-bouncy drawer height transition
        label = "shelfHeight"
    )

    val nestedScrollConnection = remember(isEditMode) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isEditMode) {
                    val dy = available.y
                    if (dy < -15f) {
                        isShelfExpanded = true
                    } else if (dy > 15f) {
                        isShelfExpanded = false
                    }
                }
                return Offset.Zero
            }
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
                    )
                    .nestedScroll(nestedScrollConnection),
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
                            val habitColor = Color(habit.habit.color)

                            val pageLayout = remember(habit.habit.statsLayout) {
                                val layout = habit.habit.statsLayout
                                val rawList = if (layout == null) {
                                    defaultLayout
                                } else {
                                    layout.split(",").filter { it.isNotEmpty() }
                                }
                                sanitizeStaticModuleList(rawList)
                            }

                            val activeList =
                                if (isEditMode && habit.habit.id == currentHabit?.habit?.id) {
                                    localActiveModules
                                } else {
                                    pageLayout
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
                                onReorderModules = ::onReorderModules
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

                    if (isEditMode && isShelfExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { isShelfExpanded = false }
                        )
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
                                .height(animatedShelfHeight),
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 8.dp,
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
                                var sheetDragAccumulator by remember { mutableFloatStateOf(0f) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isShelfExpanded = !isShelfExpanded }
                                        .draggable(
                                            orientation = Orientation.Vertical,
                                            state = rememberDraggableState { delta ->
                                                sheetDragAccumulator += delta
                                            },
                                            onDragStopped = { velocity ->
                                                if (sheetDragAccumulator < -30f || velocity < -300f) {
                                                    isShelfExpanded = true
                                                } else if (sheetDragAccumulator > 30f || velocity > 300f) {
                                                    if (isShelfExpanded) {
                                                        isShelfExpanded = false
                                                    } else {
                                                        cancelEdits()
                                                    }
                                                }
                                                sheetDragAccumulator = 0f
                                            }
                                        )
                                         .padding(top = 14.dp, bottom = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(56.dp)
                                            .height(6.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.4f
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }

                                AnimatedVisibility(
                                     visible = isShelfExpanded && currentHabit != null,
                                     enter = fadeIn(animationSpec = tween(300, delayMillis = 100)),
                                     exit = fadeOut(animationSpec = tween(200)),
                                     modifier = Modifier.weight(1f)
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
                                                         habit = currentHabit!!,
                                                         accentColor = currentHabitColor,
                                                         vibrationsEnabled = vibrationsEnabled,
                                                         borderContrast = borderContrast,
                                                         useHabitColorForCard = useHabitColor,
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
                            .offset(y = -animatedShelfHeight - 16.dp, x = -16.dp)
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
    onAdd: () -> Unit
) {
            val stats = remember(habit) { com.habitly.habitly.data.calculateStatistics(habit) }
            val monthlyStats =
                remember(habit) { com.habitly.habitly.data.calculateMonthlyStats(habit) }
            val onSurface = MaterialTheme.colorScheme.onSurface
            val displayAccentColor =
                remember(accentColor, onSurface) { lerp(accentColor, onSurface, 0.3f) }

            val haptic = LocalHapticFeedback.current

            Box(
                modifier = Modifier
                    .clickable {
                        onAdd()
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
            ) {
                when (moduleId) {
                    "longest_streak" -> StatCard(
                        label = "Longest Streak",
                        value = "${stats.longestStreak} days",
                        secondaryValue = if (stats.daysSinceLongestStreak > 0) "${stats.daysSinceLongestStreak} days ago" else "Current",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "current_streak" -> StatCard(
                        label = "Current Streak",
                        value = "${stats.currentStreak} days",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "completion_ratio" -> StatCard(
                        label = "Completed Ratio",
                        value = "${stats.completionRatio}%",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "avg_completion_time" -> StatCard(
                        label = "Avg. Completion Time",
                        value = stats.averageCompletionTime,
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "days_since_creation" -> StatCard(
                        label = "Days since creation",
                        value = "${stats.timeSinceCreation}",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "total_completions" -> StatCard(
                        label = "Total Completions",
                        value = "${stats.totalCompletions}",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "best_day_of_week" -> StatCard(
                        label = "Best Day of the Week",
                        value = stats.bestDayOfWeek,
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "rate_last_30_days" -> StatCard(
                        label = "Rate Last 30 Days",
                        value = "${stats.rateLast30Days}%",
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )

                    "monthly_chart" -> MonthlyCompletionGraph(
                        stats = monthlyStats,
                        accentColor = accentColor,
                        vibrationsEnabled = vibrationsEnabled,
                        showScrollBlur = false,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor,
                        interactive = false
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(22.dp)
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