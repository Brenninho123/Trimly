package com.brenninho.trimly.engine

import androidx.media3.effect.RgbMatrix

class FadeMatrixEffect(
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
    private val totalUs: Long
) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        val level = level(presentationTimeUs)
        return floatArrayOf(
            level, 0f, 0f, 0f,
            0f, level, 0f, 0f,
            0f, 0f, level, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun level(timeUs: Long): Float {
        var factor = 1f
        if (fadeInUs > 0L && timeUs < fadeInUs) {
            factor = minOf(factor, timeUs.toFloat() / fadeInUs)
        }
        if (fadeOutUs > 0L && totalUs > 0L) {
            val remaining = totalUs - timeUs
            if (remaining < fadeOutUs) factor = minOf(factor, remaining.toFloat() / fadeOutUs)
        }
        return factor.coerceIn(0f, 1f)
    }
}
