package com.musfit.ui

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun topLevelSelection_preservesVisitOrder_withoutDuplicateEntryOwners() {
        val backStack = mutableListOf<NavKey>(TodayNavKey)
        val navigator = AppNavigator(backStack)

        navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Food))
        navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Training))
        navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Profile))
        navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Food))

        assertEquals(listOf(TodayNavKey, TrainingNavKey, ProfileNavKey, FoodNavKey), backStack)
        assertEquals(AppDestination.Food, navigator.currentDestination)
        assertTrue(navigator.goBack())
        assertEquals(AppDestination.Profile, navigator.currentDestination)
        assertTrue(navigator.goBack())
        assertEquals(AppDestination.Training, navigator.currentDestination)
    }

    @Test
    fun selectingTopLevel_closesCurrentSubroute_beforeRecordingVisit() {
        val backStack = mutableListOf<NavKey>(TodayNavKey, ProfileNavKey, ProfileSettingsNavKey)
        val navigator = AppNavigator(backStack)

        navigator.navigate(AppNavigationAction.SelectTopLevel(AppDestination.Food))

        assertEquals(listOf(TodayNavKey, ProfileNavKey, FoodNavKey), backStack)
    }

    @Test
    fun backAtStart_isNotHandled() {
        val navigator = AppNavigator(mutableListOf<NavKey>(TodayNavKey))

        assertFalse(navigator.goBack())
    }
}
