package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.ExportQuality
import com.brenninho.trimly.model.FilterGroup
import com.brenninho.trimly.model.VideoFilter
import kotlinx.coroutines.delay

private const val GRID_COLUMNS = 4
private const val SELECTED_TAB_WEIGHT = 1.6f

enum class MenuCategory {
    EDIT,
    STYLE,
    AUDIO,
    OUTPUT;

    val icon: ImageVector
        get() = when (this) {
            EDIT -> Icons.Filled.ContentCut
            STYLE -> Icons.Filled.Palette
            AUDIO -> Icons.Filled.MusicNote
            OUTPUT -> Icons.Filled.HighQuality
        }

    fun label(s: AppStrings): String = when (this) {
        EDIT -> s.catEdit
        STYLE -> s.catStyle
        AUDIO -> s.catAudio
        OUTPUT -> s.catOutput
    }
}

private class ToolSpec(
    val id: String,
    val category: MenuCategory,
    val icon: ImageVector,
    val label: String,
    val status: String? = null,
    val badge: Int = 0,
    val active: Boolean = false,
    val soon: Boolean = false,
    val locked: Boolean = false,
    val rotation: Float = 0f,
    val mirrored: Boolean = false,
    val onClick: () -> Unit
)

private class MenuActions(
    val rotate: () -> Unit,
    val flip: () -> Unit,
    val mute: () -> Unit,
    val quality: () -> Unit,
    val filters: () -> Unit,
    val effects: () -> Unit,
    val adjust: () -> Unit,
    val merge: () -> Unit,
    val text: () -> Unit,
    val reset: () -> Unit,
    val soon: (String) -> Unit
)

