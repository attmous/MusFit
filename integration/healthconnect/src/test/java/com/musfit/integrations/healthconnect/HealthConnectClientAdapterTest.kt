package com.musfit.integrations.healthconnect

import android.annotation.SuppressLint
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.percent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@SuppressLint("RestrictedApi")
@Config(sdk = [35])
class HealthConnectClientAdapterTest {
    @Test
    fun groupedAggregates_returnEveryLocalDay_withOneCallPerMetric() = runTest {
        var aggregateCalls = 0
        val client = proxyClient { methodName, _ ->
            when (methodName) {
                "aggregateGroupByPeriod" -> {
                    aggregateCalls += 1
                    listOf(
                        aggregateRow(LocalDate.of(2026, 7, 6), multiplier = 1),
                        aggregateRow(LocalDate.of(2026, 7, 7), multiplier = 2),
                    )
                }

                else -> error("Unexpected Health Connect call: $methodName")
            }
        }
        val adapter = DefaultHealthConnectClientAdapter(client)
        val range = HealthConnectDateRange(
            LocalDate.of(2026, 7, 6),
            LocalDate.of(2026, 7, 7),
            ZoneId.of("Europe/Berlin"),
        )

        assertEquals(listOf(1_000L, 2_000L), adapter.aggregateStepsByDay(range).values.toList())
        assertEquals(
            listOf(1_000L, 2_000L),
            adapter.aggregateStepsForOriginsByDay(range, setOf("com.example.steps")).values.toList(),
        )
        assertEquals(listOf(100.0, 200.0), adapter.aggregateActiveCaloriesByDay(range).values.toList())
        assertEquals(listOf(2_000.0, 4_000.0), adapter.aggregateTotalCaloriesByDay(range).values.toList())
        assertEquals(listOf(5_000.0, 10_000.0), adapter.aggregateDistanceMetersByDay(range).values.toList())
        assertEquals(listOf(60L, 120L), adapter.aggregateSleepMinutesByDay(range).values.toList())
        assertEquals(listOf(30L, 60L), adapter.aggregateExerciseMinutesByDay(range).values.toList())
        assertEquals(7, aggregateCalls)
    }

    @Test
    fun rawRecordReaders_pageAndBucketEverySupportedRecordType() = runTest {
        val range = HealthConnectDateRange(
            LocalDate.of(2026, 7, 6),
            LocalDate.of(2026, 7, 7),
            ZoneId.of("UTC"),
        )
        val firstDay = Instant.parse("2026-07-06T08:00:00Z")
        val firstDayLater = Instant.parse("2026-07-06T18:00:00Z")
        val secondDay = Instant.parse("2026-07-07T08:00:00Z")
        val requestedWeightTokens = mutableListOf<String?>()
        val client = proxyClient { methodName, arguments ->
            when (methodName) {
                "readRecords" -> {
                    val request = arguments.first() as ReadRecordsRequest<*>
                    when (request.recordType) {
                        WeightRecord::class -> {
                            requestedWeightTokens += request.pageToken
                            if (request.pageToken == null) {
                                readPage(
                                    listOf(weight(firstDay, 80.0, "weight-1")),
                                    nextPageToken = "weight-page-2",
                                )
                            } else {
                                readPage(
                                    listOf(
                                        weight(firstDayLater, 81.0, "weight-2"),
                                        weight(secondDay, 82.0, "weight-3"),
                                    ),
                                )
                            }
                        }

                        BodyFatRecord::class -> readPage(listOf(bodyFat(secondDay, 18.5)))

                        RestingHeartRateRecord::class -> readPage(listOf(restingHeartRate(secondDay, 54L)))

                        HeartRateVariabilityRmssdRecord::class -> readPage(listOf(hrv(secondDay, 63.0)))

                        ExerciseSessionRecord::class -> readPage(listOf(crossMidnightExercise()))

                        else -> error("Unexpected record type: ${request.recordType}")
                    }
                }

                else -> error("Unexpected Health Connect call: $methodName")
            }
        }
        val adapter = DefaultHealthConnectClientAdapter(client)

        val weights = adapter.readLatestWeightMetricByDay(range)
        val bodyFat = adapter.readLatestBodyFatMetricByDay(range)
        val restingHeartRate = adapter.readLatestRestingHeartRateByDay(range)
        val hrv = adapter.readLatestHeartRateVariabilityRmssdMillisByDay(range)
        val exerciseSessions = adapter.readExerciseSessionCountByDay(range)

        assertEquals(listOf(null, "weight-page-2"), requestedWeightTokens)
        assertEquals(81.0, weights.getValue(LocalDate.of(2026, 7, 6))?.value ?: 0.0, 0.01)
        assertEquals("weight-2", weights.getValue(LocalDate.of(2026, 7, 6))?.externalId)
        assertEquals(82.0, weights.getValue(LocalDate.of(2026, 7, 7))?.value ?: 0.0, 0.01)
        assertEquals(18.5, bodyFat.getValue(LocalDate.of(2026, 7, 7))?.value ?: 0.0, 0.01)
        assertEquals(54L, restingHeartRate[LocalDate.of(2026, 7, 7)])
        assertEquals(63.0, hrv[LocalDate.of(2026, 7, 7)] ?: 0.0, 0.01)
        assertEquals(mapOf(LocalDate.of(2026, 7, 6) to 1, LocalDate.of(2026, 7, 7) to 1), exerciseSessions)
    }

