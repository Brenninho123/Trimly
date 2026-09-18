package com.brenninho.trimly.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File

fun main(args: Array<String>) = application {
    val windowState = rememberWindowState(size = DpSize(1240.dp, 820.dp))
    val icon = remember {
        runCatching { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }.getOrNull()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Trimly",
        state = windowState,
        icon = icon
    ) {
        TrimlyTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                DesktopApp(initial = args.firstOrNull()?.let { File(it) }?.takeIf { it.isFile })
            }
        }
    }
}
