# MusFit Data Model Ownership

This document is a low-drift map of who owns each model boundary and persisted
table group. It intentionally does not duplicate the live Room version, entity
fields, indexes, foreign keys, repository method lists, or UI-state catalogs.

For exact truth, read:

- `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt` for the registered
  entities, DAO accessors, and current database version;
- `app/src/main/java/com/musfit/data/local/entity/` plus the newest JSON under
  `app/schemas/com.musfit.data.local.MusFitDatabase/` for columns, indexes,
  foreign keys, defaults, and nullability;
- `app/src/main/java/com/musfit/data/local/dao/` for SQL, transactions, and DAO
  projection rows;
- `app/src/main/java/com/musfit/data/repository/` for feature-facing data
  contracts and persistence orchestration;
- `app/src/main/java/com/musfit/core/di/DatabaseModule.kt` and the other
  `core/di` modules for migrations, construction, and boundary bindings;
- the current [architecture audit](app-architecture-audit-2026-07-10.md) and
  [remediation backlog](architecture-remediation-backlog-2026-07-10.md) for
  known ownership or dependency defects.

## Ownership Rules

| Boundary | Owner | Responsibility |
| --- | --- | --- |
| Room storage | `data/local/entity` and `data/local/dao` | Persisted entities, SQL, transactions, and query-specific projection rows. These types are storage details. |
| Repository | `data/repository` | Feature-facing inputs, outputs, enums, and interfaces consumed by ViewModels. Local implementations map storage, remote, domain, and integration models. |
| UI | `ui/<feature>` | `*UiState`, editor state, presentation enums, formatted labels, and navigation-only state. UI models are not persistence contracts. |
| Domain | `domain` | Android-free models, calculators, parsers, and deterministic rules. Domain code must not depend on Compose, Room, Retrofit, or Android integration types. |
| Remote | `data/remote` | Wire DTOs, API interfaces, transport results, and provider/client implementations for Food, identity, and coach endpoints. Wire models should not leak into UI contracts. |
| Integration | `integrations/healthconnect` | Health Connect gateway, platform record mapping, and import/export boundary models. Repositories decide how those results affect local state. |
| Secrets | `AiCoachSecretStore` implementation | Account-keyed, runtime-entered AI credentials live outside Room in an Android-Keystore-backed store. Room stores connection metadata and a presence flag that is reconciled against the runtime store before display or use. No variant compiles an API-key field or fallback. |

The intended conversion paths are:

```text
Room entity / DAO projection -> repository model -> UI state
remote DTO -> provider/client result -> repository model -> UI state
repository or UI input -> pure domain logic -> repository/UI result
Health Connect record <-> integration gateway <-> repository model
```

These are ownership targets, not a claim that every current dependency is
already clean. Do not expand a cross-layer leak identified by the architecture
audit.

## Room Table And DAO Map

`MusFitDatabase` is the authoritative entity and DAO registry. The groups below
describe current ownership without restating fields.

| Area | DAO and entity source | Persisted tables | Repository boundary |
| --- | --- | --- | --- |
| Account identity/session | `AccountDao`; `AccountEntities.kt` | `accounts`, `account_session` | `AccountRepository` owns local identities and the active-account pointer. `ExternalAuthRepository` obtains provider identity; it does not turn Room into a cloud-sync store. |
| Food | `FoodDao`; `FoodEntities.kt` | `foods`, `food_servings`, `meals`, `meal_definitions`, `meal_items`, `barcode_products`, `food_goals`, `quick_calorie_presets`, `meal_templates`, `meal_template_items`, `recipes`, `recipe_ingredients`, `shopping_list_items`, `water_entries`, `food_health_connect_sync` | `FoodRepository` owns diary, saved-food, planning, recipe/template, shopping, water, goal, and Food Health Connect sync models. |
| Training | `TrainingDao`; `TrainingEntities.kt` | `exercises`, `exercise_notes`, `routines`, `routine_folders`, `routine_exercises`, `routine_exercise_sets`, `workout_sessions`, `workout_sets`, `training_settings` | `TrainingRepository` owns exercise, routine, active-workout, history, progress, and settings models. Immutable bundled exercise definitions remain shared catalog rows; custom definitions and the separate notes overlay are active-account owned. `ExerciseDatasetProvider` is the exercise-catalog ingestion boundary. |
| Health | `HealthDao`; `HealthEntities.kt` | `body_metrics`, `daily_health_summaries`, `health_connect_sync_state`, `health_connect_export_records` | `HealthRepository` owns active-account Health Connect refresh/export state. The export ledger persists stable client IDs, monotonic versions, payload fingerprints, and provider IDs per account/local entity. `ProfileRepository` uses the same active-account body metrics for profile trends. |
| Profile/settings | `ProfileDao`; `ProfileEntities.kt` | `user_profile`, `app_settings` | `ProfileRepository` owns account-reactive profile, body-goal, and app-setting models. |
| Cross-cutting goals | `UserGoalsDao`; `UserGoalsEntity.kt` | `user_goals` | `GoalsRepository` owns Today goals that are separate from Food nutrition goals. |
| AI coach settings | `AiCoachDao`; `AiCoachSettingsEntity.kt` | `ai_coach_settings` | `AiCoachRepository` owns provider/agent connection settings. `AiCoachSecretStore` owns the corresponding runtime credential outside Room. |
| AI coach chat | `AiCoachChatDao`; `AiCoachChatEntities.kt` | `ai_coach_threads`, `ai_coach_chat_messages` | `AiCoachChatRepository` owns account/provider-scoped thread and message models and calls the remote `CoachCompletionClient`. |
| Coach feed and Today pins | `CoachDao`; `CoachMessageEntity.kt`, `DashboardPinEntity.kt` | `coach_messages`, `dashboard_pins` | `CoachRepository` owns active-account deterministic coach-feed messages, dismissal/read state, retention, and Today carousel pin order. |

