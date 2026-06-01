/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion.then
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.data.HabitStatistics
import com.habitly.habitly.data.MonthlyCompletion
import com.habitly.habitly.data.calculateMonthlyStats
import com.habitly.habitly.data.calculateStatistics
import com.habitly.habitly.ui.fadingEdge
import kotlinx.coroutines.launch

private sealed interface RenderRowItem {
    data class Single(val moduleId: String, val isHalfWidth: Boolean) : RenderRowItem
    data class Pair(val firstModuleId: String, val secondModuleId: String) : RenderRowItem
}

private fun groupModules(modules: List<String>): List<RenderRowItem> {
    val result = mutableListOf<RenderRowItem>()
    var i = 0
    while (i < modules.size) {
        val current = modules[i]
        val currentIsHalf = current != "monthly_chart"
        
        if (currentIsHalf && i + 1 < modules.size && modules[i + 1] != "monthly_chart") {
            result.add(RenderRowItem.Pair(current, modules[i + 1]))
            i += 2
        } else {
            result.add(RenderRowItem.Single(current, isHalfWidth = false))
            i += 1
        }
    }
    return result
}

@Composable
private fun RenderStatCard(
    moduleId: String,
    stats: HabitStatistics,
    monthlyStats: List<MonthlyCompletion>,
    displayAccentColor: Color,
    accentColor: Color,
    borderContrast: Float,
    useHabitColorForCard: Boolean,
    vibrationsEnabled: Boolean,
    showScrollBlur: Boolean,
    isEditMode: Boolean
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
            showScrollBlur = showScrollBlur,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            interactive = !isEditMode
        )
    }
}

