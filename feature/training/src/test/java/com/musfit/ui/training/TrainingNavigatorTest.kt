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
    fun open_sameCurrentRoute_isANoOpWithoutCleanup() {
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey, TrainingRoutineLibraryNavKey)
        val removed = mutableListOf<TrainingNavKey>()
        val navigator = TrainingNavigator(stack) { removed += it }

        navigator.open(TrainingRoutineLibraryNavKey)

        assertEquals(listOf(TrainingHomeNavKey, TrainingRoutineLibraryNavKey), stack)
        assertTrue(removed.isEmpty())
    }

    @Test
    fun selectingRoutineSibling_prunesOldDetailAndExtraAndCleansBoth() {
        val oldDetail = TrainingRoutineDetailNavKey("routine-old")
        val oldExercise = TrainingExerciseDetailNavKey("exercise-old")
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey, TrainingRoutineLibraryNavKey, oldDetail, oldExercise)
        val removed = mutableListOf<TrainingNavKey>()
        val navigator = TrainingNavigator(stack) { removed += it }

        navigator.open(TrainingRoutineDetailNavKey("routine-new"))

        assertEquals(
            listOf(TrainingHomeNavKey, TrainingRoutineLibraryNavKey, TrainingRoutineDetailNavKey("routine-new")),
            stack,
        )
        assertEquals(listOf(oldExercise, oldDetail), removed)
    }

    @Test
    fun selectingExerciseSibling_preservesRoutineDetailAndCleansOldExtra() {
        val detail = TrainingRoutineDetailNavKey("routine-1")
        val oldExercise = TrainingExerciseDetailNavKey("exercise-old")
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey, TrainingRoutineLibraryNavKey, detail, oldExercise)
        val removed = mutableListOf<TrainingNavKey>()
        val navigator = TrainingNavigator(stack) { removed += it }

        navigator.open(TrainingExerciseDetailNavKey("exercise-new"))

        assertEquals(
            listOf(
                TrainingHomeNavKey,
                TrainingRoutineLibraryNavKey,
                detail,
                TrainingExerciseDetailNavKey("exercise-new"),
            ),
            stack,
        )
        assertEquals(listOf(oldExercise), removed)
    }

    @Test
    fun selectingWorkoutSibling_prunesOldDetailAndPreservesHistoryOwner() {
        val oldDetail = TrainingWorkoutHistoryDetailNavKey("session-old")
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey, TrainingHistoryNavKey, oldDetail)
        val removed = mutableListOf<TrainingNavKey>()
        val navigator = TrainingNavigator(stack) { removed += it }

        navigator.open(TrainingWorkoutHistoryDetailNavKey("session-new"))

        assertEquals(
            listOf(TrainingHomeNavKey, TrainingHistoryNavKey, TrainingWorkoutHistoryDetailNavKey("session-new")),
            stack,
        )
        assertEquals(listOf(oldDetail), removed)
    }

    @Test
    fun dashboardRoutineDetail_remainsDirectHomeToDetailForCompactBackParity() {
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey)
        val navigator = TrainingNavigator(stack)

        navigator.open(TrainingRoutineDetailNavKey("routine-1"))

        assertEquals(listOf(TrainingHomeNavKey, TrainingRoutineDetailNavKey("routine-1")), stack)
        assertTrue(navigator.back())
        assertEquals(listOf(TrainingHomeNavKey), stack)
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
    fun popThroughListOwner_cleansEveryPrunedRouteInTopDownOrder() {
        val detail = TrainingRoutineDetailNavKey("routine-1")
        val extra = TrainingExerciseDetailNavKey("exercise-1")
        val stack = mutableListOf<NavKey>(TrainingHomeNavKey, TrainingRoutineLibraryNavKey, detail, extra)
        val removed = mutableListOf<TrainingNavKey>()
        val navigator = TrainingNavigator(stack) { removed += it }

        assertTrue(navigator.popThrough(TrainingRoutineLibraryNavKey))

        assertEquals(listOf(TrainingHomeNavKey), stack)
        assertEquals(listOf(extra, detail, TrainingRoutineLibraryNavKey), removed)
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
