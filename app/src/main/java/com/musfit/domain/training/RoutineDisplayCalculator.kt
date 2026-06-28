package com.musfit.domain.training

import java.util.Locale

/**
 * Pure display helpers for routine summary rows: a rough duration estimate and a deduped,
 * title-cased muscle-group list parsed from concatenated primary-muscle strings.
 */
object RoutineDisplayCalculator {
    private const val MINUTES_PER_SET = 3

    /**
     * Rough wall-clock estimate for a routine based on its planned set count. Assumes ~3 minutes
     * per working set (work + rest), rounded to the nearest 5 minutes. Returns 0 for empty routines.
     */
    fun estimatedMinutes(targetSetCount: Int): Int {
        if (targetSetCount <= 0) return 0
        val raw = targetSetCount * MINUTES_PER_SET
        return ((raw + 2) / 5) * 5
    }

    /**
     * Parses a concatenation of primary-muscle strings (e.g. a GROUP_CONCAT across a routine's
     * exercises, each value itself possibly comma-separated) into a deduped, title-cased, ordered
     * list capped at [limit]. Comparison is case-insensitive; first-seen ordering is preserved.
     */
    fun topMuscles(concatenated: String, limit: Int = 3): List<String> {
        if (limit <= 0) return emptyList()
        val result = mutableListOf<String>()
        val seenKeys = mutableSetOf<String>()
        for (raw in concatenated.split(",")) {
            val muscle = raw.trim()
            if (muscle.isEmpty()) continue
            val normalized = muscle.lowercase(Locale.US)
            if (seenKeys.add(normalized)) {
                result.add(normalized.replaceFirstChar { it.titlecase(Locale.US) })
                if (result.size == limit) break
            }
        }
        return result
    }
}
