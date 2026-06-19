# MusFit Android MVP Design

Date: 2026-06-19

## Goal

Build the first version of MusFit: an Android-only fitness and nutrition tracker inspired by Lifesum and Hevy. The MVP tracks meals, foods, structured strength training, and selected Health Connect data. It avoids accounts and cloud sync in the first release so the app can become useful quickly while preserving a clean path for future integrations.

## Selected Approach

Use a native Android application written in Kotlin. The MVP includes Health Connect from the first usable release rather than postponing it. Local data remains the source of truth, while Health Connect and barcode lookup are integrations around that local model.

This was chosen because the app is Android-only and Health Connect/gadget integration is central to the product. Native Kotlin reduces friction for Android health permissions, Health Connect records, background sync behavior, and future device integrations.

## MVP Scope

The first release has four main areas:

- Today: daily summary with calories, macros, meals, workouts, Health Connect steps, body weight, active calories when available, and quick add actions.
- Food: manual food entry, saved foods, meal logging, barcode scan, and Open Food Facts lookup. Incomplete lookup results are editable before saving.
- Training: exercise library, reusable routines, active workout session, sets, reps, weight, rest timer, workout history, and simple personal records.
- Health: Health Connect status, permission management, last sync status, imported metrics, and clear read/write explanation.

The MVP excludes accounts, cloud sync, subscriptions, AI coaching, meal plans, social features, direct wearable cloud APIs, and advanced training progression programming.

## Architecture

Use one Android app module initially to keep the MVP simple. Organize code by responsibility:

- `ui`: Jetpack Compose screens, navigation, and reusable UI components.
- `domain`: app models, use cases, calculation logic, and interfaces.
- `data/local`: Room entities, DAOs, migrations, and local repositories.
- `data/remote`: barcode/product lookup clients and normalization.
- `integrations/healthconnect`: Health Connect availability, permissions, read/write mapping, and sync state.

Core libraries:

- Jetpack Compose and Navigation for UI.
- Room for meals, foods, workouts, routines, exercise history, body metrics, and sync metadata.
- Hilt for dependency injection.
- Retrofit with OkHttp for Open Food Facts API calls.
- Health Connect SDK for Android health data integration.
- WorkManager for supported background sync jobs.
- GitHub Actions for build, lint, tests, and debug APK artifacts.

## Local Data Model

The Room database is the source of truth. External systems provide imported or exported records but do not own app state.

Core entities:

- `Food`: canonical saved food with name, brand, default serving, calories, macros, and optional micronutrients.
- `FoodServing`: serving units and gram/ml conversion data.
- `Meal`: meal header with date, meal type, notes, and source metadata.
- `MealItem`: food quantity consumed within a meal.
- `BarcodeProduct`: barcode, lookup provider, raw provider metadata, quality flags, and linked saved food.
- `Exercise`: exercise library entry with category, equipment, target muscles, and custom flag.
- `Routine`: reusable workout template.
- `RoutineExercise`: exercise prescription inside a routine.
- `WorkoutSession`: logged training session with timestamps, notes, and Health Connect export metadata.
- `WorkoutSet`: set details including reps, weight, duration, distance where applicable, RPE, and completion state.
- `BodyMetric`: local or imported body weight and related measurements.
- `DailyHealthSummary`: cached daily health aggregates for fast Today rendering.
- `HealthConnectSyncState`: availability, permissions, last import/export timestamps, external IDs, and failure details.

## Health Connect Integration

Health Connect is part of the MVP but the app must still work when Health Connect is unavailable, denied, or partially permitted.

Initial read scope:

- Steps.
- Body weight.
- Active calories burned.
- Heart rate only if available and explicitly permitted.

Initial write scope:

- Logged workouts as Health Connect exercise sessions.

Nutrition write behavior:

- Health Connect supports nutrition records, but nutrition export should start behind an explicit setting because nutrition data is detailed and duplicate records across apps are easy to create.

Permission behavior:

- Request Health Connect permissions only from the Health tab or when the user enables sync.
- Show a clear read/write summary before asking.
- Preserve partial permission support. For example, if steps are allowed but body weight is denied, the Today screen still shows steps and hides body weight import.
- Never silently overwrite user-entered local data with imported health data.

External IDs and source metadata must be stored for imported and exported records so future gadget integrations can be added without mixing local, Health Connect, and device-cloud data.

## Barcode And Product Lookup

Open Food Facts is the MVP barcode provider because it is open and practical for early development.

Behavior:

- Scan or enter a barcode.
- Query Open Food Facts.
- Normalize product name, brand, serving size, calories, macros, and available micronutrients.
- Show an editable confirmation screen before saving.
- Mark incomplete or suspicious provider data so the UI can prompt the user to correct it.
- Cache saved products locally after confirmation.

The app should use a provider interface so additional data sources can be added later without changing Food screen logic.

## Training Experience

The training MVP should feel closer to Hevy than a generic note log.

Required behavior:

- Exercise library with built-in and custom exercises.
- Routine builder with ordered exercises.
- Active workout session from a routine or blank workout.
- Set logging for reps, weight, duration, distance, RPE, and notes where relevant.
- Rest timer.
- Workout history.
- Simple personal records such as best estimated one-rep max, heaviest weight, max reps, and total volume.

Advanced progression rules, fatigue/readiness, weekly planning, and social comparison are out of scope for MVP.

## GitHub Build And Delivery

Set up a standard native Android Gradle project with GitHub Actions.

Initial CI:

- Run Gradle build on pushes and pull requests.
- Run Kotlin compile, unit tests, lint, and basic static checks.
- Upload a debug APK artifact.

Release signing, Play Store publishing, and beta distribution automation are out of scope until the app is stable enough for external testers.

Branching recommendation:

- Use `main` as the stable branch.
- Use feature branches for app changes.
- Add required GitHub Actions checks before merging once the CI pipeline is stable.

## Privacy And Failure Handling

Health and nutrition data should stay on-device in the MVP.

Rules:

- No account requirement.
- No cloud sync.
- No analytics by default.
- No direct gadget cloud APIs in the first release.
- Barcode lookup works without an account.
- Health Connect is opt-in and permission-driven.
- The Health tab explains exactly what is read and written.
- If a remote product lookup fails, the user can still create food manually.
- If Health Connect is unavailable, the app remains a complete local meal and workout tracker.

## Testing Strategy

Focus tests on risky logic rather than broad UI coverage at the beginning.

Initial tests:

- Nutrition totals and macro calculations.
- Serving conversion and quantity handling.
- Barcode product normalization.
- Workout volume and personal record calculations.
- Health Connect mapping between local workout sessions and Health Connect exercise sessions.
- Repository tests with fake local and remote data sources.

Add UI smoke tests after the main screens settle.

## Future Expansion

Future milestones can add:

- Cloud sync and accounts.
- More food data providers.
- Nutrition export to Health Connect if testing shows it is useful and not duplicative.
- Direct gadget cloud APIs for devices that do not fully write to Health Connect.
- Wear OS companion support.
- Meal planning.
- Training progression rules.
- Readiness, recovery, and coaching features.

## References

- Android Health Connect overview: https://developer.android.com/health-and-fitness/health-connect
- Health Connect get started: https://developer.android.com/health-and-fitness/health-connect/get-started
- Health Connect data types: https://developer.android.com/health-and-fitness/health-connect/data-types
- NutritionRecord API reference: https://developer.android.com/reference/android/health/connect/datatypes/NutritionRecord
- ExerciseSessionRecord API reference: https://developer.android.com/reference/android/health/connect/datatypes/ExerciseSessionRecord
