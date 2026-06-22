# MusFit Training — Hevy-influenced UI redesign + per-tab color coding

**Date:** 2026-06-22
**Status:** Approved design pending spec review, then plan
**Area:** Training miniapp (`com.musfit.ui.training`) + a small design-system addition (`com.musfit.ui.theme`) and the bottom nav (`AppNavGraph`).

## Background

Training is the only miniapp still on **plain Material 3** (grey/lavender surfaces, default `colorScheme`) — not the B1 `MusFitTheme` tokens used by Food and Today. Its home is cluttered: a "Blank workout" resume card shown unconditionally, a standalone **Quick set** logger (Exercise/Reps/Weight + Log set), grey segmented buttons for the four sections, then the routine list. **Hevy** is the reference for the redesign.

Alongside the redesign, the user wants **per-tab color coding**: each miniapp gets its own accent identity on the shared cream/clean B1 foundation, so the app reads as four color-coded sections.

## Confirmed decisions

- **Scope:** full Hevy-influenced redesign across the overview, the active-workout logging experience, and the routine builder / progress — built in **three slices**.
- **Per-tab accent map:** Today = **Coral**, Food = **Emerald** (current), Training = **Indigo `#3D5AF1`**, Health = **Teal**. The bottom nav tints the **active** tab to its color. Training is fully re-themed to Indigo this round; the Today / Food / Health *screens* keep their current look (recoloring them is a later follow-up — only the nav reflects their colors now).

## Per-tab accent system (design-system addition)

- **Colors:** add `Indigo` (#3D5AF1, + ink/container shades) and `Teal` constants to `ui/theme/Color.kt` (Coral and Emerald already exist).
- **Token:** add `data class TabAccent(val color: Color, val onColor: Color, val container: Color, val onContainer: Color)` and a `fun tabAccentFor(destination: AppDestination): TabAccent` in `ui/theme/`, mapping each `AppDestination` to its accent (Today→Coral, Food→Emerald, Training→Indigo, Health→Teal).
- **Bottom nav** (`AppNavGraph`): each `NavigationBarItem`'s selected icon/label/indicator color = `tabAccentFor(destination).color` (unselected stays neutral). This delivers the visible "color code" cheaply, without touching the other screens.
- **Training screens** consume `tabAccentFor(AppDestination.Training)` (Indigo) for accented elements — header accents, primary buttons, active chips, the resume banner, and (Slice 2) the workout rings / rest timer. **Neutrals** (cream background, white cards, text, outlines, shapes) keep coming from `MusFitTheme.colors`/`.shapes`, so Training matches the family while owning Indigo.

This keeps `MusFitColors` as the shared neutral foundation and layers a per-destination accent on top — Food/Today/Health screens are unaffected because they don't consume `TabAccent`.

## The redesign — slices

### Slice 1 — Overview re-skin + Hevy home
- **Header:** "Training" title + a weekly stat line ("This week · N workouts · X kg") and a History shortcut icon.
- **Resume banner:** shown **only when a workout is active** (Indigo-container card + Resume), replacing the always-present "Blank workout" card.
- **Section switch:** re-skinned **chips** (Routines · History · Exercises · Progress) with an Indigo active chip, replacing grey segmented buttons.
- **Routines (home):** a prominent **Start empty workout** (Indigo), then **routine cards** — white B1 cards with name, "N exercises · N sets", muscle tags, a **Start** button, and a ⋮ menu (Edit / Duplicate / Delete; hidden for starter routines); plus **New routine**.
- **Remove** the standalone Quick-set card (you log sets inside a workout, Hevy-style).
- **History / Exercises / Progress** section content re-skinned to B1 + Indigo (cards, lists, chips) — same data and interactions, new styling.
- **No ViewModel behavior change** beyond the UI no longer rendering the quick-set card. The active-workout entry is unchanged here (re-skinned in Slice 2).

### Slice 2 — Active-workout logging (the core, Hevy-style)
- Redesign the full-screen logger: top bar (workout timer + Finish/Discard), per-exercise blocks with a clean **set table** (SET · PREVIOUS · KG · REPS · ✓), the **rest timer**, set types (warmup/working), RPE, add/duplicate set, add exercise.
- New features to brainstorm at Slice 2 (candidates: supersets, plate calculator, PR badges).

### Slice 3 — Routine builder + Progress charts
- Re-skin the routine editor (Indigo); turn the text-only Progress into simple **charts** (1RM / volume trend per exercise).

## Architecture

- **UI-only** redesign: reuse `TrainingViewModel` and `TrainingRepository` as-is. New composables replace the Material-default ones in `TrainingScreen.kt` + the four `Training*Content.kt` files. `TabAccent` lives in the theme package.
- The quick-set ViewModel state/handlers can remain (unused by the UI) and be cleaned up later, to keep this slice UI-focused.

## Testing & verification

- Existing `TrainingViewModelTest` stays green (UI-only changes; no VM behavior change).
- A small unit test for `tabAccentFor(...)` mapping (each destination → expected color).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green per slice; on-device screenshots of the Training home (+ the color-coded bottom nav) per slice.

## Non-goals

- Recoloring the Today / Food / Health **screens** (only the nav reflects their colors this round).
- Any change to the training data model or behavior (start/log/finish, routines, history, progress all keep working).
- Dark mode.

## Risks

- **Accent contrast:** Indigo text/elements must stay readable on cream and white; use the ink shade for text-on-light and `onColor` (white) for text-on-Indigo. Verify on-device.
- **Scope of re-skin:** the active-workout logger (Slice 2) is large and table-heavy; keep it to Slice 2 and brainstorm its feature set separately.

## Definition of done (design)

A Hevy-influenced Training redesign is specced: a per-tab color-coding system (Training = Indigo) with a color-coded bottom nav, a decluttered routines-first overview on B1 tokens (Slice 1), and a roadmap for the active-workout logger (Slice 2) and the routine builder + progress charts (Slice 3).
