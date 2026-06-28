package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

@Composable
fun TrainingRoutineContent(
    routines: List<RoutineSummary>,
    programOptions: List<String> = emptyList(),
    selectedProgram: String? = null,
    accent: TabAccent,
    onProgramSelected: (String?) -> Unit = {},
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
            Button(
                onClick = onStartBlank,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start empty workout")
            }
            TextButton(onClick = { onEditRoutine(null) }) {
                Text("New routine", color = accent.color)
            }
        }
        if (programOptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoutineProgramChip(
                    label = "All",
                    selected = selectedProgram == null,
                    accent = accent,
                    onClick = { onProgramSelected(null) },
                )
                programOptions.forEach { program ->
                    RoutineProgramChip(
                        label = program,
                        selected = selectedProgram == program,
                        accent = accent,
                        onClick = { onProgramSelected(program) },
                    )
                }
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
                            val meta = listOfNotNull(
                                routine.programName,
                                routine.tags.take(2).joinToString(", ").takeIf { it.isNotBlank() },
                            ).joinToString(" - ")
                            if (meta.isNotBlank()) {
                                Text(
                                    meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    val actions = routineCardActions(routine.isStarter)
                    if (ROUTINE_ACTION_START in actions) {
                        Button(
                            onClick = { onStartRoutine(routine.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                        ) {
                            Text("Start")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actions.filterNot { it == ROUTINE_ACTION_START }.forEach { action ->
                            when (action) {
                                ROUTINE_ACTION_EDIT -> {
                                    TextButton(
                                        onClick = { onEditRoutine(routine.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                                    ) {
                                        Text("Edit")
                                    }
                                }
                                ROUTINE_ACTION_DUPLICATE -> {
                                    TextButton(
                                        onClick = { onDuplicateRoutine(routine.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                                    ) {
                                        Text("Duplicate")
                                    }
                                }
                                ROUTINE_ACTION_DELETE -> {
                                    TextButton(
                                        onClick = { onDeleteRoutine(routine.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    ) {
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

@Composable
private fun RoutineProgramChip(
    label: String,
    selected: Boolean,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.container else MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(0.5.dp, MusFitTheme.colors.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
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

/** Result of validating a routine target field for display (storage still coerces independently). */
sealed interface TargetFieldResult {
    object Valid : TargetFieldResult
    data class Invalid(val message: String) : TargetFieldResult
}

internal fun validateTargetSets(raw: String): TargetFieldResult {
    val sets = raw.trim().toIntOrNull()
    return if (sets != null && sets in 1..20) {
        TargetFieldResult.Valid
    } else {
        TargetFieldResult.Invalid("Enter 1-20 sets")
    }
}

internal fun validateTargetReps(raw: String): TargetFieldResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return TargetFieldResult.Valid
    val invalid = TargetFieldResult.Invalid("Use a number or range, e.g. 8 or 8-12")
    if (trimmed.contains('-')) {
        val parts = trimmed.split('-').map { it.trim() }
        if (parts.size != 2) return invalid
        val low = parts[0].toIntOrNull() ?: return invalid
        val high = parts[1].toIntOrNull() ?: return invalid
        return if (low in 1..100 && high in 1..100 && low <= high) TargetFieldResult.Valid else invalid
    }
    val single = trimmed.toIntOrNull() ?: return invalid
    return if (single in 1..100) TargetFieldResult.Valid else invalid
}

internal fun routineEditorCanSave(name: String, exercises: List<RoutineExerciseInput>): Boolean {
    if (name.isBlank()) return false
    if (exercises.isEmpty()) return false
    return exercises.all { exercise ->
        validateTargetSets(exercise.targetSets.toString()) is TargetFieldResult.Valid &&
            validateTargetReps(exercise.targetReps.orEmpty()) is TargetFieldResult.Valid
    }
}

@Composable
fun TrainingRoutineEditor(
    editor: RoutineEditorState,
    exercises: List<ExerciseSummary>,
    accent: TabAccent,
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
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent.color,
        focusedLabelColor = accent.color,
        cursorColor = accent.color,
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                Text("Cancel")
            }
            Text(
                text = if (editor.routineId == null) "New routine" else "Edit routine",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSave,
                enabled = routineEditorCanSave(editor.name, editor.exercises),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text("Save")
            }
        }
        OutlinedTextField(
            value = editor.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = editor.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        RoutineExercisePicker(
            exercises = exercises,
            selectedExerciseIds = selectedExerciseIds,
            accent = accent,
            selectedCount = editor.exercises.size,
            fieldColors = fieldColors,
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
                // Keep the picker open while bulk-adding from a search; collapse only on a blank query.
                if (pickerQuery.isBlank()) {
                    pickerExpanded = false
                }
            },
        )
        if (editor.exercises.isEmpty()) {
            Text(
                text = "Add at least one exercise to start this routine",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
        editor.exercises.forEachIndexed { index, exercise ->
            val exerciseSummary = exerciseMap[exercise.exerciseId]
            val setsError = (validateTargetSets(exercise.targetSets.toString()) as? TargetFieldResult.Invalid)?.message
            val repsError = (validateTargetReps(exercise.targetReps.orEmpty()) as? TargetFieldResult.Invalid)?.message
            RoutineEditorExerciseCard(
                position = index + 1,
                accent = accent,
                fieldColors = fieldColors,
                setsError = setsError,
                repsError = repsError,
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
                TextButton(
                    onClick = { onDuplicate(editor.routineId) },
                    colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                ) {
                    Text("Duplicate")
                }
            }
            if (editor.routineId != null && onDelete != null) {
                TextButton(
                    onClick = { onDelete(editor.routineId) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
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
    accent: TabAccent,
    selectedCount: Int,
    fieldColors: androidx.compose.material3.TextFieldColors,
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
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(MusFitTheme.shapes.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onToggleExpanded,
                    colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedCount > 0) "Add exercise ($selectedCount added)" else "Add exercise")
                }
                if (expanded) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close exercise picker",
                            tint = MusFitTheme.colors.onSurfaceVariant,
                        )
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
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestions.isEmpty()) {
                    Text(
                        "No matching exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                } else {
                    suggestions.forEach { exercise ->
                        TextButton(
                            onClick = { onAddExercise(exercise.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = accent.color),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(exercise.name, color = MusFitTheme.colors.onSurface, modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = listOfNotNull(
                                        exercise.equipment,
                                        exercise.targetMuscles.takeIf(String::isNotBlank),
                                    ).joinToString(" - "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MusFitTheme.colors.onSurfaceVariant,
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
    position: Int,
    accent: TabAccent,
    fieldColors: androidx.compose.material3.TextFieldColors,
    setsError: String?,
    repsError: String?,
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
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        position.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onContainer,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall, color = MusFitTheme.colors.onSurface)
                    if (exerciseMeta.isNotBlank()) {
                        Text(
                            exerciseMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move exercise up",
                        tint = if (canMoveUp) accent.color else MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move exercise down",
                        tint = if (canMoveDown) accent.color else MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.error,
                    )
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
                    isError = setsError != null,
                    colors = fieldColors,
                    supportingText = setsError?.let { message -> { Text(message) } },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = onTargetRepsChange,
                    label = { Text("Reps") },
                    singleLine = true,
                    isError = repsError != null,
                    colors = fieldColors,
                    supportingText = repsError?.let { message -> { Text(message) } },
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
