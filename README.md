# MusFit

MusFit is an Android-only fitness and nutrition tracker inspired by Lifesum and Hevy.

## MVP Scope

- Meal and food tracking
- Barcode product lookup through Open Food Facts
- Structured workout logging
- Health Connect status, permissions, read summary, and workout export boundary
- Local-first Room database
- GitHub Actions debug APK builds

## Local Build

Requirements:

- JDK 17
- Android SDK with API 37
- PowerShell on Windows

Set up the local Android toolchain for this workspace:

```powershell
. .\.superpowers\sdd\android-env.ps1
```

Run the main verification/build commands:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions also builds the debug APK and uploads it as the `musfit-debug-apk` workflow artifact.

## Debug APK Usage

Install the generated debug APK on a connected Android device or emulator for local testing:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Documentation

Architecture and screen/model contracts are documented under `docs/architecture/`:

- `docs/architecture/README.md`
- `docs/architecture/screen-contracts.md`
- `docs/architecture/data-models.md`

Design-system guidance is documented under `docs/design/`:

- `docs/design/material-3-expressive.md`
- `docs/design/musfit-design-system.md`
- `docs/design/food-ui-guidelines.md`

## Privacy

The MVP stores health, meal, and workout data on-device. It has no account system, cloud sync, analytics, subscriptions, social features, AI coaching, or direct wearable cloud API integrations.
