package com.brenninho.trimly.data

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.os.SystemClock
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
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
    NO_VIDEO_IN_PAGE,
    UNSUPPORTED_SITE,
    LIVE_STREAM,
    STREAM,
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
    val name: String,
    val stage: ImportStage = ImportStage.DOWNLOADING,
    val fraction: Float? = null,
    val bytesPerSecond: Long? = null
)

data class DownloadedVideo(
    val file: File,
    val name: String,
    val sizeBytes: Long
)

private sealed interface Fetched {
    class Video(val video: DownloadedVideo) : Fetched
    class Stream(val url: String, val kind: LinkKind) : Fetched
}

object VideoDownloader {

    private const val MAX_BYTES = 4L * 1024L * 1024L * 1024L
    private const val MAX_REDIRECTS = 8
    private const val MAX_PAGE_HOPS = 3
    private const val MAX_PAGE_BYTES = 3 * 1024 * 1024
    private const val SNIFF_BYTES = 1024
    private const val MAX_RESUMES = 5
    private const val CONNECT_ATTEMPTS = 3
    private const val BUFFER_SIZE = 64 * 1024
    private const val SPACE_MARGIN = 64L * 1024L * 1024L
    private const val KEEP_MS = 7L * 24L * 60L * 60L * 1000L
    private const val REPORT_INTERVAL_MS = 150L
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36 Trimly/0.2"

