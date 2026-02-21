@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.example.attempt3.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BikeScooter
import androidx.compose.material.icons.filled.Book
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.settings.SettingsDataStore
import com.example.attempt3.ui.colors.habitColors
import com.example.attempt3.ui.colors.isBright
import androidx.compose.foundation.lazy.grid.items as gridItems
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
    onNotificationDaySelected: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)

    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }
    val dividerAlpha by animateFloatAsState(targetValue = if (isScrolled) 1f else 0f, label = "dividerAlpha")

    Text(title, style = MaterialTheme.typography.headlineLarge)
    HorizontalDivider(modifier =Modifier.fillMaxWidth(0.975f).padding(top = 10.dp).alpha(dividerAlpha), color = Color.Gray.copy(alpha = 0.2f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = cardColors(MaterialTheme.colorScheme.background)
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
            colors = cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Streak interval", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = intervalUnit != "day",
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                val items = listOf("Daily", "Weekly", "Monthly")
                val intervalValues = listOf("day", "week", "month")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = intervalUnit == intervalValues[index],
                            onClick = { onIntervalUnitChanged(intervalValues[index]) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size)
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Choose an Icon", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(habitIconMap.keys.toList()) { iconKey ->
                        val isSelected = habitIconKey == iconKey
                        val animatedBorderWidth by animateDpAsState(targetValue = if (isSelected) 2.dp else 1.dp, label = "borderWidth")
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                            label = "borderColor"
                        )
                        val animatedBackgroundColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            label = "backgroundColor"
                        )

                        val icon = habitIconMap[iconKey] ?: Icons.Default.Refresh // Fallback icon
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    animatedBackgroundColor
                                )
                                .clickable {
                                    if (!isSelected) {
                                        if (vibrationsEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onHabitIconKeyChanged(iconKey)
                                    }
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
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Choose a Color", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(habitColors) { color ->
                        val isSelected = (customColor ?: habitColor) == color
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .clickable {
                                    if (vibrationsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onHabitColorChanged(color)
                                    onClearCustomColor()
                                },

                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .animateContentSize()
                                   .fillMaxSize(if (isSelected) 0.6f else 0f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    item {
                        val isFinalCustomColorSelected = customColor != null || (habitColor !in habitColors)
                        val color = livePreviewColor ?: (if (isFinalCustomColorSelected) customColor ?: habitColor else Color.Transparent)
                        val backgroundForCustomButton = if (color == Color.White) Color.Transparent else color

                        val borderForCustomButton = if (isFinalCustomColorSelected || livePreviewColor != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(backgroundForCustomButton)
                                .border(
                                    width = 1.dp,
                                    color = borderForCustomButton,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (isFinalCustomColorSelected) {
                                        onClearCustomColor()
                                        onHabitColorChanged(habitColors.first())
                                        onShowColorPicker(true, habitColors.first())
                                    } else {
                                        val initialColor = customColor ?: habitColor.takeIf { it !in habitColors }
                                        onShowColorPicker(true, initialColor)
                                    }
                                 },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isFinalCustomColorSelected,
                                transitionSpec = {
                                    scaleIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                            scaleOut(animationSpec = tween(90))
                                }
                            ) { targetState ->
                                if (targetState) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.6f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                } else {
                                    val iconTint = if (backgroundForCustomButton.isBright()) Color.Black else Color.White
                                    Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = iconTint)
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .alpha(if (hasNotificationPermission) 1f else 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onNotificationsEnabledChanged(!notificationsEnabled) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Notifications", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = null,
                        enabled = hasNotificationPermission
                    )
                }
                AnimatedVisibility(visible = notificationsEnabled && hasNotificationPermission) {
                    NotificationTimeSelectors(
                        notificationTime = notificationTime ?: "Not Set",
                        selectedDays = notificationDays,
                        onTimeClick = onTimePickerClick,
                        onDaySelected = onNotificationDaySelected,
                        isEnabled = notificationsEnabled && hasNotificationPermission,
                        borderAlpha = 0.1f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
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
    val isKeyboardOpen by rememberUpdatedState(WindowInsets.isImeVisible)
    val haptic = LocalHapticFeedback.current
    val vibrationsEnabled by settingsDataStore.vibrations.collectAsState(initial = true)
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