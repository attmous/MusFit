package com.musfit.integrations.healthconnect

import java.time.LocalDate

data class HealthConnectFoodExportPayload(
    val date: LocalDate,
    val meals: List<HealthConnectFoodMealExport>,
    val hydrationMilliliters: Double,
)

data class HealthConnectFoodMealExport(
    val mealType: String,
    val name: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val sodiumMilligrams: Double = 0.0,
    val potassiumMilligrams: Double = 0.0,
    val calciumMilligrams: Double = 0.0,
    val ironMilligrams: Double = 0.0,
    val vitaminDMicrograms: Double = 0.0,
    val vitaminCMilligrams: Double = 0.0,
    val magnesiumMilligrams: Double = 0.0,
)

data class HealthConnectFoodExportResult(
    val nutritionRecordCount: Int,
    val hydrationRecordCount: Int,
)
