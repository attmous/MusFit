package com.musfit.ui.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.musfit.data.repository.ExerciseDetail
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.training.RoutineDisplayCalculator
import com.musfit.ui.AppDestination
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.MusFitSegmented
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.expressiveBadgeShapeFor
import com.musfit.ui.components.groupedShape
import com.musfit.ui.components.rowGroupShape
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.tabAccentFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Default weekly training-session target the "This week" strip measures progress against. */
private const val WEEKLY_SESSION_GOAL = 3

@Composable
fun TrainingScreen(
    viewModel: TrainingViewModel = hiltViewModel(),
    onOpenProgress: () -> Unit = {},
    onOpenCoach: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val activeWorkout = state.activeWorkout
    val accent = tabAccentFor(AppDestination.Training)

    // One back handler for the whole miniapp: pop exactly the top page of the Training page
    // stack. Dialogs (finish/discard confirmation, replace-exercise picker) own back themselves,
    // and once the stack is empty back falls through to tab-level navigation in AppNavGraph.
    BackHandler(
        enabled = state.pageStack.isNotEmpty() &&
            !state.finishConfirmationOpen &&
            !state.discardConfirmationOpen &&
            state.replaceExerciseTargetId == null,
    ) {
        viewModel.navigateBack()
    }
    // With the section chips gone, History is a page opened from the calendar icon; back returns
    // to the dashboard instead of leaving the tab.
    BackHandler(enabled = state.pageStack.isEmpty() && state.selectedSection != TrainingSection.Routines) {
        viewModel.selectSection(TrainingSection.Routines)
    }

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
                onOpenCoach = onOpenCoach,
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

    if (state.routineExercisePickerOpen) {
        Dialog(
            onDismissRequest = viewModel::closeRoutineExercisePicker,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MusFitTheme.colors.background,
            ) {
                RoutineExercisePickerPage(
                    exercises = state.exercises,
                    currentRoutineExerciseIds = state.routineEditor.exercises.map { it.exerciseId }.toSet(),
                    selectedExerciseIds = state.routineExercisePickerSelectedIds,
                    searchQuery = state.routineExercisePickerSearchQuery,
                    filters = state.routineExercisePickerFilters,
                    filterSheetOpen = state.routineExercisePickerFilterSheetOpen,
                    loggedExerciseIds = state.loggedExerciseIds,
                    customExerciseEditor = state.exerciseEditor,
                    accent = accent,
                    onSearchChange = viewModel::onRoutineExercisePickerSearchChanged,
                    onOpenFilters = viewModel::openRoutineExercisePickerFilters,
                    onCloseFilters = viewModel::closeRoutineExercisePickerFilters,
                    onToggleEquipment = viewModel::toggleRoutineExercisePickerEquipment,
                    onToggleMuscle = viewModel::toggleRoutineExercisePickerMuscle,
                    onOnlyDoneChange = viewModel::setRoutineExercisePickerOnlyDone,
                    onResetFilters = viewModel::resetRoutineExercisePickerFilters,
                    onClearFilters = viewModel::clearRoutineExercisePickerFilters,
                    onToggleExercise = viewModel::toggleRoutineExercisePickerSelection,
                    onOpenCustomExercise = viewModel::openCustomExerciseEditor,
                    onCloseCustomExercise = viewModel::closeCustomExerciseEditor,
                    onCustomExerciseNameChange = viewModel::onCustomExerciseNameChanged,
                    onCustomExerciseCategoryChange = viewModel::onCustomExerciseCategoryChanged,
                    onCustomExerciseEquipmentChange = viewModel::onCustomExerciseEquipmentChanged,
                    onCustomExerciseTargetMusclesChange = viewModel::onCustomExerciseTargetMusclesChanged,
                    onSaveCustomExercise = viewModel::saveCustomExercise,
                    onCancel = viewModel::closeRoutineExercisePicker,
                    onConfirm = viewModel::confirmRoutineExercisePicker,
                )
            }
        }
        return
    }

    // Full-page overlays layered by the page stack: editor above detail, details above lists.
    if (state.routineEditor.isOpen) {
        TrainingPageContainer {
            TrainingRoutineEditor(
                editor = state.routineEditor,
                exercises = state.exercises,
                accent = accent,
                onNameChange = viewModel::onRoutineNameChanged,
                onNotesChange = viewModel::onRoutineNotesChanged,
                onOpenExercisePicker = viewModel::openRoutineExercisePicker,
                onRemoveExercise = viewModel::removeRoutineExercise,
                onMoveExerciseUp = viewModel::moveRoutineExerciseUp,
                onMoveExerciseDown = viewModel::moveRoutineExerciseDown,
                onTargetSetsChange = viewModel::onRoutineExerciseTargetSetsChanged,
                onTargetRepsChange = viewModel::onRoutineExerciseTargetRepsChanged,
                onRestSecondsChange = viewModel::onRoutineExerciseRestSecondsChanged,
                onAddSet = viewModel::addRoutineExerciseSet,
                onRemoveSet = viewModel::removeRoutineExerciseSet,
                onSetTypeChange = viewModel::onRoutineExerciseSetTypeChanged,
                onSetRepsChange = viewModel::onRoutineExerciseSetRepsChanged,
                onSetWeightChange = viewModel::onRoutineExerciseSetWeightChanged,
                onSave = viewModel::saveRoutineEditor,
                onCancel = viewModel::closeRoutineEditor,
                onDuplicate = viewModel::duplicateRoutine,
                onDelete = viewModel::deleteRoutine,
            )
        }
        return
    }

    val exerciseDetail = state.selectedExerciseDetail
    if (exerciseDetail != null) {
        TrainingPageContainer {
            ExerciseDetailPage(
                detail = exerciseDetail,
                target = state.exerciseDetailTarget,
                notesInput = state.exerciseDetailNotesInput,
                accent = accent,
                onNotesChange = viewModel::onExerciseDetailNotesChanged,
                onSaveNotes = viewModel::saveExerciseDetailNotes,
                onClose = viewModel::closeExerciseDetail,
            )
        }
        return
    }

    val routineDetail = state.selectedRoutineDetail
    if (routineDetail != null) {
        TrainingPageContainer {
            RoutineDetailContent(
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
        }
        return
    }

    if (state.routineLibraryPageOpen) {
        RoutineLibraryPage(state = state, accent = accent, viewModel = viewModel, onBack = viewModel::navigateBack)
        return
    }

    if (state.selectedSection == TrainingSection.History) {
        TrainingHistoryPage(
            state = state,
            accent = accent,
            onBack = { viewModel.selectSection(TrainingSection.Routines) },
            onOpenDetail = viewModel::openWorkoutDetail,
            onCloseDetail = viewModel::closeWorkoutDetail,
        )
        return
    }

    TrainingDashboard(
        state = state,
        accent = accent,
        onOpenHistory = { viewModel.selectSection(TrainingSection.History) },
        onResume = viewModel::resumeActiveWorkout,
        onDiscardActiveWorkout = viewModel::requestDiscardActiveWorkout,
        onStartRoutine = viewModel::startRoutine,
        onStartBlankWorkout = viewModel::startBlankWorkout,
        onOpenRoutineDetail = viewModel::openRoutineDetail,
        onOpenAllRoutines = viewModel::openRoutineLibraryPage,
        onNewRoutine = { viewModel.openRoutineEditor(null) },
        onOpenProgress = onOpenProgress,
        onOpenCoach = onOpenCoach,
    )
    // The resume hero's split-button menu can request a discard from the
    // dashboard, so the confirmation dialogs live here too.
    ActiveWorkoutConfirmationDialogs(
        state = state,
        onConfirmFinish = viewModel::finishActiveWorkout,
        onCancelFinish = viewModel::cancelFinishActiveWorkout,
        onConfirmDiscard = viewModel::discardActiveWorkout,
        onCancelDiscard = viewModel::cancelDiscardActiveWorkout,
    )
}

