package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.FilterGroup
import com.brenninho.trimly.model.VideoFilter
import kotlinx.coroutines.delay

enum class MenuCategory {
    EDIT,
    STYLE,
    AUDIO,
    OUTPUT;

    fun label(s: AppStrings): String = when (this) {
        EDIT -> s.catEdit
        STYLE -> s.catStyle
        AUDIO -> s.catAudio
        OUTPUT -> s.catOutput
    }
}

private class MenuEntry(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val active: Boolean = false,
    val soon: Boolean = false,
    val locked: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun EditorMenu(
    category: MenuCategory,
    expanded: Boolean,
    options: ExportOptions,
    hasEdits: Boolean,
    enabled: Boolean,
    onCategoryChange: (MenuCategory) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onMute: () -> Unit,
    onQuality: () -> Unit,
    onFilters: () -> Unit,
    onEffects: () -> Unit,
    onAdjust: () -> Unit,
    onReset: () -> Unit,
    onSoon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(250),
        label = "menuChevron"
    )
    val filterActive = options.filter != VideoFilter.NONE && options.filter.group == FilterGroup.FILTER
    val effectActive = options.filter != VideoFilter.NONE && options.filter.group == FilterGroup.EFFECT

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(
                selectedTabIndex = category.ordinal,
                containerColor = Color.Transparent,
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                MenuCategory.entries.forEach { item ->
                    Tab(
                        selected = item == category,
                        onClick = {
                            if (item == category) {
                                onExpandedChange(!expanded)
                            } else {
                                onCategoryChange(item)
                                onExpandedChange(true)
                            }
                        },
                        text = { Text(item.label(s)) }
                    )
                }
            }
            IconButton(onClick = { onExpandedChange(!expanded) }) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) s.collapseMenu else s.expandMenu,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = chevron }
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(260)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
        ) {
            AnimatedContent(
                targetState = category,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(260)) { direction * it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(200)) { -direction * it / 3 } + fadeOut(tween(150)))
                },
                label = "menuCategory"
            ) { current ->
                val entries = when (current) {
                    MenuCategory.EDIT -> listOf(
                        MenuEntry("trim", Icons.Filled.ContentCut, s.toolTrim, active = true, locked = true, onClick = {}),
                        MenuEntry(
                            "rotate",
                            Icons.Filled.RotateRight,
                            if (options.rotationDegrees % 360 != 0) s.rotateLabel(options.rotationDegrees) else s.toolRotate,
                            active = options.rotationDegrees % 360 != 0,
                            onClick = onRotate
                        ),
                        MenuEntry("flip", Icons.Filled.Flip, s.flip, active = options.flipHorizontal, onClick = onFlip),
                        MenuEntry("split", Icons.Filled.Layers, s.split, soon = true, onClick = { onSoon(s.split) }),
                        MenuEntry("speed", Icons.Filled.Speed, s.toolSpeed, soon = true, onClick = { onSoon(s.toolSpeed) })
                    )

                    MenuCategory.STYLE -> listOf(
                        MenuEntry(
                            "filters",
                            Icons.Filled.Palette,
                            if (filterActive) s.filterName(options.filter) else s.toolFilters,
                            active = filterActive,
                            onClick = onFilters
                        ),
                        MenuEntry(
                            "effects",
                            Icons.Filled.AutoAwesome,
                            if (effectActive) s.filterName(options.filter) else s.toolEffects,
                            active = effectActive,
                            onClick = onEffects
                        ),
                        MenuEntry("adjust", Icons.Filled.Tune, s.toolAdjust, active = options.hasAdjustments, onClick = onAdjust),
                        MenuEntry("text", Icons.Filled.TextFields, s.toolText, soon = true, onClick = { onSoon(s.toolText) })
                    )

                    MenuCategory.AUDIO -> listOf(
                        MenuEntry(
                            "mute",
                            if (options.muted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
                            if (options.muted) s.unmute else s.mute,
                            active = options.muted,
                            onClick = onMute
                        ),
                        MenuEntry("volume", Icons.Filled.MusicNote, s.volume, soon = true, onClick = { onSoon(s.volume) }),
                        MenuEntry("fade", Icons.Filled.Tune, s.fade, soon = true, onClick = { onSoon(s.fade) })
                    )

                    MenuCategory.OUTPUT -> listOf(
                        MenuEntry(
                            "quality",
                            Icons.Filled.HighQuality,
                            options.shortSide?.let { "${it}p" } ?: s.toolQuality,
                            active = options.shortSide != null,
                            onClick = onQuality
                        ),
                        MenuEntry(
                            "reset",
                            Icons.Filled.Refresh,
                            s.resetAll,
                            locked = !hasEdits,
                            onClick = onReset
                        )
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        MenuItem(entry = entry, index = index, enabled = enabled)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    entry: MenuEntry,
    index: Int,
    enabled: Boolean
) {
    val s = LocalStrings.current
    val source = remember { MutableInteractionSource() }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 45L)
        shown = true
    }

    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "menuItemAppear"
    )
    val activeScale by animateFloatAsState(
        targetValue = if (entry.active) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "menuItemActive"
    )
    val fill by animateColorAsState(
        targetValue = if (entry.active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(250),
        label = "menuItemFill"
    )
    val tint by animateColorAsState(
        targetValue = if (entry.active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(250),
        label = "menuItemTint"
    )
    val dimmed = (!enabled || entry.locked) && !entry.active
    val soonAlpha = if (entry.soon) 0.55f else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .graphicsLayer {
                val progress = appear.coerceIn(0f, 1f)
                alpha = progress * (if (dimmed) 0.4f else 1f) * soonAlpha
                scaleX = 0.7f + 0.3f * progress
                scaleY = 0.7f + 0.3f * progress
                translationY = (1f - progress) * 28f
            }
            .pressScale(source)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled && !entry.locked,
                onClick = entry.onClick
            )
            .padding(vertical = 8.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = activeScale
                        scaleY = activeScale
                    }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(fill)
            ) {
                Icon(imageVector = entry.icon, contentDescription = null, tint = tint)
            }
            ActiveDot(visible = entry.active && !entry.locked)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (entry.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (entry.soon) s.soon else " ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun ActiveDot(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
internal fun Modifier.pressScale(
    source: MutableInteractionSource,
    pressedScale: Float = 0.94f
): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
