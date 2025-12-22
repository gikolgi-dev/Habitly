package com.example.attempt3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
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
            .padding(horizontal = 20.dp)
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
            AutoSizeText(
                text = notificationTime,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 100.sp, // Start large
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Thin
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

@Composable
private fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null
) {
    var scaledTextStyle by remember { mutableStateOf(style) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier
            .drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            },
        style = scaledTextStyle,
        softWrap = false,
        maxLines = 1,
        textAlign = textAlign,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                scaledTextStyle.copy(
                    fontSize = scaledTextStyle.fontSize * 0.95f
                )
            } else {
                readyToDraw = true
            }
        }
    )
}
