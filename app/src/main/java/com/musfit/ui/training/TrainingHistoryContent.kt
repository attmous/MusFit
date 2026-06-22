package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onCloseDetail) { Text("Back") }
            Text(selectedDetail.summary.title, style = MaterialTheme.typography.headlineSmall)
            Text("${selectedDetail.summary.completedSetCount} sets - ${selectedDetail.summary.totalVolumeKg.formatKg()} kg")
            selectedDetail.exerciseBlocks.forEach { block ->
                Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
                block.sets.forEach { set ->
                    Text("${set.weightKg?.formatKg() ?: "-"} kg x ${set.reps ?: "-"} - ${set.setType}")
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

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
