package com.musfit.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musfit.data.repository.ExerciseDetail
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.training.RoutineDisplayCalculator
import com.musfit.feature.training.R
import com.musfit.ui.components.ExpressiveBadge
import com.musfit.ui.components.ExpressiveBadgeShape
import com.musfit.ui.components.InnerScreenHeader
import com.musfit.ui.components.MusFitScreenScaffold
import com.musfit.ui.components.MusFitSegmented
import com.musfit.ui.components.SectionHeader
import com.musfit.ui.components.TonalHeaderIconButton
import com.musfit.ui.components.expressiveBadgeShapeFor
import com.musfit.ui.components.groupedShape
import com.musfit.ui.icons.filled.ExpandMore
import com.musfit.ui.icons.filled.FitnessCenter
import com.musfit.ui.icons.outlined.CalendarMonth
import com.musfit.ui.icons.outlined.History
import com.musfit.ui.text.LocalizedFormatter
import com.musfit.ui.text.UiText
import com.musfit.ui.text.asString
import com.musfit.ui.text.pluralUiText
import com.musfit.ui.text.uiText
import com.musfit.ui.theme.MusFitTheme
import com.musfit.ui.theme.TabAccent
import com.musfit.ui.theme.TabAccentRole
import com.musfit.ui.theme.tabAccentFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TrainingScreen(
    routeKey: TrainingNavKey = TrainingHomeNavKey,
    navigation: TrainingNavigationActions = TrainingNavigationActions(),
    viewModel: TrainingViewModel = hiltViewModel(),
    onOpenCoach: () -> Unit = {},
) {
    when (routeKey) {
        TrainingActiveWorkoutNavKey,
        TrainingHistoryNavKey,
        is TrainingWorkoutHistoryDetailNavKey,
        -> {
            val state by viewModel.activeHistoryState.collectAsStateWithLifecycle()
            TrainingProjectedSurface(
                routeKey = routeKey,
                state = state.content,
                viewModel = viewModel,
                navigation = navigation,
                onOpenCoach = onOpenCoach,
            )
        }

        else -> {
            val state by viewModel.routinesLibraryState.collectAsStateWithLifecycle()
            TrainingProjectedSurface(
                routeKey = routeKey,
                state = state.content,
                viewModel = viewModel,
                navigation = navigation,
                onOpenCoach = onOpenCoach,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("LongMethod", "ReturnCount", "CyclomaticComplexMethod", "ComplexCondition")
private fun TrainingProjectedSurface(
    routeKey: TrainingNavKey,
    state: TrainingUiState,
    viewModel: TrainingViewModel,
    navigation: TrainingNavigationActions,
    onOpenCoach: () -> Unit,
) {
    val activeWorkout = state.activeWorkout
    val accent = tabAccentFor(TabAccentRole.Training)
    // The strategy opts out of single-pane layouts, so this local is non-null only while this
    // destination is actually rendered beside another Training pane.
    val isExpandedPane = LocalListDetailSceneScope.current != null
    val finishActiveWorkout = {
        viewModel.finishActiveWorkout { sessionId ->
            navigation.resetTo(
                buildList {
                    add(TrainingHistoryNavKey)
                    if (sessionId != null) add(TrainingWorkoutHistoryDetailNavKey(sessionId))
                },
            )
        }
    }
    val discardActiveWorkout = {
        viewModel.discardActiveWorkout { navigation.resetTo(emptyList()) }
    }

    if (routeKey == TrainingActiveWorkoutNavKey && activeWorkout != null) {
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
                onClose = navigation.back,
                onFinish = viewModel::requestFinishActiveWorkout,
                onDiscard = viewModel::requestDiscardActiveWorkout,
            )
            ActiveWorkoutConfirmationDialogs(
                state = state,
                onConfirmFinish = finishActiveWorkout,
                onCancelFinish = viewModel::cancelFinishActiveWorkout,
                onConfirmDiscard = discardActiveWorkout,
                onCancelDiscard = viewModel::cancelDiscardActiveWorkout,
            )
        }
        return
    }

    if (routeKey == TrainingActiveWorkoutNavKey) {
        ActiveWorkoutPlaceholder(state = state, onBack = navigation.back)
        return
    }

    if (routeKey == TrainingExercisePickerNavKey) {
        Dialog(
            onDismissRequest = navigation.back,
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
                    onCancel = navigation.back,
                    onConfirm = {
                        viewModel.confirmRoutineExercisePicker()
                        navigation.back()
                    },
                )
            }
        }
        return
    }

    if (
        routeKey is TrainingRoutineEditorNavKey &&
        state.routineEditor.isOpen &&
        state.routineEditor.routineId == routeKey.routineId
    ) {
        TrainingPageContainer {
            TrainingRoutineEditor(
                editor = state.routineEditor,
                exercises = state.exercises,
                accent = accent,
                onNameChange = viewModel::onRoutineNameChanged,
                onNotesChange = viewModel::onRoutineNotesChanged,
                onOpenExercisePicker = {
                    viewModel.openRoutineExercisePicker()
                    navigation.open(TrainingExercisePickerNavKey)
                },
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
                onSave = {
                    viewModel.saveRoutineEditor(navigation.back)
                },
                onCancel = navigation.back,
                onDuplicate = viewModel::duplicateRoutine,
                onDelete = { routineId ->
                    viewModel.deleteRoutine(routineId, navigation.back)
                },
            )
        }
        return
    }
    if (routeKey is TrainingRoutineEditorNavKey) {
        TrainingRoutePlaceholder(
            uiText(R.string.training_routine_editor),
            state.message ?: uiText(R.string.training_loading_routine),
            navigation.back,
        )
        return
    }

    val exerciseDetail = state.selectedExerciseDetail
    if (
        routeKey is TrainingExerciseDetailNavKey &&
        exerciseDetail != null &&
        exerciseDetail.id == routeKey.exerciseId &&
        state.exerciseDetailTarget == routeKey.target
    ) {
        TrainingPageContainer {
            ExerciseDetailPage(
                detail = exerciseDetail,
                target = state.exerciseDetailTarget,
                notesInput = state.exerciseDetailNotesInput,
                accent = accent,
                onNotesChange = viewModel::onExerciseDetailNotesChanged,
                onSaveNotes = viewModel::saveExerciseDetailNotes,
                onClose = navigation.back,
                showBackAction = !isExpandedPane,
            )
        }
        return
    }
    if (routeKey is TrainingExerciseDetailNavKey) {
        TrainingRoutePlaceholder(
            uiText(R.string.training_exercise),
            state.message ?: uiText(R.string.training_loading_exercise),
            navigation.back,
        )
        return
    }

    val routineDetail = state.selectedRoutineDetail
    if (
        routeKey is TrainingRoutineDetailNavKey &&
        routineDetail != null &&
        routineDetail.id == routeKey.routineId
    ) {
        TrainingPageContainer {
            RoutineDetailContent(
                detail = routineDetail,
                accent = accent,
                onStart = {
                    viewModel.startRoutine(routineDetail.id) {
                        navigation.open(TrainingActiveWorkoutNavKey)
                    }
                },
                onEdit = {
                    navigation.open(TrainingRoutineEditorNavKey(routineDetail.id))
                },
                onOpenExercise = { exerciseId, target ->
                    navigation.open(TrainingExerciseDetailNavKey(exerciseId, target))
                },
                onDuplicate = {
                    viewModel.closeRoutineDetail()
                    viewModel.duplicateRoutine(routineDetail.id)
                    navigation.back()
                },
                onDelete = {
                    viewModel.deleteRoutine(routineDetail.id, navigation.back)
                },
                onClose = navigation.back,
                showBackAction = !isExpandedPane,
            )
        }
        return
    }
    if (routeKey is TrainingRoutineDetailNavKey) {
        TrainingRoutePlaceholder(
            uiText(R.string.training_routine),
            state.message ?: uiText(R.string.training_loading_routine),
            navigation.back,
        )
        return
    }

    if (routeKey == TrainingRoutineLibraryNavKey) {
        RoutineLibraryPage(
            state = state,
            accent = accent,
            viewModel = viewModel,
            navigation = navigation,
            onBack = { navigation.popThrough(TrainingRoutineLibraryNavKey) },
        )
        return
    }

    val workoutDetail = state.selectedWorkoutDetail
    if (
        routeKey is TrainingWorkoutHistoryDetailNavKey &&
        workoutDetail?.summary?.sessionId != routeKey.sessionId
    ) {
        TrainingRoutePlaceholder(
            uiText(R.string.training_workout),
            state.message ?: uiText(R.string.training_loading_workout),
            navigation.back,
        )
        return
    }

    if (routeKey is TrainingWorkoutHistoryDetailNavKey) {
        TrainingWorkoutHistoryDetailPage(
            detail = requireNotNull(workoutDetail),
            accent = accent,
            onClose = navigation.back,
            onOpenCoach = onOpenCoach,
            showCloseAction = !isExpandedPane,
        )
        return
    }

    if (routeKey == TrainingHistoryNavKey) {
        TrainingHistoryListPage(
            state = state,
            accent = accent,
            onBack = { navigation.popThrough(TrainingHistoryNavKey) },
            onOpenDetail = { sessionId ->
                navigation.open(TrainingWorkoutHistoryDetailNavKey(sessionId))
            },
        )
        return
    }

    TrainingDashboard(
        state = state,
        accent = accent,
        onOpenHistory = { navigation.open(TrainingHistoryNavKey) },
        onResume = {
            navigation.open(TrainingActiveWorkoutNavKey)
        },
        onDiscardActiveWorkout = viewModel::requestDiscardActiveWorkout,
        onStartRoutine = { routineId ->
            viewModel.startRoutine(routineId) { navigation.open(TrainingActiveWorkoutNavKey) }
        },
        onStartBlankWorkout = {
            viewModel.startBlankWorkout { navigation.open(TrainingActiveWorkoutNavKey) }
        },
        onOpenRoutineDetail = { routineId ->
            navigation.open(TrainingRoutineDetailNavKey(routineId))
        },
        onOpenAllRoutines = {
            navigation.open(TrainingRoutineLibraryNavKey)
        },
        onNewRoutine = {
            navigation.open(TrainingRoutineEditorNavKey())
        },
        onOpenProgress = { navigation.open(TrainingProgressFeatureNavKey) },
        onOpenCoach = onOpenCoach,
    )
    // The resume hero's split-button menu can request a discard from the
    // dashboard, so the confirmation dialogs live here too.
    ActiveWorkoutConfirmationDialogs(
        state = state,
        onConfirmFinish = finishActiveWorkout,
        onCancelFinish = viewModel::cancelFinishActiveWorkout,
        onConfirmDiscard = discardActiveWorkout,
        onCancelDiscard = viewModel::cancelDiscardActiveWorkout,
    )
}

