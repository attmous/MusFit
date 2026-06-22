# Local Account System Design

Date: 2026-06-22

## Objective

Add a local-first account system so MusFit data is linked to a real user identity on the device. This is an account and ownership foundation, not an online authentication feature.

The first version creates an active local account, moves existing singleton profile/settings/goals data behind that active account, and prepares the data model for later Food, Training, Health, and cloud sync ownership. MusFit remains Android-only, local-first, and uses the existing Kotlin, Jetpack Compose Material 3, Hilt ViewModel, Room, Kotlin Flow, and repository patterns.

## Approved Product Direction

- Build local account identity first.
- Do not add cloud login, remote sync, analytics, subscriptions, social features, or backend services in this slice.
- Do not show a fake email/password login screen without a real server behind it.
- Auto-create a default local account for existing installs so current data is preserved and the app still opens directly.
- Use the Profile tab as the account management surface.
- Thread account ownership into data incrementally, starting with profile, app settings, and Today goals.

## Current Code Context

- The Profile tab already exists and stores body profile state in `UserProfileEntity` (`user_profile`) with a singleton id of `"user"`.
- App settings are stored in `AppSettingsEntity` (`app_settings`) with a singleton id of `"app"`.
- Today goals are stored in `UserGoalsEntity` (`user_goals`) with a singleton id of `"default"`.
- Food goals are stored in `FoodGoalEntity` (`food_goals`) with a repository default id; Food diary, saved foods, recipes, templates, water, shopping list, and barcode cache are not account-scoped.
- Training routines, workout sessions, exercises, and sets are not account-scoped.
- Health summaries, body metrics, and Health Connect sync state are not account-scoped.
- `MusFitDatabase` is currently version 18 and uses exported Room schemas. Schema changes require a version bump, explicit migration, schema JSON, and tests.

The account work should follow the existing repository direction:

```text
Compose -> ViewModel -> Repository -> DAO -> Room
```

## User Experience

### First Launch And Existing Installs

The app must not block users with onboarding just because account storage exists. On startup, the repository ensures an active local account exists:

- If an active account exists, use it.
- If accounts exist but no session row exists, make the most recently updated account active.
- If no account exists, create a default local account named `You` and make it active.

Existing profile, settings, and goals data is migrated to the default local account. From the user's perspective, their current Profile, Today, Food, and Training surfaces keep working.

### Profile Account Card

The Profile tab gets a compact account identity card near the top:

- Avatar initial derived from display name.
- Display name.
- Optional email label when present.
- Local account badge.
- Edit action.

Editing account identity allows:

- Display name, required after trimming.
- Email, optional, stored as plain text for identity/contact labeling only.

The copy must be clear that this is a local account. Do not imply backup, sync, or password protection.

### Settings Surface

Profile Settings may include a small Account section:

- Active account name.
- Local-only status.
- Future sync row, disabled and labeled later.

Account switching can be supported in repository APIs in this design, but a polished multi-account switcher UI is a later slice unless it is needed for tests or migration verification.

## Account Data Model

Create two new Room entities.

### `AccountEntity`

Table: `accounts`

Fields:

- `id: String` primary key.
- `displayName: String` non-empty after trimming.
- `email: String?` optional.
- `remoteUserId: String?` optional future sync mapping.
- `createdAtEpochMillis: Long`.
- `updatedAtEpochMillis: Long`.

Indexes:

- Unique nullable index on `remoteUserId`.
- Non-unique index on `email`.

### `AccountSessionEntity`

Table: `account_session`

Fields:

- `key: String` primary key. The only current key is `"active"`.
- `activeAccountId: String` foreign key to `accounts.id`, cascade on delete is not used.
- `updatedAtEpochMillis: Long`.

The session row is a local device preference stored in Room so repositories can observe it consistently with the rest of app state.

## Active Account Boundary

Add an `AccountRepository` interface and `LocalAccountRepository` implementation.

Read APIs:

```kotlin
fun observeActiveAccount(): Flow<Account>
fun observeAccounts(): Flow<List<Account>>
```

Write APIs:

```kotlin
suspend fun ensureActiveAccount(): Account
suspend fun createAccount(displayName: String, email: String? = null): String
suspend fun updateActiveAccount(displayName: String, email: String?)
suspend fun switchAccount(accountId: String)
```

Repository-level model:

```kotlin
data class Account(
    val id: String,
    val displayName: String,
    val email: String?,
    val remoteUserId: String?,
)
```

Validation:

- `displayName.trim()` must not be blank.
- `email` is optional. If blank, store `null`.
- No password field exists.
- `remoteUserId` is not editable in UI.

`ensureActiveAccount()` is the only method allowed to auto-create the default account. Other read APIs should observe current database state and not create rows as a side effect.

## Ownership Migration Strategy

The implementation should be staged to keep each slice testable and reduce Room migration risk.

### Slice 1: Account Foundation

Add `accounts` and `account_session`, DAO, repository, DI binding, default account creation, Profile account card, and repository/ViewModel tests.

This slice does not yet change other tables, but it provides the active-account Flow that later repositories consume.

### Slice 2: Profile, Settings, And Today Goals Ownership

Add `accountId` to:

- `user_profile`
- `app_settings`
- `user_goals`

Migration behavior:

- Add `accountId` with default `local-default`.
- Create the `local-default` account and active session if missing.
- Preserve existing singleton rows by assigning them to `local-default`.
- Re-key or query rows by `accountId` instead of fixed ids.

