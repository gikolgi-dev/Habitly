package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderScreen(habitViewModel: HabitViewModel, onBack: () -> Unit, settingsDataStore: SettingsDataStore) {
    val habitsUiState by habitViewModel.habitsUiState.collectAsState()
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Reorder Habits", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            content = { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (val state = habitsUiState) {
                        is HabitsUiState.Loading -> {
                            Text("Loading...")
                        }
                        is HabitsUiState.Success -> {
                            var habits by remember { mutableStateOf(state.habits.map { it.habit }) }
                            var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
                            var verticalDragOffset by remember { mutableStateOf(0f) }
                            val itemHeightDp = 88.dp
                            val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }
                            val haptic = LocalHapticFeedback.current
                            val viewConfiguration = LocalViewConfiguration.current
                            val shortPressViewConfiguration = remember(viewConfiguration) {
                                object : ViewConfiguration {
                                    override val longPressTimeoutMillis: Long = 50L // Default is 500L
                                    override val doubleTapTimeoutMillis: Long = viewConfiguration.doubleTapTimeoutMillis
                                    override val doubleTapMinTimeMillis: Long = viewConfiguration.doubleTapMinTimeMillis
                                    override val touchSlop: Float = viewConfiguration.touchSlop
                                }
                            }

                            CompositionLocalProvider(LocalViewConfiguration provides shortPressViewConfiguration) {
                                LazyColumn(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    itemsIndexed(habits, key = { _, habit -> habit.id }) { index, habit ->
                                        val isBeingDragged = index == draggedItemIndex
                                        val displacement = if (isBeingDragged) verticalDragOffset else 0f
                                        val latestIndex by rememberUpdatedState(index)

                                        ReorderHabitItem(
                                            habit = habit,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    translationY = displacement
                                                    shadowElevation = if (isBeingDragged) 8.dp.toPx() else 0f
                                                }
                                                .pointerInput(habit) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggedItemIndex = latestIndex
                                                            if (vibrationsEnabled) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            draggedItemIndex?.let {
                                                                val reorderedHabits = habits.mapIndexed { newIndex, h ->
                                                                    h.copy(orderIndex = newIndex)
                                                                }
                                                                habitViewModel.reorderHabits(reorderedHabits)
                                                                if (vibrationsEnabled) {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                                                }
                                                            }
                                                            draggedItemIndex = null
                                                            verticalDragOffset = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggedItemIndex = null
                                                            verticalDragOffset = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            draggedItemIndex?.let {
                                                                val newOffset = verticalDragOffset + dragAmount.y
                                                                val topBound = -it * itemHeightPx
                                                                val bottomBound = (habits.size - 1 - it) * itemHeightPx
                                                                verticalDragOffset = newOffset.coerceIn(topBound, bottomBound)

                                                                val draggedItem = habits[it]
                                                                val newIndexDown = (it + 1).coerceAtMost(habits.size - 1)
                                                                val newIndexUp = (it - 1).coerceAtLeast(0)

                                                                if (verticalDragOffset > itemHeightPx * 0.5f && it != newIndexDown) {
                                                                    habits = habits.toMutableList().apply {
                                                                        removeAt(it)
                                                                        add(newIndexDown, draggedItem)
                                                                    }
                                                                    verticalDragOffset -= itemHeightPx
                                                                    draggedItemIndex = newIndexDown
                                                                    if (vibrationsEnabled) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    }
                                                                } else if (verticalDragOffset < -itemHeightPx * 0.5f && it != newIndexUp) {
                                                                    habits = habits.toMutableList().apply {
                                                                        removeAt(it)
                                                                        add(newIndexUp, draggedItem)
                                                                    }
                                                                    verticalDragOffset += itemHeightPx
                                                                    draggedItemIndex = newIndexUp
                                                                    if (vibrationsEnabled) {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ReorderHabitItem(habit: Habit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = habitIconMap[habit.icon] ?: Icons.Default.Refresh // Fallback icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(habit.color).copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        Color(habit.color).copy(alpha = 0.25f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = habit.icon,
                    modifier = Modifier.size(40.dp),
                    tint = Color(habit.color).copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.size(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}