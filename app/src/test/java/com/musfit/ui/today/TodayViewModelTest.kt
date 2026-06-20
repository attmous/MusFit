package com.musfit.ui.today

import com.musfit.data.local.entity.DailyHealthSummaryEntity
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.HealthRepository
import com.musfit.data.repository.LoggedWorkoutSet
import com.musfit.data.repository.TrainingRepository
import com.musfit.data.repository.TrainingSummary
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

    private class FakeFoodRepository : FoodRepository {
        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String = "food-1"

        override suspend fun logFood(input: FoodLogInput): String = "meal-item-1"

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> =
            MutableStateFlow(NutritionTotals(600.0, 45.0, 70.0, 18.0))
    }

    private class FakeTrainingRepository : TrainingRepository {
        override suspend fun addCompletedSet(
            exerciseName: String,
            reps: Int,
            weightKg: Double,
        ): LoggedWorkoutSet = LoggedWorkoutSet("set-1", exerciseName, reps, weightKg, true)

        override suspend fun setCompletion(setId: String, completed: Boolean) = Unit

        override fun observeDailyTrainingSummary(date: LocalDate): Flow<TrainingSummary> =
            MutableStateFlow(
                TrainingSummary(
                    completedSetCount = 2,
                    totalVolumeKg = 1250.0,
                    bestEstimatedOneRepMaxKg = 130.0,
                ),
            )

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
        override suspend fun status(): HealthConnectStatus =
            HealthConnectStatus(HealthConnectAvailability.Available, emptySet())

        override suspend fun requestablePermissions(): Set<String> = emptySet()

        override fun observeDailySummary(date: LocalDate): Flow<DailyHealthSummaryEntity?> =
            MutableStateFlow(
                DailyHealthSummaryEntity(
                    dateEpochDay = this.date.toEpochDay(),
                    steps = 8200L,
                    activeCaloriesKcal = 420.0,
                    latestWeightKg = 82.4,
                    restingHeartRateBpm = 58,
                    updatedAtEpochMillis = 1L,
                ),
            )

        override suspend fun importDailySummary(date: LocalDate): ImportedDailyHealthSummary =
            ImportedDailyHealthSummary(8200L, 420.0, 82.4, 58)

        override suspend fun exportLatestWorkout(): String? = null
    }
}