Repository behavior:

- `LocalProfileRepository` observes active account and loads profile/settings for that account.
- `LocalGoalsRepository` observes active account and loads Today goals for that account.
- Saving profile, settings, or goals writes the active account id.

Per-account singleton rule:

- Tables with one row per account must use the account id as their real ownership key.
- The preferred implementation is to store `id = accountId` for `user_profile`, `app_settings`, `user_goals`, and later `food_goals`.
- If a table keeps a separate `id`, it must add a unique `accountId` index and all repository queries must use `accountId`.
- Fixed singleton ids must not prevent multiple local accounts from storing separate rows.

The old constants `PROFILE_ID = "user"`, `SETTINGS_ID = "app"`, and `DEFAULT_ID = "default"` should stop being the ownership boundary after this slice.

### Slice 3: Food Ownership

Add `accountId` to user-created Food tables:

- `food_goals`
- `foods`
- `food_servings` through `foods`
- `meals`
- `meal_items` through `meals`
- `meal_definitions`
- `quick_calorie_presets`
- `meal_templates`
- `recipes`
- `shopping_list_items`
- `water_entries`
- `food_health_connect_sync`

The barcode product cache can remain global because Open Food Facts results are provider cache data, but `linkedFoodId` may point to account-owned saved foods. If this causes cross-account ambiguity, later slices can make barcode links account-specific.

Repository behavior:

- Diary, saved-food, goal, water, recipes, templates, and shopping list queries filter by active account.
- Food goals follow the per-account singleton rule, using active account id instead of a fixed default goal id.
- Existing rows migrate to `local-default`.
- Starter foods can remain global only if the app treats them as immutable shared catalog items. If they are user-editable, they should be copied or owned by the active account.

### Slice 4: Training And Health Ownership

Add `accountId` to user-owned Training and Health tables:

- `routines`
- `workout_sessions`
- `body_metrics`
- `daily_health_summaries`
- `health_connect_sync_state`

Exercise library rows can stay global for starter exercises, but custom exercises need ownership before multi-account switching is exposed widely. A later training slice should decide whether `exercises.isCustom` rows get `accountId` while starter rows remain shared.

Health Connect sync state should remain tied to the active local account because Health Connect data belongs to the person using the device profile.

## UI Architecture

Add account state to `ProfileViewModel` rather than creating a new top-level tab.

`ProfileUiState` should gain:

```kotlin
val account: AccountUiState
val accountEditorOpen: Boolean
val accountNameInput: String
val accountEmailInput: String
val accountErrorMessage: String?
```

`AccountUiState`:

```kotlin
data class AccountUiState(
    val displayName: String = "You",
    val email: String? = null,
    val isLocalOnly: Boolean = true,
)
```

Profile actions:

```kotlin
fun openAccountEditor()
fun closeAccountEditor()
fun onAccountNameChanged(value: String)
fun onAccountEmailChanged(value: String)
fun saveAccount()
```

The account card should use existing Material 3 cards, typography, and compact Profile dashboard patterns. It should feel like part of the app, not a marketing or onboarding screen.

## Error Handling

- If account creation fails, keep the app on the current screen and show a concise error state in Profile when visible.
- If active account id points to a missing account, `ensureActiveAccount()` creates or selects a valid account and repairs the session row.
- If saving account identity receives a blank name, keep the editor open and show an inline validation message.
- If a repository observes account-owned data before an account is ensured, it should fall back to defaults or wait for the active-account Flow rather than throwing.

## Testing Strategy

Use TDD for every behavior change.

Account repository tests:

- Empty database: `ensureActiveAccount()` creates `You` and an active session.
- Existing account without session: `ensureActiveAccount()` selects an existing account.
- Updating active account trims display name and stores blank email as `null`.
- Switching account updates observed active account.
- Blank display name is rejected.

Database tests:

- DAO round-trips accounts and active session.
- Migration from version 18 to the new version creates account tables and preserves existing singleton profile/settings/goals rows in later ownership slices.

ViewModel tests:

- Profile state exposes active account display name and local-only badge state.
- Editing account identity delegates to `AccountRepository`.
- Blank display name keeps the editor open and exposes validation text.

Integration repository tests for ownership slices:

- Profile and settings reads follow the active account.
- Today goals reads follow the active account.
- Food diary queries do not show another account's meals after Food ownership is implemented.
- Training history does not show another account's sessions after Training ownership is implemented.

Full verification before claiming completion or pushing:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

## Acceptance Criteria

The first local account system is acceptable when:

- A fresh install auto-creates one local account and makes it active.
- Existing installs keep their current profile/settings/goals data attached to the default local account.
- The Profile tab shows a local account identity card.
- The user can edit display name and optional email locally.
- Account repository APIs can create, observe, update, and switch active accounts.
- Profile/settings/Today goals are scoped to the active account once ownership Slice 2 is implemented.
- No UI claims cloud backup, remote login, password protection, or cross-device sync.
- Room migrations, schema exports, repository tests, ViewModel tests, lint, and debug assembly pass.

## Non-Goals

- Cloud authentication.
- Password storage.
- Email verification.
- Cloud sync or backup.
- Account deletion with data cleanup.
- Full multi-account switching UX.
- Social features or sharing.
- Analytics or subscriptions.
