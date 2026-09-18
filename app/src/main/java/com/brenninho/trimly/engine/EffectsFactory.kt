package com.brenninho.trimly.engine

import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import com.brenninho.trimly.model.ColorGrade
import com.brenninho.trimly.model.ExportOptions

object EffectsFactory {

    fun build(options: ExportOptions): List<Effect> {
        val effects = ArrayList<Effect>()

        if (options.hasVideoTransform) {
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(options.rotationDegrees.toFloat())
                    .setScale(if (options.flipHorizontal) -1f else 1f, 1f)
                    .build()
            )
        }

        effects.addAll(gradeEffects(options.filterGrade))
        effects.addAll(gradeEffects(options.adjustGrade))

        options.targetHeight?.let { effects.add(Presentation.createForHeight(it)) }

        return effects
    }

    private fun gradeEffects(grade: ColorGrade): List<Effect> {
        if (grade.isIdentity) return emptyList()

        val effects = ArrayList<Effect>()

        if (grade.saturation != 0f) {
            effects.add(
                HslAdjustment.Builder()
                    .adjustSaturation(grade.saturation * 100f)
                    .build()
            )
        }

        if (grade.red != 1f || grade.green != 1f || grade.blue != 1f) {
            effects.add(
                RgbAdjustment.Builder()
                    .setRedScale(grade.red)
                    .setGreenScale(grade.green)
                    .setBlueScale(grade.blue)
                    .build()
            )
        }

        if (grade.contrast != 0f) effects.add(Contrast(grade.contrast))
        if (grade.brightness != 0f) effects.add(Brightness(grade.brightness))
        if (grade.invert) effects.add(RgbFilter.createInvertedFilter())

        return effects
    }
}
