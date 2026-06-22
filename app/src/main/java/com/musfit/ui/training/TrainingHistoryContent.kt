package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import java.util.Locale

@Composable
fun TrainingHistoryContent(
    history: List<WorkoutHistorySummary>,
    selectedDetail: WorkoutHistoryDetail?,
    onOpenDetail: (String) -> Unit,
    onCloseDetail: () -> Unit,
) {
    if (selectedDetail != null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCloseDetail) { Text("Back") }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(selectedDetail.summary.title, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HistorySummaryMetric(
                            label = "Sets",
                            value = selectedDetail.summary.completedSetCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        HistorySummaryMetric(
                            label = "Volume",
                            value = "${selectedDetail.summary.totalVolumeKg.formatKg()} kg",
                            modifier = Modifier.weight(1f),
                        )
                        selectedDetail.summary.durationLabel()?.let { duration ->
                            HistorySummaryMetric(
                                label = "Duration",
                                value = duration,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            selectedDetail.exerciseBlocks.forEach { block ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
                        block.sets.forEachIndexed { index, set ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = if (set.setType.equals("warmup", ignoreCase = true)) "W" else "${index + 1}",
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
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("History", style = MaterialTheme.typography.titleMedium)
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
                        Text("Open")
                    }
                }
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

private fun WorkoutHistorySummary.durationLabel(): String? {
    val endedAt = endedAtEpochMillis ?: return null
    val totalSeconds = ((endedAt - startedAtEpochMillis).coerceAtLeast(0L) / 1000L).toInt()
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