@Composable
fun HabitStatisticsContent(
    habit: HabitWithCompletions,
    accentColor: Color,
    vibrationsEnabled: Boolean,
    showScrollBlur: Boolean,
    borderContrast: Float,
    useHabitColorForCard: Boolean = true,
    isEditMode: Boolean = false,
    activeModules: List<String> = emptyList(),
    onRemoveModule: (String) -> Unit = {},
    onReorderModules: (List<String>) -> Unit = {}
) {
    val stats = remember(habit) {
        calculateStatistics(habit)
    }
    val monthlyStats = remember(habit) {
        calculateMonthlyStats(habit)
    }
    
    val onSurface = MaterialTheme.colorScheme.onSurface
    val displayAccentColor = remember(accentColor, onSurface) {
        lerp(accentColor, onSurface, 0.3f)
    }

    val modulesToRender = activeModules

    val currentModulesToRender by rememberUpdatedState(modulesToRender)
    val currentOnReorderModules by rememberUpdatedState(onReorderModules)

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val haptic = LocalHapticFeedback.current
    val lazyGridState = rememberLazyGridState()

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var fingerPositionX by remember { mutableFloatStateOf(0f) }
    var fingerPositionY by remember { mutableFloatStateOf(0f) }
    var touchOffsetWithinItem by remember { mutableStateOf(Offset.Zero) }
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (modulesToRender.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No modules selected for empty stats",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = (if (isEditMode) 96.dp else 80.dp) + navigationBarsPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = modulesToRender,
                key = { _, item -> item },
                span = { _, item ->
                    GridItemSpan(if (item == "monthly_chart") 2 else 1)
                }
            ) { index, moduleId ->
                val isBeingDragged = index == draggedItemIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val itemInfo = lazyGridState.layoutInfo.visibleItemsInfo.find { it.key == moduleId }
                            if (isBeingDragged && itemInfo != null) {
                                translationX = fingerPositionX - touchOffsetWithinItem.x - itemInfo.offset.x
                                translationY = fingerPositionY - touchOffsetWithinItem.y - itemInfo.offset.y
                                shadowElevation = 8.dp.value * density
                            } else {
                                translationX = 0f
                                translationY = 0f
                                shadowElevation = 0f
                            }
                            scaleX = 1f
                            scaleY = 1f
                        }
                        .then(
                            if (isEditMode) {
                                Modifier.pointerInput(moduleId) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggedItemIndex = currentModulesToRender.indexOf(moduleId)
                                            touchOffsetWithinItem = offset
                                            val itemInfo = lazyGridState.layoutInfo.visibleItemsInfo.find { it.key == moduleId }
                                            if (itemInfo != null) {
                                                fingerPositionX = itemInfo.offset.x + offset.x
                                                fingerPositionY = itemInfo.offset.y + offset.y
                                            }
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDragEnd = {
                                            draggedItemIndex = null
                                            fingerPositionX = 0f
                                            fingerPositionY = 0f
                                            touchOffsetWithinItem = Offset.Zero
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            }
                                        },
                                        onDragCancel = {
                                            draggedItemIndex = null
                                            fingerPositionX = 0f
                                            fingerPositionY = 0f
                                            touchOffsetWithinItem = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            fingerPositionX += dragAmount.x
                                            fingerPositionY += dragAmount.y
                                            
                                            val visibleItems = lazyGridState.layoutInfo.visibleItemsInfo
                                            val currentItem = visibleItems.find { it.key == moduleId }
                                            
                                            if (currentItem != null && currentItem.index == draggedItemIndex) {
                                                val cursorPosition = Offset(fingerPositionX, fingerPositionY)
                                                
                                                val targetItem = visibleItems.find { item ->
                                                    val itemOffset = item.offset
                                                    val itemSize = item.size
                                                    
                                                    val isInside = cursorPosition.x >= itemOffset.x && cursorPosition.x <= itemOffset.x + itemSize.width &&
                                                                   cursorPosition.y >= itemOffset.y && cursorPosition.y <= itemOffset.y + itemSize.height
                                                    
                                                    if (isInside && item.key != moduleId) {
                                                        val targetMidX = itemOffset.x + itemSize.width / 2f
                                                        val targetMidY = itemOffset.y + itemSize.height / 2f
                                                        
                                                        val buffer = 10 * density
                                                        val isBelow = itemOffset.y > currentItem.offset.y
                                                        val isAbove = itemOffset.y < currentItem.offset.y
                                                        val isRight = itemOffset.x > currentItem.offset.x
                                                        val isLeft = itemOffset.x < currentItem.offset.x
                                                        
                                                        when {
                                                            isBelow -> cursorPosition.y >= targetMidY + buffer
                                                            isAbove -> cursorPosition.y <= targetMidY - buffer
                                                            isRight -> cursorPosition.x >= targetMidX + buffer
                                                            isLeft -> cursorPosition.x <= targetMidX - buffer
                                                            else -> true
                                                        }
                                                    } else {
                                                        false
                                                    }
                                                }
                                                
                                                if (targetItem != null) {
                                                    val currentIndex = currentItem.index
                                                    val isDraggedFullWidth = moduleId == "monthly_chart"
                                                    
                                                    val targetRowItems = visibleItems.filter { 
                                                        kotlin.math.abs(it.offset.y - targetItem.offset.y) < 5 
                                                    }
                                                    val sortedTargetRowIndices = targetRowItems.map { item ->
                                                        modulesToRender.indexOfFirst { it == item.key }
                                                    }.filter { it != -1 }.sorted()

                                                    val insertIndex = if (sortedTargetRowIndices.isNotEmpty()) {
                                                        val firstTargetIndex = sortedTargetRowIndices.first()
                                                        val lastTargetIndex = sortedTargetRowIndices.last()
                                                        
                                                        if (kotlin.math.abs(currentItem.offset.y - targetItem.offset.y) < 5) {
                                                            targetItem.index
                                                        } else if (targetItem.offset.y > currentItem.offset.y) {
                                                            if (isDraggedFullWidth) lastTargetIndex else targetItem.index
                                                        } else {
                                                            if (isDraggedFullWidth) firstTargetIndex else targetItem.index
                                                        }
                                                    } else {
                                                        targetItem.index
                                                    }
                                                    
                                                    val newList = currentModulesToRender.toMutableList()
                                                    if (insertIndex != currentIndex && currentIndex in newList.indices && insertIndex in newList.indices) {
                                                        newList.removeAt(currentIndex)
                                                        newList.add(insertIndex, moduleId)
                                                        
                                                        val sanitizedList = sanitizeModuleOrder(
                                                            newList = newList,
                                                            oldList = currentModulesToRender,
                                                            draggedModuleId = moduleId
                                                        )
                                                        
                                                        val newDraggedIndex = sanitizedList.indexOf(moduleId)
                                                        if (newDraggedIndex != -1) {
                                                            draggedItemIndex = newDraggedIndex
                                                        }
                                                        currentOnReorderModules(sanitizedList)
                                                        if (vibrationsEnabled) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            } else Modifier
                        )
                        .then(
                            if (isBeingDragged) {
                                Modifier
                            } else {
                                Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    fadeInSpec = tween(200),
                                    fadeOutSpec = tween(200)
                                )
                            }
                        )
                ) {
                    RenderStatCard(
                        moduleId = moduleId,
                        stats = stats,
                        monthlyStats = monthlyStats,
                        displayAccentColor = displayAccentColor,
                        accentColor = accentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        vibrationsEnabled = vibrationsEnabled,
                        showScrollBlur = showScrollBlur,
                        isEditMode = isEditMode
                    )
                    
                    AnimatedVisibility(
                        visible = isEditMode,
                        enter = scaleIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 300
                            )
                        ),
                        exit = scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onRemoveModule(moduleId)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    secondaryValue: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    borderContrast: Float,
    useHabitColorForCard: Boolean = false,
    habitColor: Color = Color.Transparent
) {
    val cardBackgroundColor = if (useHabitColorForCard) {
        lerp(habitColor, MaterialTheme.colorScheme.surfaceVariant, 0.75f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val cardBorderColor = if (useHabitColorForCard) {
        lerp(habitColor, lerp(habitColor, MaterialTheme.colorScheme.surfaceVariant, 0.75f), 1f - borderContrast)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast)
    }

    Card(
        modifier = modifier.fillMaxWidth().height(96.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            if (secondaryValue != null) {
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    modifier: Modifier = Modifier,
    habits: List<HabitWithCompletions>,
    currentPage: Int,
    borderContrast: Float,
    useHabitColorForCard: Boolean = false,
    currentHabitColor: Color = Color.Transparent
) {
    val containerColor = if (useHabitColorForCard) {
        lerp(currentHabitColor, MaterialTheme.colorScheme.surfaceVariant, 0.85f).copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
    }

    val borderColor = if (useHabitColorForCard) {
        lerp(currentHabitColor, lerp(currentHabitColor, MaterialTheme.colorScheme.surfaceVariant, 0.85f), 1f - borderContrast)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            habits.forEachIndexed { index, _ ->
                val isSelected = index == currentPage
                val habitColor = habits[index].habit.color.let { Color(it) }
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) habitColor else habitColor.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun MonthlyCompletionGraph(
    stats: List<MonthlyCompletion>,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    vibrationsEnabled: Boolean,
    showScrollBlur: Boolean,
    borderContrast: Float,
    useHabitColorForCard: Boolean = false,
    habitColor: Color = Color.Transparent,
    interactive: Boolean = true
) {
    var isZoomedOut by remember { mutableStateOf(false) }

    val cardBackgroundColor = if (useHabitColorForCard) {
        lerp(habitColor, MaterialTheme.colorScheme.surfaceVariant, 0.75f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val cardBorderColor = if (useHabitColorForCard) {
        lerp(habitColor, lerp(habitColor, MaterialTheme.colorScheme.surfaceVariant, 0.75f), 1f - borderContrast)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Completion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (interactive) {
                    IconButton(
                        onClick = { isZoomedOut = !isZoomedOut },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isZoomedOut) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                            contentDescription = if (isZoomedOut) "Zoom In" else "Zoom Out",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (stats.isEmpty()) {
                Box(modifier = Modifier.height(180.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No data available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (isZoomedOut) {
                    MonthlyLineChart(
                        data = stats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        showLabels = true,
                        lineColor = accentColor,
                        vibrationsEnabled = vibrationsEnabled,
                        interactive = interactive
                    )
                } else {
                    val minWidthPerItem = 44.dp
                    val calculatedWidth = minWidthPerItem * stats.size
                    val scrollState = rememberScrollState()
                    val coroutineScope = rememberCoroutineScope()
                    val flingBehavior = ScrollableDefaults.flingBehavior()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .fadingEdge(scrollState, enabled = showScrollBlur)
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    if (scrollState.maxValue > 0) {
                                        scrollState.dispatchRawDelta(delta)
                                    }
                                },
                                orientation = Orientation.Horizontal,
                                enabled = scrollState.maxValue > 0 && interactive,
                                onDragStopped = { velocity ->
                                    scrollState.scroll {
                                        with(flingBehavior) {
                                            performFling(velocity)
                                        }
                                    }
                                }
                            )
                            .horizontalScroll(
                                state = scrollState,
                                reverseScrolling = true,
                                enabled = false 
                            )
                    ) {
                        MonthlyLineChart(
                            data = stats,
                            modifier = Modifier
                                .width(max(300.dp, calculatedWidth))
                                .fillMaxHeight(),
                            showLabels = true,
                            lineColor = accentColor,
                            vibrationsEnabled = vibrationsEnabled,
                            interactive = interactive,
                            onPointSelected = { x ->
                                if (interactive) {
                                    coroutineScope.launch {
                                        val viewportWidth = scrollState.viewportSize
                                        if (viewportWidth > 0) {
                                            val targetScrollValue =
                                                scrollState.maxValue + (viewportWidth / 2f) - x
                                            scrollState.animateScrollTo(
                                                targetScrollValue
                                                    .coerceIn(0f, scrollState.maxValue.toFloat())
                                                    .toInt()
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun sanitizeStaticModuleList(list: List<String>): List<String> {
    val chartIndex = list.indexOf("monthly_chart")
    if (chartIndex == -1) return list
    if (chartIndex % 2 == 0) return list
    
    val targetIndex = if (chartIndex + 1 < list.size) chartIndex + 1 else chartIndex - 1
    val resolvedIndex = if (targetIndex % 2 == 0) targetIndex else {
        if (targetIndex - 1 >= 0) targetIndex - 1 else 0
    }
    
    if (resolvedIndex == chartIndex) return list
    
    val result = list.toMutableList()
    result.removeAt(chartIndex)
    result.add(resolvedIndex, "monthly_chart")
    return result
}

private fun sanitizeModuleOrder(
    newList: List<String>,
    oldList: List<String>,
    draggedModuleId: String?
): List<String> {
    val chartIndex = newList.indexOf("monthly_chart")
    if (chartIndex == -1) return newList
    
    if (chartIndex % 2 == 0) return newList
    
    val oldChartIndex = oldList.indexOf("monthly_chart")
    
    val targetIndex = if (draggedModuleId == "monthly_chart") {
        if (chartIndex > oldChartIndex) {
            chartIndex + 1
        } else {
            chartIndex - 1
        }
    } else if (draggedModuleId != null) {
        val oldModuleIndex = oldList.indexOf(draggedModuleId)
        val newModuleIndex = newList.indexOf(draggedModuleId)
        
        if (oldModuleIndex != -1 && newModuleIndex != -1) {
            if (oldModuleIndex > oldChartIndex && newModuleIndex < chartIndex) {
                chartIndex + 1
            } else if (oldModuleIndex < oldChartIndex && newModuleIndex > chartIndex) {
                chartIndex - 1
            } else {
                if (chartIndex > oldChartIndex) chartIndex + 1 else chartIndex - 1
            }
        } else {
            if (chartIndex > oldChartIndex) chartIndex + 1 else chartIndex - 1
        }
    } else {
        if (chartIndex + 1 < newList.size) chartIndex + 1 else chartIndex - 1
    }
    
    val coercedIndex = targetIndex.coerceIn(0, newList.size - 1)
    val finalIndex = if (coercedIndex % 2 == 0) coercedIndex else {
        if (coercedIndex > chartIndex) {
            (coercedIndex + 1).coerceAtMost(newList.size - 1)
        } else {
            (coercedIndex - 1).coerceAtLeast(0)
        }
    }
    
    val resolvedIndex = if (finalIndex % 2 == 0) finalIndex else {
        if (finalIndex - 1 >= 0) finalIndex - 1 else 0
    }
    
    if (resolvedIndex == chartIndex) return newList
    
    val result = newList.toMutableList()
    result.removeAt(chartIndex)
    result.add(resolvedIndex, "monthly_chart")
    return result
}
