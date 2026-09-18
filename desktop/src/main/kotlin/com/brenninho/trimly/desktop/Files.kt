package com.brenninho.trimly.desktop

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Locale

object Recents {

    private val store = File(System.getProperty("user.home"), ".trimly/recent.txt")

    fun load(): List<File> =
        try {
            if (store.isFile) {
                store.readLines().filter { it.isNotBlank() }.map { File(it) }.filter { it.isFile }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

    fun add(file: File): List<File> {
        val list = (listOf(file) + load().filter { it.absolutePath != file.absolutePath }).take(8)
        try {
            store.parentFile?.mkdirs()
            store.writeText(list.joinToString("\n") { it.absolutePath })
        } catch (e: Exception) {
        }
        return list
    }
}

fun pickVideo(parent: Frame): File? {
    val dialog = FileDialog(parent, "Open video", FileDialog.LOAD)
    dialog.file = "*.mp4;*.mov;*.mkv;*.avi;*.webm;*.m4v;*.wmv"
    dialog.isVisible = true
    val name = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    return File(directory, name)
}

fun chooseSaveFile(parent: Frame, source: File): File? {
    val dialog = FileDialog(parent, "Export video", FileDialog.SAVE)
    dialog.file = "${source.nameWithoutExtension}_trimly.mp4"
    dialog.isVisible = true
    val name = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    val target = File(directory, name)
    return if (target.extension.equals("mp4", ignoreCase = true)) target else File(directory, "$name.mp4")
}

fun showInFolder(file: File) {
    try {
        val system = System.getProperty("os.name").orEmpty().lowercase()
        if (system.contains("win")) {
            ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
        } else {
            Desktop.getDesktop().open(file.parentFile)
        }
    } catch (e: Exception) {
    }
}

fun openWithSystem(file: File) {
    try {
        Desktop.getDesktop().open(file)
    } catch (e: Exception) {
    }
}

fun formatTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSeconds = safe / 1000
    val tenths = (safe % 1000) / 100
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d.%d".format(Locale.ROOT, hours, minutes, seconds, tenths)
    } else {
        "%02d:%02d.%d".format(Locale.ROOT, minutes, seconds, tenths)
    }
}
