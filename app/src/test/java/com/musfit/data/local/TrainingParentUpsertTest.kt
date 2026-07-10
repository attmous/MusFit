package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.dao.TrainingDao
import com.musfit.data.local.entity.ExerciseEntity
import com.musfit.data.local.entity.RoutineEntity
import com.musfit.data.local.entity.RoutineExerciseEntity
import com.musfit.data.local.entity.RoutineExerciseSetEntity
import com.musfit.data.local.entity.RoutineFolderEntity
import com.musfit.data.local.entity.TrainingSettingsEntity
import com.musfit.data.local.entity.WorkoutSessionEntity
import com.musfit.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrainingParentUpsertTest {
    private lateinit var database: MusFitDatabase
    private lateinit var dao: TrainingDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.trainingDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun exerciseUpserts_preserveRoutineAndWorkoutReferences() = runTest {
        val graph = insertTrainingGraph()

        dao.upsertExercise(graph.exercise.copy(name = "Bench Press Updated"))
        dao.upsertExercises(
            listOf(
                graph.exercise.copy(
                    name = "Bench Press Updated Again",
                    localNotes = "Keep this graph attached.",
                ),
            ),
        )

        assertEquals("Bench Press Updated Again", dao.getExercise(graph.exercise.id)?.name)
        assertEquals("Keep this graph attached.", dao.getExercise(graph.exercise.id)?.localNotes)
        assertEquals(listOf(graph.routineExercise), dao.getRoutineExercises(graph.routine.id))
        assertEquals(listOf(graph.workoutSet), dao.getWorkoutSets(graph.session.id))
    }

    @Test
    fun routineAndFolderUpserts_preserveExercisePlansAndSessionLink() = runTest {
        val graph = insertTrainingGraph()

        dao.upsertRoutineFolder(graph.folder.copy(name = "Strength Block Updated"))
        dao.upsertRoutine(graph.routine.copy(name = "Push Updated"))
        dao.upsertRoutines(listOf(graph.routine.copy(name = "Push Updated Again")))

        assertEquals("Strength Block Updated", dao.getRoutineFolder(graph.folder.id)?.name)
        assertEquals("Push Updated Again", dao.getRoutine(graph.routine.id)?.name)
        assertEquals(graph.folder.id, dao.getRoutine(graph.routine.id)?.folderId)
        assertEquals(listOf(graph.routineExercise), dao.getRoutineExercises(graph.routine.id))
        assertEquals(
            listOf(graph.routineExerciseSet.id),
            dao.getRoutineExerciseSetDetailRows(graph.routine.id).map { it.id },
        )
        assertEquals(graph.routine.id, dao.getWorkoutSession(graph.session.id)?.routineId)
    }

    @Test
    fun routineExerciseUpserts_preservePlannedSets() = runTest {
        val graph = insertTrainingGraph()

        dao.upsertRoutineExercise(graph.routineExercise.copy(targetSets = 4, targetReps = "6"))
        dao.upsertRoutineExercises(
            listOf(graph.routineExercise.copy(targetSets = 5, targetReps = "5")),
        )

        val savedExercise = dao.getRoutineExercises(graph.routine.id).single()
        val savedPlans = dao.getRoutineExerciseSetDetailRows(graph.routine.id)
        assertEquals(5, savedExercise.targetSets)
        assertEquals("5", savedExercise.targetReps)
        assertEquals(listOf(graph.routineExerciseSet.id), savedPlans.map { it.id })
        assertEquals("working", savedPlans.single().setType)
        assertEquals(100.0, savedPlans.single().targetWeightKg ?: 0.0, 0.01)
    }

    @Test
    fun workoutSessionUpsert_preservesSetsAndExportMetadata() = runTest {
        val graph = insertTrainingGraph()

        dao.upsertWorkoutSession(
            graph.session.copy(
                title = "Push Updated",
                notes = "Metadata only",
            ),
        )

        val savedSession = dao.getWorkoutSession(graph.session.id)
        assertEquals("Push Updated", savedSession?.title)
        assertEquals("Metadata only", savedSession?.notes)
        assertEquals(graph.session.healthConnectRecordId, savedSession?.healthConnectRecordId)
        assertEquals(
            graph.session.healthConnectLastExportedAtEpochMillis,
            savedSession?.healthConnectLastExportedAtEpochMillis,
        )
        assertEquals(listOf(graph.workoutSet), dao.getWorkoutSets(graph.session.id))
    }

    @Test
    fun trainingSettingsUpsert_updatesScalarState() = runTest {
        dao.upsertTrainingSettings(TrainingSettingsEntity())

        dao.upsertTrainingSettings(
            TrainingSettingsEntity(
                defaultRestSeconds = 90,
                barWeightKg = 15.0,
                availablePlatesKg = "20,10,5,2.5",
            ),
        )

        assertEquals(
            TrainingSettingsEntity(
                defaultRestSeconds = 90,
                barWeightKg = 15.0,
                availablePlatesKg = "20,10,5,2.5",
            ),
            dao.getTrainingSettings(),
        )
    }

    @Test
    fun failedTrainingMetadataTransaction_rollsBackWholeGraph() = runTest {
        val graph = insertTrainingGraph()
        var rolledBack = false

        try {
            database.withTransaction {
                dao.upsertRoutineFolder(graph.folder.copy(name = "Temporary folder"))
                dao.upsertRoutine(graph.routine.copy(name = "Temporary routine"))
                dao.upsertWorkoutSession(graph.session.copy(title = "Temporary session"))
                throw ForcedRollback
            }
        } catch (_: ForcedRollbackException) {
            rolledBack = true
        }

        assertTrue(rolledBack)
        assertEquals(graph.folder, dao.getRoutineFolder(graph.folder.id))
        assertEquals(graph.routine, dao.getRoutine(graph.routine.id))
        assertEquals(graph.session, dao.getWorkoutSession(graph.session.id))
        assertEquals(listOf(graph.routineExercise), dao.getRoutineExercises(graph.routine.id))
        assertEquals(
            listOf(graph.routineExerciseSet.id),
            dao.getRoutineExerciseSetDetailRows(graph.routine.id).map { it.id },
        )
        assertEquals(listOf(graph.workoutSet), dao.getWorkoutSets(graph.session.id))
    }

    private suspend fun insertTrainingGraph(): TrainingGraph {
        val exercise = ExerciseEntity(
            id = "exercise-bench",
            name = "Bench Press",
            category = "strength",
            equipment = "barbell",
            targetMuscles = "chest,triceps",
            isCustom = true,
        )
        val folder = RoutineFolderEntity(
            id = "folder-strength",
            name = "Strength Block",
            sortOrder = 0,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L,
        )
        val routine = RoutineEntity(
            id = "routine-push",
            name = "Push",
            notes = "Heavy",
            createdAtEpochMillis = 2_000L,
            updatedAtEpochMillis = 2_000L,
            folderId = folder.id,
        )
        val routineExercise = RoutineExerciseEntity(
            id = "routine-exercise-bench",
            routineId = routine.id,
            exerciseId = exercise.id,
            sortOrder = 0,
            targetSets = 3,
            targetReps = "8",
            restSeconds = 180,
        )
        val routineExerciseSet = RoutineExerciseSetEntity(
            id = "routine-exercise-bench-set-0",
            routineExerciseId = routineExercise.id,
            sortOrder = 0,
            setType = "working",
            targetReps = "8",
            targetWeightKg = 100.0,
        )
        val session = WorkoutSessionEntity(
            id = "session-push",
            routineId = routine.id,
            title = "Push",
            status = "completed",
            startedAtEpochMillis = 3_000L,
            endedAtEpochMillis = 4_000L,
            notes = "Original session",
            healthConnectRecordId = "health-session-push",
            healthConnectLastExportedAtEpochMillis = 5_000L,
        )
        val workoutSet = WorkoutSetEntity(
            id = "workout-set-bench",
            sessionId = session.id,
            exerciseId = exercise.id,
            sortOrder = 0,
            setType = "working",
            reps = 8,
            weightKg = 100.0,
            durationSeconds = null,
            distanceMeters = null,
            rpe = 8.0,
            notes = "Original set",
            completed = true,
        )

        dao.upsertExercise(exercise)
        dao.upsertRoutineFolder(folder)
        dao.upsertRoutine(routine)
        dao.upsertRoutineExercise(routineExercise)
        dao.upsertRoutineExerciseSets(listOf(routineExerciseSet))
        dao.upsertWorkoutSession(session)
        dao.upsertWorkoutSet(workoutSet)
        return TrainingGraph(
            exercise = exercise,
            folder = folder,
            routine = routine,
            routineExercise = routineExercise,
            routineExerciseSet = routineExerciseSet,
            session = session,
            workoutSet = workoutSet,
        )
    }

    private data class TrainingGraph(
        val exercise: ExerciseEntity,
        val folder: RoutineFolderEntity,
        val routine: RoutineEntity,
        val routineExercise: RoutineExerciseEntity,
        val routineExerciseSet: RoutineExerciseSetEntity,
        val session: WorkoutSessionEntity,
        val workoutSet: WorkoutSetEntity,
    )

    private object ForcedRollback : ForcedRollbackException()

    private open class ForcedRollbackException : RuntimeException()
}
