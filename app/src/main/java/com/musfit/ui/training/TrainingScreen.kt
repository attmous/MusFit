package com.musfit.ui.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.ExerciseDetail
import com.musfit.ui.AppDestination
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.MusFitSegmented
import com.musfit.ui.components.MusFitSummaryCard
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.util.Locale

/** Max exercise rows rendered in the (non-lazy) library list before nudging the user to search. */
private const val EXERCISE_LIST_DISPLAY_LIMIT = 40

@Composable
fun TrainingScreen(viewModel: TrainingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val activeWorkout = state.activeWorkout
    val accent = tabAccentFor(AppDestination.Training)

    if (state.activeWorkoutRouteOpen && activeWorkout != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MusFitTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrainingActiveWorkoutContent(
                workout = activeWorkout,
                exercises = state.exercises,
                restTimer = state.restTimer,
                workoutNotes = state.activeWorkoutNotesInput,
                restTimerDefaultSecondsInput = state.restTimerDefaultSecondsInput,
                barWeightKg = state.trainingSettings.barWeightKg,
                availablePlatesKg = state.trainingSettings.availablePlatesKg,
                accent = accent,
                onAddExercise = viewModel::addExerciseToActiveWorkout,
                onAddSet = viewModel::addWorkoutSet,
                onAddSuggestedWarmupSet = viewModel::addSuggestedWarmupSet,
                onDuplicateSet = viewModel::duplicateLastWorkoutSet,
                onUpdateSet = viewModel::updateWorkoutSetFields,
                onDeleteSet = viewModel::deleteWorkoutSet,
                onToggleSet = viewModel::toggleWorkoutSetCompletion,
                onWorkoutNotesChange = viewModel::onActiveWorkoutNotesChanged,
                onSaveWorkoutNotes = viewModel::saveActiveWorkoutNotes,
                onRestTimerDefaultSecondsChange = viewModel::onRestTimerDefaultSecondsChanged,
                onSaveRestTimer = viewModel::saveRestTimerSettings,
                onMoveSetUp = viewModel::moveWorkoutSetUp,
                onMoveSetDown = viewModel::moveWorkoutSetDown,
                onTickRestTimer = viewModel::tickRestTimer,
                onPauseRestTimer = viewModel::pauseRestTimer,
                onResumeRestTimer = viewModel::resumeRestTimer,
                onSkipRestTimer = viewModel::skipRestTimer,
                onAdjustRestTimer = viewModel::adjustRestTimerSeconds,
                onMakeSuperset = viewModel::makeSupersetWithNext,
                onDissolveSuperset = viewModel::dissolveSuperset,
                onRemoveExercise = viewModel::removeExerciseFromActiveWorkout,
                onMoveExercise = viewModel::moveExerciseInActiveWorkout,
                onReplaceExercise = viewModel::openReplaceExercisePicker,
                replaceExerciseTargetId = state.replaceExerciseTargetId,
                onReplacePick = viewModel::replaceActiveWorkoutExercise,
                onReplaceDismiss = viewModel::closeReplaceExercisePicker,
                onClose = viewModel::closeActiveWorkoutRoute,
                onFinish = viewModel::requestFinishActiveWorkout,
                onDiscard = viewModel::requestDiscardActiveWorkout,
            )
            ActiveWorkoutConfirmationDialogs(
                state = state,
                onConfirmFinish = viewModel::finishActiveWorkout,
                onCancelFinish = viewModel::cancelFinishActiveWorkout,
                onConfirmDiscard = viewModel::discardActiveWorkout,
                onCancelDiscard = viewModel::cancelDiscardActiveWorkout,
            )
        }
        return
    }

    if (state.activeWorkoutRouteOpen) {
        ActiveWorkoutPlaceholder(state = state, onBack = viewModel::closeActiveWorkoutRoute)
        return
    }

    MusFitScreenScaffold(
        title = "Training",
        actions = {
            IconButton(onClick = { viewModel.selectSection(TrainingSection.History) }) {
                Icon(Icons.Outlined.History, contentDescription = "Workout history", tint = MusFitTheme.colors.onSurfaceVariant)
            }
        },
    ) {
        state.activeWorkoutSummary?.let { summary ->
            ResumeBanner(
                title = summary.title,
                subtitle = "${summary.completedSetCount} sets · ${summary.totalVolumeKg.formatKg()} kg",
                accent = accent,
                onResume = viewModel::resumeActiveWorkout,
            )
        }

        TrainingWeekSummaryCard(overview = state.historyOverview, accent = accent)

        SectionTabs(
            selected = state.selectedSection,
            accent = accent,
            onSelect = viewModel::selectSection,
        )

        state.message?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        }

        when (state.selectedSection) {
            TrainingSection.Routines -> {
                val routineDetail = state.selectedRoutineDetail
                val exerciseDetail = state.selectedExerciseDetail
                when {
                    state.routineEditor.isOpen -> TrainingRoutineEditor(
                        editor = state.routineEditor,
                        exercises = state.exercises,
                        accent = accent,
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
                    exerciseDetail != null -> ExerciseDetailPage(
                        detail = exerciseDetail,
                        target = state.exerciseDetailTarget,
                        notesInput = state.exerciseDetailNotesInput,
                        accent = accent,
                        onNotesChange = viewModel::onExerciseDetailNotesChanged,
                        onSaveNotes = viewModel::saveExerciseDetailNotes,
                        onClose = viewModel::closeExerciseDetail,
                    )
                    routineDetail != null -> RoutineDetailContent(
                        detail = routineDetail,
                        accent = accent,
                        onStart = { viewModel.startRoutine(routineDetail.id) },
                        onEdit = { viewModel.openRoutineEditor(routineDetail.id) },
                        onOpenExercise = viewModel::openRoutineExerciseDetail,
                        onDuplicate = {
                            viewModel.closeRoutineDetail()
                            viewModel.duplicateRoutine(routineDetail.id)
                        },
                        onDelete = {
                            viewModel.closeRoutineDetail()
                            viewModel.deleteRoutine(routineDetail.id)
                        },
                        onClose = viewModel::closeRoutineDetail,
                    )
                    else -> TrainingRoutineContent(
                        routines = state.visibleRoutines,
                        programOptions = state.routineProgramOptions,
                        selectedProgram = state.selectedRoutineProgram,
                        accent = accent,
                        onProgramSelected = viewModel::onRoutineProgramFilterChanged,
                        onStartRoutine = viewModel::startRoutine,
                        onEditRoutine = viewModel::openRoutineEditor,
                        onOpenRoutineDetail = viewModel::openRoutineDetail,
                    )
                }
            }
            TrainingSection.Exercises -> {
                val exerciseDetail = state.selectedExerciseDetail
                if (exerciseDetail != null) {
                    ExerciseDetailPage(
                        detail = exerciseDetail,
                        target = state.exerciseDetailTarget,
                        notesInput = state.exerciseDetailNotesInput,
                        accent = accent,
                        onNotesChange = viewModel::onExerciseDetailNotesChanged,
                        onSaveNotes = viewModel::saveExerciseDetailNotes,
                        onClose = viewModel::closeExerciseDetail,
                    )
                } else {
                    TrainingExerciseLibraryContent(
                        visibleExercises = state.visibleExercises,
                        allExercises = state.exercises,
                        searchQuery = state.exerciseSearchQuery,
                        muscleFilter = state.exerciseMuscleFilter,
                        equipmentFilter = state.exerciseEquipmentFilter,
                        editor = state.exerciseEditor,
                        accent = accent,
                        onSearchChange = viewModel::onExerciseSearchQueryChanged,
                        onMuscleFilterChange = viewModel::onExerciseMuscleFilterChanged,
                        onEquipmentFilterChange = viewModel::onExerciseEquipmentFilterChanged,
                        onClearFilters = viewModel::clearExerciseFilters,
                        onOpenExerciseDetail = viewModel::openExerciseDetail,
                        onOpenCustomExercise = viewModel::openCustomExerciseEditor,
                        onCloseCustomExercise = viewModel::closeCustomExerciseEditor,
                        onCustomExerciseNameChange = viewModel::onCustomExerciseNameChanged,
                        onCustomExerciseCategoryChange = viewModel::onCustomExerciseCategoryChanged,
                        onCustomExerciseEquipmentChange = viewModel::onCustomExerciseEquipmentChanged,
                        onCustomExerciseTargetMusclesChange = viewModel::onCustomExerciseTargetMusclesChanged,
                        onSaveCustomExercise = viewModel::saveCustomExercise,
                    )
                }
            }
            TrainingSection.History ->
                TrainingHistoryContent(
                    history = state.workoutHistory,
                    selectedDetail = state.selectedWorkoutDetail,
                    overview = state.historyOverview,
                    accent = accent,
                    onOpenDetail = viewModel::openWorkoutDetail,
                    onCloseDetail = viewModel::closeWorkoutDetail,
                )
            TrainingSection.Progress ->
                TrainingProgressContent(
                    exercises = state.exercises,
                    selectedExerciseId = state.selectedProgressExerciseId,
                    progress = state.selectedExerciseProgress,
                    analytics = state.progressAnalytics,
                    accent = accent,
                    onSelectExercise = viewModel::selectProgressExercise,
                )
        }
    }
}

