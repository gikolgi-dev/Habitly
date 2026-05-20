/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.Database.HabitViewModel
import com.habitly.habitly.data.Database.HabitsUiState
import com.habitly.habitly.ui.AppBackButton
import com.habitly.habitly.ui.components.HabitStatisticsContent
import com.habitly.habitly.ui.components.PageIndicator

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
    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val habits = (habitsUiState as? HabitsUiState.Success)?.habits?.filterNot { it.habit.archived } ?: emptyList()
    val haptic = LocalHapticFeedback.current

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
            val currentIndex = if (actualCount > 0) ((pagerState.currentPage % actualCount) + actualCount) % actualCount else 0
            habits.getOrNull(currentIndex)?.habit?.color?.let { Color(it) } ?: primaryColor
        }
    }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (useHabitColor && habits.isNotEmpty()) {
            lerp(currentHabitColor, MaterialTheme.colorScheme.surface, 0.95f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    val backButtonBackgroundColor = if (useHabitColor && habits.isNotEmpty()) {
        lerp(currentHabitColor, MaterialTheme.colorScheme.surfaceVariant, 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val currentIndex = if (actualCount > 0) pagerState.currentPage % actualCount else 0
                    val currentHabitName = habits.getOrNull(currentIndex)?.habit?.name

                    Text(
                        text = "Statistics for $currentHabitName",
                        fontWeight = FontWeight.SemiBold,
                        color = currentHabitColor
                    )
                },
                actions = {
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
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 24.dp,
                        beyondViewportPageCount = 1
                    ) { page ->
                        val index = page % actualCount
                        val habit = habits.getOrNull(index)
                        if (habit != null) {
                            val habitColor = Color(habit.habit.color)
                            HabitStatisticsContent(
                                habit = habit,
                                accentColor = habitColor,
                                vibrationsEnabled = vibrationsEnabled,
                                showScrollBlur = showScrollBlur && "Line Chart" in scrollBlurTargets,
                                borderContrast = borderContrast,
                                useHabitColorForCard = useHabitColor
                            )
                        }
                    }

                    if (actualCount > 1) {
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
                }
            }
        }
    }
}