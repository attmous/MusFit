package com.musfit.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseDetail
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseInput
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.WorkoutSetInputData
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineFolder
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineSetInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSettings
import com.musfit.data.repository.TrainingSettingsInput
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.PlateCalculator
import com.musfit.domain.training.WarmupSetCalculator
import com.musfit.domain.training.WorkoutCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class TrainingSection {
    Routines,
    Exercises,
    History,
}

data class RoutineEditorState(
    val routineId: String? = null,
    val name: String = "",
    val notes: String = "",
    val folderId: String? = null,
    val folderName: String = "",
    val exercises: List<RoutineExerciseInput> = emptyList(),
    val isStarter: Boolean = false,
    val isOpen: Boolean = false,
)

data class RoutineFolderEditorState(
    val folderId: String? = null,
    val name: String = "",
    val isOpen: Boolean = false,
)

data class ExerciseEditorState(
    val isOpen: Boolean = false,
    val name: String = "",
    val category: String = "strength",
    val equipment: String = "",
    val targetMuscles: String = "",
)

data class RestTimerState(
    val isVisible: Boolean = false,
    val sourceSetId: String? = null,
    val durationSeconds: Int = DEFAULT_REST_TIMER_SECONDS,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
)

data class TrainingHistoryCalendarDay(
    val date: LocalDate,
    val workoutCount: Int,
    val completedSetCount: Int,
    val totalVolumeKg: Double,
)

data class TrainingHistoryOverview(
    val monthLabel: String = "",
    val calendarWeeks: List<List<TrainingHistoryCalendarDay?>> = emptyList(),
    val currentWeekWorkoutCount: Int = 0,
    val currentWeekTrainingDayCount: Int = 0,
    val currentWeekCompletedSetCount: Int = 0,
    val currentWeekVolumeKg: Double = 0.0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
)

data class TrainingDashboardState(
    val nextSuggestedRoutine: RoutineSummary? = null,
    val quickStartRoutines: List<RoutineSummary> = emptyList(),
    val recentWorkout: WorkoutHistorySummary? = null,
)

/**
 * Full-screen pages the Training miniapp layers over its home scaffold. Their visit order lives in
 * [TrainingUiState.pageStack]; system back pops exactly one page at a time via
 * [TrainingViewModel.navigateBack], so leaving e.g. the routine editor lands on the routine detail
 * or library it was opened from instead of dumping the whole flow. Page *content* stays in the
 * dedicated state fields ([TrainingUiState.routineEditor], [TrainingUiState.selectedRoutineDetail],
 * ...); the stack only decides which page is on top and what back does.
 */
enum class TrainingPage {
    RoutineLibrary,
    RoutineDetail,
    RoutineEditor,
    ExerciseDetail,
    ExercisePicker,
    WorkoutHistoryDetail,
    ActiveWorkout,
}

data class TrainingUiState(
    val selectedSection: TrainingSection = TrainingSection.Routines,
    val routines: List<RoutineSummary> = emptyList(),
    val homeRoutines: List<RoutineSummary> = emptyList(),
    val visibleRoutines: List<RoutineSummary> = emptyList(),
    val routineFolders: List<RoutineFolder> = emptyList(),
    val homeFolders: List<RoutineFolder> = emptyList(),
    val routineProgramOptions: List<String> = emptyList(),
    val selectedRoutineProgram: String? = null,
    val exercises: List<ExerciseSummary> = emptyList(),
    val visibleExercises: List<ExerciseSummary> = emptyList(),
    val activeWorkoutSummary: ActiveWorkoutSummary? = null,
    val activeWorkout: ActiveWorkoutDetail? = null,
    val workoutHistory: List<WorkoutHistorySummary> = emptyList(),
    val historyOverview: TrainingHistoryOverview = TrainingHistoryOverview(),
    val dashboard: TrainingDashboardState = TrainingDashboardState(),
    val selectedWorkoutDetail: WorkoutHistoryDetail? = null,
    val exerciseSearchQuery: String = "",
    val exerciseMuscleFilter: String? = null,
    val exerciseEquipmentFilter: String? = null,
    val exerciseEditor: ExerciseEditorState = ExerciseEditorState(),
    val selectedExerciseDetail: ExerciseDetail? = null,
    val exerciseDetailNotesInput: String = "",
    val exerciseDetailTarget: String? = null,
    val exerciseName: String = "",
    val reps: String = "",
    val weightKg: String = "",
    val sets: List<LoggedWorkoutSet> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val bestEstimatedOneRepMaxKg: Double = 0.0,
    val routineEditor: RoutineEditorState = RoutineEditorState(),
    val routineFolderEditor: RoutineFolderEditorState = RoutineFolderEditorState(),
    val routineExercisePickerSelectedIds: Set<String> = emptySet(),
    val routineExercisePickerSearchQuery: String = "",
    val routineExercisePickerMuscleFilter: String? = null,
    val routineExercisePickerEquipmentFilter: String? = null,
    val selectedRoutineDetail: RoutineDetail? = null,
    val pageStack: List<TrainingPage> = emptyList(),
    val replaceExerciseTargetId: String? = null,
    val activeWorkoutNotesInput: String = "",
    val trainingSettings: TrainingSettings = TrainingSettings(),
    val restTimerDefaultSecondsInput: String = "120",
    val plateBarWeightInput: String = "20",
    val availablePlatesInput: String = "25, 20, 15, 10, 5, 2.5, 1.25",
    val restTimer: RestTimerState = RestTimerState(),
    val finishConfirmationOpen: Boolean = false,
    val discardConfirmationOpen: Boolean = false,
    val message: String? = null,
) {
    val routineLibraryPageOpen: Boolean
        get() = TrainingPage.RoutineLibrary in pageStack

    val routineExercisePickerOpen: Boolean
        get() = pageStack.lastOrNull() == TrainingPage.ExercisePicker

    val activeWorkoutRouteOpen: Boolean
        get() = pageStack.lastOrNull() == TrainingPage.ActiveWorkout

    fun pushPage(page: TrainingPage): TrainingUiState =
        copy(pageStack = pageStack.filterNot { it == page } + page)

    fun removePage(page: TrainingPage): TrainingUiState =
        copy(pageStack = pageStack.filterNot { it == page })
}