/** The dashboard's connected-button-group destinations (mock 6c). */
private enum class TrainingDashboardSegment(val labelResource: Int) {
    Routines(R.string.training_routines),
    History(R.string.training_history),
    Progress(R.string.training_progress),
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
    val segmentLabels = TrainingDashboardSegment.entries.associateWith { segment ->
        stringResource(segment.labelResource)
    }
    val locale = LocalConfiguration.current.locales[0]
    MusFitScreenScaffold(
        title = stringResource(R.string.training_title),
        actions = {
            TonalHeaderIconButton(
                icon = Icons.Outlined.History,
                contentDescription = stringResource(R.string.training_workout_history),
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

        TrainingWeekCard(
            overview = state.historyOverview,
            days = trainingWeekDays(
                history = state.workoutHistory,
                today = LocalDate.now(),
                weeklyTarget = state.weeklySessionTarget,
                locale = locale,
            ),
            weeklyTarget = state.weeklySessionTarget,
            accent = accent,
            locale = locale,
        )

        MusFitSegmented(
            options = TrainingDashboardSegment.entries,
            selected = TrainingDashboardSegment.Routines,
            accent = accent,
            label = segmentLabels::getValue,
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
                    text = stringResource(R.string.training_start_empty_workout),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.5.sp),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            TextButton(onClick = onNewRoutine) {
                Text(
                    text = stringResource(R.string.training_new_routine),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent.color,
                )
            }
        }

        DashboardRoutineList(
            routines = state.homeRoutines.ifEmpty { state.visibleRoutines }.take(DASHBOARD_ROUTINE_PREVIEW_LIMIT),
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
                weeklyTarget = state.weeklySessionTarget,
            ),
            onView = onOpenCoach,
        )

        state.message?.let { message ->
            Text(text = message.asString(), style = MaterialTheme.typography.bodySmall, color = MusFitTheme.colors.onSurfaceVariant)
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
    val locale = LocalConfiguration.current.locales[0]
    val meta = pluralStringResource(
        R.plurals.training_sets_volume,
        sets,
        sets,
        summary.totalVolumeKg.formatKg(locale),
    )
    Surface(color = accent.container, shape = MusFitTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.training_in_progress).uppercase(locale),
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
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { role = Role.Button },
        ) {
            Text(
                text = stringResource(R.string.training_resume),
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
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.Button },
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 13.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.training_workout_options),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.training_view_details)) }, onClick = {
                    menuOpen = false
                    onResume()
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.training_discard)) }, onClick = {
                    menuOpen = false
                    onDiscard()
                })
            }
        }
    }
}

