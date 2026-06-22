package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import com.musfit.data.repository.WorkoutSetInputData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class LocalTrainingRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalTrainingRepository
    private var currentInstant: Instant = WORKOUT_START

    @Before
    fun setUp() {
        currentInstant = WORKOUT_START
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = LocalTrainingRepository(
            database = database,
            trainingDao = database.trainingDao(),
            clock = { currentInstant.toEpochMilli() },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addCompletedSet_persistsExerciseSessionSetAndDailySummary() = runTest {
        val savedSet = repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val sets = database.trainingDao().getWorkoutSets(sessions.single().id)
        val summary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()

        assertEquals("Bench Press", savedSet.exerciseName)
        assertEquals(5, savedSet.reps)
        assertEquals(100.0, savedSet.weightKg, 0.01)
        assertEquals(true, savedSet.completed)
        assertEquals(sessions.single().id, sets.single().sessionId)
        assertEquals(1, summary.completedSetCount)
        assertEquals(500.0, summary.totalVolumeKg, 0.01)
        assertEquals(116.67, summary.bestEstimatedOneRepMaxKg, 0.01)
    }

    @Test
    fun setCompletion_updatesPersistedSummary() = runTest {
        val savedSet = repository.addCompletedSet(
            exerciseName = "Squat",
            reps = 3,
            weightKg = 120.0,
        )

        repository.setCompletion(savedSet.id, completed = false)

        val summary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()
        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val sets = database.trainingDao().getWorkoutSets(sessions.single().id)

        assertEquals(false, sets.single().completed)
        assertEquals(0, summary.completedSetCount)
        assertEquals(0.0, summary.totalVolumeKg, 0.01)
    }

    @Test
    fun addCompletedSet_createsCompletedSessionExportableFromQuickLoggerPath() = runTest {
        repository.addCompletedSet(
            exerciseName = "Deadlift",
            reps = 2,
            weightKg = 160.0,
        )

        val workout = repository.getLatestWorkoutForExport()

        assertNotNull(workout)
        assertEquals("completed", workout?.session?.status)
        assertEquals(1, workout?.sets?.size)
    }

    @Test
    fun addCompletedSet_afterDateChanges_startsNewSessionForNewDaySummary() = runTest {
        repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val nextDate = WORKOUT_DATE.plusDays(1)
        currentInstant = nextDate
            .atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        repository.addCompletedSet(
            exerciseName = "Squat",
            reps = 3,
            weightKg = 120.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val firstDaySummary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()
        val nextDaySummary = repository.observeDailyTrainingSummary(nextDate).first()

        assertEquals(2, sessions.size)
        assertEquals(1, firstDaySummary.completedSetCount)
        assertEquals(500.0, firstDaySummary.totalVolumeKg, 0.01)
        assertEquals(1, nextDaySummary.completedSetCount)
        assertEquals(360.0, nextDaySummary.totalVolumeKg, 0.01)
    }

    @Test
    fun addCompletedSet_multipleSetsSameDay_preservesEarlierSetsAcrossFreshCompletedSessions() = runTest {
        repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )
        repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 6,
            weightKg = 102.5,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val allSets = sessions.flatMap { session -> database.trainingDao().getWorkoutSets(session.id) }
        val summary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()

        assertEquals(2, sessions.size)
        assertEquals(listOf(5, 6), allSets.mapNotNull { it.reps }.sorted())
        assertEquals(2, summary.completedSetCount)
        assertEquals(1115.0, summary.totalVolumeKg, 0.01)
    }

    @Test
    fun seedStarterTrainingData_importsExercisesAndRoutinesOnce() = runTest {
        repository.seedStarterTrainingData()
        repository.seedStarterTrainingData()

        val exercises = repository.observeExercises().first()
        val routines = repository.observeRoutineSummaries().first()

        assertTrue(exercises.any { it.name == "Barbell Bench Press" && !it.isCustom })
        assertTrue(exercises.any { it.name == "Back Squat" && it.equipment == "barbell" })
        assertTrue(routines.any { it.name == "Full Body A" && it.isStarter })
        assertTrue(routines.any { it.name == "Push" && it.exerciseCount >= 4 })
        assertEquals(exercises.map { it.id }.distinct().size, exercises.size)
        assertEquals(routines.map { it.id }.distinct().size, routines.size)
    }

    @Test
    fun createUpdateDuplicateAndDeleteRoutine_persistsRoutineExerciseTargets() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val squat = repository.observeExercises(query = "squat").first().single()

        val routineId = repository.createRoutine(
            RoutineInput(
                name = "Strength A",
                notes = "Heavy work",
                exercises = listOf(
                    RoutineExerciseInput(bench.id, targetSets = 3, targetReps = "5"),
                    RoutineExerciseInput(squat.id, targetSets = 4, targetReps = "6"),
                ),
            ),
        )

        repository.updateRoutine(
            routineId,
            RoutineInput(
                name = "Strength A Updated",
                notes = "Heavy plus backoff",
                exercises = listOf(
                    RoutineExerciseInput(squat.id, targetSets = 5, targetReps = "5"),
                ),
            ),
        )
        val duplicateId = repository.duplicateRoutine(routineId)
        repository.deleteRoutine(routineId)

        val summaries = repository.observeRoutineSummaries().first()
        val duplicate = repository.getRoutineDetail(duplicateId)

        assertTrue(summaries.none { it.id == routineId })
        assertEquals("Strength A Updated Copy", duplicate?.name)
        assertEquals(1, duplicate?.exercises?.size)
        assertEquals("Back Squat", duplicate?.exercises?.single()?.exercise?.name)
        assertEquals(5, duplicate?.exercises?.single()?.targetSets)
    }

    @Test
    fun startWorkoutFromRoutine_createsActiveSessionWithPlannedSets() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }

        val sessionId = repository.startWorkoutFromRoutine(routine.id)
        val active = repository.observeActiveWorkoutSummary().first()

        val sets = database.trainingDao().getWorkoutSets(sessionId)

        assertEquals(sessionId, active?.sessionId)
        assertEquals("Full Body A", active?.title)
        assertEquals(routine.targetSetCount, sets.size)
        assertTrue(sets.all { !it.completed })
        assertTrue(sets.all { it.setType == "working" })
    }

    @Test
    fun startBlankWorkout_createsActiveBlankWorkout() = runTest {
        val sessionId = repository.startBlankWorkout()

        val active = repository.observeActiveWorkoutSummary().first()
        val sets = database.trainingDao().getWorkoutSets(sessionId)

        assertEquals("Blank workout", active?.title)
        assertTrue(sets.isEmpty())
    }

    @Test
    fun activeWorkout_addEditDuplicateCompleteDeleteAndFinish_persistsExpectedState() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()

        repository.addExerciseToActiveWorkout(sessionId, bench.id)
        val setId = repository.addSetToExercise(
            sessionId,
            bench.id,
            WorkoutSetInputData(
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                rpe = 8.0,
                notes = "Solid",
                completed = true,
            ),
        )
        repository.duplicateLastSet(sessionId, bench.id)
        repository.updateWorkoutSet(
            setId,
            WorkoutSetInputData("warmup", reps = 8, weightKg = 60.0, rpe = 6.0, notes = "Warm", completed = true),
        )

        val detail = repository.observeActiveWorkoutDetail().first()
        val sets = detail?.exerciseBlocks?.single()?.sets.orEmpty()

        assertEquals(1, detail?.exerciseBlocks?.size)
        assertEquals(3, sets.size)
        assertEquals("warmup", sets.firstOrNull { it.id == setId }?.setType)
        assertEquals(1, detail?.completedSetCount)
        assertEquals(480.0, detail?.totalVolumeKg ?: 0.0, 0.01)

        val duplicatedSet = sets.lastOrNull()
        repository.deleteWorkoutSet(duplicatedSet?.id ?: error("Missing duplicated set"))
        repository.finishWorkout(sessionId)

        assertEquals(null, repository.observeActiveWorkoutDetail().first())
        assertEquals(null, repository.observeActiveWorkoutSummary().first())
    }

    @Test
    fun addExerciseToActiveWorkout_addsVisibleIncompleteWorkingSet() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()

        repository.addExerciseToActiveWorkout(sessionId, bench.id)

        val detail = repository.observeActiveWorkoutDetail().first()
        val block = detail?.exerciseBlocks?.singleOrNull()
        val set = block?.sets?.singleOrNull()

        assertEquals(1, detail?.exerciseBlocks?.size)
        assertEquals(bench.id, block?.exercise?.id)
        assertEquals("working", set?.setType)
        assertEquals(false, set?.completed)
        assertEquals(null, set?.reps)
        assertEquals(null, set?.weightKg)
    }

    @Test
    fun observeActiveWorkoutDetail_fromRoutineSurfacesTargetReps() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }

        repository.startWorkoutFromRoutine(routine.id)

        val detail = repository.observeActiveWorkoutDetail().first()

        assertTrue(detail?.exerciseBlocks?.isNotEmpty() == true)
        assertEquals("5", detail?.exerciseBlocks?.first()?.targetReps)
    }

    @Test
    fun observeActiveWorkoutDetail_usesLatestPriorCompletedSetForPreviousLabel() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()

        repository.addCompletedSet(
            exerciseName = bench.name,
            reps = 5,
            weightKg = 95.5,
        )

        currentInstant = WORKOUT_START.plusSeconds(7200)
        val sessionId = repository.startBlankWorkout()
        repository.addSetToExercise(
            sessionId = sessionId,
            exerciseId = bench.id,
            input = WorkoutSetInputData(
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                rpe = null,
                notes = null,
                completed = false,
            ),
        )

        val detail = repository.observeActiveWorkoutDetail().first()
        val set = detail?.exerciseBlocks?.singleOrNull()?.sets?.singleOrNull()

        assertEquals("95.5 kg x 5", set?.previousLabel)
    }

    @Test
    fun discardWorkout_removesActiveSessionFromActiveReads() = runTest {
        val sessionId = repository.startBlankWorkout()

        repository.discardWorkout(sessionId)

        assertEquals(null, repository.observeActiveWorkoutDetail().first())
        assertEquals(null, repository.observeActiveWorkoutSummary().first())
    }

    @Test
    fun activeWorkoutMutations_afterFinish_doNotMutateFinishedSession() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()
        val originalSetId = repository.addSetToExercise(
            sessionId = sessionId,
            exerciseId = bench.id,
            input = WorkoutSetInputData(
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                rpe = 8.0,
                notes = "Keep",
                completed = false,
            ),
        )

        repository.finishWorkout(sessionId)

        val addedAfterFinish = repository.addSetToExercise(
            sessionId = sessionId,
            exerciseId = bench.id,
            input = WorkoutSetInputData(
                setType = "warmup",
                reps = 10,
                weightKg = 40.0,
                rpe = 5.0,
                notes = "Should not save",
                completed = false,
            ),
        )
        val duplicatedAfterFinish = repository.duplicateLastSet(sessionId, bench.id)
        repository.updateWorkoutSet(
            originalSetId,
            WorkoutSetInputData(
                setType = "amrap",
                reps = 12,
                weightKg = 90.0,
                rpe = 9.0,
                notes = "Should not update",
                completed = true,
            ),
        )
        repository.deleteWorkoutSet(originalSetId)

        val session = database.trainingDao().getWorkoutSession(sessionId)
        val sets = database.trainingDao().getWorkoutSets(sessionId)
        val preservedSet = sets.singleOrNull { it.id == originalSetId }

        assertEquals("", addedAfterFinish)
        assertEquals(null, duplicatedAfterFinish)
        assertEquals("completed", session?.status)
        assertEquals(1, sets.size)
        assertEquals("working", preservedSet?.setType)
        assertEquals(5, preservedSet?.reps)
        assertEquals(100.0, preservedSet?.weightKg ?: 0.0, 0.01)
        assertEquals(8.0, preservedSet?.rpe ?: 0.0, 0.01)
        assertEquals("Keep", preservedSet?.notes)
        assertEquals(false, preservedSet?.completed)
    }

    @Test
    fun activeWorkoutMutations_afterDiscard_doNotMutateDiscardedSession() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()
        val originalSetId = repository.addSetToExercise(
            sessionId = sessionId,
            exerciseId = bench.id,
            input = WorkoutSetInputData(
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                rpe = 8.0,
                notes = "Keep",
                completed = false,
            ),
        )

        repository.discardWorkout(sessionId)

        val addedAfterDiscard = repository.addSetToExercise(
            sessionId = sessionId,
            exerciseId = bench.id,
            input = WorkoutSetInputData(
                setType = "warmup",
                reps = 10,
                weightKg = 40.0,
                rpe = 5.0,
                notes = "Should not save",
                completed = false,
            ),
        )
        val duplicatedAfterDiscard = repository.duplicateLastSet(sessionId, bench.id)
        repository.updateWorkoutSet(
            originalSetId,
            WorkoutSetInputData(
                setType = "amrap",
                reps = 12,
                weightKg = 90.0,
                rpe = 9.0,
                notes = "Should not update",
                completed = true,
            ),
        )
        repository.deleteWorkoutSet(originalSetId)

        val session = database.trainingDao().getWorkoutSession(sessionId)
        val sets = database.trainingDao().getWorkoutSets(sessionId)
        val preservedSet = sets.singleOrNull { it.id == originalSetId }

        assertEquals("", addedAfterDiscard)
        assertEquals(null, duplicatedAfterDiscard)
        assertEquals("discarded", session?.status)
        assertEquals(1, sets.size)
        assertEquals("working", preservedSet?.setType)
        assertEquals(5, preservedSet?.reps)
        assertEquals(100.0, preservedSet?.weightKg ?: 0.0, 0.01)
        assertEquals(8.0, preservedSet?.rpe ?: 0.0, 0.01)
        assertEquals("Keep", preservedSet?.notes)
        assertEquals(false, preservedSet?.completed)
    }

    @Test
    fun addCompletedSet_afterFinishingActiveWorkout_createsFreshQuickLogSession() = runTest {
        repository.seedStarterTrainingData()
        val activeSessionId = repository.startBlankWorkout()

        repository.finishWorkout(activeSessionId)
        repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first().sortedBy { it.startedAtEpochMillis }
        val quickLogSessions = sessions.filter { it.id != activeSessionId }
        val quickLogSets = quickLogSessions.flatMap { session -> database.trainingDao().getWorkoutSets(session.id) }

        assertEquals(2, sessions.size)
        assertEquals("completed", sessions.first { it.id == activeSessionId }.status)
        assertEquals(1, quickLogSessions.size)
        assertEquals("completed", quickLogSessions.single().status)
        assertEquals(listOf(activeSessionId), sessions.filter { it.id == activeSessionId }.map { it.id })
        assertEquals(1, quickLogSets.size)
        assertEquals(quickLogSessions.single().id, quickLogSets.single().sessionId)
    }

    @Test
    fun addCompletedSet_afterDiscardingActiveWorkout_doesNotReuseDiscardedSession() = runTest {
        repository.seedStarterTrainingData()
        val activeSessionId = repository.startBlankWorkout()

        repository.discardWorkout(activeSessionId)
        repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions().first().sortedBy { it.startedAtEpochMillis }
        val discardedSession = sessions.first { it.id == activeSessionId }
        val quickLogSessions = sessions.filter { it.id != activeSessionId }
        val discardedSets = database.trainingDao().getWorkoutSets(activeSessionId)
        val quickLogSets = quickLogSessions.flatMap { session -> database.trainingDao().getWorkoutSets(session.id) }

        assertEquals(2, sessions.size)
        assertEquals("discarded", discardedSession.status)
        assertTrue(discardedSets.isEmpty())
        assertEquals(1, quickLogSessions.size)
        assertEquals("completed", quickLogSessions.single().status)
        assertEquals(1, quickLogSets.size)
        assertEquals(quickLogSessions.single().id, quickLogSets.single().sessionId)
    }

    @Test
    fun startWorkoutFromRoutine_whenBlankActiveWorkoutExists_returnsExistingSessionUnchanged() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }

        val blankSessionId = repository.startBlankWorkout()
        val returnedSessionId = repository.startWorkoutFromRoutine(routine.id)

        val active = repository.observeActiveWorkoutSummary().first()
        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val sets = database.trainingDao().getWorkoutSets(blankSessionId)

        assertEquals(blankSessionId, returnedSessionId)
        assertEquals(1, sessions.count { it.status == "active" })
        assertEquals(blankSessionId, active?.sessionId)
        assertEquals("Blank workout", active?.title)
        assertTrue(sets.isEmpty())
    }

    @Test
    fun startWorkoutFromRoutine_whenDifferentRoutineIsAlreadyActive_returnsExistingSessionUnchanged() = runTest {
        repository.seedStarterTrainingData()
        val routines = repository.observeRoutineSummaries().first()
        val fullBody = routines.first { it.name == "Full Body A" }
        val push = routines.first { it.name == "Push" }

        val activeSessionId = repository.startWorkoutFromRoutine(fullBody.id)
        val originalSets = database.trainingDao().getWorkoutSets(activeSessionId)

        val returnedSessionId = repository.startWorkoutFromRoutine(push.id)

        val active = repository.observeActiveWorkoutSummary().first()
        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val setsAfterSecondLaunch = database.trainingDao().getWorkoutSets(activeSessionId)

        assertEquals(activeSessionId, returnedSessionId)
        assertEquals(1, sessions.count { it.status == "active" })
        assertEquals(activeSessionId, active?.sessionId)
        assertEquals("Full Body A", active?.title)
        assertEquals(originalSets.size, setsAfterSecondLaunch.size)
        assertEquals(originalSets.map { it.id }, setsAfterSecondLaunch.map { it.id })
    }

    @Test
    fun startWorkoutFromRoutine_whenSameRoutineIsStartedRepeatedly_doesNotDuplicatePlannedSets() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }

        val firstSessionId = repository.startWorkoutFromRoutine(routine.id)
        val originalSets = database.trainingDao().getWorkoutSets(firstSessionId)

        val secondSessionId = repository.startWorkoutFromRoutine(routine.id)

        val active = repository.observeActiveWorkoutSummary().first()
        val sessions = database.trainingDao().observeWorkoutSessions().first()
        val setsAfterRepeat = database.trainingDao().getWorkoutSets(firstSessionId)

        assertEquals(firstSessionId, secondSessionId)
        assertEquals(1, sessions.count { it.status == "active" })
        assertEquals(firstSessionId, active?.sessionId)
        assertEquals("Full Body A", active?.title)
        assertEquals(originalSets.size, setsAfterRepeat.size)
        assertEquals(originalSets.map { it.id }, setsAfterRepeat.map { it.id })
    }

    @Test
    fun observeExercises_filtersBySearchMuscleAndEquipment() = runTest {
        repository.seedStarterTrainingData()

        val bench = repository.observeExercises(query = "bench").first()
        val quads = repository.observeExercises(muscle = "quads").first()
        val dumbbell = repository.observeExercises(equipment = "dumbbell").first()

        assertTrue(bench.all { it.name.contains("Bench", ignoreCase = true) })
        assertTrue(quads.any { it.name == "Back Squat" })
        assertTrue(dumbbell.any { it.name == "Incline Dumbbell Press" })
    }

    @Test
    fun seedStarterTrainingData_whenCustomStarterExerciseExists_seedsRemainingCatalogWithoutDuplicates() = runTest {
        val customBenchPress =
            ExerciseEntity(
                id = "custom-bench-press",
                name = "Barbell Bench Press",
                category = "strength",
                equipment = null,
                targetMuscles = "chest",
                isCustom = true,
            )
        database.trainingDao().upsertExercise(customBenchPress)

        repository.seedStarterTrainingData()

        val exercises = repository.observeExercises().first()
        val routines = repository.observeRoutineSummaries().first()
        val pushRoutineExercises = database.trainingDao().getRoutineExercises("starter-routine-push")
        val matchingBenchPressExercises = exercises.filter { it.name == "Barbell Bench Press" }

        assertEquals(TrainingStarterData.exercises.map { it.name }.sorted(), exercises.map { it.name }.sorted())
        assertEquals(1, matchingBenchPressExercises.size)
        assertTrue(matchingBenchPressExercises.single().isCustom)
        assertEquals(customBenchPress.id, matchingBenchPressExercises.single().id)
        assertEquals(TrainingStarterData.routines.map { it.id }.sorted(), routines.map { it.id }.sorted())
        assertEquals(
            TrainingStarterData.routines.first { it.id == "starter-routine-push" }.exercises.size,
            pushRoutineExercises.size,
        )
        assertTrue(pushRoutineExercises.any { it.exerciseId == customBenchPress.id })
    }

    @Test
    fun seedStarterTrainingData_repeatedSeedPreservesRoutineLinkOnExistingWorkoutSession() = runTest {
        repository.seedStarterTrainingData()
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                id = "session-linked-routine",
                routineId = "starter-routine-full-body-a",
                title = "Full Body A",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(1800).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )

        repository.seedStarterTrainingData()

        val savedSession = database.trainingDao().getWorkoutSession("session-linked-routine")

        assertEquals("starter-routine-full-body-a", savedSession?.routineId)
    }

    @Test
    fun getLatestWorkoutForExport_skipsActiveSessionAndReturnsLatestCompletedWorkout() = runTest {
        val exercise =
            ExerciseEntity(
                id = "exercise-deadlift",
                name = "Deadlift",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "back,glutes,hamstrings",
                isCustom = false,
            )
        val completedSession =
            WorkoutSessionEntity(
                id = "session-completed",
                routineId = null,
                title = "Completed workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(1800).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        val activeSession =
            WorkoutSessionEntity(
                id = "session-active",
                routineId = null,
                title = "Active workout",
                status = "active",
                startedAtEpochMillis = WORKOUT_START.plusSeconds(3600).toEpochMilli(),
                endedAtEpochMillis = null,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )

        database.trainingDao().upsertExercise(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSession(activeSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-completed",
                sessionId = completedSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 2,
                weightKg = 160.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-active",
                sessionId = activeSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )

        val workout = repository.getLatestWorkoutForExport()

        assertNotNull(workout)
        assertEquals(completedSession.id, workout?.session?.id)
        assertEquals("completed", workout?.session?.status)
        assertEquals(listOf("set-completed"), workout?.sets?.map { it.id })
    }

    @Test
    fun getLatestWorkoutForExport_filtersOutIncompleteSetsFromCompletedSession() = runTest {
        val exercise =
            ExerciseEntity(
                id = "exercise-bench",
                name = "Bench Press",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "chest,triceps,shoulders",
                isCustom = false,
            )
        val completedSession =
            WorkoutSessionEntity(
                id = "session-completed",
                routineId = null,
                title = "Bench workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(1800).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )

        database.trainingDao().upsertExercise(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-completed",
                sessionId = completedSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-incomplete",
                sessionId = completedSession.id,
                exerciseId = exercise.id,
                sortOrder = 1,
                setType = "working",
                reps = 8,
                weightKg = 90.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = false,
            ),
        )

        val workout = repository.getLatestWorkoutForExport()

        assertNotNull(workout)
        assertEquals(listOf("set-completed"), workout?.sets?.map { it.id })
    }

    @Test
    fun observeDailyTrainingSummary_ignoresActiveAndDiscardedSessions() = runTest {
        val exercise =
            ExerciseEntity(
                id = "exercise-bench",
                name = "Bench Press",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "chest,triceps,shoulders",
                isCustom = false,
            )
        val completedSession =
            WorkoutSessionEntity(
                id = "session-completed",
                routineId = null,
                title = "Completed workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(1200).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        val activeSession =
            WorkoutSessionEntity(
                id = "session-active",
                routineId = null,
                title = "Active workout",
                status = "active",
                startedAtEpochMillis = WORKOUT_START.plusSeconds(1800).toEpochMilli(),
                endedAtEpochMillis = null,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        val discardedSession =
            WorkoutSessionEntity(
                id = "session-discarded",
                routineId = null,
                title = "Discarded workout",
                status = "discarded",
                startedAtEpochMillis = WORKOUT_START.plusSeconds(2400).toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(3000).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )

        database.trainingDao().upsertExercise(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSession(activeSession)
        database.trainingDao().upsertWorkoutSession(discardedSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-completed",
                sessionId = completedSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 5,
                weightKg = 100.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-active",
                sessionId = activeSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 3,
                weightKg = 120.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                id = "set-discarded",
                sessionId = discardedSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
                setType = "working",
                reps = 10,
                weightKg = 60.0,
                durationSeconds = null,
                distanceMeters = null,
                rpe = null,
                notes = null,
                completed = true,
            ),
        )

        val summary = repository.observeDailyTrainingSummary(WORKOUT_DATE).first()

        assertEquals(1, summary.completedSetCount)
        assertEquals(500.0, summary.totalVolumeKg, 0.01)
        assertEquals(116.67, summary.bestEstimatedOneRepMaxKg, 0.01)
    }

    private companion object {
        val WORKOUT_DATE: LocalDate = LocalDate.of(2026, 6, 20)
        val WORKOUT_START: Instant = WORKOUT_DATE
            .atTime(10, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    }
}
