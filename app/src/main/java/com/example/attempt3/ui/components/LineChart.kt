package com.example.attempt3.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attempt3.data.MonthlyCompletion

@Composable
fun MonthlyLineChart(
    data: List<MonthlyCompletion>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showLabels: Boolean = true
) {
    val density = LocalDensity.current
    val textPaint = remember(density, textColor) {
        Paint().apply {
            color = textColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = density.run { 10.sp.toPx() }
        }
    }

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val textSpace = if (showLabels) 20.dp.toPx() else 0f
        val graphHeight = size.height - textSpace
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
            
            val y = graphHeight - (item.percentage / maxPercentage) * graphHeight
            
            val point = Offset(x, y)
            
            if (i == 0) {
                path.moveTo(point.x, point.y)
                fillPath.moveTo(point.x, graphHeight)
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
        
        fillPath.lineTo(previousPoint.x, graphHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                endY = graphHeight
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
            
            val y = graphHeight - (item.percentage / maxPercentage) * graphHeight
            
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            
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
