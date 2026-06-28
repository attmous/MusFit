# Material 3 Expressive + Google fonts — app-wide overhaul (design)

**Date:** 2026-06-28
**Status:** Approved design — implementation plans to follow (phased)
**Scope:** App-wide (Today, Food, Training, Health) — cross-cutting design-system work, explicitly requested.

## Goal

Re-skin all of MusFit in **Material 3 Expressive (M3E)** — Google's 2025 design language — with Google's now-open typefaces, so the app reads like the new Google Health app: bold rounded shapes, vibrant tonal color, confident type, and springy expressive motion, in **both light and dark**. No feature/behaviour changes — this is presentation, typography, theming, and motion only.

The approved visual target is the light/dark M3E mockup of the Today screen (rounded 28dp cards, pill segmented toggles, bold Google-Sans-style numbers, rounded-square expressive FAB, pill nav indicator), keeping MusFit's warm brand (cream light / warm near-black dark, fixed per-tab accents).

## Locked decisions

- **Whole app** in scope (Today, Food, Training, Health), delivered as **phased plans** (foundation first).
- **Light + dark** themes, switched by `isSystemInDarkTheme()`. Warm cream light / warm near-black dark — brand survives the switch.
- **Fonts:** Google Sans Flex (display/headline/title + big numbers) + Roboto (body/label), **bundled** in `res/font/` (offline-first; ~0.5–1 MB APK).
- **Fixed per-tab accents** (Coral/Emerald/Indigo/Teal), light + dark variants — **no Dynamic Color** (matches Google Health's fixed accent).
- **Adopt M3E motion** (springy specs) for chart load-in, sheet expand, FAB feedback, and selection.
- **Implementation approach = hybrid:** wrap `MaterialExpressiveTheme` so standard components get M3E for free, *and* keep MusFit's token layer (`MusFitTheme`, `TabAccent`, spacing) on top. Keep the hand-rolled Canvas chart kit (it adopts the new tokens; we do not switch to a chart library).

## Non-goals (YAGNI)

- No Dynamic Color / wallpaper theming.
- No third-party chart library — the existing Canvas kit stays.
- No data, ViewModel, repository, DAO, or navigation-graph changes; no Room migration.
- No new screens or features; layout changes only where M3E components/shape require.
- Not changing the per-tab accent *hues* (only adding dark variants + M3E tonal roles).

## Current state (grounding)

- `Theme.kt` wraps the **standard** `MaterialTheme` (not expressive), **light only** — `MusFitTheme(darkTheme)` exists but `darkTheme` is an unused seam; colors come from `lightMusFitColors`; `musFitColorScheme()` builds a `lightColorScheme(...)`.
- `Type.kt` = `MusFitTypography = Typography()` — stock M3 defaults (system Roboto); a comment already earmarks "the intentional type scale + FontWeight cleanup" for a later slice.
- `MusFitColors.kt` — one `lightMusFitColors` instance; `LocalMusFitColors` is a `staticCompositionLocalOf`. Tokens: brand/onBrand/brandInk, accent/onAccent/accentContainer/onAccentContainer, background, surface, surfaceVariant, onSurface, onSurfaceVariant, outline, track, macroProtein/Carbs/Fat, positive/positiveContainer, warning/warningContainer, water.
- `Shape.kt` — `MusFitShapes = Shapes(extraSmall=6, small=8, medium=12, large=16, extraLarge=20)`.
- `Spacing.kt` — `MusFitSpacing(xs=4..xxl=24)` via `LocalMusFitSpacing`.
- `TabAccent.kt` — `tabAccentFor(AppDestination)` returns a fixed `TabAccent(color,onColor,container,onContainer)` per tab (Today=Coral, Food=Emerald, Training=Indigo, Profile=Teal); not composition-local-backed, obtained per screen.
- `MusFitTheme` object exposes `colors/spacing/typography/shapes`.
- `composeBom = 2026.04.01` (recent — exposes the M3E API surface). No `res/font/` directory yet (system Roboto).

## Architecture

### Foundation (the design-system layer — restyles ~80% via tokens)

1. **Theme entry.** `MusFitTheme(darkTheme)` switches between `lightMusFitColors` / `darkMusFitColors` and wraps `MaterialExpressiveTheme(colorScheme = …, motionScheme = …, shapes = …, typography = …)` instead of `MaterialTheme`. The `MusFitTheme` object accessors and `TabAccent` are unchanged in shape (gain dark awareness). Default `darkTheme = isSystemInDarkTheme()` at the call site (`MainActivity`/root).

2. **Color, light + dark.** Add `darkMusFitColors` (warm near-black surfaces, brighter accents) alongside `lightMusFitColors`. `musFitColorScheme(colors, dark)` returns `expressiveLightColorScheme(...)` / `expressiveDarkColorScheme(...)` mapped from MusFit tokens. `TabAccent` gains light + dark variants per tab (brighter container/onContainer in dark). All token hues stay; we add the dark counterparts + any M3E tonal roles the components need.

3. **Fonts.** Bundle Google Sans Flex (variable) + Roboto in `res/font/`. Build two `FontFamily`s via `Font(resId, variationSettings = FontVariation.Settings(...))` (variable-font axes; min API 26 ≤ minSdk 28 ✓). Rebuild `MusFitTypography` as a full M3 type scale: display/headline/title roles → Google Sans Flex (heavier/rounded for the expressive feel), body/label → Roboto. Big numeric values (rings, hero stats) use the Google Sans Flex display weights.

4. **Shape.** Expand `MusFitShapes` to the M3E scale (larger radii — cards ~28dp). Add `androidx.graphics:graphics-shapes` and use `MaterialShapes`/`RoundedPolygon`+`Morph` for expressive shape-morph elements (loaders, selection states).

5. **Motion.** Adopt the M3E `MotionScheme` (springy spatial/effects specs) via `MaterialExpressiveTheme`; expose a small `MusFitMotion` token surface (or read `MaterialTheme.motionScheme`) for: chart load-in (left-to-right), sheet/expander transitions, FAB and selection feedback.

> The exact M3E API surface at this BOM (whether `MaterialExpressiveTheme`/components are stable or `@ExperimentalMaterial3ExpressiveApi`, the precise component names, the `graphics-shapes` version, and the variable-font wiring) is verified in a dedicated research pass and pinned in the implementation plan's first tasks.

### Rollout (per miniapp — adopt M3E components + motion, verify dark)

Each surface, after the foundation lands:
- **Bottom nav** → M3E expressive navigation (pill active indicator).
- **FABs** → expressive FAB shape/motion (Food add, etc.).
- **Toggles / toolbars** → M3E (Training metric toggle, Food date nav, any segmented selectors).
- **Cards / sheets / buttons** → M3E shapes + motion; Food's ~10 modal sheets get the expressive dress.
- **Charts** (`MetricRing`/`WeekBarChart`/`TrendLineChart` + `ExerciseTrendChart`) → consume the new shape/color/motion tokens, gain dark colors and a load-in animation.
- **Dark-mode pass** → every screen verified in dark; fix any hardcoded colors that bypass tokens.

## Decomposition — one design, three phased plans

Each plan produces working, verifiable software on its own.

1. **Phase 1 — Foundation + Today pilot.** All of the Foundation layer above, plus applying it to the Today screen (the freshest, already chart-ified surface) and the bottom nav, proving the whole system end-to-end in light **and** dark. This is the dependency for everything else and the first executable plan.
2. **Phase 2 — Food.** The largest surface: `FoodScreen` + `FoodComponents` + `FoodModalSheets` + `FoodAddPanelUi` + `FoodTrackersUi` adopt M3E components/shape/motion; full dark QA across the sheets.
3. **Phase 3 — Training + Health + final QA.** Training (incl. `ExerciseTrendChart`) and Health screens; cross-app light/dark screenshot QA; any token cleanup.

The implementation plan written now covers **Phase 1**; Phases 2–3 get their own plans when reached (each its own spec-aligned plan → execute cycle).

## Risks & mitigations

- **Experimental M3E APIs.** `MaterialExpressiveTheme` and several expressive components may require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` and could shift between versions. Mitigation: pin the Compose BOM, isolate all expressive usage behind `MusFitTheme`/the component kit, and centralize opt-ins. **Fallback:** if the expressive APIs are too unstable, evolve MusFit's own tokens (custom theme) to mimic M3E — same visuals (Google fonts, shapes, color, hand-rolled springy motion), more custom code, no `MaterialExpressiveTheme`. The visual target and fonts are unaffected either way.
- **Dark mode doubles the color surface** (tokens + per-screen QA). Mitigation: drive everything through tokens; the dark pass is an explicit phase per miniapp.
- **APK size** from bundled variable fonts (~0.5–1 MB). Acceptable for offline-first; revisit only if it becomes an issue.
- **Hardcoded colors** that bypass `MusFitTheme.colors` will break in dark. Mitigation: a grep/audit task per phase to route them through tokens.
- **Scope** is multi-session; Food is the long pole. Mitigation: phased plans, each independently shippable.
- **Build/process:** implement in a git worktree **outside OneDrive** (recurring constraint: OneDrive syncs `.worktrees/**/app/build`, causing Gradle `AccessDenied` and locking cleanup). Source the env from the main tree. Run the full Gradle gate; capture **light + dark** on-device screenshots.

## Verification

- **Pure unit tests** where logic is testable: color-token mapping (light vs dark), type-scale construction, any `FontVariation`/shape helpers extracted as pure functions, contrast sanity checks.
- **Full Gradle gate** (`testDebugUnitTest lintDebug assembleDebug`) per phase.
- **On-device, light + dark** screenshots per surface (the only way to confirm the visual + dark correctness, since there's no screenshot-test infra).
- No behavioural regressions: existing ViewModel/domain tests stay green (we touch theme/UI only).

## Open considerations (resolved unless flagged)

1. **Light theme tone** — staying warm cream (not cool/white) per the approved mockup. (Resolved.)
2. **Card radius** — 28dp hero cards per the mockup; smaller M3E radii (12/16dp) for dense/inner elements. (Resolved in foundation; tune in pilot.)
3. **Google Sans Flex axes** — use weight + an expressive grade/roundness setting for the "rounded" M3E feel; exact axis values tuned in the pilot against on-device screenshots.
4. **Motion intensity** — start with M3E default springs; dial per surface during the pilot.
