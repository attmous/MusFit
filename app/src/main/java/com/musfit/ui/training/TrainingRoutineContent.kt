package com.musfit.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineSummary
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent

@Composable
fun TrainingRoutineContent(
    routines: List<RoutineSummary>,
    accent: TabAccent,
    onStartRoutine: (String) -> Unit,
    onStartBlank: () -> Unit,
    onEditRoutine: (String?) -> Unit,
    onDuplicateRoutine: (String) -> Unit,
    onDeleteRoutine: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onStartBlank,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  Start empty workout")
        }
        Text(
            text = "YOUR ROUTINES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        routines.forEach { routine ->
            RoutineCard(
                routine = routine,
                accent = accent,
                onStart = onStartRoutine,
                onEdit = onEditRoutine,
                onDuplicate = onDuplicateRoutine,
                onDelete = onDeleteRoutine,
            )
        }
        OutlinedButton(onClick = { onEditRoutine(null) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = accent.color)
            Text("  New routine", color = accent.color)
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineSummary,
    accent: TabAccent,
    onStart: (String) -> Unit,
    onEdit: (String?) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
                    Text(
                        text = "${routine.exerciseCount} exercises · ${routine.targetSetCount} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Routine options", tint = MusFitTheme.colors.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (!routine.isStarter) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit(routine.id) })
                        }
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate(routine.id) })
                        if (!routine.isStarter) {
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete(routine.id) })
                        }
                    }
                }
            }
            Button(
                onClick = { onStart(routine.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text("  Start")
            }
        }
    }
}

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
        Text("Routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
        OutlinedTextField(value = editor.name, onValueChange = onNameChange, label = { Text("Name") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = editor.notes, onValueChange = onNotesChange, label = { Text("Notes") }, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = exerciseSearchQuery, onValueChange = { exerciseSearchQuery = it }, label = { Text("Find exercise") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
            Box {
                Button(onClick = { addMenuExpanded = true }, enabled = availableExercises.isNotEmpty()) { Text("Add") }
                DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                    availableExercises.forEach { exercise ->
                        DropdownMenuItem(text = { Text(exercise.name) }, onClick = { addMenuExpanded = false; onAddExercise(exercise.id) })
                    }
                }
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        editor.exercises.forEachIndexed { index, exercise ->
            val exerciseSummary = exerciseMap[exercise.exerciseId]
            Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(exerciseSummary?.name ?: "Unknown exercise", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MusFitTheme.colors.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = exercise.targetSets.toString(), onValueChange = { onTargetSetsChange(index, it) }, label = { Text("Sets") }, singleLine = true, shape = MusFitTheme.shapes.small, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = exercise.targetReps.orEmpty(), onValueChange = { onTargetRepsChange(index, it) }, label = { Text("Reps") }, singleLine = true, shape = MusFitTheme.shapes.small, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onMoveExerciseUp(index) }, enabled = index > 0) { Text("Up") }
                        TextButton(onClick = { onMoveExerciseDown(index) }, enabled = index < editor.exercises.lastIndex) { Text("Down") }
                        TextButton(onClick = { onRemoveExercise(index) }) { Text("Remove") }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = editor.name.isNotBlank()) { Text("Save") }
            if (editor.routineId != null && onDuplicate != null) {
                TextButton(onClick = { onDuplicate(editor.routineId) }) { Text("Duplicate") }
            }
            if (editor.routineId != null && onDelete != null) {
                TextButton(onClick = { onDelete(editor.routineId) }) { Text("Delete") }
            }
        }
    }
}
