# Training Hevy Parity Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the MusFit Training tab to practical local-first Hevy-style parity, one scoped slice at a time.

**Architecture:** Training stays Android-only, local-first, and built on the existing Compose screen, Hilt ViewModel, Room repository, Flow, and pure domain calculator boundaries. Existing Training features must be verified before any new behavior is added so implemented surfaces are not rebuilt.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt ViewModels, Room, Kotlin Flow/coroutines, local domain calculators.

## Global Constraints

- Do not copy Hevy assets, proprietary scoring, names, or exact layouts.
- No accounts, cloud sync, analytics, subscriptions, social feed, followers, public profiles, or community features.
- Keep Training focused on strength training unless a narrow local cardio/duration field already exists and can be supported safely.
- Keep Today and Health Connect export compatible with completed workouts.
- Any Room schema change must bump the database version, add/register a migration in `DatabaseModule.kt`, and commit the new schema JSON under `app/schemas`.
- Do not push to origin unless explicitly instructed.

---

## Slice Queue

1. Slice 0: Training Parity Audit And Ledger
2. Slice 1: Training Architecture And Docs Sync
3. Slice 2: Exercise Library Detail
4. Slice 3: Routine Organization And Program Library
5. Slice 4: Active Workout Logger Completion
6. Slice 5: Rest, Warm-Up, And Plate Tools
7. Slice 6: Superset And Grouping Polish
8. Slice 7: Workout History Calendar And Consistency
9. Slice 8: Progress Analytics
10. Slice 9: Training Dashboard Polish
11. Slice 10: Finish Flow And Workout Recap
12. Slice 11: Performance, Reliability, And Closeout

## Current Status Per Slice

