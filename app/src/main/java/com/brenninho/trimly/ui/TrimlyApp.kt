package com.brenninho.trimly.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.brenninho.trimly.MainState
import com.brenninho.trimly.data.RecentVideo
import com.brenninho.trimly.data.ImportActions
import com.brenninho.trimly.data.ImportState
import com.brenninho.trimly.editor.EditorScreen
import com.brenninho.trimly.settings.SettingsActions
import com.brenninho.trimly.settings.SettingsState

@Composable
fun TrimlyApp(
    state: MainState,
    recents: List<RecentVideo>,
    gridMode: Boolean,
    settings: SettingsState,
    actions: SettingsActions,
    importState: ImportState,
    importActions: ImportActions,
    onPick: (Uri) -> Unit,
    onOpenRecent: (RecentVideo) -> Unit,
    onRemoveRecent: (RecentVideo) -> Unit,
    onClearRecents: () -> Unit,
    onToggleGrid: () -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it is MainState.Ready },
        transitionSpec = {
            if (targetState is MainState.Ready) {
                (slideInVertically(tween(360)) { it / 10 } + fadeIn(tween(320)) + scaleIn(tween(360), initialScale = 0.96f)) togetherWith
                    (fadeOut(tween(220)) + scaleOut(tween(300), targetScale = 1.04f))
            } else {
                (fadeIn(tween(300)) + scaleIn(tween(320), initialScale = 1.04f)) togetherWith
                    (slideOutVertically(tween(300)) { it / 10 } + fadeOut(tween(240)) + scaleOut(tween(300), targetScale = 0.96f))
            }
        },
        label = "appScreen"
    ) { target ->
        if (target is MainState.Ready) {
            EditorScreen(clip = target.clip, onBack = onClose)
        } else {
            HomeScreen(
                state = target,
                recents = recents,
                gridMode = gridMode,
                settings = settings,
                actions = actions,
                importState = importState,
                importActions = importActions,
                onPick = onPick,
                onOpenRecent = onOpenRecent,
                onRemoveRecent = onRemoveRecent,
                onClearRecents = onClearRecents,
                onToggleGrid = onToggleGrid,
                onDismissError = onDismissError
            )
        }
    }
}
