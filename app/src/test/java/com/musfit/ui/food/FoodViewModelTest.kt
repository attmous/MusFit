package com.musfit.ui.food

import androidx.lifecycle.SavedStateHandle
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntry
import com.musfit.data.repository.FoodDiaryEntryStatus
import com.musfit.data.repository.FoodDiaryMeal
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodHealthConnectSyncResult
import com.musfit.data.repository.FoodHealthConnectSyncState
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodMealDefinition
import com.musfit.data.repository.FoodMealDefinitionInput
import com.musfit.data.repository.FoodPlanDay
import com.musfit.data.repository.FoodProductProvider
import com.musfit.data.repository.FoodProgressSummary
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.FoodServingInput
import com.musfit.data.repository.FoodServingOption
import com.musfit.data.repository.FoodWaterSummary
import com.musfit.data.repository.FoodWeeklyDaySummary
import com.musfit.data.repository.FoodWeeklySummary
import com.musfit.data.repository.ManualShoppingListItemInput
import com.musfit.data.repository.MealTemplate
import com.musfit.data.repository.MealTemplateItem
import com.musfit.data.repository.MealTemplateItemInput
import com.musfit.data.repository.MealTemplateUpdateInput
import com.musfit.data.repository.NutritionDetails
import com.musfit.data.repository.ProductDataQuality
import com.musfit.data.repository.ProductLookupResult
import com.musfit.data.repository.ProductSearchResult
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.QuickCaloriePreset
import com.musfit.data.repository.QuickCaloriePresetInput
import com.musfit.data.repository.Recipe
import com.musfit.data.repository.RecipeIngredient
import com.musfit.data.repository.RecipeUpsertInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.data.repository.ShoppingListGroup
import com.musfit.data.repository.ShoppingListItem
import com.musfit.data.repository.WaterLogInput
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import com.musfit.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FoodViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val dispatcher get() = mainDispatcherRule.dispatcher

    @Test
    fun unrelatedEditorUpdateDoesNotEmitDiaryOrTrackerSlices() = runTest {
        val viewModel = FoodViewModel(FakeProductProvider(), FakeFoodRepository())
        testScheduler.advanceUntilIdle()
        val diaryBefore = viewModel.diaryState.value
        val trackersBefore = viewModel.trackerState.value
        val routeBefore = viewModel.routeState.value

        viewModel.onFoodDatabaseQueryChanged("oats")
        testScheduler.advanceUntilIdle()

        assertSame(diaryBefore, viewModel.diaryState.value)
        assertSame(trackersBefore, viewModel.trackerState.value)
        assertSame(routeBefore, viewModel.routeState.value)
    }

    @Test
    fun destinationStateFlowsOnlyEmitForTheirOwnDomain() = runTest {
        val viewModel = FoodViewModel(FakeProductProvider(), FakeFoodRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.addDatabaseState.collect { }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.editorPlanningState.collect { }
        }
        testScheduler.advanceUntilIdle()

        val addBeforeDatabaseChange = viewModel.addDatabaseState.value
        val editorBeforeDatabaseChange = viewModel.editorPlanningState.value
        viewModel.onFoodDatabaseQueryChanged("oats")
        testScheduler.advanceUntilIdle()

        assertNotSame(addBeforeDatabaseChange, viewModel.addDatabaseState.value)
        assertSame(editorBeforeDatabaseChange, viewModel.editorPlanningState.value)

        val addBeforePlanningChange = viewModel.addDatabaseState.value
        val editorBeforePlanningChange = viewModel.editorPlanningState.value
        viewModel.onRecipeBrowserMealChanged("dinner")
        testScheduler.advanceUntilIdle()

        assertSame(addBeforePlanningChange, viewModel.addDatabaseState.value)
        assertNotSame(editorBeforePlanningChange, viewModel.editorPlanningState.value)
    }

    @Test
    fun restoration_retainsFoodAddRouteAndBoundedDraftWithoutTransientMessage() = runTest {
        val savedStateHandle = SavedStateHandle()
        val first = FoodViewModel(FakeProductProvider(), FakeFoodRepository(), savedStateHandle)
        first.onScannedBarcode("123abc45")
        dispatcher.scheduler.advanceUntilIdle()

        val restored = FoodViewModel(FakeProductProvider(), FakeFoodRepository(), savedStateHandle)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodSheetMode.AddFood, restored.state.value.sheetMode)
        assertEquals(AddTab.Create, restored.state.value.addTab)
        assertEquals("12345", restored.state.value.barcode)
        assertEquals("Greek Yogurt", restored.state.value.productName)
        assertNull(restored.state.value.message)
        assertFalse(restored.state.value.isLoading)
        assertFalse(restored.state.value.isSaving)
    }

    @Test
    fun restoration_retainsFoodDatabaseQueryAndSelectedDate() = runTest {
        val savedStateHandle = SavedStateHandle()
        val first = FoodViewModel(FakeProductProvider(), FakeFoodRepository(), savedStateHandle)
        first.goToPreviousDay()
        first.openFoodDatabase()
        first.onFoodDatabaseQueryChanged("oats")
        dispatcher.scheduler.advanceUntilIdle()

        val restored = FoodViewModel(FakeProductProvider(), FakeFoodRepository(), savedStateHandle)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodSheetMode.FoodDatabase, restored.state.value.sheetMode)
        assertEquals("oats", restored.state.value.foodDatabaseQuery)
        assertEquals(first.state.value.selectedDate, restored.state.value.selectedDate)
    }

    @Test
    fun restoration_retainsSelectedDateWithoutAnOpenFoodSheet() = runTest {
        val savedStateHandle = SavedStateHandle()
        val first = FoodViewModel(FakeProductProvider(), FakeFoodRepository(), savedStateHandle)
        first.goToPreviousDay()
        dispatcher.scheduler.advanceUntilIdle()

        val restored = FoodViewModel(FakeProductProvider(), FakeFoodRepository(), savedStateHandle)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(first.state.value.selectedDate, restored.state.value.selectedDate)
        assertNull(restored.state.value.sheetMode)
    }

    @Test
    fun onScannedBarcode_routesToCreateTabAndAutofills() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        viewModel.onScannedBarcode("123abc45")
        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.state.value
        assertEquals(AddTab.Create, result.addTab)
        assertTrue(result.isAddPanelVisible)
        assertEquals(FoodSheetMode.AddFood, result.sheetMode)
        assertEquals(FoodAddMode.Saved, result.addMode)
        assertEquals("12345", result.barcode)
        assertEquals("Greek Yogurt", result.productName)
        assertEquals("59.0", result.caloriesPer100g)
    }

    @Test
    fun onScannedLabel_parsesAndAutofillsCreateTab() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        viewModel.onScannedLabel("Energy 250 kcal\nFat 12 g\nCarbohydrate 30 g\nProtein 8 g")
        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.state.value
        assertEquals(AddTab.Create, result.addTab)
        assertEquals("250", result.caloriesPer100g)
        assertEquals("8", result.proteinPer100g)
        assertEquals("30", result.carbsPer100g)
        assertEquals("12", result.fatPer100g)
    }

    @Test
    fun lookupBarcode_withBlankBarcode_setsValidationMessage() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Enter a barcode", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun emptyFoodDiaryDoesNotShowStartActionsOnFoodHome() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(diary = emptyFoodDiary()),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isFoodDiaryEmpty)
        assertTrue(viewModel.state.value.emptyDiaryActions.isEmpty())
    }

    @Test
    fun loggedFoodHidesEmptyDiaryStartActions() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(150.0, 12.0, 18.0, 4.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Yogurt",
                                    brand = null,
                                    quantityGrams = 170.0,
                                    caloriesKcal = 150.0,
                                    proteinGrams = 12.0,
                                    carbsGrams = 18.0,
                                    fatGrams = 4.0,
                                ),
                            ),
                            totals = NutritionTotals(150.0, 12.0, 18.0, 4.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isFoodDiaryEmpty)
        assertTrue(viewModel.state.value.emptyDiaryActions.isEmpty())
    }

    @Test
    fun dailyInsightsHighlightProteinFiberAndNextProtein() = runTest {
        val breakfastDetails = NutritionDetails(fiberGrams = 4.0, sodiumMilligrams = 600.0)
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(600.0, 20.0, 80.0, 15.0),
                    detailTotals = breakfastDetails,
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Toast",
                                    brand = null,
                                    quantityGrams = 100.0,
                                    caloriesKcal = 600.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 80.0,
                                    fatGrams = 15.0,
                                    nutritionDetails = breakfastDetails,
                                ),
                            ),
                            totals = NutritionTotals(600.0, 20.0, 80.0, 15.0),
                            detailTotals = breakfastDetails,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("Protein is low", "Fiber is below target", "Add protein next"),
            viewModel.state.value.dailyInsights.map { it.title },
        )
    }

    @Test
    fun dailyInsightsWarnForHighSodiumAndRecognizeBalancedMeal() = runTest {
        val breakfastDetails = NutritionDetails(fiberGrams = 6.0, sodiumMilligrams = 500.0)
        val dayDetails = NutritionDetails(fiberGrams = 30.0, sodiumMilligrams = 2800.0)
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(1900.0, 110.0, 210.0, 55.0),
                    detailTotals = dayDetails,
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Yogurt bowl",
                                    brand = null,
                                    quantityGrams = 300.0,
                                    caloriesKcal = 450.0,
                                    proteinGrams = 32.0,
                                    carbsGrams = 45.0,
                                    fatGrams = 12.0,
                                    nutritionDetails = breakfastDetails,
                                ),
                            ),
                            totals = NutritionTotals(450.0, 32.0, 45.0, 12.0),
                            detailTotals = breakfastDetails,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val insightTitles = viewModel.state.value.dailyInsights.map { it.title }
        assertTrue("Sodium is high" in insightTitles)
        assertTrue("Breakfast was balanced" in insightTitles)
    }

    @Test
    fun ratingsMarkBalancedDayAndMealAsGreat() = runTest {
        val breakfastDetails = NutritionDetails(fiberGrams = 7.0, sodiumMilligrams = 450.0)
        val dayDetails = NutritionDetails(fiberGrams = 32.0, sodiumMilligrams = 1800.0)
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(2000.0, 108.0, 230.0, 62.0),
                    detailTotals = dayDetails,
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Protein oats",
                                    brand = null,
                                    quantityGrams = 300.0,
                                    caloriesKcal = 520.0,
                                    proteinGrams = 34.0,
                                    carbsGrams = 58.0,
                                    fatGrams = 14.0,
                                    nutritionDetails = breakfastDetails,
                                ),
                            ),
                            totals = NutritionTotals(520.0, 34.0, 58.0, 14.0),
                            detailTotals = breakfastDetails,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Great", viewModel.state.value.dayRating.label)
        assertEquals("Great", viewModel.state.value.mealSections.first { it.id == "breakfast" }.rating?.label)
    }

    @Test
    fun ratingsExplainHighSodiumAndLowProtein() = runTest {
        val breakfastDetails = NutritionDetails(fiberGrams = 1.0, sodiumMilligrams = 1000.0)
        val dayDetails = NutritionDetails(fiberGrams = 10.0, sodiumMilligrams = 3200.0)
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(1800.0, 30.0, 240.0, 60.0),
                    detailTotals = dayDetails,
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Pastry",
                                    brand = null,
                                    quantityGrams = 150.0,
                                    caloriesKcal = 500.0,
                                    proteinGrams = 8.0,
                                    carbsGrams = 70.0,
                                    fatGrams = 18.0,
                                    nutritionDetails = breakfastDetails,
                                ),
                            ),
                            totals = NutritionTotals(500.0, 8.0, 70.0, 18.0),
                            detailTotals = breakfastDetails,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Needs work", viewModel.state.value.dayRating.label)
        assertTrue(viewModel.state.value.dayRating.reason.contains("sodium"))
        val breakfastRating = requireNotNull(viewModel.state.value.mealSections.first { it.id == "breakfast" }.rating)
        assertEquals("Needs work", breakfastRating.label)
        assertTrue(breakfastRating.reason.contains("Protein"))
    }

    @Test
    fun ratingDrillDownUsesDietModeAndPerFoodQuality() = runTest {
        val yogurtDetails = NutritionDetails(fiberGrams = 0.0, sugarGrams = 6.0, saturatedFatGrams = 1.0, sodiumMilligrams = 90.0)
        val pastryDetails = NutritionDetails(fiberGrams = 1.0, sugarGrams = 30.0, saturatedFatGrams = 9.0, sodiumMilligrams = 210.0)
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(570.0, 30.0, 70.0, 20.0),
                    detailTotals = NutritionDetails(fiberGrams = 1.0, sugarGrams = 36.0, saturatedFatGrams = 10.0, sodiumMilligrams = 300.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-yogurt",
                                    foodId = "food-yogurt",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 180.0,
                                    caloriesKcal = 150.0,
                                    proteinGrams = 25.0,
                                    carbsGrams = 15.0,
                                    fatGrams = 2.0,
                                    nutritionDetails = yogurtDetails,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-pastry",
                                    foodId = "food-pastry",
                                    name = "Sweet pastry",
                                    brand = null,
                                    quantityGrams = 120.0,
                                    caloriesKcal = 420.0,
                                    proteinGrams = 5.0,
                                    carbsGrams = 55.0,
                                    fatGrams = 18.0,
                                    nutritionDetails = pastryDetails,
                                ),
                            ),
                            totals = NutritionTotals(570.0, 30.0, 70.0, 20.0),
                            detailTotals = NutritionDetails(fiberGrams = 1.0, sugarGrams = 36.0, saturatedFatGrams = 10.0, sodiumMilligrams = 300.0),
                        ),
                    ),
                ),
                foodGoal = highProteinGoal(),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val dayRating = viewModel.state.value.dayRating
        assertTrue(dayRating.score != null && dayRating.score < 70)
        assertTrue(
            dayRating.factors.any { factor ->
                factor.label == "High protein focus" &&
                    factor.tone == FoodInsightTone.Warning &&
                    factor.explanation.contains("High Protein")
            },
        )

        val entries = viewModel.state.value.mealSections.first { it.id == "breakfast" }.entries
        val yogurtRating = requireNotNull(entries.first { it.id == "entry-yogurt" }.rating)
        val pastryRating = requireNotNull(entries.first { it.id == "entry-pastry" }.rating)
        assertEquals("Great", yogurtRating.label)
        assertEquals("Needs work", pastryRating.label)
        assertTrue(pastryRating.reason.contains("sugar", ignoreCase = true))
    }

    @Test
    fun habitTrackersReflectFruitVegetableFishAndWaterProgress() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(760.0, 46.0, 62.0, 24.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "lunch",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-apple",
                                    foodId = "food-apple",
                                    name = "Apple slices",
                                    brand = null,
                                    quantityGrams = 120.0,
                                    caloriesKcal = 70.0,
                                    proteinGrams = 0.0,
                                    carbsGrams = 18.0,
                                    fatGrams = 0.0,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-spinach",
                                    foodId = "food-spinach",
                                    name = "Spinach vegetable salad",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 190.0,
                                    proteinGrams = 6.0,
                                    carbsGrams = 20.0,
                                    fatGrams = 9.0,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-salmon",
                                    foodId = "food-salmon",
                                    name = "Grilled salmon fish",
                                    brand = null,
                                    quantityGrams = 160.0,
                                    caloriesKcal = 500.0,
                                    proteinGrams = 40.0,
                                    carbsGrams = 24.0,
                                    fatGrams = 15.0,
                                ),
                            ),
                            totals = NutritionTotals(760.0, 46.0, 62.0, 24.0),
                        ),
                    ),
                ),
                waterSummary = FoodWaterSummary(LocalDate.now(), consumedMilliliters = 1500.0, goalMilliliters = 2000.0),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val habits = viewModel.state.value.habitTrackers.associateBy { it.id }
        assertEquals(FoodHabitStatus.Complete, habits.getValue("fruit").status)
        assertEquals(FoodHabitStatus.Complete, habits.getValue("vegetables").status)
        assertEquals(FoodHabitStatus.Complete, habits.getValue("fish").status)
        assertEquals(FoodHabitStatus.InProgress, habits.getValue("water").status)
        assertEquals(0.75, habits.getValue("water").progress, 0.01)
        assertEquals("1500 / 2000 ml", habits.getValue("water").valueLabel)
    }

    @Test
    fun burnedCaloriesFromRepositoryFlowIntoState() = runTest {
        val repository = FakeFoodRepository(burnedCalories = 312.0)
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(312.0, viewModel.state.value.burnedCaloriesKcal, 0.0)
    }

    @Test
    fun burnedCaloriesSurviveOtherStateCollectors() = runTest {
        // burned (250.0) is seeded alongside water/diary/goal data whose own init-time
        // collectors call withWaterSummary/withDiary/withFoodGoal. None must clobber the
        // burned field. Asserting waterProgress proves the water collector actually ran.
        val repository =
            FakeFoodRepository(
                waterSummary = FoodWaterSummary(LocalDate.now(), consumedMilliliters = 1500.0, goalMilliliters = 2000.0),
                burnedCalories = 250.0,
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(250.0, viewModel.state.value.burnedCaloriesKcal, 0.0)
        assertEquals(0.75, viewModel.state.value.waterProgress, 0.01)
    }

    @Test
    fun includeTrainingCaloriesAddsBurnedToRemainingBudget() = runTest {
        fun goal(includeTraining: Boolean) = FoodGoal(
            dailyCaloriesKcal = 2083.0,
            proteinGrams = 104.0,
            carbsGrams = 260.0,
            fatGrams = 69.0,
            fiberGrams = 30.0,
            sugarGrams = 50.0,
            saturatedFatGrams = 20.0,
            sodiumMilligrams = 2300.0,
            mode = FoodGoalMode.Balanced,
            includeTrainingCalories = includeTraining,
        )

        val off = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(foodGoal = goal(includeTraining = false), burnedCalories = 300.0),
        )
        dispatcher.scheduler.advanceUntilIdle()
        val offState = off.state.value
        // Toggle off: burned is informational only, budget stays goal - eaten.
        assertEquals(0.0, offState.trainingCalorieAllowanceKcal, 0.0)
        assertEquals(offState.calorieGoalKcal, offState.effectiveCalorieBudgetKcal, 0.001)
        assertEquals(offState.calorieGoalKcal - offState.eatenCaloriesKcal, offState.remainingCaloriesKcal, 0.001)

        val on = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(foodGoal = goal(includeTraining = true), burnedCalories = 300.0),
        )
        dispatcher.scheduler.advanceUntilIdle()
        val onState = on.state.value
        // Toggle on: burned raises the allowance, so remaining = goal - eaten + burned.
        assertEquals(300.0, onState.trainingCalorieAllowanceKcal, 0.0)
        assertEquals(onState.calorieGoalKcal + 300.0, onState.effectiveCalorieBudgetKcal, 0.001)
        assertEquals(
            onState.calorieGoalKcal - onState.eatenCaloriesKcal + 300.0,
            onState.remainingCaloriesKcal,
            0.001,
        )
    }

    @Test
    fun habitTrackersIgnoreSubstringFalsePositives() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(900.0, 30.0, 110.0, 35.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "lunch",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-pepperoni",
                                    foodId = "food-pepperoni",
                                    name = "Pepperoni pizza",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 540.0,
                                    proteinGrams = 22.0,
                                    carbsGrams = 60.0,
                                    fatGrams = 24.0,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-shake",
                                    foodId = "food-shake",
                                    name = "Strawberry milkshake",
                                    brand = null,
                                    quantityGrams = 300.0,
                                    caloriesKcal = 360.0,
                                    proteinGrams = 8.0,
                                    carbsGrams = 50.0,
                                    fatGrams = 11.0,
                                ),
                            ),
                            totals = NutritionTotals(900.0, 30.0, 110.0, 35.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val habits = viewModel.state.value.habitTrackers.associateBy { it.id }
        // "Pepperoni" must not count as the "pepper" vegetable; "Strawberry" must not count as "berry" fruit.
        assertEquals(FoodHabitStatus.Missing, habits.getValue("vegetables").status)
        assertEquals(FoodHabitStatus.Missing, habits.getValue("fruit").status)
        assertEquals(FoodHabitStatus.Missing, habits.getValue("fish").status)
    }

    @Test
    fun weeklyMusFitScoreExplainsNutritionHydrationHabitsAndUnavailableTrainingSignal() = runTest {
        val startDate = LocalDate.now()
        val repository =
            FakeFoodRepository(
                weeklySummary = weeklySummaryForScore(startDate),
            )
        val viewModel = NutritionTrendsViewModel(repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val score = viewModel.state.value.weeklyScore
        assertEquals("Weekly MusFit score", score.title)
        assertTrue(score.score in 60..85)
        assertTrue(score.summary.contains("3 tracked days"))
        assertEquals(
            listOf("Nutrition consistency", "Hydration", "Habits", "Training signal"),
            score.factors.map { it.label },
        )
        assertTrue(score.factors.first { it.label == "Hydration" }.valueLabel.contains("72%"))
        assertEquals(FoodInsightTone.Neutral, score.factors.first { it.label == "Training signal" }.tone)
        assertTrue(score.suggestion.contains("water", ignoreCase = true) || score.suggestion.contains("protein", ignoreCase = true))
    }

    @Test
    fun weeklyMusFitScoreUsesTrailingSevenDayWindow() = runTest {
        val today = LocalDate.now()
        val repository = FakeFoodRepository()
        NutritionTrendsViewModel(repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(today.minusDays(6), repository.weeklySummaryStartDates.last())
    }

    @Test
    fun foodProgressStatsSummarizeWeeklyMonthlyAdherenceAndTrends() = runTest {
        val startDate = LocalDate.now().minusDays(27)
        val repository =
            FakeFoodRepository(
                progressSummary = progressSummaryForStats(startDate),
            )
        val viewModel = NutritionTrendsViewModel(repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.state.value.progressStats

        assertEquals("Last 7 days", stats.weekly.title)
        assertEquals("3 tracked days", stats.weekly.trackedDaysLabel)
        val weeklyMetrics = stats.weekly.metrics.associate { it.caption to it.value }
        assertEquals("2160 kcal", weeklyMetrics["Avg calories"])
        assertEquals("96 g", weeklyMetrics["Avg protein"])
        assertEquals("2/3 days", weeklyMetrics["Calorie target"])
        assertEquals("2/3 days", weeklyMetrics["Hydration"])
        assertEquals("Last 28 days", stats.monthly.title)
        assertTrue(stats.monthly.trackedDaysLabel.contains("5 tracked days"))
        assertTrue(stats.monthly.trendLabel.contains("up", ignoreCase = true))
    }

    @Test
    fun openWaterSheet_showsWaterSheet() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openWaterSheet()

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.Water, sheetMode)
        }
    }

    @Test
    fun openHealthConnectSheet_showsHealthConnectSheet() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openHealthConnectSheet()

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.HealthConnect, sheetMode)
        }
    }

    @Test
    fun lookupBarcode_populatesEditableResultAndSaveUsesEdits() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        assertEquals("", viewModel.state.value.barcode)

        viewModel.onBarcodeChanged("123abc45")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.state.value
        assertEquals("12345", result.barcode)
        assertEquals("Greek Yogurt", result.productName)
        assertEquals("Example Dairy", result.brand)
        assertEquals("59.0", result.caloriesPer100g)
        assertEquals("10.0", result.proteinPer100g)
        assertEquals("3.6", result.carbsPer100g)
        assertEquals("0.4", result.fatPer100g)
        assertNull(result.message)

        viewModel.onProductNameChanged("Edited Yogurt")
        viewModel.onBrandChanged("")
        viewModel.onCaloriesChanged("61")
        viewModel.onProteinChanged("10.5")
        viewModel.onCarbsChanged("4.0")
        viewModel.onFatChanged("0.5")
        viewModel.onMealTypeChanged("snack")
        viewModel.onQuantityChanged("170")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Logged food", viewModel.state.value.message)
        assertEquals("Edited Yogurt", repository.savedLog?.name)
        assertNull(repository.savedLog?.brand)
        assertEquals("snack", repository.savedLog?.mealType)
        assertEquals(170.0, repository.savedLog?.quantityGrams ?: 0.0, 0.01)
        assertEquals(
            FoodNutrition(
                caloriesKcal = 61.0,
                proteinGrams = 10.5,
                carbsGrams = 4.0,
                fatGrams = 0.5,
            ),
            repository.savedLog?.nutritionPer100g,
        )
    }

    @Test
    fun lookupBarcode_previewsNutritionForEditedAmount() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(
                result = foundProduct(
                    servingQuantityGrams = 170.0,
                    nutrition = FoodNutrition(
                        caloriesKcal = 59.0,
                        proteinGrams = 10.0,
                        carbsGrams = 3.6,
                        fatGrams = 0.4,
                    ),
                ),
            ),
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("123456")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("170", viewModel.state.value.quantityGrams)
        assertEquals(100.3, viewModel.state.value.amountNutritionPreview?.caloriesKcal ?: 0.0, 0.01)
        assertEquals(17.0, viewModel.state.value.amountNutritionPreview?.proteinGrams ?: 0.0, 0.01)
        assertEquals(6.12, viewModel.state.value.amountNutritionPreview?.carbsGrams ?: 0.0, 0.01)
        assertEquals(0.68, viewModel.state.value.amountNutritionPreview?.fatGrams ?: 0.0, 0.01)

        viewModel.onQuantityChanged("250")

        assertEquals(147.5, viewModel.state.value.amountNutritionPreview?.caloriesKcal ?: 0.0, 0.01)
        assertEquals(25.0, viewModel.state.value.amountNutritionPreview?.proteinGrams ?: 0.0, 0.01)
        assertEquals(9.0, viewModel.state.value.amountNutritionPreview?.carbsGrams ?: 0.0, 0.01)
        assertEquals(1.0, viewModel.state.value.amountNutritionPreview?.fatGrams ?: 0.0, 0.01)
    }

    @Test
    fun lookupBarcode_exposesServingChoicesAndSelectionUpdatesAmount() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(
                result = foundProduct(
                    servingQuantityGrams = 170.0,
                    nutrition = FoodNutrition(59.0, 10.0, 3.6, 0.4),
                ),
            ),
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("123456")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        val initialChoices = viewModel.state.value.amountServingChoices
        assertEquals(listOf("100 g", "Serving 170 g"), initialChoices.map { it.label })

        val per100gChoice = initialChoices.first { it.grams == 100.0 }
        viewModel.onAmountServingChoiceSelected(per100gChoice.id)

        assertEquals("100", viewModel.state.value.quantityGrams)
        assertEquals(59.0, viewModel.state.value.amountNutritionPreview?.caloriesKcal ?: 0.0, 0.01)
    }

    @Test
    fun logFood_withoutLookup_logsManualMealEntry() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(result = ProductLookupResult.NotFound("")),
            repository = repository,
        )

        viewModel.onProductNameChanged("Oats")
        viewModel.onBrandChanged("Pantry")
        viewModel.onCaloriesChanged("380")
        viewModel.onProteinChanged("13")
        viewModel.onCarbsChanged("67")
        viewModel.onFatChanged("7")
        viewModel.onMealTypeChanged("breakfast")
        viewModel.onQuantityChanged("50")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Logged food", viewModel.state.value.message)
        assertNull(repository.savedLog?.lookupResult)
        assertEquals("Oats", repository.savedLog?.name)
        assertEquals("Pantry", repository.savedLog?.brand)
        assertEquals("breakfast", repository.savedLog?.mealType)
        assertEquals(50.0, repository.savedLog?.quantityGrams ?: 0.0, 0.01)
    }

    @Test
    fun lookupBarcode_setsLoadingWhileRequestIsInFlight() = runTest {
        val provider = BlockingProductProvider()
        val viewModel = FoodViewModel(
            provider = provider,
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("123456")
        viewModel.lookupBarcode()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.message)

        provider.completeWith(foundProduct(barcode = "123456"))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Greek Yogurt", viewModel.state.value.productName)
    }

    @Test
    fun lookupBarcode_ignoresStaleInFlightResponseAfterBarcodeChanges() = runTest {
        val provider = BarcodeBlockingProductProvider()
        val viewModel = FoodViewModel(
            provider = provider,
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("111")
        viewModel.lookupBarcode()
        dispatcher.scheduler.runCurrent()

        viewModel.onBarcodeChanged("222")
        provider.completeWith("111", foundProduct(barcode = "111", name = "Old Product"))
        dispatcher.scheduler.advanceUntilIdle()

        with(viewModel.state.value) {
            assertEquals("222", barcode)
            assertFalse(isLoading)
            assertNull(lookupResult)
            assertEquals("", productName)
            assertEquals("", brand)
            assertEquals("", caloriesPer100g)
            assertEquals("", proteinPer100g)
            assertEquals("", carbsPer100g)
            assertEquals("", fatPer100g)
        }
    }

    @Test
    fun lookupBarcode_whenProductNotFound_setsMessage() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(result = ProductLookupResult.NotFound("999")),
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("999")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Product not found. Add details to create it.", viewModel.state.value.message)
        assertNull(viewModel.state.value.lookupResult)
    }

    @Test
    fun lookupBarcode_whenLookupFails_setsFailureMessage() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(result = ProductLookupResult.Failed("999", "Lookup failed")),
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("999")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Lookup failed", viewModel.state.value.message)
        assertNull(viewModel.state.value.lookupResult)
    }

    @Test
    fun onBarcodeChanged_afterSuccessfulLookup_clearsStaleLookupAndEditableFields() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Greek Yogurt", viewModel.state.value.productName)
        assertTrue(viewModel.state.value.lookupResult != null)

        viewModel.onBarcodeChanged("54321")

        with(viewModel.state.value) {
            assertEquals("54321", barcode)
            assertNull(lookupResult)
            assertEquals("", productName)
            assertEquals("", brand)
            assertEquals("", caloriesPer100g)
            assertEquals("", proteinPer100g)
            assertEquals("", carbsPer100g)
            assertEquals("", fatPer100g)
        }
    }

    @Test
    fun lookupBarcode_withZeroNutrition_keepsZeroValuesVisibleAndSaveable() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(result = foundProduct(nutrition = FoodNutrition(0.0, 0.0, 0.0, 0.0))),
            repository = repository,
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        with(viewModel.state.value) {
            assertEquals("0.0", caloriesPer100g)
            assertEquals("0.0", proteinPer100g)
            assertEquals("0.0", carbsPer100g)
            assertEquals("0.0", fatPer100g)
        }

        viewModel.onCaloriesChanged("0")
        viewModel.onProteinChanged("0")
        viewModel.onCarbsChanged("0")
        viewModel.onFatChanged("0")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            FoodNutrition(
                caloriesKcal = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
            ),
            repository.savedLog?.nutritionPer100g,
        )
    }

    @Test
    fun saveProduct_whileSaveInFlight_onlySavesOnce() = runTest {
        val repository = BlockingFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logFood()
        viewModel.logFood()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.saveCalls)

        repository.completeSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Logged food", viewModel.state.value.message)
    }

    @Test
    fun saveProduct_withInvalidNutritionText_setsValidationMessageAndDoesNotSave() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCaloriesChanged("..")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Enter valid nutrition values", viewModel.state.value.message)
        assertNull(repository.savedLog)
    }

    @Test
    fun saveScannedProductToDatabase_savesEditedLookupWithoutLoggingMeal() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onProductNameChanged("Edited yogurt")
        viewModel.onBrandChanged("")
        viewModel.onCaloriesChanged("61")
        viewModel.onProteinChanged("10.5")
        viewModel.onCarbsChanged("4")
        viewModel.onFatChanged("0.5")
        viewModel.saveScannedProductToDatabase()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Saved product to database", viewModel.state.value.message)
        assertEquals("Edited yogurt", repository.confirmedProductSave?.editedName)
        assertNull(repository.confirmedProductSave?.editedBrand)
        assertEquals(61.0, repository.confirmedProductSave?.editedNutrition?.caloriesKcal ?: 0.0, 0.01)
        assertNull(repository.savedLog)
    }

    @Test
    fun saveProduct_withBlankNutritionText_setsValidationMessageAndDoesNotSave() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onProteinChanged("")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Enter valid nutrition values", viewModel.state.value.message)
        assertNull(repository.savedLog)
    }

    @Test
    fun logFood_withNegativeNutrition_setsValidationMessageAndDoesNotSave() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.onBarcodeChanged("12345")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCaloriesChanged("-100")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Enter valid nutrition values", viewModel.state.value.message)
        assertNull(repository.savedLog)
    }

    @Test
    fun state_loadsDiarySectionsAndSavedFoods() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(
                        caloriesKcal = 250.0,
                        proteinGrams = 20.0,
                        carbsGrams = 25.0,
                        fatGrams = 8.0,
                    ),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = "Kitchen",
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                        FoodDiaryMeal(
                            type = "snacks",
                            entries = emptyList(),
                            totals = NutritionTotals(130.0, 0.0, 17.0, 6.0),
                        ),
                    ),
                ),
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 200.0,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = 60.0,
                            proteinGrams = 10.0,
                            carbsGrams = 4.0,
                            fatGrams = 1.0,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals(250.0, state.eatenCaloriesKcal, 0.01)
        assertEquals(1833.0, state.remainingCaloriesKcal, 0.01)
        assertEquals(listOf("Breakfast", "Lunch", "Dinner", "Snacks"), state.mealSections.map { it.title })
        assertEquals(120.0, state.mealSections.first { it.id == "breakfast" }.caloriesKcal, 0.01)
        assertEquals("Greek yogurt", state.mealSections.first { it.id == "breakfast" }.entries.single().name)
        assertTrue(state.mealSections.first { it.id == "lunch" }.entries.isEmpty())
        assertEquals("Greek yogurt", state.savedFoods.single().name)
        assertEquals(120.0, state.savedFoods.single().caloriesPerServingKcal, 0.01)
    }

    @Test
    fun savedFoodsExposeSourceBadgesAndDuplicateGroups() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        barcode = "111",
                    ),
                    SavedFoodItem(
                        id = "food-2",
                        name = "Greek yogurt alt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        barcode = "111",
                    ),
                    SavedFoodItem(
                        id = "food-3",
                        name = "Oats",
                        brand = "Pantry",
                        defaultServingGrams = 40.0,
                        nutritionPer100g = FoodNutrition(389.0, 17.0, 66.0, 7.0),
                    ),
                    SavedFoodItem(
                        id = "food-4",
                        name = "oats",
                        brand = "pantry",
                        defaultServingGrams = 40.0,
                        nutritionPer100g = FoodNutrition(389.0, 17.0, 66.0, 7.0),
                    ),
                    SavedFoodItem(
                        id = "food-5",
                        name = "Protein cereal",
                        brand = null,
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(220.0, 12.0, 30.0, 8.0),
                        category = "Nutrition label",
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals("Scanned", state.savedFoods.first { it.id == "food-1" }.sourceLabel)
        assertEquals("Manual", state.savedFoods.first { it.id == "food-3" }.sourceLabel)
        assertEquals("Label", state.savedFoods.first { it.id == "food-5" }.sourceLabel)
        assertEquals(2, state.duplicateFoodGroups.size)
        assertEquals("food-1", state.duplicateFoodGroups[0].primaryFoodId)
        assertEquals(listOf("food-2"), state.duplicateFoodGroups[0].duplicateFoodIds)
        assertEquals("Barcode 111", state.duplicateFoodGroups[0].reason)
        assertEquals("food-3", state.duplicateFoodGroups[1].primaryFoodId)
        assertEquals(listOf("food-4"), state.duplicateFoodGroups[1].duplicateFoodIds)
        assertEquals("Name and brand", state.duplicateFoodGroups[1].reason)
    }

    @Test
    fun savedFoodsExposeTrustGuidanceForImportsManualEntriesAndLabelScans() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        barcode = "111",
                    ),
                    SavedFoodItem(
                        id = "food-2",
                        name = "Oats",
                        brand = "Pantry",
                        defaultServingGrams = 40.0,
                        nutritionPer100g = FoodNutrition(389.0, 17.0, 66.0, 7.0),
                    ),
                    SavedFoodItem(
                        id = "food-3",
                        name = "Protein cereal",
                        brand = null,
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(220.0, 12.0, 30.0, 8.0),
                        category = "Nutrition label",
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val scanned = viewModel.state.value.savedFoods.first { it.id == "food-1" }
        val manual = viewModel.state.value.savedFoods.first { it.id == "food-2" }
        val labelScan = viewModel.state.value.savedFoods.first { it.id == "food-3" }

        assertEquals(FoodTrustLevel.Imported, scanned.trust.level)
        assertEquals("Barcode import", scanned.trust.label)
        assertEquals("Check", scanned.trust.actionLabel)
        assertEquals(FoodTrustLevel.Manual, manual.trust.level)
        assertEquals("Manual entry", manual.trust.label)
        assertEquals(FoodTrustLevel.NeedsReview, labelScan.trust.level)
        assertEquals("Review label scan", labelScan.trust.label)
    }

    @Test
    fun reportingSavedFoodMarksLocalReviewAndCorrectionOpensEditor() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        barcode = "111",
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openSavedFoodDetail("food-1")
        viewModel.reportSavedFoodForReview("food-1")

        assertEquals("Marked food for local review", viewModel.state.value.message)
        assertTrue(viewModel.state.value.reportedSavedFoodIds.contains("food-1"))
        assertEquals(FoodTrustLevel.NeedsReview, viewModel.state.value.savedFoods.single().trust.level)
        assertEquals(FoodTrustLevel.NeedsReview, viewModel.state.value.selectedSavedFoodDetail?.trust?.level)
        assertTrue(viewModel.state.value.selectedSavedFoodDetail?.trust?.isReported == true)

        viewModel.startSavedFoodCorrection("food-1")

        val editor = requireNotNull(viewModel.state.value.savedFoodEditor)
        assertEquals(FoodSheetMode.SavedFoodEditor, viewModel.state.value.sheetMode)
        assertEquals("Greek yogurt", editor.name)
        assertEquals("Review and correct nutrition before saving.", viewModel.state.value.message)
    }

    @Test
    fun mergeDuplicateFoods_delegatesToRepositoryAndShowsCleanupMessage() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.mergeDuplicateFoods("food-1", listOf("food-2", "food-3"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MergeDuplicateFoodsCall("food-1", listOf("food-2", "food-3")), repository.mergeDuplicateFoodsCall)
        assertEquals("Merged duplicate foods", viewModel.state.value.message)
    }

    @Test
    fun openMealDetail_selectsMealAndExposesMealMacroTotals() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(360.0, 30.0, 24.0, 6.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-2",
                                    foodId = "food-2",
                                    name = "Banana",
                                    brand = null,
                                    quantityGrams = 118.0,
                                    caloriesKcal = 105.0,
                                    proteinGrams = 1.3,
                                    carbsGrams = 27.0,
                                    fatGrams = 0.4,
                                ),
                            ),
                            totals = NutritionTotals(225.0, 21.3, 35.0, 2.4),
                            detailTotals = NutritionDetails(
                                fiberGrams = 4.5,
                                sugarGrams = 17.0,
                                saturatedFatGrams = 1.2,
                                sodiumMilligrams = 96.0,
                            ),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openMealDetail("breakfast")

        val state = viewModel.state.value
        val selectedMeal = state.mealSections.first { it.id == state.selectedMealDetailId }
        assertEquals("breakfast", state.selectedMealDetailId)
        assertEquals("Breakfast", selectedMeal.title)
        assertEquals(225.0, selectedMeal.caloriesKcal, 0.01)
        assertEquals(21.3, selectedMeal.proteinGrams, 0.01)
        assertEquals(35.0, selectedMeal.carbsGrams, 0.01)
        assertEquals(2.4, selectedMeal.fatGrams, 0.01)
        assertEquals(4.5, selectedMeal.fiberGrams, 0.01)
        assertEquals(17.0, selectedMeal.sugarGrams, 0.01)
        assertEquals(1.2, selectedMeal.saturatedFatGrams, 0.01)
        assertEquals(96.0, selectedMeal.sodiumMilligrams, 0.01)
        assertEquals(listOf("Greek yogurt", "Banana"), selectedMeal.entries.map { it.name })

        viewModel.closeMealDetail()

        assertNull(viewModel.state.value.selectedMealDetailId)
    }

    @Test
    fun micronutrientsExposeCompactDayAndMealRows() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(420.0, 32.0, 48.0, 12.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = emptyList(),
                            totals = NutritionTotals(420.0, 32.0, 48.0, 12.0),
                            detailTotals = NutritionDetails(
                                sodiumMilligrams = 510.0,
                                potassiumMilligrams = 700.0,
                                calciumMilligrams = 220.0,
                                ironMilligrams = 4.2,
                                vitaminDMicrograms = 2.5,
                                vitaminCMilligrams = 18.0,
                                magnesiumMilligrams = 95.0,
                            ),
                        ),
                    ),
                    detailTotals = NutritionDetails(
                        sodiumMilligrams = 510.0,
                        potassiumMilligrams = 700.0,
                        calciumMilligrams = 220.0,
                        ironMilligrams = 4.2,
                        vitaminDMicrograms = 2.5,
                        vitaminCMilligrams = 18.0,
                        magnesiumMilligrams = 95.0,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val dayMicronutrients = viewModel.state.value.micronutrients.associateBy { it.label }

        assertEquals(510.0, requireNotNull(dayMicronutrients["Sodium"]).value, 0.01)
        assertEquals("mg", requireNotNull(dayMicronutrients["Sodium"]).unit)
        assertEquals(700.0, requireNotNull(dayMicronutrients["Potassium"]).value, 0.01)
        assertEquals(2.5, requireNotNull(dayMicronutrients["Vitamin D"]).value, 0.01)
        assertEquals("mcg", requireNotNull(dayMicronutrients["Vitamin D"]).unit)
        assertEquals(95.0, requireNotNull(dayMicronutrients["Magnesium"]).value, 0.01)

        viewModel.openMealDetail("breakfast")

        val mealMicronutrients = requireNotNull(viewModel.state.value.selectedMealDetailForDisplay())
            .micronutrients
            .associateBy { it.label }

        assertEquals(220.0, requireNotNull(mealMicronutrients["Calcium"]).value, 0.01)
        assertEquals(4.2, requireNotNull(mealMicronutrients["Iron"]).value, 0.01)
        assertEquals(18.0, requireNotNull(mealMicronutrients["Vitamin C"]).value, 0.01)
    }

    @Test
    fun mealDetailSortMode_sortsSelectedMealItemsForDisplay() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(405.0, 35.3, 36.0, 12.4),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-2",
                                    foodId = "food-2",
                                    name = "Banana",
                                    brand = null,
                                    quantityGrams = 118.0,
                                    caloriesKcal = 105.0,
                                    proteinGrams = 1.3,
                                    carbsGrams = 27.0,
                                    fatGrams = 0.4,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-3",
                                    foodId = "food-3",
                                    name = "Eggs",
                                    brand = null,
                                    quantityGrams = 100.0,
                                    caloriesKcal = 180.0,
                                    proteinGrams = 14.0,
                                    carbsGrams = 1.0,
                                    fatGrams = 10.0,
                                ),
                            ),
                            totals = NutritionTotals(405.0, 35.3, 36.0, 12.4),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openMealDetail("breakfast")

        assertEquals(
            listOf("Greek yogurt", "Banana", "Eggs"),
            viewModel.state.value.selectedMealDetailForDisplay()?.entries?.map { it.name },
        )

        viewModel.onMealDetailSortChanged(MealDetailSortMode.Calories)
        assertEquals(
            listOf("Eggs", "Greek yogurt", "Banana"),
            viewModel.state.value.selectedMealDetailForDisplay()?.entries?.map { it.name },
        )

        viewModel.onMealDetailSortChanged(MealDetailSortMode.Protein)
        assertEquals(
            listOf("Greek yogurt", "Eggs", "Banana"),
            viewModel.state.value.selectedMealDetailForDisplay()?.entries?.map { it.name },
        )

        viewModel.onMealDetailSortChanged(MealDetailSortMode.Name)
        assertEquals(
            listOf("Banana", "Eggs", "Greek yogurt"),
            viewModel.state.value.selectedMealDetailForDisplay()?.entries?.map { it.name },
        )
    }

    @Test
    fun mealDetail_exposesCalorieProgressAndItemContributions() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(312.5, 25.0, 50.0, 10.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 200.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 20.0,
                                    fatGrams = 5.0,
                                ),
                                FoodDiaryEntry(
                                    id = "entry-2",
                                    foodId = "food-2",
                                    name = "Banana oats",
                                    brand = null,
                                    quantityGrams = 150.0,
                                    caloriesKcal = 112.5,
                                    proteinGrams = 5.0,
                                    carbsGrams = 30.0,
                                    fatGrams = 5.0,
                                ),
                            ),
                            totals = NutritionTotals(312.5, 25.0, 50.0, 10.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openMealDetail("breakfast")

        val meal = requireNotNull(viewModel.state.value.selectedMealDetailForDisplay())
        assertEquals(625.0, meal.calorieTargetKcal, 0.01)
        assertEquals(0.5, meal.calorieProgress, 0.01)

        val yogurt = meal.entries.first { it.id == "entry-1" }
        assertEquals(0.64, yogurt.calorieContribution, 0.01)
        assertEquals(0.80, yogurt.proteinContribution, 0.01)
        assertEquals(0.40, yogurt.carbsContribution, 0.01)
        assertEquals(0.50, yogurt.fatContribution, 0.01)

        val oats = meal.entries.first { it.id == "entry-2" }
        assertEquals(0.36, oats.calorieContribution, 0.01)
        assertEquals(0.20, oats.proteinContribution, 0.01)
        assertEquals(0.60, oats.carbsContribution, 0.01)
        assertEquals(0.50, oats.fatContribution, 0.01)
    }

    @Test
    fun customMealDefinitionsAppearInDiaryAndAddFlow() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(180.0, 20.0, 12.0, 6.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "pre_workout",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Protein shake",
                                    brand = null,
                                    quantityGrams = 300.0,
                                    caloriesKcal = 180.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 12.0,
                                    fatGrams = 6.0,
                                ),
                            ),
                            totals = NutritionTotals(180.0, 20.0, 12.0, 6.0),
                        ),
                    ),
                ),
                customMealDefinitions = listOf(
                    FoodMealDefinition(
                        id = "pre_workout",
                        name = "Pre-workout",
                        timeMinutes = 16 * 60 + 30,
                        sortOrder = 1,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf("Breakfast", "Pre-workout", "Lunch", "Dinner", "Snacks"),
            viewModel.state.value.mealSections.map { it.title },
        )
        val customMeal = viewModel.state.value.mealSections.first { it.id == "pre_workout" }
        assertEquals("16:30", customMeal.recommendation)
        assertEquals("Protein shake", customMeal.entries.single().name)

        viewModel.openAddFood("pre_workout")

        assertEquals("pre_workout", viewModel.state.value.mealType)
        assertEquals("Pre-workout", viewModel.state.value.selectedMealTitle)
    }

    @Test
    fun saveCustomMealDefinitionDelegatesToRepositoryAndShowsMessage() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.openMealSettings()
        viewModel.onCustomMealNameChanged("Post-workout")
        viewModel.onCustomMealTimeChanged("18:45")
        viewModel.saveCustomMealDefinition()
        dispatcher.scheduler.advanceUntilIdle()

        val input = requireNotNull(repository.customMealDefinitionUpsert)
        assertEquals("Post-workout", input.name)
        assertEquals(18 * 60 + 45, input.timeMinutes)
        assertEquals("Saved custom meal", viewModel.state.value.message)
    }

    @Test
    fun editMealDefinitionDelegatesExplicitIdNameTimeAndSortOrder() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.openMealSettings()
        viewModel.openMealDefinitionEditor("breakfast")
        viewModel.onCustomMealNameChanged("Morning")
        viewModel.onCustomMealTimeChanged("07:30")
        viewModel.onCustomMealSortOrderChanged("12")
        viewModel.saveCustomMealDefinition()
        dispatcher.scheduler.advanceUntilIdle()

        val input = requireNotNull(repository.customMealDefinitionUpsert)
        assertEquals("breakfast", input.mealId)
        assertEquals("Morning", input.name)
        assertEquals(7 * 60 + 30, input.timeMinutes)
        assertEquals(12, input.sortOrder)
        assertEquals("Saved meal", viewModel.state.value.message)
    }

    @Test
    fun hiddenMealDefinitionIsExcludedFromDiarySectionsButStillCounts() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(180.0, 20.0, 12.0, 6.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "pre_workout",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Protein shake",
                                    brand = null,
                                    quantityGrams = 300.0,
                                    caloriesKcal = 180.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 12.0,
                                    fatGrams = 6.0,
                                ),
                            ),
                            totals = NutritionTotals(180.0, 20.0, 12.0, 6.0),
                        ),
                    ),
                ),
                customMealDefinitions = listOf(
                    FoodMealDefinition(
                        id = "pre_workout",
                        name = "Pre-workout",
                        timeMinutes = 16 * 60 + 30,
                        sortOrder = 1,
                        isHidden = true,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        // A hidden meal never renders as a diary section, even with logged entries.
        assertFalse(viewModel.state.value.mealSections.any { it.id == "pre_workout" })
        // But it stays in the definition list so it can be un-hidden.
        val hidden = viewModel.state.value.mealDefinitions.first { it.id == "pre_workout" }
        assertTrue(hidden.isHidden)
        // Its logged calories still count toward the day total.
        assertEquals(180.0, viewModel.state.value.eatenCaloriesKcal, 0.01)
    }

    @Test
    fun toggleMealHiddenDelegatesToRepositoryWithHiddenFlag() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.toggleMealHidden("breakfast")
        dispatcher.scheduler.advanceUntilIdle()

        val input = requireNotNull(repository.customMealDefinitionUpsert)
        assertEquals("breakfast", input.mealId)
        assertTrue(input.isHidden)
    }

    @Test
    fun toggleMealHiddenRefusesToHideLastVisibleMeal() = runTest {
        val repository =
            FakeFoodRepository(
                customMealDefinitions = listOf(
                    FoodMealDefinition(id = "breakfast", name = "Breakfast", timeMinutes = null, sortOrder = 0, isHidden = true),
                    FoodMealDefinition(id = "lunch", name = "Lunch", timeMinutes = null, sortOrder = 10, isHidden = true),
                    FoodMealDefinition(id = "dinner", name = "Dinner", timeMinutes = null, sortOrder = 20, isHidden = true),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleMealHidden("snacks")
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.customMealDefinitionUpsert)
        assertEquals("Keep at least one meal visible", viewModel.state.value.message)
    }

    @Test
    fun openAddFoodFromMealDetail_usesSelectedMeal() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        viewModel.openMealDetail("lunch")
        viewModel.openAddFoodFromMealDetail()

        with(viewModel.state.value) {
            assertEquals("lunch", selectedMealDetailId)
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.AddFood, sheetMode)
            assertEquals("lunch", mealType)
            assertEquals("Lunch", selectedMealTitle)
            assertEquals(FoodAddMode.Saved, addMode)
        }
    }

    @Test
    fun openAddFood_selectsMealAndSavedMode() = runTest {
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = FakeFoodRepository(),
        )

        viewModel.openAddFood("lunch")

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals("lunch", mealType)
            assertEquals("Lunch", selectedMealTitle)
            assertEquals(FoodAddMode.Saved, addMode)
        }
    }

    @Test
    fun logSavedFood_logsSelectedFoodIntoSelectedMeal() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openAddFood("dinner")
        viewModel.onSavedFoodQuantityChanged("75")
        viewModel.logSavedFood("food-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("food-1", repository.savedFoodLog?.foodId)
        assertEquals("dinner", repository.savedFoodLog?.mealType)
        assertEquals(75.0, repository.savedFoodLog?.quantityGrams ?: 0.0, 0.01)
        assertEquals("Logged food", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isAddPanelVisible)
    }

    @Test
    fun onMealTypeChanged_refreshesSameAsYesterdayForNewMeal() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openAddFood("breakfast")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onMealTypeChanged("lunch")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("breakfast", "lunch"), repository.sameAsYesterdayRequests)
        assertEquals("lunch", viewModel.state.value.mealType)
        assertEquals("Lunch", viewModel.state.value.selectedMealTitle)
    }

    @Test
    fun logSameAsYesterday_logsEveryItemIntoSelectedMeal() = runTest {
        val repository = FakeFoodRepository()
        repository.sameAsYesterdayItems = listOf(
            SavedFoodItem(
                id = "food-1",
                name = "Greek yogurt",
                brand = "Kitchen",
                defaultServingGrams = 200.0,
                nutritionPer100g = FoodNutrition(60.0, 10.0, 4.0, 1.0),
            ),
            SavedFoodItem(
                id = "food-3",
                name = "Oats",
                brand = "Pantry",
                defaultServingGrams = 40.0,
                nutritionPer100g = FoodNutrition(389.0, 17.0, 66.0, 7.0),
            ),
        )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openAddFood("lunch")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.logSameAsYesterday()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("food-1", "food-3"), repository.savedFoodLogs.map { it.foodId })
        assertEquals(listOf("lunch", "lunch"), repository.savedFoodLogs.map { it.mealType })
        assertEquals(200.0, repository.savedFoodLogs[0].quantityGrams, 0.01)
        assertEquals(40.0, repository.savedFoodLogs[1].quantityGrams, 0.01)
        assertEquals("Logged 2 foods", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isAddPanelVisible)
    }

    @Test
    fun quickLog_logsCaloriesIntoSelectedMeal() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openAddFood("snacks")
        viewModel.selectAddMode(FoodAddMode.Quick)
        viewModel.onQuickCaloriesChanged("320")
        viewModel.onQuickProteinChanged("22")
        viewModel.onQuickCarbsChanged("36")
        viewModel.onQuickFatChanged("9")
        viewModel.quickLog()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("snacks", repository.quickLog?.mealType)
        assertEquals(320.0, repository.quickLog?.caloriesKcal ?: 0.0, 0.01)
        assertEquals(22.0, repository.quickLog?.proteinGrams ?: 0.0, 0.01)
        assertEquals(36.0, repository.quickLog?.carbsGrams ?: 0.0, 0.01)
        assertEquals(9.0, repository.quickLog?.fatGrams ?: 0.0, 0.01)
        assertEquals("Logged quick calories", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isAddPanelVisible)
    }

    @Test
    fun saveFavoriteQuickLogUsesCurrentQuickInputs() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.selectAddMode(FoodAddMode.Quick)
        viewModel.onQuickCaloriesChanged("320")
        viewModel.onQuickProteinChanged("22")
        viewModel.onQuickCarbsChanged("36")
        viewModel.onQuickFatChanged("9")
        viewModel.saveFavoriteQuickLog()
        dispatcher.scheduler.advanceUntilIdle()

        val preset = requireNotNull(repository.favoriteQuickLogSave)
        assertEquals("320 kcal quick log", preset.name)
        assertEquals(320.0, preset.caloriesKcal, 0.01)
        assertEquals(22.0, preset.proteinGrams, 0.01)
        assertEquals(36.0, preset.carbsGrams, 0.01)
        assertEquals(9.0, preset.fatGrams, 0.01)
        assertTrue(preset.isFavorite)
        assertEquals("Saved quick log favorite", viewModel.state.value.message)
    }

    @Test
    fun logFavoriteQuickLogLogsPresetIntoSelectedMeal() = runTest {
        val repository =
            FakeFoodRepository(
                quickCaloriePresets = listOf(
                    QuickCaloriePreset(
                        id = "quick-1",
                        name = "Protein snack",
                        caloriesKcal = 320.0,
                        proteinGrams = 22.0,
                        carbsGrams = 36.0,
                        fatGrams = 9.0,
                        isFavorite = true,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.quickCaloriePresets.single().isFavorite)

        viewModel.openAddFood("snacks")
        viewModel.logFavoriteQuickLog("quick-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LogFavoriteQuickLogCall("quick-1", "snacks", LocalDate.now()), repository.logFavoriteQuickLogCall)
        assertEquals("Logged favorite quick log", viewModel.state.value.message)
    }

    @Test
    fun addFlowFavoritesExposeOnlyFavoriteItemsAcrossFoodTemplatesRecipesAndQuickLogs() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-fav",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        isFavorite = true,
                    ),
                    SavedFoodItem(
                        id = "food-other",
                        name = "Plain rice",
                        brand = null,
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(130.0, 2.7, 28.0, 0.3),
                        isFavorite = false,
                    ),
                ),
                templates = listOf(
                    MealTemplate(
                        id = "template-fav",
                        name = "Usual breakfast",
                        mealType = "breakfast",
                        isFavorite = true,
                        items = listOf(MealTemplateItem("food-fav", "Greek yogurt", "Kitchen", 170.0)),
                    ),
                    MealTemplate(
                        id = "template-other",
                        name = "Plain lunch",
                        mealType = "lunch",
                        isFavorite = false,
                        items = listOf(MealTemplateItem("food-other", "Plain rice", null, 100.0)),
                    ),
                ),
                recipes = listOf(
                    Recipe(
                        id = "recipe-fav",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        isFavorite = true,
                        ingredients = listOf(RecipeIngredient("food-fav", "Greek yogurt", "Kitchen", 170.0)),
                        nutritionPerServing = FoodNutrition(420.0, 38.0, 42.0, 11.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
                quickCaloriePresets = listOf(
                    QuickCaloriePreset(
                        id = "quick-fav",
                        name = "Protein snack",
                        caloriesKcal = 320.0,
                        proteinGrams = 22.0,
                        carbsGrams = 36.0,
                        fatGrams = 9.0,
                        isFavorite = true,
                    ),
                    QuickCaloriePreset(
                        id = "quick-other",
                        name = "Old snack",
                        caloriesKcal = 120.0,
                        proteinGrams = 2.0,
                        carbsGrams = 20.0,
                        fatGrams = 3.0,
                        isFavorite = false,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val favorites = viewModel.state.value.favoriteAddItems

        assertEquals(
            listOf(
                FavoriteAddItemType.Food,
                FavoriteAddItemType.MealTemplate,
                FavoriteAddItemType.Recipe,
                FavoriteAddItemType.QuickLog,
            ),
            favorites.map { it.type },
        )
        assertEquals(
            listOf("Greek yogurt", "Usual breakfast", "Chicken bowl", "Protein snack"),
            favorites.map { it.title },
        )
        assertEquals(listOf("food-fav", "template-fav", "recipe-fav", "quick-fav"), favorites.map { it.id })
    }

    @Test
    fun logFood_afterOpeningMealUsesSelectedMeal() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openAddFood("lunch")
        viewModel.onProductNameChanged("Oats")
        viewModel.onCaloriesChanged("380")
        viewModel.onProteinChanged("13")
        viewModel.onCarbsChanged("67")
        viewModel.onFatChanged("7")
        viewModel.onQuantityChanged("50")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("lunch", repository.savedLog?.mealType)
        assertEquals("Logged food", viewModel.state.value.message)
    }

    @Test
    fun openDiaryEntryEditor_populatesFormAndSaveUpdatesSelectedEntry() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = "Kitchen",
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDiaryEntryEditor("entry-1")
        viewModel.onDiaryEntryQuantityChanged("125")
        viewModel.onDiaryEntryMealChanged("dinner")
        viewModel.saveDiaryEntry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            DiaryEntryUpdateInput(
                mealItemId = "entry-1",
                mealType = "dinner",
                quantityGrams = 125.0,
                date = LocalDate.now(),
            ),
            repository.diaryEntryUpdate,
        )
        assertEquals("Updated diary item", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isAddPanelVisible)
    }

    @Test
    fun diaryEntryEditor_updatesMacroPreviewWhenAmountChanges() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = "Kitchen",
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDiaryEntryEditor("entry-1")
        viewModel.onDiaryEntryQuantityChanged("100")

        with(viewModel.state.value.diaryEntryEditor!!) {
            assertEquals(60.0, previewCaloriesKcal, 0.01)
            assertEquals(10.0, previewProteinGrams, 0.01)
            assertEquals(4.0, previewCarbsGrams, 0.01)
            assertEquals(1.0, previewFatGrams, 0.01)
        }
    }

    @Test
    fun diaryEntryEditor_exposesServingChoicesAndSelectionUpdatesPreview() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = "Kitchen",
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                    ),
                ),
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(60.0, 10.0, 4.0, 1.0),
                        servingName = "Cup",
                        servings = listOf(
                            FoodServingOption("serving-1", "Half cup", 85.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDiaryEntryEditor("entry-1")

        val choices = viewModel.state.value.diaryEntryEditor!!.servingChoices
        assertEquals(listOf("100 g", "Cup", "Half cup"), choices.map { it.label })

        viewModel.onDiaryEntryServingChoiceSelected(choices.first { it.label == "Half cup" }.id)

        with(viewModel.state.value.diaryEntryEditor!!) {
            assertEquals("85", quantityGrams)
            assertEquals(51.0, previewCaloriesKcal, 0.01)
            assertEquals(8.5, previewProteinGrams, 0.01)
            assertEquals(3.4, previewCarbsGrams, 0.01)
            assertEquals(0.85, previewFatGrams, 0.01)
        }
    }

    @Test
    fun deleteDiaryEntry_removesSelectedDiaryItem() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDiaryEntryEditor("entry-1")
        with(viewModel.state.value) {
            assertEquals(FoodSheetMode.DiaryEntryEditor, sheetMode)
            val editor = diaryEntryEditor!!
            assertEquals("entry-1", editor.id)
            assertEquals("Greek yogurt", editor.name)
            assertEquals("200", editor.quantityGrams)
        }

        viewModel.deleteDiaryEntry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("entry-1", repository.deletedDiaryEntryId)
        assertEquals("Deleted diary item", viewModel.state.value.message)
        assertFalse(viewModel.state.value.isAddPanelVisible)
    }

    @Test
    fun openFoodDatabase_tracksSearchAndOpensExistingFoodEditor() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 200.0,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = 60.0,
                            proteinGrams = 10.0,
                            carbsGrams = 4.0,
                            fatGrams = 1.0,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openFoodDatabase()
        viewModel.onFoodDatabaseQueryChanged("yog")
        viewModel.openSavedFoodEditor("food-1")

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.SavedFoodEditor, sheetMode)
            assertEquals("yog", foodDatabaseQuery)
            val editor = savedFoodEditor!!
            assertEquals("food-1", editor.id)
            assertEquals("Greek yogurt", editor.name)
            assertEquals("Kitchen", editor.brand)
            assertEquals("200", editor.servingGrams)
            assertEquals("60", editor.caloriesPer100g)
            assertEquals("10", editor.proteinPer100g)
            assertEquals("4", editor.carbsPer100g)
            assertEquals("1", editor.fatPer100g)
        }
    }

    @Test
    fun foodDatabaseQueryFiltersVisibleSavedFoodsByNameBrandBarcodeAndCategory() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-oats",
                        name = "Rolled oats",
                        brand = "Pantry",
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(379.0, 13.0, 68.0, 6.5),
                        barcode = "1112223334445",
                        category = "Grains",
                    ),
                    SavedFoodItem(
                        id = "food-yogurt",
                        name = "Greek yogurt",
                        brand = "DairyCo",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        barcode = "5556667778889",
                        category = "Dairy",
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("Rolled oats", "Greek yogurt"), viewModel.state.value.visibleSavedFoods.map { it.name })

        viewModel.onFoodDatabaseQueryChanged("dairyco")
        assertEquals(listOf("Greek yogurt"), viewModel.state.value.visibleSavedFoods.map { it.name })

        viewModel.onFoodDatabaseQueryChanged("111222")
        assertEquals(listOf("Rolled oats"), viewModel.state.value.visibleSavedFoods.map { it.name })

        viewModel.onFoodDatabaseQueryChanged("grains")
        assertEquals(listOf("Rolled oats"), viewModel.state.value.visibleSavedFoods.map { it.name })
    }

    @Test
    fun saveSavedFood_upsertsEditorValuesAndReturnsToDatabase() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openNewSavedFoodEditor()
        viewModel.onSavedFoodNameChanged("Chicken breast")
        viewModel.onSavedFoodBrandChanged("Kitchen")
        viewModel.onSavedFoodServingChanged("150")
        viewModel.onSavedFoodCaloriesChanged("165")
        viewModel.onSavedFoodProteinChanged("31")
        viewModel.onSavedFoodCarbsChanged("0")
        viewModel.onSavedFoodFatChanged("3.6")
        viewModel.onSavedFoodPotassiumChanged("256")
        viewModel.onSavedFoodCalciumChanged("15")
        viewModel.onSavedFoodIronChanged("0.7")
        viewModel.onSavedFoodVitaminDChanged("0.2")
        viewModel.onSavedFoodVitaminCChanged("0")
        viewModel.onSavedFoodMagnesiumChanged("29")
        viewModel.saveSavedFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            SavedFoodUpsertInput(
                foodId = null,
                name = "Chicken breast",
                brand = "Kitchen",
                defaultServingGrams = 150.0,
                nutritionPer100g = FoodNutrition(
                    caloriesKcal = 165.0,
                    proteinGrams = 31.0,
                    carbsGrams = 0.0,
                    fatGrams = 3.6,
                ),
                nutritionDetailsPer100g = NutritionDetails(
                    potassiumMilligrams = 256.0,
                    calciumMilligrams = 15.0,
                    ironMilligrams = 0.7,
                    vitaminDMicrograms = 0.2,
                    vitaminCMilligrams = 0.0,
                    magnesiumMilligrams = 29.0,
                ),
            ),
            repository.savedFoodUpsert,
        )
        assertEquals(FoodSheetMode.FoodDatabase, viewModel.state.value.sheetMode)
        assertEquals("Saved food", viewModel.state.value.message)
    }

    @Test
    fun deleteSavedFood_deletesCurrentEditorFood() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = null,
                        defaultServingGrams = 200.0,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = 60.0,
                            proteinGrams = 10.0,
                            carbsGrams = 4.0,
                            fatGrams = 1.0,
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openSavedFoodEditor("food-1")
        viewModel.deleteSavedFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("food-1", repository.deletedSavedFoodId)
        assertEquals(FoodSheetMode.FoodDatabase, viewModel.state.value.sheetMode)
        assertEquals("Deleted food", viewModel.state.value.message)
    }

    @Test
    fun saveSavedFood_preservesServingOptionsWhenEditingExistingFood() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = 61.0,
                            proteinGrams = 10.0,
                            carbsGrams = 4.0,
                            fatGrams = 1.0,
                        ),
                        servingName = "Cup",
                        servings = listOf(
                            FoodServingOption("serving-1", "Cup", 170.0),
                            FoodServingOption("serving-2", "Half cup", 85.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openSavedFoodEditor("food-1")
        viewModel.onSavedFoodNameChanged("Greek yogurt plain")
        viewModel.saveSavedFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                FoodServingInput("Cup", 170.0),
                FoodServingInput("Half cup", 85.0),
            ),
            repository.savedFoodUpsert?.servings,
        )
    }

    @Test
    fun duplicateSavedFood_createsEditableCopyWithoutBarcode() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(
                            caloriesKcal = 61.0,
                            proteinGrams = 10.0,
                            carbsGrams = 4.0,
                            fatGrams = 1.0,
                        ),
                        nutritionDetailsPer100g = NutritionDetails(
                            fiberGrams = 0.2,
                            sugarGrams = 3.6,
                            saturatedFatGrams = 0.6,
                            sodiumMilligrams = 45.0,
                        ),
                        servingName = "Cup",
                        barcode = "123456",
                        category = "Dairy",
                        isFavorite = true,
                        servings = listOf(
                            FoodServingOption("serving-1", "Cup", 170.0),
                            FoodServingOption("serving-2", "Half cup", 85.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openSavedFoodEditor("food-1")
        viewModel.duplicateSavedFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            SavedFoodUpsertInput(
                foodId = null,
                name = "Greek yogurt copy",
                brand = "Kitchen",
                defaultServingGrams = 170.0,
                nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                nutritionDetailsPer100g = NutritionDetails(0.2, 3.6, 0.6, 45.0),
                servingName = "Cup",
                barcode = null,
                category = "Dairy",
                isFavorite = true,
                servings = listOf(
                    FoodServingInput("Cup", 170.0),
                    FoodServingInput("Half cup", 85.0),
                ),
            ),
            repository.savedFoodUpsert,
        )
        assertEquals(FoodSheetMode.FoodDatabase, viewModel.state.value.sheetMode)
        assertEquals("Duplicated food", viewModel.state.value.message)
    }

    @Test
    fun openNutritionLabelScan_prefillsEditableExtractedFieldsAndSavesCorrection() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openNutritionLabelScan()

        with(viewModel.state.value) {
            assertTrue(isAddPanelVisible)
            assertEquals(FoodSheetMode.NutritionLabelScan, sheetMode)
            val editor = savedFoodEditor!!
            assertEquals("Scanned label", editor.name)
            assertEquals("100", editor.servingGrams)
            assertEquals("250", editor.caloriesPer100g)
            assertEquals("12", editor.proteinPer100g)
            assertEquals("30", editor.carbsPer100g)
            assertEquals("8", editor.fatPer100g)
            assertEquals("Review extracted nutrition before saving.", message)
        }

        viewModel.onSavedFoodNameChanged("Protein cereal")
        viewModel.onSavedFoodCaloriesChanged("220")
        viewModel.saveSavedFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            SavedFoodUpsertInput(
                foodId = null,
                name = "Protein cereal",
                brand = null,
                defaultServingGrams = 100.0,
                nutritionPer100g = FoodNutrition(220.0, 12.0, 30.0, 8.0),
                nutritionDetailsPer100g = NutritionDetails(),
                servingName = "Label serving",
                barcode = null,
                category = "Nutrition label",
                isFavorite = false,
            ),
            repository.savedFoodUpsert,
        )
        assertEquals(FoodSheetMode.FoodDatabase, viewModel.state.value.sheetMode)
    }

    @Test
    fun dateNavigation_updatesSelectedDateAndLogsAgainstThatDate() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.goToPreviousDay()
        viewModel.openAddFood("breakfast")
        viewModel.selectAddMode(FoodAddMode.Quick)
        viewModel.onQuickCaloriesChanged("250")
        viewModel.quickLog()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocalDate.now().minusDays(1), viewModel.state.value.selectedDate)
        assertEquals(LocalDate.now().minusDays(1), repository.quickLog?.date)

        viewModel.goToToday()

        assertEquals(LocalDate.now(), viewModel.state.value.selectedDate)
    }

    @Test
    fun copyMealFromYesterdayAndSaveTemplate_useSelectedMealAndDate() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openMealDetail("breakfast")
        viewModel.copySelectedMealFromYesterday()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.saveSelectedMealAsTemplate("Usual breakfast")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            CopyMealCall(
                fromDate = LocalDate.now().minusDays(1),
                toDate = LocalDate.now(),
                mealType = "breakfast",
            ),
            repository.copyMealCall,
        )
        assertEquals(
            SaveTemplateCall(
                date = LocalDate.now(),
                mealType = "breakfast",
                name = "Usual breakfast",
            ),
            repository.saveTemplateCall,
        )
    }

    @Test
    fun planningModePlansFoodShowsWeeklyPlanAndMarksEntryLogged() = runTest {
        val targetDate = LocalDate.now().plusDays(1)
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
                    plannedTotals = NutritionTotals(59.0, 10.0, 3.6, 0.4),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = "Example Dairy",
                                    quantityGrams = 100.0,
                                    caloriesKcal = 59.0,
                                    proteinGrams = 10.0,
                                    carbsGrams = 3.6,
                                    fatGrams = 0.4,
                                    status = FoodDiaryEntryStatus.Planned,
                                ),
                            ),
                            totals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
                            plannedTotals = NutritionTotals(59.0, 10.0, 3.6, 0.4),
                        ),
                    ),
                ),
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Example Dairy",
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(59.0, 10.0, 3.6, 0.4),
                    ),
                ),
                weeklyPlan = listOf(
                    FoodPlanDay(
                        date = targetDate,
                        loggedTotals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
                        plannedTotals = NutritionTotals(59.0, 10.0, 3.6, 0.4),
                        loggedEntryCount = 0,
                        plannedEntryCount = 1,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.goToNextDay()
        viewModel.togglePlanningMode()
        viewModel.openAddFood("breakfast")
        viewModel.logSavedFood("food-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isPlanningMode)
        assertEquals(SavedFoodLogInput("food-1", "breakfast", 100.0, targetDate), repository.plannedFoodLog)
        assertEquals(59.0, viewModel.state.value.weeklyPlan.single().plannedCaloriesKcal, 0.01)
        assertTrue(viewModel.state.value.mealSections.single { it.id == "breakfast" }.entries.single().isPlanned)

        viewModel.openDiaryEntryEditor("entry-1")
        viewModel.markDiaryEntryLogged()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("entry-1", repository.markedLoggedEntryId)
        assertEquals("Logged planned food", viewModel.state.value.message)
    }

    @Test
    fun planningModeDoesNotPlanSavedFoodBeyondOneWeekAhead() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Example Dairy",
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(59.0, 10.0, 3.6, 0.4),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        repeat(8) { viewModel.goToNextDay() }
        viewModel.togglePlanningMode()
        viewModel.openAddFood("breakfast")
        viewModel.logSavedFood("food-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocalDate.now().plusDays(8), viewModel.state.value.selectedDate)
        assertEquals(null, repository.plannedFoodLog)
        assertEquals("You can plan up to 1 week ahead.", viewModel.state.value.message)
    }

    @Test
    fun recipeBrowserNextDayStopsAtOneWeekPlanningLimit() = runTest {
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = FakeFoodRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeBrowser()
        repeat(8) { viewModel.goToNextRecipeBrowserDay() }

        assertEquals(LocalDate.now().plusDays(7), viewModel.state.value.recipeBrowserDate)
        assertEquals("You can plan up to 1 week ahead.", viewModel.state.value.message)
    }

    @Test
    fun shoppingListGeneratesAddsManualItemAndTogglesCheckedState() = runTest {
        val startDate = LocalDate.of(2026, 6, 22)
        val endDate = LocalDate.of(2026, 6, 24)
        val repository =
            FakeFoodRepository(
                shoppingGroups = listOf(
                    ShoppingListGroup(
                        category = "Grains",
                        items = listOf(
                            ShoppingListItem(
                                id = "shopping-1",
                                name = "Rice",
                                category = "Grains",
                                quantityGrams = 200.0,
                                isChecked = false,
                                isManual = false,
                            ),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openShoppingList()
        viewModel.onShoppingStartDateChanged(startDate.toString())
        viewModel.onShoppingEndDateChanged(endDate.toString())
        viewModel.generateShoppingList()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodSheetMode.ShoppingList, viewModel.state.value.sheetMode)
        assertEquals(GenerateShoppingListCall(startDate, endDate), repository.generateShoppingListCall)
        assertEquals("Rice", viewModel.state.value.shoppingListGroups.single().items.single().name)

        viewModel.onManualShoppingNameChanged("Sparkling water")
        viewModel.onManualShoppingCategoryChanged("Drinks")
        viewModel.onManualShoppingQuantityChanged("1000")
        viewModel.addManualShoppingListItem()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            ManualShoppingListItemInput(
                name = "Sparkling water",
                category = "Drinks",
                quantityGrams = 1000.0,
            ),
            repository.manualShoppingListItem,
        )

        viewModel.toggleShoppingListItem("shopping-1", true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("shopping-1" to true, repository.toggledShoppingItem)
        assertEquals("Updated shopping item", viewModel.state.value.message)
    }

    @Test
    fun waterTrackingLogsQuickCustomAmountAndUpdatesGoal() = runTest {
        val today = LocalDate.now()
        val repository =
            FakeFoodRepository(
                waterSummary = FoodWaterSummary(
                    date = today,
                    consumedMilliliters = 750.0,
                    goalMilliliters = 2000.0,
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(750.0, viewModel.state.value.waterConsumedMilliliters, 0.01)
        assertEquals(2000.0, viewModel.state.value.waterGoalMilliliters, 0.01)
        assertEquals(0.375, viewModel.state.value.waterProgress, 0.01)
        assertEquals("2000", viewModel.state.value.waterGoalInput)

        viewModel.logQuickWater(250.0)
        dispatcher.scheduler.runCurrent()

        assertEquals(WaterLogInput(today, 250.0), repository.waterLogInput)
        assertEquals("Added 250 ml water", viewModel.state.value.message)

        viewModel.onWaterCustomAmountChanged("333")
        viewModel.logCustomWater()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WaterLogInput(today, 333.0), repository.waterLogInput)
        assertEquals("", viewModel.state.value.waterCustomAmountInput)

        viewModel.onWaterGoalChanged("2400")
        viewModel.saveWaterGoal()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2400.0, repository.waterGoalMilliliters ?: -1.0, 0.01)
        assertEquals("Updated water goal", viewModel.state.value.message)
    }

    @Test
    fun logQuickWater_clearsAddedWaterHintAfterDelay() = runTest {
        val today = LocalDate.now()
        val repository =
            FakeFoodRepository(
                waterSummary = FoodWaterSummary(today, consumedMilliliters = 750.0, goalMilliliters = 2000.0),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.logQuickWater(250.0)
        dispatcher.scheduler.runCurrent()

        assertEquals("Added 250 ml water", viewModel.state.value.message)

        dispatcher.scheduler.advanceTimeBy(2_999)
        dispatcher.scheduler.runCurrent()

        assertEquals("Added 250 ml water", viewModel.state.value.message)

        dispatcher.scheduler.advanceTimeBy(1)
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun removeQuickWater_subtractsAmountAndReportsRemoved() = runTest {
        val today = LocalDate.now()
        val repository =
            FakeFoodRepository(
                waterSummary = FoodWaterSummary(today, consumedMilliliters = 750.0, goalMilliliters = 2000.0),
            )
        repository.waterRemoveResult = 250.0
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.removeQuickWater(250.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WaterLogInput(today, 250.0), repository.waterRemoveInput)
        assertEquals("Removed 250 ml water", viewModel.state.value.message)
    }

    @Test
    fun removeCustomWater_subtractsCustomAmountAndClearsInput() = runTest {
        val today = LocalDate.now()
        val repository =
            FakeFoodRepository(
                waterSummary = FoodWaterSummary(today, consumedMilliliters = 750.0, goalMilliliters = 2000.0),
            )
        repository.waterRemoveResult = 333.0
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onWaterCustomAmountChanged("333")
        viewModel.removeCustomWater()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WaterLogInput(today, 333.0), repository.waterRemoveInput)
        assertEquals("", viewModel.state.value.waterCustomAmountInput)
        assertEquals("Removed 333 ml water", viewModel.state.value.message)
    }

    @Test
    fun removeQuickWater_reportsNothingToRemoveWhenDayIsEmpty() = runTest {
        val today = LocalDate.now()
        val repository =
            FakeFoodRepository(
                waterSummary = FoodWaterSummary(today, consumedMilliliters = 0.0, goalMilliliters = 2000.0),
            )
        repository.waterRemoveResult = 0.0
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.removeQuickWater(250.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WaterLogInput(today, 250.0), repository.waterRemoveInput)
        assertEquals("No water to remove", viewModel.state.value.message)
    }

    @Test
    fun logMealTemplate_logsTemplateIntoSelectedMealAndDate() = runTest {
        val repository =
            FakeFoodRepository(
                templates = listOf(
                    MealTemplate(
                        id = "template-1",
                        name = "Usual breakfast",
                        mealType = "breakfast",
                        items = listOf(MealTemplateItem("food-1", "Oats", null, 50.0)),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAddFood("lunch")
        viewModel.logMealTemplate("template-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            LogTemplateCall(
                templateId = "template-1",
                mealType = "lunch",
                date = LocalDate.now(),
            ),
            repository.logTemplateCall,
        )
        assertEquals("Logged meal template", viewModel.state.value.message)
    }

    @Test
    fun saveFoodGoal_updatesRepositoryAndLocalGoalState() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openGoalEditor()
        viewModel.onGoalCaloriesChanged("2400")
        viewModel.onGoalProteinChanged("180")
        viewModel.onGoalCarbsChanged("250")
        viewModel.onGoalFatChanged("80")
        viewModel.onGoalFiberChanged("35")
        viewModel.onGoalSugarChanged("60")
        viewModel.onGoalSaturatedFatChanged("22")
        viewModel.onGoalSodiumChanged("2300")
        viewModel.onGoalModeChanged(FoodGoalMode.Custom)
        viewModel.onGoalIncludeTrainingChanged(true)
        viewModel.saveFoodGoal()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2400.0, repository.foodGoalUpdate?.dailyCaloriesKcal ?: 0.0, 0.01)
        assertEquals(180.0, repository.foodGoalUpdate?.proteinGrams ?: 0.0, 0.01)
        assertEquals(FoodGoalMode.Custom, repository.foodGoalUpdate?.mode)
        assertTrue(repository.foodGoalUpdate?.includeTrainingCalories == true)
        assertEquals(2400.0, viewModel.state.value.calorieGoalKcal, 0.01)
        assertEquals("Updated nutrition goals", viewModel.state.value.message)
    }

    @Test
    fun selectingHighProteinGoalModeFillsPresetTargetsAndSavesThem() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.openGoalEditor()
        viewModel.onGoalModeChanged(FoodGoalMode.HighProtein)
        viewModel.saveFoodGoal()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodGoalMode.HighProtein, repository.foodGoalUpdate?.mode)
        assertEquals(2083.0, repository.foodGoalUpdate?.dailyCaloriesKcal ?: 0.0, 0.01)
        assertEquals(156.0, repository.foodGoalUpdate?.proteinGrams ?: 0.0, 0.01)
        assertEquals(208.0, repository.foodGoalUpdate?.carbsGrams ?: 0.0, 0.01)
        assertEquals(69.0, repository.foodGoalUpdate?.fatGrams ?: 0.0, 0.01)
        assertEquals(156.0, viewModel.state.value.proteinGoalGrams, 0.01)
    }

    @Test
    fun foodProgramsExposeTargetsHabitsAndPlanningGuidance() = runTest {
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = FakeFoodRepository())
        dispatcher.scheduler.advanceUntilIdle()

        val programs = viewModel.state.value.foodPrograms

        assertEquals(
            listOf(
                "Balanced",
                "High protein",
                "Muscle gain",
                "Weight loss",
                "Keto low carb",
                "Mediterranean-style",
                "Clean eating",
            ),
            programs.map { it.title },
        )
        assertTrue(programs.first { it.title == "Balanced" }.isSelected)
        assertTrue(programs.first { it.title == "High protein" }.macroTargetsLabel.contains("156 g protein"))
        assertTrue(programs.first { it.title == "Mediterranean-style" }.suggestedHabits.any { it.contains("olive", ignoreCase = true) })
        assertTrue(programs.first { it.title == "Clean eating" }.mealPlanningTip.contains("prep", ignoreCase = true))
    }

    @Test
    fun applyingMediterraneanProgramPersistsProgramTargetsAndSelection() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.applyFoodProgram("mediterranean-style")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodGoalMode.MediterraneanStyle, repository.foodGoalUpdate?.mode)
        assertEquals(2083.0, repository.foodGoalUpdate?.dailyCaloriesKcal ?: 0.0, 0.01)
        assertEquals(120.0, repository.foodGoalUpdate?.proteinGrams ?: 0.0, 0.01)
        assertEquals(240.0, repository.foodGoalUpdate?.carbsGrams ?: 0.0, 0.01)
        assertEquals(77.0, repository.foodGoalUpdate?.fatGrams ?: 0.0, 0.01)
        assertTrue(viewModel.state.value.foodPrograms.first { it.id == "mediterranean-style" }.isSelected)
        assertEquals("Applied Mediterranean-style program", viewModel.state.value.message)
    }

    @Test
    fun manualGoalOverrideSwitchesModeToCustomAndPersistsOverride() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.openGoalEditor()
        viewModel.onGoalModeChanged(FoodGoalMode.WeightLoss)
        viewModel.onGoalCaloriesChanged("1900")
        viewModel.saveFoodGoal()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodGoalMode.Custom, repository.foodGoalUpdate?.mode)
        assertEquals(1900.0, repository.foodGoalUpdate?.dailyCaloriesKcal ?: 0.0, 0.01)
        assertEquals(1900.0, viewModel.state.value.calorieGoalKcal, 0.01)
    }

    @Test
    fun diaryProgressUsesSelectedGoalModeTargets() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(
                        caloriesKcal = 500.0,
                        proteinGrams = 50.0,
                        carbsGrams = 80.0,
                        fatGrams = 20.0,
                    ),
                    meals = emptyList(),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openGoalEditor()
        viewModel.onGoalModeChanged(FoodGoalMode.HighProtein)
        viewModel.saveFoodGoal()
        dispatcher.scheduler.advanceUntilIdle()

        val proteinProgress = viewModel.state.value.macroProgress.first { it.label == "Protein" }
        assertEquals(50.0, proteinProgress.currentGrams, 0.01)
        assertEquals(156.0, proteinProgress.goalGrams, 0.01)
        assertEquals(1583.0, viewModel.state.value.remainingCaloriesKcal, 0.01)
    }

    @Test
    fun netCarbsGoalTogglePersistsAndSubtractsFiberFromDiaryCarbs() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(
                        caloriesKcal = 700.0,
                        proteinGrams = 42.0,
                        carbsGrams = 90.0,
                        fatGrams = 21.0,
                    ),
                    meals = emptyList(),
                    detailTotals = NutritionDetails(fiberGrams = 24.0),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Carbs", viewModel.state.value.macroProgress.first().label)

        viewModel.openGoalEditor()
        viewModel.onGoalUseNetCarbsChanged(true)
        viewModel.saveFoodGoal()
        dispatcher.scheduler.advanceUntilIdle()

        val carbProgress = viewModel.state.value.macroProgress.first { it.label == "Net carbs" }
        assertTrue(repository.foodGoalUpdate?.useNetCarbs == true)
        assertTrue(viewModel.state.value.useNetCarbs)
        assertTrue(viewModel.state.value.goalEditor.useNetCarbsInput)
        assertEquals(66.0, carbProgress.currentGrams, 0.01)
        assertEquals(260.0, carbProgress.goalGrams, 0.01)
    }

    @Test
    fun diaryAdvancedNutritionProgressUsesGoalsAndLimits() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(
                        caloriesKcal = 900.0,
                        proteinGrams = 70.0,
                        carbsGrams = 120.0,
                        fatGrams = 30.0,
                    ),
                    meals = emptyList(),
                    detailTotals = NutritionDetails(
                        fiberGrams = 18.0,
                        sugarGrams = 62.0,
                        saturatedFatGrams = 12.0,
                        sodiumMilligrams = 1800.0,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        val nutrients = viewModel.state.value.advancedNutritionProgress.associateBy { it.label }

        with(requireNotNull(nutrients["Fiber"])) {
            assertEquals(18.0, currentValue, 0.01)
            assertEquals(30.0, goalValue, 0.01)
            assertEquals("g", unit)
            assertFalse(isLimit)
        }
        with(requireNotNull(nutrients["Sugar"])) {
            assertEquals(62.0, currentValue, 0.01)
            assertEquals(50.0, goalValue, 0.01)
            assertEquals("g", unit)
            assertTrue(isLimit)
        }
        with(requireNotNull(nutrients["Sat fat"])) {
            assertEquals(12.0, currentValue, 0.01)
            assertEquals(20.0, goalValue, 0.01)
            assertEquals("g", unit)
            assertTrue(isLimit)
        }
        with(requireNotNull(nutrients["Sodium"])) {
            assertEquals(1800.0, currentValue, 0.01)
            assertEquals(2300.0, goalValue, 0.01)
            assertEquals("mg", unit)
            assertTrue(isLimit)
        }
    }

    @Test
    fun mealDetailAdvancedNutritionProgressUsesMealTotalsAndGoals() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(480.0, 35.0, 50.0, 14.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = emptyList(),
                            totals = NutritionTotals(480.0, 35.0, 50.0, 14.0),
                            detailTotals = NutritionDetails(
                                fiberGrams = 9.0,
                                sugarGrams = 16.0,
                                saturatedFatGrams = 4.0,
                                sodiumMilligrams = 610.0,
                            ),
                        ),
                    ),
                    detailTotals = NutritionDetails(
                        fiberGrams = 9.0,
                        sugarGrams = 16.0,
                        saturatedFatGrams = 4.0,
                        sodiumMilligrams = 610.0,
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openMealDetail("breakfast")

        val meal = requireNotNull(viewModel.state.value.selectedMealDetailForDisplay())
        val nutrients = meal.advancedNutritionProgress.associateBy { it.label }

        assertEquals(9.0, requireNotNull(nutrients["Fiber"]).currentValue, 0.01)
        assertEquals(30.0, requireNotNull(nutrients["Fiber"]).goalValue, 0.01)
        assertFalse(requireNotNull(nutrients["Fiber"]).isLimit)
        assertEquals(16.0, requireNotNull(nutrients["Sugar"]).currentValue, 0.01)
        assertEquals(50.0, requireNotNull(nutrients["Sugar"]).goalValue, 0.01)
        assertTrue(requireNotNull(nutrients["Sugar"]).isLimit)
        assertEquals(610.0, requireNotNull(nutrients["Sodium"]).currentValue, 0.01)
        assertEquals(2300.0, requireNotNull(nutrients["Sodium"]).goalValue, 0.01)
    }

    @Test
    fun deleteDiaryEntryCanBeUndoneByReloggingFood() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDiaryEntryEditor("entry-1")
        viewModel.deleteDiaryEntry()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.undoDeleteDiaryEntry()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("entry-1", repository.deletedDiaryEntryId)
        assertEquals(
            SavedFoodLogInput(
                foodId = "food-1",
                mealType = "breakfast",
                quantityGrams = 200.0,
                date = LocalDate.now(),
            ),
            repository.savedFoodLog,
        )
        assertEquals("Restored diary item", viewModel.state.value.message)
    }

    @Test
    fun keepAddingSavedFoodsLeavesAddPanelOpen() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )

        viewModel.openAddFood("dinner")
        viewModel.onKeepAddingFoodsChanged(true)
        viewModel.logSavedFood("food-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isAddPanelVisible)
        assertEquals("Logged food", viewModel.state.value.message)
    }

    @Test
    fun saveRecipeAndLogRecipe_useEditorIngredientsAndSelectedMeal() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Chicken",
                        brand = null,
                        defaultServingGrams = 150.0,
                        nutritionPer100g = FoodNutrition(165.0, 31.0, 0.0, 3.6),
                    ),
                ),
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(
            provider = FakeProductProvider(),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeEditor()
        viewModel.onRecipeNameChanged("Chicken bowl")
        viewModel.onRecipeCategoryChanged("Dinner")
        viewModel.onRecipeServingNameChanged("Bowl")
        viewModel.onRecipeServingGramsChanged("350")
        viewModel.onRecipeIngredientFoodChanged("food-1")
        viewModel.onRecipeIngredientQuantityChanged("150")
        viewModel.addRecipeIngredient()
        viewModel.saveRecipe()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.openAddFood("dinner")
        viewModel.logRecipe("recipe-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Chicken bowl", repository.recipeUpsert?.name)
        assertEquals(listOf("food-1"), repository.recipeUpsert?.ingredients?.map { it.foodId })
        assertEquals(150.0, repository.recipeUpsert?.ingredients?.single()?.quantityGrams ?: 0.0, 0.01)
        assertEquals(
            LogRecipeCall(
                recipeId = "recipe-1",
                mealType = "dinner",
                servings = 1.0,
                date = LocalDate.now(),
            ),
            repository.logRecipeCall,
        )
        assertEquals("Logged recipe", viewModel.state.value.message)
    }

    @Test
    fun searchOnlineFoods_exposesRemoteResultsAndSavingResultCreatesSavedFood() = runTest {
        val repository = FakeFoodRepository()
        val viewModel =
            FoodViewModel(
                provider = FakeProductProvider(
                    searchResult = ProductSearchResult.Success(
                        query = "oats",
                        products = listOf(
                            foundProduct(
                                barcode = "5000108236832",
                                name = "Oats so simple",
                                brand = "Quaker",
                                nutrition = FoodNutrition(379.0, 8.9, 68.0, 6.4),
                            ).copy(
                                nutritionDetailsPer100g = NutritionDetails(
                                    fiberGrams = 7.2,
                                    sugarGrams = 19.0,
                                    saturatedFatGrams = 1.2,
                                    sodiumMilligrams = 20.0,
                                ),
                                category = "Breakfast cereals",
                                imageUrl = "https://images.openfoodfacts.org/oats.jpg",
                            ),
                        ),
                    ),
                ),
                repository = repository,
            )

        viewModel.openFoodDatabase()
        viewModel.onFoodDatabaseQueryChanged("oats")
        viewModel.searchOnlineFoods()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Oats so simple", viewModel.state.value.onlineFoodResults.single().name)
        assertEquals("Quaker", viewModel.state.value.onlineFoodResults.single().brand)
        assertEquals(379.0, viewModel.state.value.onlineFoodResults.single().caloriesPer100g, 0.01)

        viewModel.saveOnlineFoodResult("5000108236832")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Saved online food", viewModel.state.value.message)
        assertEquals("Oats so simple", repository.savedFoodUpsert?.name)
        assertEquals("5000108236832", repository.savedFoodUpsert?.barcode)
        assertEquals("Breakfast cereals", repository.savedFoodUpsert?.category)
        assertEquals(7.2, repository.savedFoodUpsert?.nutritionDetailsPer100g?.fiberGrams ?: 0.0, 0.01)
    }

    @Test
    fun searchOnlineFoodsHandlesUnexpectedProviderFailure() = runTest {
        val viewModel =
            FoodViewModel(
                provider = ThrowingSearchProductProvider(),
                repository = FakeFoodRepository(),
            )

        viewModel.openFoodDatabase()
        viewModel.onFoodDatabaseQueryChanged("banana")
        viewModel.searchOnlineFoods()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSearchingFoods)
        assertTrue(viewModel.state.value.onlineFoodResults.isEmpty())
        assertEquals("Online food search failed", viewModel.state.value.message)
    }

    @Test
    fun selectingSavedFoodServing_updatesAmountUsedForLogging() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = null,
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        servings = listOf(
                            com.musfit.data.repository.FoodServingOption("serving-1", "Cup", 170.0),
                            com.musfit.data.repository.FoodServingOption("serving-2", "Half cup", 85.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAddFood("breakfast")
        viewModel.onSavedFoodServingSelected("food-1", 85.0)
        viewModel.logSavedFood("food-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("85", viewModel.state.value.savedFoodQuantityGrams)
        assertEquals(85.0, repository.savedFoodLog?.quantityGrams ?: 0.0, 0.01)
    }

    @Test
    fun foodDetailOpensReadOnlyNutritionBeforeEditing() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        nutritionDetailsPer100g = NutritionDetails(fiberGrams = 0.2, sugarGrams = 3.6),
                        category = "Dairy",
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openSavedFoodDetail("food-1")

        assertEquals(FoodSheetMode.FoodDetail, viewModel.state.value.sheetMode)
        assertEquals("Greek yogurt", viewModel.state.value.selectedSavedFoodDetail?.name)
        assertEquals("Dairy", viewModel.state.value.selectedSavedFoodDetail?.category)
        assertEquals(3.6, viewModel.state.value.selectedSavedFoodDetail?.sugarPer100g ?: 0.0, 0.01)
    }

    @Test
    fun templateManagementRenamesDuplicatesAndDeletesFromViewModel() = runTest {
        val repository =
            FakeFoodRepository(
                templates = listOf(
                    MealTemplate(
                        id = "template-1",
                        name = "Usual breakfast",
                        mealType = "breakfast",
                        items = listOf(MealTemplateItem("food-1", "Oats", null, 40.0)),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openMealTemplateEditor("template-1")
        viewModel.onTemplateNameChanged("Gym breakfast")
        viewModel.onTemplateMealTypeChanged("snacks")
        viewModel.saveMealTemplateEdits()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.duplicateMealTemplate("template-1")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteMealTemplate("template-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("template-1" to "Gym breakfast", repository.renameTemplateCall?.let { it.templateId to it.name })
        assertEquals("snacks", repository.renameTemplateCall?.mealType)
        assertEquals("template-1", repository.duplicatedTemplateId)
        assertEquals("template-1", repository.deletedTemplateId)
    }

    @Test
    fun mealTemplateEditorEditsRemovesAddsItemsAndSavesTemplateUpdate() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Oats",
                        brand = null,
                        defaultServingGrams = 40.0,
                        nutritionPer100g = FoodNutrition(389.0, 16.9, 66.3, 6.9),
                    ),
                    SavedFoodItem(
                        id = "food-2",
                        name = "Yogurt",
                        brand = null,
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                    ),
                    SavedFoodItem(
                        id = "food-3",
                        name = "Eggs",
                        brand = null,
                        defaultServingGrams = 100.0,
                        nutritionPer100g = FoodNutrition(143.0, 12.6, 0.7, 9.5),
                    ),
                ),
                templates = listOf(
                    MealTemplate(
                        id = "template-1",
                        name = "Usual breakfast",
                        mealType = "breakfast",
                        items = listOf(
                            MealTemplateItem("food-1", "Oats", null, 40.0),
                            MealTemplateItem("food-2", "Yogurt", null, 170.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openMealTemplateEditor("template-1")
        viewModel.onTemplateNameChanged("Gym breakfast")
        viewModel.onTemplateMealTypeChanged("snacks")
        viewModel.onTemplateDraftItemQuantityChanged(0, "60")
        viewModel.removeTemplateDraftItem(1)
        viewModel.onTemplateItemFoodChanged("food-3")
        viewModel.onTemplateNewItemQuantityChanged("200")
        viewModel.addTemplateItem()
        viewModel.saveMealTemplateEdits()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            MealTemplateUpdateInput(
                templateId = "template-1",
                name = "Gym breakfast",
                mealType = "snacks",
                items = listOf(
                    MealTemplateItemInput("food-1", 60.0),
                    MealTemplateItemInput("food-3", 200.0),
                ),
            ),
            repository.templateUpdate,
        )
        assertEquals("Updated meal template", viewModel.state.value.message)
    }

    @Test
    fun toggleFavoriteMealTemplateUpdatesRepositoryAndUiState() = runTest {
        val repository =
            FakeFoodRepository(
                templates = listOf(
                    MealTemplate(
                        id = "template-1",
                        name = "Usual breakfast",
                        mealType = "breakfast",
                        isFavorite = false,
                        items = listOf(MealTemplateItem("food-1", "Oats", null, 40.0)),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.mealTemplates.single().isFavorite)

        viewModel.toggleFavoriteMealTemplate("template-1", true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("template-1" to true, repository.favoriteTemplateToggle)
        assertEquals("Template added to favorites", viewModel.state.value.message)
    }

    @Test
    fun recipeManagementLoadsRecipeForEditAndDeletesRecipe() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Chicken",
                        brand = null,
                        defaultServingGrams = 150.0,
                        nutritionPer100g = FoodNutrition(165.0, 31.0, 0.0, 3.6),
                    ),
                ),
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeEditor("recipe-1")
        viewModel.onRecipeNameChanged("Chicken power bowl")
        viewModel.saveRecipe()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteRecipe("recipe-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("recipe-1", repository.recipeUpsert?.recipeId)
        assertEquals("Chicken power bowl", repository.recipeUpsert?.name)
        assertEquals("recipe-1", repository.deletedRecipeId)
    }

    @Test
    fun recipeEditorUsesIngredientServingChoicesAndCookedYieldInputs() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Protein powder",
                        brand = null,
                        defaultServingGrams = 60.0,
                        nutritionPer100g = FoodNutrition(400.0, 70.0, 12.0, 8.0),
                        servingName = "Serving",
                        servings = listOf(
                            FoodServingOption(id = "serving-scoop", label = "Scoop", grams = 30.0),
                            FoodServingOption(id = "serving-package", label = "Package", grams = 120.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeEditor()
        viewModel.onRecipeNameChanged("Protein bites")
        viewModel.onRecipeServingNameChanged("Bite")
        viewModel.onRecipeServingsCountChanged("8")
        viewModel.onRecipeCookedYieldGramsChanged("240")
        viewModel.onRecipeIngredientFoodChanged("food-1")

        assertEquals(
            listOf("g", "Serving", "Scoop", "Package"),
            viewModel.state.value.recipeEditor!!.ingredientServingChoices.map { it.label },
        )

        viewModel.onRecipeIngredientServingChoiceSelected("serving-scoop")
        viewModel.onRecipeIngredientQuantityChanged("2")
        viewModel.addRecipeIngredient()
        viewModel.saveRecipe()
        dispatcher.scheduler.advanceUntilIdle()

        val recipe = requireNotNull(repository.recipeUpsert)
        val ingredient = recipe.ingredients.single()
        assertEquals("Protein bites", recipe.name)
        assertEquals("Bite", recipe.servingName)
        assertEquals(8.0, recipe.servings, 0.01)
        assertEquals(240.0, recipe.cookedYieldGrams, 0.01)
        assertEquals(30.0, recipe.servingGrams, 0.01)
        assertEquals("food-1", ingredient.foodId)
        assertEquals(60.0, ingredient.quantityGrams, 0.01)
        assertEquals("Scoop", ingredient.unitLabel)
        assertEquals(30.0, ingredient.unitGrams, 0.01)
        assertEquals(2.0, ingredient.unitQuantity, 0.01)
    }

    @Test
    fun duplicateRecipeCallsRepositoryWithCopyName() = runTest {
        val repository =
            FakeFoodRepository(
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 180.0,
                        servings = 4.0,
                        cookedYieldGrams = 720.0,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 300.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.duplicateRecipe("recipe-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DuplicateRecipeCall("recipe-1", "Chicken bowl copy"), repository.duplicateRecipeCall)
        assertEquals("Duplicated recipe", viewModel.state.value.message)
    }

    @Test
    fun toggleFavoriteRecipeUpdatesRepositoryAndUiState() = runTest {
        val repository =
            FakeFoodRepository(
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        isFavorite = false,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.recipes.single().isFavorite)

        viewModel.toggleFavoriteRecipe("recipe-1", true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("recipe-1" to true, repository.favoriteRecipeToggle)
        assertEquals("Recipe added to favorites", viewModel.state.value.message)
    }

    @Test
    fun recipeDiscoveryFiltersLocalCatalogSavedFavoritesAndPrograms() = runTest {
        val repository =
            FakeFoodRepository(
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        isFavorite = true,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                    Recipe(
                        id = "recipe-2",
                        name = "Pasta plate",
                        category = "Dinner",
                        servingName = "Plate",
                        servingGrams = 420.0,
                        isFavorite = false,
                        ingredients = listOf(RecipeIngredient("food-2", "Pasta", null, 160.0)),
                        nutritionPerServing = FoodNutrition(560.0, 18.0, 82.0, 16.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.recipeDiscovery.items.any { it.sourceRecipeId == "recipe-1" && it.isSavedRecipe })
        assertTrue(viewModel.state.value.recipeDiscovery.items.any { it.id.startsWith("catalog-") })

        viewModel.selectRecipeDiscoveryFilter(RecipeDiscoveryFilter.HighProtein)

        val highProteinItems = viewModel.state.value.recipeDiscovery.visibleItems
        assertTrue(highProteinItems.any { it.title == "Chicken bowl" })
        assertTrue(highProteinItems.all { "High protein" in it.tagLabels })

        viewModel.selectRecipeDiscoveryFilter(RecipeDiscoveryFilter.Favorites)

        assertEquals(listOf("Chicken bowl"), viewModel.state.value.recipeDiscovery.visibleItems.map { it.title })

        viewModel.applyFoodProgram("mediterranean-style")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.selectRecipeDiscoveryFilter(RecipeDiscoveryFilter.Program)

        val programItems = viewModel.state.value.recipeDiscovery.visibleItems
        assertTrue(programItems.any { it.title == "Mediterranean chickpea bowl" })
        assertTrue(programItems.all { it.programRelevant })
    }

    @Test
    fun openRecipeBrowserStartsInDedicatedBrowserMode() = runTest {
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = FakeFoodRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeBrowser()

        assertTrue(viewModel.state.value.isAddPanelVisible)
        assertEquals(FoodSheetMode.RecipeBrowser, viewModel.state.value.sheetMode)
        assertNull(viewModel.state.value.recipeEditor)
    }

    @Test
    fun usingRecipeDiscoveryItemLogsSavedRecipeOrPrefillsCatalogRecipe() = runTest {
        val repository =
            FakeFoodRepository(
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAddFood("lunch")
        val savedRecipeDiscoveryId = viewModel.state.value.recipeDiscovery.items.single { it.sourceRecipeId == "recipe-1" }.id
        viewModel.useRecipeDiscoveryItem(savedRecipeDiscoveryId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            LogRecipeCall(
                recipeId = "recipe-1",
                mealType = "lunch",
                servings = 1.0,
                date = LocalDate.now(),
            ),
            repository.logRecipeCall,
        )

        viewModel.useRecipeDiscoveryItem("catalog-mediterranean-chickpea-bowl")

        val editor = requireNotNull(viewModel.state.value.recipeEditor)
        assertEquals(FoodSheetMode.RecipeEditor, viewModel.state.value.sheetMode)
        assertEquals("Mediterranean chickpea bowl", editor.name)
        assertEquals("Vegetarian", editor.category)
        assertEquals("Bowl", editor.servingName)
        assertEquals("Review and save Mediterranean chickpea bowl", viewModel.state.value.message)
        assertTrue(editor.ingredients.isEmpty())
    }

    @Test
    fun recipeBrowserPlansSavedRecipeForChosenDayAndMeal() = runTest {
        val targetDate = LocalDate.now().plusDays(1)
        val repository =
            FakeFoodRepository(
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Dinner",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(250.0, 35.0, 20.0, 5.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeBrowser()
        viewModel.goToNextRecipeBrowserDay()
        viewModel.onRecipeBrowserMealChanged("dinner")
        viewModel.onRecipeServingsToLogChanged("1.5")
        viewModel.planRecipe("recipe-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodSheetMode.RecipeBrowser, viewModel.state.value.sheetMode)
        assertEquals(targetDate, viewModel.state.value.recipeBrowserDate)
        assertEquals("dinner", viewModel.state.value.recipeBrowserMealType)
        assertEquals(
            PlanRecipeCall(
                recipeId = "recipe-1",
                mealType = "dinner",
                servings = 1.5,
                date = targetDate,
            ),
            repository.planRecipeCall,
        )
        assertEquals("Planned recipe", viewModel.state.value.message)
    }

    @Test
    fun recipeBrowserLogsSavedRecipeForChosenDayAndMeal() = runTest {
        val targetDate = LocalDate.now().plusDays(1)
        val repository =
            FakeFoodRepository(
                recipes = listOf(
                    Recipe(
                        id = "recipe-1",
                        name = "Chicken bowl",
                        category = "Lunch",
                        servingName = "Bowl",
                        servingGrams = 350.0,
                        ingredients = listOf(RecipeIngredient("food-1", "Chicken", null, 150.0)),
                        nutritionPerServing = FoodNutrition(420.0, 38.0, 42.0, 11.0),
                        detailNutritionPerServing = NutritionDetails(),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openRecipeBrowser()
        viewModel.goToNextRecipeBrowserDay()
        viewModel.onRecipeBrowserMealChanged("lunch")
        viewModel.logRecipeFromBrowser("recipe-1")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FoodSheetMode.RecipeBrowser, viewModel.state.value.sheetMode)
        assertEquals(targetDate, viewModel.state.value.recipeBrowserDate)
        assertEquals("lunch", viewModel.state.value.recipeBrowserMealType)
        assertEquals(
            LogRecipeCall(
                recipeId = "recipe-1",
                mealType = "lunch",
                servings = 1.0,
                date = targetDate,
            ),
            repository.logRecipeCall,
        )
        assertEquals("Logged recipe", viewModel.state.value.message)
    }

    @Test
    fun recipeDiscoverySearchFiltersByTitleMealAndTags() = runTest {
        val viewModel =
            FoodViewModel(
                provider = FakeProductProvider(),
                repository = FakeFoodRepository(
                    recipes = listOf(
                        Recipe(
                            id = "recipe-1",
                            name = "Salmon breakfast bowl",
                            category = "Breakfast",
                            servingName = "Bowl",
                            servingGrams = 350.0,
                            ingredients = listOf(RecipeIngredient("food-1", "Salmon", null, 150.0)),
                            nutritionPerServing = FoodNutrition(420.0, 38.0, 42.0, 11.0),
                            detailNutritionPerServing = NutritionDetails(),
                        ),
                        Recipe(
                            id = "recipe-2",
                            name = "Plain rice bowl",
                            category = "Lunch",
                            servingName = "Bowl",
                            servingGrams = 300.0,
                            ingredients = listOf(RecipeIngredient("food-1", "Rice", null, 150.0)),
                            nutritionPerServing = FoodNutrition(300.0, 6.0, 64.0, 2.0),
                            detailNutritionPerServing = NutritionDetails(),
                        ),
                    ),
                ),
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRecipeDiscoveryQueryChanged("salmon breakfast")

        assertEquals(
            listOf("Salmon breakfast bowl"),
            viewModel.state.value.recipeDiscovery.visibleItems.map { it.title },
        )
    }

    @Test
    fun diaryEntryCanBeCopiedToAnotherMealAndDate() = runTest {
        val repository =
            FakeFoodRepository(
                diary = FoodDiary(
                    totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                    meals = listOf(
                        FoodDiaryMeal(
                            type = "breakfast",
                            entries = listOf(
                                FoodDiaryEntry(
                                    id = "entry-1",
                                    foodId = "food-1",
                                    name = "Greek yogurt",
                                    brand = null,
                                    quantityGrams = 200.0,
                                    caloriesKcal = 120.0,
                                    proteinGrams = 20.0,
                                    carbsGrams = 8.0,
                                    fatGrams = 2.0,
                                ),
                            ),
                            totals = NutritionTotals(120.0, 20.0, 8.0, 2.0),
                        ),
                    ),
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openDiaryEntryEditor("entry-1")
        viewModel.copyDiaryEntryTo("dinner", viewModel.state.value.selectedDate.plusDays(1))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            CopyDiaryEntryCall("entry-1", "dinner", LocalDate.now().plusDays(1)),
            repository.copyDiaryEntryCall,
        )
        assertEquals("Copied diary item", viewModel.state.value.message)
    }

    @Test
    fun barcodeNotFoundKeepsBarcodeAndPrefillsCreateFlow() = runTest {
        val viewModel =
            FoodViewModel(
                provider = FakeProductProvider(result = ProductLookupResult.NotFound("999")),
                repository = FakeFoodRepository(),
            )

        viewModel.onBarcodeChanged("999")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("999", viewModel.state.value.barcode)
        assertEquals("Barcode 999", viewModel.state.value.productName)
        assertEquals("100", viewModel.state.value.quantityGrams)
        assertEquals("Product not found. Add details to create it.", viewModel.state.value.message)
    }

    @Test
    fun saveScannedProductToDatabase_afterBarcodeMissCreatesSavedFoodWithoutLogging() = runTest {
        val repository = FakeFoodRepository()
        val viewModel =
            FoodViewModel(
                provider = FakeProductProvider(result = ProductLookupResult.NotFound("999")),
                repository = repository,
            )

        viewModel.onBarcodeChanged("999")
        viewModel.lookupBarcode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onProductNameChanged("Tahini bar")
        viewModel.onBrandChanged("Kitchen")
        viewModel.onQuantityChanged("45")
        viewModel.onCaloriesChanged("430")
        viewModel.onProteinChanged("12")
        viewModel.onCarbsChanged("22")
        viewModel.onFatChanged("31")
        viewModel.saveScannedProductToDatabase()
        dispatcher.scheduler.advanceUntilIdle()

        val savedFood = requireNotNull(repository.savedFoodUpsert)
        assertNull(repository.savedLog)
        assertEquals("Saved product to database", viewModel.state.value.message)
        assertEquals("Tahini bar", savedFood.name)
        assertEquals("Kitchen", savedFood.brand)
        assertEquals("999", savedFood.barcode)
        assertEquals("45 g", savedFood.servingName)
        assertEquals(45.0, savedFood.defaultServingGrams, 0.01)
        assertEquals(FoodNutrition(430.0, 12.0, 22.0, 31.0), savedFood.nutritionPer100g)
    }

    @Test
    fun barcodeComparisonUsesSavedFoodAndRemoteLookupWithMacroHighlights() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        nutritionDetailsPer100g = NutritionDetails(sugarGrams = 3.6, sodiumMilligrams = 36.0),
                        barcode = "111",
                    ),
                ),
            )
        val viewModel =
            FoodViewModel(
                provider = FakeProductProvider(
                    resultsByBarcode = mapOf(
                        "222" to foundProduct(
                            barcode = "222",
                            name = "Protein bar",
                            brand = "Fuel",
                            nutrition = FoodNutrition(240.0, 20.0, 22.0, 8.0),
                        ).copy(
                            nutritionDetailsPer100g = NutritionDetails(sugarGrams = 9.0, sodiumMilligrams = 180.0),
                        ),
                    ),
                ),
                repository = repository,
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBarcodeComparison()
        viewModel.onBarcodeComparisonBarcodeChanged(BarcodeComparisonSide.Left, "111abc")
        viewModel.onBarcodeComparisonBarcodeChanged(BarcodeComparisonSide.Right, "222")
        viewModel.compareBarcodeProducts()
        dispatcher.scheduler.advanceUntilIdle()

        val comparison = viewModel.state.value.barcodeComparison
        assertEquals(FoodSheetMode.BarcodeComparison, viewModel.state.value.sheetMode)
        assertEquals("111", comparison.leftBarcodeInput)
        assertEquals("222", comparison.rightBarcodeInput)
        assertEquals("Greek yogurt", comparison.leftItem?.name)
        assertEquals("Saved food", comparison.leftItem?.sourceLabel)
        assertEquals("Protein bar", comparison.rightItem?.name)
        assertEquals("Open Food Facts", comparison.rightItem?.sourceLabel)
        assertEquals(BarcodeComparisonSide.Right, comparison.highlights.first { it.label == "Protein" }.winnerSide)
        assertEquals(BarcodeComparisonSide.Left, comparison.highlights.first { it.label == "Sugar" }.winnerSide)
        assertEquals("Compared barcode products", viewModel.state.value.message)
    }

    @Test
    fun barcodeComparisonCanCompareTwoSavedFoodsWithoutRemoteLookup() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Oats",
                        brand = "Pantry",
                        defaultServingGrams = 40.0,
                        nutritionPer100g = FoodNutrition(389.0, 17.0, 66.0, 7.0),
                        barcode = "111",
                    ),
                    SavedFoodItem(
                        id = "food-2",
                        name = "Granola",
                        brand = "Pantry",
                        defaultServingGrams = 45.0,
                        nutritionPer100g = FoodNutrition(470.0, 10.0, 62.0, 18.0),
                        barcode = "222",
                    ),
                ),
            )
        val provider = FakeProductProvider(resultsByBarcode = emptyMap())
        val viewModel = FoodViewModel(provider = provider, repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBarcodeComparison()
        viewModel.onBarcodeComparisonBarcodeChanged(BarcodeComparisonSide.Left, "111")
        viewModel.onBarcodeComparisonBarcodeChanged(BarcodeComparisonSide.Right, "222")
        viewModel.compareBarcodeProducts()
        dispatcher.scheduler.advanceUntilIdle()

        val comparison = viewModel.state.value.barcodeComparison
        assertEquals("Oats", comparison.leftItem?.name)
        assertEquals("Granola", comparison.rightItem?.name)
        assertEquals(listOf("111", "222"), comparison.items.map { it.barcode })
        assertTrue(provider.lookupCalls.isEmpty())
    }

    @Test
    fun barcodeComparisonDoesNotCrownMissingNutrientAsHealthier() = runTest {
        val repository =
            FakeFoodRepository(
                savedFoods = listOf(
                    SavedFoodItem(
                        id = "food-1",
                        name = "Greek yogurt",
                        brand = "Kitchen",
                        defaultServingGrams = 170.0,
                        nutritionPer100g = FoodNutrition(61.0, 10.0, 4.0, 1.0),
                        nutritionDetailsPer100g = NutritionDetails(sugarGrams = 4.0, sodiumMilligrams = 50.0),
                        barcode = "111",
                    ),
                ),
            )
        val viewModel =
            FoodViewModel(
                provider = FakeProductProvider(
                    resultsByBarcode = mapOf(
                        // Open Food Facts reports this product's sodium as absent, which the provider maps to 0.0.
                        "222" to foundProduct(
                            barcode = "222",
                            name = "Mystery bar",
                            brand = "Fuel",
                            nutrition = FoodNutrition(240.0, 20.0, 22.0, 8.0),
                        ).copy(
                            nutritionDetailsPer100g = NutritionDetails(sugarGrams = 9.0, sodiumMilligrams = 0.0),
                        ),
                    ),
                ),
                repository = repository,
            )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openBarcodeComparison()
        viewModel.onBarcodeComparisonBarcodeChanged(BarcodeComparisonSide.Left, "111")
        viewModel.onBarcodeComparisonBarcodeChanged(BarcodeComparisonSide.Right, "222")
        viewModel.compareBarcodeProducts()
        dispatcher.scheduler.advanceUntilIdle()

        val comparison = viewModel.state.value.barcodeComparison
        // Right product's sodium is 0.0 (unknown), so neither side is crowned the healthier on sodium.
        assertTrue(comparison.highlights.first { it.label == "Sodium" }.winnerSide == null)
        // Sugar is populated on both sides, so the lower one still wins.
        assertEquals(BarcodeComparisonSide.Left, comparison.highlights.first { it.label == "Sugar" }.winnerSide)
    }

    @Test
    fun fastingProgramsExposePresetWindowsAndStartTimeSchedule() = runTest {
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = FakeFoodRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openFastingTimer()

        assertEquals(FoodSheetMode.FastingTimer, viewModel.state.value.sheetMode)
        assertEquals(
            listOf("12-12", "14-10", "16-8", "custom"),
            viewModel.state.value.fastingTimer.programs.map { it.id },
        )
        assertEquals("16-8", viewModel.state.value.fastingTimer.selectedProgramId)
        assertEquals("20:00 - 12:00", viewModel.state.value.fastingTimer.fastingWindowLabel)
        assertEquals("12:00 - 20:00", viewModel.state.value.fastingTimer.eatingWindowLabel)
        assertEquals(16.0 / 24.0, viewModel.state.value.fastingTimer.progress, 0.01)

        viewModel.selectFastingProgram("14-10")
        viewModel.onFastingStartTimeChanged("21:30")

        assertEquals("14-10", viewModel.state.value.fastingTimer.selectedProgramId)
        assertEquals("21:30 - 11:30", viewModel.state.value.fastingTimer.fastingWindowLabel)
        assertEquals("11:30 - 21:30", viewModel.state.value.fastingTimer.eatingWindowLabel)
    }

    @Test
    fun customFastingProgramAppliesValidatedLocalSplit() = runTest {
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = FakeFoodRepository())
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openFastingTimer()
        viewModel.onCustomFastingHoursChanged("18")
        viewModel.onCustomEatingHoursChanged("6")
        viewModel.applyCustomFastingProgram()

        val custom = viewModel.state.value.fastingTimer.programs.single { it.id == "custom" }
        assertEquals("custom", viewModel.state.value.fastingTimer.selectedProgramId)
        assertEquals("Custom 18:6", custom.title)
        assertEquals("20:00 - 14:00", viewModel.state.value.fastingTimer.fastingWindowLabel)
        assertEquals("Custom fasting plan active", viewModel.state.value.message)

        viewModel.onCustomFastingHoursChanged("19")
        viewModel.onCustomEatingHoursChanged("8")
        viewModel.applyCustomFastingProgram()

        assertEquals("Enter fasting and eating hours that total 24", viewModel.state.value.message)
        assertEquals("custom", viewModel.state.value.fastingTimer.selectedProgramId)
    }

    @Test
    fun aiTextLoggingCreatesEditableDraftWithoutSavingUntilReviewed() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.selectAddMode(FoodAddMode.Ai)
        viewModel.onAiLoggingTextChanged("2 eggs and toast")
        viewModel.generateAiTextFoodDraft()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.savedLog)
        assertTrue(viewModel.state.value.aiLoggingHasDraft)
        assertEquals("2 eggs and toast", viewModel.state.value.productName)
        assertEquals("240", viewModel.state.value.caloriesPer100g)
        assertEquals("16", viewModel.state.value.proteinPer100g)
        assertEquals("19", viewModel.state.value.carbsPer100g)
        assertEquals("11", viewModel.state.value.fatPer100g)
        assertEquals("Local estimate: eggs, toast", viewModel.state.value.aiLoggingDraftReview)
        assertEquals("Review AI suggestion before logging.", viewModel.state.value.message)

        viewModel.onCaloriesChanged("360")
        viewModel.logFood()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Logged food", viewModel.state.value.message)
        assertEquals("2 eggs and toast", repository.savedLog?.name)
        assertEquals(360.0, repository.savedLog?.nutritionPer100g?.caloriesKcal ?: 0.0, 0.01)
    }

    @Test
    fun scannedNutritionLabelParsesAdvancedNutrientsAndConfidence() = runTest {
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = FakeFoodRepository())

        viewModel.onScannedLabel(
            """
            Energy 220 kcal
            Fat 8 g
            of which saturates 1.5 g
            Carbohydrate 30 g
            of which sugars 12 g
            Fibre 5 g
            Protein 10 g
            Sodium 320 mg
            """.trimIndent(),
        )

        assertEquals("220", viewModel.state.value.caloriesPer100g)
        assertEquals("10", viewModel.state.value.proteinPer100g)
        assertEquals("30", viewModel.state.value.carbsPer100g)
        assertEquals("8", viewModel.state.value.fatPer100g)
        assertEquals("5", viewModel.state.value.fiberPer100g)
        assertEquals("12", viewModel.state.value.sugarPer100g)
        assertEquals("1.5", viewModel.state.value.saturatedFatPer100g)
        assertEquals("320", viewModel.state.value.sodiumMgPer100g)
        assertEquals("Strong parse", viewModel.state.value.nutritionLabelScanReview?.confidenceLabel)
        assertEquals(8, viewModel.state.value.nutritionLabelScanReview?.parsedFieldCount)
    }

    @Test
    fun aiVoiceAndPhotoPlaceholdersCreateReviewDraftsWithoutSaving() = runTest {
        val repository = FakeFoodRepository()
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)

        viewModel.startAiVoiceLoggingPlaceholder()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.savedLog)
        assertTrue(viewModel.state.value.aiLoggingHasDraft)
        assertEquals("Voice draft", viewModel.state.value.productName)
        assertEquals("Voice placeholder ready. Review before logging.", viewModel.state.value.message)

        viewModel.startAiPhotoLoggingPlaceholder()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.savedLog)
        assertTrue(viewModel.state.value.aiLoggingHasDraft)
        assertEquals("Photo draft", viewModel.state.value.productName)
        assertEquals("Photo placeholder ready. Review before logging.", viewModel.state.value.message)
    }

    @Test
    fun foodHealthConnectSyncRefreshToggleAndSyncUsesSelectedDate() = runTest {
        val repository =
            FakeFoodRepository(
                foodHealthConnectSyncState = FoodHealthConnectSyncState(
                    isEnabled = false,
                    availability = HealthConnectAvailability.Available,
                    grantedPermissionCount = 2,
                    requestablePermissionCount = 2,
                    requestablePermissions = setOf("write-nutrition", "write-hydration"),
                    canRequestPermissions = true,
                    canSync = false,
                ),
                foodHealthConnectSyncResult = FoodHealthConnectSyncResult(
                    nutritionRecordCount = 2,
                    hydrationRecordCount = 1,
                ),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.refreshFoodHealthConnectSync()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onFoodHealthConnectSyncEnabledChanged(true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.syncFoodToHealthConnect()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, repository.foodHealthConnectEnabled)
        assertEquals(LocalDate.now(), repository.foodHealthConnectSyncDate)
        assertEquals(setOf("write-nutrition", "write-hydration"), viewModel.state.value.foodHealthConnectRequestablePermissions)
        assertTrue(viewModel.state.value.foodHealthConnectCanRequestPermissions)
        assertTrue(viewModel.state.value.foodHealthConnectCanSync)
        assertEquals("Synced 2 meals and water to Health Connect", viewModel.state.value.message)
    }

    @Test
    fun foodHealthConnectSyncErrorIsVisibleAndNonBlocking() = runTest {
        val repository =
            FakeFoodRepository(
                foodHealthConnectSyncState = FoodHealthConnectSyncState(
                    isEnabled = true,
                    availability = HealthConnectAvailability.Available,
                    grantedPermissionCount = 2,
                    requestablePermissionCount = 2,
                    requestablePermissions = setOf("write-nutrition", "write-hydration"),
                    canRequestPermissions = true,
                    canSync = true,
                ),
                foodHealthConnectSyncError = IllegalStateException("Check Health Connect permissions"),
            )
        val viewModel = FoodViewModel(provider = FakeProductProvider(), repository = repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.syncFoodToHealthConnect()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaving)
        assertEquals("Check Health Connect permissions", viewModel.state.value.message)
    }

    private data class CopyMealCall(
        val fromDate: LocalDate,
        val toDate: LocalDate,
        val mealType: String,
        val status: FoodDiaryEntryStatus = FoodDiaryEntryStatus.Logged,
    )

    private data class CopyDayCall(
        val fromDate: LocalDate,
        val toDate: LocalDate,
        val status: FoodDiaryEntryStatus,
    )

    private data class GenerateShoppingListCall(
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    private data class SaveTemplateCall(
        val date: LocalDate,
        val mealType: String,
        val name: String,
    )

    private data class LogTemplateCall(
        val templateId: String,
        val mealType: String,
        val date: LocalDate,
    )

    private data class LogRecipeCall(
        val recipeId: String,
        val mealType: String,
        val servings: Double,
        val date: LocalDate,
    )

    private data class PlanRecipeCall(
        val recipeId: String,
        val mealType: String,
        val servings: Double,
        val date: LocalDate,
    )

    private data class DuplicateRecipeCall(
        val recipeId: String,
        val name: String,
    )

    private data class LogFavoriteQuickLogCall(
        val presetId: String,
        val mealType: String,
        val date: LocalDate,
    )

    private data class RenameTemplateCall(
        val templateId: String,
        val name: String,
        val mealType: String,
    )

    private data class CopyDiaryEntryCall(
        val mealItemId: String,
        val mealType: String,
        val date: LocalDate,
    )

    private data class MergeDuplicateFoodsCall(
        val primaryFoodId: String,
        val duplicateFoodIds: List<String>,
    )

    private data class ConfirmedProductSaveCall(
        val result: ProductLookupResult.Found,
        val editedName: String,
        val editedBrand: String?,
        val editedNutrition: FoodNutrition,
    )

    private class FakeProductProvider(
        private val result: ProductLookupResult = foundProduct(),
        private val searchResult: ProductSearchResult = ProductSearchResult.Success("", emptyList()),
        private val resultsByBarcode: Map<String, ProductLookupResult> = emptyMap(),
    ) : FoodProductProvider {
        val lookupCalls = mutableListOf<String>()

        override suspend fun lookupBarcode(barcode: String): ProductLookupResult {
            lookupCalls += barcode
            return when (val lookupResult = resultsByBarcode[barcode] ?: result) {
                is ProductLookupResult.Found -> lookupResult.copy(barcode = barcode)
                is ProductLookupResult.NotFound -> lookupResult
                is ProductLookupResult.Failed -> lookupResult
            }
        }

        override suspend fun searchProducts(query: String, pageSize: Int): ProductSearchResult = searchResult
    }

    private class BlockingProductProvider : FoodProductProvider {
        private val result = CompletableDeferred<ProductLookupResult>()

        override suspend fun lookupBarcode(barcode: String): ProductLookupResult = result.await()

        fun completeWith(value: ProductLookupResult) {
            result.complete(value)
        }
    }

    private class BarcodeBlockingProductProvider : FoodProductProvider {
        private val resultsByBarcode = mutableMapOf<String, CompletableDeferred<ProductLookupResult>>()

        override suspend fun lookupBarcode(barcode: String): ProductLookupResult = resultsByBarcode.getOrPut(barcode) { CompletableDeferred() }.await()

        fun completeWith(barcode: String, value: ProductLookupResult) {
            resultsByBarcode.getOrPut(barcode) { CompletableDeferred() }.complete(value)
        }
    }

    private class ThrowingSearchProductProvider : FoodProductProvider {
        override suspend fun lookupBarcode(barcode: String): ProductLookupResult = ProductLookupResult.NotFound(barcode)

        override suspend fun searchProducts(query: String, pageSize: Int): ProductSearchResult = throw IllegalStateException("network unavailable")
    }

    private fun highProteinGoal(): FoodGoal = FoodGoal(
        dailyCaloriesKcal = 2083.0,
        proteinGrams = 150.0,
        carbsGrams = 210.0,
        fatGrams = 70.0,
        fiberGrams = 30.0,
        sugarGrams = 50.0,
        saturatedFatGrams = 20.0,
        sodiumMilligrams = 2300.0,
        mode = FoodGoalMode.HighProtein,
        includeTrainingCalories = false,
    )

    private fun weeklySummaryForScore(startDate: LocalDate): FoodWeeklySummary {
        val goal =
            FoodGoal(
                dailyCaloriesKcal = 2000.0,
                proteinGrams = 120.0,
                carbsGrams = 220.0,
                fatGrams = 70.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
                waterGoalMilliliters = 2000.0,
            )
        val trackedDays =
            listOf(
                weeklyDay(startDate, calories = 1980.0, protein = 118.0, fiber = 32.0, sodium = 1800.0, water = 2000.0),
                weeklyDay(startDate.plusDays(1), calories = 1900.0, protein = 104.0, fiber = 25.0, sodium = 2100.0, water = 1800.0),
                weeklyDay(startDate.plusDays(2), calories = 2600.0, protein = 65.0, fiber = 8.0, sodium = 3400.0, water = 500.0),
            )
        val emptyDays =
            (3L..6L).map { offset ->
                FoodWeeklyDaySummary(
                    date = startDate.plusDays(offset),
                    diary = FoodDiary(NutritionTotals(0.0, 0.0, 0.0, 0.0), emptyList()),
                    water = FoodWaterSummary(startDate.plusDays(offset), 0.0, 2000.0),
                )
            }
        return FoodWeeklySummary(startDate = startDate, days = trackedDays + emptyDays, goal = goal)
    }

    private fun progressSummaryForStats(startDate: LocalDate): FoodProgressSummary {
        val goal =
            FoodGoal(
                dailyCaloriesKcal = 2000.0,
                proteinGrams = 120.0,
                carbsGrams = 220.0,
                fatGrams = 70.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
                waterGoalMilliliters = 2000.0,
            )
        val days =
            (0L..27L).map { offset ->
                when (offset) {
                    2L -> weeklyDay(startDate.plusDays(offset), calories = 1500.0, protein = 80.0, fiber = 18.0, sodium = 2100.0, water = 2000.0)

                    8L -> weeklyDay(startDate.plusDays(offset), calories = 1700.0, protein = 90.0, fiber = 22.0, sodium = 1900.0, water = 1800.0)

                    21L -> weeklyDay(startDate.plusDays(offset), calories = 1980.0, protein = 118.0, fiber = 32.0, sodium = 1800.0, water = 2000.0)

                    22L -> weeklyDay(startDate.plusDays(offset), calories = 1900.0, protein = 104.0, fiber = 25.0, sodium = 2100.0, water = 2000.0)

                    23L -> weeklyDay(startDate.plusDays(offset), calories = 2600.0, protein = 65.0, fiber = 8.0, sodium = 3400.0, water = 500.0)

                    else ->
                        FoodWeeklyDaySummary(
                            date = startDate.plusDays(offset),
                            diary = FoodDiary(NutritionTotals(0.0, 0.0, 0.0, 0.0), emptyList()),
                            water = FoodWaterSummary(startDate.plusDays(offset), 0.0, 2000.0),
                        )
                }
            }
        return FoodProgressSummary(startDate = startDate, dayCount = 28, days = days, goal = goal)
    }

    private fun weeklyDay(
        date: LocalDate,
        calories: Double,
        protein: Double,
        fiber: Double,
        sodium: Double,
        water: Double,
    ): FoodWeeklyDaySummary = FoodWeeklyDaySummary(
        date = date,
        diary = FoodDiary(
            totals = NutritionTotals(calories, protein, carbsGrams = calories / 10.0, fatGrams = calories / 40.0),
            meals = emptyList(),
            detailTotals = NutritionDetails(fiberGrams = fiber, sodiumMilligrams = sodium),
        ),
        water = FoodWaterSummary(date, water, 2000.0),
    )

    private class FakeFoodRepository(
        diary: FoodDiary = emptyFoodDiary(),
        savedFoods: List<SavedFoodItem> = emptyList(),
        templates: List<MealTemplate> = emptyList(),
        recipes: List<Recipe> = emptyList(),
        quickCaloriePresets: List<QuickCaloriePreset> = emptyList(),
        customMealDefinitions: List<FoodMealDefinition> = emptyList(),
        weeklyPlan: List<FoodPlanDay> = emptyList(),
        weeklySummary: FoodWeeklySummary = FoodWeeklySummary(
            startDate = LocalDate.now(),
            days = emptyList(),
            goal = FoodGoal(
                dailyCaloriesKcal = 2083.0,
                proteinGrams = 104.0,
                carbsGrams = 260.0,
                fatGrams = 69.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
            ),
        ),
        progressSummary: FoodProgressSummary = FoodProgressSummary(
            startDate = LocalDate.now().minusDays(27),
            dayCount = 28,
            days = emptyList(),
            goal = FoodGoal(
                dailyCaloriesKcal = 2083.0,
                proteinGrams = 104.0,
                carbsGrams = 260.0,
                fatGrams = 69.0,
                fiberGrams = 30.0,
                sugarGrams = 50.0,
                saturatedFatGrams = 20.0,
                sodiumMilligrams = 2300.0,
                mode = FoodGoalMode.Balanced,
                includeTrainingCalories = false,
            ),
        ),
        shoppingGroups: List<ShoppingListGroup> = emptyList(),
        waterSummary: FoodWaterSummary = FoodWaterSummary(LocalDate.now(), 0.0, 2000.0),
        foodHealthConnectSyncState: FoodHealthConnectSyncState = FoodHealthConnectSyncState(),
        private val foodHealthConnectSyncResult: FoodHealthConnectSyncResult =
            FoodHealthConnectSyncResult(nutritionRecordCount = 0, hydrationRecordCount = 0),
        private val foodHealthConnectSyncError: Exception? = null,
        foodGoal: FoodGoal = FoodGoal(
            dailyCaloriesKcal = 2083.0,
            proteinGrams = 104.0,
            carbsGrams = 260.0,
            fatGrams = 69.0,
            fiberGrams = 30.0,
            sugarGrams = 50.0,
            saturatedFatGrams = 20.0,
            sodiumMilligrams = 2300.0,
            mode = FoodGoalMode.Balanced,
            includeTrainingCalories = false,
        ),
        private val burnedCalories: Double = 0.0,
    ) : FoodRepository {
        private val diaryFlow = MutableStateFlow(diary)
        private val savedFoodsFlow = MutableStateFlow(savedFoods)
        private val templatesFlow = MutableStateFlow(templates)
        private val recipesFlow = MutableStateFlow(recipes)
        private val quickCaloriePresetsFlow = MutableStateFlow(quickCaloriePresets)
        private val customMealDefinitionsFlow = MutableStateFlow(customMealDefinitions)
        private val weeklyPlanFlow = MutableStateFlow(weeklyPlan)
        private val weeklySummaryFlow = MutableStateFlow(weeklySummary)
        private val progressSummaryFlow = MutableStateFlow(progressSummary)
        private val shoppingGroupsFlow = MutableStateFlow(shoppingGroups)
        private val waterSummaryFlow = MutableStateFlow(waterSummary)
        private val foodHealthConnectSyncStateFlow = MutableStateFlow(foodHealthConnectSyncState)
        private val foodGoalFlow = MutableStateFlow(foodGoal)
        var savedLog: FoodLogInput? = null
        var savedFoodLog: SavedFoodLogInput? = null
        val savedFoodLogs = mutableListOf<SavedFoodLogInput>()
        var sameAsYesterdayItems: List<SavedFoodItem> = emptyList()
        val sameAsYesterdayRequests = mutableListOf<String>()
        var plannedFoodLog: SavedFoodLogInput? = null
        var quickLog: QuickCalorieLogInput? = null
        var favoriteQuickLogSave: QuickCaloriePresetInput? = null
        var favoriteQuickLogToggle: Pair<String, Boolean>? = null
        var logFavoriteQuickLogCall: LogFavoriteQuickLogCall? = null
        val weeklySummaryStartDates = mutableListOf<LocalDate>()
        var customMealDefinitionUpsert: FoodMealDefinitionInput? = null
        var diaryEntryUpdate: DiaryEntryUpdateInput? = null
        var deletedDiaryEntryId: String? = null
        var savedFoodUpsert: SavedFoodUpsertInput? = null
        var deletedSavedFoodId: String? = null
        var mergeDuplicateFoodsCall: MergeDuplicateFoodsCall? = null
        var favoriteToggle: Pair<String, Boolean>? = null
        var foodGoalUpdate: FoodGoal? = null
        var copyMealCall: CopyMealCall? = null
        var copyDayCall: CopyDayCall? = null
        var copyDiaryEntryCall: CopyDiaryEntryCall? = null
        var saveTemplateCall: SaveTemplateCall? = null
        var logTemplateCall: LogTemplateCall? = null
        var renameTemplateCall: RenameTemplateCall? = null
        var templateUpdate: MealTemplateUpdateInput? = null
        var duplicatedTemplateId: String? = null
        var deletedTemplateId: String? = null
        var favoriteTemplateToggle: Pair<String, Boolean>? = null
        var recipeUpsert: RecipeUpsertInput? = null
        var logRecipeCall: LogRecipeCall? = null
        var planRecipeCall: PlanRecipeCall? = null
        var duplicateRecipeCall: DuplicateRecipeCall? = null
        var deletedRecipeId: String? = null
        var favoriteRecipeToggle: Pair<String, Boolean>? = null
        var starterFoodsSeeded = false
        var confirmedProductSave: ConfirmedProductSaveCall? = null
        var markedLoggedEntryId: String? = null
        var generateShoppingListCall: GenerateShoppingListCall? = null
        var manualShoppingListItem: ManualShoppingListItemInput? = null
        var toggledShoppingItem: Pair<String, Boolean>? = null
        var waterLogInput: WaterLogInput? = null
        var waterRemoveInput: WaterLogInput? = null
        var waterRemoveResult: Double = 0.0
        var waterGoalMilliliters: Double? = null
        var foodHealthConnectEnabled: Boolean? = null
        var foodHealthConnectSyncDate: LocalDate? = null

        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String {
            confirmedProductSave = ConfirmedProductSaveCall(
                result = result,
                editedName = editedName,
                editedBrand = editedBrand,
                editedNutrition = editedNutrition,
            )
            return "food-1"
        }

        override suspend fun logFood(input: FoodLogInput): String {
            savedLog = input
            return "meal-item-1"
        }

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> = flowOf(NutritionTotals(0.0, 0.0, 0.0, 0.0))

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> = diaryFlow

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> = savedFoodsFlow

        override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> = flowOf(emptyList())

        override fun observeSameAsYesterday(mealType: String, date: java.time.LocalDate): Flow<List<SavedFoodItem>> {
            sameAsYesterdayRequests += mealType
            return flowOf(sameAsYesterdayItems)
        }

        override suspend fun getFoodDetail(foodId: String): SavedFoodItem? = savedFoodsFlow.value.firstOrNull { it.id == foodId }

        override suspend fun logSavedFood(input: SavedFoodLogInput): String {
            savedFoodLog = input
            savedFoodLogs += input
            return "meal-item-1"
        }

        override suspend fun planSavedFood(input: SavedFoodLogInput): String {
            plannedFoodLog = input
            return "entry-1"
        }

        override suspend fun quickLog(input: QuickCalorieLogInput): String {
            quickLog = input
            return "meal-item-1"
        }

        override fun observeFoodPlan(startDate: LocalDate): Flow<List<FoodPlanDay>> = weeklyPlanFlow

        override fun observeWeeklyFoodSummary(startDate: LocalDate): Flow<FoodWeeklySummary> {
            weeklySummaryStartDates += startDate
            return weeklySummaryFlow
        }

        override fun observeFoodProgressSummary(startDate: LocalDate, dayCount: Int): Flow<FoodProgressSummary> = progressSummaryFlow

        override fun observeShoppingList(): Flow<List<ShoppingListGroup>> = shoppingGroupsFlow

        override suspend fun generateShoppingList(startDate: LocalDate, endDate: LocalDate): List<ShoppingListGroup> {
            generateShoppingListCall = GenerateShoppingListCall(startDate, endDate)
            return shoppingGroupsFlow.value
        }

        override suspend fun addManualShoppingListItem(input: ManualShoppingListItemInput): String {
            manualShoppingListItem = input
            return "shopping-manual"
        }

        override suspend fun toggleShoppingListItem(itemId: String, isChecked: Boolean) {
            toggledShoppingItem = itemId to isChecked
        }

        override fun observeWaterSummary(date: LocalDate): Flow<FoodWaterSummary> = waterSummaryFlow

        override fun observeBurnedCalories(date: LocalDate): Flow<Double> = MutableStateFlow(burnedCalories)

        override suspend fun logWater(input: WaterLogInput): String {
            waterLogInput = input
            return "water-1"
        }

        override suspend fun removeWater(input: WaterLogInput): Double {
            waterRemoveInput = input
            return waterRemoveResult
        }

        override suspend fun updateWaterGoal(goalMilliliters: Double) {
            waterGoalMilliliters = goalMilliliters
            waterSummaryFlow.value = waterSummaryFlow.value.copy(goalMilliliters = goalMilliliters)
        }

        override fun observeFoodHealthConnectSyncState(): Flow<FoodHealthConnectSyncState> = foodHealthConnectSyncStateFlow

        override suspend fun refreshFoodHealthConnectSyncState(): FoodHealthConnectSyncState = foodHealthConnectSyncStateFlow.value

        override suspend fun setFoodHealthConnectSyncEnabled(isEnabled: Boolean) {
            foodHealthConnectEnabled = isEnabled
            foodHealthConnectSyncStateFlow.value =
                foodHealthConnectSyncStateFlow.value.copy(
                    isEnabled = isEnabled,
                    canSync = isEnabled &&
                        foodHealthConnectSyncStateFlow.value.availability == HealthConnectAvailability.Available &&
                        foodHealthConnectSyncStateFlow.value.grantedPermissionCount ==
                        foodHealthConnectSyncStateFlow.value.requestablePermissionCount,
                )
        }

        override suspend fun syncFoodToHealthConnect(date: LocalDate): FoodHealthConnectSyncResult {
            foodHealthConnectSyncDate = date
            foodHealthConnectSyncError?.let { error -> throw error }
            return foodHealthConnectSyncResult
        }

        override fun observeQuickCaloriePresets(): Flow<List<QuickCaloriePreset>> = quickCaloriePresetsFlow

        override suspend fun saveFavoriteQuickLog(input: QuickCaloriePresetInput): String {
            favoriteQuickLogSave = input
            return "quick-1"
        }

        override suspend fun toggleFavoriteQuickLog(presetId: String, isFavorite: Boolean) {
            favoriteQuickLogToggle = presetId to isFavorite
        }

        override suspend fun logFavoriteQuickLog(presetId: String, mealType: String, date: LocalDate): String {
            logFavoriteQuickLogCall = LogFavoriteQuickLogCall(presetId, mealType, date)
            return "meal-item-1"
        }

        override fun observeCustomMealDefinitions(): Flow<List<FoodMealDefinition>> = customMealDefinitionsFlow

        override suspend fun upsertCustomMealDefinition(input: FoodMealDefinitionInput): String {
            customMealDefinitionUpsert = input
            return input.mealId ?: "custom-meal-1"
        }

        override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) {
            diaryEntryUpdate = input
        }

        override suspend fun deleteDiaryEntry(mealItemId: String) {
            deletedDiaryEntryId = mealItemId
        }

        override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String {
            savedFoodUpsert = input
            return input.foodId ?: "food-new"
        }

        override suspend fun deleteSavedFood(foodId: String) {
            deletedSavedFoodId = foodId
        }

        override suspend fun mergeDuplicateFoods(primaryFoodId: String, duplicateFoodIds: List<String>) {
            mergeDuplicateFoodsCall = MergeDuplicateFoodsCall(primaryFoodId, duplicateFoodIds)
        }

        override suspend fun toggleFavoriteFood(foodId: String, isFavorite: Boolean) {
            favoriteToggle = foodId to isFavorite
        }

        override fun observeFoodGoal(): Flow<FoodGoal> = foodGoalFlow

        override suspend fun updateFoodGoal(goal: FoodGoal) {
            foodGoalUpdate = goal
            foodGoalFlow.value = goal
        }

        override fun observeMealTemplates(): Flow<List<MealTemplate>> = templatesFlow

        override suspend fun saveMealAsTemplate(date: LocalDate, mealType: String, name: String): String {
            saveTemplateCall = SaveTemplateCall(date, mealType, name)
            return "template-1"
        }

        override suspend fun logMealTemplate(templateId: String, mealType: String, date: LocalDate): List<String> {
            logTemplateCall = LogTemplateCall(templateId, mealType, date)
            return listOf("meal-item-1")
        }

        override suspend fun copyMeal(
            fromDate: LocalDate,
            toDate: LocalDate,
            mealType: String,
            status: FoodDiaryEntryStatus,
        ): List<String> {
            copyMealCall = CopyMealCall(fromDate, toDate, mealType, status)
            return listOf("meal-item-1")
        }

        override suspend fun copyDay(fromDate: LocalDate, toDate: LocalDate, status: FoodDiaryEntryStatus): List<String> {
            copyDayCall = CopyDayCall(fromDate, toDate, status)
            return listOf("meal-item-1")
        }

        override suspend fun renameMealTemplate(templateId: String, name: String, mealType: String) {
            renameTemplateCall = RenameTemplateCall(templateId, name, mealType)
        }

        override suspend fun updateMealTemplate(input: MealTemplateUpdateInput) {
            templateUpdate = input
            renameTemplateCall = RenameTemplateCall(input.templateId, input.name, input.mealType)
        }

        override suspend fun duplicateMealTemplate(templateId: String, name: String): String {
            duplicatedTemplateId = templateId
            return "template-copy"
        }

        override suspend fun deleteMealTemplate(templateId: String) {
            deletedTemplateId = templateId
        }

        override suspend fun toggleFavoriteMealTemplate(templateId: String, isFavorite: Boolean) {
            favoriteTemplateToggle = templateId to isFavorite
        }

        override suspend fun copyDiaryEntry(mealItemId: String, mealType: String, date: LocalDate): String {
            copyDiaryEntryCall = CopyDiaryEntryCall(mealItemId, mealType, date)
            return "meal-item-copy"
        }

        override suspend fun markDiaryEntryLogged(mealItemId: String) {
            markedLoggedEntryId = mealItemId
        }

        override fun observeRecipes(): Flow<List<Recipe>> = recipesFlow

        override suspend fun upsertRecipe(input: RecipeUpsertInput): String {
            recipeUpsert = input
            return input.recipeId ?: "recipe-1"
        }

        override suspend fun logRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String {
            logRecipeCall = LogRecipeCall(recipeId, mealType, servings, date)
            return "meal-item-1"
        }

        override suspend fun planRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String {
            planRecipeCall = PlanRecipeCall(recipeId, mealType, servings, date)
            return "meal-item-1"
        }

        override suspend fun duplicateRecipe(recipeId: String, name: String): String {
            duplicateRecipeCall = DuplicateRecipeCall(recipeId, name)
            return "recipe-copy"
        }

        override suspend fun deleteRecipe(recipeId: String) {
            deletedRecipeId = recipeId
        }

        override suspend fun toggleFavoriteRecipe(recipeId: String, isFavorite: Boolean) {
            favoriteRecipeToggle = recipeId to isFavorite
        }

        override suspend fun seedStarterFoods() {
            starterFoodsSeeded = true
        }
    }

    private class BlockingFoodRepository : FoodRepository {
        private val saveResult = CompletableDeferred<String>()
        var saveCalls: Int = 0

        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String {
            saveCalls += 1
            return saveResult.await()
        }

        override suspend fun logFood(input: FoodLogInput): String {
            saveCalls += 1
            return saveResult.await()
        }

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> = flowOf(NutritionTotals(0.0, 0.0, 0.0, 0.0))

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> = flowOf(FoodDiary(totals = NutritionTotals(0.0, 0.0, 0.0, 0.0), meals = emptyList()))

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> = flowOf(emptyList())

        override fun observeRecentFoods(limit: Int): Flow<List<SavedFoodItem>> = flowOf(emptyList())

        override fun observeSameAsYesterday(mealType: String, date: java.time.LocalDate): Flow<List<SavedFoodItem>> = flowOf(emptyList())

        override suspend fun logSavedFood(input: SavedFoodLogInput): String {
            saveCalls += 1
            return saveResult.await()
        }

        override suspend fun quickLog(input: QuickCalorieLogInput): String {
            saveCalls += 1
            return saveResult.await()
        }

        override suspend fun updateDiaryEntry(input: DiaryEntryUpdateInput) {
            saveCalls += 1
            saveResult.await()
        }

        override suspend fun deleteDiaryEntry(mealItemId: String) {
            saveCalls += 1
            saveResult.await()
        }

        override suspend fun upsertSavedFood(input: SavedFoodUpsertInput): String {
            saveCalls += 1
            return saveResult.await()
        }

        override suspend fun deleteSavedFood(foodId: String) {
            saveCalls += 1
            saveResult.await()
        }

        fun completeSave(value: String = "food-1") {
            saveResult.complete(value)
        }
    }
}

private fun foundProduct(
    barcode: String = "1234567890123",
    name: String = "Greek Yogurt",
    brand: String? = "Example Dairy",
    servingQuantityGrams: Double? = 170.0,
    nutrition: FoodNutrition = FoodNutrition(59.0, 10.0, 3.6, 0.4),
) = ProductLookupResult.Found(
    barcode = barcode,
    name = name,
    brand = brand,
    servingQuantityGrams = servingQuantityGrams,
    nutritionPer100g = nutrition,
    quality = ProductDataQuality.Complete,
    rawJson = "{}",
)

private fun emptyFoodDiary(): FoodDiary = FoodDiary(
    totals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
    meals = emptyList(),
)
