package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.SpeedPresets
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun SpeedPanel(
    options: ExportOptions,
    sourceMs: Long,
    enabled: Boolean,
    onSpeed: (Float) -> Unit,
    onKeepPitch: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val speed = options.speed

    Column(modifier = modifier.fillMaxWidth()) {
        PanelHeader(
            title = s.speedTitle,
            subtitle = s.speedLength(formatTime(options.outputMs(sourceMs))),
            onClose = onClose
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SpeedPresets.values) { preset ->
                ChoicePill(
                    label = SpeedPresets.label(preset),
                    selected = abs(preset - speed) < 0.001f,
                    enabled = enabled,
                    onClick = { onSpeed(preset) }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = SpeedPresets.label(SpeedPresets.MIN),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = speedToSlider(speed),
                onValueChange = { onSpeed(sliderToSpeed(it)) },
                valueRange = 0f..1f,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = SpeedPresets.label(speed),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (options.hasSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(52.dp)
            )
        }

        SwitchRow(
            title = s.speedKeepPitch,
            hint = s.speedKeepPitchHint,
            checked = options.keepPitch,
            enabled = enabled && !options.muted,
            onChange = onKeepPitch
        )

        Text(
            text = s.speedPreviewNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun AudioPanel(
    options: ExportOptions,
    outputMs: Long,
    enabled: Boolean,
    onVolume: (Int) -> Unit,
    onFadeIn: (Long) -> Unit,
    onFadeOut: (Long) -> Unit,
    onFadeVideo: (Boolean) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val limit = (outputMs / 2L).coerceAtLeast(0L)
    val steps = remember(limit) { SpeedPresets.fadeSteps.filter { it == 0L || it <= limit } }

    Column(modifier = modifier.fillMaxWidth()) {
        PanelHeader(
            title = s.audioTitle,
            subtitle = if (options.muted) s.volumeMutedHint else s.chipVolume(options.volume),
            onClose = onClose
        )

        Column(
            modifier = Modifier
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = s.volume,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = options.volume.toFloat(),
                    onValueChange = { value ->
                        val rounded = (value / 5f).roundToInt() * 5
                        onVolume(rounded)
                    },
                    valueRange = 0f..200f,
                    enabled = enabled && !options.muted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${options.volume}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (options.hasVolume) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(48.dp)
                )
            }

            AnimatedVisibility(visible = options.volume > 100 && !options.muted) {
                Text(
                    text = s.volumeBoostHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            FadeRow(
                title = s.fadeIn,
                value = options.fadeInMs,
                steps = steps,
                enabled = enabled,
                onChange = onFadeIn
            )

            FadeRow(
                title = s.fadeOut,
                value = options.fadeOutMs,
                steps = steps,
                enabled = enabled,
                onChange = onFadeOut
            )

            SwitchRow(
                title = s.fadeVideo,
                hint = s.fadeVideoHint,
                checked = options.fadeVideo,
                enabled = enabled && options.hasFade,
                onChange = onFadeVideo
            )

            TextButton(
                onClick = onReset,
                enabled = enabled && (options.hasVolume || options.hasFade || options.fadeVideo),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(s.resetAudio)
            }
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    val s = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onClose) { Text(s.done) }
    }
}

@Composable
private fun FadeRow(
    title: String,
    value: Long,
    steps: List<Long>,
    enabled: Boolean,
    onChange: (Long) -> Unit
) {
    val s = LocalStrings.current
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(steps) { step ->
                ChoicePill(
                    label = fadeLabel(s, step),
                    selected = step == value,
                    enabled = enabled,
                    onClick = { onChange(step) }
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(180),
        label = "pillContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "pillContent"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .pressScale(source, 0.92f)
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
    }
}

private fun fadeLabel(s: AppStrings, ms: Long): String {
    if (ms <= 0L) return s.fadeOff
    val seconds = ms / 1000f
    val text = if (ms % 1000L == 0L) {
        (ms / 1000L).toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", seconds)
    }
    return s.fadeSeconds(text)
}

private val SPEED_RANGE = ln(SpeedPresets.MAX / SpeedPresets.MIN)

private fun speedToSlider(speed: Float): Float =
    (ln(speed.coerceIn(SpeedPresets.MIN, SpeedPresets.MAX) / SpeedPresets.MIN) / SPEED_RANGE).coerceIn(0f, 1f)

private fun sliderToSpeed(position: Float): Float {
    val raw = SpeedPresets.MIN * (SpeedPresets.MAX / SpeedPresets.MIN).pow(position.coerceIn(0f, 1f))
    val snapped = SpeedPresets.values.firstOrNull { abs(it - raw) < 0.04f } ?: raw
    return (snapped * 20f).roundToInt() / 20f
}
