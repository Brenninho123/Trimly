package com.brenninho.trimly.engine

import androidx.media3.common.Effect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import com.brenninho.trimly.model.ColorMath
import com.brenninho.trimly.model.ExportOptions

object EffectsFactory {

    fun build(options: ExportOptions): List<Effect> {
        val effects = ArrayList<Effect>()

        if (options.rotationDegrees % 360 != 0) {
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(options.rotationDegrees.toFloat())
                    .build()
            )
        }

        if (options.flipHorizontal) {
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setScale(-1f, 1f)
                    .build()
            )
        }

        ColorMath.combined(options)?.let { matrix ->
            effects.add(ColorMatrixEffect(ColorMath.toGl(matrix)))
        }

        options.targetHeight?.let { effects.add(Presentation.createForHeight(it)) }

        return effects
    }
}
