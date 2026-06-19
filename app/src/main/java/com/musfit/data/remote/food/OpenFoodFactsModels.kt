package com.musfit.data.remote.food

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class OpenFoodFactsResponse(
    val status: Int,
    val product: OpenFoodFactsProduct?,
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsProduct(
    @param:Json(name = "product_name") val productName: String?,
    val brands: String?,
    @param:Json(name = "serving_quantity") val servingQuantity: Double?,
    val nutriments: OpenFoodFactsNutriments?,
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsNutriments(
    @param:Json(name = "energy-kcal_100g") val energyKcal100g: Double?,
    @param:Json(name = "proteins_100g") val proteins100g: Double?,
    @param:Json(name = "carbohydrates_100g") val carbohydrates100g: Double?,
    @param:Json(name = "fat_100g") val fat100g: Double?,
)
