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
    borderContrast: Float,
    useHabitColorForCard: Boolean = true
) {
    val stats = remember(habit) {
        calculateStatistics(habit)
    }
    val monthlyStats = remember(habit) {
        calculateMonthlyStats(habit)
    }
    
    // Capture theme color outside the remember lambda to avoid @Composable invocation error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val displayAccentColor = remember(accentColor, onSurface) {
        lerp(accentColor, onSurface, 0.3f)
    }

    val cardBackgroundColor = if (useHabitColorForCard) {
        lerp(accentColor, MaterialTheme.colorScheme.surfaceVariant, 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val cardBorderColor = if (useHabitColorForCard) {
        lerp(accentColor, lerp(accentColor, MaterialTheme.colorScheme.surfaceVariant, 0.85f), 1f - borderContrast)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderContrast)
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
            colors = CardDefaults.cardColors(
                containerColor = cardBackgroundColor
            ),
            border = BorderStroke(1.dp, cardBorderColor)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Longest Streak",
                        value = "${stats.longestStreak} days",
                        secondaryValue = if (stats.daysSinceLongestStreak > 0) "${stats.daysSinceLongestStreak} days ago" else "Current",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )
                    StatCard(
                        label = "Completed Ratio",
                        value = "${stats.completionRatio}%",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Avg. Completion Time",
                        value = stats.averageCompletionTime,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )
                    StatCard(
                        label = "Days since creation",
                        value = "${stats.timeSinceCreation}",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = displayAccentColor,
                        borderContrast = borderContrast,
                        useHabitColorForCard = useHabitColorForCard,
                        habitColor = accentColor
                    )
                }

                MonthlyCompletionGraph(
                    stats = monthlyStats,
                    accentColor = accentColor,
                    vibrationsEnabled = vibrationsEnabled,
                    showScrollBlur = showScrollBlur,
                    borderContrast = borderContrast,
                    useHabitColorForCard = useHabitColorForCard,
                    habitColor = accentColor
                )
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
        modifier = modifier.fillMaxWidth(),
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
    habits: List<HabitWithCompletions>,
    currentPage: Int,
    borderContrast: Float,
    useHabitColorForCard: Boolean = false,
    currentHabitColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
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
    habitColor: Color = Color.Transparent
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Completion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { isZoomedOut = !isZoomedOut }) {
                    Icon(
                        imageVector = if (isZoomedOut) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                        contentDescription = if (isZoomedOut) "Zoom In" else "Zoom Out",
                        modifier = Modifier.size(20.dp)
                    )
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
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    if (scrollState.maxValue > 0) {
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
