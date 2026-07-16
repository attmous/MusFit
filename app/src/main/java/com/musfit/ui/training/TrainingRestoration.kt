package com.musfit.ui.training

import com.musfit.data.repository.RoutineExerciseInput
import com.musfit.data.repository.RoutineSetInput
import java.io.Serializable

internal const val TRAINING_RESTORATION_STATE_KEY = "training.restoration.state"
internal const val MAX_RESTORED_NAME_LENGTH = 160
private const val MAX_RESTORED_ID_LENGTH = 160
private const val MAX_RESTORED_NOTES_LENGTH = 2_000
private const val MAX_RESTORED_QUERY_LENGTH = 80
private const val MAX_RESTORED_ROUTINE_EXERCISES = 50
private const val MAX_RESTORED_ROUTINE_SETS_PER_EXERCISE = 20
private const val MAX_RESTORED_PICKER_IDS = 100
private const val MAX_RESTORED_FILTER_VALUES = 20

internal data class RestoredRoutineSetDraft(
    val setType: String,
    val targetReps: String?,
    val targetWeightKg: Double?,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

internal data class RestoredRoutineExerciseDraft(
    val exerciseId: String,
    val targetSets: Int,
    val targetReps: String?,
    val restSeconds: Int?,
    val setPlans: List<RestoredRoutineSetDraft>,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

internal data class RestoredRoutineEditorDraft(
    val routineId: String?,
    val name: String,
    val notes: String,
    val folderId: String?,
    val folderName: String,
    val exercises: List<RestoredRoutineExerciseDraft>,
    val isStarter: Boolean,
    val isOpen: Boolean,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

internal data class TrainingRestorationState(
    val selectedSection: String,
    val pageStack: List<String>,
    val selectedRoutineDetailId: String?,
    val selectedExerciseDetailId: String?,
    val exerciseDetailTarget: String?,
    val selectedWorkoutDetailId: String?,
    val routineEditor: RestoredRoutineEditorDraft?,
    val routineFolderEditorId: String?,
    val routineFolderEditorName: String,
    val routineFolderEditorOpen: Boolean,
    val exerciseEditorOpen: Boolean,
    val exerciseEditorName: String,
    val exerciseEditorCategory: String,
    val exerciseEditorEquipment: String,
    val exerciseEditorTargetMuscles: String,
    val routineExercisePickerSelectedIds: List<String>,
    val routineExercisePickerSearchQuery: String,
    val routineExercisePickerEquipmentFilters: List<String>,
    val routineExercisePickerMuscleFilters: List<String>,
    val routineExercisePickerOnlyDone: Boolean,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}

internal fun String.boundedRestoredId(): String? = trim().take(MAX_RESTORED_ID_LENGTH).takeIf(String::isNotBlank)

private fun RoutineEditorState.toRestoredDraft(): RestoredRoutineEditorDraft? {
    if (!isOpen) return null
    return RestoredRoutineEditorDraft(
        routineId = routineId?.boundedRestoredId(),
        name = name.take(MAX_RESTORED_NAME_LENGTH),
        notes = notes.take(MAX_RESTORED_NOTES_LENGTH),
        folderId = folderId?.boundedRestoredId(),
        folderName = folderName.take(MAX_RESTORED_NAME_LENGTH),
        exercises = exercises.take(MAX_RESTORED_ROUTINE_EXERCISES).mapNotNull { exercise ->
            val exerciseId = exercise.exerciseId.boundedRestoredId() ?: return@mapNotNull null
            RestoredRoutineExerciseDraft(
                exerciseId = exerciseId,
                targetSets = exercise.targetSets.coerceIn(1, MAX_RESTORED_ROUTINE_SETS_PER_EXERCISE),
                targetReps = exercise.targetReps?.take(MAX_RESTORED_QUERY_LENGTH),
                restSeconds = exercise.restSeconds?.coerceIn(0, 3_600),
                setPlans = exercise.setPlans.take(MAX_RESTORED_ROUTINE_SETS_PER_EXERCISE).map { set ->
                    RestoredRoutineSetDraft(
                        setType = set.setType.take(MAX_RESTORED_QUERY_LENGTH),
                        targetReps = set.targetReps?.take(MAX_RESTORED_QUERY_LENGTH),
                        targetWeightKg = set.targetWeightKg?.takeIf { it.isFinite() && it >= 0.0 },
                    )
                },
            )
        },
        isStarter = isStarter,
        isOpen = true,
    )
}

internal fun RestoredRoutineEditorDraft.toRoutineEditorState(): RoutineEditorState = RoutineEditorState(
    routineId = routineId?.boundedRestoredId(),
    name = name.take(MAX_RESTORED_NAME_LENGTH),
    notes = notes.take(MAX_RESTORED_NOTES_LENGTH),
    folderId = folderId?.boundedRestoredId(),
    folderName = folderName.take(MAX_RESTORED_NAME_LENGTH),
    exercises = exercises.take(MAX_RESTORED_ROUTINE_EXERCISES).mapNotNull { exercise ->
        val exerciseId = exercise.exerciseId.boundedRestoredId() ?: return@mapNotNull null
        RoutineExerciseInput(
            exerciseId = exerciseId,
            targetSets = exercise.targetSets.coerceIn(1, MAX_RESTORED_ROUTINE_SETS_PER_EXERCISE),
            targetReps = exercise.targetReps?.take(MAX_RESTORED_QUERY_LENGTH),
            restSeconds = exercise.restSeconds?.coerceIn(0, 3_600),
            setPlans = exercise.setPlans.take(MAX_RESTORED_ROUTINE_SETS_PER_EXERCISE).map { set ->
                RoutineSetInput(
                    setType = set.setType.take(MAX_RESTORED_QUERY_LENGTH),
                    targetReps = set.targetReps?.take(MAX_RESTORED_QUERY_LENGTH),
                    targetWeightKg = set.targetWeightKg?.takeIf { it.isFinite() && it >= 0.0 },
                )
            },
        )
    },
    isStarter = isStarter,
    isOpen = isOpen,
)

internal fun TrainingUiState.toTrainingRestorationState(): TrainingRestorationState = TrainingRestorationState(
    selectedSection = selectedSection.name,
    pageStack = pageStack.map(TrainingPage::name).distinct().take(TrainingPage.entries.size),
    selectedRoutineDetailId = selectedRoutineDetail?.id?.boundedRestoredId(),
    selectedExerciseDetailId = selectedExerciseDetail?.id?.boundedRestoredId(),
    exerciseDetailTarget = exerciseDetailTarget?.take(MAX_RESTORED_NAME_LENGTH),
    selectedWorkoutDetailId = selectedWorkoutDetail?.summary?.sessionId?.boundedRestoredId(),
    routineEditor = routineEditor.toRestoredDraft(),
    routineFolderEditorId = routineFolderEditor.folderId?.boundedRestoredId(),
    routineFolderEditorName = routineFolderEditor.name.take(MAX_RESTORED_NAME_LENGTH),
    routineFolderEditorOpen = routineFolderEditor.isOpen,
    exerciseEditorOpen = exerciseEditor.isOpen,
    exerciseEditorName = exerciseEditor.name.take(MAX_RESTORED_NAME_LENGTH),
    exerciseEditorCategory = exerciseEditor.category.take(MAX_RESTORED_NAME_LENGTH),
    exerciseEditorEquipment = exerciseEditor.equipment.take(MAX_RESTORED_NAME_LENGTH),
    exerciseEditorTargetMuscles = exerciseEditor.targetMuscles.take(MAX_RESTORED_NAME_LENGTH),
    routineExercisePickerSelectedIds = routineExercisePickerSelectedIds
        .mapNotNull(String::boundedRestoredId)
        .distinct()
        .take(MAX_RESTORED_PICKER_IDS),
    routineExercisePickerSearchQuery = routineExercisePickerSearchQuery.take(MAX_RESTORED_QUERY_LENGTH),
    routineExercisePickerEquipmentFilters = routineExercisePickerFilters.equipment
        .map { it.take(MAX_RESTORED_NAME_LENGTH) }
        .take(MAX_RESTORED_FILTER_VALUES),
    routineExercisePickerMuscleFilters = routineExercisePickerFilters.muscles
        .map { it.take(MAX_RESTORED_NAME_LENGTH) }
        .take(MAX_RESTORED_FILTER_VALUES),
    routineExercisePickerOnlyDone = routineExercisePickerFilters.onlyDone,
)

internal fun TrainingRestorationState.toTrainingUiState(): TrainingUiState = TrainingUiState(
    selectedSection = TrainingSection.entries.firstOrNull { it.name == selectedSection }
        ?: TrainingSection.Routines,
    pageStack = pageStack.mapNotNull { savedPage ->
        TrainingPage.entries.firstOrNull { it.name == savedPage }
    }.distinct().take(TrainingPage.entries.size),
    routineEditor = routineEditor?.toRoutineEditorState() ?: RoutineEditorState(),
    routineFolderEditor = RoutineFolderEditorState(
        folderId = routineFolderEditorId?.boundedRestoredId(),
        name = routineFolderEditorName.take(MAX_RESTORED_NAME_LENGTH),
        isOpen = routineFolderEditorOpen,
    ),
    exerciseEditor = ExerciseEditorState(
        isOpen = exerciseEditorOpen,
        name = exerciseEditorName.take(MAX_RESTORED_NAME_LENGTH),
        category = exerciseEditorCategory.take(MAX_RESTORED_NAME_LENGTH),
        equipment = exerciseEditorEquipment.take(MAX_RESTORED_NAME_LENGTH),
        targetMuscles = exerciseEditorTargetMuscles.take(MAX_RESTORED_NAME_LENGTH),
    ),
    routineExercisePickerSelectedIds = routineExercisePickerSelectedIds
        .mapNotNull(String::boundedRestoredId)
        .toSet(),
    routineExercisePickerSearchQuery = routineExercisePickerSearchQuery.take(MAX_RESTORED_QUERY_LENGTH),
    routineExercisePickerFilters = TrainingPickerFilters(
        equipment = routineExercisePickerEquipmentFilters.take(MAX_RESTORED_FILTER_VALUES).toSet(),
        muscles = routineExercisePickerMuscleFilters.take(MAX_RESTORED_FILTER_VALUES).toSet(),
        onlyDone = routineExercisePickerOnlyDone,
    ),
)