| Slice | Status | Audit notes | Files touched | Tests added/changed | Verification |
| --- | --- | --- | --- | --- | --- |
| 0. Training Parity Audit And Ledger | Complete | Ledger created from current docs, previous Training plans, current Training source/tests, and passing focused baseline Training tests. | `docs/superpowers/plans/2026-06-28-training-hevy-parity-loop.md` | None | Passed: four focused Training baseline commands on 2026-06-28 |
| 1. Training Architecture And Docs Sync | Complete | Synced Training docs for implemented supersets, rest timer controls, plate hints, PR badges, active route finish/discard polish, repository/grouping models, and current schema limitations. | `docs/architecture/screen-contracts.md`, `docs/architecture/data-models.md`, `docs/architecture/README.md`, `docs/superpowers/plans/2026-06-28-training-hevy-parity-loop.md` | None | Docs-only; focused Training baseline tests passed before docs sync |
| 2. Exercise Library Detail | Complete | Added reachable inline exercise detail cards with equipment/category, primary/secondary muscles, original starter instructions, editable local notes, and richer local search/filter coverage. Added DB v22 detail columns and migration. | `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`, `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`, `app/src/main/java/com/musfit/data/local/entity/TrainingEntities.kt`, `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`, `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/data/repository/TrainingStarterData.kt`, `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/schemas/com.musfit.data.local.MusFitDatabase/22.json`, docs | Added repository detail/local-notes tests, ViewModel detail/filter tests, and `TrainingExerciseDetailMigration21To22Test` | Passed focused repository, ViewModel, and migration tests on 2026-06-28 |
| 3. Routine Organization And Program Library | Complete | Added routine `programName` plus tags, program filter chips in Routines, local starter program catalog covering Full Body, Push Pull Legs, Upper Lower, Strength, Hypertrophy, and Beginner, and duplicate preservation of program metadata. Added DB v23 migration. | `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`, `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`, `app/src/main/java/com/musfit/data/local/entity/TrainingEntities.kt`, `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`, `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/data/repository/TrainingStarterData.kt`, `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`, `app/schemas/com.musfit.data.local.MusFitDatabase/23.json`, docs | Added repository program catalog/duplicate test, ViewModel program filter test, and `TrainingRoutineProgramMigration22To23Test` | Passed focused repository, ViewModel, and migration tests on 2026-06-28 |
| 4. Active Workout Logger Completion | Complete | Added active workout session notes, persisted note trimming/nulling, set up/down reorder by active-session sort order, compact set-type menu for warmup/working/drop/failure, and D/F display labels while preserving add/edit/duplicate/delete/complete, rest timer, PR, plate, and superset behavior. Empty active route placeholder remains in place for stale route state. | `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`, `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`, `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`, `app/src/test/java/com/musfit/ui/training/TrainingActiveWorkoutContentTest.kt`, docs | Added repository active-notes/reorder test, ViewModel notes/reorder delegation test, and active set formatter drop/failure label test | Passed focused repository, ViewModel, active content, and calculator tests on 2026-06-28 |
| 5. Rest, Warm-Up, And Plate Tools | Complete | Added persisted global Training settings for default rest seconds, bar weight, and available plates; active Training tools card; deterministic warm-up suggestions from the latest work set; add-suggested-warmup action; configurable plate hints; and DB v24 migration/schema. Settings remain global rather than per-exercise. | `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`, `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`, `app/src/main/java/com/musfit/data/local/entity/TrainingEntities.kt`, `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`, `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/domain/training/WarmupSetCalculator.kt`, `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`, `app/schemas/com.musfit.data.local.MusFitDatabase/24.json`, docs | Added repository settings persistence test, ViewModel settings/rest/warmup tests, active plate-line test, `WarmupSetCalculatorTest`, and `TrainingSettingsMigration23To24Test` | Passed focused repository, ViewModel, active content, warmup calculator, plate calculator, and migration tests on 2026-06-28 |
| 6. Superset And Grouping Polish | Complete | Verified repository, ViewModel, and active workout support for create/dissolve/grouping labels, inheritance, auto-dissolve, and contiguous member reindexing. Added completed-workout history detail groupings so supersets stay grouped after finish, plus grouped history display with A/B labels and flat fallback behavior. Supersets remain pair-oriented through "make with next." | `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`, `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`, `app/src/test/java/com/musfit/ui/training/TrainingHistoryContentTest.kt`, docs | Added repository history-detail superset preservation test and history display grouping/fallback tests | Passed focused repository, ViewModel, active content, history content, and calculator tests on 2026-06-28 |
| 7. Workout History Calendar And Consistency | Complete | Added derived `TrainingHistoryOverview` state with current-month calendar weeks, current-week workouts, distinct training days, completed sets, volume, current streak, and best streak. History screen now shows a compact month grid and consistency summary while preserving list/detail and superset detail behavior. Overview remains current-month/current-week focused rather than a drillable multi-month calendar. | `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`, `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`, docs | Added overview builder/state tests for week totals, calendar aggregation, and streaks | Passed focused ViewModel, repository, active content, history content, and calculator tests on 2026-06-28 |
| 8. Progress Analytics | Complete | Added derived all-training analytics for muscle-group volume/set counts and weekly volume; extended selected exercise progress with per-day history rows, best daily set summaries, and estimated-1RM PR timeline; Progress tab now renders analytics overview and selected-exercise deep dive. Analytics are local/deterministic and not cached. | `app/src/main/java/com/musfit/domain/model/TrainingModels.kt`, `app/src/main/java/com/musfit/domain/training/WorkoutCalculator.kt`, `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`, `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/src/main/java/com/musfit/ui/training/TrainingProgressContent.kt`, tests, docs | Added domain progress history/best-set/PR timeline assertions, repository muscle/weekly analytics test, and ViewModel analytics-state test | Passed focused domain, repository, ViewModel, active content, and history content tests on 2026-06-28 |
| 9. Training Dashboard Polish | Complete | Added derived dashboard state and a top dashboard card with next visible routine, quick-start routines, blank workout start, and recent completed workout while preserving active workout resume and weekly header. Next routine is a deterministic visible-routine heuristic, not adaptive progression. | `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`, `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`, `app/src/test/java/com/musfit/ui/training/TrainingViewModelTest.kt`, docs | Added ViewModel dashboard state test for routine suggestion, quick starts, recent workout, and program filter coherence | Passed focused ViewModel, repository, active content, history content, and calculator tests on 2026-06-28 |
| 10. Finish Flow And Workout Recap | Complete | Finish/discard confirmations already existed. Added completed-workout recap data and History detail recap card with duration, exercises, completed sets, volume, PR count, and session notes; finishing a workout already opens that detail route. No social sharing/feed was added. | `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`, `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`, `app/src/test/java/com/musfit/data/repository/LocalTrainingRepositoryTest.kt`, `app/src/test/java/com/musfit/ui/training/TrainingHistoryContentTest.kt`, docs | Added repository recap derivation test and history recap metric formatting test | Passed focused repository/history tests and focused Training regression on 2026-06-28 |
| 11. Performance, Reliability, And Closeout | Complete | Audited DAO query shape, active route stale-state fallback, history/progress empty states, migration risk, and full verification. Tightened history recap fallback for older/fake details and replaced completed-detail summary lookup with a direct DAO query. No schema change was made. | `app/src/main/java/com/musfit/data/local/dao/TrainingDao.kt`, `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`, `app/src/test/java/com/musfit/ui/training/TrainingHistoryContentTest.kt`, `docs/superpowers/plans/2026-06-28-training-hevy-parity-loop.md` | Added history recap fallback metric test | Passed focused Training regression and full `testDebugUnitTest lintDebug assembleDebug` on 2026-06-28 |

