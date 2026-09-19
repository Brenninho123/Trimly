# Trimly

A free, open source video editor for Android and Windows. Trim, merge, add text, apply filters and export, all on your own device. No account, no upload, no watermark.

[![Build APK](https://github.com/Brenninho123/Trimly/actions/workflows/build.yml/badge.svg)](https://github.com/Brenninho123/Trimly/actions/workflows/build.yml)
[![Windows](https://github.com/Brenninho123/Trimly/actions/workflows/windows.yml/badge.svg)](https://github.com/Brenninho123/Trimly/actions/workflows/windows.yml)
![Platform](https://img.shields.io/badge/platform-Android%207.0%2B%20%7C%20Windows-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![Languages](https://img.shields.io/badge/app%20languages-EN%20%7C%20PT%20%7C%20ES-blue)
![License](https://img.shields.io/badge/license-Unlicense-lightgrey)

## Table of contents

- [Features](#features)
- [Getting started](#getting-started)
- [Using the app](#using-the-app)
- [Configuration](#configuration)
- [Documentation](#documentation)
  - [Architecture](#architecture)
  - [Project structure](#project-structure)
  - [Core models](#core-models)
  - [Editor state and history](#editor-state-and-history)
  - [Preview and export pipelines](#preview-and-export-pipelines)
  - [Export engine](#export-engine)
  - [Text overlays](#text-overlays)
  - [Merging clips](#merging-clips)
  - [Localization](#localization)
  - [Settings and Discord login](#settings-and-discord-login)
  - [Intents, shortcuts and deep links](#intents-shortcuts-and-deep-links)
  - [Windows app](#windows-app)
  - [Continuous integration](#continuous-integration)
- [Troubleshooting](#troubleshooting)
- [Known limitations](#known-limitations)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Features

### Home

| Feature | Description |
| --- | --- |
| Open videos | Photo Picker, file browser, camera recording, share and open-with from other apps, drag and drop in split screen |
| Recent videos | Up to 20 items with thumbnails, last opened time and duration |
| Sorting | Last opened, oldest opened, name A-Z, name Z-A, longest first, shortest first |
| Layouts | List, grid and compact |
| Filters | All, under 1 minute, 1 to 5 minutes, over 5 minutes, favorites only |
| Favorites | Star videos to keep them in a section at the top |
| Search | Live search by file name |
| Multi-select | Long-press to select, then favorite or remove several at once |
| Undo removal | Removed items can be restored from the snackbar |
| Overview | Greeting, animated totals and a continue-editing card |
| Tips | Rotating tips that can be turned off in Settings |
| Settings | Gear button with language, Discord login, tips and about |

### Editor

| Feature | Description |
| --- | --- |
| Trim | Timeline with a 10-frame strip, draggable handles with time bubbles, scrubbing, minimum cut of 0.5 s |
| Precise cuts | Set start and end at the playhead, jump 5 seconds, double-tap the video to seek |
| Playback | Play, pause, loop the trimmed range, compare original colors by holding a button |
| History | Undo and redo up to 50 steps |
| Transform | Rotate in 90 degree steps, flip horizontally, mute |
| Filters | 9 filters plus Original, with adjustable intensity |
| Effects | 7 stylized effects |
| Adjust | Brightness, contrast, saturation and warmth |
| Text | Multiple text overlays with color, size, bold, background, position, dragging and timing |
| Merge | Join up to 10 clips in any order, each clip keeps its own trim |
| Output quality | Resolution (original, 1080p, 720p, 480p) and bitrate level (standard, high, maximum) |
| Export | MP4 saved to the gallery, ETA, cancel, share and open, automatic retry, stall detection |
| Menu | Categorized tool dock with status labels, badges and an all-tools grid |

### Planned tools

Speed, split, volume and fade appear in the menu as "soon".

## Getting started

### Requirements

- Android 7.0 (API 24) or newer to run the app
- JDK 17, Android Studio and Android SDK 35 to build it
- Windows 10 or newer to run the desktop app

### Build the Android app

```bash
git clone https://github.com/Brenninho123/Trimly.git
cd Trimly
gradle assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

If the Gradle wrapper is present in your checkout, use `./gradlew assembleDebug` instead. To install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Get a build without compiling

Every push to `main` runs the **Build APK** workflow. Open the latest run in the **Actions** tab and download the `Trimly-debug` artifact.

### Build the Windows app

```bash
gradle :desktop:run -PtrimlyDesktop=true
gradle :desktop:createDistributable -PtrimlyDesktop=true
gradle :desktop:packageMsi -PtrimlyDesktop=true
```

Details are in [Windows app](#windows-app).

## Using the app

### Home

1. Tap **Select video**, **Files** or **Record**, or share a video to Trimly from another app.
2. Tap a recent video to open it. Long-press to start selecting several.
3. Open the gear icon for language, Discord login and other options.

### Editor

| Action | How |
| --- | --- |
| Play or pause | Tap the video or the play button |
| Seek 5 seconds | Double-tap the left or right side of the video, or use the 5-second buttons |
| Set the cut | Move the playhead, then tap **Set start** or **Set end**, or drag the handles on the timeline |
| Loop | Turn on the repeat button to loop the trimmed range |
| Compare colors | Hold **Hold to compare** on the video |
| Undo and redo | Arrows in the top bar |
| Apply a filter | Style, then Filters or Effects, then pick one and set the intensity |
| Add text | Style, then Text, then **Add text**. Drag the text on the video to move it |
| Merge clips | Edit, then Merge, then **Add clip**. Tap a clip to trim it, use the arrows to reorder |
| Choose quality | Output, then Quality |
| Export | **Export** in the top bar |

The menu has four categories: Edit, Style, Audio and Output. Tap the grid button to see every tool at once, and tap the handle or the arrow to collapse it.

## Configuration

### Discord login

The login uses Discord OAuth2 with the implicit grant and the `identify` scope. To make it work with your own Discord application:

1. Open the Discord Developer Portal and select your application.
2. Go to **OAuth2**, then **Redirects**.
3. Add exactly this redirect:

```
com.brenninho.trimly://oauth/discord
```

4. Set your application ID in `DiscordAuth.CLIENT_ID`.

No client secret is used or stored. See [Settings and Discord login](#settings-and-discord-login) for the full flow.

### Permissions

| Permission | Why |
| --- | --- |
| `INTERNET` | Discord login, profile data and the profile picture |
| `WRITE_EXTERNAL_STORAGE` (API 28 and lower) | Declared for legacy devices. The Photo Picker and the gallery export on newer versions need no storage permission |

The app does not need storage permission to read videos, because files come from the Photo Picker, the document picker or share intents.

### Languages

Open **Settings**, then **Languages**, and choose System default, English, Portuguese (Brazil) or Spanish. The change is applied immediately and remembered.

## Documentation

### Architecture

```
MainActivity
  handles intents, shortcuts, drag and drop, language provider, keep screen on
    TrimlyApp
      AnimatedContent between screens
        HomeScreen        recents, search, sort, favorites, settings sheet
        EditorScreen      player, timeline, tool dock, panels, export dialogs

MainViewModel             recents, open flow, settings, Discord login
EditorViewModel           segments, options, text items, history, export job

engine                    VideoExporter, EffectsFactory, TextOverlays, MediaSaver
model                     Clip, ExportOptions, TextItem, VideoFilter, ColorMath
i18n                      AppStrings and the three language objects
```

Design rules:

- The UI is Jetpack Compose with Material 3 and a dark theme.
- State lives in two view models exposed as `StateFlow`.
- The `model` package is pure Kotlin except `Clip`. It is shared with the desktop module.
- The `engine` package has no Compose dependency.
- Every user-facing string goes through `AppStrings`.

### Project structure

```
app/src/main/java/com/brenninho/trimly/
  MainActivity.kt
  MainViewModel.kt
  TrimlyApplication.kt
  auth/
    DiscordAuth.kt
  data/
    FrameExtractor.kt
    RecentStore.kt
    RecentVideo.kt
    RemoteImages.kt
    ThumbnailLoader.kt
  editor/
    EditorScreen.kt
    EditorViewModel.kt
    EditorMenu.kt
    EditorControls.kt
    StylePanel.kt
    MergePanel.kt
    TextPanel.kt
    TextPreview.kt
    TrimTimeline.kt
    ExportDialog.kt
    ColorPreview.kt
  engine/
    VideoExporter.kt
    ExportModels.kt
    EffectsFactory.kt
    ColorMatrixEffect.kt
    TextOverlays.kt
    MediaSaver.kt
  i18n/
    Strings.kt
  model/
    Clip.kt
    ExportOptions.kt
    TextItem.kt
    VideoFilter.kt
    ColorMath.kt
  settings/
    AppSettings.kt
    SettingsModels.kt
  ui/
    HomeScreen.kt
    SettingsSheet.kt
    TrimlyApp.kt
    theme/Theme.kt

desktop/
  build.gradle.kts
  src/main/kotlin/com/brenninho/trimly/desktop/

.github/workflows/
  build.yml
  windows.yml
```

### Core models

#### Clip

An immutable video reference with a trim range. It validates itself on creation.

```kotlin
data class Clip(
    val uri: Uri,
    val durationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = durationMs
)
```

| Member | Description |
| --- | --- |
| `trimmedDurationMs` | Length of the kept range |
| `isTrimmed` | True when the range is shorter than the source |
| `withRange(start, end)` | Copy with a clamped range |
| `withStart(ms, minLength)` | Move only the start, keeping a minimum length |
| `withEnd(ms, minLength)` | Move only the end, keeping a minimum length |
| `shifted(delta)` | Move the whole range, keeping its length |
| `reset()` | Back to the full video |
| `coercePosition(ms)` | Clamp a source position into the range |
| `relativePosition(ms)` | Source position to position inside the trimmed range |
| `sourcePosition(ms)` | Position inside the trimmed range to source position |

#### ExportOptions

Everything that changes how the result looks or is encoded.

```kotlin
data class ExportOptions(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val muted: Boolean = false,
    val shortSide: Int? = null,
    val targetHeight: Int? = null,
    val filter: VideoFilter = VideoFilter.NONE,
    val filterIntensity: Float = 1f,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val saturation: Int = 0,
    val warmth: Int = 0,
    val quality: ExportQuality = ExportQuality.STANDARD,
    val texts: List<TextItem> = emptyList()
)
```

- `rotationDegrees` is counterclockwise, in steps of 90.
- `shortSide` is the user choice (1080, 720, 480). `targetHeight` is computed at export time from the source size and the rotation.
- Adjustments range from -100 to 100.

#### Filters and effects

| Group | Items |
| --- | --- |
| Filters | Original, Warm, Cool, Vivid, Faded, Vintage, Sepia, Cinematic, B&W, Noir |
| Effects | Neon, Dream, Chrome, Sunset, Frost, Night, Invert |

Each preset is a `ColorGrade` (brightness, contrast, saturation, RGB scale, invert). Intensity scales the grade linearly. Invert has no intensity.

#### TextItem

```kotlin
data class TextItem(
    val id: Long,
    val text: String,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val sizeFraction: Float = 0.06f,
    val x: Float = 0.5f,
    val y: Float = 0.85f,
    val bold: Boolean = true,
    val background: Boolean = false,
    val startMs: Long = 0L,
    val endMs: Long? = null
)
```

Position and size are fractions of the video frame, so a text looks the same at any resolution.

### Editor state and history

`EditorViewModel` exposes `state: StateFlow<EditorState>`.

```kotlin
data class EditorState(
    val segments: List<Segment>,
    val selected: Int,
    val options: ExportOptions,
    val selectedTextId: Long?,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val notice: EditorNotice?,
    val export: ExportStatus
)
```

Computed properties: `clip` (the selected segment), `totalTrimmedMs`, `offsetBeforeSelectedMs`, `hasEdits`, `canExport` and `availableShortSides`.

| Area | Functions |
| --- | --- |
| Trim | `beginRangeEdit`, `setRange`, `setStartAt`, `setEndAt`, `resetTrim` |
| Segments | `selectSegment`, `addSegment`, `removeSegment`, `moveSegment` |
| Options | `updateOptions`, `rotate`, `toggleFlip`, `toggleMute`, `setQuality`, `setQualityLevel` |
| Style | `setFilter`, `setFilterIntensity`, `setAdjustment`, `resetAdjustments` |
| Text | `addText`, `selectText`, `updateText`, `moveText`, `removeText` |
| History | `undo`, `redo`, `reset` |
| Export | `startExport`, `cancelExport`, `dismissExport` |

History stores snapshots of the segments, the selection and the options, up to 50 entries.

- Dragging a trim handle records one entry per gesture, through `beginRangeEdit`.
- Changes with a key, such as a slider or typing in a text field, are merged when they happen within one second of each other.
- Selecting a segment or a text does not create history.

### Preview and export pipelines

The preview and the exported file use the same color math, but different renderers.

| Step | Preview | Export |
| --- | --- | --- |
| Video surface | `TextureView` fed by ExoPlayer | Media3 `Transformer` |
| Rotation and flip | View rotation and scale | `ScaleAndRotateTransformation` |
| Colors | `ColorMatrixColorFilter` on the layer | `ColorMatrixEffect` (an `RgbMatrix`) |
| Text | Compose text over the video area | `OverlayEffect` with `TextOverlay` |

Flip is defined as a mirror applied after rotation, in both pipelines.

Color operations are composed in this order into a single matrix: saturation, RGB scale, contrast, brightness, invert. The filter grade is applied first and the manual adjustments second.

Export effect order for one clip: rotate, flip, color, resolution, text. For merged clips each item gets rotate, flip, color and a scale-to-fit resolution, and the text is applied to the composition.

### Export engine

`VideoExporter` turns segments and options into an MP4 file.

```kotlin
val exporter = VideoExporter(context)

val outcome = exporter.exportDetailed(
    segments = listOf(ExportSegment(uri, startMs = 2_000L, endMs = 14_000L)),
    options = ExportOptions(shortSide = 720, targetHeight = 720),
    output = File(context.cacheDir, "result.mp4")
) { progress ->
    val percent = (progress.fraction * 100).toInt()
    val remainingMs = progress.etaMs
}

val file = outcome.file
val size = outcome.sizeBytes
```

Older entry points, `export` and `trim`, still exist and return only the file.

#### Steps of an export

1. **Validation.** Every segment must be at least 200 ms long and readable.
2. **Plan.** The exporter reads the source size, frame rate and bitrate, builds the effect list and picks a bitrate.
3. **Space check.** The estimated size times 1.6 must fit in the free space of the output folder.
4. **Transform.** Media3 runs the transformer, with progress polled every 250 ms.
5. **Retry.** If the first attempt fails for an encoder, format or unknown reason, one more attempt runs in safe mode with H.264 and default encoder settings.
6. **Save.** The result is copied to the gallery.

#### Bitrate

```
bitrate = width x height x frameRate x 0.12 x levelFactor
```

- Frame rate is limited to 15 to 60 and defaults to 30.
- The result is limited to 0.8 to 50 Mbps.
- It is also capped by the source bitrate times the level cap.

| Level | Factor | Source cap |
| --- | --- | --- |
| Standard | 1.0 | 1.2 |
| High | 1.6 | 1.6 |
| Maximum | 2.4 | 2.4 |

#### Fast trim

When a single clip is only trimmed, with no filter, adjustment, rotation, flip, text, resolution change or mute, the exporter enables the Media3 trim optimization. Only the beginning of the range is re-encoded and the rest is copied. The result dialog shows a label when this was used.

#### Failures

Errors are reported as `ExportFailedException` with one of these reasons.

| Reason | Meaning |
| --- | --- |
| `EMPTY_RANGE` | A segment is too short |
| `SOURCE_UNREADABLE` | A source file cannot be read or was moved |
| `NO_SPACE` | Not enough free storage |
| `STALLED` | No progress for 90 seconds |
| `UNSUPPORTED` | The device cannot decode or encode the format |
| `ENCODER` | The video encoder failed to start |
| `OTHER` | Anything else, with the technical message |

Cancelling the coroutine cancels the transformer and deletes the partial file.

#### Where files are saved

- Android 10 and newer: `Movies/Trimly` in the shared gallery through `MediaStore`.
- Older versions: an app folder named `Trimly` inside the app movies directory.

### Text overlays

Text items are converted by `TextOverlays.build` into a Media3 `OverlayEffect`.

- The font size in pixels is `frameHeight x sizeFraction`.
- The anchor is placed by converting `x` and `y` (0 to 1, top-left origin) to normalized device coordinates.
- Visibility over time is handled by returning a fully transparent overlay outside the time range.
- The preview draws the same items with Compose above the video area. Dragging is enabled only while the Text panel is open.

Timing values are in the timeline of the final video. For merged clips the time of a text includes the trimmed length of every earlier clip.

### Merging clips

The editor keeps a list of segments. Each segment has its own clip and trim range.

- The limit is 10 segments, defined by `MAX_SEGMENTS`.
- The preview shows only the selected segment. The export joins all of them in order.
- With more than one segment, the exporter builds a `Composition` with one sequence. Text is applied at the composition level.
- All clips are scaled to fit the size of the first clip.
- Filters, adjustments, rotation, flip and mute apply to every clip.

### Localization

All text shown to the user is defined by the `AppStrings` interface in `i18n/Strings.kt`. It has one implementation per language: `EnglishStrings`, `PortugueseStrings` and `SpanishStrings`.

- `stringsFor(language)` returns the implementation. For the system option it follows the device language and falls back to English.
- `LocalStrings` provides the current implementation to Compose. It is set in `MainActivity` and reacts to language and configuration changes.
- Filter and adjustment names are translated with `filterName` and `adjustmentName`.

To add a language:

1. Add an entry to `AppLanguage`, with a code and its native name.
2. Create an object that implements every member of `AppStrings`. The compiler reports anything missing.
3. Return it from `stringsFor`.

To add a string, add a member to the interface and implement it in every language object.

### Settings and Discord login

Settings are stored in the `trimly_settings` preferences.

| Key | Content |
| --- | --- |
| Language | The selected language code |
| Tips | Whether tips are shown |
| Profile | Discord id, username, display name and avatar URL |
| Pending state | The random value of a login in progress |

Login flow:

1. `MainViewModel.beginDiscordLogin` creates a random state, stores it and returns the authorize URL.
2. `MainActivity` opens it in the browser.
3. Discord redirects to `com.brenninho.trimly://oauth/discord`, which reopens the app.
4. `handleAuthRedirect` compares the returned state with the stored one and rejects any mismatch.
5. The access token is used once to read the user profile from the Discord API. The token is not stored.
6. The profile is saved and shown in the top bar, the greeting and the settings sheet.

Logging out removes the saved profile. Login errors are reported with these reasons: cancelled, failed, network, security check failed and no browser.

### Intents, shortcuts and deep links

| Input | Behavior |
| --- | --- |
| `VIEW` and `EDIT` with `video/*` | Opens the video |
| `SEND` and `SEND_MULTIPLE` with `video/*` | Opens the first video and warns when there are more |
| `com.brenninho.trimly.action.PICK_VIDEO` | Opens the Photo Picker |
| `com.brenninho.trimly.action.RECORD_VIDEO` | Opens the camera recorder |
| `com.brenninho.trimly.action.CONTINUE_LAST` | Opens the most recent video |
| `com.brenninho.trimly://oauth/discord` | Finishes the Discord login |
| Drag and drop | Opens the first dropped video |

Launcher shortcuts:

- **Select video** and **Record** come from `res/xml/shortcuts.xml`.
- **Continue editing** is published at runtime while there are recent videos, in the current app language.

When the app is launched again from the recent apps screen, the original intent is ignored so the same video is not opened twice.

### Windows app

The `desktop` module is a Compose Desktop application. It is not part of the Android build unless you enable it.

```bash
gradle :desktop:run -PtrimlyDesktop=true
```

The property name has no dot on purpose. PowerShell splits arguments such as `-Ptrimly.desktop=true` and the module would not be included.

| Topic | Details |
| --- | --- |
| Toolkit | Compose Multiplatform 1.7.1, Material 3 |
| Shared code | The `model` folder of the Android app, except `Clip.kt` |
| Video engine | FFmpeg and FFprobe, called as external processes |
| Preview | Still frames, with colors and rotation applied in Compose |
| Export | H.264 video and AAC audio in MP4, progress and cancel |

FFmpeg is located in this order:

1. The application resources folder of the packaged app
2. The `TRIMLY_FFMPEG_DIR` environment variable
3. A folder named `ffmpeg` next to the app
4. The `PATH`

Features on Windows: open a video, recent files, trim with a range slider, filters, effects, adjustments, rotate, flip, mute, resolution and export. Not available yet: playback inside the editor, undo and redo, merge, text, and the Android quality levels.

The CI workflow downloads a GPL build of FFmpeg and bundles it. Distributing that binary requires following the FFmpeg license in addition to this project license.

### Continuous integration

| Workflow | Trigger | Output |
| --- | --- | --- |
| `build.yml` | Push, pull request, manual | `Trimly-debug` APK artifact |
| `windows.yml` | Changes in `desktop`, `model`, Gradle files, manual | `Trimly-windows-portable` zip and an MSI when available |

## Troubleshooting

| Problem | Cause and fix |
| --- | --- |
| `Unresolved reference` or `No parameter with name` after an update | Files from different versions are mixed. Replace all the files of the update together, especially `ExportOptions`, `EditorViewModel`, `EditorScreen`, `EditorMenu`, `StylePanel`, `ExportDialog` and `Strings` |
| `project 'desktop' not found` | Pass `-PtrimlyDesktop=true` to Gradle |
| Discord login does not return to the app | The redirect is not registered in the Discord portal, or it differs from `com.brenninho.trimly://oauth/discord` |
| Export says there is not enough storage | Free space on the device. The check is an estimate with a safety margin |
| Export fails on some videos | The device may not support the format. Try a lower resolution or the Standard quality |
| Merge fails with clips that have mixed audio | Media3 1.5 can reject a sequence where only some clips have audio. Mute the video or use clips that all have audio |
| Colors look wrong on HDR videos | HDR input is not tone mapped. Filters and adjustments may look different from the preview |
| A recent video does not open | The permission to the file expired. It is removed from the list and can be added again |

## Known limitations

- Speed, split, volume and fade are not implemented.
- Merge previews one clip at a time.
- Text uses the system font and has no rotation.
- HDR content is not tone mapped.
- The Windows app has no playback and no merge or text.
- Exports run only while the editor is open.

## Roadmap

- [ ] Speed control
- [ ] Split a clip in two
- [ ] Volume and audio fades
- [ ] Text styles, fonts and animations
- [ ] Merge and text on Windows
- [ ] Playback in the Windows editor
- [ ] Release signing and store distribution

## Contributing

1. Fork the repository.
2. Create a branch: `git checkout -b feature/my-feature`.
3. Make your changes and make sure `gradle assembleDebug` succeeds.
4. Open a pull request.

Project conventions:

- Keep the `engine` package free of Compose code.
- Add every new user-facing string to `AppStrings` in all three languages.
- Keep `model` free of Android types so the desktop module can use it. `Clip` is the only exception.
- The code base avoids comments, so prefer descriptive names.

## License

Trimly is released into the public domain under the [Unlicense](LICENSE).
