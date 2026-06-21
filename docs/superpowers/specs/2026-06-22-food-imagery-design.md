# MusFit Food UI — Slice 3: Food Imagery

**Date:** 2026-06-22
**Status:** Approved design pending review, then plan
**Area:** Food data + UI (`com.musfit.data.*`, `com.musfit.ui.food`)
**Builds on:** Slice 1 (tokens) and Slice 2 (meal-card structure).

## Background

The Food UI uses category icons everywhere. The agreed north-star shows real product photos on logged
foods, like Lifesum. The network layer is already prepared — the Open Food Facts provider populates
`ProductLookupResult.Found.imageUrl` for both barcode lookups and search hits — but that URL is dropped: it is
never persisted, and nothing renders it. This slice threads the image URL from the provider into storage and
the UI, and loads photos with Coil, falling back to category art when there is no image.

This is the third and final slice of the Food UI redesign.

## Decision (confirmed)

**Network images + category fallback.** Persist the Open Food Facts image URL (a Room migration), load photos
with Coil, and show category art for foods without an image (manual foods, quick logs).

## Goals

1. Persist `imageUrl` on saved foods (`FoodEntity`) via a Room migration.
2. Capture the provider's `imageUrl` when a scanned or searched product is saved to the food database.
3. Surface `imageUrl` on logged diary entries and saved foods through the repository and ViewModel.
4. Render thumbnails with Coil where individual foods appear, with a tasteful category/placeholder fallback.

## Non-goals

- Storing image bytes locally (Coil's disk cache handles caching).
- Images for foods with no source (manual/quick) — they show the fallback.
- Meal-section card art — those keep the Slice 2 category icons (they represent a meal, not a single food).
- Recipe/template thumbnails — out of scope for this slice.

## Design

### Dependency

Add `io.coil-kt:coil-compose` (Coil 2.x) via `libs.versions.toml` + `app/build.gradle.kts`. Confirm the
manifest already declares `android.permission.INTERNET` (the app already calls Open Food Facts via Retrofit;
add it if somehow absent).

### Data model + migration

- `FoodEntity` gains `@ColumnInfo... val imageUrl: String? = null`.
- **`MIGRATION_14_15`**: `ALTER TABLE foods ADD COLUMN imageUrl TEXT`. Register it in `DatabaseModule` and bump
  `MusFitDatabase` `version = 15`. The schema is exported (`exportSchema = true`); commit the generated
  `app/schemas/com.musfit.data.local.MusFitDatabase/15.json`.

### Capture

In the repository, the save paths that originate from an Open Food Facts `Found` already build a `FoodEntity`:
- scanned product → database (`saveScannedProductToDatabase`),
- online search import (`saveOnlineFoodResult`),
- log-and-save flows that persist a new food.

Each carries the `Found.imageUrl` (or `FoodLogInput.imageUrl`) onto the created `FoodEntity.imageUrl`. Manual
food creation leaves it null. `FoodLogInput` and the saved-food upsert input gain an optional `imageUrl`.

### Read

- The diary query already joins `foods`; add `foods.imageUrl AS imageUrl` and a matching
  `FoodDiaryEntryRow.imageUrl: String?`.
- Saved-food reads return `FoodEntity`, so `imageUrl` is available once the column exists.
- `SavedFoodItem` and `FoodDiaryEntry` (repository models) gain `imageUrl: String?`.

### Expose

`SavedFoodUiState` and `FoodMealEntryUiState` (the diary entry row state) gain `imageUrl: String?`, mapped from
the repository models in `FoodViewModel`.

### Render

A reusable composable:

```kotlin
@Composable
private fun FoodThumb(imageUrl: String?, fallback: ImageVector, modifier: Modifier = Modifier)
```

renders a rounded-square (`MusFitTheme.shapes.medium`, `surfaceVariant` background). When `imageUrl` is non-null it
shows Coil's `AsyncImage` (cropped to fill) with the fallback icon as `placeholder`/`error`; otherwise it shows
the fallback icon tinted `brand`. Use `FoodThumb` in:
- `DiaryEntryRow` (logged foods in meal cards and meal detail) — fallback `Icons.Outlined.Restaurant`.
- `SavedFoodDatabaseRow` and the `FoodDatabasePreview` rows — fallback `Icons.Outlined.Restaurant`.
- `FoodDetailPanel` — a larger thumb.
- The barcode/online result rows already use `FoodAvatar`; swap to `FoodThumb` so scanned items preview their photo.

## Testing & verification

- **Migration test** (Robolectric, `MusFitDatabaseTest`): open at v14 with a `foods` row, run `MIGRATION_14_15`,
  assert the row survives and `imageUrl` is queryable (null).
- **Repository round-trip** (`LocalFoodRepositoryTest`): saving a food with an `imageUrl` reads back with it set;
  a logged entry from that food exposes the `imageUrl`.
- `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug` green.
- Install; scan a barcode (or import an online food) with a photo and confirm the thumbnail renders on the diary
  row and in the database; confirm manual/quick foods show the category fallback. Screenshot.

## Risks & mitigations

- **Room migration correctness** — a missing/incorrect migration crashes existing installs. Mitigate with the
  explicit `ALTER TABLE`, the migration test, and committing the exported `15.json` (Room verifies the schema
  against it at build time).
- **Network image failures / slowness** — Coil handles errors and async loading; the fallback icon covers
  load failures and missing URLs, so rows never render blank.
- **OFF image URL availability** — many products lack images; the fallback makes this graceful.

## Definition of done

- `FoodEntity.imageUrl` + `MIGRATION_14_15`; DB at version 15; `15.json` committed; migration test passes.
- Provider image URL persists on saved/scanned/imported foods and is exposed on saved foods and diary entries.
- `FoodThumb` renders OFF photos via Coil with a category fallback in diary rows, the database, food detail,
  and barcode/online results.
- Build gate green; existing tests pass; round-trip + migration tests added; screenshots captured.
