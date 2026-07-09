package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.WorkoutExerciseBlock
import com.musfit.domain.training.PlateCalculator
import com.musfit.domain.training.WarmupSetCalculator
import com.musfit.domain.training.WarmupSetSuggestion
import com.musfit.domain.training.WorkoutCalculator
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Active workout, focused (mock 5c): a small header with one merged stat line, the rest
 * timer as the screen's single tonal container, full set UI for the current exercise
 * only, and the remaining exercises as plain "Up next" hairline rows.
 */
@Composable
fun TrainingActiveWorkoutContent(
    workout: ActiveWorkoutDetail,
    exercises: List<ExerciseSummary>,
    restTimer: RestTimerState,
    workoutNotes: String,
    restTimerDefaultSecondsInput: String,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
    accent: TabAccent,
    onAddExercise: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onWorkoutNotesChange: (String) -> Unit,
    onSaveWorkoutNotes: () -> Unit,
    onRestTimerDefaultSecondsChange: (String) -> Unit,
    onSaveRestTimer: () -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
    onTickRestTimer: () -> Unit,
    onSkipRestTimer: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onMakeSuperset: (String) -> Unit,
    onDissolveSuperset: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (exerciseId: String, direction: Int) -> Unit,
    onReplaceExercise: (exerciseId: String) -> Unit,
    replaceExerciseTargetId: String?,
    onReplacePick: (String) -> Unit,
    onReplaceDismiss: () -> Unit,
    onOpenCoach: () -> Unit,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    RestTimerTicker(restTimer = restTimer, onTick = onTickRestTimer)

    val blocks = orderedWorkoutBlocks(workout)
    var focusedOverrideId by rememberSaveable(workout.sessionId) { mutableStateOf<String?>(null) }
    val focusedId = focusedOverrideId.takeIf { id -> blocks.any { it.exercise.id == id } }
        ?: defaultFocusedExerciseId(blocks)
    val focusedIndex = blocks.indexOfFirst { it.exercise.id == focusedId }

    var notesDialogOpen by rememberSaveable { mutableStateOf(false) }
    var restDialogOpen by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ActiveWorkoutHeader(
            workout = workout,
            accent = accent,
            onClose = onClose,
            onFinish = onFinish,
            onOpenNotes = { notesDialogOpen = true },
            onOpenRestSettings = { restDialogOpen = true },
            onDiscard = onDiscard,
        )

        RestTimerHero(
            restTimer = restTimer,
            accent = accent,
            onSkip = onSkipRestTimer,
            onAdjust = onAdjustRestTimer,
        )

        // Exercises already passed (before the focused one) stay reachable as quiet rows.
        if (focusedIndex > 0) {
            Column {
                blocks.take(focusedIndex).forEachIndexed { index, block ->
                    CollapsedExerciseRow(
                        block = block,
                        accent = accent,
                        onClick = { focusedOverrideId = block.exercise.id },
                    )
                    if (index < focusedIndex - 1) {
                        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                    }
                }
            }
        }

        blocks.getOrNull(focusedIndex)?.let { block ->
            FocusedExerciseSection(
                block = block,
                accent = accent,
                barWeightKg = barWeightKg,
                availablePlatesKg = availablePlatesKg,
                canMakeSuperset = blocks.size > 1 && block.supersetGroupId == null,
                canMoveUp = focusedIndex > 0,
                canMoveDown = focusedIndex < blocks.lastIndex,
                onAddSet = onAddSet,
                onAddSuggestedWarmupSet = onAddSuggestedWarmupSet,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSet = onToggleSet,
                onMoveSetUp = onMoveSetUp,
                onMoveSetDown = onMoveSetDown,
                onMakeSuperset = onMakeSuperset,
                onDissolveSuperset = onDissolveSuperset,
                onRemoveExercise = onRemoveExercise,
                onMoveExercise = onMoveExercise,
                onReplaceExercise = onReplaceExercise,
                onOpenCoach = onOpenCoach,
            )
        }

        if (focusedIndex < blocks.lastIndex) {
            HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
            Column {
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                val upNext = blocks.drop(focusedIndex + 1)
                upNext.forEachIndexed { index, block ->
                    CollapsedExerciseRow(
                        block = block,
                        accent = accent,
                        onClick = { focusedOverrideId = block.exercise.id },
                    )
                    if (index < upNext.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
        AddExerciseRow(exercises = exercises, accent = accent, onAddExercise = onAddExercise)
    }

    if (notesDialogOpen) {
        WorkoutNotesDialog(
            notes = workoutNotes,
            accent = accent,
            onNotesChange = onWorkoutNotesChange,
            onSave = {
                onSaveWorkoutNotes()
                notesDialogOpen = false
            },
            onDismiss = { notesDialogOpen = false },
        )
    }
    if (restDialogOpen) {
        RestTimerDefaultDialog(
            restTimerDefaultSecondsInput = restTimerDefaultSecondsInput,
            accent = accent,
            onValueChange = onRestTimerDefaultSecondsChange,
            onSave = {
                onSaveRestTimer()
                restDialogOpen = false
            },
            onDismiss = { restDialogOpen = false },
        )
    }
    if (replaceExerciseTargetId != null) {
        ReplaceExercisePickerSheet(
            exercises = exercises,
            accent = accent,
            onPick = onReplacePick,
            onDismiss = onReplaceDismiss,
        )
    }
}

/** Small header (mock 5c): back, title with the merged stat line under it, filled Finish. */
@Composable
private fun ActiveWorkoutHeader(
    workout: ActiveWorkoutDetail,
    accent: TabAccent,
    onClose: () -> Unit,
    onFinish: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenRestSettings: () -> Unit,
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
    val totalSets = workout.exerciseBlocks.sumOf { it.sets.size }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = MusFitTheme.colors.onSurface,
            modifier = Modifier.size(24.dp).clickable(onClick = onClose),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = activeWorkoutStatLine(
                    elapsedSeconds = elapsedSeconds,
                    totalVolumeKg = workout.totalVolumeKg,
                    completedSetCount = workout.completedSetCount,
                    totalSetCount = totalSets,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
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
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Workout options",
                tint = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp).clickable { menu = true },
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Workout notes") },
                    onClick = {
                        menu = false
                        onOpenNotes()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Rest timer") },
                    onClick = {
                        menu = false
                        onOpenRestSettings()
                    },
                )
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

/**
 * The rest timer is the one tonal container on this screen (mock 5c): a thin 40sp
 * countdown, one "+30s" text action, and a filled Skip.
 */
@Composable
private fun RestTimerHero(
    restTimer: RestTimerState,
    accent: TabAccent,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    if (!restTimer.isVisible) return
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rest",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
                    color = accent.onContainer,
                )
                Text(
                    text = restTimer.remainingSeconds.toMinSec(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                    fontWeight = FontWeight.Light,
                    color = accent.onContainer,
                )
            }
            Text(
                text = "+30s",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accent.onContainer,
                modifier = Modifier
                    .clickable { onAdjust(30) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text("Skip")
            }
        }
    }
}

/** A non-focused exercise as one hairline row: name + "3 × 8" target, tap to focus. */
@Composable
private fun CollapsedExerciseRow(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    onClick: () -> Unit,
) {
    val isComplete = block.sets.isNotEmpty() && block.sets.all { it.completed }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = block.exercise.name,
            style = MaterialTheme.typography.titleSmall,
            color = MusFitTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isComplete) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "${block.exercise.name} complete",
                tint = accent.color,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = upNextTarget(block),
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/** The current exercise: full set UI, one utility row, everything else behind the options sheet. */
@Composable
private fun FocusedExerciseSection(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
    canMakeSuperset: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onAddSet: (String) -> Unit,
    onAddSuggestedWarmupSet: (exerciseId: String, reps: Int, weightKg: Double) -> Unit,
    onUpdateSet: (setId: String, setType: String, reps: String, weightKg: String, rpe: String, notes: String) -> Unit,
    onDeleteSet: (String) -> Unit,
    onToggleSet: (String, Boolean) -> Unit,
    onMoveSetUp: (String) -> Unit,
    onMoveSetDown: (String) -> Unit,
    onMakeSuperset: (String) -> Unit,
    onDissolveSuperset: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (exerciseId: String, direction: Int) -> Unit,
    onReplaceExercise: (String) -> Unit,
    onOpenCoach: () -> Unit,
) {
    var optionsOpen by remember { mutableStateOf(false) }
    val warmupSuggestions = warmupSuggestionsFor(block, barWeightKg)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    block.supersetLabel?.let { SupersetBadge(label = it, accent = accent) }
                    Text(
                        text = block.exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Normal,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                lastTimeLabel(block)?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Exercise options",
                tint = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp).clickable { optionsOpen = true },
            )
        }

        val rows = formatWorkoutSetRowsForDisplay(block.sets, block.priorBestEstimatedOneRepMaxKg)
        rows.forEach { row ->
            FocusedSetRow(
                row = row,
                accent = accent,
                onUpdateSet = onUpdateSet,
                onDeleteSet = onDeleteSet,
                onToggleSet = onToggleSet,
                onMoveSetUp = onMoveSetUp,
                onMoveSetDown = onMoveSetDown,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onAddSet(block.exercise.id) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = accent.color,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "Set",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.Medium,
                    color = accent.color,
                )
            }
            plateSummary(block, barWeightKg, availablePlatesKg)?.let { plates ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = plates,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                        color = MusFitTheme.colors.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(onClick = onOpenCoach),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Forum,
                    contentDescription = null,
                    tint = MusFitTheme.colors.accent,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "Coach",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.Medium,
                    color = MusFitTheme.colors.accent,
                )
            }
        }
    }

    if (optionsOpen) {
        ExerciseOptionsSheet(
            exerciseName = block.exercise.name,
            canMakeSuperset = canMakeSuperset,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            hasWarmupSuggestions = warmupSuggestions.isNotEmpty(),
            inSuperset = block.supersetGroupId != null,
            onMoveUp = { onMoveExercise(block.exercise.id, -1) },
            onMoveDown = { onMoveExercise(block.exercise.id, 1) },
            onAddWarmupSets = {
                warmupSuggestions.forEach { suggestion ->
                    onAddSuggestedWarmupSet(block.exercise.id, suggestion.reps, suggestion.weightKg)
                }
            },
            onMakeSuperset = { onMakeSuperset(block.exercise.id) },
            onDissolveSuperset = block.supersetGroupId?.let { groupId -> { onDissolveSuperset(groupId) } },
            onReplace = { onReplaceExercise(block.exercise.id) },
            onRemove = { onRemoveExercise(block.exercise.id) },
            onDismiss = { optionsOpen = false },
        )
    }
}

