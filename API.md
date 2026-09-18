# Trimly API Reference

This document describes the public classes that make up the Trimly engine and app state. All examples are written in Kotlin.

Package root: `com.brenninho.trimly`

## Contents

- [Overview](#overview)
- [Clip](#clip)
- [VideoExporter](#videoexporter)
- [MainState](#mainstate)
- [MainViewModel](#mainviewmodel)
- [End to End Example](#end-to-end-example)
- [Error Handling](#error-handling)
- [Threading](#threading)

## Overview

| Class | Package | Purpose |
| --- | --- | --- |
| `Clip` | `model` | Immutable description of a video and its trim range |
| `VideoExporter` | `engine` | Trims a video and writes the result to a file |
| `MainState` | root | UI state exposed by the view model |
| `MainViewModel` | root | Loads a video and exposes its state |

The engine (`Clip`, `VideoExporter`) has no dependency on Compose, so it can be reused from any Android component.

## Clip

```kotlin
data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = durationMs
)
```

An immutable snapshot of a video and the range that should be kept.

### Properties

| Name | Type | Description |
| --- | --- | --- |
| `uri` | `Uri` | Source video, any URI readable through `ContentResolver` |
| `durationMs` | `Long` | Full duration of the source in milliseconds |
| `startMs` | `Long` | Start of the kept range in milliseconds, defaults to `0` |
| `endMs` | `Long` | End of the kept range in milliseconds, defaults to `durationMs` |
| `trimmedDurationMs` | `Long` | Length of the kept range, never negative |

### Functions

#### `withRange(newStart: Long, newEnd: Long): Clip`

Returns a copy with a new trim range. Values are clamped so the result is always valid:

- `start` is clamped to `0..durationMs`
- `end` is clamped to `start..durationMs`

```kotlin
val clip = Clip(uri = videoUri, durationMs = 60_000L)

val trimmed = clip.withRange(newStart = 5_000L, newEnd = 20_000L)

trimmed.startMs
trimmed.endMs
trimmed.trimmedDurationMs
```

Out of range values never throw:

```kotlin
val safe = clip.withRange(newStart = -500L, newEnd = 999_999L)

safe.startMs
safe.endMs
```

Result: `startMs = 0`, `endMs = 60000`.

## VideoExporter

```kotlin
class VideoExporter(private val context: Context)
```

Wraps the Media3 `Transformer` and exposes a coroutine based API.

### Constructor

| Parameter | Type | Description |
| --- | --- | --- |
| `context` | `Context` | Any context, an application context is recommended |

### Functions

#### `trim`

```kotlin
suspend fun trim(
    source: Uri,
    startMs: Long,
    endMs: Long,
    output: File,
    onProgress: (Float) -> Unit
): File
```

Cuts `source` to the range `startMs..endMs` and writes it to `output`.

| Parameter | Type | Description |
| --- | --- | --- |
| `source` | `Uri` | Input video |
| `startMs` | `Long` | Start of the range to keep, in milliseconds |
| `endMs` | `Long` | End of the range to keep, in milliseconds |
| `output` | `File` | Destination file, use the `.mp4` extension |
| `onProgress` | `(Float) -> Unit` | Called about every 200 ms with a value from `0f` to `1f`, and once with `1f` on completion |

**Returns** the same `output` file once the export has finished.

**Throws** `ExportException` if the export fails.

**Cancellation** is supported. Cancelling the calling coroutine cancels the underlying transformer.

```kotlin
val exporter = VideoExporter(applicationContext)
val output = File(cacheDir, "trimmed.mp4")

val result = exporter.trim(
    source = clip.uri,
    startMs = clip.startMs,
    endMs = clip.endMs,
    output = output,
    onProgress = { progress -> println("Export ${(progress * 100).toInt()}%") }
)
```

## MainState

```kotlin
sealed interface MainState {
    data object Idle : MainState
    data object Loading : MainState
    data class Ready(val clip: Clip) : MainState
    data class Failed(val message: String) : MainState
}
```

| State | Meaning |
| --- | --- |
| `Idle` | No video is open, the home screen is shown |
| `Loading` | A video was selected and its metadata is being read |
| `Ready` | The video is loaded, `clip` holds the parsed data |
| `Failed` | The video could not be read, `message` describes why |

## MainViewModel

```kotlin
class MainViewModel(application: Application) : AndroidViewModel(application)
```

### Properties

| Name | Type | Description |
| --- | --- | --- |
| `state` | `StateFlow<MainState>` | Current app state |

### Functions

| Function | Description |
| --- | --- |
| `open(uri: Uri)` | Reads the duration of the video on a background dispatcher and moves to `Ready`, or `Failed` if the duration cannot be read |
| `close()` | Returns to `Idle` |

State transitions:

```
Idle -> Loading -> Ready
                -> Failed
Ready -> Idle
Failed -> Loading (on a new open call)
```

Collecting the state in Compose:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()

when (state) {
    is MainState.Ready -> PreviewScreen(clip = (state as MainState.Ready).clip, onBack = viewModel::close)
    else -> HomeScreen(state = state, onPick = viewModel::open)
}
```

## End to End Example

Open a video, pick a range and export it from a `ViewModel`:

```kotlin
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val exporter = VideoExporter(application)

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _result = MutableStateFlow<File?>(null)
    val result: StateFlow<File?> = _result.asStateFlow()

    fun export(clip: Clip) {
        viewModelScope.launch {
            val output = File(getApplication<Application>().cacheDir, "export_${System.currentTimeMillis()}.mp4")
            try {
                _result.value = exporter.trim(
                    source = clip.uri,
                    startMs = clip.startMs,
                    endMs = clip.endMs,
                    output = output,
                    onProgress = { _progress.value = it }
                )
            } catch (e: ExportException) {
                _result.value = null
            }
        }
    }
}
```

Usage:

```kotlin
val clip = Clip(uri = videoUri, durationMs = durationMs)
    .withRange(newStart = 3_000L, newEnd = 12_500L)

editorViewModel.export(clip)
```

## Error Handling

`VideoExporter.trim` throws `androidx.media3.transformer.ExportException`. Useful fields:

| Field | Description |
| --- | --- |
| `errorCode` | Numeric code, see the constants on `ExportException` |
| `message` | Human readable description |
| `cause` | Underlying exception, when available |

Recommended pattern:

```kotlin
try {
    exporter.trim(source, startMs, endMs, output, onProgress)
} catch (e: ExportException) {
    val code = e.errorCode
    val text = e.message
}
```

Common causes: the source file is not readable, the device has no decoder for the source format, the output path is not writable or there is not enough free storage.

## Threading

- `VideoExporter.trim` must be called from a coroutine. It switches to the main dispatcher internally because Media3 `Transformer` requires a thread with a `Looper`
- `onProgress` is invoked on the main thread
- `MainViewModel.open` reads metadata on `Dispatchers.IO` and publishes state on the main thread
- `Clip` is immutable and safe to share across threads
