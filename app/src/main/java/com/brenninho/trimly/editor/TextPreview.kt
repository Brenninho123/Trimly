package com.brenninho.trimly.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.model.TextItem
import kotlin.math.roundToInt

@Composable
fun TextPreviewLayer(
    items: List<TextItem>,
    timelineMs: Long,
    selectedId: Long?,
    editable: Boolean,
    onSelect: (Long?) -> Unit,
    onMove: (Long, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        items.forEach { item ->
            if (editable || item.visibleAt(timelineMs)) {
                key(item.id) {
                    PreviewText(
                        item = item,
                        dimmed = !item.visibleAt(timelineMs),
                        selected = item.id == selectedId,
                        editable = editable,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        onSelect = onSelect,
                        onMove = onMove
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewText(
    item: TextItem,
    dimmed: Boolean,
    selected: Boolean,
    editable: Boolean,
    widthPx: Float,
    heightPx: Float,
    onSelect: (Long?) -> Unit,
    onMove: (Long, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val latest by rememberUpdatedState(item)
    val fontSize = with(density) { (item.sizeFraction * heightPx).toSp() }
    val primary = MaterialTheme.colorScheme.primary

    Text(
        text = item.text,
        color = Color(item.colorArgb),
        fontSize = fontSize,
        fontWeight = if (item.bold) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        (item.x * constraints.maxWidth - placeable.width / 2f).roundToInt(),
                        (item.y * constraints.maxHeight - placeable.height / 2f).roundToInt()
                    )
                }
            }
            .graphicsLayer { alpha = if (dimmed) 0.45f else 1f }
            .then(if (item.background) Modifier.background(Color.Black.copy(alpha = 0.67f)) else Modifier)
            .then(if (selected && editable) Modifier.border(1.5.dp, primary, RoundedCornerShape(4.dp)) else Modifier)
            .pointerInput(editable) {
                if (!editable) return@pointerInput
                detectTapGestures(onTap = { onSelect(latest.id) })
            }
            .pointerInput(editable, selected) {
                if (!editable || !selected) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(
                        latest.id,
                        latest.x + dragAmount.x / widthPx,
                        latest.y + dragAmount.y / heightPx
                    )
                }
            }
    )
}
