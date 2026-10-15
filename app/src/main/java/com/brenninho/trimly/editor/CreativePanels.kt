package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.AspectRatio
import com.brenninho.trimly.model.Datamosh
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.MoshMode
import com.brenninho.trimly.model.MusicTrack
import kotlin.math.roundToInt

@Composable
fun AspectPanel(
    current: AspectRatio,
    outputSize: Pair<Int, Int>,
    enabled: Boolean,
    onSelect: (AspectRatio) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val (width, height) = outputSize
    Column(modifier = modifier.fillMaxWidth()) {
        PanelHeader(
            title = s.aspectTitle,
            subtitle = if (width > 0 && height > 0) "$width × $height · ${s.aspectHint}" else s.aspectHint,
            onClose = onClose
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(AspectRatio.entries) { aspect ->
                AspectTile(
                    aspect = aspect,
                    title = if (aspect == AspectRatio.ORIGINAL) s.aspectOriginal else aspect.label,
                    caption = s.aspectCaption(aspect),
                    selected = aspect == current,
                    enabled = enabled,
                    onClick = { onSelect(aspect) }
                )
            }
        }
    }
}

@Composable
private fun AspectTile(
    aspect: AspectRatio,
    title: String,
    caption: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val border by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(180),
        label = "aspectBorder"
    )
    val fill by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(180),
        label = "aspectFill"
    )
    val ratio = aspect.ratio ?: (16f / 10f)
    val boxHeight = 44f
    val boxWidth = (boxHeight * ratio).coerceIn(22f, 70f)
    val drawnHeight = if (boxHeight * ratio > 70f) 70f / ratio else boxHeight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(82.dp)
            .pressScale(source, 0.94f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(boxWidth.dp, drawnHeight.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(fill)
                    .border(2.dp, border, RoundedCornerShape(6.dp))
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MusicPanel(
    options: ExportOptions,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onVolume: (Int) -> Unit,
    onLoop: (Boolean) -> Unit,
    onOffset: (Long) -> Unit,
    onVideoVolume: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val music = options.music

    Column(modifier = modifier.fillMaxWidth()) {
        PanelHeader(
            title = s.musicTitle,
            subtitle = music?.let { "${it.name} · ${formatTime(it.durationMs)}" } ?: s.musicEmpty,
            onClose = onClose
        )

        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (music == null) {
                FilledTonalButton(
                    onClick = onPick,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.musicPick)
                }
            } else {
                MusicCard(music = music, enabled = enabled, onReplace = onPick, onRemove = onRemove)

                LabeledSlider(
                    label = s.musicVolume,
                    value = music.volume,
                    range = 0..200,
                    enabled = enabled,
                    onChange = onVolume
                )
                LabeledSlider(
                    label = s.musicOriginal,
                    value = if (options.muted) 0 else options.volume,
                    range = 0..200,
                    enabled = enabled && !options.muted,
                    onChange = onVideoVolume
                )

                val maxOffset = (music.durationMs - 1000L).coerceAtLeast(0L)
                if (maxOffset > 0L) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = s.musicStart,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(96.dp)
                        )
                        Slider(
                            value = music.startOffsetMs.toFloat(),
                            onValueChange = { onOffset(((it / 500f).roundToInt() * 500L)) },
                            valueRange = 0f..maxOffset.toFloat(),
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatTime(music.startOffsetMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }

                SwitchRow(
                    title = s.musicLoop,
                    hint = s.musicLoopHint,
                    checked = music.loop,
                    enabled = enabled,
                    onChange = onLoop
                )
            }
        }
    }
}

