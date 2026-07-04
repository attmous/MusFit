package com.musfit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStackTest {

    @Test
    fun startsOnToday() {
        val stack = AppNavigationStack()

        assertEquals(AppDestination.Today, stack.current)
        assertFalse(stack.canPop)
        assertEquals(listOf(AppDestination.Today), stack.entries)
    }

    @Test
    fun selectingTabs_pushesDistinctDestinations_andBackPopsInReverseOrder() {
        val stack = AppNavigationStack()

        stack.select(AppDestination.Food)
        stack.select(AppDestination.Training)
        stack.select(AppDestination.Profile)

        assertEquals(
            listOf(
                AppDestination.Today,
                AppDestination.Food,
                AppDestination.Training,
                AppDestination.Profile,
            ),
            stack.entries,
        )
        assertTrue(stack.canPop)
        assertEquals(AppDestination.Training, stack.pop())
        assertEquals(AppDestination.Food, stack.pop())
        assertEquals(AppDestination.Today, stack.pop())
        assertFalse(stack.canPop)
        assertNull(stack.pop())
    }

    @Test
    fun selectingCurrentDestination_doesNotAddDuplicateEntry() {
        val stack = AppNavigationStack()

        stack.select(AppDestination.Food)
        stack.select(AppDestination.Food)

        assertEquals(listOf(AppDestination.Today, AppDestination.Food), stack.entries)
    }

    @Test
    fun replace_updatesCurrentDestinationWithoutGrowingHistory() {
        val stack = AppNavigationStack()
        stack.select(AppDestination.Food)
        stack.select(AppDestination.Training)

        stack.replace(AppDestination.Food)

        assertEquals(listOf(AppDestination.Today, AppDestination.Food), stack.entries)
        assertEquals(AppDestination.Food, stack.current)
    }
}
