# CLAUDE.md

Read [`AGENTS.md`](AGENTS.md) in full before changing this repository. It is the
canonical MusFit contract for product boundaries, current safety warnings,
branch/PR policy, verification, emulator/device handling, and source-of-truth
rules. Do not create a separate Claude-only workflow here.

## Repository Orientation

MusFit is an Android-only, modular Kotlin application with production id
`com.musfit` and side-by-side internal id `com.musfit.internal`. `:app` is the
composition root; coarse feature, core, and integration modules own the product
code. Top-level destinations are: Today, Food, Training, Profile. Route truth
is `app/src/main/java/com/musfit/ui/AppDestination.kt`; the adaptive root shell
is composed by `app/src/main/java/com/musfit/ui/AppNavGraph.kt` and uses
`MusFitBottomNav` for compact windows.

Important source locations:

- app shell/navigation: `app/src/main/java/com/musfit/ui/`
- feature UI/state: `feature/*/src/main/java/com/musfit/ui/`
- repositories: `core/data/src/main/java/com/musfit/data/repository/`
- Room database/DAOs/entities: `core/database/src/main/java/com/musfit/data/local/`
- pure domain logic: `core/model/src/main/kotlin/com/musfit/domain/`
- remote clients: `core/network/src/main/java/com/musfit/data/remote/`
- Health Connect: `integration/healthconnect/src/main/java/com/musfit/integrations/healthconnect/`
- scanner/camera integration: `integration/scanner/src/main/java/com/musfit/integrations/scanner/`
- local/unit tests: the owning module's `src/test` or `src/testInternalDebug`
- device/instrumentation tests: `app/src/androidTest/java/com/musfit/`

The intended dependency direction is:

```text
Compose UI -> ViewModel -> repository boundary -> DAO / remote / integration boundary
```

Use [`docs/architecture/README.md`](docs/architecture/README.md) for the living
architecture map. The July 2026
[`app architecture audit`](docs/architecture/app-architecture-audit-2026-07-10.md)
is historical evidence, not an active work queue.

## Claude Working Rules

- Follow the scope, sharp-edge warnings, test strategy, device rules, and
  branch-to-draft-PR flow in `AGENTS.md`.
- Inspect current source and tests before relying on prose. Historical plans are
  intent records, not live status.
- Do not copy volatile file counts or a Room version into handoff prose. Derive
  the version from
  `core/database/src/main/java/com/musfit/data/local/MusFitDatabase.kt` and verify the
  newest exported schema matches it.
- Follow the local state pattern: some ViewModels wrap a private
  `MutableStateFlow`; others combine repository flows with `stateIn`.
- Keep pure domain code free of Android, Compose, Room, and Retrofit imports.
- On Windows PowerShell, source the checked-in environment before direct Gradle
  or adb commands:

  ```powershell
  . .\scripts\android\android-env.ps1
  ```

- Run the applicable focused, workflow-contract, full-variant, and device checks
  specified in `AGENTS.md`. Never seed/reset a physical device, and never push
  directly to `origin/master` without an explicit emergency instruction.
