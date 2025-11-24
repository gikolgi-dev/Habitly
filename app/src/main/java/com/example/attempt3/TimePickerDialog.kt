package com.example.attempt3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    initialMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    borderContrast: Float,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
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
                .fillMaxWidth(0.9f)
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
                horizontalArrangement = Arrangement.End
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