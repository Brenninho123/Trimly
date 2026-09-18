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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Amber = Color(0xFFFFC857)

private val LocalShimmer = compositionLocalOf<State<Float>?> { null }

private data class Tool(
    val label: String,
    val icon: ImageVector,
    val available: Boolean
)

private val tools = listOf(
    Tool("Trim", Icons.Filled.ContentCut, true),
    Tool("Filters", Icons.Filled.Palette, true),
    Tool("Effects", Icons.Filled.AutoAwesome, true),
    Tool("Adjust", Icons.Filled.Tune, true),
    Tool("Rotate", Icons.Filled.RotateRight, true),
    Tool("Quality", Icons.Filled.HighQuality, true),
    Tool("Merge", Icons.Filled.Layers, false),
    Tool("Speed", Icons.Filled.Speed, false),
    Tool("Text", Icons.Filled.TextFields, false)
)

private val tips = listOf(
    "Long-press a video to select several at once",
    "Add videos to Favorites to keep them on top",
    "Use Adjust in the editor to tune brightness and warmth",
    "Export at 720p to get smaller files",
    "Drag the orange handles to set the exact cut"
)

private val bucketLabels = listOf("Today", "Yesterday", "Earlier this week", "Older")

private enum class SortMode(val label: String) {
    RECENT("Last opened"),
    OLDEST("Oldest opened"),
    NAME("Name A-Z"),
    NAME_DESC("Name Z-A"),
    LONGEST("Longest first"),
    SHORTEST("Shortest first")
}

private enum class LayoutMode(val label: String, val icon: ImageVector) {
    LIST("List", Icons.Filled.ViewAgenda),
    GRID("Grid", Icons.Filled.GridView),
    COMPACT("Compact", Icons.Filled.Menu)
}

private enum class DurationFilter(val label: String) {
    ALL("All"),
    SHORT("Under 1 min"),
    MEDIUM("1-5 min"),
    LONG("Over 5 min");

    fun matches(durationMs: Long): Boolean = when (this) {
        ALL -> true
        SHORT -> durationMs < 60_000L
        MEDIUM -> durationMs in 60_000L..300_000L
        LONG -> durationMs > 300_000L
    }
}

private enum class BarMode {
    NORMAL,
    SEARCH,
    SELECT
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

private class HomePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("trimly_home_ui", Context.MODE_PRIVATE)

    var sortIndex: Int
        get() = prefs.getInt("sort", 0)
        set(value) {
            prefs.edit().putInt("sort", value).apply()
        }

    var filterIndex: Int
        get() = prefs.getInt("filter", 0)
        set(value) {
            prefs.edit().putInt("filter", value).apply()
        }

    var compact: Boolean
        get() = prefs.getBoolean("compact", false)
        set(value) {
            prefs.edit().putBoolean("compact", value).apply()
        }

    var tipHidden: Boolean
        get() = prefs.getBoolean("tip_hidden", false)
        set(value) {
            prefs.edit().putBoolean("tip_hidden", value).apply()
        }

