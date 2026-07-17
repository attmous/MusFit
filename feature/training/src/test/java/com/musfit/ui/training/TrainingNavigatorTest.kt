package com.musfit.ui.training

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainingNavigatorTest {
    @Test
    fun routeStack_preservesRoutineContextAndBackOrder() {
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey)
        val navigator = TrainingNavigator(stack)

        navigator.open(TrainingRoutineLibraryNavKey)
        navigator.open(TrainingRoutineDetailNavKey("routine-1"))
        navigator.open(TrainingRoutineEditorNavKey("routine-1"))

        assertEquals(TrainingRoutineEditorNavKey("routine-1"), navigator.currentKey)
        assertTrue(navigator.back())
        assertEquals(TrainingRoutineDetailNavKey("routine-1"), navigator.currentKey)
        assertTrue(navigator.back())
        assertEquals(TrainingRoutineLibraryNavKey, navigator.currentKey)
    }

    @Test
    fun popThroughLibrary_returnsToHomeWithoutDuplicateOwner() {
        val stack = mutableListOf<NavKey>(
            TrainingHomeNavKey,
            TrainingRoutineLibraryNavKey,
            TrainingRoutineDetailNavKey("routine-1"),
        )
        val navigator = TrainingNavigator(stack)

        assertTrue(navigator.popThrough(TrainingRoutineLibraryNavKey))
        assertEquals(listOf(TrainingHomeNavKey), stack)
        assertFalse(navigator.popThrough(TrainingRoutineLibraryNavKey))
    }

    @Test
    fun resetTo_replacesTheFeatureStackWithOneHomeOwner() {
        val stack = mutableListOf<NavKey>(
            TrainingHomeNavKey,
            TrainingRoutineLibraryNavKey,
            TrainingRoutineDetailNavKey("routine-old"),
        )
        val navigator = TrainingNavigator(stack)

        navigator.resetTo(listOf(TrainingHistoryNavKey, TrainingWorkoutHistoryDetailNavKey("session-7")))

        assertEquals(
            listOf(TrainingHomeNavKey, TrainingHistoryNavKey, TrainingWorkoutHistoryDetailNavKey("session-7")),
            stack,
        )
    }

    @Test
    fun typedStack_roundTripsThroughProcessSavedState() {
        val stack: List<TrainingNavKey> = listOf(
            TrainingHomeNavKey,
            TrainingRoutineLibraryNavKey,
            TrainingRoutineDetailNavKey("routine-3"),
            TrainingExerciseDetailNavKey("exercise-8", "3 x 10"),
            TrainingActiveWorkoutNavKey,
        )

        val restored = decodeFromSavedState<List<TrainingNavKey>>(encodeToSavedState(stack))

        assertEquals(stack, restored)
    }
}
