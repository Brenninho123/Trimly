package com.brenninho.trimly.engine

import com.brenninho.trimly.model.ExportOptions

data class ExportProgress(
    val fraction: Float,
    val etaMs: Long?
)

data class ExportOutcome(
    val outputPath: String,
    val sizeBytes: Long,
    val elapsedMs: Long
)

enum class ExportFailure {
    EMPTY_RANGE,
    SOURCE_UNREADABLE,
    NO_SPACE,
    STALLED,
    UNSUPPORTED,
    ENCODER,
    OTHER
}

class ExportFailedException(
    val failure: ExportFailure,
    message: String?
) : Exception(message)

interface ExportEngine {
    suspend fun exportTrim(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        options: ExportOptions,
        outputPath: String,
        onProgress: (ExportProgress) -> Unit
    ): ExportOutcome
}
