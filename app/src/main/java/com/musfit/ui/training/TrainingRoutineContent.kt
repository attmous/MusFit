package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineSummary

@Composable
fun TrainingRoutineContent(
    routines: List<RoutineSummary>,
    onStartRoutine: (String) -> Unit,
    onStartBlank: () -> Unit,
    onEditRoutine: (String?) -> Unit,
    onDuplicateRoutine: (String) -> Unit,
    onDeleteRoutine: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(onClick = onStartBlank, modifier = Modifier.weight(1f)) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start empty workout")
            }
            TextButton(onClick = { onEditRoutine(null) }) {
                Text("New routine")
            }
        }
        routines.forEach { routine ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(routine.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${routine.exerciseCount} exercises - ${routine.targetSetCount} sets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val actions = routineCardActions(routine.isStarter)
                    if (ROUTINE_ACTION_START in actions) {
                        Button(
                            onClick = { onStartRoutine(routine.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Start")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actions.filterNot { it == ROUTINE_ACTION_START }.forEach { action ->
                            when (action) {
                                ROUTINE_ACTION_EDIT -> {
                                    TextButton(onClick = { onEditRoutine(routine.id) }) {
                                        Text("Edit")
                                    }
                                }
                                ROUTINE_ACTION_DUPLICATE -> {
                                    TextButton(onClick = { onDuplicateRoutine(routine.id) }) {
                                        Text("Duplicate")
                                    }
                                }
                                ROUTINE_ACTION_DELETE -> {
                                    TextButton(onClick = { onDeleteRoutine(routine.id) }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun nextQuickLogExpanded(current: Boolean): Boolean = !current

internal fun routineCardActions(isStarter: Boolean): List<String> =
    if (isStarter) {
        listOf(ROUTINE_ACTION_START, ROUTINE_ACTION_DUPLICATE)
    } else {
        listOf(ROUTINE_ACTION_START, ROUTINE_ACTION_EDIT, ROUTINE_ACTION_DUPLICATE, ROUTINE_ACTION_DELETE)
    }

private const val ROUTINE_ACTION_START = "Start"
private const val ROUTINE_ACTION_EDIT = "Edit"
private const val ROUTINE_ACTION_DUPLICATE = "Duplicate"
private const val ROUTINE_ACTION_DELETE = "Delete"

@Composable
fun TrainingRoutineEditor(
    editor: RoutineEditorState,
    exercises: List<ExerciseSummary>,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddExercise: (String) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onMoveExerciseUp: (Int) -> Unit,
    onMoveExerciseDown: (Int) -> Unit,
    onTargetSetsChange: (Int, String) -> Unit,
    onTargetRepsChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDuplicate: ((String) -> Unit)? = null,
    onDelete: ((String) -> Unit)? = null,
) {
    val exerciseMap = remember(exercises) { exercises.associateBy { it.id } }
    val selectedExerciseIds = remember(editor.exercises) { editor.exercises.map { it.exerciseId }.toSet() }
    var pickerExpanded by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Text(
                text = if (editor.routineId == null) "New routine" else "Edit routine",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSave, enabled = editor.name.isNotBlank()) {
                Text("Save")
            }
        }
        OutlinedTextField(
            value = editor.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = editor.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
        )
        RoutineExercisePicker(
            exercises = exercises,
            selectedExerciseIds = selectedExerciseIds,
            query = pickerQuery,
            expanded = pickerExpanded,
            onQueryChange = { pickerQuery = it },
            onToggleExpanded = { pickerExpanded = !pickerExpanded },
            onClose = {
                pickerExpanded = false
                pickerQuery = ""
            },
            onAddExercise = { exerciseId ->
                onAddExercise(exerciseId)
                pickerExpanded = false
                pickerQuery = ""
            },
        )
        editor.exercises.forEachIndexed { index, exercise ->
            val exerciseSummary = exerciseMap[exercise.exerciseId]
            RoutineEditorExerciseCard(
                exerciseName = exerciseSummary?.name ?: "Unknown exercise",
                exerciseMeta = exerciseSummary?.let {
                    listOfNotNull(it.equipment, it.targetMuscles.takeIf(String::isNotBlank))
                        .joinToString(" - ")
                }.orEmpty(),
                targetSets = exercise.targetSets.toString(),
                targetReps = exercise.targetReps.orEmpty(),
                canMoveUp = index > 0,
                canMoveDown = index < editor.exercises.lastIndex,
                onMoveUp = { onMoveExerciseUp(index) },
                onMoveDown = { onMoveExerciseDown(index) },
                onRemove = { onRemoveExercise(index) },
                onTargetSetsChange = { onTargetSetsChange(index, it) },
                onTargetRepsChange = { onTargetRepsChange(index, it) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (editor.routineId != null && onDuplicate != null) {
                TextButton(onClick = { onDuplicate(editor.routineId) }) {
                    Text("Duplicate")
                }
            }
            if (editor.routineId != null && onDelete != null) {
                TextButton(onClick = { onDelete(editor.routineId) }) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun RoutineExercisePicker(
    exercises: List<ExerciseSummary>,
    selectedExerciseIds: Set<String>,
    query: String,
    expanded: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onClose: () -> Unit,
    onAddExercise: (String) -> Unit,
) {
    val suggestions = routineExercisePickerSuggestions(
        exercises = exercises,
        selectedExerciseIds = selectedExerciseIds,
        query = query,
        expanded = expanded,
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(MaterialTheme.shapes.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add exercise")
                }
                if (expanded) {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close exercise picker")
                    }
                }
            }
            if (expanded) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search exercises") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestions.isEmpty()) {
                    Text(
                        "No matching exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                } else {
                    suggestions.forEach { exercise ->
                        TextButton(
                            onClick = { onAddExercise(exercise.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(exercise.name, modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = listOfNotNull(
                                        exercise.equipment,
                                        exercise.targetMuscles.takeIf(String::isNotBlank),
                                    ).joinToString(" - "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineEditorExerciseCard(
    exerciseName: String,
    exerciseMeta: String,
    targetSets: String,
    targetReps: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onTargetSetsChange: (String) -> Unit,
    onTargetRepsChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                    if (exerciseMeta.isNotBlank()) {
                        Text(
                            exerciseMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(imageVector = Icons.Outlined.KeyboardArrowUp, contentDescription = "Move exercise up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = "Move exercise down")
                }
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Remove exercise")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = targetSets,
                    onValueChange = onTargetSetsChange,
                    label = { Text("Sets") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = onTargetRepsChange,
                    label = { Text("Reps") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal fun routineExercisePickerSuggestions(
    exercises: List<ExerciseSummary>,
    selectedExerciseIds: Set<String>,
    query: String,
    expanded: Boolean,
): List<ExerciseSummary> {
    if (!expanded) return emptyList()

    val trimmedQuery = query.trim()
    val available = exercises.filterNot { it.id in selectedExerciseIds }
    val filtered = if (trimmedQuery.isBlank()) {
        available
    } else {
        available.filter { exercise ->
            exercise.name.contains(trimmedQuery, ignoreCase = true) ||
                exercise.category.contains(trimmedQuery, ignoreCase = true) ||
                exercise.equipment.orEmpty().contains(trimmedQuery, ignoreCase = true) ||
                exercise.targetMuscles.contains(trimmedQuery, ignoreCase = true)
        }
    }
    return filtered.take(if (trimmedQuery.isBlank()) 3 else 6)
}
