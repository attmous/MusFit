# M3E Phase 2+3 — Food, Training, Health rollout + final QA (implementation plan)

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** Roll the M3E foundation through the remaining miniapps (Food, Training, Health/Profile) and finish with a cross-app light/dark QA pass.

**Architecture:** The Phase-1 foundation already does the heavy lifting — light/dark, Google Sans Flex, and the expanded shape scale propagate through `MusFitTheme` tokens. An audit found the app is **fully token-driven** (the only literal color is a legitimate `Color.Black.copy(alpha=0.45f)` modal scrim), so dark mode already works app-wide. The remaining work is shape polish (round content cards to the 28dp hero radius for consistency with Today) plus on-device light/dark verification per surface.

**Spec:** [`docs/superpowers/specs/2026-06-28-m3e-google-fonts-overhaul-design.md`](../specs/2026-06-28-m3e-google-fonts-overhaul-design.md)

## Process constraints

Worktree **outside OneDrive** (`C:\Users\att1a\.mfwt\m3e23`). Source env from main tree. Full gate + on-device **light and dark** screenshots (`adb shell "cmd uimode night yes|no"`, pull via `adb`). Stable Compose only — no `material3:1.5.0-alpha`.

---

### Task 1: Round content cards to the 28dp M3E hero radius

The Phase-1 foundation set `MusFitTheme.shapes.large = 20dp` and `extraLarge = 28dp`, and Today's hero cards use `extraLarge`. For app-wide consistency, bump the full-width content-card surfaces in Food/Training/Profile from `shapes.large` to `shapes.extraLarge`.

**Files (each: replace `shape = MusFitTheme.shapes.large` → `shape = MusFitTheme.shapes.extraLarge` on the full-width card Surfaces):**
- `ui/training/TrainingScreen.kt` (6), `ui/training/TrainingActiveWorkoutContent.kt` (6), `ui/training/TrainingProgressContent.kt` (3), `ui/training/TrainingRoutineContent.kt` (2)
- `ui/food/AddFoodScreen.kt` (2)
- `ui/profile/ProfileScreen.kt`, `ui/profile/ProfileSettingsScreen.kt` (inspect + bump card surfaces)

- [ ] **Step 1:** For each file above, read it, then `replace_all` `MusFitTheme.shapes.large` → `MusFitTheme.shapes.extraLarge` (these are all full-width content cards; verify none is a small inner element where 28dp would look bulbous — if so, leave that one at `large`).
- [ ] **Step 2:** Inspect `ProfileScreen.kt` / `ProfileSettingsScreen.kt` and any `FoodComponents.kt` / `FoodScreen.kt` card surfaces; bump the hero/content cards similarly. Leave intentional small `RoundedCornerShape(Ndp)` literals (pills, thumbnails, chart bits) untouched.
- [ ] **Step 3: Compile** — `.\gradlew.bat compileDebugKotlin --no-daemon --console=plain`. Expected: SUCCESS.
- [ ] **Step 4: Commit** — `git commit -m "feat(m3e): round content cards to 28dp across Food/Training/Health"`.

---

### Task 2: Full gate

- [ ] **Step 1:** `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`. Expected: `BUILD SUCCESSFUL`, tests pass, lint clean.

---

### Task 3: On-device light/dark QA across all miniapps

- [ ] **Step 1: Install** — `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
- [ ] **Step 2: For each mode** (`adb shell "cmd uimode night no"` then `... yes`), launch and navigate to **Food**, **Training**, and **Health/Profile** (tap the bottom-nav items; Pixel 8 Pro is 1344×2992), screenshotting each (`adb shell screencap -p /sdcard/x.png; adb pull ...`).
- [ ] **Step 3: Eyeball all six** (3 screens × light/dark). Confirm: warm-cream light / warm-dark surfaces; 28dp rounded cards; Google Sans Flex headings; per-tab accents (Food=Emerald, Training=Indigo, Profile=Teal — brighter in dark); no unreadable text, no light-mode-only colors bleeding into dark, no over-rounded small elements.
- [ ] **Step 4: Fix any stragglers** found (a card that should/shouldn't be 28dp, a contrast issue), re-run Task 2, commit `fix(m3e): ...`. Restore `cmd uimode night no` when done.

---

## Notes
- Expected total code change is small (shape bumps); the value is the verification that the token-driven foundation renders correctly across every surface in both themes.
- If a Training/Food chart (`ExerciseTrendChart`, the Canvas kit) reads off in dark, confirm it's pulling `tabAccentFor`/`MusFitTheme.colors` (dark-aware) rather than a literal.
