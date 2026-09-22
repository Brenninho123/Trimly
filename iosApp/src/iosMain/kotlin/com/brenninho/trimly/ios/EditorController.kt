package com.brenninho.trimly.ios

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.brenninho.trimly.engine.AvFoundationExportEngine
import com.brenninho.trimly.engine.ExportFailedException
import com.brenninho.trimly.model.ExportOptions
import kotlin.math.roundToLong
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL

const val MIN_TRIM_MS = 500L

data class VideoInfo(
    val width: Int,
    val height: Int,
    val durationMs: Long
)

sealed interface ExportUi {
    data object Idle : ExportUi
    data class Running(val progress: Float) : ExportUi
    data class Done(val path: String) : ExportUi
    data class Failed(val message: String) : ExportUi
}

@OptIn(ExperimentalForeignApi::class)
fun probeVideo(path: String): VideoInfo? {
    val asset = AVURLAsset(NSURL.fileURLWithPath(path), null)
    val track = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? platform.AVFoundation.AVAssetTrack
        ?: return null
    val size = track.naturalSize.useContents { this }
    val durationMs = (asset.duration.useContents { value.toDouble() / timescale.toDouble() } * 1000.0).roundToLong()
    return VideoInfo(width = size.width.toInt(), height = size.height.toInt(), durationMs = durationMs)
}

class EditorController(
    private val scope: CoroutineScope,
    val path: String,
    val info: VideoInfo
) {

    var startMs by mutableLongStateOf(0L)
        private set

    var endMs by mutableLongStateOf(info.durationMs)
        private set

    var options by mutableStateOf(ExportOptions())
        private set

    var export by mutableStateOf<ExportUi>(ExportUi.Idle)
        private set

    private var exportJob: Job? = null
    private val engine = AvFoundationExportEngine()

    val trimmedMs: Long
        get() = endMs - startMs

    val hasEdits: Boolean
        get() = startMs > 0L || endMs < info.durationMs || options != ExportOptions()

    val canExport: Boolean
        get() = trimmedMs >= minOf(MIN_TRIM_MS, info.durationMs) && export !is ExportUi.Running

    fun setRange(newStart: Long, newEnd: Long) {
        val gap = minOf(MIN_TRIM_MS, info.durationMs)
        var start = newStart.coerceIn(0L, info.durationMs)
        var end = newEnd.coerceIn(0L, info.durationMs)
        if (end - start < gap) {
            if (start != startMs) start = (end - gap).coerceAtLeast(0L) else end = (start + gap).coerceAtMost(info.durationMs)
        }
        startMs = start
        endMs = end
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

    fun reset() {
        startMs = 0L
        endMs = info.durationMs
        options = ExportOptions()
    }

    fun startExport() {
        if (export is ExportUi.Running || !canExport) return
        export = ExportUi.Running(0f)
        val start = startMs
        val end = endMs
        val resolved = options
        val outputPath = NSHomeDirectory() + "/tmp/trimly_" + start + "_" + end + ".mp4"
        exportJob = scope.launch {
            try {
                val outcome = engine.exportTrim(path, start, end, resolved, outputPath) { progress ->
                    export = ExportUi.Running(progress.fraction)
                }
                export = ExportUi.Done(outcome.outputPath)
            } catch (e: CancellationException) {
                export = ExportUi.Idle
                throw e
            } catch (e: ExportFailedException) {
                export = ExportUi.Failed(e.message ?: "Export failed")
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
}
