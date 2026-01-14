package com.example.attempt3.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@Composable
fun NotificationTimeSelectors(
    notificationTime: String,
    selectedDays: Set<String>,
    onTimeClick: () -> Unit,
    onDaySelected: (String) -> Unit,
    isEnabled: Boolean,
    borderAlpha: Float,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(targetValue = if (isEnabled) 1f else 0.5f, label = "")
    Column(
        modifier = modifier
            .alpha(alpha)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    onClick = onTimeClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = notificationTime,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 140.sp, // Start large
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W600
                ),
                textAlign = TextAlign.Center
            )
        }
        DayOfWeekSelector(
            selectedDays = selectedDays,
            enabled = isEnabled,
            onDaySelected = onDaySelected,
            borderAlpha = borderAlpha,
            horizontalPadding = 0.dp
        )
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayOfWeekSelector(
    selectedDays: Set<String>,
    onDaySelected: (String) -> Unit,
    enabled: Boolean = true,
    borderAlpha: Float = 0.1f,
    horizontalPadding: Dp = 0.dp
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val dayValues = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayValues.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .border(
                        width = if(isSelected) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = borderAlpha),
                        shape = MaterialShapes.Square.toShape()
                    )
                    .clip(MaterialShapes.Square.toShape())
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable(enabled = enabled) { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = days[index],
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    initialMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    borderContrast: Float,
    is24Hour: Boolean
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    Color.Gray.copy(alpha = borderContrast),
                    RoundedCornerShape(8.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimePicker(state = timePickerState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                TextButton(onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }) {
                    Text("OK")
                }
            }
        }
    }
}