package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var exerciseSearchQuery by remember { mutableStateOf("") }
    val availableExercises = exercises.filter { candidate ->
        val query = exerciseSearchQuery.trim()
        editor.exercises.none { it.exerciseId == candidate.id } &&
            (
                query.isBlank() ||
                    candidate.name.contains(query, ignoreCase = true) ||
                    candidate.equipment.orEmpty().contains(query, ignoreCase = true) ||
                    candidate.targetMuscles.contains(query, ignoreCase = true)
                )
    }
    var addMenuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Routine", style = MaterialTheme.typography.titleLarge)
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = exerciseSearchQuery,
                onValueChange = { exerciseSearchQuery = it },
                label = { Text("Find exercise") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { addMenuExpanded = true },
                enabled = availableExercises.isNotEmpty(),
            ) {
                Text("Add exercise")
            }
            DropdownMenu(
                expanded = addMenuExpanded,
                onDismissRequest = { addMenuExpanded = false },
            ) {
                availableExercises.forEach { exercise ->
                    DropdownMenuItem(
                        text = { Text(exercise.name) },
                        onClick = {
                            addMenuExpanded = false
                            onAddExercise(exercise.id)
                        },
                    )
                }
            }
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
        editor.exercises.forEachIndexed { index, exercise ->
            val exerciseSummary = exerciseMap[exercise.exerciseId]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        exerciseSummary?.name ?: "Unknown exercise",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = exercise.targetSets.toString(),
                            onValueChange = { onTargetSetsChange(index, it) },
                            label = { Text("Sets") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = exercise.targetReps.orEmpty(),
                            onValueChange = { onTargetRepsChange(index, it) },
                            label = { Text("Reps") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onMoveExerciseUp(index) },
                            enabled = index > 0,
                        ) {
                            Text("Up")
                        }
                        TextButton(
                            onClick = { onMoveExerciseDown(index) },
                            enabled = index < editor.exercises.lastIndex,
                        ) {
                            Text("Down")
                        }
                        TextButton(onClick = { onRemoveExercise(index) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = editor.name.isNotBlank()) {
                Text("Save")
            }
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
