package com.musfit.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.TrainingPrRecord
import com.musfit.data.repository.WeeklyTrainingVolume
import com.musfit.domain.model.ExerciseProgress
import com.musfit.domain.model.TrainingTrendPoint
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.groupedShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Progress page body (Turn 10 §10f): the anchored exercise's e1RM trend as the
 * tonal hero, the weekly volume card, and Recent PR grouped rows with the
 * "All exercises" re-anchor link.
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
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        EstimatedOneRepMaxHero(progress = progress, period = period, accent = accent)
        WeeklyVolumeCard(weeklyVolume = weeklyVolume, accent = accent)
        RecentPrsSection(recentPrs = recentPrs, accent = accent, onOpenAllExercises = onOpenAllExercises)
    }
}

/** e1RM hero (10f): overline, 44/800 figure, trend delta, and the line chart. */
@Composable
private fun EstimatedOneRepMaxHero(
    progress: ExerciseProgress?,
    period: TrainingProgressPeriod,
    accent: TabAccent,
) {
    val trend = progress?.let { filterTrendByPeriod(it.trend, period, LocalDate.now()) }.orEmpty()
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = progressHeroOverline(progress?.exerciseName),
                style = MaterialTheme.typography.labelSmall,
                color = accent.onContainer,
            )
            if (progress == null || trend.isEmpty()) {
                Text(
                    text = "Complete workouts to build an e1RM trend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.onContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                return@Column
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = trend.last().bestEstimatedOneRepMaxKg.formatE1rm(),
                    style = MaterialTheme.typography.displayMedium,
                    color = accent.onContainer,
                )
                Text(
                    text = " kg",
                    style = MaterialTheme.typography.titleLarge,
                    color = accent.onContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                val delta = e1rmDeltaKg(trend)
                if (delta != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Icon(
                            imageVector = if (delta >= 0) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = accent.onContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = deltaLabel(delta),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = accent.onContainer,
                        )
                    }
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
                val labels = monthLabelsFor(trend)
                labels.forEachIndexed { index, label ->
                    val isCurrent = index == labels.lastIndex
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isCurrent) accent.onContainer else accent.onContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/** 2.5dp line + endpoint dot over an on-container hairline baseline (10f hero). */
@Composable
private fun ProgressLineChart(
    values: List<Double>,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    val lineColor = accent.color
    val baselineColor = accent.onContainer.copy(alpha = 0.18f)
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

/** Weekly volume (10f): white card, 6 rounded bars — tonal past, filled current. */
@Composable
private fun WeeklyVolumeCard(
    weeklyVolume: List<WeeklyTrainingVolume>,
    accent: TabAccent,
) {
    val weeks = weeklyVolume.sortedBy { it.weekStartEpochDay }.takeLast(VOLUME_WEEK_COUNT)
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Weekly volume",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MusFitTheme.colors.onSurface,
                )
                weeks.lastOrNull()?.let { current ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(fontWeight = FontWeight.ExtraBold, color = MusFitTheme.colors.onSurface),
                            ) {
                                append(volumeTonsLabel(current.totalVolumeKg))
                            }
                            append(" this week")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            if (weeks.isEmpty()) {
                Text(
                    text = "Complete workouts to build weekly volume.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                return@Column
            }
            val maxVolume = weeks.maxOf { it.totalVolumeKg }.takeIf { it > 0 } ?: 1.0
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
                                    color = if (isCurrent) accent.color else accent.container,
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
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isCurrent) MusFitTheme.colors.onSurface else MusFitTheme.colors.onSurfaceFaint,
                        )
                    }
                }
            }
        }
    }
}

/** Recent PRs (10f): grouped rows with amber trophy badges + teal e1RM figures. */
@Composable
private fun RecentPrsSection(
    recentPrs: List<TrainingPrRecord>,
    accent: TabAccent,
    onOpenAllExercises: () -> Unit,
) {
    val prs = recentPrs.take(RECENT_PR_COUNT)
    // PR rows plus the trailing "All exercises" link share one grouped list.
    val groupCount = prs.size + 1
    Column {
        Text(
            text = "Recent PRs",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            fontWeight = FontWeight.ExtraBold,
            color = MusFitTheme.colors.onSurface,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        if (prs.isEmpty()) {
            Text(
                text = "Beat a previous best to log a PR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            prs.forEachIndexed { index, pr ->
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = groupedShape(index, groupCount),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExpressiveBadge(
                            icon = Icons.Filled.EmojiEvents,
                            shape = if (index % 2 == 0) ExpressiveBadgeShape.Sunny else ExpressiveBadgeShape.Circle,
                            containerColor = MusFitTheme.colors.warningContainer,
                            contentColor = MusFitTheme.colors.warning,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pr.exerciseName,
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                color = MusFitTheme.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = prMetaLabel(pr, LocalDate.now()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        Text(
                            text = "e1RM ${pr.estimatedOneRepMaxKg.formatE1rm()}",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MusFitTheme.colors.positive,
                        )
                    }
                }
            }
            Surface(
                onClick = onOpenAllExercises,
                color = MusFitTheme.colors.surface,
                shape = groupedShape(groupCount - 1, groupCount),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                ) {
                    Text(
                        text = "All exercises",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent.color,
                    )
                }
            }
        }
    }
}

// --- Display helpers (pure, unit-tested) ---

/** "BACK SQUAT · ESTIMATED 1RM" — the e1RM hero overline. */
internal fun progressHeroOverline(exerciseName: String?): String =
    listOfNotNull(
        exerciseName?.trim()?.takeIf(String::isNotBlank)?.uppercase(Locale.US),
        "ESTIMATED 1RM",
    ).joinToString(" · ")

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
