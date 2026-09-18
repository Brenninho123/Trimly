package com.brenninho.trimly.engine

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.brenninho.trimly.model.ExportOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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
    ): File = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val holder = ProgressHolder()
            var running = true

            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        running = false
                        onProgress(1f)
                        if (continuation.isActive) continuation.resume(output)
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
                })
                .build()

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(source)
                .setClippingConfiguration(clipping)
                .build()

            val edited = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(options.muted)
                .setEffects(Effects(emptyList(), EffectsFactory.build(options)))
                .build()

            transformer.start(edited, output.absolutePath)

            val poll = object : Runnable {
                override fun run() {
                    if (!running) return
                    transformer.getProgress(holder)
                    onProgress(holder.progress / 100f)
                    handler.postDelayed(this, 200)
                }
            }
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
