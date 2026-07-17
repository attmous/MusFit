# MusFit Screen And Navigation Contracts

This document records the stable public contracts between the app shell and
MusFit's current screens. Source remains authoritative. Keep this document
focused on routes, entrypoints, ownership, and result flow; do not copy complete
UI-state constructors, ViewModel action lists, file sizes, or other volatile
implementation detail into it.

For known architecture, restoration, testing, and integration limitations, use
the source-derived [July 2026 architecture audit](app-architecture-audit-2026-07-10.md)
and its [remediation backlog](architecture-remediation-backlog-2026-07-10.md).
Revalidate a dated finding against current source before changing behavior.

## App Shell

Source:

- `app/src/main/java/com/musfit/ui/AppDestination.kt`
- `app/src/main/java/com/musfit/ui/AppNavGraph.kt`
- `app/src/main/java/com/musfit/ui/AppNavigationContract.kt`

### Top-Level Destinations

`AppDestination` is the source of truth for the four bottom destinations:

| Destination | Route | Label | Screen |
| --- | --- | --- | --- |
| `Today` | `today` | Today | `TodayScreen` |
| `Food` | `food` | Food | `FoodScreen` |
| `Training` | `training` | Training | `TrainingScreen` |
| `Profile` | `profile` | Profile | `ProfileScreen` |

`AppNavGraph()` starts at `TodayNavKey`, renders the custom `MusFitBottomNav`,
and uses a saveable Navigation 3 back stack plus `NavDisplay`. Re-selecting a
previously visited tab moves its retained entry to the end of the visit order;
there is never more than one ViewModel/collector owner for a top-level screen.
`NavDisplay` owns system and predictive-back handling.

### Typed Keys And Actions

| Key | Owner | Entrypoint |
| --- | --- | --- |
| `ProfileSettingsNavKey` | Profile | `ProfileSettingsScreen` |
| `TrainingProgressNavKey` | Profile | `TrainingProgressScreen` |
| `NutritionTrendsNavKey` | Profile | `NutritionTrendsScreen` |
| `BarcodeScannerNavKey` | Food | `BarcodeScannerScreen` |
| `NutritionLabelScannerNavKey` | Food | `NutritionLabelScannerScreen` |

`AppNavigationAction` is the typed app-shell contract used by Today, Profile,
Training, and Food callbacks. Profile keys keep Profile selected; scanner keys
keep Food selected. Secondary screens return through `AppNavigator.goBack()`.

### Scanner Results

`AppNavGraph` holds pending barcode and OCR text results as saveable strings.
After a scanner emits a nonblank `AppNavigationResult`, the navigator delivers
the typed result exactly once, pops the producer key, and passes the value into
`FoodScreen`. Food forwards it to `FoodViewModel` and invokes the matching
consumed callback so it is not processed twice.

## Global Coach Sheet

Source:

- `app/src/main/java/com/musfit/ui/today/CoachFeedUi.kt`
- `app/src/main/java/com/musfit/ui/today/CoachChatViewModel.kt`
- `app/src/main/java/com/musfit/data/repository/AiCoachChatRepository.kt`
- `app/src/main/java/com/musfit/data/remote/coach/HermesCoachClient.kt`

Stable entrypoints:

```kotlin
@Composable
fun ChatPreviewFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun ChatPreviewSheet(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit,
    viewModel: CoachChatViewModel = hiltViewModel(),
)
```

The coach FAB is part of the global bottom chrome, not a `NavHost` destination.
It opens `ChatPreviewSheet` over the current screen. Training may also request
the same sheet through `onOpenCoach`. The sheet's configure action navigates to
Profile, where provider settings are available.

Coach chat uses the active configured endpoint and sends a bounded snapshot of
current local context with the user's prompt. Chat history is stored locally.
The current chat contract is read-only with respect to MusFit data: it does not
log, edit, or delete tracker records.

## Today

Source:

- `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`
- `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`
- `app/src/main/java/com/musfit/ui/today/MetricCarouselUi.kt`
- `app/src/main/java/com/musfit/ui/today/CoachFeedUi.kt`

Key: `TodayNavKey`

```kotlin
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenFood: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenHealth: () -> Unit = {},
)
```

Today is the cross-feature dashboard. `TodayViewModel` owns its observable
screen state and derives the carousel, readiness, dashboard editor, and
deterministic coach feed from local repositories. The screen refreshes relevant
data when it resumes.

Navigation is callback-only: `onOpenFood` selects Food, `onOpenTraining`
selects Training, and the legacy-named `onOpenHealth` selects Profile. Today
does not own the global coach sheet or a navigation controller.

## Food

Source:

- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/FoodAddPanelUi.kt`
- `app/src/main/java/com/musfit/ui/food/FoodModalSheets.kt`
- `app/src/main/java/com/musfit/ui/food/FoodTrackersUi.kt`
- `app/src/main/java/com/musfit/data/repository/FoodRepository.kt`

Route: `food`

```kotlin
@Composable
fun FoodScreen(
    scannedBarcode: String? = null,
    onScanClick: () -> Unit = {},
    onScannedBarcodeConsumed: () -> Unit = {},
    scannedLabelText: String? = null,
    onLabelScanClick: () -> Unit = {},
    onScannedLabelConsumed: () -> Unit = {},
    viewModel: FoodViewModel = hiltViewModel(),
)
```

Food currently uses one `FoodViewModel` and a state-driven feature shell.
Diary, meal detail, add flow, recipe browsing/editing, and modal tools are
in-feature surfaces selected through `FoodAddMode`, `FoodSheetMode`, and related
state. They are not separate app-shell routes. Read the
[Food system reference](food-system.md) before changing these transitions.

Barcode and nutrition-label capture are the exceptions: `onScanClick` and
`onLabelScanClick` open app-shell scanner routes. Returned values must be
forwarded once and then consumed through the corresponding callbacks.

`NutritionTrendsScreen` is a Profile-owned secondary route even though it reads
Food data. Food's Health Connect controls are an integration boundary; current
operational limitations are tracked by the architecture audit rather than in
this screen contract.

## Training

Source:

- `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`
- `app/src/main/java/com/musfit/data/repository/TrainingRepository.kt`

Route: `training`

```kotlin
@Composable
fun TrainingScreen(
    viewModel: TrainingViewModel = hiltViewModel(),
    onOpenProgress: () -> Unit = {},
    onOpenCoach: () -> Unit = {},
)
```

`TrainingViewModel` owns routines, exercises, workout history, active-workout
state, and the current in-feature page stack. The visible top level is the
Training dashboard plus History opened from the dashboard. Routine/exercise
detail, editors, pickers, libraries, workout detail, and active-workout surfaces
are coordinated through `TrainingPage`; `TrainingSection.Exercises` remains only
as a legacy state marker while opening exercise detail. These are not separate
app-shell routes.

The screen selects a destination-lifetime projection through
`TrainingRouteUiState`: routines, the exercise library, and their editors collect
`TrainingRoutinesLibraryUiState`, while active workout and history collect
`TrainingActiveHistoryUiState`. `TrainingUiState` remains the internal mutation
and restoration compatibility model; no Training composable collects it as one
aggregate flow. Active workouts remain Room-owned, and closing the active-workout
screen still clears the screen-scoped rest timer.

`onOpenProgress` dispatches the Profile-owned `TrainingProgressNavKey` action.
`onOpenCoach` opens the same global coach sheet used by the bottom coach FAB.
Training handles its in-feature back stack before back falls through to the
app-level bottom-destination stack.

## Profile

Source:

- `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`
- `app/src/main/java/com/musfit/ui/profile/ProfileSettingsScreen.kt`
- `app/src/main/java/com/musfit/ui/profile/ProfileSettingsViewModel.kt`

Key: `ProfileNavKey`

```kotlin
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onOpenFood: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenTrainingProgress: () -> Unit = {},
    onOpenNutritionTrends: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
)
```

Profile is the body/progress hub and owns cross-feature entrypoints for Food,
Training, Training Progress, Nutrition Trends, and Settings. It refreshes
external Health Connect status on resume but keeps profile/body state behind
`ProfileViewModel` and repository boundaries.

Profile Settings is a secondary route:

```kotlin
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
)
```

Settings owns local account/profile editing, optional Google/GitHub identity
linking, AI-coach provider configuration and connection testing, Health Connect
permissions/import/export controls, step-source selection, and app preferences.
Provider identity does not imply cloud synchronization of tracker data.

Profile also owns two focused secondary screens:

```kotlin
@Composable
fun TrainingProgressScreen(
    onBack: () -> Unit,
    viewModel: TrainingProgressViewModel = hiltViewModel(),
)

@Composable
fun NutritionTrendsScreen(
    onBack: () -> Unit,
    viewModel: NutritionTrendsViewModel = hiltViewModel(),
)
```

These screens return with their `onBack` callback and keep Profile selected in
the bottom chrome.

## Scanner Screens

Source:

- `app/src/main/java/com/musfit/ui/food/BarcodeScannerScreen.kt`
- `app/src/main/java/com/musfit/ui/food/NutritionLabelScannerScreen.kt`

```kotlin
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
)

@Composable
fun NutritionLabelScannerScreen(
    onLabelCaptured: (String) -> Unit,
)
```

Both scanner screens own camera permission and CameraX lifecycle work. Barcode
capture uses ML Kit barcode recognition and emits one detected value. Label
capture uses ML Kit text recognition and emits raw OCR text; Food performs the
best-effort parse and review-before-save flow after the route returns.

## Contract Change Checklist

When changing a route or public screen entrypoint:

1. update `AppDestination.kt`, `AppNavGraph.kt`, and the owning screen together;
2. preserve bottom-owner mapping for secondary routes;
3. update navigation/result tests and this document in the same PR;
4. run `scripts/dev/test-dev-workflow.ps1 -SelfTest`;
5. run the focused tests and the standard repository verification gate.

Do not expand this document back into a snapshot of every state field or action.
The current source, tests, audit finding, and remediation package are the
appropriate authorities for that level of detail.
