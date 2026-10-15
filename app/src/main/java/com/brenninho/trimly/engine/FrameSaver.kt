package com.brenninho.trimly.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.brenninho.trimly.model.ColorMath
import com.brenninho.trimly.model.ExportOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FrameSaver {

    private const val QUALITY = 95
    private val BACKGROUND = 0xAA000000.toInt()

    suspend fun save(
        context: Context,
        uri: Uri,
        positionMs: Long,
        timelineMs: Long,
        options: ExportOptions
    ): MediaSaver.Saved = withContext(Dispatchers.IO) {
        val frame = grab(context, uri, positionMs)
        val transformed = transform(frame, options)
        if (transformed !== frame) frame.recycle()
        val shaped = crop(transformed, options)
        val cropped = if (shaped.isMutable) shaped else shaped.copy(Bitmap.Config.ARGB_8888, true)
        if (cropped !== shaped) shaped.recycle()
        if (shaped !== transformed && cropped !== transformed) transformed.recycle()
        drawTexts(cropped, options, timelineMs)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = "Trimly_$stamp.jpg"
        try {
            if (Build.VERSION.SDK_INT >= 29) toGallery(context, cropped, name) else toAppFolder(context, cropped, name)
        } finally {
            cropped.recycle()
        }
    }

    private fun grab(context: Context, uri: Uri, positionMs: Long): Bitmap {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(positionMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: throw IOException("Could not read this frame")
        } finally {
            retriever.release()
        }
    }

    private fun transform(source: Bitmap, options: ExportOptions): Bitmap {
        val matrix = Matrix()
        val degrees = options.rotationDegrees % 360
        if (degrees != 0) matrix.postRotate(-degrees.toFloat())
        if (options.flipHorizontal) matrix.postScale(-1f, 1f)
        val colors = ColorMath.combined(options)
        if (matrix.isIdentity && colors == null) return source

        val bounds = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        matrix.mapRect(bounds)
        matrix.postTranslate(-bounds.left, -bounds.top)

        val result = Bitmap.createBitmap(
            bounds.width().toInt().coerceAtLeast(1),
            bounds.height().toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        if (colors != null) paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(colors))
        Canvas(result).drawBitmap(source, matrix, paint)
        return result
    }

    private fun crop(source: Bitmap, options: ExportOptions): Bitmap {
        if (!options.hasAspect) return source
        val (width, height) = options.aspect.crop(source.width, source.height)
        if (width >= source.width && height >= source.height) return source
        val left = ((source.width - width) / 2).coerceAtLeast(0)
        val top = ((source.height - height) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(
            source,
            left,
            top,
            width.coerceAtMost(source.width - left),
            height.coerceAtMost(source.height - top)
        )
    }

    private fun drawTexts(bitmap: Bitmap, options: ExportOptions, timelineMs: Long) {
        val visible = options.texts.filter { it.text.isNotBlank() && it.visibleAt(timelineMs) }
        if (visible.isEmpty()) return
        val canvas = Canvas(bitmap)
        visible.forEach { item ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorArgb
                textSize = (bitmap.height * item.sizeFraction).coerceAtLeast(12f)
                typeface = if (item.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            val lines = item.text.split('\n')
            val lineHeight = paint.fontSpacing
            val centerX = bitmap.width * item.x
            val centerY = bitmap.height * item.y
            val top = centerY - lineHeight * lines.size / 2f
            lines.forEachIndexed { index, line ->
                val baseline = top + lineHeight * index - paint.ascent()
                if (item.background) {
                    val half = paint.measureText(line) / 2f
                    val background = Paint().apply { color = BACKGROUND }
                    canvas.drawRect(
                        centerX - half,
                        top + lineHeight * index,
                        centerX + half,
                        top + lineHeight * (index + 1),
                        background
                    )
                }
                canvas.drawText(line, centerX, baseline, paint)
            }
        }
    }

    private fun toGallery(context: Context, bitmap: Bitmap, name: String): MediaSaver.Saved {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Trimly")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create the gallery entry")
        try {
            val stream = resolver.openOutputStream(uri) ?: throw IOException("Could not open the gallery entry")
            stream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return MediaSaver.Saved(uri, "Pictures/Trimly")
    }

    private fun toAppFolder(context: Context, bitmap: Bitmap, name: String): MediaSaver.Saved {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val folder = File(base, "Trimly")
        folder.mkdirs()
        val file = File(folder, name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        return MediaSaver.Saved(null, file.absolutePath)
    }
}
