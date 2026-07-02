# Profile Body & Progress Hub Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Profile tab as a body & progress hub per the approved spec `docs/superpowers/specs/2026-07-02-profile-body-hub-design.md`, including the target-weight ownership change that touches Today.

**Architecture:** Data-only migration 26→27 makes `user_profile.goalWeightKg` canonical (recency-aware carry-over from `user_goals.targetWeightKg`); Today's dashboard sheet drops its target-weight field and the coach reads the profile store. `ProfileViewModel` reworks its state into a weight hero (30-day `TrendLineChart`, goal badge, BMI caption), measurement sparkline tiles (three states), plan launcher cards, and an HC connect nudge; `EntriesSheet` gains a chart header; Account moves to Settings. No new repositories or repository read APIs.

**Tech Stack:** Kotlin, Compose M3, Room (manual migrations, `exportSchema`), Hilt, Coroutines/Flow, JUnit + Robolectric + hand-written fakes.

**Branch/worktree:** create a fresh worktree off current `master` (e.g. branch `claude/profile-body-hub`) before Task 1; all commits land there.

**Before any Gradle/adb command** (once per shell; chain into the same command — each PowerShell call is a fresh shell):

```powershell
. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat <targets> --no-daemon --console=plain
```

OneDrive recovery (environmental `AccessDeniedException`/`Cannot snapshot` under `app/build`): `.\gradlew.bat --stop; Start-Sleep -Seconds 3; Remove-Item -LiteralPath (Resolve-Path 'app\build').Path -Recurse -Force`, rerun (≤3 attempts).

---

## File structure

| File | Responsibility |
|---|---|
| Modify `app/src/main/java/com/musfit/core/di/DatabaseModule.kt` | `MIGRATION_26_27` (data-only carry-over) |
| Modify `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt` | `version = 27` |
| Modify `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt` | drop target-weight field wiring; coach reads profile goal weight |
| Modify `app/src/main/java/com/musfit/ui/today/MetricCarouselUi.kt` | `DashboardEditSheet` loses the target-weight field |
| Modify `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt` | hero/tiles/plans/nudge state rework; account editor moves out |
| Modify `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt` | body-hub layout; Identity/Vitals/Account cards removed |
| Modify `app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt` | `EntriesSheet` chart header; `LogMeasurementDialog.initialType` |
| Create `app/src/main/java/com/musfit/ui/profile/AccountUi.kt` | `AccountEditDialog` (moved from ProfileScreen) + Settings account section composable |
| Modify `app/src/main/java/com/musfit/ui/profile/ProfileSettingsViewModel.kt` | gains Account + Profile repositories, account-editor + profile-details state |
| Modify `app/src/main/java/com/musfit/ui/profile/ProfileSettingsScreen.kt` | Account section + "Profile details" row |
| Modify `app/src/main/java/com/musfit/ui/AppNavGraph.kt` | `ProfileScreen` gains `onOpenFood`/`onOpenTraining` |
| Commit `app/schemas/com.musfit.data.local.MusFitDatabase/27.json` | generated |
| Tests: create `GoalWeightMigration26To27Test.kt`; extend `TodayViewModelTest.kt`, `ProfileViewModelTest.kt`, `ProfileSettingsViewModelTest.kt`; chain-append the new migration in the 7 existing migration tests | |

Ground-truth facts used throughout (verified on `master` @ `ab7aa33`): `observeWeightSeries` returns entries **newest-first**; `FoodGoalMode.label` exists as an internal extension in `com.musfit.ui.food` (FoodScreen.kt:2317) and is accessible module-wide; `ProfileViewModel` currently has only an `@Inject` 4-arg constructor; `DEFAULT_USER_PROFILE` = (sex null, birth null, height null, ActivityLevel.Moderate, GoalType.Maintain, pace 0.5, goalWeight null); the Today carousel's weight-delta anchor is `(today.toEpochDay() − 7) * DAY_MILLIS`.

---

### Task 1: Migration 26→27 — target-weight carry-over

**Files:**
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`, `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: the 7 existing migration tests (chain-append `MIGRATION_26_27`)
- Test: `app/src/test/java/com/musfit/data/local/GoalWeightMigration26To27Test.kt` (new)

- [ ] **Step 1: Write the failing migration test.** Copy the four private schema-bootstrap helpers + companion from `CoachMigration25To26Test.kt` (per-class convention), `TEST_DATABASE_NAME = "goal-weight-26-27"`, bootstrap `version = 26`. Raw-SQL fixture helpers:

```kotlin
    private fun insertGoals(id: String, targetWeightKg: Double, updatedAt: Long) {
        db().execSQL(
            "INSERT INTO user_goals (id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis) " +
                "VALUES ('$id', 10000, 4, $targetWeightKg, $updatedAt)",
        )
    }

    private fun insertProfile(id: String, goalWeightKg: Double?, updatedAt: Long) {
        db().execSQL(
            "INSERT INTO user_profile (id, sex, birthDateEpochDay, heightCm, activityLevel, goalType, " +
                "goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis) " +
                "VALUES ('$id', NULL, NULL, NULL, 'Moderate', 'Maintain', 0.5, ${goalWeightKg ?: "NULL"}, $updatedAt)",
        )
    }

    private fun db(): SQLiteDatabase =
        SQLiteDatabase.openDatabase(context.getDatabasePath(TEST_DATABASE_NAME).path, null, SQLiteDatabase.OPEN_READWRITE)
```

(Close each opened db handle — wrap in `use`-style try/finally per the sibling tests; or open once per helper and close inside.) The four spec-mandated cases, each: bootstrap → insert fixtures → run `Room.databaseBuilder(...).addMigrations(DatabaseModule.MIGRATION_26_27)` open/close → re-open read-only → assert:

```kotlin
    @Test
    fun migration26To27_insertsProfileRowWhenMissing() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 75.0, updatedAt = 100L)

        runMigration()

        queryProfile("local-default") { goalWeight, activityLevel, goalType, pace ->
            assertEquals(75.0, goalWeight!!, 0.001)
            assertEquals("Moderate", activityLevel) // DEFAULT_USER_PROFILE values
            assertEquals("Maintain", goalType)
            assertEquals(0.5, pace, 0.001)
        }
    }

    @Test
    fun migration26To27_copiesIntoNullProfileGoal() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 75.0, updatedAt = 100L)
        insertProfile("local-default", goalWeightKg = null, updatedAt = 200L)

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(75.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_doesNotOverwriteNewerProfileValue() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 70.0, updatedAt = 100L)
        insertProfile("local-default", goalWeightKg = 80.0, updatedAt = 200L) // profile is newer

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(80.0, goalWeight!!, 0.001) }
    }

    @Test
    fun migration26To27_overwritesStaleProfileValueWithNewerGoals() {
        createDatabaseFromExportedSchema(version = 26)
        insertGoals("local-default", targetWeightKg = 70.0, updatedAt = 300L) // Today-set, newer
        insertProfile("local-default", goalWeightKg = 80.0, updatedAt = 200L)

        runMigration()

        queryProfile("local-default") { goalWeight, _, _, _ -> assertEquals(70.0, goalWeight!!, 0.001) }
    }
```

with small private helpers `runMigration()` (Room open/close with the single migration) and `queryProfile(id) { ... }` (raw SELECT of goalWeightKg, activityLevel, goalType, goalPaceKgPerWeek; `getDouble`/`isNull` handling).

