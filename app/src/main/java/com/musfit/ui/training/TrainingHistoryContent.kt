package com.musfit.ui.training

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.SupersetGroup
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutRecapSummary
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.util.Locale

@Composable
fun TrainingHistoryContent(
    history: List<WorkoutHistorySummary>,
    selectedDetail: WorkoutHistoryDetail?,
    overview: TrainingHistoryOverview,
    accent: TabAccent,
    onOpenDetail: (String) -> Unit,
    onCloseDetail: () -> Unit,
) {
    if (selectedDetail != null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCloseDetail) { Text("Back", color = accent.color) }
            WorkoutRecapCard(selectedDetail)
            historyDetailGroupingsForDisplay(selectedDetail).forEach { grouping ->
                when (grouping) {
                    is ExerciseGrouping.Single -> HistoryExerciseBlockCard(grouping.block, accent)
                    is ExerciseGrouping.Superset -> HistorySupersetGroupCard(grouping.group, accent)
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "History")
        HistoryOverviewCard(overview = overview, accent = accent)
        if (history.isEmpty()) {
            Text(
                "Finish a workout to build history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        // Hairline list rows — the whole row opens the workout, no card chrome.
        Column(modifier = Modifier.fillMaxWidth()) {
            history.forEach { workout ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Open ${workout.title}") { onOpenDetail(workout.sessionId) }
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(workout.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${workout.completedSetCount} sets · ${workout.totalVolumeKg.formatKg()} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }
    }
}

@Composable
private fun WorkoutRecapCard(detail: WorkoutHistoryDetail) {
    val recap = detail.effectiveRecap()
    val metrics = workoutRecapMetricsForDisplay(recap)
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(detail.summary.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Workout recap",
                style = MaterialTheme.typography.labelLarge,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            metrics.chunked(3).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowMetrics.forEach { metric ->
                        HistorySummaryMetric(
                            label = metric.label,
                            value = metric.value,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowMetrics.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            recap.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                Text(
                    "Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                Text(notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HistoryOverviewCard(
    overview: TrainingHistoryOverview,
    accent: TabAccent,
) {
    // Naked overview hero — stats and the month calendar sit directly on the surface.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HistorySummaryMetric(
                label = "This week",
                value = overview.currentWeekWorkoutCount.toString(),
                modifier = Modifier.weight(1f),
            )
            HistorySummaryMetric(
                label = "Days",
                value = overview.currentWeekTrainingDayCount.toString(),
                modifier = Modifier.weight(1f),
            )
            HistorySummaryMetric(
                label = "Sets",
                value = overview.currentWeekCompletedSetCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HistorySummaryMetric(
                label = "Volume",
                value = "${overview.currentWeekVolumeKg.formatKg()} kg",
                modifier = Modifier.weight(1f),
            )
            HistorySummaryMetric(
                label = "Streak",
                value = "${overview.currentStreakDays}d",
                modifier = Modifier.weight(1f),
            )
            HistorySummaryMetric(
                label = "Best",
                value = "${overview.bestStreakDays}d",
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = overview.monthLabel.ifBlank { "This month" },
            style = MaterialTheme.typography.titleSmall,
        )
        HistoryCalendarGrid(overview = overview, accent = accent)
    }
}

@Composable
private fun HistoryCalendarGrid(
    overview: TrainingHistoryOverview,
    accent: TabAccent,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        overview.calendarWeeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    HistoryCalendarDayCell(day = day, accent = accent, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HistoryCalendarDayCell(
    day: TrainingHistoryCalendarDay?,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    if (day == null) {
        Box(modifier = modifier.height(34.dp))
        return
    }
    val hasWorkout = day.workoutCount > 0
    Surface(
        color = if (hasWorkout) accent.container else MusFitTheme.colors.surfaceVariant,
        shape = MusFitTheme.shapes.small,
        modifier = modifier.height(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasWorkout) FontWeight.Medium else FontWeight.Normal,
                color = if (hasWorkout) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistorySupersetGroupCard(
    group: SupersetGroup,
    accent: TabAccent,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(color = accent.container, shape = MusFitTheme.shapes.small) {
                    Text(
                        text = "Superset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = "${group.exerciseBlocks.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            group.exerciseBlocks.forEachIndexed { index, block ->
                HistoryExerciseBlockSection(block, accent)
                if (index < group.exerciseBlocks.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                }
            }
        }
    }
}

@Composable
private fun HistoryExerciseBlockCard(block: WorkoutExerciseBlock, accent: TabAccent) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        HistoryExerciseBlockSection(
            block = block,
            accent = accent,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun HistoryExerciseBlockSection(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            block.supersetLabel?.let { label ->
                Surface(color = accent.container, shape = MusFitTheme.shapes.small) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
        }
        block.sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = historySetLabel(index, set.setType),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(0.35f),
                )
                Text(
                    text = set.setType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    text = "${set.weightKg?.formatKg() ?: "-"} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    text = "${set.reps ?: "-"} reps",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    text = set.rpe?.let { "RPE ${it.formatKg()}" } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f),
                )
            }
            if (index < block.sets.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            }
        }
    }
}

@Composable
private fun HistorySummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    // Mirrors TrainingScreen's WeekSummaryMetric: plain value over a quiet caption.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MusFitTheme.colors.onSurface,
            maxLines = 1,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
    }
}

internal fun historyDetailGroupingsForDisplay(detail: WorkoutHistoryDetail): List<ExerciseGrouping> =
    detail.exerciseGroupings.ifEmpty {
        detail.exerciseBlocks.map { ExerciseGrouping.Single(it) }
    }

internal data class RecapMetric(
    val label: String,
    val value: String,
)

internal fun workoutRecapMetricsForDisplay(recap: WorkoutRecapSummary): List<RecapMetric> =
    listOf(
        RecapMetric("Duration", recap.durationSeconds.durationLabel()),
        RecapMetric("Sets", recap.completedSetCount.toString()),
        RecapMetric("Volume", "${recap.totalVolumeKg.formatKg()} kg"),
        RecapMetric("Exercises", recap.exerciseCount.toString()),
        RecapMetric("PRs", recap.personalRecordCount.toString()),
    )

internal fun workoutRecapMetricsForDisplay(detail: WorkoutHistoryDetail): List<RecapMetric> =
    workoutRecapMetricsForDisplay(detail.effectiveRecap())

private fun WorkoutHistoryDetail.effectiveRecap(): WorkoutRecapSummary =
    if (recap.hasVisibleData()) {
        recap
    } else {
        WorkoutRecapSummary(
            durationSeconds = summary.durationSeconds(),
            exerciseCount = exerciseBlocks.size,
            completedSetCount = summary.completedSetCount,
            totalVolumeKg = summary.totalVolumeKg,
            personalRecordCount = 0,
            notes = null,
        )
    }

private fun WorkoutRecapSummary.hasVisibleData(): Boolean =
    durationSeconds > 0 ||
        exerciseCount > 0 ||
        completedSetCount > 0 ||
        totalVolumeKg > 0.0 ||
        personalRecordCount > 0 ||
        !notes.isNullOrBlank()

private fun WorkoutHistorySummary.durationSeconds(): Int {
    val endedAt = endedAtEpochMillis ?: startedAtEpochMillis
    return ((endedAt - startedAtEpochMillis).coerceAtLeast(0L) / 1000L).toInt()
}

private fun historySetLabel(index: Int, setType: String): String =
    when (setType.lowercase()) {
        "warmup" -> "W"
        "drop" -> "D"
        "failure" -> "F"
        else -> "${index + 1}"
    }

private fun Int.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
