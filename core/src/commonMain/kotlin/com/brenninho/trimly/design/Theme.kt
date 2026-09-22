package com.brenninho.trimly.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TrimlyColors = darkColorScheme(
    primary = Color(0xFF7C9CFF),
    onPrimary = Color(0xFF0B1330),
    primaryContainer = Color(0xFF2C3B70),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFFFFB86B),
    onSecondary = Color(0xFF3A2200),
    tertiary = Color(0xFF6BE0C8),
    onTertiary = Color(0xFF00382E),
    background = Color(0xFF101318),
    onBackground = Color(0xFFE6E8EE),
    surface = Color(0xFF181C23),
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF232833),
    onSurfaceVariant = Color(0xFFB4BAC8),
    surfaceTint = Color(0xFF7C9CFF),
    outline = Color(0xFF3B4250),
    outlineVariant = Color(0xFF2A2F3A),
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF3B0A0A)
)

val TrimlyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val TrimlyTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun TrimlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrimlyColors,
        shapes = TrimlyShapes,
        typography = TrimlyTypography,
        content = content
    )
}
