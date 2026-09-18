package com.brenninho.trimly.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaSaver {

    data class Saved(
        val uri: Uri?,
        val location: String
    )

    suspend fun save(context: Context, file: File): Saved = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 29) {
            saveToGallery(context, file)
        } else {
            saveToAppFolder(context, file)
        }
    }

    private fun saveToGallery(context: Context, file: File): Saved {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Trimly")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create the gallery entry")

        try {
            val stream = resolver.openOutputStream(uri)
                ?: throw IOException("Could not open the gallery entry")
            stream.use { out -> file.inputStream().use { input -> input.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }

        file.delete()
        return Saved(uri, "Movies/Trimly")
    }

    private fun saveToAppFolder(context: Context, file: File): Saved {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val folder = File(base, "Trimly")
        folder.mkdirs()
        val target = File(folder, file.name)
        file.copyTo(target, overwrite = true)
        file.delete()
        return Saved(null, target.absolutePath)
    }
}