    private fun aggregateRow(date: LocalDate, multiplier: Int): AggregationResultGroupedByPeriod = AggregationResultGroupedByPeriod(
        result = AggregationResult(
            longValues = mapOf(
                StepsRecord.COUNT_TOTAL.metricKey to 1_000L * multiplier,
                SleepSessionRecord.SLEEP_DURATION_TOTAL.metricKey to 60L * 60_000L * multiplier,
                ExerciseSessionRecord.EXERCISE_DURATION_TOTAL.metricKey to 30L * 60_000L * multiplier,
            ),
            doubleValues = mapOf(
                ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL.metricKey to 100.0 * multiplier,
                TotalCaloriesBurnedRecord.ENERGY_TOTAL.metricKey to 2_000.0 * multiplier,
                DistanceRecord.DISTANCE_TOTAL.metricKey to 5_000.0 * multiplier,
            ),
            dataOrigins = emptySet(),
        ),
        startTime = date.atStartOfDay(),
        endTime = date.plusDays(1).atStartOfDay(),
    )

    private fun weight(time: Instant, kilograms: Double, id: String) = WeightRecord(
        time = time,
        zoneOffset = null,
        weight = kilograms.kilograms,
        metadata = Metadata.manualEntryWithId(id),
    )

    private fun bodyFat(time: Instant, percentage: Double) = BodyFatRecord(
        time = time,
        zoneOffset = null,
        percentage = percentage.percent,
        metadata = Metadata.manualEntry(),
    )

    private fun restingHeartRate(time: Instant, beatsPerMinute: Long) = RestingHeartRateRecord(
        time = time,
        zoneOffset = null,
        beatsPerMinute = beatsPerMinute,
        metadata = Metadata.manualEntry(),
    )

    private fun hrv(time: Instant, millis: Double) = HeartRateVariabilityRmssdRecord(
        time = time,
        zoneOffset = null,
        heartRateVariabilityMillis = millis,
        metadata = Metadata.manualEntry(),
    )

    private fun crossMidnightExercise() = ExerciseSessionRecord(
        startTime = Instant.parse("2026-07-06T23:30:00Z"),
        startZoneOffset = null,
        endTime = Instant.parse("2026-07-07T00:30:00Z"),
        endZoneOffset = null,
        metadata = Metadata.manualEntry(),
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
    )

    private fun proxyClient(
        response: (methodName: String, arguments: Array<out Any?>) -> Any?,
    ): HealthConnectClient = Proxy.newProxyInstance(
        HealthConnectClient::class.java.classLoader,
        arrayOf(HealthConnectClient::class.java),
    ) { _, method, arguments -> response(method.name, arguments.orEmpty()) } as HealthConnectClient

    private fun readPage(
        records: List<Record>,
        nextPageToken: String? = null,
    ): ReadRecordsResponse<Record> = ReadRecordsResponse(records, nextPageToken)
}
