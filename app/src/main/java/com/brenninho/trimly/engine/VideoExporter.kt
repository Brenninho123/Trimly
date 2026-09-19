package com.brenninho.trimly.engine

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.brenninho.trimly.model.ExportOptions
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val MIN_RANGE_MS = 200L
private const val BITS_PER_PIXEL = 0.12
private const val MIN_BITRATE = 800_000L
private const val MAX_BITRATE = 50_000_000L
private const val AUDIO_BITRATE = 128_000L
private const val STALL_MS = 90_000L
private const val POLL_MS = 250L
private const val SPACE_MARGIN_NUMERATOR = 16L
private const val SPACE_MARGIN_DENOMINATOR = 10L

private data class SourceInfo(
    val width: Int,
    val height: Int,
    val bitrate: Long?,
    val frameRate: Float?,
    val durationMs: Long
)

private class Plan(
    val effects: List<Effect>,
    val bitrate: Int?,
    val fastTrim: Boolean,
    val forceH264: Boolean,
    val estimatedBytes: Long,
    val muted: Boolean
)

class VideoExporter(private val context: Context) {

    suspend fun trim(
        source: Uri,
        startMs: Long,
        endMs: Long,
        output: File,
        onProgress: (Float) -> Unit
    ): File = export(
        source = source,
        startMs = startMs,
        endMs = endMs,
        options = ExportOptions(),
        output = output,
        onProgress = onProgress
    )

    suspend fun export(
        source: Uri,
        startMs: Long,
        endMs: Long,
        options: ExportOptions,
        output: File,
        onProgress: (Float) -> Unit
    ): File = exportDetailed(source, startMs, endMs, options, output) { onProgress(it.fraction) }.file

    suspend fun exportDetailed(
        source: Uri,
        startMs: Long,
        endMs: Long,
        options: ExportOptions,
        output: File,
        onProgress: (ExportProgress) -> Unit
    ): ExportOutcome {
        val startedAt = SystemClock.elapsedRealtime()

        if (endMs - startMs < MIN_RANGE_MS) {
            throw ExportFailedException(ExportFailure.EMPTY_RANGE, "The selected range is too short")
        }

        val info = withContext(Dispatchers.IO) { probe(source) }
            ?: throw ExportFailedException(ExportFailure.SOURCE_UNREADABLE, "The source video could not be read")

        var attempt = 1
        var plan = buildPlan(options, info, startMs, endMs, safe = false)
        checkSpace(output, plan.estimatedBytes)

        while (true) {
            try {
                runAttempt(source, startMs, endMs, plan, output, startedAt, onProgress)
                return ExportOutcome(
                    file = output,
                    sizeBytes = output.length(),
                    elapsedMs = SystemClock.elapsedRealtime() - startedAt,
                    attempts = attempt,
                    fastTrim = plan.fastTrim
                )
            } catch (e: CancellationException) {
                output.delete()
                throw e
            } catch (e: Exception) {
                output.delete()
                val failure = classify(e)
                val canRetry = attempt == 1 && failure in RETRYABLE
                if (!canRetry) {
                    throw (e as? ExportFailedException) ?: ExportFailedException(failure, e.message, e)
                }
                attempt += 1
                plan = buildPlan(options, info, startMs, endMs, safe = true)
            }
        }
    }

    private suspend fun runAttempt(
        source: Uri,
        startMs: Long,
        endMs: Long,
        plan: Plan,
        output: File,
        startedAt: Long,
        onProgress: (ExportProgress) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val handler = Handler(Looper.getMainLooper())
                val holder = ProgressHolder()
                var running = true
                var best = 0f
                var sawProgress = false
                var lastAdvance = SystemClock.elapsedRealtime()

                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        running = false
                        onProgress(ExportProgress(1f, SystemClock.elapsedRealtime() - startedAt, 0L))
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        running = false
                        output.delete()
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                }

                val transformer = buildTransformer(plan, listener)

                val clipping = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(source)
                    .setClippingConfiguration(clipping)
                    .build()

                val edited = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(plan.muted)
                    .setEffects(Effects(emptyList(), plan.effects))
                    .build()

