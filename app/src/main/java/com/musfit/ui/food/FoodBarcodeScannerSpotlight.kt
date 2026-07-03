package com.musfit.ui.food

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlin.math.PI
import kotlin.math.sin

private const val FoodBarcodeScannerSpotlightDurationMillis = 1600

internal data class FoodBarcodeScannerSpotlightTransform(
    val containerScale: Float,
    val iconScale: Float,
    val containerAlpha: Float,
    val borderAlpha: Float,
)

@Composable
internal fun rememberFoodBarcodeScannerSpotlightProgress(): State<Float> {
    val transition = rememberInfiniteTransition(label = "foodBarcodeScannerSpotlight")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = FoodBarcodeScannerSpotlightDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "foodBarcodeScannerSpotlightProgress",
    )
}

internal fun foodBarcodeScannerSpotlightTransform(progress: Float): FoodBarcodeScannerSpotlightTransform {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val pulse = sin(clampedProgress * PI).toFloat().coerceAtLeast(0f)
    return FoodBarcodeScannerSpotlightTransform(
        containerScale = 1f + 0.06f * pulse,
        iconScale = 1f + 0.12f * pulse,
        containerAlpha = 0.84f + 0.16f * pulse,
        borderAlpha = 0.14f + 0.28f * pulse,
    )
}
