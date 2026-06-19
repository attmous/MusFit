package com.musfit.data.remote.food

import com.musfit.domain.model.FoodNutrition

interface FoodProductProvider {
    suspend fun lookupBarcode(barcode: String): ProductLookupResult
}

sealed interface ProductLookupResult {
    data class Found(
        val barcode: String,
        val name: String,
        val brand: String?,
        val servingQuantityGrams: Double?,
        val nutritionPer100g: FoodNutrition,
        val quality: ProductDataQuality,
        val rawJson: String,
    ) : ProductLookupResult

    data class NotFound(
        val barcode: String,
    ) : ProductLookupResult

    data class Failed(
        val barcode: String,
        val message: String,
    ) : ProductLookupResult
}

enum class ProductDataQuality {
    Complete,
    Incomplete,
}
