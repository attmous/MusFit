package com.musfit.integrations.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord

enum class HealthPermissionAccess {
    Read,
    Write,
}

enum class HealthPermissionRationale {
    Steps,
    ActiveCalories,
    TotalCalories,
    Distance,
    Sleep,
    ExerciseSessions,
    Weight,
    BodyFat,
    RestingHeartRate,
    HeartRateVariability,
    Workouts,
    MealsAndNutrition,
    Water,
}

internal enum class HealthPermissionRequestGroup {
    HealthAndWorkout,
    Food,
}

class HealthPermissionRationaleItem internal constructor(
    val permission: String,
    val access: HealthPermissionAccess,
    val rationale: HealthPermissionRationale,
    private val requestGroup: HealthPermissionRequestGroup,
) {
    internal fun belongsTo(group: HealthPermissionRequestGroup): Boolean = requestGroup == group
}

object HealthPermissionInventory {
    val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    val readActiveCaloriesPermission =
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    val readTotalCaloriesPermission =
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    val readDistancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    val readSleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    val readExercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    val readWeightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    val readBodyFatPermission = HealthPermission.getReadPermission(BodyFatRecord::class)
    val readRestingHeartRatePermission =
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    val readHeartRateVariabilityPermission =
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    val writeExercisePermission = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    val writeNutritionPermission = HealthPermission.getWritePermission(NutritionRecord::class)
    val writeHydrationPermission = HealthPermission.getWritePermission(HydrationRecord::class)

    val rationaleItems: List<HealthPermissionRationaleItem> = listOf(
        readItem(readStepsPermission, HealthPermissionRationale.Steps),
        readItem(
            readActiveCaloriesPermission,
            HealthPermissionRationale.ActiveCalories,
        ),
        readItem(
            readTotalCaloriesPermission,
            HealthPermissionRationale.TotalCalories,
        ),
        readItem(readDistancePermission, HealthPermissionRationale.Distance),
        readItem(readSleepPermission, HealthPermissionRationale.Sleep),
        readItem(
            readExercisePermission,
            HealthPermissionRationale.ExerciseSessions,
        ),
        readItem(readWeightPermission, HealthPermissionRationale.Weight),
        readItem(readBodyFatPermission, HealthPermissionRationale.BodyFat),
        readItem(
            readRestingHeartRatePermission,
            HealthPermissionRationale.RestingHeartRate,
        ),
        readItem(
            readHeartRateVariabilityPermission,
            HealthPermissionRationale.HeartRateVariability,
        ),
        writeItem(
            permission = writeExercisePermission,
            rationale = HealthPermissionRationale.Workouts,
            requestGroup = HealthPermissionRequestGroup.HealthAndWorkout,
        ),
        writeItem(
            permission = writeNutritionPermission,
            rationale = HealthPermissionRationale.MealsAndNutrition,
            requestGroup = HealthPermissionRequestGroup.Food,
        ),
        writeItem(
            permission = writeHydrationPermission,
            rationale = HealthPermissionRationale.Water,
            requestGroup = HealthPermissionRequestGroup.Food,
        ),
    )

    val healthAndWorkoutPermissions: Set<String> = rationaleItems
        .filter { it.belongsTo(HealthPermissionRequestGroup.HealthAndWorkout) }
        .mapTo(linkedSetOf()) { it.permission }

    val foodPermissions: Set<String> = rationaleItems
        .filter { it.belongsTo(HealthPermissionRequestGroup.Food) }
        .mapTo(linkedSetOf()) { it.permission }

    private fun readItem(
        permission: String,
        rationale: HealthPermissionRationale,
    ) = HealthPermissionRationaleItem(
        permission = permission,
        access = HealthPermissionAccess.Read,
        rationale = rationale,
        requestGroup = HealthPermissionRequestGroup.HealthAndWorkout,
    )

    private fun writeItem(
        permission: String,
        rationale: HealthPermissionRationale,
        requestGroup: HealthPermissionRequestGroup,
    ) = HealthPermissionRationaleItem(
        permission = permission,
        access = HealthPermissionAccess.Write,
        rationale = rationale,
        requestGroup = requestGroup,
    )
}
