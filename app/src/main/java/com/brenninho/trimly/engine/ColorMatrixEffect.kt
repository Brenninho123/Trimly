package com.brenninho.trimly.engine

import androidx.media3.effect.RgbMatrix

class ColorMatrixEffect(private val matrix: FloatArray) : RgbMatrix {

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix
}