/**
 * The Turn 8 "This week" card (Delta 3): a sessions/volume header over seven
 * day circles — completed sessions as indigo sunny checks, the next planned
 * session as a dashed indigo circle, rest days as quiet tonal dots. Replaces
 * the stat cells; a day streak is not a meaningful strength metric.
 */
@Composable
private fun TrainingWeekCard(
    overview: TrainingHistoryOverview,
    days: List<TrainingWeekDay>,
    weeklyTarget: Int,
    accent: TabAccent,
    locale: Locale,
) {
    Surface(
        color = MusFitTheme.colors.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.training_this_week),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MusFitTheme.colors.onSurface,
                )
                Text(
                    text = trainingWeekHeaderMeta(overview, weeklyTarget, locale),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MusFitTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                days.forEach { day -> TrainingWeekDayCircle(day = day, accent = accent) }
            }
        }
    }
}

/** "2 of 3 sessions · 3.8 t volume" with the numbers in emphasized ink. */
@Composable
private fun trainingWeekHeaderMeta(
    overview: TrainingHistoryOverview,
    weeklyTarget: Int,
    locale: Locale,
): androidx.compose.ui.text.AnnotatedString {
    val sessionCount = if (weeklyTarget > 0) {
        stringResource(
            R.string.training_week_sessions_target,
            overview.currentWeekWorkoutCount,
            weeklyTarget,
        )
    } else {
        stringResource(R.string.training_week_sessions_count, overview.currentWeekWorkoutCount)
    }
    val sessionUnit = pluralStringResource(R.plurals.training_session_unit, overview.currentWeekWorkoutCount)
    val volumeFigure = trainingWeekVolumeFigure(overview.currentWeekVolumeKg, locale).asString()
    val volumeLabel = stringResource(R.string.training_volume)
    return buildAnnotatedString {
        val emphasized = SpanStyle(fontWeight = FontWeight.ExtraBold, color = MusFitTheme.colors.onSurface)
        withStyle(emphasized) { append(sessionCount) }
        append(" ")
        append(sessionUnit)
        withStyle(emphasized) { append(volumeFigure) }
        append(" ")
        append(volumeLabel)
    }
}

