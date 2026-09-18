package com.brenninho.trimly.model

data class ColorGrade(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val red: Float = 1f,
    val green: Float = 1f,
    val blue: Float = 1f,
    val invert: Boolean = false
) {
    val isIdentity: Boolean
        get() = brightness == 0f &&
            contrast == 0f &&
            saturation == 0f &&
            red == 1f &&
            green == 1f &&
            blue == 1f &&
            !invert

    fun scaled(amount: Float): ColorGrade = ColorGrade(
        brightness = brightness * amount,
        contrast = contrast * amount,
        saturation = saturation * amount,
        red = 1f + (red - 1f) * amount,
        green = 1f + (green - 1f) * amount,
        blue = 1f + (blue - 1f) * amount,
        invert = invert
    )
}

enum class FilterGroup {
    FILTER,
    EFFECT
}

enum class VideoFilter(
    val label: String,
    val group: FilterGroup,
    val grade: ColorGrade,
    val adjustable: Boolean = true
) {
    NONE("Original", FilterGroup.FILTER, ColorGrade()),
    WARM("Warm", FilterGroup.FILTER, ColorGrade(saturation = 0.08f, red = 1.14f, blue = 0.88f)),
    COOL("Cool", FilterGroup.FILTER, ColorGrade(red = 0.88f, blue = 1.16f)),
    VIVID("Vivid", FilterGroup.FILTER, ColorGrade(saturation = 0.45f, contrast = 0.12f)),
    FADED("Faded", FilterGroup.FILTER, ColorGrade(saturation = -0.3f, contrast = -0.12f, brightness = 0.06f)),
    VINTAGE(
        "Vintage",
        FilterGroup.FILTER,
        ColorGrade(saturation = -0.25f, contrast = -0.08f, red = 1.1f, green = 1.02f, blue = 0.82f)
    ),
    SEPIA("Sepia", FilterGroup.FILTER, ColorGrade(saturation = -1f, red = 1.25f, green = 1f, blue = 0.72f)),
    CINEMATIC(
        "Cinematic",
        FilterGroup.FILTER,
        ColorGrade(saturation = -0.1f, contrast = 0.25f, brightness = -0.03f, red = 1.06f, blue = 0.96f)
    ),
    BLACK_WHITE("B&W", FilterGroup.FILTER, ColorGrade(saturation = -1f)),
    NOIR("Noir", FilterGroup.FILTER, ColorGrade(saturation = -1f, contrast = 0.4f, brightness = -0.05f)),

    NEON("Neon", FilterGroup.EFFECT, ColorGrade(saturation = 0.9f, contrast = 0.3f, brightness = -0.04f)),
    DREAM(
        "Dream",
        FilterGroup.EFFECT,
        ColorGrade(saturation = 0.3f, contrast = -0.2f, brightness = 0.12f, red = 1.05f, blue = 1.08f)
    ),
    CHROME("Chrome", FilterGroup.EFFECT, ColorGrade(saturation = -0.35f, contrast = 0.4f)),
    SUNSET(
        "Sunset",
        FilterGroup.EFFECT,
        ColorGrade(saturation = 0.3f, contrast = 0.1f, red = 1.25f, green = 0.95f, blue = 0.75f)
    ),
    FROST(
        "Frost",
        FilterGroup.EFFECT,
        ColorGrade(saturation = -0.15f, brightness = 0.08f, red = 0.85f, blue = 1.25f)
    ),
    NIGHT_VISION(
        "Night",
        FilterGroup.EFFECT,
        ColorGrade(saturation = -0.5f, brightness = 0.05f, red = 0.35f, green = 1.35f, blue = 0.4f)
    ),
    INVERT("Invert", FilterGroup.EFFECT, ColorGrade(invert = true), adjustable = false)
}

enum class Adjustment(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    WARMTH("Warmth")
}
