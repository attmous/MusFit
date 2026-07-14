package com.musfit.integrations.healthconnect

enum class HealthConnectAuthoredRecordType {
    Workout,
    Nutrition,
    Hydration,
}

data class HealthConnectAuthoredRecord(
    val type: HealthConnectAuthoredRecordType,
    val clientRecordId: String,
)

data class HealthConnectDeleteFailure(
    val type: HealthConnectAuthoredRecordType,
    val message: String,
)

sealed interface HealthConnectDeleteResult {
    val deletedRecords: Set<HealthConnectAuthoredRecord>

    data class Complete(
        override val deletedRecords: Set<HealthConnectAuthoredRecord>,
    ) : HealthConnectDeleteResult

    data class Partial(
        override val deletedRecords: Set<HealthConnectAuthoredRecord>,
        val failures: List<HealthConnectDeleteFailure>,
    ) : HealthConnectDeleteResult

    data class Unavailable(
        val message: String,
    ) : HealthConnectDeleteResult {
        override val deletedRecords: Set<HealthConnectAuthoredRecord> = emptySet()
    }

    data class Failure(
        val message: String,
    ) : HealthConnectDeleteResult {
        override val deletedRecords: Set<HealthConnectAuthoredRecord> = emptySet()
    }
}
