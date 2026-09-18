package com.brenninho.trimly.model

import android.net.Uri

data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = durationMs
) {

    init {
        require(durationMs >= 0L) { "durationMs must not be negative" }
        require(startMs in 0L..endMs) { "startMs must be within 0..endMs" }
        require(endMs <= durationMs) { "endMs must not exceed durationMs" }
    }

    val trimmedDurationMs: Long
        get() = endMs - startMs

    val isTrimmed: Boolean
        get() = startMs > 0L || endMs < durationMs

    val startFraction: Float
        get() = if (durationMs == 0L) 0f else startMs.toFloat() / durationMs

    val endFraction: Float
        get() = if (durationMs == 0L) 1f else endMs.toFloat() / durationMs

    fun withRange(newStart: Long, newEnd: Long): Clip {
        val start = newStart.coerceIn(0L, durationMs)
        val end = newEnd.coerceIn(start, durationMs)
        return copy(startMs = start, endMs = end)
    }

    fun withStart(newStart: Long, minLengthMs: Long = 0L): Clip {
        val minimum = minLengthMs.coerceAtLeast(0L)
        val limit = (endMs - minimum).coerceIn(0L, endMs)
        return copy(startMs = newStart.coerceIn(0L, limit))
    }

    fun withEnd(newEnd: Long, minLengthMs: Long = 0L): Clip {
        val minimum = minLengthMs.coerceAtLeast(0L)
        val floor = (startMs + minimum).coerceAtMost(durationMs)
        return copy(endMs = newEnd.coerceIn(floor, durationMs))
    }

    fun shifted(deltaMs: Long): Clip {
        val length = trimmedDurationMs
        val start = (startMs + deltaMs).coerceIn(0L, durationMs - length)
        return copy(startMs = start, endMs = start + length)
    }

    fun reset(): Clip = copy(startMs = 0L, endMs = durationMs)

    operator fun contains(positionMs: Long): Boolean = positionMs in startMs..endMs

    fun coercePosition(positionMs: Long): Long = positionMs.coerceIn(startMs, endMs)

    fun relativePosition(positionMs: Long): Long =
        (positionMs - startMs).coerceIn(0L, trimmedDurationMs)

    fun sourcePosition(relativeMs: Long): Long =
        (startMs + relativeMs).coerceIn(startMs, endMs)
}
