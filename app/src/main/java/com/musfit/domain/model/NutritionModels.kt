package com.musfit.domain.model

data class FoodNutrition(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)

data class MealItemInput(
    val foodId: String,
    val quantityGrams: Double,
    val nutritionPer100g: FoodNutrition,
)

data class NutritionTotals(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
