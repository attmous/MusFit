package com.musfit.domain.health

data class HealthConnectStatus(
    val availability: HealthConnectAvailability,
    val grantedPermissions: Set<String>,
)

enum class HealthConnectAvailability {
    Available,
    NotInstalled,
    NotSupported,
}

data class ImportedDailyHealthSummary(
    val steps: Long?,
    val activeCaloriesKcal: Double?,
    val latestWeightKg: Double?,
    val restingHeartRateBpm: Long?,
)
