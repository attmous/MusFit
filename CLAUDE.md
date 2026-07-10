# CLAUDE.md

Read [`AGENTS.md`](AGENTS.md) in full before changing this repository. It is the
canonical MusFit contract for product boundaries, current safety warnings,
branch/PR policy, verification, emulator/device handling, and source-of-truth
rules. Do not create a separate Claude-only workflow here.

## Repository Orientation

MusFit is an Android-only, single-module (`:app`) Kotlin application with package
id `com.musfit`. Top-level destinations are: Today, Food, Training, Profile. The
custom bottom chrome is `MusFitBottomNav` in
`app/src/main/java/com/musfit/ui/AppNavGraph.kt`; route truth is
`app/src/main/java/com/musfit/ui/AppDestination.kt`.

Important source locations:

- UI/navigation: `app/src/main/java/com/musfit/ui/`
- repositories: `app/src/main/java/com/musfit/data/repository/`
- Room database/DAOs/entities: `app/src/main/java/com/musfit/data/local/`
- pure domain logic: `app/src/main/java/com/musfit/domain/`
- remote clients: `app/src/main/java/com/musfit/data/remote/`
- Health Connect: `app/src/main/java/com/musfit/integrations/healthconnect/`
- DI and migrations: `app/src/main/java/com/musfit/core/di/`
- local/unit tests: `app/src/test/java/com/musfit/`
- device/instrumentation tests: `app/src/androidTest/java/com/musfit/`

The intended dependency direction is:

```text
Compose UI -> ViewModel -> repository boundary -> DAO / remote / integration boundary
```

Current source has known exceptions. Before cross-feature, persistence,
navigation, build, release, or structural work, read:

- [`docs/architecture/app-architecture-audit-2026-07-10.md`](docs/architecture/app-architecture-audit-2026-07-10.md)
- [`docs/architecture/architecture-remediation-backlog-2026-07-10.md`](docs/architecture/architecture-remediation-backlog-2026-07-10.md)

## Claude Working Rules

- Follow the scope, sharp-edge warnings, test strategy, device rules, and
  branch-to-draft-PR flow in `AGENTS.md`.
- Inspect current source and tests before relying on prose. Historical plans are
  intent records, not live status.
- Do not copy volatile file counts or a Room version into handoff prose. Derive
  the version from
  `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt` and verify the
  newest exported schema matches it.
- Follow the local state pattern: some ViewModels wrap a private
  `MutableStateFlow`; others combine repository flows with `stateIn`.
- Keep pure domain code free of Android, Compose, Room, and Retrofit imports.
- On Windows PowerShell, source the checked-in environment before direct Gradle
  or adb commands:

  ```powershell
  . .\scripts\android\android-env.ps1
  ```

- Run the applicable focused, workflow-contract, full-debug, and device checks
  specified in `AGENTS.md`. Never seed/reset a physical device, and never push
  directly to `origin/master` without an explicit emergency instruction.
