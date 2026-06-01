/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val currentIndex = if (actualCount > 0) pagerState.currentPage % actualCount else 0
    val currentHabit = habits.getOrNull(currentIndex)

    fun enterEditMode() {
        val currentLayout = currentHabit?.habit?.statsLayout?.split(",")?.filter { it.isNotEmpty() }
            ?: defaultLayout
        savedLayout = currentLayout
        localActiveModules = currentLayout
        isEditMode = true
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
        localActiveModules = localActiveModules.filter { it != moduleId }
    }

    fun onAddModule(moduleId: String) {
        if (!localActiveModules.contains(moduleId)) {
            localActiveModules = localActiveModules + moduleId
        }
    }

    fun onReorderModules(newList: List<String>) {
        localActiveModules = newList
    }

    var isShelfExpanded by remember { mutableStateOf(false) }
    val animatedShelfHeight by animateDpAsState(
        targetValue = if (isShelfExpanded) 580.dp else 60.dp,
        animationSpec = tween(durationMillis = 300), // Smooth non-bouncy drawer height transition
        label = "shelfHeight"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val currentHabitName = currentHabit?.habit?.name ?: ""
                    Text(
                        text = "Statistics for $currentHabitName",
                        fontWeight = FontWeight.SemiBold,
                        color = currentHabitColor
                    )
                },
                actions = {
                    if (isEditMode) {
                        var showDropdown by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable {
                                        saveEdits(applyToAll = false)
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Save",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.6f)
                                    .width(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = 0.2f
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { showDropdown = true }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Save Options",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showDropdown,
                                    onDismissRequest = { showDropdown = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Save") },
                                        onClick = {
                                            showDropdown = false
                                            saveEdits(applyToAll = false)
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Save & Apply to All") },
                                        onClick = {
                                            showDropdown = false
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

                        Spacer(modifier = Modifier.width(8.dp))

                        AppBackButton(
                            onBack = {
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                }
                                cancelEdits()
                            },
                            borderContrast = borderContrast,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = currentHabitColor,
                            backgroundColor = backButtonBackgroundColor
                        )
                    } else {
                        IconButton(
                            onClick = {
                                enterEditMode()
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Layout",
                                tint = currentHabitColor
                            )
                        }
                        AppBackButton(
                            onBack = {
                                if (vibrationsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                }
                                onBack()
                            },
                            borderContrast = borderContrast,
                            icon = Icons.Default.Close,
                            tint = currentHabitColor,
                            backgroundColor = backButtonBackgroundColor
                        )
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
                    .padding(paddingValues),
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
                                if (layout.isNullOrEmpty()) {
                                    defaultLayout
                                } else {
                                    layout.split(",").filter { it.isNotEmpty() }
                                }
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
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
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        var sheetDragAccumulator by remember {
                                            mutableFloatStateOf(
                                                0f
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(Unit) {
                                                    detectDragGestures(
                                                        onDragEnd = {
                                                            if (sheetDragAccumulator < -50f) {
                                                                isShelfExpanded = true
                                                            } else if (sheetDragAccumulator > 50f) {
                                                                isShelfExpanded = false
                                                            }
                                                            sheetDragAccumulator = 0f
                                                        },
                                                        onDragCancel = {
                                                            sheetDragAccumulator = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            sheetDragAccumulator += dragAmount.y
                                                        }
                                                    )
                                                }
                                                .clickable { isShelfExpanded = !isShelfExpanded }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(36.dp)
                                                        .height(4.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.4f
                                                            ),
                                                            shape = CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = if (isShelfExpanded) "Potential Modules (Drag up/Tap to Add)" else "Add Modules (Tap/Swipe Up to Expand)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (isShelfExpanded && currentHabit != null) {
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
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .padding(
                                                            horizontal = 12.dp,
                                                            vertical = 8.dp
                                                        ),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    itemsIndexed(
                                                        items = inactiveModules,
                                                        key = { _, item -> item },
                                                        span = { _, item ->
                                                            GridItemSpan(if (item == "monthly_chart") 2 else 1)
                                                        }
                                                    ) { _, moduleId ->
                                                        InactiveModuleCard(
                                                            moduleId = moduleId,
                                                            habit = currentHabit,
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
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            val animatedDragOffsetY by animateFloatAsState(
                targetValue = dragOffsetY,
                label = "dragOffsetY"
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        translationY = dragOffsetY
                        val dragScale = if (dragOffsetY < 0f) (1f + dragOffsetY / 1000f).coerceIn(
                            0.8f,
                            1f
                        ) else 1f
                        scaleX = dragScale
                        scaleY = dragScale
                    }
                    .pointerInput(moduleId) {
                        detectDragGestures(
                            onDragStart = {
                                dragOffsetY = 0f
                            },
                            onDragEnd = {
                                if (dragOffsetY < -150f) {
                                    onAdd()
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtMost(0f)
                            }
                        )
                    }
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
    }
}