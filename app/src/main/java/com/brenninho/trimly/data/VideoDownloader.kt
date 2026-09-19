package com.brenninho.trimly.data

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.os.SystemClock
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DownloadFailure {
    INVALID_URL,
    INSECURE,
    NOT_VIDEO,
    PLAYLIST,
    TOO_LARGE,
    NO_SPACE,
    HTTP,
    NETWORK
}

class DownloadException(
    val reason: DownloadFailure,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

data class DownloadProgress(
    val bytes: Long,
    val total: Long?,
    val name: String
)

data class DownloadedVideo(
    val file: File,
    val name: String,
    val sizeBytes: Long
)

object VideoDownloader {

    private const val MAX_BYTES = 2L * 1024L * 1024L * 1024L
    private const val MAX_REDIRECTS = 5
    private const val BUFFER_SIZE = 64 * 1024
    private const val SPACE_MARGIN = 64L * 1024L * 1024L
    private const val KEEP_MS = 7L * 24L * 60L * 60L * 1000L
    private const val REPORT_INTERVAL_MS = 120L
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val USER_AGENT = "Trimly/1.0 (Android)"

    private val urlPattern = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
    private val dispositionPattern = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
    private val unsafeName = Regex("[^\\p{L}\\p{N}._ -]")
    private val opaqueTypes = setOf(
        "application/octet-stream",
        "binary/octet-stream",
        "application/mp4",
        "application/x-matroska",
        "application/force-download",
        "application/download"
    )

    fun normalize(input: String): String? {
        val text = input.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val withScheme = if (text.contains("://")) text else "https://$text"
        val uri = Uri.parse(withScheme)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return withScheme
    }

    fun extractUrl(text: String): String? {
        val match = urlPattern.find(text) ?: return null
        return match.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
    }

    suspend fun download(
        context: Context,
        rawUrl: String,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadedVideo = coroutineScope {
        val url = normalize(rawUrl) ?: throw DownloadException(DownloadFailure.INVALID_URL)
        if (url.startsWith("http://", ignoreCase = true)) {
            throw DownloadException(DownloadFailure.INSECURE)
        }

        val root = File(context.cacheDir, "imports")
        val holder = AtomicReference<HttpURLConnection?>(null)
        val closer = launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                holder.get()?.disconnect()
            }
        }

        try {
            withContext(Dispatchers.IO) {
                transfer(root, url, holder, { isActive }, onProgress)
            }
        } finally {
            closer.cancel()
            holder.get()?.disconnect()
        }
    }

    private fun transfer(
        root: File,
        url: String,
        holder: AtomicReference<HttpURLConnection?>,
        alive: () -> Boolean,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadedVideo {
        root.mkdirs()
        cleanup(root)

        var target: File? = null
        try {
            val connection = connect(url, holder)
            val finalUrl = connection.url.toString()
            val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            validateType(type, finalUrl)

            val length = connection.contentLengthLong.takeIf { it > 0L }
            if (length != null && length > MAX_BYTES) {
                throw DownloadException(DownloadFailure.TOO_LARGE)
            }
            val free = try {
                StatFs(root.path).availableBytes
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
            if (length != null && free < length + SPACE_MARGIN) {
                throw DownloadException(DownloadFailure.NO_SPACE)
            }

            val name = fileName(connection, finalUrl, type)
            val folder = File(root, hash(finalUrl))
            folder.mkdirs()
            val file = File(folder, name)
            target = file

            var written = 0L
            var lastReport = 0L
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (!alive()) throw CancellationException()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (written > MAX_BYTES) throw DownloadException(DownloadFailure.TOO_LARGE)
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastReport >= REPORT_INTERVAL_MS) {
                            lastReport = now
                            onProgress(DownloadProgress(written, length, name))
                        }
                    }
                }
            }

            if (written == 0L) throw DownloadException(DownloadFailure.NOT_VIDEO)
            onProgress(DownloadProgress(written, length, name))
            return DownloadedVideo(file, name, written)
        } catch (e: CancellationException) {
            target?.delete()
            throw e
        } catch (e: DownloadException) {
            target?.delete()
            throw e
        } catch (e: IOException) {
            target?.delete()
            if (!alive()) throw CancellationException()
            throw DownloadException(DownloadFailure.NETWORK, e.message, e)
        }
    }

    private fun connect(
        start: String,
        holder: AtomicReference<HttpURLConnection?>
    ): HttpURLConnection {
        var current = start
        var hops = 0
        while (true) {
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "video/*,application/octet-stream;q=0.9,*/*;q=0.5")
            holder.set(connection)

            val code = try {
                connection.responseCode
            } catch (e: IOException) {
                connection.disconnect()
                throw e
            }

            when {
                code in 200..299 -> return connection
                code in 300..399 -> {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrBlank() || hops >= MAX_REDIRECTS) {
                        throw DownloadException(DownloadFailure.HTTP, "redirect")
                    }
                    val next = URL(URL(current), location).toString()
                    if (next.startsWith("http://", ignoreCase = true)) {
                        throw DownloadException(DownloadFailure.INSECURE)
                    }
                    current = next
                    hops += 1
                }
                else -> {
                    connection.disconnect()
                    throw DownloadException(DownloadFailure.HTTP, code.toString())
                }
            }
        }
    }

    private fun validateType(type: String?, url: String) {
        val path = Uri.parse(url).path.orEmpty().lowercase()
        if (path.endsWith(".m3u8") || (type != null && type.contains("mpegurl"))) {
            throw DownloadException(DownloadFailure.PLAYLIST)
        }
        if (type == null) return
        val accepted = type.startsWith("video/") || type in opaqueTypes
        if (!accepted) throw DownloadException(DownloadFailure.NOT_VIDEO, type)
    }

    private fun fileName(connection: HttpURLConnection, url: String, type: String?): String {
        val disposition = connection.getHeaderField("Content-Disposition")
        val fromHeader = disposition?.let { dispositionPattern.find(it)?.groupValues?.get(1) }
        val fromPath = Uri.parse(url).lastPathSegment
        val raw = Uri.decode(fromHeader ?: fromPath ?: "video")
        var base = unsafeName.replace(raw, "_").trim().trimStart('.').take(80)
        if (base.isBlank()) base = "video"
        val extension = base.substringAfterLast('.', "")
        val hasExtension = base.contains('.') && extension.length in 2..4
        return if (hasExtension) base else "$base.${extensionFor(type)}"
    }

    private fun extensionFor(type: String?): String = when {
        type == null -> "mp4"
        type.contains("webm") -> "webm"
        type.contains("quicktime") -> "mov"
        type.contains("matroska") -> "mkv"
        type.contains("3gpp") -> "3gp"
        else -> "mp4"
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(12)
    }

    private fun cleanup(root: File) {
        val limit = System.currentTimeMillis() - KEEP_MS
        root.listFiles()?.forEach { entry ->
            if (entry.isDirectory) {
                entry.listFiles()?.forEach { file ->
                    if (file.lastModified() < limit) file.delete()
                }
                if (entry.listFiles().isNullOrEmpty()) entry.delete()
            } else if (entry.lastModified() < limit) {
                entry.delete()
            }
        }
    }
}
