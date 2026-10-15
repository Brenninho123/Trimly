package com.brenninho.trimly.model

enum class MoshMode {
    MELT,
    BLOOM
}

data class Datamosh(
    val mode: MoshMode = MoshMode.MELT,
    val startMs: Long = 1000L,
    val endMs: Long? = null,
    val keyframeSeconds: Float = 1f,
    val bloomFrames: Int = 20
) {
    companion object {
        val keyframeSteps = listOf(0.5f, 1f, 2f, 4f)
        const val MIN_BLOOM = 3
        const val MAX_BLOOM = 90
    }
}
