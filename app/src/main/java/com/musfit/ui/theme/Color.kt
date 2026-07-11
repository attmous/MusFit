package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color

// MusFit Material 3 Expressive palette — light.
// Neutrals: warm cream ground, pure white cards, warm inks.
val Cream = Color(0xFFF8F2E9)
val CardWhite = Color(0xFFFFFFFF)
val TonalFill = Color(0xFFF1E8DA) // tonal icon buttons, quiet chips and strips
val SegmentFill = Color(0xFFEDE4D5) // unselected segments and neutral progress tracks
val InkPrimary = Color(0xFF1E1B16)
val InkSecondary = Color(0xFF6F675C)
val InkFaint = Color(0xFF9A9184) // placeholders, timestamps
val Hairline = Color(0xFFEFE6D8)

// Today (coral). BrandCoral is reserved for the coach FAB; Coral is the
// text/progress-safe accent (WCAG AA on white).
val Coral = Color(0xFFC2470F)
val CoralContainer = Color(0xFFFFDBD1)
val CoralInk = Color(0xFF5C1A00)
val CoralInkStrong = Color(0xFF390C00) // hero display numerals on the coral container
val CoralBadge = Color(0xFFFFB59F)
val CoralChip = Color(0xFFFFF5F1)
val CoralTrack = Color(0xFFF3AD97)
val BrandCoral = Color(0xFFF05D42)

// Food (green) — doubles as the app-wide brand green.
val Green = Color(0xFF1F6B3D)
val GreenContainer = Color(0xFFD5E8CE)
val GreenInk = Color(0xFF12401F)
val GreenBody = Color(0xFF1F5432)
val GreenTrack = Color(0xFFB9D8AE)

// Training (indigo).
val Indigo = Color(0xFF3D5AF1)
val IndigoContainer = Color(0xFFDEE1FF)
val IndigoInk = Color(0xFF1B2A80)
val IndigoTrack = Color(0xFFC2C8F9)
// Indigo at ~25% on white — past-week bars on the Training progress charts.
val IndigoMuted = Color(0xFFC9D0F5)
// Resting outline for selection circles and drag handles (Training pickers/editor).
val NeutralOutline = Color(0xFFCBC3B4)
val NeutralOutlineDark = Color(0xFF4A443A)
// Unselected radio ring (Turn 11 grouped radio rows) — darker than NeutralOutline
// so the empty ring reads as an affordance on white rows.
val RadioOutline = Color(0xFFA79C8E)
val RadioOutlineDark = Color(0xFF6E6558)

// Profile (teal).
val Teal = Color(0xFF177D6E)
val TealContainer = Color(0xFFCBE8E2)
val TealInk = Color(0xFF0C4A42)
val TealBody = Color(0xFF155E52)
val TealTrack = Color(0xFFC4E2DC)

// Macro trio — distinct from every tab container they sit on. Protein is rose
// (Turn 8): the old teal collided with the Food green and Profile teal.
val MacroCarbs = Color(0xFFB98300)
val MacroProtein = Color(0xFFE11D48)
val MacroFat = Color(0xFF6750A4)

// Macro wavy-progress tracks on white cards (tonal containers use white @75%
// instead — two-track system per the Turn 8/9 design kit).
val MacroCarbsTrack = Color(0xFFEDDDB6)
val MacroProteinTrack = Color(0xFFF6CDD8)
val MacroFatTrack = Color(0xFFD9D2EC)

// Destructive family — delete pills and rose program badges.
val DestructiveContainer = Color(0xFFFBDCE3)
val DestructiveInk = Color(0xFF8F1239)

// Trust/provenance chips ("Open Food Facts", "Edited by you").
val TrustChipFill = Color(0xFFEDE5D8)
val TrustChipInk = Color(0xFF4A4238)

// Selected chip pair — the near-black "dark chip" used for selected filter,
// sort, unit and preset chips on both cream and white grounds.
val ChipDark = Color(0xFF2A2420)
val OnChipDark = Color(0xFFFBF7F1)

// Camera chrome (barcode / nutrition-label scanners) — used as-is in both
// themes; the camera surface is dark by nature.
val CameraSurface = Color(0xFF1B1813)
val ViewfinderBracket = Color(0xFF7BC98F)
// Translucent circles/pills floated over the camera preview (cream @15%).
val CameraTranslucent = Color(0xFFF8F2E9).copy(alpha = 0.15f)

// Water tracker.
val Water = Color(0xFF1668A8)
val WaterFill = Color(0xFFDDEBF7) // empty tracker cells, tonal water buttons