                val poll = object : Runnable {
                    override fun run() {
                        if (!running) return
                        val state = transformer.getProgress(holder)
                        val now = SystemClock.elapsedRealtime()

                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            sawProgress = true
                            val fraction = (holder.progress / 100f).coerceIn(0f, 1f)
                            if (fraction > best + 0.001f) {
                                best = fraction
                                lastAdvance = now
                            }
                            val elapsed = now - startedAt
                            val eta = if (best > 0.03f) (elapsed * (1f - best) / best).toLong() else null
                            onProgress(ExportProgress(best, elapsed, eta))
                        }

                        if (sawProgress && now - lastAdvance > STALL_MS) {
                            running = false
                            transformer.cancel()
                            output.delete()
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    ExportFailedException(ExportFailure.STALLED, "The export stopped making progress")
                                )
                            }
                            return
                        }

                        handler.postDelayed(this, POLL_MS)
                    }
                }

                transformer.start(edited, output.absolutePath)
                handler.post(poll)

                continuation.invokeOnCancellation {
                    running = false
                    handler.post {
                        transformer.cancel()
                        output.delete()
                    }
                }
            }
        }
    }

    private fun buildTransformer(plan: Plan, listener: Transformer.Listener): Transformer {
        val builder = Transformer.Builder(context).addListener(listener)

        if (plan.forceH264) {
            builder.setVideoMimeType(MimeTypes.VIDEO_H264)
        }

        plan.bitrate?.let { bitrate ->
            val settings = VideoEncoderSettings.Builder()
                .setBitrate(bitrate)
                .setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                .build()
            builder.setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(settings)
                    .build()
            )
        }

        if (plan.fastTrim) {
            builder.experimentalSetTrimOptimizationEnabled(true)
        }

        return builder.build()
    }

    private fun buildPlan(
        options: ExportOptions,
        info: SourceInfo,
        startMs: Long,
        endMs: Long,
        safe: Boolean
    ): Plan {
        val effects = EffectsFactory.build(options)
        val turned = options.rotationDegrees % 180 != 0
        val displayWidth = if (turned) info.height else info.width
        val displayHeight = if (turned) info.width else info.height
        val outputHeight = options.targetHeight ?: displayHeight
        val outputWidth = (displayWidth.toLong() * outputHeight / displayHeight).toInt()
        val frameRate = (info.frameRate ?: 30f).coerceIn(15f, 60f)
        val seconds = (endMs - startMs) / 1000.0

        val heuristic = (outputWidth.toDouble() * outputHeight * frameRate * BITS_PER_PIXEL)
            .toLong()
            .coerceIn(MIN_BITRATE, MAX_BITRATE)
        val sourceBitrate = info.bitrate?.takeIf { it > 0L }
        val bitrate = if (sourceBitrate != null) {
            min(heuristic, (sourceBitrate * 1.2).toLong()).coerceAtLeast(MIN_BITRATE)
        } else {
            heuristic
        }

        val trimmed = startMs > 0L || endMs < info.durationMs
        val fastTrim = !safe && effects.isEmpty() && !options.muted && trimmed
        val estimateRate = if (fastTrim) (sourceBitrate ?: bitrate) else bitrate + AUDIO_BITRATE
        val estimatedBytes = (estimateRate * seconds / 8.0).toLong()

        return Plan(
            effects = effects,
            bitrate = if (safe) null else bitrate.toInt(),
            fastTrim = fastTrim,
            forceH264 = safe,
            estimatedBytes = estimatedBytes,
            muted = options.muted
        )
    }

    private fun checkSpace(output: File, estimatedBytes: Long) {
        val directory = output.parentFile ?: return
        val free = try {
            StatFs(directory.path).availableBytes
        } catch (e: Exception) {
            return
        }
        val needed = estimatedBytes * SPACE_MARGIN_NUMERATOR / SPACE_MARGIN_DENOMINATOR
        if (free < needed) {
            throw ExportFailedException(ExportFailure.NO_SPACE, "Not enough free storage for this export")
        }
    }

    private fun probe(uri: Uri): SourceInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()

            if (width == null || height == null || width <= 0 || height <= 0) {
                null
            } else if (rotation == 90 || rotation == 270) {
                SourceInfo(height, width, bitrate, frameRate, duration)
            } else {
                SourceInfo(width, height, bitrate, frameRate, duration)
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun classify(error: Throwable): ExportFailure = when (error) {
        is ExportFailedException -> error.failure
        is ExportException -> when (error.errorCode) {
            ExportException.ERROR_CODE_IO_FILE_NOT_FOUND -> ExportFailure.SOURCE_UNREADABLE
            ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED -> ExportFailure.UNSUPPORTED
            ExportException.ERROR_CODE_DECODER_INIT_FAILED,
            ExportException.ERROR_CODE_ENCODER_INIT_FAILED -> ExportFailure.ENCODER
            else -> ExportFailure.OTHER
        }
        is IOException -> ExportFailure.OTHER
        else -> ExportFailure.OTHER
    }

    private companion object {
        val RETRYABLE = setOf(ExportFailure.ENCODER, ExportFailure.UNSUPPORTED, ExportFailure.OTHER)
    }
}
