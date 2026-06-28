package com.musfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.musfit.ui.AppDestination

/** A miniapp's signature accent, layered on the shared neutral B1 tokens for per-tab color coding. */
data class TabAccent(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

@Composable
fun tabAccentFor(destination: AppDestination): TabAccent =
    if (isSystemInDarkTheme()) tabAccentForDark(destination) else tabAccentForLight(destination)

internal fun tabAccentForLight(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(Coral, CardWhite, CoralContainer, CoralInk)
    AppDestination.Food -> TabAccent(Emerald, CardWhite, PositiveContainer, EmeraldInk)
    AppDestination.Training -> TabAccent(Indigo, CardWhite, IndigoContainer, IndigoInk)
    AppDestination.Profile -> TabAccent(Teal, CardWhite, TealContainer, TealInk)
}

internal fun tabAccentForDark(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(CoralBright, Color(0xFF3A1606), CoralContainerDark, CoralInkDark)
    AppDestination.Food -> TabAccent(EmeraldBright, EmeraldOnDark, EmeraldContainerDark, EmeraldInkDark)
    AppDestination.Training -> TabAccent(IndigoBright, Color(0xFF0B1240), IndigoContainerDark, IndigoInkDark)
    AppDestination.Profile -> TabAccent(TealBright, Color(0xFF06302F), TealContainerDark, TealInkDark)
}
