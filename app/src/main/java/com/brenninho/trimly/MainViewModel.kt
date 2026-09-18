package com.brenninho.trimly

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brenninho.trimly.model.Clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MainState {
    data object Idle : MainState
    data object Loading : MainState
    data class Ready(val clip: Clip) : MainState
    data class Failed(val message: String) : MainState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<MainState>(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun open(uri: Uri) {
        _state.value = MainState.Loading
        viewModelScope.launch {
            val duration = withContext(Dispatchers.IO) { readDuration(uri) }
            _state.value = if (duration != null && duration > 0L) {
                MainState.Ready(Clip(uri, duration))
            } else {
                MainState.Failed("Could not read this video")
            }
        }
    }

    fun close() {
        _state.value = MainState.Idle
    }

    private fun readDuration(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<Application>(), uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}
