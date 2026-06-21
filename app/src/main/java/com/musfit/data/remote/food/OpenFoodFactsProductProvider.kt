package com.musfit.data.remote.food

import com.musfit.data.repository.NutritionDetails
import com.musfit.domain.model.FoodNutrition
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class OpenFoodFactsProductProvider @Inject constructor(
    private val api: OpenFoodFactsApi,
    private val moshi: Moshi,
) : FoodProductProvider {
    private val responseAdapter = moshi.adapter(OpenFoodFactsResponse::class.java)
    private val searchResponseAdapter = moshi.adapter(OpenFoodFactsSearchResponse::class.java)

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

    override suspend fun searchProducts(query: String, pageSize: Int): ProductSearchResult {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) {
            return ProductSearchResult.Success(query = trimmedQuery, products = emptyList())
        }

        return try {
            val rawJson = api.searchProducts(trimmedQuery, pageSize.coerceIn(1, 50)).string()
            val response =
                searchResponseAdapter.fromJson(rawJson)
                    ?: return ProductSearchResult.Failed(
                        query = trimmedQuery,
                        message = "Search failed",
                    )
            ProductSearchResult.Success(
                query = trimmedQuery,
                products = normalizeSearchHits(trimmedQuery, response, rawJson),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            ProductSearchResult.Failed(
                query = trimmedQuery,
                message = exception.message ?: "Search failed",
            )
        }
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
                nutritionDetailsPer100g = nutriments.toNutritionDetails(),
                category = product.categories?.trim().takeUnless { it.isNullOrEmpty() },
                imageUrl = product.imageUrl?.trim().takeUnless { it.isNullOrEmpty() },
                quality = if (hasRequiredNutrition) ProductDataQuality.Complete else ProductDataQuality.Incomplete,
                rawJson = rawJson,
            )
        }

        fun normalizeSearchHits(
            query: String,
            response: OpenFoodFactsSearchResponse,
            rawJson: String = "{}",
        ): List<ProductLookupResult.Found> =
            response.hits.mapNotNull { hit ->
                val barcode = hit.code?.filter(Char::isDigit).orEmpty()
                if (barcode.isBlank()) {
                    return@mapNotNull null
                }
                val nutriments = hit.nutriments
                val calories = nutriments?.energyKcal100g
                val protein = nutriments?.proteins100g
                val carbs = nutriments?.carbohydrates100g
                val fat = nutriments?.fat100g
                val hasRequiredNutrition = calories != null && protein != null && carbs != null && fat != null

                ProductLookupResult.Found(
                    barcode = barcode,
                    name = hit.productName?.trim().takeUnless { it.isNullOrEmpty() } ?: query,
                    brand = hit.brands
                        ?.mapNotNull { brand -> brand.trim().takeIf { it.isNotEmpty() } }
                        ?.distinct()
                        ?.joinToString(", ")
                        ?.takeIf { it.isNotEmpty() },
                    servingQuantityGrams = null,
                    nutritionPer100g = FoodNutrition(
                        caloriesKcal = calories ?: 0.0,
                        proteinGrams = protein ?: 0.0,
                        carbsGrams = carbs ?: 0.0,
                        fatGrams = fat ?: 0.0,
                    ),
                    nutritionDetailsPer100g = nutriments.toNutritionDetails(),
                    category = hit.categories?.trim().takeUnless { it.isNullOrEmpty() },
                    imageUrl = hit.imageUrl?.trim().takeUnless { it.isNullOrEmpty() },
                    quality = if (hasRequiredNutrition) ProductDataQuality.Complete else ProductDataQuality.Incomplete,
                    rawJson = rawJson,
                )
            }

        private fun OpenFoodFactsNutriments?.toNutritionDetails(): NutritionDetails =
            NutritionDetails(
                fiberGrams = this?.fiber100g ?: 0.0,
                sugarGrams = this?.sugars100g ?: 0.0,
                saturatedFatGrams = this?.saturatedFat100g ?: 0.0,
                sodiumMilligrams = (this?.sodium100g ?: 0.0) * 1000.0,
                potassiumMilligrams = (this?.potassium100g ?: 0.0) * 1000.0,
                calciumMilligrams = (this?.calcium100g ?: 0.0) * 1000.0,
                ironMilligrams = (this?.iron100g ?: 0.0) * 1000.0,
                vitaminDMicrograms = (this?.vitaminD100g ?: 0.0) * 1_000_000.0,
                vitaminCMilligrams = (this?.vitaminC100g ?: 0.0) * 1000.0,
                magnesiumMilligrams = (this?.magnesium100g ?: 0.0) * 1000.0,
            )
    }
}
