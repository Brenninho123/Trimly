package com.brenninho.trimly.model

data class MusicTrack(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val volume: Int = 100,
    val loop: Boolean = true,
    val startOffsetMs: Long = 0L
)
