@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.brenninho.trimly.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninho.trimly.MainState
import com.brenninho.trimly.data.RecentVideo
import com.brenninho.trimly.data.ThumbnailLoader
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

private data class Tool(
    val label: String,
    val icon: ImageVector,
    val available: Boolean
)

private val tools = listOf(
    Tool("Trim", Icons.Filled.ContentCut, true),
    Tool("Merge", Icons.Filled.Layers, false),
    Tool("Speed", Icons.Filled.Speed, false),
    Tool("Filters", Icons.Filled.AutoFixHigh, false),
    Tool("Audio", Icons.Filled.GraphicEq, false)
)

private enum class SortMode(val label: String) {
    RECENT("Last opened"),
    NAME("Name"),
    LONGEST("Longest first"),
    SHORTEST("Shortest first")
}

private sealed interface Entry {
    val key: String

    data class Header(val label: String) : Entry {
        override val key: String get() = "header_$label"
    }

    data class Video(val item: RecentVideo) : Entry {
        override val key: String get() = item.uri
    }
}

private data class DayBounds(
    val today: Long,
    val yesterday: Long,
    val week: Long
)

private val bucketLabels = listOf("Today", "Yesterday", "Earlier this week", "Older")

@Composable
fun HomeScreen(
    state: MainState,
    recents: List<RecentVideo>,
    gridMode: Boolean,
    onPick: (Uri) -> Unit,
    onOpenRecent: (RecentVideo) -> Unit,
    onRemoveRecent: (RecentVideo) -> Unit,
    onClearRecents: () -> Unit,
    onToggleGrid: () -> Unit,
    onDismissError: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()

    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedUris by remember { mutableStateOf(emptySet<String>()) }
    var detailsFor by remember { mutableStateOf<RecentVideo?>(null) }

    val busy = state is MainState.Loading
    val sortMode = SortMode.entries[sortIndex.coerceIn(0, SortMode.entries.lastIndex)]

    val activeSelection = remember(selectedUris, recents) {
        val known = recents.map { it.uri }.toSet()
        selectedUris.filter { it in known }.toSet()
    }
    val selecting = activeSelection.isNotEmpty()
    val focusMode = searching || selecting

    val visible = remember(recents, query, sortMode) {
        val needle = query.trim()
        val filtered = if (needle.isEmpty()) {
            recents
        } else {
            recents.filter { it.name.contains(needle, ignoreCase = true) }
        }
        when (sortMode) {
            SortMode.RECENT -> filtered.sortedByDescending { it.openedAt }
            SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortMode.LONGEST -> filtered.sortedByDescending { it.durationMs }
            SortMode.SHORTEST -> filtered.sortedBy { it.durationMs }
        }
    }

    val grouped = sortMode == SortMode.RECENT && query.isBlank()
    val entries = remember(visible, grouped) {
        buildEntries(visible, grouped, System.currentTimeMillis())
    }
    val latest = remember(recents) { recents.maxByOrNull { it.openedAt } }
    val totalMs = remember(recents) { recents.sumOf { it.durationMs } }

    val fabScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val showFab = fabScrolled && !focusMode && !busy

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPick) }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPick) }

    val recorder = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let(onPick)
    }

    val pickFromLibrary: () -> Unit = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    val browseFiles: () -> Unit = {
        documentPicker.launch(arrayOf("video/*"))
    }

    val record: () -> Unit = {
        try {
            recorder.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar("No camera app found") }
        }
    }

    val closeSearch: () -> Unit = {
        searching = false
        query = ""
        focusManager.clearFocus()
    }

    val clearSelection: () -> Unit = {
        selectedUris = emptySet()
    }

    val toggle: (String) -> Unit = { uri ->
        selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri
    }

    LaunchedEffect(state) {
        if (state is MainState.Failed) {
            snackbar.showSnackbar(state.message)
            onDismissError()
        }
    }

    LaunchedEffect(searching) {
        if (searching) runCatching { focusRequester.requestFocus() }
    }

    BackHandler(enabled = searching) { closeSearch() }
    BackHandler(enabled = selecting) { clearSelection() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (selecting) {
                        Text("${activeSelection.size} selected")
                    } else if (searching) {
                        SearchField(
                            query = query,
                            onQueryChange = { query = it },
                            focusRequester = focusRequester,
                            onSubmit = { focusManager.clearFocus() }
                        )
                    } else {
                        Text("Trimly")
                    }
                },
                navigationIcon = {
                    if (selecting) {
                        IconButton(onClick = clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    } else if (searching) {
                        IconButton(onClick = closeSearch) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = { selectedUris = visible.map { it.uri }.toSet() }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = { showDeleteSelected = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove selected")
                        }
                    } else if (searching) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear text")
                            }
                        }
                    } else {
                        if (recents.isNotEmpty()) {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        }
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "About")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (selecting) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.background
                    },
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = pickFromLibrary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New video") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyVerticalGrid(
            state = listState,
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!focusMode) {
                item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                    HeroCard(
                        busy = busy,
                        onSelect = pickFromLibrary,
                        onBrowse = browseFiles,
                        onRecord = record
                    )
                }

                if (latest != null) {
                    item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
                        ContinueCard(
                            item = latest,
                            enabled = !busy,
                            onOpen = { onOpenRecent(latest) }
                        )
                    }
                }

                item(key = "tools", span = { GridItemSpan(maxLineSpan) }) {
                    ToolsRow(enabled = !busy, onTrim = pickFromLibrary)
                }
            }

            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                RecentHeader(
                    hasItems = recents.isNotEmpty(),
                    selecting = selecting,
                    gridMode = gridMode,
                    sortMode = sortMode,
                    summary = if (query.isNotBlank()) {
                        "${visible.size} ${if (visible.size == 1) "result" else "results"}"
                    } else {
                        summary(recents.size, totalMs)
                    },
                    onSort = { sortIndex = it.ordinal },
                    onToggleGrid = onToggleGrid,
                    onClear = { showClear = true }
                )
            }

            if (recents.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyRecents()
                }
            } else if (visible.isEmpty()) {
                item(key = "no_results", span = { GridItemSpan(maxLineSpan) }) {
                    NoResults(query = query)
                }
            } else {
                items(
                    items = entries,
                    key = { it.key },
                    span = { entry ->
                        GridItemSpan(if (entry is Entry.Header || !gridMode) maxLineSpan else 1)
                    }
                ) { entry ->
                    when (entry) {
                        is Entry.Header -> SectionLabel(
                            label = entry.label,
                            modifier = Modifier.animateItem()
                        )

                        is Entry.Video -> {
                            val item = entry.item
                            RecentItem(
                                item = item,
                                grid = gridMode,
                                selecting = selecting,
                                selected = item.uri in activeSelection,
                                enabled = !busy,
                                onClick = {
                                    if (selecting) toggle(item.uri) else onOpenRecent(item)
                                },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    toggle(item.uri)
                                },
                                onSelect = { toggle(item.uri) },
                                onDetails = { detailsFor = item },
                                onRemove = { onRemoveRecent(item) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Clear recent videos?") },
            text = { Text("This only clears the list. Your videos are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showClear = false
                    onClearRecents()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteSelected) {
        val count = activeSelection.size
        AlertDialog(
            onDismissRequest = { showDeleteSelected = false },
            title = { Text("Remove $count ${if (count == 1) "video" else "videos"}?") },
            text = { Text("They are removed from the recent list only. Your files are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteSelected = false
                    recents.filter { it.uri in activeSelection }.forEach(onRemoveRecent)
                    selectedUris = emptySet()
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelected = false }) { Text("Cancel") }
            }
        )
    }

    detailsFor?.let { item ->
        AlertDialog(
            onDismissRequest = { detailsFor = null },
            title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    DetailLine("Duration", clock(item.durationMs))
                    DetailLine(
                        "Last opened",
                        DateUtils.formatDateTime(
                            context,
                            item.openedAt,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsFor = null }) { Text("Close") }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Trimly") },
            text = {
                Column {
                    Text("Version ${appVersion(context)}")
                    Spacer(Modifier.height(8.dp))
                    Text("A free video editor that runs entirely on your device. No account, no upload, no watermark.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Search videos") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}

@Composable
private fun HeroCard(
    busy: Boolean,
    onSelect: () -> Unit,
    onBrowse: () -> Unit,
    onRecord: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Start editing",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pick a video to trim, or record a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(24.dp))

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Reading video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Select video")
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Files")
                    }
                    OutlinedButton(onClick = onRecord, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Record")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(
    item: RecentVideo,
    enabled: Boolean,
    onOpen: () -> Unit
) {
    Card(
        onClick = onOpen,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                VideoThumbnail(uri = item.uri, modifier = Modifier.fillMaxSize())
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue editing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${clock(item.durationMs)} · ${relativeTime(item.openedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToolsRow(
    enabled: Boolean,
    onTrim: () -> Unit
) {
    Column {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            tools.forEach { tool ->
                AssistChip(
                    onClick = onTrim,
                    enabled = enabled && tool.available,
                    label = { Text(if (tool.available) tool.label else "${tool.label} (soon)") },
                    leadingIcon = {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RecentHeader(
    hasItems: Boolean,
    selecting: Boolean,
    gridMode: Boolean,
    sortMode: SortMode,
    summary: String,
    onSort: (SortMode) -> Unit,
    onToggleGrid: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (hasItems) {
                SortMenu(current = sortMode, onSelect = onSort)
                IconButton(onClick = onToggleGrid) {
                    Icon(
                        imageVector = if (gridMode) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                        contentDescription = if (gridMode) "List view" else "Grid view",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!selecting) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear recent",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (hasItems) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SortMenu(
    current: SortMode,
    onSelect: (SortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    leadingIcon = {
                        if (mode == current) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        } else {
                            Spacer(Modifier.size(24.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    label: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

@Composable
private fun EmptyRecents() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No recent videos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No videos match \"${query.trim()}\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ItemSurface(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClickLabel = "Select",
                onLongClick = onLongClick
            )
    ) {
        content()
    }
}

@Composable
private fun RecentItem(
    item: RecentVideo,
    grid: Boolean,
    selecting: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    ItemSurface(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    ) {
        if (grid) {
            Column(modifier = Modifier.padding(8.dp)) {
                Thumb(
                    item = item,
                    selecting = selecting,
                    selected = selected,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = relativeTime(item.openedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (selecting) {
                        Spacer(Modifier.size(48.dp))
                    } else {
                        RecentMenu(onSelect = onSelect, onDetails = onDetails, onRemove = onRemove)
                    }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Thumb(
                    item = item,
                    selecting = selecting,
                    selected = selected,
                    modifier = Modifier.width(120.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = relativeTime(item.openedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selecting) {
                    Spacer(Modifier.size(48.dp))
                } else {
                    RecentMenu(onSelect = onSelect, onDetails = onDetails, onRemove = onRemove)
                }
            }
        }
    }
}

@Composable
private fun Thumb(
    item: RecentVideo,
    selecting: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        VideoThumbnail(uri = item.uri, modifier = Modifier.fillMaxSize())
        DurationBadge(
            durationMs = item.durationMs,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
        )
        if (selecting) {
            SelectionMark(
                selected = selected,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun SelectionMark(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
        contentDescription = if (selected) "Selected" else "Not selected",
        tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        modifier = modifier
            .padding(6.dp)
            .size(22.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
    )
}

@Composable
private fun RecentMenu(
    onSelect: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Select") },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSelect()
                }
            )
            DropdownMenuItem(
                text = { Text("Details") },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDetails()
                }
            )
            DropdownMenuItem(
                text = { Text("Remove from recent") },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DurationBadge(
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Text(
        text = clock(durationMs),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun VideoThumbnail(
    uri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = ThumbnailLoader.load(context, uri)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private fun buildEntries(
    items: List<RecentVideo>,
    grouped: Boolean,
    now: Long
): List<Entry> {
    if (!grouped) return items.map { Entry.Video(it) }
    val bounds = dayBounds(now)
    val result = ArrayList<Entry>(items.size + bucketLabels.size)
    var current = -1
    items.forEach { item ->
        val bucket = when {
            item.openedAt >= bounds.today -> 0
            item.openedAt >= bounds.yesterday -> 1
            item.openedAt >= bounds.week -> 2
            else -> 3
        }
        if (bucket != current) {
            current = bucket
            result.add(Entry.Header(bucketLabels[bucket]))
        }
        result.add(Entry.Video(item))
    }
    return result
}

private fun dayBounds(now: Long): DayBounds {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val today = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    val week = calendar.timeInMillis
    return DayBounds(today = today, yesterday = yesterday, week = week)
}

private fun summary(count: Int, totalMs: Long): String {
    val totalMinutes = totalMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val length = when {
        hours > 0 -> "%d h %02d min".format(Locale.ROOT, hours, minutes)
        totalMinutes > 0 -> "%d min".format(Locale.ROOT, totalMinutes)
        else -> "%d s".format(Locale.ROOT, totalMs / 1000)
    }
    val noun = if (count == 1) "video" else "videos"
    return "$count $noun · $length"
}

private fun clock(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, seconds)
    } else {
        "%02d:%02d".format(Locale.ROOT, minutes, seconds)
    }
}

private fun relativeTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

private fun appVersion(context: Context): String =
    try {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName.orEmpty()
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }
