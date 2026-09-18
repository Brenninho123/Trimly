package com.brenninho.trimly.editor

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportDialog(
    status: ExportStatus,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit,
    onOpen: (Uri) -> Unit
) {
    when (status) {
        is ExportStatus.Idle -> Unit

        is ExportStatus.Running -> AlertDialog(
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
                        text = "${(status.progress * 100).toInt().coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        )

        is ExportStatus.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export complete") },
            text = { Text("Saved to ${status.location}") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Done") }
            },
            dismissButton = {
                val uri = status.uri
                if (uri != null) {
                    Row {
                        TextButton(onClick = { onShare(uri) }) { Text("Share") }
                        TextButton(onClick = { onOpen(uri) }) { Text("Open") }
                    }
                }
            }
        )

        is ExportStatus.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Export failed") },
            text = { Text(status.message) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

@Composable
fun QualityDialog(
    selected: Int?,
    options: List<Int>,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export quality") },
        text = {
            Column {
                QualityRow(label = "Original", selected = selected == null) { onSelect(null) }
                options.forEach { side ->
                    QualityRow(label = "${side}p", selected = selected == side) { onSelect(side) }
                }
                if (options.isEmpty()) {
                    Text(
                        text = "Lower resolutions are not available for this video.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun QualityRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
