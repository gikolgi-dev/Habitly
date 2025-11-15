
package com.example.attempt3

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AnimatedHabitItemCard(
    habit: Habit,
    isCompleted: Boolean,
    completions: List<Completion>,
    showCheckbox: Boolean,
    showMonthLabels: Boolean,
    dayOfWeekLabelsVisible: Boolean,
    dayOfWeekLabelsOnRight: Boolean,
    showAllDayOfWeekLabels: Boolean,
    borderContrast: Float,
    onComplete: () -> Unit,
    onClick: () -> Unit,
    onUnarchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    HabitItemCard(
        habit = habit,
        isCompleted = isCompleted,
        completions = completions,
        showCheckbox = showCheckbox,
        showMonthLabels = showMonthLabels,
        dayOfWeekLabelsVisible = dayOfWeekLabelsVisible,
        dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
        showAllDayOfWeekLabels = showAllDayOfWeekLabels,
        borderContrast = borderContrast,
        onComplete = onComplete,
        onClick = onClick,
        onUnarchive = onUnarchive,
        onDelete = onDelete,
        modifier = Modifier.sharedElement(
            rememberSharedContentState(key = "card-${habit.id}"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    )
}