- [ ] **Step 2: Run red** (`Unresolved reference: MIGRATION_26_27`):

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.GoalWeightMigration26To27Test" --no-daemon --console=plain
```

- [ ] **Step 3: Implement.** `MusFitDatabase.kt`: `version = 26` → `27` (no entity changes). `DatabaseModule.kt`: append after `MIGRATION_25_26` and register in `addMigrations(...)`:

```kotlin
    internal val MIGRATION_26_27 =
        object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Data-only: user_profile.goalWeightKg becomes the canonical target weight.
                // The retired runtime reads preferred user_goals.targetWeightKg when > 0, so
                // that value is carried over — recency-aware, and creating the profile row
                // when the user only ever set the target in Today's sheet.
                db.execSQL(
                    """
                    INSERT INTO user_profile (
                        id, sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                        goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                    )
                    SELECT g.id, NULL, NULL, NULL, 'Moderate', 'Maintain', 0.5,
                           g.targetWeightKg, g.updatedAtEpochMillis
                    FROM user_goals g
                    WHERE g.targetWeightKg > 0
                      AND NOT EXISTS (SELECT 1 FROM user_profile p WHERE p.id = g.id)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE user_profile SET goalWeightKg = (
                        SELECT g.targetWeightKg FROM user_goals g WHERE g.id = user_profile.id
                    )
                    WHERE EXISTS (
                        SELECT 1 FROM user_goals g
                        WHERE g.id = user_profile.id AND g.targetWeightKg > 0
                          AND (user_profile.goalWeightKg IS NULL OR user_profile.goalWeightKg <= 0
                               OR g.updatedAtEpochMillis > user_profile.updatedAtEpochMillis)
                    )
                    """.trimIndent(),
                )
            }
        }
```

('Moderate'/'Maintain'/0.5 are the `DEFAULT_USER_PROFILE` enum names/pace — verify against `ProfileRepository.kt` before committing.)

- [ ] **Step 4: Chain-append `DatabaseModule.MIGRATION_26_27`** in the `addMigrations` chains of the 7 existing migration tests (`AccountMigrationTest`, `AccountMigration20To21Test`, `TrainingExerciseDetailMigration21To22Test`, `TrainingRoutineProgramMigration22To23Test`, `TrainingSettingsMigration23To24Test`, `ExerciseMediaMigration24To25Test`, `CoachMigration25To26Test`) — the version bump breaks their to-latest opens otherwise (same mechanical edit every bump requires).

- [ ] **Step 5: Run green** (the new test class 4/4, then the full `com.musfit.data.local` package to prove the chain-appends). Confirm `app/schemas/com.musfit.data.local.MusFitDatabase/27.json` generated (only version/identityHash differ from 26).

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/musfit/data/local app/src/main/java/com/musfit/core/di/DatabaseModule.kt app/src/test/java/com/musfit/data/local app/schemas
git commit -m "feat(profile): migration 26->27 carries target weight into user_profile"
```

---

### Task 2: Today drops target weight; coach reads the profile store

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/today/MetricCarouselUi.kt`
- Modify: `app/src/main/java/com/musfit/ui/today/TodayScreen.kt` (DashboardEditSheet call site)
- Test: `app/src/test/java/com/musfit/ui/today/TodayViewModelTest.kt`

- [ ] **Step 1: Write/adjust the failing tests.**

(a) In `FakeHealthRepository` add a mutable weight series (needed to pin the coach's target-weight source — the weight rule only fires with a delta):

```kotlin
        val weightSeries = MutableStateFlow<List<BodyMetricEntity>>(emptyList())
        override fun observeWeightSeries(fromEpochMillis: Long): Flow<List<BodyMetricEntity>> = weightSeries
