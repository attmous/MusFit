package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseSummary
import java.util.Locale

@Composable
fun TrainingScreen(viewModel: TrainingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val activeWorkout = state.activeWorkout
    var quickLogExpanded by remember { mutableStateOf(false) }

    if (state.activeWorkoutRouteOpen && activeWorkout != null) {
        Column(
            modifier = Modifier
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
            ActiveWorkoutResumeBanner(
                summary = summary,
                onResume = viewModel::resumeActiveWorkout,
            )
        }
        QuickLogDisclosure(
            expanded = quickLogExpanded,
            onToggle = { quickLogExpanded = nextQuickLogExpanded(quickLogExpanded) },
        )
        if (quickLogExpanded) {
            QuickSetLoggerCard(
                state = state,
                onExerciseChanged = viewModel::onExerciseChanged,
                onRepsChanged = viewModel::onRepsChanged,
                onWeightChanged = viewModel::onWeightChanged,
                onAddSet = viewModel::addSet,
                onToggleSetCompletion = viewModel::toggleSetCompletion,
            )
        }
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
                    onOpenDetail = viewModel::openWorkoutDetail,
                    onCloseDetail = viewModel::closeWorkoutDetail,
                )
            TrainingSection.Progress ->
                TrainingProgressContent(
                    exercises = state.exercises,
                    selectedExerciseId = state.selectedProgressExerciseId,
                    progress = state.selectedExerciseProgress,
                    onSelectExercise = viewModel::selectProgressExercise,
                )
        }
    }
}

@Composable
private fun ActiveWorkoutResumeBanner(
    summary: ActiveWorkoutSummary,
    onResume: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${summary.completedSetCount} sets - ${summary.totalVolumeKg.formatKg()} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onResume) {
                Text("Resume")
            }
        }
    }
}

@Composable
private fun QuickLogDisclosure(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Outlined.FitnessCenter, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (expanded) "Hide quick log" else "Quick log")
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
private fun TrainingExerciseLibraryContent(
    visibleExercises: List<ExerciseSummary>,
    allExercises: List<ExerciseSummary>,
    searchQuery: String,
    muscleFilter: String?,
    equipmentFilter: String?,
    editor: ExerciseEditorState,
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
    val equipmentOptions = allExercises
        .mapNotNull { it.equipment?.takeIf(String::isNotBlank) }
        .distinct()
        .sorted()
    val muscleOptions = allExercises
        .flatMap { it.targetMuscles.split(",") }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onOpenCustomExercise) {
                Text("Custom")
            }
        }
        FilterChipRow(
            title = "Equipment",
            options = equipmentOptions,
            selected = equipmentFilter,
            onSelected = onEquipmentFilterChange,
        )
        FilterChipRow(
            title = "Muscle",
            options = muscleOptions.take(8),
            selected = muscleFilter,
            onSelected = onMuscleFilterChange,
        )
        if (searchQuery.isNotBlank() || equipmentFilter != null || muscleFilter != null) {
            TextButton(onClick = onClearFilters) {
                Text("Clear filters")
            }
        }
        if (editor.isOpen) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Custom exercise", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = onCustomExerciseNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editor.category,
                            onValueChange = onCustomExerciseCategoryChange,
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = editor.equipment,
                            onValueChange = onCustomExerciseEquipmentChange,
                            label = { Text("Equipment") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = editor.targetMuscles,
                        onValueChange = onCustomExerciseTargetMusclesChange,
                        label = { Text("Target muscles") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSaveCustomExercise,
                            enabled = editor.name.isNotBlank(),
                        ) {
                            Text("Save")
                        }
                        TextButton(onClick = onCloseCustomExercise) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
        Text(
            text = "${visibleExercises.size} exercises",
            style = MaterialTheme.typography.titleMedium,
        )
        if (visibleExercises.isEmpty()) {
            Text(
                text = "No exercises match these filters.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        visibleExercises.forEach { exercise ->
            ListItem(
                headlineContent = { Text(exercise.name) },
                supportingContent = {
                    Text(
                        listOfNotNull(
                            exercise.equipment,
                            exercise.targetMuscles.takeIf(String::isNotBlank),
                            if (exercise.isCustom) "Custom" else "Library",
                        ).joinToString(" - "),
                    )
                },
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    title: String,
    options: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    if (options.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected.equals(option, ignoreCase = true),
                    onClick = {
                        onSelected(
                            if (selected.equals(option, ignoreCase = true)) {
                                null
                            } else {
                                option
                            },
                        )
                    },
                    label = { Text(option) },
                )
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
