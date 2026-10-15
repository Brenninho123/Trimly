package com.brenninho.trimly.data

import android.net.Uri
import android.util.Base64
import java.net.URL

enum class LinkKind {
    DIRECT,
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    GITHUB,
    IMGUR,
    HLS,
    DASH,
    PAGE,
    UNSUPPORTED
}

data class LinkInfo(
    val original: String,
    val url: String,
    val kind: LinkKind,
    val site: String? = null
) {
    val isStream: Boolean
        get() = kind == LinkKind.HLS || kind == LinkKind.DASH
}

object LinkResolver {

    private val videoExtensions = setOf(
        "mp4", "m4v", "mov", "webm", "mkv", "3gp", "3gpp", "avi", "ts", "mts", "m2ts",
        "mpg", "mpeg", "flv", "wmv", "ogv", "gifv"
    )

    private val unsupportedSites = linkedMapOf(
        "youtube.com" to "YouTube",
        "youtu.be" to "YouTube",
        "youtube-nocookie.com" to "YouTube",
        "tiktok.com" to "TikTok",
        "instagram.com" to "Instagram",
        "facebook.com" to "Facebook",
        "fb.watch" to "Facebook",
        "twitter.com" to "X",
        "x.com" to "X",
        "twitch.tv" to "Twitch",
        "kick.com" to "Kick",
        "vimeo.com" to "Vimeo",
        "dailymotion.com" to "Dailymotion",
        "netflix.com" to "Netflix",
        "primevideo.com" to "Prime Video",
        "disneyplus.com" to "Disney+",
        "kwai.com" to "Kwai",
        "spotify.com" to "Spotify"
    )

