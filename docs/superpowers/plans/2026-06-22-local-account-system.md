# Local Account System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first shippable local account system: a real active on-device account, Profile account editing, and active-account ownership for Profile, Settings, and Today goals.

**Architecture:** Add account storage in Room, expose it through `AccountRepository`, ensure a local account exists at app startup, and keep all account state behind repository Flow APIs. Then rekey the existing singleton Profile, Settings, and Today goals rows so `id = activeAccount.id`, which gives each local account its own rows without introducing extra per-account singleton columns.

**Tech Stack:** Android Kotlin, Jetpack Compose Material 3, Hilt, Room, Kotlin Flow/coroutines, Robolectric/JUnit tests, exported Room schemas.

## Global Constraints

- MusFit remains Android-only, local-first, and uses the existing Kotlin, Jetpack Compose Material 3, Hilt ViewModel, Room, Kotlin Flow, and repository patterns.
- Build local account identity first.
- Do not add cloud login, remote sync, analytics, subscriptions, social features, or backend services in this slice.
- Do not show a fake email/password login screen without a real server behind it.
- Auto-create a default local account for existing installs so current data is preserved and the app still opens directly.
- Use the Profile tab as the account management surface.
- Thread account ownership into data incrementally, starting with profile, app settings, and Today goals.
- Room schema changes require a version bump, explicit migration, schema JSON, and tests.
- Before Gradle/adb commands on Windows, run `. .\.superpowers\sdd\android-env.ps1`.
- Full verification before claiming completion: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain`.

---

## File Structure

- Create `app/src/main/java/com/musfit/data/local/entity/AccountEntities.kt` for `AccountEntity`, `AccountSessionEntity`, `ACTIVE_ACCOUNT_SESSION_KEY`, and `LOCAL_DEFAULT_ACCOUNT_ID`.
- Create `app/src/main/java/com/musfit/data/local/dao/AccountDao.kt` for account/session queries and writes.
- Modify `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt` to add account entities, expose `accountDao()`, and bump from version 18 to 19, then 20.
- Modify `app/src/main/java/com/musfit/core/di/DatabaseModule.kt` to provide `AccountDao` and add `MIGRATION_18_19` plus `MIGRATION_19_20`.
- Create `app/src/main/java/com/musfit/data/repository/AccountRepository.kt` for the repository model, interface, and local implementation.
- Modify `app/src/main/java/com/musfit/core/di/RepositoryModule.kt` to bind `AccountRepository`.
- Modify `app/src/main/java/com/musfit/MainActivity.kt` to call `ensureActiveAccount()` at startup.
- Modify `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt` to expose account state and account editor actions.
- Modify `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt` to show the account card and editor dialog.
- Modify `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt` so profile/settings rows use the active account id.
- Modify `app/src/main/java/com/musfit/data/repository/GoalsRepository.kt` so Today goals rows use the active account id.
- Modify `docs/architecture/data-models.md` to document accounts and update the database version.
- Test files:
  - Create `app/src/test/java/com/musfit/data/local/AccountDaoTest.kt`.
  - Create `app/src/test/java/com/musfit/data/repository/LocalAccountRepositoryTest.kt`.
  - Create `app/src/test/java/com/musfit/data/repository/LocalGoalsRepositoryTest.kt`.
  - Create `app/src/test/java/com/musfit/data/local/MusFitMigrationTest.kt`.
  - Modify `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt`.
  - Modify `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`.
  - Modify `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`.

---

### Task 1: Account Room Storage

**Files:**
- Create: `app/src/test/java/com/musfit/data/local/AccountDaoTest.kt`
- Create: `app/src/main/java/com/musfit/data/local/entity/AccountEntities.kt`
- Create: `app/src/main/java/com/musfit/data/local/dao/AccountDao.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Modify: `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt`
- Generated: `app/schemas/com.musfit.data.local.MusFitDatabase/19.json`

**Interfaces:**
- Produces: `AccountDao`, `AccountEntity`, `AccountSessionEntity`, `ACTIVE_ACCOUNT_SESSION_KEY`, `LOCAL_DEFAULT_ACCOUNT_ID`, and `MusFitDatabase.accountDao()`.
- Consumes: existing Room setup, Hilt `DatabaseModule`, exported schema setup in `app/build.gradle.kts`.

- [ ] **Step 1: Write the failing DAO test**

Create `app/src/test/java/com/musfit/data/local/AccountDaoTest.kt`:

```kotlin
package com.musfit.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountDao: AccountDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountDao = database.accountDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun accountDao_roundTripsAccountsAndActiveSession() = runTest {
        val account = AccountEntity(
            id = "account-1",
            displayName = "Ava",
            email = "ava@example.com",
            remoteUserId = null,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 2_000L,
        )
        val session = AccountSessionEntity(
            key = ACTIVE_ACCOUNT_SESSION_KEY,
            activeAccountId = account.id,
            updatedAtEpochMillis = 3_000L,
        )

        accountDao.upsertAccount(account)
        accountDao.upsertSession(session)

        assertEquals(listOf(account), accountDao.observeAccounts().first())
        assertEquals(account, accountDao.getAccount(account.id))
        assertEquals(account, accountDao.getActiveAccount())
        assertEquals(account, accountDao.observeActiveAccount().first())
        assertEquals(session, accountDao.getSession())
        assertNull(accountDao.getAccount("missing"))
    }
}
```

- [ ] **Step 2: Run the DAO test to verify it fails**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.AccountDaoTest" --no-daemon --console=plain
```

Expected: FAIL because `AccountDao`, `AccountEntity`, `AccountSessionEntity`, and `MusFitDatabase.accountDao()` do not exist.

- [ ] **Step 3: Add account entities**

Create `app/src/main/java/com/musfit/data/local/entity/AccountEntities.kt`:

```kotlin
package com.musfit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val ACTIVE_ACCOUNT_SESSION_KEY = "active"
const val LOCAL_DEFAULT_ACCOUNT_ID = "local-default"

