package com.brenninho.trimly.editor

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brenninho.trimly.engine.ExportFailedException
import com.brenninho.trimly.engine.ExportFailure
import com.brenninho.trimly.engine.ExportSegment
import com.brenninho.trimly.engine.MediaSaver
import com.brenninho.trimly.engine.VideoExporter
import android.provider.OpenableColumns
import com.brenninho.trimly.engine.FrameSaver
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.AspectRatio
import com.brenninho.trimly.model.Datamosh
import com.brenninho.trimly.model.MusicTrack
import com.brenninho.trimly.model.Clip
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.ExportQuality
import com.brenninho.trimly.model.SpeedPresets
import com.brenninho.trimly.model.TextItem
import com.brenninho.trimly.model.VideoFilter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MIN_TRIM_MS = 500L
const val MAX_SEGMENTS = 10

private const val MAX_HISTORY = 50
private const val COALESCE_WINDOW_MS = 1000L

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Running(val progress: Float, val etaMs: Long? = null) : ExportStatus
    data class Done(
        val uri: Uri?,
        val location: String,
        val sizeBytes: Long = 0L,
        val elapsedMs: Long = 0L,
        val fastTrim: Boolean = false
    ) : ExportStatus
    data class Failed(val message: String?, val failure: ExportFailure? = null) : ExportStatus
}

enum class EditorNotice {
    ADD_FAILED,
    LIMIT,
    MUSIC_FAILED,
    SNAPSHOT_SAVED,
    SNAPSHOT_FAILED
}

enum class SplitResult {
    DONE,
    TOO_CLOSE,
    LIMIT
}

data class Segment(
    val id: Long,
    val clip: Clip
)

data class EditorState(
    val segments: List<Segment>,
    val selected: Int = 0,
    val options: ExportOptions = ExportOptions(),
    val selectedTextId: Long? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val notice: EditorNotice? = null,
    val export: ExportStatus = ExportStatus.Idle
) {
    val clip: Clip
        get() = segments[selected.coerceIn(0, segments.lastIndex)].clip

    val totalTrimmedMs: Long
        get() = segments.sumOf { it.clip.trimmedDurationMs }

    val outputDurationMs: Long
        get() = options.outputMs(totalTrimmedMs)

    val offsetBeforeSelectedMs: Long
        get() = segments.take(selected).sumOf { it.clip.trimmedDurationMs }

    val hasEdits: Boolean
        get() = segments.size > 1 || segments.any { it.clip.isTrimmed } || options != ExportOptions()

    val canExport: Boolean
        get() = totalTrimmedMs >= MIN_TRIM_MS &&
            segments.all { it.clip.trimmedDurationMs > 0L } &&
            export !is ExportStatus.Running

    val outputSize: Pair<Int, Int>
        get() {
            val turned = options.rotationDegrees % 180 != 0
            val width = if (turned) sourceHeight else sourceWidth
            val height = if (turned) sourceWidth else sourceHeight
            return options.aspect.crop(width, height)
        }

    val availableShortSides: List<Int>
        get() {
            val (width, height) = outputSize
            val shortSide = min(width, height)
            return if (shortSide <= 0) emptyList() else listOf(1080, 720, 480).filter { it < shortSide }
        }
}

private data class Snapshot(
    val segments: List<Segment>,
    val selected: Int,
    val options: ExportOptions
)

