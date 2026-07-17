package com.musfit.domain.profile

import kotlin.math.max
import kotlin.math.roundToInt

object EnergyCalculator {
    private const val KCAL_PER_KG = 7700.0
    private const val MIN_CALORIES = 1200.0
    private const val PROTEIN_G_PER_KG = 1.8
    private const val FAT_FRACTION = 0.25
    private const val KCAL_PER_G_PROTEIN = 4.0
    private const val KCAL_PER_G_CARB = 4.0
    private const val KCAL_PER_G_FAT = 9.0

    fun basalMetabolicRate(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            Sex.Male -> base + 5.0
            Sex.Female -> base - 161.0
        }
    }

    fun totalDailyEnergyExpenditure(bmr: Double, activityLevel: ActivityLevel): Double = bmr * activityLevel.factor

    fun recommendedTargets(
        sex: Sex,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        activityLevel: ActivityLevel,
        goalType: GoalType,
        goalPaceKgPerWeek: Double,
    ): RecommendedTargets {
        val tdee = totalDailyEnergyExpenditure(basalMetabolicRate(sex, weightKg, heightCm, ageYears), activityLevel)
        val dailyAdjustment = goalPaceKgPerWeek * KCAL_PER_KG / 7.0
        val targetCalories = when (goalType) {
            GoalType.Maintain -> tdee
            GoalType.Lose -> max(MIN_CALORIES, tdee - dailyAdjustment)
            GoalType.Gain -> tdee + dailyAdjustment
        }
        val proteinGrams = PROTEIN_G_PER_KG * weightKg
        val fatGrams = FAT_FRACTION * targetCalories / KCAL_PER_G_FAT
        val remainingKcal = max(0.0, targetCalories - proteinGrams * KCAL_PER_G_PROTEIN - fatGrams * KCAL_PER_G_FAT)
        val carbsGrams = remainingKcal / KCAL_PER_G_CARB
        return RecommendedTargets(
            caloriesKcal = targetCalories.roundToInt().toDouble(),
            proteinGrams = proteinGrams.roundToInt().toDouble(),
            carbsGrams = carbsGrams.roundToInt().toDouble(),
            fatGrams = fatGrams.roundToInt().toDouble(),
        )
    }
}
