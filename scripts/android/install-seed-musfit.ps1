param(
    [string] $AvdName = "MusFit_API36",
    [string] $DeviceSerial = "",
    [switch] $Reset,
    [switch] $SkipBuild,
    [switch] $Headless,
    [switch] $NoLaunch
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $repoRoot "scripts\android\android-env.ps1")

$emulator = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"

function Get-EmulatorSerial {
    $lines = & adb devices
    foreach ($line in $lines) {
        if ($line -match "^(emulator-\d+)\s+device$") {
            return $Matches[1]
        }
    }
    return $null
}

function Wait-ForEmulatorBoot([string] $serial) {
    & adb -s $serial wait-for-device
    for ($i = 0; $i -lt 120; $i++) {
        $boot = (& adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
        if ($boot -eq "1") {
            & adb -s $serial shell input keyevent 82 2>$null | Out-Null
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for emulator boot: $serial"
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $DeviceSerial = Get-EmulatorSerial
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $arguments = @("-avd", $AvdName)
    if ($Headless) {
        $arguments += @("-no-window", "-no-audio")
    }
    Start-Process -FilePath $emulator -ArgumentList $arguments

    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 2
        $DeviceSerial = Get-EmulatorSerial
        if (-not [string]::IsNullOrWhiteSpace($DeviceSerial)) {
            break
        }
    }
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    throw "No running emulator found. Start $AvdName or pass -DeviceSerial emulator-5554."
}

Wait-ForEmulatorBoot $DeviceSerial
Write-Host "Using emulator: $DeviceSerial"

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat assembleDebug --no-daemon --console=plain
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Debug APK not found: $apk"
}

& adb -s $DeviceSerial install -r $apk

$resetValue = if ($Reset) { "true" } else { "false" }
& adb -s $DeviceSerial shell am broadcast `
    --receiver-foreground `
    -a com.musfit.debug.SEED_TEST_DATA `
    -n com.musfit/com.musfit.debug.MusFitDebugSeedReceiver `
    --ez reset $resetValue

if (-not $NoLaunch) {
    & adb -s $DeviceSerial shell monkey -p com.musfit -c android.intent.category.LAUNCHER 1
}

Write-Host "MusFit debug APK installed and seeded on $DeviceSerial (reset=$resetValue)."
