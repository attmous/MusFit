package com.musfit.integrations.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.musfit.domain.health.HealthConnectAvailability
import com.musfit.domain.health.HealthConnectDailyReadResult
import com.musfit.domain.health.HealthConnectDeleteResult
import com.musfit.domain.health.HealthConnectMetric
import com.musfit.domain.health.HealthConnectMetricFailure
import com.musfit.domain.health.HealthConnectStatus
import com.musfit.domain.health.HealthConnectUnavailableReason
import com.musfit.domain.health.ImportedBodyMetric
import com.musfit.domain.health.ImportedDailyHealthSummary
import com.musfit.domain.health.StepSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
class HealthConnectManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HealthConnectGateway {
    private var statusReader: suspend () -> HealthConnectStatus = { readStatus(context) }
    private var clientFactory: () -> HealthConnectClientAdapter = {
        DefaultHealthConnectClientAdapter(HealthConnectClient.getOrCreate(context))
    }

    internal constructor(
        context: Context,
        statusReader: suspend () -> HealthConnectStatus,
        clientFactory: () -> HealthConnectClientAdapter,
    ) : this(context) {
        this.statusReader = statusReader
        this.clientFactory = clientFactory
    }

    private val permissions = HealthPermissionInventory.healthAndWorkoutPermissions
    private val foodPermissions = HealthPermissionInventory.foodPermissions

    override suspend fun status(): HealthConnectStatus = statusReader()

    override suspend fun requestablePermissions(): Set<String> = permissions

    override suspend fun foodRequestablePermissions(): Set<String> = foodPermissions

    override suspend fun readDailySummary(
        date: LocalDate,
        preferredStepsPackage: String?,
    ): HealthConnectDailyReadResult = readDailySummaries(date, date, preferredStepsPackage).getValue(date)

    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount", "TooGenericExceptionCaught")
    override suspend fun readDailySummaries(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        preferredStepsPackage: String?,
    ): Map<LocalDate, HealthConnectDailyReadResult> {
        val range = HealthConnectDateRange(startDate, endDateInclusive, ZoneId.systemDefault())
        val currentStatus = try {
            status()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return range.sameResult(HealthConnectDailyReadResult.Failure(failure.healthConnectMessage()))
        }
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return range.sameResult(
                HealthConnectDailyReadResult.Unavailable(
                    status = currentStatus,
                    reason = HealthConnectUnavailableReason.ProviderUnavailable,
                    message = "Health Connect is unavailable.",
                ),
            )
        }

        val grantedPermissions = currentStatus.grantedPermissions
        val canReadSteps = READ_STEPS_PERMISSION in grantedPermissions
        val canReadActiveCalories = READ_ACTIVE_CALORIES_PERMISSION in grantedPermissions
        val canReadTotalCalories = READ_TOTAL_CALORIES_PERMISSION in grantedPermissions
        val canReadDistance = READ_DISTANCE_PERMISSION in grantedPermissions
        val canReadSleep = READ_SLEEP_PERMISSION in grantedPermissions
        val canReadExercise = READ_EXERCISE_PERMISSION in grantedPermissions
        val canReadWeight = READ_WEIGHT_PERMISSION in grantedPermissions
        val canReadBodyFat = READ_BODY_FAT_PERMISSION in grantedPermissions
        val canReadRestingHeartRate = READ_RESTING_HEART_RATE_PERMISSION in grantedPermissions
        val canReadHeartRateVariability = READ_HEART_RATE_VARIABILITY_PERMISSION in grantedPermissions

        if (
            !canReadSteps &&
            !canReadActiveCalories &&
            !canReadTotalCalories &&
            !canReadDistance &&
            !canReadSleep &&
            !canReadExercise &&
            !canReadWeight &&
            !canReadBodyFat &&
            !canReadRestingHeartRate &&
            !canReadHeartRateVariability
        ) {
            return range.sameResult(
                HealthConnectDailyReadResult.Unavailable(
                    status = currentStatus,
                    reason = HealthConnectUnavailableReason.PermissionsUnavailable,
                    message = "Health Connect read permissions are unavailable.",
                ),
            )
        }

        val client = try {
            clientFactory()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return range.sameResult(
                HealthConnectDailyReadResult.Failure(
                    message = failure.healthConnectMessage(),
                    status = currentStatus,
                ),
            )
        }
        val completedMetrics = range.dates.associateWith { linkedSetOf<HealthConnectMetric>() }
        val failures = range.dates.associateWith { mutableListOf<HealthConnectMetricFailure>() }

