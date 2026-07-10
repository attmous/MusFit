# M3E Phase 1 — Foundation + Today pilot (implementation plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`).

**Goal:** Build the stable-Compose M3E design foundation (light+dark color, bundled Google Sans Flex, expanded rounded shapes, springy motion tokens) and prove it end-to-end on the Today screen + bottom nav, in light and dark.

**Architecture:** No `MaterialExpressiveTheme`/alpha. Evolve MusFit's own token layer over the **standard** `MaterialTheme` (material3 1.4.0 stable): add `darkMusFitColors`, switch on `isSystemInDarkTheme()`, bundle a Google Sans Flex variable font via `FontVariation`, expand `MusFitShapes`, add `MusFitMotion` `spring()` tokens. Apply to Today + nav; dark-correct the Canvas chart kit.

**Tech Stack:** Kotlin, Jetpack Compose (material3 1.4.0 stable), variable fonts (`FontVariation`, API 26 ≤ minSdk 28), JUnit. No new alpha deps; no Room change.

**Spec:** [`docs/superpowers/specs/2026-06-28-m3e-google-fonts-overhaul-design.md`](../specs/2026-06-28-m3e-google-fonts-overhaul-design.md)

---

## Process constraints

- Work in an isolated git worktree so concurrent changes remain untouched. Source the environment with `. .\scripts\android\android-env.ps1`.
- Full gate: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.
- On-device verification needs **light + dark** screenshots (toggle system dark mode via `adb shell "cmd uimode night yes|no"`). Screenshot via `adb exec-out`/`adb pull` (PowerShell `>` corrupts PNGs). Pixel 8 Pro is 1344×2992.
- Colour values below are a starting point — tune on-device during this pilot (spec allows).

## File structure

| File | Change |
|------|--------|
| `ui/theme/Color.kt` (modify) | Add the dark raw palette + dark accent stops |
| `ui/theme/MusFitColors.kt` (modify) | Add `darkMusFitColors` |
| `ui/theme/TabAccent.kt` (modify) | Make `tabAccentFor` dark-aware (`@Composable`) |
| `ui/theme/Theme.kt` (modify) | `musFitColorScheme(colors, dark)` → light/darkColorScheme; `MusFitTheme(darkTheme = isSystemInDarkTheme())` |
| `ui/theme/Type.kt` (modify) | Google Sans Flex `FontFamily` + full M3 type scale |
| `app/src/main/res/font/google_sans_flex.ttf` (create) | Bundled variable font (OFL) |
| `ui/theme/Shape.kt` (modify) | Expanded M3E radii |
| `ui/theme/Motion.kt` (create) | `MusFitMotion` spring tokens |
| `ui/today/TodayScreen.kt` (modify) | Larger shapes + Google Sans Flex numerics; chart load-in motion |
| `ui/components/charts/*` (modify) | Dark-correct (already token-driven; verify) + optional load-in animation |
| `ui/AppNavGraph.kt` (modify) | M3E nav styling (pill indicator), light+dark |
| `test/.../ui/theme/MusFitColorsTest.kt` (create) | Pure light/dark token assertions |

---

### Task 1: Dark color system + light/dark theme switch

**Files:** Modify `Color.kt`, `MusFitColors.kt`, `TabAccent.kt`, `Theme.kt`; Test `MusFitColorsTest.kt`.

- [ ] **Step 1: Write the failing test** — create `app/src/test/java/com/musfit/ui/theme/MusFitColorsTest.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusFitColorsTest {
    @Test
    fun lightAndDarkPalettesDiffer_andDarkIsActuallyDark() {
        assertTrue("dark background must be darker than light",
            darkMusFitColors.background.luminance() < lightMusFitColors.background.luminance())
        assertTrue("dark surface must be dark",
            darkMusFitColors.surface.luminance() < 0.2f)
        assertTrue("light surface must be light",
            lightMusFitColors.surface.luminance() > 0.8f)
    }

    @Test
    fun macroColorsOrderPreservedInBothPalettes() {
        assertEquals(listOf(lightMusFitColors.macroCarbs, lightMusFitColors.macroProtein, lightMusFitColors.macroFat),
            lightMusFitColors.macroColors)
        assertEquals(listOf(darkMusFitColors.macroCarbs, darkMusFitColors.macroProtein, darkMusFitColors.macroFat),
            darkMusFitColors.macroColors)
    }
}
```

