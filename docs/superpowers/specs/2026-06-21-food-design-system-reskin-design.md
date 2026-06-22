# MusFit Food UI — Slice 1: Design System & Re-skin

**Date:** 2026-06-21
**Status:** Approved design, pending implementation plan
**Area:** Food miniapp (`com.musfit.ui.food`, `com.musfit.ui.theme`)

## Background

The Food screen is functionally rich but visually incoherent. The Material theme
([`Theme.kt`](../../../app/src/main/java/com/musfit/ui/theme/Theme.kt)) defines only four colors and is
bypassed entirely: [`FoodScreen.kt`](../../../app/src/main/java/com/musfit/ui/food/FoodScreen.kt) declares
its own file-local palette and carries ~111 hardcoded `Color(0x…)` literals plus ~226 ad-hoc `.sp`/`FontWeight`
usages (337 inline style literals total). The result is a neon-lime gradient, a forest-green ink, pastel macro
colors, and a warm-beige background all competing — and no dark-mode capability.

This is the first of three sequenced slices that move the Food miniapp toward an agreed "north-star" redesign
(premium, Lifesum-influenced). Each slice ships and is verified independently.

- **Slice 1 (this spec): Design system + re-skin.** Build a real token layer and route the Food screen through
  it. Same layout, coherent premium skin.
- **Slice 2 (future): Structure & hierarchy.** Header rework (centered date nav, Eaten/Burned ring), condense the
  dashboard so meals surface, category-icon meal cards, coral FAB, real bottom-nav icons.
- **Slice 3 (future): Food imagery.** Image loading + Open Food Facts product photos with category-art fallback,
  richer meal cards.

## Goals

1. Establish a single source of truth for color, typography, spacing, and shape, exposed through `MusFitTheme`.
2. Re-skin the Food screen by replacing every hardcoded style literal with a token reference. **No layout or
   behavior change.**
3. Adopt the approved **"Evergreen × Lifesum" (B1)** palette: emerald-green primary, warm coral accent, warm
   cream surfaces.
4. Structure the token layer so dark mode can be added later by defining one additional color set (light ships
   now; dark is **not** wired in this slice).

## Non-goals (explicitly out of scope for Slice 1)

- Any layout, navigation, or information-architecture change (the action button-strip, weekly-plan strip, all six
  stat cards, and letter-based bottom nav **stay exactly where they are**).
- Header rework, FAB, real nav icons, food thumbnails, card elevation/shadow redesign — these are Slice 2/3.
- Defining dark-theme color values or wiring `isSystemInDarkTheme()`.
- A custom font (the system font is kept; a typeface swap is a later option).
- Refactoring Today, Training, or Health screens.

## Design direction (B1 — "Evergreen × Lifesum", light)

Green remains the signature (data, calorie ring, primary), warmed by Lifesum-style cream surfaces, with coral as
the secondary accent for add actions, favorites, and highlights.

| Token (semantic) | Hex | Use |
|---|---|---|
| `brand` | `#1E7A53` | Primary emerald (maps to M3 `primary`) |
| `onBrand` | `#FFFFFF` | Content on `brand` |
| `brandInk` | `#0F3D2E` | Deep-green text/icons on the gradient header |
| `brandGradient` | `#8FE38C → #43C57E → #1E7A53` | Header background brush |
| `accent` | `#FF7A66` | Coral — add buttons, favorites, highlights |
| `onAccent` | `#FFFFFF` | Content on `accent` |
| `accentContainer` | `#FFEAE4` | Soft coral fill (tags, empty-meal add) |
| `onAccentContainer` | `#E45B43` | Content on `accentContainer` |
| `background` | `#FBF7F1` | App background (cream; maps to M3 `background`) |
| `surface` | `#FFFFFF` | Cards / sheets (maps to M3 `surface`) |
| `surfaceVariant` | `#F4EEE6` | Subtle warm fills (avatars, chips) |
| `onSurface` | `#2A2420` | Primary text (warm ink) |
| `onSurfaceVariant` | `#8C8178` | Secondary/muted text |
| `outline` | `#ECE4DA` | Hairlines / dividers |
| `track` | `#EFE7DC` | Progress-bar tracks |
| `macroProtein` | `#0D9488` | Protein (teal) |
| `macroCarbs` | `#F59E0B` | Carbs (amber) |
| `macroFat` | `#6D5BD0` | Fat (violet) |
| `positive` | `#1E7A53` | "Good" rating |
| `positiveContainer` | `#E7F4EC` | "Good" rating pill background |
| `warning` | `#E45B43` | "Needs work" rating |
| `warningContainer` | `#FFEAE4` | "Needs work" pill background |

All values are tunable; the macro trio in particular is a one-line change.

## Token architecture

New/changed files under `app/src/main/java/com/musfit/ui/theme/`:

- **`Color.kt`** — raw hex constants for the B1 palette (replaces the four current colors).
- **`MusFitColors.kt`** — `data class MusFitColors(...)` holding every semantic color above, plus a derived
  `macroColors: List<Color>` for index-based access; a `lightMusFitColors` instance; and
  `val LocalMusFitColors = staticCompositionLocalOf<MusFitColors> { error("MusFitColors not provided") }`.
  The data class is the dark-ready seam — a future `darkMusFitColors` is added here with no call-site changes.
- **`Type.kt`** — an explicit M3 `Typography` that **matches the current Material 3 default metrics**, so no text
  reflows in Slice 1. This establishes a single seam for type; the intentional type scale and the removal of
  ad-hoc `FontWeight` land in Slice 2, where layout changes are expected and welcome.
