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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val TEST_ACCOUNT_ID = "local-default"
private const val TEST_EXERCISE_GIF_MIRROR_BASE =
    "https://gitlab.stud.idi.ntnu.no/gruppe-1/prog2052-prosjekt/-/raw/main/backend/assets/exercises/"

@RunWith(RobolectricTestRunner::class)
class LocalTrainingRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalTrainingRepository
    private lateinit var accountRepository: LocalAccountRepository
    private var currentInstant: Instant = WORKOUT_START

    @Before
    fun setUp() {
        currentInstant = WORKOUT_START
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        accountRepository = LocalAccountRepository(database.accountDao(), clock = { 1_000L })
        runBlocking { accountRepository.ensureActiveAccount() }
        repository = LocalTrainingRepository(
            database = database,
            trainingDao = database.trainingDao(),
            clock = { currentInstant.toEpochMilli() },
            accountRepository = accountRepository,
        )
    }

    @Test
    fun activeAccount_isolatesEveryTrainingOwnedSurface() = runTest {
        val sharedExerciseId = "shared-bench"
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = sharedExerciseId,
                name = "Shared bench",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "chest",
                isCustom = false,
            ),
        )
        repository.updateExerciseLocalNotes(sharedExerciseId, "Account A shared note")
        val accountAExerciseId = repository.createCustomExercise(
            ExerciseInput(
                name = "Account A press",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "chest",
            ),
        )
        repository.updateExerciseLocalNotes(accountAExerciseId, "Account A setup")
        repository.updateTrainingSettings(
            TrainingSettingsInput(
                defaultRestSeconds = 75,
                barWeightKg = 15.0,
                availablePlatesKg = listOf(20.0, 10.0, 5.0),
            ),
        )
        repository.createRoutine(
            RoutineInput(
                name = "Account A routine",
                notes = "Private plan",
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = accountAExerciseId,
                        targetSets = 3,
                        targetReps = "8",
                    ),
                ),
            ),
        )
        val accountAWorkoutId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(accountAWorkoutId, accountAExerciseId)

        val secondAccountId = accountRepository.createAccount("Account B")
        accountRepository.switchAccount(secondAccountId)

        assertTrue(repository.observeExercises().first().none { it.id == accountAExerciseId })
        assertEquals(null, repository.getExerciseDetail(accountAExerciseId))
        assertEquals(null, repository.getExerciseDetail(sharedExerciseId)?.localNotes)
        assertTrue(repository.observeRoutineSummaries().first().isEmpty())
        assertEquals(null, repository.observeActiveWorkoutSummary().first())
        assertEquals(null, repository.observeActiveWorkoutDetail().first())
        assertTrue(repository.observeWorkoutHistory().first().isEmpty())
        assertEquals(TrainingSettings(), repository.observeTrainingSettings().first())
        repository.finishWorkout(accountAWorkoutId)

        val accountBExerciseId = repository.createCustomExercise(
            ExerciseInput(
                name = "Account B row",
                category = "strength",
                equipment = "cable",
                targetMuscles = "back",
            ),
        )
        assertEquals("Account B row", repository.observeExercises().first().single { it.id == accountBExerciseId }.name)

        accountRepository.switchAccount("local-default")

        assertEquals("Account A setup", repository.getExerciseDetail(accountAExerciseId)?.localNotes)
        assertEquals("Account A shared note", repository.getExerciseDetail(sharedExerciseId)?.localNotes)
        assertEquals("Account A routine", repository.observeRoutineSummaries().first().single().name)
        assertEquals(accountAWorkoutId, repository.observeActiveWorkoutSummary().first()?.sessionId)
        assertTrue(repository.observeExercises().first().none { it.id == accountBExerciseId })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun trainingSettings_defaultAndUpdatePersistRestAndPlateTools() = runTest {
        val defaults = repository.observeTrainingSettings().first()

        assertEquals(120, defaults.defaultRestSeconds)
        assertEquals(20.0, defaults.barWeightKg, 0.01)
        assertEquals(listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25), defaults.availablePlatesKg)

        repository.updateTrainingSettings(
            TrainingSettingsInput(
                defaultRestSeconds = 90,
                barWeightKg = 15.0,
                availablePlatesKg = listOf(20.0, 10.0, 5.0, 2.5, 2.5),
            ),
        )

        val updated = repository.observeTrainingSettings().first()
        assertEquals(90, updated.defaultRestSeconds)
        assertEquals(15.0, updated.barWeightKg, 0.01)
        assertEquals(listOf(20.0, 10.0, 5.0, 2.5), updated.availablePlatesKg)
    }

    @Test
    fun addCompletedSet_persistsExerciseSessionSetAndDailySummary() = runTest {
        val savedSet = repository.addCompletedSet(
            exerciseName = "Bench Press",
            reps = 5,
            weightKg = 100.0,
        )

        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessions.single().id)
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
        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessions.single().id)

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

        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
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

        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
        val allSets = sessions.flatMap { session -> database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, session.id) }
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
    fun seedStarterTrainingData_preservesEditedStarterRoutine() = runTest {
        repository.seedStarterTrainingData()
        val fullBody = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }
        val latPulldown = repository.observeExercises(query = "lat pulldown").first().single()

        repository.updateRoutine(
            routineId = fullBody.id,
            input = RoutineInput(
                name = "Full Body A edited",
                notes = "My edited template",
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = latPulldown.id,
                        targetSets = 4,
                        targetReps = "10",
                    ),
                ),
            ),
        )

        repository.seedStarterTrainingData()

        val detail = repository.getRoutineDetail(fullBody.id)
        assertEquals("Full Body A edited", detail?.name)
        assertEquals("My edited template", detail?.notes)
        assertEquals(true, detail?.isStarter)
        assertEquals("Full Body", detail?.programName)
        assertEquals(listOf(latPulldown.id), detail?.exercises?.map { it.exercise.id })
        assertEquals(listOf(4), detail?.exercises?.map { it.targetSets })
        assertEquals(listOf("10"), detail?.exercises?.map { it.targetReps })
    }

    @Test
    fun createCustomExercise_trimsAndPersistsCustomExercise() = runTest {
        val exerciseId = repository.createCustomExercise(
            ExerciseInput(
                name = "  Belt Squat  ",
                category = " strength ",
                equipment = " machine ",
                targetMuscles = " quads, glutes ",
            ),
        )

        val exercises = repository.observeExercises(query = "belt").first()
        val exercise = exercises.single()

        assertEquals(exerciseId, exercise.id)
        assertEquals("Belt Squat", exercise.name)
        assertEquals("strength", exercise.category)
        assertEquals("machine", exercise.equipment)
        assertEquals("quads, glutes", exercise.targetMuscles)
        assertTrue(exercise.isCustom)
    }

    @Test
    fun createCustomExercise_reusesExistingExerciseNameWithoutDuplicatingStarterExercise() = runTest {
        repository.seedStarterTrainingData()
        val before = repository.observeExercises().first()
        val existingBench = before.first { it.name == "Barbell Bench Press" }

        val exerciseId = repository.createCustomExercise(
            ExerciseInput(
                name = "  barbell bench press  ",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "chest",
            ),
        )

        val after = repository.observeExercises().first()
        val savedBench = after.first { it.id == existingBench.id }

        assertEquals(existingBench.id, exerciseId)
        assertEquals(before.size, after.size)
        assertFalse(savedBench.isCustom)
    }

    @Test
    fun getExerciseDetail_exposesStarterMetadataAndPersistsLocalNotes() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()

        val detail = repository.getExerciseDetail(bench.id)

        assertEquals("Barbell Bench Press", detail?.name)
        assertEquals("chest", detail?.primaryMuscles)
        assertEquals("triceps, shoulders", detail?.secondaryMuscles)
        assertTrue(detail?.instructions?.isNotBlank() == true)
        assertEquals(null, detail?.localNotes)

        repository.updateExerciseLocalNotes(bench.id, "  Competition grip. Pause the first rep.  ")

        val updated = repository.getExerciseDetail(bench.id)
        assertEquals("Competition grip. Pause the first rep.", updated?.localNotes)
    }

    @Test
    fun createCustomExercise_detailUsesTargetMusclesAsPrimaryMuscles() = runTest {
        val exerciseId = repository.createCustomExercise(
            ExerciseInput(
                name = "  Meadows Row  ",
                category = "strength",
                equipment = "landmine",
                targetMuscles = "lats, upper back",
            ),
        )

        val detail = repository.getExerciseDetail(exerciseId)

        assertEquals("Meadows Row", detail?.name)
        assertEquals("lats, upper back", detail?.primaryMuscles)
        assertEquals("", detail?.secondaryMuscles)
        assertEquals(null, detail?.instructions)
        assertEquals(null, detail?.localNotes)
    }

    @Test
    fun getExerciseDetail_rewritesLegacyDatasetMediaUrlsToGifMirrorUrls() = runTest {
        database.trainingDao().upsertExerciseDefinition(
            ExerciseEntity(
                id = "legacy-media",
                name = "Legacy Media Curl",
                category = "strength",
                equipment = "dumbbell",
                targetMuscles = "biceps",
                isCustom = false,
                imageUrl = EXERCISE_DATASET_LEGACY_CDN_BASE + "images/0294-x.jpg",
                gifUrl = EXERCISE_DATASET_LEGACY_CDN_BASE + "videos/0294-x.gif",
            ),
        )

        val detail = repository.getExerciseDetail("legacy-media")

        assertNotNull(detail)
        assertEquals(TEST_EXERCISE_GIF_MIRROR_BASE + "x.gif", detail?.imageUrl)
        assertEquals(TEST_EXERCISE_GIF_MIRROR_BASE + "x.gif", detail?.gifUrl)
    }

    @Test
    fun seedStarterTrainingData_importsDatasetWithGifMirrorMedia() = runTest {
        val dataset = ExerciseDatasetProvider {
            listOf(
                ExerciseDatasetRecord(
                    id = "0025",
                    name = "barbell bench press",
                    category = "chest",
                    equipment = "barbell",
                    target = "pectorals",
                    secondary = "triceps, front delts",
                    instructions = "Lower the bar then press up.",
                    image = "images/0025-x.jpg",
                    gif = "videos/0025-x.gif",
                ),
                ExerciseDatasetRecord(
                    id = "9999",
                    name = "some other move",
                    category = "cardio",
                    equipment = "body weight",
                    target = "abs",
                    secondary = "",
                    instructions = "Do the thing.",
                    image = "images/9999-y.jpg",
                    gif = "videos/9999-y.gif",
                ),
            )
        }
        val repositoryWithDataset = LocalTrainingRepository(
            database = database,
            trainingDao = database.trainingDao(),
            clock = { currentInstant.toEpochMilli() },
            exerciseDataset = dataset,
        )

        repositoryWithDataset.seedStarterTrainingData()

        val imported = database.trainingDao().getExercise(TEST_ACCOUNT_ID, "ds-0025")
        assertNotNull(imported)
        assertEquals(TEST_EXERCISE_GIF_MIRROR_BASE + "x.gif", imported?.imageUrl)
        assertEquals(TEST_EXERCISE_GIF_MIRROR_BASE + "x.gif", imported?.gifUrl)

        // The built-in "Barbell Bench Press" starter keeps the useful text while attaching the
        // matching ExerciseDB demo media from the imported catalog row.
        val starter = database.trainingDao().getExerciseByName(TEST_ACCOUNT_ID, "Barbell Bench Press")
        assertNotNull(starter)
        assertEquals(TEST_EXERCISE_GIF_MIRROR_BASE + "x.gif", starter?.imageUrl)
        assertEquals(TEST_EXERCISE_GIF_MIRROR_BASE + "x.gif", starter?.gifUrl)
    }

    @Test
    fun observeRoutineSummaries_aggregatesMuscleGroupsWithoutInflatingCounts() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().first()
        val squat = repository.observeExercises(query = "squat").first().first()

        repository.createRoutine(
            RoutineInput(
                name = "Mixed Day",
                notes = null,
                exercises = listOf(
                    RoutineExerciseInput(bench.id, targetSets = 3, targetReps = "5"),
                    RoutineExerciseInput(squat.id, targetSets = 4, targetReps = "6"),
                ),
            ),
        )

        val summary = repository.observeRoutineSummaries().first().single { it.name == "Mixed Day" }

        // Extra LEFT JOIN to exercises must not multiply the aggregate counts.
        assertEquals(2, summary.exerciseCount)
        assertEquals(7, summary.targetSetCount)
        // Primary muscles aggregated across the routine's exercises, deduped + title-cased, capped at 3.
        assertTrue(summary.muscleGroups.contains("Chest"))
        assertTrue(summary.muscleGroups.contains("Quads"))
        assertTrue(summary.muscleGroups.size <= 3)
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
    fun createUpdateDuplicateAndDeleteRoutine_persistsFolderRestAndSetPlans() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val folderId = repository.createRoutineFolder("PPL System x4 - Phase 1")

        val routineId = repository.createRoutine(
            RoutineInput(
                name = "Push #1",
                notes = "Heavy press day",
                folderId = folderId,
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = bench.id,
                        targetSets = 3,
                        targetReps = "8",
                        restSeconds = 210,
                        setPlans = listOf(
                            RoutineSetInput(setType = "warmup", targetReps = "15", targetWeightKg = 20.0),
                            RoutineSetInput(setType = "working", targetReps = "8", targetWeightKg = 70.0),
                            RoutineSetInput(setType = "drop", targetReps = "12", targetWeightKg = 45.0),
                        ),
                    ),
                ),
            ),
        )

        repository.updateRoutineFolder(folderId, "PPL System x4 - Phase 1A")
        val duplicateId = repository.duplicateRoutine(routineId)
        val duplicate = repository.getRoutineDetail(duplicateId)
        repository.deleteRoutineFolder(folderId)

        val original = repository.getRoutineDetail(routineId)
        val folders = repository.observeRoutineFolders().first()

        assertTrue(folders.none { it.id == folderId })
        assertEquals(null, original?.folderId)
        assertEquals(null, original?.folderName)
        assertEquals("Push #1 Copy", duplicate?.name)
        assertEquals("PPL System x4 - Phase 1A", duplicate?.folderName)
        assertEquals(210, duplicate?.exercises?.single()?.restSeconds)
        assertEquals(listOf("warmup", "working", "drop"), duplicate?.exercises?.single()?.setPlans?.map { it.setType })
        assertEquals(listOf("15", "8", "12"), duplicate?.exercises?.single()?.setPlans?.map { it.targetReps })
        assertEquals(listOf(20.0, 70.0, 45.0), duplicate?.exercises?.single()?.setPlans?.map { it.targetWeightKg })
    }

    @Test
    fun assignRoutineToFolder_movesAndUnassignsRoutineWithoutEditingRoutineContents() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val folderId = repository.createRoutineFolder("Powerbuilding")
        val routineId = repository.createRoutine(
            RoutineInput(
                name = "Bench Blocks",
                notes = "Keep the close-grip work",
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = bench.id,
                        targetSets = 3,
                        targetReps = "8",
                        restSeconds = 210,
                        setPlans = listOf(
                            RoutineSetInput(setType = "warmup", targetReps = "12", targetWeightKg = 40.0),
                            RoutineSetInput(setType = "working", targetReps = "8", targetWeightKg = 90.0),
                        ),
                    ),
                ),
            ),
        )

        repository.assignRoutineToFolder(routineId, folderId)

        val assigned = repository.getRoutineDetail(routineId)
        val assignedSummary = repository.observeRoutineSummaries().first().single { it.id == routineId }
        assertEquals(folderId, assigned?.folderId)
        assertEquals("Powerbuilding", assigned?.folderName)
        assertEquals(folderId, assignedSummary.folderId)
        assertEquals("Powerbuilding", assignedSummary.folderName)
        assertEquals("Bench Blocks", assigned?.name)
        assertEquals(listOf("warmup", "working"), assigned?.exercises?.single()?.setPlans?.map { it.setType })

        repository.assignRoutineToFolder(routineId, null)

        val unassigned = repository.getRoutineDetail(routineId)
        val unassignedSummary = repository.observeRoutineSummaries().first().single { it.id == routineId }
        assertEquals(null, unassigned?.folderId)
        assertEquals(null, unassigned?.folderName)
        assertEquals(null, unassignedSummary.folderId)
        assertEquals(null, unassignedSummary.folderName)
        assertEquals(listOf("12", "8"), unassigned?.exercises?.single()?.setPlans?.map { it.targetReps })
    }

    @Test
    fun startWorkoutFromRoutine_materializesSavedSetTypesWeightsAndExerciseRestSeconds() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val routineId = repository.createRoutine(
            RoutineInput(
                name = "Bench Ladder",
                notes = null,
                exercises = listOf(
                    RoutineExerciseInput(
                        exerciseId = bench.id,
                        targetSets = 3,
                        targetReps = "8",
                        restSeconds = 180,
                        setPlans = listOf(
                            RoutineSetInput(setType = "warmup", targetReps = "12", targetWeightKg = 40.0),
                            RoutineSetInput(setType = "working", targetReps = "8", targetWeightKg = 90.0),
                            RoutineSetInput(setType = "failure", targetReps = "AMRAP", targetWeightKg = 90.0),
                        ),
                    ),
                ),
            ),
        )

        val sessionId = repository.startWorkoutFromRoutine(routineId)
        val active = repository.observeActiveWorkoutDetail().first()
        val persistedSets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)

        assertEquals(listOf("warmup", "working", "failure"), active?.exerciseBlocks?.single()?.sets?.map { it.setType })
        assertEquals(listOf("12", "8", "AMRAP"), active?.exerciseBlocks?.single()?.sets?.map { it.targetReps })
        assertEquals(listOf(40.0, 90.0, 90.0), active?.exerciseBlocks?.single()?.sets?.map { it.weightKg })
        assertEquals(listOf(180, 180, 180), active?.exerciseBlocks?.single()?.sets?.map { it.restSeconds })
        assertEquals(listOf(180, 180, 180), persistedSets.map { it.restSeconds })
    }

    @Test
    fun seedStarterTrainingData_exposesProgramCatalogAndDuplicateKeepsOrganization() = runTest {
        repository.seedStarterTrainingData()

        val summaries = repository.observeRoutineSummaries().first()

        assertTrue(summaries.any { it.programName == "Full Body" && it.name == "Full Body A" })
        assertTrue(summaries.any { it.programName == "Push Pull Legs" && it.name == "Push" })
        assertTrue(summaries.any { it.programName == "Upper Lower" })
        assertTrue(summaries.any { it.programName == "Strength" })
        assertTrue(summaries.any { it.programName == "Hypertrophy" })
        assertTrue(summaries.any { it.programName == "Beginner" })

        val fullBody = summaries.first { it.name == "Full Body A" }
        assertTrue("beginner" in fullBody.tags)
        assertTrue("strength" in fullBody.tags)

        val duplicateId = repository.duplicateRoutine(fullBody.id)
        val duplicate = repository.getRoutineDetail(duplicateId)

        assertEquals("Full Body A Copy", duplicate?.name)
        assertEquals(false, duplicate?.isStarter)
        assertEquals("Full Body", duplicate?.programName)
        assertTrue("beginner" in duplicate?.tags.orEmpty())
    }

    @Test
    fun updateRoutine_preservesLinkedActiveAndCompletedWorkoutSessionRoutineIds() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val routineId = repository.createRoutine(
            RoutineInput(
                name = "Upper A",
                notes = "Press focus",
                exercises = listOf(
                    RoutineExerciseInput(bench.id, targetSets = 3, targetReps = "5"),
                ),
            ),
        )
        val activeSessionId = repository.startWorkoutFromRoutine(routineId)
        database.trainingDao().upsertWorkoutSession(
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-completed-linked",
                routineId = routineId,
                title = "Upper A",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.minusSeconds(3600).toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.minusSeconds(1800).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            ),
        )

        repository.updateRoutine(
            routineId,
            RoutineInput(
                name = "Upper A Updated",
                notes = "Updated press focus",
                exercises = listOf(
                    RoutineExerciseInput(bench.id, targetSets = 4, targetReps = "6"),
                ),
            ),
        )

        val activeSession = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, activeSessionId)
        val completedSession = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-completed-linked")

        assertEquals(routineId, activeSession?.routineId)
        assertEquals(routineId, completedSession?.routineId)
    }

    @Test
    fun startWorkoutFromRoutine_createsActiveSessionWithPlannedSets() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }

        val sessionId = repository.startWorkoutFromRoutine(routine.id)
        val active = repository.observeActiveWorkoutSummary().first()

        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)

        assertEquals(sessionId, active?.sessionId)
        assertEquals("Full Body A", active?.title)
        assertEquals(routine.targetSetCount, sets.size)
        assertTrue(sets.all { !it.completed })
        assertTrue(sets.all { it.setType == "working" })
    }

    @Test
    fun moveExerciseInActiveWorkout_reordersExerciseBlocksAndKeepsSortOrdersContiguous() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }
        val sessionId = repository.startWorkoutFromRoutine(routine.id)

        suspend fun exerciseOrder(): List<String> = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)
            .sortedBy { it.sortOrder }
            .map { it.exerciseId }
            .distinct()

        val before = exerciseOrder()
        assertTrue(before.size >= 2)
        val second = before[1]

        repository.moveExerciseInActiveWorkout(sessionId, second, direction = -1)

        val after = exerciseOrder()
        assertEquals(second, after[0])
        assertEquals(before[0], after[1])
        // All of the moved block's later exercises stay in place.
        assertEquals(before.drop(2), after.drop(2))
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId).sortedBy { it.sortOrder }
        assertEquals(sets.indices.toList(), sets.map { it.sortOrder })
    }

    @Test
    fun replaceExerciseInActiveWorkout_transfersSetsToNewExercise() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }
        val sessionId = repository.startWorkoutFromRoutine(routine.id)

        val workoutExerciseIds = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId).map { it.exerciseId }.toSet()
        val original = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId).sortedBy { it.sortOrder }.first().exerciseId
        val originalSetCount = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId).count { it.exerciseId == original }
        val replacement = repository.observeExercises(query = "").first().first { it.id !in workoutExerciseIds }

        repository.replaceExerciseInActiveWorkout(sessionId, fromExerciseId = original, toExerciseId = replacement.id)

        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)
        assertEquals(0, sets.count { it.exerciseId == original })
        assertEquals(originalSetCount, sets.count { it.exerciseId == replacement.id })
    }

    @Test
    fun startBlankWorkout_createsActiveBlankWorkout() = runTest {
        val sessionId = repository.startBlankWorkout()

        val active = repository.observeActiveWorkoutSummary().first()
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)

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
    fun activeWorkoutNotesAndSetReorder_persistInActiveDetail() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(sessionId, bench.id)
        val initialSetId = repository.observeActiveWorkoutDetail()
            .first()
            ?.exerciseBlocks
            ?.single()
            ?.sets
            ?.single()
            ?.id ?: error("Missing initial set")
        val firstSetId = repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 5, 100.0, null, null, false))
        val secondSetId = repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("drop", 8, 80.0, null, null, false))
        val thirdSetId = repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("failure", 10, 60.0, null, null, false))

        repository.updateActiveWorkoutNotes(sessionId, "  Keep rest strict  ")
        repository.moveWorkoutSet(thirdSetId, direction = -1)

        val detail = repository.observeActiveWorkoutDetail().first()
        val sets = detail?.exerciseBlocks?.single()?.sets.orEmpty()

        assertEquals("Keep rest strict", detail?.notes)
        assertEquals(listOf(initialSetId, firstSetId, thirdSetId, secondSetId), sets.map { it.id })
        assertEquals(listOf("working", "working", "failure", "drop"), sets.map { it.setType })
    }

    @Test
    fun createSuperset_groupsTwoExercisesWithDerivedLabels_andValidates() = runTest {
        repository.seedStarterTrainingData()
        val exercises = repository.observeExercises(query = "").first()
        val a = exercises[0]
        val b = exercises[1]
        val c = exercises[2]
        val sessionId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(sessionId, a.id)
        repository.addExerciseToActiveWorkout(sessionId, b.id)
        repository.addExerciseToActiveWorkout(sessionId, c.id)

        assertEquals(null, repository.createSuperset(sessionId, a.id, a.id))

        val groupId = repository.createSuperset(sessionId, a.id, b.id)
        assertNotNull(groupId)

        val detail = repository.observeActiveWorkoutDetail().first()
        assertEquals(3, detail?.exerciseBlocks?.size)
        val groupings = detail?.exerciseGroupings.orEmpty()
        val superset = groupings.filterIsInstance<ExerciseGrouping.Superset>().single()
        assertEquals(2, superset.group.exerciseBlocks.size)
        assertEquals(listOf("A", "B"), superset.group.exerciseBlocks.map { it.supersetLabel })

        // Re-grouping an already-grouped exercise is rejected.
        assertEquals(null, repository.createSuperset(sessionId, a.id, c.id))
    }

    @Test
    fun dissolveSuperset_returnsExercisesToStandalone() = runTest {
        repository.seedStarterTrainingData()
        val exercises = repository.observeExercises(query = "").first()
        val a = exercises[0]
        val b = exercises[1]
        val sessionId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(sessionId, a.id)
        repository.addExerciseToActiveWorkout(sessionId, b.id)
        val groupId = repository.createSuperset(sessionId, a.id, b.id) ?: error("expected group")

        repository.dissolveSuperset(sessionId, groupId)

        val groupings = repository.observeActiveWorkoutDetail().first()?.exerciseGroupings.orEmpty()
        assertEquals(2, groupings.size)
        assertTrue(groupings.all { it is ExerciseGrouping.Single })
    }

    @Test
    fun superset_inheritsGroupOnAddSet_andAutoDissolvesOnUnderflow() = runTest {
        repository.seedStarterTrainingData()
        val exercises = repository.observeExercises(query = "").first()
        val a = exercises[0]
        val b = exercises[1]
        val sessionId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(sessionId, a.id)
        repository.addExerciseToActiveWorkout(sessionId, b.id)
        val groupId = repository.createSuperset(sessionId, a.id, b.id) ?: error("expected group")

        repository.addSetToExercise(
            sessionId,
            a.id,
            WorkoutSetInputData(setType = "working", reps = null, weightKg = null, rpe = null, notes = null, completed = false),
        )
        val supersetAfterAdd = repository.observeActiveWorkoutDetail().first()
            ?.exerciseGroupings?.filterIsInstance<ExerciseGrouping.Superset>()?.single()
            ?: error("expected superset")
        val aBlock = supersetAfterAdd.group.exerciseBlocks.first { it.exercise.id == a.id }
        assertEquals(2, aBlock.sets.size)
        assertTrue(aBlock.sets.all { it.supersetGroupId == groupId })

        val bBlock = supersetAfterAdd.group.exerciseBlocks.first { it.exercise.id == b.id }
        bBlock.sets.forEach { repository.deleteWorkoutSet(it.id) }

        val groupingsAfterDelete = repository.observeActiveWorkoutDetail().first()?.exerciseGroupings.orEmpty()
        assertTrue(groupingsAfterDelete.all { it is ExerciseGrouping.Single })
    }

    @Test
    fun createSuperset_reindexesMembersContiguously() = runTest {
        repository.seedStarterTrainingData()
        val exercises = repository.observeExercises(query = "").first()
        val a = exercises[0]
        val mid = exercises[1]
        val b = exercises[2]
        val sessionId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(sessionId, a.id)
        repository.addExerciseToActiveWorkout(sessionId, mid.id)
        repository.addExerciseToActiveWorkout(sessionId, b.id)

        repository.createSuperset(sessionId, a.id, b.id)

        val groupings = repository.observeActiveWorkoutDetail().first()?.exerciseGroupings.orEmpty()
        assertEquals(2, groupings.size)
        val superset = groupings.filterIsInstance<ExerciseGrouping.Superset>().single()
        assertEquals(setOf(a.id, b.id), superset.group.exerciseBlocks.map { it.exercise.id }.toSet())
        assertTrue(groupings.any { it is ExerciseGrouping.Single && it.block.exercise.id == mid.id })
    }

    @Test
    fun workoutHistoryDetail_preservesSupersetGroupingsAfterFinish() = runTest {
        repository.seedStarterTrainingData()
        val exercises = repository.observeExercises(query = "").first()
        val a = exercises[0]
        val b = exercises[1]
        val sessionId = repository.startBlankWorkout()
        repository.addExerciseToActiveWorkout(sessionId, a.id)
        repository.addExerciseToActiveWorkout(sessionId, b.id)
        val groupId = repository.createSuperset(sessionId, a.id, b.id) ?: error("expected group")

        repository.finishWorkout(sessionId)

        val detail = repository.getWorkoutHistoryDetail(sessionId)
        val groupings = detail?.exerciseGroupings.orEmpty()
        val superset = groupings.filterIsInstance<ExerciseGrouping.Superset>().single()

        assertEquals(groupId, superset.group.supersetGroupId)
        assertEquals(listOf("A", "B"), superset.group.exerciseBlocks.map { it.supersetLabel })
        assertEquals(listOf(a.id, b.id), superset.group.exerciseBlocks.map { it.exercise.id })
    }

    @Test
    fun workoutHistoryDetail_includesWorkoutRecapAfterFinish() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val row = repository.observeExercises(query = "row").first().first()

        currentInstant = WORKOUT_START.minusSeconds(86_400)
        val previousSessionId = repository.startBlankWorkout()
        repository.addSetToExercise(
            previousSessionId,
            bench.id,
            WorkoutSetInputData("working", reps = 5, weightKg = 90.0, rpe = null, notes = null, completed = true),
        )
        repository.finishWorkout(previousSessionId)

        currentInstant = WORKOUT_START
        val sessionId = repository.startBlankWorkout()
        repository.updateActiveWorkoutNotes(sessionId, "  Strong top set, kept rows strict.  ")
        repository.addSetToExercise(
            sessionId,
            bench.id,
            WorkoutSetInputData("working", reps = 5, weightKg = 100.0, rpe = 8.0, notes = null, completed = true),
        )
        repository.addSetToExercise(
            sessionId,
            bench.id,
            WorkoutSetInputData("drop", reps = 10, weightKg = 70.0, rpe = null, notes = null, completed = true),
        )
        repository.addSetToExercise(
            sessionId,
            row.id,
            WorkoutSetInputData("working", reps = 8, weightKg = 80.0, rpe = null, notes = null, completed = true),
        )

        currentInstant = WORKOUT_START.plusSeconds(2_700)
        repository.finishWorkout(sessionId)

        val recap = repository.getWorkoutHistoryDetail(sessionId)?.recap ?: error("Missing recap")

        assertEquals(2_700, recap.durationSeconds)
        assertEquals(2, recap.exerciseCount)
        assertEquals(3, recap.completedSetCount)
        assertEquals(1_840.0, recap.totalVolumeKg, 0.01)
        assertEquals(2, recap.personalRecordCount)
        assertEquals("Strong top set, kept rows strict.", recap.notes)
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
    fun observeActiveWorkoutDetail_keepsLaunchTimeTargetRepsAfterRoutineEdit() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val routineId = repository.createRoutine(
            RoutineInput(
                name = "Bench Focus",
                notes = null,
                exercises = listOf(
                    RoutineExerciseInput(bench.id, targetSets = 3, targetReps = "5"),
                ),
            ),
        )

        repository.startWorkoutFromRoutine(routineId)
        repository.updateRoutine(
            routineId,
            RoutineInput(
                name = "Bench Focus",
                notes = "Adjusted plan",
                exercises = listOf(
                    RoutineExerciseInput(bench.id, targetSets = 3, targetReps = "8"),
                ),
            ),
        )

        val detail = repository.observeActiveWorkoutDetail().first()

        assertEquals("5", detail?.exerciseBlocks?.singleOrNull()?.targetReps)
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
    fun observeActiveWorkoutDetail_alignsPreviousLabelsPerSet() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()

        val priorSessionId = repository.startBlankWorkout()
        listOf(5 to 100.0, 5 to 102.5, 4 to 105.0).forEach { (reps, weightKg) ->
            repository.addSetToExercise(
                sessionId = priorSessionId,
                exerciseId = bench.id,
                input = WorkoutSetInputData(
                    setType = "working",
                    reps = reps,
                    weightKg = weightKg,
                    rpe = null,
                    notes = null,
                    completed = true,
                ),
            )
        }
        repository.finishWorkout(priorSessionId)

        currentInstant = WORKOUT_START.plusSeconds(7_200)
        val sessionId = repository.startBlankWorkout()
        repeat(4) {
            repository.addSetToExercise(
                sessionId = sessionId,
                exerciseId = bench.id,
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

        val sets = repository.observeActiveWorkoutDetail().first()
            ?.exerciseBlocks
            ?.singleOrNull()
            ?.sets
            .orEmpty()

        // Turn 10 per-set LAST column: labels align positionally with the prior
        // session's sets; the extra fourth set reuses the final prior set.
        assertEquals(
            listOf("100 kg x 5", "102.5 kg x 5", "105 kg x 4", "105 kg x 4"),
            sets.map { it.previousLabel },
        )
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

        val session = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, sessionId)
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)
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

        val session = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, sessionId)
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, sessionId)
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

        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first().sortedBy { it.startedAtEpochMillis }
        val quickLogSessions = sessions.filter { it.id != activeSessionId }
        val quickLogSets = quickLogSessions.flatMap { session -> database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, session.id) }

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

        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first().sortedBy { it.startedAtEpochMillis }
        val discardedSession = sessions.first { it.id == activeSessionId }
        val quickLogSessions = sessions.filter { it.id != activeSessionId }
        val discardedSets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, activeSessionId)
        val quickLogSets = quickLogSessions.flatMap { session -> database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, session.id) }

        assertEquals(2, sessions.size)
        assertEquals("discarded", discardedSession.status)
        assertTrue(discardedSets.isEmpty())
        assertEquals(1, quickLogSessions.size)
        assertEquals("completed", quickLogSessions.single().status)
        assertEquals(1, quickLogSets.size)
        assertEquals(quickLogSessions.single().id, quickLogSets.single().sessionId)
    }

    @Test
    fun finishWorkout_onCompletedSession_doesNotRewriteTerminalState() = runTest {
        val endedAt = WORKOUT_START.plusSeconds(1200).toEpochMilli()
        val session =
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-completed",
                routineId = null,
                title = "Completed workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = endedAt,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        database.trainingDao().upsertWorkoutSession(session)
        currentInstant = WORKOUT_START.plusSeconds(3600)

        repository.finishWorkout(session.id)

        val savedSession = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, session.id)

        assertEquals("completed", savedSession?.status)
        assertEquals(endedAt, savedSession?.endedAtEpochMillis)
    }

    @Test
    fun discardWorkout_onDiscardedSession_doesNotRewriteTerminalState() = runTest {
        val endedAt = WORKOUT_START.plusSeconds(1800).toEpochMilli()
        val session =
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-discarded",
                routineId = null,
                title = "Discarded workout",
                status = "discarded",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = endedAt,
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        database.trainingDao().upsertWorkoutSession(session)
        currentInstant = WORKOUT_START.plusSeconds(5400)

        repository.discardWorkout(session.id)

        val savedSession = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, session.id)

        assertEquals("discarded", savedSession?.status)
        assertEquals(endedAt, savedSession?.endedAtEpochMillis)
    }

    @Test
    fun startWorkoutFromRoutine_whenBlankActiveWorkoutExists_returnsExistingSessionUnchanged() = runTest {
        repository.seedStarterTrainingData()
        val routine = repository.observeRoutineSummaries().first().first { it.name == "Full Body A" }

        val blankSessionId = repository.startBlankWorkout()
        val returnedSessionId = repository.startWorkoutFromRoutine(routine.id)

        val active = repository.observeActiveWorkoutSummary().first()
        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
        val sets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, blankSessionId)

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
        val originalSets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, activeSessionId)

        val returnedSessionId = repository.startWorkoutFromRoutine(push.id)

        val active = repository.observeActiveWorkoutSummary().first()
        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
        val setsAfterSecondLaunch = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, activeSessionId)

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
        val originalSets = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, firstSessionId)

        val secondSessionId = repository.startWorkoutFromRoutine(routine.id)

        val active = repository.observeActiveWorkoutSummary().first()
        val sessions = database.trainingDao().observeWorkoutSessions(TEST_ACCOUNT_ID).first()
        val setsAfterRepeat = database.trainingDao().getWorkoutSets(TEST_ACCOUNT_ID, firstSessionId)

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
        database.trainingDao().upsertExerciseDefinition(customBenchPress)

        repository.seedStarterTrainingData()

        val exercises = repository.observeExercises().first()
        val routines = repository.observeRoutineSummaries().first()
        val pushRoutineExercises = database.trainingDao().getRoutineExercises(TEST_ACCOUNT_ID, "starter-routine-push")
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
                accountId = TEST_ACCOUNT_ID,
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

        val savedSession = database.trainingDao().getWorkoutSession(TEST_ACCOUNT_ID, "session-linked-routine")

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
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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

        database.trainingDao().upsertExerciseDefinition(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSession(activeSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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

        database.trainingDao().upsertExerciseDefinition(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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
    fun getLatestWorkoutForExport_fallsBackWhenNewestCompletedWorkoutHasNoCompletedSets() = runTest {
        val exercise =
            ExerciseEntity(
                id = "exercise-bench",
                name = "Bench Press",
                category = "strength",
                equipment = "barbell",
                targetMuscles = "chest,triceps,shoulders",
                isCustom = false,
            )
        val olderCompletedSession =
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-older-completed",
                routineId = null,
                title = "Older workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(1800).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )
        val newerEmptyCompletedSession =
            WorkoutSessionEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "session-newer-empty",
                routineId = null,
                title = "Newer empty workout",
                status = "completed",
                startedAtEpochMillis = WORKOUT_START.plusSeconds(3600).toEpochMilli(),
                endedAtEpochMillis = WORKOUT_START.plusSeconds(5400).toEpochMilli(),
                notes = null,
                healthConnectRecordId = null,
                healthConnectLastExportedAtEpochMillis = null,
            )

        database.trainingDao().upsertExerciseDefinition(exercise)
        database.trainingDao().upsertWorkoutSession(olderCompletedSession)
        database.trainingDao().upsertWorkoutSession(newerEmptyCompletedSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
                id = "set-older-completed",
                sessionId = olderCompletedSession.id,
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
                accountId = TEST_ACCOUNT_ID,
                id = "set-newer-incomplete",
                sessionId = newerEmptyCompletedSession.id,
                exerciseId = exercise.id,
                sortOrder = 0,
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
        assertEquals(olderCompletedSession.id, workout?.session?.id)
        assertEquals(listOf("set-older-completed"), workout?.sets?.map { it.id })
    }

    @Test
    fun historyAndLatestExport_includeCompletedWorkoutsOnly() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val activeSession = repository.startBlankWorkout()
        repository.addSetToExercise(
            activeSession,
            bench.id,
            WorkoutSetInputData("working", reps = 5, weightKg = 100.0, rpe = 8.0, notes = null, completed = true),
        )

        assertTrue(repository.observeWorkoutHistory().first().isEmpty())
        assertEquals(null, repository.getLatestWorkoutForExport())

        repository.finishWorkout(activeSession)

        val history = repository.observeWorkoutHistory().first()
        val detail = repository.getWorkoutHistoryDetail(activeSession)
        val export = repository.getLatestWorkoutForExport()

        assertEquals(1, history.size)
        assertEquals("Blank workout", history.single().title)
        assertEquals(500.0, history.single().totalVolumeKg, 0.01)
        assertEquals(1, detail?.exerciseBlocks?.single()?.sets?.size)
        assertEquals(activeSession, export?.session?.id)
    }

    @Test
    fun observeExerciseProgress_returnsPrsAndTrendsForCompletedSetsOnly() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 5, 100.0, 8.0, null, true))
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 1, 130.0, 10.0, null, false))
        repository.finishWorkout(sessionId)

        val progress = repository.observeExerciseProgress(bench.id).first()

        assertEquals("Barbell Bench Press", progress?.exerciseName)
        assertEquals(100.0, progress?.heaviestWeightKg ?: 0.0, 0.01)
        assertEquals(116.67, progress?.bestEstimatedOneRepMaxKg ?: 0.0, 0.01)
        assertEquals(1, progress?.trend?.size)
    }

    @Test
    fun observeExerciseProgress_returnsNullWhenCompletedWorkoutsOnlyContainInvalidOrIncompleteSets() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val sessionId = repository.startBlankWorkout()
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", null, 100.0, 8.0, null, true))
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 5, null, 8.0, null, true))
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 0, 100.0, 8.0, null, true))
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 5, 0.0, 8.0, null, true))
        repository.addSetToExercise(sessionId, bench.id, WorkoutSetInputData("working", 8, 90.0, 9.0, null, false))
        repository.finishWorkout(sessionId)

        val progress = repository.observeExerciseProgress(bench.id).first()

        assertEquals(null, progress)
    }

    @Test
    fun observeTrainingProgressAnalytics_groupsMusclesAndWeeklyVolume() = runTest {
        repository.seedStarterTrainingData()
        val bench = repository.observeExercises(query = "bench").first().single()
        val row = repository.observeExercises(query = "row").first().first()

        currentInstant = WORKOUT_START.minusSeconds(6 * 86_400L)
        val previousWeekSession = repository.startBlankWorkout()
        repository.addSetToExercise(previousWeekSession, bench.id, WorkoutSetInputData("working", 5, 100.0, null, null, true))
        repository.finishWorkout(previousWeekSession)

        currentInstant = WORKOUT_START
        val currentWeekSession = repository.startBlankWorkout()
        repository.addSetToExercise(currentWeekSession, row.id, WorkoutSetInputData("working", 10, 50.0, null, null, true))
        repository.finishWorkout(currentWeekSession)

        val analytics = repository.observeTrainingProgressAnalytics().first()
        val muscles = analytics.muscleGroups.associateBy { it.muscle }

        assertEquals(500.0, muscles.getValue("chest").totalVolumeKg, 0.01)
        assertEquals(1, muscles.getValue("chest").completedSetCount)
        assertEquals(500.0, muscles.getValue("back").totalVolumeKg, 0.01)
        assertEquals(1, muscles.getValue("back").completedSetCount)
        assertEquals(2, analytics.weeklyVolume.size)
        assertTrue(analytics.weeklyVolume.first().weekStartEpochDay < analytics.weeklyVolume.last().weekStartEpochDay)
        assertEquals(listOf(1, 1), analytics.weeklyVolume.map { it.workoutCount })
        assertEquals(listOf(500.0, 500.0), analytics.weeklyVolume.map { it.totalVolumeKg })
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
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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

        database.trainingDao().upsertExerciseDefinition(exercise)
        database.trainingDao().upsertWorkoutSession(completedSession)
        database.trainingDao().upsertWorkoutSession(activeSession)
        database.trainingDao().upsertWorkoutSession(discardedSession)
        database.trainingDao().upsertWorkoutSet(
            WorkoutSetEntity(
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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
                accountId = TEST_ACCOUNT_ID,
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
