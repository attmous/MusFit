# MusFit Screen Contracts

This document describes the public UI contracts for each screen: navigation route, Composable entrypoint, ViewModel state model, important user events, and backing data sources.

## Contract Pattern

Most feature screens follow this contract:

```text
AppNavGraph route -> Screen composable -> Hilt ViewModel -> StateFlow<UiState> -> repository Flow/write APIs
```

Composables should stay presentation-focused. Persistent state, validation, repository calls, and business actions live in ViewModels and repositories.

## App Shell

### `AppDestination`

Source: `app/src/main/java/com/musfit/ui/AppDestination.kt`

| Destination | Route | Label | Icon |
| --- | --- | --- | --- |
| `Today` | `today` | Today | `Icons.Outlined.Today` |
| `Food` | `food` | Food | `Icons.Outlined.Restaurant` |
| `Training` | `training` | Training | `Icons.Outlined.FitnessCenter` |
| `Health` | `health` | Health | `Icons.Outlined.MonitorHeart` |

Additional routes:

| Route constant | Route | Purpose |
| --- | --- | --- |
| `BARCODE_SCANNER_ROUTE` | `barcode-scanner` | Full-screen barcode capture. |
| `NUTRITION_LABEL_SCANNER_ROUTE` | `nutrition-label-scanner` | Full-screen OCR capture for nutrition labels. |

### `AppNavGraph`

