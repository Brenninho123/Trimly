package com.brenninho.trimly.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.settings.AccentColor
import com.brenninho.trimly.settings.ThemeMode

@Composable
internal fun AppearanceSection(
    theme: ThemeMode,
    accent: AccentColor,
    onTheme: (ThemeMode) -> Unit,
    onAccent: (AccentColor) -> Unit
) {
    val s = LocalStrings.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = s.themeTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeTile(
                        icon = themeIcon(mode),
                        label = s.themeName(mode),
                        selected = mode == theme,
                        onClick = { onTheme(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = s.accentTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                AccentColor.entries.forEach { item ->
                    val available = item != AccentColor.DYNAMIC || Build.VERSION.SDK_INT >= 31
                    AccentDot(
                        accent = item,
                        selected = item == accent,
                        enabled = available,
                        onClick = { onAccent(item) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (accent == AccentColor.DYNAMIC) s.accentDynamicHint else s.accentName(accent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "themeTile"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "themeTileContent"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun AccentDot(
    accent: AccentColor,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val ring by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = tween(200),
        label = "accentRing"
    )
    val brush = if (accent == AccentColor.DYNAMIC) {
        Brush.sweepGradient(
            listOf(Color(0xFF7C9CFF), Color(0xFF5FD4A8), Color(0xFFFFD166), Color(0xFFFF7AA8), Color(0xFF7C9CFF))
        )
    } else {
        Brush.linearGradient(listOf(Color(accent.seed), Color(accent.seed)))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        when {
            selected -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
            accent == AccentColor.DYNAMIC -> Icon(
                imageVector = Icons.Filled.Wallpaper,
                contentDescription = null,
                tint = Color.Black.copy(alpha = if (enabled) 0.6f else 0.25f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun themeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.SYSTEM -> Icons.Filled.Brightness4
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
    ThemeMode.BLACK -> Icons.Filled.Contrast
}
