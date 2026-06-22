package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.WorkoutExerciseBlock
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TrainingActiveWorkoutContent(
    workout: ActiveWorkoutDetail,
    exercises: List<ExerciseSummary>,
    restTimer: RestTimerState,
    onTickRestTimer: () -> Unit,
    onPauseRestTimer: () -> Unit,
    onResumeRestTimer: () -> Unit,
    onSkipRestTimer: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onAddExercise: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RestTimerTicker(
            restTimer = restTimer,
            onTick = onTickRestTimer,
        )
        ActiveWorkoutHeader(
            workout = workout,
            onClose = onClose,
            onFinish = onFinish,
            onDiscard = onDiscard,
        )
        RestTimerBanner(
            restTimer = restTimer,
            onPause = onPauseRestTimer,
            onResume = onResumeRestTimer,
            onSkip = onSkipRestTimer,
            onAdjust = onAdjustRestTimer,
        )
        AddExerciseCompactBar(
            exercises = exercises,
            onAddExercise = onAddExercise,
        )
        workout.exerciseBlocks.forEach { block ->
            ActiveExerciseBlock(
                block = block,
                onAddSet = onAddSet,
                onDuplicateSet = onDuplicateSet,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSet = onToggleSet,
            )
        }
    }
}

@Composable
private fun RestTimerTicker(
    restTimer: RestTimerState,
    onTick: () -> Unit,
) {
    LaunchedEffect(restTimer.isVisible, restTimer.isRunning, restTimer.remainingSeconds) {
        if (restTimer.isVisible && restTimer.isRunning && restTimer.remainingSeconds > 0) {
            delay(1_000L)
            onTick()
        }
    }
}

