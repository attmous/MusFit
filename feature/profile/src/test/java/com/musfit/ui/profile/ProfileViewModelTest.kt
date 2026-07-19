package com.musfit.ui.profile

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.repeatOnLifecycle
import com.musfit.data.local.entity.DailyHealthSummaryEntity
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
import com.musfit.data.repository.ProductLookupResult
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
import com.musfit.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val dispatcher get() = mainDispatcherRule.dispatcher

    // UTC epoch-day anchors: the ViewModel's window math is epoch-day based, so
    // local-zone times would flake in far-west zones.
    private val fixedDate = LocalDate.of(2026, 7, 2)
    private val dayMillis = 86_400_000L
    private fun daysAgo(n: Long) = (fixedDate.toEpochDay() - n) * dayMillis + 12L * 3_600_000L

    private fun TestScope.profileViewModel(
        profileRepository: ProfileRepository = FakeProfileRepository(),
        healthRepository: HealthRepository = FakeHealthRepo(),
        foodRepository: FoodRepository = FakeFoodGoalRepo(),
        trainingRepository: TrainingRepository = FakeTrainingRepo(),
        goalsRepository: GoalsRepository = FakeGoalsRepo(),
        dateProvider: () -> LocalDate = { fixedDate },
    ): ProfileViewModel {
        val viewModel = ProfileViewModel(
            profileRepository = profileRepository,
            healthRepository = healthRepository,
            foodRepository = foodRepository,
            trainingRepository = trainingRepository,
            goalsRepository = goalsRepository,
            dateProvider = dateProvider,
        )
        observeProfile(viewModel)
        return viewModel
    }

    @Test
    fun lifecycleOwner_stoppedAndStarted_resumesCurrentValuesWithoutDuplicateCollectors() = runTest {
        val trainingRepository = FakeTrainingRepo().apply {
            routines.value = listOf(routineSummary("Initial program"))
        }
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            healthRepository = FakeHealthRepo(),
            foodRepository = FakeFoodGoalRepo(),
            trainingRepository = trainingRepository,
            goalsRepository = FakeGoalsRepo(),
            dateProvider = { fixedDate },
        )
        testScheduler.runCurrent()
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        val lifecycle = owner.lifecycle
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val lifecycleCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { }
            }
        }
        testScheduler.runCurrent()

        assertEquals(0, trainingRepository.activeRoutineSummaryCollectors)
        assertEquals(0, trainingRepository.routineSummarySubscriptionStarts)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        testScheduler.runCurrent()
        assertTrue(viewModel.state.value.plansSummary.contains("Initial program"))
        assertEquals(1, trainingRepository.activeRoutineSummaryCollectors)
        assertEquals(1, trainingRepository.routineSummarySubscriptionStarts)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        testScheduler.advanceTimeBy(4_999)
        testScheduler.runCurrent()
        assertEquals(1, trainingRepository.activeRoutineSummaryCollectors)
        testScheduler.advanceTimeBy(2)
        testScheduler.runCurrent()
        assertEquals(0, trainingRepository.activeRoutineSummaryCollectors)

        trainingRepository.routines.value = listOf(routineSummary("Updated while stopped"))
        testScheduler.runCurrent()
        assertTrue(viewModel.state.value.plansSummary.contains("Initial program"))

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        testScheduler.runCurrent()
        assertTrue(viewModel.state.value.plansSummary.contains("Updated while stopped"))
        assertEquals(1, trainingRepository.activeRoutineSummaryCollectors)
        assertEquals(2, trainingRepository.routineSummarySubscriptionStarts)

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleCollection.join()
        assertEquals(1, trainingRepository.maxRoutineSummaryCollectors)
    }

    @Test
    fun incompleteProfile_hidesRecommendation() = runTest {
        val viewModel = profileViewModel()
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
        val viewModel = profileViewModel(profileRepository = repo)
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
        // Two entries per delta window so the averaging semantics are discriminated
        // from the retired changeOverWindow (latest − newest entry at/before the 7d
        // anchor). Computed by hand:
        //   weightTrend:       avg(82, 84) − avg(85, 89) = 83 − 87 = −4.0
        //   changeOverWindow:  82 − 85                            = −3.0
        repo.weightSeries = listOf(
            WeightEntry("w6", daysAgo(1), 82.0, "manual"), // this week
            WeightEntry("w5", daysAgo(3), 84.0, "manual"), // this week
            WeightEntry("w4", daysAgo(8), 85.0, "manual"), // prior week
            WeightEntry("w3", daysAgo(10), 89.0, "manual"), // prior week
            WeightEntry("w2", daysAgo(29), 86.0, "manual"), // just inside the 30-day chart window
            WeightEntry("w1", daysAgo(31), 90.0, "manual"), // just outside the 30-day chart window
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(82.0, hero.latestWeightKg!!, 0.001)
        assertEquals(-4.0, hero.deltaKg!!, 0.001) // weekly averages, not endpoints
        assertEquals(75.0, hero.goalWeightKg!!, 0.001)
        // progress baseline is the ALL-TIME first entry (90): (90−82)/(90−75) = 8/15
        assertEquals(8.0 / 15.0, hero.goalProgressFraction!!, 0.001)
        // 30-day window pins the 29/31-day boundary, oldest→newest
        assertEquals(listOf(86.0, 89.0, 85.0, 84.0, 82.0), hero.chartSeries)
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
        assertEquals(84.0, hero.latestWeightKg!!, 0.001) // all-time latest survives
        assertTrue(hero.chartSeries.isEmpty()) // 30-day window empty
        assertEquals(true, hero.hasAnyEntry) // → "no entries in last 30 days", not first-log
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
            WeightEntry("w2", daysAgo(1), 78.0, "manual"), // past the goal
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
                "thighs" to listOf( // 90-day boundary: day 89 is inside, day 91 is out
                    BodyMeasurement("t2", "thighs", 60.0, "cm", daysAgo(89)),
                    BodyMeasurement("t1", "thighs", 62.0, "cm", daysAgo(91)),
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
        assertEquals(listOf(104.0), tiles["chest"]!!.sparkline) // lone in-window point kept (dot)
        assertEquals(-1.0, tiles["body_fat"]!!.deltaFromPrevious!!, 0.001) // delta is all-time
        assertEquals(listOf(18.5), tiles["body_fat"]!!.sparkline) // window has 1 point
        assertEquals(0, tiles["arms"]!!.entryCount) // never logged
        // Stale-newest tile: value + all-time delta survive; chart empty (the tile analog
        // of the stale-logger hero).
        assertEquals(96.0, tiles["hips"]!!.value!!, 0.001)
        assertEquals(-2.0, tiles["hips"]!!.deltaFromPrevious!!, 0.001)
        assertTrue(tiles["hips"]!!.sparkline.isEmpty())
        assertEquals(2, tiles["hips"]!!.entryCount)
        // 90-day boundary pin: exactly the day-89 entry is windowed in.
        assertEquals(listOf(60.0), tiles["thighs"]!!.sparkline)
        assertEquals(2, tiles["thighs"]!!.entryCount)
    }

    @Test
    fun tiles_thirtyDayTrendComparesAgainstValueAsOfThirtyDaysAgo() = runTest {
        val repo = FakeProfileRepository(
            measurements = mapOf(
                "waist" to listOf( // baseline is the newest entry at/before the 30-day cutoff
                    BodyMeasurement("w3", "waist", 82.0, "cm", daysAgo(1)),
                    BodyMeasurement("w2", "waist", 82.6, "cm", daysAgo(10)),
                    BodyMeasurement("w1", "waist", 83.2, "cm", daysAgo(40)),
                ),
                "chest" to listOf( // whole history inside the window → oldest entry is the baseline
                    BodyMeasurement("c2", "chest", 104.5, "cm", daysAgo(5)),
                    BodyMeasurement("c1", "chest", 103.7, "cm", daysAgo(20)),
                ),
                "arms" to listOf( // a single entry has no baseline
                    BodyMeasurement("a1", "arms", 38.5, "cm", daysAgo(3)),
                ),
                "hips" to listOf( // stale logger: nothing since the window → no trend claimed
                    BodyMeasurement("h2", "hips", 96.0, "cm", daysAgo(100)),
                    BodyMeasurement("h1", "hips", 98.0, "cm", daysAgo(150)),
                ),
            ),
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val tiles = viewModel.state.value.tiles.associateBy { it.type }
        assertEquals(-1.2, tiles["waist"]!!.delta30d!!, 0.001)
        assertEquals(0.8, tiles["chest"]!!.delta30d!!, 0.001)
        assertNull(tiles["arms"]!!.delta30d)
        assertNull(tiles["hips"]!!.delta30d)
        assertNull(tiles["thighs"]!!.delta30d) // never logged
    }

    @Test
    fun onScreenResumed_reanchorsDateWindows() = runTest {
        var currentDate = fixedDate
        val repo = FakeProfileRepository()
        repo.weightSeries = listOf(
            WeightEntry("w2", daysAgo(1), 82.0, "manual"), // inside every window in play
            WeightEntry("w1", daysAgo(25), 84.0, "manual"), // in the 30d chart at fixedDate, out at +8d
        )
        val viewModel = profileViewModel(
            profileRepository = repo,
            dateProvider = { currentDate },
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(84.0, 82.0), viewModel.state.value.hero.chartSeries)

        currentDate = fixedDate.plusDays(8) // midnight passes while the process is cached
        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()

        // The 30d chart window re-anchors and slides past w1.
        assertEquals(listOf(82.0), viewModel.state.value.hero.chartSeries)
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
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logWeight(83.6)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(83.6, repo.loggedWeight!!, 0.001)
    }

    @Test
    fun editEntry_callsRepositoryWithIdAndValue() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.editEntry("abc", 81.3)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc", repo.updatedId)
        assertEquals(81.3, repo.updatedValue!!, 0.001)
    }

    @Test
    fun deleteEntry_callsRepositoryWithId() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteEntry("xyz")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("xyz", repo.deletedId)
    }

    @Test
    fun state_exposesWeightEntriesForSheet() = runTest {
        val repo = FakeProfileRepository(latestWeight = WeightEntry("w9", 1_000L, 84.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("w9", viewModel.state.value.weightEntries.first().id)
    }

    @Test
    fun plansSummary_combinesPaceDietAndProgram() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 183.0, ActivityLevel.Moderate, GoalType.Gain, 0.3, 82.0),
        )
        val training = FakeTrainingRepo()
        training.routines.value = listOf(
            RoutineSummary(id = "r1", name = "Machine A", notes = null, exerciseCount = 5, targetSetCount = 15, isStarter = false, programName = "Beginner Program"),
        )
        val goals = FakeGoalsRepo() // default weeklySessionTarget = 4
        val viewModel = profileViewModel(
            profileRepository = repo,
            trainingRepository = training,
            goalsRepository = goals,
        )
        dispatcher.scheduler.advanceUntilIdle()

        // FakeFoodGoalRepo default: Balanced, 2000 kcal → the calorie figure with grouping.
        assertEquals("Gain 0.3 kg/wk · 2,000 kcal · Beginner Program ×4", viewModel.state.value.plansSummary)
    }

    @Test
    fun plansSummary_proteinLedModeShowsProteinFigure() = runTest {
        val food = FakeFoodGoalRepo(
            initial = FoodGoal(
                2400.0, 180.0, 220.0, 70.0, 30.0, 50.0, 20.0, 2300.0,
                FoodGoalMode.HighProtein, includeTrainingCalories = false,
            ),
        )
        val viewModel = profileViewModel(foodRepository = food)
        dispatcher.scheduler.advanceUntilIdle()

        // DEFAULT_USER_PROFILE is Maintain; no routine → no program part.
        assertEquals("Maintain · 180 g protein", viewModel.state.value.plansSummary)
    }

    @Test
    fun plansSummary_noRoutineOmitsProgram_andZeroTargetDropsMultiplier() = runTest {
        val training = FakeTrainingRepo() // no routines
        val goals = FakeGoalsRepo(userGoals = UserGoals(weeklySessionTarget = 0))
        val viewModel = profileViewModel(trainingRepository = training, goalsRepository = goals)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Maintain · 2,000 kcal", viewModel.state.value.plansSummary)

        training.routines.value = listOf(
            RoutineSummary(id = "r1", name = "Machine A", notes = null, exerciseCount = 5, targetSetCount = 15, isStarter = false),
        )
        dispatcher.scheduler.advanceUntilIdle()
        // No programName → routine name; target 0 drops the ×n multiplier.
        assertEquals("Maintain · 2,000 kcal · Machine A", viewModel.state.value.plansSummary)
    }

    @Test
    fun plansSummary_allEightModesGetFigureKind() = runTest {
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

            val summary = viewModel.state.value.plansSummary
            if (mode in proteinLed) {
                assertTrue("$mode figure", summary.contains("160 g protein"))
            } else {
                assertTrue("$mode figure", summary.contains("2,100 kcal"))
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

    private fun TestScope.observeProfile(viewModel: ProfileViewModel): Job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.state.collect { }
    }.also {
        testScheduler.runCurrent()
    }

    private fun routineSummary(name: String) = RoutineSummary(
        id = "routine-1",
        name = name,
        notes = null,
        exerciseCount = 1,
        targetSetCount = 3,
        isStarter = false,
        programName = name,
    )

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
        override suspend fun logWeight(weightKg: Double, source: String) {
            loggedWeight = weightKg
        }
        override fun observeLatestWeight(): Flow<WeightEntry?> = flowOf(latestWeight)
        override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> = flowOf(weightSeries)
        override suspend fun logMeasurement(type: String, value: Double, unit: String) = Unit
        override suspend fun deleteEntry(id: String) {
            deletedId = id
        }
        override suspend fun updateEntryValue(id: String, value: Double) {
            updatedId = id
            updatedValue = value
        }
        override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> = flowOf(measurements)
        override fun observeSettings(): Flow<AppSettings> = flowOf(DEFAULT_APP_SETTINGS)
        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeHealthRepo : HealthRepository {
        override suspend fun status(): HealthConnectStatus = HealthConnectStatus(HealthConnectAvailability.Available, setOf("steps"))
        override suspend fun requestablePermissions(): Set<String> = setOf("steps")
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
        override suspend fun importDailySummary(date: LocalDate): com.musfit.data.repository.HealthConnectImportResult = com.musfit.data.repository.HealthConnectImportResult.Empty(ImportedDailyHealthSummary())
        override suspend fun exportLatestWorkout(): String? = null
    }

    /** Pins the `availability != Available` half of the nudge predicate. */
    private class FakeHealthRepoNotInstalled : HealthRepository {
        override suspend fun status(): HealthConnectStatus = HealthConnectStatus(HealthConnectAvailability.NotInstalled, emptySet())
        override suspend fun requestablePermissions(): Set<String> = emptySet()
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
        override suspend fun importDailySummary(date: LocalDate): com.musfit.data.repository.HealthConnectImportResult = com.musfit.data.repository.HealthConnectImportResult.Empty(ImportedDailyHealthSummary())
        override suspend fun exportLatestWorkout(): String? = null
    }

    /** [FakeHealthRepo] whose granted-permission set can change between [status] calls. */
    private class FakeHealthRepoMutableStatus : HealthRepository {
        var granted: Set<String> = emptySet()
        override suspend fun status(): HealthConnectStatus = HealthConnectStatus(HealthConnectAvailability.Available, granted)
        override suspend fun requestablePermissions(): Set<String> = setOf("steps")
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(null)
        override suspend fun importDailySummary(date: LocalDate): com.musfit.data.repository.HealthConnectImportResult = com.musfit.data.repository.HealthConnectImportResult.Empty(ImportedDailyHealthSummary())
        override suspend fun exportLatestWorkout(): String? = null
    }

    private class FakeTrainingRepo : TrainingRepository {
        val routines = MutableStateFlow<List<RoutineSummary>>(emptyList())
        val history = MutableStateFlow<List<WorkoutHistorySummary>>(emptyList())
        var activeRoutineSummaryCollectors = 0
        var maxRoutineSummaryCollectors = 0
        var routineSummarySubscriptionStarts = 0

        override fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = routines
            .onStart {
                activeRoutineSummaryCollectors += 1
                routineSummarySubscriptionStarts += 1
                maxRoutineSummaryCollectors = maxOf(
                    maxRoutineSummaryCollectors,
                    activeRoutineSummaryCollectors,
                )
            }
            .onCompletion { activeRoutineSummaryCollectors -= 1 }

        override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = history

        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet = LoggedWorkoutSet("set-1", exerciseName, reps, weightKg, true)

        override suspend fun setCompletion(setId: String, completed: Boolean) = Unit

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> = MutableStateFlow(TrainingSummary())

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

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> = MutableStateFlow(NutritionTotals(0.0, 0.0, 0.0, 0.0))

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> = MutableStateFlow(FoodDiary(totals = NutritionTotals(0.0, 0.0, 0.0, 0.0), meals = emptyList()))

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override fun observeSameAsYesterday(mealType: String, date: LocalDate): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override suspend fun logSavedFood(input: SavedFoodLogInput): String = ""

        override suspend fun quickLog(input: QuickCalorieLogInput): String = ""

        override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) = Unit

        override suspend fun deleteDiaryEntry(mealItemId: String) = Unit

        override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String = ""

        override suspend fun deleteSavedFood(foodId: String) = Unit
    }
}
