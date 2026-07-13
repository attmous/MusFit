# Performance benchmarks

MusFit measures a production-shaped, minified, profileable build instead of the
debug build. The benchmark surface covers cold and warm startup plus the Food,
Training, and Profile journeys. Journey runs capture frame timing, maximum heap
and anonymous RSS memory, and a Perfetto trace for every iteration.

## Targets

The `benchmark` module drives `com.musfit` through the `productionBenchmark`
variant. That app variant inherits the release configuration, R8/resource
shrinking, production network policy, and the generated app-owned Baseline
Profile. It is non-debuggable and is signed with the public debug key only so it
can be installed by local and CI managed devices; it is never a release
artifact.

The managed-device matrix is:

| Target | Purpose | Result transport |
| --- | --- | --- |
| API 28, Pixel 2, x86_64 | Minimum-supported-device execution | Instrumentation result and pass/fail |
| API 37, Pixel 2, x86_64, 16 KB pages | Current-platform execution and regression baseline | Benchmark JSON, test reports, and Perfetto traces |

Android's managed-device additional-output transport is unavailable below API
29. API 28 therefore remains an execution gate, while repeatable numeric
comparison uses API 37's exported benchmark JSON.

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
- any increase greater than 10% fails the workflow;
- exactly 10% remains within the approved boundary;
- a built-in deliberate-regression self-test proves that the comparison is
  capable of failing.

The initial approved values use the per-metric maximum from the complete local
run and the first exact-head GitHub-hosted run. That measured two-host envelope
avoids treating runner CPU differences as product regressions while retaining a
strict >10% failure above every verified environment. Later baseline updates
use a complete, reviewed API 37 run and must explain the measured product or
runner change; they must not be used simply to make a regression green.

## CI and evidence

`.github/workflows/performance.yml` runs for performance-relevant pull requests,
on matching pushes to the default branch, weekly, and on manual dispatch. It
retains benchmark JSON, managed-device test reports, regression reports, and
Perfetto traces for 30 days.

The first approved local run completed all five tests on both managed devices:

- dry run: 5/5 on API 28 and 5/5 on API 37 in 4m 51s;
- corrected measured run: 5/5 on API 28 and 5/5 on API 37 in 16m 05s;
- app-only Baseline Profile generation: 3,857 `com/musfit` rules in about 7m.

Physical Pixel confirmation is non-gating. Do not install this debug-signed
benchmark target over a Play-signed installation; that requires explicit device
authorization and is outside the safe non-destructive evidence contract.
