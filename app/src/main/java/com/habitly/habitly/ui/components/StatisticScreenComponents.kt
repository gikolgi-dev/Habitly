/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp

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
    if (moduleId.startsWith("spacer_")) {
        Spacer(modifier = Modifier.fillMaxWidth().height(96.dp))
        return
    }
    val isFullWidth = moduleId.endsWith("_full")
    val baseModuleId = if (isFullWidth) moduleId.removeSuffix("_full") else moduleId
    when (baseModuleId) {
        "longest_streak" -> StatCard(
            label = "Longest Streak",
            value = "${stats.longestStreak} days",
            secondaryValue = if (stats.daysSinceLongestStreak > 0) "${stats.daysSinceLongestStreak} days ago" else "Current",
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "current_streak" -> StatCard(
            label = "Current Streak",
            value = "${stats.currentStreak} days",
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "completion_ratio" -> StatCard(
            label = "Completed Ratio",
            value = "${stats.completionRatio}%",
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "avg_completion_time" -> StatCard(
            label = "Avg. Completion Time",
            value = stats.averageCompletionTime,
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "days_since_creation" -> StatCard(
            label = "Days since creation",
            value = "${stats.timeSinceCreation}",
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "total_completions" -> StatCard(
            label = "Total Completions",
            value = "${stats.totalCompletions}",
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "best_day_of_week" -> StatCard(
            label = "Best Day of the Week",
            value = stats.bestDayOfWeek,
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
        )
        "rate_last_30_days" -> StatCard(
            label = "Rate Last 30 Days",
            value = "${stats.rateLast30Days}%",
            accentColor = displayAccentColor,
            borderContrast = borderContrast,
            useHabitColorForCard = useHabitColorForCard,
            habitColor = accentColor,
            isFullWidth = isFullWidth
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
    var selectedModuleForResize by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            selectedModuleForResize = null
        }
    }

    fun resizeModule(
        moduleId: String,
        expand: Boolean,
        shrinkLeftToRight: Boolean = false
    ) {
        val newList = activeModules.toMutableList()
        val index = newList.indexOf(moduleId)
        if (index == -1) return
        
        if (expand) {
            val baseId = if (moduleId.endsWith("_full")) moduleId.removeSuffix("_full") else moduleId
            val newId = "${baseId}_full"
            newList[index] = newId
            
            // Remove any adjacent spacers
            if (index > 0 && newList[index - 1].startsWith("spacer_")) {
                newList.removeAt(index - 1)
            } else if (index + 1 < newList.size && newList[index + 1].startsWith("spacer_")) {
                newList.removeAt(index + 1)
            }
            
            selectedModuleForResize = newId
        } else {
            val baseId = if (moduleId.endsWith("_full")) moduleId.removeSuffix("_full") else moduleId
            
            if (shrinkLeftToRight) {
                var lowerModuleIndex = -1
                for (j in (index + 1) until newList.size) {
                    val item = newList[j]
                    val isItemFull = item == "monthly_chart" || item.endsWith("_full")
                    if (!isItemFull && !item.startsWith("spacer_")) {
                        lowerModuleIndex = j
                        break
                    }
                }
                
                if (lowerModuleIndex != -1) {
                    val lowerModule = newList[lowerModuleIndex]
                    newList.removeAt(lowerModuleIndex)
                    newList[index] = lowerModule
                    newList.add(index + 1, baseId)
                } else {
                    newList[index] = "spacer_${System.currentTimeMillis()}"
                    newList.add(index + 1, baseId)
                }
            } else {
                newList[index] = baseId
            }
            
            selectedModuleForResize = baseId
        }
        
        onReorderModules(newList)
    }

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

    val moduleColumns = remember(modulesToRender) {
        val map = mutableMapOf<String, Int>()
        var currentColumn = 0
        for (id in modulesToRender) {
            val isFull = id == "monthly_chart" || id.endsWith("_full")
            if (isFull) {
                if (currentColumn == 1) {
                    currentColumn = 0
                }
                map[id] = 0
                currentColumn = 0
            } else {
                map[id] = currentColumn
                currentColumn = (currentColumn + 1) % 2
            }
        }
        map
    }

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
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = (if (isEditMode) 96.dp else 80.dp) + navigationBarsPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = modulesToRender,
                key = { _, item -> if (item.endsWith("_full")) item.removeSuffix("_full") else item },
                span = { _, item ->
                    GridItemSpan(if (item == "monthly_chart" || item.endsWith("_full")) 2 else 1)
                }
            ) { index, moduleId ->
                val isBeingDragged = index == draggedItemIndex

                var cardWidthPx by remember { mutableStateOf(0) }
                val dragAmountXAnim = remember { Animatable(0f) }
                var isDraggingLeft by remember { mutableStateOf(false) }
                var isDraggingRight by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val isFull = moduleId.endsWith("_full")
                val isSelectedForResize = isEditMode && selectedModuleForResize == moduleId

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isSelectedForResize || isDraggingLeft || isDraggingRight) 1f else 0f)
                        .onGloballyPositioned { coordinates ->
                            cardWidthPx = coordinates.size.width
                        }
                        .graphicsLayer {
                            if (cardWidthPx > 0 && (isDraggingLeft || isDraggingRight)) {
                                val dragVal = dragAmountXAnim.value
                                val scale = if (isDraggingLeft) {
                                    if (isFull) {
                                        ((cardWidthPx - dragVal.coerceAtLeast(0f)) / cardWidthPx).coerceIn(0.5f, 1f)
                                    } else {
                                        ((cardWidthPx - dragVal.coerceAtMost(0f)) / cardWidthPx).coerceIn(1f, 2f)
                                    }
                                } else {
                                    if (isFull) {
                                        ((cardWidthPx + dragVal.coerceAtMost(0f)) / cardWidthPx).coerceIn(0.5f, 1f)
                                    } else {
                                        ((cardWidthPx + dragVal.coerceAtLeast(0f)) / cardWidthPx).coerceIn(1f, 2f)
                                    }
                                }
                                scaleX = scale
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (isDraggingLeft) 1f else 0f,
                                    pivotFractionY = 0.5f
                                )
                            }
                            
                            val stableKey = if (moduleId.endsWith("_full")) moduleId.removeSuffix("_full") else moduleId
                            val itemInfo = lazyGridState.layoutInfo.visibleItemsInfo.find { it.key == stableKey }
                            if (isBeingDragged && itemInfo != null) {
                                translationX = fingerPositionX - touchOffsetWithinItem.x - itemInfo.offset.x
                                translationY = fingerPositionY - touchOffsetWithinItem.y - itemInfo.offset.y
                                shadowElevation = 8.dp.value * density
                            } else {
                                if (!(isDraggingLeft || isDraggingRight)) {
                                    translationX = 0f
                                    translationY = 0f
                                }
                                shadowElevation = 0f
                            }
                        }
                        .then(
                            if (isEditMode && moduleId != "monthly_chart" && !moduleId.startsWith("spacer_")) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedModuleForResize = if (selectedModuleForResize == moduleId) null else moduleId
                                }
                            } else Modifier
                        )
                        .then(
                            if (isEditMode && !moduleId.startsWith("spacer_")) {
                                Modifier.pointerInput(moduleId) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val isLongPress = withTimeoutOrNull(300) {
                                            var pointerId = down.id
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == pointerId }
                                                if (change == null || change.changedToUp()) {
                                                    return@withTimeoutOrNull false
                                                }
                                                val dist = (change.position - down.position).getDistance()
                                                if (dist > viewConfiguration.touchSlop) {
                                                    return@withTimeoutOrNull false
                                                }
                                            }
                                            false
                                        }
                                        
                                        if (isLongPress == null) {
                                            // Trigger onDragStart
                                            selectedModuleForResize = null
                                            draggedItemIndex = currentModulesToRender.indexOf(moduleId)
                                            touchOffsetWithinItem = down.position
                                            val stableKey = if (moduleId.endsWith("_full")) moduleId.removeSuffix("_full") else moduleId
                                            val itemInfo = lazyGridState.layoutInfo.visibleItemsInfo.find { it.key == stableKey }
                                            if (itemInfo != null) {
                                                fingerPositionX = itemInfo.offset.x + down.position.x
                                                fingerPositionY = itemInfo.offset.y + down.position.y
                                            }
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            
                                            // Track drag
                                            var pointerId = down.id
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == pointerId }
                                                if (change == null) {
                                                    // onDragCancel
                                                    draggedItemIndex = null
                                                    fingerPositionX = 0f
                                                    fingerPositionY = 0f
                                                    touchOffsetWithinItem = Offset.Zero
                                                    break
                                                }
                                                if (change.changedToUp()) {
                                                    // onDragEnd
                                                    draggedItemIndex = null
                                                    fingerPositionX = 0f
                                                    fingerPositionY = 0f
                                                    touchOffsetWithinItem = Offset.Zero
                                                    if (vibrationsEnabled) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                                    }
                                                    break
                                                }
                                                
                                                // onDrag
                                                change.consume()
                                                val dragAmount = change.position - change.previousPosition
                                                fingerPositionX += dragAmount.x
                                                fingerPositionY += dragAmount.y
                                                
                                                val visibleItems = lazyGridState.layoutInfo.visibleItemsInfo
                                                val currentItem = visibleItems.find { it.key == stableKey }
                                                
                                                if (currentItem != null && currentItem.index == draggedItemIndex) {
                                                    val cursorPosition = Offset(fingerPositionX, fingerPositionY)
                                                    
                                                    val targetItem = visibleItems.find { item ->
                                                        val itemOffset = item.offset
                                                        val itemSize = item.size
                                                        
                                                        val isInside = cursorPosition.x >= itemOffset.x && cursorPosition.x <= itemOffset.x + itemSize.width &&
                                                                       cursorPosition.y >= itemOffset.y && cursorPosition.y <= itemOffset.y + itemSize.height
                                                        
                                                         if (isInside && item.key != stableKey) {
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
                                                        val isDraggedFullWidth = moduleId == "monthly_chart" || moduleId.endsWith("_full")
                                                        
                                                        val targetRowItems = visibleItems.filter { 
                                                            kotlin.math.abs(it.offset.y - targetItem.offset.y) < 5 
                                                        }
                                                        val sortedTargetRowIndices = targetRowItems.map { item ->
                                                            modulesToRender.indexOfFirst {
                                                                val b1 = if (it.endsWith("_full")) it.removeSuffix("_full") else it
                                                                b1 == item.key
                                                            }
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
                                        }
                                    }
                                }
                            } else Modifier
                        )
                        .then(
                            if (isBeingDragged) {
                                Modifier
                            } else {
                                Modifier.animateItem(
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
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
                    
                    val selectionAlpha by animateFloatAsState(
                        targetValue = if (isSelectedForResize) 1f else 0f,
                        animationSpec = tween(300),
                        label = "selectionAlpha"
                    )
                    
                    if (selectionAlpha > 0f) {
                        val strokeColor = if (useHabitColorForCard) accentColor else MaterialTheme.colorScheme.primary
                        val columnIndex = moduleColumns[moduleId] ?: 0
                        val showLeftDot = isFull || columnIndex == 1
                        val showRightDot = isFull || columnIndex == 0
                        
                        val leftDotAlpha by animateFloatAsState(
                            targetValue = if (showLeftDot) 1f else 0f,
                            animationSpec = tween(300),
                            label = "leftDotAlpha"
                        )
                        val rightDotAlpha by animateFloatAsState(
                            targetValue = if (showRightDot) 1f else 0f,
                            animationSpec = tween(300),
                            label = "rightDotAlpha"
                        )
                        
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        border = BorderStroke(2.dp, strokeColor.copy(alpha = selectionAlpha)),
                                        shape = CardDefaults.shape
                                    )
                            )
                            
                            // Left dot
                            if (leftDotAlpha > 0f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (-24).dp)
                                        .size(48.dp)
                                        .systemGestureExclusion()
                                        .pointerInput(moduleId) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    if (!showLeftDot) return@detectDragGestures
                                                    isDraggingLeft = true
                                                    coroutineScope.launch { dragAmountXAnim.snapTo(0f) }
                                                },
                                                onDragEnd = {
                                                    if (!showLeftDot) return@detectDragGestures
                                                    val finalDrag = dragAmountXAnim.value
                                                    val threshold = cardWidthPx / 3.5f
                                                    var resized = false
                                                    if (isFull) {
                                                        if (finalDrag > threshold) {
                                                            resizeModule(moduleId, expand = false, shrinkLeftToRight = true)
                                                            resized = true
                                                        }
                                                    } else {
                                                        if (finalDrag < -threshold) {
                                                            resizeModule(moduleId, expand = true)
                                                            resized = true
                                                        }
                                                    }
                                                    
                                                    if (resized && vibrationsEnabled) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    
                                                    coroutineScope.launch {
                                                        dragAmountXAnim.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
                                                        isDraggingLeft = false
                                                    }
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch {
                                                        dragAmountXAnim.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
                                                        isDraggingLeft = false
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (!showLeftDot) return@detectDragGestures
                                                    change.consume()
                                                    coroutineScope.launch {
                                                        dragAmountXAnim.snapTo(dragAmountXAnim.value + dragAmount.x)
                                                    }
                                                }
                                            )
                                        }
                                        .clickable(
                                            enabled = showLeftDot,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (isFull) {
                                                resizeModule(moduleId, expand = false, shrinkLeftToRight = true)
                                            } else {
                                                resizeModule(moduleId, expand = true)
                                            }
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(16.dp)
                                            .background(strokeColor.copy(alpha = selectionAlpha * leftDotAlpha), CircleShape)
                                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = selectionAlpha * leftDotAlpha)), CircleShape)
                                    )
                                }
                            }
                            
                            // Right dot
                            if (rightDotAlpha > 0f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 24.dp)
                                        .size(48.dp)
                                        .systemGestureExclusion()
                                        .pointerInput(moduleId) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    if (!showRightDot) return@detectDragGestures
                                                    isDraggingRight = true
                                                    coroutineScope.launch { dragAmountXAnim.snapTo(0f) }
                                                },
                                                onDragEnd = {
                                                    if (!showRightDot) return@detectDragGestures
                                                    val finalDrag = dragAmountXAnim.value
                                                    val threshold = cardWidthPx / 3.5f
                                                    var resized = false
                                                    if (isFull) {
                                                        if (finalDrag < -threshold) {
                                                            resizeModule(moduleId, expand = false, shrinkLeftToRight = false)
                                                            resized = true
                                                        }
                                                    } else {
                                                        if (finalDrag > threshold) {
                                                            resizeModule(moduleId, expand = true)
                                                            resized = true
                                                        }
                                                    }
                                                    
                                                    if (resized && vibrationsEnabled) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    
                                                    coroutineScope.launch {
                                                        dragAmountXAnim.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
                                                        isDraggingRight = false
                                                    }
                                                },
                                                onDragCancel = {
                                                    coroutineScope.launch {
                                                        dragAmountXAnim.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
                                                        isDraggingRight = false
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (!showRightDot) return@detectDragGestures
                                                    change.consume()
                                                    coroutineScope.launch {
                                                        dragAmountXAnim.snapTo(dragAmountXAnim.value + dragAmount.x)
                                                    }
                                                }
                                            )
                                        }
                                        .clickable(
                                            enabled = showRightDot,
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (isFull) {
                                                resizeModule(moduleId, expand = false, shrinkLeftToRight = false)
                                            } else {
                                                resizeModule(moduleId, expand = true)
                                            }
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(16.dp)
                                            .background(strokeColor.copy(alpha = selectionAlpha * rightDotAlpha), CircleShape)
                                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = selectionAlpha * rightDotAlpha)), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = isEditMode && !moduleId.startsWith("spacer_"),
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
    habitColor: Color = Color.Transparent,
    isFullWidth: Boolean = false
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

    val height = 96.dp

    Card(
        modifier = modifier.fillMaxWidth().height(height),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        if (isFullWidth) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    if (secondaryValue != null) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = secondaryValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
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
    val result = list.toMutableList()
    var i = 0
    var currentSumOfSpans = 0
    while (i < result.size) {
        val id = result[i]
        val span = if (id == "monthly_chart" || id.endsWith("_full")) 2 else 1
        if (span == 2 && currentSumOfSpans % 2 != 0) {
            if (i > 0) {
                val temp = result[i]
                result[i] = result[i - 1]
                result[i - 1] = temp
                i--
                currentSumOfSpans--
                continue
            }
        }
        currentSumOfSpans += span
        i++
    }
    return result
}

private fun sanitizeModuleOrder(
    newList: List<String>,
    oldList: List<String>,
    draggedModuleId: String?
): List<String> {
    return sanitizeStaticModuleList(newList)
}