internal fun trainingWeekVolumeFigure(
    volumeKg: Double,
    locale: Locale = Locale.getDefault(),
): UiText = if (volumeKg >= 1000.0) {
    uiText(
        R.string.training_tonnes,
        UiText.Argument.Text(LocalizedFormatter.number(volumeKg / 1000.0, maximumFractionDigits = 1, locale = locale)),
    )
} else {
    uiText(R.string.training_kilograms, UiText.Argument.Text(volumeKg.formatKg(locale)))
}

@Composable
private fun TrainingWeekDayCircle(day: TrainingWeekDay, accent: TabAccent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            day.isDone -> ExpressiveBadge(
                icon = Icons.Filled.Check,
                shape = ExpressiveBadgeShape.Sunny,
                containerColor = accent.color,
                contentColor = accent.onColor,
                size = 36.dp,
                iconSize = 16.dp,
            )

            day.isPlanned -> {
                val dashColor = accent.color
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .drawBehind {
                            val stroke = 2.dp.toPx()
                            val dash = 5.dp.toPx()
                            drawCircle(
                                color = dashColor,
                                radius = (size.minDimension - stroke) / 2f,
                                style = Stroke(
                                    width = stroke,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                                ),
                            )
                        },
                )
            }

            else -> Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MusFitTheme.colors.surfaceVariant, CircleShape),
            )
        }
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            fontWeight = if (day.isPlanned) FontWeight.ExtraBold else FontWeight.Normal,
            color = when {
                day.isPlanned -> accent.onContainer
                day.isDone -> MusFitTheme.colors.onSurfaceVariant
                else -> MusFitTheme.colors.onSurfaceFaint
            },
            maxLines = 1,
        )
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
            title = stringResource(R.string.training_routines),
            trailingActionLabel = stringResource(R.string.training_all),
            trailingActionColor = accent.color,
            onTrailingAction = onOpenAllRoutines,
        )
        if (routines.isEmpty()) {
            Text(
                text = stringResource(R.string.training_create_routine_hint),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Button },
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
                                text = routineLastPerformedMeta(
                                    routine = routine,
                                    history = history,
                                    today = LocalDate.now(),
                                    locale = LocalConfiguration.current.locales[0],
                                ).asString(),
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
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { role = Role.Button },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.training_start_named, routine.name),
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
private fun TrainingCoachRow(cue: UiText, onView: () -> Unit) {
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
            text = cue.asString(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            color = MusFitTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onView) {
            Text(
                text = stringResource(R.string.training_view),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MusFitTheme.colors.accent,
            )
        }
    }
}

