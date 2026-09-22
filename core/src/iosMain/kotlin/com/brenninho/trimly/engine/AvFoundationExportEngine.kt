package com.brenninho.trimly.engine

import com.brenninho.trimly.model.ExportOptions
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetExportPresetHighestQuality
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCancelled
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVMutableVideoComposition
import platform.AVFoundation.AVMutableVideoCompositionInstruction
import platform.AVFoundation.AVMutableVideoCompositionLayerInstruction
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMPersistentTrackID_Invalid
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
class AvFoundationExportEngine : ExportEngine {

    override suspend fun exportTrim(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        options: ExportOptions,
        outputPath: String,
        onProgress: (ExportProgress) -> Unit
    ): ExportOutcome {
        val sourceUrl = NSURL.fileURLWithPath(sourcePath)
        val outputUrl = NSURL.fileURLWithPath(outputPath)
        NSFileManager.defaultManager.removeItemAtURL(outputUrl, null)

        val asset = AVURLAsset(sourceUrl, null)
        val videoTrack = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
            ?: throw ExportFailedException(ExportFailure.SOURCE_UNREADABLE, "The source has no video track")

        val composition = AVMutableComposition()
        val compositionVideoTrack = composition.addMutableTrackWithMediaType(
            AVMediaTypeVideo,
            kCMPersistentTrackID_Invalid
        ) ?: throw ExportFailedException(ExportFailure.OTHER, "Could not create a composition video track")

        val timescale = videoTrack.naturalTimeScale
        val startTime = CMTimeMake(startMs * timescale / 1000L, timescale)
        val durationTime = CMTimeMake((endMs - startMs) * timescale / 1000L, timescale)
        val sourceRange = CMTimeRangeMake(startTime, durationTime)

        compositionVideoTrack.insertTimeRange(sourceRange, videoTrack, kCMTimeZero, null)

        if (!options.muted) {
            val audioTrack = asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack
            val compositionAudioTrack = audioTrack?.let {
                composition.addMutableTrackWithMediaType(AVMediaTypeAudio, kCMPersistentTrackID_Invalid)
            }
            compositionAudioTrack?.insertTimeRange(sourceRange, audioTrack!!, kCMTimeZero, null)
        }

        var transform = videoTrack.preferredTransform
        if (options.rotationDegrees != 0) {
            val radians = options.rotationDegrees.toDouble() / 180.0 * PI
            transform = CGAffineTransformConcat(transform, CGAffineTransformMakeRotation(radians))
        }
        if (options.flipHorizontal) {
            transform = CGAffineTransformConcat(transform, CGAffineTransformMakeScale(-1.0, 1.0))
        }

        val layerInstruction =
            AVMutableVideoCompositionLayerInstruction.videoCompositionLayerInstructionWithAssetTrack(compositionVideoTrack)
        layerInstruction.setTransform(transform, kCMTimeZero)

        val instruction = AVMutableVideoCompositionInstruction()
        instruction.timeRange = CMTimeRangeMake(kCMTimeZero, durationTime)
        instruction.layerInstructions = listOf(layerInstruction)

        val naturalSize = videoTrack.naturalSize.useContents { this }
        val videoComposition = AVMutableVideoComposition()
        videoComposition.instructions = listOf(instruction)
        videoComposition.frameDuration = CMTimeMake(1L, 30)
        videoComposition.renderSize = CGSizeMake(naturalSize.width, naturalSize.height)

        val exportSession = AVAssetExportSession(composition, AVAssetExportPresetHighestQuality)
            ?: throw ExportFailedException(ExportFailure.ENCODER, "Could not create an export session")
        exportSession.outputURL = outputUrl
        exportSession.outputFileType = AVFileTypeMPEG4
        exportSession.videoComposition = videoComposition

        val started = NSDate()

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { exportSession.cancelExport() }
            exportSession.exportAsynchronouslyWithCompletionHandler {
                when (exportSession.status) {
                    AVAssetExportSessionStatusCompleted -> {
                        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(outputPath, null)
                        val size = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
                        val elapsedMs = (NSDate().timeIntervalSinceDate(started) * 1000.0).toLong()
                        continuation.resume(ExportOutcome(outputPath, size, elapsedMs))
                    }
                    AVAssetExportSessionStatusCancelled -> {
                        continuation.cancel()
                    }
                    else -> {
                        val message = exportSession.error?.localizedDescription ?: "Export failed"
                        continuation.resumeWith(Result.failure(ExportFailedException(ExportFailure.OTHER, message)))
                    }
                }
            }
        }
    }
}