/** The dashboard's connected-button-group destinations (mock 6c). */
private enum class TrainingDashboardSegment(val label: String) {
    Routines("Routines"),
    History("History"),
    Progress("Progress"),
}

/**
 * The M3 Expressive Training tab (mock 6c): the Resume hero (only while a
 * workout is running) with a split button, a three-cell stats row, the
 * connected button group, a filled start-workout action row, and the routine
 * list as grouped white rows with alternating expressive badges.
 */
@Composable
private fun TrainingDashboard(
    state: TrainingUiState,
    accent: TabAccent,
    onOpenHistory: () -> Unit,
    onResume: () -> Unit,
    onDiscardActiveWorkout: () -> Unit,
    onStartRoutine: (String) -> Unit,
    onStartBlankWorkout: () -> Unit,
    onOpenRoutineDetail: (String) -> Unit,
    onOpenAllRoutines: () -> Unit,
    onNewRoutine: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCoach: () -> Unit,
) {
    MusFitScreenScaffold(
        title = "Training",
        actions = {
            TonalHeaderIconButton(
                icon = Icons.Outlined.History,
                contentDescription = "Workout history",
                onClick = onOpenHistory,
            )
        },
    ) {
        state.activeWorkoutSummary?.let { summary ->
            TrainingResumeHero(
                summary = summary,
                accent = accent,
                onResume = onResume,
                onDiscard = onDiscardActiveWorkout,
            )
        }

        TrainingStatsRow(overview = state.historyOverview)

        MusFitSegmented(
            options = TrainingDashboardSegment.entries,
            selected = TrainingDashboardSegment.Routines,
            accent = accent,
            label = { it.label },
            onSelect = { segment ->
                when (segment) {
                    TrainingDashboardSegment.Routines -> Unit
                    TrainingDashboardSegment.History -> onOpenHistory()
                    TrainingDashboardSegment.Progress -> onOpenProgress()
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onStartBlankWorkout,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = "Start empty workout",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.5.sp),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            TextButton(onClick = onNewRoutine) {
                Text(
                    text = "New routine",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent.color,
                )
            }
        }

        DashboardRoutineList(
            routines = state.homeRoutines.ifEmpty { state.visibleRoutines },
            history = state.workoutHistory,
            accent = accent,
            onOpenRoutineDetail = onOpenRoutineDetail,
            onStartRoutine = onStartRoutine,
            onOpenAllRoutines = onOpenAllRoutines,
        )

        TrainingCoachRow(
            cue = trainingCoachCue(
                overview = state.historyOverview,
                nextRoutineName = (state.dashboard.nextSuggestedRoutine ?: state.visibleRoutines.firstOrNull())?.name,
            ),
            onView = onOpenCoach,
        )

        state.message?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
        }
    }
}