@Composable
fun EditorMenu(
    category: MenuCategory,
    expanded: Boolean,
    options: ExportOptions,
    segmentCount: Int,
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
    onMerge: () -> Unit,
    onText: () -> Unit,
    onReset: () -> Unit,
    onSoon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var showAll by rememberSaveable { mutableStateOf(false) }

    val actions = MenuActions(
        rotate = onRotate,
        flip = onFlip,
        mute = onMute,
        quality = onQuality,
        filters = onFilters,
        effects = onEffects,
        adjust = onAdjust,
        merge = onMerge,
        text = onText,
        reset = onReset,
        soon = onSoon
    )
    val tools = buildTools(s, options, segmentCount, hasEdits, actions)
    val activeCategories = tools.filter { it.active && !it.locked }.map { it.category }.toSet()
    val editCount = countEdits(options, segmentCount)
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 0f else 180f,
        animationSpec = tween(250),
        label = "menuChevron"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
            ) {
                CategoryTabs(
                    selected = category,
                    activeCategories = activeCategories,
                    onSelect = { item ->
                        showAll = false
                        onCategoryChange(item)
                        onExpandedChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        showAll = !showAll
                        if (showAll) onExpandedChange(true)
                    }
                ) {
                    AnimatedContent(
                        targetState = showAll,
                        transitionSpec = { (scaleIn(tween(160)) + fadeIn(tween(160))) togetherWith (scaleOut(tween(120)) + fadeOut(tween(120))) },
                        label = "menuModeIcon"
                    ) { all ->
                        Icon(
                            imageVector = if (all) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                            contentDescription = if (all) s.menuShowLess else s.menuShowAll,
                            tint = if (all) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

            MenuBody(
                expanded = expanded,
                showAll = showAll,
                category = category,
                tools = tools,
                enabled = enabled,
                editCount = editCount,
                hasEdits = hasEdits,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun MenuBody(
    expanded: Boolean,
    showAll: Boolean,
    category: MenuCategory,
    tools: List<ToolSpec>,
    enabled: Boolean,
    editCount: Int,
    hasEdits: Boolean,
    onReset: () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(260)) + fadeIn(tween(200)),
        exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
    ) {
        AnimatedContent(
            targetState = showAll,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "menuMode"
        ) { all ->
            if (all) {
                ToolGrid(
                    tools = tools,
                    enabled = enabled,
                    editCount = editCount,
                    hasEdits = hasEdits,
                    onReset = onReset
                )
            } else {
                ToolRow(category = category, tools = tools, enabled = enabled)
            }
        }
    }
}

@Composable
private fun ToolRow(
    category: MenuCategory,
    tools: List<ToolSpec>,
    enabled: Boolean
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
        val items = tools.filter { it.category == current }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items, key = { _, spec -> spec.id }) { index, spec ->
                ToolCard(
                    spec = spec,
                    index = index,
                    enabled = enabled,
                    modifier = Modifier.width(84.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolGrid(
    tools: List<ToolSpec>,
    enabled: Boolean,
    editCount: Int,
    hasEdits: Boolean,
    onReset: () -> Unit
) {
    val s = LocalStrings.current

    Column(
        modifier = Modifier
            .heightIn(max = 340.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = s.menuAllTools,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (editCount > 0) {
                TextButton(onClick = onReset, enabled = enabled && hasEdits) {
                    Text("${s.menuEdits(editCount)} · ${s.resetAll}")
                }
            }
        }

        var index = 0
        MenuCategory.entries.forEach { category ->
            val group = tools.filter { it.category == category }
            if (group.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = category.label(s),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                group.chunked(GRID_COLUMNS).forEach { rowTools ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowTools.forEach { spec ->
                            ToolCard(
                                spec = spec,
                                index = index++,
                                enabled = enabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(GRID_COLUMNS - rowTools.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CategoryTabs(
    selected: MenuCategory,
    activeCategories: Set<MenuCategory>,
    onSelect: (MenuCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        val count = MenuCategory.entries.size
        val unit = maxWidth / (count - 1 + SELECTED_TAB_WEIGHT)
        val indicatorX by animateDpAsState(
            targetValue = unit * selected.ordinal,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "tabIndicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(unit * SELECTED_TAB_WEIGHT)
                .fillMaxHeight()
                .padding(3.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            MenuCategory.entries.forEach { item ->
                CategoryTab(
                    category = item,
                    selected = item == selected,
                    hasActive = item in activeCategories,
                    targetWidth = if (item == selected) unit * SELECTED_TAB_WEIGHT else unit,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun CategoryTab(
    category: MenuCategory,
    selected: Boolean,
    hasActive: Boolean,
    targetWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val s = LocalStrings.current
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabWidth"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tabTint"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.label(s),
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            TabLabel(visible = selected, text = category.label(s), tint = tint)
        }
        TabDot(
            visible = hasActive && !selected,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun TabLabel(
    visible: Boolean,
    text: String,
    tint: Color
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + expandHorizontally(tween(200)),
        exit = fadeOut(tween(100)) + shrinkHorizontally(tween(160))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun TabDot(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 10.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
        )
    }
}

@Composable
private fun ToolCard(
    spec: ToolSpec,
    index: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val source = remember { MutableInteractionSource() }
    var shown by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    LaunchedEffect(Unit) {
        delay(index * 40L)
        shown = true
    }

    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "toolAppear"
    )
    val activeScale by animateFloatAsState(
        targetValue = if (spec.active) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toolActive"
    )
    val fill by animateColorAsState(
        targetValue = if (spec.active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(250),
        label = "toolFill"
    )
    val outline by animateColorAsState(
        targetValue = if (spec.active) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else Color.Transparent,
        animationSpec = tween(250),
        label = "toolOutline"
    )
    val tint by animateColorAsState(
        targetValue = if (spec.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "toolTint"
    )
    val iconRotation by animateFloatAsState(
        targetValue = -spec.rotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "toolRotation"
    )
    val mirror by animateFloatAsState(
        targetValue = if (spec.mirrored) -1f else 1f,
        animationSpec = tween(260),
        label = "toolMirror"
    )
    val dimmed = (!enabled || spec.locked) && !spec.active
    val soonAlpha = if (spec.soon) 0.55f else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                val progress = appear.coerceIn(0f, 1f)
                alpha = progress * (if (dimmed) 0.4f else 1f) * soonAlpha
                scaleX = 0.7f + 0.3f * progress
                scaleY = 0.7f + 0.3f * progress
                translationY = (1f - progress) * 28f
            }
            .pressScale(source)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled && !spec.locked,
                onClick = spec.onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = activeScale
                        scaleY = activeScale
                    }
                    .size(52.dp)
                    .clip(shape)
                    .background(fill)
                    .border(1.5.dp, outline, shape)
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = iconRotation
                        scaleX = mirror
                    }
                )
            }
            CountBadge(count = spec.badge)
            ActiveDot(visible = spec.active && !spec.locked && spec.badge == 0)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (spec.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = spec.status ?: if (spec.soon) s.soon else " ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CountBadge(count: Int) {
    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 5.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
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

private fun buildTools(
    s: AppStrings,
    options: ExportOptions,
    segmentCount: Int,
    hasEdits: Boolean,
    actions: MenuActions
): List<ToolSpec> {
    val rotated = options.rotationDegrees % 360 != 0
    val filterActive = options.filter != VideoFilter.NONE && options.filter.group == FilterGroup.FILTER
    val effectActive = options.filter != VideoFilter.NONE && options.filter.group == FilterGroup.EFFECT
    val qualityText = qualityLabel(options, s)
    val qualityActive = options.shortSide != null || options.quality != ExportQuality.STANDARD

    return listOf(
        ToolSpec(
            id = "trim",
            category = MenuCategory.EDIT,
            icon = Icons.Filled.ContentCut,
            label = s.toolTrim,
            active = true,
            locked = true,
            onClick = {}
        ),
        ToolSpec(
            id = "rotate",
            category = MenuCategory.EDIT,
            icon = Icons.Filled.RotateRight,
            label = s.toolRotate,
            status = if (rotated) "${options.rotationDegrees}°" else null,
            active = rotated,
            rotation = options.rotationDegrees.toFloat(),
            onClick = actions.rotate
        ),
        ToolSpec(
            id = "flip",
            category = MenuCategory.EDIT,
            icon = Icons.Filled.Flip,
            label = s.flip,
            active = options.flipHorizontal,
            mirrored = options.flipHorizontal,
            onClick = actions.flip
        ),
        ToolSpec(
            id = "merge",
            category = MenuCategory.EDIT,
            icon = Icons.Filled.Layers,
            label = s.toolMerge,
            status = if (segmentCount > 1) s.mergeCount(segmentCount) else null,
            badge = if (segmentCount > 1) segmentCount else 0,
            active = segmentCount > 1,
            onClick = actions.merge
        ),
        ToolSpec(
            id = "speed",
            category = MenuCategory.EDIT,
            icon = Icons.Filled.Speed,
            label = s.toolSpeed,
            soon = true,
            onClick = { actions.soon(s.toolSpeed) }
        ),
        ToolSpec(
            id = "filters",
            category = MenuCategory.STYLE,
            icon = Icons.Filled.Palette,
            label = s.toolFilters,
            status = if (filterActive) s.filterName(options.filter) else null,
            active = filterActive,
            onClick = actions.filters
        ),
        ToolSpec(
            id = "effects",
            category = MenuCategory.STYLE,
            icon = Icons.Filled.AutoAwesome,
            label = s.toolEffects,
            status = if (effectActive) s.filterName(options.filter) else null,
            active = effectActive,
            onClick = actions.effects
        ),
        ToolSpec(
            id = "adjust",
            category = MenuCategory.STYLE,
            icon = Icons.Filled.Tune,
            label = s.toolAdjust,
            active = options.hasAdjustments,
            onClick = actions.adjust
        ),
        ToolSpec(
            id = "text",
            category = MenuCategory.STYLE,
            icon = Icons.Filled.TextFields,
            label = s.toolText,
            badge = options.texts.size,
            active = options.texts.isNotEmpty(),
            onClick = actions.text
        ),
        ToolSpec(
            id = "mute",
            category = MenuCategory.AUDIO,
            icon = if (options.muted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
            label = if (options.muted) s.unmute else s.mute,
            status = if (options.muted) s.muted else null,
            active = options.muted,
            onClick = actions.mute
        ),
        ToolSpec(
            id = "volume",
            category = MenuCategory.AUDIO,
            icon = Icons.Filled.MusicNote,
            label = s.volume,
            soon = true,
            onClick = { actions.soon(s.volume) }
        ),
        ToolSpec(
            id = "fade",
            category = MenuCategory.AUDIO,
            icon = Icons.Filled.Tune,
            label = s.fade,
            soon = true,
            onClick = { actions.soon(s.fade) }
        ),
        ToolSpec(
            id = "quality",
            category = MenuCategory.OUTPUT,
            icon = Icons.Filled.HighQuality,
            label = s.toolQuality,
            status = if (qualityActive) qualityText else null,
            active = qualityActive,
            onClick = actions.quality
        ),
        ToolSpec(
            id = "reset",
            category = MenuCategory.OUTPUT,
            icon = Icons.Filled.Refresh,
            label = s.resetAll,
            locked = !hasEdits,
            onClick = actions.reset
        )
    )
}

private fun countEdits(options: ExportOptions, segmentCount: Int): Int {
    var count = 0
    if (options.filter != VideoFilter.NONE) count += 1
    if (options.hasAdjustments) count += 1
    if (options.rotationDegrees % 360 != 0) count += 1
    if (options.flipHorizontal) count += 1
    if (options.muted) count += 1
    if (options.shortSide != null) count += 1
    if (options.quality != ExportQuality.STANDARD) count += 1
    count += options.texts.size
    if (segmentCount > 1) count += 1
    return count
}

private fun qualityLabel(options: ExportOptions, s: AppStrings): String {
    val side = options.shortSide?.let { "${it}p" }
    val level = when (options.quality) {
        ExportQuality.STANDARD -> null
        ExportQuality.HIGH -> s.qualityHigh
        ExportQuality.MAX -> s.qualityMax
    }
    return when {
        side != null && level != null -> "$side · $level"
        side != null -> side
        level != null -> level
        else -> s.toolQuality
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
