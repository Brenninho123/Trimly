@file:OptIn(ExperimentalMaterial3Api::class)

package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

class EditChip(
    val id: String,
    val label: String,
    val onClear: () -> Unit
)

@Composable
fun TransportBar(
    playing: Boolean,
    looping: Boolean,
    enabled: Boolean,
    positionLabel: String,
    onToStart: () -> Unit,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onForward: () -> Unit,
    onToEnd: () -> Unit,
    onLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = positionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            TransportButton(Icons.Filled.SkipPrevious, "Go to start", enabled, onToStart)
            TransportButton(Icons.Filled.Replay5, "Back 5 seconds", enabled, onBack)
            PlayButton(playing = playing, enabled = enabled, onClick = onToggle)
            TransportButton(Icons.Filled.Forward5, "Forward 5 seconds", enabled, onForward)
            TransportButton(Icons.Filled.SkipNext, "Go to end", enabled, onToEnd)
        }
        LoopButton(looping = looping, enabled = enabled, onClick = onLoop)
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = Modifier
            .size(40.dp)
            .pressScale(source, 0.85f)
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

@Composable
private fun PlayButton(
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(52.dp)
            .pressScale(source, 0.9f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = { (scaleIn(tween(160)) + fadeIn(tween(160))) togetherWith (scaleOut(tween(120)) + fadeOut(tween(120))) },
            label = "playIcon"
        ) { isPlaying ->
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun LoopButton(
    looping: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        targetValue = if (looping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "loopTint"
    )
    val fill by animateColorAsState(
        targetValue = if (looping) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(200),
        label = "loopFill"
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = Modifier
            .size(40.dp)
            .pressScale(source, 0.85f)
            .clip(CircleShape)
            .background(fill)
    ) {
        Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = if (looping) "Loop on" else "Loop off",
            tint = tint
        )
    }
}

@Composable
fun TrimReadout(
    startMs: Long,
    lengthMs: Long,
    endMs: Long,
    enabled: Boolean,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        TimeChip(
            label = "Set start",
            value = formatTime(startMs),
            alignment = Alignment.Start,
            enabled = enabled,
            onClick = onSetStart
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Length",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(lengthMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        TimeChip(
            label = "Set end",
            value = formatTime(endMs),
            alignment = Alignment.End,
            enabled = enabled,
            onClick = onSetEnd
        )
    }
}

@Composable
private fun TimeChip(
    label: String,
    value: String,
    alignment: Alignment.Horizontal,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.pressScale(source)
    ) {
        Column(
            horizontalAlignment = alignment,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EditChipsRow(
    chips: List<EditChip>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = chips.isNotEmpty(),
        enter = expandVertically(tween(240)) + fadeIn(tween(200)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            items(chips, key = { it.id }) { chip ->
                AssistChip(
                    onClick = chip.onClear,
                    enabled = enabled,
                    label = { Text(chip.label) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove ${chip.label}",
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun SeekBubble(
    forward: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        modifier = modifier.padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = if (forward) Icons.Filled.Forward5 else Icons.Filled.Replay5,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = if (forward) "+5s" else "-5s",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun CompareButton(
    onHold: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val latest by rememberUpdatedState(onHold)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        latest(true)
                        tryAwaitRelease()
                        latest(false)
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Compare,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Hold to compare",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
fun InfoBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun OverlayVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        content()
    }
}
