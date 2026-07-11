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

internal enum class HealthPermissionAccess {
    Read,
    Write,
}

internal enum class HealthPermissionRequestGroup {
    HealthAndWorkout,
    Food,
}

internal data class HealthPermissionRationaleItem(
    val permission: String,
    val access: HealthPermissionAccess,
    val label: String,
    val purpose: String,
    private val requestGroup: HealthPermissionRequestGroup,
) {
    internal fun belongsTo(group: HealthPermissionRequestGroup): Boolean = requestGroup == group
}

internal object HealthPermissionInventory {
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
        readItem(readStepsPermission, "Steps", "Show daily movement and progress."),
        readItem(
            readActiveCaloriesPermission,
            "Active calories",
            "Show energy burned through activity.",
        ),
        readItem(
            readTotalCaloriesPermission,
            "Total calories",
            "Show total energy burned during the day.",
        ),
        readItem(readDistancePermission, "Distance", "Show distance-based activity progress."),
        readItem(readSleepPermission, "Sleep", "Show sleep duration in your health summary."),
        readItem(
            readExercisePermission,
            "Exercise sessions",
            "Show completed exercise and time spent training.",
        ),
        readItem(readWeightPermission, "Weight", "Show weight measurements and trends."),
        readItem(readBodyFatPermission, "Body fat", "Show body-fat measurements and trends."),
        readItem(
            readRestingHeartRatePermission,
            "Resting heart rate",
            "Show resting heart-rate trends.",
        ),
        readItem(
            readHeartRateVariabilityPermission,
            "Heart rate variability",
            "Show recovery trends from heart-rate variability.",
        ),
        writeItem(
            permission = writeExercisePermission,
            label = "Workouts",
            purpose = "Save workouts you log in MusFit.",
            requestGroup = HealthPermissionRequestGroup.HealthAndWorkout,
        ),
        writeItem(
            permission = writeNutritionPermission,
            label = "Meals and nutrition",
            purpose = "Save meals and their nutrition when you choose Food sync.",
            requestGroup = HealthPermissionRequestGroup.Food,
        ),
        writeItem(
            permission = writeHydrationPermission,
            label = "Water",
            purpose = "Save water totals when you choose Food sync.",
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
        label: String,
        purpose: String,
    ) = HealthPermissionRationaleItem(
        permission = permission,
        access = HealthPermissionAccess.Read,
        label = label,
        purpose = purpose,
        requestGroup = HealthPermissionRequestGroup.HealthAndWorkout,
    )

    private fun writeItem(
        permission: String,
        label: String,
        purpose: String,
        requestGroup: HealthPermissionRequestGroup,
    ) = HealthPermissionRationaleItem(
        permission = permission,
        access = HealthPermissionAccess.Write,
        label = label,
        purpose = purpose,
        requestGroup = requestGroup,
    )
}
