package com.musfit.ui

import com.musfit.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {

    @Test
    fun entries_match_expected_bottom_tab_contract() {
        assertEquals(
            listOf("today", "food", "training", "profile"),
            AppDestination.entries.map { it.route },
        )
        assertEquals(
            listOf(
                R.string.app_destination_today,
                R.string.app_destination_food,
                R.string.app_destination_training,
                R.string.app_destination_profile,
            ),
            AppDestination.entries.map { it.labelRes },
        )
    }

    @Test
    fun navigationKeys_map_to_their_bottom_navigation_owner() {
        assertEquals(AppDestination.Today, bottomDestinationForKey(TodayNavKey))
        assertEquals(AppDestination.Food, bottomDestinationForKey(FoodNavKey))
        assertEquals(AppDestination.Training, bottomDestinationForKey(TrainingNavKey))
        assertEquals(AppDestination.Profile, bottomDestinationForKey(ProfileNavKey))
    }

    @Test
    fun profileAndProgressKeys_keep_profile_tab_selected() {
        assertEquals(AppDestination.Profile, bottomDestinationForKey(ProfileSettingsNavKey))
        assertEquals(AppDestination.Profile, bottomDestinationForKey(TrainingProgressNavKey))
        assertEquals(AppDestination.Profile, bottomDestinationForKey(NutritionTrendsNavKey))
    }
}
