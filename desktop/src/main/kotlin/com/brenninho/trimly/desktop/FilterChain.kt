package com.brenninho.trimly.desktop

import com.brenninho.trimly.model.ColorGrade
import com.brenninho.trimly.model.ExportOptions
import java.util.Locale

object FilterChain {

    fun build(options: ExportOptions): String {
        val parts = ArrayList<String>()

        when (((options.rotationDegrees % 360) + 360) % 360) {
            90 -> parts.add("transpose=2")
            180 -> parts.add("transpose=2,transpose=2")
            270 -> parts.add("transpose=1")
        }

        if (options.flipHorizontal) parts.add("hflip")

        val color = ArrayList<String>()
        color.addAll(grade(options.filterGrade))
        color.addAll(grade(options.adjustGrade))
        if (color.isNotEmpty()) {
            parts.add("format=rgb24")
            parts.addAll(color)
        }

        options.targetHeight?.let { parts.add("scale=-2:$it") }

        return parts.joinToString(",")
    }

    private fun grade(grade: ColorGrade): List<String> {
        if (grade.isIdentity) return emptyList()

        val result = ArrayList<String>()

        if (grade.saturation != 0f) {
            val amount = (1f + grade.saturation).coerceAtLeast(0f)
            val inverse = 1f - amount
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            result.add(
                "colorchannelmixer=" +
                    "rr=${number(inverse * lumR + amount)}:rg=${number(inverse * lumG)}:rb=${number(inverse * lumB)}:" +
                    "gr=${number(inverse * lumR)}:gg=${number(inverse * lumG + amount)}:gb=${number(inverse * lumB)}:" +
                    "br=${number(inverse * lumR)}:bg=${number(inverse * lumG)}:bb=${number(inverse * lumB + amount)}"
            )
        }

        val scale = floatArrayOf(grade.red, grade.green, grade.blue)
        val slope = FloatArray(3)
        val shift = FloatArray(3)

        for (channel in 0 until 3) {
            var a = scale[channel]
            var b = 0f

            if (grade.contrast != 0f) {
                val factor = 1f + grade.contrast
                a *= factor
                b = b * factor + 0.5f * (1f - factor) * 255f
            }

            if (grade.brightness != 0f) {
                b += grade.brightness * 255f
            }

            if (grade.invert) {
                a = -a
                b = 255f - b
            }

            slope[channel] = a
            shift[channel] = b
        }

        val changed = (0 until 3).any { slope[it] != 1f || shift[it] != 0f }
        if (changed) {
            result.add(
                "lutrgb=" +
                    "r='${expression(slope[0], shift[0])}':" +
                    "g='${expression(slope[1], shift[1])}':" +
                    "b='${expression(slope[2], shift[2])}'"
            )
        }

        return result
    }

    private fun expression(slope: Float, shift: Float): String =
        "clip(val*${number(slope)}+(${number(shift)}),minval,maxval)"

    private fun number(value: Float): String = String.format(Locale.ROOT, "%.4f", value)
}
