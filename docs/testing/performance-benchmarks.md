# Performance benchmarks

MusFit measures a production-shaped, minified, profileable build instead of the
debug build. The benchmark surface covers cold and warm startup plus the Food,
Training, Profile, and warmed 100-item exercise-image browse journeys. Journey
runs capture frame timing, maximum heap and anonymous RSS memory, and a Perfetto
trace for every iteration.

## Targets

The `benchmark` module drives `com.musfit` through the `productionBenchmark`
variant. That app variant inherits the release configuration, R8/resource
shrinking, production network policy, and the generated app-owned Baseline
Profile. It is non-debuggable and is signed with the public debug key only so it
can be installed by local and CI managed devices; it is never a release
artifact.

Journey setup has one bounded retry for the managed-emulator first-launch race
where the process is created but has not attached when `am start -W` returns.
The retry occurs before measurement; a second failure remains a hard test
failure. Startup measurements never retry inside their measured block.

The exercise-image journey opens Training -> New routine -> Add exercise, waits
for Coil success semantics while visiting 100 distinct image-bearing exercises
in setup, verifies the list's first-page anchor after each rewind, and requires
all 100 IDs to report `MEMORY_CACHE` on a second pass. The measured pass again
traverses and proves the same 100 memory-cache IDs inside the frame-timing block,
then records an end-of-browse `dumpsys meminfo` total-PSS snapshot and peak
process memory.

The benchmark variant alone replaces picker thumbnail URLs with a deterministic
256 x 256 PNG byte fixture. Each exercise keeps a distinct decoded-memory key;
fixture disk and network caching are disabled. This isolates warmed decoded
bitmap and process-loader behavior from CDN availability and encoded-cache
variance. Production and internal variants continue to load the real media
URLs, and the generic Training journey remains a separate whole-destination
signal.

Before this journey's setup, the harness verifies `ro.kernel.qemu=1`, disables
the preinstalled Google Photos package, and confirms the disabled package state.
The documented Gradle tasks create disposable managed emulators, and the guard
hard-fails before mutating a physical device. A controlled trace comparison
found Photos indexing on both emulator CPUs during the candidate's worst
runnable delay; MusFit never opens Photos, so excluding that unrelated work
makes the same-fixture comparison repeatable.

The managed-device matrix is:

| Target | Purpose | Result transport |
| --- | --- | --- |
| API 28, Pixel 2, x86_64 | Minimum-supported-device execution | Instrumentation result and pass/fail |
| API 37, Pixel 2, x86_64, 16 KB pages | Current-platform execution and regression baseline | Benchmark JSON, test reports, and Perfetto traces |

Android's managed-device additional-output transport is unavailable below API
29. API 28 therefore remains an execution gate, while repeatable numeric
comparison uses API 37's exported benchmark JSON. The long 100-image journey
runs once on API 28 and five times on API 37; the other journeys retain five
iterations on both devices.

## Local commands

Initialize the Android environment before Gradle commands:

```powershell
. .\scripts\android\android-env.ps1
```

Run a quick structural/device smoke pass:

```powershell
.\gradlew.bat :benchmark:benchmarkApi28And37GroupBenchmarkAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true' `
  --no-daemon --console=plain
```

Run the measured suite and compare it with the approved baseline:

```powershell
.\gradlew.bat :benchmark:benchmarkApi28And37GroupBenchmarkAndroidTest `
  --no-daemon --console=plain

.\scripts\performance\verify-benchmark-regression.ps1 `
  -ResultsPath benchmark\build\intermediates\managed_device_android_test_additional_output `
  -SelfTest
```

Validate only the long image journey after a targeted API 37 run by selecting
its exact fully qualified benchmark ID. Without `-BenchmarkId`, the verifier
continues to require the complete approved suite. A selected subset cannot be
used with `-WriteBaseline`.

```powershell
.\gradlew.bat :benchmark:benchmarkApi37BenchmarkAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.musfit.benchmark.MusFitJourneyBenchmark#trainingExerciseImageBrowse100Items' `
  --no-daemon --console=plain

.\scripts\performance\verify-benchmark-regression.ps1 `
  -ResultsPath benchmark\build\intermediates\managed_device_android_test_additional_output\benchmark\benchmarkApi37BenchmarkAndroidTest `
  -BenchmarkId 'com.musfit.benchmark.MusFitJourneyBenchmark.trainingExerciseImageBrowse100Items' `
  -SelfTest
```

Generate a fresh app-owned Baseline Profile only after a reviewed journey
change:

```powershell
.\gradlew.bat :app:generateBaselineProfile --no-daemon --console=plain
```

