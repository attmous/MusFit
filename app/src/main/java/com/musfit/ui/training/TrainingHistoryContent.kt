package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.util.Locale

@Composable
fun TrainingHistoryContent(
    history: List<WorkoutHistorySummary>,
    selectedDetail: WorkoutHistoryDetail?,
    accent: TabAccent,
    onOpenDetail: (String) -> Unit,
    onCloseDetail: () -> Unit,
) {
    if (selectedDetail != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCloseDetail) { Text("Back", color = accent.color) }
            Text(selectedDetail.summary.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
            Text(
                text = "${selectedDetail.summary.completedSetCount} sets · ${selectedDetail.summary.totalVolumeKg.formatKg()} kg",
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            selectedDetail.exerciseBlocks.forEach { block ->
                Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(block.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MusFitTheme.colors.onSurface)
                        block.sets.forEach { set ->
                            Text(
                                text = "${set.weightKg?.formatKg() ?: "-"} kg × ${set.reps ?: "-"} · ${set.setType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (history.isEmpty()) {
            Text("Finish a workout to build history.", color = MusFitTheme.colors.onSurfaceVariant)
        }
        history.forEach { workout ->
            Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(workout.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
                    Text(
                        text = "${workout.completedSetCount} sets · ${workout.totalVolumeKg.formatKg()} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                    TextButton(onClick = { onOpenDetail(workout.sessionId) }) { Text("Open", color = accent.color) }
                }
            }
        }
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
