package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.musfit.feature.training.R
import com.musfit.ui.components.CircularWavyRing
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.PillButton
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.groupedShape
import com.musfit.ui.icons.filled.AutoAwesome
import com.musfit.ui.icons.filled.ChatBubble
import com.musfit.ui.icons.outlined.EmojiEvents
import com.musfit.ui.icons.outlined.FitnessCenter
import com.musfit.ui.icons.outlined.SwapHoriz
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.BrandCoral
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Active workout (Turn 10 §10a on the Turn-5 declutter structure): a back-circle
 * header with one merged stat line and a filled Finish pill, the rest timer as
 * the screen's single tonal container (M3E circular wavy countdown), the current
 * exercise as a white card with a per-set LAST column, and the remaining
 * exercises as grouped "Up next" rows.
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

        // Exercises already passed (before the focused one) stay reachable as
        // quiet grouped rows above the current card.
        if (focusedIndex > 0) {
            val passed = blocks.take(focusedIndex)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                passed.forEachIndexed { index, block ->
                    CollapsedExerciseRow(
                        block = block,
                        accent = accent,
                        shape = groupedShape(index, passed.size),
                        badgeShape = if (index % 2 == 0) ExpressiveBadgeShape.Circle else ExpressiveBadgeShape.Squircle,
                        onClick = { focusedOverrideId = block.exercise.id },
                    )
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
            Column {
                Text(
                    text = stringResource(R.string.training_up_next),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                )
                val upNext = blocks.drop(focusedIndex + 1)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    upNext.forEachIndexed { index, block ->
                        CollapsedExerciseRow(
                            block = block,
                            accent = accent,
                            shape = groupedShape(index, upNext.size),
                            badgeShape = if (index % 2 == 0) ExpressiveBadgeShape.Circle else ExpressiveBadgeShape.Squircle,
                            onClick = { focusedOverrideId = block.exercise.id },
                        )
                    }
                }
            }
        }

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

/**
 * 10a header: 44dp tonal back circle, 22/800 title over the merged stat line
 * (live duration emphasized), and a filled Finish pill. Notes, rest settings,
 * and discard stay reachable behind a small overflow — the mock hides them, but
 * dropping the features is out of scope for a restyle.
 */
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
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.sessionId) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((nowMillis - workout.startedAtEpochMillis) / 1_000L).coerceAtLeast(0L)
    val totalSets = workout.exerciseBlocks.sumOf { it.sets.size }
    val statLine = activeWorkoutStatLine(
        elapsedSeconds = elapsedSeconds,
        totalVolumeKg = workout.totalVolumeKg,
        completedSetCount = workout.completedSetCount,
        totalSetCount = totalSets,
    ).asString()
    val clock = elapsedSeconds.toElapsedClock()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TonalHeaderIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.training_back),
            onClick = onClose,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MusFitTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(fontWeight = FontWeight.ExtraBold, color = MusFitTheme.colors.onSurface),
                    ) {
                        append(clock)
                    }
                    append(statLine.removePrefix(clock))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        PillButton(
            text = stringResource(R.string.training_finish),
            onClick = onFinish,
            containerColor = accent.color,
            contentColor = accent.onColor,
            height = MinimumInteractiveSize,
        )
        Box {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }, modifier = Modifier.size(MinimumInteractiveSize)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.training_workout_options),
                    tint = MusFitTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.training_workout_notes)) },
                    onClick = {
                        menu = false
                        onOpenNotes()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.training_rest_timer)) },
                    onClick = {
                        menu = false
                        onOpenRestSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.training_discard_workout), color = MusFitTheme.colors.warning) },
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
 * The rest hero (10a) — the screen's one tonal container: the M3E circular wavy
 * countdown ring (elapsed arc wavy, remainder a quiet track with a stop dot)
 * with the time inside, next to a REST overline, the connected −30s/+30s pair,
 * and a filled Skip pill.
 */
