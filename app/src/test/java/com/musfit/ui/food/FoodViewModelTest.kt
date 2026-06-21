package com.musfit.ui.food

import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.remote.food.ProductSearchResult
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntry
import com.musfit.data.repository.FoodDiaryMeal
import com.musfit.data.repository.FoodGoal
import com.musfit.data.repository.FoodGoalMode
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodServingInput
import com.musfit.data.repository.FoodServingOption
import com.musfit.data.repository.MealTemplate
import com.musfit.data.repository.MealTemplateItem
import com.musfit.data.repository.NutritionDetails
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.QuickCaloriePreset
import com.musfit.data.repository.QuickCaloriePresetInput
import com.musfit.data.repository.DiaryEntryUpdateInput
import com.musfit.data.repository.Recipe
import com.musfit.data.repository.RecipeIngredient
import com.musfit.data.repository.RecipeUpsertInput
import com.musfit.data.repository.SavedFoodItem
import com.musfit.data.repository.SavedFoodLogInput
import com.musfit.data.repository.SavedFoodUpsertInput
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FoodViewModelTest {
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

        with(viewModel.state.value) {
            assertEquals(60.0, editingDiaryEntryPreviewCaloriesKcal, 0.01)
            assertEquals(10.0, editingDiaryEntryPreviewProteinGrams, 0.01)
            assertEquals(4.0, editingDiaryEntryPreviewCarbsGrams, 0.01)
            assertEquals(1.0, editingDiaryEntryPreviewFatGrams, 0.01)
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

        val choices = viewModel.state.value.editingDiaryEntryServingChoices
        assertEquals(listOf("100 g", "Cup", "Half cup"), choices.map { it.label })

        viewModel.onDiaryEntryServingChoiceSelected(choices.first { it.label == "Half cup" }.id)

        with(viewModel.state.value) {
            assertEquals("85", editingDiaryEntryQuantityGrams)
            assertEquals(51.0, editingDiaryEntryPreviewCaloriesKcal, 0.01)
            assertEquals(8.5, editingDiaryEntryPreviewProteinGrams, 0.01)
            assertEquals(3.4, editingDiaryEntryPreviewCarbsGrams, 0.01)
            assertEquals(0.85, editingDiaryEntryPreviewFatGrams, 0.01)
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
            assertEquals("entry-1", editingDiaryEntryId)
            assertEquals("Greek yogurt", editingDiaryEntryName)
            assertEquals("200", editingDiaryEntryQuantityGrams)
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
            assertEquals("food-1", editingSavedFoodId)
            assertEquals("Greek yogurt", savedFoodName)
            assertEquals("Kitchen", savedFoodBrand)
            assertEquals("200", savedFoodServingGrams)
            assertEquals("60", savedFoodCaloriesPer100g)
            assertEquals("10", savedFoodProteinPer100g)
            assertEquals("4", savedFoodCarbsPer100g)
            assertEquals("1", savedFoodFatPer100g)
        }
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
            assertEquals("Scanned label", savedFoodName)
            assertEquals("100", savedFoodServingGrams)
            assertEquals("250", savedFoodCaloriesPer100g)
            assertEquals("12", savedFoodProteinPer100g)
            assertEquals("30", savedFoodCarbsPer100g)
            assertEquals("8", savedFoodFatPer100g)
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
        viewModel.onGoalModeChanged(FoodGoalMode.MuscleGain)
        viewModel.onGoalIncludeTrainingChanged(true)
        viewModel.saveFoodGoal()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2400.0, repository.foodGoalUpdate?.dailyCaloriesKcal ?: 0.0, 0.01)
        assertEquals(180.0, repository.foodGoalUpdate?.proteinGrams ?: 0.0, 0.01)
        assertEquals(FoodGoalMode.MuscleGain, repository.foodGoalUpdate?.mode)
        assertTrue(repository.foodGoalUpdate?.includeTrainingCalories == true)
        assertEquals(2400.0, viewModel.state.value.calorieGoalKcal, 0.01)
        assertEquals("Updated nutrition goals", viewModel.state.value.message)
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

    private data class CopyMealCall(
        val fromDate: LocalDate,
        val toDate: LocalDate,
        val mealType: String,
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
    ) : FoodProductProvider {
        override suspend fun lookupBarcode(barcode: String): ProductLookupResult =
            when (result) {
                is ProductLookupResult.Found -> result.copy(barcode = barcode)
                else -> result
            }

        override suspend fun searchProducts(query: String, pageSize: Int): ProductSearchResult =
            searchResult
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

        override suspend fun lookupBarcode(barcode: String): ProductLookupResult =
            resultsByBarcode.getOrPut(barcode) { CompletableDeferred() }.await()

        fun completeWith(barcode: String, value: ProductLookupResult) {
            resultsByBarcode.getOrPut(barcode) { CompletableDeferred() }.complete(value)
        }
    }

    private class FakeFoodRepository(
        diary: FoodDiary = emptyFoodDiary(),
        savedFoods: List<SavedFoodItem> = emptyList(),
        templates: List<MealTemplate> = emptyList(),
        recipes: List<Recipe> = emptyList(),
        quickCaloriePresets: List<QuickCaloriePreset> = emptyList(),
        foodGoal: FoodGoal = FoodGoal(
            dailyCaloriesKcal = 2083.0,
            proteinGrams = 104.0,
            carbsGrams = 260.0,
            fatGrams = 69.0,
            fiberGrams = 30.0,
            sugarGrams = 50.0,
            saturatedFatGrams = 20.0,
            sodiumMilligrams = 2300.0,
            mode = FoodGoalMode.Maintain,
            includeTrainingCalories = false,
        ),
    ) : FoodRepository {
        private val diaryFlow = MutableStateFlow(diary)
        private val savedFoodsFlow = MutableStateFlow(savedFoods)
        private val templatesFlow = MutableStateFlow(templates)
        private val recipesFlow = MutableStateFlow(recipes)
        private val quickCaloriePresetsFlow = MutableStateFlow(quickCaloriePresets)
        private val foodGoalFlow = MutableStateFlow(foodGoal)
        var savedLog: FoodLogInput? = null
        var savedFoodLog: SavedFoodLogInput? = null
        var quickLog: QuickCalorieLogInput? = null
        var favoriteQuickLogSave: QuickCaloriePresetInput? = null
        var favoriteQuickLogToggle: Pair<String, Boolean>? = null
        var logFavoriteQuickLogCall: LogFavoriteQuickLogCall? = null
        var diaryEntryUpdate: DiaryEntryUpdateInput? = null
        var deletedDiaryEntryId: String? = null
        var savedFoodUpsert: SavedFoodUpsertInput? = null
        var deletedSavedFoodId: String? = null
        var mergeDuplicateFoodsCall: MergeDuplicateFoodsCall? = null
        var favoriteToggle: Pair<String, Boolean>? = null
        var foodGoalUpdate: FoodGoal? = null
        var copyMealCall: CopyMealCall? = null
        var copyDiaryEntryCall: CopyDiaryEntryCall? = null
        var saveTemplateCall: SaveTemplateCall? = null
        var logTemplateCall: LogTemplateCall? = null
        var renameTemplateCall: RenameTemplateCall? = null
        var duplicatedTemplateId: String? = null
        var deletedTemplateId: String? = null
        var favoriteTemplateToggle: Pair<String, Boolean>? = null
        var recipeUpsert: RecipeUpsertInput? = null
        var logRecipeCall: LogRecipeCall? = null
        var deletedRecipeId: String? = null
        var favoriteRecipeToggle: Pair<String, Boolean>? = null
        var starterFoodsSeeded = false
        var confirmedProductSave: ConfirmedProductSaveCall? = null

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

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> =
            flowOf(NutritionTotals(0.0, 0.0, 0.0, 0.0))

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> =
            diaryFlow

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> =
            savedFoodsFlow

        override suspend fun getFoodDetail(foodId: String): SavedFoodItem? =
            savedFoodsFlow.value.firstOrNull { it.id == foodId }

        override suspend fun logSavedFood(input: SavedFoodLogInput): String {
            savedFoodLog = input
            return "meal-item-1"
        }

        override suspend fun quickLog(input: QuickCalorieLogInput): String {
            quickLog = input
            return "meal-item-1"
        }

        override fun observeQuickCaloriePresets(): Flow<List<QuickCaloriePreset>> =
            quickCaloriePresetsFlow

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

        override fun observeFoodGoal(): Flow<FoodGoal> =
            foodGoalFlow

        override suspend fun updateFoodGoal(goal: FoodGoal) {
            foodGoalUpdate = goal
            foodGoalFlow.value = goal
        }

        override fun observeMealTemplates(): Flow<List<MealTemplate>> =
            templatesFlow

        override suspend fun saveMealAsTemplate(date: LocalDate, mealType: String, name: String): String {
            saveTemplateCall = SaveTemplateCall(date, mealType, name)
            return "template-1"
        }

        override suspend fun logMealTemplate(templateId: String, mealType: String, date: LocalDate): List<String> {
            logTemplateCall = LogTemplateCall(templateId, mealType, date)
            return listOf("meal-item-1")
        }

        override suspend fun copyMeal(fromDate: LocalDate, toDate: LocalDate, mealType: String): List<String> {
            copyMealCall = CopyMealCall(fromDate, toDate, mealType)
            return listOf("meal-item-1")
        }

        override suspend fun renameMealTemplate(templateId: String, name: String, mealType: String) {
            renameTemplateCall = RenameTemplateCall(templateId, name, mealType)
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

        override fun observeRecipes(): Flow<List<Recipe>> =
            recipesFlow

        override suspend fun upsertRecipe(input: RecipeUpsertInput): String {
            recipeUpsert = input
            return input.recipeId ?: "recipe-1"
        }

        override suspend fun logRecipe(recipeId: String, mealType: String, servings: Double, date: LocalDate): String {
            logRecipeCall = LogRecipeCall(recipeId, mealType, servings, date)
            return "meal-item-1"
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

        override fun observeDailyNutrition(date: LocalDate): Flow<NutritionTotals> =
            flowOf(NutritionTotals(0.0, 0.0, 0.0, 0.0))

        override fun observeFoodDiary(date: LocalDate): Flow<FoodDiary> =
            flowOf(FoodDiary(totals = NutritionTotals(0.0, 0.0, 0.0, 0.0), meals = emptyList()))

        override fun observeSavedFoods(): Flow<List<SavedFoodItem>> =
            flowOf(emptyList())

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

private fun emptyFoodDiary(): FoodDiary =
    FoodDiary(
        totals = NutritionTotals(0.0, 0.0, 0.0, 0.0),
        meals = emptyList(),
    )
