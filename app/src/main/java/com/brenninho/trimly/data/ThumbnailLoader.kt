package com.brenninho.trimly.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailLoader {

    private const val MAX_WIDTH = 360

    private val cache = LruCache<String, ImageBitmap>(48)

    suspend fun load(context: Context, uri: String): ImageBitmap? {
        cache.get(uri)?.let { return it }
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context.applicationContext, Uri.parse(uri))
                val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                frame?.let { scaled(it).asImageBitmap() }?.also { cache.put(uri, it) }
            } catch (e: Exception) {
                null
            } finally {
                retriever.release()
            }
        }
    }

    private fun scaled(source: Bitmap): Bitmap {
        if (source.width <= MAX_WIDTH) return source
        val ratio = MAX_WIDTH.toFloat() / source.width
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, MAX_WIDTH, height, true)
    }
}
