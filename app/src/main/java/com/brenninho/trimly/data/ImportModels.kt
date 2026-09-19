package com.brenninho.trimly.data

sealed interface ImportState {

    data object Idle : ImportState

    data class Downloading(
        val name: String?,
        val bytes: Long,
        val total: Long?
    ) : ImportState

    data class Failed(
        val reason: DownloadFailure,
        val detail: String?
    ) : ImportState
}

class ImportActions(
    val onImport: (String) -> Unit,
    val onCancel: () -> Unit,
    val onDismiss: () -> Unit
)
