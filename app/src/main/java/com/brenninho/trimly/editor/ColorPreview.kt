package com.brenninho.trimly.editor

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.brenninho.trimly.model.ColorGrade
import com.brenninho.trimly.model.ColorMath

fun ColorGrade.toColorFilter(): ColorFilter? {
    val values = ColorMath.rowMajor(this) ?: return null
    return ColorFilter.colorMatrix(ColorMatrix(values))
}
