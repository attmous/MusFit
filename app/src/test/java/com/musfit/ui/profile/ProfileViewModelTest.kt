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
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.ProfileRepository
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.data.repository.UserProfile
import com.musfit.data.repository.WeightEntry
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

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun incompleteProfile_hidesRecommendation() = runTest {
        val viewModel = ProfileViewModel(FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo())
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
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isProfileComplete)
        assertEquals(2759.0, viewModel.state.value.recommendedTargets!!.caloriesKcal, 0.001)
        assertEquals(24.7, viewModel.state.value.bmi!!, 0.05)
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
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), food)
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
    fun vitals_mapFromHealthConnectDailySummary() = runTest {
        val health = FakeHealthRepo(
            summary = DailyHealthSummaryEntity(
                dateEpochDay = LocalDate.now().toEpochDay(),
                steps = 7420L, activeCaloriesKcal = 410.0, latestWeightKg = 84.2,
                restingHeartRateBpm = 58L, updatedAtEpochMillis = 1L,
            ),
        )
        val viewModel = ProfileViewModel(FakeProfileRepository(), health, FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(7420L, viewModel.state.value.vitals!!.steps)
        assertEquals(58L, viewModel.state.value.vitals!!.restingHeartRateBpm)
    }

    @Test
    fun logWeight_callsRepository() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logWeight(83.6)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(83.6, repo.loggedWeight!!, 0.001)
    }

    @Test
    fun editEntry_callsRepositoryWithIdAndValue() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.editEntry("abc", 81.3)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc", repo.updatedId)
        assertEquals(81.3, repo.updatedValue!!, 0.001)
    }

    @Test
    fun deleteEntry_callsRepositoryWithId() = runTest {
        val repo = FakeProfileRepository()
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteEntry("xyz")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("xyz", repo.deletedId)
    }

    @Test
    fun state_exposesWeightEntriesForSheet() = runTest {
        val repo = FakeProfileRepository(latestWeight = WeightEntry("w9", 1_000L, 84.0, "manual"))
        val viewModel = ProfileViewModel(repo, FakeHealthRepo(), FakeFoodGoalRepo())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("w9", viewModel.state.value.weightEntries.first().id)
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
        override fun observeProfile(): Flow<UserProfile> = flowOf(profile)
        override suspend fun saveProfile(profile: UserProfile) = Unit
        override fun observeRecommendedTargets(): Flow<RecommendedTargets?> = flowOf(targets)
        override suspend fun logWeight(weightKg: Double, source: String) { loggedWeight = weightKg }
        override fun observeLatestWeight(): Flow<WeightEntry?> = flowOf(latestWeight)
        override fun observeWeightSeries(sinceEpochMillis: Long): Flow<List<WeightEntry>> =
            flowOf(listOfNotNull(latestWeight))
        override suspend fun logMeasurement(type: String, value: Double, unit: String) = Unit
        override suspend fun deleteEntry(id: String) { deletedId = id }
        override suspend fun updateEntryValue(id: String, value: Double) { updatedId = id; updatedValue = value }
        override fun observeRecentMeasurements(sinceEpochMillis: Long): Flow<Map<String, List<BodyMeasurement>>> =
            flowOf(measurements)
        override fun observeSettings(): Flow<AppSettings> = flowOf(DEFAULT_APP_SETTINGS)
        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeHealthRepo(
        private val summary: DailyHealthSummaryEntity? = null,
    ) : HealthRepository {
        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.Available, setOf("steps"))
        override suspend fun requestablePermissions(): Set<String> = setOf("steps")
        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> = flowOf(summary)
        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(null, null, null, null)
        override suspend fun exportLatestWorkout(): String? = null
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