- [ ] **Step 2: Run it red**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.theme.MusFitColorsTest" --no-daemon --console=plain
```
Expected: FAIL — `darkMusFitColors` unresolved.

- [ ] **Step 3: Add the dark raw palette.** Append to `Color.kt`:

```kotlin
// MusFit M3E dark palette — warm near-black surfaces, brighter accents.
val DarkBg = Color(0xFF14110F)
val DarkSurface = Color(0xFF221E1A)
val DarkSurfaceVariant = Color(0xFF2B2621)
val DarkOnSurface = Color(0xFFF3EDE6)
val DarkOnSurfaceVariant = Color(0xFFB0A79E)
val DarkOutline = Color(0xFF332E29)
val DarkTrack = Color(0xFF332E29)

val EmeraldBright = Color(0xFF3CCB9B)
val EmeraldOnDark = Color(0xFF08321F)
val EmeraldContainerDark = Color(0xFF13402E)
val EmeraldInkDark = Color(0xFFBFF3DD)
val CoralBright = Color(0xFFFF8A66)
val CoralContainerDark = Color(0xFF48230F)
val CoralInkDark = Color(0xFFFFD9C9)
val IndigoBright = Color(0xFF8593FF)
val IndigoContainerDark = Color(0xFF29306B)
val IndigoInkDark = Color(0xFFDDE1FF)
val TealBright = Color(0xFF3CCBC9)
val TealContainerDark = Color(0xFF0C3B3A)
val TealInkDark = Color(0xFFC7F2F1)
val MacroProteinDark = Color(0xFF2DD4BF)
val MacroCarbsDark = Color(0xFFFBBF24)
val MacroFatDark = Color(0xFFA78BFA)
val PositiveContainerDark = Color(0xFF1B3A2B)
val WaterDark = Color(0xFF5BA8E8)
```

- [ ] **Step 4: Add `darkMusFitColors`.** Append to `MusFitColors.kt` (after `lightMusFitColors`, before `LocalMusFitColors`):

```kotlin
val darkMusFitColors = MusFitColors(
    brand = EmeraldBright,
    onBrand = EmeraldOnDark,
    brandInk = EmeraldInkDark,
    brandGradient = listOf(GradientLime, GradientGreen, EmeraldBright),
    accent = CoralBright,
    onAccent = Color(0xFF3A1606),
    accentContainer = CoralContainerDark,
    onAccentContainer = CoralInkDark,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    track = DarkTrack,
    macroProtein = MacroProteinDark,
    macroCarbs = MacroCarbsDark,
    macroFat = MacroFatDark,
    positive = EmeraldBright,
    positiveContainer = PositiveContainerDark,
    warning = CoralBright,
    warningContainer = CoralContainerDark,
    water = WaterDark,
)
```

- [ ] **Step 5: Make `tabAccentFor` dark-aware.** Replace `TabAccent.kt`'s function:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
fun tabAccentFor(destination: AppDestination): TabAccent =
    if (isSystemInDarkTheme()) tabAccentForDark(destination) else tabAccentForLight(destination)

private fun tabAccentForLight(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(Coral, CardWhite, CoralContainer, CoralInk)
    AppDestination.Food -> TabAccent(Emerald, CardWhite, PositiveContainer, EmeraldInk)
    AppDestination.Training -> TabAccent(Indigo, CardWhite, IndigoContainer, IndigoInk)
    AppDestination.Profile -> TabAccent(Teal, CardWhite, TealContainer, TealInk)
}

private fun tabAccentForDark(destination: AppDestination): TabAccent = when (destination) {
    AppDestination.Today -> TabAccent(CoralBright, Color(0xFF3A1606), CoralContainerDark, CoralInkDark)
    AppDestination.Food -> TabAccent(EmeraldBright, EmeraldOnDark, EmeraldContainerDark, EmeraldInkDark)
    AppDestination.Training -> TabAccent(IndigoBright, Color(0xFF0B1240), IndigoContainerDark, IndigoInkDark)
    AppDestination.Profile -> TabAccent(TealBright, Color(0xFF06302F), TealContainerDark, TealInkDark)
}
```

