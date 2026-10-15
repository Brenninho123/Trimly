package com.brenninho.trimly.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.data.DownloadFailure
import com.brenninho.trimly.data.ImportStage
import com.brenninho.trimly.data.ImportState
import com.brenninho.trimly.data.LinkKind
import com.brenninho.trimly.data.VideoDownloader
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings

@Composable
fun ImportLinkDialog(
    state: ImportState,
    onImport: (String) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf("") }
    val downloading = state is ImportState.Downloading
    val failed = state as? ImportState.Failed

    LaunchedEffect(failed?.url) {
        if (failed != null && text.isBlank()) text = failed.url
    }

    val info = remember(text) { VideoDownloader.inspect(text) }
    val blocked = info?.kind == LinkKind.UNSUPPORTED
    val valid = info != null && !blocked
    val retrying = failed != null && text.trim() == failed.url

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(s.linkTitle) },
        text = {
            if (state is ImportState.Downloading) {
                DownloadStatus(state)
            } else {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        isError = failed != null || blocked,
                        placeholder = { Text(s.linkPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { if (valid) onImport(text.trim()) }),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val pasted = clipboard.getText()?.text.orEmpty()
                                    if (pasted.isNotBlank()) {
                                        text = VideoDownloader.extractUrl(pasted) ?: pasted.trim()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = s.linkPaste
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = info != null,
                        enter = fadeIn(tween(160)),
                        exit = fadeOut(tween(120))
                    ) {
                        val kind = info?.kind ?: LinkKind.DIRECT
                        KindBadge(
                            icon = kindIcon(kind),
                            label = if (kind == LinkKind.UNSUPPORTED) {
                                s.linkErrSite(info?.site.orEmpty())
                            } else {
                                s.linkKind(kind)
                            },
                            warning = kind == LinkKind.UNSUPPORTED,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (failed != null && !blocked) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorText(s, failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = s.linkHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = s.linkNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!downloading) {
                TextButton(onClick = { onImport(text.trim()) }, enabled = valid) {
                    Text(if (retrying) s.linkRetry else s.linkImport)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = if (downloading) onCancel else onDismiss) {
                Text(s.cancel)
            }
        }
    )
}

@Composable
private fun KindBadge(
    icon: ImageVector,
    label: String,
    warning: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DownloadStatus(state: ImportState.Downloading) {
    val s = LocalStrings.current
    val target = state.fraction
    val progress by animateFloatAsState(
        targetValue = target ?: 0f,
        animationSpec = tween(200),
        label = "downloadProgress"
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.kind?.let { kind ->
                Icon(
                    imageVector = kindIcon(kind),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = state.name ?: s.linkDownloading,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = s.linkStage(state.stage),
            style = MaterialTheme.typography.bodySmall,
            color = if (state.stage == ImportStage.RETRYING) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(Modifier.height(12.dp))
        if (target != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = detailLine(s, state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun detailLine(s: AppStrings, state: ImportState.Downloading): String {
    if (state.stage == ImportStage.CONVERTING) {
        val percent = state.fraction?.let { "${(it * 100).toInt()}%" }
        return listOfNotNull(percent, state.bytes.takeIf { it > 0L }?.let { sizeLabel(it) }).joinToString(" · ")
    }
    if (state.stage == ImportStage.CONNECTING || state.stage == ImportStage.RESOLVING) return ""
    val parts = ArrayList<String>()
    parts.add(s.linkProgress(sizeLabel(state.bytes), state.total?.let { sizeLabel(it) }))
    state.bytesPerSecond?.takeIf { it > 0L }?.let { parts.add(s.linkSpeed(sizeLabel(it))) }
    state.etaMs?.let { parts.add(s.linkEta(durationLabel(it))) }
    return parts.joinToString(" · ")
}

private fun kindIcon(kind: LinkKind): ImageVector = when (kind) {
    LinkKind.DIRECT -> Icons.Filled.Movie
    LinkKind.GOOGLE_DRIVE, LinkKind.DROPBOX, LinkKind.ONEDRIVE -> Icons.Filled.Cloud
    LinkKind.GITHUB -> Icons.Filled.Code
    LinkKind.IMGUR -> Icons.Filled.Image
    LinkKind.HLS, LinkKind.DASH -> Icons.Filled.LiveTv
    LinkKind.PAGE -> Icons.Filled.Language
    LinkKind.UNSUPPORTED -> Icons.Filled.Block
}

private fun errorText(s: AppStrings, state: ImportState.Failed): String = when (state.reason) {
    DownloadFailure.INVALID_URL -> s.linkErrInvalid
    DownloadFailure.INSECURE -> s.linkErrInsecure
    DownloadFailure.NOT_VIDEO -> s.linkErrNotVideo
    DownloadFailure.NO_VIDEO_IN_PAGE -> s.linkErrNoVideoInPage
    DownloadFailure.UNSUPPORTED_SITE -> s.linkErrSite(state.detail.orEmpty())
    DownloadFailure.LIVE_STREAM -> s.linkErrLive
    DownloadFailure.STREAM -> s.linkErrStream
    DownloadFailure.TOO_LARGE -> s.linkErrTooLarge
    DownloadFailure.NO_SPACE -> s.linkErrNoSpace
    DownloadFailure.HTTP -> state.detail?.toIntOrNull()?.let { s.linkErrHttp(it.toString()) } ?: s.linkErrNetwork
    DownloadFailure.NETWORK -> s.linkErrNetwork
}

private fun sizeLabel(bytes: Long): String {
    val kilobytes = bytes / 1024.0
    val megabytes = kilobytes / 1024.0
    return when {
        megabytes >= 1024.0 -> "%.2f GB".format(megabytes / 1024.0)
        megabytes >= 1.0 -> "%.1f MB".format(megabytes)
        else -> "%.0f KB".format(kilobytes)
    }
}

private fun durationLabel(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(1L)
    return when {
        seconds >= 3600L -> "%dh %02dm".format(seconds / 3600L, (seconds % 3600L) / 60L)
        seconds >= 60L -> "%dm %02ds".format(seconds / 60L, seconds % 60L)
        else -> "${seconds}s"
    }
}
