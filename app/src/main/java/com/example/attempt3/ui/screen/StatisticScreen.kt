package com.example.attempt3.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.attempt3.data.Database.HabitViewModel
import com.example.attempt3.data.Database.HabitsUiState
import com.example.attempt3.data.settings.SettingsDataStore
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    initialHabitId: String? = null,
    settingsDataStore: SettingsDataStore
) {
    val habitsUiState by viewModel.habitsUiState.collectAsState()
    val habits = (habitsUiState as? HabitsUiState.Success)?.habits ?: emptyList()
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val showScrollBlur by settingsDataStore.showScrollBlur.collectAsState(initial = true)
    val scrollBlurTargets by settingsDataStore.scrollBlurTargets.collectAsState(initial = setOf("Heatmap", "Line Chart"))
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

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
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
                        text = "Statistics for $currentHabitName",
                        fontWeight = FontWeight.SemiBold,
                        color = currentHabitColor
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        }
                        onBack()
                    }) {
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
                                accentColor = habitColor,
                                vibrationsEnabled = vibrationsEnabled,
                                showScrollBlur = showScrollBlur && "Line Chart" in scrollBlurTargets
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
