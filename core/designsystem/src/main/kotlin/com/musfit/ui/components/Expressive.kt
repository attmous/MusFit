package com.musfit.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.musfit.ui.theme.MusFitMotion
import com.musfit.ui.theme.MusFitTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Material 3 Expressive linear progress: a sinusoidal wave for the filled
 * portion, a straight light track for the remainder, and a stop dot at the
 * 100% end. The fill sweeps in on entry; [live] adds a slow phase drift for
 * values still accumulating today (static for historical values).
 */
@Composable
fun WavyProgressBar(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.5.dp,
    amplitude: Dp = 3.5.dp,
    wavelength: Dp = 20.dp,
    live: Boolean = false,
) {
    val target = progress.coerceIn(0f, 1f)
    // Entry sweep 0 → value with the spatial spring, then follow value changes.
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) { animated.animateTo(target, MusFitMotion.spatial()) }

    val phase by if (live) {
        rememberInfiniteTransition(label = "wavyPhase").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 4000, easing = LinearEasing), RepeatMode.Restart),
            label = "wavyPhaseValue",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(strokeWidth + amplitude * 2 + 2.dp)) {
        val stroke = strokeWidth.toPx()
        val amp = amplitude.toPx()
        val wl = wavelength.toPx()
        val dotRadius = stroke * 0.6f
        val midY = size.height / 2f
        // Keep round caps and the stop dot inside the canvas.
        val startX = stroke / 2f
        val endX = size.width - dotRadius * 2f
        val span = endX - startX
        val waveEnd = startX + span * animated.value
        val gap = 4.dp.toPx()

        if (animated.value > 0.001f) {
            val path = Path()
            var x = startX
            path.moveTo(x, midY + amp * sin(phase * 2f * PI.toFloat()))
            while (x < waveEnd) {
                x = (x + 2f).coerceAtMost(waveEnd)
                val y = midY + amp * sin((x - startX) / wl * 2f * PI.toFloat() + phase * 2f * PI.toFloat())
                path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        if (waveEnd + gap < endX) {
            drawLine(
                color = trackColor,
                start = Offset(waveEnd + gap, midY),
                end = Offset(endX, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        // Stop dot at the 100% end.
        drawCircle(color = color, radius = dotRadius, center = Offset(size.width - dotRadius, midY))
    }
}

/**
 * The M3 Expressive circular wavy countdown ring (the expressive-Clock timer
 * pattern, hand-built because material3 1.4.0 keeps
 * `CircularWavyProgressIndicator` internal). The elapsed arc is a sinusoidal
 * wave sweeping clockwise from 12 o'clock; the remainder is a quiet straight
 * track ending in a stop dot. [running] drifts the wave phase; [flatten]
 * (0 → 1) eases the wave to flat as the countdown completes.
 */
@Composable
fun CircularWavyRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.5.dp,
    amplitude: Dp = 2.5.dp,
    waves: Int = 16,
    running: Boolean = false,
    flatten: Float = 0f,
) {
    val target = progress.coerceIn(0f, 1f)
    // Entry sweep 0 → value with the spatial spring, then follow value changes.
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) { animated.animateTo(target, MusFitMotion.spatial()) }

    val phase by if (running) {
        rememberInfiniteTransition(label = "wavyRingPhase").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing), RepeatMode.Restart),
            label = "wavyRingPhaseValue",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
    val amplitudeScale by animateFloatAsState(
        targetValue = (1f - flatten).coerceIn(0f, 1f),
        animationSpec = MusFitMotion.effects(),
        label = "wavyRingAmplitudeScale",
    )

    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val maxAmp = amplitude.toPx()
        val amp = maxAmp * amplitudeScale
        val radius = (size.minDimension - stroke) / 2f - maxAmp
        if (radius <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val startAngle = -90f
        val sweep = animated.value * 360f
        val gapDegrees = 12f
        val dotAngle = startAngle + 360f - 7f

        if (animated.value > 0.003f) {
            val path = Path()
            var deg = 0f
            while (deg <= sweep) {
                val theta = (startAngle + deg) * (PI.toFloat() / 180f)
                val r = radius + amp * sin(deg / 360f * waves * 2f * PI.toFloat() + phase * 2f * PI.toFloat())
                val x = center.x + r * cos(theta)
                val y = center.y + r * sin(theta)
                if (deg == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                deg += 2f
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }

        val trackStart = startAngle + sweep + gapDegrees
        val trackSweep = dotAngle - 6f - trackStart
        if (trackSweep > 0f) {
            drawArc(
                color = trackColor,
                startAngle = trackStart,
                sweepAngle = trackSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        // Stop dot at the 100% end of the ring.
        val dotTheta = dotAngle * (PI.toFloat() / 180f)
        drawCircle(
            color = if (animated.value >= 0.999f) color else trackColor,
            radius = stroke * 0.6f,
            center = Offset(center.x + radius * cos(dotTheta), center.y + radius * sin(dotTheta)),
        )
    }
}

/**
 * The M3 Expressive badge shape family, alternated down lists for variety:
 * sunny (8-petal flower) → circle → squircle.
 */
enum class ExpressiveBadgeShape { Sunny, Circle, Squircle }

/** Cycle for row [index] in a list: sunny, circle, squircle, sunny, … */
fun expressiveBadgeShapeFor(index: Int): ExpressiveBadgeShape = ExpressiveBadgeShape.entries[index % ExpressiveBadgeShape.entries.size]

/**
 * The "sunny" flower: two superimposed squares with 38% corner radius, one
 * rotated 45° (the M3E `MaterialShapes.Sunny` silhouette, hand-built because
 * material3 1.4.0 doesn't expose MaterialShapes).
 */
object SunnyShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val side = minOf(size.width, size.height)
        // The rotated square's diagonal must match the upright square's side, so
        // both petals reach the same radius: inner side = side / sqrt(2) … but the
        // classic sunny keeps both squares equal and lets the corners overlap.
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = side * 0.38f
        fun square(): Path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f),
                    radiusX = radius,
                    radiusY = radius,
                ),
            )
        }
        val rotated = square().apply {
            transform(
                Matrix().apply {
                    translate(cx, cy)
                    rotateZ(45f)
                    translate(-cx, -cy)
                },
            )
        }
        val union = Path.combine(PathOperation.Union, square(), rotated)
        return Outline.Generic(union)
    }
}

