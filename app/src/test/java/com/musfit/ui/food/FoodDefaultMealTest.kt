package com.musfit.ui.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodDefaultMealTest {
    private fun meal(id: String, title: String) = FoodMealSectionUiState(
        id = id,
        title = title,
        recommendation = "",
        caloriesKcal = 0.0,
        calorieTargetKcal = 0.0,
        calorieProgress = 0.0,
        entries = emptyList(),
    )

    private val meals = listOf(
        meal("breakfast", "Breakfast"),
        meal("lunch", "Lunch"),
        meal("dinner", "Dinner"),
        meal("snacks", "Snacks"),
    )

    @Test
    fun morningPicksBreakfast() = assertEquals("breakfast", defaultAddMealId(meals, 8))

    @Test
    fun middayPicksLunch() = assertEquals("lunch", defaultAddMealId(meals, 13))

    @Test
    fun eveningPicksDinner() = assertEquals("dinner", defaultAddMealId(meals, 19))

    @Test
    fun lateNightPicksSnacks() = assertEquals("snacks", defaultAddMealId(meals, 23))

    @Test
    fun matchesByTitleWhenIdIsCustom() {
        val custom = listOf(meal("meal-1", "Morning"), meal("meal-2", "Lunch plate"))
        assertEquals("meal-2", defaultAddMealId(custom, 13))
    }

    @Test
    fun fallsBackToFirstWhenNoKeywordMatch() {
        val custom = listOf(meal("meal-1", "Fuel"), meal("meal-2", "Refuel"))
        assertEquals("meal-1", defaultAddMealId(custom, 13))
    }

    @Test
    fun emptyReturnsNull() = assertNull(defaultAddMealId(emptyList(), 8))
}