/**
 * The Resume hero (mock 6c): indigo tonal container, emphasized overline/title,
 * and an M3E split button — the leading segment resumes, the trailing chevron
 * opens a menu with Discard and View details.
 */
@Composable
private fun TrainingResumeHero(
    summary: com.musfit.data.repository.ActiveWorkoutSummary,
    accent: TabAccent,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    val sets = summary.completedSetCount
    val meta = "$sets ${if (sets == 1) "set" else "sets"} · ${summary.totalVolumeKg.formatKg()} kg"
    Surface(color = accent.container, shape = MusFitTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IN PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.onContainer,
                )
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent.onContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent.onContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            TrainingResumeSplitButton(accent = accent, onResume = onResume, onDiscard = onDiscard)
        }
    }
}

/** M3E split button: pill-edged leading action + 2dp gap + chevron menu segment. */
@Composable
private fun TrainingResumeSplitButton(
    accent: TabAccent,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(
            onClick = onResume,
            color = accent.color,
            contentColor = accent.onColor,
            shape = RoundedCornerShape(topStart = 99.dp, bottomStart = 99.dp, topEnd = 8.dp, bottomEnd = 8.dp),
        ) {
            Text(
                text = "Resume",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
        Box {
            Surface(
                onClick = { menuOpen = true },
                color = accent.color,
                contentColor = accent.onColor,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 99.dp, bottomEnd = 99.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 13.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = "Workout options",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("View details") }, onClick = { menuOpen = false; onResume() })
                DropdownMenuItem(text = { Text("Discard") }, onClick = { menuOpen = false; onDiscard() })
            }
        }
    }
}