@Composable
private fun RestTimerHero(
    restTimer: RestTimerState,
    accent: TabAccent,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    if (!restTimer.isVisible) return
    val quietPill = MusFitTheme.colors.surface.copy(alpha = 0.75f)
    val remainingDescription = restTimerRemainingContentDescription(restTimer.remainingSeconds).asString()
    Surface(
        color = accent.container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.size(118.dp), contentAlignment = Alignment.Center) {
                CircularWavyRing(
                    progress = restElapsedFraction(restTimer),
                    color = accent.color,
                    trackColor = quietPill,
                    running = restTimer.isRunning,
                    flatten = restWaveFlatten(restTimer.remainingSeconds),
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = restTimer.remainingSeconds.toMinSec(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                        color = accent.onContainer,
                        modifier = Modifier.semantics {
                            contentDescription = remainingDescription
                        },
                    )
                    Text(
                        text = stringResource(R.string.training_of_time, restTimer.durationSeconds.toMinSec()),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 0.sp),
                        fontWeight = FontWeight.Medium,
                        color = accent.onContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.training_rest),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.onContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                    RestAdjustSegment(
                        label = stringResource(R.string.training_minus_thirty_seconds),
                        contentDescription = stringResource(R.string.training_shorten_rest),
                        color = quietPill,
                        contentColor = accent.onContainer,
                        shape = RoundedCornerShape(topStart = 99.dp, bottomStart = 99.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                        onClick = { onAdjust(-30) },
                        modifier = Modifier.weight(1f),
                    )
                    RestAdjustSegment(
                        label = stringResource(R.string.training_plus_thirty_seconds),
                        contentDescription = stringResource(R.string.training_extend_rest),
                        color = quietPill,
                        contentColor = accent.onContainer,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 99.dp, bottomEnd = 99.dp),
                        onClick = { onAdjust(30) },
                        modifier = Modifier.weight(1f),
                    )
                }
                PillButton(
                    text = stringResource(R.string.training_skip),
                    onClick = onSkip,
                    containerColor = accent.color,
                    contentColor = accent.onColor,
                    height = MinimumInteractiveSize,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** One half of the connected −30s/+30s pair: 99 outer / 8 inner corners, 2dp gap. */
@Composable
private fun RestAdjustSegment(
    label: String,
    contentDescription: String,
    color: Color,
    contentColor: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = color,
        contentColor = contentColor,
        shape = shape,
        modifier = modifier
            .height(MinimumInteractiveSize)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/**
 * A non-focused exercise as one grouped white row (10a "Up next"): 40dp tonal
 * badge, 15/700 name, trailing "3 × 8" target (or a check once complete). Tap
 * brings the exercise into focus.
 */
@Composable
private fun CollapsedExerciseRow(
    block: WorkoutExerciseBlock,
    accent: TabAccent,
    shape: RoundedCornerShape,
    badgeShape: ExpressiveBadgeShape,
    onClick: () -> Unit,
) {
    val isComplete = block.sets.isNotEmpty() && block.sets.all { it.completed }
    val completeDescription = stringResource(R.string.training_complete)
    val upNextDescription = upNextTarget(block).asString()
    Surface(
        onClick = onClick,
        color = MusFitTheme.colors.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = block.exercise.name
                stateDescription = if (isComplete) completeDescription else upNextDescription
                role = Role.Button
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpressiveBadge(
                icon = Icons.Outlined.FitnessCenter,
                shape = badgeShape,
                containerColor = accent.container,
                contentColor = accent.onContainer,
                size = 40.dp,
                iconSize = 18.dp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                block.supersetLabel?.let { SupersetBadge(label = it, accent = accent) }
                Text(
                    text = block.exercise.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CollapsedExerciseStatus(
                isComplete = isComplete,
                upNextDescription = upNextDescription,
                accent = accent,
            )
        }
    }
}

@Composable
private fun CollapsedExerciseStatus(
    isComplete: Boolean,
    upNextDescription: String,
    accent: TabAccent,
) {
    if (isComplete) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = accent.color,
            modifier = Modifier.size(18.dp),
        )
    } else {
        Text(
            text = upNextDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
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
    val exerciseOptionsDescription = stringResource(R.string.training_exercise_options)
    val addSetDescription = stringResource(R.string.training_add_set)
    var optionsOpen by remember { mutableStateOf(false) }
    val warmupSuggestions = warmupSuggestionsFor(block, barWeightKg)
    val rows = formatWorkoutSetRowsForDisplay(block.sets, block.priorBestEstimatedOneRepMaxKg)
    val currentRowIndex = rows.indexOfFirst { !it.set.completed }

    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                block.supersetLabel?.let { SupersetBadge(label = it, accent = accent) }
                Text(
                    text = block.exercise.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = MusFitTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { optionsOpen = true },
                    modifier = Modifier.size(MinimumInteractiveSize),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = exerciseOptionsDescription,
                        tint = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            SetColumnHeaderRow()

            rows.forEachIndexed { index, row ->
                FocusedSetRow(
                    row = row,
                    isCurrent = index == currentRowIndex,
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
                Surface(
                    onClick = { onAddSet(block.exercise.id) },
                    color = accent.container,
                    contentColor = accent.onContainer,
                    shape = RoundedCornerShape(99.dp),
                    modifier = Modifier
                        .heightIn(min = MinimumInteractiveSize)
                        .semantics(mergeDescendants = true) {
                            contentDescription = addSetDescription
                            role = Role.Button
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = stringResource(R.string.training_set),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                plateSummary(block, barWeightKg, availablePlatesKg)?.asString()?.let { plates ->
                    Text(
                        text = plates,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                CoachGlyphButton(onClick = onOpenCoach)
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

/** Shared column geometry for the set table (header + rows must line up). */
private val SetColumnWidth = 48.dp
private val LastColumnWidth = 64.dp
private val CheckButtonSize = 48.dp
private val MinimumInteractiveSize = 48.dp
private val SetTableSingleRowMinWidth = 296.dp

/** SET · LAST · KG · REPS column headers (10a), 10.5/800 +0.6 faint. */
@Composable
private fun SetColumnHeaderRow() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < SetTableSingleRowMinWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SetColumnHeader(stringResource(R.string.training_set), Modifier.width(SetColumnWidth))
                    SetColumnHeader(stringResource(R.string.training_last), Modifier.width(LastColumnWidth))
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(CheckButtonSize))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SetColumnHeader(stringResource(R.string.training_kg), Modifier.weight(1f), TextAlign.Center)
                    SetColumnHeader(stringResource(R.string.training_reps), Modifier.weight(1f), TextAlign.Center)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SetColumnHeader(stringResource(R.string.training_set), Modifier.width(SetColumnWidth))
                SetColumnHeader(stringResource(R.string.training_last), Modifier.width(LastColumnWidth))
                SetColumnHeader(stringResource(R.string.training_kg), Modifier.weight(1f), TextAlign.Center)
                SetColumnHeader(stringResource(R.string.training_reps), Modifier.weight(1f), TextAlign.Center)
                Spacer(modifier = Modifier.width(CheckButtonSize))
            }
        }
    }
}

@Composable
private fun SetColumnHeader(
    label: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = 0.6.sp),
        color = MusFitTheme.colors.onSurfaceFaint,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** The coral coach mark (filled chat bubble + knocked-out sparkle) → chat. */
@Composable
private fun CoachGlyphButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(MinimumInteractiveSize),
    ) {
        Box(modifier = Modifier.size(22.dp)) {
            Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = stringResource(R.string.training_ask_coach_workout),
                tint = BrandCoral,
                modifier = Modifier.size(22.dp),
            )
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MusFitTheme.colors.surface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(10.dp),
            )
        }
    }
}

/**
 * One set row (10a): set number (tap for type/detail options, trophy glyph on a
 * PR), the per-set LAST column (bold ink on the current set), kg/reps fields at
 * 14dp radius (tonal once done, pre-filled placeholders from LAST while
 * pending), and a 42dp check button. RPE, notes, and move/delete live behind
 * the set-number menu.
 */
@Composable
private fun FocusedSetRow(
    row: WorkoutSetRowDisplay,
    isCurrent: Boolean,
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
    val setOptionsDescription = stringResource(R.string.training_set_options, row.setLabel)
    val personalRecordDescription = stringResource(R.string.training_personal_record_lower)
    val weightDescription = stringResource(R.string.training_weight_kilograms)
    val repetitionsDescription = stringResource(R.string.training_repetitions)
    val completionDescription = stringResource(
        if (set.completed) R.string.training_mark_incomplete else R.string.training_mark_complete,
    )
    val setTypeOptions = listOf(
        SET_TYPE_WARMUP to stringResource(R.string.training_set_type_warmup),
        SET_TYPE_WORKING to stringResource(R.string.training_set_type_working),
        SET_TYPE_DROP to stringResource(R.string.training_set_type_drop),
        SET_TYPE_FAILURE to stringResource(R.string.training_set_type_failure),
    )

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

    val previousParts = previousSetParts(row.previousLabel)
    val setOptionsCell: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(SetColumnWidth, CheckButtonSize),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .semantics(mergeDescendants = true) {
                        contentDescription = buildString {
                            append(setOptionsDescription)
                            if (row.isPr) append(", $personalRecordDescription")
                        }
                    }
                    .clickable(role = Role.Button) { setMenuOpen = true },
            ) {
                Text(
                    text = row.setLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                    color = when (row.setLabel) {
                        "W" -> MusFitTheme.colors.macroCarbs
                        "D", "F" -> accent.color
                        else -> MusFitTheme.colors.onSurface
                    },
                )
                if (row.isPr) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = MusFitTheme.colors.warning,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            DropdownMenu(expanded = setMenuOpen, onDismissRequest = { setMenuOpen = false }) {
                setTypeOptions.forEach { (type, label) ->
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
                    text = {
                        Text(
                            stringResource(
                                if (detailsOpen) R.string.training_hide_details else R.string.training_rpe_notes,
                            ),
                        )
                    },
                    onClick = {
                        setMenuOpen = false
                        detailsOpen = !detailsOpen
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.training_move_up)) },
                    onClick = {
                        setMenuOpen = false
                        onMoveSetUp(set.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.training_move_down)) },
                    onClick = {
                        setMenuOpen = false
                        onMoveSetDown(set.id)
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.training_delete_set),
                            color = MusFitTheme.colors.warning,
                        )
                    },
                    onClick = {
                        setMenuOpen = false
                        onDeleteSet(set.id)
                    },
                )
            }
        }
    }
    val previousCell: @Composable () -> Unit = {
        Text(
            text = compactLastLabel(row.previousLabel),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isCurrent) MusFitTheme.colors.onSurface else MusFitTheme.colors.onSurfaceFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(LastColumnWidth),
        )
    }
    val weightField: @Composable (Modifier) -> Unit = { fieldModifier ->
        SetValueField(
            label = weightDescription,
            value = weightKg,
            onValueChange = {
                weightKg = it
                persist(nextWeightKg = it)
            },
            completed = set.completed,
            accent = accent,
            keyboardType = KeyboardType.Decimal,
            placeholder = previousParts?.first,
            modifier = fieldModifier,
        )
    }
    val repsField: @Composable (Modifier) -> Unit = { fieldModifier ->
        SetValueField(
            label = repetitionsDescription,
            value = reps,
            onValueChange = {
                reps = it
                persist(nextReps = it)
            },
            completed = set.completed,
            accent = accent,
            keyboardType = KeyboardType.Number,
            placeholder = previousParts?.second,
            modifier = fieldModifier,
        )
    }
    val completionCell: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(CheckButtonSize)
                .clip(RoundedCornerShape(14.dp))
                .background(if (set.completed) accent.color else MusFitTheme.colors.surfaceVariant)
                .semantics(mergeDescendants = true) {
                    contentDescription = completionDescription
                }
                .clickable(role = Role.Button) { onToggleSet(set.id, !set.completed) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (set.completed) accent.onColor else MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val usesStackedInputs = maxWidth < SetTableSingleRowMinWidth
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (usesStackedInputs) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    setOptionsCell()
                    previousCell()
                    Spacer(modifier = Modifier.weight(1f))
                    completionCell()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    weightField(Modifier.weight(1f))
                    repsField(Modifier.weight(1f))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    setOptionsCell()
                    previousCell()
                    weightField(Modifier.weight(1f))
                    repsField(Modifier.weight(1f))
                    completionCell()
                }
            }
            if (detailsOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (usesStackedInputs) 0.dp else 40.dp),
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
}

