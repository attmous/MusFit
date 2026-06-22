package com.musfit.domain.nutrition

import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.MealItemInput
import com.musfit.domain.model.NutritionTotals

object NutritionCalculator {
    /**
     * Scales a per-100 g nutrition record to an arbitrary gram amount.
     * Centralizes the `value * grams / 100` math used by add/preview flows.
     */
    fun nutritionForAmount(nutritionPer100g: FoodNutrition, quantityGrams: Double): FoodNutrition {
        val scale = quantityGrams / 100.0
        return FoodNutrition(
            caloriesKcal = nutritionPer100g.caloriesKcal * scale,
            proteinGrams = nutritionPer100g.proteinGrams * scale,
            carbsGrams = nutritionPer100g.carbsGrams * scale,
            fatGrams = nutritionPer100g.fatGrams * scale,
        )
    }

    fun calculateMealTotals(items: List<MealItemInput>): NutritionTotals {
        val validItems = items.filter { it.quantityGrams > 0.0 }
        return NutritionTotals(
            caloriesKcal = validItems.sumOf { it.nutritionPer100g.caloriesKcal * it.quantityGrams / 100.0 },
            proteinGrams = validItems.sumOf { it.nutritionPer100g.proteinGrams * it.quantityGrams / 100.0 },
            carbsGrams = validItems.sumOf { it.nutritionPer100g.carbsGrams * it.quantityGrams / 100.0 },
            fatGrams = validItems.sumOf { it.nutritionPer100g.fatGrams * it.quantityGrams / 100.0 },
        )
    }
}
