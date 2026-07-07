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
| `PROFILE_SETTINGS_ROUTE` | `profile-settings` | Health Connect and profile settings. |

### `AppNavGraph`

Source: `app/src/main/java/com/musfit/ui/AppNavGraph.kt`

Entrypoint:

```kotlin
@Composable
fun AppNavGraph()
```

Responsibilities:

- Owns `NavController`.
- Owns the app-level `AppNavigationStack` for bottom-tab history.
- Renders the bottom navigation bar.
- Starts at `AppDestination.Today.route`.
- Passes tab-switch callbacks into `TodayScreen`.
- Pushes Today/Food/Training/Profile visits onto the app stack and consumes system/gesture back on bottom routes to pop back through that sequence.
- Holds one-shot scanner return values:
  - `scannedBarcode: String?`
  - `scannedLabelText: String?`
- Routes scanner results back into `FoodScreen`; scanner routes keep the Food tab selected while normal route back exits the scanner first.

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

Today is the dashboard and cross-feature summary. It displays the configurable metric carousel, a local readiness estimate when Health Connect has enough recovery signals, deterministic coaching cues, and a dashboard editor.

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
| `readiness` | Optional header chip with MusFit's local readiness estimate from sleep, HRV RMSSD, and resting heart rate. |
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
- `HealthRepository.observeDailySummaries(date.minusDays(7), date)` for readiness baselines.
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
| `RecipeBrowser` | Recipe discovery and saved-recipe planning to a selected date and meal. |
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

- `openRecipeBrowser()`, `onRecipeBrowserMealChanged(value)`, `goToPreviousRecipeBrowserDay()`, `goToNextRecipeBrowserDay()`, `goToTodayRecipeBrowserDay()`, `planRecipe(recipeId)`
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
| `Home` | Training landing surface with quick actions for starting an empty workout, creating a routine, and opening the full-page routine Library. It renders user-saved routines only; pre-saved starter routines stay in the Library. |
| `Library` | Folder-grouped saved and starter routine list, user-configurable routine folders, routine descriptions and muscle chips, starter/custom routine editor, duplicate/delete/start routine actions, and primary new-folder CTA. |
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
| `selectedSection` | Current Training tab; defaults to `Home`. |
| `routines` | Routine summaries. |
| `homeRoutines` | Non-starter routine summaries shown on the Training home tab. |
| `visibleRoutines` | Routine summaries shown in the folder-grouped Library list. |
| `routineFolders` | User-configurable routine folders used to group routine cards. |
| `routineProgramOptions` | Legacy field retained empty; program/type chips are no longer used for routine organization. |
| `selectedRoutineProgram` | Legacy field retained null; routine folders do not hide routines by type. |
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
| `routineEditor` | Routine create/edit state, including folder assignment, per-exercise rest seconds, and saved set plans. |
| `routineFolderEditor` | Inline folder create/rename/delete state. |
| `routineExercisePickerOpen`, picker selected ids, query, muscle filter, equipment filter | Full-screen routine exercise picker state. |
| `routineLibraryPageOpen` | Route-like full-page routine Library opened from the Training home CTA. |
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
- `openRoutineLibraryPage()`, `closeRoutineLibraryPage()`
- `resumeActiveWorkout()`
- `closeActiveWorkoutRoute()`

Routine management:

- `onRoutineProgramFilterChanged(value)`
- `openRoutineEditor(routineId)`
- `closeRoutineEditor()`
- `onRoutineNameChanged(value)`, `onRoutineNotesChanged(value)`, `onRoutineEditorFolderNameChanged(value)`
- `openRoutineExercisePicker()`, picker filter/selection handlers, `confirmRoutineExercisePicker()`, `closeRoutineExercisePicker()`
- `addRoutineExercise(exerciseId)`, `addRoutineExercises(exerciseIds)`, `removeRoutineExercise(index)`
- `onRoutineExerciseTargetSetsChanged(index, value)`, `onRoutineExerciseTargetRepsChanged(index, value)`
- `onRoutineExerciseRestSecondsChanged(index, value)`
- `addRoutineExerciseSet(index)`, `removeRoutineExerciseSet(index, setIndex)`
- `onRoutineExerciseSetTypeChanged(exerciseIndex, setIndex, setType)`, `onRoutineExerciseSetRepsChanged(...)`, `onRoutineExerciseSetWeightChanged(...)`
- `moveRoutineExerciseUp(index)`, `moveRoutineExerciseDown(index)`
- `saveRoutineEditor()`, `duplicateRoutine(routineId)`, `deleteRoutine(routineId)`
- `openRoutineFolderEditor(folderId)`, `closeRoutineFolderEditor()`, `onRoutineFolderNameChanged(value)`, `saveRoutineFolderEditor()`, `deleteRoutineFolder(folderId)`
- `assignRoutineToFolder(routineId, folderId)` where `folderId = null` moves the routine back to `My routines`

Exercise library:

- `onExerciseSearchQueryChanged(value)`
- `onExerciseMuscleFilterChanged(value)`, `onExerciseEquipmentFilterChanged(value)`, `clearExerciseFilters()`
- `openExerciseDetail(exerciseId)`, `closeExerciseDetail()`
- `onExerciseDetailNotesChanged(value)`, `saveExerciseDetailNotes()`
- `openCustomExerciseEditor()`, `closeCustomExerciseEditor()`
- `onCustomExerciseNameChanged(value)`, `onCustomExerciseCategoryChanged(value)`, `onCustomExerciseEquipmentChanged(value)`, `onCustomExerciseTargetMusclesChanged(value)`
- `saveCustomExercise()`

Workout flows:

- `startRoutine(routineId)`
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
| `TrainingHomeContent` | active workout presence and non-starter home routines | Start empty workout, create routine, open full-page routine Library, start/open user routine callbacks. |
| `TrainingRoutineContent` | visible routines, folders, folder editor state | Library folder create/edit/delete, routine descriptions and muscle chips, drag/drop and menu-based routine assignment, start, edit, duplicate, delete routine callbacks. |
| `TrainingRoutineEditor` | `RoutineEditorState`, exercises, folders | Edit routine metadata, folder assignment, exercise rest seconds, and set plans. |
| `RoutineExercisePickerPage` | exercises, current routine ids, selected ids, query/filter state | Full-screen search/filter/multi-select picker for adding exercises to a routine after confirmation. |
| `TrainingActiveWorkoutContent` | `ActiveWorkoutDetail`, exercises, `RestTimerState`, active workout notes draft, Training tool setting drafts | Set edits, set type changes, set reorder, workout notes, add exercise/set, timer controls, Training tool save, warm-up suggestions, PR/plate display, superset create/dissolve, finish/discard. |
| `TrainingHistoryContent` | `history`, `historyOverview`, selected detail | Render month overview, consistency metrics, open/close workout detail, completed-workout recap, and completed supersets with grouped sections and A/B labels. |
| `TrainingProgressContent` | exercises, selected id, progress, progress analytics | Select exercise and render all-training analytics plus selected-exercise trend/PR/history display. |

Exercise library detail behavior:

- Tapping an exercise row opens an inline detail card in the Exercises section.
- Detail displays equipment, category, library/custom state, primary muscles, secondary muscles, original MusFit instructions when available, and editable local notes.
- Saving notes trims blank space and stores notes locally through `TrainingRepository.updateExerciseLocalNotes`; blank notes are stored as null.
- Starter exercises have original MusFit instruction copy. Custom exercises use their target muscles as primary muscles and start with no instructions.

Routine organization behavior:

- Starter routines still carry legacy local `programName` and tags for metadata/backward compatibility, but visible organization is folder-based.
- The Library section and the full-page Browse routines route group routine cards by `RoutineFolder`; routines without a folder appear under `My routines`.
- Routine rows show summary notes when present, fall back to a pre-saved label for starter routines, and surface target muscle chips from routine exercise metadata.
- Users can create, rename, and delete folders locally. Deleting a folder unassigns its routines rather than deleting those routines.
- Users can drag the routine handle in Library onto folder chips, or use the row move menu, to assign routines. Dropping or selecting `My routines` clears a folder assignment.
- Duplicating a starter/foldered routine creates an editable non-starter local copy and preserves folder assignment plus legacy metadata.
- The routine editor uses a full-screen exercise picker for search/filter/multi-select. Exercises are only added after the user confirms with OK.
- Saved routine exercises support per-exercise rest seconds plus row-level set type, target reps, and optional target weight. Starting a routine materializes those saved set rows into the active workout.

Training dashboard behavior:

- The home dashboard and home routine rows use only `homeRoutines`, so pre-saved starter routines do not appear on the Home tab.
- The next-routine suggestion is a deterministic local heuristic from the user-saved home routine list.
- The dashboard keeps the existing active workout resume banner and weekly header behavior intact.

Active workout UI behavior:

- Full-screen route-like surface opens when an active workout is resumed or started.
- Top bar shows elapsed time, completed sets, total volume, `Finish`, and a discard overflow action.
- Completing a valid active set starts the deterministic rest timer using that set's planned rest seconds when present, otherwise the saved default duration; the user can pause, resume, skip, or adjust by 15 seconds.
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
- Routine organization is folder-based; there is no separate multi-week program calendar or progression schedule yet.
- Routine-level per-exercise rest is implemented; there are no advanced warm-up preference profiles yet.
- Supersets are intentionally pair-oriented through "make with next"; there is no arbitrary multi-exercise superset editor yet.
- History overview is current-month/current-week focused; there is no drillable multi-month calendar or per-day workout filter yet.
- Progress analytics are local and deterministic; there is no advanced chart filtering, period picker, or stored analytics cache yet.
- Dashboard routine suggestions are deterministic and local; there is no adaptive progression or fatigue model yet.
- Post-workout recap is local and private only; there is no social sharing, public feed, or exported recap image.

## Profile Settings Health Connect

Source:

- `app/src/main/java/com/musfit/ui/profile/ProfileSettingsScreen.kt`
- `app/src/main/java/com/musfit/ui/profile/ProfileSettingsViewModel.kt`

Route: `profile-settings`

Composable entrypoint:

```kotlin
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
)
```

### Purpose

Health Connect controls live in Profile Settings. They report availability and permissions, launch permission requests, import recent health summaries, and export the latest workout.

### ViewModel State

`ProfileSettingsViewModel` exposes:

```kotlin
val state: StateFlow<ProfileSettingsUiState>
```

`ProfileSettingsUiState` health fields:

| Field | Purpose |
| --- | --- |
| `availabilityLabel` | Human readable Health Connect availability. |
| `grantedPermissionCount` | Count of currently granted permissions. |
| `requestablePermissionCount` | Count of permissions the app can request. |
| `requestablePermissions` | Permission strings for launcher. |
| `canRequestPermissions` | Enables the permission button. |
| `message` | User-facing status or result message. |
| `isHealthConnectSyncing` | True while the recent Health Connect import is in flight. |

### ViewModel Actions

- `refreshStatus()`
- `importToday()`
- `syncRecentHealthData()`
- `exportLatestWorkout()`

### Data Sources

Profile Settings uses `HealthRepository`, which delegates to `HealthConnectGateway` and local Room storage through `HealthDao`.

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
