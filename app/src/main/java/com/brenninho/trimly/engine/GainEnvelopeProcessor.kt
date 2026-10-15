package com.brenninho.trimly.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

class GainEnvelopeProcessor(
    private val gain: Float,
    private val fadeInUs: Long,
    private val fadeOutUs: Long,
    private val totalUs: Long
) : BaseAudioProcessor() {

    private var frames = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        if (gain == 1f && fadeInUs <= 0L && fadeOutUs <= 0L) return AudioProcessor.AudioFormat.NOT_SET
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val format = inputAudioFormat
        val frameSize = format.bytesPerFrame
        val count = inputBuffer.remaining() / frameSize
        if (count == 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }
        val output = replaceOutputBuffer(count * frameSize)
        val channels = format.channelCount
        val rate = format.sampleRate.toLong().coerceAtLeast(1L)
        val pcm16 = format.encoding == C.ENCODING_PCM_16BIT

        for (frame in 0 until count) {
            val timeUs = (frames + frame) * 1_000_000L / rate
            val factor = gain * envelope(timeUs)
            for (channel in 0 until channels) {
                if (pcm16) {
                    val sample = inputBuffer.short * factor
                    output.putShort(sample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort())
                } else {
                    output.putFloat((inputBuffer.float * factor).coerceIn(-1f, 1f))
                }
            }
        }
        frames += count
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    override fun onFlush() {
        frames = 0L
    }

    override fun onReset() {
        frames = 0L
    }

    private fun envelope(timeUs: Long): Float {
        var factor = 1f
        if (fadeInUs > 0L && timeUs < fadeInUs) {
            factor = minOf(factor, timeUs.toFloat() / fadeInUs)
        }
        if (fadeOutUs > 0L && totalUs > 0L) {
            val remaining = totalUs - timeUs
            if (remaining < fadeOutUs) factor = minOf(factor, (remaining.toFloat() / fadeOutUs).coerceAtLeast(0f))
        }
        return factor.coerceIn(0f, 1f)
    }
}