/**
 * Full-page History (Turn 10 §10g): back-circle header with a calendar toggle
 * over the month hero and week sections. An open session summary (§10b) takes
 * over the page with its own header.
 */
@Composable
private fun TrainingHistoryListPage(
    state: TrainingUiState,
    accent: TabAccent,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .padding(start = MusFitTheme.spacing.xl, end = MusFitTheme.spacing.xl, top = MusFitTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.xl),
    ) {
        InnerScreenHeader(title = stringResource(R.string.training_history), onBack = onBack) {
            TonalHeaderIconButton(
                icon = Icons.Outlined.CalendarMonth,
                contentDescription = stringResource(
                    if (calendarOpen) R.string.training_hide_calendar else R.string.training_show_calendar,
                ),
                onClick = { calendarOpen = !calendarOpen },
            )
        }
        TrainingHistoryContent(
            history = state.workoutHistory,
            overview = state.historyOverview,
            accent = accent,
            calendarOpen = calendarOpen,
            onOpenDetail = onOpenDetail,
            modifier = Modifier.weight(1f),
        )
    }
}

private const val DASHBOARD_ROUTINE_PREVIEW_LIMIT = 6

@Composable
private fun TrainingWorkoutHistoryDetailPage(
    detail: com.musfit.data.repository.WorkoutHistoryDetail,
    accent: TabAccent,
    onClose: () -> Unit,
    onOpenCoach: () -> Unit,
    showCloseAction: Boolean,
) {
    TrainingPageContainer {
        WorkoutCompleteContent(
            detail = detail,
            accent = accent,
            onClose = onClose,
            onOpenCoach = onOpenCoach,
            showCloseAction = showCloseAction,
        )
    }
}

/**
 * "All routines" page behind the dashboard's "All" link: the full management
 * workspace — user routines organized into folders (drag to move), plus the
 * pre-made routine library.
 */
