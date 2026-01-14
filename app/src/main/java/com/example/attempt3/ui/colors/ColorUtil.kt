package com.example.attempt3.ui.colors

import androidx.compose.ui.graphics.Color

fun Color.isBright(): Boolean {
    val red = this.red * 255
    val green = this.green * 255
    val blue = this.blue * 255
    return (red * 0.299 + green * 0.587 + blue * 0.114) > 186
}
