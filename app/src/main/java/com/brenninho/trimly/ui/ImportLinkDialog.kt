package com.brenninho.trimly.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.data.DownloadFailure
import com.brenninho.trimly.data.ImportState
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
    val valid = VideoDownloader.normalize(text) != null

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
                        isError = state is ImportState.Failed,
                        placeholder = { Text(s.linkPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { if (valid) onImport(text) }),
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
                    if (state is ImportState.Failed) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorText(s, state),
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
                TextButton(onClick = { onImport(text) }, enabled = valid) {
                    Text(s.linkImport)
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
private fun DownloadStatus(state: ImportState.Downloading) {
    val s = LocalStrings.current
    val total = state.total
    val target = if (total != null && total > 0L) {
        (state.bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    val progress by animateFloatAsState(
        targetValue = target ?: 0f,
        animationSpec = tween(200),
        label = "downloadProgress"
    )

    Column {
        Text(
            text = state.name ?: s.linkDownloading,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            text = s.linkProgress(sizeLabel(state.bytes), total?.let { sizeLabel(it) }),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun errorText(s: AppStrings, state: ImportState.Failed): String = when (state.reason) {
    DownloadFailure.INVALID_URL -> s.linkErrInvalid
    DownloadFailure.INSECURE -> s.linkErrInsecure
    DownloadFailure.NOT_VIDEO -> s.linkErrNotVideo
    DownloadFailure.PLAYLIST -> s.linkErrPlaylist
    DownloadFailure.TOO_LARGE -> s.linkErrTooLarge
    DownloadFailure.NO_SPACE -> s.linkErrNoSpace
    DownloadFailure.HTTP -> state.detail?.toIntOrNull()?.let { s.linkErrHttp(it.toString()) } ?: s.linkErrNetwork
    DownloadFailure.NETWORK -> s.linkErrNetwork
}

private fun sizeLabel(bytes: Long): String {
    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1024.0) {
        "%.2f GB".format(megabytes / 1024.0)
    } else {
        "%.1f MB".format(megabytes)
    }
}