        val steps = if (canReadSteps) {
            readMetricByDay(HealthConnectMetric.Steps, range, completedMetrics, failures) {
                if (preferredStepsPackage != null) {
                    client.aggregateStepsForOriginsByDay(range, setOf(preferredStepsPackage))
                } else {
                    client.aggregateStepsByDay(range)
                }
            }
        } else {
            emptyMap()
        }

        val activeCalories = if (canReadActiveCalories) {
            readMetricByDay(HealthConnectMetric.ActiveCalories, range, completedMetrics, failures) {
                client.aggregateActiveCaloriesByDay(range)
            }
        } else {
            emptyMap()
        }

        val totalCalories = if (canReadTotalCalories) {
            readMetricByDay(HealthConnectMetric.TotalCalories, range, completedMetrics, failures) {
                client.aggregateTotalCaloriesByDay(range)
            }
        } else {
            emptyMap()
        }

        val distance = if (canReadDistance) {
            readMetricByDay(HealthConnectMetric.Distance, range, completedMetrics, failures) {
                client.aggregateDistanceMetersByDay(range)
            }
        } else {
            emptyMap()
        }

        val sleepMinutes = if (canReadSleep) {
            readMetricByDay(HealthConnectMetric.Sleep, range, completedMetrics, failures) {
                client.aggregateSleepMinutesByDay(range)
            }
        } else {
            emptyMap()
        }

        val exerciseMinutes = if (canReadExercise) {
            readMetricByDay(HealthConnectMetric.ExerciseDuration, range, completedMetrics, failures) {
                client.aggregateExerciseMinutesByDay(range)
            }
        } else {
            emptyMap()
        }

        val exerciseSessionCount = if (canReadExercise) {
            readMetricByDay(HealthConnectMetric.ExerciseSessions, range, completedMetrics, failures) {
                client.readExerciseSessionCountByDay(range)
            }
        } else {
            emptyMap()
        }

        val latestWeight = if (canReadWeight) {
            readMetricByDay(HealthConnectMetric.Weight, range, completedMetrics, failures) {
                client.readLatestWeightMetricByDay(range)
            }
        } else {
            emptyMap()
        }

        val latestBodyFat = if (canReadBodyFat) {
            readMetricByDay(HealthConnectMetric.BodyFat, range, completedMetrics, failures) {
                client.readLatestBodyFatMetricByDay(range)
            }
        } else {
            emptyMap()
        }

        val restingHeartRate = if (canReadRestingHeartRate) {
            readMetricByDay(HealthConnectMetric.RestingHeartRate, range, completedMetrics, failures) {
                client.readLatestRestingHeartRateByDay(range)
            }
        } else {
            emptyMap()
        }

        val hrvRmssd = if (canReadHeartRateVariability) {
            readMetricByDay(HealthConnectMetric.HeartRateVariability, range, completedMetrics, failures) {
                client.readLatestHeartRateVariabilityRmssdMillisByDay(range)
            }
        } else {
            emptyMap()
        }

