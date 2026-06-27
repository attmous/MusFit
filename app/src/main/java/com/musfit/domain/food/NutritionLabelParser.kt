package com.musfit.domain.food

/** Best-effort macros read from a nutrition label, all per the label's stated basis (usually 100 g). */
data class ParsedNutritionLabel(
    val caloriesKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val fiberGrams: Double? = null,
    val sugarGrams: Double? = null,
    val saturatedFatGrams: Double? = null,
    val sodiumMilligrams: Double? = null,
) {
    val hasAnyValue: Boolean
        get() = parsedFieldCount > 0

    val parsedFieldCount: Int
        get() =
            listOfNotNull(
                caloriesKcal,
                proteinGrams,
                carbsGrams,
                fatGrams,
                fiberGrams,
                sugarGrams,
                saturatedFatGrams,
                sodiumMilligrams,
            ).size

    val confidenceLabel: String
        get() =
            when {
                parsedFieldCount >= 7 -> "Strong parse"
                parsedFieldCount >= 4 -> "Partial parse"
                parsedFieldCount > 0 -> "Low parse"
                else -> "No parse"
            }
}

/**
 * Tolerant parser that maps OCR'd nutrition-label text into macro values. Handles English and German
 * keywords and comma decimals. Always best-effort: callers must present the result for manual review.
 */
object NutritionLabelParser {
    private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")
    private val KCAL = Regex("""(\d+(?:[.,]\d+)?)\s*kcal""")
    private val SATURATED_HINTS = listOf("satur", "gesätt", "gesatt", "davon", "of which")

    fun parse(rawText: String): ParsedNutritionLabel {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        return ParsedNutritionLabel(
            caloriesKcal = calories(lines),
            proteinGrams = firstNumberOnLineMatching(lines, listOf("protein", "eiwei")),
            carbsGrams = firstNumberOnLineMatching(lines, listOf("carbohydrate", "kohlenhydrate", "carbs")),
            fatGrams = totalFat(lines),
            fiberGrams = firstNumberOnLineMatching(lines, listOf("fibre", "fiber", "ballast")),
            sugarGrams = firstNumberOnLineMatching(lines, listOf("sugar", "zucker")),
            saturatedFatGrams = firstNumberOnLineMatching(lines, listOf("satur", "gesatt", "gesÃ¤tt")),
            sodiumMilligrams = sodium(lines),
        )
    }

    private fun calories(lines: List<String>): Double? {
        lines.forEach { line ->
            KCAL.find(line.lowercase())?.let { return it.groupValues[1].toNum() }
        }
        return null
    }

    /** Prefer the total-fat line over the "of which saturates / davon gesättigte" sub-line. */
    private fun totalFat(lines: List<String>): Double? {
        val matches = lines.filter { val l = it.lowercase(); "fat" in l || "fett" in l }
        val total = matches.firstOrNull { line -> SATURATED_HINTS.none { it in line.lowercase() } }
        return (total ?: matches.firstOrNull())?.let { firstNumber(it) }
    }

    private fun firstNumberOnLineMatching(lines: List<String>, keywords: List<String>): Double? {
        val line = lines.firstOrNull { val l = it.lowercase(); keywords.any { k -> k in l } } ?: return null
        return firstNumber(line)
    }

    private fun firstNumber(line: String): Double? = NUMBER.find(line)?.value?.toNum()

    private fun sodium(lines: List<String>): Double? {
        val line = lines.firstOrNull { "sodium" in it.lowercase() } ?: return null
        val value = firstNumber(line) ?: return null
        return if ("mg" in line.lowercase()) value else value * 1000.0
    }

    private fun String.toNum(): Double? = replace(',', '.').toDoubleOrNull()
}
