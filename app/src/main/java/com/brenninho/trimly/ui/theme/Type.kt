package com.brenninho.trimly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

private val base = Typography()

private fun TextStyle.tight(weight: FontWeight, spacing: Float) =
    copy(fontWeight = weight, letterSpacing = spacing.em)

val TrimlyTypography = Typography(
    displayLarge = base.displayLarge.tight(FontWeight.Bold, -0.02f),
    displayMedium = base.displayMedium.tight(FontWeight.Bold, -0.02f),
    displaySmall = base.displaySmall.tight(FontWeight.Bold, -0.015f),
    headlineLarge = base.headlineLarge.tight(FontWeight.Bold, -0.015f),
    headlineMedium = base.headlineMedium.tight(FontWeight.Bold, -0.01f),
    headlineSmall = base.headlineSmall.tight(FontWeight.SemiBold, -0.01f),
    titleLarge = base.titleLarge.tight(FontWeight.SemiBold, -0.005f),
    titleMedium = base.titleMedium.tight(FontWeight.SemiBold, 0f),
    titleSmall = base.titleSmall.tight(FontWeight.SemiBold, 0f),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.tight(FontWeight.SemiBold, 0.005f),
    labelMedium = base.labelMedium.tight(FontWeight.Medium, 0.01f),
    labelSmall = base.labelSmall.tight(FontWeight.Medium, 0.02f)
)
