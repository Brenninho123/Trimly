package com.brenninho.trimly

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brenninho.trimly.auth.DiscordAuth
import com.brenninho.trimly.data.RecentStore
import com.brenninho.trimly.data.RecentVideo
import com.brenninho.trimly.i18n.AppLanguage
import com.brenninho.trimly.model.Clip
import com.brenninho.trimly.settings.AppSettings
import com.brenninho.trimly.settings.LoginError
import com.brenninho.trimly.settings.LoginStatus
import com.brenninho.trimly.settings.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OpenFailure {
    UNREADABLE
}

sealed interface MainState {
    data object Idle : MainState
    data object Loading : MainState
    data class Ready(val clip: Clip) : MainState
    data class Failed(val reason: OpenFailure) : MainState
}

private data class VideoInfo(val name: String, val durationMs: Long)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RecentStore(application)
    private val settings = AppSettings(application)

    private val _state = MutableStateFlow<MainState>(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val _recents = MutableStateFlow(store.load())
    val recents: StateFlow<List<RecentVideo>> = _recents.asStateFlow()

    private val _gridMode = MutableStateFlow(store.loadGrid())
    val gridMode: StateFlow<Boolean> = _gridMode.asStateFlow()

    private val _settingsState = MutableStateFlow(
        SettingsState(
            language = settings.language,
            tipsEnabled = settings.tipsEnabled,
            profile = settings.profile
        )
    )
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    fun open(uri: Uri) {
        _state.value = MainState.Loading
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { readInfo(uri) }
            if (info != null) {
                addRecent(uri, info)
                _state.value = MainState.Ready(Clip(uri, info.durationMs))
            } else {
                forget(uri.toString())
                _state.value = MainState.Failed(OpenFailure.UNREADABLE)
            }
        }
    }

    fun openRecent(item: RecentVideo) {
        open(Uri.parse(item.uri))
    }

    fun removeRecent(item: RecentVideo) {
        forget(item.uri)
    }

    fun clearRecents() {
        commit(emptyList())
    }

    fun toggleGrid() {
        val next = !_gridMode.value
        _gridMode.value = next
        store.saveGrid(next)
    }

    fun dismissError() {
        if (_state.value is MainState.Failed) _state.value = MainState.Idle
    }

    fun close() {
        _state.value = MainState.Idle
    }

    fun setLanguage(language: AppLanguage) {
        settings.language = language
        _settingsState.update { it.copy(language = language) }
    }

    fun setTipsEnabled(enabled: Boolean) {
        settings.tipsEnabled = enabled
        _settingsState.update { it.copy(tipsEnabled = enabled) }
    }

    fun beginDiscordLogin(): String {
        val state = DiscordAuth.newState()
        settings.pendingState = state
        _settingsState.update { it.copy(login = LoginStatus.Waiting) }
        return DiscordAuth.authorizeUrl(state)
    }

    fun reportLoginError(error: LoginError) {
        settings.pendingState = null
        _settingsState.update { it.copy(login = LoginStatus.Failed(error)) }
    }

    fun cancelLogin() {
        settings.pendingState = null
        _settingsState.update { it.copy(login = LoginStatus.Idle) }
    }

    fun handleAuthRedirect(uri: Uri) {
        val redirect = DiscordAuth.parseRedirect(uri)
        val expected = settings.pendingState
        settings.pendingState = null
        val token = redirect.token

        when {
            redirect.error != null -> {
                val error = if (redirect.error == "access_denied") LoginError.CANCELLED else LoginError.FAILED
                reportLoginError(error)
            }

            expected == null || redirect.state != expected -> reportLoginError(LoginError.SECURITY)

            token.isNullOrBlank() -> reportLoginError(LoginError.FAILED)

            else -> {
                _settingsState.update { it.copy(login = LoginStatus.Loading) }
                viewModelScope.launch {
                    try {
                        val profile = DiscordAuth.fetchProfile(token)
                        settings.profile = profile
                        _settingsState.update { it.copy(profile = profile, login = LoginStatus.Idle) }
                    } catch (e: Exception) {
                        reportLoginError(LoginError.NETWORK)
                    }
                }
            }
        }
    }

    fun logout() {
        settings.profile = null
        settings.pendingState = null
        _settingsState.update { it.copy(profile = null, login = LoginStatus.Idle) }
    }

    private fun addRecent(uri: Uri, info: VideoInfo) {
        val entry = RecentVideo(
            uri = uri.toString(),
            name = info.name,
            durationMs = info.durationMs,
            openedAt = System.currentTimeMillis()
        )
        val updated = (listOf(entry) + _recents.value.filterNot { it.uri == entry.uri })
            .take(MAX_RECENTS)
        commit(updated)
    }

    private fun forget(uri: String) {
        commit(_recents.value.filterNot { it.uri == uri })
    }

    private fun commit(list: List<RecentVideo>) {
        _recents.value = list
        store.save(list)
    }

    private fun readInfo(uri: Uri): VideoInfo? {
        keepAccess(uri)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<Application>(), uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            if (duration != null && duration > 0L) VideoInfo(queryName(uri), duration) else null
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun keepAccess(uri: Uri) {
        try {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
        }
    }

    private fun queryName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val name = try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return name ?: uri.lastPathSegment ?: "Video"
    }

    private companion object {
        const val MAX_RECENTS = 20
    }
}
