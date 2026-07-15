# Compose testing

MusFit uses two deliberately small UI-test layers.

Screenshot, adaptive, and accessibility regression coverage is documented in
[`screenshot-regression.md`](screenshot-regression.md). It reuses this local
fixture boundary but runs as a separate deterministic CI lane.

## Local semantics layer

`app/src/testInternalDebug` hosts Robolectric-backed Compose tests. These tests
render real composables with immutable UI state and callback fakes. They verify
semantics, user actions, navigation state, and saved-instance-state restoration
without Hilt, Room, network, camera, Health Connect, or an emulator.

Keep tests in this layer when the contract can be proven from the semantics
tree and an in-memory callback. Prefer user-visible text, content descriptions,
selection state, and scroll actions over layout coordinates or implementation
tags. A test should not repeat calculations already covered by ViewModel or
repository unit tests.

Run the focused suite with:

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat testInternalDebugUnitTest --tests "com.musfit.ui.MusFitComposeSemanticsTest" --no-daemon --console=plain
```

The Compose host manifest is a `debugImplementation` dependency, following the
Compose testing contract. It is excluded from production and migration APKs;
the shared JUnit test API remains on the unit-test classpath. Local tests are
pinned to SDK 35 because Robolectric 4.16 does not provide an SDK 37 sandbox.

## Managed-device journey layer

Use managed-device tests for contracts that require Android framework ownership:
Hilt wiring, Room persistence, process death, camera/scanner round-trips,
permission denial and recovery, Health Connect, offline transport, and complete
Food or Training journeys. Those tests belong to `W2-TEST-02B`; do not recreate
their framework behavior in Robolectric fakes.

The critical journey matrix is:

| Journey | Local semantics | Managed device |
| --- | --- | --- |
| Visit-order navigation and restoration | Yes | Final route/back verification |
| Food logging and scanner return | Surface callbacks only | Full persistence and scanner route |
| Training start, set, rest, finish | Entry/action semantics | Full Room-owned workout lifecycle |
| Profile and settings | Hub actions and state | Persistence, permissions, external boundaries |
| Denied permission and offline service | Presentation state only | Framework denial/recovery and transport |

Every UI test must name the user contract it protects. Add a new high-level
journey only when a lower layer cannot provide equivalent confidence.

`MusFitCriticalJourneyInstrumentationTest` is the managed-device layer. Its
seven orchestrated cases cover:

- a real Hilt/Room Food quick log and persistence after Activity recreation;
- Food database search-draft restoration after Activity recreation without a
  database write;
- the Room-owned active-workout set/rest lifecycle and leaving/re-entering the
  Training destination;
- Profile preference persistence plus restored visit-order back navigation;
- unsaved Profile editor draft restoration across Activity recreation;
- a deterministic scanner-result return on both devices, with fixed
  camera-permission denial and offline transport additionally exercised on API
  37 (the Android 9 legacy permission dialog is intentionally not automated);
- 20 rapid scanner open/back/rotation cycles without retaining a camera
  session.

The instrumentation-only seeder resets each isolated test process. External
services and camera hardware are never required. Every wait is bounded to 15
seconds. A failing case writes a UI hierarchy and screenshot to Gradle's
additional-test-output directory on API 29+; Gradle does not support additional
test output on API 28, where the managed-device runner still retains JUnit and
logcat output.

Run the API 28/37 lane with:

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat criticalJourneysApi28And37GroupInternalDebugAndroidTest `
  '-Pmusfit.testInstrumentationRunnerArguments.class=com.musfit.ui.MusFitCriticalJourneyInstrumentationTest' `
  --no-daemon --console=plain
```

The suite has no automatic retry: any failure is treated as real and its
artifacts are reviewed. The CI budget is 16 minutes per device (32 minutes for
the two-device lane); the local API 36 baseline is recorded in the PR evidence.