DAO projection rows belong beside the SQL that produces them. They are read
shapes, not additional tables and not stable repository APIs. Repositories must
map them before exposing data to ViewModels.

## Account Ownership Contract

All persisted user-specific feature data is scoped to the active account.
Food, Training, Health summaries/body metrics/sync state, profile/settings,
cross-cutting goals, AI settings/chat, coach-feed messages, and dashboard pins
must be read and mutated through an explicit owner. AI secrets are keyed by
account outside Room. Immutable bundled reference definitions may remain shared
only where their repository and schema contract says so; Training's starter
exercise definitions are the current shared example, while their notes and all
user-created definitions remain account owned.

Any new owned table requires an account-leading key or index, a legacy-row
mapping, cascade/deletion policy, repository scoping, and two-account tests.
Do not infer a shared contract merely because a row originated from seed data.

## Non-Room Model Boundaries

### Repository and UI

Repository interfaces are the feature boundary. Their input/output models may
aggregate several tables, a DAO projection, remote data, or a domain result.
ViewModels then translate those contracts into immutable observable UI state.
Composables should not depend directly on Room entities or transport DTOs.

The current repository set includes account/auth, Food, Training, Health,
Profile, Goals, AI coach settings, AI coach chat, coach feed/pins, and exercise
dataset ingestion. Read the interface source for exact methods and models rather
than copying them into documentation.

### Domain

Pure models and logic live under `domain`, including nutrition and training
calculators, OCR parsing, Today metrics/readiness/goals, coach rules, profile
target calculation, and Health Connect-neutral status/import models. Keep these
models independent of how data is stored, rendered, or transported.

### Remote

- `data/remote/food` owns Open Food Facts API DTOs and the
  `FoodProductProvider` transport boundary.
- `data/remote/auth` owns GitHub OAuth wire DTOs and API calls; repositories map
  provider identity into the local account contract.
- `data/remote/coach` owns OpenAI-compatible/Hermes request-response DTOs and the
  `CoachCompletionClient` implementation.

Remote responses are untrusted transport data. Normalize and map them before
persistence or UI exposure.

### Health Connect integration

`HealthConnectGateway`, its payload/result types, record mapper, and Android
manager live under `integrations/healthconnect`. Repository models decide what
the app stores and displays; platform record types should remain at the
integration boundary. Daily reads return metric-aware complete, partial, empty,
unavailable, or failure results. `HealthRepository` uses the successful-metric
set to clear confirmed-empty fields, preserve only failed/ungranted fields, and
atomically persist summaries, body metrics, and sync state. Cancellation is
control flow and must never be converted to a result or stale success.
Recent imports use one inclusive local-date range. Calendar-period aggregates
retain 23/24/25-hour day semantics, raw record types traverse every page token,
and results are still classified and reconciled independently per local day.

For MusFit-authored Health Connect records, local MusFit entities remain the
source of truth and `health_connect_export_records` is the deletion allowlist.
Deletion uses only the stable client IDs in that account-owned ledger; it never
scans or deletes manual records or records written by another app. Provider
confirmation removes the matching ledger row, failed record-type batches stay
pending for retry, and successful retries are no-ops. An explicit Food sync
reconciles removed meal aggregates and zeroed hydration. Account erasure can use
the same active-account cleanup boundary for workouts, nutrition, and hydration.
Successful or partial Weight/BodyFat imports also reconcile the completed local
day against cached `source = health_connect` body-metric IDs. Provider-deleted
IDs are removed in the same Room transaction as summary/body/sync-state writes.
Failed or ungranted metric types preserve their cached rows and the sync state
marks them stale through the visible failure without advancing last success.
Manual body measurements are never candidates for provider reconciliation.

## Schema And Migration Contract

For every persisted schema change:

1. Derive the current version and entity set from `MusFitDatabase.kt`; never copy
   the number into living prose.
2. Change the entity/DAO definition, increment the database version, add the
   exact `MIGRATION_x_y` in `DatabaseModule.kt`, and register it in
   `addMigrations(...)`.
3. Generate and commit the next exported schema JSON. Keep prior schema files;
   they are migration inputs and history.
4. Add a focused migration test under `app/src/test/java/com/musfit/data/local/`
   and repository/relationship tests for behavior affected by the new shape.
5. Preserve existing rows, foreign-key relationships, defaults, and indexes.
   There is no destructive-migration fallback.
6. Run the repository workflow contract and full debug verification before
   publication.

Current Food and Training/AI relationship-bearing parent writers use true
upsert/update semantics with relationship-preservation tests. Do not
reintroduce parent-row `OnConflictStrategy.REPLACE`: it can delete dependent
rows or reject edits through foreign-key behavior. Any retained `REPLACE` must
be relationship-free, intentional, and tested.

When a change adds, removes, or transfers ownership of a table or model boundary,
update this map in the same PR. Field-only changes belong in source and exported
schema, not in an exhaustive prose catalog.
