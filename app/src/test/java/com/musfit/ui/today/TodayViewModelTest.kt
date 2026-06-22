package com.musfit.ui.today

import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.RoutineDetail
import com.musfit.data.repository.RoutineInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
import com.musfit.data.repository.WorkoutHistoryDetail
import com.musfit.data.repository.WorkoutHistorySummary
import com.musfit.data.repository.WorkoutForExport
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun state_aggregatesFoodTrainingAndHealthDataForToday() = runTest {
        val date = LocalDate.now()
        val foodRepository = FakeFoodRepository()
        val trainingRepository = FakeTrainingRepository()
        val healthRepository = FakeHealthRepository(date)

        val viewModel = TodayViewModel(
            foodRepository = foodRepository,
            trainingRepository = trainingRepository,
            healthRepository = healthRepository,
            dateProvider = { date },
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals(600.0, state.caloriesKcal, 0.01)
        assertEquals(45.0, state.proteinGrams, 0.01)
        assertEquals(70.0, state.carbsGrams, 0.01)
        assertEquals(18.0, state.fatGrams, 0.01)
        assertEquals("2 sets - 1250 kg volume", state.trainingSummary)
        assertEquals(8200L, state.steps)
        assertEquals(420.0, state.activeCaloriesKcal ?: 0.0, 0.01)
        assertEquals(82.4, state.bodyWeightKg ?: 0.0, 0.01)
    }

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
            dateProvider = { targetDate },
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(targetDate), foodRepository.observedDates)
        assertEquals(listOf(targetDate), trainingRepository.observedDates)
        assertEquals(listOf(targetDate), healthRepository.observedDates)
    }

    private class FakeFoodRepository : FoodRepository {
        val observedDates = mutableListOf<LocalDate>()

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

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> =
            MutableStateFlow(FoodDiary(totals = NutritionTotals(0.0, 0.0, 0.0, 0.0), meals = emptyList()))

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> =
            MutableStateFlow(emptyList())

        override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> =
            MutableStateFlow(emptyList())

        override fun observeSameAsYesterday(mealType: String, date: java.time.LocalDate): Flow<List<SavedFoodItem>> =
            MutableStateFlow(emptyList())

        override suspend fun logSavedFood(input: SavedFoodLogInput): String = "meal-item-1"

        override suspend fun quickLog(input: QuickCalorieLogInput): String = "meal-item-1"

        override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) = Unit

        override suspend fun deleteDiaryEntry(mealItemId: String) = Unit

        override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String =
            input.foodId ?: "food-1"

        override suspend fun deleteSavedFood(foodId: String) = Unit
    }

    private class FakeTrainingRepository : TrainingRepository {
        val observedDates = mutableListOf<LocalDate>()

        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet = LoggedWorkoutSet("set-1", exerciseName, reps, weightKg, true)

        override suspend fun setCompletion(setId: String, completed: Boolean) = Unit

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

        override suspend fun updateRoutine(routineId: String, input: RoutineInput) = Unit

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

        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.Available, emptySet())

        override suspend fun requestablePermissions(): Set<String> = emptySet()

        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> {
            observedDates += date
            return MutableStateFlow(
                DailyHealthSummaryEntity(
                    dateEpochDay = this.date.toEpochDay(),
                    steps = 8200L,
                    activeCaloriesKcal = 420.0,
                    latestWeightKg = 82.4,
                    restingHeartRateBpm = 58,
                    updatedAtEpochMillis = 1L,
                ),
            )
        }

        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(8200L, 420.0, 82.4, 58)

        override suspend fun exportLatestWorkout(): String? = null
    }
}
