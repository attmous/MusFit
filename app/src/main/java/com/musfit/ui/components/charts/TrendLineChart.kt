package com.musfit.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.musfit.domain.training.ChartPoint
import com.musfit.domain.training.TrendChartScaler
import com.musfit.ui.theme.MusFitTheme

/**
 * A smooth area-filled trend line in the Google-Health style: a Catmull-Rom-smoothed curve, a soft
 * area fill to the baseline, and an end-dot ringed in the surface colour. Reuses TrendChartScaler
 * for value→pixel geometry. Degrades to a single dot / nothing for 1 / 0 points.
 */
@Composable
fun TrendLineChart(
    values: List<Double>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val surface = MusFitTheme.colors.surface
    val areaColor = accent.copy(alpha = ChartDefaults.areaAlpha)
    val pad = 8f

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val geometry = TrendChartScaler.computeChartGeometry(values, size.width, size.height, pad, pad, pad, pad)
        val points = geometry.points
        val baseline = size.height - pad
        // The standard 5dp endpoint dot overwhelms the 24dp tile sparklines, so scale it
        // with height on short charts; anything ≥40dp tall keeps the full radius.
        val dotRadius = minOf(ChartDefaults.dotRadius.toPx(), size.height / 8f)

        if (points.size == 1) {
            drawCircle(color = accent, radius = dotRadius, center = Offset(points[0].x, points[0].y))
            return@Canvas
        }

        val linePath = smoothPath(points)
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, baseline)
            lineTo(points.first().x, baseline)
            close()
        }
        drawPath(areaPath, areaColor)
        drawPath(
            linePath,
            color = accent,
            style = Stroke(width = ChartDefaults.lineStroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val last = points.last()
        drawCircle(color = surface, radius = dotRadius + 2f, center = Offset(last.x, last.y))
        drawCircle(color = accent, radius = dotRadius, center = Offset(last.x, last.y))
    }
}

/** A Catmull-Rom spline through [points] converted to cubic Béziers, for a smooth line. */
private fun smoothPath(points: List<ChartPoint>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[if (i == 0) i else i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]
        val c1x = p1.x + (p2.x - p0.x) / 6f
        val c1y = p1.y + (p2.y - p0.y) / 6f
        val c2x = p2.x - (p3.x - p1.x) / 6f
        val c2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
    }
    return path
}
