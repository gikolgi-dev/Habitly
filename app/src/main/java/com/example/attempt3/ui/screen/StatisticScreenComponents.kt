package com.example.attempt3.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.attempt3.data.Database.HabitWithCompletions
import com.example.attempt3.data.MonthlyCompletion
import com.example.attempt3.data.calculateMonthlyStats
import com.example.attempt3.data.calculateStatistics
import com.example.attempt3.ui.components.MonthlyLineChart
import com.example.attempt3.ui.fadingEdge
import kotlinx.coroutines.launch

@Composable
fun HabitStatisticsContent(
    habit: HabitWithCompletions,
    accentColor: Color,
    vibrationsEnabled: Boolean,
    showScrollBlur: Boolean,
    borderContrast: Float
) {
    val stats = remember(habit) {
        calculateStatistics(habit)
    }
    val monthlyStats = remember(habit) {
        calculateMonthlyStats(habit)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = borderContrast))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Longest Streak",
                        value = "${stats.longestStreak} days",
                        secondaryValue = if (stats.daysSinceLongestStreak > 0) "${stats.daysSinceLongestStreak} days ago" else "Current",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f),
                        borderContrast = borderContrast
                    )
                    StatCard(
                        label = "Completion Ratio",
                        value = "${stats.completionRatio}%",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f),
                        borderContrast = borderContrast
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Average Completion Time",
                        value = stats.averageCompletionTime,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f),
                        borderContrast = borderContrast
                    )
                    StatCard(
                        label = "Days Since Habit Creation",
                        value = "${stats.timeSinceCreation}",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f),
                        borderContrast = borderContrast
                    )
                }

                MonthlyCompletionGraph(
                    stats = monthlyStats,
                    accentColor = accentColor,
                    vibrationsEnabled = vibrationsEnabled,
                    showScrollBlur = showScrollBlur,
                    borderContrast = borderContrast
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    secondaryValue: String? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    borderContrast: Float
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = borderContrast))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            if (secondaryValue != null) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    habits: List<HabitWithCompletions>,
    currentPage: Int,
    borderContrast: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 2.5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = borderContrast))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            habits.forEachIndexed { index, habit ->
                val isSelected = index == currentPage
                val habitColor = habit.habit.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) habitColor else habitColor.copy(alpha = 0.25f),
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
    borderContrast: Float
) {
    var isZoomedOut by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = borderContrast))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Completion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { isZoomedOut = !isZoomedOut }) {
                    Icon(
                        imageVector = if (isZoomedOut) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                        contentDescription = if (isZoomedOut) "Zoom In" else "Zoom Out"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (stats.isEmpty()) {
                Text("No data available", style = MaterialTheme.typography.bodyMedium)
            } else {
                if (isZoomedOut) {
                    MonthlyLineChart(
                        data = stats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        showLabels = false,
                        lineColor = accentColor,
                        vibrationsEnabled = vibrationsEnabled
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
                            // Explicitly handle horizontal drags to prevent conflict with HorizontalPager.
                            // We only consume and handle the drag if the content is actually scrollable.
                            // draggable provides inertia (fling) support via onDragStopped.
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    if (scrollState.maxValue > 0) {
                                        // delta is positive when dragging right.
                                        // In reverseScrolling=true, dragging right increases the scroll value.
                                        scrollState.dispatchRawDelta(delta)
                                    }
                                },
                                orientation = Orientation.Horizontal,
                                enabled = scrollState.maxValue > 0,
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
                                enabled = false // Manual handling via draggable above
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
                            onPointSelected = { x ->
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
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
