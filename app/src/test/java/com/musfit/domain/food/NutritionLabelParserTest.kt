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
    fun parsesGermanLabelWithCommaDecimalsAndIgnoresSaturatedFat() {
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
