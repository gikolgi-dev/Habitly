package com.example.attempt3

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@Composable
fun HabitList(
    habitsWithCompletions: List<Pair<Habit, List<Completion>>>,
    isHabitCompleted: (Habit) -> Boolean,
    showCheckbox: Boolean,
    showMonthLabels: Boolean,
    dayOfWeekLabelsVisible: Boolean,
    dayOfWeekLabelsOnRight: Boolean,
    showAllDayOfWeekLabels: Boolean,
    borderContrast: Float,
    onComplete: (Habit) -> Unit,
    onClick: (Habit) -> Unit
) {
    LazyColumn {
        items(habitsWithCompletions, key = { it.first.id }) { (habit, completions) ->
            HabitItemCard(
                habit = habit,
                isCompleted = isHabitCompleted(habit),
                completions = completions,
                showCheckbox = showCheckbox,
                showMonthLabels = showMonthLabels,
                dayOfWeekLabelsVisible = dayOfWeekLabelsVisible,
                dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                showAllDayOfWeekLabels = showAllDayOfWeekLabels,
                borderContrast = borderContrast,
                onComplete = { onComplete(habit) },
                onClick = { onClick(habit) }
            )
        }
    }
}

@Composable
fun HabitTitleAndDescription(
    habit: Habit,
    isDetailView: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = habit.name,
            style = if (isDetailView) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (habit.description.isNotBlank()) {
            if (isDetailView) {
                Spacer(modifier = Modifier.height(4.dp))
                var isExpanded by remember { mutableStateOf(false) }
                var isOverflowing by remember { mutableStateOf(false) }

                val isClickable = (isOverflowing || isExpanded)
                Column(
                    modifier = Modifier
                        .animateContentSize(animationSpec = tween(durationMillis = 300)) // Animate the size change of the Column
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isClickable
                        ) { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        onTextLayout = { textLayoutResult ->
                            if (!isOverflowing && !isExpanded) {
                                isOverflowing = textLayoutResult.hasVisualOverflow
                            }
                        }
                    )
                    if (isClickable) {
                        Text(
                            text = if (isExpanded) "Read less" else "Read more",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun HabitItemCard(
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
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface

        ),
        border = BorderStroke(
            1.dp,
            Color.Gray.copy(alpha = borderContrast)
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display the habit's icon
                val icon = habitIconMap[habit.icon] ?: Icons.Default.Refresh // Fallback icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(habit.color).copy(alpha = 0.1f))
                        .border(
                            1.dp,
                            Color(habit.color).copy(alpha = borderContrast),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onComplete() },
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

                HabitTitleAndDescription(habit = habit, isDetailView = false, modifier = Modifier.weight(1f))
                
                Spacer(modifier = Modifier.size(16.dp))
                if (showCheckbox) { // Conditionally display the checkbox
                    val color = Color(habit.color)
                    val backgroundColor = if (isCompleted) color else color.copy(alpha = 0.1f)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .border(
                                1.dp,
                                if (isCompleted) color else color.copy(alpha = borderContrast),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onComplete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = if (isCompleted && color.isBright()) MaterialTheme.colorScheme.onPrimary
                            else if (isCompleted) Color.White
                            else color,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Row {
                        onUnarchive?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Default.Restore, contentDescription = "Unarchive")
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
            if (showCheckbox) {
                Spacer(modifier = Modifier.height(8.dp))
                Heatmap(
                    completions = completions,
                    habitColor = Color(habit.color),
                    modifier = Modifier.fillMaxWidth(),
                    isScrollable = false,
                    showMonthLabels = showMonthLabels,
                    dayOfWeekLabelsVisible = dayOfWeekLabelsVisible,
                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                    showAllDayOfWeekLabels = showAllDayOfWeekLabels
                )
            }
        }
    }
}