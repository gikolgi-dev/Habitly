/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)

package com.habitly.habitly.ui

import android.annotation.SuppressLint
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.getDailyTarget
import com.habitly.habitly.ui.colors.isBright
import com.habitly.habitly.ui.components.RotatingHabitIcon
import java.util.Calendar

val circleToSquareMorph = Morph(MaterialShapes.Circle, MaterialShapes.Square)

val precomputedMorphPaths = Array(101) { i ->
    val path = Path()
    circleToSquareMorph.toPath(i / 100f, path.asAndroidPath())
    path
}

@Composable
fun HabitTitleAndDescription(
    habit: Habit,
    isDetailView: Boolean,
    modifier: Modifier = Modifier,
    autoScrollText: Boolean = false,
    autoScrollTextElements: Set<String> = emptySet(),
    autoScrollTextScreens: Set<String> = emptySet()
) {
    val isScreenEnabled = if (isDetailView) "Detail Screen" in autoScrollTextScreens else "Main Screen" in autoScrollTextScreens
    val shouldAutoScrollTitle = autoScrollText && isScreenEnabled && "Title" in autoScrollTextElements
    val shouldAutoScrollDescription = autoScrollText && isScreenEnabled && "Description" in autoScrollTextElements
    Column(
        modifier = modifier
    ) {
        Text(
            text = habit.name,
            style = if (isDetailView) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = if (shouldAutoScrollTitle) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = if (shouldAutoScrollTitle) {
                Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    spacing = MarqueeSpacing(24.dp),
                    velocity = 30.dp
                )
            } else {
                Modifier
            }
        )
        if (habit.description.isNotBlank()) {
            if (isDetailView) {
                Spacer(modifier = Modifier.height(4.dp))
                if (shouldAutoScrollDescription) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            spacing = MarqueeSpacing(24.dp),
                            velocity = 30.dp
                        )
                    )
                } else {
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
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = if (shouldAutoScrollDescription) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = if (shouldAutoScrollDescription) {
                        Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            spacing = MarqueeSpacing(24.dp),
                            velocity = 30.dp
                        )
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}