@Composable
private fun AddExerciseCompactBar(
    exercises: List<ExerciseSummary>,
    onAddExercise: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val suggestions = compactExerciseSuggestions(
        exercises = exercises,
        query = query,
        expanded = expanded,
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Add exercise",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (expanded) {
                    IconButton(
                        onClick = {
                            expanded = false
                            query = ""
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close exercise search",
                        )
                    }
                }
            }

            if (expanded) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    placeholder = { Text("Search exercise") },
                )
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    suggestions.forEach { exercise ->
                        TextButton(
                            onClick = {
                                onAddExercise(exercise.id)
                                query = ""
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = exercise.name,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    if (query.isNotBlank() && suggestions.isEmpty()) {
                        Text(
                            text = "No matching exercises",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutHeader(
    workout: ActiveWorkoutDetail,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.title, style = MaterialTheme.typography.headlineSmall)
                Text("Log workout", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
            Button(onClick = onFinish) {
                Text("Finish")
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatCell(label = "Sets", value = workout.completedSetCount.toString(), modifier = Modifier.weight(1f))
                StatCell(label = "Volume", value = "${workout.totalVolumeKg.formatKg()} kg", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RestTimerBanner(
    restTimer: RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = restTimerDisplayText(restTimer),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (restTimer.isVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onAdjust(-15) }) {
                        Text("-15s")
                    }
                    IconButton(onClick = if (restTimer.isRunning) onPause else onResume) {
                        Icon(
                            imageVector = if (restTimer.isRunning) {
                                Icons.Outlined.Pause
                            } else {
                                Icons.Outlined.PlayArrow
                            },
                            contentDescription = if (restTimer.isRunning) {
                                "Pause rest timer"
                            } else {
                                "Resume rest timer"
                            },
                        )
                    }
                    TextButton(onClick = { onAdjust(15) }) {
                        Text("+15s")
                    }
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseBlock(
    block: WorkoutExerciseBlock,
    onAddSet: (String) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    block.exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                block.targetReps?.let {
                    Text("Target reps $it", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SetTableHeader()
        formatWorkoutSetRowsForDisplay(block.sets).forEachIndexed { rowIndex, row ->
            WorkoutSetTableRow(
                row = row,
                isAlternate = rowIndex % 2 == 1,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSet = onToggleSet,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onAddSet(block.exercise.id) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Set")
            }
            TextButton(onClick = { onDuplicateSet(block.exercise.id) }) {
                Text("Duplicate")
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("SET", modifier = Modifier.width(42.dp), textAlign = TextAlign.Start)
        HeaderCell("PREVIOUS", modifier = Modifier.weight(1.35f), textAlign = TextAlign.Start)
        HeaderCell("KG", modifier = Modifier.weight(0.82f))
        HeaderCell("REPS", modifier = Modifier.weight(0.82f))
        HeaderCell("RPE", modifier = Modifier.weight(0.82f))
        HeaderCell("", modifier = Modifier.width(42.dp))
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

@Composable
private fun WorkoutSetTableRow(
    row: WorkoutSetRowDisplay,
    isAlternate: Boolean,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
) {
    val set = row.set
    var reps by remember(set.id) { mutableStateOf(row.reps) }
    var weightKg by remember(set.id) { mutableStateOf(row.weightKg) }
    var rpe by remember(set.id) { mutableStateOf(row.rpe) }
    var notes by remember(set.id) { mutableStateOf(set.notes.orEmpty()) }
    var expanded by remember(set.id) { mutableStateOf(!set.notes.isNullOrBlank()) }

    LaunchedEffect(set.reps, set.weightKg, set.rpe, set.notes) {
        reps = row.reps
        weightKg = row.weightKg
        rpe = row.rpe
        notes = set.notes.orEmpty()
    }

    fun persist(
        nextReps: String = reps,
        nextWeightKg: String = weightKg,
        nextRpe: String = rpe,
        nextNotes: String = notes,
    ) {
        onUpdateSet(set.id, set.setType, nextReps, nextWeightKg, nextRpe, nextNotes)
    }

    val rowColor = if (isAlternate) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.setLabel,
                modifier = Modifier
                    .width(42.dp)
                    .clickable {
                        val nextType = if (row.setLabel == "W") {
                            SET_TYPE_WORKING
                        } else {
                            SET_TYPE_WARMUP
                        }
                        onUpdateSet(set.id, nextType, reps, weightKg, rpe, notes)
                    },
                style = MaterialTheme.typography.titleMedium,
                color = if (row.setLabel == "W") {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                row.previousLabel,
                modifier = Modifier
                    .weight(1.35f)
                    .clickable { expanded = !expanded },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompactCellTextField(
                value = weightKg,
                onValueChange = {
                    weightKg = it
                    persist(nextWeightKg = it)
                },
                modifier = Modifier.weight(0.82f),
                keyboardType = KeyboardType.Decimal,
            )
            CompactCellTextField(
                value = reps,
                onValueChange = {
                    reps = it
                    persist(nextReps = it)
                },
                modifier = Modifier.weight(0.82f),
                keyboardType = KeyboardType.Number,
            )
            CompactRpeField(
                value = rpe,
                onValueChange = {
                    rpe = it
                    persist(nextRpe = it)
                },
                modifier = Modifier.weight(0.82f),
            )
            CompletionButton(
                completed = set.completed,
                onClick = { onToggleSet(set.id, !set.completed) },
                modifier = Modifier.width(42.dp),
            )
        }
        if (expanded || notes.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactNotesField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        persist(nextNotes = it)
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onDeleteSet(set.id) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete set",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun compactExerciseSuggestions(
    exercises: List<ExerciseSummary>,
    query: String,
    expanded: Boolean,
): List<ExerciseSummary> {
    if (!expanded) return emptyList()

    val trimmedQuery = query.trim()
    val filtered = if (trimmedQuery.isBlank()) {
        exercises
    } else {
        exercises.filter { exercise ->
            exercise.name.contains(trimmedQuery, ignoreCase = true) ||
                exercise.category.contains(trimmedQuery, ignoreCase = true) ||
                exercise.targetMuscles.contains(trimmedQuery, ignoreCase = true) ||
                exercise.equipment?.contains(trimmedQuery, ignoreCase = true) == true
        }
    }
    val limit = if (trimmedQuery.isBlank()) 3 else 6
    return filtered.take(limit)
}

@Composable
private fun CompactCellTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    keyboardType: KeyboardType,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier.padding(horizontal = 4.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun CompactRpeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier.padding(horizontal = 4.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isBlank()) {
                    Text(
                        "RPE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun CompactNotesField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text(
                        "Add notes here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun CompletionButton(
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (completed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = if (completed) "Mark incomplete" else "Mark complete",
                tint = if (completed) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

internal data class WorkoutSetRowDisplay(
    val set: LoggedWorkoutSetDetail,
    val setLabel: String,
    val previousLabel: String,
    val weightKg: String,
    val reps: String,
    val rpe: String,
)

internal fun formatWorkoutSetRowsForDisplay(sets: List<LoggedWorkoutSetDetail>): List<WorkoutSetRowDisplay> {
    var workingSetNumber = 0
    return sets.map { set ->
        val isWarmup = set.setType.equals("warmup", ignoreCase = true) ||
            set.setType.equals("warm-up", ignoreCase = true)
        val setLabel = if (isWarmup) {
            "W"
        } else {
            workingSetNumber += 1
            workingSetNumber.toString()
        }
        WorkoutSetRowDisplay(
            set = set,
            setLabel = setLabel,
            previousLabel = set.previousLabel?.takeIf { it.isNotBlank() } ?: "-",
            weightKg = set.weightKg.formatCompact(),
            reps = set.reps?.toString().orEmpty(),
            rpe = set.rpe.formatCompact(),
        )
    }
}

internal fun restTimerDisplayText(restTimer: RestTimerState): String =
    when {
        !restTimer.isVisible || restTimer.remainingSeconds <= 0 -> "Rest Timer: OFF"
        restTimer.isRunning -> "Rest Timer: ${restTimer.remainingSeconds.formatDuration()}"
        else -> "Rest Timer: Paused at ${restTimer.remainingSeconds.formatDuration()}"
    }

private fun Double?.formatCompact(): String =
    when {
        this == null -> ""
        this % 1.0 == 0.0 -> toInt().toString()
        else -> String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    }

private fun Int.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) {
        "${minutes}min ${seconds}s"
    } else {
        "${seconds}s"
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }

private const val SET_TYPE_WARMUP = "warmup"
private const val SET_TYPE_WORKING = "working"
