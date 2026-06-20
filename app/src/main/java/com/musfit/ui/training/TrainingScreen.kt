package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.LoggedWorkoutSet
import java.util.Locale

@Composable
fun TrainingScreen(viewModel: TrainingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Training",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = state.exerciseName,
            onValueChange = viewModel::onExerciseChanged,
            label = { Text("Exercise") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.reps,
                onValueChange = viewModel::onRepsChanged,
                label = { Text("Reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = state.weightKg,
                onValueChange = viewModel::onWeightChanged,
                label = { Text("Weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        TextButton(
            onClick = viewModel::addSet,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add set")
        }

        Text(
            text = "Session summary",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Volume: ${state.totalVolumeKg.formatKg()} kg",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Best est. 1RM: ${state.bestEstimatedOneRepMaxKg.formatKg()} kg",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.sets.isEmpty()) {
            Text(
                text = "Add your first working set to start the session.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = "Sets",
                style = MaterialTheme.typography.titleMedium,
            )

            state.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(
                        checked = set.completed,
                        onCheckedChange = { viewModel.toggleSetCompletion(index) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = set.displayExerciseLabel(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Set ${index + 1}: ${set.reps} reps x ${set.weightKg.formatKg()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                        )
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

private fun LoggedWorkoutSet.displayExerciseLabel(): String =
    exerciseName.takeUnless { it.isBlank() } ?: "Custom exercise"