```

(new test-file imports: `com.musfit.data.local.entity.BodyMetricEntity`, `java.time.DayOfWeek`, `java.time.temporal.TemporalAdjusters` — the file currently imports only `LocalDate`/`ZoneId` from java.time).

(b) New test — the coach's target weight comes from the profile store (FakeProfileRepository already carries `goalWeightKg = 75.0`):

```kotlin
    @Test
    fun coachInput_targetWeightComesFromProfileStore() = runTest {
        val date = LocalDate.now()
        val coachRepository = FakeCoachRepository()
        val healthRepository = FakeHealthRepository(date)
        // Two flat weights spanning both of the coach's Monday-anchored 7-day windows
        // (one in [weekStart, weekStart+7d), one in [weekStart−7d, weekStart)) →
        // weightDelta ≈ 0 → the "steady" message interpolates the target:
        // "…keep nudging toward 75 kg." Anchoring to weekStart (not "now") keeps the
        // test deterministic on every weekday.
        val weekStartEpochDay = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        healthRepository.weightSeries.value = listOf(
            BodyMetricEntity("w1", "weight", 82.0, "kg", (weekStartEpochDay - 3L) * 86_400_000L, "manual", null),
            BodyMetricEntity("w2", "weight", 82.0, "kg", weekStartEpochDay * 86_400_000L, "manual", null),
        )
        val viewModel = todayViewModel(coachRepository = coachRepository, date = date, healthRepository = healthRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()

        val weightMessage = coachRepository.lastCandidates.first { it.ruleKey == "weight_trend" }
        assertTrue(weightMessage.body.contains("75"))
    }
```

Extend the `todayViewModel(...)` helper with `healthRepository: FakeHealthRepository? = null` (use it when non-null, else `FakeHealthRepository(date)`).

(c) Update `dashboardEditor_prefillsPinsAndGoalsWithProfileFallbackForTargetWeight`: rename to `dashboardEditor_prefillsPinsAndGoals`, delete the `targetWeightInput` assertion (the field is going away; keep the pins + `isDashboardEditorVisible` assertions and add `assertEquals("10000", state.stepGoalInput)`).

(d) Update `saveDashboard_blankTargetWeightKeepsStoredValue` → rename `saveDashboard_neverTouchesStoredTargetWeight`: drop the `onTargetWeightInputChanged` call (handler is going away); assert `goalsRepository.saved!!.targetWeightKg` still equals `FakeGoalsRepositoryRecording.STORED_TARGET_WEIGHT` after `openDashboardEditor()` + `saveDashboard()`.

- [ ] **Step 2: Run red** (compile failures on the fake + assertions).

- [ ] **Step 3: Implement in `TodayViewModel.kt`.**

- `TodayUiState`: remove `targetWeightInput`. Remove `onTargetWeightInputChanged`. Remove the `currentProfileGoalWeightKg` cache (it existed only for the prefill).
- `openDashboardEditor`: drop the `targetWeight` computation and the `targetWeightInput` copy.
- `saveDashboard`: `targetWeightKg = currentUserGoals.targetWeightKg` (pass-through — Today never modifies it again; keep the comment explaining why the field is preserved verbatim).
- Coach source change: in `coachInputFlow`, extend the training combine with the profile flow:

```kotlin
        val trainingGoalsProfile = combine(
            trainingRepository.observeWorkoutHistory(),
            trainingRepository.observeRoutineSummaries(),
            goalsRepository.observeUserGoals(),
            profileRepository.observeProfile(),
        ) { history, routines, userGoals, profile ->
            TrainingGoalsProfile(history, routines, userGoals, profile.goalWeightKg)
        }
```

with a small private holder at file bottom:

```kotlin
private data class TrainingGoalsProfile(
    val history: List<WorkoutHistorySummary>,
    val routines: List<RoutineSummary>,
    val userGoals: UserGoals,
    val profileGoalWeightKg: Double?,
)
```

The outer combine destructures it; `buildCoachInput` gains a `profileGoalWeightKg: Double?` parameter and sets `targetWeightKg = profileGoalWeightKg?.takeIf { it > 0.0 }` (replacing the `userGoals.targetWeightKg` read).

Also: `carouselFlow`'s `bodyTrainingProfile` combine currently carries a `profileRepository.observeProfile()` arm whose ONLY purpose was populating the removed `currentProfileGoalWeightKg` cache — **drop that arm** (the combine shrinks to measurements + history; the destructuring and `buildCarousel` call adjust accordingly). The coach's profile read now lives solely in `trainingGoalsProfile`.

- [ ] **Step 4: `MetricCarouselUi.kt`** — delete the "Target weight (kg)" `OutlinedTextField` from `DashboardEditSheet` and its `onTargetWeightChanged` parameter; update the call site in `TodayScreen.kt` (drop the `onTargetWeightChanged = viewModel::onTargetWeightInputChanged` argument).

- [ ] **Step 5: Run green** (`TodayViewModelTest` full class — expect 18: 17 current, renames net zero, +1 new; trust the class total the compiler produces, all green), then the **slice-1 gate** (full triple, required per spec):

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.TodayViewModelTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
git add app/src/main/java/com/musfit/ui/today app/src/test/java/com/musfit/ui/today
git commit -m "feat(today): target weight moves to Profile ownership; coach reads profile store"
```

---

### Task 3: ProfileViewModel — hero + tile state rework

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt` (mechanical read-site rewires — keeps this task's commit build-green)
- Test: extend `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`

- [ ] **Step 1: Add test seams + failing tests.**

`ProfileViewModel` gets a `TodayViewModel`-style internal primary constructor adding `clock: () -> Long` and `dateProvider: () -> LocalDate` — **mirror `TodayViewModel` exactly: NO defaults on the seams in the primary; the `@Inject` secondary takes the current 4 repos and delegates with the seams passed explicitly** (`dateProvider = { LocalDate.now() }, clock = { System.currentTimeMillis() }`) — this avoids the cyclic-delegation ambiguity that seam defaults can create, and all existing positional 4-arg constructions keep hitting the secondary. Existing `today`/`System.currentTimeMillis()` reads switch to the injected seams.

`FakeProfileRepository` gains richer fixtures: change `observeWeightSeries` to return a settable list (`var weightSeries: List<WeightEntry> = listOfNotNull(latestWeight)`), newest-first like the real DAO.

Add the construction helper NOW with its full four-repo defaulted signature (Task 6 appends two more params; Task 8 removes one — keep this shape so later tasks only touch what they introduce):

```kotlin
    private fun profileViewModel(
        accountRepository: AccountRepository = FakeAccountRepository(),
        profileRepository: ProfileRepository = FakeProfileRepository(),
        healthRepository: HealthRepository = FakeHealthRepo(),
        foodRepository: FoodRepository = FakeFoodGoalRepo(),
    ) = ProfileViewModel(
        accountRepository = accountRepository,
        profileRepository = profileRepository,
        healthRepository = healthRepository,
        foodRepository = foodRepository,
        dateProvider = { fixedDate },
        clock = { nowMillis },
    )
```

New tests use **UTC epoch-day-anchored timestamps** (the window math is epoch-day-based; local-zone times flake in far-west zones): `private val fixedDate = LocalDate.of(2026, 7, 2)`, `private val DAY = 86_400_000L`, `private val nowMillis = fixedDate.toEpochDay() * DAY + 14L * 3_600_000L`, `private fun daysAgo(n: Long) = (fixedDate.toEpochDay() - n) * DAY + 12L * 3_600_000L` (mid-day inside every window boundary). Test-file imports gain nothing time-zone-related (no ZoneId needed with this form).

```kotlin
    @Test
    fun hero_deltaUsesWeeklyAverages_andChartWindows30Days() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Lose, 0.5, 75.0),
        )
        repo.weightSeries = listOf(
            WeightEntry("w3", daysAgo(1), 82.0, "manual"),   // this week
            WeightEntry("w2", daysAgo(10), 83.0, "manual"),  // prior week
            WeightEntry("w1", daysAgo(45), 85.0, "manual"),  // outside the 30-day chart window
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(82.0, hero.latestWeightKg!!, 0.001)
        assertEquals(-1.0, hero.deltaKg!!, 0.001)              // avg(82) − avg(83)
        assertEquals(75.0, hero.goalWeightKg!!, 0.001)
        // progress baseline is the ALL-TIME first entry (85): (85−82)/(85−75) = 0.3
        assertEquals(0.3, hero.goalProgressFraction!!, 0.001)
        assertEquals(listOf(83.0, 82.0), hero.chartSeries)     // 30-day window, oldest→newest
        assertEquals(true, hero.hasAnyEntry)
    }

    @Test
    fun hero_singleEntryHasNoDeltaButKeepsFigure() = runTest {
        val repo = FakeProfileRepository()
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(82.0, hero.latestWeightKg!!, 0.001)
        assertNull(hero.deltaKg)
    }

    @Test
    fun hero_goalBadgeHiddenWithoutGoalWeight() = runTest {
        val repo = FakeProfileRepository() // DEFAULT_USER_PROFILE: goalWeightKg null
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.hero.goalWeightKg)
        assertNull(viewModel.state.value.hero.goalProgressFraction)
    }

    @Test
    fun hero_staleLoggerKeepsFigureWithEmptyChart() = runTest {
        val repo = FakeProfileRepository()
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(40), 84.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertEquals(84.0, hero.latestWeightKg!!, 0.001)   // all-time latest survives
        assertTrue(hero.chartSeries.isEmpty())              // 30-day window empty
        assertEquals(true, hero.hasAnyEntry)                // → "no entries in last 30 days", not first-log
    }

    @Test
    fun tiles_threeStatesFromAllTimeCountWithWindowedSparkline() = runTest {
        val repo = FakeProfileRepository(
            measurements = mapOf(
                "waist" to listOf( // 2 entries → sparkline
                    BodyMeasurement("m2", "waist", 88.0, "cm", daysAgo(1)),
                    BodyMeasurement("m1", "waist", 89.0, "cm", daysAgo(20)),
                ),
                "chest" to listOf( // 1 entry → no sparkline, delta null
                    BodyMeasurement("c1", "chest", 104.0, "cm", daysAgo(5)),
                ),
                "body_fat" to listOf( // 2 all-time but only 1 in the 90-day window → sparkline suppressed
                    BodyMeasurement("b2", "body_fat", 18.5, "%", daysAgo(10)),
                    BodyMeasurement("b1", "body_fat", 19.5, "%", daysAgo(120)),
                ),
            ),
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val tiles = viewModel.state.value.tiles.associateBy { it.type }
        assertEquals(listOf(89.0, 88.0), tiles["waist"]!!.sparkline)
        assertEquals(-1.0, tiles["waist"]!!.deltaFromPrevious!!, 0.001)
        assertEquals(2, tiles["waist"]!!.entryCount)
        assertNull(tiles["chest"]!!.deltaFromPrevious)
        assertTrue(tiles["chest"]!!.sparkline.size < 2)
        assertEquals(-1.0, tiles["body_fat"]!!.deltaFromPrevious!!, 0.001) // delta is all-time
        assertTrue(tiles["body_fat"]!!.sparkline.size < 2)                  // window has 1 point
        assertEquals(0, tiles["arms"]!!.entryCount)                         // never logged
    }
```

Plus four more small hero tests (spec test-list parity — badge percentless/capped, both chart-empty states, BMI gating):

```kotlin
    @Test
    fun hero_goalBadgePercentlessWhenStartEqualsGoal() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Lose, 0.5, 82.0),
        )
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual")) // start == goal → progress null
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(82.0, viewModel.state.value.hero.goalWeightKg!!, 0.001)
        assertNull(viewModel.state.value.hero.goalProgressFraction)
    }

    @Test
    fun hero_goalProgressCapsAtFull() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Lose, 0.5, 80.0),
        )
        repo.weightSeries = listOf(
            WeightEntry("w2", daysAgo(1), 78.0, "manual"),  // past the goal
            WeightEntry("w1", daysAgo(20), 85.0, "manual"),
        )
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1.0, viewModel.state.value.hero.goalProgressFraction!!, 0.001)
    }

    @Test
    fun hero_zeroEntriesEverShowsFirstLogState() = runTest {
        val repo = FakeProfileRepository()
        repo.weightSeries = emptyList()
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        val hero = viewModel.state.value.hero
        assertNull(hero.latestWeightKg)
        assertEquals(false, hero.hasAnyEntry)
    }

    @Test
    fun hero_bmiNullWithoutHeight() = runTest {
        val repo = FakeProfileRepository() // DEFAULT_USER_PROFILE: heightCm null
        repo.weightSeries = listOf(WeightEntry("w1", daysAgo(2), 82.0, "manual"))
        val viewModel = profileViewModel(profileRepository = repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.hero.bmi)
    }