@Composable
fun HabitCompletionButton(
    modifier: Modifier = Modifier,
    habit: Habit,
    isCompleted: Boolean,
    borderContrast: Float,
    disableAnimations: Boolean,
    disablePressAnimation: Boolean = false,
    onComplete: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    visible: Boolean = true,
    transitionProgressProvider: () -> Float = { 0f },
    theme: String = "system",
    detailBgColor: Color = Color.Unspecified,
    detailBorderColor: Color = Color.Unspecified,
    currentCompletions: Int = if (isCompleted) habit.getDailyTarget() else 0,
    onDecrement: (() -> Unit)? = null
) {
    val target = habit.getDailyTarget()
    val color = Color(habit.color)
    val targetProgress = if (isCompleted) 1f else 0f
    val progressState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(300),
        label = "MorphProgress"
    )

    val animatedCompletions = animateFloatAsState(
        targetValue = currentCompletions.toFloat(),
        animationSpec = tween(300),
        label = "animatedCompletions"
    )

    val animatedHabitColorState =
        animateColorAsState(targetValue = color, animationSpec = tween(300), label = "habitColor")


    val rotationAnimationSpec = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
    val rotationState = animateFloatAsState(
        if (isCompleted) 180f else 0f,
        label = "fab_icon_rotation",
        animationSpec = rotationAnimationSpec
    )

    var isPressed by remember { mutableStateOf(false) }
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && !disableAnimations && !disablePressAnimation) 0.85f else 1f,
        animationSpec = if (isPressed) spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val buttonModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElementWithCallerManagedVisibility(
                rememberSharedContentState(key = "button-${habit.id}"),
                visible = visible,
                boundsTransform = { _, _ -> tween(durationMillis = 300, easing = FastOutSlowInEasing) }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(buttonModifier)
            .size(64.dp)
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
            }
            .drawBehind {
                val p = progressState.value
                val tp = transitionProgressProvider()
                val morphPercentage = p + (1f - p) * tp
                val index = (morphPercentage * 100).roundToInt().coerceIn(0, 100)
                val cachedPath = precomputedMorphPaths[index]

                val bgAlpha = lerp(0.1f, 1f, p)
                val strokeAlpha = lerp(borderContrast, 1f, p)
                val currentColor = animatedHabitColorState.value

                val itemBgColor = currentColor.copy(alpha = bgAlpha)
                val itemStrokeColor = currentColor.copy(alpha = strokeAlpha)
                
                val targetBgColor = if (detailBgColor != Color.Unspecified) detailBgColor else itemBgColor
                val targetBorderColor = if (detailBorderColor != Color.Unspecified) detailBorderColor else itemStrokeColor
                
                val currentBgColor = lerp(itemBgColor, targetBgColor, tp)
                val currentStrokeColor = lerp(itemStrokeColor, targetBorderColor, tp)
                
                val innerSize = if (target > 1) (44.dp.toPx() + (size.width - 44.dp.toPx()) * morphPercentage) else size.width
                val offset = (size.width - innerSize) / 2f

                translate(left = offset, top = offset) {
                    scale(
                        scaleX = innerSize,
                        scaleY = innerSize,
                        pivot = Offset.Zero
                    ) {
                        drawPath(cachedPath, color = currentBgColor)
                        drawPath(cachedPath, color = currentStrokeColor, style = Stroke(width = 1.dp.toPx() / innerSize))
                    }
                }

                if (target > 1) {
                    val chunksAlpha = ((1f - p) * (1f - tp)).coerceIn(0f, 1f)
                    if (chunksAlpha > 0f) {
                        val count = target.coerceIn(2, 14)
                        val segmentAngle = 360f / count
                        val gapAngle = ((360f / count) * 0.28f).coerceIn(6f, 16f)
                        val sweepAngle = segmentAngle - gapAngle
                        val ringRadiusPx = (27.5.dp - 3.5.dp * p).toPx()
                        val strokeWidthPx = (3.dp * (1f - 0.4f * p)).toPx()
                        val comp = animatedCompletions.value
                        val trackAlpha = lerp(0.15f, 0.3f, borderContrast) * chunksAlpha
                        val filledAlpha = chunksAlpha

                        for (i in 0 until count) {
                            val startAngle = -90f + i * segmentAngle + (gapAngle / 2f)
                            drawArc(
                                color = currentColor.copy(alpha = trackAlpha),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - ringRadiusPx, center.y - ringRadiusPx),
                                size = Size(ringRadiusPx * 2f, ringRadiusPx * 2f),
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )

                            if (comp > i) {
                                val fillFraction = (comp - i).coerceIn(0f, 1f)
                                val fillSweep = sweepAngle * fillFraction
                                if (fillSweep > 0f) {
                                    drawArc(
                                        color = currentColor.copy(alpha = filledAlpha),
                                        startAngle = startAngle,
                                        sweepAngle = fillSweep,
                                        useCenter = false,
                                        topLeft = Offset(center.x - ringRadiusPx, center.y - ringRadiusPx),
                                        size = Size(ringRadiusPx * 2f, ringRadiusPx * 2f),
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(isCompleted, currentCompletions, disablePressAnimation) {
                detectTapGestures(
                    onPress = {
                        if (!disablePressAnimation) {
                            isPressed = true
                        }
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onLongPress = {
                        onDecrement?.invoke()
                    },
                    onTap = {
                        onComplete()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val animatedHabitColor = animatedHabitColorState.value
        val iconTintColor = if (isCompleted) {
            if (animatedHabitColor.isBright()) MaterialTheme.colorScheme.onPrimary else Color.White
        } else {
            animatedHabitColor
        }
        val tp = transitionProgressProvider()
        val baseIconSize = if (target > 1) 22.dp else 32.dp
        val targetIconSize = if (target > 1) baseIconSize + (32.dp - baseIconSize) * progressState.value else 32.dp
        val iconSize = targetIconSize + (20.dp - targetIconSize) * tp
        val delayStart = if (isCompleted) 0f else 0.4f
        val fadeProgress = if (tp < delayStart) 0f else (tp - delayStart) / (1f - delayStart)
        val iconAlpha = (1f - fadeProgress).coerceIn(0f, 1f)

        Crossfade(
            targetState = isCompleted,
            animationSpec = tween(durationMillis = 300),
            label = "IconCrossfade"
        ) { completed ->
            Icon(
                imageVector = if (completed) Icons.Default.Close else Icons.Default.Check,
                contentDescription = if (completed) "Completed" else "Complete",
                tint = iconTintColor,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        rotationZ = rotationState.value
                        alpha = if (isCompleted) 1f else iconAlpha
                    }
            )
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
    visibleDayLabels: Set<String>,
    dayOfWeekLabelsOnRight: Boolean,
    showYearDivider: Boolean,
    showYearLabels: Boolean,
    heatmapNotificationDot: Boolean,
    heatmapNotificationDotRange: String,
    showScrollBlur: Boolean = true,
    borderContrast: Float,
    useHabitColor: Boolean,
    disableAnimations: Boolean,
    onComplete: () -> Unit,
    onDecrement: (() -> Unit)? = null,
    onClick: () -> Unit,
    onUnarchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    heatmapScrollEnabled: Boolean = false,
    heatmapWeeks: Int = 0,
    heatmapInfinite: Boolean = false,
    isPreview: Boolean = false,
    currentDateMillis: Long = System.currentTimeMillis(),
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    visible: Boolean = true,
    transitionProgressProvider: () -> Float = { 0f },
    theme: String = "system",
    detailBgColor: Color = Color.Unspecified,
    animateTileChanges: Boolean = false,
    firstDayOfWeek: Int = Calendar.MONDAY,
    autoScrollText: Boolean = false,
    autoScrollTextElements: Set<String> = emptySet(),
    autoScrollTextScreens: Set<String> = emptySet()
) {
    val targetCardBackgroundColor = if (useHabitColor) {
        lerp(Color(habit.color), MaterialTheme.colorScheme.surfaceVariant, 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val targetCardBorderColor = if (useHabitColor) {
        lerp(
            Color(habit.color),
            lerp(Color(habit.color), MaterialTheme.colorScheme.surfaceVariant, 0.85f),
            1f - borderContrast
        )
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
    }

    val cardBackgroundColor by animateColorAsState(
        targetValue = targetCardBackgroundColor,
        animationSpec = tween(300),
        label = "cardBgColor"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = targetCardBorderColor,
        animationSpec = tween(300),
        label = "cardBorderColor"
    )
    val useDarkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val secondaryContainerAlpha = if (useDarkTheme) 0.25f else 1f
    val resolvedDetailBgColor = if (detailBgColor != Color.Unspecified) {
        detailBgColor
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = secondaryContainerAlpha)
    }
    val detailBorderColor = cardBorderColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp)
            .then(modifier)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            1.dp,
            cardBorderColor
        ),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RotatingHabitIcon(
                    habit = habit,
                    borderContrast = borderContrast,
                    shouldAnimate = !disableAnimations
                )

                Spacer(modifier = Modifier.size(16.dp))

                HabitTitleAndDescription(
                    habit = habit,
                    isDetailView = false,
                    modifier = Modifier.weight(1f),
                    autoScrollText = autoScrollText,
                    autoScrollTextElements = autoScrollTextElements,
                    autoScrollTextScreens = autoScrollTextScreens,
                )

                Spacer(modifier = Modifier.size(16.dp))
                if (showCheckbox) { // Conditionally display the checkbox
                    val effectiveCount = remember(habit, completions, currentDateMillis) {
                        com.habitly.habitly.data.Database.getEffectiveCompletionsForDay(habit, completions, currentDateMillis, currentDateMillis)
                    }
                    val isReallyCompleted = remember(habit, completions, currentDateMillis) {
                        com.habitly.habitly.data.Database.isDayCompleted(habit, completions, currentDateMillis, currentDateMillis)
                    }
                    HabitCompletionButton(
                        habit = habit,
                        isCompleted = isReallyCompleted,
                        currentCompletions = effectiveCount,
                        borderContrast = borderContrast,
                        disableAnimations = disableAnimations,
                        disablePressAnimation = isPreview,
                        onComplete = onComplete,
                        onDecrement = onDecrement,
                        sharedTransitionScope = sharedTransitionScope,
                        visible = visible,
                        transitionProgressProvider = transitionProgressProvider,
                        theme = theme,
                        detailBgColor = resolvedDetailBgColor,
                        detailBorderColor = detailBorderColor
                    )
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
                    isScrollable = heatmapScrollEnabled,
                    showMonthLabels = showMonthLabels,
                    visibleDayLabels = visibleDayLabels,
                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                    showYearDivider = showYearDivider,
                    showYearLabels = showYearLabels,
                    showScrollBlur = showScrollBlur,
                    minWeeks = heatmapWeeks,
                    isInfinite = heatmapInfinite,
                    currentDateMillis = currentDateMillis,
                    habit = habit,
                    showNotificationDot = heatmapNotificationDot,
                    notificationDotRange = heatmapNotificationDotRange,
                    animateTileChanges = animateTileChanges,
                    firstDayOfWeek = firstDayOfWeek
                )
            }
        }
    }
}

class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        morph.toPath(percentage, path.asAndroidPath())
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width, size.height)
        path.asAndroidPath().transform(matrix)
        return Outline.Generic(path)
    }
}
