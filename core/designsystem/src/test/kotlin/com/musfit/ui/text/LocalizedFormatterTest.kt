package com.musfit.ui.text

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class LocalizedFormatterTest {
    @Test
    fun `German display numbers use current locale separators`() {
        assertEquals(
            "1.234,5",
            LocalizedFormatter.number(
                value = 1234.5,
                maximumFractionDigits = 1,
                locale = Locale.GERMANY,
            ),
        )
        assertEquals("1.234", LocalizedFormatter.integer(1234, locale = Locale.GERMANY))
    }

    @Test
    fun `German display dates use localized ordering`() {
        assertEquals(
            "20.07.2026",
            LocalizedFormatter.date(LocalDate.of(2026, 7, 20), locale = Locale.GERMANY),
        )
    }
}