```

Existing tests keep their positional 4-arg constructions (the `@Inject` secondary). Delete the old `bmi` assertion path only if the field moves (it moves into `hero.bmi` — update `completeProfile_exposesTargetsAndBmi` accordingly).

- [ ] **Step 2: Run red.**

- [ ] **Step 3: Implement.** New top-level state types:

```kotlin
data class WeightHeroState(
    val latestWeightKg: Double? = null,
    val deltaKg: Double? = null,
    val goalWeightKg: Double? = null,
    val goalProgressFraction: Double? = null,
    val bmi: Double? = null,
    val chartSeries: List<Double> = emptyList(),
    val hasAnyEntry: Boolean = false,
)

data class MeasurementTile(
    val type: String,
    val label: String,
    val value: Double?,
    val unit: String,
    val deltaFromPrevious: Double?,
    val sparkline: List<Double>,
    val entryCount: Int,
)
```

`ProfileUiState`: replace `latestWeightKg`/`bmi`/`bodyFatPercent`/`weightTrend`/`goalProgressFraction`/`weeklyWeightDeltaKg`/`measurements` with `hero: WeightHeroState = WeightHeroState()` and `tiles: List<MeasurementTile> = emptyList()`. Keep `weightEntries`/`measurementEntries` (sheets), `profile`, `ageYears`, `isProfileComplete`, `recommendedTargets`, `vitals` (removed in Task 7), account fields (moved in Task 8).

`dataState` mapping (same 4 source flows) — key formulas:

```kotlin
        val todayEpochDay = dateProvider().toEpochDay()
        val deltaAnchorMillis = (todayEpochDay - 7L) * DAY_MILLIS          // matches the Today carousel
        val chartFromMillis = (todayEpochDay - 30L) * DAY_MILLIS
        val sparkFromMillis = (todayEpochDay - 90L) * DAY_MILLIS
        val latest = weightSeries.firstOrNull()                              // newest-first
        val (_, delta) = WeeklyGoalsCalculator.weightTrend(
            weightSeries.map { it.measuredAtEpochMillis to it.weightKg },
            deltaAnchorMillis,
        )
        val goalWeight = profile.goalWeightKg?.takeIf { it > 0.0 }
        val start = weightSeries.lastOrNull()                                // all-time first entry
        val progress = if (start != null && latest != null && goalWeight != null) {
            BodyMetricsCalculator.goalProgressFraction(start.weightKg, latest.weightKg, goalWeight)
        } else null
        val hero = WeightHeroState(
            latestWeightKg = latest?.weightKg,
            deltaKg = delta,
            goalWeightKg = goalWeight,
            goalProgressFraction = progress?.coerceAtMost(1.0),
            bmi = if (latest != null && profile.heightCm != null) {
                BodyMetricsCalculator.bodyMassIndex(latest.weightKg, profile.heightCm)
            } else null,
            chartSeries = weightSeries.filter { it.measuredAtEpochMillis >= chartFromMillis }
                .map { it.weightKg }.reversed(),
            hasAnyEntry = weightSeries.isNotEmpty(),
        )
        val tiles = MEASUREMENT_TYPES.map { type ->
            val history = measurements[type].orEmpty()                        // newest-first
            MeasurementTile(
                type = type,
                label = MEASUREMENT_LABELS[type] ?: type,
                value = history.firstOrNull()?.value,
                unit = history.firstOrNull()?.unit ?: if (type == "body_fat") "%" else "cm",
                deltaFromPrevious = history.getOrNull(1)?.let { prev -> history.first().value - prev.value },
                sparkline = history.filter { it.measuredAtEpochMillis >= sparkFromMillis }
                    .map { it.value }.reversed(),
                entryCount = history.size,
            )
        }
```

(`private const val DAY_MILLIS = 86_400_000L` at file bottom; imports `WeeklyGoalsCalculator`.) **`MeasurementRow`/`measurements` stay for now** (still built exactly as today, alongside the new `tiles`) — `MeasurementsCard` keeps consuming them until Task 5 reworks the grid, and Task 5 deletes them. `isProfileComplete` keeps its current definition against `latest`.

- [ ] **Step 3b: Mechanical read-site rewires in `ProfileScreen.kt`** (this is what keeps the Task 3 commit build-green — every removed scalar has exactly one new home):

| Old read | New read | Sites |
|---|---|---|
| `state.latestWeightKg` | `state.hero.latestWeightKg` | `ProfileEditDialog(initialWeightKg=…)` (~:123), `LogWeightDialog(prefillKg=…)` (~:134) — **the spec's "prefilled from the latest logged weight" is preserved by these two rewires**, `IdentityCard` mini (~:293), `GoalCard` current→goal line (~:315,317), `WeightCard` (~:367,371) |
| `state.bmi` | `state.hero.bmi` | `IdentityCard` mini (~:294) |
| `state.bodyFatPercent` | `state.tiles.firstOrNull { it.type == "body_fat" }?.value` | `IdentityCard` mini (~:295) |
| `state.goalProgressFraction` | `state.hero.goalProgressFraction` | `GoalCard` progress bar (~:322-323) |
| `state.weeklyWeightDeltaKg` | `state.hero.deltaKg` | `WeightCard` delta line (~:375) |
| `state.weightTrend` | `state.hero.chartSeries` | `WeightCard` sparkline (~:384-385; the old `Sparkline` composable renders the 30-day series until Task 5 replaces it) |

No visual changes in this task — same composables, new field paths.

- [ ] **Step 4: Run green** (full `ProfileViewModelTest` — the run compiles the screen too), commit:

```powershell
git add app/src/main/java/com/musfit/ui/profile app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt
git commit -m "feat(profile): weight hero and measurement tile state with windowed charts"
```

---

### Task 4: `EntriesSheet` chart header + `LogMeasurementDialog.initialType`

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt`

