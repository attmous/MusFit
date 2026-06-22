package com.musfit.ui.training

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingHomeContentTest {
    @Test
    fun nextQuickLogExpanded_togglesPanelState() {
        assertEquals(true, nextQuickLogExpanded(current = false))
        assertEquals(false, nextQuickLogExpanded(current = true))
    }

    @Test
    fun routineCardActions_keepsStarterRoutineReadOnlyButStartable() {
        val actions = routineCardActions(isStarter = true)

        assertEquals(listOf("Start", "Duplicate"), actions)
    }

    @Test
    fun routineCardActions_includesEditAndDeleteForCustomRoutine() {
        val actions = routineCardActions(isStarter = false)

        assertEquals(listOf("Start", "Edit", "Duplicate", "Delete"), actions)
    }
}
