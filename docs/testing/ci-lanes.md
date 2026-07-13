# CI lanes and budgets

MusFit separates fast pull-request feedback from heavier default-branch and
nightly evidence. Every release candidate must be the exact commit verified by
all three push-triggered workflows below. Release preflight queries GitHub for
those exact SHA-bound runs before any signing, Play upload, or promotion job can
start.

## PR latency budget

| Workflow/job | Trigger | Hard budget | Required evidence |
| --- | --- | ---: | --- |
| Android / `verify` | Every PR, default-branch push, manual | 30 minutes target | Workflow contract; configuration-cache contract; internal, migration-bridge, and production unit/lint/build; APK/AAB identities; R8 reports; unit/lint reports |
| Android performance / `macrobenchmark` | Performance-relevant PRs | 35 minutes | API 28 execution, API 37 benchmark JSON, summary, regression report, and Perfetto traces |

PRs do not boot managed UI/migration devices or run screenshot verification.
Those checks run after merge and nightly, while performance-relevant PRs retain
the dedicated production-shaped performance signal.

## Nightly and main budget

`Android device and UI` runs on every default-branch push, nightly at 02:41 UTC,
and on manual dispatch. Its jobs run in parallel on isolated runners:

| Job | Matrix | Hard timeout | Retained evidence |
| --- | --- | ---: | --- |
| `migration-matrix` | API 28 and API 37, serialized per runner | 40 minutes | Managed-device reports, protobuf/XML results, additional output, migration metrics |
| `critical-journeys` | API 28 and API 37, serialized per runner | 35 minutes | Managed-device reports, failure logcat, screenshots/additional output |
| `screenshots` | Deterministic 400/610/900 dp Roborazzi matrix | 12 minutes | HTML/JUnit reports and image diffs |

`Android performance` also runs on every default-branch push, weekly, and on
manual dispatch with a 35-minute timeout. This guarantees every commit eligible
for release has an exact-SHA performance/profile run, even when the commit did
not touch a performance path. API 28 and API 37 execute serially on the hosted
runner so their emulator VMs do not compete for memory after R8 packaging.

## Release promotion contract

Production release preflight requires successful push runs for the exact commit
from:

- `Android` (`.github/workflows/android.yml`);
- `Android device and UI` (`.github/workflows/device-ui.yml`);
- `Android performance` (`.github/workflows/performance.yml`).

The candidate job checks out only that verified SHA, reruns the release gate,
signs and uploads its AAB, verifies the Google-signed universal APK, and stores
one immutable candidate artifact. The promotion job downloads and reverifies
that artifact without rebuilding it. Its release notes record all three source
workflow run IDs.
