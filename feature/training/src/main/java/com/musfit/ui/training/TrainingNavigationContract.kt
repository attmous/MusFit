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

internal class TrainingNavigator(private val backStack: MutableList<NavKey>) {
    val currentKey: TrainingNavKey
        get() = backStack.lastOrNull() as? TrainingNavKey ?: TrainingHomeNavKey

    fun open(key: TrainingNavKey) {
        if (backStack.lastOrNull() != key) backStack += key
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun popThrough(key: TrainingNavKey): Boolean {
        val index = backStack.indexOfLast { it == key }
        if (index < 0) return false
        while (backStack.lastIndex >= index) backStack.removeAt(backStack.lastIndex)
        if (backStack.isEmpty()) backStack += TrainingHomeNavKey
        return true
    }

    fun resetTo(keys: List<TrainingNavKey>) {
        backStack.clear()
        backStack += TrainingHomeNavKey
        keys.forEach(::open)
    }
}
