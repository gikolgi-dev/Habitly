/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.habitly.habitly.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BikeScooter
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoAdultContent
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.habitly.habitly.data.settings.SettingsDataStore
import com.habitly.habitly.ui.colors.habitColors
import com.habitly.habitly.ui.colors.isBright
import com.habitly.habitly.ui.components.NotificationTimeSelectors
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.sqrt

val habitIconMap = mapOf(
    "Book" to Icons.Default.Book,
    "DirectionsRun" to Icons.AutoMirrored.Default.DirectionsRun,
    "Email" to Icons.Default.Email,
    "Face" to Icons.Default.Face,
    "Favorite" to Icons.Default.Favorite,
    "FitnessCenter" to Icons.Default.FitnessCenter,
    "Healing" to Icons.Default.Healing,
    "Lightbulb" to Icons.Default.Lightbulb,
    "Restaurant" to Icons.Default.Restaurant,
    "School" to Icons.Default.School,
    "SelfImprovement" to Icons.Default.SelfImprovement,
    "ThumbUp" to Icons.Default.ThumbUp,
    "People" to Icons.Default.People,
    "Person" to Icons.Default.Person,
    "Public" to Icons.Default.Public,
    "Spa" to Icons.Default.Spa,
    "Palette" to Icons.Default.Palette,
    "MusicNote" to Icons.Default.MusicNote,
    "Hiking" to Icons.Default.Hiking,
    "BikeScooter" to Icons.Default.BikeScooter,
    "Edit" to Icons.Default.Edit,
    "Bedtime" to Icons.Default.Bedtime,
    "SmokeFree" to Icons.Default.SmokeFree,
    "Labs" to Icons.Default.Science, // Using Science for Labs
    "MonitorHeart" to Icons.Default.MonitorHeart, // For Cardio Load
    "Dining" to Icons.Default.Dining, // For Dine In
    "Exclamation" to Icons.Default.Error, // Using Error for Exclamation
    "Science" to Icons.Default.Science,
    "NoAdultContent" to Icons.Default.NoAdultContent,
    "Psychology" to Icons.Default.Psychology,
    "Relax" to Icons.Default.HotTub,
    "Heartbroken" to Icons.Default.SentimentVeryDissatisfied
)

const val defaultHabitIconKey = "SelfImprovement"

val habitIconRows = habitIconMap.keys.toList().chunked(8)
val habitColorRows = (0..habitColors.size).chunked(8)

private fun calculateGridDistance(index1: Int, index2: Int, columns: Int): Float {
    val r1 = index1 / columns
    val c1 = index1 % columns
    val r2 = index2 / columns
    val c2 = index2 % columns
    return sqrt(((r1 - r2) * (r1 - r2) + (c1 - c2) * (c1 - c2)).toDouble()).toFloat()
}

