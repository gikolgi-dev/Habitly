/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
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
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(targetValue = if (isEnabled) 1f else 0.5f, label = "")

    @Suppress("DEPRECATION")
    val timeTextStyle = MaterialTheme.typography.displayLarge.copy(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.SansSerif,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

    Column(
        modifier = modifier
            .alpha(alpha)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    onClick = onTimeClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (notificationTime == "Not Set") {
                Text(
                    text = "Not Set",
                    modifier = Modifier.fillMaxWidth(),
                    style = timeTextStyle.copy(
                        fontSize = 80.sp,
                        lineHeight = 80.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                val parts = notificationTime.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: 0
                    val minute = parts[1].toIntOrNull() ?: 0

                    if (is24Hour) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(java.util.Locale.getDefault(), "%02d", hour),
                                style = timeTextStyle.copy(
                                    fontSize = 140.sp,
                                    lineHeight = 140.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = String.format(java.util.Locale.getDefault(), "%02d", minute),
                                style = timeTextStyle.copy(
                                    fontSize = 140.sp,
                                    lineHeight = 140.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        val amPm = if (hour < 12) "AM" else "PM"
                        val displayHour = when {
                            hour == 0 -> 12
                            hour > 12 -> hour - 12
                            else -> hour
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(java.util.Locale.getDefault(), "%02d", displayHour),
                                style = timeTextStyle.copy(
                                    fontSize = 140.sp,
                                    lineHeight = 140.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.getDefault(), "%02d", minute),
                                    style = timeTextStyle.copy(
                                        fontSize = 90.sp,
                                        lineHeight = 90.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = amPm,
                                    style = timeTextStyle.copy(
                                        fontSize = 36.sp,
                                        lineHeight = 36.sp
                                    ),
                                    modifier = Modifier.offset(y = (-4).dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = notificationTime,
                        modifier = Modifier.fillMaxWidth(),
                        style = timeTextStyle.copy(
                            fontSize = 80.sp,
                            lineHeight = 80.sp
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onConfirm(timePickerState.hour, timePickerState.minute)
                    },
                    shape = CircleShape
                ) {
                    Text("OK")
                }
            }
        }
    }
}