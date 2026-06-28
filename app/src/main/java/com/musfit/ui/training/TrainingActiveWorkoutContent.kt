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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.SupersetGroup
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.domain.training.PlateCalculator
import com.musfit.domain.training.WarmupSetCalculator
import com.musfit.domain.training.WarmupSetSuggestion
import com.musfit.domain.training.WorkoutCalculator
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TrainingActiveWorkoutContent(
    workout: ActiveWorkoutDetail,
    exercises: List<ExerciseSummary>,
    restTimer: RestTimerState,
    workoutNotes: String,
    restTimerDefaultSecondsInput: String,
    plateBarWeightInput: String,
    availablePlatesInput: String,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
    accent: TabAccent,
    onAddExercise: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onWorkoutNotesChange: (String) -> Unit,
    onSaveWorkoutNotes: () -> Unit,
    onRestTimerDefaultSecondsChange: (String) -> Unit,
    onPlateBarWeightChange: (String) -> Unit,
    onAvailablePlatesChange: (String) -> Unit,
    onSaveTrainingTools: () -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
    onTickRestTimer: () -> Unit,
    onPauseRestTimer: () -> Unit,
    onResumeRestTimer: () -> Unit,
    onSkipRestTimer: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onMakeSuperset: (String) -> Unit,
    onDissolveSuperset: (String) -> Unit,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    RestTimerTicker(restTimer = restTimer, onTick = onTickRestTimer)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActiveWorkoutTopBar(
            workout = workout,
            accent = accent,
            onClose = onClose,
            onFinish = onFinish,
            onDiscard = onDiscard,
        )
        WorkoutStatRow(workout = workout)
        ActiveWorkoutNotesCard(
            notes = workoutNotes,
            accent = accent,
            onNotesChange = onWorkoutNotesChange,
            onSave = onSaveWorkoutNotes,
        )
        TrainingToolsCard(
            restTimerDefaultSecondsInput = restTimerDefaultSecondsInput,
            plateBarWeightInput = plateBarWeightInput,
            availablePlatesInput = availablePlatesInput,
            accent = accent,
            onRestTimerDefaultSecondsChange = onRestTimerDefaultSecondsChange,
            onPlateBarWeightChange = onPlateBarWeightChange,
            onAvailablePlatesChange = onAvailablePlatesChange,
            onSave = onSaveTrainingTools,
        )
        RestTimerBar(
            restTimer = restTimer,
            accent = accent,
            onPause = onPauseRestTimer,
            onResume = onResumeRestTimer,
            onSkip = onSkipRestTimer,
            onAdjust = onAdjustRestTimer,
        )
        AddExerciseCompactBar(
            exercises = exercises,
            accent = accent,
            onAddExercise = onAddExercise,
        )
        val groupings = workout.exerciseGroupings.ifEmpty {
            workout.exerciseBlocks.map { ExerciseGrouping.Single(it) }
        }
        groupings.forEachIndexed { index, grouping ->
            when (grouping) {
                is ExerciseGrouping.Single -> {
                    val canMakeSuperset = groupings.drop(index + 1).any { it is ExerciseGrouping.Single }
                    ActiveExerciseBlock(
                        block = grouping.block,
                        accent = accent,
                        barWeightKg = barWeightKg,
                        availablePlatesKg = availablePlatesKg,
                        onAddSet = onAddSet,
                        onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
                        onDuplicateSet = onDuplicateSet,
                        onUpdateSet = onUpdateSet,
                        onDeleteSet = onDeleteSet,
                        onToggleSet = onToggleSet,
                        onMoveSetUp = onMoveSetUp,
                        onMoveSetDown = onMoveSetDown,
                        canMakeSuperset = canMakeSuperset,
                        onMakeSuperset = onMakeSuperset,
                    )
                }
                is ExerciseGrouping.Superset ->
                    if (grouping.group.exerciseBlocks.size > 1) {
                        SupersetGroupCard(
                            group = grouping.group,
                            accent = accent,
                            barWeightKg = barWeightKg,
                            availablePlatesKg = availablePlatesKg,
                            onAddSet = onAddSet,
                            onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
                            onDuplicateSet = onDuplicateSet,
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = onDeleteSet,
                            onToggleSet = onToggleSet,
                            onMoveSetUp = onMoveSetUp,
                            onMoveSetDown = onMoveSetDown,
                            onDissolveSuperset = onDissolveSuperset,
                        )
                    } else {
                        ActiveExerciseBlock(
                            block = grouping.group.exerciseBlocks.first(),
                            accent = accent,
                            barWeightKg = barWeightKg,
                            availablePlatesKg = availablePlatesKg,
                            onAddSet = onAddSet,
                            onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
                            onDuplicateSet = onDuplicateSet,
                            onUpdateSet = onUpdateSet,
                            onDeleteSet = onDeleteSet,
                            onToggleSet = onToggleSet,
                            onMoveSetUp = onMoveSetUp,
                            onMoveSetDown = onMoveSetDown,
                        )
                    }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutTopBar(
    workout: ActiveWorkoutDetail,
    accent: TabAccent,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.sessionId) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((nowMillis - workout.startedAtEpochMillis) / 1_000L).coerceAtLeast(0L)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MusFitTheme.colors.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                workout.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
            )
            Text(
                text = elapsedSeconds.toElapsedClock(),
                style = MaterialTheme.typography.bodyMedium,
                color = accent.color,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
        ) {
            Text("Finish")
        }
        Box {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Discard workout", color = MusFitTheme.colors.warning) },
                    onClick = {
                        menu = false
                        onDiscard()
                    },
                )
            }
        }
    }
}

