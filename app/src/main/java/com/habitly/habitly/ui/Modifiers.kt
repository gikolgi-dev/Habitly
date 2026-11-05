/* Habitly - Licensed under GNU GPL v3.0 or later. See <https://www.gnu.org/licenses/gpl-3.0.html> */

package com.habitly.habitly.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.MarqueeDefaults
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.roundToInt

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

private object MarqueeSyncRegistry {
    private val startTimesMillis = ConcurrentHashMap<Any, Long>()
    private val nodeContainerWidths = ConcurrentHashMap<Any, ConcurrentHashMap<SyncedMarqueeNode, Int>>()
    @Volatile
    var latestFrameMillis: Long = 0L

    fun getStartTimeMillis(key: Any, nowMillis: Long): Long {
        return startTimesMillis.getOrPut(key) { nowMillis }
    }

    fun updateNodeContainerWidth(key: Any, node: SyncedMarqueeNode, width: Int) {
        if (width > 0) {
            nodeContainerWidths.getOrPut(key) { ConcurrentHashMap() }[node] = width
        }
    }

    fun removeNode(key: Any, node: SyncedMarqueeNode) {
        val map = nodeContainerWidths[key] ?: return
        map.remove(node)
        if (map.isEmpty()) {
            nodeContainerWidths.remove(key, map)
        }
    }

    fun getEffectiveContainerWidth(key: Any, fallbackWidth: Int): Int {
        val widths = nodeContainerWidths[key]?.values
        val minPositive = widths?.filter { it > 0 }?.minOrNull()
        return if (minPositive != null && fallbackWidth > 0) minOf(minPositive, fallbackWidth) else fallbackWidth
    }

    fun currentTimeMillis(): Long {
        val now = System.nanoTime() / 1_000_000L
        val frame = latestFrameMillis
        return if (frame > 0L && (now - frame) in 0L..32L) frame else now
    }
}

/**
 * A marquee modifier whose scroll phase is synchronized across compositions sharing the same [syncKey]
 * (for example, between a card on the main screen and its morphed detail screen).
 */
@Stable
fun Modifier.syncedMarquee(
    syncKey: Any,
    iterations: Int = Int.MAX_VALUE,
    repeatDelayMillis: Int = MarqueeDefaults.RepeatDelayMillis,
    initialDelayMillis: Int = repeatDelayMillis,
    spacing: MarqueeSpacing = MarqueeSpacing(24.dp),
    velocity: Dp = MarqueeDefaults.Velocity,
): Modifier = this then SyncedMarqueeElement(
    syncKey = syncKey,
    iterations = iterations,
    delayMillis = repeatDelayMillis,
    initialDelayMillis = initialDelayMillis,
    spacing = spacing,
    velocity = velocity,
)

