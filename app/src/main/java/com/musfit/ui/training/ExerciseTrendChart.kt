package com.musfit.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.domain.training.TrendChartScaler
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TrendMetric(val label: String) {
    Volume("Volume"),
    EstOneRepMax("Est. 1RM"),
    Heaviest("Heaviest"),
}

internal fun TrainingTrendPoint.valueFor(metric: TrendMetric): Double = when (metric) {
    TrendMetric.Volume -> volumeKg
    TrendMetric.EstOneRepMax -> bestEstimatedOneRepMaxKg
    TrendMetric.Heaviest -> heaviestWeightKg
}

@Composable
fun TrendMetricToggle(
    selected: TrendMetric,
    accent: TabAccent,
    onSelect: (TrendMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrendMetric.entries.forEach { metric ->
            FilterChip(
                selected = metric == selected,
                onClick = { onSelect(metric) },
                label = { Text(metric.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.container,
                    selectedLabelColor = accent.onContainer,
                ),
            )
        }
    }
}

@Composable
fun ExerciseTrendChart(
    trend: List<TrainingTrendPoint>,
    metric: TrendMetric,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    val values = remember(trend, metric) { trend.map { it.valueFor(metric) } }
    var selectedIndex by remember(trend, metric) { mutableStateOf<Int?>(null) }

    val lineColor = accent.color
    val areaColor = accent.color.copy(alpha = 0.10f)
    val gridColor = MusFitTheme.colors.onSurfaceVariant.copy(alpha = 0.12f)
    val guideColor = MusFitTheme.colors.onSurfaceVariant.copy(alpha = 0.30f)
    val pillColor = accent.container
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.bodySmall.copy(color = MusFitTheme.colors.onSurfaceVariant)
    val pillStyle = MaterialTheme.typography.labelMedium.copy(color = accent.onContainer, fontWeight = FontWeight.Bold)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }

    val padL = 40f
    val padR = 16f
    val padT = 14f
    val padB = 22f

    Canvas(
        modifier = modifier.pointerInput(values) {
            detectTapGestures { offset ->
                if (values.isEmpty()) return@detectTapGestures
                val geometry = TrendChartScaler.computeChartGeometry(
                    values, size.width.toFloat(), size.height.toFloat(), padL, padR, padT, padB,
                )
                selectedIndex = TrendChartScaler.nearestIndex(offset.x, geometry.points.map { it.x })
            }
        },
    ) {
        if (values.isEmpty()) return@Canvas
        val geometry = TrendChartScaler.computeChartGeometry(values, size.width, size.height, padL, padR, padT, padB)
        val points = geometry.points
        val baseline = size.height - padB

        // Gridlines + y-axis tick labels.
        geometry.yTicks.forEach { tick ->
            val fraction = if (geometry.maxValue == geometry.minValue) {
                0.5
            } else {
                (tick - geometry.minValue) / (geometry.maxValue - geometry.minValue)
            }
            val y = padT + (size.height - padT - padB) * (1f - fraction.toFloat())
            drawLine(gridColor, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1f)
            val measured = textMeasurer.measure(formatAxisValue(tick, metric), axisStyle)
            drawText(measured, topLeft = Offset(0f, y - measured.size.height / 2f))
        }

        // Area fill + line (only meaningful for 2+ points).
        if (points.size > 1) {
            val areaPath = Path().apply {
                moveTo(points.first().x, baseline)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, baseline)
                close()
            }
            drawPath(areaPath, areaColor)
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                linePath,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        // Scrub guideline at the selected point.
        val calloutIndex = selectedIndex ?: points.lastIndex
        val calloutPoint = points[calloutIndex]
        if (selectedIndex != null) {
            drawLine(guideColor, Offset(calloutPoint.x, padT), Offset(calloutPoint.x, baseline), strokeWidth = 1f)
        }

        // Data dots (the callout point is enlarged).
        points.forEachIndexed { index, point ->
            drawCircle(
                color = lineColor,
                radius = if (index == calloutIndex) 5.dp.toPx() else 2.5.dp.toPx(),
                center = Offset(point.x, point.y),
            )
        }

        // Callout / tooltip pill: scrubbing shows the date too.
        val valueText = formatAxisValue(values[calloutIndex], metric) + " kg"
        val pillText = if (selectedIndex != null) {
            LocalDate.ofEpochDay(trend[calloutIndex].dateEpochDay).format(dateFormatter) + "  " + valueText
        } else {
            valueText
        }
        val measuredPill = textMeasurer.measure(pillText, pillStyle)
        val padX = 8.dp.toPx()
        val padY = 4.dp.toPx()
        val pillW = measuredPill.size.width + padX * 2
        val pillH = measuredPill.size.height + padY * 2
        val pillX = (calloutPoint.x - pillW / 2f).coerceIn(padL, (size.width - padR - pillW).coerceAtLeast(padL))
        val pillY = (calloutPoint.y - pillH - 6.dp.toPx()).coerceAtLeast(padT)
        drawRoundRect(
            color = pillColor,
            topLeft = Offset(pillX, pillY),
            size = Size(pillW, pillH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawText(measuredPill, topLeft = Offset(pillX + padX, pillY + padY))

        // X-axis: first and last date only.
        if (trend.isNotEmpty()) {
            val firstLabel = textMeasurer.measure(LocalDate.ofEpochDay(trend.first().dateEpochDay).format(dateFormatter), axisStyle)
            drawText(firstLabel, topLeft = Offset(padL, baseline + 4f))
            if (trend.size > 1) {
                val lastLabel = textMeasurer.measure(LocalDate.ofEpochDay(trend.last().dateEpochDay).format(dateFormatter), axisStyle)
                drawText(lastLabel, topLeft = Offset(size.width - padR - lastLabel.size.width, baseline + 4f))
            }
        }
    }
}

private fun formatAxisValue(value: Double, metric: TrendMetric): String =
    if (metric == TrendMetric.Volume) compactK(value) else value.formatKgValue()

private fun compactK(value: Double): String =
    if (value >= 1000.0) {
        val k = value / 1000.0
        if (k % 1.0 == 0.0) "${k.toInt()}k" else String.format(Locale.US, "%.1fk", k)
    } else {
        value.formatKgValue()
    }

private fun Double.formatKgValue(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)
