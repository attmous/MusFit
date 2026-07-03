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

data class ImportedBodyMetric(
    val type: String,
    val value: Double,
    val unit: String,
    val measuredAtEpochMillis: Long,
    val externalId: String?,
)

data class ImportedDailyHealthSummary(
    val steps: Long? = null,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val distanceMeters: Double? = null,
    val sleepMinutes: Long? = null,
    val exerciseMinutes: Long? = null,
    val exerciseSessionCount: Int? = null,
    val latestWeightKg: Double? = null,
    val latestBodyFatPercent: Double? = null,
    val restingHeartRateBpm: Long? = null,
    val bodyMetrics: List<ImportedBodyMetric> = emptyList(),
)
