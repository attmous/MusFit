# Production signing and install migration

## Approved decision

W1-REL-02 uses **encrypted export/import** to preserve data from legacy
`com.musfit` installations signed with the committed public development key.
The user approved preservation on 2026-07-12. Reusing that public certificate
for production is forbidden.

The signing boundary is intentionally asymmetric:

| Artifact | Application ID | Certificate | Purpose |
| --- | --- | --- | --- |
| `internalDebug` | `com.musfit.internal` | public development key | side-by-side development only |
| `legacyMigrationRelease` | `com.musfit` | public development key | one-time, non-debuggable export bridge |
| `productionRelease` | `com.musfit` | external production/upload key | final app and encrypted import |

The bridge must never be treated as a production release. CI verifies it but
does not upload or publish it. Its only supported lifecycle is: update a legacy
install, export, uninstall, then install production.

## User procedure

1. Install the exact verified migration bridge over the existing legacy app.
   Android must accept it as an update without clearing data.
2. Open **Move MusFit data**, choose **Export encrypted backup**, and enter a
   unique passphrase of at least 12 characters twice.
3. Record the displayed row count and 12-character SHA-256 prefix. Keep the
   `.musfitbackup` file and passphrase separately.
4. Uninstall the legacy `com.musfit` app. This is the only destructive step.
5. Install the verified production-signed `com.musfit` APK.
6. Open **Profile → Settings → Data transfer → Import encrypted backup**,
   select the file, and enter the passphrase.
7. After MusFit reports that the backup is verified and staged, fully close and
   reopen it. Confirm the restore receipt has the same row count/checksum and
   inspect Food, Training, Profile, and history.

Do not uninstall before step 2 succeeds and the file is copied somewhere that
survives app removal.

## Security and privacy contract

- Archive format 1 uses AES-256-GCM and PBKDF2-HMAC-SHA256 with 310,000
  iterations, a random 128-bit salt, and random 96-bit IV.
- Header, manifest, table counts, checksum, and database bytes are authenticated.
- Wrong passphrases and modified files produce the same generic authentication
  failure; no oracle distinguishes them.
- Android's Storage Access Framework is the only external-file boundary. MusFit
  requests no broad storage permission and never stores the passphrase.
- The archive includes the Room database only. Android Keystore material,
  encrypted AI credentials, OAuth/browser state, Health Connect permissions,
  notifications, and app preferences are excluded.
- The exported database copy forces `ai_coach_settings.apiKeyStored = 0`; the
  live legacy database is not changed.
- Import validates GCM authentication, SHA-256, schema version, every user-table
  row count, and SQLite `integrity_check` before staging. Startup validates
  again before replacement and retains a rollback database until validation
  succeeds.

The transfer limit is 256 MiB of database data. The acceptance budget on
`MusFit_API36` is under 10 seconds each for export and import at the seeded-data
size. Evidence records encrypted archive size, duration, row counts, and hash.

## Key custody

- `app/keystore/musfit.debug.keystore` is public legacy/development material.
  It signs only `internalDebug` and the temporary migration bridge.
- Production signing/upload keys and passwords stay outside Git, local
  properties, build outputs, screenshots, and PR evidence. W1-REL-04 owns their
  CI secret configuration and artifact promotion.
- W1-REL-02 production verification uses a freshly generated, gitignored,
  disposable emulator-only key. It is never published or reused.

## Verification and rollback

Run:

```powershell
. .\scripts\android\android-env.ps1
.\scripts\release\verify-data-migration-artifacts.ps1
```

The script verifies package IDs, launchers, non-debuggable state, lack of local
network permission, and that the bridge certificate exactly matches the public
legacy key. The device matrix then proves:

- pre-REL-01 debug-signed app → bridge is accepted as an in-place update;
- seeded row counts survive bridge launch;
- encrypted export rejects wrong passphrases and tampering;
- uninstall → disposable-key production install → import/restart restores the
  same counts/checksum;
- internal and production identities still install side by side.

Rollback before uninstall: reinstall the previous legacy APK. Rollback after a
successful export: keep the encrypted file, uninstall the failed production
build, install a corrected production build signed with the same approved key,
and import again. Never roll back by signing production with the public key.

The bridge may be retired only after the documented migration window ends and
the user approves removal. Retirement is a separate PR that removes the flavor,
launcher, and public bridge build while retaining production import support.
