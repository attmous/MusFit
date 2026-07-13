# Build and configuration caching

MusFit enables both Gradle build caching and configuration caching for local,
CI, benchmark, and protected publication builds. Configuration-cache problems
are fatal; new build logic must not capture a `Project`, task container, or
build-script object in task actions.

## Canonical verification

Initialize the Android environment before Gradle commands:

```powershell
. .\scripts\android\android-env.ps1
```

The focused contract proves same-graph reuse and that version derivation fails
closed when Git cannot run:

```powershell
.\scripts\dev\test-configuration-cache.ps1 -SelfTest
```

The canonical full gate uses the enabled cache automatically:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Full -RetryOnGeneratedOutputIssue
```

Run the identical full gate twice when changing root build logic. The first run
must end with `Configuration cache entry stored.` and zero problems. The second
must end with `Configuration cache entry reused.` and remain behaviorally green.

## Version-code contract

`versionCode` is the positive `git rev-list --count HEAD` from a non-shallow
checkout. The value is obtained through a tracked Gradle `ValueSource`, so a new
commit invalidates stale configuration without running a process directly from
the build script.

MusFit deliberately fails configuration when:

- Git cannot be executed;
- a Git command exits nonzero;
- the checkout is shallow;
- the count is missing, nonnumeric, zero, or outside the positive `Int` range.

Every CI lane that invokes Gradle therefore uses `fetch-depth: 0`. There is no
fallback to version code `1`; silently publishing or testing a non-monotonic
identity is less safe than failing the build.

`musfit.gitExecutable` exists only to inject the deliberate missing-Git test.
Production and normal developer builds leave it unset and use `git`.

## R8 and diagnostics

Production and legacy-migration R8 report directories are prepared by typed
tasks with declared output directories. R8 depends on those tasks, avoiding the
execution-time project capture that previously made the full gate incompatible
with configuration caching.

Android CI runs the focused cache/versioning test before the full gate and
retains `build/reports/configuration-cache/` and `build/reports/problems/` for 14
days when Gradle emits diagnostics. Cache state itself is not uploaded as an
artifact.

Measured W2-BUILD-02 local evidence on the unchanged full graph:

- first clean-compatible full graph: 4m 37s, 253 tasks, zero problems, entry
  stored;
- identical second graph: 9s, 253 tasks, entry reused;
- focused variant-matrix configuration: 16s stored, 4s reused.
