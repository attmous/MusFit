package com.musfit.ui.today

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayViewModelTest {
    @Test
    fun defaultState_isEmptyButReadable() {
        val viewModel = TodayViewModel()

        val state = viewModel.state.value

        assertEquals(0.0, state.caloriesKcal, 0.01)
        assertEquals("No workout logged today", state.trainingSummary)
    }
}
