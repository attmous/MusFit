package com.musfit.domain.profile

enum class Sex { Male, Female }

enum class ActivityLevel(val factor: Double) {
    Sedentary(1.2),
    Light(1.375),
    Moderate(1.55),
    Active(1.725),
    VeryActive(1.9),
}

enum class GoalType { Lose, Maintain, Gain }

data class RecommendedTargets(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
)
