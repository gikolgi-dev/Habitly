package com.example.attempt3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationSelectors(
    notificationTime: String,
    selectedDays: Set<String>,
    onTimeClick: () -> Unit,
    onDaySelected: (String) -> Unit,
    isEnabled: Boolean,
    borderAlpha: Float
) {
    val alpha by animateFloatAsState(targetValue = if (isEnabled) 1f else 0.5f, label = "")
    Column(
        modifier = Modifier
            .alpha(alpha)
            .padding(horizontal = 0.dp)
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
                    fontSize = 148.sp, // Start large
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
