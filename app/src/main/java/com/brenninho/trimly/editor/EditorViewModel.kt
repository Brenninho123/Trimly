package com.brenninho.trimly.editor

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brenninho.trimly.engine.MediaSaver
import com.brenninho.trimly.engine.VideoExporter
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.Clip
import com.brenninho.trimly.model.ExportOptions
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

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Running(val progress: Float) : ExportStatus
    data class Done(val uri: Uri?, val location: String) : ExportStatus
    data class Failed(val message: String) : ExportStatus
}

data class EditorState(
    val clip: Clip,
    val options: ExportOptions = ExportOptions(),
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val export: ExportStatus = ExportStatus.Idle
) {
    val hasEdits: Boolean
        get() = clip.startMs > 0L || clip.endMs < clip.durationMs || options != ExportOptions()

    val canExport: Boolean
        get() = clip.trimmedDurationMs >= MIN_TRIM_MS && export !is ExportStatus.Running

    val availableShortSides: List<Int>
        get() {
            val shortSide = min(sourceWidth, sourceHeight)
            return if (shortSide <= 0) emptyList() else listOf(1080, 720, 480).filter { it < shortSide }
        }
}

class EditorViewModel(
    private val app: Application,
    clip: Clip
) : ViewModel() {

    private val original = clip
    private val exporter = VideoExporter(app)
    private val _state = MutableStateFlow(EditorState(clip))
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) { readSize(clip.uri) }
            if (size != null) setSourceSize(size.first, size.second)
        }
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

    fun setRange(startMs: Long, endMs: Long) {
        _state.update { it.copy(clip = it.clip.withRange(startMs, endMs)) }
    }

    fun rotate() {
        _state.update { it.copy(options = it.options.rotated()) }
    }

    fun toggleFlip() {
        _state.update { it.copy(options = it.options.copy(flipHorizontal = !it.options.flipHorizontal)) }
    }

    fun toggleMute() {
        _state.update { it.copy(options = it.options.copy(muted = !it.options.muted)) }
    }

    fun setQuality(shortSide: Int?) {
        _state.update { it.copy(options = it.options.copy(shortSide = shortSide)) }
    }

    fun setFilter(filter: VideoFilter) {
        _state.update {
            if (it.options.filter == filter) {
                it
            } else {
                it.copy(options = it.options.copy(filter = filter, filterIntensity = 1f))
            }
        }
    }

    fun setFilterIntensity(value: Float) {
        _state.update { it.copy(options = it.options.copy(filterIntensity = value.coerceIn(0f, 1f))) }
    }

    fun setAdjustment(kind: Adjustment, value: Int) {
        _state.update { it.copy(options = it.options.withAdjustment(kind, value.coerceIn(-100, 100))) }
    }

    fun resetAdjustments() {
        _state.update { it.copy(options = it.options.clearAdjustments()) }
    }

    fun reset() {
        _state.update {
            EditorState(
                clip = original,
                sourceWidth = it.sourceWidth,
                sourceHeight = it.sourceHeight
            )
        }
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
                val file = exporter.export(
                    source = current.clip.uri,
                    startMs = current.clip.startMs,
                    endMs = current.clip.endMs,
                    options = resolveOptions(current),
                    output = output,
                    onProgress = { progress ->
                        _state.update { s ->
                            if (s.export is ExportStatus.Running) {
                                s.copy(export = ExportStatus.Running(progress))
                            } else {
                                s
                            }
                        }
                    }
                )
                val saved = MediaSaver.save(app, file)
                _state.update { it.copy(export = ExportStatus.Done(saved.uri, saved.location)) }
            } catch (e: CancellationException) {
                output.delete()
                throw e
            } catch (e: Exception) {
                output.delete()
                val detail = listOfNotNull(e.message, e.cause?.message).distinct().joinToString(": ")
                _state.update { it.copy(export = ExportStatus.Failed(detail.ifBlank { "Export failed" })) }
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

    private fun resolveOptions(state: EditorState): ExportOptions {
        val shortSide = state.options.shortSide ?: return state.options.copy(targetHeight = null)
        val turned = state.options.rotationDegrees % 180 != 0
        val width = if (turned) state.sourceHeight else state.sourceWidth
        val height = if (turned) state.sourceWidth else state.sourceHeight
        if (width <= 0 || height <= 0) return state.options.copy(targetHeight = null)
        val target = if (width >= height) shortSide else (shortSide.toLong() * height / width).toInt()
        return state.options.copy(targetHeight = target - target % 2)
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
