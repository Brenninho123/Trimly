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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.Clip
import com.brenninho.trimly.model.ColorMath
import com.brenninho.trimly.model.VideoFilter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FRAME_COUNT = 10
private const val SEEK_STEP_MS = 5000L

@Composable
fun EditorScreen(
    clip: Clip,
    onBack: () -> Unit
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
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
    val exportStatus = state.export
    val exporting = exportStatus is ExportStatus.Running

    val exoPlayer = remember(clip.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(clip.uri))
            prepare()
        }
    }

    var entered by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var looping by rememberSaveable { mutableStateOf(false) }
    var compareHeld by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<PanelTab?>(null) }
    var lastPanel by remember { mutableStateOf(PanelTab.FILTERS) }
    var menuCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var menuExpanded by rememberSaveable { mutableStateOf(true) }
    var flashDirection by remember { mutableIntStateOf(0) }
    var flashTick by remember { mutableIntStateOf(0) }

    val menuCategory = MenuCategory.entries[menuCategoryIndex.coerceIn(0, MenuCategory.entries.lastIndex)]
    val range by rememberUpdatedState(trimStart to trimEnd)
    val loopEnabled by rememberUpdatedState(looping)
    val layerPaint = remember { Paint() }
    val previewMatrix = remember(options) { ColorMath.combined(options) }
    val shownMatrix = if (compareHeld) null else previewMatrix
    val playerAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(450),
        label = "playerAlpha"
    )

    val frames = remember(clip.uri) {
        mutableStateListOf<ImageBitmap?>().apply { repeat(FRAME_COUNT) { add(null) } }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && loopEnabled) {
                    exoPlayer.seekTo(range.first)
                    exoPlayer.play()
                }
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

    LaunchedEffect(Unit) {
        entered = true
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.seekTo(trimStart)
        while (true) {
            val current = exoPlayer.currentPosition
            positionMs = current
            if (exoPlayer.isPlaying && current >= range.second) {
                exoPlayer.seekTo(range.first)
                if (!loopEnabled) exoPlayer.pause()
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

    LaunchedEffect(panel) {
        panel?.let { lastPanel = it }
    }

    LaunchedEffect(flashTick) {
        if (flashTick > 0) {
            delay(650)
            flashDirection = 0
        }
    }

    val tick: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val togglePlay: () -> Unit = {
        tick()
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

    val seekBy: (Long) -> Unit = { delta ->
        exoPlayer.seekTo(state.clip.coercePosition(exoPlayer.currentPosition + delta))
    }

    val latestToggle by rememberUpdatedState(togglePlay)
    val latestSeekBy by rememberUpdatedState(seekBy)

    val announce: (String) -> Unit = { message ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    val setStartHere: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.setStartAt(exoPlayer.currentPosition)
        announce(s.startSetTo(formatTime(viewModel.state.value.clip.startMs)))
    }

    val setEndHere: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.setEndAt(exoPlayer.currentPosition)
        announce(s.endSetTo(formatTime(viewModel.state.value.clip.endMs)))
    }

    val requestBack: () -> Unit = {
        if (!exporting) {
            if (state.hasEdits) showDiscard = true else onBack()
        }
    }

    BackHandler(onBack = requestBack)
    BackHandler(enabled = panel != null) { panel = null }

    val chips = remember(options, state.clip, s) {
        buildList {
            if (state.clip.isTrimmed) {
                add(EditChip("trim", s.chipTrim(formatTime(state.clip.trimmedDurationMs))) { viewModel.resetTrim() })
            }
            if (options.filter != VideoFilter.NONE) {
                val label = if (options.filter.adjustable) {
                    "${s.filterName(options.filter)} ${(options.filterIntensity * 100).roundToInt()}%"
                } else {
                    s.filterName(options.filter)
                }
                add(EditChip("filter", label) { viewModel.setFilter(VideoFilter.NONE) })
            }
            if (options.hasAdjustments) {
                add(EditChip("adjust", s.toolAdjust) { viewModel.resetAdjustments() })
            }
            if (options.rotationDegrees % 360 != 0) {
                add(EditChip("rotate", s.rotateLabel(options.rotationDegrees)) {
                    viewModel.updateOptions { it.copy(rotationDegrees = 0) }
                })
            }
            if (options.flipHorizontal) {
                add(EditChip("flip", s.flip) { viewModel.updateOptions { it.copy(flipHorizontal = false) } })
            }
            if (options.muted) {
                add(EditChip("mute", s.muted) { viewModel.updateOptions { it.copy(muted = false) } })
            }
            options.shortSide?.let { side ->
                add(EditChip("quality", "${side}p") { viewModel.setQuality(null) })
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.editorTitle)
                        EditedDot(visible = state.hasEdits)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo && !exporting) {
                        Icon(Icons.Filled.Undo, contentDescription = s.undo)
                    }
                    IconButton(onClick = viewModel::redo, enabled = state.canRedo && !exporting) {
                        Icon(Icons.Filled.Redo, contentDescription = s.redo)
                    }
                    Button(
                        onClick = viewModel::startExport,
                        enabled = state.canExport,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    ) {
                        Text(s.export)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = exporting) {
                LinearProgressIndicator(
                    progress = { (exportStatus as? ExportStatus.Running)?.progress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { alpha = playerAlpha }
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
                val matrix = shownMatrix

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
                        .pointerInput(exporting) {
                            if (exporting) return@pointerInput
                            detectTapGestures(
                                onTap = { latestToggle() },
                                onDoubleTap = { offset ->
                                    val forward = offset.x > size.width / 2f
                                    latestSeekBy(if (forward) SEEK_STEP_MS else -SEEK_STEP_MS)
                                    flashDirection = if (forward) 1 else -1
                                    flashTick += 1
                                }
                            )
                        }
                )

                OverlayVisibility(visible = !playing && !buffering) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = s.play,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                OverlayVisibility(visible = buffering) {
                    CircularProgressIndicator(color = Color.White)
                }

                OverlayVisibility(
                    visible = flashDirection == -1,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    SeekBubble(forward = false)
                }

                OverlayVisibility(
                    visible = flashDirection == 1,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    SeekBubble(forward = true)
                }

                OverlayVisibility(
                    visible = state.sourceWidth > 0,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    InfoBadge(
                        text = "${state.sourceWidth} x ${state.sourceHeight}",
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OverlayVisibility(
                    visible = previewMatrix != null,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    CompareButton(
                        onHold = { compareHeld = it },
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            EditorEntrance(visible = entered, delayMillis = 80) {
                TransportBar(
                    playing = playing,
                    looping = looping,
                    enabled = !exporting,
                    positionLabel = formatTime(state.clip.relativePosition(positionMs)),
                    onToStart = {
                        tick()
                        exoPlayer.seekTo(trimStart)
                    },
                    onBack = {
                        tick()
                        seekBy(-SEEK_STEP_MS)
                    },
                    onToggle = togglePlay,
                    onForward = {
                        tick()
                        seekBy(SEEK_STEP_MS)
                    },
                    onToEnd = {
                        tick()
                        exoPlayer.seekTo(trimEnd)
                    },
                    onLoop = {
                        tick()
                        looping = !looping
                    }
                )
            }

            Spacer(Modifier.height(4.dp))

            EditorEntrance(visible = entered, delayMillis = 140) {
                TrimReadout(
                    startMs = trimStart,
                    lengthMs = trimmed,
                    endMs = trimEnd,
                    enabled = !exporting,
                    onSetStart = setStartHere,
                    onSetEnd = setEndHere
                )
            }

            Spacer(Modifier.height(8.dp))

            EditorEntrance(visible = entered, delayMillis = 200) {
                TrimTimeline(
                    durationMs = clip.durationMs,
                    startMs = trimStart,
                    endMs = trimEnd,
                    positionMs = positionMs,
                    frames = frames,
                    enabled = !exporting,
                    onDragStarted = {
                        exoPlayer.pause()
                        viewModel.beginRangeEdit()
                    },
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
            }

            EditChipsRow(chips = chips, enabled = !exporting)

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            EditorEntrance(visible = entered, delayMillis = 260) {
                AnimatedContent(
                    targetState = panel != null,
                    transitionSpec = {
                        (slideInVertically(tween(280)) { it / 2 } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically(tween(220)) { it / 2 } + fadeOut(tween(160)))
                    },
                    label = "bottomArea"
                ) { showPanel ->
                    if (showPanel) {
                        StylePanel(
                            tab = panel ?: lastPanel,
                            options = options,
                            previewFrame = frames.getOrNull(FRAME_COUNT / 2) ?: frames.firstOrNull { it != null },
                            enabled = !exporting,
                            onTabChange = {
                                tick()
                                panel = it
                            },
                            onFilter = {
                                tick()
                                viewModel.setFilter(it)
                            },
                            onIntensity = viewModel::setFilterIntensity,
                            onAdjust = viewModel::setAdjustment,
                            onResetAdjust = viewModel::resetAdjustments,
                            onClose = { panel = null }
                        )
                    } else {
                        EditorMenu(
                            category = menuCategory,
                            expanded = menuExpanded,
                            options = options,
                            hasEdits = state.hasEdits,
                            enabled = !exporting,
                            onCategoryChange = {
                                tick()
                                menuCategoryIndex = it.ordinal
                            },
                            onExpandedChange = { menuExpanded = it },
                            onRotate = {
                                tick()
                                viewModel.rotate()
                            },
                            onFlip = {
                                tick()
                                viewModel.toggleFlip()
                            },
                            onMute = {
                                tick()
                                viewModel.toggleMute()
                            },
                            onQuality = { showQuality = true },
                            onFilters = { panel = PanelTab.FILTERS },
                            onEffects = { panel = PanelTab.EFFECTS },
                            onAdjust = { panel = PanelTab.ADJUST },
                            onReset = {
                                viewModel.reset()
                                exoPlayer.seekTo(0L)
                            },
                            onSoon = { announce(s.comingSoon(it)) }
                        )
                    }
                }
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
            title = { Text(s.discardTitle) },
            text = { Text(s.discardBody) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text(s.discardConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text(s.keepEditing) }
            }
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