        return range.dates.associateWith { date ->
            val summary = ImportedDailyHealthSummary(
                steps = steps[date],
                activeCaloriesKcal = activeCalories[date],
                totalCaloriesKcal = totalCalories[date],
                distanceMeters = distance[date],
                sleepMinutes = sleepMinutes[date],
                exerciseMinutes = exerciseMinutes[date],
                exerciseSessionCount = exerciseSessionCount[date],
                latestWeightKg = latestWeight[date]?.value,
                latestBodyFatPercent = latestBodyFat[date]?.value,
                restingHeartRateBpm = restingHeartRate[date],
                hrvRmssdMillis = hrvRmssd[date],
                bodyMetrics = listOfNotNull(latestWeight[date], latestBodyFat[date]),
            )
            dailyReadResult(summary, completedMetrics.getValue(date), failures.getValue(date), currentStatus)
        }
    }

    override suspend fun readStepSources(date: LocalDate): List<StepSource> {
        val currentStatus = status()
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return emptyList()
        }
        if (READ_STEPS_PERMISSION !in currentStatus.grantedPermissions) {
            return emptyList()
        }

        val client = clientFactory()
        val range = date.asHealthConnectTimeRange()
        val byOrigin = healthConnectCatchingOrNull { client.readStepCountsByOrigin(range) }.orEmpty()
        return byOrigin
            .map { (packageName, steps) ->
                StepSource(
                    packageName = packageName,
                    label = labelForStepSource(packageName),
                    steps = steps,
                )
            }
            .sortedByDescending { it.steps }
    }

    private fun labelForStepSource(packageName: String): String = when {
        packageName == ON_DEVICE_STEPS_PACKAGE ||
            packageName.startsWith(ON_DEVICE_SPN_PREFIX) -> "Your phone"

        else -> runCatching {
            val packageManager = context.packageManager
            packageManager
                .getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
        }.getOrNull()?.takeIf { it.isNotBlank() && it != packageName } ?: packageName
    }

    override suspend fun exportWorkout(
        workout: HealthConnectWorkoutExport,
        identity: HealthConnectRecordIdentity,
    ): String? {
        val currentStatus = status()
        if (
            currentStatus.availability != HealthConnectAvailability.Available ||
            WRITE_EXERCISE_PERMISSION !in currentStatus.grantedPermissions
        ) {
            return null
        }

        return healthConnectCatchingOrNull {
            clientFactory().insertExerciseSession(
                HealthConnectRecordMapper.toExerciseSessionRecord(workout, identity = identity),
            )
        }
    }

    override suspend fun exportFood(payload: HealthConnectFoodExportPayload): HealthConnectFoodExportResult? {
        val currentStatus = status()
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return null
        }

        val canWriteNutrition = WRITE_NUTRITION_PERMISSION in currentStatus.grantedPermissions
        val canWriteHydration = WRITE_HYDRATION_PERMISSION in currentStatus.grantedPermissions
        if (!canWriteNutrition && !canWriteHydration) {
            return null
        }

        val nutritionExports = payload.writeableNutritionExports(canWriteNutrition)
        val nutritionRecords = payload.toNutritionRecords(nutritionExports)
        val hydrationRecord = payload.toHydrationRecord(canWriteHydration)

        if (nutritionRecords.isEmpty() && hydrationRecord == null) {
            return HealthConnectFoodExportResult(nutritionRecordCount = 0, hydrationRecordCount = 0)
        }

        return healthConnectCatchingOrNull { insertFoodRecords(nutritionExports, nutritionRecords, hydrationRecord) }
    }

    @Suppress("CyclomaticComplexMethod", "ReturnCount", "TooGenericExceptionCaught")
    override suspend fun deleteAuthoredRecords(records: Set<HealthConnectAuthoredRecord>): HealthConnectDeleteResult {
        if (records.isEmpty()) return HealthConnectDeleteResult.Complete(emptySet())
        val currentStatus = try {
            status()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return HealthConnectDeleteResult.Failure(failure.healthConnectMessage())
        }
        if (currentStatus.availability != HealthConnectAvailability.Available) {
            return HealthConnectDeleteResult.Unavailable("Health Connect is unavailable.")
        }

        val groupedRecords = records.groupBy(HealthConnectAuthoredRecord::type)
        val permittedTypes = groupedRecords.keys.filter { type ->
            type.requiredWritePermission() in currentStatus.grantedPermissions
        }
        if (permittedTypes.isEmpty()) {
            return HealthConnectDeleteResult.Unavailable("Health Connect write permissions are unavailable.")
        }
        val client = try {
            clientFactory()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return HealthConnectDeleteResult.Failure(failure.healthConnectMessage())
        }

        val deletedRecords = linkedSetOf<HealthConnectAuthoredRecord>()
        val failures = mutableListOf<HealthConnectDeleteFailure>()
        HealthConnectAuthoredRecordType.entries.forEach { type ->
            val typedRecords = groupedRecords[type].orEmpty()
            if (typedRecords.isEmpty()) return@forEach
            if (type !in permittedTypes) {
                failures += HealthConnectDeleteFailure(type, "Health Connect write permission is unavailable.")
                return@forEach
            }
            try {
                val clientRecordIds = typedRecords.map(HealthConnectAuthoredRecord::clientRecordId).sorted()
                when (type) {
                    HealthConnectAuthoredRecordType.Workout -> client.deleteExerciseSessions(clientRecordIds)
                    HealthConnectAuthoredRecordType.Nutrition -> client.deleteNutritionRecords(clientRecordIds)
                    HealthConnectAuthoredRecordType.Hydration -> client.deleteHydrationRecords(clientRecordIds)
                }
                deletedRecords += typedRecords
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                failures += HealthConnectDeleteFailure(type, failure.healthConnectMessage())
            }
        }
        return when {
            failures.isEmpty() -> HealthConnectDeleteResult.Complete(deletedRecords)
            deletedRecords.isNotEmpty() -> HealthConnectDeleteResult.Partial(deletedRecords, failures)
            else -> HealthConnectDeleteResult.Failure(failures.joinToString("; ") { it.message })
        }
    }

    private suspend fun insertFoodRecords(
        nutritionExports: List<HealthConnectFoodMealExport>,
        nutritionRecords: List<NutritionRecord>,
        hydrationRecord: HydrationRecord?,
    ): HealthConnectFoodExportResult {
        val client = clientFactory()
        val nutritionProviderIds = if (nutritionRecords.isEmpty()) {
            emptyList()
        } else {
            client.insertNutritionRecords(nutritionRecords)
        }
        val hydrationProviderId = hydrationRecord?.let { record -> client.insertHydrationRecord(record) }
        return HealthConnectFoodExportResult(
            nutritionRecordCount = nutritionProviderIds.size,
            hydrationRecordCount = if (hydrationProviderId != null) 1 else 0,
            nutritionProviderRecordIds = nutritionExports
                .map { meal -> meal.localMealId }
                .zip(nutritionProviderIds)
                .toMap(),
            hydrationProviderRecordId = hydrationProviderId,
        )
    }

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        // On-device steps: "android" historically, a per-device Synthetic Package Name from the
        // June 2026 Health Connect update onwards (e.g. "com.android.healthconnect.phone.<hash>").
        const val ON_DEVICE_STEPS_PACKAGE = "android"
        const val ON_DEVICE_SPN_PREFIX = "com.android.healthconnect.phone."
        val READ_STEPS_PERMISSION = HealthPermissionInventory.readStepsPermission
        val READ_WEIGHT_PERMISSION = HealthPermissionInventory.readWeightPermission
        val READ_BODY_FAT_PERMISSION = HealthPermissionInventory.readBodyFatPermission
        val READ_ACTIVE_CALORIES_PERMISSION = HealthPermissionInventory.readActiveCaloriesPermission
        val READ_TOTAL_CALORIES_PERMISSION = HealthPermissionInventory.readTotalCaloriesPermission
        val READ_DISTANCE_PERMISSION = HealthPermissionInventory.readDistancePermission
        val READ_SLEEP_PERMISSION = HealthPermissionInventory.readSleepPermission
        val READ_EXERCISE_PERMISSION = HealthPermissionInventory.readExercisePermission
        val READ_RESTING_HEART_RATE_PERMISSION = HealthPermissionInventory.readRestingHeartRatePermission
        val READ_HEART_RATE_VARIABILITY_PERMISSION =
            HealthPermissionInventory.readHeartRateVariabilityPermission
        val WRITE_EXERCISE_PERMISSION = HealthPermissionInventory.writeExercisePermission
        val WRITE_NUTRITION_PERMISSION = HealthPermissionInventory.writeNutritionPermission
        val WRITE_HYDRATION_PERMISSION = HealthPermissionInventory.writeHydrationPermission
        val EMPTY_SUMMARY = ImportedDailyHealthSummary(
            steps = null,
            activeCaloriesKcal = null,
            totalCaloriesKcal = null,
            distanceMeters = null,
            sleepMinutes = null,
            exerciseMinutes = null,
            exerciseSessionCount = null,
            latestWeightKg = null,
            latestBodyFatPercent = null,
            restingHeartRateBpm = null,
            hrvRmssdMillis = null,
            bodyMetrics = emptyList(),
        )

        suspend fun readStatus(context: Context): HealthConnectStatus {
            val availability = when (
                HealthConnectClient.getSdkStatus(
                    context = context,
                    providerPackageName = HEALTH_CONNECT_PROVIDER_PACKAGE,
                )
            ) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available

                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    HealthConnectAvailability.NotInstalled

                else -> HealthConnectAvailability.NotSupported
            }

            val grantedPermissions = if (availability == HealthConnectAvailability.Available) {
                HealthConnectClient
                    .getOrCreate(context)
                    .permissionController
                    .getGrantedPermissions()
            } else {
                emptySet()
            }

            return HealthConnectStatus(
                availability = availability,
                grantedPermissions = grantedPermissions,
            )
        }
    }
}

