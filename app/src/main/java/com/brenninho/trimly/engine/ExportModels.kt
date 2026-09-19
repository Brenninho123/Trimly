package com.brenninho.trimly.engine

import java.io.File

data class ExportProgress(
    val fraction: Float,
    val elapsedMs: Long,
    val etaMs: Long?
)

data class ExportOutcome(
    val file: File,
    val sizeBytes: Long,
    val elapsedMs: Long,
    val attempts: Int,
    val fastTrim: Boolean
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
    message: String?,
    cause: Throwable? = null
) : Exception(message, cause)