/** Three white stat cells with grouped corners: workouts · volume · streak. */
@Composable
private fun TrainingStatsRow(overview: TrainingHistoryOverview) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val volume = overview.currentWeekVolumeKg
        val volumeFigure = if (volume >= 1000.0) {
            String.format(Locale.US, "%.1f t", volume / 1000.0)
        } else {
            "${volume.formatKg()} kg"
        }
        val cells = listOf(
            "${overview.currentWeekWorkoutCount}" to
                (if (overview.currentWeekWorkoutCount == 1) "workout" else "workouts"),
            volumeFigure to "volume",
            "${overview.currentStreakDays}" to
                (if (overview.currentStreakDays == 1) "day streak" else "days streak"),
        )
        cells.forEachIndexed { index, (figure, label) ->
            Surface(
                color = MusFitTheme.colors.surface,
                shape = rowGroupShape(index, cells.size),
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = figure,
                        style = MaterialTheme.typography.displaySmall,
                        color = MusFitTheme.colors.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MusFitTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Routine rows as an M3E grouped list: alternating sunny/circle/squircle badge,
 * emphasized title, one meta line, and a tonal play button.
 */
@Composable
private fun DashboardRoutineList(
    routines: List<RoutineSummary>,
    history: List<WorkoutHistorySummary>,
    accent: TabAccent,
    onOpenRoutineDetail: (String) -> Unit,
    onStartRoutine: (String) -> Unit,
    onOpenAllRoutines: () -> Unit,
) {
    Column {
        SectionHeader(
            title = "Routines",
            trailingActionLabel = "All",
            trailingActionColor = accent.color,
            onTrailingAction = onOpenAllRoutines,
        )
        if (routines.isEmpty()) {
            Text(
                text = "Create a routine and it will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MusFitTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            routines.forEachIndexed { index, routine ->
                Surface(
                    onClick = { onOpenRoutineDetail(routine.id) },
                    color = MusFitTheme.colors.surface,
                    shape = groupedShape(index, routines.size),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExpressiveBadge(
                            icon = Icons.Filled.FitnessCenter,
                            shape = expressiveBadgeShapeFor(index),
                            containerColor = accent.container,
                            contentColor = accent.onContainer,
                            size = 46.dp,
                            iconSize = 20.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MusFitTheme.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = routineLastPerformedMeta(routine, history, LocalDate.now()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MusFitTheme.colors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        Surface(
                            onClick = { onStartRoutine(routine.id) },
                            color = accent.container,
                            contentColor = accent.onContainer,
                            shape = if (index % 2 == 0) CircleShape else RoundedCornerShape(16.dp),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Start ${routine.name}",
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The coach shrunk to a hairline row: 7dp azure dot, one sentence, one text action. */
@Composable
private fun TrainingCoachRow(cue: String, onView: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(MusFitTheme.colors.accent, CircleShape),
        )
        Text(
            text = cue,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "View",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MusFitTheme.colors.accent,
            modifier = Modifier.clickable(onClick = onView),
        )
    }
}

/** Full-page History (opened from the calendar icon): back header + the existing history content. */
@Composable
private fun TrainingHistoryPage(
    state: TrainingUiState,
    accent: TabAccent,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onCloseDetail: () -> Unit,
) {
    TrainingPageContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MusFitTheme.colors.onSurface,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
            )
        }
        TrainingHistoryContent(
            history = state.workoutHistory,
            selectedDetail = state.selectedWorkoutDetail,
            overview = state.historyOverview,
            accent = accent,
            onOpenDetail = onOpenDetail,
            onCloseDetail = onCloseDetail,
        )
    }
}

/**
 * "All routines" page behind the dashboard's "All" link: the full management
 * workspace — user routines organized into folders (drag to move), plus the
 * pre-made routine library.
 */
@Composable
private fun RoutineLibraryPage(
    state: TrainingUiState,
    accent: TabAccent,
    viewModel: TrainingViewModel,
    onBack: () -> Unit,
) {
    TrainingPageContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MusFitTheme.colors.onSurface,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Text(
                text = "Routines",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
            )
        }
        TrainingHomeContent(
            hasActiveWorkout = state.activeWorkoutSummary != null,
            routines = state.homeRoutines,
            folders = state.homeFolders,
            folderEditor = state.routineFolderEditor,
            accent = accent,
            onStartBlankWorkout = viewModel::startBlankWorkout,
            onNewRoutine = { viewModel.openRoutineEditor(null) },
            onOpenLibrary = {},
            showLibraryLink = false,
            onOpenFolderEditor = viewModel::openRoutineFolderEditor,
            onFolderNameChange = viewModel::onRoutineFolderNameChanged,
            onSaveFolder = viewModel::saveRoutineFolderEditor,
            onCancelFolder = viewModel::closeRoutineFolderEditor,
            onDeleteFolder = viewModel::deleteRoutineFolder,
            onAssignRoutineToFolder = viewModel::assignRoutineToFolder,
            onStartRoutine = viewModel::startRoutine,
            onEditRoutine = viewModel::openRoutineEditor,
            onOpenRoutineDetail = viewModel::openRoutineDetail,
        )
        TrainingRoutineLibraryList(
            routines = state.visibleRoutines,
            accent = accent,
            onStartRoutine = viewModel::startRoutine,
            onOpenRoutineDetail = viewModel::openRoutineDetail,
        )
    }
}

/** Shared scrolling container for Training's full-screen inner pages. */
@Composable
internal fun TrainingPageContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(start = MusFitTheme.spacing.xl, end = MusFitTheme.spacing.xl, top = MusFitTheme.spacing.lg)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.xl),
    ) {
        content()
    }
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
                TextButton(onClick = onBack) { Text("Back to home") }
            }
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
        var gifUnavailable by remember(gifUrl) { mutableStateOf(false) }
        if (!gifUrl.isNullOrBlank() && !gifUnavailable) {
            ExerciseGif(
                gifUrl = gifUrl,
                contentDescription = detail.name,
                accent = accent,
                onError = { gifUnavailable = true },
            )
        } else if (!imageUrl.isNullOrBlank()) {
            ExerciseThumb(
                imageUrl = imageUrl,
                contentDescription = detail.name,
                accent = accent,
                size = 200.dp,
                shape = MusFitTheme.shapes.large,
            )
        } else {
            ExerciseThumb(
                imageUrl = null,
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
                fontWeight = FontWeight.Normal,
                color = MusFitTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!target.isNullOrBlank()) {
                Surface(color = accent.container, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        target,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
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
                    // Neutral chip: quiet cool fill, no border chrome.
                    Surface(
                        color = MusFitTheme.colors.surfaceVariant,
                        shape = RoundedCornerShape(999.dp),
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

// --- Dashboard display helpers (pure, unit-tested) ---

internal data class TrainingWeekDay(
    val label: String,
    val isDone: Boolean,
    val isToday: Boolean,
)

/** Mon-Sun dots for the current week: a day is done when any workout was started on it. */
internal fun trainingWeekDays(
    history: List<WorkoutHistorySummary>,
    today: LocalDate,
): List<TrainingWeekDay> {
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val trainedDays = history
        .map { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSet()
    return (0..6).map { offset ->
        val date = startOfWeek.plusDays(offset.toLong())
        TrainingWeekDay(
            label = date.format(DateTimeFormatter.ofPattern("EEE", Locale.US)),
            isDone = date in trainedDays,
            isToday = date == today,
        )
    }
}

/** Hero overline: "TODAY · QUADS DAY" from the routine's lead muscle group, else just "TODAY". */
internal fun trainingHeroOverline(routine: RoutineSummary): String {
    val muscle = routine.muscleGroups.firstOrNull()?.trim()?.takeIf(String::isNotBlank)
    return if (muscle == null) "TODAY" else "TODAY · ${muscle.uppercase(Locale.US)} DAY"
}

internal fun trainingHeroMeta(routine: RoutineSummary): String {
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(routine.targetSetCount)
    return buildString {
        append("${routine.exerciseCount} ${if (routine.exerciseCount == 1) "exercise" else "exercises"}")
        if (estimatedMinutes > 0) append(" · ~$estimatedMinutes min")
    }
}

/**
 * One meta line per routine row: when the routine has been run (matched by session title),
 * "last: Wed" style recency; otherwise fall back to the size summary.
 */
internal fun routineLastPerformedMeta(
    routine: RoutineSummary,
    history: List<WorkoutHistorySummary>,
    today: LocalDate,
): String {
    val lastRun = history
        .filter { it.title.equals(routine.name, ignoreCase = true) }
        .maxByOrNull { it.startedAtEpochMillis }
        ?.let { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
    if (lastRun != null) {
        return "last: ${lastRun.recencyLabel(today)}"
    }
    return trainingHeroMeta(routine)
}

private fun LocalDate.recencyLabel(today: LocalDate): String = when {
    this == today -> "today"
    this == today.minusDays(1) -> "yesterday"
    !isBefore(today.minusDays(6)) -> format(DateTimeFormatter.ofPattern("EEE", Locale.US))
    else -> format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
}

/** One deterministic coaching sentence for the hairline coach row — data-derived, no prose engine. */
internal fun trainingCoachCue(
    overview: TrainingHistoryOverview,
    nextRoutineName: String?,
): String {
    val done = overview.currentWeekWorkoutCount
    return when {
        done >= WEEKLY_SESSION_GOAL ->
            "Weekly goal done — $done of $WEEKLY_SESSION_GOAL sessions. Recovery counts too."
        done == 0 ->
            if (nextRoutineName != null) {
                "No sessions yet this week — $nextRoutineName would be a good start."
            } else {
                "No sessions yet this week — start with an empty workout."
            }
        else ->
            if (nextRoutineName != null) {
                "$done of $WEEKLY_SESSION_GOAL sessions this week — $nextRoutineName is up next."
            } else {
                "$done of $WEEKLY_SESSION_GOAL sessions this week — one more keeps the plan on track."
            }
    }
}

private fun Double.formatKg(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
