package com.brenninho.trimly.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.FilterGroup
import com.brenninho.trimly.model.VideoFilter

@Composable
fun EditorMenu(
    options: ExportOptions,
    hasEdits: Boolean,
    enabled: Boolean,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onMute: () -> Unit,
    onQuality: () -> Unit,
    onFilters: () -> Unit,
    onEffects: () -> Unit,
    onAdjust: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterActive = options.filter != VideoFilter.NONE && options.filter.group == FilterGroup.FILTER
    val effectActive = options.filter != VideoFilter.NONE && options.filter.group == FilterGroup.EFFECT

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        MenuItem(
            icon = Icons.Filled.ContentCut,
            label = "Trim",
            active = true,
            enabled = false,
            onClick = {}
        )
        MenuItem(
            icon = Icons.Filled.Palette,
            label = if (filterActive) options.filter.label else "Filters",
            active = filterActive,
            enabled = enabled,
            onClick = onFilters
        )
        MenuItem(
            icon = Icons.Filled.AutoAwesome,
            label = if (effectActive) options.filter.label else "Effects",
            active = effectActive,
            enabled = enabled,
            onClick = onEffects
        )
        MenuItem(
            icon = Icons.Filled.Tune,
            label = "Adjust",
            active = options.hasAdjustments,
            enabled = enabled,
            onClick = onAdjust
        )
        MenuItem(
            icon = Icons.Filled.RotateRight,
            label = if (options.rotationDegrees % 360 != 0) "Rotate ${options.rotationDegrees}°" else "Rotate",
            active = options.rotationDegrees % 360 != 0,
            enabled = enabled,
            onClick = onRotate
        )
        MenuItem(
            icon = Icons.Filled.Flip,
            label = "Flip",
            active = options.flipHorizontal,
            enabled = enabled,
            onClick = onFlip
        )
        MenuItem(
            icon = if (options.muted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
            label = if (options.muted) "Unmute" else "Mute",
            active = options.muted,
            enabled = enabled,
            onClick = onMute
        )
        MenuItem(
            icon = Icons.Filled.HighQuality,
            label = options.shortSide?.let { "${it}p" } ?: "Quality",
            active = options.shortSide != null,
            enabled = enabled,
            onClick = onQuality
        )
        MenuItem(
            icon = Icons.Filled.Speed,
            label = "Speed (soon)",
            active = false,
            enabled = false,
            onClick = {}
        )
        MenuItem(
            icon = Icons.Filled.Refresh,
            label = "Reset",
            active = false,
            enabled = enabled && hasEdits,
            onClick = onReset
        )
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val fill = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val dimmed = !enabled && !active

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (dimmed) 0.4f else 1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(fill)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
