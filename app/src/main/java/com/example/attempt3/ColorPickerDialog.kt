package com.example.attempt3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onColorChanged: (Color) -> Unit,
    onConfirm: (Color) -> Unit
) {
    val controller = rememberColorPickerController()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Choose a color") },
        text = {
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                controller = controller,
                onColorChanged = { colorEnvelope: ColorEnvelope ->
                    onColorChanged(colorEnvelope.color)
                },
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onConfirm(controller.selectedColor.value) }) {
                    Text("OK")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BorderedColorSelector(
    color: Color,
    selectorRadius: Dp = 12.dp,
    borderSize: Dp = 2.dp,
    borderColor: Color = Color.Black
) {
    Canvas(
        modifier = Modifier
            .size(selectorRadius * 2),
    ) {
        val strokeWidth: Float = borderSize.toPx()
        drawCircle(
            color = borderColor,
            radius = selectorRadius.toPx(),
        )
        drawCircle(
            color = color,
            radius = selectorRadius.toPx() - strokeWidth,
        )
    }
}