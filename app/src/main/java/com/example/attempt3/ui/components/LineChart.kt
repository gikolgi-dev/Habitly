package com.example.attempt3.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attempt3.data.MonthlyCompletion
import kotlin.math.roundToInt

@Composable
fun MonthlyLineChart(
    data: List<MonthlyCompletion>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showLabels: Boolean = true,
    vibrationsEnabled: Boolean = true,
    onPointSelected: (Float) -> Unit = {}
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val textPaint = remember(density, textColor) {
        Paint().apply {
            color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = density.run { 10.sp.toPx() }
        }
    }
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipTextColor = MaterialTheme.colorScheme.onPrimary
    val tooltipPaint = remember(density, tooltipTextColor) {
        Paint().apply {
            color = tooltipTextColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = density.run { 12.sp.toPx() }
        }
    }

    Canvas(
        modifier = modifier.pointerInput(data, vibrationsEnabled) {
            detectTapGestures { offset ->
                if (data.isEmpty()) return@detectTapGestures
                
                val horizontalPadding = 10.dp.toPx()
                val availableWidth = size.width - 2 * horizontalPadding
                
                val index = if (data.size > 1) {
                    val fraction = (offset.x - horizontalPadding) / availableWidth
                    (fraction * (data.size - 1)).roundToInt().coerceIn(0, data.size - 1)
                } else {
                    0
                }
                
                if (selectedIndex != index) {
                    selectedIndex = index
                    if (vibrationsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    val x = if (data.size > 1) {
                        horizontalPadding + index * (size.width - 2 * horizontalPadding) / (data.size - 1)
                    } else {
                        size.width.toFloat() / 2
                    }
                    onPointSelected(x)
                } else {
                    selectedIndex = null
                }
            }
        }
    ) {
        if (data.isEmpty()) return@Canvas

        val topPadding = 40.dp.toPx()
        val bottomPadding = if (showLabels) 20.dp.toPx() else 0f
        val usableHeight = size.height - topPadding - bottomPadding
        val xAxisY = size.height - bottomPadding
        val maxPercentage = 100f
        val horizontalPadding = 10.dp.toPx()
        
        val path = Path()
        val fillPath = Path()
        var previousPoint = Offset.Zero
        
        data.forEachIndexed { i, item ->
            val x = if (data.size > 1) {
                horizontalPadding + i * (size.width - 2 * horizontalPadding) / (data.size - 1)
            } else {
                size.width / 2
            }
            
            val y = xAxisY - (item.percentage / maxPercentage) * usableHeight
            
            val point = Offset(x, y)
            
            if (i == 0) {
                path.moveTo(point.x, point.y)
                fillPath.moveTo(point.x, xAxisY)
                fillPath.lineTo(point.x, point.y)
            } else {
                 val conX1 = (previousPoint.x + point.x) / 2f
                 val conY1 = previousPoint.y
                 val conX2 = (previousPoint.x + point.x) / 2f
                 val conY2 = point.y
                 
                 path.cubicTo(conX1, conY1, conX2, conY2, point.x, point.y)
                 fillPath.cubicTo(conX1, conY1, conX2, conY2, point.x, point.y)
            }
            previousPoint = point
        }
        
        fillPath.lineTo(previousPoint.x, xAxisY)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                endY = xAxisY
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        
        data.forEachIndexed { i, item ->
            val x = if (data.size > 1) {
                horizontalPadding + i * (size.width - 2 * horizontalPadding) / (data.size - 1)
            } else {
                size.width / 2
            }
            
            val y = xAxisY - (item.percentage / maxPercentage) * usableHeight
            
            // Draw outline for selected point
            if (i == selectedIndex) {
                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            
            if (i == selectedIndex) {
                 val label = "${item.percentage.toInt()}%"
                 val padding = 8.dp.toPx()
                 val textWidth = tooltipPaint.measureText(label)
                 val boxWidth = textWidth + padding * 2
                 val boxHeight = 28.dp.toPx()
                 
                 // Constrain boxX to prevent clipping at edges
                 val boxX = (x - boxWidth / 2).coerceIn(0f, size.width - boxWidth)
                 val boxY = (y - 8.dp.toPx() - boxHeight).coerceAtLeast(0f)
                 
                 drawRoundRect(
                     color = lineColor,
                     topLeft = Offset(boxX, boxY),
                     size = Size(boxWidth, boxHeight),
                     cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                 )
                 
                 drawContext.canvas.nativeCanvas.drawText(
                     label,
                     boxX + boxWidth / 2, // Center text within the constrained box
                     boxY + boxHeight / 2 - (tooltipPaint.ascent() + tooltipPaint.descent()) / 2,
                     tooltipPaint
                 )
            }
            
            if (showLabels) {
                drawContext.canvas.nativeCanvas.drawText(
                    item.monthLabel,
                    x,
                    size.height - 5.dp.toPx(), 
                    textPaint
                )
            }
        }
    }
}