    private val driveIdPath = Regex("/file/d/([A-Za-z0-9_-]{10,})")
    private val metaTag = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val mediaTag = Regex("<(?:video|source)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val formTag = Regex("<form\\b([^>]*)>(.*?)</form>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val inputTag = Regex("<input\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val attribute = Regex("([a-zA-Z:_-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))")
    private val jsonUrl = Regex(
        "\"(?:contentUrl|contentURL|videoUrl|video_url|playbackUrl|playback_url|hlsUrl|hls_url|mp4Url|mp4_url|downloadUrl|download_url)\"\\s*:\\s*\"([^\"]+)\""
    )
    private val looseLink = Regex(
        "(?:href|src|data-src|data-video|data-url)\\s*=\\s*[\"']([^\"']+\\.(?:mp4|m4v|mov|webm|mkv|m3u8|mpd)(?:\\?[^\"']*)?)[\"']",
        RegexOption.IGNORE_CASE
    )
    private val bareLink = Regex(
        "https?:\\\\?/\\\\?/[^\\s\"'<>]+?\\.(?:mp4|m4v|webm|mov|m3u8)(?:\\?[^\\s\"'<>]*)?",
        RegexOption.IGNORE_CASE
    )
    private val metaKeys = listOf(
        "og:video:secure_url",
        "og:video:url",
        "og:video",
        "twitter:player:stream",
        "twitter:player:stream:url"
    )

    fun inspect(url: String): LinkInfo {
        val uri = Uri.parse(url)
        val host = cleanHost(uri.host)
        val path = uri.path.orEmpty()
        val lowerPath = path.lowercase()
        val extension = lowerPath.substringAfterLast('/').substringAfterLast('.', "")

        siteFor(host)?.let { return LinkInfo(url, url, LinkKind.UNSUPPORTED, it) }

        return when {
            host == "drive.google.com" || host == "docs.google.com" || host == "drive.usercontent.google.com" ->
                LinkInfo(url, driveUrl(uri) ?: url, LinkKind.GOOGLE_DRIVE)

            host == "dropbox.com" || host.endsWith(".dropbox.com") || host == "dl.dropboxusercontent.com" ->
                LinkInfo(url, dropboxUrl(uri), LinkKind.DROPBOX)

            host == "1drv.ms" || host == "onedrive.live.com" ->
                LinkInfo(url, oneDriveUrl(url), LinkKind.ONEDRIVE)

            host == "github.com" && (path.contains("/blob/") || path.contains("/raw/")) ->
                LinkInfo(url, gitHubUrl(uri), LinkKind.GITHUB)

            host == "raw.githubusercontent.com" || (host == "github.com" && path.contains("/releases/download/")) ->
                LinkInfo(url, url, LinkKind.GITHUB)

            host == "imgur.com" || host == "i.imgur.com" ->
                LinkInfo(url, imgurUrl(uri) ?: url, LinkKind.IMGUR)

            extension == "m3u8" -> LinkInfo(url, url, LinkKind.HLS)
            extension == "mpd" -> LinkInfo(url, url, LinkKind.DASH)
            extension in videoExtensions -> LinkInfo(url, url, LinkKind.DIRECT)
            else -> LinkInfo(url, url, LinkKind.PAGE)
        }
    }

    fun findInHtml(html: String, baseUrl: String): String? {
        candidates(html).forEach { raw ->
            val resolved = absolute(raw, baseUrl) ?: return@forEach
            if (resolved != baseUrl && inspect(resolved).kind != LinkKind.UNSUPPORTED) return resolved
        }
        return null
    }

    private fun candidates(html: String): Sequence<String> = sequence {
        val metas = metaTag.findAll(html).map { attributes(it.value) }.toList()
        for (key in metaKeys) {
            metas.firstOrNull { (it["property"] ?: it["name"])?.lowercase() == key }
                ?.get("content")
                ?.let { yield(it) }
        }

        downloadForm(html)?.let { yield(it) }

        mediaTag.findAll(html).forEach { match ->
            val attrs = attributes(match.value)
            (attrs["src"] ?: attrs["data-src"])?.let { yield(it) }
        }

        jsonUrl.findAll(html).forEach { yield(unescapeJson(it.groupValues[1])) }
        looseLink.findAll(html).forEach { yield(it.groupValues[1]) }
        bareLink.findAll(html).forEach { yield(unescapeJson(it.value)) }
    }

    private fun downloadForm(html: String): String? {
        formTag.findAll(html).forEach { match ->
            val formAttrs = attributes("<form ${match.groupValues[1]}>")
            val action = formAttrs["action"] ?: return@forEach
            val id = formAttrs["id"].orEmpty()
            if (!action.contains("download", ignoreCase = true) && !id.contains("download", ignoreCase = true)) {
                return@forEach
            }
            val builder = Uri.parse(decodeEntities(action)).buildUpon()
            inputTag.findAll(match.groupValues[2]).forEach { input ->
                val attrs = attributes(input.value)
                val name = attrs["name"]
                val value = attrs["value"]
                if (!name.isNullOrBlank() && value != null) builder.appendQueryParameter(name, value)
            }
            return builder.build().toString()
        }
        return null
    }

    private fun attributes(tag: String): Map<String, String> {
        val map = HashMap<String, String>()
        attribute.findAll(tag).forEach { match ->
            val value = match.groupValues[3].ifEmpty { match.groupValues[4].ifEmpty { match.groupValues[5] } }
            map[match.groupValues[1].lowercase()] = decodeEntities(value)
        }
        return map
    }

    private fun absolute(raw: String, base: String): String? {
        val text = decodeEntities(raw.trim())
        if (text.isEmpty() || text.startsWith("blob:") || text.startsWith("data:") || text.startsWith("javascript:")) {
            return null
        }
        return try {
            val resolved = URL(URL(base), text).toString()
            val scheme = Uri.parse(resolved).scheme?.lowercase()
            if (scheme == "http" || scheme == "https") resolved else null
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeEntities(text: String): String = text
        .replace("&amp;", "&")
        .replace("&#38;", "&")
        .replace("&#x26;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&#x2F;", "/")
        .replace("&#47;", "/")

    private fun unescapeJson(text: String): String = text
        .replace("\\/", "/")
        .replace("\\u0026", "&")
        .replace("\\u002F", "/")
        .replace("\\u003D", "=")

    private fun cleanHost(host: String?): String {
        var value = host.orEmpty().lowercase()
        listOf("www.", "m.", "mobile.").forEach { prefix ->
            if (value.startsWith(prefix)) value = value.removePrefix(prefix)
        }
        return value
    }

    private fun siteFor(host: String): String? =
        unsupportedSites.entries.firstOrNull { (domain, _) -> host == domain || host.endsWith(".$domain") }?.value

    private fun driveUrl(uri: Uri): String? {
        val id = driveIdPath.find(uri.path.orEmpty())?.groupValues?.get(1)
            ?: uri.getQueryParameter("id")
            ?: return null
        return "https://drive.usercontent.google.com/download?id=$id&export=download&confirm=t"
    }

    private fun dropboxUrl(uri: Uri): String {
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames.filter { it != "dl" && it != "raw" }.forEach { name ->
            uri.getQueryParameters(name).forEach { builder.appendQueryParameter(name, it) }
        }
        builder.appendQueryParameter("dl", "1")
        return builder.build().toString()
    }

    private fun oneDriveUrl(url: String): String {
        val encoded = Base64.encodeToString(
            url.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
        return "https://api.onedrive.com/v1.0/shares/u!$encoded/root/content"
    }

    private fun gitHubUrl(uri: Uri): String {
        val parts = uri.pathSegments
        if (parts.size < 5) return uri.toString()
        val rest = parts.drop(4).joinToString("/") { Uri.encode(it) }
        return "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}/${parts[3]}/$rest"
    }

    private fun imgurUrl(uri: Uri): String? {
        val parts = uri.pathSegments
        if (parts.size != 1) return null
        val id = parts[0].substringBeforeLast('.')
        if (id.isBlank() || id in setOf("a", "gallery", "t", "user")) return null
        return "https://i.imgur.com/$id.mp4"
    }
}