/**
 * One set row (mock 5c): set number (tap for type/detail options, trophy glyph on a PR),
 * equal kg/reps input fields at 14dp radius, and a 42dp check button. RPE, notes, and
 * move/delete live behind "Details" on the set-number menu.
 */
@Composable
private fun FocusedSetRow(
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
    var detailsOpen by remember(set.id) { mutableStateOf(!set.notes.isNullOrBlank()) }
    var setMenuOpen by remember(set.id) { mutableStateOf(false) }

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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.width(40.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable { setMenuOpen = true },
                ) {
                    Text(
                        text = row.setLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (row.setLabel) {
                            "W" -> MusFitTheme.colors.macroCarbs
                            "D", "F" -> accent.color
                            else -> MusFitTheme.colors.onSurface
                        },
                    )
                    if (row.isPr) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = "Personal record",
                            tint = MusFitTheme.colors.warning,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                DropdownMenu(expanded = setMenuOpen, onDismissRequest = { setMenuOpen = false }) {
                    SET_TYPE_OPTIONS.forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                setMenuOpen = false
                                persist(nextSetType = type)
                            },
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = MusFitTheme.colors.outline)
                    DropdownMenuItem(
                        text = { Text(if (detailsOpen) "Hide details" else "RPE & notes") },
                        onClick = {
                            setMenuOpen = false
                            detailsOpen = !detailsOpen
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        onClick = {
                            setMenuOpen = false
                            onMoveSetUp(set.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        onClick = {
                            setMenuOpen = false
                            onMoveSetDown(set.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete set", color = MusFitTheme.colors.warning) },
                        onClick = {
                            setMenuOpen = false
                            onDeleteSet(set.id)
                        },
                    )
                }
            }
            SetValueField(
                value = weightKg,
                onValueChange = {
                    weightKg = it
                    persist(nextWeightKg = it)
                },
                completed = set.completed,
                accent = accent,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            SetValueField(
                value = reps,
                onValueChange = {
                    reps = it
                    persist(nextReps = it)
                },
                completed = set.completed,
                accent = accent,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (set.completed) accent.color else MusFitTheme.colors.surfaceVariant)
                    .clickable { onToggleSet(set.id, !set.completed) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = if (set.completed) "Mark incomplete" else "Mark complete",
                    tint = if (set.completed) accent.onColor else MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        if (detailsOpen) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 50.dp),
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
            }
        }
    }
}

/** kg/reps cell: done = tonal fill + ink at 500; pending = plain card fill, quiet value. */
@Composable
private fun SetValueField(
    value: String,
    onValueChange: (String) -> Unit,
    completed: Boolean,
    accent: TabAccent,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = if (completed) FontWeight.Medium else FontWeight.Normal,
            color = if (completed) accent.onContainer else MusFitTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(if (completed) accent.onContainer else MusFitTheme.colors.onSurface),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (completed) accent.container else MusFitTheme.colors.surface)
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

/** Quiet "+ Add exercise" row that expands into an inline library search. */
@Composable
private fun AddExerciseRow(
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = null,
                tint = accent.color,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Add exercise",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = accent.color,
            )
        }
        if (expanded) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                placeholder = { Text("Search exercise") },
                shape = MusFitTheme.shapes.medium,
            )
            suggestions.forEach { exercise ->
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddExercise(exercise.id)
                            query = ""
                            expanded = false
                        }
                        .padding(vertical = 10.dp),
                )
            }
            if (query.isNotBlank() && suggestions.isEmpty()) {
                Text(
                    text = "No matching exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkoutNotesDialog(
    notes: String,
    accent: TabAccent,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout notes") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                minLines = 2,
                maxLines = 5,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RestTimerDefaultDialog(
    restTimerDefaultSecondsInput: String,
    accent: TabAccent,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rest timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = restTimerSettingsSummaryText(restTimerDefaultSecondsInput),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = restTimerDefaultSecondsInput,
                    onValueChange = onValueChange,
                    label = { Text("Seconds after set") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MusFitTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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

/** Exercise options bottom sheet (M3 list items). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseOptionsSheet(
    exerciseName: String,
    canMakeSuperset: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    hasWarmupSuggestions: Boolean,
    inSuperset: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddWarmupSets: () -> Unit,
    onMakeSuperset: () -> Unit,
    onDissolveSuperset: (() -> Unit)?,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MusFitTheme.colors.surface) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (hasWarmupSuggestions) {
                ExerciseSheetItem(Icons.Outlined.FitnessCenter, "Add suggested warm-up sets", MusFitTheme.colors.onSurface) { onAddWarmupSets(); onDismiss() }
            }
            if (canMoveUp) {
                ExerciseSheetItem(Icons.Outlined.KeyboardArrowUp, "Move up", MusFitTheme.colors.onSurface) { onMoveUp(); onDismiss() }
            }
            if (canMoveDown) {
                ExerciseSheetItem(Icons.Outlined.KeyboardArrowDown, "Move down", MusFitTheme.colors.onSurface) { onMoveDown(); onDismiss() }
            }
            ExerciseSheetItem(Icons.Outlined.SwapHoriz, "Replace exercise", MusFitTheme.colors.onSurface) { onReplace(); onDismiss() }
            if (canMakeSuperset) {
                ExerciseSheetItem(Icons.Outlined.Add, "Add to superset", MusFitTheme.colors.onSurface) { onMakeSuperset(); onDismiss() }
            }
            if (inSuperset && onDissolveSuperset != null) {
                ExerciseSheetItem(Icons.Outlined.Close, "Dissolve superset", MusFitTheme.colors.onSurface) { onDissolveSuperset(); onDismiss() }
            }
            ExerciseSheetItem(Icons.Outlined.Delete, "Remove exercise", MusFitTheme.colors.warning) { onRemove(); onDismiss() }
        }
    }
}

@Composable
private fun ExerciseSheetItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = tint,
            leadingIconColor = tint,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Modal picker for the "Replace exercise" action — search the library and pick a replacement. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplaceExercisePickerSheet(
    exercises: List<ExerciseSummary>,
    accent: TabAccent,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        val q = query.trim()
        exercises.filter {
            q.isBlank() ||
                it.name.contains(q, ignoreCase = true) ||
                it.targetMuscles.contains(q, ignoreCase = true) ||
                it.equipment.orEmpty().contains(q, ignoreCase = true)
        }.take(40)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MusFitTheme.colors.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text(
                "Replace exercise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                filtered.forEach { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            val meta = listOfNotNull(
                                exercise.equipment,
                                exercise.targetMuscles.takeIf(String::isNotBlank),
                            ).joinToString(" · ")
                            if (meta.isNotBlank()) Text(meta)
                        },
                        leadingContent = {
                            ExerciseThumb(
                                imageUrl = exercise.imageUrl,
                                contentDescription = exercise.name,
                                accent = accent,
                                size = 40.dp,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onPick(exercise.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SupersetBadge(label: String, accent: TabAccent) {
    Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = accent.onContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
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
        modifier = modifier,
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
                    .background(MusFitTheme.colors.surfaceVariant)
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

// --- Display helpers (pure, unit-tested) ---

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

/** Exercises in on-screen order: grouping order when present, superset members inline. */
internal fun orderedWorkoutBlocks(workout: ActiveWorkoutDetail): List<WorkoutExerciseBlock> {
    val groupings = workout.exerciseGroupings
    if (groupings.isEmpty()) return workout.exerciseBlocks
    return groupings.flatMap { grouping ->
        when (grouping) {
            is ExerciseGrouping.Single -> listOf(grouping.block)
            is ExerciseGrouping.Superset -> grouping.group.exerciseBlocks
        }
    }
}

/** The exercise in focus: the first with an unfinished set, else the last one (or null when empty). */
internal fun defaultFocusedExerciseId(blocks: List<WorkoutExerciseBlock>): String? =
    blocks.firstOrNull { block -> block.sets.any { !it.completed } }?.exercise?.id
        ?: blocks.lastOrNull()?.exercise?.id

/** Merged header stat line (mock 5c): "1:12 · 537.5 kg · set 2 of 13". */
internal fun activeWorkoutStatLine(
    elapsedSeconds: Long,
    totalVolumeKg: Double,
    completedSetCount: Int,
    totalSetCount: Int,
): String {
    val parts = mutableListOf(
        elapsedSeconds.toElapsedClock(),
        "${totalVolumeKg.formatKg()} kg",
    )
    if (totalSetCount > 0) {
        parts += "set ${(completedSetCount + 1).coerceAtMost(totalSetCount)} of $totalSetCount"
    }
    return parts.joinToString(" · ")
}

/** Up-next trailing target: "3 × 8" from the planned sets, or a plain set count. */
internal fun upNextTarget(block: WorkoutExerciseBlock): String {
    val setCount = block.sets.size
    val reps = block.targetReps?.trim()?.takeIf(String::isNotBlank)
    return when {
        setCount == 0 -> "no sets"
        reps != null -> "$setCount × $reps"
        else -> "$setCount ${if (setCount == 1) "set" else "sets"}"
    }
}

/** "last time 105 kg × 5" from the first set carrying a previous label (replaces the PREVIOUS column). */
internal fun lastTimeLabel(block: WorkoutExerciseBlock): String? {
    val previous = block.sets.firstNotNullOfOrNull { set ->
        set.previousLabel?.trim()?.takeIf(String::isNotBlank)
    }
    if (previous != null) return "last time $previous"
    val target = block.targetReps?.trim()?.takeIf(String::isNotBlank)
    return target?.let { "target $it reps" }
}

internal fun warmupSuggestionsFor(
    block: WorkoutExerciseBlock,
    barWeightKg: Double,
): List<WarmupSetSuggestion> {
    val workingSet = block.sets.lastOrNull { set ->
        !set.setType.equals(SET_TYPE_WARMUP, ignoreCase = true) &&
            set.reps != null &&
            set.weightKg != null
    } ?: return emptyList()
    return WarmupSetCalculator.suggestions(
        workingWeightKg = workingSet.weightKg,
        workingReps = workingSet.reps,
        barWeightKg = barWeightKg,
    )
}

/** "Plates 25 + 15 + 2.5" (per side) for the focused exercise's heaviest entered weight. */
internal fun plateSummary(
    block: WorkoutExerciseBlock,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
): String? {
    val weight = block.sets.lastOrNull { (it.weightKg ?: 0.0) > 0.0 }?.weightKg ?: return null
    val plates = PlateCalculator.platesPerSide(weight, barWeightKg, availablePlatesKg)
    if (plates.isEmpty()) return null
    return "Plates ${plates.joinToString(" + ") { it.formatPlate() }}"
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

internal fun restTimerSettingsSummaryText(restTimerDefaultSecondsInput: String): String {
    val seconds = restTimerDefaultSecondsInput.trim().takeIf(String::isNotBlank)
    return if (seconds == null) {
        "Set rest after each completed set"
    } else {
        "$seconds sec after each completed set"
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

private fun Double.formatPlate(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    }

private fun Int.toMinSec(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
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
        String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
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