/**
 * kg/reps cell (10a): done = accent tonal fill with 14.5/800 accent ink;
 * pending = quiet cream fill whose [placeholder] pre-fills from that set's
 * LAST performance.
 */
@Composable
@Suppress("LongParameterList")
private fun SetValueField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    completed: Boolean,
    accent: TabAccent,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 14.5.sp,
            fontWeight = if (completed) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (completed) accent.onContainer else MusFitTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(if (completed) accent.onContainer else MusFitTheme.colors.onSurface),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (completed) accent.container else MusFitTheme.colors.background)
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isBlank() && !placeholder.isNullOrBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                        fontWeight = FontWeight.Medium,
                        color = MusFitTheme.colors.onSurfaceFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
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
        Surface(
            onClick = { expanded = !expanded },
            color = MusFitTheme.colors.surface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { role = Role.Button },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.Close else Icons.Outlined.Add,
                        contentDescription = null,
                        tint = accent.onContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.training_add_exercise),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = accent.color,
                )
            }
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
                placeholder = { Text(stringResource(R.string.training_search_exercise)) },
                shape = MusFitTheme.shapes.medium,
            )
            suggestions.forEach { exercise ->
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusFitTheme.colors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinimumInteractiveSize)
                        .clickable(role = Role.Button) {
                            onAddExercise(exercise.id)
                            query = ""
                            expanded = false
                        }
                        .padding(vertical = 10.dp),
                )
            }
            if (query.isNotBlank() && suggestions.isEmpty()) {
                Text(
                    text = stringResource(R.string.training_no_matching_exercises),
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
        title = { Text(stringResource(R.string.training_workout_notes)) },
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
                Text(stringResource(R.string.training_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.training_cancel)) }
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
        title = { Text(stringResource(R.string.training_rest_timer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = restTimerSettingsSummaryText(restTimerDefaultSecondsInput).asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MusFitTheme.colors.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = restTimerDefaultSecondsInput,
                    onValueChange = onValueChange,
                    label = { Text(stringResource(R.string.training_seconds_after_set)) },
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
                Text(stringResource(R.string.training_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.training_cancel)) }
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
@Suppress("LongMethod")
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
                ExerciseSheetItem(
                    Icons.Outlined.FitnessCenter,
                    stringResource(R.string.training_add_warmup_sets),
                    MusFitTheme.colors.onSurface,
                ) {
                    onAddWarmupSets()
                    onDismiss()
                }
            }
            if (canMoveUp) {
                ExerciseSheetItem(
                    Icons.Outlined.KeyboardArrowUp,
                    stringResource(R.string.training_move_up),
                    MusFitTheme.colors.onSurface,
                ) {
                    onMoveUp()
                    onDismiss()
                }
            }
            if (canMoveDown) {
                ExerciseSheetItem(
                    Icons.Outlined.KeyboardArrowDown,
                    stringResource(R.string.training_move_down),
                    MusFitTheme.colors.onSurface,
                ) {
                    onMoveDown()
                    onDismiss()
                }
            }
            ExerciseSheetItem(
                Icons.Outlined.SwapHoriz,
                stringResource(R.string.training_replace_exercise),
                MusFitTheme.colors.onSurface,
            ) {
                onReplace()
                onDismiss()
            }
            if (canMakeSuperset) {
                ExerciseSheetItem(
                    Icons.Outlined.Add,
                    stringResource(R.string.training_add_to_superset),
                    MusFitTheme.colors.onSurface,
                ) {
                    onMakeSuperset()
                    onDismiss()
                }
            }
            if (inSuperset && onDissolveSuperset != null) {
                ExerciseSheetItem(
                    Icons.Outlined.Close,
                    stringResource(R.string.training_dissolve_superset),
                    MusFitTheme.colors.onSurface,
                ) {
                    onDissolveSuperset()
                    onDismiss()
                }
            }
            ExerciseSheetItem(
                Icons.Outlined.Delete,
                stringResource(R.string.training_remove_exercise),
                MusFitTheme.colors.warning,
            ) {
                onRemove()
                onDismiss()
            }
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
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
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
                stringResource(R.string.training_replace_exercise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.training_search_exercises)) },
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
                                contentDescription = null,
                                accent = accent,
                                size = 40.dp,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(role = Role.Button) { onPick(exercise.id) },
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
    val rpeDescription = stringResource(R.string.training_rpe)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MusFitTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = rpeDescription },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(MusFitTheme.shapes.small)
                    .background(MusFitTheme.colors.surfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isBlank()) {
                    Text(
                        stringResource(R.string.training_rpe),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clearAndSetSemantics { },
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
    val notesDescription = stringResource(R.string.training_set_notes)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MusFitTheme.colors.onSurface),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = notesDescription },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(MusFitTheme.shapes.small)
                    .background(MusFitTheme.colors.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isBlank()) {
                    Text(
                        stringResource(R.string.training_add_notes_here),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics { },
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
        val normalizedSetType = set.setType.lowercase(Locale.ROOT)
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
internal fun defaultFocusedExerciseId(blocks: List<WorkoutExerciseBlock>): String? = blocks.firstOrNull { block -> block.sets.any { !it.completed } }?.exercise?.id
    ?: blocks.lastOrNull()?.exercise?.id

/** Merged header stat line (mock 5c): "1:12 · 537.5 kg · set 2 of 13". */
internal fun activeWorkoutStatLine(
    elapsedSeconds: Long,
    totalVolumeKg: Double,
    completedSetCount: Int,
    totalSetCount: Int,
): UiText {
    val parts = mutableListOf<UiText>(
        UiText.Verbatim(elapsedSeconds.toElapsedClock()),
        uiText(
            R.string.training_kilograms,
            UiText.Argument.Text(totalVolumeKg.formatKg()),
        ),
    )
    if (totalSetCount > 0) {
        parts += uiText(
            R.string.training_set_position,
            UiText.Argument.Integer((completedSetCount + 1).coerceAtMost(totalSetCount)),
            UiText.Argument.Integer(totalSetCount),
        )
    }
    return parts.joinedWithMiddleDot()
}

/** Up-next trailing target: "3 × 8" from the planned sets, or a plain set count. */
internal fun upNextTarget(block: WorkoutExerciseBlock): UiText {
    val setCount = block.sets.size
    val reps = block.targetReps?.trim()?.takeIf(String::isNotBlank)
    return when {
        setCount == 0 -> uiText(R.string.training_no_sets)

        reps != null -> uiText(
            R.string.training_sets_by_reps,
            UiText.Argument.Integer(setCount),
            UiText.Argument.Text(reps),
        )

        else -> pluralUiText(
            R.plurals.training_set_count,
            setCount,
            UiText.Argument.Integer(setCount),
        )
    }
}

/** LAST column text: "102.5 kg x 5" → "102.5 × 5"; em dash when no history. */
internal fun compactLastLabel(previousLabel: String?): String {
    val label = previousLabel?.trim()?.takeIf(String::isNotBlank) ?: return "—"
    return previousSetParts(label)?.let { (weightKg, reps) -> "$weightKg × $reps" } ?: label
}

/** kg/reps parsed from a previous-set label — feeds pending-row placeholders. */
internal fun previousSetParts(previousLabel: String?): Pair<String, String>? {
    val label = previousLabel?.trim() ?: return null
    val match = PREVIOUS_LABEL_PATTERN.find(label) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}

private val PREVIOUS_LABEL_PATTERN = Regex("""^([\d.,]+)\s*kg\s*[x×]\s*(\d+)$""", RegexOption.IGNORE_CASE)

/** Fraction of the rest ALREADY elapsed — the wavy arc of the countdown ring. */
internal fun restElapsedFraction(restTimer: RestTimerState): Float {
    val duration = restTimer.durationSeconds.coerceAtLeast(1)
    return (1f - restTimer.remainingSeconds.toFloat() / duration).coerceIn(0f, 1f)
}

/** The wave flattens across the final ten seconds of rest (0 = full wave, 1 = flat). */
internal fun restWaveFlatten(remainingSeconds: Int): Float = ((10 - remainingSeconds) / 10f).coerceIn(0f, 1f)

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
): UiText? {
    val plates = block.sets
        .lastOrNull { (it.weightKg ?: 0.0) > 0.0 }
        ?.weightKg
        ?.let { weight -> PlateCalculator.platesPerSide(weight, barWeightKg, availablePlatesKg) }
        .orEmpty()
    return plates.takeIf(List<Double>::isNotEmpty)?.let { available ->
        uiText(
            R.string.training_plates,
            UiText.Argument.Text(available.joinToString(" + ") { it.formatPlate() }),
        )
    }
}

internal fun plateLineText(
    weightKg: Double,
    barWeightKg: Double,
    availablePlatesKg: List<Double>,
): UiText? {
    val plates = PlateCalculator.platesPerSide(weightKg, barWeightKg, availablePlatesKg)
    if (plates.isEmpty()) return null
    return uiText(
        R.string.training_plates_per_side,
        UiText.Argument.Text(plates.joinToString(" + ") { it.formatPlate() }),
    )
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

internal fun restTimerSettingsSummaryText(restTimerDefaultSecondsInput: String): UiText {
    val seconds = restTimerDefaultSecondsInput.trim().takeIf(String::isNotBlank)
    return if (seconds == null) {
        uiText(R.string.training_rest_after_each_set_default)
    } else {
        uiText(
            R.string.training_rest_after_each_set_value,
            UiText.Argument.Text(seconds),
        )
    }
}

internal fun restTimerDisplayText(restTimer: RestTimerState): UiText = when {
    !restTimer.isVisible || restTimer.remainingSeconds <= 0 -> uiText(R.string.training_rest_timer_off)

    restTimer.isRunning -> uiText(
        R.string.training_rest_timer_running,
        UiText.Argument.Nested(restTimer.remainingSeconds.formatDuration()),
    )

    else -> uiText(
        R.string.training_rest_timer_paused,
        UiText.Argument.Nested(restTimer.remainingSeconds.formatDuration()),
    )
}

internal fun restTimerRemainingContentDescription(remainingSeconds: Int): UiText {
    val safeSeconds = remainingSeconds.coerceAtLeast(0)
    return pluralUiText(
        R.plurals.training_rest_seconds_remaining,
        safeSeconds,
        UiText.Argument.Integer(safeSeconds),
    )
}

private fun Double?.formatCompact(): String = when {
    this == null -> ""
    else -> LocalizedFormatter.number(this, grouping = false)
}

private fun Double.formatPlate(): String = LocalizedFormatter.number(this, grouping = false)

private fun Int.toMinSec(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun Int.formatDuration(): UiText {
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) {
        pluralUiText(
            R.plurals.training_duration_minutes_seconds,
            minutes,
            UiText.Argument.Integer(minutes),
            UiText.Argument.Integer(seconds),
        )
    } else {
        pluralUiText(
            R.plurals.training_duration_seconds,
            seconds,
            UiText.Argument.Integer(seconds),
        )
    }
}

private fun Long.toElapsedClock(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

private fun Double.formatKg(): String = LocalizedFormatter.number(this, grouping = false)

private const val SET_TYPE_WARMUP = "warmup"
private const val SET_TYPE_WORKING = "working"
private const val SET_TYPE_DROP = "drop"
private const val SET_TYPE_FAILURE = "failure"
