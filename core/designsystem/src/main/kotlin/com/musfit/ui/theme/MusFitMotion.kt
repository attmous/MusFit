package com.musfit.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Springy M3 Expressive motion tokens (per the published M3E motion values:
 * bouncy spatial springs, critically-damped effects). material3 1.4.0 keeps its
 * MotionScheme API internal, so these are applied at call sites instead.
 */
object MusFitMotion {
    /** Bounds/shape/offset — overshoot-friendly. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> spatialFast(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Color/alpha — no overshoot. */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}
