# Actionable coverage

MusFit uses the stable JaCoCo integration provided by the Android Gradle plugin.
Coverage is a review signal and change ratchet; it does not replace focused
behavior, migration, Compose, screenshot, or managed-device tests.

## Eligible source and thresholds

The policy in `config/coverage-policy.json` includes executable lines from:

- domain logic;
- repositories and remote/transfer boundaries;
- Health Connect integrations;
- ViewModels and the typed navigation state boundary.

Generated Hilt code, Room declarations/entities, dependency-injection wiring,
and Compose rendering boilerplate are excluded. The baseline at commit
`c754f7595f2b32a510ac2a5db79ac5a046fdeae5` is **57.8316%**
(8,385/14,499 eligible executable lines). The ratchet enforces:

- changed eligible business logic: at least 80% line coverage;
- changed domain or repository logic: at least 90% line coverage;
- overall eligible coverage: never below the checked-in baseline.

The 90% critical threshold applies to newly changed executable lines. Existing
repository debt remains visible in the aggregate summary and cannot lower the
overall ratio, but belongs to focused behavior packages rather than being hidden
by exclusions or inflated with rendering tests.

## Local report

Initialize the Android environment, generate the HTML/XML unit report, then run
the policy against the branch diff:

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat :app:createInternalDebugUnitTestCoverageReport --no-daemon --console=plain
.\scripts\coverage\verify-coverage.ps1 `
  -ReportPath app\build\reports\coverage\test\internal\debug\report.xml `
  -BaseRef origin/master
```

The browsable report is written beneath
`app/build/reports/coverage/test/internal/debug/`. The aggregate machine and
review summaries are written to `build/reports/coverage-policy/`.

## Unit and instrumented aggregation

The policy accepts multiple JaCoCo XML reports and merges coverage by source
line: a line covered in either the unit or instrumented report is covered in the
aggregate. The managed-device workflow runs the existing API 28/API 37 critical
journeys, creates the AGP managed-device report, adds the unit report, and uploads
both original dashboards plus the aggregate JSON/Markdown summary.

Coverage-producing Orchestrator runs pass the MusFit-owned
`musfit.testInstrumentationRunnerArguments.clearPackageData=false` project
property, which is wired into the runner configuration. Orchestrator
otherwise removes the target package's coverage payload before AGP can collect
it. This exception applies only to the isolated coverage rerun; ordinary
instrumentation keeps `clearPackageData=true`, and the critical-journey tests
continue to reset their own state.

Normal pull-request CI runs the unit report because the device matrix is a
separate, serialized lane. Both workflows retain their reports; no coverage
service, repository token, or source upload is required.

Robolectric loads Android-facing repositories through its sandbox classloader.
Unit-test JaCoCo tasks therefore include no-location classes and exclude JDK
internals; without that task configuration, executable repository tests pass
but their production classes are incorrectly reported as entirely uncovered.

## Runtime budget and baseline updates

The measured clean local unit report completed in 223.6 seconds for 814 tests;
the warm report-only rerun completed in 16.6 seconds. The unit coverage step
has a hard CI budget of **10 minutes**. Managed-device coverage reuses the
already-required critical-journey execution and has an additional 15-minute
reporting budget. The encompassing critical-journey job has a 55-minute budget
so the 32-minute two-device journey allowance, coverage, and artifact retention
can all complete.

Do not lower a threshold or replace the baseline merely to make CI green. A
baseline update requires a measured report from current `origin/master`, the
exact covered/missed counts, a full commit SHA, and review evidence explaining
why eligible source ownership changed.
