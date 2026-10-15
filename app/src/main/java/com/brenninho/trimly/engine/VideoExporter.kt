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
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
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
private const val MOSH_SHARE = 0.85f

class ExportSegment(
    val uri: Uri,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}

private data class SourceInfo(
    val width: Int,
    val height: Int,
    val bitrate: Long?,
    val frameRate: Float?,
    val durationMs: Long
)

private class ItemAudio(
    val speed: Float,
    val keepPitch: Boolean,
    val gain: Float
) {
    fun processors(): List<AudioProcessor> {
        val list = ArrayList<AudioProcessor>()
        if (speed != 1f) {
            list.add(
                SonicAudioProcessor().apply {
                    setSpeed(speed)
                    setPitch(if (keepPitch) 1f else speed)
                }
            )
        }
        if (gain != 1f) list.add(GainEnvelopeProcessor(gain, 0L, 0L, 0L))
        return list
    }
}

private class MusicPlan(
    val uri: Uri,
    val gain: Float,
    val loop: Boolean,
    val startMs: Long,
    val endMs: Long?
)

private class Plan(
    val itemEffects: List<Effect>,
    val compositionEffects: List<Effect>,
    val bitrate: Int?,
    val keyframeSeconds: Float?,
    val fastTrim: Boolean,
    val forceH264: Boolean,
    val estimatedBytes: Long,
    val muted: Boolean,
    val itemAudio: ItemAudio?,
    val fadeInUs: Long,
    val fadeOutUs: Long,
    val totalUs: Long,
    val music: MusicPlan?,
    val forceAudioTrack: Boolean,
    val mosh: MoshPlan?
) {
    fun compositionAudio(): List<AudioProcessor> =
        if (fadeInUs > 0L || fadeOutUs > 0L) {
            listOf(GainEnvelopeProcessor(1f, fadeInUs, fadeOutUs, totalUs))
        } else {
            emptyList()
        }
}

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
    ): File = exportDetailed(
        segments = listOf(ExportSegment(source, startMs, endMs)),
        options = options,
        output = output
    ) { onProgress(it.fraction) }.file

    suspend fun exportDetailed(
        segments: List<ExportSegment>,
        options: ExportOptions,
        output: File,
        onProgress: (ExportProgress) -> Unit
    ): ExportOutcome {
        val startedAt = SystemClock.elapsedRealtime()

        if (segments.isEmpty() || segments.any { it.durationMs < MIN_RANGE_MS }) {
            throw ExportFailedException(ExportFailure.EMPTY_RANGE, "The selected range is too short")
        }

        val infos = withContext(Dispatchers.IO) { segments.map { probe(it.uri) } }
        if (infos.any { it == null }) {
            throw ExportFailedException(ExportFailure.SOURCE_UNREADABLE, "A source video could not be read")
        }
        val first = infos.first() ?: throw ExportFailedException(ExportFailure.SOURCE_UNREADABLE, null)

        var attempt = 1
        var plan = buildPlan(options, first, segments, safe = false)
        checkSpace(output, plan.estimatedBytes * if (plan.mosh != null) 2L else 1L)

        val stage = if (plan.mosh != null) File(output.parentFile, output.nameWithoutExtension + "_raw.mp4") else output
        val share = if (plan.mosh != null) MOSH_SHARE else 1f

        while (true) {
            try {
                runAttempt(segments, plan, stage, startedAt, share, onProgress)
                break
            } catch (e: CancellationException) {
                stage.delete()
                throw e
            } catch (e: Exception) {
                stage.delete()
                val failure = classify(e)
                val canRetry = attempt == 1 && failure in RETRYABLE
                if (!canRetry) {
                    throw (e as? ExportFailedException) ?: ExportFailedException(failure, e.message, e)
                }
                attempt += 1
                plan = buildPlan(options, first, segments, safe = true)
            }
        }

        val mosh = plan.mosh
        if (mosh != null) {
            try {
                Datamosher.process(stage, output, mosh) { fraction ->
                    val total = share + (1f - share) * fraction
                    val elapsed = SystemClock.elapsedRealtime() - startedAt
                    onProgress(ExportProgress(total, elapsed, null))
                }
            } catch (e: CancellationException) {
                output.delete()
                throw e
            } catch (e: Exception) {
                output.delete()
                throw ExportFailedException(ExportFailure.OTHER, e.message, e)
            } finally {
                stage.delete()
            }
        }

        onProgress(ExportProgress(1f, SystemClock.elapsedRealtime() - startedAt, 0L))
        return ExportOutcome(
            file = output,
            sizeBytes = output.length(),
            elapsedMs = SystemClock.elapsedRealtime() - startedAt,
            attempts = attempt,
            fastTrim = plan.fastTrim
        )
    }

    private suspend fun runAttempt(
        segments: List<ExportSegment>,
        plan: Plan,
        output: File,
        startedAt: Long,
        share: Float,
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
                        onProgress(ExportProgress(share, SystemClock.elapsedRealtime() - startedAt, null))
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
                            val overall = best * share
                            val eta = if (overall > 0.03f) (elapsed * (1f - overall) / overall).toLong() else null
                            onProgress(ExportProgress(overall, elapsed, eta))
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

                transformer.start(composition(segments, plan), output.absolutePath)
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

    private fun composition(segments: List<ExportSegment>, plan: Plan): Composition {
        val video = EditedMediaItemSequence.Builder()
        segments.forEach { video.addItem(editedItem(it, plan)) }
        val sequences = ArrayList<EditedMediaItemSequence>()
        sequences.add(video.build())
        plan.music?.let { sequences.add(musicSequence(it)) }

        return Composition.Builder(sequences)
            .setEffects(Effects(plan.compositionAudio(), plan.compositionEffects))
            .experimentalSetForceAudioTrack(plan.forceAudioTrack)
            .build()
    }

    private fun editedItem(segment: ExportSegment, plan: Plan): EditedMediaItem {
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(segment.startMs)
            .setEndPositionMs(segment.endMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(segment.uri)
            .setClippingConfiguration(clipping)
            .build()

        return EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(plan.muted)
            .setEffects(Effects(plan.itemAudio?.processors().orEmpty(), plan.itemEffects))
            .build()
    }

    private fun musicSequence(music: MusicPlan): EditedMediaItemSequence {
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(music.startMs)
        music.endMs?.let { clipping.setEndPositionMs(it) }

        val item = MediaItem.Builder()
            .setUri(music.uri)
            .setClippingConfiguration(clipping.build())
            .build()

        val processors = if (music.gain != 1f) {
            listOf<AudioProcessor>(GainEnvelopeProcessor(music.gain, 0L, 0L, 0L))
        } else {
            emptyList()
        }

        val edited = EditedMediaItem.Builder(item)
            .setRemoveVideo(true)
            .setEffects(Effects(processors, emptyList()))
            .build()

        return EditedMediaItemSequence.Builder(edited)
            .setIsLooping(music.loop)
            .build()
    }

    private fun buildTransformer(plan: Plan, listener: Transformer.Listener): Transformer {
        val builder = Transformer.Builder(context).addListener(listener)

        if (plan.forceH264) {
            builder.setVideoMimeType(MimeTypes.VIDEO_H264)
        }

        if (plan.bitrate != null || plan.keyframeSeconds != null) {
            val settings = VideoEncoderSettings.Builder()
            plan.bitrate?.let {
                settings.setBitrate(it)
                settings.setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            }
            plan.keyframeSeconds?.let { settings.setiFrameIntervalSeconds(it) }
            builder.setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(settings.build())
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
        segments: List<ExportSegment>,
        safe: Boolean
    ): Plan {
        val merged = segments.size > 1
        val baseEffects = EffectsFactory.build(options)

        val turned = options.rotationDegrees % 180 != 0
        val rotatedWidth = if (turned) info.height else info.width
        val rotatedHeight = if (turned) info.width else info.height
        val (displayWidth, displayHeight) = options.aspect.crop(rotatedWidth, rotatedHeight)
        val outputHeight = evenAtLeast(options.targetHeight ?: displayHeight)
        val outputWidth = evenAtLeast((displayWidth.toLong() * outputHeight / displayHeight.coerceAtLeast(1)).toInt())

        val overlay = TextOverlays.build(options.texts, outputHeight, options.speed)
        val speed = options.speed.coerceIn(0.1f, 10f)
        val segmentUs = segments.map { (it.durationMs * 1000.0 / speed).toLong() }
        val totalUs = segmentUs.sum()
        val fade = if (options.fadeVideo && options.hasFade) {
            FadeMatrixEffect(options.fadeInMs * 1000L, options.fadeOutMs * 1000L, totalUs)
        } else {
            null
        }

        val itemEffects = ArrayList<Effect>(baseEffects)
        val compositionEffects = ArrayList<Effect>()
        if (merged) {
            val layout = if (options.hasAspect) Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP else Presentation.LAYOUT_SCALE_TO_FIT
            itemEffects.add(Presentation.createForWidthAndHeight(outputWidth, outputHeight, layout))
            if (overlay != null) compositionEffects.add(overlay)
            if (fade != null) compositionEffects.add(fade)
        } else {
            if (overlay != null) itemEffects.add(overlay)
            if (fade != null) itemEffects.add(fade)
        }

        val itemAudio = if (options.muted || (!options.hasSpeed && !options.hasVolume)) {
            null
        } else {
            ItemAudio(speed, options.keepPitch, options.gain)
        }

        val music = options.music?.let { track ->
            val wantedMs = totalUs / 1000L
            val end = if (track.loop) null else min(track.durationMs, track.startOffsetMs + wantedMs).takeIf { it > track.startOffsetMs }
            MusicPlan(
                uri = Uri.parse(track.uri),
                gain = track.volume / 100f,
                loop = track.loop,
                startMs = track.startOffsetMs.coerceAtLeast(0L),
                endMs = end
            )
        }

        val hasAudioOutput = !options.muted || music != null
        val fadeInUs = if (hasAudioOutput) options.fadeInMs * 1000L else 0L
        val fadeOutUs = if (hasAudioOutput) options.fadeOutMs * 1000L else 0L

        val mosh = options.datamosh?.let { settings ->
            val start = (settings.startMs * 1000.0 / speed).toLong().coerceAtLeast(0L)
            val end = settings.endMs?.let { (it * 1000.0 / speed).toLong() } ?: Long.MAX_VALUE
            MoshPlan(settings.mode, start, end, settings.bloomFrames)
        }

        val frameRate = (info.frameRate ?: 30f).coerceIn(15f, 60f)
        val seconds = totalUs / 1_000_000.0
        val quality = options.quality

        val heuristic = (outputWidth.toDouble() * outputHeight * frameRate * BITS_PER_PIXEL * quality.bitrateFactor)
            .toLong()
            .coerceIn(MIN_BITRATE, MAX_BITRATE)
        val sourceBitrate = info.bitrate?.takeIf { it > 0L }
        val bitrate = if (sourceBitrate != null) {
            min(heuristic, (sourceBitrate * quality.sourceCap).toLong()).coerceAtLeast(MIN_BITRATE)
        } else {
            heuristic
        }

        val trimmed = segments.first().startMs > 0L || segments.first().endMs < info.durationMs
        val audioWork = itemAudio != null || music != null || fadeInUs > 0L || fadeOutUs > 0L
        val fastTrim = !safe && !merged && itemEffects.isEmpty() && !audioWork && mosh == null && !options.muted && trimmed
        val estimateRate = if (fastTrim) (sourceBitrate ?: bitrate) else bitrate + AUDIO_BITRATE
        val estimatedBytes = (estimateRate * seconds / 8.0).toLong()

        return Plan(
            itemEffects = itemEffects,
            compositionEffects = compositionEffects,
            bitrate = if (safe) null else bitrate.toInt(),
            keyframeSeconds = options.datamosh?.keyframeSeconds,
            fastTrim = fastTrim,
            forceH264 = safe || mosh != null,
            estimatedBytes = estimatedBytes,
            muted = options.muted,
            itemAudio = itemAudio,
            fadeInUs = fadeInUs,
            fadeOutUs = fadeOutUs,
            totalUs = totalUs,
            music = music,
            forceAudioTrack = music != null || (merged && !options.muted),
            mosh = mosh
        )
    }

    private fun evenAtLeast(value: Int): Int {
        val safe = value.coerceAtLeast(2)
        return safe - safe % 2
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
