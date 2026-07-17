package com.musfit.domain.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionLabelParserTest {

    @Test
    fun parsesEnglishLabel() {
        val text = """
            Nutrition Facts
            Energy 250 kcal
            Fat 12 g
            Carbohydrate 30 g
            Protein 8 g
        """.trimIndent()

        val parsed = NutritionLabelParser.parse(text)

        assertEquals(250.0, parsed.caloriesKcal!!, 0.001)
        assertEquals(12.0, parsed.fatGrams!!, 0.001)
        assertEquals(30.0, parsed.carbsGrams!!, 0.001)
        assertEquals(8.0, parsed.proteinGrams!!, 0.001)
        assertTrue(parsed.hasAnyValue)
    }

    @Test
    fun parsesGermanLabelWithCommaDecimalsAndSaturatedFat() {
        val text = """
            Nährwerte pro 100 g
            Brennwert 1046 kJ / 250 kcal
            Fett 12,5 g
            davon gesättigte Fettsäuren 5,0 g
            Kohlenhydrate 30 g
            Eiweiß 8 g
        """.trimIndent()

        val parsed = NutritionLabelParser.parse(text)

        assertEquals(250.0, parsed.caloriesKcal!!, 0.001)
        assertEquals(12.5, parsed.fatGrams!!, 0.001)
        assertEquals(30.0, parsed.carbsGrams!!, 0.001)
        assertEquals(8.0, parsed.proteinGrams!!, 0.001)
        // Total fat stays the 12,5 g line; the "davon gesättigte" sub-line parses as saturated fat.
        assertEquals(5.0, parsed.saturatedFatGrams!!, 0.001)
    }

    @Test
    fun parsesAdvancedNutrientsAndConvertsSodiumGramsToMilligrams() {
        val grams =
            NutritionLabelParser.parse(
                """
                Fibre 4 g
                Sugar 9 g
                Sodium 0.3 g
                """.trimIndent(),
            )
        assertEquals(4.0, grams.fiberGrams!!, 0.001)
        assertEquals(9.0, grams.sugarGrams!!, 0.001)
        // A label that states sodium in grams is converted to milligrams.
        assertEquals(300.0, grams.sodiumMilligrams!!, 0.001)

        val milligrams = NutritionLabelParser.parse("Sodium 320 mg")
        assertEquals(320.0, milligrams.sodiumMilligrams!!, 0.001)
    }

    @Test
    fun confidenceLabelReflectsParsedFieldCount() {
        assertEquals("No parse", NutritionLabelParser.parse("Just some text").confidenceLabel)
        assertEquals("Low parse", NutritionLabelParser.parse("Protein 8 g").confidenceLabel)
        assertEquals(
            "Partial parse",
            NutritionLabelParser.parse("Energy 250 kcal\nFat 12 g\nCarbohydrate 30 g\nProtein 8 g").confidenceLabel,
        )
        val full =
            NutritionLabelParser.parse(
                """
                Energy 250 kcal
                Fat 12 g
                saturated 5 g
                Carbohydrate 30 g
                Sugar 9 g
                Protein 8 g
                Fibre 4 g
                Sodium 320 mg
                """.trimIndent(),
            )
        assertEquals(8, full.parsedFieldCount)
        assertEquals("Strong parse", full.confidenceLabel)
    }

    @Test
    fun picksKcalFromEnergyLineWithKilojoules() {
        val parsed = NutritionLabelParser.parse("Energy 1046 kJ / 250 kcal")
        assertEquals(250.0, parsed.caloriesKcal!!, 0.001)
    }

    @Test
    fun returnsNullsForTextWithoutNutrition() {
        val parsed = NutritionLabelParser.parse("Hello world\nThis is not a label")
        assertNull(parsed.caloriesKcal)
        assertNull(parsed.proteinGrams)
        assertNull(parsed.carbsGrams)
        assertNull(parsed.fatGrams)
        assertFalse(parsed.hasAnyValue)
    }
}