Source: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`

Entrypoint:

```kotlin
@Composable
fun AppNavGraph()
```

Responsibilities:

- Owns `NavController`.
- Renders the bottom navigation bar.
- Starts at `AppDestination.Today.route`.
- Passes tab-switch callbacks into `TodayScreen`.
- Holds one-shot scanner return values:
  - `scannedBarcode: String?`
  - `scannedLabelText: String?`
- Routes scanner results back into `FoodScreen`.

## Today Screen

Source:

- `app/src/main/java/com/musfit/ui/today/TodayScreen.kt`
- `app/src/main/java/com/musfit/ui/today/TodayViewModel.kt`

Route: `today`

Composable entrypoint:

```kotlin
@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onOpenFood: () -> Unit = {},
    onOpenTraining: () -> Unit = {},
    onOpenHealth: () -> Unit = {},
)
```

### Purpose

Today is the dashboard and cross-feature summary. It displays nutrition rings, macro split, training glimpse, weekly goal progress, weight context, deterministic coaching cues, and a goals editor.

### ViewModel State

`TodayViewModel` exposes:

```kotlin
val state: StateFlow<TodayUiState>
```

`TodayUiState` fields:

| Field | Purpose |
| --- | --- |
| `dateLabel` | Formatted current date. |
| `rings` | Daily goal rings for calories, protein, and steps. |
| `macros` | Carb/protein/fat grams for today. |
| `training` | Short training summary tile. |
| `weightKg` | Latest known weight. |
| `weekly` | Seven-day rollup from `WeeklyGoalsCalculator`. |
| `isGoalsEditorVisible` | Controls the goals editor bottom sheet. |
| `stepGoalInput` | Editable step goal field. |
| `sessionTargetInput` | Editable weekly training session target. |
| `targetWeightInput` | Editable target weight. |
| `coach` | Deterministic `CoachBriefing`. |

Supporting UI models:

- `DailyRingUiState(kind, centerLabel, goalLabel, progress)`
- `MacroBreakdownUiState(carbsGrams, proteinGrams, fatGrams)`
- `TrainingGlimpseUiState(title, subtitle, hasWorkout)`
- `RingKind.Calories`, `RingKind.Protein`, `RingKind.Steps`

### ViewModel Actions

User actions:

- `openGoalsEditor()`
- `closeGoalsEditor()`
- `onStepGoalInputChanged(value)`
- `onSessionTargetInputChanged(value)`
- `onTargetWeightInputChanged(value)`
- `saveUserGoals()`

### Data Sources

`TodayViewModel` combines:

- `FoodRepository.observeDailyNutrition(date)`
- `FoodRepository.observeFoodGoal()`
- `TrainingRepository.observeDailyTrainingSummary(date)`
- `HealthRepository.observeDailySummary(date)`
- `GoalsRepository.observeUserGoals()`

Weekly and coaching state also use:

- `TrainingRepository.observeWorkoutHistory()`
- `TrainingRepository.observeRoutineSummaries()`
- `HealthRepository.observeDailySummaries(startDate, endDate)`
- `HealthRepository.observeWeightSeries(fromEpochMillis)`
- `WeeklyGoalsCalculator`
- `CoachEngine`

### Navigation Outputs

Today does not navigate directly through `NavController`. It emits callbacks supplied by `AppNavGraph`:

- Food shortcut: `onOpenFood`
- Training shortcut: `onOpenTraining`
- Health shortcut: `onOpenHealth`

## Food Screen

Source:

- `app/src/main/java/com/musfit/ui/food/FoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/AddFoodScreen.kt`
- `app/src/main/java/com/musfit/ui/food/FoodViewModel.kt`
- `app/src/main/java/com/musfit/ui/food/BarcodeScannerScreen.kt`
- `app/src/main/java/com/musfit/ui/food/NutritionLabelScannerScreen.kt`

Route: `food`

Composable entrypoint:

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

### Purpose

Food is the largest miniapp. It owns daily food diary tracking, add-food workflows, saved food database, Open Food Facts barcode import, nutrition label OCR review, quick calories, recipes, meal templates, meal planning, custom meals, water, shopping list, nutrition goals, and food Health Connect sync.

### Screen Modes

Food uses state-driven modes instead of separate nav routes for most Food surfaces.

`FoodSheetMode`:

| Mode | Surface |
| --- | --- |
| `AddFood` | Add-food flow for saved, manual, barcode, quick, and AI shell inputs. |
| `FoodDatabase` | Saved food database, online search import, duplicates, starter-food import. |
| `FoodDetail` | Read-only saved-food detail and log action. |
| `DiaryEntryEditor` | Edit logged/planned diary item amount, meal, copy, delete, undo. |
| `SavedFoodEditor` | Full saved-food editor. |
| `NutritionLabelScan` | OCR shell and review-before-save fields. |
| `GoalEditor` | Food calorie/macro/detail goal editor. |
| `RecipeEditor` | Recipe create/edit/log support. |
| `MealTemplates` | Meal template list, edit, duplicate, favorite, log. |
| `MealSettings` | Custom meal definitions and meal times. |
| `ShoppingList` | Generated and manual shopping list. |

`FoodAddMode`:

| Mode | Purpose |
| --- | --- |
| `Saved` | Recent, same-as-yesterday, saved foods, favorites, templates, and recipes. |
| `Manual` | Create/log a manually entered food. |
| `Barcode` | Lookup/edit/log barcode products. |
| `Quick` | Quick calorie and macro entry. |
| `Ai` | Text/voice/photo placeholder shell with editable confirmation before logging. |

`AddTab`:

- `Recents`
- `Favorites`
- `Create`

`MealDetailSortMode`:

- `Logged`
- `Calories`
- `Protein`
- `Name`

### Main Render States

The top-level `FoodScreen` chooses between:

| Condition | Rendered surface |
| --- | --- |
| `selectedMealDetailForDisplay() != null` | Full-screen `MealDetailScreen`. |
| `state.isAddPanelVisible && state.sheetMode == AddFood` | Full-screen `AddFoodScreen`. |
| Otherwise | Main Food diary screen. |
| `state.isAddPanelVisible` and modal mode applies | `ModalBottomSheet` keyed by `FoodSheetMode`. |

The floating action button opens a meal picker when the diary is visible. Tapping a meal or plus action calls `FoodViewModel.openAddFood(mealType)`.

### ViewModel State

`FoodViewModel` exposes:

```kotlin
val state: StateFlow<FoodUiState>
```

`FoodUiState` is intentionally broad because Food currently uses one ViewModel for diary, add flow, database, editors, and related panels.

State groups:

| Group | Representative fields |
| --- | --- |
| Date and loading | `selectedDate`, `isLoading`, `isSaving`, `message` |
| Barcode/manual draft | `barcode`, `productName`, `brand`, `caloriesPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`, `quantityGrams`, `lookupResult` |
| Amount preview | `amountNutritionPreview`, `amountServingChoices` |
| Goals | `calorieGoalKcal`, `proteinGoalGrams`, `carbsGoalGrams`, `fatGoalGrams`, detail nutrient goals, `goalMode`, `includeTrainingCalories`, `useNetCarbs` |
| Diary summary | `eatenCaloriesKcal`, `remainingCaloriesKcal`, `macroProgress`, `advancedNutritionProgress`, `micronutrients`, `dailyInsights`, `dayRating` |
| Meals | `mealSections`, `mealDefinitions`, `selectedMealDetailId`, `mealDetailSortMode`, `selectedMealTitle`, `mealType` |
| Planning | `weeklyPlan`, `isPlanningMode` |
| Water | `waterConsumedMilliliters`, `waterGoalMilliliters`, `waterProgress`, water input fields |
| Food Health Connect | sync enabled, permission state, permission summary, last sync, last failure |
| Saved foods | `savedFoods`, `visibleSavedFoods`, `duplicateFoodGroups`, `selectedSavedFoodDetail`, saved-food editor inputs |
| Add flow | `isAddPanelVisible`, `sheetMode`, `addMode`, `addTab`, `keepAddingFoods`, `recentFoods`, `sameAsYesterday`, `foodDatabaseQuery` |
| Diary item editor | editing id, meal, serving choices, original and preview nutrition values, planned/logged flag |
| Templates | `mealTemplates`, template editor fields and draft items |
| Recipes | `recipes`, recipe editor fields, ingredient draft, serving choices |
| Quick calories | quick calorie/macro fields, quick favorite presets |
| Shopping | shopping list groups, date inputs, manual item inputs |

Important Food UI models:

| Model | Purpose |
| --- | --- |
| `FoodMealSectionUiState` | One meal section with totals, goals, details, micronutrients, rating, and entries. |
| `FoodMealEntryUiState` | One logged or planned diary row with calculated nutrition and contribution ratios. |
| `SavedFoodUiState` | Saved food display/edit model, including per-100 g nutrition, serving info, barcode, category, favorite, source, image, and custom servings. |
| `OnlineFoodResultUiState` | Open Food Facts search/import result. |
| `FoodAmountNutritionPreviewUiState` | Live calculated calories/protein/carbs/fat for selected quantity. |
| `FoodAmountServingChoiceUiState` | Selectable serving chips for amount entry. |
| `MealTemplateUiState` | Meal template summary and editable draft items. |
| `RecipeUiState` | Recipe summary, ingredients, cooked yield, serving count, and per-serving nutrition. |
| `QuickCaloriePresetUiState` | Favorite quick log. |
| `ShoppingListGroupUiState` and `ShoppingListItemUiState` | Shopping list display model. |
| `FoodRatingUiState` and `FoodInsightUiState` | Deterministic day/meal feedback. |
| `FoodMealDefinitionUiState` | Default or custom meal type metadata. |

### ViewModel Action Surface

Date and planning:

- `goToPreviousDay()`, `goToNextDay()`, `goToToday()`
- `togglePlanningMode()`
- `copySelectedDayToTomorrow()`

Main add flow and meal detail:

- `openAddFood(mealType)`, `closeAddFood()`, `selectAddMode(mode)`, `selectAddTab(tab)`
- `openMealDetail(mealType)`, `closeMealDetail()`, `onMealDetailSortChanged(sortMode)`
- `openAddFoodFromMealDetail()`, `copySelectedMealFromYesterday()`, `saveSelectedMealAsTemplate(name)`

Food database and saved-food editor:

- `openFoodDatabase()`, `onFoodDatabaseQueryChanged(value)`, `searchOnlineFoods()`
- `openSavedFoodDetail(foodId)`, `openNewSavedFoodEditor()`, `openSavedFoodEditor(foodId)`
- `saveSavedFood()`, `deleteSavedFood()`, `duplicateSavedFood()`
- `toggleFavoriteFood(foodId, isFavorite)`
- `mergeDuplicateFoods(primaryFoodId, duplicateFoodIds)`
- `seedStarterFoods()`
- Saved-food field handlers such as `onSavedFoodNameChanged`, `onSavedFoodBarcodeChanged`, and nutrient field handlers.

Barcode, OCR, manual, and quick logging:

- `onBarcodeChanged(value)`, `onScannedBarcode(barcode)`, `lookupBarcode()`
- `saveProduct()`, `saveScannedProductToDatabase()`, `logFood()`
- `onScannedLabel(rawText)`, `openNutritionLabelScan()`
- `onProductNameChanged`, `onBrandChanged`, `onQuantityChanged`, nutrient field handlers
- `quickLog()`, `saveFavoriteQuickLog()`, `logFavoriteQuickLog(presetId)`, `toggleFavoriteQuickLog(presetId, isFavorite)`

Saved food and diary item logging:

- `logSavedFood(foodId)`
- `onSavedFoodServingSelected(foodId, grams)`, `onSavedFoodQuantityChanged(value)`
- `openDiaryEntryEditor(entryId)`, `saveDiaryEntry()`, `deleteDiaryEntry()`
- `undoDeleteDiaryEntry()`, `copyDiaryEntryTo(mealType, date)`, `markDiaryEntryLogged()`

Recipes and templates:

- `openRecipeEditor(recipeId)`, `saveRecipe()`, `deleteRecipe(recipeId)`, `duplicateRecipe(recipeId)`, `logRecipe(recipeId)`, `toggleFavoriteRecipe(recipeId, isFavorite)`
- Recipe field and ingredient handlers.
- `openMealTemplates()`, `openMealTemplateEditor(templateId)`, `saveMealTemplateEdits()`, `duplicateMealTemplate(templateId)`, `deleteMealTemplate(templateId)`, `logMealTemplate(templateId)`, `toggleFavoriteMealTemplate(templateId, isFavorite)`

Goals, custom meals, water, shopping, Health Connect:

- `openGoalEditor()`, goal field handlers, `saveFoodGoal()`
- `openMealSettings()`, `openMealDefinitionEditor(mealId)`, custom meal field handlers, `saveCustomMealDefinition()`
- `logQuickWater(amountMilliliters)`, `logCustomWater()`, `saveWaterGoal()`
- `openShoppingList()`, shopping field handlers, `generateShoppingList()`, `addManualShoppingListItem()`, `toggleShoppingListItem(itemId, isChecked)`
- `refreshFoodHealthConnectSync()`, `onFoodHealthConnectSyncEnabledChanged(isEnabled)`, `syncFoodToHealthConnect()`

### Data Sources

Food ViewModel observes:

- `FoodRepository.observeFoodDiary(date)`
- `FoodRepository.observeFoodPlan(startDate)`
- `FoodRepository.observeWaterSummary(date)`
- `FoodRepository.observeFoodHealthConnectSyncState()`
- `FoodRepository.observeSavedFoods()`
- `FoodRepository.observeRecentFoods(limit)`
- `FoodRepository.observeSameAsYesterday(mealType, date)`
- `FoodRepository.observeFoodGoal()`
- `FoodRepository.observeMealTemplates()`
- `FoodRepository.observeCustomMealDefinitions()`
- `FoodRepository.observeShoppingList()`
- `FoodRepository.observeRecipes()`
- `FoodRepository.observeQuickCaloriePresets()`

Food ViewModel also calls `FoodProductProvider` for direct barcode/product search workflows.

### Scanner Screens

Barcode scanner:

```kotlin
@Composable
fun BarcodeScannerScreen(onBarcodeDetected: (String) -> Unit)
```

Nutrition label scanner:

```kotlin
@Composable
fun NutritionLabelScannerScreen(onLabelCaptured: (String) -> Unit)
```

Both screens handle camera permission through `rememberLauncherForActivityResult`. They are route-level screens because they need camera lifecycle handling outside the Food modal stack.

## Training Screen

Source:

- `app/src/main/java/com/musfit/ui/training/TrainingScreen.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingViewModel.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingRoutineContent.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingActiveWorkoutContent.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingHistoryContent.kt`
- `app/src/main/java/com/musfit/ui/training/TrainingProgressContent.kt`

Route: `training`

Composable entrypoint:

```kotlin
@Composable
fun TrainingScreen(viewModel: TrainingViewModel = hiltViewModel())
```

### Purpose

Training handles strength routine management, exercise library filtering and custom exercise creation, active workout logging, quick set logging, workout history, exercise progress, rest timer behavior, plate hints, PR badges, supersets, and Health Connect-compatible completed workouts.

### Internal Sections

`TrainingSection`:

| Section | Purpose |
| --- | --- |
| `Routines` | Routine list, local program filters, starter routines, routine editor, duplicate/delete/start routine, and start blank workout. |
| `Exercises` | Exercise library, search/filter, equipment/muscle chips, exercise detail, local notes, and custom exercise creation. |
| `History` | Completed workout overview, month grid, consistency metrics, list, workout recap, and workout detail with set rows. |
| `Progress` | All-training analytics, muscle volume, weekly volume, exercise PR cards, history rows, best sets, PR timeline, and trend chart points. |

### ViewModel State

`TrainingViewModel` exposes:

```kotlin
val state: StateFlow<TrainingUiState>
```

`TrainingUiState` fields:

| Field | Purpose |
| --- | --- |
| `selectedSection` | Current Training tab. |
| `routines` | Routine summaries. |
| `visibleRoutines` | Routine summaries filtered by selected program. |
| `routineProgramOptions` | Distinct local program names from routine summaries. |
| `selectedRoutineProgram` | Current routine program filter, or null for all routines. |
| `dashboard` | Derived dashboard state: next suggested visible routine, quick-start routines, and most recent completed workout. |
| `exercises` | All exercise summaries. |
| `visibleExercises` | Search/filter result list. |
| `activeWorkoutSummary` | Compact resume banner data. |
| `activeWorkout` | Full active workout detail with flat exercise blocks, grouped superset blocks, sets, completed count, and volume. |
| `workoutHistory` | Completed workout summaries. |
| `historyOverview` | Derived month calendar, current-week totals, training-day counts, and streak metrics from completed workout summaries. |
| `selectedProgressExerciseId` | Selected exercise for progress. |
| `selectedExerciseProgress` | Heaviest, max reps, best estimated 1RM, best day volume, trend points, history rows, best sets, and PR timeline for the selected exercise. |
| `progressAnalytics` | Derived all-training muscle group volume and weekly volume from completed workout sets. |
| `selectedWorkoutDetail` | Expanded completed-workout history detail with recap metrics, flat exercise blocks, and derived superset groupings. |
| `exerciseSearchQuery`, `exerciseMuscleFilter`, `exerciseEquipmentFilter` | Exercise library filters. |
| `exerciseEditor` | Custom exercise editor state. |
| `selectedExerciseDetail` | Open exercise detail/drill-down row, including instructions and local notes. |
| `exerciseDetailNotesInput` | Editable local notes draft for the selected exercise detail. |
| `exerciseName`, `reps`, `weightKg`, `sets` | Quick set logger state. |
| `totalVolumeKg`, `bestEstimatedOneRepMaxKg` | Quick logger summaries. |
| `routineEditor` | Routine create/edit state. |
| `activeWorkoutRouteOpen` | Controls full active workout route-like UI. |
| `activeWorkoutNotesInput` | Editable notes draft for the current active workout session. |
| `restTimer` | Timer visibility, source set id, duration, remaining time, and running state. |
| `trainingSettings` | Persisted global Training tool settings for default rest duration, bar weight, and available plates. |
| `restTimerDefaultSecondsInput`, `plateBarWeightInput`, `availablePlatesInput` | Editable drafts for Training tool settings. |
| `finishConfirmationOpen`, `discardConfirmationOpen` | Active workout dialogs. |
| `message` | User-facing result/error text. |

Supporting models:

- `RoutineEditorState`
- `ExerciseEditorState`
- `RestTimerState`

Repository models that are important to the active logger:

- `WorkoutExerciseBlock` contains exercise metadata, target reps, sets, prior best estimated 1RM, optional superset group id, and optional A/B superset label.
- `ExerciseGrouping.Single` and `ExerciseGrouping.Superset` allow the UI to render standalone exercises and grouped supersets from the same active workout detail.
- `LoggedWorkoutSetDetail` carries set type, reps, weight, RPE, user note, completion, previous-set label, and superset group id.

### ViewModel Actions

Section and route state:

- `selectSection(section)`
- `resumeActiveWorkout()`
- `closeActiveWorkoutRoute()`

Routine management:

- `onRoutineProgramFilterChanged(value)`
- `openRoutineEditor(routineId)`
- `closeRoutineEditor()`
- `onRoutineNameChanged(value)`, `onRoutineNotesChanged(value)`
- `addRoutineExercise(exerciseId)`, `removeRoutineExercise(index)`
- `onRoutineExerciseTargetSetsChanged(index, value)`, `onRoutineExerciseTargetRepsChanged(index, value)`
- `moveRoutineExerciseUp(index)`, `moveRoutineExerciseDown(index)`
- `saveRoutineEditor()`, `duplicateRoutine(routineId)`, `deleteRoutine(routineId)`

Exercise library:

- `onExerciseSearchQueryChanged(value)`
- `onExerciseMuscleFilterChanged(value)`, `onExerciseEquipmentFilterChanged(value)`, `clearExerciseFilters()`
- `openExerciseDetail(exerciseId)`, `closeExerciseDetail()`
- `onExerciseDetailNotesChanged(value)`, `saveExerciseDetailNotes()`
- `openCustomExerciseEditor()`, `closeCustomExerciseEditor()`
- `onCustomExerciseNameChanged(value)`, `onCustomExerciseCategoryChanged(value)`, `onCustomExerciseEquipmentChanged(value)`, `onCustomExerciseTargetMusclesChanged(value)`
- `saveCustomExercise()`

Workout flows:

- `startBlankWorkout()`, `startRoutine(routineId)`
- `addExerciseToActiveWorkout(exerciseId)`
- `addWorkoutSet(exerciseId)`, `duplicateLastWorkoutSet(exerciseId)`
- `updateWorkoutSetFields(setId, setType, reps, weightKg, rpe, notes)`
- `onActiveWorkoutNotesChanged(value)`, `saveActiveWorkoutNotes()`
- `moveWorkoutSetUp(setId)`, `moveWorkoutSetDown(setId)`
- `deleteWorkoutSet(setId)`, `toggleWorkoutSetCompletion(setId, completed)`
- `makeSupersetWithNext(exerciseId)`, `dissolveSuperset(groupId)`
- `requestFinishActiveWorkout()`, `finishActiveWorkout()`, `cancelFinishActiveWorkout()`
- `requestDiscardActiveWorkout()`, `discardActiveWorkout()`, `cancelDiscardActiveWorkout()`

Quick set and progress:

- `onExerciseChanged(value)`, `onRepsChanged(value)`, `onWeightChanged(value)`, `addSet()`, `toggleSetCompletion(setIndex)`
- `selectProgressExercise(exerciseId)`
- `openWorkoutDetail(sessionId)`, `closeWorkoutDetail()`

Rest timer:

- `tickRestTimer()`
- `pauseRestTimer()`
- `resumeRestTimer()`
- `skipRestTimer()`
- `adjustRestTimerSeconds(deltaSeconds)`
- `onRestTimerDefaultSecondsChanged(value)`, `onPlateBarWeightChanged(value)`, `onAvailablePlatesChanged(value)`
- `saveTrainingToolSettings()`
- `addSuggestedWarmupSet(exerciseId, reps, weightKg)`

### Content Composables

| Composable | Inputs | Outputs |
| --- | --- | --- |
| `TrainingRoutineContent` | visible routines, program options, selected program | Program filter, start, edit, duplicate, delete routine callbacks. |
| `TrainingRoutineEditor` | `RoutineEditorState`, exercises | Edit routine metadata and exercise targets. |
| `TrainingActiveWorkoutContent` | `ActiveWorkoutDetail`, exercises, `RestTimerState`, active workout notes draft, Training tool setting drafts | Set edits, set type changes, set reorder, workout notes, add exercise/set, timer controls, Training tool save, warm-up suggestions, PR/plate display, superset create/dissolve, finish/discard. |
| `TrainingHistoryContent` | `history`, `historyOverview`, selected detail | Render month overview, consistency metrics, open/close workout detail, completed-workout recap, and completed supersets with grouped sections and A/B labels. |
| `TrainingProgressContent` | exercises, selected id, progress, progress analytics | Select exercise and render all-training analytics plus selected-exercise trend/PR/history display. |

Exercise library detail behavior:

- Tapping an exercise row opens an inline detail card in the Exercises section.
- Detail displays equipment, category, library/custom state, primary muscles, secondary muscles, original MusFit instructions when available, and editable local notes.
- Saving notes trims blank space and stores notes locally through `TrainingRepository.updateExerciseLocalNotes`; blank notes are stored as null.
- Starter exercises have original MusFit instruction copy. Custom exercises use their target muscles as primary muscles and start with no instructions.

Routine organization behavior:

- Starter routines carry local `programName` and tags for Full Body, Push Pull Legs, Upper Lower, Strength, Hypertrophy, and Beginner programs.
- The Routines section shows horizontal program chips derived from available routines.
- Selecting a program filters the routine cards without changing persistence or active workouts.
- Duplicating a starter/program routine creates an editable non-starter local copy and preserves the program name/tags.

Training dashboard behavior:

- The home dashboard shows the next visible routine, quick-start routine buttons, blank-workout start, and most recent completed workout.
- The next-routine suggestion is a deterministic local heuristic from the currently visible routine list; program filtering changes the suggestion.
- The dashboard keeps the existing active workout resume banner and weekly header behavior intact.

Active workout UI behavior:

- Full-screen route-like surface opens when an active workout is resumed or started.
- Top bar shows elapsed time, completed sets, total volume, `Finish`, and a discard overflow action.
- Completing a valid active set starts the deterministic rest timer at the saved default duration; the user can pause, resume, skip, or adjust by 15 seconds.
- The Training tools card persists default rest seconds, bar weight, and available plate inventory through local Training settings.
- Workout-level notes are editable from the active workout route and are stored on the active session.
- Set rows support warmup/working/drop/failure display labels, reps, weight, RPE, notes, delete, duplicate-last-set, up/down reorder, and completion.
- Warm-up suggestions are deterministic from the latest non-warmup working set and can be added as warmup rows.
- PR badges compare completed working sets against the prior best estimated 1RM captured before the active session.
- Plate hints use `PlateCalculator` with the saved bar and plate settings.
- Superset creation pairs a standalone exercise with the next standalone exercise below it; grouped rows show A/B labels and can be dissolved.
- Finish and discard both require confirmation dialogs.
- Finishing a workout closes the active route and opens the completed workout detail in History.

History detail behavior:

- History list mode shows a current-month workout grid, current-week workouts/training days/sets/volume, current streak, and best streak.
- Completed workout detail starts with a recap card covering duration, completed sets, volume, exercise count, PR count, and stored workout notes.
- Recap PR count follows the active-workout PR badge rule: completed non-warmup/non-drop sets that beat the prior best estimated 1RM for that exercise.
- Completed workout detail preserves the same derived `ExerciseGrouping` shape as the active workout detail.
- Completed supersets render as grouped history sections with a `SUPERSET` label and each member's A/B badge.
- Older details or fallback repository implementations that provide only flat blocks still render as standalone exercise cards.

Progress analytics behavior:

- Progress analytics are derived from completed local workout sets; no derived stats are stored.
- The Progress tab shows top muscle groups by completed volume/set count and recent weekly volume totals.
- Selecting an exercise shows PR totals, trend charts, per-day exercise history rows, best daily sets, and estimated-1RM PR timeline entries.

### Data Sources

Training ViewModel observes:

- `TrainingRepository.observeRoutineSummaries()`
- `TrainingRepository.observeExercises()`
- `TrainingRepository.observeActiveWorkoutSummary()`
- `TrainingRepository.observeActiveWorkoutDetail()`
- `TrainingRepository.observeTrainingSettings()`
- `TrainingRepository.observeWorkoutHistory()`
- `TrainingRepository.observeExerciseProgress(exerciseId)`
- `TrainingRepository.getWorkoutHistoryDetail(sessionId)`

It calls write APIs for routine CRUD, active workout lifecycle, set edits, custom exercises, starter data seeding, superset creation/dissolve, workout finish/discard, and workout history detail.

### Current Training Limitations

- Exercise detail exists as an inline card, but there is no media, animation, or externally sourced technique library.
- Routine organization is program/tag based; there is no separate multi-week program calendar or progression schedule yet.
- Rest timer duration, bar weight, and available plates are global Training settings; there are no per-exercise rest defaults or warm-up preference profiles yet.
- Supersets are intentionally pair-oriented through "make with next"; there is no arbitrary multi-exercise superset editor yet.
- History overview is current-month/current-week focused; there is no drillable multi-month calendar or per-day workout filter yet.
- Progress analytics are local and deterministic; there is no advanced chart filtering, period picker, or stored analytics cache yet.
- Dashboard routine suggestions are deterministic and local; there is no adaptive progression or fatigue model yet.
- Post-workout recap is local and private only; there is no social sharing, public feed, or exported recap image.

## Health Screen

Source:

- `app/src/main/java/com/musfit/ui/health/HealthScreen.kt`
- `app/src/main/java/com/musfit/ui/health/HealthViewModel.kt`

Route: `health`

Composable entrypoint:

```kotlin
@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel())
```

### Purpose

Health is the user-facing Health Connect utility screen. It reports availability and permissions, launches permission requests, imports today's health summary, and exports the latest workout.

### ViewModel State

`HealthViewModel` exposes:

```kotlin
val state: StateFlow<HealthUiState>
```

`HealthUiState` fields:

| Field | Purpose |
| --- | --- |
| `availabilityLabel` | Human readable Health Connect availability. |
| `grantedPermissionCount` | Count of currently granted permissions. |
| `requestablePermissionCount` | Count of permissions the app can request. |
| `requestablePermissions` | Permission strings for launcher. |
| `canRequestPermissions` | Enables the permission button. |
| `message` | User-facing status or result message. |

### ViewModel Actions

- `refreshStatus()`
- `importToday()`
- `exportLatestWorkout()`

### Data Sources

Health ViewModel uses `HealthRepository`, which delegates to `HealthConnectGateway` and local Room storage through `HealthDao`.

## Cross-Screen Relationships

| Relationship | Contract |
| --- | --- |
| Today -> Food | `TodayScreen` emits `onOpenFood`; `AppNavGraph` navigates to Food. |
| Today -> Training | `TodayScreen` emits `onOpenTraining`; `AppNavGraph` navigates to Training. |
| Today -> Health | `TodayScreen` emits `onOpenHealth`; `AppNavGraph` navigates to Health. |
| Food -> Barcode scanner | `FoodScreen` receives `onScanClick`; `AppNavGraph` opens `barcode-scanner`; result returns through `scannedBarcode`. |
| Food -> Nutrition label scanner | `FoodScreen` receives `onLabelScanClick`; `AppNavGraph` opens `nutrition-label-scanner`; result returns through `scannedLabelText`. |
| Health -> Training data | `HealthRepository.exportLatestWorkout()` reads latest completed workout through `TrainingDao`. |
| Today dashboard | Combines Food, Training, Health, and Goals repositories. |