Presentational-only; verification = compile (behavior pins live at the ViewModel layer and the existing sheet flows are unchanged).

- [ ] **Step 1:** `EntriesSheet` gains an optional chart slot: add parameter `chartSeries: List<Double> = emptyList()` (oldest→newest). Directly under the title `Text`, render:

```kotlin
            if (chartSeries.size >= 2) {
                TrendLineChart(
                    values = chartSeries,
                    accent = tabAccentFor(AppDestination.Profile).color,
                    modifier = Modifier.fillMaxWidth().height(96.dp).padding(vertical = 8.dp),
                )
            }
```

(imports: `TrendLineChart`, `tabAccentFor`, `AppDestination`, `height`. The `< 2` gate means deleting entries mid-sheet collapses the chart cleanly — same rule as the tiles.)

- [ ] **Step 2:** `LogMeasurementDialog` gains `initialType: String = MEASUREMENT_TYPES.first()` and seeds `var type by remember { mutableStateOf(initialType) }`.

- [ ] **Step 3:** compile (`.\gradlew.bat compileDebugKotlin --no-daemon --console=plain`), commit:

```powershell
git add app/src/main/java/com/musfit/ui/profile/ProfileEditContent.kt
git commit -m "feat(profile): chart header in EntriesSheet, preselectable measurement dialog"
```

---

### Task 5: ProfileScreen — body-hub layout (hero + tiles)

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Rework `WeightCard` into the hero.** Replace its body with (still `MusFitSummaryCard(accent, onClick = onOpenEntries)`):

```kotlin
@Composable
private fun WeightCard(state: ProfileUiState, accent: TabAccent, onLog: () -> Unit, onOpenEntries: () -> Unit) {
    val hero = state.hero
    MusFitSummaryCard(accent = accent, onClick = onOpenEntries) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Weight", style = MaterialTheme.typography.titleMedium, color = accent.onContainer, modifier = Modifier.weight(1f))
                hero.goalWeightKg?.let { goal ->
                    val percent = hero.goalProgressFraction?.let { " · ${(it * 100).roundToInt()}%" } ?: ""
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small) {
                        Text(
                            "goal ${goal.format1()} kg$percent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent.onContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            if (hero.latestWeightKg != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${hero.latestWeightKg.format1()} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent.onContainer)
                    Text(
                        hero.deltaKg?.let { d -> "${if (d < 0) "−" else "+"}${abs(d).format1()} kg · 7d" } ?: "latest",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.onContainer,
                    )
                }
                hero.bmi?.let { Text("BMI ${it.format1()}", style = MaterialTheme.typography.labelSmall, color = accent.onContainer) }
                when {
                    hero.chartSeries.size >= 2 ->
                        TrendLineChart(values = hero.chartSeries, accent = accent.color, modifier = Modifier.fillMaxWidth().height(72.dp))
                    hero.chartSeries.isEmpty() -> // entries exist (outer branch) but none in the window
                        Text("No entries in the last 30 days.", style = MaterialTheme.typography.bodySmall, color = accent.onContainer)
                    else -> // exactly one point in the window — a chart or "no entries" text would both mislead
                        Text("Log again to see a trend.", style = MaterialTheme.typography.bodySmall, color = accent.onContainer)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("30 days", style = MaterialTheme.typography.labelSmall, color = accent.onContainer, modifier = Modifier.weight(1f))
                    // Accent-tonal per spec: content colored with the accent ink on the tinted card.
                    TextButton(onClick = onLog) { Text("+ Log weight", color = accent.onContainer, fontWeight = FontWeight.SemiBold) }
                }
            } else {
                Text("No weight logged yet.", style = MaterialTheme.typography.bodyMedium, color = accent.onContainer)
                TextButton(onClick = onLog) { Text("+ Log weight") }
            }
        }
    }
}
```

(New imports: `TrendLineChart` from `com.musfit.ui.components.charts`, `Surface`, `kotlin.math.roundToInt`; note the Unicode minus `−` in the delta. The old `Sparkline` private composable and its Canvas imports are deleted.)

- [ ] **Step 2: Rework `MeasurementsCard` + `MeasurementCell`** to consume `state.tiles: List<MeasurementTile>` with the three states:

```kotlin
@Composable
private fun MeasurementCell(tile: MeasurementTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(tile.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tile.entryCount == 0) {
            Text("— · Tap to log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${tile.value!!.format1()} ${tile.unit}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                val delta = tile.deltaFromPrevious
                when {
                    delta == null -> Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // spec: 1-entry state shows value + "—" delta
                    delta != 0.0 -> Text(
                        "${if (delta < 0) "▼" else "▲"}${abs(delta).format1()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (tile.sparkline.size >= 2) {
                TrendLineChart(
                    values = tile.sparkline,
                    accent = tabAccentFor(AppDestination.Profile).color,
                    modifier = Modifier.fillMaxWidth().height(24.dp).padding(top = 4.dp),
                )
            }
        }
    }
}
```

`MeasurementsCard` iterates `state.tiles.chunked(2)` unchanged otherwise. Tap routing in `ProfileScreen`:

```kotlin
            MeasurementsCard(
                state = state,
                onLog = { showLogMeasurement = true },
                onOpenType = { type ->
                    val tile = state.tiles.first { it.type == type }
                    if (tile.entryCount == 0) logMeasurementInitialType = type else measurementSheetType = type
                },
            )
```

with `var logMeasurementInitialType by remember { mutableStateOf<String?>(null) }`; the existing `showLogMeasurement` flow stays for the section "+ Log", and a second `LogMeasurementDialog` instance renders when `logMeasurementInitialType != null`, passing `initialType = logMeasurementInitialType!!` (clear on dismiss/confirm).

