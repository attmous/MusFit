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

private const val FoodAddItemSpotlightDurationMillis = 1800

internal data class FoodAddItemSpotlightTransform(
    val rowScale: Float,
    val borderAlpha: Float,
    val addIconScale: Float,
    val addContainerAlpha: Float,
)

@Composable
internal fun rememberFoodAddItemSpotlightProgress(): State<Float> {
    val transition = rememberInfiniteTransition(label = "foodAddItemSpotlight")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = FoodAddItemSpotlightDurationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "foodAddItemSpotlightProgress",
    )
}

internal fun foodAddItemSpotlightTransform(progress: Float): FoodAddItemSpotlightTransform {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val pulse = sin(clampedProgress * PI).toFloat().coerceAtLeast(0f)
    return FoodAddItemSpotlightTransform(
        rowScale = 1f + 0.014f * pulse,
        borderAlpha = 0.12f + 0.22f * pulse,
        addIconScale = 1f + 0.08f * pulse,
        addContainerAlpha = 0.82f + 0.18f * pulse,
    )
}
