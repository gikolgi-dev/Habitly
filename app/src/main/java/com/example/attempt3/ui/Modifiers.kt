/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.example.attempt3.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Adds an animated fade-out effect to the specified edges of a scrollable component.
 * Supports horizontal fade-out on the left and right edges based on [ScrollableState].
 * The gradients fade in and out as the user scrolls.
 */
fun Modifier.fadingEdge(
    scrollableState: ScrollableState,
    enabled: Boolean = true
): Modifier = if (enabled) {
    this.composed {
        // Animate the "intensity" of the fade (0f = no fade, 1f = full gradient)
        val leftIntensity by animateFloatAsState(
            targetValue = if (scrollableState.canScrollForward) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "leftEdgeFadeIntensity"
        )
        val rightIntensity by animateFloatAsState(
            targetValue = if (scrollableState.canScrollBackward) 1f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "rightEdgeFadeIntensity"
        )

        this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeWidth = 20.dp.toPx()

                // Left edge fade: 
                // We animate the starting color from Black (fully opaque) to Transparent.
                // This ensures the transition at 'fadeWidth' is always Opaque (Black).
                if (leftIntensity > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 1f - leftIntensity), Color.Black),
                            startX = 0f,
                            endX = fadeWidth
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }

                // Right edge fade
                if (rightIntensity > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - rightIntensity)),
                            startX = size.width - fadeWidth,
                            endX = size.width
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    }
} else this
