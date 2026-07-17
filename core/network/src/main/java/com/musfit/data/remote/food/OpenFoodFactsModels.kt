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
    val categories: String? = null,
    @param:Json(name = "image_url") val imageUrl: String? = null,
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsNutriments(
    @param:Json(name = "energy-kcal_100g") val energyKcal100g: Double?,
    @param:Json(name = "proteins_100g") val proteins100g: Double?,
    @param:Json(name = "carbohydrates_100g") val carbohydrates100g: Double?,
    @param:Json(name = "fat_100g") val fat100g: Double?,
    @param:Json(name = "fiber_100g") val fiber100g: Double? = null,
    @param:Json(name = "sugars_100g") val sugars100g: Double? = null,
    @param:Json(name = "saturated-fat_100g") val saturatedFat100g: Double? = null,
    @param:Json(name = "sodium_100g") val sodium100g: Double? = null,
    @param:Json(name = "potassium_100g") val potassium100g: Double? = null,
    @param:Json(name = "calcium_100g") val calcium100g: Double? = null,
    @param:Json(name = "iron_100g") val iron100g: Double? = null,
    @param:Json(name = "vitamin-d_100g") val vitaminD100g: Double? = null,
    @param:Json(name = "vitamin-c_100g") val vitaminC100g: Double? = null,
    @param:Json(name = "magnesium_100g") val magnesium100g: Double? = null,
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsSearchResponse(
    val hits: List<OpenFoodFactsSearchHit> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class OpenFoodFactsSearchHit(
    val code: String?,
    @param:Json(name = "product_name") val productName: String?,
    val brands: List<String>? = null,
    val categories: String? = null,
    @param:Json(name = "image_url") val imageUrl: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null,
)
