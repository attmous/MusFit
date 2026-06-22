package com.musfit.ui.training

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
import com.musfit.domain.model.ExerciseProgress
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.util.Locale

@Composable
fun TrainingProgressContent(
    exercises: List<ExerciseSummary>,
    selectedExerciseId: String?,
    progress: ExerciseProgress?,
    accent: TabAccent,
    onSelectExercise: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            exercises.forEach { exercise ->
                FilterChip(
                    selected = exercise.id == selectedExerciseId,
                    onClick = { onSelectExercise(exercise.id) },
                    label = { Text(exercise.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent.container,
                        selectedLabelColor = accent.onContainer,
                    ),
                )
            }
        }
        if (selectedExerciseId == null) {
            Text("Select an exercise to see PRs and trends.", color = MusFitTheme.colors.onSurfaceVariant)
            return
        }
        if (progress == null) {
            Text("Complete sets for an exercise to see PRs and trends.", color = MusFitTheme.colors.onSurfaceVariant)
            return
        }
        Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(progress.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
                Text("Heaviest · ${progress.heaviestWeightKg.formatKg()} kg", color = MusFitTheme.colors.onSurfaceVariant)
                Text("Max reps · ${progress.maxReps}", color = MusFitTheme.colors.onSurfaceVariant)
                Text("Best est. 1RM · ${progress.bestEstimatedOneRepMaxKg.formatKg()} kg", color = MusFitTheme.colors.onSurfaceVariant)
                Text("Best day volume · ${progress.bestWorkoutVolumeKg.formatKg()} kg", color = MusFitTheme.colors.onSurfaceVariant)
            }
        }
        if (progress.trend.isEmpty()) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            var metric by rememberSaveable { mutableStateOf(TrendMetric.EstOneRepMax) }
            TrendMetricToggle(selected = metric, accent = accent, onSelect = { metric = it })
            ExerciseTrendChart(
                trend = progress.trend,
                metric = metric,
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
            )
        }
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
