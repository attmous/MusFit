package com.musfit.ui.today

import com.musfit.data.local.entity.BodyMetricEntity
import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.AppSettings
import com.musfit.data.repository.BodyMeasurement
import com.musfit.data.repository.CoachMessage
import com.musfit.data.repository.CoachRepository
import com.musfit.data.repository.DEFAULT_APP_SETTINGS
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.GoalsRepository
import com.musfit.data.repository.HealthConnectRefreshResult
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.ProductLookupResult
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.UserGoals
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
import com.musfit.data.repository.WorkoutForExport
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.domain.coach.CoachAction
import com.musfit.domain.coach.CoachMessageCandidate
import com.musfit.domain.coach.CoachMessageCategory
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import com.musfit.domain.profile.ActivityLevel
import com.musfit.domain.profile.GoalType
import com.musfit.domain.profile.RecommendedTargets
import com.musfit.domain.today.MetricValue
import com.musfit.domain.today.TodayMetric
import com.musfit.testing.MainDispatcherRule
import com.musfit.ui.AppDestination
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val dispatcher get() = mainDispatcherRule.dispatcher

    @Test
    fun state_usesInjectedDateProviderBeforeStartingRepositoryFlows() = runTest {
        val targetDate = LocalDate.now().plusDays(3)
        val foodRepository = FakeFoodRepository()
        val trainingRepository = FakeTrainingRepository()
        val healthRepository = FakeHealthRepository(targetDate)

        TodayViewModel(
            foodRepository = foodRepository,
            trainingRepository = trainingRepository,
            healthRepository = healthRepository,
            goalsRepository = FakeGoalsRepository(),
            coachRepository = FakeCoachRepository(),
            profileRepository = FakeProfileRepository(),
            dateProvider = { targetDate },
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(foodRepository.observedDates.isNotEmpty() && foodRepository.observedDates.all { it == targetDate })
        assertTrue(healthRepository.observedDates.isNotEmpty() && healthRepository.observedDates.all { it == targetDate })
    }

    @Test
    fun onScreenResumed_refreshesRecentHealthConnectDataForActiveDate() = runTest {
        val date = LocalDate.of(2026, 7, 2)
        val healthRepository = FakeHealthRepository(date)
        val viewModel =
            todayViewModel(
                coachRepository = FakeCoachRepository(),
                date = date,
                healthRepository = healthRepository,
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(date), healthRepository.refreshDates)
    }

    @Test
    fun refreshTodayData_setsRefreshingWhileRecentHealthConnectDataLoads() = runTest {
        val date = LocalDate.of(2026, 7, 3)
        val healthRepository =
            FakeHealthRepository(date).apply {
                refreshGate = CompletableDeferred()
            }
        val viewModel =
            todayViewModel(
                coachRepository = FakeCoachRepository(),
                date = date,
                healthRepository = healthRepository,
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshTodayData()
        runCurrent()

        assertTrue(viewModel.state.value.isRefreshing)
        assertEquals(listOf(date), healthRepository.refreshDates)

        healthRepository.refreshGate?.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun todayRefreshIndicatorState_tracksPullProgressWhilePulling() {
        val indicator = todayRefreshIndicatorUiState(isRefreshing = false, pullDistanceFraction = 0.42f)

        assertTrue(indicator.isVisible)
        assertEquals(0.42f, indicator.progress ?: -1f, 0.001f)
    }

    @Test
    fun todayRefreshIndicatorState_showsIndeterminateProgressWhileRefreshing() {
        val indicator = todayRefreshIndicatorUiState(isRefreshing = true, pullDistanceFraction = 0.42f)

        assertTrue(indicator.isVisible)
        assertNull(indicator.progress)
    }

    @Test
    fun todayRefreshIndicatorState_hiddenWhenIdle() {
        val indicator = todayRefreshIndicatorUiState(isRefreshing = false, pullDistanceFraction = 0f)

        assertFalse(indicator.isVisible)
        assertNull(indicator.progress)
    }

    @Test
    fun feed_groupsByDayWithTodayAndYesterdayLabelsNewestFirst() = runTest {
        val date = LocalDate.now()
        val coachRepository = FakeCoachRepository()
        val viewModel = todayViewModel(coachRepository = coachRepository, date = date)
        coachRepository.feed.value =
            listOf(
                message(3, date, firstSeenAt = 300L),
                message(2, date, firstSeenAt = 100L),
                message(1, date.minusDays(1), firstSeenAt = 50L),
            )
        dispatcher.scheduler.advanceUntilIdle()

        val groups = viewModel.state.value.feed
        assertEquals(listOf("Today", "Yesterday"), groups.map { it.label })
        assertEquals(listOf(3L, 2L), groups[0].messages.map { it.id })
        assertEquals(listOf(1L), groups[1].messages.map { it.id })
    }

    @Test
    fun sync_candidatesIncludeNewFieldRules_waterLowWiredEndToEnd() = runTest {
        // Pins that the extended CoachInput fields are actually wired (they are defaulted,
        // so a wiring omission fails no compile — only this test).
        val coachRepository = FakeCoachRepository()
        val viewModel = todayViewModel(coachRepository = coachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(coachRepository.lastCandidates.any { it.ruleKey == "water_low" })
    }

    @Test
    fun sync_regeneratesOnDataChangeWhileResumed() = runTest {
        val date = LocalDate.now()
        val coachRepository = FakeCoachRepository()
        val foodRepository = FakeFoodRepository()
        val viewModel =
            todayViewModel(
                coachRepository = coachRepository,
                date = date,
                foodRepository = foodRepository,
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(coachRepository.lastCandidates.any { it.ruleKey == "water_low" })

        foodRepository.waterSummary.value =
            FoodWaterSummary(date, consumedMilliliters = 1900.0, goalMilliliters = 2000.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(coachRepository.lastCandidates.none { it.ruleKey == "water_low" })
    }

    @Test
    fun buildFeedGroups_labelsOlderDaysWithDate() {
        val today = LocalDate.now()
        val groups =
            buildFeedGroups(
                listOf(
                    message(1, today, firstSeenAt = 10L),
                    message(2, today.minusDays(1), firstSeenAt = 20L),
                    message(3, today.minusDays(3), firstSeenAt = 30L),
                ),
                today,
            )

        assertEquals(listOf(1L, 2L, 3L), groups.map { it.messages.single().id })
        assertEquals("Today", groups[0].label)
        assertEquals("Yesterday", groups[1].label)
        assertTrue(groups[2].label != "Today" && groups[2].label != "Yesterday")
    }

    @Test
    fun buildFeedGroups_usesCurrentLocaleAfterProcessLocaleChanges() {
        val originalLocale = Locale.getDefault()
        val today = LocalDate.of(2026, 7, 13)
        val message = message(1, today.minusDays(3), firstSeenAt = 30L)
        try {
            Locale.setDefault(Locale.ENGLISH)
            val english = buildFeedGroups(listOf(message), today).single().label
            Locale.setDefault(Locale.GERMAN)
            val german = buildFeedGroups(listOf(message), today).single().label

            assertEquals("Friday, 10 July", english)
            assertEquals("Freitag, 10 Juli", german)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun onScreenResumed_syncsUsingCurrentDate_midnightRollover() = runTest {
        var date = LocalDate.of(2026, 7, 1)
        val coachRepository = FakeCoachRepository()
        val viewModel = todayViewModel(coachRepository = coachRepository, dateProvider = { date })
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()
        date = LocalDate.of(2026, 7, 2) // midnight passes while the process lives
        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(coachRepository.syncedDays.contains(LocalDate.of(2026, 7, 1)))
        assertTrue(coachRepository.syncedDays.contains(LocalDate.of(2026, 7, 2)))
        assertTrue(coachRepository.lastCandidates.isNotEmpty())
    }

    @Test
    fun onScreenPaused_marksAllRead() = runTest {
        val coachRepository = FakeCoachRepository()
        val viewModel = todayViewModel(coachRepository = coachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenPaused()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, coachRepository.markAllReadCount)
    }

    @Test
    fun dismissMessage_delegatesToRepository() = runTest {
        val coachRepository = FakeCoachRepository()
        val viewModel = todayViewModel(coachRepository = coachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissMessage(42L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(42L), coachRepository.dismissedIds)
    }

    @Test
    fun vitals_deriveTilesFromPinsAndLiveData() = runTest {
        val coachRepository = FakeCoachRepository()
        coachRepository.pins.value = listOf(TodayMetric.Calories, TodayMetric.Steps, TodayMetric.Protein)
        val viewModel = todayViewModel(coachRepository = coachRepository)
        dispatcher.scheduler.advanceUntilIdle()

        val vitals = viewModel.state.value.vitals
        assertEquals(
            listOf(TodayMetric.Calories, TodayMetric.Steps, TodayMetric.Protein),
            vitals.map { it.metric },
        )
        // FakeFoodRepository: 600 eaten of 2000 goal → the tile shows EATEN with percent.
        val calories = vitals[0].value as MetricValue.WithGoal
        assertEquals("600", calories.figure)
        assertEquals("of 2,000 kcal · 30%", calories.caption)
        assertEquals(0.3f, calories.progress, 0.001f)
    }

    @Test
    fun readiness_isDerivedFromCurrentAndRecentHealthSummaries() = runTest {
        val date = LocalDate.of(2026, 7, 7)
        val healthRepository =
            FakeHealthRepository(date).apply {
                dailySummaries.value =
                    (7L downTo 1L).map { daysAgo ->
                        dailySummary(
                            date = date.minusDays(daysAgo),
                            sleepMinutes = 420L,
                            restingHeartRateBpm = 58L,
                            hrvRmssdMillis = 55.0,
                        )
                    } +
                    dailySummary(
                        date = date,
                        sleepMinutes = 480L,
                        restingHeartRateBpm = 53L,
                        hrvRmssdMillis = 68.0,
                    )
            }

        val viewModel =
            todayViewModel(
                coachRepository = FakeCoachRepository(),
                date = date,
                healthRepository = healthRepository,
            )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            86,
            viewModel.state.value.readiness
                ?.score,
        )
        assertEquals(
            "Ready 86",
            viewModel.state.value.readiness
                ?.label,
        )
    }

    @Test
    fun coachInput_targetWeightComesFromProfileStore() = runTest {
        val date = LocalDate.now()
        val coachRepository = FakeCoachRepository()
        val healthRepository = FakeHealthRepository(date)
        // Two flat weights spanning both of the coach's Monday-anchored 7-day windows
        // (one in [weekStart, weekStart+7d), one in [weekStart−7d, weekStart)) →
        // weightDelta ≈ 0 → the "steady" message interpolates the target:
        // "…keep nudging toward 75 kg." Anchoring to weekStart (not "now") keeps the
        // test deterministic on every weekday.
        val weekStartEpochDay = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        healthRepository.weightSeries.value =
            listOf(
                BodyMetricEntity("test-account", "w1", "weight", 82.0, "kg", (weekStartEpochDay - 3L) * 86_400_000L, "manual", null),
                BodyMetricEntity("test-account", "w2", "weight", 82.0, "kg", weekStartEpochDay * 86_400_000L, "manual", null),
            )
        val viewModel =
            todayViewModel(
                coachRepository = coachRepository,
                date = date,
                healthRepository = healthRepository,
                // Seed a DIFFERENT nonzero legacy user_goals target: a surviving fallback
                // read of user_goals.targetWeightKg would surface 68, not the profile's 75.
                goalsRepository = FakeGoalsRepository(UserGoals(targetWeightKg = 68.0)),
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()

        val weightMessage = coachRepository.lastCandidates.first { it.ruleKey == "weight_trend" }
        assertTrue(weightMessage.body, weightMessage.body.contains("75 kg"))
        assertFalse(weightMessage.body, weightMessage.body.contains("68"))
    }

    @Test
    fun dashboardEditor_prefillsPinsAndGoals() = runTest {
        val coachRepository = FakeCoachRepository()
        val viewModel =
            todayViewModel(
                coachRepository = coachRepository,
                goalsRepository = FakeGoalsRepository(UserGoals(stepGoal = 12_345)),
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDashboardEditor()

        val state = viewModel.state.value
        assertEquals(true, state.isDashboardEditorVisible)
        assertEquals(TodayMetric.DEFAULT_PINS, state.editPins)
        assertEquals("12345", state.stepGoalInput)
    }

    @Test
    fun dashboardEditor_togglePinNeverRemovesTheLastOne() = runTest {
        val viewModel = todayViewModel(coachRepository = FakeCoachRepository())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openDashboardEditor()

        viewModel.togglePin(TodayMetric.Steps)
        viewModel.togglePin(TodayMetric.Protein)
        viewModel.togglePin(TodayMetric.Water)
        viewModel.togglePin(TodayMetric.Calories) // last pin — must be ignored

        assertEquals(listOf(TodayMetric.Calories), viewModel.state.value.editPins)
    }

    @Test
    fun dashboardEditor_moveAndSavePersistPinsAndGoals() = runTest {
        val coachRepository = FakeCoachRepository()
        val viewModel = todayViewModel(coachRepository = coachRepository)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openDashboardEditor()

        viewModel.togglePin(TodayMetric.Weight) // append
        viewModel.movePin(TodayMetric.Weight, up = true) // → before Water
        viewModel.saveDashboard()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(TodayMetric.Calories, TodayMetric.Steps, TodayMetric.Protein, TodayMetric.Weight, TodayMetric.Water),
            coachRepository.savedPins,
        )
        assertEquals(false, viewModel.state.value.isDashboardEditorVisible)
    }

    @Test
    fun movePin_boundsAreNoOps() = runTest {
        val viewModel = todayViewModel(coachRepository = FakeCoachRepository())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openDashboardEditor() // [Calories, Steps, Protein, Water]

        viewModel.movePin(TodayMetric.Calories, up = true) // first up — no-op
        viewModel.movePin(TodayMetric.Water, up = false) // last down — no-op

        assertEquals(TodayMetric.DEFAULT_PINS, viewModel.state.value.editPins)
    }

    @Test
    fun saveDashboard_neverTouchesStoredTargetWeight() = runTest {
        val goalsRepository = FakeGoalsRepositoryRecording()
        val viewModel =
            todayViewModel(
                coachRepository = FakeCoachRepository(),
                goalsRepository = goalsRepository,
            )
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openDashboardEditor()

        viewModel.saveDashboard()
        dispatcher.scheduler.advanceUntilIdle()

        // The column stays but Profile owns the value — Today passes it through verbatim.
        assertEquals(FakeGoalsRepositoryRecording.STORED_TARGET_WEIGHT, goalsRepository.saved!!.targetWeightKg, 0.001)
    }

    @Test
    fun metricDestination_mapsEveryMetricToItsHomeTab() {
        assertEquals(AppDestination.Food, metricDestination(TodayMetric.Calories))
        assertEquals(AppDestination.Food, metricDestination(TodayMetric.Water))
        assertEquals(AppDestination.Training, metricDestination(TodayMetric.Sessions))
        assertEquals(AppDestination.Training, metricDestination(TodayMetric.Exercise))
        assertEquals(AppDestination.Profile, metricDestination(TodayMetric.Steps))
        assertEquals(AppDestination.Profile, metricDestination(TodayMetric.Sleep))
        assertEquals(AppDestination.Profile, metricDestination(TodayMetric.Weight))
        assertEquals(AppDestination.Profile, metricDestination(TodayMetric.RestingHeartRate))
    }

    @Test
    fun coachActionDestination_mapsAllActionsIncludingDeletedRoutineFallback() {
        assertEquals(AppDestination.Food, coachActionDestination(CoachAction.OpenFood))
        assertEquals(AppDestination.Training, coachActionDestination(CoachAction.OpenTraining))
        assertEquals(AppDestination.Profile, coachActionDestination(CoachAction.OpenHealth))
        // Deleted or live, StartRoutine lands on the Training tab in v1 (spec non-goal: no sub-route anchors).
        assertEquals(AppDestination.Training, coachActionDestination(CoachAction.StartRoutine("deleted-routine")))
    }

    private fun todayViewModel(
        coachRepository: CoachRepository,
        date: LocalDate = LocalDate.now(),
        dateProvider: (() -> LocalDate)? = null,
        foodRepository: FakeFoodRepository = FakeFoodRepository(),
        profileRepository: ProfileRepository = FakeProfileRepository(),
        goalsRepository: GoalsRepository = FakeGoalsRepository(),
        healthRepository: FakeHealthRepository? = null,
    ) = TodayViewModel(
        foodRepository = foodRepository,
        trainingRepository = FakeTrainingRepository(),
        healthRepository = healthRepository ?: FakeHealthRepository(date),
        goalsRepository = goalsRepository,
        coachRepository = coachRepository,
        profileRepository = profileRepository,
        dateProvider = dateProvider ?: { date },
        // Deterministic afternoon clock: the engine's time-gated rules (water_low is
        // NOT-morning, plan_morning is morning-only) must not flake with wall time.
        clock = {
            date
                .atTime(14, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        },
    )

    private fun message(
        id: Long,
        day: LocalDate,
        firstSeenAt: Long,
        isRead: Boolean = false,
        ruleKey: String = "rule-$id",
    ) = CoachMessage(
        id = id,
        day = day,
        ruleKey = ruleKey,
        category = CoachMessageCategory.Nutrition,
        title = "t$id",
        body = "b$id",
        action = null,
        firstSeenAtEpochMillis = firstSeenAt,
        isRead = isRead,
        source = "rules",
    )

    private class FakeCoachRepository : CoachRepository {
        val feed = MutableStateFlow<List<CoachMessage>>(emptyList())
        val syncedDays = mutableListOf<LocalDate>()
        var lastCandidates: List<CoachMessageCandidate> = emptyList()
        val dismissedIds = mutableListOf<Long>()
        var markAllReadCount = 0
        val pins = MutableStateFlow(TodayMetric.DEFAULT_PINS)
        var savedPins: List<TodayMetric>? = null

        override fun observeFeed(): Flow<List<CoachMessage>> = feed

        override suspend fun syncToday(
            day: LocalDate,
            candidates: List<CoachMessageCandidate>,
        ) {
            syncedDays += day
            lastCandidates = candidates
        }

        override suspend fun dismiss(id: Long) {
            dismissedIds += id
        }

        override suspend fun markAllRead() {
            markAllReadCount++
        }

        override fun observeDashboardPins(): Flow<List<TodayMetric>> = pins

        override suspend fun saveDashboardPins(ordered: List<TodayMetric>) {
            savedPins = ordered
        }
    }

    private class FakeFoodRepository : FoodRepository {
        val observedDates = mutableListOf<LocalDate>()
        val waterSummary =
            MutableStateFlow(
                FoodWaterSummary(LocalDate.now(), consumedMilliliters = 100.0, goalMilliliters = 2000.0),
            )

        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String = "food-1"

        override suspend fun logFood(input: FoodLogInput): String = "meal-item-1"

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> {
            observedDates += date
            return MutableStateFlow(NutritionTotals(600.0, 45.0, 70.0, 18.0))
        }

        override fun observeFoodGoal(): Flow<FoodGoal> = MutableStateFlow(
            FoodGoal(
                dailyCaloriesKcal = 2000.0,
                proteinGrams = 150.0,
                carbsGrams = 200.0,
                fatGrams = 60.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
            ),
        )

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> = MutableStateFlow(FoodDiary(totals = NutritionTotals(0.0, 0.0, 0.0, 0.0), meals = emptyList()))

        override fun observeWaterSummary(date: LocalDate): Flow<FoodWaterSummary> = waterSummary

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override fun observeSameAsYesterday(
            mealType: String,
            date: java.time.LocalDate,
        ): Flow<List<SavedFoodItem>> = MutableStateFlow(emptyList())

        override suspend fun logSavedFood(input: SavedFoodLogInput): String = "meal-item-1"

        override suspend fun quickLog(input: QuickCalorieLogInput): String = "meal-item-1"

        override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) = Unit

        override suspend fun deleteDiaryEntry(mealItemId: String) = Unit

        override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String = input.foodId ?: "food-1"

        override suspend fun deleteSavedFood(foodId: String) = Unit
    }

    private class FakeGoalsRepository(
        private val userGoals: UserGoals = UserGoals(),
    ) : GoalsRepository {
        override fun observeUserGoals(): Flow<UserGoals> = MutableStateFlow(userGoals)

        override suspend fun updateUserGoals(goals: UserGoals) = Unit
    }

    private class FakeGoalsRepositoryRecording : GoalsRepository {
        var saved: UserGoals? = null

        override fun observeUserGoals(): Flow<UserGoals> = MutableStateFlow(UserGoals(targetWeightKg = STORED_TARGET_WEIGHT))

        override suspend fun updateUserGoals(goals: UserGoals) {
            saved = goals
        }

        companion object {
            const val STORED_TARGET_WEIGHT = 82.0
        }
    }

    private class FakeProfileRepository : ProfileRepository {
        val measurements = MutableStateFlow<Map<String, List<BodyMeasurement>>>(emptyMap())
        val profile =
            MutableStateFlow(
                UserProfile(
                    sex = null,
                    birthDateEpochDay = null,
                    heightCm = null,
                    activityLevel = ActivityLevel.Moderate,
                    goalType = GoalType.Maintain,
                    goalPaceKgPerWeek = 0.0,
                    goalWeightKg = 75.0,
                ),
            )

        override fun observeProfile(): Flow<UserProfile> = profile

        override suspend fun saveProfile(profile: UserProfile) = Unit

        override fun observeRecommendedTargets(): Flow<RecommendedTargets?> = MutableStateFlow(null)

        override suspend fun logWeight(
            weightKg: Double,
            source: String,
        ) = Unit

        override fun observeLatestWeight(): Flow<WeightEntry?> = MutableStateFlow(null)

        override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> = MutableStateFlow(emptyList())

        override suspend fun logMeasurement(
            type: String,
            value: Double,
            unit: String,
        ) = Unit

        override suspend fun deleteEntry(id: String) = Unit

        override suspend fun updateEntryValue(
            id: String,
            value: Double,
        ) = Unit

        override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> = measurements

        override fun observeSettings(): Flow<AppSettings> = MutableStateFlow(DEFAULT_APP_SETTINGS)

        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeTrainingRepository : TrainingRepository {
        val observedDates = mutableListOf<LocalDate>()

        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet = LoggedWorkoutSet("set-1", exerciseName, reps, weightKg, true)

        override suspend fun setCompletion(
            setId: String,
            completed: Boolean,
        ) = Unit

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> {
            observedDates += date
            return MutableStateFlow(
                TrainingSummary(
                    completedSetCount = 2,
                    totalVolumeKg = 1250.0,
                    bestEstimatedOneRepMaxKg = 130.0,
                ),
            )
        }

        override suspend fun createRoutine(input: RoutineInput): String = "routine-1"

        override suspend fun updateRoutine(
            routineId: String,
            input: RoutineInput,
        ) = Unit

        override suspend fun duplicateRoutine(routineId: String): String = "$routineId-copy"

        override suspend fun deleteRoutine(routineId: String) = Unit

        override suspend fun getRoutineDetail(routineId: String): RoutineDetail? = null

        override suspend fun startBlankWorkout(): String = "session-blank"

        override suspend fun startWorkoutFromRoutine(routineId: String): String = "session-$routineId"

        override fun observeWorkoutHistory(): Flow<List<WorkoutHistorySummary>> = MutableStateFlow(emptyList())

        override suspend fun getWorkoutHistoryDetail(sessionId: String): WorkoutHistoryDetail? = null

        override suspend fun getLatestWorkoutForExport(): WorkoutForExport? = null

        override suspend fun markWorkoutExported(
            sessionId: String,
            recordId: String,
            exportedAtEpochMillis: Long,
        ) = Unit
    }

    private class FakeHealthRepository(
        private val date: LocalDate,
    ) : HealthRepository {
        val observedDates = mutableListOf<LocalDate>()
        val refreshDates = mutableListOf<LocalDate>()
        val weightSeries = MutableStateFlow<List<BodyMetricEntity>>(emptyList())
        var refreshGate: CompletableDeferred<Unit>? = null

        override fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>> = weightSeries

        override suspend fun status(): HealthConnectStatus = HealthConnectStatus(HealthConnectAvailability.Available, emptySet())

        override suspend fun requestablePermissions(): Set<String> = emptySet()

        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> {
            observedDates += date
            return MutableStateFlow(
                dailySummaries.value.firstOrNull { it.dateEpochDay == date.toEpochDay() }
                    ?: dailySummary(date = this.date),
            )
        }

        override suspend fun importDailySummary(date: LocalDate): com.musfit.data.repository.HealthConnectImportResult = com.musfit.data.repository.HealthConnectImportResult.Complete(
            ImportedDailyHealthSummary(
                steps = 8200L,
                activeCaloriesKcal = 420.0,
                totalCaloriesKcal = 2_300.0,
                distanceMeters = 5_000.0,
                sleepMinutes = 450L,
                exerciseMinutes = 40L,
                exerciseSessionCount = 1,
                latestWeightKg = 82.4,
                latestBodyFatPercent = null,
                restingHeartRateBpm = 58,
                hrvRmssdMillis = 55.0,
            ),
        )

        override suspend fun refreshRecentData(
            endDate: LocalDate,
            days: Int,
        ): HealthConnectRefreshResult {
            refreshDates += endDate
            refreshGate?.await()
            return HealthConnectRefreshResult(importedDayCount = days, bodyMetricCount = 0)
        }

        override suspend fun exportLatestWorkout(): String? = null

        val dailySummaries = MutableStateFlow<List<DailyHealthSummaryEntity>>(emptyList())

        override fun observeDailySummaries(
            startDate: LocalDate,
            endDate: LocalDate,
        ): Flow<List<DailyHealthSummaryEntity>> = MutableStateFlow(
            dailySummaries.value.filter { summary ->
                summary.dateEpochDay in startDate.toEpochDay()..endDate.toEpochDay()
            },
        )

        fun dailySummary(
            date: LocalDate,
            sleepMinutes: Long = 450L,
            restingHeartRateBpm: Long = 58L,
            hrvRmssdMillis: Double = 55.0,
        ) = DailyHealthSummaryEntity(
            accountId = "test-account",
            dateEpochDay = date.toEpochDay(),
            steps = 8200L,
            activeCaloriesKcal = 420.0,
            totalCaloriesKcal = 2_300.0,
            distanceMeters = 5_000.0,
            sleepMinutes = sleepMinutes,
            exerciseMinutes = 40L,
            exerciseSessionCount = 1,
            latestWeightKg = 82.4,
            latestBodyFatPercent = null,
            restingHeartRateBpm = restingHeartRateBpm,
            hrvRmssdMillis = hrvRmssdMillis,
            updatedAtEpochMillis = 1L,
        )
    }
}
