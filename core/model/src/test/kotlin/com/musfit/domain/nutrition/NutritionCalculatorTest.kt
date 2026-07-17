package com.musfit.domain.nutrition

import com.musfit.domain.model.FoodNutrition
import com.musfit.domain.model.MealItemInput
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {
    @Test
    fun calculateMealTotals_scalesPer100gNutritionByQuantity() {
        val items = listOf(
            MealItemInput(
                foodId = "rice",
                quantityGrams = 150.0,
                nutritionPer100g = FoodNutrition(
                    caloriesKcal = 130.0,
                    proteinGrams = 2.7,
                    carbsGrams = 28.0,
                    fatGrams = 0.3,
                ),
            ),
        )

        val totals = NutritionCalculator.calculateMealTotals(items)

        assertEquals(195.0, totals.caloriesKcal, 0.01)
        assertEquals(4.05, totals.proteinGrams, 0.01)
        assertEquals(42.0, totals.carbsGrams, 0.01)
        assertEquals(0.45, totals.fatGrams, 0.01)
    }

    @Test
    fun nutritionForAmount_scalesPer100gByQuantity() {
        val per100g = FoodNutrition(
            caloriesKcal = 250.0,
            proteinGrams = 12.0,
            carbsGrams = 30.0,
            fatGrams = 8.0,
        )

        val scaled = NutritionCalculator.nutritionForAmount(per100g, quantityGrams = 150.0)

        assertEquals(375.0, scaled.caloriesKcal, 0.01)
        assertEquals(18.0, scaled.proteinGrams, 0.01)
        assertEquals(45.0, scaled.carbsGrams, 0.01)
        assertEquals(12.0, scaled.fatGrams, 0.01)
    }

    @Test
    fun nutritionForAmount_returnsZeroForZeroQuantity() {
        val per100g = FoodNutrition(250.0, 12.0, 30.0, 8.0)

        val scaled = NutritionCalculator.nutritionForAmount(per100g, quantityGrams = 0.0)

        assertEquals(0.0, scaled.caloriesKcal, 0.01)
        assertEquals(0.0, scaled.proteinGrams, 0.01)
        assertEquals(0.0, scaled.carbsGrams, 0.01)
        assertEquals(0.0, scaled.fatGrams, 0.01)
    }

    @Test
    fun calculateMealTotals_ignoresZeroAndNegativeQuantities() {
        val items = listOf(
            MealItemInput(
                foodId = "bad",
                quantityGrams = -40.0,
                nutritionPer100g = FoodNutrition(100.0, 10.0, 10.0, 10.0),
            ),
            MealItemInput(
                foodId = "zero",
                quantityGrams = 0.0,
                nutritionPer100g = FoodNutrition(100.0, 10.0, 10.0, 10.0),
            ),
        )

        val totals = NutritionCalculator.calculateMealTotals(items)

        assertEquals(0.0, totals.caloriesKcal, 0.01)
        assertEquals(0.0, totals.proteinGrams, 0.01)
        assertEquals(0.0, totals.carbsGrams, 0.01)
        assertEquals(0.0, totals.fatGrams, 0.01)
    }
}
