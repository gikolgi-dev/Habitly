package com.example.attempt3

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MorphingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
    borderColor: Color = Color.Gray,
    borderAlpha: Float = 0.25f,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    val buttonsizeanim by animateDpAsState(
        targetValue = buttonSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Button Size Animation"
    )
    Box(
        modifier = modifier
            .size(buttonsizeanim)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                borderColor.copy(alpha = borderAlpha),
                RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}
