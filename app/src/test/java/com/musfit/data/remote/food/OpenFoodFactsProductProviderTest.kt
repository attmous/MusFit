package com.musfit.data.remote.food

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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

    @Test
    fun lookupBarcode_preservesOriginalRawJsonIncludingUnknownFields() = runTest {
        val rawJson =
            """
            {
              "status": 1,
              "product": {
                "product_name": "Greek Yogurt",
                "brands": "Example Dairy",
                "serving_quantity": 170.0,
                "unknown_field": "kept",
                "nutriments": {
                  "energy-kcal_100g": 59.0,
                  "proteins_100g": 10.0,
                  "carbohydrates_100g": 3.6,
                  "fat_100g": 0.4
                }
              },
              "extra_root": {
                "nested": true
              }
            }
            """.trimIndent()
        val provider = OpenFoodFactsProductProvider(FakeApi(rawJson = rawJson), testMoshi())

        val result = provider.lookupBarcode("1234567890123")

        assertTrue(result is ProductLookupResult.Found)
        assertEquals(rawJson, (result as ProductLookupResult.Found).rawJson)
    }

    @Test
    fun lookupBarcode_propagatesCancellation() = runTest {
        val provider =
            OpenFoodFactsProductProvider(
                FakeApi(exception = CancellationException("cancelled")),
                testMoshi(),
            )

        try {
            provider.lookupBarcode("1234567890123")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    private fun testMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    private class FakeApi(
        private val rawJson: String? = null,
        private val exception: Exception? = null,
    ) : OpenFoodFactsApi {
        override suspend fun getProduct(barcode: String): ResponseBody {
            exception?.let { throw it }
            return checkNotNull(rawJson)
                .toResponseBody("application/json".toMediaType())
        }
    }
}
