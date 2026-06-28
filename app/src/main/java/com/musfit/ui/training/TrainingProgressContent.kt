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
import com.musfit.data.repository.TrainingProgressAnalytics
import com.musfit.domain.model.ExerciseProgress
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import java.util.Locale

@Composable
fun TrainingProgressContent(
    exercises: List<ExerciseSummary>,
    selectedExerciseId: String?,
    progress: ExerciseProgress?,
    analytics: TrainingProgressAnalytics,
    accent: TabAccent,
    onSelectExercise: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProgressAnalyticsOverview(analytics = analytics, accent = accent)
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
        SelectedExerciseDeepDive(progress = progress)
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

@Composable
private fun ProgressAnalyticsOverview(
    analytics: TrainingProgressAnalytics,
    accent: TabAccent,
) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Training analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (analytics.muscleGroups.isEmpty() && analytics.weeklyVolume.isEmpty()) {
                Text("Complete workouts to build muscle and weekly volume analytics.", color = MusFitTheme.colors.onSurfaceVariant)
                return@Column
            }
            if (analytics.muscleGroups.isNotEmpty()) {
                Text("Muscle volume", style = MaterialTheme.typography.labelLarge, color = accent.color)
                analytics.muscleGroups.take(4).forEach { muscle ->
                    Text(
                        "${muscle.muscle.replaceFirstChar { it.uppercase() }} · ${muscle.completedSetCount} sets · ${muscle.totalVolumeKg.formatKg()} kg",
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            if (analytics.weeklyVolume.isNotEmpty()) {
                Text("Weekly volume", style = MaterialTheme.typography.labelLarge, color = accent.color)
                analytics.weeklyVolume.takeLast(4).forEach { week ->
                    Text(
                        "Week ${week.weekStartEpochDay} · ${week.workoutCount} workouts · ${week.completedSetCount} sets · ${week.totalVolumeKg.formatKg()} kg",
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedExerciseDeepDive(progress: ExerciseProgress) {
    if (progress.history.isEmpty() && progress.bestSets.isEmpty() && progress.prTimeline.isEmpty()) return
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Exercise history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            progress.bestSets.firstOrNull()?.let { best ->
                Text(
                    "Best set · ${best.weightKg.formatKg()} kg x ${best.reps} · est. ${best.estimatedOneRepMaxKg.formatKg()} kg",
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            progress.history.takeLast(4).forEach { entry ->
                Text(
                    "Day ${entry.dateEpochDay} · ${entry.completedSetCount} sets · ${entry.volumeKg.formatKg()} kg · ${entry.bestSetLabel}",
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            if (progress.prTimeline.isNotEmpty()) {
                Text("PR timeline", style = MaterialTheme.typography.labelLarge)
                progress.prTimeline.takeLast(4).forEach { pr ->
                    Text(
                        "Day ${pr.dateEpochDay} · ${pr.weightKg.formatKg()} kg x ${pr.reps} · est. ${pr.estimatedOneRepMaxKg.formatKg()} kg",
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
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
