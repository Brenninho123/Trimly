@file:OptIn(ExperimentalMaterial3Api::class)

package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.TextItem

private val Palette = listOf(
    0xFFFFFFFF,
    0xFF000000,
    0xFFFFE066,
    0xFFFFB86B,
    0xFFFF6B6B,
    0xFFFF8FD8,
    0xFF7C9CFF,
    0xFF6BD68A
).map { it.toInt() }

@Composable
fun TextPanel(
    texts: List<TextItem>,
    selectedId: Long?,
    totalMs: Long,
    enabled: Boolean,
    onAdd: () -> Unit,
    onSelect: (Long?) -> Unit,
    onChange: (Long, (TextItem) -> TextItem) -> Unit,
    onDelete: (Long) -> Unit,
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
            Text(
                text = s.toolText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClose) { Text(s.done) }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(texts, key = { it.id }) { item ->
                FilterChip(
                    selected = item.id == selectedId,
                    onClick = { onSelect(item.id) },
                    enabled = enabled,
                    label = {
                        Text(
                            text = item.text.take(14).ifBlank { s.toolText },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
            item(key = "add") {
                AssistChip(
                    onClick = onAdd,
                    enabled = enabled,
                    label = { Text(s.textAdd) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }

        AnimatedContent(
            targetState = selectedId?.takeIf { id -> texts.any { it.id == id } },
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "textEditor"
        ) { id ->
            val item = texts.firstOrNull { it.id == id }
            if (item == null) {
                Text(
                    text = s.textEmptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                )
            } else {
                TextEditor(
                    item = item,
                    totalMs = totalMs,
                    enabled = enabled,
                    onChange = { transform -> onChange(item.id, transform) },
                    onDelete = { onDelete(item.id) }
                )
            }
        }
    }
}

@Composable
private fun TextEditor(
    item: TextItem,
    totalMs: Long,
    enabled: Boolean,
    onChange: ((TextItem) -> TextItem) -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalStrings.current
    var field by remember(item.id) { mutableStateOf(item.text) }
    val whole = item.endMs == null
    val safeTotal = totalMs.coerceAtLeast(1000L)

    LaunchedEffect(item.text) {
        if (item.text != field) field = item.text
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .heightIn(max = 250.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = field,
            onValueChange = { value ->
                field = value
                onChange { current -> current.copy(text = value) }
            },
            label = { Text(s.textEditLabel) },
            maxLines = 3,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = s.textDragHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = s.textColor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(84.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Palette.forEach { color ->
                    ColorDot(
                        color = color,
                        selected = color == item.colorArgb,
                        enabled = enabled,
                        onClick = { onChange { current -> current.copy(colorArgb = color) } }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = s.textSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(84.dp)
            )
            Slider(
                value = item.sizeFraction,
                onValueChange = { value -> onChange { current -> current.copy(sizeFraction = value) } },
                valueRange = 0.03f..0.16f,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = item.bold,
                onClick = { onChange { current -> current.copy(bold = !current.bold) } },
                enabled = enabled,
                label = { Text(s.textBold) }
            )
            FilterChip(
                selected = item.background,
                onClick = { onChange { current -> current.copy(background = !current.background) } },
                enabled = enabled,
                label = { Text(s.textBackground) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = s.textPosition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(76.dp)
            )
            OutlinedButton(
                onClick = { onChange { current -> current.copy(x = 0.5f, y = 0.12f) } },
                enabled = enabled
            ) { Text(s.posTop) }
            OutlinedButton(
                onClick = { onChange { current -> current.copy(x = 0.5f, y = 0.5f) } },
                enabled = enabled
            ) { Text(s.posCenter) }
            OutlinedButton(
                onClick = { onChange { current -> current.copy(x = 0.5f, y = 0.88f) } },
                enabled = enabled
            ) { Text(s.posBottom) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = s.textWholeVideo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = whole,
                onCheckedChange = { checked ->
                    onChange { current ->
                        if (checked) current.copy(startMs = 0L, endMs = null) else current.copy(startMs = 0L, endMs = safeTotal)
                    }
                },
                enabled = enabled
            )
        }

        AnimatedVisibility(visible = !whole) {
            Column {
                Text(
                    text = s.textFrom(formatTime(item.startMs), formatTime(item.endMs ?: safeTotal)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RangeSlider(
                    value = item.startMs.toFloat()..(item.endMs ?: safeTotal).toFloat().coerceAtLeast(item.startMs + 500f),
                    onValueChange = { range ->
                        onChange { current ->
                            current.copy(
                                startMs = range.start.toLong(),
                                endMs = range.endInclusive.toLong().coerceAtLeast(range.start.toLong() + 500L)
                            )
                        }
                    },
                    valueRange = 0f..safeTotal.toFloat(),
                    enabled = enabled
                )
            }
        }

        OutlinedButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.textDelete)
        }
    }
}

@Composable
private fun ColorDot(
    color: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val ring by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "colorRing"
    )

    Row(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {}
}