> `tabAccentFor` is now `@Composable`. All callsites (`TodayScreen`, `TrainingScreen`, `AppNavGraph`) already call it inside `@Composable` scope, so no callsite signature change — but re-verify they compile in Step 7.

- [ ] **Step 6: Wire the dark switch in `Theme.kt`.** Replace the `musFitColorScheme` + `MusFitTheme` with:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme

private fun musFitColorScheme(colors: MusFitColors, dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = colors.brand, onPrimary = colors.onBrand,
            secondary = colors.accent, onSecondary = colors.onAccent,
            secondaryContainer = colors.accentContainer, onSecondaryContainer = colors.onAccentContainer,
            background = colors.background, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
        )
    } else {
        lightColorScheme(
            primary = colors.brand, onPrimary = colors.onBrand,
            secondary = colors.accent, onSecondary = colors.onAccent,
            secondaryContainer = colors.accentContainer, onSecondaryContainer = colors.onAccentContainer,
            background = colors.background, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
        )
    }

@Composable
fun MusFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkMusFitColors else lightMusFitColors
    CompositionLocalProvider(
        LocalMusFitColors provides colors,
        LocalMusFitSpacing provides MusFitSpacing(),
    ) {
        MaterialTheme(
            colorScheme = musFitColorScheme(colors, darkTheme),
            typography = MusFitTypography,
            shapes = MusFitShapes,
            content = content,
        )
    }
}
```

(Add `import androidx.compose.material3.lightColorScheme` if not already present — it is.)

- [ ] **Step 7: Run green + compile**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.theme.MusFitColorsTest" --no-daemon --console=plain
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: test PASS; compile SUCCESS (confirms `tabAccentFor` callsites still resolve).

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/theme/Color.kt app/src/main/java/com/musfit/ui/theme/MusFitColors.kt app/src/main/java/com/musfit/ui/theme/TabAccent.kt app/src/main/java/com/musfit/ui/theme/Theme.kt app/src/test/java/com/musfit/ui/theme/MusFitColorsTest.kt
git commit -m "feat(theme): add dark palette and light/dark switch (M3E foundation)"
```

---

### Task 2: Bundle Google Sans Flex + M3E type scale

**Files:** Create `res/font/google_sans_flex.ttf`; Modify `Type.kt`.

- [ ] **Step 1: Download the variable font (OFL).** Google Sans Flex is on Google Fonts under the SIL Open Font License. Fetch the variable TTF into `res/font/` (lowercase, digits/underscore only filename):

```powershell
New-Item -ItemType Directory -Force app\src\main\res\font | Out-Null
# Google Fonts variable TTF (find the current raw URL from fonts.google.com / github.com/google/fonts ofl/googlesansflex):
Invoke-WebRequest -Uri "https://github.com/google/fonts/raw/main/ofl/googlesansflex/GoogleSansFlex%5BGRAD,ROND,opsz,slnt,wght%5D.ttf" -OutFile app\src\main\res\font\google_sans_flex.ttf
```
> If that exact path 404s, locate the file under `github.com/google/fonts` `ofl/googlesansflex/` (or `apache/`), or download the family ZIP from `https://fonts.google.com/download?family=Google%20Sans%20Flex` and extract the variable TTF. Confirm it is the **variable** TTF (has `wght`/`ROND` axes). Also commit the font's `OFL.txt` alongside if shipping requires the license file.

- [ ] **Step 2: Rebuild `Type.kt`** with a Google Sans Flex display/heading family + Roboto body:

