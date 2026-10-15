package com.brenninho.trimly.model

enum class AspectRatio(val width: Int, val height: Int) {
    ORIGINAL(0, 0),
    PORTRAIT_9_16(9, 16),
    SQUARE(1, 1),
    PORTRAIT_4_5(4, 5),
    LANDSCAPE_16_9(16, 9),
    LANDSCAPE_4_3(4, 3),
    CINEMA_21_9(21, 9);

    val ratio: Float?
        get() = if (this == ORIGINAL) null else width.toFloat() / height

    val label: String
        get() = if (this == ORIGINAL) "" else "$width:$height"

    fun crop(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        val target = ratio ?: return sourceWidth to sourceHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return sourceWidth to sourceHeight
        val source = sourceWidth.toFloat() / sourceHeight
        return if (source > target) {
            even((sourceHeight * target).toInt()) to even(sourceHeight)
        } else {
            even(sourceWidth) to even((sourceWidth / target).toInt())
        }
    }

    private fun even(value: Int): Int {
        val safe = value.coerceAtLeast(2)
        return safe - safe % 2
    }
}
