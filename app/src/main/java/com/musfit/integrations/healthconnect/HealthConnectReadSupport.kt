package com.musfit.integrations.healthconnect

import androidx.health.connect.client.time.TimeRangeFilter
import com.musfit.domain.health.HealthConnectDailyReadResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class HealthConnectDateRange(
    val startDate: LocalDate,
    val endDateInclusive: LocalDate,
    val zoneId: ZoneId,
) {
    init {
        require(!endDateInclusive.isBefore(startDate)) { "endDateInclusive must not precede startDate" }
    }

    val dates: List<LocalDate> = generateSequence(startDate) { date -> date.plusDays(1) }
        .takeWhile { date -> !date.isAfter(endDateInclusive) }
        .toList()
    val startTime: Instant = startDate.atStartOfDay(zoneId).toInstant()
    val endTime: Instant = endDateInclusive.plusDays(1).atStartOfDay(zoneId).toInstant()

    fun asTimeRangeFilter(): TimeRangeFilter = TimeRangeFilter.between(startTime, endTime)

    fun asLocalTimeRangeFilter(): TimeRangeFilter = TimeRangeFilter.between(
        startDate.atStartOfDay(),
        endDateInclusive.plusDays(1).atStartOfDay(),
    )

    fun dayRange(date: LocalDate): HealthConnectTimeRange {
        require(date in startDate..endDateInclusive) { "date must be inside this Health Connect range" }
        return date.asHealthConnectTimeRange(zoneId)
    }

    fun sameResult(result: HealthConnectDailyReadResult): Map<LocalDate, HealthConnectDailyReadResult> = dates.associateWith { result }
}

internal data class HealthConnectRecordPage<T>(
    val records: List<T>,
    val nextPageToken: String?,
)

internal suspend fun <T> readAllHealthConnectPages(
    readPage: suspend (pageToken: String?) -> HealthConnectRecordPage<T>,
): List<T> {
    val records = mutableListOf<T>()
    var pageToken: String? = null
    do {
        val page = readPage(pageToken)
        records += page.records
        pageToken = page.nextPageToken
    } while (pageToken != null)
    return records
}

internal data class HealthConnectTimeRange(
    val startTime: Instant,
    val endTime: Instant,
) {
    fun asTimeRangeFilter(): TimeRangeFilter = TimeRangeFilter.between(startTime, endTime)
}

internal suspend fun <T> HealthConnectDateRange.readEachDay(
    read: suspend (HealthConnectTimeRange) -> T,
): Map<LocalDate, T> = dates.associateWith { date -> read(dayRange(date)) }

internal fun LocalDate.asHealthConnectTimeRange(
    zoneId: ZoneId = ZoneId.systemDefault(),
): HealthConnectTimeRange {
    val dayStart = atStartOfDay(zoneId).toInstant()
    val dayEnd = plusDays(1).atStartOfDay(zoneId).toInstant()
    return HealthConnectTimeRange(startTime = dayStart, endTime = dayEnd)
}
