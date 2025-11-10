package com.example.attempt3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.fromColorLong
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
    intervalUnit: String,
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
    scrollState: ScrollState
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Interval", style = MaterialTheme.typography.titleMedium)
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
                    supportingText = { if (completionsError != null) Text(completionsError) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        val items = listOf("Daily", "Weekly", "Monthly")
        val intervalValues = listOf("day", "week", "month")
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, item ->
                OutlinedButton(
                    onClick = { onIntervalUnitChanged(intervalValues[index]) },
                    shape = when (index) {
                        0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                        items.lastIndex -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                        else -> RoundedCornerShape(0.dp)
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = if (intervalUnit == intervalValues[index]) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .offset(x = (-1 * index).dp)
                ) {
                    Text(text = item)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))


        Text("Choose an Icon", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(habitIconMap.keys.toList()) { iconKey ->
                val isSelected = habitIconKey == iconKey
                val icon = habitIconMap[iconKey] ?: Icons.Default.Refresh // Fallback icon
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHabitIconKeyChanged(iconKey)
                        }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                                alpha = 0.3f
                            ),
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

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        Text("Choose a Color", style = MaterialTheme.typography.titleMedium,modifier = Modifier.padding(vertical = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
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
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
            item {
                val isFinalCustomColorSelected = customColor != null || (habitColor !in habitColors)
                val backgroundForCustomButton = livePreviewColor ?: (if (isFinalCustomColorSelected) customColor ?: habitColor else Color.Transparent)

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
                            val initialColor = customColor ?: habitColor.takeIf { it !in habitColors }
                            onShowColorPicker(true, initialColor)
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
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                        } else {
                            val iconTint = if (backgroundForCustomButton.isBright()) Color.Black else Color.White
                            Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = iconTint)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp)) // Spacer for the floating button
    }
}

@Composable
fun SaveHabitButton(
    buttonText: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
        )
    ) {
        Text(buttonText, color = MaterialTheme.colorScheme.onPrimary)
    }
}
