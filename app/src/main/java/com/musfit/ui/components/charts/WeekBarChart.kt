package com.musfit.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.musfit.domain.charts.BarChartScaler
import com.musfit.ui.theme.MusFitTheme
import kotlin.math.roundToInt

/** One bar: a day label and an optional value (null = untracked). */
data class BarDatum(val value: Double?, val label: String)

/**
 * A weekly bar chart in the Google-Health style: rounded-top bars, the selected bar solid in the
 * accent colour with the others faded, an optional dashed target line, a value bubble over the
 * selected bar, and day labels. Tapping a bar invokes [onBarSelected]. Stateless — the caller owns
 * the selection.
 */
@Composable
fun WeekBarChart(
    bars: List<BarDatum>,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    target: Double? = null,
    selectedIndex: Int? = null,
    valueFormatter: (Double) -> String = { it.roundToInt().toString() },
    onBarSelected: (Int) -> Unit = {},
) {
    val contextColor = accent.copy(alpha = ChartDefaults.barContextAlpha)
    val targetColor = MusFitTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MusFitTheme.colors.onSurfaceVariant)
    val selectedLabelStyle = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold)
    val bubbleStyle = MaterialTheme.typography.labelMedium.copy(color = onAccent, fontWeight = FontWeight.Bold)

    val geometry = remember(bars, target) { BarChartScaler.compute(bars.map { it.value }, target) }

    Canvas(
        modifier = modifier.pointerInput(bars) {
            detectTapGestures { offset ->
                if (bars.isEmpty()) return@detectTapGestures
                val slot = size.width / bars.size
                onBarSelected((offset.x / slot).toInt().coerceIn(0, bars.size - 1))
            }
        },
    ) {
        if (bars.isEmpty()) return@Canvas
        val labelGap = 18.dp.toPx()
        val bubbleSpace = 26.dp.toPx()
        val baseline = size.height - labelGap
        val plotH = (baseline - bubbleSpace).coerceAtLeast(0f)
        val slot = size.width / bars.size
        val barW = ChartDefaults.barWidth.toPx().coerceAtMost(slot * 0.6f)

        // Target line (under the bars).
        geometry.targetFraction?.let { tf ->
            val y = baseline - plotH * tf.toFloat()
            drawLine(
                color = targetColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(ChartDefaults.targetDash),
            )
        }

        // Bars + day labels.
        bars.forEachIndexed { index, datum ->
            val fraction = geometry.bars.getOrNull(index)
            val cx = slot * index + slot / 2f
            val left = cx - barW / 2f
            val right = cx + barW / 2f
            val selected = index == selectedIndex
            if (fraction == null) {
                drawLine(
                    color = contextColor,
                    start = Offset(left, baseline),
                    end = Offset(right, baseline),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            } else {
                val top = baseline - plotH * fraction.toFloat()
                val r = ChartDefaults.barCorner.toPx().coerceAtMost(barW / 2f).coerceAtMost(baseline - top).coerceAtLeast(0f)
                val barPath = Path().apply {
                    moveTo(left, baseline)
                    lineTo(left, top + r)
                    quadraticTo(left, top, left + r, top)
                    lineTo(right - r, top)
                    quadraticTo(right, top, right, top + r)
                    lineTo(right, baseline)
                    close()
                }
                drawPath(barPath, color = if (selected) accent else contextColor)
            }
            val measured = textMeasurer.measure(datum.label, if (selected) selectedLabelStyle else labelStyle)
            drawText(
                measured,
                topLeft = Offset(cx - measured.size.width / 2f, baseline + (labelGap - measured.size.height) / 2f),
            )
        }

        // Value bubble over the selected, tracked bar.
        val sel = selectedIndex ?: return@Canvas
        val selFraction = geometry.bars.getOrNull(sel) ?: return@Canvas
        val selValue = bars[sel].value ?: return@Canvas
        val cx = slot * sel + slot / 2f
        val top = baseline - plotH * selFraction.toFloat()
        val bubble = textMeasurer.measure(valueFormatter(selValue), bubbleStyle)
        val padX = 8.dp.toPx()
        val padY = 4.dp.toPx()
        val bw = bubble.size.width + padX * 2
        val bh = bubble.size.height + padY * 2
        val bx = (cx - bw / 2f).coerceIn(0f, (size.width - bw).coerceAtLeast(0f))
        val by = (top - bh - 6.dp.toPx()).coerceAtLeast(0f)
        drawRoundRect(
            color = accent,
            topLeft = Offset(bx, by),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(bh / 2f, bh / 2f),
        )
        drawText(bubble, topLeft = Offset(bx + padX, by + padY))
    }
}
