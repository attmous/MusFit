param(
    [string] $AvdName = "MusFit_API36",
    [string] $DeviceSerial = "",
    [switch] $Reset,
    [switch] $SkipBuild,
    [switch] $Headless,
    [switch] $NoLaunch,
    [string] $EvidenceDir = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $repoRoot "scripts\android\android-env.ps1")

$emulator = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"
$targetPackage = "com.musfit.internal"
$testPackage = "com.musfit.internal.test"
$mainComponent = "com.musfit.internal/com.musfit.MainActivity"
$apk = Join-Path $repoRoot "app\build\outputs\apk\internal\debug\app-internal-debug.apk"
$testApk = Join-Path $repoRoot "app\build\outputs\apk\androidTest\internal\debug\app-internal-debug-androidTest.apk"

function Assert-LastExitCode([string] $Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

function Get-EmulatorAvdName([string] $serial) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & adb -s $serial emu avd name 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        return $null
    }

    @($output |
        ForEach-Object { $_.ToString().Trim() } |
        Where-Object { $_ -and $_ -ne "OK" }) |
        Select-Object -First 1
}

function Get-EmulatorSerial {
    $lines = & adb devices
    Assert-LastExitCode "adb devices"
    foreach ($line in $lines) {
        if ($line -match "^(emulator-\d+)\s+device$") {
            $serial = $Matches[1]
            if ((Get-EmulatorAvdName $serial) -eq $AvdName) {
                return $serial
            }
        }
    }
    return $null
}

function Wait-ForEmulatorBoot([string] $serial) {
    & adb -s $serial wait-for-device
    Assert-LastExitCode "adb wait-for-device"
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

function Wait-ForUiDump([string] $serial, [string] $devicePath) {
    for ($i = 0; $i -lt 8; $i++) {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $dumpOutput = & adb -s $serial shell uiautomator dump $devicePath 2>&1
            $dumpExitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        $dumpText = $dumpOutput -join "`n"
        if ($dumpExitCode -eq 0 -and $dumpText -notmatch "null root node" -and $dumpText -notmatch "ERROR") {
            $dumpOutput | ForEach-Object { Write-Host $_ }
            return $true
        }

        Start-Sleep -Seconds 1
    }

    return $false
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $DeviceSerial = Get-EmulatorSerial
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $arguments = @("-avd", $AvdName)
    if ($Headless) {
        $arguments += @("-no-window", "-no-audio")
    }
    $startArgs = @{
        FilePath = $emulator
        ArgumentList = $arguments
    }
    if ($Headless) {
        $startArgs.WindowStyle = "Hidden"
    }
    Start-Process @startArgs

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

if ($DeviceSerial -notmatch '^emulator-\d+$') {
    throw "Refusing to seed or reset non-emulator device '$DeviceSerial'. Use a dedicated Android emulator."
}

$actualAvdName = Get-EmulatorAvdName $DeviceSerial
if ($actualAvdName -ne $AvdName) {
    throw "Refusing to seed emulator '$DeviceSerial' because it is AVD '$actualAvdName', not '$AvdName'."
}

Wait-ForEmulatorBoot $DeviceSerial
Write-Host "Using emulator: $DeviceSerial"

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat assembleInternalDebug assembleInternalDebugAndroidTest --no-daemon --console=plain
        Assert-LastExitCode "assembleInternalDebug and assembleInternalDebugAndroidTest"
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Internal APK not found: $apk"
}
if (-not (Test-Path -LiteralPath $testApk)) {
    throw "Internal instrumentation APK not found: $testApk"
}

& adb -s $DeviceSerial install -r $apk
Assert-LastExitCode "adb install"
& adb -s $DeviceSerial install -r $testApk
Assert-LastExitCode "adb install instrumentation APK"

if ($Reset) {
    $clearOutput = & adb -s $DeviceSerial shell pm clear $targetPackage 2>&1
    $clearExitCode = $LASTEXITCODE
    $clearOutput | ForEach-Object { Write-Host $_ }
    if ($clearExitCode -ne 0 -or (($clearOutput -join "`n") -notmatch "Success")) {
        throw "App data reset failed. Output: $($clearOutput -join ' ')"
    }
}

$resetValue = if ($Reset) { "true" } else { "false" }
$seedOutput = & adb -s $DeviceSerial shell am instrument `
    -w `
    -r `
    -e reset $resetValue `
    -e class com.musfit.debug.MusFitDebugSeedInstrumentationTest `
    $testPackage/androidx.test.runner.AndroidJUnitRunner 2>&1
$seedExitCode = $LASTEXITCODE
$seedOutput | ForEach-Object { Write-Host $_ }
if ($seedExitCode -ne 0) {
    throw "Internal seed instrumentation failed with exit code $seedExitCode"
}
$seedText = $seedOutput -join "`n"
if ($seedText -match "FAILURES!!!" -or $seedText -notmatch "OK \(1 test\)" -or $seedText -notmatch "INSTRUMENTATION_CODE: -1") {
    throw "Internal seed instrumentation did not pass. Output: $($seedOutput -join ' ')"
}

if (-not $NoLaunch) {
    & adb -s $DeviceSerial shell am start -W -n $mainComponent
    Assert-LastExitCode "explicit app launch"
    Start-Sleep -Seconds 2
}

if (-not [string]::IsNullOrWhiteSpace($EvidenceDir)) {
    $resolvedEvidenceDir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($EvidenceDir)
    New-Item -ItemType Directory -Force -Path $resolvedEvidenceDir | Out-Null

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $screenshotDevicePath = "/sdcard/musfit-evidence-$timestamp.png"
    $uiDevicePath = "/sdcard/musfit-window-$timestamp.xml"
    $screenshotPath = Join-Path $resolvedEvidenceDir "musfit-$DeviceSerial-$timestamp.png"
    $uiPath = Join-Path $resolvedEvidenceDir "musfit-$DeviceSerial-$timestamp.xml"
    $logPath = Join-Path $resolvedEvidenceDir "musfit-$DeviceSerial-$timestamp-logcat.txt"

    & adb -s $DeviceSerial shell screencap -p $screenshotDevicePath
    Assert-LastExitCode "screencap"
    & adb -s $DeviceSerial pull $screenshotDevicePath $screenshotPath
    Assert-LastExitCode "pull screenshot"
    & adb -s $DeviceSerial shell rm $screenshotDevicePath
    Assert-LastExitCode "remove device screenshot"

    if (Wait-ForUiDump $DeviceSerial $uiDevicePath) {
        & adb -s $DeviceSerial pull $uiDevicePath $uiPath
        Assert-LastExitCode "pull UI dump"
        & adb -s $DeviceSerial shell rm $uiDevicePath
        Assert-LastExitCode "remove device UI dump"
    } else {
        $warningPath = Join-Path $resolvedEvidenceDir "musfit-$DeviceSerial-$timestamp-ui-warning.txt"
        "uiautomator dump did not produce a non-empty root after retrying." |
            Out-File -LiteralPath $warningPath -Encoding utf8
        Write-Warning "UI dump was not available after retrying; wrote $warningPath"
    }

    & adb -s $DeviceSerial logcat -d | Out-File -LiteralPath $logPath -Encoding utf8
    Assert-LastExitCode "logcat capture"

    Write-Host "Evidence written to $resolvedEvidenceDir"
}

Write-Host "MusFit internal APK installed and instrumentation-seeded on $DeviceSerial (reset=$resetValue)."
