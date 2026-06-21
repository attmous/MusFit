package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.musfit.data.repository.RoutineSummary
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
            TrainingSection.Routines -> RoutineListPreview(state.routines)
            TrainingSection.Exercises -> ExerciseListPreview(state.exercises)
            TrainingSection.History -> Text("Finish a workout to build history.")
            TrainingSection.Progress -> Text("Complete workouts to see progress.")
        }
    }
}

@Composable
private fun RoutineListPreview(routines: List<RoutineSummary>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Routines", style = MaterialTheme.typography.titleMedium)
        routines.forEach { routine ->
            ListItem(
                headlineContent = { Text(routine.name) },
                supportingContent = { Text("${routine.exerciseCount} exercises - ${routine.targetSetCount} sets") },
            )
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
