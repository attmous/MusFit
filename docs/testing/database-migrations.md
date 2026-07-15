# Database migration verification

MusFit retains and supports every committed Room schema, from version 1 through
the current version. A schema may be retired only through a separately approved
compatibility decision that documents affected installs and the user-data path.
There is no destructive-migration fallback.

`MusFitMigrationInstrumentationTest` runs every retained origin schema against
the exact ordered migration registry used by production. Each path validates
the current Room schema, stable sentinel identities and relationships, and
`PRAGMA foreign_key_check`. The five most recent adjacent hops are also tested
independently so a later migration cannot hide an invalid intermediate schema.

`MusFitFrameworkDaoInstrumentationTest` verifies representative DAO graph
writes, foreign-key enforcement, and transaction rollback on Android framework
SQLite. Host-side Robolectric database tests remain useful for fast repository
feedback, but they do not replace this device suite.

## Device matrix

The Gradle-managed `migrationApi28And37` group runs the suite on API 28 using
the AOSP x86 image and API 37 using Google's available 16 KB-page x86_64 image.
The explicit class list is intentional: command-line
`android.testInstrumentationRunnerArguments.class` does not reach this
project's orchestrated managed-device group tasks and can execute the entire
instrumentation suite. The MusFit-owned property is embedded as test-APK
manifest metadata, and `MusFitAndroidJUnitRunner` applies it before discovery
on both devices.

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat migrationApi28And37GroupInternalDebugAndroidTest `
  '-Pmusfit.testInstrumentationRunnerArguments.class=com.musfit.data.local.MusFitMigrationInstrumentationTest,com.musfit.data.local.MusFitRecentMigrationInstrumentationTest,com.musfit.data.local.MusFitLargeMigrationInstrumentationTest,com.musfit.data.local.MusFitFrameworkDaoInstrumentationTest' `
  --no-daemon --console=plain
```

For focused work against an already running dedicated emulator:

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat connectedInternalDebugAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.musfit.data.local.MusFitMigrationInstrumentationTest' `
  --no-daemon --console=plain
```

The large-database scenario migrates 10,000 related rows from schema 1 to the
latest schema. Its device budget is 30 seconds for the migration itself; the
test emits a `W2_TEST_01_MIGRATION_METRIC` JSON record with API level, database
sizes, elapsed time, and budget. Any budget change requires measured API 28 and
API 37 evidence in its own reviewed PR.

Baseline recorded on 2026-07-12 for a 1,351,680-byte schema-1 database that
became 1,826,816 bytes after migration:

| Device | Page size/image | Migration |
| --- | --- | ---: |
| Managed API 28 | AOSP x86 | 136 ms |
| Seeded API 36 | Google APIs x86_64 | 164 ms |
| Managed API 37 | Google APIs 16 KB x86_64 | 642 ms |

All measurements are well below the 30,000 ms compatibility budget. They are a
guardrail for migration safety, not a general app performance benchmark.
