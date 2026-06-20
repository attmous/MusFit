package com.musfit.ui.food

import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodLogInput
import com.musfit.data.repository.FoodRepository
import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.NutritionTotals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

    private class FakeFoodRepository : FoodRepository {
        var savedLog: FoodLogInput? = null

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