@Composable
private fun MusicCard(
    music: MusicTrack,
    enabled: Boolean,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    val s = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTime(music.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onReplace, enabled = enabled) { Text(s.musicReplace) }
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(Icons.Filled.Close, contentDescription = s.musicRemove)
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange((it / 5f).roundToInt() * 5) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$value%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
fun DatamoshPanel(
    mosh: Datamosh?,
    playheadMs: Long,
    totalMs: Long,
    enabled: Boolean,
    onEnable: (Boolean) -> Unit,
    onChange: (String?, (Datamosh) -> Datamosh) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        PanelHeader(
            title = s.moshTitle,
            subtitle = s.moshIntro,
            onClose = onClose
        )

        Column(
            modifier = Modifier
                .heightIn(max = 340.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SwitchRow(
                title = s.moshOn,
                hint = s.moshPlayerNote,
                checked = mosh != null,
                enabled = enabled,
                onChange = onEnable
            )

            AnimatedVisibility(visible = mosh != null) {
                val current = mosh ?: Datamosh()
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        ModeCard(
                            icon = Icons.Filled.Waves,
                            title = s.moshMelt,
                            hint = s.moshMeltHint,
                            selected = current.mode == MoshMode.MELT,
                            enabled = enabled,
                            onClick = { onChange(null) { it.copy(mode = MoshMode.MELT) } },
                            modifier = Modifier.weight(1f)
                        )
                        ModeCard(
                            icon = Icons.Filled.AutoAwesome,
                            title = s.moshBloom,
                            hint = s.moshBloomHint,
                            selected = current.mode == MoshMode.BLOOM,
                            enabled = enabled,
                            onClick = { onChange(null) { it.copy(mode = MoshMode.BLOOM) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = s.moshRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                    RangeBar(
                        startMs = current.startMs,
                        endMs = current.endMs ?: totalMs,
                        playheadMs = playheadMs,
                        totalMs = totalMs
                    )
                    Text(
                        text = s.moshFrom(formatTime(current.startMs)) + "  ·  " +
                            (current.endMs?.let { s.moshTo(formatTime(it)) } ?: s.moshToEnd),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onChange(null) { value ->
                                    val end = value.endMs
                                    val start = playheadMs.coerceAtLeast(0L)
                                    value.copy(startMs = start, endMs = end?.takeIf { it > start })
                                }
                            },
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        ) { Text(s.moshSetStart, maxLines = 1) }
                        OutlinedButton(
                            onClick = {
                                onChange(null) { value ->
                                    val end = playheadMs.coerceAtLeast(value.startMs + 200L)
                                    value.copy(endMs = end)
                                }
                            },
                            enabled = enabled,
                            modifier = Modifier.weight(1f)
                        ) { Text(s.moshSetEnd, maxLines = 1) }
                    }
                    if (current.endMs != null) {
                        TextButton(
                            onClick = { onChange(null) { it.copy(endMs = null) } },
                            enabled = enabled,
                            modifier = Modifier.padding(start = 8.dp)
                        ) { Text(s.moshClearEnd) }
                    }

                    Text(
                        text = s.moshKeyframes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(Datamosh.keyframeSteps) { step ->
                            ChoicePill(
                                label = s.fadeSeconds(if (step % 1f == 0f) step.toInt().toString() else step.toString()),
                                selected = step == current.keyframeSeconds,
                                enabled = enabled,
                                onClick = { onChange(null) { it.copy(keyframeSeconds = step) } }
                            )
                        }
                    }
                    Text(
                        text = s.moshKeyframesHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    AnimatedVisibility(visible = current.mode == MoshMode.BLOOM) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = s.moshBloomLength,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(96.dp)
                            )
                            Slider(
                                value = current.bloomFrames.toFloat(),
                                onValueChange = { value ->
                                    onChange("mosh_bloom") { it.copy(bloomFrames = value.roundToInt()) }
                                },
                                valueRange = Datamosh.MIN_BLOOM.toFloat()..Datamosh.MAX_BLOOM.toFloat(),
                                enabled = enabled,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = s.moshFrames(current.bloomFrames),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(72.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    hint: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(180),
        label = "modeCard"
    )
    val border by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(180),
        label = "modeBorder"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RangeBar(
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    totalMs: Long
) {
    val total = totalMs.coerceAtLeast(1L).toFloat()
    val start = (startMs / total).coerceIn(0f, 1f)
    val end = (endMs / total).coerceIn(start, 1f)
    val head = (playheadMs / total).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val marker = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(track)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                color = primary.copy(alpha = 0.7f),
                topLeft = Offset(size.width * start, 0f),
                size = Size(size.width * (end - start), size.height)
            )
            val x = size.width * head
            drawRect(
                color = marker,
                topLeft = Offset(x - 1.5f, 0f),
                size = Size(3f, size.height)
            )
        }
    }
}
