package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.ExerciseSummary
import java.util.Locale

@Composable
fun TrainingScreen(viewModel: TrainingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val activeWorkout = state.activeWorkout

    if (state.activeWorkoutRouteOpen && activeWorkout != null) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrainingActiveWorkoutContent(
                workout = activeWorkout,
                restTimer = state.restTimer,
                onToggleSet = viewModel::toggleWorkoutSetCompletion,
                onFinish = viewModel::finishActiveWorkout,
                onDiscard = viewModel::discardActiveWorkout,
            )
        }
        return
    }

    if (state.activeWorkoutRouteOpen) {
        ActiveWorkoutPlaceholder(
            state = state,
            onBack = viewModel::closeActiveWorkoutRoute,
        )
        return
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Training", style = MaterialTheme.typography.headlineSmall)
        state.activeWorkoutSummary?.let { summary ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = summary.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = "${summary.completedSetCount} sets completed")
                    Text(text = "Volume: ${summary.totalVolumeKg.formatKg()} kg")
                    TextButton(onClick = viewModel::resumeActiveWorkout) {
                        Text("Resume workout")
                    }
                }
            }
        }
        QuickSetLoggerCard(
            state = state,
            onExerciseChanged = viewModel::onExerciseChanged,
            onRepsChanged = viewModel::onRepsChanged,
            onWeightChanged = viewModel::onWeightChanged,
            onAddSet = viewModel::addSet,
            onToggleSetCompletion = viewModel::toggleSetCompletion,
        )
        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TrainingSection.entries.forEachIndexed { index, section ->
                SegmentedButton(
                    selected = state.selectedSection == section,
                    onClick = { viewModel.selectSection(section) },
                    shape = SegmentedButtonDefaults.itemShape(index, TrainingSection.entries.size),
                ) {
                    Text(section.name)
                }
            }
        }
        when (state.selectedSection) {
            TrainingSection.Routines ->
                if (state.routineEditor.isOpen) {
                    TrainingRoutineEditor(
                        editor = state.routineEditor,
                        exercises = state.exercises,
                        onNameChange = viewModel::onRoutineNameChanged,
                        onNotesChange = viewModel::onRoutineNotesChanged,
                        onAddExercise = viewModel::addRoutineExercise,
                        onRemoveExercise = viewModel::removeRoutineExercise,
                        onTargetSetsChange = viewModel::onRoutineExerciseTargetSetsChanged,
                        onTargetRepsChange = viewModel::onRoutineExerciseTargetRepsChanged,
                        onSave = viewModel::saveRoutineEditor,
                        onCancel = viewModel::closeRoutineEditor,
                        onDuplicate = viewModel::duplicateRoutine,
                        onDelete = viewModel::deleteRoutine,
                    )
                } else {
                    TrainingRoutineContent(
                        routines = state.routines,
                        onStartRoutine = viewModel::startRoutine,
                        onStartBlank = viewModel::startBlankWorkout,
                        onEditRoutine = viewModel::openRoutineEditor,
                        onDuplicateRoutine = viewModel::duplicateRoutine,
                        onDeleteRoutine = viewModel::deleteRoutine,
                    )
                }
            TrainingSection.Exercises -> ExerciseListPreview(state.exercises)
            TrainingSection.History -> Text("Finish a workout to build history.")
            TrainingSection.Progress -> Text("Complete workouts to see progress.")
        }
    }
}

@Composable
private fun ActiveWorkoutPlaceholder(
    state: TrainingUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Active workout", style = MaterialTheme.typography.headlineSmall)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = state.activeWorkoutSummary?.title ?: "Workout in progress",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Task 2 placeholder for the active workout route.")
                state.activeWorkoutSummary?.let { summary ->
                    Text("${summary.completedSetCount} sets completed")
                }
                TextButton(onClick = onBack) {
                    Text("Back to routines")
                }
            }
        }
    }
}

@Composable
private fun QuickSetLoggerCard(
    state: TrainingUiState,
    onExerciseChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAddSet: () -> Unit,
    onToggleSetCompletion: (Int) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Quick set", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.exerciseName,
                onValueChange = onExerciseChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exercise") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.reps,
                onValueChange = onRepsChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reps") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.weightKg,
                onValueChange = onWeightChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight (kg)") },
                singleLine = true,
            )
            Button(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Log set")
            }
            Text(
                text = "Volume ${state.totalVolumeKg.formatKg()} kg | Best 1RM ${state.bestEstimatedOneRepMaxKg.formatKg()} kg",
                style = MaterialTheme.typography.bodySmall,
            )
            state.sets.forEachIndexed { index, set ->
                ListItem(
                    headlineContent = { Text("${set.exerciseName} | ${set.weightKg.formatKg()} kg x ${set.reps}") },
                    supportingContent = {
                        Text(if (set.completed) "Included in totals" else "Excluded from totals")
                    },
                    trailingContent = {
                        Checkbox(
                            checked = set.completed,
                            onCheckedChange = { onToggleSetCompletion(index) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ExerciseListPreview(exercises: List<ExerciseSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Exercises", style = MaterialTheme.typography.titleMedium)
        exercises.take(12).forEach { exercise ->
            ListItem(
                headlineContent = { Text(exercise.name) },
                supportingContent = { Text(listOfNotNull(exercise.equipment, exercise.targetMuscles).joinToString(" - ")) },
            )
        }
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
