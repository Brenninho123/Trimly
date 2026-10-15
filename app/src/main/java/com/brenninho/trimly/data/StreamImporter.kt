package com.brenninho.trimly.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

object StreamImporter {

    private const val POLL_MS = 250L
    private const val STALL_MS = 90_000L

    suspend fun import(
        context: Context,
        url: String,
        kind: LinkKind,
        folder: File,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadedVideo {
        folder.mkdirs()
        val name = streamName(url)
        val file = File(folder, name)
        file.delete()

        onProgress(DownloadProgress(0L, null, name, ImportStage.CONVERTING, 0f))

        try {
            convert(context, url, kind, file, name, onProgress)
        } catch (e: DownloadException) {
            file.delete()
            throw e
        } catch (e: ExportException) {
            file.delete()
            throw DownloadException(DownloadFailure.STREAM, e.errorCodeName, e)
        }

        val size = file.length()
        if (size <= 0L) {
            file.delete()
            throw DownloadException(DownloadFailure.STREAM)
        }
        onProgress(DownloadProgress(size, size, name, ImportStage.CONVERTING, 1f))
        return DownloadedVideo(file, name, size)
    }

    private suspend fun convert(
        context: Context,
        url: String,
        kind: LinkKind,
        file: File,
        name: String,
        onProgress: (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val holder = ProgressHolder()
            var running = true
            var best = 0f
            var lastAdvance = SystemClock.elapsedRealtime()

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    running = false
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    running = false
                    if (continuation.isActive) continuation.resumeWithException(exportException)
                }
            }

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(listener)
                .build()

            val mime = if (kind == LinkKind.DASH) MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            val item = MediaItem.Builder()
                .setUri(url)
                .setMimeType(mime)
                .build()

            val fail: (DownloadFailure) -> Unit = { reason ->
                running = false
                transformer.cancel()
                if (continuation.isActive) continuation.resumeWithException(DownloadException(reason))
            }

            val poll = object : Runnable {
                override fun run() {
                    if (!running) return
                    val now = SystemClock.elapsedRealtime()
                    when (transformer.getProgress(holder)) {
                        Transformer.PROGRESS_STATE_UNAVAILABLE -> {
                            fail(DownloadFailure.LIVE_STREAM)
                            return
                        }

                        Transformer.PROGRESS_STATE_AVAILABLE -> {
                            val fraction = (holder.progress / 100f).coerceIn(0f, 1f)
                            if (fraction > best + 0.001f) {
                                best = fraction
                                lastAdvance = now
                            }
                            onProgress(DownloadProgress(file.length(), null, name, ImportStage.CONVERTING, best))
                        }

                        else -> Unit
                    }
                    if (now - lastAdvance > STALL_MS) {
                        fail(DownloadFailure.NETWORK)
                        return
                    }
                    handler.postDelayed(this, POLL_MS)
                }
            }

            transformer.start(EditedMediaItem.Builder(item).build(), file.absolutePath)
            handler.post(poll)

            continuation.invokeOnCancellation {
                running = false
                handler.post { transformer.cancel() }
            }
        }
    }

    private fun streamName(url: String): String {
        val segments = Uri.parse(url).pathSegments
        val meaningful = segments.lastOrNull { segment ->
            val base = segment.substringBeforeLast('.').lowercase()
            base.isNotBlank() && base !in setOf("index", "master", "playlist", "manifest", "stream", "video")
        }
        val base = meaningful?.substringBeforeLast('.')
            ?.replace(Regex("[^\\p{L}\\p{N}._ -]"), "_")
            ?.take(60)
            ?.ifBlank { null }
            ?: "stream"
        return "$base.mp4"
    }
}
