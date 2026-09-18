package com.brenninho.trimly.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import com.brenninho.trimly.MainState

@Composable
fun TrimlyApp(
    state: MainState,
    onPick: (Uri) -> Unit,
    onClose: () -> Unit
) {
    when (state) {
        is MainState.Ready -> PreviewScreen(clip = state.clip, onBack = onClose)
        else -> HomeScreen(state = state, onPick = onPick)
    }
}
