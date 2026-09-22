package com.brenninho.trimly.model

data class TextItem(
    val id: Long,
    val text: String,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val sizeFraction: Float = 0.06f,
    val x: Float = 0.5f,
    val y: Float = 0.85f,
    val bold: Boolean = true,
    val background: Boolean = false,
    val startMs: Long = 0L,
    val endMs: Long? = null
) {
    fun visibleAt(timelineMs: Long): Boolean =
        timelineMs >= startMs && (endMs == null || timelineMs <= endMs)
}