- [ ] **Step 3: Wire the chart headers into the sheets.** Weight sheet: `chartSeries = state.weightEntries.map { it.weightKg }.reversed()` (full series — the sheet's chart is deliberately all-time). Measurement sheet: `chartSeries = rows.map { it.value }.reversed()`.

**With the grid now consuming `state.tiles`, delete the transitional `MeasurementRow` type, the `measurements` state field, its mapping, and the old `Sparkline` composable** (kept alive by Task 3 solely for build-greenness). Note: the spec's "never-logged preselect tap / stale-logger history routing" is pinned at the state layer via `entryCount` (Task 3 tests); the screen-side routing here is compile-verified and exercised in Task 9's emulator pass — recorded as the intended split.

- [ ] **Step 4: Compile + VM tests + slice-2 gate, commit:**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
git add app/src/main/java/com/musfit/ui/profile app/src/test/java/com/musfit/ui/profile
git commit -m "feat(profile): body-hub weight hero and sparkline measurement tiles"
```

---

### Task 6: Plan cards + HC nudge state

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`
- Test: extend `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`

- [ ] **Step 1: Failing tests.** `ProfileViewModel` gains `trainingRepository: TrainingRepository` + `goalsRepository: GoalsRepository` on BOTH constructors — **every existing construction site in the test class gains two fakes** (define a minimal `FakeTrainingRepo : TrainingRepository` overriding only the defaulted-interface members it needs — `observeRoutineSummaries`/`observeWorkoutHistory` as settable `MutableStateFlow`s, all abstract members as no-ops copied from `TodayViewModelTest`'s `FakeTrainingRepository` — and a `FakeGoalsRepo(var userGoals: UserGoals = UserGoals()) : GoalsRepository` with a settable flow). The `profileViewModel(...)` helper (four-repo shape from Task 3) **appends** `trainingRepository: TrainingRepository = FakeTrainingRepo()` and `goalsRepository: GoalsRepository = FakeGoalsRepo()` — `foodRepository` is already a helper param, so `profileViewModel(foodRepository = food)` below compiles.

```kotlin
    @Test
    fun planCards_dietAlwaysAndTrainingWithProgram() = runTest {
        val training = FakeTrainingRepo()
        training.routines.value = listOf(
            RoutineSummary(id = "r1", name = "Machine A", notes = null, exerciseCount = 5, targetSetCount = 15, isStarter = false, programName = "Beginner Program"),
        )
        training.history.value = listOf(
            WorkoutHistorySummary("s1", "Machine A", nowMillis - 86_400_000L, null, 12, 1000.0),
        )
        val goals = FakeGoalsRepo() // default weeklySessionTarget = 4
        val viewModel = profileViewModel(trainingRepository = training, goalsRepository = goals)
        dispatcher.scheduler.advanceUntilIdle()

        val cards = viewModel.state.value.planCards
        assertEquals("diet", cards[0].id)
        assertEquals("Balanced diet", cards[0].title)                    // FakeFoodGoalRepo mode = Balanced
        assertTrue(cards[0].subtitle.contains("2000 kcal target"))       // calorie figure for non-protein-led modes
        assertEquals("training", cards[1].id)
        assertEquals("Beginner Program", cards[1].title)
        assertTrue(cards[1].subtitle.contains("1 of 4 sessions this week"))
    }

    @Test
    fun planCards_proteinLedModeShowsProteinFigure() = runTest {
        val food = FakeFoodGoalRepo(
            initial = FoodGoal(
                2400.0, 180.0, 220.0, 70.0, 30.0, 50.0, 20.0, 2300.0,
                FoodGoalMode.HighProtein, includeTrainingCalories = false,
            ),
        )
        val viewModel = profileViewModel(foodRepository = food)
        dispatcher.scheduler.advanceUntilIdle()

        val diet = viewModel.state.value.planCards.first { it.id == "diet" }
        assertEquals("High protein diet", diet.title)
        assertTrue(diet.subtitle.contains("180 g protein target"))
    }

    @Test
    fun planCards_noRoutinesShowsSetupCard_andZeroTargetDropsDenominator() = runTest {
        val training = FakeTrainingRepo() // no routines, no history
        val goals = FakeGoalsRepo(userGoals = UserGoals(weeklySessionTarget = 0))
        val viewModel = profileViewModel(trainingRepository = training, goalsRepository = goals)
        dispatcher.scheduler.advanceUntilIdle()

        val card = viewModel.state.value.planCards.first { it.id == "training" }
        assertEquals("No program yet", card.title)
        assertEquals("Set one up in Training", card.subtitle)

        training.routines.value = listOf(
            RoutineSummary(id = "r1", name = "Machine A", notes = null, exerciseCount = 5, targetSetCount = 15, isStarter = false),
        )
        dispatcher.scheduler.advanceUntilIdle()
        val withRoutine = viewModel.state.value.planCards.first { it.id == "training" }
        assertEquals("Machine A", withRoutine.title)                     // no programName → routine name
        assertTrue(withRoutine.subtitle.contains("0 sessions this week"))
        assertEquals(false, withRoutine.subtitle.contains(" of "))       // target 0 drops the denominator
    }

    @Test
    fun hcNudge_visibleWhenNoPermissionsGranted() = runTest {
        val health = FakeHealthRepo() // status(): Available + granted setOf("steps") → hidden
        val viewModel = profileViewModel(healthRepository = health)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel.state.value.isHealthConnectNudgeVisible)

        val disconnected = FakeHealthRepoMutableStatus() // starts with no granted permissions
        val viewModel2 = profileViewModel(healthRepository = disconnected)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel2.state.value.isHealthConnectNudgeVisible)

        // Spec: "disappears once connected" — the user grants permissions in the HC app
        // and returns; the resume refresh must hide the nudge.
        disconnected.granted = setOf("steps")
        viewModel2.onScreenResumed()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, viewModel2.state.value.isHealthConnectNudgeVisible)
    }

    @Test
    fun planCards_allEightModesGetLabelAndFigureKind() = runTest {
        val proteinLed = setOf(FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain)
        FoodGoalMode.entries.forEach { mode ->
            val food = FakeFoodGoalRepo(
                initial = FoodGoal(
                    2100.0, 160.0, 220.0, 70.0, 30.0, 50.0, 20.0, 2300.0,
                    mode, includeTrainingCalories = false,
                ),
            )
            val viewModel = profileViewModel(foodRepository = food)
            dispatcher.scheduler.advanceUntilIdle()

            val diet = viewModel.state.value.planCards.first { it.id == "diet" }
            assertTrue("$mode title", diet.title.endsWith(" diet"))
            if (mode in proteinLed) {
                assertTrue("$mode figure", diet.subtitle.contains("160 g protein target"))
            } else {
                assertTrue("$mode figure", diet.subtitle.contains("2100 kcal target"))
            }
        }
    }
```

(`FakeHealthRepoMutableStatus` = copy of `FakeHealthRepo` with `var granted: Set<String> = emptySet()` and `status()` returning `HealthConnectStatus(HealthConnectAvailability.Available, granted)`. Verify `RoutineSummary`'s constructor against `TrainingRepository.kt:107` and adjust named args; `UserGoals` defaults per `GoalsRepository.kt`.)

**Recorded deviation (diet labels):** titles reuse Food's existing `FoodGoalMode.label` extension per the spec's "so the two surfaces agree" clause, so the exact strings are Food's — "High protein diet", "Keto low carb diet", "Muscle gain diet", "Weight loss diet", "Mediterranean-style diet", "Clean eating diet" — not the spec table's hyphenated variants. Agreement between surfaces beats the table's typography; the tests assert the reused strings.

- [ ] **Step 2: Run red, then implement.**

```kotlin
data class PlanCard(val id: String, val title: String, val subtitle: String)
```

`ProfileUiState` gains `planCards: List<PlanCard> = emptyList()` and `isHealthConnectNudgeVisible: Boolean = false`. A new flow merged into the outer `state` combine (arity: fold it into `dataState` or a second `combine` stage — the outer combine is at 5; add a nested `combine(planCardsFlow, nudgeFlow)` pair as a 6th source via restructuring into two stages):

```kotlin
    private val planCardsFlow: Flow<List<PlanCard>> = combine(
        foodRepository.observeFoodGoal(),
        trainingRepository.observeRoutineSummaries(),
        trainingRepository.observeWorkoutHistory(),
        goalsRepository.observeUserGoals(),
    ) { goal, routines, history, userGoals ->
        buildPlanCards(goal, routines, history, userGoals, dateProvider())
    }

    private val nudgeFlow = MutableStateFlow(false)

    /** Re-checked on screen resume — permissions are granted OUTSIDE the app, and the
     *  ViewModel survives Profile→Settings→Profile, so init-only would never hide it. */
    fun onScreenResumed() = refreshHealthConnectNudge()

    private fun refreshHealthConnectNudge() {
        viewModelScope.launch {
            runCatching { healthRepository.status() }.onSuccess { status ->
                nudgeFlow.value = status.availability != HealthConnectAvailability.Available ||
                    status.grantedPermissions.isEmpty()
            }
        }
    }
    // in init: refreshHealthConnectNudge()
```

(Task 7 wires `onScreenResumed` from the screen with the same `DisposableEffect`/`LifecycleEventObserver` pattern `TodayScreen` uses — ON_RESUME only; no pause action needed.)

Pure builder at file bottom (import `com.musfit.ui.food.label`, `countSessionsInWeek`, `TemporalAdjusters`, `DayOfWeek`):

```kotlin
private fun buildPlanCards(
    goal: FoodGoal,
    routines: List<RoutineSummary>,
    history: List<WorkoutHistorySummary>,
    userGoals: UserGoals,
    today: LocalDate,
): List<PlanCard> {
    val dietFigure = when (goal.mode) {
        FoodGoalMode.HighProtein, FoodGoalMode.MuscleGain -> "${goal.proteinGrams.roundToInt()} g protein target"
        else -> "${goal.dailyCaloriesKcal.roundToInt()} kcal target"
    }
    val diet = PlanCard(id = "diet", title = "${goal.mode.label} diet", subtitle = "$dietFigure · manage in Food")

    val weekStartMillis = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay() * DAY_MILLIS
    val sessions = countSessionsInWeek(history.map { it.startedAtEpochMillis }, weekStartMillis)
    val routine = routines.firstOrNull() // the coach's existing "next routine" convention
    val training = if (routine == null) {
        PlanCard(id = "training", title = "No program yet", subtitle = "Set one up in Training")
    } else {
        val target = userGoals.weeklySessionTarget
        val subtitle = if (target > 0) "$sessions of $target sessions this week" else "$sessions sessions this week"
        PlanCard(id = "training", title = routine.programName ?: routine.name, subtitle = subtitle)
    }
    return listOf(diet, training)
}
```

(The `mode.label` extension yields "Balanced"/"High protein"/… so titles read "Balanced diet", "High protein diet" — matching the test assertions above.)

**Concrete combine restructure** (the outer `state` combine is at kotlinx's 5-flow typed cap): pre-combine into two pairs so the outer combine stays at 5 —

```kotlin
    private val uiExtras = combine(messageFlow, accountEditorFlow) { message, editor -> message to editor }
    private val hubExtras = combine(planCardsFlow, nudgeFlow) { cards, nudge -> cards to nudge }

    val state: StateFlow<ProfileUiState> = combine(
        dataState,
        accountRepository.observeActiveAccount(),
        healthRepository.observeDailySummary(today),
        uiExtras,
        hubExtras,
    ) { base, account, summary, (message, editor), (cards, nudge) -> … }
```

Task 7 then simply deletes the `observeDailySummary` arm (vitals) leaving 4 sources; Task 8 moves `accountEditorFlow` out, at which point `uiExtras` collapses back to `messageFlow`. Encode this shape now — later tasks only shrink it.

- [ ] **Step 3: Run green** (full class), commit:

```powershell
git add app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt
git commit -m "feat(profile): plan launcher cards and Health Connect nudge state"
```

---

### Task 7: Screen sections, removals, and navigation callbacks

**Files:**
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt` (drop `vitals` state)
- Modify: `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt` (delete the vitals test)

- [ ] **Step 1: `AppNavGraph.kt`** — the Profile destination becomes:

```kotlin
            composable(AppDestination.Profile.route) {
                ProfileScreen(
                    onSettingsClick = { navController.navigate(PROFILE_SETTINGS_ROUTE) },
                    onOpenFood = { go(AppDestination.Food.route) },
                    onOpenTraining = { go(AppDestination.Training.route) },
                )
            }
```

- [ ] **Step 2: `ProfileScreen.kt` body rework.** Signature gains `onOpenFood: () -> Unit = {}` and `onOpenTraining: () -> Unit = {}`. New body order (inside the existing snackbar Scaffold/Column):

```kotlin
            MusFitScreenHeader(title = "Profile", actions = { /* settings gear unchanged */ })
            if (state.isHealthConnectNudgeVisible) {
                HealthConnectNudge(onOpen = onSettingsClick)
            }
            WeightCard(state = state, accent = accent, onLog = { showLogWeight = true }, onOpenEntries = { showWeightSheet = true })
            SectionHeader(title = "Measurements", trailingActionLabel = "+ Log", trailingActionColor = accent.color, onTrailingAction = { showLogMeasurement = true })
            MeasurementsGrid(state = state, onOpenType = { ... as Task 5 ... })
            SectionHeader(title = "Goal", trailingActionLabel = "Edit", trailingActionColor = accent.color, onTrailingAction = { showEditor = true })
            GoalCard(state = state, onApply = viewModel::applyTargetsToFood, onComplete = { showEditor = true })
            SectionHeader(title = "Plans")
            state.planCards.forEach { card ->
                PlanCardRow(card = card, onClick = { if (card.id == "diet") onOpenFood() else onOpenTraining() })
            }
            AccountCard(state = state, onEdit = viewModel::openAccountEditor) // last; moves to Settings in Task 8
```

Also wire the nudge refresh: a `DisposableEffect(lifecycleOwner)` + `LifecycleEventObserver` calling `viewModel.onScreenResumed()` on `ON_RESUME` (copy `TodayScreen`'s pattern; no pause branch).

- `SectionHeader` is the shared component (`com.musfit.ui.components.SectionHeader`). The in-card "Measurements"/"Log" header row and Goal's inline title are removed in favor of the section headers (`MeasurementsCard` → `MeasurementsGrid`, a plain tile grid without the Card wrapper title row; `GoalCard` drops its own "Goal" title `Text`).
- New composables:

```kotlin
@Composable
private fun HealthConnectNudge(onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = MaterialTheme.shapes.extraLarge) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Connect Health Connect to mirror steps and heart rate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("Set up", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PlanCardRow(card: PlanCard, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- **DELETE:** `IdentityCard`, `VitalsCard`, `MiniMetric`, `identitySubtitle` (+ their imports: `Person` icon, etc.). `AccountCard` + `AccountEditDialog` stay until Task 8. The `vitals` field and its `toVitals()` mapping leave `ProfileViewModel`/`ProfileUiState`; `VitalsSummary` type deleted; the `healthRepository.observeDailySummary(today)` arm drops out of the outer combine (freeing the arity slot Task 6 needed — note for the implementer: do Task 6's restructure with this in mind if executing sequentially).

- [ ] **Step 3: Compile + full Profile test class green + commit**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain
git add app/src/main/java/com/musfit/ui app/src/test/java/com/musfit/ui/profile
git commit -m "feat(profile): body-hub sections, plan cards, HC nudge; identity and vitals cards retired"
```

(Delete the `vitals_mapFromHealthConnectDailySummary` test with the state field — it is part of THIS commit so the tree stays bisectable.)

---

### Task 8: Account → Settings (+ "Profile details" row)

**Files:**
- Create: `app/src/main/java/com/musfit/ui/profile/AccountUi.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileSettingsViewModel.kt`, `ProfileSettingsScreen.kt`, `ProfileScreen.kt`, `ProfileViewModel.kt`
- Test: extend `app/src/test/java/com/musfit/ui/profile/ProfileSettingsViewModelTest.kt`; trim `ProfileViewModelTest.kt`

- [ ] **Step 1: Failing tests in `ProfileSettingsViewModelTest`.** Read the existing test file first; its tests construct `ProfileSettingsViewModel(...)` directly. The ViewModel gains `accountRepository: AccountRepository` and `profileRepository: ProfileRepository` — **create a `settingsViewModel(healthRepository = ..., accountRepository = FakeAccountRepository(), profileRepository = FakeProfileRepository())` helper with all three defaulted** and migrate the existing construction sites to it (copy `FakeAccountRepository` from `ProfileViewModelTest`; reuse its `FakeProfileRepository` shape). Port the three account tests (`accountState_exposesActiveLocalAccount`, `saveAccount_updatesRepositoryAndClosesEditor`, `saveAccount_blankNameKeepsEditorOpenWithValidation`) against the settings ViewModel, add an open/edit prefill test:

```kotlin
    @Test
    fun openAccountEditor_prefillsFromActiveAccountAndEditingClearsError() = runTest {
        val accountRepository = FakeAccountRepository(
            initial = Account(id = "account-1", displayName = "Ava", email = "ava@example.com", remoteUserId = null),
        )
        val viewModel = settingsViewModel(accountRepository = accountRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openAccountEditor()
        assertEquals("Ava", viewModel.state.value.accountNameInput)
        assertEquals("ava@example.com", viewModel.state.value.accountEmailInput)

        viewModel.onAccountNameChanged("")
        viewModel.saveAccount() // blank → error
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onAccountNameChanged("A") // editing clears the error
        assertEquals(null, viewModel.state.value.accountErrorMessage)
    }
```

plus:

```kotlin
    @Test
    fun profileDetails_exposesProfileAndLatestWeightForEditor() = runTest {
        val profileRepo = FakeProfileRepository(
            profile = UserProfile(Sex.Male, 0L, 180.0, ActivityLevel.Moderate, GoalType.Maintain, 0.0, 80.0),
            latestWeight = WeightEntry("w1", 1_000L, 80.0, "manual"),
        )
        val viewModel = settingsViewModel(profileRepository = profileRepo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(180.0, viewModel.state.value.profile.heightCm!!, 0.001)
        assertEquals(80.0, viewModel.state.value.latestWeightKg!!, 0.001)
    }
```

- [ ] **Step 2: Implement.**

- `ProfileSettingsViewModel`: constructor `(healthRepository, accountRepository, profileRepository)`. State gains `account: AccountUiState`, the four editor fields, `profile: UserProfile = DEFAULT_USER_PROFILE`, `latestWeightKg: Double? = null`. Restructure `state` as a combine of the existing `mutableState`, `accountRepository.observeActiveAccount()`, an `accountEditorFlow` (moved verbatim from `ProfileViewModel` with `openAccountEditor`/`closeAccountEditor`/`onAccountNameChanged`/`onAccountEmailChanged`/`saveAccount`), `profileRepository.observeProfile()`, and `profileRepository.observeLatestWeight()`; add `saveProfile(profile)`/`logWeight(kg)` delegating like `ProfileViewModel`'s.
- Move `AccountUiState` + the `Account.toUiState()` mapper + `accountInitial()` into `AccountUi.kt` together with `AccountEditDialog` (made non-private) and a new `AccountSection(account, onEdit)` composable (the old `AccountCard` content, minus the tab-card framing if desired — keep it a `Card`).
- `ProfileSettingsScreen`: above "Health Connect & sync" render `Text("Account", …titleMedium)` + `AccountSection(state.account, onEdit = viewModel::openAccountEditor)` + the dialog when `state.accountEditorOpen`; add a "Profile details" row (simple `TextButton` or clickable row) opening `ProfileEditDialog(initial = state.profile, initialWeightKg = state.latestWeightKg, onSave = { p, w -> viewModel.saveProfile(p); w?.let(viewModel::logWeight) ... })` behind a local `showProfileDialog` state.
- `ProfileViewModel`/`ProfileUiState`: remove the account editor state/actions + `account` field + the `ensureActiveAccount` init block moves to `ProfileSettingsViewModel`'s init; `ProfileScreen` drops `AccountCard` + the dialog block. `ProfileViewModel` keeps `accountRepository`? — after this, nothing in it uses accounts: **remove the dependency** (update all test constructions — the helper makes this one-line).
- `ProfileViewModelTest`: delete the three account tests (ported) and `FakeAccountRepository` usage from the helper.

- [ ] **Step 3: Run both Profile test classes green, then the slice-3 gate, commit**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --tests "com.musfit.ui.profile.ProfileSettingsViewModelTest" --no-daemon --console=plain
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
git add app/src/main/java/com/musfit/ui/profile app/src/test/java/com/musfit/ui/profile
git commit -m "feat(profile): account editing and profile details move to Settings"
```

(Note: removing `accountRepository` from `ProfileViewModel` updates the helper's default AND the remaining direct construction sites in `ProfileViewModelTest` — mechanical, compiler-guided.)

---

### Task 9: Polish, full verification, emulator pass

- [ ] **Step 1: Sweep.** Grep `app/src` for `IdentityCard|VitalsCard|MiniMetric|VitalsSummary|Sparkline|MeasurementRow|targetWeightInput|onTargetWeightInputChanged` → zero hits expected; remove stragglers/unused imports the compiler flags. Then grep `app/src/main` for `\.targetWeightKg` and verify the ONLY remaining hits are: `GoalsRepository.kt` (the entity mapping + `UserGoals` field), `TodayViewModel.saveDashboard`'s verbatim pass-through, and the migration SQL/tests — i.e. the spec's "retired from every read path" claim holds (no coach/carousel/editor read survives).
- [ ] **Step 2: Full gate** (required): `testDebugUnitTest lintDebug assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3: Emulator** (`.\scripts\android\install-seed-musfit.ps1 -Reset`; skip with DONE_WITH_CONCERNS after 2 failed attempts): screenshots via `adb shell screencap` + `adb pull` (never `>`) into a git-excluded `verification/` dir — Profile hub light + dark, a measurement history sheet with chart, the dashboard editor on Today (target-weight field gone), Settings with the Account section. Verify: hero chart renders, tiles' three states (seed data willing), plan cards navigate, HC nudge state matches the emulator's HC status, Today's editor has no target-weight field, coach weight message uses the profile goal.
- [ ] **Step 4: Observations** (report, don't fix): chart header sizing in sheets, tile sparkline legibility at 24dp, plan-card tap feedback, anything broken in dark mode.
- [ ] **Step 5: Commit** any polish as `chore(profile): final verification pass` (only if files changed).

---

## Verification summary (spec §Testing ↔ tasks)

| Spec requirement | Covered by |
|---|---|
| Migration 26→27 four cases + chain-append + 27.json | Task 1 |
| Today drops target weight; coach reads profile store (pinned) | Task 2 |
| Hero mapping (weightTrend delta, badge states, BMI, windowed chart, stale logger) | Task 3 |
| Tile three states + windowed sparklines + preselect routing | Tasks 3, 5 |
| EntriesSheet chart header (<2 gating) + LogMeasurementDialog.initialType | Tasks 4, 5 |
| Plan cards (8-mode labels via Food's `label`, figure rule, sessions n/m incl. 0) | Task 6 |
| HC nudge visibility | Tasks 6, 7 |
| Identity/Vitals/Account removals + Settings account/profile-details + nav callbacks | Tasks 7, 8 |
| Full gate + emulator light/dark | Task 9 |