@Composable
private fun HabitIconItem(
    iconKey: String,
    isSelected: Boolean,
    index: Int,
    pressedIconIndexProvider: () -> Int?,
    vibrationsEnabled: Boolean,
    borderContrast: Float,
    onIconKeyChanged: (String) -> Unit,
    onPressChanged: (Int?) -> Unit,
    reduceGridReactions: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val animatedBorderWidth by animateDpAsState(targetValue = if (isSelected) 2.dp else 1.dp, label = "borderWidth")
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = borderContrast),
        label = "borderColor"
    )
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        label = "backgroundColor"
    )

    val pressedIconIndex = pressedIconIndexProvider()
    val distance = if (pressedIconIndex != null) calculateGridDistance(index, pressedIconIndex, 8) else 100f

    val scale by animateFloatAsState(
        targetValue = if (pressedIconIndex == index && !reduceGridReactions) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val maxDist = 4.5f
    val translationX by animateFloatAsState(
        targetValue = if (!reduceGridReactions && pressedIconIndex != null && pressedIconIndex != index && distance < maxDist) {
            val diff = (index % 8) - (pressedIconIndex % 8)
            val strength = (maxDist - distance) / maxDist
            diff * 12f * (strength * strength)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "translationX"
    )
    val translationY by animateFloatAsState(
        targetValue = if (!reduceGridReactions && pressedIconIndex != null && pressedIconIndex != index && distance < maxDist) {
            val diff = (index / 8) - (pressedIconIndex / 8)
            val strength = (maxDist - distance) / maxDist
            diff * 12f * (strength * strength)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "translationY"
    )

    val icon = habitIconMap[iconKey] ?: Icons.Default.Refresh
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
                this.translationY = translationY
            }
            .clip(RoundedCornerShape(6.dp))
            .background(animatedBackgroundColor)
            .pointerInput(iconKey) {
                detectTapGestures(
                    onPress = {
                        onPressChanged(index)
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        try {
                            awaitRelease()
                        } finally {
                            onPressChanged(null)
                        }
                    },
                    onTap = {
                        if (!isSelected) {
                            onIconKeyChanged(iconKey)
                        }
                    }
                )
            }
            .border(
                width = animatedBorderWidth,
                color = animatedBorderColor,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconKey,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HabitColorItem(
    color: Color,
    isSelected: Boolean,
    index: Int,
    pressedColorIndexProvider: () -> Int?,
    vibrationsEnabled: Boolean,
    onColorChanged: (Color) -> Unit,
    onClearCustomColor: () -> Unit,
    onPressChanged: (Int?) -> Unit,
    reduceGridReactions: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val pressedColorIndex = pressedColorIndexProvider()
    val distance = if (pressedColorIndex != null) calculateGridDistance(index, pressedColorIndex, 8) else 100f

    val scale by animateFloatAsState(
        targetValue = if (pressedColorIndex == index && !reduceGridReactions) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val maxDist = 4.5f
    val translationX by animateFloatAsState(
        targetValue = if (!reduceGridReactions && pressedColorIndex != null && pressedColorIndex != index && distance < maxDist) {
            val diff = (index % 8) - (pressedColorIndex % 8)
            val strength = (maxDist - distance) / maxDist
            diff * 12f * (strength * strength)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "translationX"
    )
    val translationY by animateFloatAsState(
        targetValue = if (!reduceGridReactions && pressedColorIndex != null && pressedColorIndex != index && distance < maxDist) {
            val diff = (index / 8) - (pressedColorIndex / 8)
            val strength = (maxDist - distance) / maxDist
            diff * 12f * (strength * strength)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "translationY"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
                this.translationY = translationY
            }
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .pointerInput(color) {
                detectTapGestures(
                    onPress = {
                        onPressChanged(index)
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        try {
                            awaitRelease()
                        } finally {
                            onPressChanged(null)
                        }
                    },
                    onTap = {
                        onColorChanged(color)
                        onClearCustomColor()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val selectScale by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
            label = "selectScale"
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.6f)
                .graphicsLayer {
                    scaleX = selectScale
                    scaleY = selectScale
                    alpha = if (selectScale > 0.01f) 1f else 0f
                }
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun CustomColorItem(
    index: Int,
    isFinalCustomColorSelected: Boolean,
    backgroundForCustomButton: Color,
    borderContrast: Float,
    vibrationsEnabled: Boolean,
    reduceGridReactions: Boolean,
    pressedColorIndexProvider: () -> Int?,
    onPressChanged: (Int?) -> Unit,
    onTap: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val pressedColorIndex = pressedColorIndexProvider()
    val distance = if (pressedColorIndex != null) calculateGridDistance(index, pressedColorIndex, 8) else 100f

    val scale by animateFloatAsState(
        targetValue = if (pressedColorIndex == index && !reduceGridReactions) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val maxDist = 4.5f
    val translationX by animateFloatAsState(
        targetValue = if (!reduceGridReactions && pressedColorIndex != null && pressedColorIndex != index && distance < maxDist) {
            val diff = (index % 8) - (pressedColorIndex % 8)
            val strength = (maxDist - distance) / maxDist
            diff * 12f * (strength * strength)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "translationX"
    )
    val translationY by animateFloatAsState(
        targetValue = if (!reduceGridReactions && pressedColorIndex != null && pressedColorIndex != index && distance < maxDist) {
            val diff = (index / 8) - (pressedColorIndex / 8)
            val strength = (maxDist - distance) / maxDist
            diff * 12f * (strength * strength)
        } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "translationY"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
                this.translationY = translationY
            }
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundForCustomButton)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (borderContrast > 0.1f) borderContrast else 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChanged(index)
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        try {
                            awaitRelease()
                        } finally {
                            onPressChanged(null)
                        }
                    },
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isFinalCustomColorSelected,
            transitionSpec = {
                scaleIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                        scaleOut(animationSpec = tween(90))
            },
            label = "CustomColorAnimatedContent"
        ) { targetState ->
            if (targetState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            } else {
                val iconTint = if (backgroundForCustomButton.isBright()) Color.Black else Color.White
                Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = iconTint)
            }
        }
    }
}

@Composable
private fun CloseButton(
    onClose: () -> Unit,
    vibrationsEnabled: Boolean,
    reduceGridReactions: Boolean,
    borderContrast: Float
) {
    val haptic = LocalHapticFeedback.current
    var isClosePressed by remember { mutableStateOf(false) }
    val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed && !reduceGridReactions) 0.85f else 1f,
        animationSpec = if (isClosePressed) spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )
    val useDarkTheme = isSystemInDarkTheme()
    val secondaryContainerAlpha = if (useDarkTheme) 0.25f else 1f

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = closeScale
                scaleY = closeScale
            }
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = secondaryContainerAlpha))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = borderContrast),
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        try {
                            awaitRelease()
                        } finally {
                        }
                    },
                    onTap = {
                        if (vibrationsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        }
                        onClose()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HabitSheetContent(
    title: String,
    habitName: String,
    onHabitNameChanged: (String) -> Unit,
    habitDescription: String,
    onHabitDescriptionChanged: (String) -> Unit,
    completionsPerInterval: String,
    onCompletionsPerIntervalChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    intervalUnit: String = "day",
    onIntervalUnitChanged: (String) -> Unit,
    completionsError: String?,
    habitIconKey: String,
    onHabitIconKeyChanged: (String) -> Unit,
    habitColor: Color,
    onHabitColorChanged: (Color) -> Unit,
    customColor: Color?,
    onShowColorPicker: (Boolean, Color?) -> Unit,
    onClearCustomColor: () -> Unit,
    livePreviewColor: Color?,
    scrollState: ScrollState,
    settingsDataStore: SettingsDataStore,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    notificationTime: String?,
    onTimePickerClick: () -> Unit,
    hasNotificationPermission: Boolean,
    notificationDays: Set<String>,
    onNotificationDaySelected: (String) -> Unit,
    headerModifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val borderContrast by settingsDataStore.borders.collectAsState(initial = 0.25f)
    val is24Hour by settingsDataStore.is24Hour.collectAsState(initial = false)
    val reduceMovement by settingsDataStore.reduceMovement.collectAsState(initial = false)
    val reduceMovementTargets by settingsDataStore.reduceMovementTargets.collectAsState(initial = emptySet())
    val reduceGridReactions by remember { derivedStateOf { reduceMovement && "Grid Reactions" in reduceMovementTargets } }

    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val dividerAlpha by animateFloatAsState(targetValue = if (isScrolled) 1f else 0f, label = "dividerAlpha")

    val pressedIconIndexState = remember { mutableStateOf<Int?>(null) }
    val pressedColorIndexState = remember { mutableStateOf<Int?>(null) }

    var isInitial by remember { mutableStateOf(true) }

    // Scroll to bottom when notifications are enabled to follow expansion
    LaunchedEffect(notificationsEnabled, hasNotificationPermission) {
        if (isInitial) {
            isInitial = false
            return@LaunchedEffect
        }
        if (notificationsEnabled && hasNotificationPermission) {
            withTimeoutOrNull(600) {
                snapshotFlow { scrollState.maxValue }.collect { max ->
                    scrollState.scrollTo(max)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = headerModifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    CloseButton(
                        onClose = onClose,
                        vibrationsEnabled = vibrationsEnabled,
                        reduceGridReactions = reduceGridReactions,
                        borderContrast = borderContrast
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp).alpha(dividerAlpha), color = Color.Gray.copy(alpha = 0.2f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .verticalScroll(scrollState, enabled = scrollState.maxValue > 0),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp,vertical = 4.dp)) {
                    OutlinedTextField(
                        value = habitName,
                        onValueChange = onHabitNameChanged,
                        label = { Text("Habit Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = habitDescription,
                        onValueChange = onHabitDescriptionChanged,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Streak interval", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    val items = listOf("Daily", "Weekly", "Monthly")
                    val intervalValues = listOf("day", "week", "month")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        items.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = intervalUnit == intervalValues[index],
                                onClick = { onIntervalUnitChanged(intervalValues[index]) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                    activeBorderColor = MaterialTheme.colorScheme.primary,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = borderContrast)
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = intervalUnit != "day",
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(400, easing = EaseInOutQuint)),
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(400, easing = EaseInOutQuint))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = completionsPerInterval,
                                onValueChange = onCompletionsPerIntervalChanged,
                                label = { Text("Completions per ${intervalUnit.replaceFirstChar { it.uppercase() }}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = completionsError != null,
                                singleLine = true,
                                supportingText = { if (completionsError != null) Text(completionsError) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Choose an Icon", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Optimized Non-Lazy Grid
                    val iconRows = habitIconRows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        iconRows.forEachIndexed { rowIndex, rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIcons.forEachIndexed { colIndex, iconKey ->
                                    val index = rowIndex * 8 + colIndex
                                    Box(modifier = Modifier.weight(1f)) {
                                        HabitIconItem(
                                            iconKey = iconKey,
                                            isSelected = habitIconKey == iconKey,
                                            index = index,
                                            pressedIconIndexProvider = { pressedIconIndexState.value },
                                            vibrationsEnabled = vibrationsEnabled,
                                            borderContrast = borderContrast,
                                            onIconKeyChanged = onHabitIconKeyChanged,
                                            onPressChanged = { pressedIconIndexState.value = it },
                                            reduceGridReactions = reduceGridReactions
                                        )
                                    }
                                }
                                repeat(8 - rowIcons.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Choose a Color", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Optimized Non-Lazy Grid
                    val colorRows = habitColorRows

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorRows.forEachIndexed { rowIndex, rowIndices ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowIndices.forEach { index ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (index < habitColors.size) {
                                            val color = habitColors[index]
                                            HabitColorItem(
                                                color = color,
                                                isSelected = (customColor ?: habitColor) == color,
                                                index = index,
                                                pressedColorIndexProvider = { pressedColorIndexState.value },
                                                vibrationsEnabled = vibrationsEnabled,
                                                onColorChanged = onHabitColorChanged,
                                                onClearCustomColor = onClearCustomColor,
                                                onPressChanged = { pressedColorIndexState.value = it },
                                                reduceGridReactions = reduceGridReactions
                                            )
                                        } else {
                                            // Custom Color Button
                                            val isFinalCustomColorSelected = customColor != null || (habitColor !in habitColors)
                                            val color = livePreviewColor ?: (if (isFinalCustomColorSelected) customColor ?: habitColor else Color.Transparent)
                                            val backgroundForCustomButton = if (color == Color.White) Color.Transparent else color

                                            CustomColorItem(
                                                index = index,
                                                isFinalCustomColorSelected = isFinalCustomColorSelected,
                                                backgroundForCustomButton = backgroundForCustomButton,
                                                borderContrast = borderContrast,
                                                vibrationsEnabled = vibrationsEnabled,
                                                reduceGridReactions = reduceGridReactions,
                                                pressedColorIndexProvider = { pressedColorIndexState.value },
                                                onPressChanged = { pressedColorIndexState.value = it },
                                                onTap = {
                                                    if (isFinalCustomColorSelected) {
                                                        onClearCustomColor()
                                                        onHabitColorChanged(habitColors.first())
                                                        onShowColorPicker(true, habitColors.first())
                                                    } else {
                                                        val initialColor = customColor ?: habitColor.takeIf { it !in habitColors }
                                                        onShowColorPicker(true, initialColor)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                repeat(8 - rowIndices.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (hasNotificationPermission) 1f else 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = {
                                    onNotificationsEnabledChanged(!notificationsEnabled)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable Notifications", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = null,
                            enabled = hasNotificationPermission
                        )
                    }
                    AnimatedVisibility(visible = notificationsEnabled && hasNotificationPermission,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(400, easing = EaseInOutQuint)),
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(400, easing = EaseInOutQuint))
                    ) {
                        Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                            NotificationTimeSelectors(
                                notificationTime = notificationTime ?: "Not Set",
                                selectedDays = notificationDays,
                                onTimeClick = onTimePickerClick,
                                onDaySelected = onNotificationDaySelected,
                                isEnabled = notificationsEnabled && hasNotificationPermission,
                                borderAlpha = borderContrast,
                                is24Hour = is24Hour,
                                vibrationsEnabled = vibrationsEnabled
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SaveHabitButton(
    buttonText: String,
    isEnabled: Boolean,
    settingsDataStore: SettingsDataStore,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
    val isKeyboardOpen by rememberUpdatedState(WindowInsets.isImeVisible)
    val padding by animateDpAsState(targetValue = if (isKeyboardOpen) 8.dp else 20.dp, label = "buttonPadding")

    Button(
        onClick = {
            if (vibrationsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        },
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .imePadding(),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 24.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color.Gray,
            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
        )
    ) {
        AnimatedContent(
            targetState = buttonText,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
            },
            label = "buttonTextAnimation"
        ) { text ->
            Text(text, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