    private val urlPattern = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
    private val bareDomainPattern = Regex(
        "(?:^|\\s)((?:[a-z0-9-]+\\.)+[a-z]{2,}/[^\\s\"'<>]*)",
        RegexOption.IGNORE_CASE
    )
    private val dispositionStar = Regex("filename\\*\\s*=\\s*(?:[\\w-]+)?'[^']*'([^;]+)", RegexOption.IGNORE_CASE)
    private val dispositionPlain = Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
    private val charsetPattern = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE)
    private val unsafeName = Regex("[^\\p{L}\\p{N}._ -]")
    private val opaqueTypes = setOf(
        "application/octet-stream",
        "binary/octet-stream",
        "application/mp4",
        "application/x-matroska",
        "application/force-download",
        "application/download",
        "application/x-download",
        "application/binary",
        "application/unknown"
    )
    private val retryCodes = setOf(408, 425, 429, 500, 502, 503, 504)

    fun normalize(input: String): String? {
        val text = input.trim().trim('<', '>', '"', '\'')
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val withScheme = if (text.contains("://")) text else "https://$text"
        val uri = Uri.parse(withScheme)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host
        if (host.isNullOrBlank() || !host.contains('.')) return null
        return withScheme
    }

    fun extractUrl(text: String): String? {
        val match = urlPattern.find(text)?.value ?: bareDomainPattern.find(text)?.groupValues?.get(1)
        return match?.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'')
    }

    fun extractUrls(text: String): List<String> =
        urlPattern.findAll(text)
            .map { it.value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'') }
            .distinct()
            .toList()

    fun inspect(input: String): LinkInfo? {
        val url = normalize(input) ?: return null
        return LinkResolver.inspect(secure(url))
    }

    suspend fun download(
        context: Context,
        rawUrl: String,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadedVideo = coroutineScope {
        val normalized = normalize(rawUrl) ?: throw DownloadException(DownloadFailure.INVALID_URL)
        val upgraded = normalized.startsWith("http://", ignoreCase = true)
        val info = LinkResolver.inspect(secure(normalized))
        if (info.kind == LinkKind.UNSUPPORTED) {
            throw DownloadException(DownloadFailure.UNSUPPORTED_SITE, info.site)
        }

        val root = File(context.cacheDir, "imports")
        withContext(Dispatchers.IO) {
            root.mkdirs()
            cleanup(root)
        }

        if (info.isStream) {
            return@coroutineScope StreamImporter.import(context, info.url, info.kind, File(root, hash(info.url)), onProgress)
        }

        val holder = AtomicReference<HttpURLConnection?>(null)
        val closer = launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                holder.get()?.disconnect()
            }
        }

        val fetched = try {
            withContext(Dispatchers.IO) {
                fetch(root, info.url, holder, { isActive }, onProgress)
            }
        } catch (e: DownloadException) {
            if (upgraded && e.reason == DownloadFailure.NETWORK) {
                throw DownloadException(DownloadFailure.INSECURE, e.message, e)
            }
            throw e
        } finally {
            closer.cancel()
            holder.get()?.disconnect()
        }

        when (fetched) {
            is Fetched.Video -> fetched.video
            is Fetched.Stream -> StreamImporter.import(
                context,
                fetched.url,
                fetched.kind,
                File(root, hash(fetched.url)),
                onProgress
            )
        }
    }

    private fun secure(url: String): String =
        if (url.startsWith("http://", ignoreCase = true)) "https://" + url.substring(7) else url

    private fun fetch(
        root: File,
        start: String,
        holder: AtomicReference<HttpURLConnection?>,
        alive: () -> Boolean,
        onProgress: (DownloadProgress) -> Unit
    ): Fetched {
        var url = start
        var hops = 0
        val label = Uri.parse(start).lastPathSegment ?: Uri.parse(start).host.orEmpty()

        while (true) {
            if (!alive()) throw CancellationException()
            val stage = if (hops == 0) ImportStage.CONNECTING else ImportStage.RESOLVING
            onProgress(DownloadProgress(0L, null, label, stage))

            val connection = connectWithRetry(url, holder, 0L, alive)
            val finalUrl = connection.url.toString()
            val type = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            val path = Uri.parse(finalUrl).path.orEmpty().lowercase()

            if (isHls(type, path)) {
                connection.disconnect()
                return Fetched.Stream(finalUrl, LinkKind.HLS)
            }
            if (isDash(type, path)) {
                connection.disconnect()
                return Fetched.Stream(finalUrl, LinkKind.DASH)
            }

            val input = try {
                BufferedInputStream(connection.inputStream, BUFFER_SIZE)
            } catch (e: IOException) {
                connection.disconnect()
                if (!alive()) throw CancellationException()
                throw DownloadException(DownloadFailure.NETWORK, e.message, e)
            }
            val head = peek(input)

            if (looksLikeHls(head)) {
                input.close()
                connection.disconnect()
                return Fetched.Stream(finalUrl, LinkKind.HLS)
            }
            if (looksLikeDash(head)) {
                input.close()
                connection.disconnect()
                return Fetched.Stream(finalUrl, LinkKind.DASH)
            }

            val videoType = type != null && type.startsWith("video/")
            if (!videoType && (isHtml(type) || looksLikeHtml(head))) {
                if (hops >= MAX_PAGE_HOPS) {
                    input.close()
                    connection.disconnect()
                    throw DownloadException(DownloadFailure.NO_VIDEO_IN_PAGE)
                }
                onProgress(DownloadProgress(0L, null, label, ImportStage.RESOLVING))
                val page = readPage(input, charsetOf(connection.contentType))
                input.close()
                connection.disconnect()

                val found = LinkResolver.findInHtml(page, finalUrl)
                    ?: throw DownloadException(DownloadFailure.NO_VIDEO_IN_PAGE)
                val next = LinkResolver.inspect(secure(found))
                if (next.kind == LinkKind.UNSUPPORTED) {
                    throw DownloadException(DownloadFailure.UNSUPPORTED_SITE, next.site)
                }
                if (next.isStream) return Fetched.Stream(next.url, next.kind)
                url = next.url
                hops += 1
                continue
            }

            if (type != null && !videoType && type !in opaqueTypes && !looksLikeVideo(head)) {
                input.close()
                connection.disconnect()
                throw DownloadException(DownloadFailure.NOT_VIDEO, type)
            }

            return Fetched.Video(transfer(root, connection, input, finalUrl, type, holder, alive, onProgress))
        }
    }

    private fun transfer(
        root: File,
        first: HttpURLConnection,
        firstInput: InputStream,
        url: String,
        type: String?,
        holder: AtomicReference<HttpURLConnection?>,
        alive: () -> Boolean,
        onProgress: (DownloadProgress) -> Unit
    ): DownloadedVideo {
        val length = first.contentLengthLong.takeIf { it > 0L }
        if (length != null && length > MAX_BYTES) {
            firstInput.close()
            first.disconnect()
            throw DownloadException(DownloadFailure.TOO_LARGE)
        }
        val free = try {
            StatFs(root.path).availableBytes
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
        if (length != null && free < length + SPACE_MARGIN) {
            firstInput.close()
            first.disconnect()
            throw DownloadException(DownloadFailure.NO_SPACE)
        }

        val name = fileName(first, url, type)
        val folder = File(root, hash(url))
        folder.mkdirs()
        val file = File(folder, name)

        var connection = first
        var input = firstInput
        var written = 0L
        var resumes = 0
        var lastReport = 0L
        var lastBytes = 0L
        var speed: Double? = null

        try {
            while (true) {
                try {
                    FileOutputStream(file, written > 0L).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            if (!alive()) throw CancellationException()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (written > MAX_BYTES) throw DownloadException(DownloadFailure.TOO_LARGE)
                            val now = SystemClock.elapsedRealtime()
                            val elapsed = now - lastReport
                            if (elapsed >= REPORT_INTERVAL_MS) {
                                if (lastReport > 0L) {
                                    val instant = (written - lastBytes) * 1000.0 / elapsed
                                    speed = speed?.let { it * 0.75 + instant * 0.25 } ?: instant
                                }
                                lastReport = now
                                lastBytes = written
                                onProgress(progress(written, length, name, speed))
                            }
                        }
                    }
                    if (length != null && written < length) throw IOException("Connection closed early")
                    break
                } catch (e: IOException) {
                    input.closeQuietly()
                    connection.disconnect()
                    if (!alive()) throw CancellationException()
                    resumes += 1
                    if (resumes > MAX_RESUMES) throw DownloadException(DownloadFailure.NETWORK, e.message, e)

                    onProgress(DownloadProgress(written, length, name, ImportStage.RETRYING, fraction(written, length)))
                    pause(1000L * resumes, alive)

                    connection = connectWithRetry(url, holder, written, alive)
                    if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                        written = 0L
                        lastBytes = 0L
                        file.delete()
                    }
                    input = BufferedInputStream(connection.inputStream, BUFFER_SIZE)
                }
            }
        } catch (e: CancellationException) {
            input.closeQuietly()
            file.delete()
            throw e
        } catch (e: DownloadException) {
            input.closeQuietly()
            file.delete()
            throw e
        } catch (e: IOException) {
            input.closeQuietly()
            file.delete()
            if (!alive()) throw CancellationException()
            throw DownloadException(DownloadFailure.NETWORK, e.message, e)
        }

        input.closeQuietly()
        connection.disconnect()
        if (written == 0L) {
            file.delete()
            throw DownloadException(DownloadFailure.NOT_VIDEO)
        }
        onProgress(progress(written, length ?: written, name, speed))
        return DownloadedVideo(file, name, written)
    }

    private fun progress(bytes: Long, total: Long?, name: String, speed: Double?) = DownloadProgress(
        bytes = bytes,
        total = total,
        name = name,
        stage = ImportStage.DOWNLOADING,
        fraction = fraction(bytes, total),
        bytesPerSecond = speed?.toLong()
    )

    private fun fraction(bytes: Long, total: Long?): Float? =
        if (total != null && total > 0L) (bytes.toFloat() / total).coerceIn(0f, 1f) else null

    private fun connectWithRetry(
        url: String,
        holder: AtomicReference<HttpURLConnection?>,
        rangeFrom: Long,
        alive: () -> Boolean
    ): HttpURLConnection {
        var attempt = 0
        while (true) {
            attempt += 1
            try {
                return connect(url, holder, rangeFrom)
            } catch (e: DownloadException) {
                val code = e.message?.toIntOrNull()
                val retryable = e.reason == DownloadFailure.HTTP && code != null && code in retryCodes
                if (!retryable || attempt >= CONNECT_ATTEMPTS) throw e
            } catch (e: IOException) {
                if (!alive()) throw CancellationException()
                if (attempt >= CONNECT_ATTEMPTS) throw DownloadException(DownloadFailure.NETWORK, e.message, e)
            }
            if (!alive()) throw CancellationException()
            pause(800L * attempt, alive)
        }
    }

    private fun connect(
        start: String,
        holder: AtomicReference<HttpURLConnection?>,
        rangeFrom: Long
    ): HttpURLConnection {
        var current = start
        var hops = 0
        var referer: String? = null
        while (true) {
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty(
                "Accept",
                "video/*,application/vnd.apple.mpegurl,application/dash+xml,application/octet-stream;q=0.9,text/html;q=0.8,*/*;q=0.5"
            )
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.8")
            if (referer != null) connection.setRequestProperty("Referer", referer)
            if (rangeFrom > 0L) connection.setRequestProperty("Range", "bytes=$rangeFrom-")
            holder.set(connection)

            val code = try {
                connection.responseCode
            } catch (e: IOException) {
                connection.disconnect()
                throw e
            }

            when {
                code in 200..299 -> return connection
                code in 300..399 && code != 304 -> {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrBlank() || hops >= MAX_REDIRECTS) {
                        throw DownloadException(DownloadFailure.HTTP, code.toString())
                    }
                    val next = URL(URL(current), location).toString()
                    if (next.startsWith("http://", ignoreCase = true)) {
                        throw DownloadException(DownloadFailure.INSECURE)
                    }
                    LinkResolver.inspect(next).takeIf { it.kind == LinkKind.UNSUPPORTED }?.let {
                        throw DownloadException(DownloadFailure.UNSUPPORTED_SITE, it.site)
                    }
                    referer = current
                    current = next
                    hops += 1
                }
                code == 416 && rangeFrom > 0L -> {
                    connection.disconnect()
                    return connect(current, holder, 0L)
                }
                else -> {
                    connection.disconnect()
                    throw DownloadException(DownloadFailure.HTTP, code.toString())
                }
            }
        }
    }

    private fun pause(ms: Long, alive: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + ms
        while (SystemClock.elapsedRealtime() < until) {
            if (!alive()) throw CancellationException()
            try {
                Thread.sleep(100L)
            } catch (e: InterruptedException) {
                throw CancellationException()
            }
        }
    }

    private fun peek(input: BufferedInputStream): ByteArray {
        input.mark(SNIFF_BYTES)
        val buffer = ByteArray(SNIFF_BYTES)
        var filled = 0
        while (filled < SNIFF_BYTES) {
            val read = input.read(buffer, filled, SNIFF_BYTES - filled)
            if (read < 0) break
            filled += read
        }
        input.reset()
        return buffer.copyOf(filled)
    }

    private fun readPage(input: InputStream, charset: Charset): String {
        val buffer = ByteArray(BUFFER_SIZE)
        val out = java.io.ByteArrayOutputStream()
        while (out.size() < MAX_PAGE_BYTES) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return String(out.toByteArray(), charset)
    }

    private fun charsetOf(contentType: String?): Charset {
        val name = contentType?.let { charsetPattern.find(it)?.groupValues?.get(1) } ?: return Charsets.UTF_8
        return try {
            Charset.forName(name)
        } catch (e: Exception) {
            Charsets.UTF_8
        }
    }

    private fun isHls(type: String?, path: String): Boolean =
        path.endsWith(".m3u8") || (type != null && type.contains("mpegurl"))

    private fun isDash(type: String?, path: String): Boolean =
        path.endsWith(".mpd") || type == "application/dash+xml"

    private fun isHtml(type: String?): Boolean =
        type != null && type.contains("html")

    private fun text(head: ByteArray): String =
        String(head, Charsets.ISO_8859_1).trimStart('\uFEFF', ' ', '\n', '\r', '\t').lowercase()

    private fun looksLikeHls(head: ByteArray): Boolean = text(head).startsWith("#extm3u")

    private fun looksLikeDash(head: ByteArray): Boolean {
        val start = text(head)
        return start.startsWith("<?xml") && start.contains("<mpd")
    }

    private fun looksLikeHtml(head: ByteArray): Boolean {
        val start = text(head)
        return start.startsWith("<!doctype html") || start.startsWith("<html") ||
            start.startsWith("<head") || (start.startsWith("<") && start.contains("<html"))
    }

    private fun looksLikeVideo(head: ByteArray): Boolean {
        if (head.size < 12) return false
        val ftyp = head[4] == 'f'.code.toByte() && head[5] == 't'.code.toByte() &&
            head[6] == 'y'.code.toByte() && head[7] == 'p'.code.toByte()
        val ebml = head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() &&
            head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()
        val riff = head[0] == 'R'.code.toByte() && head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() && head[3] == 'F'.code.toByte()
        val transport = head[0] == 0x47.toByte()
        val flv = head[0] == 'F'.code.toByte() && head[1] == 'L'.code.toByte() && head[2] == 'V'.code.toByte()
        return ftyp || ebml || riff || transport || flv
    }

    private fun fileName(connection: HttpURLConnection, url: String, type: String?): String {
        val disposition = connection.getHeaderField("Content-Disposition")
        val fromHeader = disposition?.let {
            dispositionStar.find(it)?.groupValues?.get(1) ?: dispositionPlain.find(it)?.groupValues?.get(1)
        }
        val fromPath = Uri.parse(url).lastPathSegment?.takeIf { it.contains('.') }
        val raw = Uri.decode(fromHeader ?: fromPath ?: "video")
        var base = unsafeName.replace(raw, "_").trim().trimStart('.').take(80)
        if (base.isBlank()) base = "video"
        val extension = base.substringAfterLast('.', "").lowercase()
        val hasExtension = base.contains('.') && extension.length in 2..4 && extension.all { it.isLetterOrDigit() }
        return if (hasExtension) base else "$base.${extensionFor(type)}"
    }

    private fun extensionFor(type: String?): String = when {
        type == null -> "mp4"
        type.contains("webm") -> "webm"
        type.contains("quicktime") -> "mov"
        type.contains("matroska") -> "mkv"
        type.contains("3gpp") -> "3gp"
        type.contains("mp2t") -> "ts"
        else -> "mp4"
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(12)
    }

    private fun InputStream.closeQuietly() {
        try {
            close()
        } catch (e: IOException) {
        }
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
