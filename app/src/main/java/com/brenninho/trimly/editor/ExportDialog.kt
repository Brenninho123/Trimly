package com.brenninho.trimly.editor

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.engine.ExportFailure
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.ExportQuality
import java.util.Locale
import kotlin.math.roundToInt

private val Success = Color(0xFF6BD68A)

@Composable
fun ExportDialog(
    status: ExportStatus,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit,
    onOpen: (Uri) -> Unit
) {
    val s = LocalStrings.current

    when (status) {
        is ExportStatus.Idle -> Unit

        is ExportStatus.Running -> AlertDialog(
            onDismissRequest = {},
            title = { Text(s.exporting) },
            text = {
                RunningContent(
                    progress = status.progress,
                    hint = etaLabel(s, status.etaMs) ?: s.exportingHint
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onCancel) { Text(s.cancel) }
            }
        )

        is ExportStatus.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { ResultIcon(success = true) },
            title = { Text(s.exportComplete) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = s.savedTo(status.location),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (status.sizeBytes > 0L) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${s.exportSize(formatSize(status.sizeBytes))} · ${s.exportTook(formatElapsed(status.elapsedMs))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (status.fastTrim) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = s.exportFastTrim,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(s.done) }
            },
            dismissButton = {
                val uri = status.uri
                if (uri != null) {
                    Row {
                        TextButton(onClick = { onShare(uri) }) { Text(s.share) }
                        TextButton(onClick = { onOpen(uri) }) { Text(s.openVideo) }
                    }
                }
            }
        )

        is ExportStatus.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { ResultIcon(success = false) },
            title = { Text(s.exportFailed) },
            text = {
                Text(
                    text = failureText(s, status),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(s.close) }
            }
        )
    }
}

private fun failureText(s: AppStrings, status: ExportStatus.Failed): String = when (status.failure) {
    ExportFailure.NO_SPACE -> s.exportErrNoSpace
    ExportFailure.UNSUPPORTED -> s.exportErrUnsupported
    ExportFailure.SOURCE_UNREADABLE -> s.exportErrSource
    ExportFailure.STALLED -> s.exportErrStalled
    ExportFailure.EMPTY_RANGE -> s.exportErrRange
    ExportFailure.ENCODER -> s.exportErrEncoder
    ExportFailure.OTHER, null -> status.message ?: s.exportFailedHint
}

private fun etaLabel(s: AppStrings, etaMs: Long?): String? {
    if (etaMs == null) return null
    val seconds = (etaMs / 1000).coerceAtLeast(1L)
    return if (seconds < 90L) s.exportEtaSeconds(seconds) else s.exportEtaMinutes((seconds + 30L) / 60L)
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "%.2f GB".format(Locale.ROOT, bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> "%.1f MB".format(Locale.ROOT, bytes / 1_000_000.0)
    else -> "%d KB".format(Locale.ROOT, bytes / 1000L)
}

private fun formatElapsed(ms: Long): String {
    val total = ms / 1000L
    return if (total < 60L) {
        "$total s"
    } else {
        "%d:%02d".format(Locale.ROOT, total / 60L, total % 60L)
    }
}

@Composable
private fun RunningContent(
    progress: Float,
    hint: String
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(260),
        label = "exportProgress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animated },
                strokeWidth = 8.dp,
                modifier = Modifier.size(104.dp)
            )
            Text(
                text = "${(animated * 100).roundToInt().coerceIn(0, 100)}%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ResultIcon(success: Boolean) {
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        shown = true
    }

    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "resultScale"
    )
    val tint by animateColorAsState(
        targetValue = if (success) Success else MaterialTheme.colorScheme.error,
        animationSpec = tween(200),
        label = "resultTint"
    )

    Icon(
        imageVector = if (success) Icons.Filled.CheckCircle else Icons.Filled.Warning,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
fun QualityDialog(
    selected: Int?,
    options: List<Int>,
    level: ExportQuality,
    onSelect: (Int?) -> Unit,
    onLevel: (ExportQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.qualityTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = s.qualityResolution,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                QualityRow(label = s.qualityOriginal, hint = null, selected = selected == null) { onSelect(null) }
                options.forEach { side ->
                    QualityRow(label = "${side}p", hint = null, selected = selected == side) { onSelect(side) }
                }
                if (options.isEmpty()) {
                    Text(
                        text = s.qualityUnavailable,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = s.qualityLevelTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                QualityRow(
                    label = s.qualityStandard,
                    hint = s.qualityStandardHint,
                    selected = level == ExportQuality.STANDARD
                ) { onLevel(ExportQuality.STANDARD) }
                QualityRow(
                    label = s.qualityHigh,
                    hint = s.qualityHighHint,
                    selected = level == ExportQuality.HIGH
                ) { onLevel(ExportQuality.HIGH) }
                QualityRow(
                    label = s.qualityMax,
                    hint = s.qualityMaxHint,
                    selected = level == ExportQuality.MAX
                ) { onLevel(ExportQuality.MAX) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.close) }
        }
    )
}

@Composable
private fun QualityRow(
    label: String,
    hint: String?,
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
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