@Composable
private fun WorkoutStatRow(workout: ActiveWorkoutDetail) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatCell(label = "Sets", value = workout.completedSetCount.toString(), modifier = Modifier.weight(1f))
            StatCell(label = "Volume", value = "${workout.totalVolumeKg.formatKg()} kg", modifier = Modifier.weight(1f))
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MusFitTheme.colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
    }
}

@Composable
private fun ActiveWorkoutNotesCard(
    notes: String,
    accent: TabAccent,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier.weight(1f),
                label = { Text("Workout notes") },
                minLines = 1,
                maxLines = 3,
            )
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.color,
                    contentColor = accent.onColor,
                ),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun TrainingToolsCard(
    restTimerDefaultSecondsInput: String,
    plateBarWeightInput: String,
    availablePlatesInput: String,
    accent: TabAccent,
    onRestTimerDefaultSecondsChange: (String) -> Unit,
    onPlateBarWeightChange: (String) -> Unit,
    onAvailablePlatesChange: (String) -> Unit,
    onSave: () -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = restTimerDefaultSecondsInput,
                    onValueChange = onRestTimerDefaultSecondsChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Rest sec") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = plateBarWeightInput,
                    onValueChange = onPlateBarWeightChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Bar kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = availablePlatesInput,
                    onValueChange = onAvailablePlatesChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Plates kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.color,
                        contentColor = accent.onColor,
                    ),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/** Drives the rest-timer countdown from the UI: ticks once per second while running. */
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
private fun RestTimerBar(
    restTimer: RestTimerState,
    accent: TabAccent,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    if (!restTimer.isVisible) return
    Surface(
        color = accent.container,
        shape = MusFitTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = accent.onContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = restTimer.remainingSeconds.toMinSec(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent.onContainer,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            )
            TextButton(onClick = { onAdjust(-15) }) {
                Text("−15", color = accent.onContainer, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { onAdjust(15) }) {
                Text("+15", color = accent.onContainer, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { if (restTimer.isRunning) onPause() else onResume() }) {
                Icon(
                    imageVector = if (restTimer.isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (restTimer.isRunning) "Pause rest" else "Resume rest",
                    tint = accent.onContainer,
                )
            }
            TextButton(onClick = onSkip) {
                Text("Skip", color = accent.onContainer, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AddExerciseCompactBar(
    exercises: List<ExerciseSummary>,
    accent: TabAccent,
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
        color = MusFitTheme.colors.surface,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clip(MusFitTheme.shapes.medium)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = accent.color,
                )
                Text(
                    text = "Add exercise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MusFitTheme.colors.onSurface,
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
                            tint = MusFitTheme.colors.onSurfaceVariant,
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
                                color = MusFitTheme.colors.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    if (query.isNotBlank() && suggestions.isEmpty()) {
                        Text(
                            text = "No matching exercises",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MusFitTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseBlock(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
    onAddSet: (String) -> Unit,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
    nested: Boolean = false,
    canMakeSuperset: Boolean = false,
    onMakeSuperset: ((String) -> Unit)? = null,
) {
    if (nested) {
        ActiveExerciseBlockBody(
            block = block,
            accent = accent,
            contentPadding = 12.dp,
            canMakeSuperset = false,
            onMakeSuperset = null,
            barWeightKg = barWeightKg,
            availablePlatesKg = availablePlatesKg,
            onAddSet = onAddSet,
            onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
            onDuplicateSet = onDuplicateSet,
            onUpdateSet = onUpdateSet,
            onDeleteSet = onDeleteSet,
            onToggleSet = onToggleSet,
            onMoveSetUp = onMoveSetUp,
            onMoveSetDown = onMoveSetDown,
        )
    } else {
        Surface(
            color = MusFitTheme.colors.surface,
            shape = MusFitTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ActiveExerciseBlockBody(
                block = block,
                accent = accent,
                contentPadding = 14.dp,
                canMakeSuperset = canMakeSuperset,
                onMakeSuperset = onMakeSuperset,
                barWeightKg = barWeightKg,
                availablePlatesKg = availablePlatesKg,
                onAddSet = onAddSet,
                onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
                onDuplicateSet = onDuplicateSet,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSet = onToggleSet,
                onMoveSetUp = onMoveSetUp,
                onMoveSetDown = onMoveSetDown,
            )
        }
    }
}

@Composable
private fun ActiveExerciseBlockBody(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    contentPadding: Dp,
    canMakeSuperset: Boolean,
    onMakeSuperset: ((String) -> Unit)?,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
    onAddSet: (String) -> Unit,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val exerciseImage = block.exercise.imageUrl
            if (exerciseImage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = accent.onContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                ExerciseThumb(
                    imageUrl = exerciseImage,
                    contentDescription = block.exercise.name,
                    accent = accent,
                    size = 40.dp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    block.supersetLabel?.let { SupersetBadge(label = it, accent = accent) }
                    Text(
                        block.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.color,
                    )
                }
                block.targetReps?.let {
                    Text(
                        "Target reps $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            if (onMakeSuperset != null) {
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Exercise options",
                            tint = accent.color,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Make superset with next") },
                            enabled = canMakeSuperset,
                            onClick = {
                                menu = false
                                onMakeSuperset(block.exercise.id)
                            },
                        )
                    }
                }
            }
        }

        SetTableHeader()
        val rows = formatWorkoutSetRowsForDisplay(block.sets, block.priorBestEstimatedOneRepMaxKg)
        rows.forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MusFitTheme.colors.outline.copy(alpha = 0.4f),
                )
            }
            WorkoutSetTableRow(
                row = row,
                accent = accent,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSet = onToggleSet,
                onMoveSetUp = onMoveSetUp,
                onMoveSetDown = onMoveSetDown,
            )
        }

        WarmupSuggestionRow(
            block = block,
            barWeightKg = barWeightKg,
            accent = accent,
            onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
        )
        PlateLine(
            block = block,
            barWeightKg = barWeightKg,
            availablePlatesKg = availablePlatesKg,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onAddSet(block.exercise.id) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accent.container, contentColor = accent.onContainer),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add set")
            }
            TextButton(onClick = { onDuplicateSet(block.exercise.id) }) {
                Text("Duplicate", color = accent.color)
            }
        }
    }
}

@Composable
private fun SupersetGroupCard(
    group: SupersetGroup,
    accent: TabAccent,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
    onAddSet: (String) -> Unit,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onDuplicateSet: (String) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
    onDissolveSuperset: (String) -> Unit,
) {
    Surface(
        color = accent.container,
        shape = MusFitTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
            ) {
                Surface(color = accent.color, shape = MusFitTheme.shapes.small) {
                    Text(
                        text = "SUPERSET",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onDissolveSuperset(group.supersetGroupId) }) {
                    Text("Dissolve", color = accent.onContainer, fontWeight = FontWeight.SemiBold)
                }
            }
            group.exerciseBlocks.forEach { member ->
                Surface(
                    color = MusFitTheme.colors.surface,
                    shape = MusFitTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ActiveExerciseBlock(
                        block = member,
                        accent = accent,
                        barWeightKg = barWeightKg,
                        availablePlatesKg = availablePlatesKg,
                        onAddSet = onAddSet,
                        onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
                        onDuplicateSet = onDuplicateSet,
                        onUpdateSet = onUpdateSet,
                        onDeleteSet = onDeleteSet,
                        onToggleSet = onToggleSet,
                        onMoveSetUp = onMoveSetUp,
                        onMoveSetDown = onMoveSetDown,
                        nested = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupersetBadge(label: String, accent: TabAccent) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(accent.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent.onContainer,
        )
    }
}

@Composable
private fun WarmupSuggestionRow(
    block: WorkoutExerciseBlock,
    barWeightKg: Double,
    accent: TabAccent,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
) {
    val workingSet = block.sets.lastOrNull { set ->
        !set.setType.equals(SET_TYPE_WARMUP, ignoreCase = true) &&
            set.reps != null &&
            set.weightKg != null
    } ?: return
    val suggestions = WarmupSetCalculator.suggestions(
        workingWeightKg = workingSet.weightKg,
        workingReps = workingSet.reps,
        barWeightKg = barWeightKg,
    )
    if (suggestions.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Warm-up",
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        suggestions.forEach { suggestion ->
            TextButton(
                onClick = {
                    onAddSuggestedWarmupSet(block.exercise.id, suggestion.reps, suggestion.weightKg)
                },
            ) {
                Text(
                    text = suggestion.label(),
                    color = accent.color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PlateLine(
    block: WorkoutExerciseBlock,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
) {
    val weight = block.sets.lastOrNull { (it.weightKg ?: 0.0) > 0.0 }?.weightKg ?: return
    val text = plateLineText(weight, barWeightKg, availablePlatesKg) ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MusFitTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

internal fun plateLineText(
    weightKg: Double,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
): String? {
    val plates = PlateCalculator.platesPerSide(weightKg, barWeightKg, availablePlatesKg)
    if (plates.isEmpty()) return null
    return "Plates · ${plates.joinToString(" + ") { it.formatPlate() }} / side"
}

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("SET", modifier = Modifier.width(40.dp), textAlign = TextAlign.Start)
        HeaderCell("PREVIOUS", modifier = Modifier.weight(1.1f), textAlign = TextAlign.Start)
        HeaderCell("KG", modifier = Modifier.weight(0.9f))
        HeaderCell("REPS", modifier = Modifier.weight(0.9f))
        HeaderCell("", modifier = Modifier.width(44.dp))
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
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MusFitTheme.colors.onSurfaceVariant,
        textAlign = textAlign,
    )
}

@Composable
private fun WorkoutSetTableRow(
    row: WorkoutSetRowDisplay,
    accent: TabAccent,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
) {
    val set = row.set
    var reps by remember(set.id) { mutableStateOf(row.reps) }
    var weightKg by remember(set.id) { mutableStateOf(row.weightKg) }
    var rpe by remember(set.id) { mutableStateOf(row.rpe) }
    var notes by remember(set.id) { mutableStateOf(set.notes.orEmpty()) }
    var expanded by remember(set.id) { mutableStateOf(!set.notes.isNullOrBlank()) }
    var setTypeMenuOpen by remember(set.id) { mutableStateOf(false) }

    LaunchedEffect(set.reps, set.weightKg, set.rpe, set.notes) {
        reps = row.reps
        weightKg = row.weightKg
        rpe = row.rpe
        notes = set.notes.orEmpty()
    }

    fun persist(
        nextSetType: String = set.setType,
        nextReps: String = reps,
        nextWeightKg: String = weightKg,
        nextRpe: String = rpe,
        nextNotes: String = notes,
    ) {
        onUpdateSet(set.id, nextSetType, nextReps, nextWeightKg, nextRpe, nextNotes)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Box {
                    Text(
                        row.setLabel,
                        modifier = Modifier.clickable { setTypeMenuOpen = true },
                        style = MaterialTheme.typography.titleMedium,
                        color = when (row.setLabel) {
                            "W" -> MusFitTheme.colors.macroCarbs
                            "D", "F" -> accent.color
                            else -> MusFitTheme.colors.onSurface
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                    DropdownMenu(expanded = setTypeMenuOpen, onDismissRequest = { setTypeMenuOpen = false }) {
                        SET_TYPE_OPTIONS.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    setTypeMenuOpen = false
                                    persist(nextSetType = type)
                                },
                            )
                        }
                    }
                }
                if (row.isPr) {
                    PrChip(accent = accent)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(MusFitTheme.shapes.small)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.previousLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide set details" else "Show set details",
                    tint = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
            CompactCellTextField(
                value = weightKg,
                onValueChange = {
                    weightKg = it
                    persist(nextWeightKg = it)
                },
                modifier = Modifier.weight(0.9f),
                keyboardType = KeyboardType.Decimal,
            )
            CompactCellTextField(
                value = reps,
                onValueChange = {
                    reps = it
                    persist(nextReps = it)
                },
                modifier = Modifier.weight(0.9f),
                keyboardType = KeyboardType.Number,
            )
            CompletionButton(
                completed = set.completed,
                accent = accent,
                onClick = { onToggleSet(set.id, !set.completed) },
                modifier = Modifier.width(44.dp),
            )
        }
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactRpeField(
                    value = rpe,
                    onValueChange = {
                        rpe = it
                        persist(nextRpe = it)
                    },
                    modifier = Modifier.width(64.dp),
                )
                CompactNotesField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        persist(nextNotes = it)
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onMoveSetUp(set.id) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move set up",
                        tint = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { onMoveSetDown(set.id) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move set down",
                        tint = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { onDeleteSet(set.id) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete set",
                        tint = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrChip(accent: TabAccent) {
    Surface(color = accent.color, shape = MusFitTheme.shapes.extraSmall) {
        Text(
            text = "PR",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent.onColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
        )
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
            color = MusFitTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier.padding(horizontal = 4.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(MusFitTheme.shapes.small)
                    .background(MusFitTheme.colors.surfaceVariant)
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
            color = MusFitTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier.padding(horizontal = 4.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(MusFitTheme.shapes.small)
                    .background(MusFitTheme.colors.surfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isBlank()) {
                    Text(
                        "RPE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
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
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MusFitTheme.colors.onSurface),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(MusFitTheme.shapes.small)
                    .background(MusFitTheme.colors.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text(
                        "Add notes here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
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
    accent: TabAccent,
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
                .clip(MusFitTheme.shapes.small)
                .background(if (completed) accent.color else MusFitTheme.colors.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = if (completed) "Mark incomplete" else "Mark complete",
                tint = if (completed) accent.onColor else MusFitTheme.colors.onSurfaceVariant,
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
    val isPr: Boolean,
)

internal fun formatWorkoutSetRowsForDisplay(
    sets: List<LoggedWorkoutSetDetail>,
    priorBestEstimatedOneRepMaxKg: Double = 0.0,
): List<WorkoutSetRowDisplay> {
    var workingSetNumber = 0
    return sets.map { set ->
        val normalizedSetType = set.setType.lowercase(Locale.US)
        val isWarmup = normalizedSetType == SET_TYPE_WARMUP || normalizedSetType == "warm-up"
        val isDropSet = normalizedSetType == SET_TYPE_DROP
        val isFailureSet = normalizedSetType == SET_TYPE_FAILURE
        val setLabel = when {
            isWarmup -> "W"
            isDropSet -> "D"
            isFailureSet -> "F"
            else -> {
                workingSetNumber += 1
                workingSetNumber.toString()
            }
        }
        val reps = set.reps
        val weightKg = set.weightKg
        val isPr = !isWarmup && !isDropSet && set.completed && reps != null && weightKg != null &&
            WorkoutCalculator.estimatedOneRepMax(weightKg, reps) > priorBestEstimatedOneRepMaxKg + 1e-6
        WorkoutSetRowDisplay(
            set = set,
            setLabel = setLabel,
            previousLabel = set.previousLabel?.takeIf { it.isNotBlank() } ?: "-",
            weightKg = set.weightKg.formatCompact(),
            reps = set.reps?.toString().orEmpty(),
            rpe = set.rpe.formatCompact(),
            isPr = isPr,
        )
    }
}

private fun Double?.formatCompact(): String =
    when {
        this == null -> ""
        this % 1.0 == 0.0 -> toInt().toString()
        else -> String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    }

private fun Double.formatPlate(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    }

private fun WarmupSetSuggestion.label(): String = "${weightKg.formatPlate()} kg x $reps"

private fun Int.toMinSec(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

internal fun restTimerDisplayText(restTimer: RestTimerState): String =
    when {
        !restTimer.isVisible || restTimer.remainingSeconds <= 0 -> "Rest Timer: OFF"
        restTimer.isRunning -> "Rest Timer: ${restTimer.remainingSeconds.formatDuration()}"
        else -> "Rest Timer: Paused at ${restTimer.remainingSeconds.formatDuration()}"
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

private fun Long.toElapsedClock(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
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
private const val SET_TYPE_DROP = "drop"
private const val SET_TYPE_FAILURE = "failure"
private val SET_TYPE_OPTIONS = listOf(
    SET_TYPE_WARMUP to "Warmup",
    SET_TYPE_WORKING to "Working",
    SET_TYPE_DROP to "Drop set",
    SET_TYPE_FAILURE to "Failure",
)
