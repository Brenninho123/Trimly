package com.brenninho.trimly.model

object SpeedPresets {

    val values = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f)

    const val MIN = 0.25f
    const val MAX = 4f

    val fadeSteps = listOf(0L, 500L, 1000L, 2000L, 3000L, 5000L)

    fun label(speed: Float): String {
        val rounded = Math.round(speed * 100f) / 100f
        val text = if (rounded == rounded.toInt().toFloat()) {
            rounded.toInt().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
        return "${text}x"
    }
}
