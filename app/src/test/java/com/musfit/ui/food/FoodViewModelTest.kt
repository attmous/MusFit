package com.musfit.ui.food

import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodDiary
import com.musfit.data.repository.FoodDiaryEntry
import com.musfit.data.repository.FoodDiaryMeal
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.data.repository.QuickCalorieLogInput
import com.musfit.data.repository.DiaryEntryUpdateInput
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

        assertEquals("Product not found", viewModel.state.value.message)
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

    private class FakeProductProvider(
        private val result: ProductLookupResult = foundProduct(),
    ) : FoodProductProvider {
        override suspend fun lookupBarcode(barcode: String): ProductLookupResult =
            when (result) {
                is ProductLookupResult.Found -> result.copy(barcode = barcode)
                else -> result
            }
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
    ) : FoodRepository {
        private val diaryFlow = MutableStateFlow(diary)
        private val savedFoodsFlow = MutableStateFlow(savedFoods)
        var savedLog: FoodLogInput? = null
        var savedFoodLog: SavedFoodLogInput? = null
        var quickLog: QuickCalorieLogInput? = null
        var diaryEntryUpdate: DiaryEntryUpdateInput? = null
        var deletedDiaryEntryId: String? = null
        var savedFoodUpsert: SavedFoodUpsertInput? = null
        var deletedSavedFoodId: String? = null

        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String {
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

        override suspend fun logSavedFood(input: SavedFoodLogInput): String {
            savedFoodLog = input
            return "meal-item-1"
        }

        override suspend fun quickLog(input: QuickCalorieLogInput): String {
            quickLog = input
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
