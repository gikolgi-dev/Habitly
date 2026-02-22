@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.attempt3.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.attempt3.data.Database.Habit

@Composable
fun RotatingHabitIcon(
    habit: Habit,
    borderContrast: Float,
    modifier: Modifier = Modifier,
    shouldAnimate: Boolean = true
) {
    val rotation = if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        val animatedRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        animatedRotation
    } else {
        0f
    }

    val icon = habitIconMap[habit.icon] ?: Icons.Default.Refresh

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(Color(habit.color).copy(alpha = 0.1f))
                .border(
                    1.dp,
                    Color(habit.color).copy(alpha = borderContrast),
                    MaterialShapes.Cookie12Sided.toShape()
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = habit.icon,
            modifier = Modifier.size(40.dp),
            tint = Color(habit.color).copy(alpha = 0.85f)
        )
    }
}