```kotlin
package com.musfit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.musfit.R

private fun gsf(weight: Int, round: Float = 50f) = Font(
    R.font.google_sans_flex,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("ROND", round), // rounded M3E feel
    ),
    weight = FontWeight(weight),
)

private val GoogleSansFlex = FontFamily(
    gsf(400), gsf(500), gsf(600), gsf(700),
)

/** Body/label stay on the system Roboto; display/headline/title use Google Sans Flex. */
val MusFitTypography: Typography = Typography().let { base ->
    fun TextStyle.gsf(weight: FontWeight) = copy(fontFamily = GoogleSansFlex, fontWeight = weight)
    base.copy(
        displayLarge = base.displayLarge.gsf(FontWeight.W700),
        displayMedium = base.displayMedium.gsf(FontWeight.W700),
        displaySmall = base.displaySmall.gsf(FontWeight.W600),
        headlineLarge = base.headlineLarge.gsf(FontWeight.W700),
        headlineMedium = base.headlineMedium.gsf(FontWeight.W700),
        headlineSmall = base.headlineSmall.gsf(FontWeight.W600),
        titleLarge = base.titleLarge.gsf(FontWeight.W600),
        titleMedium = base.titleMedium.gsf(FontWeight.W600),
        titleSmall = base.titleSmall.gsf(FontWeight.W600),
        // body* and label* keep the default Roboto family.
    )
}
```

> `FontVariation.Setting("ROND", value)` sets a custom axis; if the literal `ROND` axis isn't present in the shipped TTF, drop that line (weight alone still works). Verify min API: `variationSettings` needs API 26 — minSdk 28 ✓.

- [ ] **Step 3: Compile + verify the font loads**

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```
Expected: SUCCESS. (Visual confirmation happens in Task 7's screenshots.)

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/res/font/ app/src/main/java/com/musfit/ui/theme/Type.kt
git commit -m "feat(theme): bundle Google Sans Flex and build the M3E type scale"
```

---

### Task 3: Expand shapes to the M3E scale

**Files:** Modify `Shape.kt`.

- [ ] **Step 1: Bump the radii.** Replace `MusFitShapes`:

