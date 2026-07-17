package com.musfit.integrations.healthconnect

import com.musfit.domain.health.HealthConnectFoodMealExport
import com.musfit.domain.health.HealthConnectWorkoutExport
import java.time.LocalDate

internal fun workoutExportFingerprint(workout: HealthConnectWorkoutExport): String = com.musfit.domain.health.workoutExportFingerprint(workout)

internal fun nutritionExportFingerprint(date: LocalDate, meal: HealthConnectFoodMealExport): String = com.musfit.domain.health.nutritionExportFingerprint(date, meal)

internal fun hydrationExportFingerprint(date: LocalDate, milliliters: Double): String = com.musfit.domain.health.hydrationExportFingerprint(date, milliliters)
