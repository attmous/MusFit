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

Run a quick structural/device smoke pass. Keep the device-specific tasks in
separate Gradle invocations when the host is capped to one managed device; a
group task can make the second device exceed Gradle's 600-second lock wait while
the first suite is still active.

```powershell
.\gradlew.bat :benchmark:benchmarkApi28BenchmarkAndroidTest `
  '-Pandroid.experimental.testOptions.managedDevices.maxConcurrentDevices=1' `
  '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true' `
  --no-daemon --console=plain
.\gradlew.bat :benchmark:benchmarkApi37BenchmarkAndroidTest `
  '-Pandroid.experimental.testOptions.managedDevices.maxConcurrentDevices=1' `
  '-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true' `
  --no-daemon --console=plain
```

Run the measured suite and compare it with the approved baseline:

```powershell
.\gradlew.bat :benchmark:benchmarkApi28BenchmarkAndroidTest `
  '-Pandroid.experimental.testOptions.managedDevices.maxConcurrentDevices=1' `
  --no-daemon --console=plain
.\gradlew.bat :benchmark:benchmarkApi37BenchmarkAndroidTest `
  '-Pandroid.experimental.testOptions.managedDevices.maxConcurrentDevices=1' `
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
`8913598edcd7a67a6e17520437b1c3d396c548fb`; the final candidate product-code
head was `a5260d993699e4211b2e94992de038f0bd80ad07`. The exact-head rerun was made
after the restoration fix; subsequent S15 changes are behavior-preserving
static-analysis refactors plus acceptance tests and this evidence documentation.

| Acceptance metric | Controlled master | Exact-head candidate | Change |
| --- | ---: | ---: | ---: |
| Frame CPU P90 | 15.0513 ms | 12.92425 ms | -14.13% |
| Frame overrun P90 | 2.216126 ms | -1.224151 ms | -155.24% |
| Maximum heap | 15,463 KB | 14,344 KB | -7.24% |
| Maximum anonymous RSS | 104,036 KB | 102,784 KB | -1.20% |
| Maximum end-of-browse total PSS | 74,430 KB | 67,418 KB | -9.42% |
| Memory-cache hits per setup/measured traversal | 0/100 | 100/100 in all five iterations | behavioral gate passed |

Master PSS samples were `69,470, 68,524, 72,014, 73,578, 74,430` KB; exact-head
candidate samples were `66,940, 66,549, 64,170, 64,928, 67,418` KB. Exact-head
candidate maximum-heap samples were `14,344, 14,231, 14,167, 14,023, 14,167`
KB, and anonymous-RSS samples were `102,784, 98,532, 98,616, 100,660,
102,356` KB. The retained five traces and benchmark JSON are the auditable
source for the frame distributions and memory values; retained logcat confirms
100/100 memory-cache hits in every setup and measured traversal.

The baseline compatibility harness backports the same fixture, traversal, PSS
metric, and log probe onto `origin/master`, but it cannot hard-require memory
hits because the old per-composable loader reports none. The candidate harness
keeps the same traversal and adds the acceptance assertion. Therefore the two
harnesses are deliberately equivalent rather than byte-identical. Retained
logcat is the auditable source for cache hits because cache source is not a
Macrobenchmark JSON metric.

## Training chart and adaptive-scene comparison (2026-07-19)

The seeded `MusFit_API36` emulator supplied direct `gfxinfo` checks for the two
S15 interactions not isolated by the production-shaped Macrobenchmark. Both
comparisons preserved Room data while switching between baseline commit
`22487be46fb1d6e7602c17f49a01166f11eb6f81` and the candidate. The Progress
chart journey performed 25 repeated chart selections in compact layout. Its
frame P90 remained 18 ms (0.00% regression), while P99 improved from 65 ms to
34 ms.

The expanded journey used a 1080 x 2400 display at a 160 dpi override and, per
sample, three warm-ups followed by 25 Profile -> Training -> bidirectional
routine-scroll -> alternating routine-selection cycles. Three matched samples
were taken for each exact revision; the candidate source was
`a5260d993699e4211b2e94992de038f0bd80ad07`.

| Expanded metric | Baseline samples | Candidate samples | Median change |
| --- | --- | --- | ---: |
| Frame P90 | 18, 30, 32 ms | 19, 21, 27 ms | 30 -> 21 ms (-30.00%) |
| Janky frames | 8.35%, 8.62%, 9.55% | 7.23%, 6.91%, 6.88% | 8.62% -> 6.91% (-19.84%) |

Matched medians are used because the ADB-driven path showed discrete histogram
and synthetic-input scheduling variance. These direct checks satisfy the chart
and expanded-interaction <=5% contracts; the managed-device Macrobenchmark and
its regression verifier remain the authoritative production-shaped gate.

The same candidate was also captured in a 35.85-second Perfetto lifecycle
trace. While Training was foregrounded, its `arch_disk_io_*` threads ran 329
slices (240.184 ms), confirming that the trace could see repository work. The
activity stopped 0.705 seconds after HOME; after its five-second
`WhileSubscribed` timeout and a conservative 95 ms drain allowance, there were
zero main, RenderThread, `arch_disk_io_*`, pool, Dispatcher, Room, or SQLite
scheduling slices for the remaining 15.96 seconds. Only 0.138 ms of unrelated
ART finalizer/heap housekeeping remained. This is the device acceptance check
for Training's lifecycle-aware collectors.

## Profile and Today lifecycle comparison (2026-07-20)

S16 used the strict API 37 regression verifier against the checked-in approved
baselines. Profile's production-shaped journey improved frame CPU P90 by
19.58% and frame-overrun P90 by 43.44%; its maximum heap and anonymous RSS also
improved by 9.93% and 0.33%. Today's warm-start time-to-initial-display P90
improved by 32.49%. All five measurements passed the 5% regression ceiling.

The seeded `MusFit_API36` emulator also supplied a matched Today interaction
comparison between exact master commit
`f0e64e5b5317d6954b20b63b7eb38ba4174f4a29` and candidate source
`b05b388b9307251c18589469bf9b5f36ddbb5104`. Each of the three samples used
three warm-ups followed by 25 bidirectional dashboard-scroll cycles, with
`gfxinfo framestats` reset immediately before the measured block. Room data and
the emulator session were preserved while switching revisions.

| Today frame metric | Baseline samples | Candidate samples | Median change |
| --- | --- | --- | ---: |
| P90 | 23.682, 24.509, 24.694 ms | 24.266, 23.503, 24.997 ms | 24.509 -> 24.266 ms (-0.99%) |
| P99 | 25.796, 29.106, 27.572 ms | 26.589, 26.856, 26.903 ms | 27.572 -> 26.856 ms (-2.60%) |

The same candidate was captured in a 60-second Perfetto lifecycle trace with
explicit Profile/Today foreground and post-timeout markers. Profile foreground
recorded 1,635 relevant scheduling slices (1,203.051 ms CPU), and Today
foreground recorded 4,854 (3,324.127 ms CPU), confirming that the trace could
observe UI, JIT, and repository work. The post-timeout windows began 5.549 and
5.559 seconds after their respective `activityStop` slices and each ran for
more than ten seconds. Both contained zero main, RenderThread,
`arch_disk_io_*`, pool, Dispatcher, Room, SQLite, or DefaultExecutor scheduling
slices (0.000 ms CPU). The only whole-process activity was one 9.300 ms ART
profile-save pass and one 0.070 ms finalizer-watchdog tick during Today's
window; neither is application collector or query work.

GitHub-hosted emulator CPU timing varied materially between two exact-head runs
even though all device tests passed. CI therefore invokes `-ReportOnly`: a >10%
change produces a GitHub warning, a red status in the retained JSON/Markdown,
and the job summary, but does not block a PR solely on shared-host timing.
Missing/malformed results, failed device tests, and parser/self-test failures
still fail the job. Omit `-ReportOnly` on a controlled runner or local comparison
to enforce the strict exit code.

## Food and Training localization comparison (2026-07-20)

S17 ran the Food and Training production-shaped journeys from exact candidate
source `14a4e639b75726f81d56ce7c371024395918ab2b` on the API 37 managed device.
Each exact benchmark ID was compared independently with the checked-in approved
baseline and a strict 5% regression threshold. All eight approved measurements
passed.

| Journey metric | Approved | Candidate | Change |
| --- | ---: | ---: | ---: |
| Food frame CPU P90 | 104.6214 ms | 40.7240 ms | -61.07% |
| Food frame overrun P90 | 130.0770 ms | 41.0571 ms | -68.44% |
| Food maximum heap | 22,664 KB | 22,596 KB | -0.30% |
| Food maximum anonymous RSS | 108,560 KB | 99,152 KB | -8.67% |
| Training frame CPU P90 | 256.9968 ms | 81.6320 ms | -68.24% |
| Training frame overrun P90 | 441.3738 ms | 99.9025 ms | -77.37% |
| Training maximum heap | 22,672 KB | 16,908 KB | -25.42% |
| Training maximum anonymous RSS | 99,844 KB | 95,904 KB | -3.95% |

The seeded `MusFit_API36` emulator supplied final compact-layout captures and UI
trees for Food under `en-XA` at 1.5x font scale, Training under `ar-XB` at 1.5x
font scale and RTL, and Training under `de-DE`. The live `en-XA` pass exposed a
screen-header width collapse that was fixed by stacking trailing actions below
the title at large font scales. The reviewed Roborazzi baselines retain that
header regression plus Food add, Training RTL, and expanded German coverage.

## S20 measured closeout (2026-07-20)

S20 selected the warmed Training image-browse journey because exact PR #176
run `29751311490` reported frame CPU P90 `105.6049` ms and frame-overrun P90
`126.8751` ms. All five iterations showed the same slow envelope. Perfetto
then ruled out speculative stability annotations, broad caching, repository
queries, Binder, and I/O: across 798 MusFit frames, RenderThread drawing used
`27,979.241` ms of CPU and `eglSwapBuffersWithDamageKHR` used `23,359.490` ms.
The EGL slices were running for `23,359.490` ms, runnable or preempted for
`4,526.280` ms, sleeping for `1,086.824` ms, and never in I/O wait. The Ranchu
graphics composer and SurfaceFlinger used another `21,001.593` and `9,974.639`
ms of scheduled CPU. Main-thread recomposition used only `173.885` ms total.

The hosted trace therefore identified a graphics-host envelope, not an app
query or recomposition root cause that justified a production-code change. A
consecutive same-host comparison used the same API 37 image, fingerprint,
five-iteration fixture, Photos exclusion, and 100/100 warmed-memory-cache
behavior gate for exact S15 product head
`a5260d993699e4211b2e94992de038f0bd80ad07` and current master
`50faaaef98b0b5674fd7d5e1294f8ec69177dd1f`:

| Controlled image metric | Exact S15 | Current master | Change |
| --- | ---: | ---: | ---: |
| Frame CPU P90 | 32.4428 ms | 28.9724 ms | -10.70% |
| Frame overrun P90 | 28.6866264 ms | 23.0809378 ms | -19.54% |
| Median frame count | 844 | 858 | +1.66% |
| Maximum heap | 14,407 KB | 14,506 KB | +0.69% |
| Maximum anonymous RSS | 102,648 KB | 101,740 KB | -0.88% |
| Maximum end-of-browse total PSS | 68,840 KB | 67,946 KB | -1.30% |

The improvement is not explained by a shorter traversal: current master drew
more frames while lowering the selected P90s. Representative iteration-zero
trace slices also moved coherently: RenderThread drawing averaged `8.399` to
`7.699` ms per frame, EGL swap `5.107` to `4.608` ms, main input `0.595` to
`0.507` ms, traversal `0.507` to `0.466` ms, and total recomposition CPU
`204.160` to `183.863` ms. This satisfies W5-PERF-01's measured >=10%
closeout without adding an unproven cache or stability annotation.

The same fingerprint cannot identify host graphics performance: the exact
hosted image result was more than three times the controlled current P90. The
approved file now records both evidence chains and uses the reviewed per-metric
maximum as the cross-host regression envelope. It also calibrates the hosted
Training frame CPU result (`324.9192` ms), which a controlled current-head run
disproved as a product regression: Training frame CPU P90 was `168.31104` ms
against `256.9968` ms approved (-34.51%), and frame-overrun P90 was
`253.895717` ms against `441.3738` ms (-42.47%). The recalibrated baseline
passes all 19 measurements from exact PR #176 while the deliberate >10%
self-test and controlled strict mode remain capable of failing.

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
