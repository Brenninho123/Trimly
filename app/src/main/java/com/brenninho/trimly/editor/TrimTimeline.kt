package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val DRAG_NONE = 0
private const val DRAG_START = 1
private const val DRAG_END = 2
private const val DRAG_SCRUB = 3

@Composable
fun TrimTimeline(
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    positionMs: Long,
    frames: List<ImageBitmap?>,
    enabled: Boolean,
    onDragStarted: () -> Unit,
    onStartChange: (Long) -> Unit,
    onEndChange: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleWidth = 18.dp
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sheen = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    var dragging by remember { mutableIntStateOf(DRAG_NONE) }

    val latestStart by rememberUpdatedState(startMs)
    val latestEnd by rememberUpdatedState(endMs)
    val latestDuration by rememberUpdatedState(durationMs.coerceAtLeast(1L))
    val latestDragStarted by rememberUpdatedState(onDragStarted)
    val latestStartChange by rememberUpdatedState(onStartChange)
    val latestEndChange by rememberUpdatedState(onEndChange)
    val latestSeek by rememberUpdatedState(onSeek)

    val shimmerTransition = rememberInfiniteTransition(label = "timelineShimmer")
    val shimmer = shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timelineShimmerValue"
    )

    val startScale by animateFloatAsState(
        targetValue = if (dragging == DRAG_START) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "startHandleScale"
    )
    val endScale by animateFloatAsState(
        targetValue = if (dragging == DRAG_END) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "endHandleScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val handlePx = with(density) { handleWidth.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val bubbleEdge = with(density) { 36.dp.toPx() }
        val usable = (widthPx - 2f * handlePx).coerceAtLeast(1f)
        val latestUsable by rememberUpdatedState(usable)
        val safeDuration = durationMs.coerceAtLeast(1L)

        val xStart = handlePx + startMs.toFloat() / safeDuration * usable
        val xEnd = handlePx + endMs.toFloat() / safeDuration * usable
        val xPosition = handlePx + positionMs.coerceIn(0L, safeDuration).toFloat() / safeDuration * usable

        fun timeAt(x: Float): Long =
            (((x - handlePx) / latestUsable) * latestDuration)
                .roundToLong()
                .coerceIn(latestStart, latestEnd)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = handleWidth)
                .clip(RoundedCornerShape(6.dp))
        ) {
            if (frames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(trackColor)
                )
            }
            frames.forEach { frame ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(trackColor)
                ) {
                    if (frame != null) {
                        Image(
                            bitmap = frame,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val width = size.width
                                    val start = -width + shimmer.value * 2f * width
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, sheen, Color.Transparent),
                                            startX = start,
                                            endX = start + width
                                        )
                                    )
                                }
                        )
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val bar = 3.dp.toPx()
            val dim = Color.Black.copy(alpha = 0.6f)
            drawRect(
                color = dim,
                topLeft = Offset(handlePx, 0f),
                size = Size((xStart - handlePx).coerceAtLeast(0f), size.height)
            )
            drawRect(
                color = dim,
                topLeft = Offset(xEnd, 0f),
                size = Size((widthPx - handlePx - xEnd).coerceAtLeast(0f), size.height)
            )
            drawRect(
                color = primary,
                topLeft = Offset(xStart, 0f),
                size = Size((xEnd - xStart).coerceAtLeast(0f), bar)
            )
            drawRect(
                color = primary,
                topLeft = Offset(xStart, size.height - bar),
                size = Size((xEnd - xStart).coerceAtLeast(0f), bar)
            )
            if (positionMs in startMs..endMs) {
                val line = 3.dp.toPx()
                val glow = 9.dp.toPx()
                drawRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(xPosition - glow / 2f, 0f),
                    size = Size(glow, size.height)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(xPosition - line / 2f, 0f),
                    size = Size(line, size.height)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> latestSeek(timeAt(offset.x)) }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = DRAG_SCRUB
                            latestDragStarted()
                            latestSeek(timeAt(offset.x))
                        },
                        onDragEnd = { dragging = DRAG_NONE },
                        onDragCancel = { dragging = DRAG_NONE }
                    ) { change, _ ->
                        change.consume()
                        latestSeek(timeAt(change.position.x))
                    }
                }
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset((xStart - handlePx).roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = startScale
                    scaleY = startScale
                }
                .width(handleWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(primary)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = DRAG_START
                            latestDragStarted()
                        },
                        onDragEnd = { dragging = DRAG_NONE },
                        onDragCancel = { dragging = DRAG_NONE }
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount / latestUsable * latestDuration).roundToLong()
                        val limit = (latestEnd - MIN_TRIM_MS).coerceAtLeast(0L)
                        latestStartChange((latestStart + delta).coerceIn(0L, limit))
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(onPrimary.copy(alpha = 0.85f))
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(xEnd.roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = endScale
                    scaleY = endScale
                }
                .width(handleWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(primary)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = DRAG_END
                            latestDragStarted()
                        },
                        onDragEnd = { dragging = DRAG_NONE },
                        onDragCancel = { dragging = DRAG_NONE }
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount / latestUsable * latestDuration).roundToLong()
                        val floor = (latestStart + MIN_TRIM_MS).coerceAtMost(latestDuration)
                        latestEndChange((latestEnd + delta).coerceIn(floor, latestDuration))
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(onPrimary.copy(alpha = 0.85f))
            )
        }

        TimeBubble(
            visible = dragging == DRAG_START,
            text = formatTime(startMs),
            centerXPx = xStart.coerceIn(bubbleEdge, widthPx - bubbleEdge)
        )
        TimeBubble(
            visible = dragging == DRAG_END,
            text = formatTime(endMs),
            centerXPx = xEnd.coerceIn(bubbleEdge, widthPx - bubbleEdge)
        )
        TimeBubble(
            visible = dragging == DRAG_SCRUB,
            text = formatTime(positionMs),
            centerXPx = xPosition.coerceIn(bubbleEdge, widthPx - bubbleEdge)
        )
    }
}

@Composable
private fun TimeBubble(
    visible: Boolean,
    text: String,
    centerXPx: Float
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)) + scaleIn(tween(160)),
        exit = fadeOut(tween(120)) + scaleOut(tween(120)),
        modifier = Modifier
            .offset { IntOffset(centerXPx.roundToInt(), -44.dp.roundToPx()) }
            .graphicsLayer { translationX = -size.width / 2f }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
