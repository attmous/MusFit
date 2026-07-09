package com.musfit.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.TrainingPrRecord
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.ui.theme.IndigoContainerDark
import com.musfit.ui.theme.IndigoMuted
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Progress page body (mock 5b): the anchored exercise's e1RM chart with one big thin
 * number, weekly volume bars, and cross-exercise Recent PR rows — no cards, no grids.
 */
@Composable
fun TrainingProgressContent(
    progress: ExerciseProgress?,
    period: TrainingProgressPeriod,
    weeklyVolume: List<WeeklyTrainingVolume>,
    recentPrs: List<TrainingPrRecord>,
    accent: TabAccent,
    onOpenAllExercises: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        EstimatedOneRepMaxSection(progress = progress, period = period, accent = accent)
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        WeeklyVolumeSection(weeklyVolume = weeklyVolume, accent = accent)
        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        RecentPrsSection(recentPrs = recentPrs, accent = accent, onOpenAllExercises = onOpenAllExercises)
    }
}

@Composable
private fun EstimatedOneRepMaxSection(
    progress: ExerciseProgress?,
    period: TrainingProgressPeriod,
    accent: TabAccent,
) {
    val trend = progress?.let { filterTrendByPeriod(it.trend, period, LocalDate.now()) }.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (progress != null) "${progress.exerciseName} · estimated 1RM" else "Estimated 1RM",
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        if (progress == null || trend.isEmpty()) {
            Text(
                text = "Complete workouts to build an e1RM trend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            return
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = trend.last().bestEstimatedOneRepMaxKg.formatE1rm(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                    fontWeight = FontWeight.Light,
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = " kg",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Normal,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            val delta = e1rmDeltaKg(trend)
            if (delta != null) {
                Text(
                    text = deltaLabel(delta),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (delta >= 0) MusFitTheme.colors.positive else MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
        ProgressLineChart(
            values = trend.map { it.bestEstimatedOneRepMaxKg },
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            monthLabelsFor(trend).forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Bare 2.5dp line with an endpoint dot over a hairline baseline — no grid, no card (mock 5b). */
@Composable
private fun ProgressLineChart(
    values: List<Double>,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    val lineColor = accent.color
    val baselineColor = MusFitTheme.colors.outline
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val minValue = values.min()
        val maxValue = values.max()
        val range = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
        val padTop = 8.dp.toPx()
        val padBottom = 10.dp.toPx()
        val usableHeight = size.height - padTop - padBottom
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val points = values.mapIndexed { index, value ->
            val fraction = ((value - minValue) / range).toFloat()
            androidx.compose.ui.geometry.Offset(
                x = if (values.size > 1) index * stepX else size.width,
                y = padTop + usableHeight * (1f - fraction),
            )
        }
        drawLine(
            color = baselineColor,
            start = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
        )
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = points.last())
    }
}

@Composable
private fun WeeklyVolumeSection(
    weeklyVolume: List<WeeklyTrainingVolume>,
    accent: TabAccent,
) {
    val weeks = weeklyVolume.sortedBy { it.weekStartEpochDay }.takeLast(VOLUME_WEEK_COUNT)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Weekly volume",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            weeks.lastOrNull()?.let { current ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = volumeTonsLabel(current.totalVolumeKg),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = " this week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        if (weeks.isEmpty()) {
            Text(
                text = "Complete workouts to build weekly volume.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            return
        }
        val maxVolume = weeks.maxOf { it.totalVolumeKg }.takeIf { it > 0 } ?: 1.0
        val pastBarColor = if (isSystemInDarkTheme()) IndigoContainerDark else IndigoMuted
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            weeks.forEachIndexed { index, week ->
                val fraction = (week.totalVolumeKg / maxVolume).toFloat().coerceIn(0.04f, 1f)
                val isCurrent = index == weeks.lastIndex
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((78 * fraction).dp)
                            .background(
                                color = if (isCurrent) accent.color else pastBarColor,
                                shape = RoundedCornerShape(6.dp),
                            ),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            weeks.forEachIndexed { index, week ->
                val isCurrent = index == weeks.lastIndex
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = weekLabel(week.weekStartEpochDay),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                        color = if (isCurrent) MusFitTheme.colors.onSurface else MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPrsSection(
    recentPrs: List<TrainingPrRecord>,
    accent: TabAccent,
    onOpenAllExercises: () -> Unit,
) {
    Column {
        Text(
            text = "Recent PRs",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (recentPrs.isEmpty()) {
            Text(
                text = "Beat a previous best to log a PR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        recentPrs.take(RECENT_PR_COUNT).forEach { pr ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MusFitTheme.colors.warning,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pr.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = prMetaLabel(pr, LocalDate.now()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = "e1RM ${pr.estimatedOneRepMaxKg.formatE1rm()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.positive,
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAllExercises)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "All exercises",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = accent.color,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = accent.color,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// --- Display helpers (pure, unit-tested) ---

internal fun filterTrendByPeriod(
    trend: List<TrainingTrendPoint>,
    period: TrainingProgressPeriod,
    today: LocalDate,
): List<TrainingTrendPoint> {
    val cutoff = today.minusDays(period.days).toEpochDay()
    return trend.filter { it.dateEpochDay >= cutoff }.sortedBy { it.dateEpochDay }
}

/** e1RM change across the visible window; null with fewer than two points. */
internal fun e1rmDeltaKg(trend: List<TrainingTrendPoint>): Double? {
    if (trend.size < 2) return null
    return trend.last().bestEstimatedOneRepMaxKg - trend.first().bestEstimatedOneRepMaxKg
}

internal fun deltaLabel(delta: Double): String {
    val magnitude = kotlin.math.abs(delta).formatE1rm()
    return if (delta >= 0) "+$magnitude kg" else "−$magnitude kg"
}

/** Up to four month labels spread across the visible trend window. */
internal fun monthLabelsFor(trend: List<TrainingTrendPoint>): List<String> {
    if (trend.isEmpty()) return emptyList()
    val months = trend
        .map { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
        .distinct()
        .sorted()
    val picked = if (months.size <= 4) {
        months
    } else {
        listOf(
            months.first(),
            months[months.size / 3],
            months[months.size * 2 / 3],
            months.last(),
        ).distinct()
    }
    return picked.map { it.atDay(1).format(DateTimeFormatter.ofPattern("MMM", Locale.US)) }
}

/** "W27" ISO week label for a volume bar. */
internal fun weekLabel(weekStartEpochDay: Long): String {
    val week = LocalDate.ofEpochDay(weekStartEpochDay).get(WeekFields.ISO.weekOfWeekBasedYear())
    return "W$week"
}

/** "11.2 t" above a tonne, otherwise "840 kg". */
internal fun volumeTonsLabel(volumeKg: Double): String =
    if (volumeKg >= 1000.0) {
        val tons = volumeKg / 1000.0
        if (tons % 1.0 == 0.0) "${tons.toInt()} t" else String.format(Locale.US, "%.1f t", tons)
    } else {
        "${Math.round(volumeKg)} kg"
    }

/** PR row meta (mock 5b): "107.5 kg × 5 · Wed". */
internal fun prMetaLabel(pr: TrainingPrRecord, today: LocalDate): String {
    val date = LocalDate.ofEpochDay(pr.dateEpochDay)
    val dateLabel = when {
        date == today -> "today"
        date == today.minusDays(1) -> "yesterday"
        !date.isBefore(today.minusDays(6)) -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
    }
    return "${pr.weightKg.formatE1rm()} kg × ${pr.reps} · $dateLabel"
}

internal fun Double.formatE1rm(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }

private const val VOLUME_WEEK_COUNT = 6
private const val RECENT_PR_COUNT = 5