@Composable
@Suppress("LongMethod")
private fun RoutineLibraryPage(
    state: TrainingUiState,
    accent: TabAccent,
    viewModel: TrainingViewModel,
    navigation: TrainingNavigationActions,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusFitTheme.colors.background)
            .padding(start = MusFitTheme.spacing.xl, end = MusFitTheme.spacing.xl, top = MusFitTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MusFitTheme.spacing.xl),
    ) {
        InnerScreenHeader(title = stringResource(R.string.training_routines), onBack = onBack)
        TrainingHomeContent(
            hasActiveWorkout = state.activeWorkoutSummary != null,
            routines = state.homeRoutines,
            folders = state.homeFolders,
            folderEditor = state.routineFolderEditor,
            accent = accent,
            onStartBlankWorkout = {
                viewModel.startBlankWorkout { navigation.open(TrainingActiveWorkoutNavKey) }
            },
            onNewRoutine = {
                navigation.open(TrainingRoutineEditorNavKey())
            },
            onOpenLibrary = {},
            showLibraryLink = false,
            onOpenFolderEditor = viewModel::openRoutineFolderEditor,
            onFolderNameChange = viewModel::onRoutineFolderNameChanged,
            onSaveFolder = viewModel::saveRoutineFolderEditor,
            onCancelFolder = viewModel::closeRoutineFolderEditor,
            onDeleteFolder = viewModel::deleteRoutineFolder,
            onAssignRoutineToFolder = viewModel::assignRoutineToFolder,
            onStartRoutine = { routineId ->
                viewModel.startRoutine(routineId) { navigation.open(TrainingActiveWorkoutNavKey) }
            },
            onOpenRoutineDetail = { routineId ->
                navigation.open(TrainingRoutineDetailNavKey(routineId))
            },
            modifier = Modifier.weight(1f),
            libraryRoutines = state.visibleRoutines,
            onStartLibraryRoutine = { routineId ->
                viewModel.startRoutine(routineId) { navigation.open(TrainingActiveWorkoutNavKey) }
            },
            onOpenLibraryRoutineDetail = { routineId ->
                navigation.open(TrainingRoutineDetailNavKey(routineId))
            },
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
        Text(text = stringResource(R.string.training_active_workout), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MusFitTheme.colors.onSurface)
        Surface(color = MusFitTheme.colors.surface, shape = MusFitTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = state.activeWorkoutSummary?.title ?: stringResource(R.string.training_workout_in_progress), style = MaterialTheme.typography.titleMedium, color = MusFitTheme.colors.onSurface)
                TextButton(onClick = onBack) { Text(stringResource(R.string.training_back_to_home)) }
            }
        }
    }
}

@Composable
private fun TrainingRoutePlaceholder(title: UiText, message: UiText, onBack: () -> Unit) {
    TrainingPageContainer {
        Text(
            text = title.asString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MusFitTheme.colors.onSurface,
        )
        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MusFitTheme.colors.onSurfaceVariant,
        )
        TextButton(onClick = onBack) { Text(stringResource(R.string.training_back)) }
    }
}

/**
 * Full-page exercise view: a large demo (animated GIF when available), title with the planned
 * target chip (only when opened from a routine), muscles as chips, instructions, and local notes.
 * Shared by routine exercise rows and the library so there is one clean exercise screen everywhere.
 */