private data class SyncedMarqueeElement(
    private val syncKey: Any,
    private val iterations: Int,
    private val delayMillis: Int,
    private val initialDelayMillis: Int,
    private val spacing: MarqueeSpacing,
    private val velocity: Dp,
) : ModifierNodeElement<SyncedMarqueeNode>() {
    override fun create(): SyncedMarqueeNode = SyncedMarqueeNode(
        syncKey = syncKey,
        iterations = iterations,
        delayMillis = delayMillis,
        initialDelayMillis = initialDelayMillis,
        spacing = spacing,
        velocity = velocity,
    )

    override fun update(node: SyncedMarqueeNode) {
        node.update(
            syncKey = syncKey,
            iterations = iterations,
            delayMillis = delayMillis,
            initialDelayMillis = initialDelayMillis,
            spacing = spacing,
            velocity = velocity,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "syncedMarquee"
        properties["syncKey"] = syncKey
        properties["iterations"] = iterations
        properties["delayMillis"] = delayMillis
        properties["initialDelayMillis"] = initialDelayMillis
        properties["spacing"] = spacing
        properties["velocity"] = velocity
    }
}

private class SyncedMarqueeNode(
    private var syncKey: Any,
    private var iterations: Int,
    private var delayMillis: Int,
    private var initialDelayMillis: Int,
    spacing: MarqueeSpacing,
    private var velocity: Dp,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {

    private var contentWidth by mutableIntStateOf(0)
    private var containerWidth by mutableIntStateOf(0)
    private var offset by mutableFloatStateOf(0f)
    private var animationJob: Job? = null
    private var marqueeLayer: GraphicsLayer? = null
    var spacing: MarqueeSpacing by mutableStateOf(spacing)

    private val spacingPx by derivedStateOf {
        with(spacing) { requireDensity().calculateSpacing(contentWidth, containerWidth) }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        marqueeLayer?.let { graphicsContext.releaseGraphicsLayer(it) }
        marqueeLayer = graphicsContext.createGraphicsLayer()
        restartAnimation()
    }

    override fun onDetach() {
        MarqueeSyncRegistry.removeNode(syncKey, this)
        animationJob?.cancel()
        animationJob = null
        marqueeLayer?.let {
            requireGraphicsContext().releaseGraphicsLayer(it)
            marqueeLayer = null
        }
    }

    fun update(
        syncKey: Any,
        iterations: Int,
        delayMillis: Int,
        initialDelayMillis: Int,
        spacing: MarqueeSpacing,
        velocity: Dp,
    ) {
        this.spacing = spacing
        if (
            this.syncKey != syncKey ||
            this.iterations != iterations ||
            this.delayMillis != delayMillis ||
            this.initialDelayMillis != initialDelayMillis ||
            this.velocity != velocity
        ) {
            if (this.syncKey != syncKey) {
                MarqueeSyncRegistry.removeNode(this.syncKey, this)
                MarqueeSyncRegistry.updateNodeContainerWidth(syncKey, this, containerWidth)
            }
            this.syncKey = syncKey
            this.iterations = iterations
            this.delayMillis = delayMillis
            this.initialDelayMillis = initialDelayMillis
            this.velocity = velocity
            restartAnimation()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val childConstraints = constraints.copy(maxWidth = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        val resolvedContainerWidth = constraints.constrainWidth(placeable.width)
        val resolvedContentWidth = placeable.width
        if (!isLookingAhead) {
            containerWidth = resolvedContainerWidth
            contentWidth = resolvedContentWidth
            MarqueeSyncRegistry.updateNodeContainerWidth(syncKey, this@SyncedMarqueeNode, resolvedContainerWidth)
        }
        return layout(resolvedContainerWidth, placeable.height) {
            placeable.placeWithLayer(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = measurable.minIntrinsicHeight(Constraints.Infinity)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = measurable.maxIntrinsicHeight(Constraints.Infinity)

    private fun shouldScroll(): Boolean {
        val cWidth = contentWidth
        val boxWidth = containerWidth
        if (iterations <= 0 || boxWidth <= 0 || cWidth <= 0) return false
        val effectiveContainerWidth = MarqueeSyncRegistry.getEffectiveContainerWidth(syncKey, boxWidth)
        return cWidth > effectiveContainerWidth
    }

    private fun computeCurrentOffsetPx(nowMillis: Long = MarqueeSyncRegistry.currentTimeMillis()): Float {
        if (!shouldScroll()) return 0f
        val targetValue = (contentWidth + spacingPx).toFloat()
        if (targetValue <= 0f) return 0f

        val pxPerSec = with(requireDensity()) { velocity.toPx() }.absoluteValue
        val pxPerMilli = pxPerSec / 1000f
        if (pxPerMilli <= 0f) return 0f

        val moveDurationMillis = ceil(targetValue / pxPerMilli).toInt().coerceAtLeast(1)
        val cycleMillis = delayMillis + moveDurationMillis
        if (cycleMillis <= 0) return 0f

        val startTime = MarqueeSyncRegistry.getStartTimeMillis(syncKey, nowMillis)
        val elapsed = (nowMillis - startTime).coerceAtLeast(0L) + (delayMillis - initialDelayMillis).toLong()
        if (elapsed <= 0L) return 0f

        if (iterations != Int.MAX_VALUE) {
            val totalCycles = elapsed / cycleMillis
            if (totalCycles >= iterations) return 0f
        }

        val inCycle = (elapsed % cycleMillis).toInt()
        if (inCycle <= delayMillis) return 0f

        val moveElapsed = inCycle - delayMillis
        return ((moveElapsed.toFloat() / moveDurationMillis.toFloat()) * targetValue)
            .coerceIn(0f, targetValue)
    }

    override fun ContentDrawScope.draw() {
        val observedOffset = offset
        val currentOffset = if (observedOffset > 0f) observedOffset else computeCurrentOffsetPx()
        val clipWindowOffset =
            if (velocity > 0.dp) {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> currentOffset
                    LayoutDirection.Rtl ->
                        -currentOffset + contentWidth * 2 + spacingPx - containerWidth
                }
            } else {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> -currentOffset + contentWidth + spacingPx
                    LayoutDirection.Rtl -> currentOffset + contentWidth - containerWidth
                }
            }

        val firstCopyVisible = clipWindowOffset < contentWidth
        val secondCopyVisible = clipWindowOffset + containerWidth > contentWidth + spacingPx
        val secondCopyOffset = (contentWidth + spacingPx).toFloat()

        val drawHeight = size.height
        marqueeLayer?.let { layer ->
            layer.record(size = IntSize(contentWidth, drawHeight.roundToInt())) {
                this@draw.drawContent()
            }
        }
        clipRect(right = containerWidth.toFloat()) {
            translate(left = -clipWindowOffset) {
                val layer = marqueeLayer
                if (layer != null) {
                    if (firstCopyVisible) {
                        drawLayer(layer)
                    }
                    if (secondCopyVisible) {
                        translate(left = secondCopyOffset) { drawLayer(layer) }
                    }
                } else {
                    if (firstCopyVisible) {
                        this@draw.drawContent()
                    }
                    if (secondCopyVisible) {
                        translate(left = secondCopyOffset) { this@draw.drawContent() }
                    }
                }
            }
        }
    }

    private fun restartAnimation() {
        val oldJob = animationJob
        oldJob?.cancel()
        if (isAttached) {
            animationJob = coroutineScope.launch {
                oldJob?.join()
                runAnimation()
            }
        }
    }

    private suspend fun runAnimation() {
        if (iterations <= 0) return
        withContext(SyncedFixedMotionDurationScale) {
            snapshotFlow {
                if (!shouldScroll()) null else (contentWidth + spacingPx)
            }.collectLatest { contentWithSpacingWidth ->
                if (contentWithSpacingWidth == null) {
                    offset = 0f
                    return@collectLatest
                }
                val now = MarqueeSyncRegistry.currentTimeMillis()
                MarqueeSyncRegistry.getStartTimeMillis(syncKey, now)
                offset = computeCurrentOffsetPx(now)
                while (true) {
                    withInfiniteAnimationFrameMillis { frameMillis ->
                        MarqueeSyncRegistry.latestFrameMillis = frameMillis
                        offset = computeCurrentOffsetPx(frameMillis)
                    }
                }
            }
        }
    }
}

private object SyncedFixedMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = 1f
}
