package com.musfit.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountUiTest {
    @Test
    fun accountInitials_ignoreBidiControlCharactersFromPseudoLocale() {
        assertEquals("Y", "\u202eYou\u202c".accountInitials())
    }
}