private fun HealthConnectAuthoredRecordType.requiredWritePermission(): String = when (this) {
    HealthConnectAuthoredRecordType.Workout -> HealthPermissionInventory.writeExercisePermission
    HealthConnectAuthoredRecordType.Nutrition -> HealthPermissionInventory.writeNutritionPermission
    HealthConnectAuthoredRecordType.Hydration -> HealthPermissionInventory.writeHydrationPermission
}

@Suppress("TooGenericExceptionCaught")
private suspend fun <T> readMetricByDay(
    metric: HealthConnectMetric,
    range: HealthConnectDateRange,
    completedMetrics: Map<LocalDate, MutableSet<HealthConnectMetric>>,
    failures: Map<LocalDate, MutableList<HealthConnectMetricFailure>>,
    read: suspend () -> Map<LocalDate, T?>,
): Map<LocalDate, T?> = try {
    read().also {
        range.dates.forEach { date -> completedMetrics.getValue(date) += metric }
    }
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Exception) {
    range.dates.forEach { date ->
        failures.getValue(date) += HealthConnectMetricFailure(metric, failure.healthConnectMessage())
    }
    emptyMap()
}

private fun dailyReadResult(
    summary: ImportedDailyHealthSummary,
    completedMetrics: Set<HealthConnectMetric>,
    failures: List<HealthConnectMetricFailure>,
    status: HealthConnectStatus,
): HealthConnectDailyReadResult = when {
    failures.isNotEmpty() && completedMetrics.isEmpty() -> HealthConnectDailyReadResult.Failure(
        message = failures.joinToString("; ") { failure -> failure.message },
        status = status,
    )

    failures.isNotEmpty() -> HealthConnectDailyReadResult.Partial(
        summary = summary,
        completedMetrics = completedMetrics,
        failures = failures,
        status = status,
    )

    summary.isEmpty() -> HealthConnectDailyReadResult.Empty(completedMetrics, status)

    else -> HealthConnectDailyReadResult.Complete(summary, completedMetrics, status)
}

