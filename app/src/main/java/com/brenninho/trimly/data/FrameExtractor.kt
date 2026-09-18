package com.brenninho.trimly.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FrameExtractor {

    private const val FRAME_HEIGHT = 144

    suspend fun extract(
        context: Context,
        uri: Uri,
        durationMs: Long,
        count: Int,
        onFrame: (Int, ImageBitmap) -> Unit
    ) = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context.applicationContext, uri)
            for (index in 0 until count) {
                ensureActive()
                val timeUs = (durationMs * (index + 0.5) / count * 1000).toLong()
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: continue
                onFrame(index, scaled(frame).asImageBitmap())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        } finally {
            retriever.release()
        }
    }

    private fun scaled(source: Bitmap): Bitmap {
        if (source.height <= FRAME_HEIGHT) return source
        val ratio = FRAME_HEIGHT.toFloat() / source.height
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, FRAME_HEIGHT, true)
    }
}
