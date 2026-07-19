package com.musfit.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musfit.data.repository.ActiveWorkoutDetail
import com.musfit.data.repository.ActiveWorkoutSummary
import com.musfit.data.repository.ExerciseDetail
import com.musfit.data.repository.ExerciseGrouping
import com.musfit.data.repository.ExerciseInput
import com.musfit.data.repository.ExerciseSummary
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.LoggedWorkoutSetDetail
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineFolder
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineSetInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSettings
import com.musfit.data.repository.TrainingSettingsInput
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutSetInputData
import com.musfit.domain.model.WorkoutSetInput
import com.musfit.domain.training.PlateCalculator
import com.musfit.domain.training.WarmupSetCalculator
import com.musfit.domain.training.WorkoutCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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

/** Fallback weekly session target until the stored user goal streams in. */
internal const val DEFAULT_WEEKLY_SESSION_TARGET = 3

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

/**
 * The exercise picker's filter set (mock 5e): filters left the page's chip rows and live in one
 * bottom-sheet-backed state object — multi-select equipment + muscle pills and an "only exercises
 * I've done" toggle. [activeCount] feeds the badge on the picker's `tune` button.
 */
data class TrainingPickerFilters(
    val equipment: Set<String> = emptySet(),
    val muscles: Set<String> = emptySet(),
    val onlyDone: Boolean = false,
) {
    val activeCount: Int
        get() = equipment.size + muscles.size + (if (onlyDone) 1 else 0)

    val isActive: Boolean
        get() = activeCount > 0
}

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
    val weeklySessionTarget: Int = DEFAULT_WEEKLY_SESSION_TARGET,
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
    val routineExercisePickerFilters: TrainingPickerFilters = TrainingPickerFilters(),
    val routineExercisePickerFilterSheetOpen: Boolean = false,
    val loggedExerciseIds: Set<String> = emptySet(),
    val selectedRoutineDetail: RoutineDetail? = null,
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
)

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val repository: TrainingRepository,
    private val goalsRepository: GoalsRepository,
    private val savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {
    private val restoredState = savedStateHandle
        ?.get<TrainingRestorationState>(TRAINING_RESTORATION_STATE_KEY)
    private val mutableState = MutableStateFlow(restoredState?.toTrainingUiState() ?: TrainingUiState())
    val state: StateFlow<TrainingUiState> = mutableState.asStateFlow()
    private val observedState: StateFlow<TrainingUiState> = repositoryObservationFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = mutableState.value,
        )
    val routinesLibraryState: StateFlow<TrainingRoutinesLibraryUiState> = observedState
        .map(TrainingPresentationReducers::routinesLibrary)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrainingPresentationReducers.routinesLibrary(mutableState.value),
        )
    val activeHistoryState: StateFlow<TrainingActiveHistoryUiState> = observedState
        .map(TrainingPresentationReducers::activeHistory)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrainingPresentationReducers.activeHistory(mutableState.value),
        )
    private val workoutDetailLoad = RouteLoadGuard()
    private val routineDetailLoad = RouteLoadGuard()
    private val routineEditorLoad = RouteLoadGuard()
    private val exerciseDetailLoad = RouteLoadGuard()
    private val restoredWorkoutDetailRequest = workoutDetailLoad.currentRequest()
    private val restoredRoutineDetailRequest = routineDetailLoad.currentRequest()
    private val restoredRoutineEditorRequest = routineEditorLoad.currentRequest()
    private val restoredExerciseDetailRequest = exerciseDetailLoad.currentRequest()

    init {
        viewModelScope.launch {
            // Starter routine IDs must exist before restored page IDs are resolved. Beginning
            // persistence only after resolution also prevents a transient empty snapshot from
            // overwriting those IDs if the process is killed again during startup.
            repository.seedStarterTrainingData()
            restoredState?.let { restoration -> restoreReferencedPages(restoration) }
            mutableState.collect { currentState ->
                savedStateHandle?.set(
                    TRAINING_RESTORATION_STATE_KEY,
                    currentState.toTrainingRestorationState(),
                )
            }
        }
    }

    @Suppress("LongMethod") // One shared lifecycle boundary owns every UI data observer.
    private fun repositoryObservationFlow(): Flow<TrainingUiState> = channelFlow {
        supervisorScope {
            launchRepositoryObserver {
                // The "This week" card measures against the same weekly session target
                // the Today dashboard editor writes.
                goalsRepository.observeUserGoals().collect { goals ->
                    mutableState.update { it.copy(weeklySessionTarget = goals.weeklySessionTarget) }
                }
            }
            launchRepositoryObserver {
                repository.observeRoutineSummaries().collect { routines ->
                    mutableState.update {
                        it.copy(routines = routines)
                            .withVisibleRoutines()
                            .withDashboard()
                    }
                }
            }
            launchRepositoryObserver {
                repository.observeRoutineFolders().collect { folders ->
                    mutableState.update { it.copy(routineFolders = folders).withVisibleRoutines() }
                }
            }
            launchRepositoryObserver {
                repository.observeExercises().collect { exercises ->
                    mutableState.update { it.copy(exercises = exercises).withFilteredExercises() }
                }
            }
            launchRepositoryObserver {
                repository.observeActiveWorkoutSummary().collect { activeWorkout ->
                    mutableState.update { it.copy(activeWorkoutSummary = activeWorkout).withDashboard() }
                }
            }
            launchRepositoryObserver {
                repository.observeActiveWorkoutDetail().collect { activeWorkout ->
                    mutableState.update { current ->
                        val previousActiveWorkout = current.activeWorkout
                        val shouldSyncNotes = activeWorkout == null ||
                            previousActiveWorkout?.sessionId != activeWorkout.sessionId ||
                            previousActiveWorkout.notes != activeWorkout.notes
                        val updated = current.copy(
                            activeWorkout = activeWorkout,
                            activeWorkoutNotesInput = if (shouldSyncNotes) {
                                activeWorkout?.notes.orEmpty()
                            } else {
                                current.activeWorkoutNotesInput
                            },
                        )
                        if (activeWorkout == null) {
                            updated.copy(
                                restTimer = RestTimerState(),
                                finishConfirmationOpen = false,
                                discardConfirmationOpen = false,
                            )
                        } else {
                            updated
                        }
                    }
                }
            }
            launchRepositoryObserver {
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
            launchRepositoryObserver {
                repository.observeWorkoutHistory().collect { history ->
                    mutableState.update {
                        it.copy(
                            workoutHistory = history,
                            historyOverview = buildTrainingHistoryOverview(history),
                        ).withDashboard()
                    }
                }
            }
            launchRepositoryObserver {
                repository.observeLoggedExerciseIds().collect { ids ->
                    mutableState.update { it.copy(loggedExerciseIds = ids) }
                }
            }
            mutableState.collect { send(it) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun CoroutineScope.launchRepositoryObserver(observe: suspend () -> Unit): Job = launch {
        try {
            observe()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mutableState.update { current ->
                current.copy(message = error.message ?: TRAINING_DATA_OBSERVATION_ERROR_MESSAGE)
            }
        }
    }

    fun selectSection(section: TrainingSection) {
        mutableState.update { it.copy(selectedSection = section) }
    }

    fun resumeActiveWorkout() {
        // Navigation 3 owns the active-workout destination; Room owns its content.
    }

    fun openWorkoutDetail(sessionId: String, onMissing: () -> Unit = {}) {
        val request = workoutDetailLoad.begin()
        workoutDetailLoad.job = viewModelScope.launch {
            val detail = repository.getWorkoutHistoryDetail(sessionId)
            if (!workoutDetailLoad.isCurrent(request)) return@launch
            mutableState.update {
                if (detail == null) {
                    it.copy(selectedWorkoutDetail = null)
                } else {
                    it.copy(selectedWorkoutDetail = detail)
                }
            }
            if (detail == null) onMissing()
        }
    }

    fun closeWorkoutDetail() {
        workoutDetailLoad.invalidate()
        mutableState.update {
            it.copy(selectedWorkoutDetail = null)
        }
    }

    fun closeActiveWorkoutRoute() {
        mutableState.update {
            it.copy(
                restTimer = RestTimerState(),
                finishConfirmationOpen = false,
                discardConfirmationOpen = false,
                selectedSection = TrainingSection.Routines,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun restoreReferencedPages(restoration: TrainingRestorationState) {
        val routineId = restoration.selectedRoutineDetailId?.boundedRestoredId()
        val exerciseId = restoration.selectedExerciseDetailId?.boundedRestoredId()
        val workoutId = restoration.selectedWorkoutDetailId?.boundedRestoredId()
        val routineDetail = if (routineDetailLoad.isCurrent(restoredRoutineDetailRequest)) {
            routineId?.let { repository.getRoutineDetail(it) }
        } else {
            null
        }
        val exerciseDetail = if (exerciseDetailLoad.isCurrent(restoredExerciseDetailRequest)) {
            exerciseId?.let { repository.getExerciseDetail(it) }
        } else {
            null
        }
        val workoutDetail = if (workoutDetailLoad.isCurrent(restoredWorkoutDetailRequest)) {
            workoutId?.let { repository.getWorkoutHistoryDetail(it) }
        } else {
            null
        }
        val restoredEditor = restoration.routineEditor?.toRoutineEditorState()
        val existingEditorStillValid = routineEditorLoad.isCurrent(restoredRoutineEditorRequest) &&
            (
                restoredEditor?.routineId
                    ?.let { editorId ->
                        if (editorId == routineId && routineDetailLoad.isCurrent(restoredRoutineDetailRequest)) {
                            routineDetail
                        } else {
                            repository.getRoutineDetail(editorId)
                        }
                    } != null || restoredEditor?.routineId == null
                )

        mutableState.update { current ->
            var restored = current
            if (routineDetailLoad.isCurrent(restoredRoutineDetailRequest)) {
                restored = restored.copy(selectedRoutineDetail = routineDetail)
            }
            if (exerciseDetailLoad.isCurrent(restoredExerciseDetailRequest)) {
                restored = restored.copy(
                    selectedExerciseDetail = exerciseDetail,
                    exerciseDetailNotesInput = exerciseDetail?.localNotes.orEmpty(),
                    exerciseDetailTarget = restoration.exerciseDetailTarget?.take(MAX_RESTORED_NAME_LENGTH),
                )
            }
            if (workoutDetailLoad.isCurrent(restoredWorkoutDetailRequest)) {
                restored = restored.copy(selectedWorkoutDetail = workoutDetail)
            }
            if (routineEditorLoad.isCurrent(restoredRoutineEditorRequest)) {
                restored = restored.copy(
                    routineEditor = if (existingEditorStillValid) {
                        restoredEditor ?: RoutineEditorState()
                    } else {
                        RoutineEditorState()
                    },
                )
            }
            restored
        }
    }

    fun openRoutineLibraryPage() {
        mutableState.update { it.copy(message = null) }
    }

    fun closeRoutineLibraryPage() {
        routineDetailLoad.invalidate()
        routineEditorLoad.invalidate()
        exerciseDetailLoad.invalidate()
        mutableState.update {
            it.copy(
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

    fun openRoutineEditor(routineId: String?, onMissing: () -> Unit = {}) {
        val request = routineEditorLoad.begin()
        routineEditorLoad.job = viewModelScope.launch {
            val detail = routineId?.let { repository.getRoutineDetail(it) }
            if (!routineEditorLoad.isCurrent(request)) return@launch
            if (routineId != null && detail == null) {
                mutableState.update { it.copy(message = "Routine not found.") }
                onMissing()
                return@launch
            }
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
                )
            }
        }
    }

    fun closeRoutineEditor() {
        routineEditorLoad.invalidate()
        mutableState.update {
            it.copy(routineEditor = RoutineEditorState())
        }
    }

    fun openRoutineDetail(routineId: String, onMissing: () -> Unit = {}) {
        val request = routineDetailLoad.begin()
        routineDetailLoad.job = viewModelScope.launch {
            val detail = repository.getRoutineDetail(routineId)
            if (!routineDetailLoad.isCurrent(request)) return@launch
            mutableState.update {
                if (detail == null) {
                    it.copy(message = "Routine not found.")
                } else {
                    it.copy(
                        selectedRoutineDetail = detail,
                        message = null,
                    )
                }
            }
            if (detail == null) onMissing()
        }
    }

    fun closeRoutineDetail() {
        routineDetailLoad.invalidate()
        mutableState.update {
            it.copy(selectedRoutineDetail = null)
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
            it.copy(
                routineExercisePickerSelectedIds = emptySet(),
                routineExercisePickerSearchQuery = "",
                routineExercisePickerFilters = TrainingPickerFilters(),
                routineExercisePickerFilterSheetOpen = false,
            )
        }
    }

    fun closeRoutineExercisePicker() {
        mutableState.update {
            it.copy(
                routineExercisePickerSelectedIds = emptySet(),
                routineExercisePickerFilterSheetOpen = false,
            )
        }
    }

    fun onRoutineExercisePickerSearchChanged(value: String) {
        mutableState.update { it.copy(routineExercisePickerSearchQuery = value) }
    }

    fun openRoutineExercisePickerFilters() {
        mutableState.update { it.copy(routineExercisePickerFilterSheetOpen = true) }
    }

    fun closeRoutineExercisePickerFilters() {
        mutableState.update { it.copy(routineExercisePickerFilterSheetOpen = false) }
    }

    fun toggleRoutineExercisePickerEquipment(value: String) {
        mutableState.update { current ->
            val filters = current.routineExercisePickerFilters
            current.copy(
                routineExercisePickerFilters = filters.copy(
                    equipment = filters.equipment.toggled(value),
                ),
            )
        }
    }

    fun toggleRoutineExercisePickerMuscle(value: String) {
        mutableState.update { current ->
            val filters = current.routineExercisePickerFilters
            current.copy(
                routineExercisePickerFilters = filters.copy(
                    muscles = filters.muscles.toggled(value),
                ),
            )
        }
    }

    fun setRoutineExercisePickerOnlyDone(value: Boolean) {
        mutableState.update {
            it.copy(routineExercisePickerFilters = it.routineExercisePickerFilters.copy(onlyDone = value))
        }
    }

    /** The sheet's "Reset" — clears the filter set but keeps the search query. */
    fun resetRoutineExercisePickerFilters() {
        mutableState.update { it.copy(routineExercisePickerFilters = TrainingPickerFilters()) }
    }

    fun clearRoutineExercisePickerFilters() {
        mutableState.update {
            it.copy(
                routineExercisePickerSearchQuery = "",
                routineExercisePickerFilters = TrainingPickerFilters(),
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

    fun saveRoutineEditor(onSaved: () -> Unit = {}) {
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
                it.copy(
                    routineEditor = RoutineEditorState(),
                    selectedRoutineDetail = refreshedDetail ?: it.selectedRoutineDetail,
                )
            }
            onSaved()
        }
    }

    fun duplicateRoutine(routineId: String) {
        viewModelScope.launch {
            repository.duplicateRoutine(routineId)
        }
    }

    fun deleteRoutine(routineId: String, onDeleted: () -> Unit = {}) {
        if (isStarterRoutine(routineId)) {
            mutableState.update { it.copy(message = "Starter routines are read-only templates.") }
            return
        }
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            mutableState.update { current ->
                var next = current
                if (next.routineEditor.routineId == routineId) {
                    next = next.copy(routineEditor = RoutineEditorState())
                }
                if (next.selectedRoutineDetail?.id == routineId) {
                    next = next.copy(selectedRoutineDetail = null)
                }
                next
            }
            onDeleted()
        }
    }

    fun startBlankWorkout(onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.startBlankWorkout()
            onStarted()
        }
    }

    fun startRoutine(routineId: String, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.startWorkoutFromRoutine(routineId)
            // The routine detail / library stay on the stack beneath the workout page, so backing
            // out of a running workout returns to where it was started from.
            onStarted()
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
    fun openExerciseDetail(exerciseId: String, onMissing: () -> Unit = {}) = loadExerciseDetail(exerciseId, target = null, switchToExercises = true, onMissing = onMissing)

    /**
     * Opens the same exercise page from inside a routine, layered over the routine detail (no
     * section switch) and carrying the planned sets x reps so the page can show the target.
     */
    fun openRoutineExerciseDetail(exerciseId: String, target: String?, onMissing: () -> Unit = {}) = loadExerciseDetail(exerciseId, target = target, switchToExercises = false, onMissing = onMissing)

    private fun loadExerciseDetail(
        exerciseId: String,
        target: String?,
        switchToExercises: Boolean,
        onMissing: () -> Unit,
    ) {
        val request = exerciseDetailLoad.begin()
        exerciseDetailLoad.job = viewModelScope.launch {
            val detail = repository.getExerciseDetail(exerciseId)
            if (!exerciseDetailLoad.isCurrent(request)) return@launch
            mutableState.update {
                if (detail == null) {
                    it.copy(message = "Exercise not found.")
                } else {
                    it.copy(
                        selectedSection = if (switchToExercises) TrainingSection.Exercises else it.selectedSection,
                        selectedExerciseDetail = detail,
                        exerciseDetailNotesInput = detail.localNotes.orEmpty(),
                        exerciseDetailTarget = target,
                        message = null,
                    )
                }
            }
            if (detail == null) onMissing()
        }
    }

    fun closeExerciseDetail() {
        exerciseDetailLoad.invalidate()
        mutableState.update {
            it.copy(
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
        val request = exerciseDetailLoad.currentRequest()
        viewModelScope.launch {
            repository.updateExerciseLocalNotes(detail.id, notes)
            val updated = repository.getExerciseDetail(detail.id)?.copy(localNotes = notes)
            if (exerciseDetailLoad.isCurrent(request)) {
                mutableState.update { current ->
                    if (current.selectedExerciseDetail?.id != detail.id) {
                        current
                    } else {
                        current.copy(
                            selectedExerciseDetail = updated,
                            exerciseDetailNotesInput = notes.orEmpty(),
                            message = "Exercise notes saved.",
                        )
                    }
                }
            }
        }
    }

    // No section switch: since Turn 5 the custom-exercise editor opens in place (picker sheet or
    // library page) instead of yanking the user to the Exercises section.
    fun openCustomExerciseEditor() {
        mutableState.update {
            it.copy(exerciseEditor = ExerciseEditorState(isOpen = true))
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

    fun finishActiveWorkout(onFinished: (String?) -> Unit = {}) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.finishWorkout(sessionId)
            val completedDetail = repository.getWorkoutHistoryDetail(sessionId)
            mutableState.update {
                it.copy(
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
            onFinished(completedDetail?.summary?.sessionId)
        }
    }

    fun discardActiveWorkout(onDiscarded: () -> Unit = {}) {
        val sessionId = state.value.activeWorkout?.sessionId ?: return
        viewModelScope.launch {
            repository.discardWorkout(sessionId)
            mutableState.update {
                it.copy(
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
            onDiscarded()
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

    private fun TrainingUiState.withDashboard(): TrainingUiState = copy(dashboard = buildTrainingDashboard(homeRoutines, workoutHistory))

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

    private fun String.parsePlateListInput(): List<Double> = split(",")
        .mapNotNull { it.trim().toDoubleOrNull() }
        .filter { it > 0.0 }
        .distinct()
        .sortedDescending()

    private fun TrainingSettingsInput.normalized(): TrainingSettingsInput = copy(
        defaultRestSeconds = defaultRestSeconds.coerceIn(15, 900),
        barWeightKg = barWeightKg.takeIf { it > 0.0 } ?: 20.0,
        availablePlatesKg = availablePlatesKg
            .filter { it > 0.0 }
            .distinct()
            .sortedDescending()
            .ifEmpty { PlateCalculator.DEFAULT_PLATES },
    )

    private fun List<Double>.formatPlateListInput(): String = joinToString(", ") { it.formatSettingsNumber() }

    private fun Double.formatSettingsNumber(): String = if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }

    private fun LoggedWorkoutSetDetail.isValidForCompletion(): Boolean = (reps ?: 0) > 0 && (weightKg ?: 0.0) > 0.0

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

    private fun isStarterRoutine(routineId: String?): Boolean = routineId != null && state.value.routines.any { it.id == routineId && it.isStarter }

    private fun Set<String>.toggled(value: String): Set<String> = if (any { it.equals(value, ignoreCase = true) }) {
        filterNot { it.equals(value, ignoreCase = true) }.toSet()
    } else {
        this + value
    }

    private class RouteLoadGuard {
        private var request = 0L
        var job: Job? = null

        fun begin(): Long {
            job?.cancel()
            job = null
            request += 1
            return request
        }

        fun invalidate() {
            begin()
        }

        fun currentRequest(): Long = request

        fun isCurrent(candidate: Long): Boolean = candidate == request
    }
}

private fun defaultSetPlans(targetSets: Int, targetReps: String?): List<RoutineSetInput> = (0 until targetSets.coerceAtLeast(1)).map {
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

private fun String.normalizedRoutineSetType(): String = when (lowercase().trim()) {
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
): TrainingDashboardState = TrainingDashboardState(
    nextSuggestedRoutine = visibleRoutines.firstOrNull(),
    quickStartRoutines = visibleRoutines.take(3),
    recentWorkout = history.maxByOrNull { it.startedAtEpochMillis },
)

private fun WorkoutHistorySummary.trainingDate(): LocalDate = Instant.ofEpochMilli(startedAtEpochMillis)
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
private const val TRAINING_DATA_OBSERVATION_ERROR_MESSAGE = "Training data could not be refreshed"
