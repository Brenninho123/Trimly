package com.brenninho.trimly.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.data.ThumbnailLoader
import com.brenninho.trimly.i18n.LocalStrings

@Composable
fun MergePanel(
    segments: List<Segment>,
    selected: Int,
    totalMs: Long,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.mergeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = s.mergeTotal(formatTime(totalMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) { Text(s.done) }
        }

        Text(
            text = s.mergeHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(segments, key = { _, segment -> segment.id }) { index, segment ->
                SegmentCard(
                    index = index,
                    segment = segment,
                    selected = index == selected,
                    canRemove = segments.size > 1 && enabled,
                    canMoveLeft = index > 0 && enabled,
                    canMoveRight = index < segments.lastIndex && enabled,
                    onSelect = { onSelect(index) },
                    onRemove = { onRemove(index) },
                    onLeft = { onMove(index, index - 1) },
                    onRight = { onMove(index, index + 1) },
                    modifier = Modifier.animateItem()
                )
            }
            item(key = "add") {
                AddCard(enabled = enabled, onClick = onAdd, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun SegmentCard(
    index: Int,
    segment: Segment,
    selected: Boolean,
    canRemove: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    var bitmap by remember(segment.clip.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(segment.clip.uri) {
        bitmap = ThumbnailLoader.load(context, segment.clip.uri.toString())
    }

    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "segmentBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(116.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .pressScale(source, 0.95f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, borderColor, shape)
                .clickable(interactionSource = source, indication = null, onClick = onSelect)
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Text(
                text = formatTime(segment.clip.trimmedDurationMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            if (canRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = s.mergeRemove,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onRemove)
                        .padding(4.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onLeft, enabled = canMoveLeft, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = s.moveEarlier)
            }
            Text(
                text = s.mergeClipLabel(index + 1),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onRight, enabled = canMoveRight, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = s.moveLater)
            }
        }
    }
}

@Composable
private fun AddCard(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val source = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(116.dp)
            .aspectRatio(16f / 9f)
            .pressScale(source, 0.95f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.mergeAdd,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
