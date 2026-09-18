package com.brenninho.trimly.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.FilterGroup
import com.brenninho.trimly.model.VideoFilter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class PanelTab {
    FILTERS,
    EFFECTS,
    ADJUST;

    fun label(s: AppStrings): String = when (this) {
        FILTERS -> s.toolFilters
        EFFECTS -> s.toolEffects
        ADJUST -> s.toolAdjust
    }
}

@Composable
fun StylePanel(
    tab: PanelTab,
    options: ExportOptions,
    previewFrame: ImageBitmap?,
    enabled: Boolean,
    onTabChange: (PanelTab) -> Unit,
    onFilter: (VideoFilter) -> Unit,
    onIntensity: (Float) -> Unit,
    onAdjust: (Adjustment, Int) -> Unit,
    onResetAdjust: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                PanelTab.entries.forEach { item ->
                    Tab(
                        selected = item == tab,
                        onClick = { onTabChange(item) },
                        text = { Text(item.label(s)) }
                    )
                }
            }
            TextButton(onClick = onClose) { Text(s.done) }
        }

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(260)) { direction * it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(200)) { -direction * it / 4 } + fadeOut(tween(150)))
            },
            label = "panelTab"
        ) { current ->
            when (current) {
                PanelTab.FILTERS -> FilterList(
                    group = FilterGroup.FILTER,
                    options = options,
                    previewFrame = previewFrame,
                    enabled = enabled,
                    onFilter = onFilter,
                    onIntensity = onIntensity
                )

                PanelTab.EFFECTS -> FilterList(
                    group = FilterGroup.EFFECT,
                    options = options,
                    previewFrame = previewFrame,
                    enabled = enabled,
                    onFilter = onFilter,
                    onIntensity = onIntensity
                )

                PanelTab.ADJUST -> AdjustList(
                    options = options,
                    enabled = enabled,
                    onAdjust = onAdjust,
                    onReset = onResetAdjust
                )
            }
        }
    }
}

@Composable
private fun FilterList(
    group: FilterGroup,
    options: ExportOptions,
    previewFrame: ImageBitmap?,
    enabled: Boolean,
    onFilter: (VideoFilter) -> Unit,
    onIntensity: (Float) -> Unit
) {
    val s = LocalStrings.current
    val filters = remember(group) {
        listOf(VideoFilter.NONE) + VideoFilter.entries.filter { it.group == group && it != VideoFilter.NONE }
    }
    val adjustable = options.filter != VideoFilter.NONE && options.filter.adjustable

    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(filters, key = { _, item -> item.name }) { index, filter ->
                FilterCard(
                    index = index,
                    filter = filter,
                    selected = options.filter == filter,
                    enabled = enabled,
                    frame = previewFrame,
                    onClick = { onFilter(filter) }
                )
            }
        }

        AnimatedContent(
            targetState = adjustable,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "intensityRow"
        ) { isAdjustable ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp)
            ) {
                if (isAdjustable) {
                    Text(
                        text = s.intensity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(76.dp)
                    )
                    Slider(
                        value = options.filterIntensity,
                        onValueChange = onIntensity,
                        valueRange = 0f..1f,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(options.filterIntensity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp)
                    )
                } else {
                    Text(
                        text = if (options.filter == VideoFilter.NONE) s.pickOne else s.noIntensity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterCard(
    index: Int,
    filter: VideoFilter,
    selected: Boolean,
    enabled: Boolean,
    frame: ImageBitmap?,
    onClick: () -> Unit
) {
    val s = LocalStrings.current
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 35L)
        shown = true
    }

    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "filterAppear"
    )
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "filterSelected"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else Color.Transparent,
        animationSpec = tween(200),
        label = "filterBorder"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "filterLabel"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .graphicsLayer {
                val progress = appear.coerceIn(0f, 1f)
                alpha = progress
                translationY = (1f - progress) * 24f
            }
            .pressScale(source, 0.94f)
            .clip(shape)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = selectedScale
                    scaleY = selectedScale
                }
                .size(64.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, borderColor, shape)
        ) {
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = filter.grade.toColorFilter(),
                    modifier = Modifier.fillMaxSize()
                )
            }
            CheckBadge(
                visible = selected,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = s.filterName(filter),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CheckBadge(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun AdjustList(
    options: ExportOptions,
    enabled: Boolean,
    onAdjust: (Adjustment, Int) -> Unit,
    onReset: () -> Unit
) {
    val s = LocalStrings.current

    Column(
        modifier = Modifier
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Adjustment.entries.forEachIndexed { index, kind ->
            AdjustRow(
                index = index,
                label = s.adjustmentName(kind),
                value = options.adjustment(kind),
                enabled = enabled,
                onChange = { onAdjust(kind, it) }
            )
        }
        TextButton(
            onClick = onReset,
            enabled = enabled && options.hasAdjustments
        ) {
            Text(s.resetAdjustments)
        }
    }
}

@Composable
private fun AdjustRow(
    index: Int,
    label: String,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        shown = true
    }

    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(320),
        label = "adjustAppear"
    )
    val valueColor by animateColorAsState(
        targetValue = if (value != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "adjustValue"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer {
                alpha = appear
                translationX = (1f - appear) * 36f
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(92.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = -100f..100f,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}
