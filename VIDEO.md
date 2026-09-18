# Trimly Video Guide

How Trimly handles video: what it can open, how trimming works, what you get on export and how to keep things fast.

## Contents

- [Supported Input](#supported-input)
- [Opening a Video](#opening-a-video)
- [Preview](#preview)
- [Trimming](#trimming)
- [Exporting](#exporting)
- [Performance Tips](#performance-tips)
- [Limitations](#limitations)
- [Troubleshooting](#troubleshooting)

## Supported Input

Trimly relies on the decoders that Android provides on the device, so what you can open depends on the device itself.

| Item | Typically supported |
| --- | --- |
| Containers | MP4, MOV, 3GP, MKV, WebM |
| Video codecs | H.264 (AVC), H.265 (HEVC), VP8, VP9 |
| Audio codecs | AAC, MP3, Opus, Vorbis |

Support for HEVC, VP9 and 10-bit content varies between devices. If a video plays in the gallery app of the same device, it will normally work in Trimly.

## Opening a Video

There are three ways to open a video:

1. **Select video** on the home screen, which opens the system Photo Picker. No storage permission is required.
2. **Share** a video from another app and choose Trimly.
3. **Open with** from a file manager or gallery and choose Trimly.

After a video is selected, Trimly reads its duration. If the duration cannot be read, the file is rejected and an error message is shown.

## Preview

The preview screen plays the selected video with standard controls: play, pause, seek and fullscreen through the Media3 player view. Total duration is shown below the player in `mm:ss`, or `h:mm:ss` for videos longer than one hour.

## Trimming

Trimming keeps a single continuous range of the video and discards the rest.

```
source   |------------------------------------------|
                 start            end
                   |==============|
result             |==============|
```

A trim range is defined by two values in milliseconds:

| Value | Meaning |
| --- | --- |
| `startMs` | Where the kept range begins |
| `endMs` | Where the kept range ends |

Rules applied by `Clip.withRange`:

- Start can never be below `0` or above the video duration
- End can never be below the start or above the video duration
- A range where start equals end produces an empty clip, which cannot be exported

Example:

```kotlin
val clip = Clip(uri = videoUri, durationMs = 90_000L)
val range = clip.withRange(newStart = 10_000L, newEnd = 25_000L)
```

This keeps 15 seconds, from 0:10 to 0:25.

## Exporting

Export is handled by `VideoExporter.trim`, which is built on Media3 Transformer and uses hardware codecs when the device supports them.

| Property | Value |
| --- | --- |
| Output container | MP4 |
| Output location | The file you pass as `output` |
| Progress | Reported from `0f` to `1f` |
| Cancellation | Supported through coroutine cancellation |

Basic flow:

```kotlin
val exporter = VideoExporter(applicationContext)
val output = File(cacheDir, "trimmed.mp4")

exporter.trim(
    source = clip.uri,
    startMs = clip.startMs,
    endMs = clip.endMs,
    output = output,
    onProgress = { progress -> }
)
```

Files written to `cacheDir` can be cleared by the system. To keep the result, copy it to shared storage through `MediaStore`:

```kotlin
val values = ContentValues().apply {
    put(MediaStore.Video.Media.DISPLAY_NAME, "Trimly_${System.currentTimeMillis()}.mp4")
    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Trimly")
}

val target = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

target?.let { uri ->
    contentResolver.openOutputStream(uri)?.use { out ->
        output.inputStream().use { input -> input.copyTo(out) }
    }
}
```

On Android 9 and older, writing to `Movies/` through `MediaStore` requires the `WRITE_EXTERNAL_STORAGE` permission.

## Performance Tips

- Export from a file on internal storage or a fast SD card when possible
- Keep the app in the foreground during export, Android may pause background work
- Very long or 4K videos take longer and need more free space, keep at least the size of the source available
- Close other heavy apps on low memory devices before exporting
- Prefer trimming shorter ranges when testing, then export the full range

## Limitations

Current version limitations:

- Only single range trimming is implemented in the engine
- The timeline interface and export screen are not built yet
- No merging, splitting, filters, speed or audio editing yet
- Output is MP4 only
- Codec support depends on the device

See the roadmap in the [README](../README.md) for what is planned.

## Troubleshooting

| Problem | Likely cause | What to try |
| --- | --- | --- |
| "Could not read this video" | Unsupported or corrupted file | Play it in the gallery first, try a different file |
| Export fails immediately | No decoder for the source format | Try an MP4 with H.264 |
| Export fails near the end | Not enough free storage | Free up space and try again |
| Progress stays at 0% | Very large source or busy device | Wait a little, close other apps |
| Output is shorter than expected | Trim range was clamped | Check `startMs` and `endMs` against the video duration |
