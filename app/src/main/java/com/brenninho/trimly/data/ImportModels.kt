package com.brenninho.trimly.data

enum class ImportStage {
    CONNECTING,
    RESOLVING,
    DOWNLOADING,
    RETRYING,
    CONVERTING
}

sealed interface ImportState {

    data object Idle : ImportState

    data class Downloading(
        val url: String,
        val name: String?,
        val bytes: Long,
        val total: Long?,
        val stage: ImportStage = ImportStage.CONNECTING,
        val fraction: Float? = null,
        val bytesPerSecond: Long? = null,
        val kind: LinkKind? = null
    ) : ImportState {
        val etaMs: Long?
            get() {
                val speed = bytesPerSecond ?: return null
                val size = total ?: return null
                if (speed <= 0L || size <= bytes) return null
                return (size - bytes) * 1000L / speed
            }
    }

    data class Failed(
        val url: String,
        val reason: DownloadFailure,
        val detail: String?
    ) : ImportState
}

class ImportActions(
    val onImport: (String) -> Unit,
    val onCancel: () -> Unit,
    val onDismiss: () -> Unit
)
