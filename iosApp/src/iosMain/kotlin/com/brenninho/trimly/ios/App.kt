package com.brenninho.trimly.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.brenninho.trimly.design.TrimlyTheme

@Composable
fun App() {
    TrimlyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TrimlyApp()
        }
    }
}

@Composable
private fun TrimlyApp() {
    val scope = rememberCoroutineScope()
    var controller by remember { mutableStateOf<EditorController?>(null) }
    var recents by remember { mutableStateOf(Recents.load()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val open: (String) -> Unit = { path ->
        val info = probeVideo(path)
        if (info == null) {
            errorMessage = "Could not read this video."
        } else {
            controller = EditorController(scope, path, info)
            recents = Recents.add(path)
        }
    }

    LaunchedEffect(VideoPickerBridge.pickedPath.value) {
        VideoPickerBridge.pickedPath.value?.let { path ->
            VideoPickerBridge.pickedPath.value = null
            open(path)
        }
    }

    val current = controller
    if (current == null) {
        HomeScreen(
            recents = recents,
            errorMessage = errorMessage,
            onDismissError = { errorMessage = null },
            onBrowse = { VideoPickerBridge.present?.invoke() },
            onOpen = open
        )
    } else {
        EditorScreen(
            controller = current,
            onClose = { controller = null }
        )
    }
}