@Suppress("TooGenericExceptionCaught")
private suspend fun <T> healthConnectCatchingOrNull(block: suspend () -> T): T? = try {
    block()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Exception) {
    null
}

private fun Throwable.healthConnectMessage(): String = message
    ?.takeIf(String::isNotBlank)
    ?: this::class.simpleName
    ?: "Health Connect read failed."

private fun ImportedDailyHealthSummary.isEmpty(): Boolean = steps == null &&
    activeCaloriesKcal == null &&
    totalCaloriesKcal == null &&
    distanceMeters == null &&
    sleepMinutes == null &&
    exerciseMinutes == null &&
    exerciseSessionCount == null &&
    latestWeightKg == null &&
    latestBodyFatPercent == null &&
    restingHeartRateBpm == null &&
    hrvRmssdMillis == null &&
    bodyMetrics.isEmpty()

private fun HealthConnectFoodExportPayload.writeableNutritionExports(
    canWriteNutrition: Boolean,
): List<HealthConnectFoodMealExport> = if (canWriteNutrition) meals.filter { it.hasMacroNutrition() } else emptyList()

private fun HealthConnectFoodExportPayload.toNutritionRecords(
    exports: List<HealthConnectFoodMealExport>,
): List<NutritionRecord> = exports.map { meal ->
    HealthConnectRecordMapper.toNutritionRecord(
        date = date,
        meal = meal,
        identity = HealthConnectRecordIdentity.forNutrition(
            accountId = accountId,
            mealId = meal.localMealId,
            version = meal.clientRecordVersion,
        ),
    )
}

private fun HealthConnectFoodExportPayload.toHydrationRecord(canWriteHydration: Boolean): HydrationRecord? {
    if (!canWriteHydration || hydrationMilliliters <= 0.0) return null
    return HealthConnectRecordMapper.toHydrationRecord(
        date = date,
        milliliters = hydrationMilliliters,
        accountId = accountId,
        identity = HealthConnectRecordIdentity.forHydration(
            accountId = accountId,
            date = date,
            version = hydrationClientRecordVersion,
        ),
    )
}

