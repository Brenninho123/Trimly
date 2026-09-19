@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.brenninho.trimly.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Settings
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
import com.brenninho.trimly.i18n.AppStrings
import com.brenninho.trimly.i18n.LocalStrings
import com.brenninho.trimly.settings.DiscordProfile
import com.brenninho.trimly.settings.SettingsActions
import com.brenninho.trimly.settings.SettingsState
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Amber = Color(0xFFFFC857)

private val LocalShimmer = compositionLocalOf<State<Float>?> { null }

private data class Tool(
    val id: String,
    val icon: ImageVector,
    val available: Boolean
)

private val tools = listOf(
    Tool("trim", Icons.Filled.ContentCut, true),
    Tool("filters", Icons.Filled.Palette, true),
    Tool("effects", Icons.Filled.AutoAwesome, true),
    Tool("adjust", Icons.Filled.Tune, true),
    Tool("rotate", Icons.Filled.RotateRight, true),
    Tool("quality", Icons.Filled.HighQuality, true),
    Tool("merge", Icons.Filled.Layers, true),
    Tool("speed", Icons.Filled.Speed, false),
    Tool("text", Icons.Filled.TextFields, true)
)

private fun toolLabel(s: AppStrings, id: String): String = when (id) {
    "trim" -> s.toolTrim
    "filters" -> s.toolFilters
    "effects" -> s.toolEffects
    "adjust" -> s.toolAdjust
    "rotate" -> s.toolRotate
    "quality" -> s.toolQuality
    "merge" -> s.toolMerge
    "speed" -> s.toolSpeed
    else -> s.toolText
}

private enum class SortMode {
    RECENT,
    OLDEST,
    NAME,
    NAME_DESC,
    LONGEST,
    SHORTEST;

    fun label(s: AppStrings): String = when (this) {
        RECENT -> s.sortRecent
        OLDEST -> s.sortOldest
        NAME -> s.sortNameAsc
        NAME_DESC -> s.sortNameDesc
        LONGEST -> s.sortLongest
        SHORTEST -> s.sortShortest
    }
}

private enum class LayoutMode(val icon: ImageVector) {
    LIST(Icons.Filled.ViewAgenda),
    GRID(Icons.Filled.GridView),
    COMPACT(Icons.Filled.Menu);

    fun label(s: AppStrings): String = when (this) {
        LIST -> s.layoutList
        GRID -> s.layoutGrid
        COMPACT -> s.layoutCompact
    }
}

private enum class DurationFilter {
    ALL,
    SHORT,
    MEDIUM,
    LONG;

    fun label(s: AppStrings): String = when (this) {
        ALL -> s.filterAll
        SHORT -> s.filterShort
        MEDIUM -> s.filterMedium
        LONG -> s.filterLong
    }

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

    data class Header(val id: String, val label: String) : Entry {
        override val key: String get() = "header_$id"
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
    settings: SettingsState,
    actions: SettingsActions,
    onPick: (Uri) -> Unit,
    onOpenRecent: (RecentVideo) -> Unit,
    onRemoveRecent: (RecentVideo) -> Unit,
    onClearRecents: () -> Unit,
    onToggleGrid: () -> Unit,
    onDismissError: () -> Unit
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val prefs = remember { HomePrefs(context) }
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortIndex by remember { mutableIntStateOf(prefs.sortIndex) }
    var filterIndex by remember { mutableIntStateOf(prefs.filterIndex) }
    var compact by remember { mutableStateOf(prefs.compact) }
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
    val profile = settings.profile
    val greeting = remember(s, profile?.displayName) {
        val base = greetingForNow(s)
        if (profile != null) "$base, ${profile.displayName}" else base
    }

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

    val entries = remember(visible, favorites, grouped, s) {
        val (favoriteItems, others) = visible.partition { it.uri in favorites }
        buildEntries(favoriteItems, others, grouped, System.currentTimeMillis(), s)
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
            scope.launch { snackbar.showSnackbar(s.noCameraApp) }
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
                    message = if (items.size == 1) s.removedOne else s.removedMany(items.size),
                    actionLabel = s.undo,
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
            scope.launch { snackbar.showSnackbar(s.comingSoon(toolLabel(s, tool.id))) }
        }
    }

    LaunchedEffect(Unit) {
        entered = true
    }

