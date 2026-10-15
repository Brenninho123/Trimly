package com.brenninho.trimly.model

enum class ExportQuality(val bitrateFactor: Double, val sourceCap: Double) {
    STANDARD(1.0, 1.2),
    HIGH(1.6, 1.6),
    MAX(2.4, 2.4)
}

data class ExportOptions(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val muted: Boolean = false,
    val shortSide: Int? = null,
    val targetHeight: Int? = null,
    val filter: VideoFilter = VideoFilter.NONE,
    val filterIntensity: Float = 1f,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val warmth: Int = 0,
    val quality: ExportQuality = ExportQuality.STANDARD,
    val texts: List<TextItem> = emptyList(),
    val speed: Float = 1f,
    val keepPitch: Boolean = true,
    val volume: Int = 100,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val fadeVideo: Boolean = false
) {
    val hasSpeed: Boolean
        get() = speed != 1f

    val hasVolume: Boolean
        get() = volume != 100

    val hasFade: Boolean
        get() = fadeInMs > 0L || fadeOutMs > 0L

    val hasAudioProcessing: Boolean
        get() = !muted && (hasSpeed || hasVolume || hasFade)

    val gain: Float
        get() = if (muted) 0f else volume / 100f

    fun outputMs(sourceMs: Long): Long = (sourceMs / speed.toDouble()).toLong()

    fun envelopeAt(timeMs: Long, totalMs: Long): Float {
        var factor = 1f
        if (fadeInMs > 0L && timeMs < fadeInMs) {
            factor = minOf(factor, (timeMs.toFloat() / fadeInMs).coerceIn(0f, 1f))
        }
        val remaining = totalMs - timeMs
        if (fadeOutMs > 0L && remaining < fadeOutMs) {
            factor = minOf(factor, (remaining.toFloat() / fadeOutMs).coerceIn(0f, 1f))
        }
        return factor
    }

    fun clearAudio(): ExportOptions = copy(volume = 100, fadeInMs = 0L, fadeOutMs = 0L, fadeVideo = false)

    val hasVideoTransform: Boolean
        get() = rotationDegrees % 360 != 0 || flipHorizontal

    val hasAdjustments: Boolean
        get() = brightness != 0 || contrast != 0 || saturation != 0 || warmth != 0

    val filterGrade: ColorGrade
        get() = when {
            filter == VideoFilter.NONE -> ColorGrade()
            !filter.adjustable -> filter.grade
            else -> filter.grade.scaled(filterIntensity)
        }

    val adjustGrade: ColorGrade
        get() {
            val warm = warmth / 100f
            return ColorGrade(
                brightness = brightness / 100f * 0.4f,
                contrast = contrast / 100f * 0.5f,
                saturation = saturation / 100f,
                red = 1f + 0.2f * warm,
                blue = 1f - 0.2f * warm
            )
        }

    fun rotated(): ExportOptions = copy(rotationDegrees = (rotationDegrees + 90) % 360)

    fun adjustment(kind: Adjustment): Int = when (kind) {
        Adjustment.BRIGHTNESS -> brightness
        Adjustment.CONTRAST -> contrast
        Adjustment.SATURATION -> saturation
        Adjustment.WARMTH -> warmth
    }

    fun withAdjustment(kind: Adjustment, value: Int): ExportOptions = when (kind) {
        Adjustment.BRIGHTNESS -> copy(brightness = value)
        Adjustment.CONTRAST -> copy(contrast = value)
        Adjustment.SATURATION -> copy(saturation = value)
        Adjustment.WARMTH -> copy(warmth = value)
    }

    fun clearAdjustments(): ExportOptions = copy(brightness = 0, contrast = 0, saturation = 0, warmth = 0)
}
