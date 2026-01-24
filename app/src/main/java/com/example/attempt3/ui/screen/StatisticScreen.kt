package com.example.attempt3.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.attempt3.data.Database.HabitViewModel
import com.example.attempt3.data.Database.HabitWithCompletions
import com.example.attempt3.data.Database.HabitsUiState
import com.example.attempt3.data.MonthlyCompletion
import com.example.attempt3.data.calculateMonthlyStats
import com.example.attempt3.data.calculateStatistics
import com.example.attempt3.ui.components.MonthlyLineChart
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    initialHabitId: String? = null
) {
    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val habits = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()

    val actualCount = habits.size
    val pageCount = if (actualCount > 1) Int.MAX_VALUE else actualCount

    val pagerState = rememberPagerState(pageCount = { pageCount })

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

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentHabitColor by remember(habits, actualCount, primaryColor) {
        derivedStateOf {
            val currentIndex = if (actualCount > 0) ((pagerState.currentPage % actualCount) + actualCount) % actualCount else 0
            habits.getOrNull(currentIndex)?.habit?.color?.let { Color(it) } ?: primaryColor
        }
    }

    val backgroundColor by remember(habits, actualCount, surfaceColor, primaryColor) {
        derivedStateOf {
            if (actualCount == 0) return@derivedStateOf Color.Transparent
            
            val page = pagerState.currentPage
            val offset = pagerState.currentPageOffsetFraction
            
            val index1 = ((page % actualCount) + actualCount) % actualCount
            val color1 = habits.getOrNull(index1)?.habit?.color?.let { Color(it) } ?: primaryColor
            
            val nextPageIndex = if (offset > 0) page + 1 else if (offset < 0) page - 1 else page
            val index2 = ((nextPageIndex % actualCount) + actualCount) % actualCount
            val color2 = habits.getOrNull(index2)?.habit?.color?.let { Color(it) } ?: color1
            
            val morphedHabitColor = lerp(color1, color2, offset.absoluteValue)
            lerp(surfaceColor, morphedHabitColor, 0.4f)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    val currentIndex = if (actualCount > 0) pagerState.currentPage % actualCount else 0
                    val currentHabitName = habits.getOrNull(currentIndex)?.habit?.name
                    
                    Text(
                        text = "Statistics for ${currentHabitName}",
                        fontWeight = FontWeight.SemiBold,
                        color = currentHabitColor
                    )
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Back", 
                            modifier = Modifier.size(24.dp), 
                            tint = currentHabitColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
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
                         modifier = Modifier.fillMaxSize(),
                         pageSpacing = 24.dp,
                         beyondViewportPageCount = 1
                     ) { page ->
                         val index = page % actualCount
                         val habit = habits.getOrNull(index)
                         if (habit != null) {
                             val habitColor = habit.habit.color?.let { Color(it) } ?: primaryColor
                             HabitStatisticsContent(
                                 habit = habit,
                                 accentColor = habitColor
                             )
                         }
                     }

                     if (actualCount > 1) {
                         PageIndicator(
                             habits = habits,
                             currentPage = pagerState.currentPage % actualCount,
                             modifier = Modifier
                                 .align(Alignment.BottomCenter)
                                 .padding(bottom = 16.dp)
                         )
                     }
                 }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    habits: List<HabitWithCompletions>,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun HabitStatisticsContent(
    habit: HabitWithCompletions,
    accentColor: Color
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
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /*Text(
                    text = "Statistics for ${habit.habit.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )*/

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Longest Streak",
                        value = "${stats.longestStreak} days",
                        modifier = Modifier.weight(1f),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f)
                    )
                    StatCard(
                        label = "Completion Ratio",
                        value = "${stats.completionRatio}%",
                        modifier = Modifier.weight(1f),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        label = "Average Completion Time",
                        value = stats.averageCompletionTime,
                        modifier = Modifier.weight(1f),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f)
                    )
                    StatCard(
                        label = "Days Since Habit Creation",
                        value = "${stats.timeSinceCreation}",
                        modifier = Modifier.weight(1f),
                        accentColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.35f)
                    )
                }

                MonthlyCompletionGraph(
                    stats = monthlyStats,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accentColor: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
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
        }
    }
}

@Composable
fun MonthlyCompletionGraph(
    stats: List<MonthlyCompletion>, 
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var isZoomedOut by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth()
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
                        lineColor = accentColor
                    )
                } else {
                    val minWidthPerItem = 44.dp 
                    val calculatedWidth = minWidthPerItem * stats.size
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .horizontalScroll(
                                state = rememberScrollState(),
                                reverseScrolling = true
                            )
                    ) {
                        MonthlyLineChart(
                            data = stats,
                            modifier = Modifier
                                .width(max(300.dp, calculatedWidth))
                                .fillMaxHeight(),
                            showLabels = true,
                            lineColor = accentColor
                        )
                    }
                }
            }
        }
    }
}