```kotlin
/** M3E-scale corner radii — larger, rounder. Cards use extraLarge (28dp). */
val MusFitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

- [ ] **Step 2: Compile**

```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/theme/Shape.kt
git commit -m "feat(theme): expand corner radii to the M3E scale"
```

---

### Task 4: Motion tokens

**Files:** Create `ui/theme/Motion.kt`.

- [ ] **Step 1: Create `Motion.kt`** — springy specs mirroring M3E (stable `spring()`):

```kotlin
package com.musfit.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Springy M3E-style motion tokens (stable Compose; no MotionScheme opt-in). */
object MusFitMotion {
    /** Bounds/shape/offset — overshoot-friendly. */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)

    /** Color/alpha — no overshoot. */
    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
}
```

- [ ] **Step 2: Compile**

```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/theme/Motion.kt
git commit -m "feat(theme): add M3E-style spring motion tokens"
```

---

### Task 5: Apply the foundation to the Today screen + dark-correct the charts

**Files:** Modify `ui/today/TodayScreen.kt` (and verify `ui/components/charts/*` render correctly in dark — they are token-driven via passed `Color`s and `MusFitTheme.colors`, so dark is automatic; confirm no hardcoded colors).

- [ ] **Step 1: Bump card shapes + numeric type on Today.** In `TodayScreen.kt`, the cards currently use `MusFitTheme.shapes.large`. For the hero cards (`DailyRingsCard`, `WeeklyCaloriesCard`, `WeightTrendCard`, `CoachBriefingCard`) switch to `MusFitTheme.shapes.extraLarge` (28dp). Example — `DailyRingsCard`:

```kotlin
        shape = MusFitTheme.shapes.extraLarge,
```
Apply the same `shape = MusFitTheme.shapes.extraLarge` swap to `WeeklyCaloriesCard`, `WeightTrendCard`, and `CoachBriefingCard` (each currently `shapes.large`). Leave `GlimpseTile`/`WeeklyMiniTracker`/`MacroBar` on their smaller shapes.

- [ ] **Step 2: Make the big numbers use the display type.** The ring centre labels and the weight hero already use `titleSmall`/`titleLarge` (now Google Sans Flex via Task 2 — automatic). Bump the weight hero in `WeightTrendCard` from `titleLarge` to `headlineMedium` for the bold M3E numeral:

```kotlin
                        style = MaterialTheme.typography.headlineMedium,
```
(in `WeightTrendCard`, the `"${it.formatMetric()} kg"` Text).

- [ ] **Step 3: Compile + dark audit.** Search the Today path for hardcoded colors that would break in dark:

```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Then grep (Grep tool) `Color(0x` and `Color\.` under `app/src/main/java/com/musfit/ui/today/` and `ui/components/charts/` — any literal color must route through `MusFitTheme.colors`/`tabAccent`. (The chart kit takes `Color` params from `tabAccentFor`, which is now dark-aware, and `MusFitTheme.colors`, so it adapts; the `#fff` bubble text in `WeekBarChart` uses `onAccent` — confirm `onAccent` reads acceptably in dark.) Fix any stragglers.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/today/TodayScreen.kt
git commit -m "feat(today): apply M3E shapes and display type to Today cards"
```

---

### Task 6: M3E bottom-nav styling (light + dark)

**Files:** Modify `ui/AppNavGraph.kt`.

- [ ] **Step 1: Read `AppNavGraph.kt`** (the `NavigationBar`/`NavigationBarItem` block) and ensure: `NavigationBar` uses `MusFitTheme.colors.surface` container; the selected item's indicator uses the per-tab `tabAccentFor(destination).container` with `selectedIconColor = tabAccentFor(destination).onContainer` (already partly wired per the grounding). Confirm the pill indicator reads in both themes; no hardcoded colors.

- [ ] **Step 2: Compile**

```powershell
.\gradlew.bat compileDebugKotlin --no-daemon --console=plain
```
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/musfit/ui/AppNavGraph.kt
git commit -m "feat(nav): M3E bottom-nav styling for light and dark"
```

---

### Task 7: Full gate + on-device light/dark screenshots

**Files:** none (verification).

- [ ] **Step 1: Full gate**

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```
Expected: `BUILD SUCCESSFUL`; all tests pass; lint clean.

- [ ] **Step 2: Install + screenshot LIGHT**

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell "cmd uimode night no"
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
Start-Sleep -Seconds 3
adb shell screencap -p /sdcard/m3e_light.png; adb pull /sdcard/m3e_light.png "$env:TEMP\m3e_light.png"
```

- [ ] **Step 3: Screenshot DARK**

```powershell
adb shell "cmd uimode night yes"
Start-Sleep -Seconds 2
adb shell screencap -p /sdcard/m3e_dark.png; adb pull /sdcard/m3e_dark.png "$env:TEMP\m3e_dark.png"
adb shell "cmd uimode night no"
```

- [ ] **Step 4: Eyeball both.** Confirm: warm-cream light / warm-dark surfaces; 28dp rounded hero cards; bold Google Sans Flex numerals (rings, weight, "Today"); coral accents (brighter in dark); rings/bars/trend render correctly in dark with the brighter accents; pill nav indicator in both. Tune color/shape/type values if anything reads off, re-run Step 1, commit `fix(...)`. Otherwise no commit.

---

## Notes for the executor

- This is **Phase 1** of three (spec §Decomposition). It restyles the foundation + Today + nav; Food, Training, Health get their own plans.
- Dark mode is now driven entirely by `MusFitTheme.colors` + dark-aware `tabAccentFor`; the per-screen work in later phases is mostly verifying each surface in dark and bumping shapes/type.
- If `lintDebug` flags `MissingTranslation`/contrast or unused imports, fix per the message.
- Keep all expressive feel on **stable** APIs — do not pull `material3:1.5.0-alpha`.
