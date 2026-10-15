package com.brenninho.trimly.engine

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.brenninho.trimly.model.MoshMode
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MoshPlan(
    val mode: MoshMode,
    val startUs: Long,
    val endUs: Long,
    val bloomFrames: Int
)

object Datamosher {

    private const val DEFAULT_BUFFER = 2 * 1024 * 1024

    suspend fun process(
        input: File,
        output: File,
        plan: MoshPlan,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val video = MediaExtractor()
        val audio = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        try {
            video.setDataSource(input.absolutePath)
            audio.setDataSource(input.absolutePath)

            val videoTrack = findTrack(video, "video/") ?: throw IOException("No video track to datamosh")
            val audioTrack = findTrack(audio, "audio/")
            video.selectTrack(videoTrack)
            audioTrack?.let { audio.selectTrack(it) }

            val videoFormat = video.getTrackFormat(videoTrack)
            val audioFormat = audioTrack?.let { audio.getTrackFormat(it) }
            val durationUs = videoFormat.longOr(MediaFormat.KEY_DURATION, 1L).coerceAtLeast(1L)

            val mux = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mux
            if (videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                mux.setOrientationHint(videoFormat.getInteger(MediaFormat.KEY_ROTATION))
            }
            val videoOut = mux.addTrack(videoFormat)
            val audioOut = audioFormat?.let { mux.addTrack(it) }
            mux.start()
            started = true

            val size = maxOf(
                videoFormat.intOr(MediaFormat.KEY_MAX_INPUT_SIZE, 0),
                audioFormat?.intOr(MediaFormat.KEY_MAX_INPUT_SIZE, 0) ?: 0,
                DEFAULT_BUFFER
            )
            val buffer = ByteBuffer.allocateDirect(size)
            val info = MediaCodec.BufferInfo()

            var firstVideo = true
            var bloomSample: ByteArray? = null
            var bloomLeft = 0
            var bloomDone = false
            var audioDone = audioTrack == null
            var videoDone = false
            var lastReport = -1

            while (!videoDone || !audioDone) {
                if (!currentCoroutineContext().isActive) throw CancellationException()

                val videoTime = if (videoDone) Long.MAX_VALUE else video.sampleTime
                val audioTime = if (audioDone) Long.MAX_VALUE else audio.sampleTime
                if (videoTime < 0L) {
                    videoDone = true
                    continue
                }
                if (audioTime < 0L) {
                    audioDone = true
                    continue
                }

                if (videoTime <= audioTime) {
                    buffer.clear()
                    val read = video.readSampleData(buffer, 0)
                    if (read < 0) {
                        videoDone = true
                        continue
                    }
                    val sync = (video.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                    val inRange = videoTime >= plan.startUs && videoTime <= plan.endUs

                    when {
                        firstVideo -> {
                            write(mux, videoOut, buffer, read, videoTime, sync, info)
                            firstVideo = false
                        }

                        plan.mode == MoshMode.MELT && inRange && sync -> Unit

                        plan.mode == MoshMode.BLOOM && inRange && !bloomDone -> {
                            val captured = bloomSample
                            when {
                                captured == null && sync -> Unit
                                captured == null -> {
                                    val bytes = ByteArray(read)
                                    buffer.position(0)
                                    buffer.get(bytes, 0, read)
                                    bloomSample = bytes
                                    bloomLeft = plan.bloomFrames
                                    write(mux, videoOut, ByteBuffer.wrap(bytes), read, videoTime, false, info)
                                }
                                bloomLeft > 0 -> {
                                    write(mux, videoOut, ByteBuffer.wrap(captured), captured.size, videoTime, false, info)
                                    bloomLeft -= 1
                                    if (bloomLeft == 0) bloomDone = true
                                }
                                else -> Unit
                            }
                        }

                        plan.mode == MoshMode.BLOOM && inRange && sync -> Unit

                        else -> write(mux, videoOut, buffer, read, videoTime, sync, info)
                    }
                    video.advance()

                    val percent = (videoTime * 100L / durationUs).toInt().coerceIn(0, 100)
                    if (percent != lastReport) {
                        lastReport = percent
                        onProgress(percent / 100f)
                    }
                } else {
                    buffer.clear()
                    val read = audio.readSampleData(buffer, 0)
                    if (read < 0) {
                        audioDone = true
                        continue
                    }
                    val sync = (audio.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                    if (audioOut != null) write(mux, audioOut, buffer, read, audioTime, sync, info)
                    audio.advance()
                }
            }
            onProgress(1f)
        } catch (e: Exception) {
            output.delete()
            throw e
        } finally {
            video.release()
            audio.release()
            try {
                if (started) muxer?.stop()
            } catch (e: Exception) {
            }
            muxer?.release()
        }
    }

    private fun write(
        muxer: MediaMuxer,
        track: Int,
        data: ByteBuffer,
        size: Int,
        timeUs: Long,
        sync: Boolean,
        info: MediaCodec.BufferInfo
    ) {
        data.position(0)
        data.limit(size)
        info.set(0, size, timeUs, if (sync) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
        muxer.writeSampleData(track, data, info)
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) return index
        }
        return null
    }

    private fun MediaFormat.longOr(key: String, fallback: Long): Long =
        if (containsKey(key)) getLong(key) else fallback

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback
}
