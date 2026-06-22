package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.ExerciseSummary
import com.musfit.ui.AppDestination
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale

@Composable
fun TrainingScreen(viewModel: TrainingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val activeWorkout = state.activeWorkout
    val accent = tabAccentFor(AppDestination.Training)

    if (state.activeWorkoutRouteOpen && activeWorkout != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MusFitTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrainingActiveWorkoutContent(
                workout = activeWorkout,
                exercises = state.exercises,
                restTimer = state.restTimer,
                onAddExercise = viewModel::addExerciseToActiveWorkout,
                onAddSet = viewModel::addWorkoutSet,
                onDuplicateSet = viewModel::duplicateLastWorkoutSet,
                onUpdateSet = viewModel::updateWorkoutSetFields,
                onDeleteSet = viewModel::deleteWorkoutSet,
                onToggleSet = viewModel::toggleWorkoutSetCompletion,
                onClose = viewModel::closeActiveWorkoutRoute,
                onFinish = viewModel::finishActiveWorkout,
                onDiscard = viewModel::discardActiveWorkout,
            )
        }
        return
    }

    if (state.activeWorkoutRouteOpen) {
        ActiveWorkoutPlaceholder(state = state, onBack = viewModel::closeActiveWorkoutRoute)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TrainingHeader(
            history = state.workoutHistory,
            accent = accent,
            onHistory = { viewModel.selectSection(TrainingSection.History) },
        )

        state.activeWorkoutSummary?.let { summary ->
            ResumeBanner(
                title = summary.title,
                subtitle = "${summary.completedSetCount} sets · ${summary.totalVolumeKg.formatKg()} kg",
                accent = accent,
                onResume = viewModel::resumeActiveWorkout,
            )
        }

        SectionChips(
            selected = state.selectedSection,
            accent = accent,
            onSelect = viewModel::selectSection,
        )

        state.message?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
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
                        onMoveExerciseUp = viewModel::moveRoutineExerciseUp,
                        onMoveExerciseDown = viewModel::moveRoutineExerciseDown,
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
                        accent = accent,
                        onStartRoutine = viewModel::startRoutine,
                        onStartBlank = viewModel::startBlankWorkout,
                        onEditRoutine = viewModel::openRoutineEditor,
                        onDuplicateRoutine = viewModel::duplicateRoutine,
                        onDeleteRoutine = viewModel::deleteRoutine,
                    )
                }
            TrainingSection.Exercises ->
                TrainingExerciseLibraryContent(
                    visibleExercises = state.visibleExercises,
                    allExercises = state.exercises,
                    searchQuery = state.exerciseSearchQuery,
                    muscleFilter = state.exerciseMuscleFilter,
                    equipmentFilter = state.exerciseEquipmentFilter,
                    editor = state.exerciseEditor,
                    accent = accent,
                    onSearchChange = viewModel::onExerciseSearchQueryChanged,
                    onMuscleFilterChange = viewModel::onExerciseMuscleFilterChanged,
                    onEquipmentFilterChange = viewModel::onExerciseEquipmentFilterChanged,
                    onClearFilters = viewModel::clearExerciseFilters,
                    onOpenCustomExercise = viewModel::openCustomExerciseEditor,
                    onCloseCustomExercise = viewModel::closeCustomExerciseEditor,
                    onCustomExerciseNameChange = viewModel::onCustomExerciseNameChanged,
                    onCustomExerciseCategoryChange = viewModel::onCustomExerciseCategoryChanged,
                    onCustomExerciseEquipmentChange = viewModel::onCustomExerciseEquipmentChanged,
                    onCustomExerciseTargetMusclesChange = viewModel::onCustomExerciseTargetMusclesChanged,
                    onSaveCustomExercise = viewModel::saveCustomExercise,
                )
            TrainingSection.History ->
                TrainingHistoryContent(
                    history = state.workoutHistory,
                    selectedDetail = state.selectedWorkoutDetail,
                    accent = accent,
                    onOpenDetail = viewModel::openWorkoutDetail,
                    onCloseDetail = viewModel::closeWorkoutDetail,
                )
            TrainingSection.Progress ->
                TrainingProgressContent(
                    exercises = state.exercises,
                    selectedExerciseId = state.selectedProgressExerciseId,
                    progress = state.selectedExerciseProgress,
                    accent = accent,
                    onSelectExercise = viewModel::selectProgressExercise,
                )
        }
    }
}

