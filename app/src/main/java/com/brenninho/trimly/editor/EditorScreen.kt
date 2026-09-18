@file:OptIn(ExperimentalMaterial3Api::class)

package com.brenninho.trimly.editor

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.view.Gravity
import android.view.TextureView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.brenninho.trimly.data.FrameExtractor
import com.brenninho.trimly.model.Clip
import com.brenninho.trimly.model.ColorMath
import java.util.Locale
import kotlinx.coroutines.delay

private const val FRAME_COUNT = 10

@Composable
fun EditorScreen(
    clip: Clip,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val application = context.applicationContext as Application
    val viewModel: EditorViewModel = viewModel(
        key = clip.uri.toString(),
        factory = EditorViewModelFactory(application, clip)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val options = state.options
    val trimStart = state.clip.startMs
    val trimEnd = state.clip.endMs
    val trimmed = state.clip.trimmedDurationMs
    val exporting = state.export is ExportStatus.Running

    val exoPlayer = remember(clip.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(clip.uri))
            prepare()
        }
    }

    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var showQuality by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<PanelTab?>(null) }

    val range by rememberUpdatedState(trimStart to trimEnd)
    val layerPaint = remember { Paint() }
    val previewMatrix = remember(options) { ColorMath.combined(options) }

    val frames = remember(clip.uri) {
        mutableStateListOf<ImageBitmap?>().apply { repeat(FRAME_COUNT) { add(null) } }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    viewModel.setSourceSize(videoSize.width, videoSize.height)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        exoPlayer.pause()
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.seekTo(trimStart)
        while (true) {
            val current = exoPlayer.currentPosition
            positionMs = current
            if (exoPlayer.isPlaying && current >= range.second) {
                exoPlayer.pause()
                exoPlayer.seekTo(range.first)
            }
            delay(33)
        }
    }

    LaunchedEffect(clip.uri) {
        FrameExtractor.extract(context, clip.uri, clip.durationMs, FRAME_COUNT) { index, bitmap ->
            frames[index] = bitmap
        }
    }

    LaunchedEffect(exoPlayer, options.muted) {
        exoPlayer.volume = if (options.muted) 0f else 1f
    }

    val togglePlay: () -> Unit = {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            val (start, end) = range
            if (exoPlayer.currentPosition < start || exoPlayer.currentPosition >= end - 50) {
                exoPlayer.seekTo(start)
            }
            exoPlayer.play()
        }
    }

    val requestBack: () -> Unit = {
        if (!exporting) {
            if (state.hasEdits) showDiscard = true else onBack()
        }
    }

    BackHandler(onBack = requestBack)
    BackHandler(enabled = panel != null) { panel = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = viewModel::startExport,
                        enabled = state.canExport,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                val turned = options.rotationDegrees % 180 != 0
                val availableWidth = maxWidth.value
                val availableHeight = maxHeight.value
                val ratio = videoAspect.coerceAtLeast(0.1f)

                val viewHeight = if (turned) {
                    minOf(availableWidth, availableHeight / ratio)
                } else {
                    minOf(availableWidth, availableHeight * ratio) / ratio
                }
                val viewWidth = viewHeight * ratio
                val footprintWidth = if (turned) viewHeight else viewWidth
                val footprintHeight = if (turned) viewWidth else viewHeight

                val viewWidthPx = with(density) { viewWidth.dp.roundToPx() }.coerceAtLeast(1)
                val viewHeightPx = with(density) { viewHeight.dp.roundToPx() }.coerceAtLeast(1)
                val turnDegrees = options.rotationDegrees.toFloat()
                val flipped = options.flipHorizontal
                val matrix = previewMatrix

                AndroidView(
                    factory = { ctx ->
                        val texture = TextureView(ctx)
                        exoPlayer.setVideoTextureView(texture)
                        FrameLayout(ctx).apply {
                            clipChildren = false
                            clipToPadding = false
                            addView(texture, FrameLayout.LayoutParams(1, 1, Gravity.CENTER))
                        }
                    },
                    update = { frame ->
                        val texture = frame.getChildAt(0) as TextureView
                        val params = texture.layoutParams as FrameLayout.LayoutParams
                        if (params.width != viewWidthPx || params.height != viewHeightPx) {
                            params.width = viewWidthPx
                            params.height = viewHeightPx
                            texture.layoutParams = params
                        }
                        texture.rotation = if (flipped) turnDegrees else -turnDegrees
                        texture.scaleX = if (flipped) -1f else 1f
                        layerPaint.colorFilter = if (matrix != null) {
                            ColorMatrixColorFilter(ColorMatrix(matrix))
                        } else {
                            null
                        }
                        texture.setLayerPaint(layerPaint)
                    },
                    modifier = Modifier.size(footprintWidth.dp, footprintHeight.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !exporting, onClick = togglePlay)
                )

                if (!playing) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = togglePlay, enabled = !exporting) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play"
                    )
                }
                Text(
                    text = "${formatTime((positionMs - trimStart).coerceIn(0L, trimmed))} / ${formatTime(trimmed)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TimeLabel(label = "Start", value = formatTime(trimStart), alignment = TextAlign.Start)
                TimeLabel(label = "Length", value = formatTime(trimmed), alignment = TextAlign.Center)
                TimeLabel(label = "End", value = formatTime(trimEnd), alignment = TextAlign.End)
            }

            Spacer(Modifier.height(8.dp))

            TrimTimeline(
                durationMs = clip.durationMs,
                startMs = trimStart,
                endMs = trimEnd,
                positionMs = positionMs,
                frames = frames,
                enabled = !exporting,
                onDragStarted = { exoPlayer.pause() },
                onStartChange = { ms ->
                    viewModel.setRange(ms, trimEnd)
                    exoPlayer.seekTo(ms)
                },
                onEndChange = { ms ->
                    viewModel.setRange(trimStart, ms)
                    exoPlayer.seekTo(ms)
                },
                onSeek = { ms -> exoPlayer.seekTo(ms) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))

            val activePanel = panel
            if (activePanel != null) {
                StylePanel(
                    tab = activePanel,
                    options = options,
                    previewFrame = frames.getOrNull(FRAME_COUNT / 2) ?: frames.firstOrNull { it != null },
                    enabled = !exporting,
                    onTabChange = { panel = it },
                    onFilter = viewModel::setFilter,
                    onIntensity = viewModel::setFilterIntensity,
                    onAdjust = viewModel::setAdjustment,
                    onResetAdjust = viewModel::resetAdjustments,
                    onClose = { panel = null }
                )
            } else {
                EditorMenu(
                    options = options,
                    hasEdits = state.hasEdits,
                    enabled = !exporting,
                    onRotate = viewModel::rotate,
                    onFlip = viewModel::toggleFlip,
                    onMute = viewModel::toggleMute,
                    onQuality = { showQuality = true },
                    onFilters = { panel = PanelTab.FILTERS },
                    onEffects = { panel = PanelTab.EFFECTS },
                    onAdjust = { panel = PanelTab.ADJUST },
                    onReset = {
                        viewModel.reset()
                        exoPlayer.seekTo(0L)
                    }
                )
            }
        }
    }

    ExportDialog(
        status = state.export,
        onCancel = viewModel::cancelExport,
        onDismiss = viewModel::dismissExport,
        onShare = { uri -> shareVideo(context, uri) },
        onOpen = { uri -> openVideo(context, uri) }
    )

    if (showQuality) {
        QualityDialog(
            selected = options.shortSide,
            options = state.availableShortSides,
            onSelect = { side ->
                viewModel.setQuality(side)
                showQuality = false
            },
            onDismiss = { showQuality = false }
        )
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your trim and edits will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("Keep editing") }
            }
        )
    }
}

@Composable
private fun TimeLabel(
    label: String,
    value: String,
    alignment: TextAlign
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = alignment
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = alignment
        )
    }
}

internal fun formatTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSeconds = safe / 1000
    val tenths = (safe % 1000) / 100
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d.%d".format(Locale.ROOT, hours, minutes, seconds, tenths)
    } else {
        "%02d:%02d.%d".format(Locale.ROOT, minutes, seconds, tenths)
    }
}

private fun shareVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun openVideo(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
    }
}
