# Today Home — Slice 1 (Dashboard shell & re-skin) Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Replace the plain-text Today screen with a token-styled dashboard: header, three daily rings (Calories / Protein / Steps), a macro bar, and two glimpse tiles (Training today, Weight), with deep-link navigation into Food/Training/Health. No new storage (step goal is a temporary constant; weight trend + weekly tracker + coach come in later slices).

**Architecture:** `TodayViewModel` adds `FoodRepository.observeFoodGoal()` to its `combine`, derives ring/glimpse UI state, and exposes a richer `TodayUiState`. `TodayScreen` is rewritten on `MusFitTheme` tokens with small composables (`GoalRing`, `DailyRingsCard`, `MacroBar`, `GlimpseTile`). `AppNavGraph` passes tab-switch callbacks into `TodayScreen`.

**Tech Stack:** Jetpack Compose, Hilt, Kotlin Flows. Reuses `MusFitTheme`, existing repositories.

---

### Task 1: Expand `TodayUiState` + ViewModel (observe goal, derive rings) — TDD

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`
- Test: `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt`

- [ ] **Step 1 — new state shape.** Replace the flat fields with structured UI state:
```kotlin
enum class RingKind { Calories, Protein, Steps }

data class DailyRingUiState(
    val kind: RingKind,
    val centerLabel: String,   // "626", "25 g", "7.2k"
    val goalLabel: String,     // "of 2,083", "of 104 g", "of 10k"
    val progress: Float,       // 0f..1f
)

data class MacroBreakdownUiState(
    val carbsGrams: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
)

data class TrainingGlimpseUiState(
    val title: String = "No workout yet",
    val subtitle: String = "Tap to start",
    val hasWorkout: Boolean = false,
)

data class TodayUiState(
    val dateLabel: String = "",
    val rings: List<DailyRingUiState> = emptyList(),
    val macros: MacroBreakdownUiState = MacroBreakdownUiState(),
    val training: TrainingGlimpseUiState = TrainingGlimpseUiState(),
    val weightKg: Double? = null,
)
```
Add `private const val DEFAULT_STEP_GOAL = 10_000L` (replaced by `user_goals` in Slice 2).

- [ ] **Step 2 — observe the goal + derive state.** Add `foodRepository.observeFoodGoal()` as a 4th flow in `combine`, inject a `clock`/`dateProvider` already present, and map:
```kotlin
combine(
    foodRepository.observeDailyNutrition(date),
    foodRepository.observeFoodGoal(),
    trainingRepository.observeDailyTrainingSummary(date),
    healthRepository.observeDailySummary(date),
) { nutrition, goal, training, health ->
    toUiState(date, nutrition, goal, training, health)
}.collect { mutableState.value = it }
```
`toUiState` builds the three rings (`progress = (value / goalValue).coerceIn(0.0, 1.0).toFloat()`, steps goal = `DEFAULT_STEP_GOAL`), the macro breakdown, the training glimpse (`completedSetCount == 0` → default; else "N sets" / "{volume} kg volume", `hasWorkout = true`), and `weightKg = health?.latestWeightKg`. Add helpers `formatCount` ("7.2k") and reuse `formatMetric`.

- [ ] **Step 3 — update/extend the test.** Rewrite `TodayViewModelTest` assertions for the new shape: with the fake nutrition (600/45/70/18) and an overridden `observeFoodGoal()` returning a known `FoodGoal` (e.g. 2000 kcal, 150 g protein), assert `rings` contains a Calories ring with `progress == 0.3f` and `centerLabel == "600"`, a Protein ring `progress == 0.3f`, a Steps ring from 8200/10000 `progress == 0.82f`; assert `training.title == "2 sets"`-style and `weightKg == 82.4`. Override `observeFoodGoal()` in `FakeFoodRepository` to return the known goal.
- [ ] **Step 4 — run:** `.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.TodayViewModelTest" --no-daemon --console=plain` → PASS.

### Task 2: Dashboard composables + re-skin `TodayScreen`

**Files:** Modify `app/src/main/java/com/musfit/ui/today/TodayScreen.kt` (full rewrite)

- [ ] **Step 1 — `GoalRing`.** A `Canvas` donut (background `track`, foreground arc by `progress`, sweep `progress * 360f`, `StrokeCap.Round`, ~6dp stroke), with centered value text overlaid via a `Box`. Color by `RingKind`: Calories → `MusFitTheme.colors.brand`, Protein → `macroProtein`, Steps → `water`. Hoist the color out of the draw lambda (non-composable scope).
- [ ] **Step 2 — cards.** `DailyRingsCard` (a `surface` card, `Row` of three `GoalRing` + label/goalLabel, then `MacroBar`), `MacroBar` (a `Row` of three weighted bars colored `macroColors` with a "C 70 · P 25 · F 18" caption), and `GlimpseTile` (icon + value + label, `surface` card, clickable). Use `MusFitTheme.shapes`/`spacing`.
- [ ] **Step 3 — screen.** Rewrite `TodayScreen` body: background `MusFitTheme.colors.background`, scrollable `Column`; header ("Today" title + `dateLabel`); `DailyRingsCard`; a `Row` of two `GlimpseTile` (Training → `onOpenTraining`, Weight → `onOpenHealth`); rings/macro tap → `onOpenFood`. Replace all `MaterialTheme` text colors with tokens.

### Task 3: Navigation deep-links

**Files:** Modify `TodayScreen.kt`, `app/src/main/java/com/musfit/ui/AppNavGraph.kt`

- [ ] **Step 1.** Add params to `TodayScreen`: `onOpenFood: () -> Unit = {}`, `onOpenTraining: () -> Unit = {}`, `onOpenHealth: () -> Unit = {}`. Wire the cards/tiles to them.
- [ ] **Step 2.** In `AppNavGraph`, pass callbacks that navigate to the tab routes, e.g. `onOpenFood = { navController.navigate(AppDestination.Food.route) { popUpTo(AppDestination.Today.route); launchSingleTop = true } }` (same pattern as the bottom-nav items), and likewise Training/Health.

### Task 4: Verify + commit

- [ ] **Step 1.** `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` → green.
- [ ] **Step 2.** On-device: Today shows rings + tiles in the B1 palette; tapping a tile switches tabs.
- [ ] **Step 3.** Commit (`feat(today): dashboard shell with daily rings and glimpse tiles`).

## Notes
- Step goal is a constant this slice; the Weight tile shows latest only (trend arrow + weekly tracker arrive in Slice 2). No DB changes here.