    LaunchedEffect(state) {
        if (state is MainState.Failed) {
            snackbar.showSnackbar(s.openFailed)
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
                                        Icon(Icons.Filled.Search, contentDescription = s.search)
                                    }
                                }
                                if (profile != null) {
                                    IconButton(onClick = { showSettings = true }) {
                                        AvatarImage(
                                            url = profile.avatarUrl,
                                            name = profile.displayName,
                                            size = 30.dp
                                        )
                                    }
                                }
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Filled.Settings, contentDescription = s.settings)
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
                                    placeholder = s.searchPlaceholder,
                                    onQueryChange = { query = it },
                                    focusRequester = focusRequester,
                                    onSubmit = { focusManager.clearFocus() }
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = closeSearch) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.closeSearch)
                                }
                            },
                            actions = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = s.clearText)
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
                                    ) { count -> Text(s.selectedCount(count)) }
                                },
                                navigationIcon = {
                                    IconButton(onClick = clearSelection) {
                                        Icon(Icons.Filled.Close, contentDescription = s.cancelSelection)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { selectedUris = visible.map { it.uri }.toSet() }) {
                                        Icon(Icons.Filled.SelectAll, contentDescription = s.selectAll)
                                    }
                                    IconButton(onClick = {
                                        updateFavorites(
                                            if (allFavorite) favorites - activeSelection else favorites + activeSelection
                                        )
                                    }) {
                                        Icon(
                                            imageVector = if (allFavorite) Icons.Filled.StarBorder else Icons.Filled.Star,
                                            contentDescription = if (allFavorite) s.removeFromFavorites else s.addToFavorites
                                        )
                                    }
                                    IconButton(onClick = {
                                        removeWithUndo(available.filter { it.uri in activeSelection })
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = s.removeSelected)
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
                        text = { Text(s.selectVideo) }
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
                                profile = profile,
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

                    if (settings.tipsEnabled) {
                        item(key = "tip", span = { GridItemSpan(maxLineSpan) }) {
                            Entrance(visible = entered, delayMillis = 200) {
                                TipCard(onDismiss = { actions.onTips(false) })
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
                            visible.size != available.size -> s.filteredSummary(visible.size, available.size)
                            else -> summary(available.size, totalMs, s)
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
                        EmptyRecents(onSelect = pickFromLibrary)
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
                                favorites = entry.id == "favorites",
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

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            actions = actions,
            onDismiss = { showSettings = false }
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text(s.clearTitle) },
            text = { Text(s.clearBody) },
            confirmButton = {
                TextButton(onClick = {
                    showClear = false
                    onClearRecents()
                }) { Text(s.clearConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text(s.cancel) }
            }
        )
    }

    detailsFor?.let { item ->
        AlertDialog(
            onDismissRequest = { detailsFor = null },
            title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    DetailLine(s.detailsDuration, clock(item.durationMs))
                    DetailLine(
                        s.detailsLastOpened,
                        DateUtils.formatDateTime(
                            context,
                            item.openedAt,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR
                        )
                    )
                    DetailLine(s.detailsFavorite, if (item.uri in favorites) s.yes else s.no)
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsFor = null }) { Text(s.close) }
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
    placeholder: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
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
    profile: DiscordProfile?,
    onSelect: () -> Unit,
    onBrowse: () -> Unit,
    onRecord: () -> Unit
) {
    val s = LocalStrings.current
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
                if (profile != null) {
                    AvatarImage(url = profile.avatarUrl, name = profile.displayName, size = 72.dp)
                } else {
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
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = s.heroSubtitle,
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
                            text = s.readingVideo,
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
                            Text(s.selectVideo)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(s.files)
                            }
                            OutlinedButton(onClick = onRecord, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(s.record)
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
    val s = LocalStrings.current
    val context = LocalContext.current
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
                    text = s.continueEditing,
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
                    text = "${clock(item.durationMs)} · ${relativeTime(s, item.openedAt, context)}",
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
    val s = LocalStrings.current
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
            label = if (count == 1) s.statVideo else s.statVideos,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Filled.Schedule,
            value = minutesLabel(animatedMinutes, s),
            label = s.statTotalTime,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Filled.Star,
            value = animatedFavorites.toString(),
            label = s.statFavorites,
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
    val s = LocalStrings.current
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(s) {
        while (true) {
            delay(5000)
            index = (index + 1) % s.tips.size
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
                    text = s.tips[current % s.tips.size],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = s.close,
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
    val s = LocalStrings.current
    Column {
        Text(
            text = s.toolsTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tools, key = { it.id }) { tool ->
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
    val s = LocalStrings.current
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
            .width(92.dp)
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
                text = toolLabel(s, tool.id),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (tool.available) " " else s.soon,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
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
    val s = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = s.recent,
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
                            contentDescription = s.clearRecent,
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
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = s.sort,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label(s)) },
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
    val s = LocalStrings.current
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
                    contentDescription = s.layout,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LayoutMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label(s)) },
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
    val s = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            selected = favoritesOnly,
            onClick = onFavorites,
            label = { Text(s.favorites) },
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
                label = { Text(item.label(s)) }
            )
        }
    }
}

