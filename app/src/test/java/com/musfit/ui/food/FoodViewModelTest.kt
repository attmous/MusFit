package com.musfit.ui.food

import com.musfit.data.remote.food.FoodProductProvider
import com.musfit.data.remote.food.ProductDataQuality
import com.musfit.data.remote.food.ProductLookupResult
import com.musfit.data.repository.FoodRepository
import com.musfit.domain.model.FoodNutrition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
        assertEquals(59.0, result.caloriesPer100g, 0.01)
        assertEquals(10.0, result.proteinPer100g, 0.01)
        assertEquals(3.6, result.carbsPer100g, 0.01)
        assertEquals(0.4, result.fatPer100g, 0.01)
        assertNull(result.message)

        viewModel.onProductNameChanged("Edited Yogurt")
        viewModel.onBrandChanged("")
        viewModel.onCaloriesChanged("61")
        viewModel.onProteinChanged("10.5")
        viewModel.onCarbsChanged("4.0")
        viewModel.onFatChanged("0.5")
        viewModel.saveProduct()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Saved food", viewModel.state.value.message)
        assertEquals("Edited Yogurt", repository.savedName)
        assertNull(repository.savedBrand)
        assertEquals(
            FoodNutrition(
                caloriesKcal = 61.0,
                proteinGrams = 10.5,
                carbsGrams = 4.0,
                fatGrams = 0.5,
            ),
            repository.savedNutrition,
        )
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

    private class FakeFoodRepository : FoodRepository {
        var savedName: String? = null
        var savedBrand: String? = null
        var savedNutrition: FoodNutrition? = null

        override suspend fun saveConfirmedProduct(
            result: ProductLookupResult.Found,
            editedName: String,
            editedBrand: String?,
            editedNutrition: FoodNutrition,
        ): String {
            savedName = editedName
            savedBrand = editedBrand
            savedNutrition = editedNutrition
            return "food-1"
        }
    }
}

private fun foundProduct(
    barcode: String = "1234567890123",
    name: String = "Greek Yogurt",
    brand: String? = "Example Dairy",
    servingQuantityGrams: Double? = 170.0,
) = ProductLookupResult.Found(
    barcode = barcode,
    name = name,
    brand = brand,
    servingQuantityGrams = servingQuantityGrams,
    nutritionPer100g = FoodNutrition(59.0, 10.0, 3.6, 0.4),
    quality = ProductDataQuality.Complete,
    rawJson = "{}",
)
