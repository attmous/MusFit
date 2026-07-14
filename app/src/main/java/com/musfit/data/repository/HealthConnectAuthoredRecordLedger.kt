package com.musfit.data.repository

import com.musfit.data.local.entity.HealthConnectExportRecordEntity
import com.musfit.integrations.healthconnect.HealthConnectAuthoredRecord
import com.musfit.integrations.healthconnect.HealthConnectAuthoredRecordType

internal fun HealthConnectExportRecordEntity.toAuthoredRecordOrNull(): HealthConnectAuthoredRecord? {
    val type = when (recordType) {
        "workout" -> HealthConnectAuthoredRecordType.Workout
        "nutrition" -> HealthConnectAuthoredRecordType.Nutrition
        "hydration" -> HealthConnectAuthoredRecordType.Hydration
        else -> return null
    }
    return HealthConnectAuthoredRecord(type = type, clientRecordId = clientRecordId)
}