private data class TrainingInitialStreams(
    val routines: List<RoutineSummary>,
    val folders: List<RoutineFolder>,
    val exercises: List<ExerciseSummary>,
    val activeWorkout: ActiveWorkoutSummary?,
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val repository: TrainingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedStarterTrainingData()
        }
        viewModelScope.launch {
            combine(
                repository.observeRoutineSummaries(),
                repository.observeRoutineFolders(),
                repository.observeExercises(),
                repository.observeActiveWorkoutSummary(),
            ) { routines, folders, exercises, activeWorkout ->
                TrainingInitialStreams(routines, folders, exercises, activeWorkout)
            }.collect { streams ->
                mutableState.update {
                    it.copy(
                        routines = streams.routines,
                        routineFolders = streams.folders,
                        exercises = streams.exercises,
                        activeWorkoutSummary = streams.activeWorkout,
                    ).withVisibleRoutines()
                        .withDashboard()
                        .withFilteredExercises()
                }
            }
        }
        viewModelScope.launch {
            repository.observeActiveWorkoutDetail().collect { activeWorkout ->
                mutableState.update { current ->
                    val previousActiveWorkout = current.activeWorkout
                    val shouldSyncNotes = activeWorkout == null ||
                        previousActiveWorkout?.sessionId != activeWorkout.sessionId ||
                        previousActiveWorkout.notes != activeWorkout.notes
                    current.copy(
                        activeWorkout = activeWorkout,
                        activeWorkoutNotesInput = if (shouldSyncNotes) {
                            activeWorkout?.notes.orEmpty()
                        } else {
                            current.activeWorkoutNotesInput
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeTrainingSettings().collect { settings ->
                mutableState.update { current ->
                    current.copy(
                        trainingSettings = settings,
                        restTimerDefaultSecondsInput = settings.defaultRestSeconds.toString(),
                        plateBarWeightInput = settings.barWeightKg.formatSettingsNumber(),
                        availablePlatesInput = settings.availablePlatesKg.formatPlateListInput(),
                        restTimer = if (current.restTimer.isVisible) {
                            current.restTimer
                        } else {
                            current.restTimer.copy(durationSeconds = settings.defaultRestSeconds)
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.observeWorkoutHistory().collect { history ->
                mutableState.update {
                    it.copy(
                        workoutHistory = history,
                        historyOverview = buildTrainingHistoryOverview(history),
                    ).withDashboard()
                }
            }
        }
    }

    fun selectSection(section: TrainingSection) {
        mutableState.update { it.copy(selectedSection = section) }
    }

    fun resumeActiveWorkout() {
        mutableState.update { it.pushPage(TrainingPage.ActiveWorkout) }
    }

    /**
     * Pops the top Training page, delegating to that page's close function so its content state is
     * cleared alongside the stack entry. The screen's single BackHandler calls this; when the stack
     * is empty this is a no-op and system back falls through to tab-level navigation.
     */
    fun navigateBack() {
        when (state.value.pageStack.lastOrNull()) {
            TrainingPage.ExercisePicker -> closeRoutineExercisePicker()
            TrainingPage.RoutineEditor -> closeRoutineEditor()
            TrainingPage.ExerciseDetail -> closeExerciseDetail()
            TrainingPage.RoutineDetail -> closeRoutineDetail()
            TrainingPage.RoutineLibrary -> closeRoutineLibraryPage()
            TrainingPage.WorkoutHistoryDetail -> closeWorkoutDetail()
            TrainingPage.ActiveWorkout -> closeActiveWorkoutRoute()
            null -> Unit
        }
    }

    fun openWorkoutDetail(sessionId: String) {
        viewModelScope.launch {
            val detail = repository.getWorkoutHistoryDetail(sessionId)
            mutableState.update {
                if (detail == null) {
                    it.removePage(TrainingPage.WorkoutHistoryDetail).copy(selectedWorkoutDetail = null)
                } else {
                    it.pushPage(TrainingPage.WorkoutHistoryDetail).copy(selectedWorkoutDetail = detail)
                }
            }
        }
    }

    fun closeWorkoutDetail() {
        mutableState.update {
            it.removePage(TrainingPage.WorkoutHistoryDetail).copy(selectedWorkoutDetail = null)
        }
    }

    fun closeActiveWorkoutRoute() {
        mutableState.update {
            val next = it.removePage(TrainingPage.ActiveWorkout)
            // Only reset the section when back lands on the home scaffold; pages still on the
            // stack (library, routine detail) cover the sections entirely.
            if (next.pageStack.isEmpty()) {
                next.copy(selectedSection = TrainingSection.Routines)
            } else {
                next
            }
        }
    }

    fun openRoutineLibraryPage() {
        mutableState.update {
            it.pushPage(TrainingPage.RoutineLibrary).copy(message = null)
        }
    }

    fun closeRoutineLibraryPage() {
        mutableState.update {
            // Leaving the library drops it and anything layered above it, so clear the content of
            // every page that could have been stacked on top.
            val libraryIndex = it.pageStack.indexOf(TrainingPage.RoutineLibrary)
            it.copy(
                pageStack = if (libraryIndex < 0) it.pageStack else it.pageStack.take(libraryIndex),
                selectedSection = TrainingSection.Routines,
                selectedRoutineDetail = null,
                selectedExerciseDetail = null,
                exerciseDetailNotesInput = "",
                exerciseDetailTarget = null,
                routineEditor = RoutineEditorState(),
                routineFolderEditor = RoutineFolderEditorState(),
            )
        }
    }

    fun onRoutineProgramFilterChanged(value: String?) {
        mutableState.update {
            it.copy(selectedRoutineProgram = value?.trim()?.takeIf(String::isNotBlank))
                .withVisibleRoutines()
                .withDashboard()
        }
    }

    fun openRoutineFolderEditor(folderId: String?) {
        val folder = folderId?.let { id -> state.value.routineFolders.firstOrNull { it.id == id } }
        mutableState.update {
            it.copy(
                routineFolderEditor = RoutineFolderEditorState(
                    folderId = folder?.id,
                    name = folder?.name.orEmpty(),
                    isOpen = true,
                ),
                message = null,
            )
        }
    }

    fun closeRoutineFolderEditor() {
        mutableState.update { it.copy(routineFolderEditor = RoutineFolderEditorState()) }
    }

    fun onRoutineFolderNameChanged(value: String) {
        mutableState.update { it.copy(routineFolderEditor = it.routineFolderEditor.copy(name = value)) }
    }

    fun saveRoutineFolderEditor() {
        val editor = state.value.routineFolderEditor
        val name = editor.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            if (editor.folderId == null) {
                repository.createRoutineFolder(name)
            } else {
                repository.updateRoutineFolder(editor.folderId, name)
            }
            closeRoutineFolderEditor()
        }
    }

    fun deleteRoutineFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteRoutineFolder(folderId)
            if (state.value.routineFolderEditor.folderId == folderId) {
                closeRoutineFolderEditor()
            }
        }
    }

    fun assignRoutineToFolder(routineId: String, folderId: String?) {
        val routine = state.value.routines.firstOrNull { it.id == routineId } ?: return
        val targetFolder = folderId?.let { targetId ->
            state.value.routineFolders.firstOrNull { it.id == targetId } ?: return
        }
        val targetFolderId = targetFolder?.id
        if (routine.folderId == targetFolderId) return
        viewModelScope.launch {
            repository.assignRoutineToFolder(routineId, targetFolderId)
            mutableState.update {
                it.copy(message = "${routine.name} moved to ${targetFolder?.name ?: "My routines"}.")
            }
        }
    }

    fun openRoutineEditor(routineId: String?) {
        viewModelScope.launch {
            val detail = routineId?.let { repository.getRoutineDetail(it) }
            mutableState.update {
                it.copy(
                    routineEditor = RoutineEditorState(
                        routineId = routineId,
                        name = detail?.name.orEmpty(),
                        notes = detail?.notes.orEmpty(),
                        folderId = detail?.folderId,
                        folderName = detail?.folderName.orEmpty(),
                        exercises = detail?.exercises?.map { exercise ->
                            RoutineExerciseInput(
                                exerciseId = exercise.exercise.id,
                                targetSets = exercise.targetSets,
                                targetReps = exercise.targetReps,
                                restSeconds = exercise.restSeconds,
                                setPlans = exercise.setPlans.ifEmpty { defaultSetPlans(exercise.targetSets, exercise.targetReps) },
                            )
                        }.orEmpty(),
                        isStarter = detail?.isStarter == true,
                        isOpen = true,
                    ),
                    // A routine detail beneath the editor stays loaded so back returns to it.
                    message = null,
                ).pushPage(TrainingPage.RoutineEditor)
            }
        }
    }

    fun closeRoutineEditor() {
        mutableState.update {
            it.removePage(TrainingPage.RoutineEditor).copy(routineEditor = RoutineEditorState())
        }
    }

    fun openRoutineDetail(routineId: String) {
        viewModelScope.launch {
            val detail = repository.getRoutineDetail(routineId)
            mutableState.update {
                if (detail == null) {
                    it.copy(message = "Routine not found.")
                } else {
                    it.pushPage(TrainingPage.RoutineDetail).copy(
                        selectedRoutineDetail = detail,
                        message = null,
                    )
                }
            }
        }
    }

    fun closeRoutineDetail() {
        mutableState.update {
            it.removePage(TrainingPage.RoutineDetail).copy(selectedRoutineDetail = null)
        }
    }

    fun onRoutineNameChanged(value: String) {
        mutableState.update { it.copy(routineEditor = it.routineEditor.copy(name = value)) }
    }

    fun onRoutineNotesChanged(value: String) {
        mutableState.update { it.copy(routineEditor = it.routineEditor.copy(notes = value)) }
    }

    fun onRoutineEditorFolderNameChanged(value: String) {
        mutableState.update {
            it.copy(
                routineEditor = it.routineEditor.copy(
                    folderName = value,
                    folderId = null,
                ),
            )
        }
    }

    fun addRoutineExercise(exerciseId: String) {
        addRoutineExercises(listOf(exerciseId))
    }

    fun addRoutineExercises(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        val existingExerciseIds = state.value.exercises.map { it.id }.toSet()
        val currentExerciseIds = state.value.routineEditor.exercises.map { it.exerciseId }.toSet()
        val newInputs = exerciseIds
            .filter { it in existingExerciseIds && it !in currentExerciseIds }
            .distinct()
            .map { exerciseId ->
                RoutineExerciseInput(
                    exerciseId = exerciseId,
                    targetSets = 3,
                    targetReps = "8",
                    restSeconds = state.value.trainingSettings.defaultRestSeconds,
                    setPlans = defaultSetPlans(targetSets = 3, targetReps = "8"),
                )
            }
        if (newInputs.isEmpty()) return
        mutableState.update {
            it.copy(
                routineEditor = it.routineEditor.copy(
                    exercises = it.routineEditor.exercises + newInputs,
                ),
            )
        }
    }

    fun openRoutineExercisePicker() {
        mutableState.update {
            it.pushPage(TrainingPage.ExercisePicker).copy(
                routineExercisePickerSelectedIds = emptySet(),
                routineExercisePickerSearchQuery = "",
                routineExercisePickerMuscleFilter = null,
                routineExercisePickerEquipmentFilter = null,
            )
        }
    }

    fun closeRoutineExercisePicker() {
        mutableState.update {
            it.removePage(TrainingPage.ExercisePicker).copy(
                routineExercisePickerSelectedIds = emptySet(),
            )
        }
    }

    fun onRoutineExercisePickerSearchChanged(value: String) {
        mutableState.update { it.copy(routineExercisePickerSearchQuery = value) }
    }

    fun onRoutineExercisePickerMuscleFilterChanged(value: String?) {
        mutableState.update { it.copy(routineExercisePickerMuscleFilter = value) }
    }

    fun onRoutineExercisePickerEquipmentFilterChanged(value: String?) {
        mutableState.update { it.copy(routineExercisePickerEquipmentFilter = value) }
    }

    fun clearRoutineExercisePickerFilters() {
        mutableState.update {
            it.copy(
                routineExercisePickerSearchQuery = "",
                routineExercisePickerMuscleFilter = null,
                routineExercisePickerEquipmentFilter = null,
            )
        }
    }

    fun toggleRoutineExercisePickerSelection(exerciseId: String) {
        val exerciseExists = state.value.exercises.any { it.id == exerciseId }
        if (!exerciseExists) return
        val alreadyInRoutine = state.value.routineEditor.exercises.any { it.exerciseId == exerciseId }
        if (alreadyInRoutine) return
        mutableState.update { current ->
            val nextSelected = if (exerciseId in current.routineExercisePickerSelectedIds) {
                current.routineExercisePickerSelectedIds - exerciseId
            } else {
                current.routineExercisePickerSelectedIds + exerciseId
            }
            current.copy(routineExercisePickerSelectedIds = nextSelected)
        }
    }

    fun confirmRoutineExercisePicker() {
        val selectedIds = state.value.routineExercisePickerSelectedIds.toList()
        addRoutineExercises(selectedIds)
        closeRoutineExercisePicker()
    }

    fun removeRoutineExercise(index: Int) {
        mutableState.update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.filterIndexed { itemIndex, _ ->
                            itemIndex != index
                        },
                    ),
                )
            }
        }
    }

    fun onRoutineExerciseTargetSetsChanged(index: Int, value: String) {
        val parsedSets = value.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1) ?: 1
        mutableState.update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.mapIndexed { itemIndex, exercise ->
                            if (itemIndex == index) {
                                val currentPlans = exercise.setPlans.ifEmpty {
                                    defaultSetPlans(exercise.targetSets, exercise.targetReps)
                                }
                                val resizedPlans = currentPlans.resizeSetPlans(parsedSets, exercise.targetReps)
                                exercise.copy(
                                    targetSets = parsedSets,
                                    setPlans = resizedPlans,
                                )
                            } else {
                                exercise
                            }
                        },
                    ),
                )
            }
        }
    }

    fun onRoutineExerciseTargetRepsChanged(index: Int, value: String) {
        val sanitized = value.trim().takeIf { it.isNotEmpty() }
        mutableState.update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.mapIndexed { itemIndex, exercise ->
                            if (itemIndex == index) {
                                exercise.copy(
                                    targetReps = sanitized,
                                    setPlans = exercise.setPlans.ifEmpty {
                                        defaultSetPlans(exercise.targetSets, exercise.targetReps)
                                    }.map { setPlan ->
                                        setPlan.copy(targetReps = sanitized)
                                    },
                                )
                            } else {
                                exercise
                            }
                        },
                    ),
                )
            }
        }
    }

    fun onRoutineExerciseRestSecondsChanged(index: Int, value: String) {
        val restSeconds = value.filter(Char::isDigit).toIntOrNull()
        mutableState.updateRoutineExercise(index) { exercise ->
            exercise.copy(restSeconds = restSeconds)
        }
    }

    fun addRoutineExerciseSet(index: Int) {
        mutableState.updateRoutineExercise(index) { exercise ->
            val plans = exercise.setPlans.ifEmpty { defaultSetPlans(exercise.targetSets, exercise.targetReps) }
            val nextPlan = plans.lastOrNull()?.copy(setType = "working") ?: RoutineSetInput(
                setType = "working",
                targetReps = exercise.targetReps,
            )
            exercise.copy(
                targetSets = plans.size + 1,
                setPlans = plans + nextPlan,
            )
        }
    }

    fun removeRoutineExerciseSet(index: Int, setIndex: Int) {
        mutableState.updateRoutineExercise(index) { exercise ->
            val plans = exercise.setPlans.ifEmpty { defaultSetPlans(exercise.targetSets, exercise.targetReps) }
            if (plans.size <= 1 || setIndex !in plans.indices) {
                exercise
            } else {
                val nextPlans = plans.filterIndexed { itemIndex, _ -> itemIndex != setIndex }
                exercise.copy(
                    targetSets = nextPlans.size,
                    targetReps = nextPlans.firstOrNull()?.targetReps,
                    setPlans = nextPlans,
                )
            }
        }
    }

    fun onRoutineExerciseSetTypeChanged(exerciseIndex: Int, setIndex: Int, setType: String) {
        mutableState.updateRoutineSet(exerciseIndex, setIndex) { it.copy(setType = setType.normalizedRoutineSetType()) }
    }

    fun onRoutineExerciseSetRepsChanged(exerciseIndex: Int, setIndex: Int, value: String) {
        val sanitized = value.trim().takeIf { it.isNotEmpty() }
        mutableState.updateRoutineSet(exerciseIndex, setIndex) { it.copy(targetReps = sanitized) }
        val exercise = state.value.routineEditor.exercises.getOrNull(exerciseIndex) ?: return
        val firstReps = exercise.setPlans.firstOrNull()?.targetReps
        mutableState.updateRoutineExercise(exerciseIndex) { it.copy(targetReps = firstReps) }
    }

    fun onRoutineExerciseSetWeightChanged(exerciseIndex: Int, setIndex: Int, value: String) {
        val weightKg = value.sanitizeDecimalInput().toDoubleOrNull()?.takeIf { it > 0.0 }
        mutableState.updateRoutineSet(exerciseIndex, setIndex) { it.copy(targetWeightKg = weightKg) }
    }

    fun moveRoutineExerciseUp(index: Int) {
        moveRoutineExercise(fromIndex = index, toIndex = index - 1)
    }

    fun moveRoutineExerciseDown(index: Int) {
        moveRoutineExercise(fromIndex = index, toIndex = index + 1)
    }

    fun saveRoutineEditor() {
        val editor = state.value.routineEditor
        val folderName = editor.folderName.trim().takeIf { it.isNotBlank() }
        val input = RoutineInput(
            name = editor.name,
            notes = editor.notes,
            exercises = editor.exercises,
            folderId = editor.folderId,
            folderName = folderName,
        )
        viewModelScope.launch {
            if (editor.routineId == null) {
                repository.createRoutine(input)
            } else {
                repository.updateRoutine(editor.routineId, input)
            }
            // Closing the editor can land back on the routine detail beneath it, so refresh that
            // page's snapshot to show the just-saved changes.
            val refreshedDetail = editor.routineId
                ?.takeIf { state.value.selectedRoutineDetail?.id == it }
                ?.let { repository.getRoutineDetail(it) }
            mutableState.update {
                it.removePage(TrainingPage.RoutineEditor).copy(
                    routineEditor = RoutineEditorState(),
                    selectedRoutineDetail = refreshedDetail ?: it.selectedRoutineDetail,
                )
            }
        }
    }

    fun duplicateRoutine(routineId: String) {
        viewModelScope.launch {
            repository.duplicateRoutine(routineId)
        }
    }

    fun deleteRoutine(routineId: String) {
        if (isStarterRoutine(routineId)) {
            mutableState.update { it.copy(message = "Starter routines are read-only templates.") }
            return
        }
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            mutableState.update { current ->
                var next = current
                if (next.routineEditor.routineId == routineId) {
                    next = next.removePage(TrainingPage.RoutineEditor)
                        .copy(routineEditor = RoutineEditorState())
                }
                if (next.selectedRoutineDetail?.id == routineId) {
                    next = next.removePage(TrainingPage.RoutineDetail)
                        .copy(selectedRoutineDetail = null)
                }
                next
            }
        }
    }

    fun startBlankWorkout() {
        viewModelScope.launch {
            repository.startBlankWorkout()
            mutableState.update { it.pushPage(TrainingPage.ActiveWorkout) }
        }
    }

    fun startRoutine(routineId: String) {
        viewModelScope.launch {
            repository.startWorkoutFromRoutine(routineId)
            // The routine detail / library stay on the stack beneath the workout page, so backing
            // out of a running workout returns to where it was started from.
            mutableState.update { it.pushPage(TrainingPage.ActiveWorkout) }
        }
    }

    fun onExerciseChanged(value: String) {
        mutableState.update { it.copy(exerciseName = value) }
    }

    fun onExerciseSearchQueryChanged(value: String) {
        mutableState.update { it.copy(exerciseSearchQuery = value).withFilteredExercises() }
    }

    fun onExerciseMuscleFilterChanged(value: String?) {
        mutableState.update {
            it.copy(exerciseMuscleFilter = value?.trim()?.takeIf(String::isNotEmpty))
                .withFilteredExercises()
        }
    }

    fun onExerciseEquipmentFilterChanged(value: String?) {
        mutableState.update {
            it.copy(exerciseEquipmentFilter = value?.trim()?.takeIf(String::isNotEmpty))
                .withFilteredExercises()
        }
    }

    fun clearExerciseFilters() {
        mutableState.update {
            it.copy(
                exerciseSearchQuery = "",
                exerciseMuscleFilter = null,
                exerciseEquipmentFilter = null,
            ).withFilteredExercises()
        }
    }

    /** Opens the full-page exercise view from the library (switches to the Exercises section). */
    fun openExerciseDetail(exerciseId: String) =
        loadExerciseDetail(exerciseId, target = null, switchToExercises = true)

    /**
     * Opens the same exercise page from inside a routine, layered over the routine detail (no
     * section switch) and carrying the planned sets x reps so the page can show the target.
     */
    fun openRoutineExerciseDetail(exerciseId: String, target: String?) =
        loadExerciseDetail(exerciseId, target = target, switchToExercises = false)

    private fun loadExerciseDetail(exerciseId: String, target: String?, switchToExercises: Boolean) {
        viewModelScope.launch {
            val detail = repository.getExerciseDetail(exerciseId)
            mutableState.update {
                if (detail == null) {
                    it.copy(message = "Exercise not found.")
                } else {
                    it.pushPage(TrainingPage.ExerciseDetail).copy(
                        selectedSection = if (switchToExercises) TrainingSection.Exercises else it.selectedSection,
                        selectedExerciseDetail = detail,
                        exerciseDetailNotesInput = detail.localNotes.orEmpty(),
                        exerciseDetailTarget = target,
                        message = null,
                    )
                }
            }
        }
    }

    fun closeExerciseDetail() {
        mutableState.update {
            it.removePage(TrainingPage.ExerciseDetail).copy(
                selectedExerciseDetail = null,
                exerciseDetailNotesInput = "",
                exerciseDetailTarget = null,
            )
        }
    }

    fun onExerciseDetailNotesChanged(value: String) {
        mutableState.update { it.copy(exerciseDetailNotesInput = value) }
    }

    fun saveExerciseDetailNotes() {
        val detail = state.value.selectedExerciseDetail ?: return
        val notes = state.value.exerciseDetailNotesInput.trim().takeIf { it.isNotBlank() }
        viewModelScope.launch {
            repository.updateExerciseLocalNotes(detail.id, notes)
            val updated = repository.getExerciseDetail(detail.id)?.copy(localNotes = notes)
            mutableState.update {
                it.copy(
                    selectedExerciseDetail = updated,
                    exerciseDetailNotesInput = notes.orEmpty(),
                    message = "Exercise notes saved.",
                )
            }
        }
    }

    fun openCustomExerciseEditor() {
        mutableState.update {
            it.copy(
                selectedSection = TrainingSection.Exercises,
                exerciseEditor = ExerciseEditorState(isOpen = true),
            )
        }
    }

    fun closeCustomExerciseEditor() {
        mutableState.update { it.copy(exerciseEditor = ExerciseEditorState()) }
    }

    fun onCustomExerciseNameChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(name = value)) }
    }

    fun onCustomExerciseCategoryChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(category = value)) }
    }

    fun onCustomExerciseEquipmentChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(equipment = value)) }
    }

    fun onCustomExerciseTargetMusclesChanged(value: String) {
        mutableState.update { it.copy(exerciseEditor = it.exerciseEditor.copy(targetMuscles = value)) }
    }

    fun saveCustomExercise() {
        val editor = state.value.exerciseEditor
        val name = editor.name.trim()
        if (name.isBlank()) {
            mutableState.update { it.copy(message = "Exercise name is required.") }
            return
        }
        val input = ExerciseInput(
            name = name,
            category = editor.category.trim().ifBlank { "strength" },
            equipment = editor.equipment.trim().takeIf { it.isNotBlank() },
            targetMuscles = editor.targetMuscles.trim(),
        )
        viewModelScope.launch {
            repository.createCustomExercise(input)
            mutableState.update {
                it.copy(
                    selectedSection = TrainingSection.Exercises,
                    exerciseEditor = ExerciseEditorState(),
                    message = "Exercise saved.",
                ).withFilteredExercises()
            }
        }
    }

    fun onRepsChanged(value: String) {
        mutableState.update { it.copy(reps = value.filter(Char::isDigit)) }
    }

    fun onWeightChanged(value: String) {
        mutableState.update { it.copy(weightKg = value.sanitizeDecimalInput()) }
    }

    fun addSet() {
        val currentState = state.value
        val reps = currentState.reps.toIntOrNull() ?: return
        val weightKg = currentState.weightKg.toDoubleOrNull() ?: return
        if (reps <= 0 || weightKg <= 0.0) return

        viewModelScope.launch {
            val savedSet = repository.addCompletedSet(
                exerciseName = currentState.exerciseName,
                reps = reps,
                weightKg = weightKg,
            )
            mutableState.update {
                it.copy(
                    reps = "",
                    weightKg = "",
                    sets = it.sets + savedSet,
                ).withCalculatedSummary()
            }
        }
    }

    fun toggleSetCompletion(setIndex: Int) {
        val currentSets = state.value.sets
        if (setIndex !in currentSets.indices) return

        val set = currentSets[setIndex]
        val updatedSet = set.copy(completed = !set.completed)
        val nextSets = currentSets.toMutableList().apply { this[setIndex] = updatedSet }

        mutableState.update { it.copy(sets = nextSets).withCalculatedSummary() }
        viewModelScope.launch {
            repository.setCompletion(setId = updatedSet.id, completed = updatedSet.completed)
        }
    }

    fun toggleWorkoutSetCompletion(setId: String, completed: Boolean) {
        val set = state.value.activeWorkout
            ?.exerciseBlocks
            ?.flatMap { it.sets }
            ?.firstOrNull { it.id == setId }
            ?: return
        if (completed && !set.isValidForCompletion()) {
            return
        }
        viewModelScope.launch {
            repository.updateWorkoutSet(
                setId,
                WorkoutSetInputData(
                    setType = set.setType,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    rpe = set.rpe,
                    notes = set.notes,
                    completed = completed,
                ),
            )
            if (completed) {
                startRestTimer(setId)
            }
        }
    }

    fun tickRestTimer() {
        mutableState.update { current ->
            val timer = current.restTimer
            if (!timer.isVisible || !timer.isRunning) {
                current
            } else {
                val nextRemaining = (timer.remainingSeconds - 1).coerceAtLeast(0)
                current.copy(
                    restTimer = timer.copy(
                        isVisible = nextRemaining > 0,
                        isRunning = nextRemaining > 0,
                        remainingSeconds = nextRemaining,
                    ),
                )
            }
        }
    }

    fun pauseRestTimer() {
        mutableState.update { current ->
            if (!current.restTimer.isVisible) {
                current
            } else {
                current.copy(restTimer = current.restTimer.copy(isRunning = false))
            }
        }
    }

    fun resumeRestTimer() {
        mutableState.update { current ->
            val timer = current.restTimer
            if (!timer.isVisible || timer.remainingSeconds <= 0) {
                current
            } else {
                current.copy(restTimer = timer.copy(isRunning = true))
            }
        }
    }

    fun skipRestTimer() {
        mutableState.update { current ->
            current.copy(
                restTimer = current.restTimer.copy(
                    isVisible = false,
                    isRunning = false,
                    remainingSeconds = 0,
                ),
            )
        }
    }

    fun adjustRestTimerSeconds(deltaSeconds: Int) {
        if (deltaSeconds == 0) return
        mutableState.update { current ->
            val timer = current.restTimer
            if (!timer.isVisible) {
                current
            } else {
                val nextRemaining = (timer.remainingSeconds + deltaSeconds).coerceAtLeast(0)
                current.copy(
                    restTimer = timer.copy(
                        isVisible = nextRemaining > 0,
                        isRunning = timer.isRunning && nextRemaining > 0,
                        remainingSeconds = nextRemaining,
                    ),
                )
            }
        }
    }

    fun onRestTimerDefaultSecondsChanged(value: String) {
        mutableState.update { it.copy(restTimerDefaultSecondsInput = value.filter(Char::isDigit)) }
    }

    fun onPlateBarWeightChanged(value: String) {
        mutableState.update { it.copy(plateBarWeightInput = value.sanitizeDecimalInput()) }
    }

    fun onAvailablePlatesChanged(value: String) {
        mutableState.update { it.copy(availablePlatesInput = value.filter { char -> char.isDigit() || char == '.' || char == ',' || char == ' ' }) }
    }

    fun saveTrainingToolSettings() {
        saveRestTimerSettings()
    }

    fun saveRestTimerSettings() {
        val current = state.value
        val input = TrainingSettingsInput(
            defaultRestSeconds = current.restTimerDefaultSecondsInput.toIntOrNull() ?: current.trainingSettings.defaultRestSeconds,
            barWeightKg = current.trainingSettings.barWeightKg,
            availablePlatesKg = current.trainingSettings.availablePlatesKg.ifEmpty { PlateCalculator.DEFAULT_PLATES },
        ).normalized()
        viewModelScope.launch {
            repository.updateTrainingSettings(input)
            mutableState.update {
                it.copy(
                    trainingSettings = TrainingSettings(
                        defaultRestSeconds = input.defaultRestSeconds,
                        barWeightKg = input.barWeightKg,
                        availablePlatesKg = input.availablePlatesKg,
                    ),
                    restTimerDefaultSecondsInput = input.defaultRestSeconds.toString(),
                    plateBarWeightInput = input.barWeightKg.formatSettingsNumber(),
                    availablePlatesInput = input.availablePlatesKg.formatPlateListInput(),
                    restTimer = if (it.restTimer.isVisible) {
                        it.restTimer
                    } else {
                        it.restTimer.copy(durationSeconds = input.defaultRestSeconds)
                    },
                    message = "Rest timer saved.",
                )
            }
        }
    }

    fun addExerciseToActiveWorkout(exerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.addExerciseToActiveWorkout(sessionId, exerciseId)
        }
    }

    fun addWorkoutSet(exerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.addSetToExercise(
                sessionId = sessionId,
                exerciseId = exerciseId,
                input = WorkoutSetInputData(
                    setType = "working",
                    reps = null,
                    weightKg = null,
                    rpe = null,
                    notes = null,
                    completed = false,
                ),
            )
        }
    }

    fun addSuggestedWarmupSet(exerciseId: String, reps: Int, weightKg: Double) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.addSetToExercise(
                sessionId = sessionId,
                exerciseId = exerciseId,
                input = WorkoutSetInputData(
                    setType = "warmup",
                    reps = reps,
                    weightKg = weightKg,
                    rpe = null,
                    notes = null,
                    completed = false,
                ),
            )
        }
    }

    fun duplicateLastWorkoutSet(exerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.duplicateLastSet(sessionId, exerciseId)
        }
    }

    fun updateWorkoutSetFields(
        setId: String,
        setType: String,
        reps: String,
        weightKg: String,
        rpe: String,
        notes: String,
    ) {
        val existing = state.value.activeWorkout
            ?.exerciseBlocks
            ?.flatMap { it.sets }
            ?.firstOrNull { it.id == setId }
            ?: return
        viewModelScope.launch {
            repository.updateWorkoutSet(
                setId = setId,
                input = WorkoutSetInputData(
                    setType = setType.trim().ifBlank { existing.setType },
                    reps = reps.filter(Char::isDigit).toIntOrNull(),
                    weightKg = weightKg.sanitizeDecimalInput().toDoubleOrNull(),
                    rpe = rpe.sanitizeDecimalInput().toDoubleOrNull(),
                    notes = notes,
                    completed = existing.completed,
                ),
            )
        }
    }

    fun deleteWorkoutSet(setId: String) {
        viewModelScope.launch {
            repository.deleteWorkoutSet(setId)
        }
    }

    /** Removes a standalone exercise from the active workout by deleting all of its logged sets. */
    fun removeExerciseFromActiveWorkout(exerciseId: String) {
        val setIds = state.value.activeWorkout
            ?.exerciseBlocks
            ?.firstOrNull { it.exercise.id == exerciseId }
            ?.sets
            ?.map { it.id }
            .orEmpty()
        if (setIds.isEmpty()) return
        viewModelScope.launch {
            setIds.forEach { repository.deleteWorkoutSet(it) }
        }
    }

    /** Moves a standalone exercise up (-1) or down (+1) among the active workout's exercises. */
    fun moveExerciseInActiveWorkout(exerciseId: String, direction: Int) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.moveExerciseInActiveWorkout(sessionId, exerciseId, direction)
        }
    }

    fun openReplaceExercisePicker(exerciseId: String) {
        mutableState.update { it.copy(replaceExerciseTargetId = exerciseId) }
    }

    fun closeReplaceExercisePicker() {
        mutableState.update { it.copy(replaceExerciseTargetId = null) }
    }

    /** Replaces the exercise selected for replacement, transferring its sets to [newExerciseId]. */
    fun replaceActiveWorkoutExercise(newExerciseId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        val fromExerciseId = state.value.replaceExerciseTargetId ?: return
        viewModelScope.launch {
            repository.replaceExerciseInActiveWorkout(sessionId, fromExerciseId, newExerciseId)
            mutableState.update { it.copy(replaceExerciseTargetId = null) }
        }
    }

    fun onActiveWorkoutNotesChanged(value: String) {
        mutableState.update { it.copy(activeWorkoutNotesInput = value) }
    }

    fun saveActiveWorkoutNotes() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        val notes = state.value.activeWorkoutNotesInput.trim().takeIf { it.isNotBlank() }
        viewModelScope.launch {
            repository.updateActiveWorkoutNotes(sessionId, notes)
            mutableState.update {
                it.copy(
                    activeWorkoutNotesInput = notes.orEmpty(),
                    message = "Workout notes saved.",
                )
            }
        }
    }

    fun moveWorkoutSetUp(setId: String) {
        moveWorkoutSet(setId, direction = -1)
    }

    fun moveWorkoutSetDown(setId: String) {
        moveWorkoutSet(setId, direction = 1)
    }

    private fun moveWorkoutSet(setId: String, direction: Int) {
        viewModelScope.launch {
            repository.moveWorkoutSet(setId, direction)
        }
    }

    /** Pair the given standalone exercise with the next standalone exercise below it into a superset. */
    fun makeSupersetWithNext(exerciseId: String) {
        val active = state.value.activeWorkout ?: return
        val groupings = active.exerciseGroupings
        val index = groupings.indexOfFirst {
            it is ExerciseGrouping.Single && it.block.exercise.id == exerciseId
        }
        if (index < 0) return
        val next = groupings.drop(index + 1)
            .filterIsInstance<ExerciseGrouping.Single>()
            .firstOrNull() ?: return
        viewModelScope.launch {
            repository.createSuperset(active.sessionId, exerciseId, next.block.exercise.id)
        }
    }

    fun dissolveSuperset(groupId: String) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.dissolveSuperset(sessionId, groupId)
        }
    }

    fun requestFinishActiveWorkout() {
        if (state.value.activeWorkout == null) return
        mutableState.update {
            it.copy(
                finishConfirmationOpen = true,
                discardConfirmationOpen = false,
            )
        }
    }

    fun cancelFinishActiveWorkout() {
        mutableState.update { it.copy(finishConfirmationOpen = false) }
    }

    fun requestDiscardActiveWorkout() {
        if (state.value.activeWorkout == null) return
        mutableState.update {
            it.copy(
                discardConfirmationOpen = true,
                finishConfirmationOpen = false,
            )
        }
    }

    fun cancelDiscardActiveWorkout() {
        mutableState.update { it.copy(discardConfirmationOpen = false) }
    }

    fun finishActiveWorkout() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.finishWorkout(sessionId)
            val completedDetail = repository.getWorkoutHistoryDetail(sessionId)
            mutableState.update {
                // The workout flow is done: replace whatever pages led here with just the
                // completed-workout summary, so back lands on the History list.
                it.copy(
                    pageStack = if (completedDetail == null) {
                        emptyList()
                    } else {
                        listOf(TrainingPage.WorkoutHistoryDetail)
                    },
                    finishConfirmationOpen = false,
                    discardConfirmationOpen = false,
                    selectedSection = TrainingSection.History,
                    selectedWorkoutDetail = completedDetail,
                    selectedRoutineDetail = null,
                    selectedExerciseDetail = null,
                    exerciseDetailNotesInput = "",
                    exerciseDetailTarget = null,
                    routineEditor = RoutineEditorState(),
                    restTimer = RestTimerState(),
                )
            }
        }
    }

    fun discardActiveWorkout() {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.discardWorkout(sessionId)
            mutableState.update {
                it.copy(
                    pageStack = emptyList(),
                    finishConfirmationOpen = false,
                    discardConfirmationOpen = false,
                    selectedSection = TrainingSection.Routines,
                    selectedWorkoutDetail = null,
                    selectedRoutineDetail = null,
                    selectedExerciseDetail = null,
                    exerciseDetailNotesInput = "",
                    exerciseDetailTarget = null,
                    routineEditor = RoutineEditorState(),
                    restTimer = RestTimerState(),
                )
            }
        }
    }

    private fun TrainingUiState.withCalculatedSummary(): TrainingUiState {
        val personalRecords = WorkoutCalculator.personalRecords(
            sets.map { set ->
                WorkoutSetInput(
                    exerciseId = set.exerciseName,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    completed = set.completed,
                )
            },
        )
        return copy(
            totalVolumeKg = personalRecords.totalVolumeKg,
            bestEstimatedOneRepMaxKg = personalRecords.bestEstimatedOneRepMaxKg,
        )
    }

    private fun TrainingUiState.withFilteredExercises(): TrainingUiState {
        val query = exerciseSearchQuery.trim()
        val muscle = exerciseMuscleFilter?.trim()?.takeIf(String::isNotEmpty)
        val equipment = exerciseEquipmentFilter?.trim()?.takeIf(String::isNotEmpty)
        val filtered = exercises.filter { exercise ->
            val matchesQuery = query.isBlank() ||
                exercise.name.contains(query, ignoreCase = true) ||
                exercise.targetMuscles.contains(query, ignoreCase = true) ||
                exercise.primaryMuscles.contains(query, ignoreCase = true) ||
                exercise.secondaryMuscles.contains(query, ignoreCase = true) ||
                exercise.equipment.orEmpty().contains(query, ignoreCase = true)
            val matchesMuscle = muscle == null ||
                exercise.targetMuscles.contains(muscle, ignoreCase = true) ||
                exercise.primaryMuscles.contains(muscle, ignoreCase = true) ||
                exercise.secondaryMuscles.contains(muscle, ignoreCase = true)
            val matchesEquipment = equipment == null ||
                exercise.equipment.equals(equipment, ignoreCase = true)

            matchesQuery && matchesMuscle && matchesEquipment
        }
        return copy(visibleExercises = filtered)
    }

    private fun TrainingUiState.withVisibleRoutines(): TrainingUiState {
        // Folders that pre-made (starter) routines are seeded into belong to the Library, not Home;
        // Home only surfaces folders the user creates for their own routines.
        val starterFolderIds = routines.filter { it.isStarter }.mapNotNull { it.folderId }.toSet()
        return copy(
            routineProgramOptions = emptyList(),
            selectedRoutineProgram = null,
            // Home owns user-created routines (organized into folders); the Library browses pre-made ones.
            homeRoutines = routines.filterNot { it.isStarter },
            homeFolders = routineFolders.filterNot { it.id in starterFolderIds },
            visibleRoutines = routines.filter { it.isStarter },
        )
    }

    private fun TrainingUiState.withDashboard(): TrainingUiState =
        copy(dashboard = buildTrainingDashboard(homeRoutines, workoutHistory))

    private fun moveRoutineExercise(fromIndex: Int, toIndex: Int) {
        mutableState.update { current ->
            val exercises = current.routineEditor.exercises
            if (fromIndex !in exercises.indices || toIndex !in exercises.indices) {
                current
            } else {
                val nextExercises = exercises.toMutableList()
                val moved = nextExercises.removeAt(fromIndex)
                nextExercises.add(toIndex, moved)
                current.copy(routineEditor = current.routineEditor.copy(exercises = nextExercises))
            }
        }
    }

    private fun MutableStateFlow<TrainingUiState>.updateRoutineExercise(
        index: Int,
        transform: (RoutineExerciseInput) -> RoutineExerciseInput,
    ) {
        update { current ->
            if (index !in current.routineEditor.exercises.indices) {
                current
            } else {
                current.copy(
                    routineEditor = current.routineEditor.copy(
                        exercises = current.routineEditor.exercises.mapIndexed { itemIndex, exercise ->
                            if (itemIndex == index) transform(exercise) else exercise
                        },
                    ),
                )
            }
        }
    }

    private fun MutableStateFlow<TrainingUiState>.updateRoutineSet(
        exerciseIndex: Int,
        setIndex: Int,
        transform: (RoutineSetInput) -> RoutineSetInput,
    ) {
        updateRoutineExercise(exerciseIndex) { exercise ->
            val plans = exercise.setPlans.ifEmpty { defaultSetPlans(exercise.targetSets, exercise.targetReps) }
            if (setIndex !in plans.indices) {
                exercise
            } else {
                val nextPlans = plans.mapIndexed { itemIndex, setPlan ->
                    if (itemIndex == setIndex) transform(setPlan) else setPlan
                }
                exercise.copy(
                    targetSets = nextPlans.size,
                    setPlans = nextPlans,
                )
            }
        }
    }

    private fun String.sanitizeDecimalInput(): String {
        val trimmed = trim()
        val builder = StringBuilder(trimmed.length)
        var dotSeen = false

        for (char in trimmed) {
            when {
                char.isDigit() -> builder.append(char)
                char == '.' && !dotSeen -> {
                    builder.append(char)
                    dotSeen = true
                }
            }
        }

        return builder.toString()
    }

    private fun String.parsePlateListInput(): List<Double> =
        split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0.0 }
            .distinct()
            .sortedDescending()

    private fun TrainingSettingsInput.normalized(): TrainingSettingsInput =
        copy(
            defaultRestSeconds = defaultRestSeconds.coerceIn(15, 900),
            barWeightKg = barWeightKg.takeIf { it > 0.0 } ?: 20.0,
            availablePlatesKg = availablePlatesKg
                .filter { it > 0.0 }
                .distinct()
                .sortedDescending()
                .ifEmpty { PlateCalculator.DEFAULT_PLATES },
        )

    private fun List<Double>.formatPlateListInput(): String =
        joinToString(", ") { it.formatSettingsNumber() }

    private fun Double.formatSettingsNumber(): String =
        if (this % 1.0 == 0.0) {
            toInt().toString()
        } else {
            toString()
        }

    private fun LoggedWorkoutSetDetail.isValidForCompletion(): Boolean =
        (reps ?: 0) > 0 && (weightKg ?: 0.0) > 0.0

    private fun startRestTimer(setId: String) {
        mutableState.update { current ->
            val setRestSeconds = current.activeWorkout
                ?.exerciseBlocks
                ?.flatMap { it.sets }
                ?.firstOrNull { it.id == setId }
                ?.restSeconds
            val durationSeconds = (setRestSeconds ?: current.trainingSettings.defaultRestSeconds).coerceAtLeast(1)
            current.copy(
                restTimer = RestTimerState(
                    isVisible = true,
                    sourceSetId = setId,
                    durationSeconds = durationSeconds,
                    remainingSeconds = durationSeconds,
                    isRunning = true,
                ),
            )
        }
    }

    private fun isStarterRoutine(routineId: String?): Boolean =
        routineId != null && state.value.routines.any { it.id == routineId && it.isStarter }
}

