package com.brenninho.trimly.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.ColorMath
import com.brenninho.trimly.model.FilterGroup
import com.brenninho.trimly.model.VideoFilter
import java.awt.Frame
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PanelTab(val label: String) {
    EDIT("Edit"),
    FILTERS("Filters"),
    EFFECTS("Effects"),
    ADJUST("Adjust"),
    OUTPUT("Output")
}

@Composable
fun FrameWindowScope.DesktopApp(initial: File?) {
    val scope = rememberCoroutineScope()
    val tools = remember { Ffmpeg.locate() }
    var controller by remember { mutableStateOf<EditorController?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var recents by remember { mutableStateOf(Recents.load()) }

    val open: (File) -> Unit = { file ->
        val available = tools
        if (available == null) {
            message = "FFmpeg was not found. Put ffmpeg.exe and ffprobe.exe next to the app or add them to your PATH."
        } else {
            scope.launch {
                loading = true
                val info = Ffmpeg.probe(available, file)
                loading = false
                if (info == null) {
                    message = "Could not read this video."
                } else {
                    controller = EditorController(scope, available, file, info)
                    recents = Recents.add(file)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        initial?.let(open)
    }

    val current = controller
    if (current == null) {
        HomeScreen(
            loading = loading,
            ffmpegMissing = tools == null,
            recents = recents,
            onBrowse = { pickVideo(window)?.let(open) },
            onOpen = open
        )
    } else {
        EditorScreen(
            controller = current,
            window = window,
            onClose = { controller = null }
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Trimly") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    loading: Boolean,
    ffmpegMissing: Boolean,
    recents: List<File>,
    onBrowse: () -> Unit,
    onOpen: (File) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(520.dp)
                .padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Trimly",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Free video editor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (ffmpegMissing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "FFmpeg was not found. Put ffmpeg.exe and ffprobe.exe next to the app, in a folder named ffmpeg, or add them to your PATH.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = onBrowse,
                enabled = !loading && !ffmpegMissing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open video")
            }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (recents.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                recents.forEach { file ->
                    RecentRow(file = file, enabled = !loading && !ffmpegMissing, onClick = { onOpen(file) })
                }
            }
        }
    }
}

@Composable
private fun RecentRow(
    file: File,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.parent.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditorScreen(
    controller: EditorController,
    window: Frame,
    onClose: () -> Unit
) {
    var tab by remember { mutableStateOf(PanelTab.EDIT) }
    var confirmClose by remember { mutableStateOf(false) }

    LaunchedEffect(controller) {
        controller.loadStrip()
    }

    LaunchedEffect(controller, controller.positionMs) {
        delay(120)
        controller.renderFrame()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EditorTopBar(
            controller = controller,
            onClose = { if (controller.hasEdits) confirmClose = true else onClose() },
            onExport = {
                val target = chooseSaveFile(window, controller.file)
                if (target != null) controller.startExport(target)
            }
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                PreviewPane(
                    controller = controller,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(12.dp))
                TimelineSection(controller)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            SidePanel(
                controller = controller,
                tab = tab,
                onTab = { tab = it },
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
            )
        }
    }

    ExportDialogs(controller)

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("Discard changes?") },
            text = { Text("Your trim and edits will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    onClose()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text("Keep editing") }
            }
        )
    }
}

@Composable
private fun EditorTopBar(
    controller: EditorController,
    onClose: () -> Unit,
    onExport: () -> Unit
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = controller.file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${controller.info.width} x ${controller.info.height} · ${formatTime(controller.info.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = controller::reset, enabled = controller.hasEdits) {
                Text("Reset")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onExport, enabled = controller.canExport) {
                Text("Export")
            }
        }
    }
}

@Composable
private fun PreviewPane(
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val frame = controller.frame
    val options = controller.options

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(Color.Black)
    ) {
        if (frame != null) {
            val aspect = (frame.width.toFloat() / frame.height.toFloat()).coerceAtLeast(0.1f)
            val turned = options.rotationDegrees % 180 != 0
            val availableWidth = maxWidth.value
            val availableHeight = maxHeight.value
            val viewHeight = if (turned) {
                minOf(availableWidth, availableHeight / aspect)
            } else {
                minOf(availableWidth, availableHeight * aspect) / aspect
            }
            val viewWidth = viewHeight * aspect
            val degrees = options.rotationDegrees.toFloat()
            val flipped = options.flipHorizontal
            val filter = remember(options) { colorFilterFor(ColorMath.combined(options)) }

            Image(
                bitmap = frame,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = filter,
                modifier = Modifier
                    .size(viewWidth.dp, viewHeight.dp)
                    .graphicsLayer {
                        rotationZ = if (flipped) degrees else -degrees
                        scaleX = if (flipped) -1f else 1f
                    }
            )
        }

        if (controller.rendering) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun TimelineSection(controller: EditorController) {
    val duration = controller.info.durationMs.toFloat().coerceAtLeast(1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimeLabel("Start", formatTime(controller.startMs), TextAlign.Start)
            TimeLabel("Playhead", formatTime(controller.positionMs - controller.startMs), TextAlign.Center)
            TimeLabel("Length", formatTime(controller.trimmedMs), TextAlign.End)
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            controller.strip.forEach { image ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        RangeSlider(
            value = controller.startMs.toFloat()..controller.endMs.toFloat(),
            onValueChange = { range ->
                controller.setRange(range.start.toLong(), range.endInclusive.toLong())
            },
            valueRange = 0f..duration,
            modifier = Modifier.fillMaxWidth()
        )

        if (controller.endMs > controller.startMs) {
            Slider(
                value = controller.positionMs.toFloat(),
                onValueChange = { controller.seek(it.toLong()) },
                valueRange = controller.startMs.toFloat()..controller.endMs.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
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

@Composable
private fun SidePanel(
    controller: EditorController,
    tab: PanelTab,
    onTab: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        ScrollableTabRow(
            selectedTabIndex = tab.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent
        ) {
            PanelTab.entries.forEach { item ->
                Tab(
                    selected = item == tab,
                    onClick = { onTab(item) },
                    text = { Text(item.label) }
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (tab) {
                PanelTab.EDIT -> EditPanel(controller)
                PanelTab.FILTERS -> FilterPanel(controller, FilterGroup.FILTER)
                PanelTab.EFFECTS -> FilterPanel(controller, FilterGroup.EFFECT)
                PanelTab.ADJUST -> AdjustPanel(controller)
                PanelTab.OUTPUT -> OutputPanel(controller)
            }
        }
    }
}

@Composable
private fun EditPanel(controller: EditorController) {
    val options = controller.options
    val rotated = options.rotationDegrees % 360 != 0

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Trim", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "Move the playhead, then set where the cut starts or ends.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = controller::setStartAtPlayhead, modifier = Modifier.weight(1f)) {
                Text("Set start")
            }
            OutlinedButton(onClick = controller::setEndAtPlayhead, modifier = Modifier.weight(1f)) {
                Text("Set end")
            }
        }

        HorizontalDivider()

        Text("Transform", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = rotated,
                onClick = controller::rotate,
                label = { Text(if (rotated) "Rotate ${options.rotationDegrees}°" else "Rotate") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
            FilterChip(
                selected = options.flipHorizontal,
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
        }

        HorizontalDivider()

        Text("Audio", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        FilterChip(
            selected = options.muted,
            onClick = controller::toggleMute,
            enabled = controller.info.hasAudio,
            label = {
                Text(
                    when {
                        !controller.info.hasAudio -> "No audio track"
                        options.muted -> "Muted"
                        else -> "Mute"
                    }
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (options.muted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        )

        HorizontalDivider()

        OutlinedButton(
            onClick = { openWithSystem(controller.file) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play original in system player")
        }
    }
}

@Composable
private fun FilterPanel(
    controller: EditorController,
    group: FilterGroup
) {
    val options = controller.options
    val base = controller.strip.getOrNull(controller.strip.size / 2) ?: controller.strip.firstOrNull { it != null }
    val filters = remember(group) {
        listOf(VideoFilter.NONE) + VideoFilter.entries.filter { it.group == group && it != VideoFilter.NONE }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(88.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filters, key = { it.name }) { filter ->
                FilterCard(
                    filter = filter,
                    selected = options.filter == filter,
                    frame = base,
                    onClick = { controller.setFilter(filter) }
                )
            }
        }

        val adjustable = options.filter != VideoFilter.NONE && options.filter.adjustable
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
        ) {
            if (adjustable) {
                Text(
                    text = "Intensity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp)
                )
                Slider(
                    value = options.filterIntensity,
                    onValueChange = controller::setIntensity,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(options.filterIntensity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp)
                )
            } else {
                Text(
                    text = if (options.filter == VideoFilter.NONE) "Pick one to apply it" else "This effect has no intensity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    filter: VideoFilter,
    selected: Boolean,
    frame: ImageBitmap?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (selected) Modifier.border(2.dp, primary, shape) else Modifier)
        ) {
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilterFor(ColorMath.rowMajor(filter.grade)),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = filter.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdjustPanel(controller: EditorController) {
    val options = controller.options

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Adjustment.entries.forEach { kind ->
            val value = options.adjustment(kind)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = kind.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(84.dp)
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = { controller.setAdjustment(kind, it.roundToInt()) },
                    valueRange = -100f..100f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
        TextButton(onClick = controller::resetAdjustments, enabled = options.hasAdjustments) {
            Text("Reset adjustments")
        }
    }
}

@Composable
private fun OutputPanel(controller: EditorController) {
    val options = controller.options

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Resolution", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        QualityRow(
            label = "Original (${controller.info.width} x ${controller.info.height})",
            selected = options.shortSide == null
        ) { controller.setQuality(null) }
        controller.availableShortSides.forEach { side ->
            QualityRow(label = "${side}p", selected = options.shortSide == side) { controller.setQuality(side) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Format", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "MP4 with H.264 video and AAC audio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
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
            text = { Text(status.file.absolutePath) },
            confirmButton = {
                TextButton(onClick = {
                    showInFolder(status.file)
                    controller.dismissExport()
                }) { Text("Show in folder") }
            },
            dismissButton = {
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

private fun colorFilterFor(matrix: FloatArray?): ColorFilter? =
    matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
