package com.musfit.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyCalculatorTest {
    @Test
    fun bmr_usesMifflinStJeorForMale() {
        val bmr = EnergyCalculator.basalMetabolicRate(Sex.Male, weightKg = 80.0, heightCm = 180.0, ageYears = 30)
        assertEquals(1780.0, bmr, 0.5)
    }

    @Test
    fun bmr_usesMifflinStJeorForFemale() {
        val bmr = EnergyCalculator.basalMetabolicRate(Sex.Female, weightKg = 65.0, heightCm = 168.0, ageYears = 30)
        assertEquals(1389.0, bmr, 0.5)
    }

    @Test
    fun maintainTargets_equalTdeeAndSplitMacros() {
        val targets = EnergyCalculator.recommendedTargets(
            sex = Sex.Male, weightKg = 80.0, heightCm = 180.0, ageYears = 30,
            activityLevel = ActivityLevel.Moderate, goalType = GoalType.Maintain, goalPaceKgPerWeek = 0.0,
        )
        assertEquals(2759.0, targets.caloriesKcal, 1.0)
        assertEquals(144.0, targets.proteinGrams, 1.0)
        assertEquals(77.0, targets.fatGrams, 1.0)
        assertTrue(targets.carbsGrams > 0.0)
    }

    @Test
    fun loseGoal_subtractsPaceEnergyFromTdee() {
        val maintain = EnergyCalculator.recommendedTargets(
            Sex.Male, 80.0, 180.0, 30, ActivityLevel.Moderate, GoalType.Maintain, 0.0,
        )
        val lose = EnergyCalculator.recommendedTargets(
            Sex.Male, 80.0, 180.0, 30, ActivityLevel.Moderate, GoalType.Lose, 0.5,
        )
        assertEquals(550.0, maintain.caloriesKcal - lose.caloriesKcal, 1.0)
    }

    @Test
    fun loseGoal_neverDropsBelowSafeFloor() {
        val targets = EnergyCalculator.recommendedTargets(
            Sex.Female, 45.0, 150.0, 25, ActivityLevel.Sedentary, GoalType.Lose, 1.0,
        )
        assertTrue(targets.caloriesKcal >= 1200.0)
    }
}
