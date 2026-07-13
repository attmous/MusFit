# Dependency governance

MusFit is stable-first. Dependency changes are maintenance work with the same
release, device, supply-chain, and performance obligations as production code.

## Update policy

- Direct dependencies and plugins are declared in `gradle/libs.versions.toml`.
- Dependabot opens at most five weekly, grouped minor/patch PRs against
  `master`. Major updates and the two registered preview families are not
  automated; preview exit requires a human-authored compatibility PR.
- Keep one dependency family per PR when an update changes runtime, generated
  code, schemas, manifests, shrinking, or benchmark semantics. Do not mix a
  dependency update with product behavior.
- Every dependency PR runs the Android internal/migration/production matrix.
  Catalog changes also run the API 28/37 performance lane.
- Refresh strict verification metadata from an empty Gradle home and review
  every added coordinate/checksum. Never disable verification to land an update.
- Run the touched integration smoke. A dependency-only PR is rejected when
  production APK/AAB size or approved startup P50/P90 regresses by more than 5%
  without measured, reviewed justification.

## Preview exceptions

`config/dependency-preview-exceptions.json` is the authoritative exception
register. Every catalog version containing alpha, beta, RC, preview, snapshot,
dev, EAP, or milestone syntax must match one current entry with:

- exact version and coordinates;
- accountable owner;
- concrete rationale and exit criteria;
- expiry no more than 180 days away.

The Full gate fails for unregistered, stale, mismatched, or expired entries.
Expiry is a stop sign: move to stable or explicitly review and renew the
exception in a human-authored PR.

Current exceptions are intentionally narrow:

- Credentials `1.7.0-alpha02`, owned by `@attmous`, expires 2026-07-31. Current
  source uses the ordinary Google sign-in retrieval surface; stable 1.6.0 must
  be evaluated in a separate compatibility PR with API 28/37 sign-in smoke.
- Benchmark/Baseline Profile `1.5.0-alpha07`, owned by `@attmous`, expires
  2026-09-30. The approved performance baseline was produced with this engine;
  migration to stable must deliberately revalidate Perfetto and measurement
  comparability.

AndroidX currently lists Credentials 1.6.0 and Benchmark 1.4.1 as the stable
lines. The exception register, not this explanatory summary, is executable
truth for exact versions and dates.

## Unused dependency review

The W2-DEPS-01 review retained each declared alias only with a concrete owner:

| Dependency family | Current ownership evidence |
| --- | --- |
| Compose, Activity, Lifecycle, Navigation | app UI, ViewModels, semantics, and navigation graph |
| Room, Hilt, KSP, coroutines | database/DAO/repository graph and generated DI |
| Credentials, Google ID | `GoogleSignIn.kt`; Play Services bridge supports pre-Android 14 |
| CameraX, ML Kit barcode/text | barcode and nutrition-label scanner workflows |
| Retrofit, Moshi, OkHttp | Open Food Facts, GitHub auth, and opt-in coach clients |
| Coil/GIF | Training exercise image and animation loader |
| Health Connect | health record permission, mapping, read, and export boundary |
| Benchmark, Baseline Profile, Profile Installer | production-shaped startup/journey measurement and shipped profiles |
| JUnit, AndroidX Test, Robolectric, Roborazzi, Turbine | unit, migration, managed-device, and screenshot lanes only |
| CycloneDX | isolated aggregate SBOM generation; never packaged at runtime |

`test-dependency-governance.ps1` also proves every library/plugin catalog alias
is referenced by a checked-in Gradle build. The earlier unused WorkManager/Hilt
Work runtime and processors were removed by W2-BUILD-03. A future alias with no
build owner fails the Full gate instead of becoming dormant catalog debt.

## Local checks

```powershell
. .\scripts\android\android-env.ps1
.\scripts\dependencies\test-dependency-governance.ps1 -SelfTest
.\scripts\dev\verify-musfit.ps1 -Preset Full
```

Upstream references: [Credentials releases](https://developer.android.com/jetpack/androidx/releases/credentials),
[Benchmark releases](https://developer.android.com/jetpack/androidx/releases/benchmark),
and [Dependabot options](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference).
