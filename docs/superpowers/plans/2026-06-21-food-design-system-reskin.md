# Food Design System & Re-skin (Slice 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace MusFit's bypassed 4-color theme and `FoodScreen.kt`'s ~111 hardcoded color literals with a real, dark-ready design-token layer, re-skinning the Food screen in the approved "Evergreen × Lifesum" (B1) palette with no layout or behavior change.

**Architecture:** A token layer under `ui/theme/` exposes color/spacing/shape/typography through a `MusFitTheme` composable + accessor object (mirroring Material 3's `MaterialTheme` dual function/object pattern). Color tokens are fully applied to `FoodScreen`; shape tokens are applied 1:1 (8dp → `shapes.small`, layout-safe); spacing and typography tokens are created and wired now but applied in Slice 2 to avoid reflow. Light ships; the `MusFitColors` data class is the seam for a future dark scheme.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, JUnit (plain JVM unit test for token integrity), Gradle (`gradlew.bat`), adb screenshots for visual verification.

**Spec:** `docs/superpowers/specs/2026-06-21-food-design-system-reskin-design.md`

**Prerequisite for every Gradle/adb command (Windows PowerShell):**
```powershell
. .\.superpowers\sdd\android-env.ps1
```

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `app/src/main/java/com/musfit/ui/theme/Color.kt` | Raw B1 hex constants | Rewrite (replace 4 colors) |
| `app/src/main/java/com/musfit/ui/theme/MusFitColors.kt` | Semantic color token holder + `lightMusFitColors` + `LocalMusFitColors` | Create |
| `app/src/main/java/com/musfit/ui/theme/Spacing.kt` | Spacing scale token + local | Create |
| `app/src/main/java/com/musfit/ui/theme/Shape.kt` | Corner-radius `Shapes` tokens | Create |
| `app/src/main/java/com/musfit/ui/theme/Type.kt` | Baseline `Typography` seam | Create |
| `app/src/main/java/com/musfit/ui/theme/Theme.kt` | `MusFitTheme` composable + accessor object | Rewrite |
| `app/src/main/java/com/musfit/ui/food/FoodScreen.kt` | Food UI — re-skin (colors + shapes) | Modify |
| `app/src/test/java/com/musfit/ui/theme/MusFitColorsTest.kt` | Lock macro-index integrity + key hex values | Create |

---

## Task 1: Color tokens + `MusFitColors` (with integrity test)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/theme/Color.kt`
- Create: `app/src/main/java/com/musfit/ui/theme/MusFitColors.kt`
- Test: `app/src/test/java/com/musfit/ui/theme/MusFitColorsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/musfit/ui/theme/MusFitColorsTest.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MusFitColorsTest {
    @Test
    fun macroColors_preserveLegacyCarbsProteinFatOrder() {
        // FoodScreen indexes the macro list as [0]=Carbs, [1]=Protein, [2]=Fat.
        // The token-derived list MUST keep that order or the macro UI mis-colors.
        val colors = lightMusFitColors
        assertEquals(
            listOf(colors.macroCarbs, colors.macroProtein, colors.macroFat),
            colors.macroColors,
        )
    }

    @Test
    fun lightPalette_usesApprovedBrandAccentBackground() {
        assertEquals(Color(0xFF1E7A53), lightMusFitColors.brand)
        assertEquals(Color(0xFFFF7A66), lightMusFitColors.accent)
        assertEquals(Color(0xFFFBF7F1), lightMusFitColors.background)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.theme.MusFitColorsTest" --no-daemon --console=plain`
Expected: FAIL — compilation error, unresolved reference `lightMusFitColors` / `MusFitColors`.

- [ ] **Step 3: Write the raw palette constants**

Replace the entire contents of `app/src/main/java/com/musfit/ui/theme/Color.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.ui.graphics.Color

// MusFit "Evergreen x Lifesum" (B1) palette — light.
val Emerald = Color(0xFF1E7A53)
val EmeraldInk = Color(0xFF0F3D2E)
val GradientLime = Color(0xFF8FE38C)
val GradientGreen = Color(0xFF43C57E)
val GradientEmerald = Color(0xFF1E7A53)
val Coral = Color(0xFFFF7A66)
val CoralContainer = Color(0xFFFFEAE4)
val CoralInk = Color(0xFFE45B43)
val Cream = Color(0xFFFBF7F1)
val CardWhite = Color(0xFFFFFFFF)
val WarmFill = Color(0xFFF4EEE6)
val WarmInk = Color(0xFF2A2420)
val WarmMuted = Color(0xFF8C8178)
val WarmOutline = Color(0xFFECE4DA)
val WarmTrack = Color(0xFFEFE7DC)
val MacroProtein = Color(0xFF0D9488)
val MacroCarbs = Color(0xFFF59E0B)
val MacroFat = Color(0xFF6D5BD0)
val PositiveContainer = Color(0xFFE7F4EC)
```

- [ ] **Step 4: Write the `MusFitColors` token holder**

Create `app/src/main/java/com/musfit/ui/theme/MusFitColors.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for MusFit. Immutable; the dark-ready seam — a future
 * `darkMusFitColors` instance drops in here with no call-site changes.
 */
data class MusFitColors(
    val brand: Color,
    val onBrand: Color,
    val brandInk: Color,
    val brandGradient: List<Color>,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val track: Color,
    val macroProtein: Color,
    val macroCarbs: Color,
    val macroFat: Color,
    val positive: Color,
    val positiveContainer: Color,
    val warning: Color,
    val warningContainer: Color,
) {
    /** Index order preserved from the legacy `MacroColors` list: [carbs, protein, fat]. */
    val macroColors: List<Color> get() = listOf(macroCarbs, macroProtein, macroFat)
}

val lightMusFitColors = MusFitColors(
    brand = Emerald,
    onBrand = CardWhite,
    brandInk = EmeraldInk,
    brandGradient = listOf(GradientLime, GradientGreen, GradientEmerald),
    accent = Coral,
    onAccent = CardWhite,
    accentContainer = CoralContainer,
    onAccentContainer = CoralInk,
    background = Cream,
    surface = CardWhite,
    surfaceVariant = WarmFill,
    onSurface = WarmInk,
    onSurfaceVariant = WarmMuted,
    outline = WarmOutline,
    track = WarmTrack,
    macroProtein = MacroProtein,
    macroCarbs = MacroCarbs,
    macroFat = MacroFat,
    positive = Emerald,
    positiveContainer = PositiveContainer,
    warning = CoralInk,
    warningContainer = CoralContainer,
)

val LocalMusFitColors = staticCompositionLocalOf<MusFitColors> {
    error("MusFitColors not provided. Wrap content in MusFitTheme.")
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.theme.MusFitColorsTest" --no-daemon --console=plain`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/musfit/ui/theme/Color.kt app/src/main/java/com/musfit/ui/theme/MusFitColors.kt app/src/test/java/com/musfit/ui/theme/MusFitColorsTest.kt
git commit -m "feat(theme): add B1 color tokens and MusFitColors

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Spacing, Shape, and Type token files

**Files:**
- Create: `app/src/main/java/com/musfit/ui/theme/Spacing.kt`
- Create: `app/src/main/java/com/musfit/ui/theme/Shape.kt`
- Create: `app/src/main/java/com/musfit/ui/theme/Type.kt`

- [ ] **Step 1: Create `Spacing.kt`**

```kotlin
package com.musfit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Created and provided in Slice 1; applied to FoodScreen in Slice 2. */
data class MusFitSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
)

val LocalMusFitSpacing = staticCompositionLocalOf { MusFitSpacing() }
```

- [ ] **Step 2: Create `Shape.kt`**

```kotlin
package com.musfit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner-radius tokens. `small` = 8dp matches FoodScreen's current RoundedCornerShape(8.dp). */
val MusFitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
```

- [ ] **Step 3: Create `Type.kt`**

```kotlin
package com.musfit.ui.theme

import androidx.compose.material3.Typography

/**
 * Baseline typography = Material 3 defaults, so no text reflows in Slice 1.
 * The intentional type scale and FontWeight cleanup land in Slice 2.
 */
val MusFitTypography = Typography()
```

- [ ] **Step 4: Verify it compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/musfit/ui/theme/Spacing.kt app/src/main/java/com/musfit/ui/theme/Shape.kt app/src/main/java/com/musfit/ui/theme/Type.kt
git commit -m "feat(theme): add spacing, shape, and baseline type tokens

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Wire `MusFitTheme`

**Files:**
- Modify (rewrite): `app/src/main/java/com/musfit/ui/theme/Theme.kt`

- [ ] **Step 1: Rewrite `Theme.kt`**

Replace the entire contents of `app/src/main/java/com/musfit/ui/theme/Theme.kt`:

```kotlin
package com.musfit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun musFitColorScheme(colors: MusFitColors) = lightColorScheme(
    primary = colors.brand,
    onPrimary = colors.onBrand,
    secondary = colors.accent,
    onSecondary = colors.onAccent,
    secondaryContainer = colors.accentContainer,
    onSecondaryContainer = colors.onAccentContainer,
    background = colors.background,
    onBackground = colors.onSurface,
    surface = colors.surface,
    onSurface = colors.onSurface,
    surfaceVariant = colors.surfaceVariant,
    onSurfaceVariant = colors.onSurfaceVariant,
    outline = colors.outline,
)

@Composable
fun MusFitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Light only in Slice 1; `darkTheme` is the seam for a later slice.
    val colors = lightMusFitColors
    CompositionLocalProvider(
        LocalMusFitColors provides colors,
        LocalMusFitSpacing provides MusFitSpacing(),
    ) {
        MaterialTheme(
            colorScheme = musFitColorScheme(colors),
            typography = MusFitTypography,
            shapes = MusFitShapes,
            content = content,
        )
    }
}

/** Ergonomic token accessors, mirroring Material 3's `MaterialTheme` object. */
object MusFitTheme {
    val colors: MusFitColors
        @Composable get() = LocalMusFitColors.current
    val spacing: MusFitSpacing
        @Composable get() = LocalMusFitSpacing.current
    val typography: Typography
        @Composable get() = MaterialTheme.typography
    val shapes: Shapes
        @Composable get() = MaterialTheme.shapes
}
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL. (`fun MusFitTheme` and `object MusFitTheme` coexisting is valid — same pattern as Material 3's `MaterialTheme`.)

- [ ] **Step 3: Verify the app still assembles**

Run: `.\gradlew.bat assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL. (FoodScreen still has its own literals; the theme change only affects its 18 `MaterialTheme.colorScheme` reads and M3 component defaults for now.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/musfit/ui/theme/Theme.kt
git commit -m "feat(theme): build MusFitTheme from tokens with accessor object

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Re-skin `FoodScreen.kt` colors

This task removes the file-local palette and replaces every hardcoded color with a token. **Do not change any layout, modifier ordering, composable structure, or logic — only color sources.** Preserve any `.copy(alpha = …)` by applying it to the token (e.g. `MusFitTheme.colors.brandInk.copy(alpha = 0.18f)`).

First add `import com.musfit.ui.theme.MusFitTheme` to `FoodScreen.kt`'s imports (keep the existing `import androidx.compose.ui.graphics.Color` — `Color` is still used as a parameter type). All `MusFitTheme.colors.*` reads are valid here because every call site is inside a `@Composable`. Where a composable indexes the macro list, read it once at the top: `val macroColors = MusFitTheme.colors.macroColors`.

**Mapping table (apply everywhere; this is the source of truth for the sweep):**

| Legacy literal / constant | Replace with |
|---|---|
| `FoodBackground` (`0xFFF0ECE7`) | `MusFitTheme.colors.background` |
| `HeaderInk` (`0xFF073F34`) | `MusFitTheme.colors.brandInk` |
| `ActionGreen` (`0xFF43F05A`) | `MusFitTheme.colors.brand` |
| `MacroColors` list | `MusFitTheme.colors.macroColors` |
| gradient `0xFFEFFF72, 0xFF63EF69, 0xFFB8F56A` | `MusFitTheme.colors.brandGradient` |
| `Color.White` | `MusFitTheme.colors.surface` |
| `Color.Black` | `MusFitTheme.colors.brandInk` |
| `0xFF315847` (green emphasis: kcal, planned, meal initial) | `MusFitTheme.colors.brand` |
| `0xFF706D6A`, `0xFF6D6864`, `0xFF67615D`, `0xFF766C66`, and other warm grays used for text | `MusFitTheme.colors.onSurfaceVariant` |
| `0xFFEDE8E4` and other warm hairlines/dividers/borders | `MusFitTheme.colors.outline` |
| `0xFFF6F2EF`, `0xFFE9E3DF` and other soft warm fills/tints (avatar bg, add-button bg, chips) | `MusFitTheme.colors.surfaceVariant` |
| coral "needs work" text (`0xFFE4…`/red) | `MusFitTheme.colors.warning` |
| coral "needs work" pill background | `MusFitTheme.colors.warningContainer` |

**Fallback rule for any literal not listed above**, decide by role: warm-gray text → `onSurfaceVariant`; near-white surface → `surface`; warm border/divider → `outline`; soft warm fill → `surfaceVariant`; green emphasis → `brand`; dark green on the gradient header → `brandInk`. If a literal's role is genuinely ambiguous, stop and ask rather than guess.

- [ ] **Step 1: Delete the file-local palette**

Remove this block at the bottom of `FoodScreen.kt` (currently ~lines 4752–4759):

```kotlin
private val FoodBackground = Color(0xFFF0ECE7)
private val HeaderInk = Color(0xFF073F34)
private val ActionGreen = Color(0xFF43F05A)
private val MacroColors = listOf(
    Color(0xFF99A7FF),
    Color(0xFFFF91B4),
    Color(0xFFC7A7FF),
)
```

- [ ] **Step 2: Replace the header gradient and header inks**

In `FoodSummaryHeader`, change the background brush:

```kotlin
// before
.background(
    Brush.linearGradient(
        listOf(Color(0xFFEFFF72), Color(0xFF63EF69), Color(0xFFB8F56A)),
    ),
)
// after
.background(Brush.linearGradient(MusFitTheme.colors.brandGradient))
```

Replace every `HeaderInk` reference (header title/date, side metrics, calorie-ring arcs and labels) with `MusFitTheme.colors.brandInk`, preserving any `.copy(alpha = …)`. In `CalorieRing`, replace the remaining-number `color = Color.Black` with `color = MusFitTheme.colors.brandInk`.

- [ ] **Step 3: Replace macro colors**

In `MacroProgressRow`, read the list once and index it:

```kotlin
@Composable
private fun MacroProgressRow(macros: List<FoodMacroProgressUiState>) {
    val macroColors = MusFitTheme.colors.macroColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        macros.forEachIndexed { index, macro ->
            MacroProgressCard(
                macro = macro,
                color = macroColors[index % macroColors.size],
                modifier = Modifier.weight(1f),
            )
        }
    }
}
```

In `MealItemContributionBars`, replace `ActionGreen` with `MusFitTheme.colors.brand` and the `MacroColors[…]` reads with a local list:

```kotlin
@Composable
private fun MealItemContributionBars(entry: FoodMealEntryUiState) {
    val macroColors = MusFitTheme.colors.macroColors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MealItemContributionBar(label = "Calories", progress = entry.calorieContribution, color = MusFitTheme.colors.brand)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealItemContributionBar(label = "P", progress = entry.proteinContribution, color = macroColors[1], modifier = Modifier.weight(1f))
            MealItemContributionBar(label = "C", progress = entry.carbsContribution, color = macroColors[0], modifier = Modifier.weight(1f))
            MealItemContributionBar(label = "F", progress = entry.fatContribution, color = macroColors[2], modifier = Modifier.weight(1f))
        }
    }
}
```

- [ ] **Step 4: Sweep all remaining color literals**

Apply the mapping table to every remaining `Color(0x…)`, `Color.White`, and `Color.Black` in the file, including the `ModalBottomSheet(containerColor = Color.White)` in `FoodScreen` → `containerColor = MusFitTheme.colors.surface`. Work top-to-bottom, section by section. Preserve all `.copy(alpha = …)`.

- [ ] **Step 5: Verify no color literals remain**

Run: `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain` → BUILD SUCCESSFUL.
Then grep:

```bash
grep -nE "Color\(0x|Color\.White|Color\.Black" app/src/main/java/com/musfit/ui/food/FoodScreen.kt
```

Expected: no matches. If any remain, map them by the fallback rule and re-run.

- [ ] **Step 6: Run unit tests + lint**

Run: `.\gradlew.bat testDebugUnitTest lintDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL — all existing ViewModel/repository tests pass (behavior unchanged) and lint is clean.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): re-skin FoodScreen with B1 color tokens

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Apply shape tokens in `FoodScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`

- [ ] **Step 1: Replace `RoundedCornerShape(8.dp)` with the shape token**

For every `shape = RoundedCornerShape(8.dp)` (Cards, Surfaces, etc.), replace with `shape = MusFitTheme.shapes.small`. The radius is identical (8dp) so nothing moves. Leave `CircleShape` and any non-8dp `RoundedCornerShape(…)` untouched (those are deliberate and/or rationalized in Slice 2).

- [ ] **Step 2: Verify**

Run: `.\gradlew.bat :app:compileDebugKotlin testDebugUnitTest --tests "com.musfit.ui.theme.MusFitColorsTest" --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL.
Then grep to confirm the 8dp shape literal is gone:

```bash
grep -n "RoundedCornerShape(8.dp)" app/src/main/java/com/musfit/ui/food/FoodScreen.kt
```

Expected: no matches.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/musfit/ui/food/FoodScreen.kt
git commit -m "feat(food): use shape tokens for 8dp corners

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: Full verification & screenshots

**Files:** none (verification only).

- [ ] **Step 1: Full build gate**

Run: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`
Expected: BUILD SUCCESSFUL. If it fails with a OneDrive `AccessDeniedException`/`Cannot snapshot` under `app/build`, recover and retry:

```powershell
.\gradlew.bat --stop
Start-Sleep -Seconds 3
Remove-Item -LiteralPath (Resolve-Path 'app\build').Path -Recurse -Force
```

- [ ] **Step 2: Install on the connected device**

Run:
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```
Expected: `Success`, app launches.

- [ ] **Step 3: Capture screenshots of all four tabs**

For each tab (the bottom nav still shows letters T/F/T/H in Slice 1), tap it, then capture. Use stdout capture to avoid Git Bash path mangling (run from Git Bash):

```bash
ADB="/c/Users/att1a/AppData/Local/Android/Sdk/platform-tools/adb.exe"
OUT="/c/Users/att1a/WS/MusFit/.superpowers/brainstorm/shots"
mkdir -p "$OUT"
"$ADB" exec-out screencap -p > "$OUT/after_food.png"   # repeat per tab: today/food/training/health
```

Expected: Food shows the new coherent B1 skin (cream background, fresh-emerald gradient header, coral accents, teal/amber/violet macros) with the **exact same layout** as before. Today/Training/Health show only minor default-driven shifts (warmer surfaces, emerald-tinted controls) and no broken UI.

- [ ] **Step 4: Confirm no regressions**

Visually compare against `.superpowers/brainstorm/999-1782071199/shots/musfit_before.png`. The Food layout (button strip, weekly strip, six stat cards, meal sections, letter nav) is unchanged in position — only colors differ. If anything moved or broke, it is a re-skin defect: fix the offending substitution (do not adjust layout).

- [ ] **Step 5: Final confirmation**

No commit needed (verification only). Slice 1 is complete when the build gate is green, the grep sweeps return nothing, and the screenshots confirm an identical layout with the new skin.

---

## Definition of Done

- Token layer (`Color`, `MusFitColors`, `Spacing`, `Shape`, `Type`, `Theme`) implemented and provided by `MusFitTheme`; `MusFitColorsTest` green.
- `FoodScreen.kt`: zero `Color(0x…)`/`Color.White`/`Color.Black`, no local palette, no `RoundedCornerShape(8.dp)`; colors and 8dp shapes use tokens.
- `testDebugUnitTest lintDebug assembleDebug` green; before/after screenshots captured for all four tabs; layout identical, behavior unchanged.
- Spacing/typography application and the Slice 2 structural rework (header, FAB, nav icons, meal cards, food imagery) are explicitly out of scope.
