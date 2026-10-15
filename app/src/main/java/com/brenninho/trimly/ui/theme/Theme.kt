package com.brenninho.trimly.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.brenninho.trimly.settings.AccentColor
import com.brenninho.trimly.settings.ThemeMode

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.BLACK -> true
}

@Composable
fun TrimlyTheme(
    mode: ThemeMode = ThemeMode.DARK,
    accent: AccentColor = AccentColor.TRIMLY,
    content: @Composable () -> Unit
) {
    val dark = isDarkTheme(mode)
    val context = LocalContext.current
    val dynamic = accent == AccentColor.DYNAMIC && Build.VERSION.SDK_INT >= 31

    val base = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        dark -> darkScheme(Color(accent.seed))
        else -> lightScheme(Color(accent.seed))
    }
    val scheme = if (mode == ThemeMode.BLACK) base.toBlack() else base

    MaterialTheme(
        colorScheme = scheme.animated(),
        typography = TrimlyTypography,
        content = content
    )
}

private fun mix(color: Color, other: Color, amount: Float): Color =
    color.copy(alpha = amount).compositeOver(other)

private fun darkScheme(seed: Color): ColorScheme {
    val background = mix(seed, Color(0xFF0E1015), 0.04f)
    val surface = mix(seed, Color(0xFF161920), 0.05f)
    return darkColorScheme(
        primary = seed,
        onPrimary = mix(seed, Color.Black, 0.18f),
        primaryContainer = mix(seed, Color(0xFF14161C), 0.32f),
        onPrimaryContainer = mix(seed, Color.White, 0.25f),
        secondary = Color(0xFFFFB86B),
        onSecondary = Color(0xFF2B1700),
        secondaryContainer = mix(seed, Color(0xFF1F232C), 0.18f),
        onSecondaryContainer = Color(0xFFE6E8EE),
        tertiary = Color(0xFF7FE0C3),
        onTertiary = Color(0xFF00382B),
        background = background,
        onBackground = Color(0xFFE6E8EE),
        surface = surface,
        onSurface = Color(0xFFE6E8EE),
        surfaceVariant = mix(seed, Color(0xFF232833), 0.07f),
        onSurfaceVariant = Color(0xFFB4BAC8),
        surfaceTint = seed,
        outline = Color(0xFF5A6070),
        outlineVariant = Color(0xFF353A46),
        surfaceContainerLowest = mix(seed, Color(0xFF0B0D11), 0.03f),
        surfaceContainerLow = mix(seed, Color(0xFF13161C), 0.04f),
        surfaceContainer = surface,
        surfaceContainerHigh = mix(seed, Color(0xFF1E222A), 0.06f),
        surfaceContainerHighest = mix(seed, Color(0xFF262B35), 0.07f),
        error = Color(0xFFFF8A80),
        onError = Color(0xFF3B0A06)
    )
}

private fun lightScheme(seed: Color): ColorScheme {
    val strong = mix(seed, Color.Black, 0.72f)
    val background = mix(seed, Color(0xFFF8F9FC), 0.03f)
    val surface = mix(seed, Color.White, 0.02f)
    return lightColorScheme(
        primary = strong,
        onPrimary = Color.White,
        primaryContainer = mix(seed, Color.White, 0.24f),
        onPrimaryContainer = mix(seed, Color.Black, 0.45f),
        secondary = Color(0xFFB45F00),
        onSecondary = Color.White,
        secondaryContainer = mix(seed, Color(0xFFEFF1F6), 0.12f),
        onSecondaryContainer = Color(0xFF1B1F27),
        tertiary = Color(0xFF00876A),
        onTertiary = Color.White,
        background = background,
        onBackground = Color(0xFF15181E),
        surface = surface,
        onSurface = Color(0xFF15181E),
        surfaceVariant = mix(seed, Color(0xFFE9ECF2), 0.08f),
        onSurfaceVariant = Color(0xFF4F5563),
        surfaceTint = strong,
        outline = Color(0xFF8A90A0),
        outlineVariant = Color(0xFFD3D7E0),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = mix(seed, Color(0xFFF5F6FA), 0.03f),
        surfaceContainer = mix(seed, Color(0xFFF0F2F7), 0.04f),
        surfaceContainerHigh = mix(seed, Color(0xFFEAEDF3), 0.05f),
        surfaceContainerHighest = mix(seed, Color(0xFFE4E7EE), 0.06f),
        error = Color(0xFFB3261E),
        onError = Color.White
    )
}

private fun ColorScheme.toBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color(0xFF0A0A0C),
    surfaceVariant = Color(0xFF16171B),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF060607),
    surfaceContainer = Color(0xFF0A0A0C),
    surfaceContainerHigh = Color(0xFF121316),
    surfaceContainerHighest = Color(0xFF1A1B1F)
)

@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(350)
    val primary by animateColorAsState(primary, spec, label = "primary")
    val primaryContainer by animateColorAsState(primaryContainer, spec, label = "primaryContainer")
    val background by animateColorAsState(background, spec, label = "background")
    val surface by animateColorAsState(surface, spec, label = "surface")
    val surfaceVariant by animateColorAsState(surfaceVariant, spec, label = "surfaceVariant")
    val onBackground by animateColorAsState(onBackground, spec, label = "onBackground")
    val onSurface by animateColorAsState(onSurface, spec, label = "onSurface")
    val onSurfaceVariant by animateColorAsState(onSurfaceVariant, spec, label = "onSurfaceVariant")
    return copy(
        primary = primary,
        primaryContainer = primaryContainer,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onBackground = onBackground,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant
    )
}
