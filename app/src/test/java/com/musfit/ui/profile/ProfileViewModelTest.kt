package com.musfit.ui.profile

import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.AppSettings
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.DEFAULT_APP_SETTINGS
import com.musfit.data.repository.DEFAULT_USER_PROFILE
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.RoutineSummary
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.data.repository.WorkoutForExport
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.profile.Sex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // UTC epoch-day anchors: the ViewModel's window math is epoch-day based, so
    // local-zone times would flake in far-west zones.
    private val fixedDate = LocalDate.of(2026, 7, 2)
    private val DAY = 86_400_000L
    private val nowMillis = fixedDate.toEpochDay() * DAY + 14L * 3_600_000L
    private fun daysAgo(n: Long) = (fixedDate.toEpochDay() - n) * DAY + 12L * 3_600_000L

    private fun profileViewModel(
        profileRepository: ProfileRepository = FakeProfileRepository(),
        healthRepository: HealthRepository = FakeHealthRepo(),
        foodRepository: FoodRepository = FakeFoodGoalRepo(),
        trainingRepository: TrainingRepository = FakeTrainingRepo(),
        goalsRepository: GoalsRepository = FakeGoalsRepo(),
    ) = ProfileViewModel(
        profileRepository = profileRepository,
        healthRepository = healthRepository,
        foodRepository = foodRepository,
        trainingRepository = trainingRepository,
        goalsRepository = goalsRepository,
        dateProvider = { fixedDate },
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun incompleteProfile_hidesRecommendation() = runTest {
        val viewModel = ProfileViewModel(FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo(), FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoaded)
        assertEquals(false, viewModel.state.value.isProfileComplete)
        assertNull(viewModel.state.value.recommendedTargets)
    }

    @Test
    fun completeProfile_exposesTargetsAndBmi() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Maintain, 0.0, 80.0),
            latestWeight = WeightEntry("w1", 1_000L, 80.0, "manual"),
            targets = RecommendedTargets(2759.0, 144.0, 270.0, 77.0),
        )
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo(), FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isProfileComplete)
        assertEquals(2759.0, viewModel.state.value.recommendedTargets!!.caloriesKcal, 0.001)
        assertEquals(24.7, viewModel.state.value.hero.bmi!!, 0.05)
    }

    @Test
    fun hero_deltaUsesWeeklyAverages_andChartWindows30Days() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Lose, 0.5, 75.0),
        )
        repo.weightSeries = listOf(
            WeightEntry("w3", daysAgo(1), 82.0, "manual"),   // this week
            WeightEntry("w2", daysAgo(10), 83.0, "manual"),  // prior week
            WeightEntry("w1", daysAgo(45), 85.0, "manual"),  // outside the 30-day chart window
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(82.0, hero.latestWeightKg!!, 0.001)
        assertEquals(-1.0, hero.deltaKg!!, 0.001)              // avg(82) − avg(83)
        assertEquals(75.0, hero.goalWeightKg!!, 0.001)
        // progress baseline is the ALL-TIME first entry (85): (85−82)/(85−75) = 0.3
        assertEquals(0.3, hero.goalProgressFraction!!, 0.001)
        assertEquals(listOf(83.0, 82.0), hero.chartSeries)     // 30-day window, oldest→newest
        assertEquals(true, hero.hasAnyEntry)
    }

    @Test
    fun hero_singleEntryHasNoDeltaButKeepsFigure() = runTest {
        val repo = FakeProfileRepository()
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(82.0, hero.latestWeightKg!!, 0.001)
        assertNull(hero.deltaKg)
    }

    @Test
    fun hero_goalBadgeHiddenWithoutGoalWeight() = runTest {
        val repo = FakeProfileRepository() // DEFAULT_USER_PROFILE: goalWeightKg null
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.hero.goalWeightKg)
        assertNull(viewModel.state.value.hero.goalProgressFraction)
    }

    @Test
    fun hero_staleLoggerKeepsFigureWithEmptyChart() = runTest {
        val repo = FakeProfileRepository()
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(40), 84.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(84.0, hero.latestWeightKg!!, 0.001)   // all-time latest survives
        assertTrue(hero.chartSeries.isEmpty())              // 30-day window empty
        assertEquals(true, hero.hasAnyEntry)                // → "no entries in last 30 days", not first-log
    }

    @Test
    fun hero_goalBadgePercentlessWhenStartEqualsGoal() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Lose, 0.5, 82.0),
        )
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual")) // start == goal → progress null
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(82.0, viewModel.state.value.hero.goalWeightKg!!, 0.001)
        assertNull(viewModel.state.value.hero.goalProgressFraction)
    }

    @Test
    fun hero_goalProgressCapsAtFull() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Lose, 0.5, 80.0),
        )
        repo.weightSeries = listOf(
            WeightEntry("w2", daysAgo(1), 78.0, "manual"),  // past the goal
            WeightEntry("w1", daysAgo(20), 85.0, "manual"),
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1.0, viewModel.state.value.hero.goalProgressFraction!!, 0.001)
    }

    @Test
    fun hero_zeroEntriesEverShowsFirstLogState() = runTest {
        val repo = FakeProfileRepository()
        repo.weightSeries = emptyList()
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertNull(hero.latestWeightKg)
        assertEquals(false, hero.hasAnyEntry)
    }

    @Test
    fun hero_bmiNullWithoutHeight() = runTest {
        val repo = FakeProfileRepository() // DEFAULT_USER_PROFILE: heightCm null
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.hero.bmi)
    }

    @Test
    fun tiles_threeStatesFromAllTimeCountWithWindowedSparkline() = runTest {
        val repo = FakeProfileRepository(
            measurements = mapOf(
                "waist" to listOf( // 2 entries → sparkline
                    BodyMeasurement("m2", "waist", 88.0, "cm", daysAgo(1)),
                    BodyMeasurement("m1", "waist", 89.0, "cm", daysAgo(20)),
                ),
                "chest" to listOf( // 1 entry → no sparkline, delta null
                    BodyMeasurement("c1", "chest", 104.0, "cm", daysAgo(5)),
                ),
                "body_fat" to listOf( // 2 all-time but only 1 in the 90-day window → sparkline suppressed
                    BodyMeasurement("b2", "body_fat", 18.5, "%", daysAgo(10)),
                    BodyMeasurement("b1", "body_fat", 19.5, "%", daysAgo(120)),
                ),
                "hips" to listOf( // stale-newest: both outside the 90-day window
                    BodyMeasurement("h2", "hips", 96.0, "cm", daysAgo(100)),
                    BodyMeasurement("h1", "hips", 98.0, "cm", daysAgo(150)),
                ),
            ),
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val tiles = viewModel.state.value.tiles.associateBy { it.type }
        assertEquals(listOf(89.0, 88.0), tiles["waist"]!!.sparkline)
        assertEquals(-1.0, tiles["waist"]!!.deltaFromPrevious!!, 0.001)
        assertEquals(2, tiles["waist"]!!.entryCount)
        assertNull(tiles["chest"]!!.deltaFromPrevious)
        assertEquals(listOf(104.0), tiles["chest"]!!.sparkline)             // lone in-window point kept (dot)
        assertEquals(-1.0, tiles["body_fat"]!!.deltaFromPrevious!!, 0.001) // delta is all-time
        assertEquals(listOf(18.5), tiles["body_fat"]!!.sparkline)           // window has 1 point
        assertEquals(0, tiles["arms"]!!.entryCount)                         // never logged
        // Stale-newest tile: value + all-time delta survive; chart empty (the tile analog
        // of the stale-logger hero).
        assertEquals(96.0, tiles["hips"]!!.value!!, 0.001)
        assertEquals(-2.0, tiles["hips"]!!.deltaFromPrevious!!, 0.001)
        assertTrue(tiles["hips"]!!.sparkline.isEmpty())
        assertEquals(2, tiles["hips"]!!.entryCount)
    }

    @Test
    fun applyTargetsToFood_writesGoalPreservingOtherFields() = runTest {
        val food = FakeFoodGoalRepo(
            initial = FoodGoal(
                dailyCaloriesKcal = 2000.0, proteinGrams = 100.0, carbsGrams = 250.0, fatGrams = 60.0,
                fiberGrams = 30.0, sugarGrams = 50.0, saturatedFatGrams = 20.0, sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced, includeTrainingCalories = true, useNetCarbs = true,
                waterGoalMilliliters = 2500.0,
            ),
        )
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Maintain, 0.0, 80.0),
            latestWeight = WeightEntry("w1", 1_000L, 80.0, "manual"),
            targets = RecommendedTargets(2759.0, 144.0, 270.0, 77.0),
        )
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), food, FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.applyTargetsToFood()
        dispatcher.scheduler.advanceUntilIdle()

        val saved = food.saved!!
        assertEquals(2759.0, saved.dailyCaloriesKcal, 0.001)
        assertEquals(144.0, saved.proteinGrams, 0.001)
        assertEquals(true, saved.includeTrainingCalories)
        assertEquals(true, saved.useNetCarbs)
        assertEquals(2500.0, saved.waterGoalMilliliters, 0.001)
        assertEquals("Applied your targets to Food goals.", viewModel.state.value.message)
    }

    @Test
    fun saveProfile_failureSurfacesMessage() = runTest {
        val repo = FakeProfileRepository()
        repo.saveProfileError = IllegalStateException("disk full")
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.saveProfile(DEFAULT_USER_PROFILE)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("disk full", viewModel.state.value.message)
    }

    @Test
    fun logWeight_callsRepository() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo(), FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logWeight(83.6)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(83.6, repo.loggedWeight!!, 0.001)
    }

    @Test
    fun editEntry_callsRepositoryWithIdAndValue() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo(), FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.editEntry("abc", 81.3)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc", repo.updatedId)
        assertEquals(81.3, repo.updatedValue!!, 0.001)
    }

    @Test
    fun deleteEntry_callsRepositoryWithId() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo(), FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteEntry("xyz")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("xyz", repo.deletedId)
    }

    @Test
    fun state_exposesWeightEntriesForSheet() = runTest {
        val repo = FakeProfileRepository(latestWeight = WeightEntry("w9", 1_000L, 84.0, "manual"))
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo(), FakeTrainingRepo(), FakeGoalsRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("w9", viewModel.state.value.weightEntries.first().id)
    }

    @Test
    fun planCards_dietAlwaysAndTrainingWithProgram() = runTest {
        val training = FakeTrainingRepo()
        training.routines.value = listOf(
            RoutineSummary(id = "r1", name = "Machine A", notes = null, exerciseCount = 5, targetSetCount = 15, isStarter = false, programName = "Beginner Program"),
        )
        training.history.value = listOf(
            WorkoutHistorySummary("s1", "Machine A", nowMillis - 86_400_000L, null, 12, 1000.0),
        )
        val goals = FakeGoalsRepo() // default weeklySessionTarget = 4
        val viewModel = profileViewModel(trainingRepository = training, goalsRepository = goals)
        dispatcher.scheduler.advanceUntilIdle()

        val cards = viewModel.state.value.planCards
        assertEquals("diet", cards[0].id)
        assertEquals("Balanced diet", cards[0].title)                    // FakeFoodGoalRepo mode = Balanced
        assertTrue(cards[0].subtitle.contains("2000 kcal target"))       // calorie figure for non-protein-led modes
        assertEquals("training", cards[1].id)
        assertEquals("Beginner Program", cards[1].title)
        assertTrue(cards[1].subtitle.contains("1 of 4 sessions this week"))
    }

    @Test
    fun planCards_proteinLedModeShowsProteinFigure() = runTest {
        val food = FakeFoodGoalRepo(
            initial = FoodGoal(
                2400.0, 180.0, 220.0, 70.0, 30.0, 50.0, 20.0, 2300.0,
                FoodGoalMode.HighProtein, includeTrainingCalories = false,
            ),
        )
        val viewModel = profileViewModel(foodRepository = food)
        dispatcher.scheduler.advanceUntilIdle()

        val diet = viewModel.state.value.planCards.first { it.id == "diet" }
        assertEquals("High protein diet", diet.title)
        assertTrue(diet.subtitle.contains("180 g protein target"))
    }

    @Test
    fun planCards_noRoutinesShowsSetupCard_andZeroTargetDropsDenominator() = runTest {
        val training = FakeTrainingRepo() // no routines, no history
        val goals = FakeGoalsRepo(userGoals = UserGoals(weeklySessionTarget = 0))
        val viewModel = profileViewModel(trainingRepository = training, goalsRepository = goals)
        dispatcher.scheduler.advanceUntilIdle()

        val card = viewModel.state.value.planCards.first { it.id == "training" }
        assertEquals("No program yet", card.title)
        assertEquals("Set one up in Training", card.subtitle)

        training.routines.value = listOf(
            RoutineSummary(id = "r1", name = "Machine A", notes = null, exerciseCount = 5, targetSetCount = 15, isStarter = false),
        )
        dispatcher.scheduler.advanceUntilIdle()
        val withRoutine = viewModel.state.value.planCards.first { it.id == "training" }
        assertEquals("Machine A", withRoutine.title)                     // no programName → routine name
        assertTrue(withRoutine.subtitle.contains("0 sessions this week"))
        assertEquals(false, withRoutine.subtitle.contains(" of "))       // target 0 drops the denominator
    }

    @Test
    fun planCards_allEightModesGetLabelAndFigureKind() = runTest {
        val proteinLed = setOf(FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain)
        FoodGoalMode.entries.forEach { mode ->
            val food = FakeFoodGoalRepo(
                initial = FoodGoal(
                    2100.0, 160.0, 220.0, 70.0, 30.0, 50.0, 20.0, 2300.0,
                    mode, includeTrainingCalories = false,
                ),
            )
            val viewModel = profileViewModel(foodRepository = food)
            dispatcher.scheduler.advanceUntilIdle()

            val diet = viewModel.state.value.planCards.first { it.id == "diet" }
            assertTrue("$mode title", diet.title.endsWith(" diet"))
            if (mode in proteinLed) {
                assertTrue("$mode figure", diet.subtitle.contains("160 g protein target"))
            } else {
                assertTrue("$mode figure", diet.subtitle.contains("2100 kcal target"))
            }
        }
    }

    @Test
    fun hcNudge_visibleWhenNoPermissionsGranted() = runTest {
        val health = FakeHealthRepo() // status(): Available + granted setOf("steps") → hidden
        val viewModel = profileViewModel(healthRepository = health)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.state.value.isHealthConnectNudgeVisible)

        val disconnected = FakeHealthRepoMutableStatus() // starts with no granted permissions
        val viewModel2 = profileViewModel(healthRepository = disconnected)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel2.state.value.isHealthConnectNudgeVisible)

        // Spec: "disappears once connected" — the user grants permissions in the HC app
        // and returns; the resume refresh must hide the nudge.
        disconnected.granted = setOf("steps")
        viewModel2.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel2.state.value.isHealthConnectNudgeVisible)
    }

    @Test
    fun hcNudge_visibleWhenHealthConnectNotInstalled() = runTest {
        val viewModel = profileViewModel(healthRepository = FakeHealthRepoNotInstalled())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.state.value.isHealthConnectNudgeVisible)
    }

    private class FakeProfileRepository(
        private val profile: UserProfile = DEFAULT_USER_PROFILE,
        private val latestWeight: WeightEntry? = null,
        private val targets: RecommendedTargets? = null,
        private val measurements: Map<String, List<BodyMeasurement>> = emptyMap(),
    ) : ProfileRepository {
        var loggedWeight: Double? = null
        var updatedId: String? = null
        var updatedValue: Double? = null
        var deletedId: String? = null
        var saveProfileError: Throwable? = null
        var weightSeries: List<WeightEntry> = listOfNotNull(latestWeight) // newest-first like the real DAO
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)
        override suspend fun saveProfile(profile: UserProfile) {
            saveProfileError?.let { throw it }
        }
        override fun observeRecommendedTargets(): Flow<RecommendedTargets?> = flowOf(targets)
        override suspend fun logWeight(weightKg: Double, source: String) { loggedWeight = weightKg }
        override fun observeLatestWeight(): Flow<WeightEntry?> = flowOf(latestWeight)
        override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
            flowOf(weightSeries)
        override suspend fun logMeasurement(type: String, value: Double, unit: String) = Unit
        override suspend fun deleteEntry(id: String) { deletedId = id }
        override suspend fun updateEntryValue(id: String, value: Double) { updatedId = id; updatedValue = value }
        override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> =
            flowOf(measurements)
        override fun observeSettings(): Flow<AppSettings> = flowOf(DEFAULT_APP_SETTINGS)
        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeHealthRepo : HealthRepository {
        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.Available, setOf("steps"))
        override suspend fun requestablePermissions(): Set<String> = setOf("steps")
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(null, null, null, null)
        override suspend fun exportLatestWorkout(): String? = null
    }

    /** Pins the `availability != Available` half of the nudge predicate. */
    private class FakeHealthRepoNotInstalled : HealthRepository {
        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet())
        override suspend fun requestablePermissions(): Set<String> = emptySet()
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(null, null, null, null)
        override suspend fun exportLatestWorkout(): String? = null
    }

    /** [FakeHealthRepo] whose granted-permission set can change between [status] calls. */
    private class FakeHealthRepoMutableStatus : HealthRepository {
        var granted: Set<String> = emptySet()
        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.Available, granted)
        override suspend fun requestablePermissions(): Set<String> = setOf("steps")
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(null, null, null, null)
        override suspend fun exportLatestWorkout(): String? = null
    }

    private class FakeTrainingRepo : TrainingRepository {
        val routines = MutableStateFlow<List<RoutineSummary>>(emptyList())
        val history = MutableStateFlow<List<WorkoutHistorySummary>>(emptyList())

        override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = routines

        override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = history

        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet = LoggedWorkoutSet("set-1", exerciseName, reps, weightKg, true)

        override suspend fun setCompletion(setId: String, completed: Boolean) = Unit

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> =
            MutableStateFlow(TrainingSummary())

        override suspend fun createRoutine(input: RoutineInput): String = "routine-1"

        override suspend fun updateRoutine(routineId: String, input: RoutineInput) = Unit

        override suspend fun duplicateRoutine(routineId: String): String = "$routineId-copy"

        override suspend fun deleteRoutine(routineId: String) = Unit

        override suspend fun getRoutineDetail(routineId: String): RoutineDetail? = null

        override suspend fun startBlankWorkout(): String = "session-blank"

        override suspend fun startWorkoutFromRoutine(routineId: String): String = "session-$routineId"

        override suspend fun getLatestWorkoutForExport(): WorkoutForExport? = null

        override suspend fun markWorkoutExported(
            sessionId: String,
            recordId: String,
            exportedAtEpochMillis: Long,
        ) = Unit
    }

    private class FakeGoalsRepo(
        private val userGoals: UserGoals = UserGoals(),
    ) : GoalsRepository {
        override fun observeUserGoals(): Flow<UserGoals> = flowOf(userGoals)

        override suspend fun updateUserGoals(goals: UserGoals) = Unit
    }

    private class FakeFoodGoalRepo(
        private val initial: FoodGoal = FoodGoal(
            2000.0, 100.0, 250.0, 60.0, 30.0, 50.0, 20.0, 2300.0,
            FoodGoalMode.Balanced, includeTrainingCalories = false,
        ),
    ) : FoodRepository {
        val goalFlow = MutableStateFlow(initial)
        var saved: FoodGoal? = null

        override fun observeFoodGoal(): Flow<FoodGoal> = goalFlow
        override suspend fun updateFoodGoal(goal: FoodGoal) {
            saved = goal
            goalFlow.value = goal
        }

        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String = ""

        override suspend fun logFood(input: FoodLogInput): String = ""

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> =
            MutableStateFlow(NutritionTotals(0.0, 0.0, 0.0, 0.0))

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> =
            MutableStateFlow(FoodDiary(totals = NutritionTotals(0.0, 0.0, 0.0, 0.0), meals = emptyList()))

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override fun observeSameAsYesterday(mealType: String, date: LocalDate): Flow<List<SavedFoodItem>> =
            MutableStateFlow(emptyList())

        override suspend fun logSavedFood(input: SavedFoodLogInput): String = ""

        override suspend fun quickLog(input: QuickCalorieLogInput): String = ""

        override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) = Unit

        override suspend fun deleteDiaryEntry(mealItemId: String) = Unit

        override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String = ""

        override suspend fun deleteSavedFood(foodId: String) = Unit
    }
}