@Composable
@Suppress("LongMethod", "LongParameterList")
private fun ExerciseDetailPage(
    detail: ExerciseDetail,
    target: String?,
    notesInput: String,
    accent: TabAccent,
    onNotesChange: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onClose: () -> Unit,
    showBackAction: Boolean = true,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showBackAction) {
            TonalHeaderIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.training_back),
                onClick = onClose,
            )
        }

        val gifUrl = detail.gifUrl
        val imageUrl = detail.imageUrl
        var gifUnavailable by remember(gifUrl) { mutableStateOf(false) }
        when (exerciseDetailMediaMode(gifUrl, imageUrl, gifUnavailable)) {
            ExerciseDetailMediaMode.Animated -> ExerciseGif(
                gifUrl = requireNotNull(gifUrl),
                contentDescription = detail.name,
                accent = accent,
                onError = { gifUnavailable = true },
            )

            ExerciseDetailMediaMode.Static -> ExerciseThumb(
                imageUrl = imageUrl,
                contentDescription = detail.name,
                accent = accent,
                size = 200.dp,
                shape = MusFitTheme.shapes.large,
            )

            ExerciseDetailMediaMode.Placeholder -> ExerciseThumb(
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
                detail.equipment?.replaceFirstChar { it.titlecase(locale) },
                detail.category.replaceFirstChar { it.titlecase(locale) },
                stringResource(if (detail.isCustom) R.string.training_custom else R.string.training_library),
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MusFitTheme.colors.onSurfaceVariant,
        )

        MuscleChipRow(label = stringResource(R.string.training_primary), muscles = detail.primaryMuscles, accent = accent, primary = true)
        if (detail.secondaryMuscles.isNotBlank()) {
            MuscleChipRow(label = stringResource(R.string.training_secondary), muscles = detail.secondaryMuscles, accent = accent, primary = false)
        }

        detail.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
            Text(instructions, style = MaterialTheme.typography.bodyMedium, color = MusFitTheme.colors.onSurface)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.training_your_notes), style = MaterialTheme.typography.labelLarge, color = MusFitTheme.colors.onSurfaceVariant)
            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                placeholder = { Text(stringResource(R.string.training_notes_hint)) },
                minLines = 2,
                shape = MusFitTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSaveNotes,
                colors = ButtonDefaults.buttonColors(containerColor = accent.color, contentColor = accent.onColor),
            ) {
                Text(stringResource(R.string.training_save_notes))
            }
        }
    }
}

