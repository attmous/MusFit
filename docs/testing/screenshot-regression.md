# Screenshot regression testing

MusFit uses Roborazzi with Robolectric native graphics for deterministic JVM
goldens. The reviewed references live in `app/src/testInternalDebug/screenshots`;
CI verifies them and never records or replaces them.

The bounded matrix covers 400 × 800 dp phones, 610 × 900 dp foldable-sized
windows, and 900 × 700 dp tablet/desktop-sized windows. Across the seven
scenarios it exercises light and dark themes, LTR and RTL, normal and 1.5× font
scale, and representative Today, Food add, Training, Profile, scanner-denial,
and root-navigation states.

The same tests inspect every clickable semantics node. New targets must be at
least 48 dp in both dimensions. Existing undersized controls discovered while
the harness was introduced are named in `knownTouchTargetDebt`; later W5
accessibility packages remove those entries as they fix the controls. An
unrecognized regression fails immediately.

## Verify references

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat verifyRoborazziInternalDebug --no-daemon --console=plain
```

Failures retain the Roborazzi report, actual/compare images, and JUnit XML as
the `musfit-screenshot-regression` CI artifact. The screenshot job is separate
from managed-device and benchmark work and has a 12-minute hard timeout; the
initial seven-image local verification is recorded in the package PR.

## Review and update references

Reference changes are intentional review work. First run verification and
inspect its diff images. Only after approving every visual change, record:

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat recordRoborazziInternalDebug --no-daemon --console=plain
.\gradlew.bat verifyRoborazziInternalDebug --no-daemon --console=plain
```

Review every changed PNG in the source tree before committing. Never add a
record task, `verifyAndRecordRoborazzi`, or a reference-update flag to CI.
