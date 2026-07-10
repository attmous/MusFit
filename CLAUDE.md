# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

MusFit is an **Android-only** fitness and nutrition tracker (Kotlin, Jetpack Compose Material 3, Hilt, Room, Coroutines/Flow). It is local-first: local account identity exists, but there is no cloud sync, analytics, subscriptions, social features, or cloud AI. Single Gradle module (`:app`), application id `com.musfit`.

The product is organized as menu-by-menu "miniapps" reached from a bottom nav: **Today, Food, Training, Health**. **Food is the active focus** and by far the largest area — keep changes scoped to Food unless the user explicitly asks for Training, Today, Health, or cross-cutting architecture work.

`AGENTS.md` is the living handoff doc: current Food feature state, the 24-slice Food roadmap, and the known dev-phone serial. Read it for *what to build next*; read this file for *how the code is shaped and how to build/verify it*.

## Build & verify (Windows PowerShell)

First, source the committed local Android toolchain env (sets JDK 17 + Android SDK for the shell):

```powershell
. .\scripts\android\android-env.ps1
```

Full verification — run this (and confirm it passes) before claiming completion or pushing:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Repo helper equivalent with optional generated-output retry:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Full -RetryOnGeneratedOutputIssue
```

Run a single test class (the fast inner loop for Food work):

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.food.FoodViewModelTest" --no-daemon --console=plain
```

Debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install/launch on a connected device:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
```

CI (`.github/workflows/android.yml`) runs `testDebugUnitTest lintDebug assembleDebug` for PRs and pushes, uploads the APK as the `musfit-debug-apk` artifact, and runs the full `build` task only for default-branch pushes.

### Generated-output cleanup

If Gradle leaves stale or locked generated files under `app/build`, use the
repo-owned cleanup helper and then rerun the verification command:

```powershell
.\scripts\dev\clean-generated.ps1
```

## Architecture

Strict layering, one direction of dependency: **Compose screen → ViewModel → Repository → DAO / Remote → Room / Open Food Facts**.

- **UI** (`ui/<feature>/`): `@Composable` screens + a `@HiltViewModel`. ViewModels expose a single immutable `StateFlow<...State>` built from a private `MutableStateFlow(...).asStateFlow()`, drive date-scoped data with `flatMapLatest` over a date flow, and run mutations in `viewModelScope`. Navigation is a bottom-nav `NavHost` in `ui/AppNavGraph.kt`; destinations are the `AppDestination` enum.
- **Repository** (`data/repository/`): each feature has an **interface** (`FoodRepository`) plus a `Local*` implementation (`LocalFoodRepository`) bound via Hilt `@Binds` in `core/di/RepositoryModule.kt`. Repositories map Room entities ↔ domain models, expose `Flow`, and wrap multi-step writes in `withTransaction`. Public input/output types are `data class`es declared alongside the interface.
- **DAO** (`data/local/dao/`): Room DAOs return `Flow`. Aggregate/joined reads use dedicated **query-projection row** types (e.g. `FoodDiaryEntryRow`, `MealNutritionRow`) rather than full entities.
- **Domain** (`domain/`): pure Kotlin models and stateless calculators (e.g. `NutritionCalculator`, `WorkoutCalculator`) — no Android/Room dependencies, trivially unit-testable.
- **Remote** (`data/remote/food/`): Open Food Facts via Retrofit + Moshi, behind a `FoodProductProvider` interface. Barcode capture uses CameraX + ML Kit (`ui/food/BarcodeScannerScreen.kt`).
- **Health Connect** (`integrations/healthconnect/`): the boundary for Android health data (status, permissions, read summaries, food/nutrition export). Gated behind a `HealthConnectGateway` interface so it's fakeable in tests.
- **DI** (`core/di/`): `DatabaseModule` (DB + DAOs + migrations), `RepositoryModule` (interface→impl binds), `NetworkModule`, `HealthModule`. App entry: `MusFitApplication` (`@HiltAndroidApp`) → `MainActivity`.

### Room database — migrations are mandatory

`MusFitDatabase` is at **version 36** with `exportSchema = true`; every version's schema JSON is committed under `app/schemas/` and ships as a test asset. Any entity/schema change **must**: (1) add a `MIGRATION_x_y` to `core/di/DatabaseModule.kt` and register it in `addMigrations(...)`, (2) bump `version`, and (3) commit the new `app/schemas/...json`. There is no `fallbackToDestructiveMigration` — a missing migration crashes existing installs. Match the column names/types Room expects exactly (compare against the generated schema JSON).

### The Food miniapp is concentrated and large

The Food UI is large but now split by feature seam: `ui/food/FoodScreen.kt` (~2,000 lines: diary, summary, meal detail, the `FoodSheetMode` dispatch) plus `FoodComponents.kt` (shared primitives), `FoodTrackersUi.kt` (water/Health Connect), `FoodModalSheets.kt` (the 10 sheet panels), and `FoodAddPanelUi.kt` (add-food forms). `ui/food/FoodViewModel.kt` is still ~4,800 lines and `data/repository/FoodRepository.kt` ~2,160. New Food work extends these existing files and patterns — prefer following the established `FoodAddMode` / `FoodSheetMode` / state-driven sheet conventions over introducing new abstractions or splitting files unprompted.

For a full map of the Food miniapp — feature inventory, the state-driven sheet/add modes, the `FoodUiState` shape, and the in-progress structure refactor — see [`docs/architecture/food-system.md`](docs/architecture/food-system.md). Read it before large Food work.

## Testing

TDD is expected for behavior changes: add/adjust a failing test, run it red, then implement.

- **ViewModel tests** — plain JUnit with hand-written fakes (`FakeFoodRepository`, `FakeProductProvider`), `StandardTestDispatcher` + `Dispatchers.setMain/resetMain`, Turbine available for Flow assertions. No Robolectric.
- **Repository / DAO tests** — `@RunWith(RobolectricTestRunner::class)` against an **in-memory** Room database, exercising real migrations and queries.
- **Domain calculators** — pure JUnit, no Android.

## Conventions

- Android-only; do not add cloud sync, analytics, subscriptions, social, cloud-AI features, or new account providers unless explicitly requested. Keep data local-first.
- UI should be dense, clean, and practical (Lifesum-like information architecture, original layouts — do not copy Lifesum assets). Avoid marketing-style layouts inside the app.
- Plans and specs live in `docs/superpowers/plans/` and `docs/superpowers/specs/`.
- The established deploy flow is to push verified work to `origin/master` when the user asks.