## Audit Evidence

Current Training implementation:

- UI: `TrainingScreen.kt`, `TrainingRoutineContent.kt`, `TrainingActiveWorkoutContent.kt`, `TrainingHistoryContent.kt`, `TrainingProgressContent.kt`, `ExerciseTrendChart.kt`.
- ViewModel: `TrainingViewModel.kt` exposes `TrainingUiState`, section routing, routine editor state, exercise filters/editor, active workout route state, rest timer state, finish/discard confirmations, and action handlers.
- Repository/data: `TrainingRepository.kt`, `TrainingDao.kt`, `TrainingEntities.kt`, and `TrainingStarterData.kt`.
- Domain calculators: `WorkoutCalculator.kt`, `PlateCalculator.kt`, `WarmupSetCalculator.kt`, `TrendChartScaler.kt`.
- Tests: `TrainingViewModelTest.kt`, `TrainingHomeContentTest.kt`, `TrainingActiveWorkoutContentTest.kt`, `LocalTrainingRepositoryTest.kt`, `WorkoutCalculatorTest.kt`, `WarmupSetCalculatorTest.kt`, `PlateCalculatorTest.kt`, `TrendChartScalerTest.kt`, and `ExerciseTrendChartLogicTest.kt`.

Existing implemented parity foundations:

- Starter exercises and starter routines are seeded locally.
- Custom exercises can be created and searched.
- Exercise library search and muscle/equipment filters exist.
- Routine create/edit/duplicate/delete/start and exercise reordering exist.
- Active workouts can be started blank or from routines.
- Active workout set logging supports edit, duplicate, delete, completion, RPE, notes, previous labels, warmup/working/drop/failure labels, rest timer settings, warm-up suggestions, PR badge logic, configurable plate hints, and supersets.
- Completed workout history overview, month grid, consistency metrics, list, recap, and detail exist, including grouped superset display after finish.
- Exercise progress PR metrics, trend chart, history rows, best sets, PR timeline, muscle group analytics, and weekly volume analytics exist.
- Completed workouts feed Today and Health Connect export boundaries.

Known gaps from Slice 0 audit:

- Training architecture docs are stale compared with implemented active workout, supersets, PR, plate, rest timer, and finish/discard behavior.
- Exercise library lacks a true detail/drill-down surface and richer original MusFit exercise metadata.
- Routine organization is limited to a flat routine list and starter routines.
- Active logger does not expose drop/failure set type choices or set reorder.
- Finish flow opens the completed workout in History with a local recap surface.

## Verification Log

