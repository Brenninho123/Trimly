package com.brenninho.trimly.editor

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

    val latestStart by rememberUpdatedState(startMs)
    val latestEnd by rememberUpdatedState(endMs)
    val latestDuration by rememberUpdatedState(durationMs.coerceAtLeast(1L))
    val latestDragStarted by rememberUpdatedState(onDragStarted)
    val latestStartChange by rememberUpdatedState(onStartChange)
    val latestEndChange by rememberUpdatedState(onEndChange)
    val latestSeek by rememberUpdatedState(onSeek)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val handlePx = with(density) { handleWidth.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
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
                            latestDragStarted()
                            latestSeek(timeAt(offset.x))
                        }
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
                .width(handleWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(primary)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(onDragStart = { latestDragStarted() }) { change, dragAmount ->
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
                .width(handleWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(primary)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(onDragStart = { latestDragStarted() }) { change, dragAmount ->
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
    }
}
