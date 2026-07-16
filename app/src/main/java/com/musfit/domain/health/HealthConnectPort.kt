package com.musfit.domain.health

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate

/** Inward-facing Health Connect boundary. It contains no Room or platform types. */
interface HealthConnectGateway {
    suspend fun status(): HealthConnectStatus
    suspend fun requestablePermissions(): Set<String>
    suspend fun foodRequestablePermissions(): Set<String>

    suspend fun readDailySummary(
        date: LocalDate,
        preferredStepsPackage: String? = null,
    ): HealthConnectDailyReadResult

    suspend fun readDailySummaries(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        preferredStepsPackage: String? = null,
    ): Map<LocalDate, HealthConnectDailyReadResult> {
        require(!endDateInclusive.isBefore(startDate)) { "endDateInclusive must not precede startDate" }
        return generateSequence(startDate) { date -> date.plusDays(1) }
            .takeWhile { date -> !date.isAfter(endDateInclusive) }
            .associateWith { date -> readDailySummary(date, preferredStepsPackage) }
    }

    suspend fun readStepSources(date: LocalDate): List<StepSource> = emptyList()

    suspend fun exportWorkout(
        workout: HealthConnectWorkoutExport,
        identity: HealthConnectRecordIdentity,
    ): String?

    suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult?

    suspend fun deleteAuthoredRecords(records: Set<HealthConnectAuthoredRecord>): HealthConnectDeleteResult = HealthConnectDeleteResult.Failure("Health Connect authored-record deletion is not implemented.")
}

data class HealthConnectWorkoutExport(
    val accountId: String,
    val localSessionId: String,
    val title: String?,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val notes: String?,
    val sets: List<HealthConnectWorkoutSetExport>,
)

data class HealthConnectWorkoutSetExport(
    val localSetId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val setType: String,
    val reps: Int?,
    val weightKg: Double?,
    val durationSeconds: Long?,
    val distanceMeters: Double?,
    val rpe: Double?,
    val notes: String?,
    val completed: Boolean,
    val supersetGroupId: String?,
    val restSeconds: Int?,
)

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

enum class HealthConnectAuthoredRecordType { Workout, Nutrition, Hydration }

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

    data class Complete(override val deletedRecords: Set<HealthConnectAuthoredRecord>) : HealthConnectDeleteResult
    data class Partial(
        override val deletedRecords: Set<HealthConnectAuthoredRecord>,
        val failures: List<HealthConnectDeleteFailure>,
    ) : HealthConnectDeleteResult
    data class Unavailable(val message: String) : HealthConnectDeleteResult {
        override val deletedRecords: Set<HealthConnectAuthoredRecord> = emptySet()
    }
    data class Failure(val message: String) : HealthConnectDeleteResult {
        override val deletedRecords: Set<HealthConnectAuthoredRecord> = emptySet()
    }
}

data class HealthConnectRecordIdentity(
    val clientRecordId: String,
    val clientRecordVersion: Long,
) {
    init {
        require(clientRecordVersion > 0) { "Health Connect record versions must be positive" }
    }

    companion object {
        fun forWorkout(accountId: String, sessionId: String, version: Long) = create("workout", accountId, sessionId, version)
        fun forNutrition(accountId: String, mealId: String, version: Long) = create("nutrition", accountId, mealId, version)
        fun forHydration(accountId: String, date: LocalDate, version: Long) = create("hydration", accountId, date.toString(), version)

        private fun create(recordType: String, accountId: String, localEntityId: String, version: Long): HealthConnectRecordIdentity {
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
            return HealthConnectRecordIdentity("musfit-v1-$recordType-$digest", version)
        }
    }
}

fun workoutExportFingerprint(workout: HealthConnectWorkoutExport): String = fingerprint {
    component(workout.localSessionId)
    component(workout.title)
    component(workout.startedAtEpochMillis)
    component(workout.endedAtEpochMillis)
    component(workout.notes)
    workout.sets.sortedWith(compareBy<HealthConnectWorkoutSetExport> { it.sortOrder }.thenBy { it.localSetId }).forEach { set ->
        component(set.localSetId)
        component(set.exerciseId)
        component(set.sortOrder)
        component(set.setType)
        component(set.reps)
        component(set.weightKg?.toBits())
        component(set.durationSeconds)
        component(set.distanceMeters?.toBits())
        component(set.rpe?.toBits())
        component(set.notes)
        component(set.completed)
        component(set.supersetGroupId)
        component(set.restSeconds)
    }
}

fun nutritionExportFingerprint(date: LocalDate, meal: HealthConnectFoodMealExport): String = fingerprint {
    component(date)
    component(meal.localMealId)
    component(meal.mealType)
    component(meal.name)
    component(meal.caloriesKcal.toBits())
    component(meal.proteinGrams.toBits())
    component(meal.carbsGrams.toBits())
    component(meal.fatGrams.toBits())
    component(meal.fiberGrams.toBits())
    component(meal.sugarGrams.toBits())
    component(meal.saturatedFatGrams.toBits())
    component(meal.sodiumMilligrams.toBits())
    component(meal.potassiumMilligrams.toBits())
    component(meal.calciumMilligrams.toBits())
    component(meal.ironMilligrams.toBits())
    component(meal.vitaminDMicrograms.toBits())
    component(meal.vitaminCMilligrams.toBits())
    component(meal.magnesiumMilligrams.toBits())
}

fun hydrationExportFingerprint(date: LocalDate, milliliters: Double): String = fingerprint {
    component(date)
    component(milliliters.toBits())
}

private inline fun fingerprint(block: FingerprintBuilder.() -> Unit): String = FingerprintBuilder().apply(block).digest()

private class FingerprintBuilder {
    private val canonical = StringBuilder()
    fun component(value: Any?) {
        val encoded = value?.toString() ?: "<null>"
        canonical.append(encoded.length).append(':').append(encoded)
    }
    fun digest(): String = MessageDigest.getInstance("SHA-256")
        .digest(canonical.toString().toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
