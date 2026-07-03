package com.musfit.ui.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.domain.training.RoutineDisplayCalculator
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
    onEditRoutine: (String?) -> Unit,
    onOpenRoutineDetail: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { onEditRoutine(null) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New routine")
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
        if (routines.isNotEmpty()) {
            groupRoutineSummariesByProgram(routines).forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Surface(
                        color = MusFitTheme.colors.surface,
                        shape = MusFitTheme.shapes.large,
                        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            group.routines.forEachIndexed { index, routine ->
                                RoutineRow(
                                    routine = routine,
                                    accent = accent,
                                    onOpenDetail = { onOpenRoutineDetail(routine.id) },
                                    onStart = { onStartRoutine(routine.id) },
                                )
                                if (index < group.routines.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MusFitTheme.colors.outline,
                                        modifier = Modifier.padding(start = 61.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dense, tappable routine row: tinted leading icon, name + meta + muscle chips, and a compact tonal
 * start button. Tapping the row body opens the routine detail; the primary CTA on the screen is
 * routine creation, so every per-routine start remains compact.
 */
@Composable
private fun RoutineRow(
    routine: RoutineSummary,
    accent: TabAccent,
    onOpenDetail: () -> Unit,
    onStart: () -> Unit,
) {
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(routine.targetSetCount)
    val meta = buildString {
        append("${routine.exerciseCount} exercises · ${routine.targetSetCount} sets")
        if (estimatedMinutes > 0) append(" · ~$estimatedMinutes min")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = accent.onContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                routine.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
            if (routine.muscleGroups.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    routine.muscleGroups.forEach { muscle -> RoutineMuscleChip(muscle) }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.container)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Start ${routine.name}",
                tint = accent.onContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Read-only preview of a routine opened from a row tap: header with a primary Start, summary meta +
 * muscle chips, the exercise list with planned sets x reps, and secondary actions (edit/duplicate/
 * delete, gated the same way as the row overflow used to be).
 */
@Composable
fun RoutineDetailContent(
    detail: RoutineDetail,
    accent: TabAccent,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onOpenExercise: (exerciseId: String, target: String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val totalSets = detail.exercises.sumOf { it.targetSets }
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(totalSets)
    val muscles = RoutineDisplayCalculator.topMuscles(
        detail.exercises.joinToString(",") { it.exercise.primaryMuscles },
        limit = 4,
    )
    val actions = routineCardActions(detail.isStarter)
    val meta = buildString {
        append("${detail.exercises.size} exercises · $totalSets sets")
        if (estimatedMinutes > 0) append(" · ~$estimatedMinutes min")
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                Text("Back")
            }
            Text(
                text = detail.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start")
            }
        }
        Text(meta, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        val program = detail.programName?.takeIf { it.isNotBlank() }
        if (program != null || muscles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                program?.let { RoutineProgramTag(label = it, accent = accent) }
                muscles.forEach { muscle -> RoutineMuscleChip(muscle) }
            }
        }
        if (detail.notes?.isNotBlank() == true) {
            Text(detail.notes, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
        if (detail.exercises.isEmpty()) {
            Text(
                "This routine has no exercises yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        } else {
            Surface(
                color = MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.large,
                border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    detail.exercises.forEachIndexed { index, exercise ->
                        RoutineDetailExerciseRow(
                            position = index + 1,
                            exercise = exercise,
                            accent = accent,
                            onOpen = { target -> onOpenExercise(exercise.exercise.id, target) },
                        )
                        if (index < detail.exercises.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MusFitTheme.colors.outline,
                                modifier = Modifier.padding(start = 66.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ROUTINE_ACTION_EDIT in actions) {
                TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                    Text("Edit")
                }
            }
            if (ROUTINE_ACTION_DUPLICATE in actions) {
                TextButton(onClick = onDuplicate, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                    Text("Duplicate")
                }
            }
            if (ROUTINE_ACTION_DELETE in actions) {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun RoutineDetailExerciseRow(
    position: Int,
    exercise: com.musfit.data.repository.RoutineExerciseDetail,
    accent: TabAccent,
    onOpen: (target: String) -> Unit,
) {
    val target = exercise.targetReps?.takeIf { it.isNotBlank() }
        ?.let { "${exercise.targetSets} × $it" }
        ?: "${exercise.targetSets} sets"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(target) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val imageUrl = exercise.exercise.imageUrl
        if (imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
        } else {
            ExerciseThumb(
                imageUrl = imageUrl,
                contentDescription = exercise.exercise.name,
                accent = accent,
                size = 44.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.exercise.name,
                style = MaterialTheme.typography.titleSmall,
                color = MusFitTheme.colors.onSurface,
            )
            val subtitle = listOfNotNull(
                exercise.exercise.equipment,
                exercise.exercise.targetMuscles.takeIf(String::isNotBlank),
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
            }
        }
        Text(target, style = MaterialTheme.typography.labelLarge, color = accent.color)
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RoutineProgramTag(label: String, accent: TabAccent) {
    Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent.onContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun RoutineMuscleChip(label: String) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
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

internal data class RoutineGroup(
    val title: String,
    val routines: List<RoutineSummary>,
)

internal fun groupRoutineSummariesByProgram(routines: List<RoutineSummary>): List<RoutineGroup> {
    val groups = linkedMapOf<String, MutableList<RoutineSummary>>()
    routines.forEach { routine ->
        val title = routine.programName
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: "My routines"
        groups.getOrPut(title) { mutableListOf() } += routine
    }
    return groups.map { (title, groupedRoutines) ->
        RoutineGroup(title = title, routines = groupedRoutines)
    }
}

internal fun routineCardActions(isStarter: Boolean): List<String> =
    if (isStarter) {
        listOf(ROUTINE_ACTION_START, ROUTINE_ACTION_EDIT, ROUTINE_ACTION_DUPLICATE)
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
    var pickerQuery by remember { mutableStateOf("") }
    var pickerEquipmentFilter by remember { mutableStateOf<String?>(null) }
    var pickerMuscleFilter by remember { mutableStateOf<String?>(null) }
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
            equipmentFilter = pickerEquipmentFilter,
            muscleFilter = pickerMuscleFilter,
            onQueryChange = { pickerQuery = it },
            onEquipmentFilterChange = { selected ->
                pickerEquipmentFilter = if (pickerEquipmentFilter == selected) null else selected
            },
            onMuscleFilterChange = { selected ->
                pickerMuscleFilter = if (pickerMuscleFilter == selected) null else selected
            },
            onClearFilters = {
                pickerQuery = ""
                pickerEquipmentFilter = null
                pickerMuscleFilter = null
            },
            onAddExercise = { exerciseId ->
                onAddExercise(exerciseId)
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
            if (editor.routineId != null && !editor.isStarter && onDelete != null) {
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
    equipmentFilter: String?,
    muscleFilter: String?,
    onQueryChange: (String) -> Unit,
    onEquipmentFilterChange: (String) -> Unit,
    onMuscleFilterChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onAddExercise: (String) -> Unit,
) {
    val options = routineExercisePickerOptions(exercises)
    val suggestions = routineExercisePickerSuggestions(
        exercises = exercises,
        selectedExerciseIds = selectedExerciseIds,
        query = query,
        equipmentFilter = equipmentFilter,
        muscleFilter = muscleFilter,
    )
    val filtersActive = query.isNotBlank() || equipmentFilter != null || muscleFilter != null

    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saved exercises",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MusFitTheme.colors.onSurface,
                    )
                    Text(
                        text = if (selectedCount > 0) "$selectedCount added to this routine" else "Choose from your exercise library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                if (filtersActive) {
                    TextButton(onClick = onClearFilters, colors = ButtonDefaults.textButtonColors(contentColor = accent.color)) {
                        Text("Clear")
                    }
                }
            }
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
            RoutinePickerChipRow(
                title = "Equipment",
                options = options.equipment.take(8),
                selected = equipmentFilter,
                accent = accent,
                onSelected = onEquipmentFilterChange,
            )
            RoutinePickerChipRow(
                title = "Muscle",
                options = options.muscles.take(10),
                selected = muscleFilter,
                accent = accent,
                onSelected = onMuscleFilterChange,
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
                    Surface(
                        onClick = { onAddExercise(exercise.id) },
                        color = MusFitTheme.colors.background,
                        shape = MusFitTheme.shapes.medium,
                        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    exercise.name,
                                    color = MusFitTheme.colors.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                val meta = listOfNotNull(
                                    exercise.equipment,
                                    exercise.targetMuscles.takeIf(String::isNotBlank),
                                ).joinToString(" - ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MusFitTheme.colors.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add ${exercise.name}",
                                tint = accent.color,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutinePickerChipRow(
    title: String,
    options: List<String>,
    selected: String?,
    accent: TabAccent,
    onSelected: (String) -> Unit,
) {
    if (options.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp),
        )
        options.forEach { option ->
            RoutinePickerChip(
                label = option,
                selected = selected.equals(option, ignoreCase = true),
                accent = accent,
                onClick = { onSelected(option) },
            )
        }
    }
}

@Composable
private fun RoutinePickerChip(
    label: String,
    selected: Boolean,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.container else MusFitTheme.colors.background,
        shape = RoundedCornerShape(999.dp),
        border = if (selected) null else BorderStroke(0.5.dp, MusFitTheme.colors.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent.onContainer else MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
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

internal data class RoutineExercisePickerOptions(
    val equipment: List<String>,
    val muscles: List<String>,
)

internal fun routineExercisePickerOptions(exercises: List<ExerciseSummary>): RoutineExercisePickerOptions =
    RoutineExercisePickerOptions(
        equipment = exercises.mapNotNull { it.equipment?.trim()?.takeIf(String::isNotBlank) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() },
        muscles = exercises.flatMap { it.pickerMuscles() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() },
    )

internal fun routineExercisePickerSuggestions(
    exercises: List<ExerciseSummary>,
    selectedExerciseIds: Set<String>,
    query: String,
    equipmentFilter: String? = null,
    muscleFilter: String? = null,
    limit: Int = 8,
): List<ExerciseSummary> {
    val trimmedQuery = query.trim()
    val available = exercises.filterNot { it.id in selectedExerciseIds }
    val filtered = available.filter { exercise ->
        val matchesQuery = trimmedQuery.isBlank() ||
            exercise.name.contains(trimmedQuery, ignoreCase = true) ||
                exercise.category.contains(trimmedQuery, ignoreCase = true) ||
                exercise.equipment.orEmpty().contains(trimmedQuery, ignoreCase = true) ||
                exercise.targetMuscles.contains(trimmedQuery, ignoreCase = true) ||
                exercise.primaryMuscles.contains(trimmedQuery, ignoreCase = true) ||
                exercise.secondaryMuscles.contains(trimmedQuery, ignoreCase = true)
        val matchesEquipment = equipmentFilter.isNullOrBlank() ||
            exercise.equipment.equals(equipmentFilter, ignoreCase = true)
        val matchesMuscle = muscleFilter.isNullOrBlank() ||
            exercise.pickerMuscles().any { it.equals(muscleFilter, ignoreCase = true) }

        matchesQuery && matchesEquipment && matchesMuscle
    }
    return filtered.take(limit)
}

private fun ExerciseSummary.pickerMuscles(): List<String> =
    listOf(targetMuscles, primaryMuscles, secondaryMuscles)
        .flatMap { raw -> raw.split(',', '/', ';') }
        .map { it.trim() }
        .filter { it.isNotBlank() }
