# Trimly

A free, open source video editing application for Android, built with Kotlin, Jetpack Compose and AndroidX Media3.

[![Build](https://github.com/Brenninho123/Trimly/actions/workflows/build.yml/badge.svg)](https://github.com/Brenninho123/Trimly/actions/workflows/build.yml)
![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![License](https://img.shields.io/badge/license-Unlicense-blue)

## Overview

Trimly is a lightweight video editor focused on doing a few things well: opening a video, previewing it, cutting it to the exact range you want and exporting the result. It runs fully on the device. No account, no upload, no watermark.

## Status

Trimly is in early development. The foundation and the trimming engine are in place. The full editor interface is next.

| Feature | Status |
| --- | --- |
| Pick a video with the system Photo Picker | Available |
| Open videos from "Share" and "Open with" | Available |
| Read video duration and metadata | Available |
| Video preview with playback controls | Available |
| Trim engine (`VideoExporter`) | Available |
| Timeline with trim handles | Planned |
| Export screen with progress | Planned |
| Merge multiple clips | Planned |
| Filters and effects | Planned |
| Audio controls | Planned |

## Features

- Runs entirely on-device, no network access required
- No storage permission needed, uses the system Photo Picker
- Hardware accelerated processing through Media3 Transformer
- Jetpack Compose interface with a dark Material 3 theme
- Small, readable codebase with a clean engine/UI separation

## Requirements

- Android 7.0 (API 24) or newer
- To build from source: JDK 17, Android Studio (Ladybug or newer) and Android SDK 35

## Getting Started

### Build from source

```bash
git clone https://github.com/Brenninho123/Trimly.git
cd Trimly
gradle assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

If your checkout includes the Gradle wrapper, use `./gradlew assembleDebug` instead.

### Download a build

Every push to `main` runs the GitHub Actions workflow. Open the latest run in the **Actions** tab and download the `Trimly-debug` artifact.

### Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Open Trimly.
2. Tap **Select video** and choose a video from your library.
3. Preview the clip in the player.
4. Trim and export the result.

You can also share a video from another app to Trimly, or choose Trimly from an "Open with" menu.

## Project Structure

```
app/src/main/java/com/brenninho/trimly/
  MainActivity.kt
  MainViewModel.kt
  engine/
    VideoExporter.kt
  model/
    Clip.kt
  ui/
    TrimlyApp.kt
    HomeScreen.kt
    PreviewScreen.kt
    theme/
      Theme.kt
```

| Layer | Responsibility |
| --- | --- |
| `model` | Plain data types that describe what the user is editing |
| `engine` | Video processing, no Android UI dependencies |
| `ui` | Jetpack Compose screens and theme |
| `MainViewModel` | Holds app state and survives configuration changes |

## Documentation

- [Video Guide](docs/VIDEO.md): supported formats, how trimming works, export behavior and performance tips
- [API Reference](docs/API.md): the public classes of the Trimly engine with usage examples

## Tech Stack

| Component | Library |
| --- | --- |
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 ExoPlayer |
| Processing | AndroidX Media3 Transformer and Effect |
| Async | Kotlin Coroutines and Flow |
| Build | Gradle Kotlin DSL, GitHub Actions |

## Roadmap

- [ ] Timeline with draggable start and end handles
- [ ] Export screen with progress and cancel
- [ ] Save exports to the gallery
- [ ] Multi-clip projects
- [ ] Split and reorder clips
- [ ] Speed, rotation and crop
- [ ] Audio volume and mute
- [ ] Filters and color adjustments
- [ ] Release signing and store distribution

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a branch: `git checkout -b feature/my-feature`
3. Commit your changes
4. Push the branch and open a pull request

Please keep engine code free of UI dependencies and make sure `assembleDebug` succeeds before opening a pull request.

## License

Trimly is released into the public domain under the [Unlicense](LICENSE).