@Composable
private fun SectionLabel(
    label: String,
    favorites: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        if (favorites) {
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
private fun EmptyRecents(onSelect: () -> Unit) {
    val s = LocalStrings.current
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
            text = s.emptyTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = s.emptySubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSelect) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.selectVideo)
        }
    }
}

@Composable
private fun NoResults(
    query: String,
    filtered: Boolean
) {
    val s = LocalStrings.current
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
            text = if (query.isNotBlank()) s.noMatchQuery(query.trim()) else s.noMatchFilters,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (filtered) {
            Text(
                text = s.tryAnotherFilter,
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
    val s = LocalStrings.current
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
                onLongClickLabel = s.menuSelect,
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
    val s = LocalStrings.current
    val context = LocalContext.current

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
                            text = relativeTime(s, item.openedAt, context),
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
                        text = relativeTime(s, item.openedAt, context),
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
                        text = "${clock(item.durationMs)} · ${relativeTime(s, item.openedAt, context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (favorite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = s.detailsFavorite,
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
    val s = LocalStrings.current
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
                contentDescription = s.detailsFavorite,
                tint = Amber,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }
        SelectionBadge(
            visible = selecting,
            selected = selected,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun SelectionBadge(
    visible: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        SelectionMark(selected = selected)
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
        contentDescription = null,
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
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(s.menuSelect) },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSelect()
                }
            )
            DropdownMenuItem(
                text = { Text(if (favorite) s.removeFromFavorites else s.addToFavorites) },
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
                text = { Text(s.menuDetails) },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDetails()
                }
            )
            DropdownMenuItem(
                text = { Text(s.menuRemove) },
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
    now: Long,
    s: AppStrings
): List<Entry> {
    val labels = listOf(s.bucketToday, s.bucketYesterday, s.bucketWeek, s.bucketOlder)
    val result = ArrayList<Entry>(favoriteItems.size + others.size + labels.size + 2)

    if (favoriteItems.isNotEmpty()) {
        result.add(Entry.Header("favorites", s.favorites))
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
                result.add(Entry.Header("bucket_$bucket", labels[bucket]))
            }
            result.add(Entry.Video(item))
        }
    } else {
        if (favoriteItems.isNotEmpty() && others.isNotEmpty()) {
            result.add(Entry.Header("all", s.allVideos))
        }
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

private fun greetingForNow(s: AppStrings): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> s.greetingLate
        hour < 12 -> s.greetingMorning
        hour < 18 -> s.greetingAfternoon
        else -> s.greetingEvening
    }
}

private fun minutesLabel(minutes: Int, s: AppStrings): String = when {
    minutes >= 60 -> "%d h %02d m".format(Locale.ROOT, minutes / 60, minutes % 60)
    minutes > 0 -> "$minutes min"
    else -> s.lessThanMinute
}

private fun summary(count: Int, totalMs: Long, s: AppStrings): String {
    val totalMinutes = totalMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val length = when {
        hours > 0 -> "%d h %02d min".format(Locale.ROOT, hours, minutes)
        totalMinutes > 0 -> "%d min".format(Locale.ROOT, totalMinutes)
        else -> "%d s".format(Locale.ROOT, totalMs / 1000)
    }
    return "${s.videoCount(count)} · $length"
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

private fun relativeTime(s: AppStrings, timestamp: Long, context: Context): String {
    val minutes = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> s.justNow
        minutes < 60 -> s.minutesAgo(minutes)
        minutes < 60 * 24 -> s.hoursAgo(minutes / 60)
        minutes < 60 * 24 * 7 -> s.daysAgo(minutes / (60 * 24))
        else -> DateUtils.formatDateTime(
            context,
            timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        )
    }
}
