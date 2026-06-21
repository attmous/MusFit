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
- Local-first MVP: no accounts, cloud sync, analytics, subscriptions, or social features

Main package/application id: `com.musfit`.

## Build And Test

Before Gradle/adb commands on Windows:

```powershell
. .\.superpowers\sdd\android-env.ps1
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

Safe cleanup pattern:

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

The Food miniapp already includes:

- Main Food diary with date navigation, calories remaining, macro cards, and meal sections.
- Default meal sections: Breakfast, Lunch, Dinner, Snacks.
- Add-food bottom sheet with Saved, Manual, Barcode, Quick, templates, and recipes.
- Barcode lookup through Open Food Facts.
- Barcode result editing for name, brand, amount, calories, protein, carbs, fat.
- Live amount-based nutrition preview for scanned/manual foods.
- Barcode serving chips such as `100 g` and `Serving 170 g`.
- Separate scanned-product actions: `Save product` and `Log food`.
- Saved food database with create/edit/delete/search and favorite support.
- Saved food serving options.
- Online food search/import into saved database.
- Starter foods import.
- Quick calorie logging.
- Diary item editor: edit amount/meal, copy, delete, undo.
- Meal detail screen when tapping Breakfast/Lunch/Dinner/Snacks.
- Meal detail totals for calories, macros, fiber, sugar, saturated fat, sodium.
- Logged item rows show item calories and P/C/F macros.
- Meal templates basic support.
- Recipe basic support.
- Food goals editor and macro goal state.

Important recent commits:

- `6ac4013 feat: improve food barcode and meal detail UX`
- `9557e7d fix: preview barcode nutrition by amount`
- `4dfe43d feat: complete food app v4 polish`

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

- Prefer TDD for behavior changes: add/adjust failing ViewModel or repository tests first, run them red, then implement.
- Keep changes scoped to Food unless the user explicitly asks for Training, Today, Settings/Profile, or architecture changes.
- Follow existing repository/ViewModel/Compose patterns before introducing new abstractions.
- Keep UI dense but clean. Avoid marketing-style layouts inside the app.
- Do not add accounts, cloud sync, analytics, subscriptions, or social features unless explicitly requested.
- Keep data local-first.
- Push to `origin/master` when the user asks to push or when continuing the established deploy workflow after verified changes.

## Remaining Food Roadmap Slices

Use these as Codex CLI goals. Implement one slice at a time, verify, commit, push, and install on the phone when connected.

1. Unified Add Food Flow
   - Make Recent, Favorites, Search, Barcode, Manual, Quick Calories, Recipes, and Templates feel like one polished add flow.

2. Full Serving Unit System
   - Support `g`, `ml`, `oz`, `serving`, `piece`, `slice`, `scoop`, `cup`, and `package`.
   - Recalculate preview instantly and persist custom servings.

3. Full Food Editor
   - Complete saved food editing: name, brand, barcode, category, favorite, serving options, per-100g/per-serving toggle, calories/macros/details, delete, duplicate.

4. Barcode No-Match Custom Food Flow
   - On barcode miss, guide user into creating a custom food with that barcode attached.
   - Support `Save product` and `Save and log`.

5. Nutrition Label Scan Placeholder Flow
   - Add UX shell for scanning nutrition labels and editing extracted fields before save.
   - Structure so real OCR can be plugged in later.

6. Food Database Quality
   - Add source/quality badges, duplicate detection, merge/cleanup, better search/filtering, and robust empty/error/loading states.

7. Meal Item Editor Polish
   - Serving-unit editing, move/copy to meal/date, preview before save, consistent undo behavior.

8. Meal Detail Upgrade
   - Better meal progress visualization, item contribution charts, compact item cards, sorting, and richer nutrition breakdown.

9. Favorites Everywhere
   - Favorite foods, meals, recipes, templates, and quick logs. Make favorites one-tap loggable.

10. Custom Meals And Meal Times
    - Rename meals, add custom meal types, optional meal time, reorder meals.

11. Nutrition Goals And Diet Modes
    - Balanced, high protein, keto/low carb, muscle gain, weight loss, custom.
    - Let users override goals.

12. Net Carbs And Advanced Macros
    - Net carbs mode, fiber goal, sugar limit, saturated fat limit, sodium limit.

13. Micronutrient Foundation
    - Start with sodium, potassium, calcium, iron, vitamin D, vitamin C, magnesium.
    - Add model/storage/UI support without overwhelming the diary.

14. Recipes V2
    - Ingredients, serving units, cooked yield, per-serving nutrition, edit/delete/duplicate, log fractional servings.

15. Meal Templates V2
    - Editable templates, duplicate/favorite template, log to any meal/date.

16. Meal Planning
    - Future-day planning, copy day/meal, 7-day meal plan view, planned vs logged state.

17. Shopping List
    - Generate from planned meals/recipes, group ingredients, check off items, allow manual additions.

18. Water Tracking
    - Quick water buttons, custom amount, daily water goal, later Health Connect hydration sync.

19. Health Connect Nutrition Sync
    - Request permissions, write logged meals/hydration, handle unavailable Health Connect gracefully.

20. AI Logging Shell
    - Text, voice placeholder, photo placeholder.
    - Always show editable confirmation before saving.

21. Daily Food Insights
    - Deterministic coaching: protein low, fiber low, sodium high, balanced meal, what to add next.

22. Food/Meal/Day Rating
    - Lifesum-style rating with clear reason and suggested improvement.

23. UX Polish Pass
    - Spacing, typography, icons, empty/loading/error states, dark mode readiness, accessibility labels.

24. Food Performance And Reliability
    - Large database performance, search debounce, offline handling, Room indexes if needed, regression tests for core Food flows.

Recommended next goal:

```text
Implement Slice 3 and Slice 4 for the MusFit Android Food miniapp: complete the saved food editor and barcode no-match custom food flow. Keep the UX Lifesum-like, clean, and Android-only. Use existing Kotlin/Compose/Hilt/Room patterns. Write failing ViewModel tests first, implement minimally, then run testDebugUnitTest lintDebug assembleDebug. Commit and push when verified.
```