private fun HealthConnectFoodMealExport.hasMacroNutrition(): Boolean = caloriesKcal > 0.0 || proteinGrams > 0.0 || carbsGrams > 0.0 || fatGrams > 0.0

internal interface HealthConnectClientAdapter {
    suspend fun aggregateSteps(range: HealthConnectTimeRange): Long?

    suspend fun aggregateStepsForOrigins(
        range: HealthConnectTimeRange,
        packageNames: Set<String>,
    ): Long?

    suspend fun aggregateStepsByDay(range: HealthConnectDateRange): Map<LocalDate, Long?> = range.readEachDay(::aggregateSteps)

    suspend fun aggregateStepsForOriginsByDay(
        range: HealthConnectDateRange,
        packageNames: Set<String>,
    ): Map<LocalDate, Long?> = range.readEachDay { day -> aggregateStepsForOrigins(day, packageNames) }

    suspend fun readStepCountsByOrigin(range: HealthConnectTimeRange): Map<String, Long>

    suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double?

    suspend fun aggregateActiveCaloriesByDay(range: HealthConnectDateRange): Map<LocalDate, Double?> = range.readEachDay(::aggregateActiveCalories)

    suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double?

    suspend fun aggregateTotalCaloriesByDay(range: HealthConnectDateRange): Map<LocalDate, Double?> = range.readEachDay(::aggregateTotalCalories)

    suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double?

    suspend fun aggregateDistanceMetersByDay(range: HealthConnectDateRange): Map<LocalDate, Double?> = range.readEachDay(::aggregateDistanceMeters)

    suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long?

    suspend fun aggregateSleepMinutesByDay(range: HealthConnectDateRange): Map<LocalDate, Long?> = range.readEachDay(::aggregateSleepMinutes)

    suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long?

    suspend fun aggregateExerciseMinutesByDay(range: HealthConnectDateRange): Map<LocalDate, Long?> = range.readEachDay(::aggregateExerciseMinutes)

    suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int?

    suspend fun readExerciseSessionCountByDay(range: HealthConnectDateRange): Map<LocalDate, Int?> = range.readEachDay(::readExerciseSessionCount)

    suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric?

    suspend fun readLatestWeightMetricByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, ImportedBodyMetric?> = range.readEachDay(::readLatestWeightMetric)

    suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric?

    suspend fun readLatestBodyFatMetricByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, ImportedBodyMetric?> = range.readEachDay(::readLatestBodyFatMetric)

    suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long?

    suspend fun readLatestRestingHeartRateByDay(range: HealthConnectDateRange): Map<LocalDate, Long?> = range.readEachDay(::readLatestRestingHeartRate)

    suspend fun readLatestHeartRateVariabilityRmssdMillis(range: HealthConnectTimeRange): Double?

    suspend fun readLatestHeartRateVariabilityRmssdMillisByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Double?> = range.readEachDay(::readLatestHeartRateVariabilityRmssdMillis)

    suspend fun insertExerciseSession(record: ExerciseSessionRecord): String?

    suspend fun insertNutritionRecords(records: List<NutritionRecord>): List<String>

    suspend fun insertHydrationRecord(record: HydrationRecord): String?

    suspend fun deleteExerciseSessions(clientRecordIds: List<String>)

    suspend fun deleteNutritionRecords(clientRecordIds: List<String>)

    suspend fun deleteHydrationRecords(clientRecordIds: List<String>)
}

