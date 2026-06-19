package com.musfit.data.remote.food

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsProductProviderTest {
    @Test
    fun normalizeProduct_marksCompleteProductAsFound() = runTest {
        val response =
            OpenFoodFactsResponse(
                status = 1,
                product =
                    OpenFoodFactsProduct(
                        productName = "Greek Yogurt",
                        brands = "Example Dairy",
                        servingQuantity = 170.0,
                        nutriments =
                            OpenFoodFactsNutriments(
                                energyKcal100g = 59.0,
                                proteins100g = 10.0,
                                carbohydrates100g = 3.6,
                                fat100g = 0.4,
                            ),
                    ),
            )

        val result = OpenFoodFactsProductProvider.normalize(barcode = "1234567890123", response = response)

        assertTrue(result is ProductLookupResult.Found)
        val found = result as ProductLookupResult.Found
        assertEquals("1234567890123", found.barcode)
        assertEquals("Greek Yogurt", found.name)
        assertEquals("Example Dairy", found.brand)
        assertEquals(ProductDataQuality.Complete, found.quality)
        assertEquals(59.0, found.nutritionPer100g.caloriesKcal, 0.01)
    }

    @Test
    fun normalizeProduct_marksMissingMacrosAsIncomplete() = runTest {
        val response =
            OpenFoodFactsResponse(
                status = 1,
                product =
                    OpenFoodFactsProduct(
                        productName = "Mystery Bar",
                        brands = null,
                        servingQuantity = null,
                        nutriments =
                            OpenFoodFactsNutriments(
                                energyKcal100g = null,
                                proteins100g = null,
                                carbohydrates100g = null,
                                fat100g = null,
                            ),
                    ),
            )

        val result = OpenFoodFactsProductProvider.normalize(barcode = "4000000000000", response = response)

        assertTrue(result is ProductLookupResult.Found)
        assertEquals(ProductDataQuality.Incomplete, (result as ProductLookupResult.Found).quality)
    }

    @Test
    fun normalizeProduct_returnsNotFoundWhenStatusIsZero() = runTest {
        val response = OpenFoodFactsResponse(status = 0, product = null)

        val result = OpenFoodFactsProductProvider.normalize(barcode = "000", response = response)

        assertEquals(ProductLookupResult.NotFound("000"), result)
    }
}
