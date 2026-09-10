/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.habitly.habitly.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.habitly.habitly.ui.circleToSquareMorph
import com.habitly.habitly.ui.MorphPolygonShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.graphics.shapes.toPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.Database.Completion
import com.habitly.habitly.data.Database.Habit
import com.habitly.habitly.data.Database.HabitViewModel
import androidx.compose.ui.res.painterResource
import com.habitly.habitly.R
import com.habitly.habitly.data.Database.isQuit
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import com.habitly.habitly.data.Database.HabitWithCompletions
import com.habitly.habitly.notifications.NotificationScheduler
import com.habitly.habitly.ui.components.RotatingHabitIcon
import java.util.Calendar
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.HabitDetailScreen(
    habitWithCompletions: HabitWithCompletions,
    viewModel: HabitViewModel,
    isArchivedView: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismiss: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onShowStatistics: (Habit) -> Unit,
    borderContrast: Float,
    showScrollBlur: Boolean,
    showYearLabels: Boolean,
    heatmapNotificationDot: Boolean,
    heatmapNotificationDotRange: String,
    heatmapNotificationDotDetailOnly: Boolean,
    showYearDivider: Boolean,
    vibrationsEnabled: Boolean,
    showMonthLabels: Boolean,
    dayOfWeekLabelsOnRight: Boolean,
    heatmapVisibleDays: Set<String>,
    disableAnimations: Boolean,
    useHabitColor: Boolean,
    theme: String,
    heatmapWeeks: Int = 0,
    heatmapInfinite: Boolean = false,
    currentDateMillis: Long = System.currentTimeMillis(),
    isEditSheetOpen: Boolean = false,
    transitionProgressProvider: () -> Float = { 1f }
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val notificationScheduler = remember { NotificationScheduler(context) }
    var showDeleteConfirmation by remember { mutableStateOf(false) } // State for delete confirmation dialog
    val habit = habitWithCompletions.habit
    val completions = habitWithCompletions.completions
    val animatedColorState =
        animateColorAsState(targetValue = Color(habit.color), animationSpec = tween(durationMillis = 500))
    val isSharedVisible = !isEditSheetOpen && animatedVisibilityScope.transition.targetState == androidx.compose.animation.EnterExitState.Visible

    val notificationDotAlpha by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300, easing = FastOutSlowInEasing) },
        label = "notificationDotAlpha"
    ) { state ->
        if (heatmapNotificationDotDetailOnly) {
            if (state == androidx.compose.animation.EnterExitState.Visible) 1f else 0f
        } else {
            1f
        }
    }


    val streak =
        remember(habit, completions, currentDateMillis) { calculateStreak(habit, completions, currentDateMillis) }

    val useDarkTheme = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val secondaryContainerAlpha = if (useDarkTheme) 0.25f else 1f

    val cardBackgroundColor = if (useHabitColor) {
        lerp(Color(habit.color), MaterialTheme.colorScheme.surfaceVariant, 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val cardBorderColor = if (useHabitColor) {
        lerp(
            Color(habit.color),
            lerp(Color(habit.color), MaterialTheme.colorScheme.surfaceVariant, 0.85f),
            1f - borderContrast
        )
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Are you sure?") },
            text = { Text("Are you sure you want to delete this habit? This action cannot be undone.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.deleteHabit(habit)
                            notificationScheduler.cancelNotification(habit)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = null,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val animatedPaddingTopState = animateDpAsState(
        targetValue = if (isEditSheetOpen) 60.dp else 160.dp,
        animationSpec = if (isEditSheetOpen) {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 250, easing = FastOutSlowInEasing)
        },
        label = "paddingTop"
    )
    val animatedScaleState = animateFloatAsState(
        targetValue = if (isEditSheetOpen) 0.9f else 1f,
        animationSpec = if (isEditSheetOpen) {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 300, delayMillis = 200, easing = LinearOutSlowInEasing)
        },
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!isEditSheetOpen) onDismiss() }
            .offset { IntOffset(0, animatedPaddingTopState.value.roundToPx()) }
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = animatedScaleState.value
                    scaleY = animatedScaleState.value
                }
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .zIndex(1f)
                    .sharedElementWithCallerManagedVisibility(
                        rememberSharedContentState(key = "card-${habit.id}"),
                        visible = isSharedVisible,
                        boundsTransform = { _, _ -> tween(durationMillis = 300, easing = FastOutSlowInEasing) }
                    )
                    .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {/* Prevents click from dismissing the dialog if clicked inside the card */ },
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = cardBackgroundColor
            ),
            border = BorderStroke(
                1.dp,
                cardBorderColor
            ),
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState, enabled = scrollState.maxValue > 0)
                    .padding(10.dp)
            ) {
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
                    HabitTitleAndDescription(habit = habit, isDetailView = true, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center

                    )
                    {
                        var isClosePressed by remember { mutableStateOf(false) }
                        val closeScale by animateFloatAsState(
                            targetValue = if (isClosePressed && !disableAnimations) 0.85f else 1f,
                            animationSpec = if (isClosePressed) spring(
                                dampingRatio = 0.5f,
                                stiffness = Spring.StiffnessLow
                            ) else spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "button_scale"
                        )
                        val todayStart = remember(currentDateMillis) {
                            Calendar.getInstance().apply {
                                timeInMillis = currentDateMillis
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        val todayEnd = remember(currentDateMillis) {
                            Calendar.getInstance().apply {
                                timeInMillis = currentDateMillis
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                        }
                        val isCompletedToday = remember(habit, completions, todayStart, currentDateMillis) {
                            com.habitly.habitly.data.Database.isDayCompleted(habit, completions, todayStart, currentDateMillis)
                        }
                        
                        val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

                        Box(
                            modifier = Modifier
                                .sharedElementWithCallerManagedVisibility(
                                    rememberSharedContentState(key = "button-${habit.id}"),
                                    visible = isSharedVisible,
                                    boundsTransform = { _, _ -> tween(durationMillis = 300, easing = FastOutSlowInEasing) }
                                )
                                .graphicsLayer {
                                    scaleX = closeScale
                                    scaleY = closeScale
                                }
                                .size(48.dp)
                                .drawBehind {
                                    val startPercentage = if (isCompletedToday) 1f else 0f
                                    val morphPercentage = startPercentage + (1f - startPercentage) * transitionProgressProvider()
                                    val index = (morphPercentage * 100).roundToInt().coerceIn(0, 100)
                                    val cachedPath = com.habitly.habitly.ui.precomputedMorphPaths[index]

                                    val p = if (isCompletedToday) 1f else 0f
                                    val tp = transitionProgressProvider()
                                    val currentColor = animatedColorState.value

                                    val bgAlpha = 0.1f + (1f - 0.1f) * p
                                    val strokeAlpha = borderContrast + (1f - borderContrast) * p

                                    val itemBgColor = currentColor.copy(alpha = bgAlpha)
                                    val itemStrokeColor = currentColor.copy(alpha = strokeAlpha)

                                    val targetBgColor = secondaryContainerColor.copy(alpha = secondaryContainerAlpha)
                                    val targetBorderColor = cardBorderColor

                                    val currentBgColor = lerp(itemBgColor, targetBgColor, tp)
                                    val currentStrokeColor = lerp(itemStrokeColor, targetBorderColor, tp)

                                    scale(
                                        scaleX = size.width,
                                        scaleY = size.height,
                                        pivot = androidx.compose.ui.geometry.Offset.Zero
                                    ) {
                                        drawPath(cachedPath, color = currentBgColor)
                                        drawPath(cachedPath, color = currentStrokeColor, style = Stroke(width = 1.dp.toPx() / size.width))
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isClosePressed = true
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            try {
                                                awaitRelease()
                                            } finally {
                                                isClosePressed = false
                                            }
                                        },
                                        onTap = {
                                            if (vibrationsEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                            }
                                            onDismiss()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val tp = transitionProgressProvider()
                            val iconSize = 32.dp + (20.dp - 32.dp) * tp
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier
                                    .size(iconSize)
                                    .graphicsLayer {
                                        rotationZ = if (isCompletedToday) 180f else 0f
                                        alpha = if (isCompletedToday) 1f else tp
                                    },
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }


                Heatmap(
                    completions = completions,
                    habitColor = animatedColorState.value,
                    modifier = Modifier.fillMaxWidth().padding(top = if (showMonthLabels) 0.dp else 8.dp),
                    showMonthLabels = showMonthLabels,
                    visibleDayLabels = heatmapVisibleDays,
                    dayOfWeekLabelsOnRight = dayOfWeekLabelsOnRight,
                    showYearDivider = showYearDivider,
                    showYearLabels = showYearLabels,
                    showScrollBlur = showScrollBlur,
                    minWeeks = maxOf(30, heatmapWeeks), // Display a bit more heatmap columns even if they are empty
                    isInfinite = heatmapInfinite,
                    currentDateMillis = currentDateMillis,
                    habit = habit,
                    showNotificationDot = heatmapNotificationDot,
                    notificationDotRange = heatmapNotificationDotRange,
                    notificationDotAlpha = notificationDotAlpha
                )

                //Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val notificationsSet = habit.notificationsEnabled && habit.notificationTime != null
                    Box(
                        modifier = Modifier
                            .height(35.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = secondaryContainerAlpha))
                            .border(
                                1.dp,
                                cardBorderColor,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (notificationsSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Notification Time",
                                modifier = Modifier.size(20.dp),
                                tint = if (notificationsSet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.38f
                                )
                            )
                            if (notificationsSet) {
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = habit.notificationTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    val intervalText = when (habit.intervalUnit) {
                        "day" -> if (habit.completionsPerInterval > 1) "${habit.completionsPerInterval}/Day" else "Daily"
                        else -> "${habit.completionsPerInterval}/${habit.intervalUnit.replaceFirstChar { it.uppercase() }}"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .height(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = secondaryContainerAlpha))
                                .border(
                                    1.dp,
                                    cardBorderColor,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (habit.isQuit) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Quit Habit",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = "Quit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_sentiment_calm),
                                        contentDescription = "Build Habit",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = "Build",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(35.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFC9920).copy(alpha = 0.4f))
                                .border(
                                    1.dp,
                                    Color(0xFFFC9920).copy(alpha = borderContrast * 2),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFC9920)
                                )
                                Spacer(modifier = Modifier.size(2.dp))
                                AnimatedContent(
                                    targetState = streak,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                                        } else {
                                            (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                                        }
                                    },
                                    label = "streak_wheel_animation"
                                ) { targetStreak ->
                                    Text("$targetStreak", color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.size(6.dp))
                                VerticalDivider(
                                    thickness = 1.dp,
                                    color = Color(0xFFFC9920).copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxHeight(0.75f)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = intervalText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                MonthCalendar(
                    //modifier = Modifier.padding(horizontal = 8.dp),
                    completions = completions,
                    habitColor = animatedColorState.value,
                    vibrationsEnabled = vibrationsEnabled,
                    reduceGridReactions = disableAnimations,
                    currentDateMillis = currentDateMillis,
                    habit = habit,
                    onDateClick = { date, isCompleted ->
                        viewModel.toggleCompletion(habit, date)
                    }
                )
            }
        }
            val buttonsProgress = transitionProgressProvider()
            val buttonSlideProgress = ((1f - buttonsProgress) / 0.5f).coerceIn(0f, 1f)
            val buttonAlpha = ((buttonsProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .zIndex(0f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        // Slide up under the card and fade in/out
                        translationY = -(64.dp.toPx()) * buttonSlideProgress
                        alpha = buttonAlpha
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                Button(
                    onClick = {
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onEditHabit(habit)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBackgroundColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Habit",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Statistics button
                Button(
                    onClick = {
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onShowStatistics(habit)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBackgroundColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Show Statistics",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Archive/Unarchive button
                val archiveIcon = if (isArchivedView) Icons.Default.Restore else Icons.Default.Archive
                val archiveContentDescription = if (isArchivedView) "Un-archive Habit" else "Archive Habit"
                Button(
                    onClick = {
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val updatedHabit = habit.copy(archived = !isArchivedView)
                        viewModel.updateHabit(updatedHabit)
                        notificationScheduler.scheduleNotification(updatedHabit)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBackgroundColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Icon(
                        imageVector = archiveIcon,
                        contentDescription = archiveContentDescription,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun calculateStreak(habit: Habit, completions: List<Completion>, currentDateMillis: Long): Int {
    return com.habitly.habitly.data.calculateCurrentStreak(habit, completions, currentDateMillis)
}
