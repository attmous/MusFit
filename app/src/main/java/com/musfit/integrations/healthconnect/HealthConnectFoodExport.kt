package com.musfit.integrations.healthconnect

import java.time.LocalDate

data class HealthConnectFoodExportPayload(
    val accountId: String = "local-default",
    val date: LocalDate,
    val meals: List<HealthConnectFoodMealExport>,
    val hydrationMilliliters: Double,
    val hydrationClientRecordVersion: Long = 1,
)

data class HealthConnectFoodMealExport(
    val mealType: String,
    val accountId: String = "local-default",
    val localMealId: String = mealType,
    val clientRecordVersion: Long = 1,
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
    val nutritionProviderRecordIds: Map<String, String> = emptyMap(),
    val hydrationProviderRecordId: String? = null,
)
