@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui

import android.annotation.SuppressLint
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.example.attempt3.data.Database.Completion
import com.example.attempt3.data.Database.Habit
import com.example.attempt3.ui.colors.isBright

private val circleToSquareMorph = Morph(MaterialShapes.Circle, MaterialShapes.Square)

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
    showYearDivider: Boolean,
    borderContrast: Float,
    onComplete: () -> Unit,
    onClick: () -> Unit,
    onUnarchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp)
            .clip(RoundedCornerShape(8.dp))
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
                        .background(
                            Color(habit.color).copy(alpha = 0.1f),
                            MaterialShapes.Cookie12Sided.toShape()
                        )
                        .border(
                            1.dp,
                            Color(habit.color).copy(alpha = borderContrast),
                            MaterialShapes.Cookie12Sided.toShape()
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

                HabitTitleAndDescription(habit = habit, isDetailView = false, modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.size(16.dp))
                if (showCheckbox) { // Conditionally display the checkbox
                    val color = Color(habit.color)
                    val transition = updateTransition(targetState = isCompleted, label = "CompletionTransition")

                    val progress by transition.animateFloat(
                        label = "MorphProgress",
                        transitionSpec = { tween(300) }
                    ) { state ->
                        if (state) 1f else 0f
                    }
                    val backgroundColor by transition.animateColor(
                        label = "BackgroundColor",
                        transitionSpec = { tween(300) }
                    ) { state ->
                        if (state) color else color.copy(alpha = 0.1f)
                    }
                    val borderColor by transition.animateColor(
                        label = "BorderColor",
                        transitionSpec = { tween(300) }
                    ) { state ->
                        if (state) color else color.copy(alpha = borderContrast)
                    }
                    val iconTintColor by transition.animateColor(
                        label = "IconTintColor",
                        transitionSpec = { tween(300) }
                    ) { state ->
                        if (state) {
                            if (color.isBright()) MaterialTheme.colorScheme.onPrimary else Color.White
                        } else {
                            color
                        }
                    }

                    val morph = circleToSquareMorph
                    val shape = remember(progress) { MorphPolygonShape(morph, progress) }
                    val rotation by animateFloatAsState(if (isCompleted) 180f else 0f, label = "fab_icon_rotation")
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(shape)
                            .background(backgroundColor, shape)
                            .border(
                                1.dp,
                                borderColor,
                                shape
                            )
                            .clickable { onComplete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = if (isCompleted) "Completed" else "Complete",
                            tint = iconTintColor,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(rotation)
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
                Spacer(modifier = Modifier.height(if (showMonthLabels) 0.dp else 8.dp))
                Heatmap(
                    completions = completions,
                    habitColor = Color(habit.color),
                    modifier = Modifier.fillMaxWidth(),
                    isScrollable = false,
                    showMonthLabels = showMonthLabels,
                    dayOfWeekLabelsVisible = dayOfWeekLabelsVisible,
                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                    showAllDayOfWeekLabels = showAllDayOfWeekLabels,
                    showYearDivider = showYearDivider
                )
            }
        }
    }
}

class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection, // This parameter was missing
        density: Density                  // This parameter was missing
    ): Outline {
        // Below assumes that you haven't scaled the specific RoundedPolygons used
        // for the Morph yet, and that you want the shape to fit exactly into the
        // size of the Composable.
        val path = Path()
        morph.toPath(percentage, path.asAndroidPath())
        matrix.reset()
        matrix.scale(size.width, size.height)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
