# MusFit Food — Streamlined Add-Food Flow (search-first)

**Date:** 2026-06-22
**Status:** Approved design pending review, then plan
**Area:** Food add flow (`com.musfit.ui.food`)
**Builds on:** the Food UI redesign (tokens / structure / imagery, slices 1–3). The redesigned screen's coral FAB and per-meal "+" buttons open this new add screen.

## Background

Adding food today is an overloaded `ModalBottomSheet`: five mode tabs (Saved / Manual / Barcode / Quick / AI),
a standalone "Scan nutrition label" button, a "Keep adding" toggle, a Favorites block, and Templates + Recipes
lists — all stacked together. Several inputs are placeholders (AI text/voice/photo, the disabled label capture).

We're streamlining to the **Lifesum model**: a search-first, full-screen add view where the common path
(re-logging known foods) is front-and-center and everything else is one quiet tap away. Confirmed direction:
**search-first**, **AI cut**, **on-device label OCR built and folded into Create**.

## Goals

1. Replace the multi-tab bottom sheet with a single **full-screen, search-first add view**.
2. Make re-logging trivial: search + a Recents-first default (Same-as-yesterday / Last tracked / All recents).
3. Collapse the many entry points: barcode → a search-bar icon; Quick track → the ⋮ menu; Templates/Recipes →
   search results + the Create tab.
4. **Cut** the AI inputs entirely (stay local-first).
5. **Build** real on-device nutrition-label OCR, accessed from inside Create (not as a peer of Search).

## The screen

A full screen (not a sheet), opened with a target meal (e.g. "Breakfast"):

- **Top bar:** back arrow · meal title · **⋮ overflow**. The ⋮ menu holds **Quick track** (log bare
  calories/macros, no named food) and **Adjust goals** (opens the existing goal editor).
- **Search bar** "Food, meal or brand" with a **barcode-scan icon at the right end**. Search spans the user's
  saved foods, recipes, and saved meals/templates (each badged in results) plus Open Food Facts by name; the
  barcode icon opens the camera → Open Food Facts lookup.
- **Daily-intake card:** day's calories (`x / goal kcal`) + Carbs/Protein/Fat progress, mirroring the home
  screen, so the user sees headroom while logging. An "Adjust goals" affordance is also here.
- **Tabs:** **Recents · Favorites · Create**.
  - **Recents** (default): `Same as yesterday?` (yesterday's items for this meal), `Last tracked` (the most
    recent log), `All recents` (recently logged foods). Each row: thumbnail, name, kcal · serving, edit
    affordance, and a coral "+".
  - **Favorites:** the user's favorited foods / meals / recipes (reuses existing favorite data).
  - **Create:** add a food *or* a meal/recipe that isn't in any database. Top of the screen offers **Scan
    barcode** and **Scan label** to autofill; below, manual fields (name, brand, serving, macros, details).
    This is where the old Manual form, the standalone label scan, and "create a meal/recipe" converge.
- **Food rows / picking:** tapping a row (or its "+") logs it to the meal, with the existing
  serving/amount editing available on tap-through.

### Where every old input lands

| Old input | New home |
|---|---|
| Saved-food search | The search bar (primary) |
| Recents / "same as yesterday" | Recents tab (default) |
| Favorites | Favorites tab |
| Barcode | Search-bar icon → Open Food Facts |
| Manual entry | Create tab (manual fields) |
| Nutrition label | Create tab → "Scan label" (on-device OCR) |
| Templates / Recipes | Search results (badged) + Create tab ("create a meal") |
| Quick calories | ⋮ menu → Quick track |
| AI text / voice / photo | **Cut** |

## Reuse vs. new work

Most logic already exists and is simply re-presented; the genuinely new pieces are the recents queries and the
OCR feature.

- **Reused:** saved-food search + online search, favorites (`favoriteAddItems`), barcode lookup
  (`lookupBarcode` → Open Food Facts), quick-calorie logging, manual food creation, templates/recipes logging,
  goal editor, macro/daily-intake state.
- **New (data):** "recents" and "same as yesterday for this meal" — repository queries over the diary
  (recently logged foods; yesterday's items for the meal type), exposed as add-screen UI state. TDD in the
  repository/ViewModel.
- **New (feature):** on-device label OCR (ML Kit text recognition) + parsing recognized text into nutrition
  fields, wired to autofill the Create form. ML Kit text recognition is on-device (no cloud), consistent with
  the local-first constraint.

## Implementation slices

This is large; build it in sequenced, shippable slices (each its own plan):

1. **Slice A — Add screen shell (the core UX win).** New full-screen add view replacing the bottom sheet:
   top bar + ⋮ (Quick track, Adjust goals), search bar with barcode icon, daily-intake card, and the
   Recents / Favorites tabs wired to existing + new-recents data. Navigation: FAB and per-meal "+" route here.
   Cuts the AI forms and the standalone label button.
2. **Slice B — Create tab.** Consolidate manual food creation and meal/recipe creation into the Create tab,
   with "Scan barcode" autofill and a "Scan label" entry point (stubbed to a manual-review form until Slice C).
3. **Slice C — Label OCR.** ML Kit text-recognition dependency + a capture screen (reusing the CameraX setup
   from the barcode scanner) + a parser that maps recognized label text to the nutrition fields, autofilling
   Create. Includes parser unit tests over sample label text.

## Non-goals

- Cloud AI / LLM logging (text/voice/photo) — cut, stays out of scope.
- Changes to the home Food screen beyond routing its FAB/"+" into the new add screen.
- Redesigning the meal-detail screen.

## Testing & verification

- Repository/ViewModel TDD for the new recents / same-as-yesterday queries and the OCR parser.
- Existing logging tests stay green (the underlying log/save paths are reused).
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green per slice; on-device screenshots of the add
  screen (Recents default, search results, Create, barcode-in-search, Quick track via ⋮).

## Risks

- **Label-text parsing reliability** — nutrition labels vary wildly; the parser is best-effort and always lands
  on an editable review form before saving. Mitigate with a tolerant parser + manual correction + parser tests.
- **Scope** — three slices; sequence them and verify each independently. Slice A delivers the headline UX even
  if B/C come later.
- **Navigation change** — moving from a bottom sheet to a full screen touches `AppNavGraph`/routing; keep the
  barcode-scanner route and re-entry working.

## Definition of done (design)

The streamlined add flow is specced: a search-first full-screen view (search + barcode icon, daily-intake card,
Recents/Favorites/Create tabs, ⋮ with Quick track + Adjust goals), AI cut, label OCR built into Create, and a
three-slice implementation roadmap.