    var favorites: Set<String>
        get() = prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet("favorites", value).apply()
        }
}

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
    val prefs = remember { HomePrefs(context) }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortIndex by remember { mutableIntStateOf(prefs.sortIndex) }
    var filterIndex by remember { mutableIntStateOf(prefs.filterIndex) }
    var compact by remember { mutableStateOf(prefs.compact) }
    var tipHidden by remember { mutableStateOf(prefs.tipHidden) }
    var favorites by remember { mutableStateOf(prefs.favorites) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedUris by remember { mutableStateOf(emptySet<String>()) }
    var hidden by remember { mutableStateOf(emptySet<String>()) }
    var detailsFor by remember { mutableStateOf<RecentVideo?>(null) }
    var entered by remember { mutableStateOf(false) }

    val busy = state is MainState.Loading
    val sortMode = SortMode.entries[sortIndex.coerceIn(0, SortMode.entries.lastIndex)]
    val durationFilter = DurationFilter.entries[filterIndex.coerceIn(0, DurationFilter.entries.lastIndex)]
    val layoutMode = when {
        compact -> LayoutMode.COMPACT
        gridMode -> LayoutMode.GRID
        else -> LayoutMode.LIST
    }
    val greeting = remember { greetingForNow() }

    val available = remember(recents, hidden) { recents.filter { it.uri !in hidden } }

    val activeSelection = remember(selectedUris, available) {
        val known = available.map { it.uri }.toSet()
        selectedUris.filter { it in known }.toSet()
    }
    val selecting = activeSelection.isNotEmpty()
    val focusMode = searching || selecting
    val barMode = when {
        selecting -> BarMode.SELECT
        searching -> BarMode.SEARCH
        else -> BarMode.NORMAL
    }

    val visible = remember(available, query, sortMode, durationFilter, favoritesOnly, favorites) {
        val needle = query.trim()
        val filtered = available.filter { item ->
            (needle.isEmpty() || item.name.contains(needle, ignoreCase = true)) &&
                durationFilter.matches(item.durationMs) &&
                (!favoritesOnly || item.uri in favorites)
        }
        when (sortMode) {
            SortMode.RECENT -> filtered.sortedByDescending { it.openedAt }
            SortMode.OLDEST -> filtered.sortedBy { it.openedAt }
            SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortMode.LONGEST -> filtered.sortedByDescending { it.durationMs }
            SortMode.SHORTEST -> filtered.sortedBy { it.durationMs }
        }
    }

    val filtersActive = query.isNotBlank() || durationFilter != DurationFilter.ALL || favoritesOnly
    val grouped = sortMode == SortMode.RECENT && !filtersActive

    val entries = remember(visible, favorites, grouped) {
        val (favoriteItems, others) = visible.partition { it.uri in favorites }
        buildEntries(favoriteItems, others, grouped, System.currentTimeMillis())
    }

    val latest = remember(available) { available.maxByOrNull { it.openedAt } }
    val totalMs = remember(available) { available.sumOf { it.durationMs } }
    val favoriteCount = remember(available, favorites) { available.count { it.uri in favorites } }

    val scrolledPast by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }
    val fabExpanded by remember { derivedStateOf { !listState.isScrollInProgress } }
    val showFab = scrolledPast && !focusMode && !busy

    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmer = shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerValue"
    )

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

    val toggleSelected: (String) -> Unit = { uri ->
        selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri
    }

    val updateFavorites: (Set<String>) -> Unit = { updated ->
        favorites = updated
        prefs.favorites = updated
    }

    val toggleFavorite: (String) -> Unit = { uri ->
        updateFavorites(if (uri in favorites) favorites - uri else favorites + uri)
    }

    val setSort: (SortMode) -> Unit = { mode ->
        sortIndex = mode.ordinal
        prefs.sortIndex = mode.ordinal
    }

    val setDurationFilter: (DurationFilter) -> Unit = { filter ->
        filterIndex = filter.ordinal
        prefs.filterIndex = filter.ordinal
    }

    val setLayout: (LayoutMode) -> Unit = { mode ->
        when (mode) {
            LayoutMode.LIST -> {
                compact = false
                prefs.compact = false
                if (gridMode) onToggleGrid()
            }

            LayoutMode.GRID -> {
                compact = false
                prefs.compact = false
                if (!gridMode) onToggleGrid()
            }

            LayoutMode.COMPACT -> {
                compact = true
                prefs.compact = true
                if (gridMode) onToggleGrid()
            }
        }
    }

    val removeWithUndo: (List<RecentVideo>) -> Unit = { items ->
        if (items.isNotEmpty()) {
            val uris = items.map { it.uri }.toSet()
            hidden = hidden + uris
            selectedUris = selectedUris - uris
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = if (items.size == 1) "Removed from recent" else "${items.size} videos removed",
                    actionLabel = "Undo",
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                if (result != SnackbarResult.ActionPerformed) items.forEach(onRemoveRecent)
                hidden = hidden - uris
            }
        }
    }

    val onTool: (Tool) -> Unit = { tool ->
        if (tool.available) {
            pickFromLibrary()
        } else {
            scope.launch { snackbar.showSnackbar("${tool.label} is coming soon") }
        }
    }

    LaunchedEffect(Unit) {
        entered = true
    }

    LaunchedEffect(state) {
        if (state is MainState.Failed) {
            snackbar.showSnackbar(state.message)
            onDismissError()
        }
    }

    LaunchedEffect(searching) {
        if (searching) {
            delay(80)
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = searching) { closeSearch() }
    BackHandler(enabled = selecting) { clearSelection() }

    CompositionLocalProvider(LocalShimmer provides shimmer) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AnimatedContent(
                    targetState = barMode,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "topBar"
                ) { mode ->
                    when (mode) {
                        BarMode.NORMAL -> LargeTopAppBar(
                            title = { Text("Trimly") },
                            actions = {
                                if (available.isNotEmpty()) {
                                    IconButton(onClick = { searching = true }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    }
                                }
                                IconButton(onClick = { showAbout = true }) {
                                    Icon(Icons.Filled.Info, contentDescription = "About")
                                }
                            },
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            scrollBehavior = scrollBehavior
                        )

                        BarMode.SEARCH -> TopAppBar(
                            title = {
                                SearchField(
                                    query = query,
                                    onQueryChange = { query = it },
                                    focusRequester = focusRequester,
                                    onSubmit = { focusManager.clearFocus() }
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = closeSearch) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                                }
                            },
                            actions = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear text")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        BarMode.SELECT -> {
                            val allFavorite = activeSelection.all { it in favorites }
                            TopAppBar(
                                title = {
                                    AnimatedContent(
                                        targetState = activeSelection.size,
                                        transitionSpec = {
                                            (slideInVertically { it } + fadeIn()) togetherWith
                                                (slideOutVertically { -it } + fadeOut())
                                        },
                                        label = "selectedCount"
                                    ) { count -> Text("$count selected") }
                                },
                                navigationIcon = {
                                    IconButton(onClick = clearSelection) {
                                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { selectedUris = visible.map { it.uri }.toSet() }) {
                                        Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                                    }
                                    IconButton(onClick = {
                                        updateFavorites(
                                            if (allFavorite) favorites - activeSelection else favorites + activeSelection
                                        )
                                    }) {
                                        Icon(
                                            imageVector = if (allFavorite) Icons.Filled.StarBorder else Icons.Filled.Star,
                                            contentDescription = if (allFavorite) "Remove from favorites" else "Add to favorites"
                                        )
                                    }
                                    IconButton(onClick = {
                                        removeWithUndo(available.filter { it.uri in activeSelection })
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove selected")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFab,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = pickFromLibrary,
                        expanded = fabExpanded,
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
                        Entrance(visible = entered, delayMillis = 0) {
                            HeroCard(
                                busy = busy,
                                greeting = greeting,
                                onSelect = pickFromLibrary,
                                onBrowse = browseFiles,
                                onRecord = record
                            )
                        }
                    }

                    if (latest != null) {
                        item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
                            Entrance(visible = entered, delayMillis = 80) {
                                ContinueCard(
                                    item = latest,
                                    enabled = !busy,
                                    onOpen = { onOpenRecent(latest) }
                                )
                            }
                        }
                    }

                    if (available.isNotEmpty()) {
                        item(key = "stats", span = { GridItemSpan(maxLineSpan) }) {
                            Entrance(visible = entered, delayMillis = 140) {
                                StatsRow(
                                    count = available.size,
                                    totalMs = totalMs,
                                    favorites = favoriteCount
                                )
                            }
                        }
                    }

                    if (!tipHidden) {
                        item(key = "tip", span = { GridItemSpan(maxLineSpan) }) {
                            Entrance(visible = entered, delayMillis = 200) {
                                TipCard(
                                    onDismiss = {
                                        tipHidden = true
                                        prefs.tipHidden = true
                                    }
                                )
                            }
                        }
                    }

                    item(key = "tools", span = { GridItemSpan(maxLineSpan) }) {
                        Entrance(visible = entered, delayMillis = 260) {
                            ToolsRow(enabled = !busy, onTool = onTool)
                        }
                    }
                }

                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    RecentHeader(
                        hasItems = available.isNotEmpty(),
                        selecting = selecting,
                        layoutMode = layoutMode,
                        sortMode = sortMode,
                        summary = when {
                            available.isEmpty() -> ""
                            visible.size != available.size -> "${visible.size} of ${available.size} videos"
                            else -> summary(available.size, totalMs)
                        },
                        onSort = setSort,
                        onLayout = setLayout,
                        onClear = { showClear = true }
                    )
                }

                if (available.isNotEmpty()) {
                    item(key = "chips", span = { GridItemSpan(maxLineSpan) }) {
                        FilterRow(
                            filter = durationFilter,
                            favoritesOnly = favoritesOnly,
                            onFilter = setDurationFilter,
                            onFavorites = { favoritesOnly = !favoritesOnly }
                        )
                    }
                }

                if (available.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyRecents()
                    }
                } else if (visible.isEmpty()) {
                    item(key = "no_results", span = { GridItemSpan(maxLineSpan) }) {
                        NoResults(query = query, filtered = filtersActive)
                    }
                } else {
                    items(
                        items = entries,
                        key = { it.key },
                        span = { entry ->
                            GridItemSpan(
                                if (entry is Entry.Header || layoutMode != LayoutMode.GRID) maxLineSpan else 1
                            )
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
                                    mode = layoutMode,
                                    favorite = item.uri in favorites,
                                    selecting = selecting,
                                    selected = item.uri in activeSelection,
                                    enabled = !busy,
                                    onClick = {
                                        if (selecting) toggleSelected(item.uri) else onOpenRecent(item)
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        toggleSelected(item.uri)
                                    },
                                    onSelect = { toggleSelected(item.uri) },
                                    onFavorite = { toggleFavorite(item.uri) },
                                    onDetails = { detailsFor = item },
                                    onRemove = { removeWithUndo(listOf(item)) },
                                    modifier = Modifier.animateItem()
                                )
                            }
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
                    DetailLine("Favorite", if (item.uri in favorites) "Yes" else "No")
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
private fun Modifier.pressScale(
    source: MutableInteractionSource,
    pressedScale: Float = 0.96f
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

@Composable
private fun Entrance(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350, delayMillis = delayMillis)) +
            slideInVertically(
                animationSpec = tween(450, delayMillis = delayMillis, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 5 }
            )
    ) {
        content()
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
    greeting: String,
    onSelect: () -> Unit,
    onBrowse: () -> Unit,
    onRecord: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "heroPulse")
    val ringScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )
    val primary = MaterialTheme.colorScheme.primary

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
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                            alpha = ringAlpha
                        }
                        .clip(CircleShape)
                        .background(primary)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pick a video to edit, or record a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = busy,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "heroActions"
            ) { isBusy ->
                if (isBusy) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Reading video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun ContinueCard(
    item: RecentVideo,
    enabled: Boolean,
    onOpen: () -> Unit
) {
    val source = remember { MutableInteractionSource() }

    Card(
        onClick = onOpen,
        enabled = enabled,
        interactionSource = source,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
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
private fun StatsRow(
    count: Int,
    totalMs: Long,
    favorites: Int
) {
    val animatedCount by animateIntAsState(count, tween(700), label = "statCount")
    val animatedMinutes by animateIntAsState((totalMs / 60_000L).toInt(), tween(900), label = "statMinutes")
    val animatedFavorites by animateIntAsState(favorites, tween(700), label = "statFavorites")

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StatCard(
            icon = Icons.Filled.Movie,
            value = animatedCount.toString(),
            label = if (count == 1) "Video" else "Videos",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Filled.Schedule,
            value = minutesLabel(animatedMinutes),
            label = "Total time",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Filled.Star,
            value = animatedFavorites.toString(),
            label = "Favorites",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipCard(onDismiss: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            index = (index + 1) % tips.size
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = index,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "tip"
            ) { current ->
                Text(
                    text = tips[current],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss tip",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToolsRow(
    enabled: Boolean,
    onTool: (Tool) -> Unit
) {
    Column {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tools, key = { it.label }) { tool ->
                ToolCard(tool = tool, enabled = enabled, onClick = { onTool(tool) })
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: Tool,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val tint = if (tool.available) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(88.dp)
            .pressScale(source)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .graphicsLayer { alpha = if (tool.available) 1f else 0.6f }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f))
            ) {
                Icon(imageVector = tool.icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = tool.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = if (tool.available) " " else "Soon",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RecentHeader(
    hasItems: Boolean,
    selecting: Boolean,
    layoutMode: LayoutMode,
    sortMode: SortMode,
    summary: String,
    onSort: (SortMode) -> Unit,
    onLayout: (LayoutMode) -> Unit,
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
                LayoutMenu(current = layoutMode, onSelect = onLayout)
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
            AnimatedContent(
                targetState = summary,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "summary"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
private fun LayoutMenu(
    current: LayoutMode,
    onSelect: (LayoutMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            AnimatedContent(
                targetState = current,
                transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                label = "layoutIcon"
            ) { mode ->
                Icon(
                    imageVector = mode.icon,
                    contentDescription = "Layout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LayoutMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    leadingIcon = { Icon(mode.icon, contentDescription = null) },
                    trailingIcon = {
                        if (mode == current) Icon(Icons.Filled.Check, contentDescription = null)
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
private fun FilterRow(
    filter: DurationFilter,
    favoritesOnly: Boolean,
    onFilter: (DurationFilter) -> Unit,
    onFavorites: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            selected = favoritesOnly,
            onClick = onFavorites,
            label = { Text("Favorites") },
            leadingIcon = {
                Icon(
                    imageVector = if (favoritesOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        )
        DurationFilter.entries.forEach { item ->
            FilterChip(
                selected = filter == item,
                onClick = { onFilter(item) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun SectionLabel(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        if (label == "Favorites") {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyRecents() {
    val bob = rememberInfiniteTransition(label = "bob")
    val offsetY by bob.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobY"
    )

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
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { translationY = offsetY }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No recent videos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Videos you open will show up here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoResults(
    query: String,
    filtered: Boolean
) {
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
            text = if (query.isNotBlank()) "No videos match \"${query.trim()}\"" else "No videos match these filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (filtered) {
            Text(
                text = "Try another filter or clear the search",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
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
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    val primary = MaterialTheme.colorScheme.primary
    val container by animateColorAsState(
        targetValue = if (selected) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "itemContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else Color.Transparent,
        animationSpec = tween(200),
        label = "itemBorder"
    )

    Surface(
        shape = shape,
        color = container,
        border = BorderStroke(2.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            .clip(shape)
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                onLongClickLabel = "Select",
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        content()
    }
}

@Composable
private fun RecentItem(
    item: RecentVideo,
    mode: LayoutMode,
    favorite: Boolean,
    selecting: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
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
        when (mode) {
            LayoutMode.GRID -> Column(modifier = Modifier.padding(8.dp)) {
                Thumb(
                    item = item,
                    selecting = selecting,
                    selected = selected,
                    favorite = favorite,
                    showDuration = true,
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
                    ItemMenuSlot(
                        selecting = selecting,
                        favorite = favorite,
                        onSelect = onSelect,
                        onFavorite = onFavorite,
                        onDetails = onDetails,
                        onRemove = onRemove
                    )
                }
            }

            LayoutMode.LIST -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Thumb(
                    item = item,
                    selecting = selecting,
                    selected = selected,
                    favorite = favorite,
                    showDuration = true,
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
                ItemMenuSlot(
                    selecting = selecting,
                    favorite = favorite,
                    onSelect = onSelect,
                    onFavorite = onFavorite,
                    onDetails = onDetails,
                    onRemove = onRemove
                )
            }

            LayoutMode.COMPACT -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Thumb(
                    item = item,
                    selecting = selecting,
                    selected = selected,
                    favorite = false,
                    showDuration = false,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${clock(item.durationMs)} · ${relativeTime(item.openedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (favorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = Amber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                ItemMenuSlot(
                    selecting = selecting,
                    favorite = favorite,
                    onSelect = onSelect,
                    onFavorite = onFavorite,
                    onDetails = onDetails,
                    onRemove = onRemove
                )
            }
        }
    }
}

@Composable
private fun ItemMenuSlot(
    selecting: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit
) {
    if (selecting) {
        Spacer(Modifier.size(48.dp))
    } else {
        RecentMenu(
            favorite = favorite,
            onSelect = onSelect,
            onFavorite = onFavorite,
            onDetails = onDetails,
            onRemove = onRemove
        )
    }
}

@Composable
private fun Thumb(
    item: RecentVideo,
    selecting: Boolean,
    selected: Boolean,
    favorite: Boolean,
    showDuration: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        VideoThumbnail(uri = item.uri, modifier = Modifier.fillMaxSize())
        if (showDuration) {
            DurationBadge(
                durationMs = item.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
        if (favorite && showDuration) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favorite",
                tint = Amber,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }
        AnimatedVisibility(
            visible = selecting,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            SelectionMark(selected = selected)
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "markScale"
    )
    Icon(
        imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
        contentDescription = if (selected) "Selected" else "Not selected",
        tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        modifier = Modifier
            .padding(6.dp)
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
    )
}

@Composable
private fun RecentMenu(
    favorite: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
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
                text = { Text(if (favorite) "Remove from favorites" else "Add to favorites") },
                leadingIcon = {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.StarBorder else Icons.Filled.Star,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onFavorite()
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
    val shimmer = LocalShimmer.current
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    var finished by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        bitmap = ThumbnailLoader.load(context, uri)
        finished = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Crossfade(
            targetState = bitmap,
            animationSpec = tween(300),
            label = "thumbnail"
        ) { image ->
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (finished) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val progress = shimmer?.value ?: 0f
                            val width = size.width
                            val start = -width + progress * 2f * width
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                                    startX = start,
                                    endX = start + width
                                )
                            )
                        }
                )
            }
        }
    }
}

private fun buildEntries(
    favoriteItems: List<RecentVideo>,
    others: List<RecentVideo>,
    grouped: Boolean,
    now: Long
): List<Entry> {
    val result = ArrayList<Entry>(favoriteItems.size + others.size + bucketLabels.size + 2)

    if (favoriteItems.isNotEmpty()) {
        result.add(Entry.Header("Favorites"))
        favoriteItems.forEach { result.add(Entry.Video(it)) }
    }

    if (grouped) {
        val bounds = dayBounds(now)
        var current = -1
        others.forEach { item ->
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
    } else {
        if (favoriteItems.isNotEmpty() && others.isNotEmpty()) result.add(Entry.Header("All videos"))
        others.forEach { result.add(Entry.Video(it)) }
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

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "Working late?"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun minutesLabel(minutes: Int): String = when {
    minutes >= 60 -> "%d h %02d m".format(Locale.ROOT, minutes / 60, minutes % 60)
    minutes > 0 -> "$minutes min"
    else -> "<1 min"
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