| Date | Scope | Command | Result |
| --- | --- | --- | --- |
| 2026-06-28 | Baseline focused Training tests | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Baseline focused Training tests | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Baseline focused Training tests | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Baseline focused Training tests | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 1 docs sync | Reviewed `screen-contracts.md`, `data-models.md`, and `README.md` against Training repository/domain signatures. | Passed; docs-only, no Gradle rerun |
| 2026-06-28 | Slice 2 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Failed as expected before implementation: missing exercise detail APIs and migration |
| 2026-06-28 | Slice 2 repository | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 2 ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 2 migration | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.TrainingExerciseDetailMigration21To22Test" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 2 shared focused check | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 2 active UI focused check | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 3 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Failed as expected before implementation: missing routine program fields, filter state, and migration |
| 2026-06-28 | Slice 3 repository | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 3 ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 3 migration | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.TrainingRoutineProgramMigration22To23Test" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 4 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Failed as expected before implementation: missing active workout notes/reorder APIs and detail notes field |
| 2026-06-28 | Slice 4 repository | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 4 ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 4 active UI focused check | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 4 shared focused check | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 5 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WarmupSetCalculatorTest" --no-daemon --console=plain` | Failed as expected before implementation: missing Training settings, warm-up calculator, and settings migration |
| 2026-06-28 | Slice 5 repository | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 5 ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 5 active UI focused check | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 5 warm-up calculator | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WarmupSetCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 5 plate calculator regression | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.PlateCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 5 migration | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.TrainingSettingsMigration23To24Test" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Pre-push full verification after Slice 5 | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 6 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --no-daemon --console=plain` | Failed as expected before implementation: completed history detail lacked `exerciseGroupings` and history display helper |
| 2026-06-28 | Slice 6 repository/history focused check | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 6 focused Training regression | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 7 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Failed as expected before implementation: missing history overview builder/state |
| 2026-06-28 | Slice 7 ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 7 focused Training regression | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 8 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WorkoutCalculatorTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Failed as expected before implementation: missing progress analytics models/API/state |
| 2026-06-28 | Slice 8 domain/repository/ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.domain.training.WorkoutCalculatorTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 8 focused Training regression | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 9 red test | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Failed as expected before implementation: missing dashboard state |
| 2026-06-28 | Slice 9 ViewModel | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 9 focused Training regression | `. .\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 10 focused Training baseline in isolated worktree | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 10 red test | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest.workoutHistoryDetail_includesWorkoutRecapAfterFinish" --tests "com.musfit.ui.training.TrainingHistoryContentTest.workoutRecapMetricsForDisplay_formatsCoreRecapStats" --no-daemon --console=plain` | Failed as expected before implementation: missing recap model/detail field/display helper |
| 2026-06-28 | Slice 10 repository/history focused check | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalTrainingRepositoryTest.workoutHistoryDetail_includesWorkoutRecapAfterFinish" --tests "com.musfit.ui.training.TrainingHistoryContentTest.workoutRecapMetricsForDisplay_formatsCoreRecapStats" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 10 focused Training regression | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 11 red test | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingHistoryContentTest.workoutRecapMetricsForDisplay_fallsBackToSummaryWhenRecapIsEmpty" --no-daemon --console=plain` | Failed as expected before implementation: recap metric helper only accepted explicit recap data |
| 2026-06-28 | Slice 11 focused fallback check | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingHistoryContentTest.workoutRecapMetricsForDisplay_fallsBackToSummaryWhenRecapIsEmpty" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Slice 11 focused Training regression | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.training.TrainingViewModelTest" --tests "com.musfit.ui.training.TrainingActiveWorkoutContentTest" --tests "com.musfit.ui.training.TrainingHistoryContentTest" --tests "com.musfit.data.repository.LocalTrainingRepositoryTest" --tests "com.musfit.domain.training.WorkoutCalculatorTest" --no-daemon --console=plain` | Passed |
| 2026-06-28 | Final full Training parity verification | `. C:\Users\att1a\WS\MusFit\.superpowers\sdd\android-env.ps1; .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain` | Passed; debug APK assembled at `app/build/outputs/apk/debug/app-debug.apk` |

## Known Limitations

- Training remains local-first and has no social feed, public profiles, followers, cloud sync, subscriptions, accounts, or analytics.
- Hevy-style parity here means practical local feature parity for repeated strength logging, not exact visual cloning or proprietary behavior.
- Any richer exercise metadata must use original MusFit copy and local storage/content.
- Training tool settings are global. Per-exercise rest defaults and personalized warm-up profiles are intentionally left for a later slice.
- Supersets are local pair/grouping tools only; arbitrary multi-exercise superset editing is intentionally out of scope for this parity pass.
- History overview is intentionally compact: current month/current week only, without multi-month navigation or day drill-down.
- Progress analytics are derived on-device from completed sets and intentionally avoid advanced filters, cloud analytics, or cached aggregate tables.
- Dashboard suggestions use visible routine order and do not yet model fatigue, rotation schedules, or progression.
- Workout recap is local/private and intentionally has no social sharing, public feed, or exported share image.

## Blocked Items

- None currently blocked.

## Final APK

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Next Steps

- [x] Run the four focused baseline Training tests and update this ledger with results.
- [x] Complete Slice 1 by syncing `screen-contracts.md`, `data-models.md`, and architecture notes with the verified current Training surface.
- [x] Start Slice 2 with failing tests before any behavior changes.
- [x] Start Slice 3 with failing tests before any behavior changes.
- [x] Start Slice 4 with failing tests before any behavior changes.
- [x] Start Slice 5 with failing tests before any behavior changes.
- [x] Start Slice 6 with failing tests before any behavior changes.
- [x] Start Slice 7 with failing tests before any behavior changes.
- [x] Start Slice 8 with failing tests before any behavior changes.
- [x] Start Slice 9 with failing tests before any behavior changes.
- [x] Start Slice 10 with failing tests before any behavior changes.
- [x] Start Slice 11 closeout audit and full verification.
- [x] Complete final Training parity closeout.