Normal builds do not regenerate the profile. The checked-in rules under
`app/src/main/generated/baselineProfiles/` contain only `com/musfit` rules and
are packaged into production artifacts.

## Regression contract

The parser emits minimum, median, P90, maximum, and sample count for every raw
metric. The CI gate compares approved metrics from
`benchmark/baselines/approved-api37.json`:

- startup and frame metrics use P90;
- maximum heap and anonymous RSS use maximum;
- the end-of-browse Training image total-PSS metric uses maximum;
- any increase greater than 10% fails the verifier by default;
- exactly 10% remains within the approved boundary;
- a built-in deliberate-regression self-test proves that the comparison is
  capable of failing.

The original journey/startup approved values use the per-metric maximum from the
complete local run and the first exact-head GitHub-hosted run. That measured
two-host envelope avoids treating runner CPU differences as product regressions
while retaining a strict >10% failure above every verified environment. The new
image-browse entries are initialized from the controlled five-iteration
`origin/master` run below, not from the faster candidate. Later baseline updates
use a complete, reviewed API 37 run and must explain the measured product or
runner change; they must not be used simply to make a regression green.

## Training image comparison (2026-07-19)

Both runs used API 37 device key `api37-sdk_gphone16k_x86_64`, fingerprint
`google/sdk_gphone16k_x86_64/emu64xa16k:17/CE2A.260420.019/15611780:userdebug/dev-keys`,
five iterations, the deterministic 100-image fixture, and confirmed Google
Photos disabled before every launch. The baseline app source was commit
`8913598edcd7a67a6e17520437b1c3d396c548fb`; the candidate was the S15 dirty
working tree based on that commit.

| Acceptance metric | Controlled master | Final candidate | Change |
| --- | ---: | ---: | ---: |
| Frame CPU P90 | 15.0513 ms | 14.45644 ms | -3.95% |
| Frame overrun P90 | 2.216126 ms | 1.442662 ms | -34.90% |
| Maximum heap | 15,463 KB | 15,048 KB | -2.68% |
| Maximum anonymous RSS | 104,036 KB | 99,496 KB | -4.36% |
| Maximum end-of-browse total PSS | 74,430 KB | 70,155 KB | -5.74% |
| Memory-cache hits per setup/measured traversal | 0/100 | 100/100 | behavioral gate passed |

Master PSS samples were `69,470, 68,524, 72,014, 73,578, 74,430` KB; candidate
samples were `66,789, 62,835, 63,188, 66,263, 70,155` KB. Perfetto
actual-frame P90 samples were `14.7685, 12.9497, 13.2468, 12.8168, 12.6129` ms
for master and `14.1538, 12.7141, 12.9167, 13.0315, 12.3785` ms for the
candidate. App-deadline-missed frames fell from 144 to 125, and Photos consumed
0 ms scheduled CPU time in every controlled measured trace.

The baseline compatibility harness backports the same fixture, traversal, PSS
metric, and log probe onto `origin/master`, but it cannot hard-require memory
hits because the old per-composable loader reports none. The candidate harness
keeps the same traversal and adds the acceptance assertion. Therefore the two
harnesses are deliberately equivalent rather than byte-identical. Retained
logcat is the auditable source for cache hits because cache source is not a
Macrobenchmark JSON metric.

GitHub-hosted emulator CPU timing varied materially between two exact-head runs
even though all device tests passed. CI therefore invokes `-ReportOnly`: a >10%
change produces a GitHub warning, a red status in the retained JSON/Markdown,
and the job summary, but does not block a PR solely on shared-host timing.
Missing/malformed results, failed device tests, and parser/self-test failures
still fail the job. Omit `-ReportOnly` on a controlled runner or local comparison
to enforce the strict exit code.

## CI and evidence

`.github/workflows/performance.yml` runs for performance-relevant pull requests,
on matching pushes to the default branch, weekly, and on manual dispatch. It
retains benchmark JSON, managed-device test reports, regression reports, and
Perfetto traces for 30 days.

Before the image journey was added, the first approved local run completed all
five original tests on both managed devices:

- dry run: 5/5 on API 28 and 5/5 on API 37 in 4m 51s;
- corrected measured run: 5/5 on API 28 and 5/5 on API 37 in 16m 05s;
- app-only Baseline Profile generation: 3,857 `com/musfit` rules in about 7m.

Physical Pixel confirmation is non-gating. Do not install this debug-signed
benchmark target over a Play-signed installation; that requires explicit device
authorization and is outside the safe non-destructive evidence contract.