internal class DefaultHealthConnectClientAdapter(
    private val client: HealthConnectClient,
) : HealthConnectClientAdapter {
    override suspend fun aggregateSteps(range: HealthConnectTimeRange): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[StepsRecord.COUNT_TOTAL]

    override suspend fun aggregateStepsForOrigins(
        range: HealthConnectTimeRange,
        packageNames: Set<String>,
    ): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
            dataOriginFilter = packageNames.map { DataOrigin(it) }.toSet(),
        ),
    )[StepsRecord.COUNT_TOTAL]

    override suspend fun aggregateStepsByDay(range: HealthConnectDateRange): Map<LocalDate, Long?> = aggregateByDay(range, StepsRecord.COUNT_TOTAL) { result -> result[StepsRecord.COUNT_TOTAL] }

    override suspend fun aggregateStepsForOriginsByDay(
        range: HealthConnectDateRange,
        packageNames: Set<String>,
    ): Map<LocalDate, Long?> = aggregateByDay(
        range = range,
        metric = StepsRecord.COUNT_TOTAL,
        dataOriginFilter = packageNames.mapTo(linkedSetOf(), ::DataOrigin),
    ) { result -> result[StepsRecord.COUNT_TOTAL] }

    override suspend fun readStepCountsByOrigin(range: HealthConnectTimeRange): Map<String, Long> = readAllRecords(
        StepsRecord::class,
        range.asTimeRangeFilter(),
    )
        .groupBy { it.metadata.dataOrigin.packageName }
        .mapValues { (_, records) -> records.sumOf { it.count } }

    override suspend fun aggregateActiveCalories(range: HealthConnectTimeRange): Double? = client.aggregate(
        AggregateRequest(
            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories

    override suspend fun aggregateActiveCaloriesByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Double?> = aggregateByDay(range, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL) { result ->
        result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
    }

    override suspend fun aggregateTotalCalories(range: HealthConnectTimeRange): Double? = client.aggregate(
        AggregateRequest(
            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories

    override suspend fun aggregateTotalCaloriesByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Double?> = aggregateByDay(range, TotalCaloriesBurnedRecord.ENERGY_TOTAL) { result ->
        result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
    }

    override suspend fun aggregateDistanceMeters(range: HealthConnectTimeRange): Double? = client.aggregate(
        AggregateRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[DistanceRecord.DISTANCE_TOTAL]?.inMeters

    override suspend fun aggregateDistanceMetersByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Double?> = aggregateByDay(range, DistanceRecord.DISTANCE_TOTAL) { result ->
        result[DistanceRecord.DISTANCE_TOTAL]?.inMeters
    }

    override suspend fun aggregateSleepMinutes(range: HealthConnectTimeRange): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()

    override suspend fun aggregateSleepMinutesByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Long?> = aggregateByDay(range, SleepSessionRecord.SLEEP_DURATION_TOTAL) { result ->
        result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()
    }

    override suspend fun aggregateExerciseMinutes(range: HealthConnectTimeRange): Long? = client.aggregate(
        AggregateRequest(
            metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
            timeRangeFilter = range.asTimeRangeFilter(),
        ),
    )[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()

    override suspend fun aggregateExerciseMinutesByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Long?> = aggregateByDay(range, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL) { result ->
        result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()
    }

    override suspend fun readExerciseSessionCount(range: HealthConnectTimeRange): Int? = readAllRecords(
        ExerciseSessionRecord::class,
        range.asTimeRangeFilter(),
    ).size

    override suspend fun readExerciseSessionCountByDay(range: HealthConnectDateRange): Map<LocalDate, Int?> {
        val records = readAllRecords(ExerciseSessionRecord::class, range.asTimeRangeFilter())
        return range.dates.associateWith { date ->
            val day = range.dayRange(date)
            records.count { record -> record.startTime < day.endTime && record.endTime > day.startTime }
        }
    }

    override suspend fun readLatestWeightMetric(range: HealthConnectTimeRange): ImportedBodyMetric? = readAllRecords(
        WeightRecord::class,
        range.asTimeRangeFilter(),
    ).maxByOrNull { it.time }?.toImportedBodyMetric()

    override suspend fun readLatestWeightMetricByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, ImportedBodyMetric?> = readLatestByDay(
        range,
        readAllRecords(WeightRecord::class, range.asTimeRangeFilter()),
        WeightRecord::time,
        { record -> record.toImportedBodyMetric() },
    )

    override suspend fun readLatestBodyFatMetric(range: HealthConnectTimeRange): ImportedBodyMetric? = readAllRecords(
        BodyFatRecord::class,
        range.asTimeRangeFilter(),
    ).maxByOrNull { it.time }?.toImportedBodyMetric()

    override suspend fun readLatestBodyFatMetricByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, ImportedBodyMetric?> = readLatestByDay(
        range,
        readAllRecords(BodyFatRecord::class, range.asTimeRangeFilter()),
        BodyFatRecord::time,
        { record -> record.toImportedBodyMetric() },
    )

    override suspend fun readLatestRestingHeartRate(range: HealthConnectTimeRange): Long? = readAllRecords(
        RestingHeartRateRecord::class,
        range.asTimeRangeFilter(),
    ).maxByOrNull { it.time }?.beatsPerMinute

    override suspend fun readLatestRestingHeartRateByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Long?> = readLatestByDay(
        range,
        readAllRecords(RestingHeartRateRecord::class, range.asTimeRangeFilter()),
        RestingHeartRateRecord::time,
        RestingHeartRateRecord::beatsPerMinute,
    )

    override suspend fun readLatestHeartRateVariabilityRmssdMillis(range: HealthConnectTimeRange): Double? = readAllRecords(
        HeartRateVariabilityRmssdRecord::class,
        range.asTimeRangeFilter(),
    ).maxByOrNull { it.time }?.heartRateVariabilityMillis

    override suspend fun readLatestHeartRateVariabilityRmssdMillisByDay(
        range: HealthConnectDateRange,
    ): Map<LocalDate, Double?> = readLatestByDay(
        range,
        readAllRecords(HeartRateVariabilityRmssdRecord::class, range.asTimeRangeFilter()),
        HeartRateVariabilityRmssdRecord::time,
        HeartRateVariabilityRmssdRecord::heartRateVariabilityMillis,
    )

    private suspend fun <T> aggregateByDay(
        range: HealthConnectDateRange,
        metric: AggregateMetric<*>,
        dataOriginFilter: Set<DataOrigin> = emptySet(),
        value: (AggregationResult) -> T?,
    ): Map<LocalDate, T?> = client.aggregateGroupByPeriod(
        AggregateGroupByPeriodRequest(
            metrics = setOf(metric),
            timeRangeFilter = range.asLocalTimeRangeFilter(),
            timeRangeSlicer = Period.ofDays(1),
            dataOriginFilter = dataOriginFilter,
        ),
    ).associate { row -> row.startTime.toLocalDate() to value(row.result) }

    private suspend fun <T : Record> readAllRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
    ): List<T> = readAllHealthConnectPages { pageToken ->
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = timeRangeFilter,
                pageToken = pageToken,
            ),
        )
        HealthConnectRecordPage(response.records, response.pageToken)
    }

    private fun <T, R> readLatestByDay(
        range: HealthConnectDateRange,
        records: List<T>,
        time: (T) -> Instant,
        transform: (T) -> R,
    ): Map<LocalDate, R?> {
        val recordsByDate = records.groupBy { record -> time(record).atZone(range.zoneId).toLocalDate() }
        return range.dates.associateWith { date ->
            recordsByDate[date]?.maxByOrNull(time)?.let(transform)
        }
    }

    private fun WeightRecord.toImportedBodyMetric(): ImportedBodyMetric = ImportedBodyMetric(
        type = "weight",
        value = weight.inKilograms,
        unit = "kg",
        measuredAtEpochMillis = time.toEpochMilli(),
        externalId = metadata.id.takeIf { it.isNotBlank() },
    )

    private fun BodyFatRecord.toImportedBodyMetric(): ImportedBodyMetric = ImportedBodyMetric(
        type = "body_fat",
        value = percentage.value,
        unit = "%",
        measuredAtEpochMillis = time.toEpochMilli(),
        externalId = metadata.id.takeIf { it.isNotBlank() },
    )

    override suspend fun insertExerciseSession(record: ExerciseSessionRecord): String? = client.insertRecords(listOf(record)).recordIdsList.firstOrNull()

    override suspend fun insertNutritionRecords(records: List<NutritionRecord>): List<String> = client.insertRecords(records).recordIdsList

    override suspend fun insertHydrationRecord(record: HydrationRecord): String? = client.insertRecords(listOf(record)).recordIdsList.firstOrNull()

    override suspend fun deleteExerciseSessions(clientRecordIds: List<String>) = client.deleteRecords(
        ExerciseSessionRecord::class,
        recordIdsList = emptyList(),
        clientRecordIdsList = clientRecordIds,
    )

    override suspend fun deleteNutritionRecords(clientRecordIds: List<String>) = client.deleteRecords(
        NutritionRecord::class,
        recordIdsList = emptyList(),
        clientRecordIdsList = clientRecordIds,
    )

    override suspend fun deleteHydrationRecords(clientRecordIds: List<String>) = client.deleteRecords(
        HydrationRecord::class,
        recordIdsList = emptyList(),
        clientRecordIdsList = clientRecordIds,
    )
}