class EditorViewModel(
    private val app: Application,
    clip: Clip
) : ViewModel() {

    private val original = listOf(Segment(1L, clip))
    private var nextId = 2L
    private val exporter = VideoExporter(app)
    private val _state = MutableStateFlow(EditorState(segments = original))
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var job: Job? = null

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var pendingRange: Snapshot? = null
    private var coalesceKey: String? = null
    private var coalesceAt = 0L

    init {
        loadSourceSize(clip.uri)
    }

    fun setSourceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        _state.update {
            if (it.sourceWidth > 0 && it.sourceHeight > 0) {
                it
            } else {
                it.copy(sourceWidth = width, sourceHeight = height)
            }
        }
    }

    fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    fun beginRangeEdit() {
        pendingRange = snapshot(_state.value)
    }

    fun setRange(startMs: Long, endMs: Long) {
        val current = _state.value
        val next = current.clip.withRange(startMs, endMs)
        if (next == current.clip) return
        pendingRange?.let {
            record(it)
            pendingRange = null
        }
        _state.update { state -> replaceSelected(state) { it.withRange(startMs, endMs) } }
    }

    fun setStartAt(positionMs: Long) {
        commitClip { it.withStart(positionMs, MIN_TRIM_MS) }
    }

    fun setEndAt(positionMs: Long) {
        commitClip { it.withEnd(positionMs, MIN_TRIM_MS) }
    }

    fun resetTrim() {
        commitClip { it.reset() }
    }

    fun selectSegment(index: Int) {
        _state.update { if (index in it.segments.indices) it.copy(selected = index) else it }
    }

    fun addSegment(uri: Uri) {
        if (_state.value.segments.size >= MAX_SEGMENTS) {
            _state.update { it.copy(notice = EditorNotice.LIMIT) }
            return
        }
        viewModelScope.launch {
            val duration = withContext(Dispatchers.IO) {
                keepAccess(uri)
                readDuration(uri)
            }
            if (duration == null) {
                _state.update { it.copy(notice = EditorNotice.ADD_FAILED) }
                return@launch
            }
            record(snapshot(_state.value))
            coalesceKey = null
            val segment = Segment(nextId++, Clip(uri, duration))
            _state.update { it.copy(segments = it.segments + segment, selected = it.segments.size) }
        }
    }

    fun removeSegment(index: Int) {
        val current = _state.value
        if (current.segments.size <= 1 || index !in current.segments.indices) return
        record(snapshot(current))
        coalesceKey = null
        val remaining = current.segments.filterIndexed { position, _ -> position != index }
        val selected = when {
            current.selected > index -> current.selected - 1
            current.selected == index -> index
            else -> current.selected
        }.coerceIn(0, remaining.lastIndex)
        _state.update { it.copy(segments = remaining, selected = selected) }
        if (index == 0) refreshSourceSize()
    }

    fun splitAt(positionMs: Long): SplitResult {
        val current = _state.value
        if (current.segments.size >= MAX_SEGMENTS) return SplitResult.LIMIT
        val index = current.selected.coerceIn(0, current.segments.lastIndex)
        val segment = current.segments[index]
        val clip = segment.clip
        if (positionMs - clip.startMs < MIN_TRIM_MS || clip.endMs - positionMs < MIN_TRIM_MS) {
            return SplitResult.TOO_CLOSE
        }
        record(snapshot(current))
        coalesceKey = null
        val head = segment.copy(clip = clip.withRange(clip.startMs, positionMs))
        val tail = Segment(nextId++, clip.withRange(positionMs, clip.endMs))
        val list = current.segments.toMutableList()
        list[index] = head
        list.add(index + 1, tail)
        _state.update { it.copy(segments = list, selected = index + 1) }
        return SplitResult.DONE
    }

    fun duplicateSegment(index: Int) {
        val current = _state.value
        if (index !in current.segments.indices) return
        if (current.segments.size >= MAX_SEGMENTS) {
            _state.update { it.copy(notice = EditorNotice.LIMIT) }
            return
        }
        record(snapshot(current))
        coalesceKey = null
        val copy = Segment(nextId++, current.segments[index].clip)
        val list = current.segments.toMutableList()
        list.add(index + 1, copy)
        _state.update { it.copy(segments = list, selected = index + 1) }
    }

    fun moveSegment(from: Int, to: Int) {
        val current = _state.value
        if (from !in current.segments.indices || to !in current.segments.indices || from == to) return
        record(snapshot(current))
        coalesceKey = null
        val selectedId = current.segments[current.selected.coerceIn(0, current.segments.lastIndex)].id
        val list = current.segments.toMutableList()
        val moved = list.removeAt(from)
        list.add(to, moved)
        val selected = list.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        _state.update { it.copy(segments = list, selected = selected) }
        if (from == 0 || to == 0) refreshSourceSize()
    }

    fun updateOptions(key: String? = null, transform: (ExportOptions) -> ExportOptions) {
        val current = _state.value
        val next = transform(current.options)
        if (next == current.options) return

        val now = System.currentTimeMillis()
        val merge = key != null && key == coalesceKey && now - coalesceAt < COALESCE_WINDOW_MS
        if (!merge) record(snapshot(current))
        coalesceKey = key
        coalesceAt = now

        _state.update { it.copy(options = next) }
    }

    fun rotate() {
        updateOptions { it.rotated() }
    }

    fun toggleFlip() {
        updateOptions { it.copy(flipHorizontal = !it.flipHorizontal) }
    }

    fun toggleMute() {
        updateOptions { it.copy(muted = !it.muted) }
    }

    fun setQuality(shortSide: Int?) {
        updateOptions { it.copy(shortSide = shortSide) }
    }

    fun setQualityLevel(level: ExportQuality) {
        updateOptions { it.copy(quality = level) }
    }

    fun setFilter(filter: VideoFilter) {
        updateOptions { if (it.filter == filter) it else it.copy(filter = filter, filterIntensity = 1f) }
    }

    fun setFilterIntensity(value: Float) {
        updateOptions(key = "intensity") { it.copy(filterIntensity = value.coerceIn(0f, 1f)) }
    }

    fun setSpeed(value: Float) {
        val speed = value.coerceIn(SpeedPresets.MIN, SpeedPresets.MAX)
        updateOptions(key = "speed") { it.copy(speed = speed) }
    }

    fun setKeepPitch(enabled: Boolean) {
        updateOptions { it.copy(keepPitch = enabled) }
    }

    fun setVolume(percent: Int) {
        updateOptions(key = "volume") { it.copy(volume = percent.coerceIn(0, 200)) }
    }

    fun setFadeIn(ms: Long) {
        updateOptions { it.copy(fadeInMs = ms.coerceAtLeast(0L)) }
    }

    fun setFadeOut(ms: Long) {
        updateOptions { it.copy(fadeOutMs = ms.coerceAtLeast(0L)) }
    }

    fun setFadeVideo(enabled: Boolean) {
        updateOptions { it.copy(fadeVideo = enabled) }
    }

    fun resetAudio() {
        updateOptions { it.clearAudio() }
    }

    fun setAspect(aspect: AspectRatio) {
        updateOptions { it.copy(aspect = aspect) }
    }

    fun setMusic(uri: Uri) {
        viewModelScope.launch {
            val track = withContext(Dispatchers.IO) {
                keepAccess(uri)
                readMusic(uri)
            }
            if (track == null) {
                _state.update { it.copy(notice = EditorNotice.MUSIC_FAILED) }
                return@launch
            }
            val previous = _state.value.options.music
            updateOptions {
                it.copy(
                    music = track.copy(
                        volume = previous?.volume ?: track.volume,
                        loop = previous?.loop ?: track.loop
                    )
                )
            }
        }
    }

    fun removeMusic() {
        updateOptions { it.copy(music = null) }
    }

    fun setMusicVolume(percent: Int) {
        updateOptions(key = "music_volume") { options ->
            options.copy(music = options.music?.copy(volume = percent.coerceIn(0, 200)))
        }
    }

    fun setMusicLoop(loop: Boolean) {
        updateOptions { options -> options.copy(music = options.music?.copy(loop = loop)) }
    }

    fun setMusicOffset(ms: Long) {
        updateOptions(key = "music_offset") { options ->
            val music = options.music ?: return@updateOptions options
            options.copy(music = music.copy(startOffsetMs = ms.coerceIn(0L, (music.durationMs - 1000L).coerceAtLeast(0L))))
        }
    }

    fun setDatamosh(enabled: Boolean) {
        updateOptions { it.copy(datamosh = if (enabled) it.datamosh ?: Datamosh() else null) }
    }

    fun updateDatamosh(key: String? = null, transform: (Datamosh) -> Datamosh) {
        updateOptions(key = key) { options ->
            val current = options.datamosh ?: return@updateOptions options
            options.copy(datamosh = transform(current))
        }
    }

    fun saveSnapshot(positionMs: Long) {
        val current = _state.value
        val clip = current.clip
        val timeline = current.offsetBeforeSelectedMs + clip.relativePosition(positionMs)
        viewModelScope.launch {
            val notice = try {
                FrameSaver.save(app, clip.uri, positionMs, timeline, current.options)
                EditorNotice.SNAPSHOT_SAVED
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EditorNotice.SNAPSHOT_FAILED
            }
            _state.update { it.copy(notice = notice) }
        }
    }

    fun setAdjustment(kind: Adjustment, value: Int) {
        updateOptions(key = "adjust_${kind.name}") { it.withAdjustment(kind, value.coerceIn(-100, 100)) }
    }

    fun resetAdjustments() {
        updateOptions { it.clearAdjustments() }
    }

    fun addText(defaultText: String) {
        val id = nextId++
        val item = TextItem(id = id, text = defaultText)
        updateOptions { it.copy(texts = it.texts + item) }
        _state.update { it.copy(selectedTextId = id) }
    }

    fun selectText(id: Long?) {
        _state.update { it.copy(selectedTextId = id) }
    }

    fun updateText(id: Long, transform: (TextItem) -> TextItem) {
        updateOptions(key = "text_$id") { options ->
            options.copy(texts = options.texts.map { if (it.id == id) transform(it) else it })
        }
    }

    fun moveText(id: Long, x: Float, y: Float) {
        updateText(id) { it.copy(x = x.coerceIn(0.02f, 0.98f), y = y.coerceIn(0.02f, 0.98f)) }
    }

    fun removeText(id: Long) {
        updateOptions { options -> options.copy(texts = options.texts.filterNot { it.id == id }) }
        _state.update { if (it.selectedTextId == id) it.copy(selectedTextId = null) else it }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot(_state.value))
        coalesceKey = null
        restore(previous, canUndo = undoStack.isNotEmpty(), canRedo = true)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot(_state.value))
        coalesceKey = null
        restore(next, canUndo = true, canRedo = redoStack.isNotEmpty())
    }

    fun reset() {
        val current = _state.value
        if (!current.hasEdits) return
        record(snapshot(current))
        coalesceKey = null
        val firstChanged = current.segments.first().clip.uri != original.first().clip.uri
        _state.update {
            it.copy(segments = original, selected = 0, options = ExportOptions(), selectedTextId = null)
        }
        if (firstChanged) refreshSourceSize()
    }

    fun startExport() {
        val current = _state.value
        if (!current.canExport) return
        job?.cancel()
        _state.update { it.copy(export = ExportStatus.Running(0f)) }
        job = viewModelScope.launch {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val output = File(app.cacheDir, "Trimly_$stamp.mp4")
            try {
                val outcome = exporter.exportDetailed(
                    segments = current.segments.map { ExportSegment(it.clip.uri, it.clip.startMs, it.clip.endMs) },
                    options = resolveOptions(current),
                    output = output,
                    onProgress = { progress ->
                        _state.update { state ->
                            if (state.export is ExportStatus.Running) {
                                state.copy(export = ExportStatus.Running(progress.fraction, progress.etaMs))
                            } else {
                                state
                            }
                        }
                    }
                )
                val saved = MediaSaver.save(app, outcome.file)
                _state.update {
                    it.copy(
                        export = ExportStatus.Done(
                            uri = saved.uri,
                            location = saved.location,
                            sizeBytes = outcome.sizeBytes,
                            elapsedMs = outcome.elapsedMs,
                            fastTrim = outcome.fastTrim
                        )
                    )
                }
            } catch (e: CancellationException) {
                output.delete()
                throw e
            } catch (e: ExportFailedException) {
                output.delete()
                val detail = listOfNotNull(e.message, e.cause?.message).distinct().joinToString(": ")
                _state.update { it.copy(export = ExportStatus.Failed(detail.ifBlank { null }, e.failure)) }
            } catch (e: Exception) {
                output.delete()
                val detail = listOfNotNull(e.message, e.cause?.message).distinct().joinToString(": ")
                _state.update { it.copy(export = ExportStatus.Failed(detail.ifBlank { null })) }
            }
        }
    }

    fun cancelExport() {
        job?.cancel()
        _state.update { it.copy(export = ExportStatus.Idle) }
    }

    fun dismissExport() {
        _state.update { it.copy(export = ExportStatus.Idle) }
    }

    private fun snapshot(state: EditorState) = Snapshot(state.segments, state.selected, state.options)

    private fun restore(snapshot: Snapshot, canUndo: Boolean, canRedo: Boolean) {
        _state.update {
            it.copy(
                segments = snapshot.segments,
                selected = snapshot.selected.coerceIn(0, snapshot.segments.lastIndex),
                options = snapshot.options,
                selectedTextId = it.selectedTextId?.takeIf { id -> snapshot.options.texts.any { text -> text.id == id } },
                canUndo = canUndo,
                canRedo = canRedo
            )
        }
    }

    private fun replaceSelected(state: EditorState, transform: (Clip) -> Clip): EditorState {
        val index = state.selected.coerceIn(0, state.segments.lastIndex)
        val updated = state.segments.toMutableList()
        updated[index] = updated[index].copy(clip = transform(updated[index].clip))
        return state.copy(segments = updated)
    }

    private fun commitClip(transform: (Clip) -> Clip) {
        val current = _state.value
        val next = transform(current.clip)
        if (next == current.clip) return
        record(snapshot(current))
        coalesceKey = null
        _state.update { state -> replaceSelected(state, transform) }
    }

    private fun record(snapshot: Snapshot) {
        undoStack.addLast(snapshot)
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        _state.update { it.copy(canUndo = true, canRedo = false) }
    }

    private fun resolveOptions(state: EditorState): ExportOptions {
        val shortSide = state.options.shortSide ?: return state.options.copy(targetHeight = null)
        val (width, height) = state.outputSize
        if (width <= 0 || height <= 0) return state.options.copy(targetHeight = null)
        val target = if (width >= height) shortSide else (shortSide.toLong() * height / width).toInt()
        return state.options.copy(targetHeight = target - target % 2)
    }

    private fun refreshSourceSize() {
        val first = _state.value.segments.firstOrNull() ?: return
        _state.update { it.copy(sourceWidth = 0, sourceHeight = 0) }
        loadSourceSize(first.clip.uri)
    }

    private fun loadSourceSize(uri: Uri) {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) { readSize(uri) }
            if (size != null) setSourceSize(size.first, size.second)
        }
    }

    private fun keepAccess(uri: Uri) {
        try {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
        }
    }

    private fun readDuration(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun readMusic(uri: Uri): MusicTrack? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, uri)
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (!hasAudio || duration == null || duration <= 0L) {
                null
            } else {
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                MusicTrack(uri = uri.toString(), name = title?.takeIf { it.isNotBlank() } ?: displayName(uri), durationMs = duration)
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun displayName(uri: Uri): String {
        val name = try {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return (name ?: uri.lastPathSegment ?: "Music").substringBeforeLast('.')
    }

    private fun readSize(uri: Uri): Pair<Int, Int>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (width == null || height == null) {
                null
            } else if (rotation == 90 || rotation == 270) {
                height to width
            } else {
                width to height
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}

class EditorViewModelFactory(
    private val app: Application,
    private val clip: Clip
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EditorViewModel(app, clip) as T
}
