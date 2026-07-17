package com.musfit.ui.theme

import androidx.compose.runtime.Composable
import com.musfit.ui.AppDestination

@Composable
fun tabAccentFor(destination: AppDestination): TabAccent = tabAccentFor(destination.toTabAccentRole())

private fun AppDestination.toTabAccentRole(): TabAccentRole = when (this) {
    AppDestination.Today -> TabAccentRole.Today
    AppDestination.Food -> TabAccentRole.Food
    AppDestination.Training -> TabAccentRole.Training
    AppDestination.Profile -> TabAccentRole.Profile
}