// Vitals tile families (Turn 8 §8a) — metric-scoped data colors for the Today
// grid, independent of tab accents: amber kcal, indigo steps, rose protein,
// blue water. Indigo and water reuse the shared accents above; each family
// adds an on-container ink and a deeper display ink.
val VitalsAmber = Color(0xFFE08A00)
val VitalsAmberContainer = Color(0xFFFFE1B3)
val VitalsAmberOn = Color(0xFF7A4A00)
val VitalsAmberDisplay = Color(0xFF4A2C00)
val VitalsIndigoDisplay = Color(0xFF10195C)
val VitalsRoseContainer = Color(0xFFFBDCE3)
val VitalsRoseOn = Color(0xFF8F1239)
val VitalsRoseDisplay = Color(0xFF5C0D24)
val VitalsWaterOn = Color(0xFF0D4A7A)
val VitalsWaterDisplay = Color(0xFF0A3D66)

// Health Connect banner (lavender).
val LavenderContainer = Color(0xFFE6E0F5)
val LavenderBody = Color(0xFF453768)
val LavenderInk = Color(0xFF4F3D8F)

// Semantic warning amber ("goal not met", AI honesty banners) — never a tab accent.
val AmberContainer = Color(0xFFFFE1B3)
val AmberInk = Color(0xFF7A4A00)

// MusFit dark palette — warm near-black ground with elevated warm cards,
// containers deepened and accents brightened for AA on dark.
val DarkBg = Color(0xFF16120C)
val DarkCard = Color(0xFF221C14)
val DarkTonalFill = Color(0xFF2E2820)
val DarkOnSurface = Color(0xFFEDE6DA)
val DarkOnSurfaceVariant = Color(0xFFA89E90)
val DarkInkFaint = Color(0xFF7D7466)
val DarkHairline = Color(0xFF2A241B)
val DarkTrack = Color(0xFF332C22)

val CoralBright = Color(0xFFFFB59A)
val CoralOnDark = Color(0xFF4A1500)
val CoralContainerDark = Color(0xFF4C2314)
val CoralInkDark = Color(0xFFFFDBCF)
val CoralBadgeDark = Color(0xFF6E3B26)
val CoralChipDark = Color(0xFF33261D)
val CoralTrackDark = Color(0xFF6E3B26)

val GreenBright = Color(0xFF85CB93)
val GreenOnDark = Color(0xFF05320F)
val GreenContainerDark = Color(0xFF21402A)
val GreenInkDark = Color(0xFFD9EFD9)
val GreenTrackDark = Color(0xFF375A3F)

val IndigoBright = Color(0xFF99A5FF)
val IndigoOnDark = Color(0xFF0A1560)
val IndigoContainerDark = Color(0xFF2A3170)
val IndigoInkDark = Color(0xFFE1E4FF)
val IndigoTrackDark = Color(0xFF454E9E)

val TealBright = Color(0xFF5BC4B2)
val TealOnDark = Color(0xFF003731)
val TealContainerDark = Color(0xFF14453E)
val TealInkDark = Color(0xFFCFF0E9)
val TealTrackDark = Color(0xFF23685C)

val MacroCarbsDark = Color(0xFFE0B54A)
val MacroProteinDark = Color(0xFFFF8FA9)
val MacroFatDark = Color(0xFFB4A0E8)

// Muted macro tracks on dark cards (visibly tinted, low emphasis).
val MacroCarbsTrackDark = Color(0xFF4A3F22)
val MacroProteinTrackDark = Color(0xFF4A2530)
val MacroFatTrackDark = Color(0xFF3A3350)

val DestructiveContainerDark = Color(0xFF4A1D29)
val DestructiveInkDark = Color(0xFFF7CFD9)

// Trust chip in dark reuses DarkTonalFill as its fill (see MusFitColors).
val TrustChipInkDark = Color(0xFFCFC5B6)
val WaterDark = Color(0xFF62A8DC)
val WaterFillDark = Color(0xFF1C3A52)

// Vitals tile families on dark: containers deepened one warm step, display inks
// brightened past the on-container ink (mirroring the light display rule).
val VitalsAmberDisplayDark = Color(0xFFFFEFD3)
val VitalsIndigoDisplayDark = Color(0xFFF0F1FF)
val VitalsRoseContainerDark = Color(0xFF4C1A2B)
val VitalsRoseOnDark = Color(0xFFFFD9E1)
val VitalsRoseDisplayDark = Color(0xFFFFEDF1)
val VitalsWaterOnDark = Color(0xFFC9E3F7)
val VitalsWaterDisplayDark = Color(0xFFE4F2FE)
val LavenderContainerDark = Color(0xFF352C4E)
val LavenderBodyDark = Color(0xFFCFC3EF)
val LavenderInkDark = Color(0xFFD8CCFA)
val AmberBright = Color(0xFFF2B04E)
val AmberContainerDark = Color(0xFF4A340F)
val AmberInkDark = Color(0xFFF7DFB5)
