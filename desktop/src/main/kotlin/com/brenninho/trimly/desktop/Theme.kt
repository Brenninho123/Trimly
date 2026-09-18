package com.brenninho.trimly.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TrimlyColors = darkColorScheme(
    primary = Color(0xFF7C9CFF),
    onPrimary = Color(0xFF0B1330),
    secondary = Color(0xFFFFB86B),
    background = Color(0xFF101318),
    onBackground = Color(0xFFE6E8EE),
    surface = Color(0xFF181C23),
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF232833),
    onSurfaceVariant = Color(0xFFB4BAC8)
)

@Composable
fun TrimlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrimlyColors,
        content = content
    )
}
