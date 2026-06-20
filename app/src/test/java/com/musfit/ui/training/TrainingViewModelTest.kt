package com.musfit.ui.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow

class TrainingViewModelTest {
    @Test
    fun addCompletedSet_updatesVolume() {
        val viewModel = TrainingViewModel()

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("100")
        viewModel.addSet()

        val state = viewModel.state.value
        assertEquals(1, state.sets.size)
        assertEquals(500.0, state.totalVolumeKg, 0.01)
        assertEquals(116.67, state.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun addSet_rejectsZeroReps() {
        val viewModel = TrainingViewModel()

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("0")
        viewModel.onWeightChanged("100")
        viewModel.addSet()

        val state = viewModel.state.value
        assertTrue(state.sets.isEmpty())
        assertEquals(0.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun addSet_rejectsNegativeReps() {
        val viewModel = TrainingViewModel()

        viewModel.forceState(
            exerciseName = "Bench Press",
            reps = "-5",
            weightKg = "100",
        )
        viewModel.addSet()

        val state = viewModel.state.value
        assertTrue(state.sets.isEmpty())
        assertEquals(0.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun addSet_rejectsZeroWeight() {
        val viewModel = TrainingViewModel()

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("0")
        viewModel.addSet()

        val state = viewModel.state.value
        assertTrue(state.sets.isEmpty())
        assertEquals(0.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun addSet_rejectsNegativeWeight() {
        val viewModel = TrainingViewModel()

        viewModel.forceState(
            exerciseName = "Bench Press",
            reps = "5",
            weightKg = "-100",
        )
        viewModel.addSet()

        val state = viewModel.state.value
        assertTrue(state.sets.isEmpty())
        assertEquals(0.0, state.totalVolumeKg, 0.01)
    }

    @Test
    fun toggleSetCompletion_excludesIncompleteSetFromVolume() {
        val viewModel = TrainingViewModel()

        viewModel.onExerciseChanged("Bench Press")
        viewModel.onRepsChanged("5")
        viewModel.onWeightChanged("100")
        viewModel.addSet()

        viewModel.toggleSetCompletion(setIndex = 0)

        val state = viewModel.state.value
        assertFalse(state.sets[0].completed)
        assertEquals(0.0, state.totalVolumeKg, 0.01)
        assertEquals(0.0, state.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun addSet_withBlankExercise_usesCustomLabel() {
        val viewModel = TrainingViewModel()

        viewModel.onRepsChanged("8")
        viewModel.onWeightChanged("60")
        viewModel.addSet()

        val state = viewModel.state.value
        assertEquals("custom", state.sets.single().exerciseId)
        assertTrue(state.sets.single().completed)
        assertEquals(480.0, state.totalVolumeKg, 0.01)
    }

    private fun TrainingViewModel.forceState(
        exerciseName: String,
        reps: String,
        weightKg: String,
    ) {
        val field = TrainingViewModel::class.java.getDeclaredField("mutableState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutableState = field.get(this) as MutableStateFlow<TrainingUiState>
        mutableState.value = TrainingUiState(
            exerciseName = exerciseName,
            reps = reps,
            weightKg = weightKg,
            sets = emptyList(),
        )
    }
}
