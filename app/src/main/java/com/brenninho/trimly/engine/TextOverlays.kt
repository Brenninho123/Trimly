package com.brenninho.trimly.engine

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.brenninho.trimly.model.TextItem
import com.google.common.collect.ImmutableList
import kotlin.math.roundToInt

object TextOverlays {

    private val BACKGROUND = 0xAA000000.toInt()

    fun build(items: List<TextItem>, frameHeight: Int): OverlayEffect? {
        val usable = items.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return null
        val overlays: List<TextureOverlay> = usable.map { overlay(it, frameHeight) }
        return OverlayEffect(ImmutableList.copyOf(overlays))
    }

    private fun overlay(item: TextItem, frameHeight: Int): TextureOverlay {
        val sizePx = (frameHeight * item.sizeFraction).roundToInt().coerceAtLeast(12)
        val flags = Spanned.SPAN_INCLUSIVE_INCLUSIVE
        val text = SpannableString(item.text)
        text.setSpan(ForegroundColorSpan(item.colorArgb), 0, text.length, flags)
        text.setSpan(AbsoluteSizeSpan(sizePx), 0, text.length, flags)
        if (item.bold) text.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, flags)
        if (item.background) text.setSpan(BackgroundColorSpan(BACKGROUND), 0, text.length, flags)

        val anchorX = item.x * 2f - 1f
        val anchorY = 1f - item.y * 2f
        val shown = OverlaySettings.Builder()
            .setBackgroundFrameAnchor(anchorX, anchorY)
            .build()
        val hidden = OverlaySettings.Builder()
            .setBackgroundFrameAnchor(anchorX, anchorY)
            .setAlphaScale(0f)
            .build()

        val startUs = item.startMs * 1000L
        val endUs = item.endMs?.let { it * 1000L }

        return object : TextOverlay() {
            override fun getText(presentationTimeUs: Long): SpannableString = text

            override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings =
                if (presentationTimeUs >= startUs && (endUs == null || presentationTimeUs <= endUs)) shown else hidden
        }
    }
}
