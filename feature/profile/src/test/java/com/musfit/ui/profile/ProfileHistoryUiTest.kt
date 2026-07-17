package com.musfit.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProfileHistoryUiTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 7, 11)

    @Test
    fun rangeStart_includesExactlyRequestedCalendarDays() {
        assertEquals(dayStart(today.minusDays(6)), historyRangeStartEpochMillis(HistoryRange.Week, today, zone))
        assertEquals(dayStart(today.minusDays(29)), historyRangeStartEpochMillis(HistoryRange.Month, today, zone))
        assertEquals(dayStart(today.minusDays(89)), historyRangeStartEpochMillis(HistoryRange.Quarter, today, zone))
        assertNull(historyRangeStartEpochMillis(HistoryRange.All, today, zone))
    }

    @Test
    fun entriesInRange_keepsBoundaryAndRejectsPriorDay() {
        val boundary = dayStart(today.minusDays(6))
        val entries = listOf(
            HistoryEntry("today", dayStart(today), 80.0, "kg"),
            HistoryEntry("boundary", boundary, 81.0, "kg"),
            HistoryEntry("outside", boundary - 1L, 82.0, "kg"),
        )

        assertEquals(
            listOf("today", "boundary"),
            historyEntriesInRange(entries, HistoryRange.Week, today, zone).map(HistoryEntry::id),
        )
        assertEquals(entries, historyEntriesInRange(entries, HistoryRange.All, today, zone))
    }

    private fun dayStart(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
}
