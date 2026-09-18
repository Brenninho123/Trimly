package com.brenninho.trimly.model

data class ExportOptions(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val muted: Boolean = false,
    val shortSide: Int? = null,
    val targetHeight: Int? = null
) {
    val hasVideoTransform: Boolean
        get() = rotationDegrees % 360 != 0 || flipHorizontal

    fun rotated(): ExportOptions = copy(rotationDegrees = (rotationDegrees + 90) % 360)
}
