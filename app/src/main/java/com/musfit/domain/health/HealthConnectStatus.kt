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
    val hrvRmssdMillis: Double? = null,
    val bodyMetrics: List<ImportedBodyMetric> = emptyList(),
)

enum class HealthConnectMetric {
    Steps,
    ActiveCalories,
    TotalCalories,
    Distance,
    Sleep,
    ExerciseDuration,
    ExerciseSessions,
    Weight,
    BodyFat,
    RestingHeartRate,
    HeartRateVariability,
}

data class HealthConnectMetricFailure(
    val metric: HealthConnectMetric,
    val message: String,
)

enum class HealthConnectUnavailableReason {
    ProviderUnavailable,
    PermissionsUnavailable,
}

sealed interface HealthConnectDailyReadResult {
    val summary: ImportedDailyHealthSummary
    val status: HealthConnectStatus?

    val steps: Long? get() = summary.steps
    val activeCaloriesKcal: Double? get() = summary.activeCaloriesKcal
    val totalCaloriesKcal: Double? get() = summary.totalCaloriesKcal
    val distanceMeters: Double? get() = summary.distanceMeters
    val sleepMinutes: Long? get() = summary.sleepMinutes
    val exerciseMinutes: Long? get() = summary.exerciseMinutes
    val exerciseSessionCount: Int? get() = summary.exerciseSessionCount
    val latestWeightKg: Double? get() = summary.latestWeightKg
    val latestBodyFatPercent: Double? get() = summary.latestBodyFatPercent
    val restingHeartRateBpm: Long? get() = summary.restingHeartRateBpm
    val hrvRmssdMillis: Double? get() = summary.hrvRmssdMillis
    val bodyMetrics: List<ImportedBodyMetric> get() = summary.bodyMetrics

    data class Complete(
        override val summary: ImportedDailyHealthSummary,
        val completedMetrics: Set<HealthConnectMetric>,
        override val status: HealthConnectStatus,
    ) : HealthConnectDailyReadResult

    data class Partial(
        override val summary: ImportedDailyHealthSummary,
        val completedMetrics: Set<HealthConnectMetric>,
        val failures: List<HealthConnectMetricFailure>,
        override val status: HealthConnectStatus,
    ) : HealthConnectDailyReadResult

    data class Empty(
        val completedMetrics: Set<HealthConnectMetric>,
        override val status: HealthConnectStatus,
    ) : HealthConnectDailyReadResult {
        override val summary: ImportedDailyHealthSummary = ImportedDailyHealthSummary()
    }

    data class Unavailable(
        override val status: HealthConnectStatus,
        val reason: HealthConnectUnavailableReason,
        val message: String,
    ) : HealthConnectDailyReadResult {
        override val summary: ImportedDailyHealthSummary = ImportedDailyHealthSummary()
    }

    data class Failure(
        val message: String,
        override val status: HealthConnectStatus? = null,
    ) : HealthConnectDailyReadResult {
        override val summary: ImportedDailyHealthSummary = ImportedDailyHealthSummary()
    }
}
