package com.musfit.domain.health

/**
 * A single Health Connect data origin (app or device) that wrote step records for a day,
 * paired with that origin's own step total for the day.
 *
 * Health Connect's `aggregate(StepsRecord.COUNT_TOTAL)` returns a cross-source unified total
 * (deduplicated by the user's app-priority settings), which is by design >= any single source's
 * own view. Surfacing the per-source breakdown lets the user pin MusFit to the one source they
 * trust (e.g. "Health from Google") so the displayed number matches that app.
 */
data class StepSource(
    val packageName: String,
    val label: String,
    val steps: Long,
)
