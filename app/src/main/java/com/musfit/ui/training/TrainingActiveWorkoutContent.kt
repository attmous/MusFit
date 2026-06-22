package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingActiveWorkoutContent(
    workout: ActiveWorkoutDetail,
    exercises: List<ExerciseSummary>,
    restTimer: RestTimerState,
    onAddExercise: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Add exercise", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercises.forEach { exercise ->
                        TextButton(onClick = { onAddExercise(exercise.id) }) {
                            Text(exercise.name)
                        }
                    }
                }
            }
        }
        workout.exerciseBlocks.forEach { block ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(block.exercise.name, style = MaterialTheme.typography.titleMedium)
                            block.targetReps?.let { Text("Target reps $it") }
                        }
                        TextButton(onClick = { onAddSet(block.exercise.id) }) { Text("Add set") }
                        TextButton(onClick = { onDuplicateSet(block.exercise.id) }) { Text("Duplicate") }
                    }
                    block.sets.forEachIndexed { index, set ->
                        WorkoutSetEditorRow(
                            index = index,
                            set = set,
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = onDeleteSet,
                            onToggleSet = onToggleSet,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetEditorRow(
    index: Int,
    set: LoggedWorkoutSetDetail,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
) {
    var setType by remember(set.id) { mutableStateOf(set.setType) }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var weightKg by remember(set.id) { mutableStateOf(set.weightKg?.toString().orEmpty()) }
    var rpe by remember(set.id) { mutableStateOf(set.rpe?.toString().orEmpty()) }
    var notes by remember(set.id) { mutableStateOf(set.notes.orEmpty()) }

    LaunchedEffect(set.setType, set.reps, set.weightKg, set.rpe, set.notes) {
        setType = set.setType
        reps = set.reps?.toString().orEmpty()
        weightKg = set.weightKg?.toString().orEmpty()
        rpe = set.rpe?.toString().orEmpty()
        notes = set.notes.orEmpty()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Set ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                set.previousLabel?.let { Text("Prev $it", style = MaterialTheme.typography.bodySmall) }
                Checkbox(
                    checked = set.completed,
                    onCheckedChange = { checked -> onToggleSet(set.id, checked) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = setType,
                    onValueChange = { setType = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Reps") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Kg") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = rpe,
                    onValueChange = { rpe = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("RPE") },
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onUpdateSet(
                            set.id,
                            setType,
                            reps,
                            weightKg,
                            rpe,
                            notes,
                        )
                    },
                ) {
                    Text("Save")
                }
                TextButton(onClick = { onDeleteSet(set.id) }) {
                    Text("Delete")
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
