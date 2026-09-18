package com.brenninho.trimly.auth

import android.net.Uri
import com.brenninho.trimly.settings.DiscordProfile
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class DiscordRedirect(
    val token: String?,
    val state: String?,
    val error: String?
)

object DiscordAuth {

    const val CLIENT_ID = "1540653184530251847"
    const val REDIRECT_SCHEME = "com.brenninho.trimly"
    const val REDIRECT_HOST = "oauth"
    const val REDIRECT_PATH = "/discord"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST$REDIRECT_PATH"

    private const val SCOPE = "identify"
    private const val API = "https://discord.com/api/v10/users/@me"
    private const val USER_AGENT = "Trimly (https://github.com/Brenninho123/Trimly, 1.0)"

    fun newState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun authorizeUrl(state: String): String =
        Uri.Builder()
            .scheme("https")
            .authority("discord.com")
            .appendPath("oauth2")
            .appendPath("authorize")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("state", state)
            .build()
            .toString()

    fun isRedirect(uri: Uri): Boolean =
        uri.scheme == REDIRECT_SCHEME && uri.host == REDIRECT_HOST

    fun parseRedirect(uri: Uri): DiscordRedirect {
        val values = HashMap<String, String>()
        uri.encodedFragment?.let { parsePairs(it, values) }
        uri.encodedQuery?.let { parsePairs(it, values) }
        return DiscordRedirect(
            token = values["access_token"],
            state = values["state"],
            error = values["error"]
        )
    }

    suspend fun fetchProfile(token: String): DiscordProfile = withContext(Dispatchers.IO) {
        val connection = URL(API).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Discord answered ${connection.responseCode}")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val id = json.getString("id")
            val username = json.getString("username")
            val displayName = json.optString("global_name", "").ifBlank { username }
            val avatar = json.optString("avatar", "")

            DiscordProfile(
                id = id,
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl(id, avatar)
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun avatarUrl(id: String, hash: String): String {
        if (hash.isNotBlank() && hash != "null") {
            return "https://cdn.discordapp.com/avatars/$id/$hash.png?size=128"
        }
        val index = ((id.toLongOrNull() ?: 0L) shr 22) % 6
        return "https://cdn.discordapp.com/embed/avatars/$index.png"
    }

    private fun parsePairs(text: String, into: MutableMap<String, String>) {
        text.split('&').forEach { part ->
            val index = part.indexOf('=')
            if (index > 0) {
                into[Uri.decode(part.substring(0, index))] = Uri.decode(part.substring(index + 1))
            }
        }
    }
}