@Composable
private fun TrainingWeekSummaryCard(overview: TrainingHistoryOverview, accent: TabAccent) {
    MusFitSummaryCard(accent = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent.onContainer,
            )
            if (overview.currentWeekWorkoutCount == 0) {
                Text(
                    text = "No workouts yet — start one whenever you're ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.onContainer,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TrainingWeekStat(
                        value = overview.currentWeekWorkoutCount.toString(),
                        label = if (overview.currentWeekWorkoutCount == 1) "workout" else "workouts",
                        color = accent.onContainer,
                    )
                    TrainingWeekStat(value = "${overview.currentWeekVolumeKg.formatKg()} kg", label = "volume", color = accent.onContainer)
                    TrainingWeekStat(value = overview.currentStreakDays.toString(), label = "day streak", color = accent.onContainer)
                }
            }
        }
    }
}

@Composable
private fun TrainingWeekStat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun ResumeBanner(
    title: String,
    subtitle: String,
    accent: TabAccent,
    onResume: () -> Unit,
) {
    Surface(color = accent.container, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.color), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = accent.onColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accent.onContainer)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = accent.onContainer)
            }
            Button(onClick = onResume, colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor)) {
                Text("Resume")
            }
        }
    }
}

@Composable
private fun SectionTabs(
    selected: TrainingSection,
    accent: TabAccent,
    onSelect: (TrainingSection) -> Unit,
) {
    MusFitSegmented(
        options = TrainingSection.entries,
        selected = selected,
        accent = accent,
        label = { it.name },
        onSelect = onSelect,
    )
}

