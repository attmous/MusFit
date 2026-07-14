package com.musfit.integrations.healthconnect

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate

data class HealthConnectRecordIdentity(
    val clientRecordId: String,
    val clientRecordVersion: Long,
) {
    init {
        require(clientRecordVersion > 0) { "Health Connect record versions must be positive" }
    }

    companion object {
        fun forWorkout(accountId: String, sessionId: String, version: Long): HealthConnectRecordIdentity = create("workout", accountId, sessionId, version)

        fun forNutrition(accountId: String, mealId: String, version: Long): HealthConnectRecordIdentity = create("nutrition", accountId, mealId, version)

        fun forHydration(accountId: String, date: LocalDate, version: Long): HealthConnectRecordIdentity = create("hydration", accountId, date.toString(), version)

        private fun create(
            recordType: String,
            accountId: String,
            localEntityId: String,
            version: Long,
        ): HealthConnectRecordIdentity {
            require(accountId.isNotBlank()) { "Health Connect identity requires an account ID" }
            require(localEntityId.isNotBlank()) { "Health Connect identity requires a local entity ID" }
            val canonical = buildString {
                append(recordType.length).append(':').append(recordType)
                append(accountId.length).append(':').append(accountId)
                append(localEntityId.length).append(':').append(localEntityId)
            }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(canonical.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
            return HealthConnectRecordIdentity(
                clientRecordId = "musfit-v1-$recordType-$digest",
                clientRecordVersion = version,
            )
        }
    }
}
