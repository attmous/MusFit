package com.musfit.ui.training

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
import com.musfit.domain.model.ExerciseProgress
import java.util.Locale

@Composable
fun TrainingProgressContent(
    exercises: List<ExerciseSummary>,
    selectedExerciseId: String?,
    progress: ExerciseProgress?,
    onSelectExercise: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Progress", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            exercises.take(4).forEach { exercise ->
                TextButton(onClick = { onSelectExercise(exercise.id) }) {
                    Text(
                        if (exercise.id == selectedExerciseId) {
                            exercise.name
                        } else {
                            exercise.name
                        },
                    )
                }
            }
        }
        if (selectedExerciseId == null) {
            Text("Select an exercise to see PRs and trends.")
            return
        }
        if (progress == null) {
            Text("Complete sets for an exercise to see PRs and trends.")
            return
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(progress.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text("Heaviest: ${progress.heaviestWeightKg.formatKg()} kg")
                Text("Max reps: ${progress.maxReps}")
                Text("Best est. 1RM: ${progress.bestEstimatedOneRepMaxKg.formatKg()} kg")
                Text("Best day volume: ${progress.bestWorkoutVolumeKg.formatKg()} kg")
            }
        }
        progress.trend.forEach { point ->
            Text("Day ${point.dateEpochDay}: ${point.volumeKg.formatKg()} kg - 1RM ${point.bestEstimatedOneRepMaxKg.formatKg()} kg")
        }
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
