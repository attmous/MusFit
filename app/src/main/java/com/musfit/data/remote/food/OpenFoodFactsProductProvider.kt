package com.musfit.data.remote.food

import com.musfit.domain.model.FoodNutrition
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class OpenFoodFactsProductProvider @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val moshi: Moshi,
) : FoodProductProvider {
    private val responseAdapter = moshi.adapter(OpenFoodFactsResponse::class.java)

    override suspend fun lookupBarcode(barcode: String): ProductLookupResult =
        try {
            val rawJson = api.getProduct(barcode).string()
            val response =
                responseAdapter.fromJson(rawJson)
                    ?: return ProductLookupResult.Failed(
                        barcode = barcode,
                        message = "Lookup failed",
                    )
            normalize(
                barcode = barcode,
                response = response,
                rawJson = rawJson,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ProductLookupResult.Failed(
                barcode = barcode,
                message = exception.message ?: "Lookup failed",
            )
        }

    companion object {
        fun normalize(
            barcode: String,
            response: OpenFoodFactsResponse,
            rawJson: String = "{}",
        ): ProductLookupResult {
            if (response.status != 1 || response.product == null) {
                return ProductLookupResult.NotFound(barcode)
            }

            val product = response.product
            val nutriments = product.nutriments
            val calories = nutriments?.energyKcal100g
            val protein = nutriments?.proteins100g
            val carbs = nutriments?.carbohydrates100g
            val fat = nutriments?.fat100g
            val hasRequiredNutrition = calories != null && protein != null && carbs != null && fat != null

            return ProductLookupResult.Found(
                barcode = barcode,
                name = product.productName?.trim().takeUnless { it.isNullOrEmpty() } ?: "Unnamed product",
                brand = product.brands?.trim().takeUnless { it.isNullOrEmpty() },
                servingQuantityGrams = product.servingQuantity,
                nutritionPer100g =
                    FoodNutrition(
                        caloriesKcal = calories ?: 0.0,
                        proteinGrams = protein ?: 0.0,
                        carbsGrams = carbs ?: 0.0,
                        fatGrams = fat ?: 0.0,
                    ),
                quality = if (hasRequiredNutrition) ProductDataQuality.Complete else ProductDataQuality.Incomplete,
                rawJson = rawJson,
            )
        }
    }
}
