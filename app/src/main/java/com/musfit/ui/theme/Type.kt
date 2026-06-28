package com.musfit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.musfit.R

/** Google Sans Flex (bundled static weights) — the M3E display/heading face. */
private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold),
)

private fun TextStyle.gsf(weight: FontWeight) = copy(fontFamily = GoogleSansFlex, fontWeight = weight)

/**
 * M3E type scale: display/headline/title use Google Sans Flex; body/label keep the
 * system Roboto (no bundling needed — Roboto is the Android system font).
 */
val MusFitTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.gsf(FontWeight.Bold),
        displayMedium = base.displayMedium.gsf(FontWeight.Bold),
        displaySmall = base.displaySmall.gsf(FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.gsf(FontWeight.Bold),
        headlineMedium = base.headlineMedium.gsf(FontWeight.Bold),
        headlineSmall = base.headlineSmall.gsf(FontWeight.SemiBold),
        titleLarge = base.titleLarge.gsf(FontWeight.SemiBold),
        titleMedium = base.titleMedium.gsf(FontWeight.SemiBold),
        titleSmall = base.titleSmall.gsf(FontWeight.SemiBold),
    )
}
