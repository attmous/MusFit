package com.musfit.domain.nutrition

import com.musfit.domain.model.MealItemInput
import com.musfit.domain.model.NutritionTotals

object NutritionCalculator {
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
