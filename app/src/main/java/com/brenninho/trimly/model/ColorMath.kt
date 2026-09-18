package com.brenninho.trimly.model

object ColorMath {

    fun rowMajor(grade: ColorGrade): FloatArray? {
        if (grade.isIdentity) return null

        var current = identity()

        if (grade.saturation != 0f) {
            current = compose(current, saturation((1f + grade.saturation).coerceAtLeast(0f)))
        }

        if (grade.red != 1f || grade.green != 1f || grade.blue != 1f) {
            current = compose(
                current,
                floatArrayOf(
                    grade.red, 0f, 0f, 0f, 0f,
                    0f, grade.green, 0f, 0f, 0f,
                    0f, 0f, grade.blue, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        if (grade.contrast != 0f) {
            val scale = 1f + grade.contrast
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

        if (grade.brightness != 0f) {
            val shift = grade.brightness * 255f
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

        if (grade.invert) {
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

    fun combined(options: ExportOptions): FloatArray? {
        val filter = rowMajor(options.filterGrade)
        val adjust = rowMajor(options.adjustGrade)
        return when {
            filter == null -> adjust
            adjust == null -> filter
            else -> compose(filter, adjust)
        }
    }

    fun toGl(values: FloatArray): FloatArray {
        val gl = FloatArray(16)
        for (row in 0 until 3) {
            for (column in 0 until 3) {
                gl[column * 4 + row] = values[row * 5 + column]
            }
            gl[12 + row] = values[row * 5 + 4] / 255f
        }
        gl[15] = 1f
        return gl
    }

    private fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private fun saturation(amount: Float): FloatArray {
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
}
