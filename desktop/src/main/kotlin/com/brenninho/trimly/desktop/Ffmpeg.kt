package com.brenninho.trimly.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.brenninho.trimly.model.ExportOptions
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

data class Tools(
    val ffmpeg: String,
    val ffprobe: String
)

data class VideoInfo(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val hasAudio: Boolean
)

object Ffmpeg {

    private val suffix = if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) ".exe" else ""
    private val progressLine = Regex("^[A-Za-z_0-9]+=")

    fun locate(): Tools? {
        val ffmpeg = find("ffmpeg") ?: return null
        val ffprobe = find("ffprobe") ?: return null
        return Tools(ffmpeg, ffprobe)
    }

    suspend fun probe(tools: Tools, file: File): VideoInfo? {
        return try {
            val video = capture(
                listOf(
                    tools.ffprobe, "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height:stream_tags=rotate:stream_side_data=rotation:format=duration",
                    "-of", "default=noprint_wrappers=1",
                    file.absolutePath
                )
            )
            val values = video.lineSequence()
                .mapNotNull { line ->
                    val index = line.indexOf('=')
                    if (index > 0) line.substring(0, index).trim() to line.substring(index + 1).trim() else null
                }
                .toMap()

            val width = values["width"]?.toIntOrNull() ?: return null
            val height = values["height"]?.toIntOrNull() ?: return null
            val seconds = values["duration"]?.toDoubleOrNull() ?: return null
            val rotation = (values["rotation"] ?: values["TAG:rotate"])?.toDoubleOrNull()?.toInt() ?: 0
            val turned = abs(rotation) % 180 == 90

            val audio = capture(
                listOf(
                    tools.ffprobe, "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=codec_type",
                    "-of", "default=noprint_wrappers=1",
                    file.absolutePath
                )
            ).contains("audio")

            VideoInfo(
                width = if (turned) height else width,
                height = if (turned) width else height,
                durationMs = (seconds * 1000).toLong(),
                hasAudio = audio
            )
        } catch (e: IOException) {
            null
        }
    }

    suspend fun frame(tools: Tools, file: File, atMs: Long, maxWidth: Int): ImageBitmap? {
        val command = listOf(
            tools.ffmpeg, "-v", "error",
            "-ss", seconds(atMs),
            "-i", file.absolutePath,
            "-frames:v", "1",
            "-vf", "scale='min($maxWidth,iw)':-2",
            "-f", "image2pipe",
            "-vcodec", "png",
            "-"
        )
        val bytes = try {
            withProcess(command, merge = false) { process ->
                val data = process.inputStream.readBytes()
                process.waitFor()
                data
            }
        } catch (e: IOException) {
            return null
        }
        if (bytes.isEmpty()) return null
        return try {
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun export(
        tools: Tools,
        file: File,
        info: VideoInfo,
        startMs: Long,
        endMs: Long,
        options: ExportOptions,
        output: File,
        onProgress: (Float) -> Unit
    ) {
        val lengthMs = (endMs - startMs).coerceAtLeast(1L)
        val outputMs = options.outputMs(lengthMs).coerceAtLeast(1L)
        val command = ArrayList<String>()
        command += listOf(
            tools.ffmpeg, "-y", "-hide_banner",
            "-loglevel", "error",
            "-progress", "pipe:1",
            "-nostats",
            "-ss", seconds(startMs),
            "-i", file.absolutePath,
            "-t", seconds(lengthMs)
        )

        val chain = videoChain(options, outputMs)
        if (chain.isNotEmpty()) command += listOf("-vf", chain)

        command += listOf("-c:v", "libx264", "-preset", "medium", "-crf", "20", "-pix_fmt", "yuv420p")

        if (options.muted || !info.hasAudio) {
            command += "-an"
        } else {
            val audio = audioChain(options, outputMs)
            if (audio.isNotEmpty()) command += listOf("-af", audio)
            command += listOf("-c:a", "aac", "-b:a", "192k")
        }

        command += listOf("-movflags", "+faststart", output.absolutePath)

        val tail = ArrayDeque<String>()
        try {
            withProcess(command, merge = true) { process ->
                process.inputStream.bufferedReader().forEachLine { line ->
                    val key = line.substringBefore('=', "")
                    if (key == "out_time_us" || key == "out_time_ms") {
                        line.substringAfter('=').trim().toLongOrNull()?.let { micros ->
                            onProgress((micros / 1000f / outputMs).coerceIn(0f, 1f))
                        }
                    } else if (!progressLine.containsMatchIn(line)) {
                        tail.addLast(line)
                        if (tail.size > 20) tail.removeFirst()
                    }
                }
                val code = process.waitFor()
                if (code != 0) {
                    throw IOException(tail.joinToString("\n").ifBlank { "ffmpeg exited with code $code" })
                }
            }
            onProgress(1f)
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }

    private fun videoChain(options: ExportOptions, outputMs: Long): String {
        val parts = ArrayList<String>()
        FilterChain.build(options).takeIf { it.isNotEmpty() }?.let { parts.add(it) }
        if (options.hasSpeed) parts.add("setpts=PTS/${number(options.speed.toDouble())}")
        if (options.fadeVideo) {
            if (options.fadeInMs > 0L) parts.add("fade=t=in:st=0:d=${seconds(options.fadeInMs)}")
            if (options.fadeOutMs > 0L) {
                val start = (outputMs - options.fadeOutMs).coerceAtLeast(0L)
                parts.add("fade=t=out:st=${seconds(start)}:d=${seconds(options.fadeOutMs)}")
            }
        }
        return parts.joinToString(",")
    }

    private fun audioChain(options: ExportOptions, outputMs: Long): String {
        val parts = ArrayList<String>()
        if (options.hasSpeed) {
            if (options.keepPitch) {
                parts.addAll(tempoSteps(options.speed.toDouble()).map { "atempo=${number(it)}" })
            } else {
                val rate = (AUDIO_RATE * options.speed).toInt()
                parts.add("aresample=$AUDIO_RATE")
                parts.add("asetrate=$rate")
                parts.add("aresample=$AUDIO_RATE")
            }
        }
        if (options.hasVolume) parts.add("volume=${number(options.volume / 100.0)}")
        if (options.fadeInMs > 0L) parts.add("afade=t=in:st=0:d=${seconds(options.fadeInMs)}")
        if (options.fadeOutMs > 0L) {
            val start = (outputMs - options.fadeOutMs).coerceAtLeast(0L)
            parts.add("afade=t=out:st=${seconds(start)}:d=${seconds(options.fadeOutMs)}")
        }
        return parts.joinToString(",")
    }

    private fun tempoSteps(speed: Double): List<Double> {
        val steps = ArrayList<Double>()
        var remaining = speed
        while (remaining > 2.0) {
            steps.add(2.0)
            remaining /= 2.0
        }
        while (remaining < 0.5) {
            steps.add(0.5)
            remaining /= 0.5
        }
        steps.add(remaining)
        return steps
    }

    private fun number(value: Double): String = String.format(Locale.ROOT, "%.4f", value)

    private const val AUDIO_RATE = 48000

    private suspend fun capture(command: List<String>): String =
        withProcess(command, merge = false) { process ->
            val text = process.inputStream.bufferedReader().readText()
            process.waitFor()
            text
        }

    private suspend fun <T> withProcess(
        command: List<String>,
        merge: Boolean,
        block: (Process) -> T
    ): T = coroutineScope {
        val builder = ProcessBuilder(command)
        if (merge) {
            builder.redirectErrorStream(true)
        } else {
            builder.redirectError(ProcessBuilder.Redirect.DISCARD)
        }
        val process = builder.start()
        val killer = launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                process.destroyForcibly()
            }
        }
        try {
            withContext(Dispatchers.IO) { block(process) }
        } finally {
            killer.cancel()
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun find(name: String): String? {
        val candidates = ArrayList<File>()
        System.getProperty("compose.application.resources.dir")?.let {
            candidates.add(File(it, name + suffix))
        }
        System.getenv("TRIMLY_FFMPEG_DIR")?.let { candidates.add(File(it, name + suffix)) }
        candidates.add(File("ffmpeg", name + suffix))
        candidates.firstOrNull { it.isFile }?.let { return it.absolutePath }

        return System.getenv("PATH").orEmpty()
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map { File(it, name + suffix) }
            .firstOrNull { it.isFile }
            ?.absolutePath
    }

    private fun seconds(ms: Long): String = String.format(Locale.ROOT, "%.3f", ms / 1000.0)
}