@Composable
private fun TrainingHeader(
    history: List<com.musfit.data.repository.WorkoutHistorySummary>,
    accent: TabAccent,
    onHistory: () -> Unit,
) {
    val weekAgo = System.currentTimeMillis() - 7L * 86_400_000L
    val thisWeek = history.filter { it.startedAtEpochMillis >= weekAgo }
    val subtitle =
        if (thisWeek.isEmpty()) {
            "No workouts yet this week"
        } else {
            "This week · ${thisWeek.size} ${if (thisWeek.size == 1) "workout" else "workouts"} · ${thisWeek.sumOf { it.totalVolumeKg }.formatKg()} kg"
        }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Training", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        }
        IconButton(onClick = onHistory) {
            Icon(Icons.Outlined.History, contentDescription = "Workout history", tint = accent.color)
        }
    }
}

@Composable
private fun ResumeBanner(
    title: String,
    subtitle: String,
    accent: TabAccent,
    onResume: () -> Unit,
) {
    Surface(color = accent.container, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.color), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = accent.onColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accent.onContainer)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = accent.onContainer)
            }
            Button(onClick = onResume, colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor)) {
                Text("Resume")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionChips(
    selected: TrainingSection,
    accent: TabAccent,
    onSelect: (TrainingSection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrainingSection.entries.forEach { section ->
            val isSelected = section == selected
            Surface(
                onClick = { onSelect(section) },
                color = if (isSelected) accent.container else MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.large,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.5.dp, MusFitTheme.colors.outline),
            ) {
                Text(
                    text = section.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveWorkoutPlaceholder(state: TrainingUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MusFitTheme.colors.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Active workout", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
        Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.activeWorkoutSummary?.title ?: "Workout in progress", style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                TextButton(onClick = onBack) { Text("Back to routines") }
            }
        }
    }
}

@Composable
private fun TrainingExerciseLibraryContent(
    visibleExercises: List<ExerciseSummary>,
    allExercises: List<ExerciseSummary>,
    searchQuery: String,
    muscleFilter: String?,
    equipmentFilter: String?,
    editor: ExerciseEditorState,
    accent: TabAccent,
    onSearchChange: (String) -> Unit,
    onMuscleFilterChange: (String?) -> Unit,
    onEquipmentFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenCustomExercise: () -> Unit,
    onCloseCustomExercise: () -> Unit,
    onCustomExerciseNameChange: (String) -> Unit,
    onCustomExerciseCategoryChange: (String) -> Unit,
    onCustomExerciseEquipmentChange: (String) -> Unit,
    onCustomExerciseTargetMusclesChange: (String) -> Unit,
    onSaveCustomExercise: () -> Unit,
) {
    val equipmentOptions = allExercises.mapNotNull { it.equipment?.takeIf(String::isNotBlank) }.distinct().sorted()
    val muscleOptions = allExercises.flatMap { it.targetMuscles.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = searchQuery, onValueChange = onSearchChange, label = { Text("Search exercises") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
            Button(onClick = onOpenCustomExercise, colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor)) { Text("Custom") }
        }
        FilterChipRow(title = "Equipment", options = equipmentOptions, selected = equipmentFilter, accent = accent, onSelected = onEquipmentFilterChange)
        FilterChipRow(title = "Muscle", options = muscleOptions.take(8), selected = muscleFilter, accent = accent, onSelected = onMuscleFilterChange)
        if (searchQuery.isNotBlank() || equipmentFilter != null || muscleFilter != null) {
            TextButton(onClick = onClearFilters) { Text("Clear filters", color = accent.color) }
        }
        if (editor.isOpen) {
            Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Custom exercise", style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                    OutlinedTextField(value = editor.name, onValueChange = onCustomExerciseNameChange, label = { Text("Name") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editor.category, onValueChange = onCustomExerciseCategoryChange, label = { Text("Category") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = editor.equipment, onValueChange = onCustomExerciseEquipmentChange, label = { Text("Equipment") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = editor.targetMuscles, onValueChange = onCustomExerciseTargetMusclesChange, label = { Text("Target muscles") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveCustomExercise, enabled = editor.name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor)) { Text("Save") }
                        TextButton(onClick = onCloseCustomExercise) { Text("Cancel") }
                    }
                }
            }
        }
        Text(text = "${visibleExercises.size} exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MusFitTheme.colors.onSurface)
        if (visibleExercises.isEmpty()) {
            Text(text = "No exercises match these filters.", style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        }
        visibleExercises.forEach { exercise ->
            Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                    Text(
                        text = listOfNotNull(exercise.equipment, exercise.targetMuscles.takeIf(String::isNotBlank), if (exercise.isCustom) "Custom" else "Library").joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    title: String,
    options: List<String>,
    selected: String?,
    accent: TabAccent,
    onSelected: (String?) -> Unit,
) {
    if (options.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MusFitTheme.colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = selected.equals(option, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(if (isSelected) null else option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.container, selectedLabelColor = accent.onContainer),
                )
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
