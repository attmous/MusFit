package com.musfit.data.repository

import com.musfit.domain.model.FoodNutrition

/** Normalized product lookup port. Transport DTOs remain inside the remote adapter. */
interface FoodProductProvider {
    suspend fun lookupBarcode(barcode: String): ProductLookupResult

    suspend fun searchProducts(query: String, pageSize: Int = 20): ProductSearchResult = ProductSearchResult.Failed(query = query, message = "Food search is unavailable")
}

sealed interface ProductLookupResult {
    data class Found(
        val barcode: String,
        val name: String,
        val brand: String?,
        val servingQuantityGrams: Double?,
        val nutritionPer100g: FoodNutrition,
        val nutritionDetailsPer100g: NutritionDetails = NutritionDetails(),
        val category: String? = null,
        val imageUrl: String? = null,
        val quality: ProductDataQuality,
        val rawJson: String,
    ) : ProductLookupResult

    data class NotFound(val barcode: String) : ProductLookupResult
    data class Failed(val barcode: String, val message: String) : ProductLookupResult
}

enum class ProductDataQuality { Complete, Incomplete }

sealed interface ProductSearchResult {
    data class Success(
        val query: String,
        val products: List<ProductLookupResult.Found>,
    ) : ProductSearchResult
    data class Failed(val query: String, val message: String) : ProductSearchResult
}
