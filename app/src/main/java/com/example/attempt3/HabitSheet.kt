package com.example.attempt3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HabitSheet(
    habit: Habit?,
    habitDao: HabitDao,
    onDismiss: () -> Unit,
    onShowColorPicker: (Boolean, Color?) -> Unit,
    onClearCustomColor: () -> Unit,
    customColor: Color?,
    livePreviewColor: Color?,
    habits: List<HabitWithCompletions>
) {
    val isEditMode = habit != null
    val title = if (isEditMode) "Edit Habit" else "Add New Habit"
    val buttonText = if (isEditMode) "Save Changes" else "Save"

    var habitName by remember { mutableStateOf(habit?.name ?: "") }
    var habitDescription by remember { mutableStateOf(habit?.description ?: "") }
    var habitColor by remember { mutableStateOf(habit?.let { Color(it.color) } ?: habitColors.first()) }
    var habitIconKey by remember { mutableStateOf(habit?.icon ?: defaultHabitIconKey) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var completionsPerInterval by remember { mutableStateOf(habit?.completionsPerInterval?.toString() ?: "1") }
    var intervalUnit by remember { mutableStateOf(habit?.intervalUnit ?: "day") }
    var completionsError by remember { mutableStateOf<String?>(null) }

    fun validate(completionsText: String) {
        if (intervalUnit == "day") {
            completionsError = null
            return
        }
        val completions = completionsText.toIntOrNull()
        if (completions == null) {
            completionsError = "Must be a number"
        } else if (completions <= 0) {
            completionsError = "Must be > 0"
        } else {
            completionsError = null
        }
    }

    LaunchedEffect(intervalUnit) {
        if (intervalUnit == "day") {
            completionsPerInterval = "1"
        }
        validate(completionsPerInterval)
    }

    LaunchedEffect(completionsPerInterval, intervalUnit) {
        validate(completionsPerInterval)
    }

    LaunchedEffect(habit) {
        if (habit != null) {
            habitName = habit.name
            habitDescription = habit.description
            habitColor = Color(habit.color)
            habitIconKey = habit.icon
            completionsPerInterval = habit.completionsPerInterval.toString()
            intervalUnit = habit.intervalUnit
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth(0.15f)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = habitName,
                onValueChange = { habitName = it },
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
                onValueChange = { habitDescription = it },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { intervalUnit = "day" }, modifier = if (intervalUnit == "day") Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier) {
                    Text("Daily")
                }
                TextButton(onClick = { intervalUnit = "week" }, modifier = if (intervalUnit == "week") Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier) {
                    Text("Weekly")
                }
                TextButton(onClick = { intervalUnit = "month" }, modifier = if (intervalUnit == "month") Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier) {
                    Text("Monthly")
                }
            }
            OutlinedTextField(
                value = completionsPerInterval,
                onValueChange = { completionsPerInterval = it },
                label = { Text("Completions per ${intervalUnit.replaceFirstChar { it.uppercase() }}")},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = completionsError != null,
                supportingText = { if (completionsError != null) Text(completionsError!!) },
                enabled = intervalUnit != "day",
                modifier = Modifier.fillMaxWidth()
            )
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
                                habitIconKey = iconKey
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
                                habitColor = color
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val trimmedName = habitName.trim()
                    if (trimmedName.isNotBlank()) {
                        scope.launch {
                            if (isEditMode) {
                                val updatedHabit = habit.copy(
                                    name = trimmedName,
                                    description = habitDescription,
                                    icon = habitIconKey,
                                    color = (customColor ?: habitColor).toArgb(),
                                    completionsPerInterval = completionsPerInterval.toIntOrNull() ?: 1,
                                    intervalUnit = intervalUnit
                                )
                                habitDao.updateHabit(updatedHabit)
                            } else {
                                val newHabit = Habit(
                                    id = UUID
                                        .randomUUID()
                                        .toString(),
                                    name = trimmedName,
                                    description = habitDescription,
                                    icon = habitIconKey,
                                    color = (customColor ?: habitColor).toArgb(),
                                    archived = false,
                                    orderIndex = habits.size,
                                    createdAt = System
                                        .currentTimeMillis()
                                        .toString(),
                                    isInverse = false,
                                    emoji = null,
                                    completionsPerInterval = completionsPerInterval.toIntOrNull() ?: 1,
                                    intervalUnit = intervalUnit
                                )
                                habitDao.insertHabit(newHabit)
                            }
                            onDismiss()
                        }
                    }
                },
                enabled = habitName.trim().isNotBlank() && completionsError == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}