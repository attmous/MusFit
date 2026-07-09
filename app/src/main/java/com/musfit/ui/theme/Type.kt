package com.musfit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.musfit.R

/** Google Sans Flex (bundled static weights) — the display/heading face. */
private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold),
)

private fun TextStyle.gsf(weight: FontWeight) = copy(fontFamily = GoogleSansFlex, fontWeight = weight)

/**
 * Health-grade clean type scale: quiet, regular-weight headlines that read like
 * sentences, thin (300) display numerals for hero metrics, and medium — never
 * bold — section/row titles. display* stays on the system Roboto because Google
 * Sans Flex ships no Light cut; Roboto Light is exactly the mock numeral face.
 */
val MusFitTypography: Typography = Typography().let { base ->
    base.copy(
        // Thin hero numerals (34–44sp band).
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Light, fontSize = 44.sp),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Light),
        // Screen titles: 28sp regular — "Good morning", not a shouted label.
        headlineLarge = base.headlineLarge.gsf(FontWeight.Normal),
        headlineMedium = base.headlineMedium.gsf(FontWeight.Normal),
        headlineSmall = base.headlineSmall.gsf(FontWeight.Normal),
        // Stats 22/400; section headers 16/500 — small and quiet; rows 15/500.
        titleLarge = base.titleLarge.gsf(FontWeight.Normal),
        titleMedium = base.titleMedium.gsf(FontWeight.Medium),
        titleSmall = base.titleSmall.gsf(FontWeight.Medium).copy(fontSize = 15.sp),
    )
}
