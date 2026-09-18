package com.brenninho.trimly.editor

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.brenninho.trimly.model.ColorGrade

fun ColorGrade.toColorFilter(): ColorFilter? {
    if (isIdentity) return null
    return ColorFilter.colorMatrix(ColorMatrix(matrixValues()))
}

private fun ColorGrade.matrixValues(): FloatArray {
    var current = identityMatrix()

    if (saturation != 0f) {
        current = compose(current, saturationMatrix((1f + saturation).coerceAtLeast(0f)))
    }

    if (red != 1f || green != 1f || blue != 1f) {
        current = compose(
            current,
            floatArrayOf(
                red, 0f, 0f, 0f, 0f,
                0f, green, 0f, 0f, 0f,
                0f, 0f, blue, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    if (contrast != 0f) {
        val scale = 1f + contrast
        val shift = 0.5f * (1f - scale) * 255f
        current = compose(
            current,
            floatArrayOf(
                scale, 0f, 0f, 0f, shift,
                0f, scale, 0f, 0f, shift,
                0f, 0f, scale, 0f, shift,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    if (brightness != 0f) {
        val shift = brightness * 255f
        current = compose(
            current,
            floatArrayOf(
                1f, 0f, 0f, 0f, shift,
                0f, 1f, 0f, 0f, shift,
                0f, 0f, 1f, 0f, shift,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    if (invert) {
        current = compose(
            current,
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    return current
}

private fun identityMatrix(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f
)

private fun saturationMatrix(amount: Float): FloatArray {
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    val inverse = 1f - amount
    return floatArrayOf(
        inverse * lumR + amount, inverse * lumG, inverse * lumB, 0f, 0f,
        inverse * lumR, inverse * lumG + amount, inverse * lumB, 0f, 0f,
        inverse * lumR, inverse * lumG, inverse * lumB + amount, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

private fun compose(first: FloatArray, second: FloatArray): FloatArray {
    val result = FloatArray(20)
    for (row in 0 until 4) {
        for (column in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) {
                sum += second[row * 5 + k] * first[k * 5 + column]
            }
            result[row * 5 + column] = sum
        }
        var shift = second[row * 5 + 4]
        for (k in 0 until 4) {
            shift += second[row * 5 + k] * first[k * 5 + 4]
        }
        result[row * 5 + 4] = shift
    }
    return result
}
