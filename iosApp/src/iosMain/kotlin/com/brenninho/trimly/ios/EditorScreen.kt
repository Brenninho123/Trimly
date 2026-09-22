package com.brenninho.trimly.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    controller: EditorController,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditorTopBar(controller, onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Trim",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            RangeSlider(
                value = controller.startMs.toFloat()..controller.endMs.toFloat(),
                onValueChange = { range ->
                    controller.setRange(range.start.toLong(), range.endInclusive.toLong())
                },
                valueRange = 0f..controller.info.durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Transform",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val rotated = controller.options.rotationDegrees % 360 != 0
                FilterChip(
                    selected = rotated,
                    onClick = controller::rotate,
                    label = { Text(if (rotated) "Rotate ${controller.options.rotationDegrees}°" else "Rotate") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.RotateRight,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
                FilterChip(
                    selected = controller.options.flipHorizontal,
                    onClick = controller::toggleFlip,
                    label = { Text("Flip") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Flip,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
                FilterChip(
                    selected = controller.options.muted,
                    onClick = controller::toggleMute,
                    label = { Text(if (controller.options.muted) "Muted" else "Mute") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (controller.options.muted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
    }

    ExportDialogs(controller)
}

@Composable
private fun EditorTopBar(
    controller: EditorController,
    onClose: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close editor")
            }
            Text(
                text = controller.path.substringAfterLast('/'),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            OutlinedButton(onClick = controller::reset, enabled = controller.hasEdits) {
                Text("Reset")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = controller::startExport, enabled = controller.canExport) {
                Text("Export")
            }
        }
    }
}

@Composable
private fun ExportDialogs(controller: EditorController) {
    when (val status = controller.export) {
        is ExportUi.Idle -> Unit

        is ExportUi.Running -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Exporting") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { status.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${(status.progress * 100).roundToInt().coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = controller::cancelExport) { Text("Cancel") }
            }
        )

        is ExportUi.Done -> AlertDialog(
            onDismissRequest = controller::dismissExport,
            title = { Text("Export complete") },
            text = { Text(status.path) },
            confirmButton = {
                TextButton(onClick = controller::dismissExport) { Text("Close") }
            }
        )

        is ExportUi.Failed -> AlertDialog(
            onDismissRequest = controller::dismissExport,
            title = { Text("Export failed") },
            text = { Text(status.message) },
            confirmButton = {
                TextButton(onClick = controller::dismissExport) { Text("Close") }
            }
        )
    }
}