@Composable
private fun ActiveWorkoutPlaceholder(state: TrainingUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MusFitTheme.colors.background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Active workout", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
        Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.activeWorkoutSummary?.title ?: "Workout in progress", style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                TextButton(onClick = onBack) { Text("Back to routines") }
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
    accent: TabAccent,
    onSearchChange: (String) -> Unit,
    onMuscleFilterChange: (String?) -> Unit,
    onEquipmentFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenCustomExercise: () -> Unit,
    onCloseCustomExercise: () -> Unit,
    onCustomExerciseNameChange: (String) -> Unit,
    onCustomExerciseCategoryChange: (String) -> Unit,
    onCustomExerciseEquipmentChange: (String) -> Unit,
    onCustomExerciseTargetMusclesChange: (String) -> Unit,
    onSaveCustomExercise: () -> Unit,
) {
    val equipmentOptions = allExercises.mapNotNull { it.equipment?.takeIf(String::isNotBlank) }.distinct().sorted()
    val muscleOptions = allExercises
        .flatMap { exercise ->
            listOf(exercise.targetMuscles, exercise.primaryMuscles, exercise.secondaryMuscles)
                .flatMap { muscles -> muscles.split(",") }
        }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = searchQuery, onValueChange = onSearchChange, label = { Text("Search exercises") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
            Button(onClick = onOpenCustomExercise, colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor)) { Text("Custom") }
        }
        FilterChipRow(title = "Equipment", options = equipmentOptions, selected = equipmentFilter, accent = accent, onSelected = onEquipmentFilterChange)
        FilterChipRow(title = "Muscle", options = muscleOptions.take(8), selected = muscleFilter, accent = accent, onSelected = onMuscleFilterChange)
        if (searchQuery.isNotBlank() || equipmentFilter != null || muscleFilter != null) {
            TextButton(onClick = onClearFilters) { Text("Clear filters", color = accent.color) }
        }
        if (editor.isOpen) {
            Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Custom exercise", style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                    OutlinedTextField(value = editor.name, onValueChange = onCustomExerciseNameChange, label = { Text("Name") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editor.category, onValueChange = onCustomExerciseCategoryChange, label = { Text("Category") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = editor.equipment, onValueChange = onCustomExerciseEquipmentChange, label = { Text("Equipment") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = editor.targetMuscles, onValueChange = onCustomExerciseTargetMusclesChange, label = { Text("Target muscles") }, singleLine = true, shape = MusFitTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onSaveCustomExercise, enabled = editor.name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor)) { Text("Save") }
                        TextButton(onClick = onCloseCustomExercise) { Text("Cancel") }
                    }
                }
            }
        }
        Text(text = "${visibleExercises.size} exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MusFitTheme.colors.onSurface)
        if (visibleExercises.isEmpty()) {
            Text(text = "No exercises match these filters.", style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurfaceVariant)
        }
        // The catalog holds ~1,300 exercises; this list is a plain (non-lazy) column, so cap the
        // rendered rows and steer the user to search/filter instead of materialising them all.
        val shownExercises = visibleExercises.take(EXERCISE_LIST_DISPLAY_LIMIT)
        shownExercises.forEach { exercise ->
            Surface(
                color = MusFitTheme.colors.surface,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenExerciseDetail(exercise.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExerciseThumb(imageUrl = exercise.imageUrl, contentDescription = exercise.name, accent = accent, size = 44.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                        Text(
                            text = listOfNotNull(exercise.equipment, exercise.targetMuscles.takeIf(String::isNotBlank), if (exercise.isCustom) "Custom" else "Library").joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MusFitTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (visibleExercises.size > EXERCISE_LIST_DISPLAY_LIMIT) {
            Text(
                text = "Showing ${shownExercises.size} of ${visibleExercises.size}. Search or filter to narrow.",
                style = MaterialTheme.typography.bodySmall,
                color = MusFitTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full-page exercise view: a large demo (animated GIF when available), title with the planned
 * target chip (only when opened from a routine), muscles as chips, instructions, and local notes.
 * Shared by routine exercise rows and the library so there is one clean exercise screen everywhere.
 */
@Composable
private fun ExerciseDetailPage(
    detail: ExerciseDetail,
    target: String?,
    notesInput: String,
    accent: TabAccent,
    onNotesChange: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.clickable(onClick = onClose),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = accent.color,
                modifier = Modifier.size(20.dp),
            )
            Text("Back", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = accent.color)
        }

        val gifUrl = detail.gifUrl
        val imageUrl = detail.imageUrl
        if (!gifUrl.isNullOrBlank()) {
            ExerciseGif(gifUrl = gifUrl, contentDescription = detail.name)
        } else if (!imageUrl.isNullOrBlank()) {
            ExerciseThumb(
                imageUrl = imageUrl,
                contentDescription = detail.name,
                accent = accent,
                size = 200.dp,
                shape = MusFitTheme.shapes.large,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                detail.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!target.isNullOrBlank()) {
                Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        target,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.onContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Text(
            listOfNotNull(
                detail.equipment?.replaceFirstChar { it.titlecase(Locale.US) },
                detail.category.replaceFirstChar { it.titlecase(Locale.US) },
                if (detail.isCustom) "Custom" else "Library",
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )

        MuscleChipRow(label = "Primary", muscles = detail.primaryMuscles, accent = accent, primary = true)
        if (detail.secondaryMuscles.isNotBlank()) {
            MuscleChipRow(label = "Secondary", muscles = detail.secondaryMuscles, accent = accent, primary = false)
        }

        detail.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
            Text(instructions, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurface)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your notes", style = MaterialTheme.typography.labelLarge, color = MusFitTheme.colors.onSurfaceVariant)
            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                placeholder = { Text("Add a cue, weight, or reminder") },
                minLines = 2,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSaveNotes,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text("Save notes")
            }
        }
    }
}

@Composable
private fun MuscleChipRow(label: String, muscles: String, accent: TabAccent, primary: Boolean) {
    val items = muscles.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.replaceFirstChar { ch -> ch.titlecase(Locale.US) } }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { muscle ->
                if (primary) {
                    Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            muscle,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.onContainer,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                        )
                    }
                } else {
                    Surface(
                        color = MusFitTheme.colors.surface,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(0.5.dp, MusFitTheme.colors.outline),
                    ) {
                        Text(
                            muscle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MusFitTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    title: String,
    options: List<String>,
    selected: String?,
    accent: TabAccent,
    onSelected: (String?) -> Unit,
) {
    if (options.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MusFitTheme.colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = selected.equals(option, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(if (isSelected) null else option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.container, selectedLabelColor = accent.onContainer),
                )
            }
        }
    }
}

@Composable
private fun ActiveWorkoutConfirmationDialogs(
    state: TrainingUiState,
    onConfirmFinish: () -> Unit,
    onCancelFinish: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
) {
    val workout = state.activeWorkout
    if (state.finishConfirmationOpen && workout != null) {
        AlertDialog(
            onDismissRequest = onCancelFinish,
            title = { Text("Finish workout?") },
            text = {
                Text(
                    "${workout.title}\n" +
                        "${workout.completedSetCount} sets - ${workout.totalVolumeKg.formatKg()} kg",
                )
            },
            confirmButton = {
                Button(onClick = onConfirmFinish) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelFinish) {
                    Text("Cancel")
                }
            },
        )
    }
    if (state.discardConfirmationOpen && workout != null) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text("Discard workout?") },
            text = { Text("This removes the active workout from your log.") },
            confirmButton = {
                Button(onClick = onConfirmDiscard) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDiscard) {
                    Text("Keep workout")
                }
            },
        )
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
