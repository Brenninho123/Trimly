package com.brenninho.trimly.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.VideoFilter
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val MIN_TRIM_MS = 500L
private const val STRIP_COUNT = 10

sealed interface ExportUi {
    data object Idle : ExportUi
    data class Running(val progress: Float) : ExportUi
    data class Done(val file: File) : ExportUi
    data class Failed(val message: String) : ExportUi
}

class EditorController(
    private val scope: CoroutineScope,
    val tools: Tools,
    val file: File,
    val info: VideoInfo
) {

    var startMs by mutableLongStateOf(0L)
        private set

    var endMs by mutableLongStateOf(info.durationMs)
        private set

    var positionMs by mutableLongStateOf(0L)
        private set

    var options by mutableStateOf(ExportOptions())
        private set

    var frame by mutableStateOf<ImageBitmap?>(null)
        private set

    var rendering by mutableStateOf(false)
        private set

    var export by mutableStateOf<ExportUi>(ExportUi.Idle)
        private set

    val strip = mutableStateListOf<ImageBitmap?>()

    private var exportJob: Job? = null

    init {
        repeat(STRIP_COUNT) { strip.add(null) }
    }

    val trimmedMs: Long
        get() = endMs - startMs

    val hasEdits: Boolean
        get() = startMs > 0L || endMs < info.durationMs || options != ExportOptions()

    val canExport: Boolean
        get() = trimmedMs >= minOf(MIN_TRIM_MS, info.durationMs) && export !is ExportUi.Running

    val availableShortSides: List<Int>
        get() {
            val shortSide = min(info.width, info.height)
            return listOf(1080, 720, 480).filter { it < shortSide }
        }

    suspend fun loadStrip() {
        val step = info.durationMs / STRIP_COUNT
        for (index in 0 until STRIP_COUNT) {
            strip[index] = Ffmpeg.frame(tools, file, step * index + step / 2, 240)
        }
    }

    suspend fun renderFrame() {
        rendering = true
        try {
            val next = Ffmpeg.frame(tools, file, positionMs, 1280)
            if (next != null) frame = next
        } finally {
            rendering = false
        }
    }

    fun seek(ms: Long) {
        positionMs = ms.coerceIn(startMs, endMs)
    }

    fun setRange(newStart: Long, newEnd: Long) {
        val gap = minOf(MIN_TRIM_MS, info.durationMs)
        var start = newStart.coerceIn(0L, info.durationMs)
        var end = newEnd.coerceIn(0L, info.durationMs)
        if (end - start < gap) {
            if (start != startMs) start = (end - gap).coerceAtLeast(0L) else end = (start + gap).coerceAtMost(info.durationMs)
        }
        startMs = start
        endMs = end
        positionMs = positionMs.coerceIn(startMs, endMs)
    }

    fun setStartAtPlayhead() {
        setRange(positionMs, endMs)
    }

    fun setEndAtPlayhead() {
        setRange(startMs, positionMs)
    }

    fun rotate() {
        options = options.rotated()
    }

    fun toggleFlip() {
        options = options.copy(flipHorizontal = !options.flipHorizontal)
    }

    fun toggleMute() {
        options = options.copy(muted = !options.muted)
    }

    fun setQuality(shortSide: Int?) {
        options = options.copy(shortSide = shortSide)
    }

    fun setFilter(filter: VideoFilter) {
        if (options.filter != filter) options = options.copy(filter = filter, filterIntensity = 1f)
    }

    fun setIntensity(value: Float) {
        options = options.copy(filterIntensity = value.coerceIn(0f, 1f))
    }

    fun setAdjustment(kind: Adjustment, value: Int) {
        options = options.withAdjustment(kind, value.coerceIn(-100, 100))
    }

    fun resetAdjustments() {
        options = options.clearAdjustments()
    }

    fun reset() {
        startMs = 0L
        endMs = info.durationMs
        positionMs = 0L
        options = ExportOptions()
    }

    fun startExport(output: File) {
        if (export is ExportUi.Running || !canExport) return
        export = ExportUi.Running(0f)
        val start = startMs
        val end = endMs
        val resolved = resolvedOptions()
        exportJob = scope.launch {
            try {
                Ffmpeg.export(tools, file, info, start, end, resolved, output) { progress ->
                    export = ExportUi.Running(progress)
                }
                export = ExportUi.Done(output)
            } catch (e: CancellationException) {
                export = ExportUi.Idle
                throw e
            } catch (e: Exception) {
                export = ExportUi.Failed(e.message ?: "Export failed")
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        export = ExportUi.Idle
    }

    fun dismissExport() {
        export = ExportUi.Idle
    }

    private fun resolvedOptions(): ExportOptions {
        val shortSide = options.shortSide ?: return options.copy(targetHeight = null)
        val turned = options.rotationDegrees % 180 != 0
        val width = if (turned) info.height else info.width
        val height = if (turned) info.width else info.height
        if (width <= 0 || height <= 0) return options.copy(targetHeight = null)
        val target = if (width >= height) shortSide else (shortSide.toLong() * height / width).toInt()
        return options.copy(targetHeight = target - target % 2)
    }
}
