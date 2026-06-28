package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
                    is ExerciseGrouping.Single -> HistoryExerciseBlockCard(grouping.block)
                    is ExerciseGrouping.Superset -> HistorySupersetGroupCard(grouping.group, accent)
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("History", style = MaterialTheme.typography.titleMedium)
        HistoryOverviewCard(overview = overview, accent = accent)
        if (history.isEmpty()) {
            Text("Finish a workout to build history.")
        }
        history.forEach { workout ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(workout.title, style = MaterialTheme.typography.titleMedium)
                    Text("${workout.completedSetCount} sets - ${workout.totalVolumeKg.formatKg()} kg")
                    TextButton(onClick = { onOpenDetail(workout.sessionId) }) {
                        Text("Open", color = accent.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutRecapCard(detail: WorkoutHistoryDetail) {
    val recap = detail.effectiveRecap()
    val metrics = workoutRecapMetricsForDisplay(recap)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(detail.summary.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Workout recap",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                HorizontalDivider()
                Text(
                    "Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                fontWeight = FontWeight.SemiBold,
            )
            HistoryCalendarGrid(overview = overview, accent = accent)
        }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = if (hasWorkout) accent.container else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.height(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasWorkout) FontWeight.Bold else FontWeight.Normal,
                color = if (hasWorkout) accent.onContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistorySupersetGroupCard(
    group: SupersetGroup,
    accent: TabAccent,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(color = accent.container, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = "SUPERSET",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = "${group.exerciseBlocks.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            group.exerciseBlocks.forEachIndexed { index, block ->
                HistoryExerciseBlockSection(block)
                if (index < group.exerciseBlocks.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryExerciseBlockCard(block: WorkoutExerciseBlock) {
    Card(modifier = Modifier.fillMaxWidth()) {
        HistoryExerciseBlockSection(
            block = block,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun HistoryExerciseBlockSection(
    block: WorkoutExerciseBlock,
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
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f),
                )
            }
            if (index < block.sets.lastIndex) {
                HorizontalDivider()
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
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
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
