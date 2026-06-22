# Today Home — Slices 2 & 3 (Goals/Weekly tracker + Coach) Plan

> **For agentic workers:** Use superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** Slice 2 adds the `user_goals` storage (step goal, weekly-session target, target weight; migration **v17**) + a goals editor + the weekly-goals tracker (Sessions, Calories-on-target, Weight→target, Step-goal days). Slice 3 adds the on-device coach engine (nutrition pacing / training nudges / trends) + the briefing card.

**Architecture:** A new single-row `user_goals` table behind a small `GoalsRepository` (so saving food goals can't clobber it). New `HealthDao` weekly range query + `HealthRepository` methods. Pure `domain/today/WeeklyGoalsCalculator` and `domain/coach/CoachEngine`, both unit-tested. `TodayViewModel` adds a weekly collector + builds a `CoachInput`. New Compose cards: `WeeklyGoalsCard`, `TodayGoalsEditorSheet`, `CoachBriefingCard`.

---

## SLICE 2 — Goals storage + weekly tracker

### Task 2.1 — `user_goals` table + DAO + migration v17 (Robolectric test)
**Files:** new `data/local/entity/UserGoalsEntity.kt`, new `data/local/dao/UserGoalsDao.kt`, `data/local/MusFitDatabase.kt`, `core/di/DatabaseModule.kt`, new `app/schemas/.../17.json` (generated), test `data/local/UserGoalsDaoTest.kt`.
- [ ] Entity (mirror `FoodGoalEntity` single-row pattern):
```kotlin
@Entity(tableName = "user_goals")
data class UserGoalsEntity(
    @PrimaryKey val id: String,                 // "default"
    val stepGoal: Long,
    val weeklySessionTarget: Int,
    val targetWeightKg: Double,                  // 0.0 = unset
    val updatedAtEpochMillis: Long,
)
```
- [ ] DAO: `observeUserGoals(id): Flow<UserGoalsEntity?>`, `suspend getUserGoals(id)`, `@Insert(REPLACE) suspend upsertUserGoals(entity)`.
- [ ] `MusFitDatabase`: add `UserGoalsEntity::class` to `entities`, `abstract fun userGoalsDao(): UserGoalsDao`, bump `version = 17`.
- [ ] `DatabaseModule`: `MIGRATION_16_17 = CREATE TABLE IF NOT EXISTS user_goals (id TEXT NOT NULL PRIMARY KEY, stepGoal INTEGER NOT NULL, weeklySessionTarget INTEGER NOT NULL, targetWeightKg REAL NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)`; register in `addMigrations(...)`; add `@Provides fun provideUserGoalsDao(db) = db.userGoalsDao()`.
- [ ] Robolectric test: in-memory DB, upsert then observe returns the row.
- [ ] Build once to generate `17.json`; commit it.

### Task 2.2 — `GoalsRepository` (interface + Local + bind)
**Files:** new `data/repository/GoalsRepository.kt`, `core/di/RepositoryModule.kt`.
- [ ] `data class UserGoals(val stepGoal: Long = 10_000, val weeklySessionTarget: Int = 4, val targetWeightKg: Double = 0.0)`.
- [ ] `interface GoalsRepository { fun observeUserGoals(): Flow<UserGoals>; suspend fun updateUserGoals(goals: UserGoals) }` + `class LocalGoalsRepository @Inject constructor(dao)` mapping entity↔model with id `"default"` and the defaults when absent.
- [ ] `RepositoryModule`: `@Binds @Singleton abstract fun bindGoalsRepository(impl: LocalGoalsRepository): GoalsRepository`.

### Task 2.3 — Health weekly range query
**Files:** `data/local/dao/HealthDao.kt`, `data/repository/HealthRepository.kt` (interface + Local).
- [ ] `HealthDao`: `@Query("SELECT * FROM daily_health_summaries WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay") fun observeDailySummariesInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyHealthSummaryEntity>>`.
- [ ] `HealthRepository`: add `fun observeDailySummaries(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyHealthSummaryEntity>>` and `fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>>` (wraps `observeBodyMetrics("weight", from)`). Implement in Local. (Update the Today + any test fakes that implement `HealthRepository`.)

### Task 2.4 — `WeeklyGoalsCalculator` (pure, TDD)
**Files:** new `domain/today/WeeklyGoalsCalculator.kt`, test `domain/today/WeeklyGoalsCalculatorTest.kt`.
- [ ] Model:
```kotlin
data class WeeklyGoals(
    val sessionsDone: Int, val sessionTarget: Int,
    val calorieOnTargetDays: Int, val trackedDays: Int,        // trackedDays = 7
    val stepGoalDays: Int,
    val weightAvgKg: Double?, val weightDeltaKg: Double?, val targetWeightKg: Double?,
)
```
- [ ] `object WeeklyGoalsCalculator { fun compute(weekStart, nowMillis, planDays, history, dailySummaries, weightSeries, calorieGoalKcal, userGoals): WeeklyGoals }`:
  - sessionsDone = history.count { startedAtEpochMillis in [weekStartMillis, weekStartMillis+7d) }.
  - calorieOnTargetDays = planDays.count { it.loggedEntryCount > 0 && it.loggedTotals.caloriesKcal in [0.85*goal, 1.15*goal] }.
  - stepGoalDays = dailySummaries.count { (it.steps ?: 0) >= userGoals.stepGoal }.
  - weight: split weightSeries into this-week vs prior-week by `measuredAtEpochMillis`; `weightAvgKg` = avg this week (or null), `weightDeltaKg` = thisAvg − priorAvg (or null), `targetWeightKg` = userGoals.targetWeightKg.takeIf { it > 0 }.
- [ ] Tests: feed crafted inputs; assert each counter + the weight avg/delta and null-handling (no weights → nulls).

### Task 2.5 — ViewModel weekly state + step goal from storage
**Files:** `ui/today/TodayViewModel.kt`, `TodayViewModelTest.kt`.
- [ ] Inject `GoalsRepository` (4th repo). Add `userGoals` to the daily combine so the Steps ring uses `userGoals.stepGoal` (replace `DEFAULT_STEP_GOAL`).
- [ ] Add a second collector: `combine` of `observeFoodPlan(weekStart)`, `observeWorkoutHistory()`, `observeDailySummaries(weekStart, weekEnd)`, `observeWeightSeries(now-14d)`, `observeUserGoals()`, `observeFoodGoal()` → `WeeklyGoalsCalculator.compute(...)` → update `state.weekly`. (Nest combines to stay ≤5 per call.)
- [ ] Add `weekly: WeeklyGoalsUiState?` to `TodayUiState` (mapped from `WeeklyGoals`).
- [ ] Test: with fakes returning crafted history/plan/summaries/weights + `FakeGoalsRepository`, assert `state.weekly` counters. Update the two `TodayViewModel(...)` constructions + add `FakeGoalsRepository`.

### Task 2.6 — Weekly card + goals editor UI
**Files:** `ui/today/TodayScreen.kt`, `TodayViewModel.kt`.
- [ ] `WeeklyGoalsCard`: 2×2 mini trackers (Sessions n/target + dots, Calories on target n/7, Weight avg + delta→target, Step-goal days n/7) using tokens.
- [ ] Header gear → opens a `ModalBottomSheet` `TodayGoalsEditorSheet` with three `OutlinedTextField`s (step goal, weekly sessions, target weight) + Save → `viewModel.saveUserGoals(...)`. VM holds editor input state + `saveUserGoals` calling `goalsRepository.updateUserGoals`.

### Task 2.7 — Verify + commit
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green. Commit (`feat(today): user goals storage (v17), editor, and weekly tracker`).

---

## SLICE 3 — On-device coach

### Task 3.1 — `CoachEngine` (pure, TDD)
**Files:** new `domain/coach/CoachEngine.kt`, test `domain/coach/CoachEngineTest.kt`.
- [ ] Models:
```kotlin
enum class CoachCategory { NutritionPacing, TrainingNudge, Trend }
sealed interface CoachAction { object OpenFood: CoachAction; object OpenTraining: CoachAction; object OpenHealth: CoachAction; data class StartRoutine(val routineId: String): CoachAction }
data class CoachCue(val id: String, val category: CoachCategory, val priority: Int, val text: String, val action: CoachAction?)
data class CoachInput(
    val timeOfDay: TimeOfDay, val firstName: String?,
    val caloriesKcal: Double, val calorieGoalKcal: Double,
    val proteinGrams: Double, val proteinGoalGrams: Double,
    val daysSinceLastWorkout: Int?, val nextRoutineName: String?, val nextRoutineId: String?,
    val weightDeltaKg: Double?, val targetWeightKg: Double?,
    val stepsToday: Long, val stepGoal: Long,
)
enum class TimeOfDay { Morning, Afternoon, Evening }
data class CoachBriefing(val greeting: String, val cues: List<CoachCue>)
```
- [ ] `object CoachEngine { fun briefing(input): CoachBriefing }`: greeting from `timeOfDay` + `firstName`; cue rules (each emits a `CoachCue?` with a priority): protein gap (NutritionPacing), calories remaining + rough split, days-since-workout / next routine (TrainingNudge), weight trend vs target (Trend), steps behind. Sort cues by priority desc; drop nulls.
- [ ] Tests: protein-gap input → expected NutritionPacing cue + action OpenFood; 3-days-since-workout → TrainingNudge with `StartRoutine`; flat/over-target weight → Trend cue; empty/fresh (zeros) → a sensible welcome cue; assert ordering.

### Task 3.2 — ViewModel coach wiring
**Files:** `ui/today/TodayViewModel.kt`, `TodayViewModelTest.kt`.
- [ ] Inject a `clock: () -> Long` (default `System::currentTimeMillis`) for `TimeOfDay`. Build `CoachInput` from the combined daily+weekly data (+ `observeWorkoutHistory` for days-since, `observeRoutineSummaries` for next routine) and run `CoachEngine.briefing`. Map to `state.coach: CoachBriefingUiState`.
- [ ] Test: crafted snapshot → assert greeting + first cue text/category.

### Task 3.3 — `CoachBriefingCard` UI
**Files:** `ui/today/TodayScreen.kt`.
- [ ] Emerald hero card at the top: greeting, top 1–2 cue lines (leading icon), action chips mapped to nav (`OpenFood`→onOpenFood, `OpenTraining`/`StartRoutine`→onOpenTraining, etc.), and a `n / total` pager (local state) to step through cues.

### Task 3.4 — Verify + commit + deploy
- [ ] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green. Commit (`feat(today): on-device coach briefing`).
- [ ] Clean-install on device (v17 wipes the test DB), screenshot Today with coach + rings + weekly.

## Risks / notes
- Week boundary = Monday start (`date.with(DayOfWeek.MONDAY)`); cover in calculator tests.
- v17 → clean install on the dev phone (data wipe), as with v15→v16.
- Keep coach to top 1–2 cues + pager to avoid noise.
