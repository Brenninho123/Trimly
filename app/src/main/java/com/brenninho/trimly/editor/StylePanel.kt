package com.brenninho.trimly.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.ExportOptions
import com.brenninho.trimly.model.FilterGroup
import com.brenninho.trimly.model.VideoFilter
import kotlin.math.roundToInt

enum class PanelTab(val label: String) {
    FILTERS("Filters"),
    EFFECTS("Effects"),
    ADJUST("Adjust")
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
                        text = { Text(item.label) }
                    )
                }
            }
            TextButton(onClick = onClose) { Text("Done") }
        }

        when (tab) {
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

@Composable
private fun FilterList(
    group: FilterGroup,
    options: ExportOptions,
    previewFrame: ImageBitmap?,
    enabled: Boolean,
    onFilter: (VideoFilter) -> Unit,
    onIntensity: (Float) -> Unit
) {
    val filters = remember(group) {
        listOf(VideoFilter.NONE) + VideoFilter.entries.filter { it.group == group && it != VideoFilter.NONE }
    }

    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filters, key = { it.name }) { filter ->
                FilterCard(
                    filter = filter,
                    selected = options.filter == filter,
                    enabled = enabled,
                    frame = previewFrame,
                    onClick = { onFilter(filter) }
                )
            }
        }

        val adjustable = options.filter != VideoFilter.NONE && options.filter.adjustable

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp)
        ) {
            if (adjustable) {
                Text(
                    text = "Intensity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp)
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
                    text = if (options.filter == VideoFilter.NONE) {
                        "Pick one to apply it"
                    } else {
                        "This effect has no intensity"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    filter: VideoFilter,
    selected: Boolean,
    enabled: Boolean,
    frame: ImageBitmap?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (selected) Modifier.border(2.dp, primary, shape) else Modifier)
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
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = filter.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdjustList(
    options: ExportOptions,
    enabled: Boolean,
    onAdjust: (Adjustment, Int) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Adjustment.entries.forEach { kind ->
            val value = options.adjustment(kind)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = kind.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(84.dp)
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onAdjust(kind, it.roundToInt()) },
                    valueRange = -100f..100f,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
        TextButton(
            onClick = onReset,
            enabled = enabled && options.hasAdjustments
        ) {
            Text("Reset adjustments")
        }
    }
}
