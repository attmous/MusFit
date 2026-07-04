# MusFit Agent Handoff

## Product Direction

MusFit is an Android-only fitness and nutrition tracker inspired by Lifesum for food and Hevy for training. The intended top-level product structure is:

- Today
- Food / Cals in
- Training / Cals out
- Settings / Profile

Work menu by menu. The current active priority is the Food miniapp. The user wants a clean, polished Lifesum-like UX, but do not copy Lifesum assets or exact layouts. Build original UI with similar information architecture and a premium, practical feel.

## Tech Stack

- Android Kotlin
- Jetpack Compose Material 3
- Hilt ViewModels
- Room local database
- Kotlin Flow / coroutines
- Open Food Facts barcode/product lookup
- Health Connect boundary exists for Android health data, but Food nutrition sync is not implemented yet
- Local-first MVP: local account identity exists; no cloud sync, analytics, subscriptions, or social features

Main package/application id: `com.musfit`.

## Build And Test

Before Gradle/adb commands on Windows:

```powershell
. .\scripts\android\android-env.ps1
```

Focused Food tests:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalFoodRepositoryTest" --no-daemon --console=plain
```

Full verification before claiming completion or pushing:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Repo helper equivalent:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Full
```

Seeded emulator setup and verification:

```powershell
.\scripts\android\setup-musfit-emulator.ps1
.\scripts\android\install-seed-musfit.ps1 -Reset
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install and launch on a connected device:

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

Known connected phone serial used during development:

```text
38241FDJG00BLY
```

## OneDrive / Gradle Caveat

The repo is under OneDrive. Gradle occasionally fails on generated files with `AccessDeniedException`, `Cannot snapshot`, or `not a regular file` under `app/build`. This has been environmental/generated-output state, not usually a code defect.

Safe cleanup helper:

```powershell
.\scripts\dev\clean-generated.ps1
```

Manual cleanup pattern:

```powershell
.\gradlew.bat --stop
Start-Sleep -Seconds 3
$workspace = (Resolve-Path -LiteralPath '.').Path
$target = Resolve-Path -LiteralPath 'app\build' -ErrorAction SilentlyContinue
if ($target) {
  if ($target.Path.StartsWith($workspace, [System.StringComparison]::OrdinalIgnoreCase)) {
    Remove-Item -LiteralPath $target.Path -Recurse -Force -ErrorAction Stop
  } else {
    throw "Refusing to remove outside workspace: $($target.Path)"
  }
}
```

Then rerun the same verification command.

## Current Food Miniapp State

> The authoritative, detailed reference for Food is now
> [`docs/architecture/food-system.md`](docs/architecture/food-system.md) —
> full feature map, state/sheet-mode reference, data model, and the refactor
> backlog. The summary below is the quick handoff view.

The Food miniapp is feature-complete against most of the original 24-slice
roadmap. Shipped and working:

- **Diary**: date navigation, planning mode, calorie ring, macro progress,
  advanced-nutrient and micronutrient progress, meal sections + custom meals,
  per-item macro rows, deterministic daily insights and a day-rating card,
  weekly plan strip.
- **Add flow** (`FoodAddMode`): Saved (recents, same-as-yesterday, favorites,
  templates, recipes), Manual, Barcode (Open Food Facts lookup + edit + save),
  Quick calories (with favorite presets), and an AI shell. "Keep adding" mode.
- **Saved food database**: full editor (name/brand/barcode/category/favorite,
  per-100 g vs per-serving, custom servings, full macros + micros, delete,
  duplicate), local search, online search/import, duplicate detection + merge,
  starter-food import.
- **Servings**: per-food default + custom units with live amount preview.
- **Recipes v2**: ingredients, serving units, cooked yield, per-serving
  nutrition, edit/duplicate/delete/favorite, fractional-serving logging.
- **Meal templates v2**: editable items, duplicate/favorite, save current meal
  as template, log to any meal.
- **Custom meals**: rename, time, reorder.
- **Goals**: calorie + macro + advanced-nutrient targets, diet modes
  (`FoodGoalMode`: Balanced/HighProtein/KetoLowCarb/MuscleGain/WeightLoss/Custom),
  include-training-calories, net-carbs toggle.
- **Planning / shopping / water**: planned-vs-logged, copy day/meal, 7-day plan,
  shopping list generated from planned meals (grouped, checkable, manual adds),
  water tracking with goal.
- **Health Connect**: food/hydration export with sync state card.
- **Cross-cutting**: favorites across foods/templates/recipes/quick-logs, undo
  delete, copy/move entries, mark planned → logged.

Known shells / placeholders (intentional, not yet real):

- **Nutrition-label OCR** — camera + ML Kit text recognition feed a best-effort
  `NutritionLabelParser`; values are always shown for user review. Real parsing
  is partial.
- **AI voice / photo logging** — UX shells only; AI text logging produces a
  simple editable draft. Cloud AI is out of scope (local-first).

## Key Files

Food UI:

```text
app/src/main/java/com/musfit/ui/food/FoodScreen.kt
app/src/main/java/com/musfit/ui/food/FoodViewModel.kt
```

Food data/repository:

```text
app/src/main/java/com/musfit/data/repository/FoodRepository.kt
app/src/main/java/com/musfit/data/local/dao/FoodDao.kt
app/src/main/java/com/musfit/data/local/entity/FoodEntities.kt
app/src/main/java/com/musfit/data/remote/food/OpenFoodFactsProductProvider.kt
```

Food tests:

```text
app/src/test/java/com/musfit/ui/food/FoodViewModelTest.kt
app/src/test/java/com/musfit/data/repository/LocalFoodRepositoryTest.kt
```

Existing Food plans:

```text
docs/superpowers/plans/2026-06-20-food-menu-v1.md
docs/superpowers/plans/2026-06-20-food-v2-database-diary-management.md
docs/superpowers/plans/2026-06-21-food-meal-detail-menu.md
```

## Development Expectations

- New tasks use a branch + PR flow. Start from current `origin/master`, create a
  scoped branch (`codex/<task-name>`), implement, verify, push the branch, open a
  draft PR, and merge only after verification is complete.
- Prefer TDD for behavior changes: add/adjust failing ViewModel or repository tests first, run them red, then implement.
- Keep changes scoped to Food unless the user explicitly asks for Training, Today, Settings/Profile, or architecture changes.
- Follow existing repository/ViewModel/Compose patterns before introducing new abstractions.
- Keep UI dense but clean. Avoid marketing-style layouts inside the app.
- Do not add accounts, cloud sync, analytics, subscriptions, or social features unless explicitly requested.
- Keep data local-first.
- Before PR/merge, run full verification and install the debug build onto the
  seeded emulator. Use `.\scripts\android\install-seed-musfit.ps1 -Reset` and
  verify the touched workflow in-app with the seeded data.
- Do not push directly to `origin/master`; use PRs for integration unless the
  user explicitly asks for a different emergency workflow.

## Standard Task Flow

For each new task:

1. Sync from `origin/master` and create a scoped branch.
2. Read the relevant architecture docs and existing code before changing files.
3. Add or adjust focused tests first for behavior changes where practical.
4. Implement the change in the smallest relevant surface area.
5. Run focused tests for the touched area, then run:

   ```powershell
   .\scripts\dev\verify-musfit.ps1 -Preset Full
   ```

6. Install and seed the emulator:

   ```powershell
   .\scripts\android\install-seed-musfit.ps1 -Reset
   ```

7. Verify the changed user workflow on `MusFit_API36` / `emulator-5554` with
   screenshots or UI-tree evidence when the task is UI-visible.
8. Commit, push the branch, open a draft PR, and include the Gradle and emulator
   verification in the PR body.
9. Merge only after the PR is verified and approved by the user or required
   checks.

## Food Roadmap Status

The original 24-slice Food roadmap is largely delivered. Each shipped slice has
a plan under `docs/superpowers/plans/` (see "Existing Food plans" above for the
filenames). Status:

| #  | Slice | Status |
| -- | ----- | ------ |
| 1  | Unified Add Food Flow | Shipped |
| 2  | Full Serving Unit System | Shipped |
| 3  | Full Food Editor | Shipped |
| 4  | Barcode No-Match Custom Food Flow | Shipped |
| 5  | Nutrition Label Scan | Shell + best-effort parser; real OCR partial |
| 6  | Food Database Quality | Shipped (search, duplicate merge, states) |
| 7  | Meal Item Editor Polish | Shipped |
| 8  | Meal Detail Upgrade | Shipped |
| 9  | Favorites Everywhere | Shipped |
| 10 | Custom Meals And Meal Times | Shipped |
| 11 | Nutrition Goals And Diet Modes | Shipped (`FoodGoalMode`) |
| 12 | Net Carbs And Advanced Macros | Shipped |
| 13 | Micronutrient Foundation | Shipped |
| 14 | Recipes V2 | Shipped |
| 15 | Meal Templates V2 | Shipped |
| 16 | Meal Planning | Shipped |
| 17 | Shopping List | Shipped |
| 18 | Water Tracking | Shipped |
| 19 | Health Connect Nutrition Sync | Shipped |
| 20 | AI Logging Shell | Text draft works; voice/photo are shells |
| 21 | Daily Food Insights | Shipped (deterministic) |
| 22 | Food/Meal/Day Rating | Shipped (deterministic) |
| 23 | UX Polish Pass | Ongoing |
| 24 | Food Performance And Reliability | Ongoing |

Genuinely remaining Food work:

- Real nutrition-label OCR parsing (Slice 5 completion).
- Continued UX polish and performance hardening (Slices 23–24).
- AI voice/photo logging stay shells by design (no cloud AI; local-first).

### Active internal effort: Food structure refactor

A behavior-preserving **structure refactor** is underway. Done so far: the
Food UI is split by feature (`FoodScreen.kt` ~2,400 lines + `FoodComponents.kt`,
`FoodTrackersUi.kt`, `FoodModalSheets.kt`, `FoodAddPanelUi.kt`), the
amount-preview math moved into `NutritionCalculator`, and the `FoodUiState`
editor sub-state objects are **Tier 1b DONE**. The full plan, status, and the
test-coupling cost of remaining cleanup live in
[`docs/architecture/food-system.md`](docs/architecture/food-system.md#refactor-backlog).
Read it before doing large Food work so new code lands in the new structure.
