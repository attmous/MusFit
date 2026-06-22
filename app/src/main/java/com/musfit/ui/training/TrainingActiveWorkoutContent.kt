package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.LoggedWorkoutSetDetail
import java.util.Locale

@Composable
fun TrainingActiveWorkoutContent(
    workout: ActiveWorkoutDetail,
    restTimer: RestTimerState,
    onToggleSet: (String, Boolean) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.title, style = MaterialTheme.typography.headlineSmall)
                Text("${workout.completedSetCount} sets - ${workout.totalVolumeKg.formatKg()} kg")
            }
            Button(onClick = onFinish) { Text("Finish") }
            TextButton(onClick = onDiscard) { Text("Discard") }
        }
        if (restTimer.isVisible) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Rest ${restTimer.durationSeconds / 60}:00",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        workout.exerciseBlocks.forEach { block ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
                    block.targetReps?.let { Text("Target reps: $it") }
                    block.sets.forEachIndexed { index, set ->
                        WorkoutSetRow(index = index, set = set, onToggleSet = onToggleSet)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    index: Int,
    set: LoggedWorkoutSetDetail,
    onToggleSet: (String, Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("${index + 1}")
        Text(set.setType, modifier = Modifier.weight(0.8f))
        Text("${set.weightKg?.formatKg() ?: "-"} kg", modifier = Modifier.weight(1f))
        Text("${set.reps ?: "-"} reps", modifier = Modifier.weight(1f))
        Text("RPE ${set.rpe?.formatKg() ?: "-"}", modifier = Modifier.weight(1f))
        Checkbox(
            checked = set.completed,
            onCheckedChange = { checked -> onToggleSet(set.id, checked) },
        )
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
