package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import com.musfit.ui.AppDestination

/** A miniapp's signature accent, layered on the shared neutral B1 tokens for per-tab color coding. */
data class TabAccent(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

fun tabAccentFor(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(Coral, CardWhite, CoralContainer, CoralInk)
    AppDestination.Food -> TabAccent(Emerald, CardWhite, PositiveContainer, EmeraldInk)
    AppDestination.Training -> TabAccent(Indigo, CardWhite, IndigoContainer, IndigoInk)
    AppDestination.Health -> TabAccent(Teal, CardWhite, TealContainer, TealInk)
}
