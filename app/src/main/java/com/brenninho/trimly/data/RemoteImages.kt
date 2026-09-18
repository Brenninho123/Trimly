package com.brenninho.trimly.data

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RemoteImages {

    private val cache = LruCache<String, ImageBitmap>(24)

    suspend fun load(url: String): ImageBitmap? {
        if (url.isBlank()) return null
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    connection.setRequestProperty("User-Agent", "Trimly/1.0")
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                    val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                    bitmap?.asImageBitmap()?.also { cache.put(url, it) }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
