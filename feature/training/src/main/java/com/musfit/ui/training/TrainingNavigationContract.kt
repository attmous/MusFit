package com.musfit.ui.training

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface TrainingNavKey : NavKey

@Serializable
data object TrainingHomeNavKey : TrainingNavKey

@Serializable
data object TrainingRoutineLibraryNavKey : TrainingNavKey

@Serializable
data class TrainingRoutineDetailNavKey(val routineId: String) : TrainingNavKey

@Serializable
data class TrainingRoutineEditorNavKey(val routineId: String? = null) : TrainingNavKey

@Serializable
data class TrainingExerciseDetailNavKey(
    val exerciseId: String,
    val target: String? = null,
) : TrainingNavKey

@Serializable
data object TrainingExercisePickerNavKey : TrainingNavKey

@Serializable
data object TrainingHistoryNavKey : TrainingNavKey

@Serializable
data class TrainingWorkoutHistoryDetailNavKey(val sessionId: String) : TrainingNavKey

@Serializable
data object TrainingProgressFeatureNavKey : TrainingNavKey

@Serializable
data object TrainingActiveWorkoutNavKey : TrainingNavKey

internal class TrainingNavigator(
    private val backStack: MutableList<NavKey>,
    private val onRoutesRemoved: (List<TrainingNavKey>) -> Unit = {},
) {
    val currentKey: TrainingNavKey
        get() = backStack.lastOrNull() as? TrainingNavKey ?: TrainingHomeNavKey

    fun open(key: TrainingNavKey) {
        if (backStack.lastOrNull() == key) return
        val removed = when (key) {
            is TrainingRoutineDetailNavKey -> pruneRoutineSelection()
            is TrainingExerciseDetailNavKey -> pruneExerciseSelection()
            is TrainingWorkoutHistoryDetailNavKey -> pruneHistorySelection()
            else -> emptyList()
        }
        if (removed.isNotEmpty()) onRoutesRemoved(removed)
        if (backStack.lastOrNull() != key) backStack += key
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        val removed = backStack.removeAt(backStack.lastIndex) as? TrainingNavKey
        if (removed != null) onRoutesRemoved(listOf(removed))
        return true
    }

    fun popThrough(key: TrainingNavKey): Boolean {
        val index = backStack.indexOfLast { it == key }
        if (index < 0) return false
        val removed = buildList {
            while (backStack.lastIndex >= index) {
                (backStack.removeAt(backStack.lastIndex) as? TrainingNavKey)?.let(::add)
            }
        }
        if (removed.isNotEmpty()) onRoutesRemoved(removed)
        if (backStack.isEmpty()) backStack += TrainingHomeNavKey
        return true
    }

    fun resetTo(keys: List<TrainingNavKey>) {
        val removed = backStack.mapNotNull { it as? TrainingNavKey }.asReversed()
        backStack.clear()
        if (removed.isNotEmpty()) onRoutesRemoved(removed)
        backStack += TrainingHomeNavKey
        keys.forEach(::open)
    }

    private fun pruneRoutineSelection(): List<TrainingNavKey> = pruneAfterLastOwner(
        owner = TrainingRoutineLibraryNavKey,
        shouldPrune = { it is TrainingRoutineDetailNavKey || it is TrainingExerciseDetailNavKey },
    )

    private fun pruneExerciseSelection(): List<TrainingNavKey> {
        val detailIndex = backStack.indexOfLast { it is TrainingRoutineDetailNavKey }
        if (detailIndex < 0) return emptyList()
        return removeMatchingAfter(detailIndex) { it is TrainingExerciseDetailNavKey }
    }

    private fun pruneHistorySelection(): List<TrainingNavKey> = pruneAfterLastOwner(
        owner = TrainingHistoryNavKey,
        shouldPrune = { it is TrainingWorkoutHistoryDetailNavKey },
    )

    private fun pruneAfterLastOwner(
        owner: TrainingNavKey,
        shouldPrune: (TrainingNavKey) -> Boolean,
    ): List<TrainingNavKey> {
        val ownerIndex = backStack.indexOfLast { it == owner }
        if (ownerIndex < 0) return emptyList()
        return removeMatchingAfter(ownerIndex, shouldPrune)
    }

    private fun removeMatchingAfter(
        ownerIndex: Int,
        shouldPrune: (TrainingNavKey) -> Boolean,
    ): List<TrainingNavKey> = buildList {
        for (index in backStack.lastIndex downTo ownerIndex + 1) {
            val route = backStack[index] as? TrainingNavKey ?: continue
            if (shouldPrune(route)) {
                backStack.removeAt(index)
                add(route)
            }
        }
    }
}