- **`Spacing.kt`** — `data class MusFitSpacing(xs=4, sm=8, md=12, lg=16, xl=20, xxl=24)` (`Dp`) + `LocalMusFitSpacing`.
- **`Shape.kt`** — M3 `Shapes` with radius tokens: small 8, medium 12, large 16 (+ a `pill`/`CircleShape` convention
  for fully-rounded elements).
- **`Theme.kt`** — `MusFitTheme(darkTheme: Boolean = false, content: @Composable () -> Unit)`:
  - Builds an M3 `lightColorScheme` from the tokens (`primary = brand`, `onPrimary = onBrand`,
    `secondary = accent`, `background`, `surface`, `surfaceVariant`, `onSurface`, `onSurfaceVariant`, `outline`, …).
  - Sets `typography = MusFitTypography` and `shapes = MusFitShapes`.
  - Wraps content in `CompositionLocalProvider(LocalMusFitColors provides colors, LocalMusFitSpacing provides spacing)`.
  - `darkTheme` is accepted but currently always resolves to the light token set (dark values arrive in a later
    slice). It is **not** defaulted from the system yet, so no undefined dark UI ships.
  - An accessor object exposes ergonomic reads:
    `object MusFitTheme { val colors @Composable get() = LocalMusFitColors.current; val spacing @Composable get() = LocalMusFitSpacing.current; val typography @Composable get() = MaterialTheme.typography; val shapes @Composable get() = MaterialTheme.shapes }`.

`MusFitTheme` is already applied at the app root ([`MainActivity.kt`](../../../app/src/main/java/com/musfit/MainActivity.kt)),
so no wiring is required.

## Re-skin approach (`FoodScreen.kt`)

Mechanical, section-by-section substitution — **layout, composable structure, and logic are untouched**:

- Delete the file-local palette (`FoodBackground`, `HeaderInk`, `ActionGreen`, `MacroColors`) and replace all
  references with `MusFitTheme.colors.*`.
- Replace inline `Color(0x…)` literals with the nearest semantic token. The near-identical one-off grays
  (`#706D6A`, `#6D6864`, `#67615D`, …) all collapse to `onSurfaceVariant`; warm hairlines (`#EDE8E4`, `#ECE…`) to
  `outline`; card whites to `surface`; the beige background to `background`.
- Replace the header `Brush.linearGradient(listOf(0xFFEFFF72, 0xFF63EF69, 0xFFB8F56A))` with
  `Brush.linearGradient(MusFitTheme.colors.brandGradient)`; header text uses `brandInk`.
- Replace `RoundedCornerShape(8.dp)` with `MusFitTheme.shapes.small` (identical 8dp radius — layout-safe).
  **Spacing and typography tokens are created and wired in this slice but applied to `FoodScreen` in Slice 2**:
  remapping the screen's many non-scale `dp` values, or swapping in a new type scale, would reflow the layout,
  which Slice 1 forbids. Text typography is left exactly as-is.
- The `ModalBottomSheet` `containerColor = Color.White` becomes `MusFitTheme.colors.surface`.
- **Macro index integrity:** today `MacroColors[0]`→Carbs, `[1]`→Protein, `[2]`→Fat (used by
  `MacroProgressRow` and `MealItemContributionBars`). The replacement `macroColors` list **must preserve this order**:
  `listOf(macroCarbs, macroProtein, macroFat)`. Verify contribution-bar labels (C/P/F) still map to the intended
  hues after the swap.

### Intentional visual result

After Slice 1: the exact current layout, recolored — lime→emerald-fresh gradient, cool→cream surfaces,
neon/pastel accents→coral + coherent teal/amber/violet macros, consistent type and spacing. Cards remain flat
(`elevation = 0`); white-on-cream contrast provides separation. Shadow/elevation polish is deferred to Slice 2.

## Cross-screen impact

Today, Training, and Health contain no hardcoded colors and do not read `MaterialTheme.colorScheme`; they render
via M3 component defaults. Updating the theme will shift their defaults slightly (warmer surfaces, emerald-tinted
controls) — coherent and acceptable. No code in those screens changes. They are screenshot-verified to confirm
nothing regresses.

## Testing & verification

This is a presentation-only refactor, so verification is build + visual, not new unit tests:

1. `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` is green. Existing
   ViewModel/repository tests assert logic only and must pass unchanged — if any fails, the re-skin altered
   behavior and must be corrected.
2. Install the debug APK on the connected device and capture before/after screenshots of **all four tabs**
   (Today, Food, Training, Health). Food shows the new coherent skin with identical layout; the other three show
   only minor default-driven shifts.
3. Confirm no remaining `Color(0x…)` literals in `FoodScreen.kt` (grep), and that the file no longer declares a
   local palette.

## Risks & mitigations

- **Large mechanical edit in a ~4,700-line file** — risk of missed literals, mis-mapped macro indices, or subtle
  regressions. Mitigate with systematic section-by-section replacement, a final grep sweep, and screenshot diffing;
  never touch layout or logic.
- **M3 surface vs. background semantics** — ensure Scaffold/bottom-sheet/containers resolve to the intended token
  so nothing renders pure-white where cream is expected (or vice versa).
- **Tonal elevation tint** — keeping cards flat (elevation 0) avoids M3 tinting white cards with primary on the
  cream background.
- **OneDrive/Gradle flakiness** — the known `app/build` `AccessDeniedException`; recover per the documented
  stop-daemon-and-clean procedure, not by changing code.

## Definition of done

- Token layer (`Color`, `MusFitColors`, `Type`, `Spacing`, `Shape`, `Theme`) implemented and provided by
  `MusFitTheme`.
- `FoodScreen.kt` has **zero hardcoded colors** and no local palette; corner shapes use `MusFitTheme.shapes`.
  (Typography/spacing application and the resulting `FontWeight`/`dp` cleanup are Slice 2.)
- Verification steps above pass; before/after screenshots captured.
- No behavior change; all existing tests green.