@Composable
private fun MuscleChipRow(label: String, muscles: String, accent: TabAccent, primary: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val items = muscles.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.replaceFirstChar { ch -> ch.titlecase(locale) } }
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
            title = { Text(stringResource(R.string.training_finish_workout_question)) },
            text = {
                Text(
                    stringResource(
                        R.string.training_workout_summary,
                        workout.title,
                        pluralStringResource(
                            R.plurals.training_sets_volume,
                            workout.completedSetCount,
                            workout.completedSetCount,
                            workout.totalVolumeKg.formatKg(LocalConfiguration.current.locales[0]),
                        ),
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmFinish) {
                    Text(stringResource(R.string.training_finish))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelFinish) {
                    Text(stringResource(R.string.training_cancel))
                }
            },
        )
    }
    if (state.discardConfirmationOpen && workout != null) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text(stringResource(R.string.training_discard_workout_question)) },
            text = { Text(stringResource(R.string.training_discard_workout_explanation)) },
            confirmButton = {
                Button(onClick = onConfirmDiscard) {
                    Text(stringResource(R.string.training_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDiscard) {
                    Text(stringResource(R.string.training_keep_workout))
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
    val isPlanned: Boolean = false,
)

/**
 * Mon-Sun circles for the current week: a day is done when any workout was
 * started on it; at most one future-facing day is planned (dashed circle).
 */
internal fun trainingWeekDays(
    history: List<WorkoutHistorySummary>,
    today: LocalDate,
    weeklyTarget: Int = 0,
    locale: Locale = Locale.getDefault(),
): List<TrainingWeekDay> {
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val trainedDays = history
        .map { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toSet()
    val weekDates = (0..6).map { offset -> startOfWeek.plusDays(offset.toLong()) }
    val plannedDay = nextPlannedSessionDay(trainedDays, weekDates, today, weeklyTarget)
    return weekDates.map { date ->
        TrainingWeekDay(
            label = date.format(DateTimeFormatter.ofPattern("EEE", locale)),
            isDone = date in trainedDays,
            isToday = date == today,
            isPlanned = date == plannedDay,
        )
    }
}

/**
 * The dashed "next planned session" day: one rest day after the week's latest
 * session (falling forward to today, or today when nothing is logged yet).
 * Only shown while the weekly target still has sessions left and the candidate
 * fits inside this week.
 */
internal fun nextPlannedSessionDay(
    trainedDays: Set<LocalDate>,
    weekDates: List<LocalDate>,
    today: LocalDate,
    weeklyTarget: Int,
): LocalDate? {
    if (weeklyTarget <= 0) return null
    if (weekDates.count { it in trainedDays } >= weeklyTarget) return null
    val lastTrained = weekDates.filter { it in trainedDays && !it.isAfter(today) }.maxOrNull()
    val candidate = maxOf(today, lastTrained?.plusDays(2) ?: today)
    return candidate.takeIf { !it.isAfter(weekDates.last()) && it !in trainedDays }
}

/** Hero overline: "TODAY · QUADS DAY" from the routine's lead muscle group, else just "TODAY". */
internal fun trainingHeroOverline(
    routine: RoutineSummary,
    locale: Locale = Locale.getDefault(),
): UiText {
    val muscle = routine.muscleGroups.firstOrNull()?.trim()?.takeIf(String::isNotBlank)
    return if (muscle == null) {
        uiText(R.string.training_today)
    } else {
        uiText(R.string.training_today_muscle_day, UiText.Argument.Text(muscle.uppercase(locale)))
    }
}

internal fun trainingHeroMeta(routine: RoutineSummary): UiText {
    val estimatedMinutes = RoutineDisplayCalculator.estimatedMinutes(routine.targetSetCount)
    return if (estimatedMinutes > 0) {
        pluralUiText(
            R.plurals.training_exercises_minutes,
            routine.exerciseCount,
            UiText.Argument.Integer(routine.exerciseCount),
            UiText.Argument.Integer(estimatedMinutes),
        )
    } else {
        pluralUiText(
            R.plurals.training_exercise_count,
            routine.exerciseCount,
            UiText.Argument.Integer(routine.exerciseCount),
        )
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
    locale: Locale = Locale.getDefault(),
): UiText {
    val lastRun = history
        .filter { it.title.equals(routine.name, ignoreCase = true) }
        .maxByOrNull { it.startedAtEpochMillis }
        ?.let { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
    if (lastRun != null) {
        return lastRun.recencyLabel(today, locale)
    }
    return trainingHeroMeta(routine)
}

private fun LocalDate.recencyLabel(today: LocalDate, locale: Locale): UiText = when {
    this == today -> uiText(R.string.training_last_today)

    this == today.minusDays(1) -> uiText(R.string.training_last_yesterday)

    !isBefore(today.minusDays(6)) -> uiText(
        R.string.training_last_performed,
        UiText.Argument.Text(format(DateTimeFormatter.ofPattern("EEE", locale))),
    )

    else -> uiText(
        R.string.training_last_performed,
        UiText.Argument.Text(LocalizedFormatter.date(this, FormatStyle.MEDIUM, locale)),
    )
}

/** One deterministic coaching sentence for the hairline coach row — data-derived, no prose engine. */
internal fun trainingCoachCue(
    overview: TrainingHistoryOverview,
    nextRoutineName: String?,
    weeklyTarget: Int = DEFAULT_WEEKLY_SESSION_TARGET,
): UiText {
    val goal = if (weeklyTarget > 0) weeklyTarget else DEFAULT_WEEKLY_SESSION_TARGET
    val done = overview.currentWeekWorkoutCount
    return when {
        done >= goal -> pluralUiText(
            R.plurals.training_week_goal_done,
            goal,
            UiText.Argument.Integer(done),
            UiText.Argument.Integer(goal),
        )

        done == 0 ->
            if (nextRoutineName != null) {
                uiText(R.string.training_no_sessions_routine, UiText.Argument.Text(nextRoutineName))
            } else {
                uiText(R.string.training_no_sessions_empty)
            }

        else ->
            if (nextRoutineName != null) {
                pluralUiText(
                    R.plurals.training_sessions_next_routine,
                    done,
                    UiText.Argument.Integer(done),
                    UiText.Argument.Integer(goal),
                    UiText.Argument.Text(nextRoutineName),
                )
            } else {
                pluralUiText(
                    R.plurals.training_sessions_one_more,
                    done,
                    UiText.Argument.Integer(done),
                    UiText.Argument.Integer(goal),
                )
            }
    }
}

private fun Double.formatKg(locale: Locale = Locale.getDefault()): String = LocalizedFormatter.number(this, maximumFractionDigits = 1, locale = locale)