private fun defaultSetPlans(targetSets: Int, targetReps: String?): List<RoutineSetInput> =
    (0 until targetSets.coerceAtLeast(1)).map {
        RoutineSetInput(setType = "working", targetReps = targetReps)
    }

private fun List<RoutineSetInput>.resizeSetPlans(targetSets: Int, targetReps: String?): List<RoutineSetInput> {
    val desiredSize = targetSets.coerceAtLeast(1)
    return when {
        size == desiredSize -> this
        size > desiredSize -> take(desiredSize)
        else -> this + List(desiredSize - size) {
            lastOrNull()?.copy(setType = "working") ?: RoutineSetInput(setType = "working", targetReps = targetReps)
        }
    }
}

private fun String.normalizedRoutineSetType(): String =
    when (lowercase().trim()) {
        "warmup", "warm-up", "warm_up" -> "warmup"
        "drop", "drop-set", "drop_set" -> "drop"
        "failure", "fail" -> "failure"
        else -> "working"
    }

internal fun buildTrainingHistoryOverview(
    history: List<WorkoutHistorySummary>,
    today: LocalDate = LocalDate.now(),
): TrainingHistoryOverview {
    val workoutsByDate = history.groupBy { it.trainingDate() }
    val currentMonth = YearMonth.from(today)
    val firstOfMonth = currentMonth.atDay(1)
    val monthCells = mutableListOf<TrainingHistoryCalendarDay?>()
    repeat(firstOfMonth.dayOfWeek.value - 1) {
        monthCells += null
    }
    repeat(currentMonth.lengthOfMonth()) { dayIndex ->
        val date = currentMonth.atDay(dayIndex + 1)
        val workouts = workoutsByDate[date].orEmpty()
        monthCells += TrainingHistoryCalendarDay(
            date = date,
            workoutCount = workouts.size,
            completedSetCount = workouts.sumOf { it.completedSetCount },
            totalVolumeKg = workouts.sumOf { it.totalVolumeKg },
        )
    }
    while (monthCells.size % 7 != 0) {
        monthCells += null
    }

    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val endOfWeek = startOfWeek.plusDays(6)
    val currentWeekWorkouts = history.filter { workout ->
        val date = workout.trainingDate()
        !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
    }
    val trainingDays = workoutsByDate.keys.sorted()

    return TrainingHistoryOverview(
        monthLabel = currentMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
        calendarWeeks = monthCells.chunked(7),
        currentWeekWorkoutCount = currentWeekWorkouts.size,
        currentWeekTrainingDayCount = currentWeekWorkouts.map { it.trainingDate() }.distinct().size,
        currentWeekCompletedSetCount = currentWeekWorkouts.sumOf { it.completedSetCount },
        currentWeekVolumeKg = currentWeekWorkouts.sumOf { it.totalVolumeKg },
        currentStreakDays = currentStreakDays(trainingDays, today),
        bestStreakDays = bestStreakDays(trainingDays),
    )
}

internal fun buildTrainingDashboard(
    visibleRoutines: List<RoutineSummary>,
    history: List<WorkoutHistorySummary>,
): TrainingDashboardState =
    TrainingDashboardState(
        nextSuggestedRoutine = visibleRoutines.firstOrNull(),
        quickStartRoutines = visibleRoutines.take(3),
        recentWorkout = history.maxByOrNull { it.startedAtEpochMillis },
    )

private fun WorkoutHistorySummary.trainingDate(): LocalDate =
    Instant.ofEpochMilli(startedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

private fun currentStreakDays(trainingDays: List<LocalDate>, today: LocalDate): Int {
    val daySet = trainingDays.toSet()
    var cursor = when {
        today in daySet -> today
        today.minusDays(1) in daySet -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    while (cursor in daySet) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun bestStreakDays(trainingDays: List<LocalDate>): Int {
    var best = 0
    var current = 0
    var previous: LocalDate? = null
    trainingDays.forEach { day ->
        current = if (previous?.plusDays(1) == day) current + 1 else 1
        if (current > best) best = current
        previous = day
    }
    return best
}

private const val DEFAULT_REST_TIMER_SECONDS = 120
