package com.brenninho.trimly.model

import android.net.Uri

data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = durationMs
) {
    val trimmedDurationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    fun withRange(newStart: Long, newEnd: Long): Clip {
        val start = newStart.coerceIn(0L, durationMs)
        val end = newEnd.coerceIn(start, durationMs)
        return copy(startMs = start, endMs = end)
    }
}
