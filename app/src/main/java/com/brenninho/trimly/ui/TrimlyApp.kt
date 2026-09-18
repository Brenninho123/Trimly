package com.brenninho.trimly.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import com.brenninho.trimly.MainState
import com.brenninho.trimly.data.RecentVideo
import com.brenninho.trimly.editor.EditorScreen

@Composable
fun TrimlyApp(
    state: MainState,
    recents: List<RecentVideo>,
    gridMode: Boolean,
    onPick: (Uri) -> Unit,
    onOpenRecent: (RecentVideo) -> Unit,
    onRemoveRecent: (RecentVideo) -> Unit,
    onClearRecents: () -> Unit,
    onToggleGrid: () -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit
) {
    when (state) {
        is MainState.Ready -> EditorScreen(clip = state.clip, onBack = onClose)
        else -> HomeScreen(
            state = state,
            recents = recents,
            gridMode = gridMode,
            onPick = onPick,
            onOpenRecent = onOpenRecent,
            onRemoveRecent = onRemoveRecent,
            onClearRecents = onClearRecents,
            onToggleGrid = onToggleGrid,
            onDismissError = onDismissError
        )
    }
}