@Entity(
    tableName = "accounts",
    indices = [
        Index("email"),
        Index(value = ["remoteUserId"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val remoteUserId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "account_session",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["activeAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("activeAccountId")],
)
data class AccountSessionEntity(
    @PrimaryKey val key: String,
    val activeAccountId: String,
    val updatedAtEpochMillis: Long,
)
```

- [ ] **Step 4: Add the account DAO**

Create `app/src/main/java/com/musfit/data/local/dao/AccountDao.kt`:

```kotlin
package com.musfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAtEpochMillis DESC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    suspend fun getMostRecentlyUpdatedAccount(): AccountEntity?

    @Query(
        """
        SELECT accounts.* FROM account_session
        INNER JOIN accounts ON accounts.id = account_session.activeAccountId
        WHERE account_session.`key` = :key
        LIMIT 1
        """,
    )
    fun observeActiveAccount(key: String = ACTIVE_ACCOUNT_SESSION_KEY): Flow<AccountEntity?>

    @Query(
        """
        SELECT accounts.* FROM account_session
        INNER JOIN accounts ON accounts.id = account_session.activeAccountId
        WHERE account_session.`key` = :key
        LIMIT 1
        """,
    )
    suspend fun getActiveAccount(key: String = ACTIVE_ACCOUNT_SESSION_KEY): AccountEntity?

    @Query("SELECT * FROM account_session WHERE `key` = :key LIMIT 1")
    suspend fun getSession(key: String = ACTIVE_ACCOUNT_SESSION_KEY): AccountSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: AccountSessionEntity)
}
```

- [ ] **Step 5: Register the DAO and bump the database to 19**

In `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`, add imports:

```kotlin
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
```

Add `AccountEntity::class` and `AccountSessionEntity::class` to the `entities` list.

Change:

```kotlin
version = 18,
```

to:

```kotlin
version = 19,
```

Add this abstract DAO method:

```kotlin
abstract fun accountDao(): AccountDao
```

- [ ] **Step 6: Add the Room migration and Hilt provider**

In `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`, add:

```kotlin
import com.musfit.data.local.dao.AccountDao
```

Add `MIGRATION_18_19` to the `.addMigrations` call after `MIGRATION_17_18`.

Add this provider:

```kotlin
@Provides
fun provideAccountDao(database: MusFitDatabase): AccountDao = database.accountDao()
```

Add this migration at the bottom of `DatabaseModule`:

```kotlin
internal val MIGRATION_18_19 =
    object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS accounts (
                    id TEXT NOT NULL PRIMARY KEY,
                    displayName TEXT NOT NULL,
                    email TEXT,
                    remoteUserId TEXT,
                    createdAtEpochMillis INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_email ON accounts(email)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_remoteUserId ON accounts(remoteUserId)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS account_session (
                    `key` TEXT NOT NULL PRIMARY KEY,
                    activeAccountId TEXT NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    FOREIGN KEY(activeAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_account_session_activeAccountId ON account_session(activeAccountId)")
            db.execSQL(
                """
                INSERT OR IGNORE INTO accounts (
                    id, displayName, email, remoteUserId, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    'local-default', 'You', NULL, NULL, 0, 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO account_session (`key`, activeAccountId, updatedAtEpochMillis)
                VALUES ('active', 'local-default', 0)
                """.trimIndent(),
            )
        }
    }
```

- [ ] **Step 7: Extend the database smoke test**

In `app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt`, add:

```kotlin
import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
```

Add a field:

```kotlin
private lateinit var accountDao: AccountDao
```

Initialize it in `setUp()`:

```kotlin
accountDao = database.accountDao()
```

Add the assertion to `database_exposesExpectedDaosAndGeneratedImplementation()`:

```kotlin
assertEquals(AccountDao::class.java, MusFitDatabase::class.java.getMethod("accountDao").returnType)
```

Add this test:

```kotlin
@Test
fun accountDao_roundTripFromDatabaseSmokeTest() = runTest {
    val account = AccountEntity(
        id = "account-smoke",
        displayName = "Smoke",
        email = null,
        remoteUserId = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 2L,
    )
    val session = AccountSessionEntity(
        key = ACTIVE_ACCOUNT_SESSION_KEY,
        activeAccountId = account.id,
        updatedAtEpochMillis = 3L,
    )

    accountDao.upsertAccount(account)
    accountDao.upsertSession(session)

    assertEquals(account, accountDao.observeActiveAccount().first())
}
```

- [ ] **Step 8: Run the DAO/database tests to verify they pass**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.AccountDaoTest" --tests "com.musfit.data.local.MusFitDatabaseTest" --no-daemon --console=plain
```

Expected: PASS. Room should generate `app/schemas/com.musfit.data.local.MusFitDatabase/19.json`.

- [ ] **Step 9: Commit Task 1**

Run:

```powershell
git add app/src/main/java/com/musfit/data/local/entity/AccountEntities.kt app/src/main/java/com/musfit/data/local/dao/AccountDao.kt app/src/main/java/com/musfit/data/local/MusFitDatabase.kt app/src/main/java/com/musfit/core/di/DatabaseModule.kt app/src/test/java/com/musfit/data/local/AccountDaoTest.kt app/src/test/java/com/musfit/data/local/MusFitDatabaseTest.kt app/schemas/com.musfit.data.local.MusFitDatabase/19.json
git commit -m "feat: add local account database schema"
```

---

### Task 2: Account Repository And Startup Initialization

**Files:**
- Create: `app/src/test/java/com/musfit/data/repository/LocalAccountRepositoryTest.kt`
- Create: `app/src/main/java/com/musfit/data/repository/AccountRepository.kt`
- Modify: `app/src/main/java/com/musfit/core/di/RepositoryModule.kt`
- Modify: `app/src/main/java/com/musfit/MainActivity.kt`

**Interfaces:**
- Consumes: `AccountDao`, `AccountEntity`, `AccountSessionEntity`, `LOCAL_DEFAULT_ACCOUNT_ID`, `ACTIVE_ACCOUNT_SESSION_KEY`.
- Produces: `Account`, `AccountRepository`, `LocalAccountRepository`, active-account creation/update/switch APIs, and app-start account initialization.

- [ ] **Step 1: Write the failing repository tests**

Create `app/src/test/java/com/musfit/data/repository/LocalAccountRepositoryTest.kt`:

```kotlin
package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalAccountRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var repository: LocalAccountRepository
    private var clockMillis = 10_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalAccountRepository(
            accountDao = database.accountDao(),
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun ensureActiveAccount_createsDefaultLocalAccountWhenEmpty() = runTest {
        val account = repository.ensureActiveAccount()

        assertEquals(LOCAL_DEFAULT_ACCOUNT_ID, account.id)
        assertEquals("You", account.displayName)
        assertNull(account.email)
        assertEquals(account, repository.observeActiveAccount().first())
        assertEquals(listOf(account), repository.observeAccounts().first())
    }

    @Test
    fun ensureActiveAccount_selectsMostRecentlyUpdatedAccountWhenSessionMissing() = runTest {
        database.accountDao().upsertAccount(
            AccountEntity(
                id = "older",
                displayName = "Older",
                email = null,
                remoteUserId = null,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L,
            ),
        )
        database.accountDao().upsertAccount(
            AccountEntity(
                id = "newer",
                displayName = "Newer",
                email = null,
                remoteUserId = null,
                createdAtEpochMillis = 3L,
                updatedAtEpochMillis = 4L,
            ),
        )

        val account = repository.ensureActiveAccount()

        assertEquals("newer", account.id)
        assertEquals("Newer", repository.observeActiveAccount().first().displayName)
    }

    @Test
    fun updateActiveAccount_trimsNameAndStoresBlankEmailAsNull() = runTest {
        repository.ensureActiveAccount()

        repository.updateActiveAccount(displayName = "  Ava  ", email = "   ")

        val account = repository.observeActiveAccount().first()
        assertEquals("Ava", account.displayName)
        assertNull(account.email)
    }

    @Test
    fun updateActiveAccount_rejectsBlankDisplayName() = runTest {
        repository.ensureActiveAccount()

        try {
            repository.updateActiveAccount(displayName = "   ", email = null)
            fail("Expected blank display name to be rejected")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Account name is required.", expected.message)
        }
    }

    @Test
    fun switchAccount_updatesObservedActiveAccount() = runTest {
        repository.ensureActiveAccount()
        val secondId = repository.createAccount(displayName = "Partner", email = "partner@example.com")

        repository.switchAccount(secondId)

        val active = repository.observeActiveAccount().first()
        assertEquals(secondId, active.id)
        assertEquals("Partner", active.displayName)
        assertEquals("partner@example.com", active.email)
    }
}
```

- [ ] **Step 2: Run the repository tests to verify they fail**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalAccountRepositoryTest" --no-daemon --console=plain
```

Expected: FAIL because `LocalAccountRepository`, `AccountRepository`, and `Account` do not exist.

- [ ] **Step 3: Implement `AccountRepository`**

Create `app/src/main/java/com/musfit/data/repository/AccountRepository.kt`:

```kotlin
package com.musfit.data.repository

import com.musfit.data.local.dao.AccountDao
import com.musfit.data.local.entity.ACTIVE_ACCOUNT_SESSION_KEY
import com.musfit.data.local.entity.AccountEntity
import com.musfit.data.local.entity.AccountSessionEntity
import com.musfit.data.local.entity.LOCAL_DEFAULT_ACCOUNT_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

data class Account(
    val id: String,
    val displayName: String,
    val email: String?,
    val remoteUserId: String?,
)

val DEFAULT_LOCAL_ACCOUNT = Account(
    id = LOCAL_DEFAULT_ACCOUNT_ID,
    displayName = "You",
    email = null,
    remoteUserId = null,
)

interface AccountRepository {
    fun observeActiveAccount(): Flow<Account>
    fun observeAccounts(): Flow<List<Account>>
    suspend fun ensureActiveAccount(): Account
    suspend fun createAccount(displayName: String, email: String? = null): String
    suspend fun updateActiveAccount(displayName: String, email: String?)
    suspend fun switchAccount(accountId: String)
}

class LocalAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
) : AccountRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(accountDao: AccountDao, clock: () -> Long) : this(accountDao) {
        this.clock = clock
    }

    override fun observeActiveAccount(): Flow<Account> =
        accountDao.observeActiveAccount().map { entity -> entity?.toAccount() ?: DEFAULT_LOCAL_ACCOUNT }

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAccounts().map { rows -> rows.map { it.toAccount() } }

    override suspend fun ensureActiveAccount(): Account {
        accountDao.getActiveAccount()?.let { return it.toAccount() }

        val existing = accountDao.getMostRecentlyUpdatedAccount()
        if (existing != null) {
            accountDao.upsertSession(
                AccountSessionEntity(
                    key = ACTIVE_ACCOUNT_SESSION_KEY,
                    activeAccountId = existing.id,
                    updatedAtEpochMillis = clock(),
                ),
            )
            return existing.toAccount()
        }

        val now = clock()
        val default = AccountEntity(
            id = LOCAL_DEFAULT_ACCOUNT_ID,
            displayName = "You",
            email = null,
            remoteUserId = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        accountDao.upsertAccount(default)
        accountDao.upsertSession(
            AccountSessionEntity(
                key = ACTIVE_ACCOUNT_SESSION_KEY,
                activeAccountId = default.id,
                updatedAtEpochMillis = now,
            ),
        )
        return default.toAccount()
    }

    override suspend fun createAccount(displayName: String, email: String?): String {
        val normalizedName = displayName.normalizedDisplayName()
        val now = clock()
        val id = UUID.randomUUID().toString()
        accountDao.upsertAccount(
            AccountEntity(
                id = id,
                displayName = normalizedName,
                email = email.normalizedEmail(),
                remoteUserId = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            ),
        )
        return id
    }

    override suspend fun updateActiveAccount(displayName: String, email: String?) {
        val active = ensureActiveAccount()
        val existing = accountDao.getAccount(active.id) ?: error("Active account is missing.")
        accountDao.upsertAccount(
            existing.copy(
                displayName = displayName.normalizedDisplayName(),
                email = email.normalizedEmail(),
                updatedAtEpochMillis = clock(),
            ),
        )
    }

    override suspend fun switchAccount(accountId: String) {
        val account = accountDao.getAccount(accountId)
            ?: throw IllegalArgumentException("Account not found.")
        accountDao.upsertSession(
            AccountSessionEntity(
                key = ACTIVE_ACCOUNT_SESSION_KEY,
                activeAccountId = account.id,
                updatedAtEpochMillis = clock(),
            ),
        )
    }
}

private fun AccountEntity.toAccount(): Account =
    Account(
        id = id,
        displayName = displayName,
        email = email,
        remoteUserId = remoteUserId,
    )

private fun String.normalizedDisplayName(): String =
    trim().takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Account name is required.")

private fun String?.normalizedEmail(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
```

- [ ] **Step 4: Bind the repository in Hilt**

In `app/src/main/java/com/musfit/core/di/RepositoryModule.kt`, add imports:

```kotlin
import com.musfit.data.repository.AccountRepository
import com.musfit.data.repository.LocalAccountRepository
```

Add this binding:

```kotlin
@Binds
@Singleton
abstract fun bindAccountRepository(repository: LocalAccountRepository): AccountRepository
```

- [ ] **Step 5: Ensure a local account at app startup**

In `app/src/main/java/com/musfit/MainActivity.kt`, add imports:

```kotlin
import androidx.lifecycle.lifecycleScope
import com.musfit.data.repository.AccountRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
```

Add the injected repository inside `MainActivity`:

```kotlin
@Inject
lateinit var accountRepository: AccountRepository
```

Add this near the start of `onCreate`, after `super.onCreate(savedInstanceState)`:

```kotlin
lifecycleScope.launch {
    accountRepository.ensureActiveAccount()
}
```

- [ ] **Step 6: Run repository tests**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalAccountRepositoryTest" --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 7: Run a compile-focused unit test group**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.AccountDaoTest" --tests "com.musfit.data.repository.LocalAccountRepositoryTest" --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 8: Commit Task 2**

Run:

```powershell
git add app/src/main/java/com/musfit/data/repository/AccountRepository.kt app/src/main/java/com/musfit/core/di/RepositoryModule.kt app/src/main/java/com/musfit/MainActivity.kt app/src/test/java/com/musfit/data/repository/LocalAccountRepositoryTest.kt
git commit -m "feat: add local account repository"
```

---

### Task 3: Profile Account Card And Editor

**Files:**
- Modify: `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`
- Modify: `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`

**Interfaces:**
- Consumes: `AccountRepository.observeActiveAccount()`, `AccountRepository.ensureActiveAccount()`, and `AccountRepository.updateActiveAccount(displayName, email)`.
- Produces: `AccountUiState`, Profile account editor state/actions, account card UI, and account edit dialog.

- [ ] **Step 1: Write failing ProfileViewModel tests**

In `app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt`, add imports:

```kotlin
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountRepository
```

Update every existing `ProfileViewModel` test construction so the first argument is `FakeAccountRepository()`. Example:

```kotlin
val viewModel = ProfileViewModel(FakeAccountRepository(), FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo())
```

Add these tests:

```kotlin
@Test
fun accountState_exposesActiveLocalAccount() = runTest {
    val accountRepository = FakeAccountRepository(
        initial = Account(
            id = "account-1",
            displayName = "Ava",
            email = "ava@example.com",
            remoteUserId = null,
        ),
    )

    val viewModel = ProfileViewModel(accountRepository, FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo())
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals("Ava", viewModel.state.value.account.displayName)
    assertEquals("ava@example.com", viewModel.state.value.account.email)
    assertEquals(true, viewModel.state.value.account.isLocalOnly)
}

@Test
fun saveAccount_updatesRepositoryAndClosesEditor() = runTest {
    val accountRepository = FakeAccountRepository()
    val viewModel = ProfileViewModel(accountRepository, FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo())
    dispatcher.scheduler.advanceUntilIdle()

    viewModel.openAccountEditor()
    viewModel.onAccountNameChanged("Ava")
    viewModel.onAccountEmailChanged("ava@example.com")
    viewModel.saveAccount()
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals("Ava", accountRepository.updatedName)
    assertEquals("ava@example.com", accountRepository.updatedEmail)
    assertEquals(false, viewModel.state.value.accountEditorOpen)
    assertEquals(null, viewModel.state.value.accountErrorMessage)
}

@Test
fun saveAccount_blankNameKeepsEditorOpenWithValidation() = runTest {
    val accountRepository = FakeAccountRepository()
    val viewModel = ProfileViewModel(accountRepository, FakeProfileRepository(), FakeHealthRepo(), FakeFoodGoalRepo())
    dispatcher.scheduler.advanceUntilIdle()

    viewModel.openAccountEditor()
    viewModel.onAccountNameChanged("   ")
    viewModel.saveAccount()
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(true, viewModel.state.value.accountEditorOpen)
    assertEquals("Account name is required.", viewModel.state.value.accountErrorMessage)
    assertEquals(null, accountRepository.updatedName)
}
```

Add this fake inside `ProfileViewModelTest`:

```kotlin
private class FakeAccountRepository(
    initial: Account = Account("local-default", "You", null, null),
) : AccountRepository {
    private val active = MutableStateFlow(initial)
    var updatedName: String? = null
    var updatedEmail: String? = null
    var ensured = false

    override fun observeActiveAccount(): Flow<Account> = active

    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(listOf(active.value))

    override suspend fun ensureActiveAccount(): Account {
        ensured = true
        return active.value
    }

    override suspend fun createAccount(displayName: String, email: String?): String = "created"

    override suspend fun updateActiveAccount(displayName: String, email: String?) {
        updatedName = displayName
        updatedEmail = email
        active.value = active.value.copy(displayName = displayName, email = email)
    }

    override suspend fun switchAccount(accountId: String) = Unit
}
```

- [ ] **Step 2: Run ProfileViewModel tests to verify they fail**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain
```

Expected: FAIL because `ProfileViewModel` does not accept `AccountRepository`, and `ProfileUiState` has no account editor state.

- [ ] **Step 3: Extend ProfileViewModel state and constructor**

In `app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt`, add imports:

```kotlin
import com.musfit.data.repository.Account
import com.musfit.data.repository.AccountRepository
import kotlinx.coroutines.flow.update
```

Add these state models before `ProfileUiState`:

```kotlin
data class AccountUiState(
    val displayName: String = "You",
    val email: String? = null,
    val isLocalOnly: Boolean = true,
)

private data class AccountEditorState(
    val open: Boolean = false,
    val nameInput: String = "",
    val emailInput: String = "",
    val errorMessage: String? = null,
)
```

Add these fields to `ProfileUiState`:

```kotlin
val account: AccountUiState = AccountUiState(),
val accountEditorOpen: Boolean = false,
val accountNameInput: String = "",
val accountEmailInput: String = "",
val accountErrorMessage: String? = null,
```

Change the constructor to:

```kotlin
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val healthRepository: HealthRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
```

Add this field:

```kotlin
private val accountEditorFlow = MutableStateFlow(AccountEditorState())
```

Add this `init` block:

```kotlin
init {
    viewModelScope.launch {
        runCatching { accountRepository.ensureActiveAccount() }
            .onFailure { messageFlow.value = it.message ?: "Could not prepare your local account." }
    }
}
```

Replace the public `state` combine with this five-flow combine:

```kotlin
val state: StateFlow<ProfileUiState> = combine(
    dataState,
    accountRepository.observeActiveAccount(),
    healthRepository.observeDailySummary(today),
    messageFlow,
    accountEditorFlow,
) { base, account, summary, message, editor ->
    base.copy(
        account = account.toUiState(),
        vitals = summary?.toVitals(),
        message = message,
        accountEditorOpen = editor.open,
        accountNameInput = editor.nameInput,
        accountEmailInput = editor.emailInput,
        accountErrorMessage = editor.errorMessage,
    )
}.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())
```

Add these methods:

```kotlin
fun openAccountEditor() {
    val account = state.value.account
    accountEditorFlow.value = AccountEditorState(
        open = true,
        nameInput = account.displayName,
        emailInput = account.email.orEmpty(),
    )
}

fun closeAccountEditor() {
    accountEditorFlow.value = AccountEditorState()
}

fun onAccountNameChanged(value: String) {
    accountEditorFlow.update { it.copy(nameInput = value, errorMessage = null) }
}

fun onAccountEmailChanged(value: String) {
    accountEditorFlow.update { it.copy(emailInput = value, errorMessage = null) }
}

fun saveAccount() {
    val editor = accountEditorFlow.value
    if (editor.nameInput.isBlank()) {
        accountEditorFlow.update { it.copy(errorMessage = "Account name is required.") }
        return
    }
    viewModelScope.launch {
        runCatching {
            accountRepository.updateActiveAccount(
                displayName = editor.nameInput.trim(),
                email = editor.emailInput.trim().takeIf { it.isNotBlank() },
            )
        }.onSuccess {
            accountEditorFlow.value = AccountEditorState()
        }.onFailure { error ->
            accountEditorFlow.update {
                it.copy(errorMessage = error.message ?: "Could not save account.")
            }
        }
    }
}
```

Add this mapper near the existing private mappers:

```kotlin
private fun Account.toUiState() =
    AccountUiState(
        displayName = displayName,
        email = email,
        isLocalOnly = remoteUserId == null,
    )
```

- [ ] **Step 4: Add the Profile account UI**

In `app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt`, add imports:

```kotlin
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
```

Inside the main `Column`, insert `AccountCard` before `IdentityCard`:

```kotlin
AccountCard(state = state, onEdit = viewModel::openAccountEditor)
```

Add this dialog call after the existing dialogs:

```kotlin
if (state.accountEditorOpen) {
    AccountEditDialog(
        name = state.accountNameInput,
        email = state.accountEmailInput,
        error = state.accountErrorMessage,
        onNameChange = viewModel::onAccountNameChanged,
        onEmailChange = viewModel::onAccountEmailChanged,
        onDismiss = viewModel::closeAccountEditor,
        onSave = viewModel::saveAccount,
    )
}
```

Add these composables in `ProfileScreen.kt` above `IdentityCard`:

```kotlin
@Composable
private fun AccountCard(state: ProfileUiState, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.account.displayName.accountInitial(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.account.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    state.account.email ?: "Local account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.account.isLocalOnly) {
                    AssistChip(onClick = {}, label = { Text("Local only") })
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit account")
            }
        }
    }
}

@Composable
private fun AccountEditDialog(
    name: String,
    email: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Stored on this device. Sync and sign-in are not enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
```

Add this helper near `identitySubtitle`:

```kotlin
private fun String.accountInitial(): String =
    trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Y"
```

- [ ] **Step 5: Run ProfileViewModel tests**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 6: Run a compile check**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.profile.ProfileViewModelTest" --tests "com.musfit.data.repository.LocalAccountRepositoryTest" --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 7: Commit Task 3**

Run:

```powershell
git add app/src/main/java/com/musfit/ui/profile/ProfileViewModel.kt app/src/main/java/com/musfit/ui/profile/ProfileScreen.kt app/src/test/java/com/musfit/ui/profile/ProfileViewModelTest.kt
git commit -m "feat: show local account in profile"
```

---

### Task 4: Active-Account Ownership For Profile, Settings, And Today Goals

**Files:**
- Modify: `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`
- Create: `app/src/test/java/com/musfit/data/repository/LocalGoalsRepositoryTest.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt`
- Modify: `app/src/main/java/com/musfit/data/repository/GoalsRepository.kt`
- Modify: `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`
- Modify: `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`
- Generated: `app/schemas/com.musfit.data.local.MusFitDatabase/20.json`

**Interfaces:**
- Consumes: `AccountRepository.observeActiveAccount()` and `AccountRepository.ensureActiveAccount()`.
- Produces: profile/settings/Today goals repository behavior keyed by active account id.

- [ ] **Step 1: Write failing ProfileRepository ownership test**

In `app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt`, add:

```kotlin
private lateinit var accountRepository: LocalAccountRepository
```

In `setUp()`, create `accountRepository` before `repository`:

```kotlin
accountRepository = LocalAccountRepository(
    accountDao = database.accountDao(),
    clock = { clockMillis += 1_000L; clockMillis },
)
```

Update the `LocalProfileRepository` constructor call to include:

```kotlin
accountRepository = accountRepository,
```

Add this test:

```kotlin
@Test
fun profileAndSettings_followActiveAccount() = runTest {
    val first = accountRepository.ensureActiveAccount()
    repository.saveProfile(
        UserProfile(
            sex = Sex.Male,
            birthDateEpochDay = 9_000L,
            heightCm = 182.0,
            activityLevel = ActivityLevel.Active,
            goalType = GoalType.Lose,
            goalPaceKgPerWeek = 0.5,
            goalWeightKg = 78.0,
        ),
    )
    repository.saveSettings(AppSettings(unitSystem = "metric", themeMode = "dark"))

    val secondId = accountRepository.createAccount(displayName = "Partner", email = null)
    accountRepository.switchAccount(secondId)

    assertNull(repository.observeProfile().first().sex)
    assertEquals("system", repository.observeSettings().first().themeMode)

    repository.saveProfile(
        UserProfile(
            sex = Sex.Female,
            birthDateEpochDay = 8_000L,
            heightCm = 170.0,
            activityLevel = ActivityLevel.Light,
            goalType = GoalType.Gain,
            goalPaceKgPerWeek = 0.25,
            goalWeightKg = 68.0,
        ),
    )

    accountRepository.switchAccount(first.id)
    assertEquals(Sex.Male, repository.observeProfile().first().sex)
    assertEquals("dark", repository.observeSettings().first().themeMode)

    accountRepository.switchAccount(secondId)
    assertEquals(Sex.Female, repository.observeProfile().first().sex)
}
```

- [ ] **Step 2: Write failing GoalsRepository ownership test**

Create `app/src/test/java/com/musfit/data/repository/LocalGoalsRepositoryTest.kt`:

```kotlin
package com.musfit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.musfit.data.local.MusFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalGoalsRepositoryTest {
    private lateinit var database: MusFitDatabase
    private lateinit var accountRepository: LocalAccountRepository
    private lateinit var repository: LocalGoalsRepository
    private var clockMillis = 20_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepository = LocalAccountRepository(
            accountDao = database.accountDao(),
            clock = { clockMillis += 1_000L; clockMillis },
        )
        repository = LocalGoalsRepository(
            userGoalsDao = database.userGoalsDao(),
            accountRepository = accountRepository,
            clock = { clockMillis += 1_000L; clockMillis },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun userGoals_followActiveAccount() = runTest {
        val first = accountRepository.ensureActiveAccount()
        repository.updateUserGoals(UserGoals(stepGoal = 8_000L, weeklySessionTarget = 3, targetWeightKg = 80.0))

        val secondId = accountRepository.createAccount(displayName = "Partner", email = null)
        accountRepository.switchAccount(secondId)

        assertEquals(UserGoals(), repository.observeUserGoals().first())

        repository.updateUserGoals(UserGoals(stepGoal = 12_000L, weeklySessionTarget = 5, targetWeightKg = 70.0))

        accountRepository.switchAccount(first.id)
        assertEquals(8_000L, repository.observeUserGoals().first().stepGoal)
        assertEquals(3, repository.observeUserGoals().first().weeklySessionTarget)

        accountRepository.switchAccount(secondId)
        assertEquals(12_000L, repository.observeUserGoals().first().stepGoal)
        assertEquals(5, repository.observeUserGoals().first().weeklySessionTarget)
    }
}
```

- [ ] **Step 3: Run ownership tests to verify they fail**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --tests "com.musfit.data.repository.LocalGoalsRepositoryTest" --no-daemon --console=plain
```

Expected: FAIL because `LocalProfileRepository` and `LocalGoalsRepository` do not accept `AccountRepository` and still use fixed singleton ids.

- [ ] **Step 4: Make ProfileRepository account-owned**

In `app/src/main/java/com/musfit/data/repository/ProfileRepository.kt`, add imports:

```kotlin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
```

Remove these constants:

```kotlin
const val PROFILE_ID = "user"
const val SETTINGS_ID = "app"
```

Change the class header:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class LocalProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val healthDao: HealthDao,
    private val accountRepository: AccountRepository,
) : ProfileRepository {
```

Update the internal constructor:

```kotlin
internal constructor(
    profileDao: ProfileDao,
    healthDao: HealthDao,
    accountRepository: AccountRepository,
    clock: () -> Long,
) : this(profileDao, healthDao, accountRepository) {
    this.clock = clock
}
```

Replace `observeProfile()`:

```kotlin
override fun observeProfile(): Flow<UserProfile> =
    accountRepository.observeActiveAccount().flatMapLatest { account ->
        profileDao.observeProfile(account.id).map { it?.toUserProfile() ?: DEFAULT_USER_PROFILE }
    }
```

Replace `saveProfile(profile)`:

```kotlin
override suspend fun saveProfile(profile: UserProfile) {
    val account = accountRepository.ensureActiveAccount()
    profileDao.upsertProfile(profile.toEntity(id = account.id, now = clock()))
}
```

Replace `observeSettings()`:

```kotlin
override fun observeSettings(): Flow<AppSettings> =
    accountRepository.observeActiveAccount().flatMapLatest { account ->
        profileDao.observeSettings(account.id).map { it?.toAppSettings() ?: DEFAULT_APP_SETTINGS }
    }
```

Replace `saveSettings(settings)`:

```kotlin
override suspend fun saveSettings(settings: AppSettings) {
    val account = accountRepository.ensureActiveAccount()
    profileDao.upsertSettings(
        AppSettingsEntity(
            id = account.id,
            unitSystem = settings.unitSystem,
            themeMode = settings.themeMode,
            updatedAtEpochMillis = clock(),
        ),
    )
}
```

Replace `toEntity`:

```kotlin
private fun UserProfile.toEntity(id: String, now: Long) = UserProfileEntity(
    id = id,
    sex = sex?.name,
    birthDateEpochDay = birthDateEpochDay,
    heightCm = heightCm,
    activityLevel = activityLevel.name,
    goalType = goalType.name,
    goalPaceKgPerWeek = goalPaceKgPerWeek,
    goalWeightKg = goalWeightKg,
    updatedAtEpochMillis = now,
)
```

- [ ] **Step 5: Make GoalsRepository account-owned**

In `app/src/main/java/com/musfit/data/repository/GoalsRepository.kt`, add imports:

```kotlin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
```

Change the class:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class LocalGoalsRepository @Inject constructor(
    private val userGoalsDao: UserGoalsDao,
    private val accountRepository: AccountRepository,
) : GoalsRepository {
    private var clock: () -> Long = { System.currentTimeMillis() }

    internal constructor(
        userGoalsDao: UserGoalsDao,
        accountRepository: AccountRepository,
        clock: () -> Long,
    ) : this(userGoalsDao, accountRepository) {
        this.clock = clock
    }

    override fun observeUserGoals(): Flow<UserGoals> =
        accountRepository.observeActiveAccount().flatMapLatest { account ->
            userGoalsDao.observeUserGoals(account.id).map { entity ->
                entity?.let { UserGoals(it.stepGoal, it.weeklySessionTarget, it.targetWeightKg) } ?: UserGoals()
            }
        }

    override suspend fun updateUserGoals(goals: UserGoals) {
        val account = accountRepository.ensureActiveAccount()
        userGoalsDao.upsertUserGoals(
            UserGoalsEntity(
                id = account.id,
                stepGoal = goals.stepGoal.coerceAtLeast(0L),
                weeklySessionTarget = goals.weeklySessionTarget.coerceAtLeast(0),
                targetWeightKg = goals.targetWeightKg.coerceAtLeast(0.0),
                updatedAtEpochMillis = clock(),
            ),
        )
    }
}
```

- [ ] **Step 6: Add migration 19 to 20**

In `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`, change:

```kotlin
version = 19,
```

to:

```kotlin
version = 20,
```

In `app/src/main/java/com/musfit/core/di/DatabaseModule.kt`, add `MIGRATION_19_20` after `MIGRATION_18_19` in the `.addMigrations` call.

Add this migration:

```kotlin
internal val MIGRATION_19_20 =
    object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT OR REPLACE INTO user_profile (
                    id, sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                    goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                )
                SELECT
                    'local-default', sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                    goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                FROM user_profile
                WHERE id = 'user'
                """.trimIndent(),
            )
            db.execSQL("DELETE FROM user_profile WHERE id = 'user'")
            db.execSQL(
                """
                INSERT OR REPLACE INTO app_settings (
                    id, unitSystem, themeMode, updatedAtEpochMillis
                )
                SELECT
                    'local-default', unitSystem, themeMode, updatedAtEpochMillis
                FROM app_settings
                WHERE id = 'app'
                """.trimIndent(),
            )
            db.execSQL("DELETE FROM app_settings WHERE id = 'app'")
            db.execSQL(
                """
                INSERT OR REPLACE INTO user_goals (
                    id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                )
                SELECT
                    'local-default', stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                FROM user_goals
                WHERE id = 'default'
                """.trimIndent(),
            )
            db.execSQL("DELETE FROM user_goals WHERE id = 'default'")
        }
    }
```

- [ ] **Step 7: Run ownership repository tests**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --tests "com.musfit.data.repository.LocalGoalsRepositoryTest" --no-daemon --console=plain
```

Expected: PASS. Room should generate `app/schemas/com.musfit.data.local.MusFitDatabase/20.json`.

- [ ] **Step 8: Run Today and Profile ViewModel tests**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.ui.today.TodayViewModelTest" --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 9: Commit Task 4**

Run:

```powershell
git add app/src/main/java/com/musfit/data/repository/ProfileRepository.kt app/src/main/java/com/musfit/data/repository/GoalsRepository.kt app/src/main/java/com/musfit/data/local/MusFitDatabase.kt app/src/main/java/com/musfit/core/di/DatabaseModule.kt app/src/test/java/com/musfit/data/repository/LocalProfileRepositoryTest.kt app/src/test/java/com/musfit/data/repository/LocalGoalsRepositoryTest.kt app/schemas/com.musfit.data.local.MusFitDatabase/20.json
git commit -m "feat: scope profile goals to active account"
```

---

### Task 5: Migration Tests, Architecture Docs, And Full Verification

**Files:**
- Create: `app/src/test/java/com/musfit/data/local/MusFitMigrationTest.kt`
- Modify: `docs/architecture/data-models.md`

**Interfaces:**
- Consumes: exported schemas 18, 19, 20 and `DatabaseModule.MIGRATION_18_19`, `DatabaseModule.MIGRATION_19_20`.
- Produces: migration coverage for existing singleton data and architecture documentation for account ownership.

- [ ] **Step 1: Write migration tests**

Create `app/src/test/java/com/musfit/data/local/MusFitMigrationTest.kt`:

```kotlin
package com.musfit.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.musfit.core.di.DatabaseModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusFitMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MusFitDatabase::class.java,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration18To20_createsDefaultAccountAndRekeysSingletonRows() {
        helper.createDatabase(TEST_DB, 18).apply {
            execSQL(
                """
                INSERT INTO user_profile (
                    id, sex, birthDateEpochDay, heightCm, activityLevel, goalType,
                    goalPaceKgPerWeek, goalWeightKg, updatedAtEpochMillis
                ) VALUES (
                    'user', 'Male', 9000, 182.0, 'Moderate', 'Lose', 0.5, 78.0, 1000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO app_settings (
                    id, unitSystem, themeMode, updatedAtEpochMillis
                ) VALUES (
                    'app', 'metric', 'dark', 1000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO user_goals (
                    id, stepGoal, weeklySessionTarget, targetWeightKg, updatedAtEpochMillis
                ) VALUES (
                    'default', 8000, 3, 78.0, 1000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            20,
            true,
            DatabaseModule.MIGRATION_18_19,
            DatabaseModule.MIGRATION_19_20,
        )

        db.query("SELECT displayName FROM accounts WHERE id = 'local-default'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("You", cursor.getString(0))
        }
        db.query("SELECT activeAccountId FROM account_session WHERE `key` = 'active'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("local-default", cursor.getString(0))
        }
        db.query("SELECT sex FROM user_profile WHERE id = 'local-default'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Male", cursor.getString(0))
        }
        db.query("SELECT themeMode FROM app_settings WHERE id = 'local-default'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("dark", cursor.getString(0))
        }
        db.query("SELECT stepGoal FROM user_goals WHERE id = 'local-default'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(8000L, cursor.getLong(0))
        }
    }

    private companion object {
        const val TEST_DB = "musfit-migration-test"
    }
}
```

- [ ] **Step 2: Run migration tests to verify they pass**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.MusFitMigrationTest" --no-daemon --console=plain
```

Expected: PASS. If the test cannot access `DatabaseModule.MIGRATION_18_19` or `DatabaseModule.MIGRATION_19_20`, verify both migration vals are `internal val`, not `private val`.

- [ ] **Step 3: Update architecture documentation**

In `docs/architecture/data-models.md`, update the Room database version from the stale value to:

```markdown
- Version: 20
```

Add this section after the Room database overview:

```markdown
### Account Tables

Source: `app/src/main/java/com/musfit/data/local/entity/AccountEntities.kt`

| Entity | Table | Purpose | Key fields |
| --- | --- | --- | --- |
| `AccountEntity` | `accounts` | Local account identity for user-owned data. | `id`, `displayName`, optional `email`, optional future `remoteUserId`, timestamps. |
| `AccountSessionEntity` | `account_session` | Device-local active account pointer. | `key = "active"`, `activeAccountId`, `updatedAtEpochMillis`. |

The first account id is `local-default`. Profile, app settings, and Today goals use the active account id as their singleton row id. Food, Training, and Health ownership are separate follow-up slices.
```

Update the Profile and User Goals table descriptions so they no longer say fixed singleton ids. Use:

```markdown
| `UserProfileEntity` | `user_profile` | Per-account profile and body-goal inputs. | `id` stores the account id, profile inputs, goal intent, updated time. |
| `AppSettingsEntity` | `app_settings` | Per-account app preferences. | `id` stores the account id, unit system, theme mode. |
| `UserGoalsEntity` | `user_goals` | Per-account cross-cutting Today goals not stored in `food_goals`. | `id` stores the account id, step goal, weekly session target, target weight. |
```

- [ ] **Step 4: Run focused account/profile/goals tests**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest --tests "com.musfit.data.local.AccountDaoTest" --tests "com.musfit.data.local.MusFitMigrationTest" --tests "com.musfit.data.repository.LocalAccountRepositoryTest" --tests "com.musfit.data.repository.LocalProfileRepositoryTest" --tests "com.musfit.data.repository.LocalGoalsRepositoryTest" --tests "com.musfit.ui.profile.ProfileViewModelTest" --no-daemon --console=plain
```

Expected: PASS.

- [ ] **Step 5: Run full verification**

Run:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

Expected: PASS.

If Gradle fails with OneDrive-generated-output errors under `app/build`, use the AGENTS.md cleanup pattern exactly:

```powershell
.\gradlew.bat --stop
Start-Sleep -Seconds 3
$workspace = (Resolve-Path -LiteralPath '.').Path
$target = Resolve-Path -LiteralPath 'app\build' -ErrorAction SilentlyContinue
if ($target) {
  if ($target.Path.StartsWith($workspace, [System.StringComparison]::OrdinalIgnoreCase)) {
    Remove-Item -LiteralPath $target.Path -Recurse -Force -ErrorAction Stop
  } else {
    throw "Refusing to remove outside workspace: $($target.Path)"
  }
}
```

Then rerun:

```powershell
. .\.superpowers\sdd\android-env.ps1
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain
```

- [ ] **Step 6: Commit Task 5**

Run:

```powershell
git add app/src/test/java/com/musfit/data/local/MusFitMigrationTest.kt docs/architecture/data-models.md
git commit -m "test: cover local account migrations"
```

---

## Follow-Up Slices

These are intentionally outside this first implementation plan so the first account system can ship with narrow, verified ownership:

- Food ownership: `food_goals`, saved foods, meals, meal items, meal definitions, quick presets, templates, recipes, shopping list, water, and food sync state should filter by active account.
- Training ownership: routines, workout sessions, and custom exercises should filter by active account while starter exercises can remain shared.
- Health ownership: body metrics, daily health summaries, and Health Connect sync state should filter by active account.
- Account deletion and a full multi-account switcher require a separate data cleanup design.

## Final Acceptance Checklist

- [ ] Fresh installs auto-create one local account named `You`.
- [ ] Existing installs migrating from version 18 get `local-default` and active session rows.
- [ ] Existing `user_profile`, `app_settings`, and `user_goals` rows migrate to id `local-default`.
- [ ] Profile shows the account identity card.
- [ ] Account editor saves display name and optional email locally.
- [ ] Blank account names are rejected in ViewModel and repository tests.
- [ ] Profile, Settings, and Today goals are separated by active account.
- [ ] No UI copy claims cloud backup, remote login, password protection, or cross-device sync.
- [ ] `testDebugUnitTest lintDebug assembleDebug` passes.