/** Shape for a badge slot: sunny flower, plain circle, or 16dp squircle. */
fun ExpressiveBadgeShape.asShape(): Shape = when (this) {
    ExpressiveBadgeShape.Sunny -> SunnyShape
    ExpressiveBadgeShape.Circle -> CircleShape
    ExpressiveBadgeShape.Squircle -> RoundedCornerShape(16.dp)
}

/** A leading icon badge in a tab accent tint, cut to an expressive shape. */
@Composable
fun ExpressiveBadge(
    icon: ImageVector,
    shape: ExpressiveBadgeShape,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp,
) {
    Surface(color = containerColor, shape = shape.asShape(), modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
        }
    }
}

/**
 * M3E grouped-list containment: rows separated by 4dp gaps, 24dp corners on the
 * group's outside, 8dp on every inside corner. [gridGroupShape] generalizes the
 * same rule to 2D grids (stat rows, the measurement grid).
 */
fun groupedShape(index: Int, count: Int, outer: Dp = 24.dp, inner: Dp = 8.dp): RoundedCornerShape {
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

fun gridGroupShape(
    row: Int,
    rowCount: Int,
    column: Int,
    columnCount: Int,
    outer: Dp = 24.dp,
    inner: Dp = 8.dp,
): RoundedCornerShape {
    val firstRow = row == 0
    val lastRow = row == rowCount - 1
    val firstCol = column == 0
    val lastCol = column == columnCount - 1
    return RoundedCornerShape(
        topStart = if (firstRow && firstCol) outer else inner,
        topEnd = if (firstRow && lastCol) outer else inner,
        bottomStart = if (lastRow && firstCol) outer else inner,
        bottomEnd = if (lastRow && lastCol) outer else inner,
    )
}

/** Horizontal variant: cells side by side in one row (Training stats row). */
fun rowGroupShape(index: Int, count: Int, outer: Dp = 24.dp, inner: Dp = 8.dp): RoundedCornerShape = gridGroupShape(row = 0, rowCount = 1, column = index, columnCount = count, outer = outer, inner = inner)

/**
 * The shared 44dp circular tonal icon button used by every tab header
 * (edit / history / settings).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TonalHeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surfaceVariant,
        contentColor = MusFitTheme.colors.onSurface,
        shape = CircleShape,
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
    }
}
