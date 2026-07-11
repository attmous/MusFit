[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $EvidenceDir,
    [Parameter(Mandatory = $true)]
    [string] $Name,
    [Parameter(Mandatory = $true)]
    [string] $Caption,
    [string[]] $RequireText = @(),
    [ValidateSet("System", "Light", "Dark")]
    [string] $Theme = "System",
    [string] $DeviceSerial = "",
    [ValidateRange(1, 60)]
    [int] $WaitSeconds = 15
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-LastExitCode([string] $Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

if ($Name -notmatch '^[a-z0-9]+(?:-[a-z0-9]+)*$') {
    throw "Name must use lowercase letters, digits, and single hyphens only"
}
if ([string]::IsNullOrWhiteSpace($Caption)) {
    throw "Caption cannot be empty"
}

$repoRootOutput = & git rev-parse --show-toplevel 2>&1
Assert-LastExitCode "Resolve repository root"
$repoRoot = [IO.Path]::GetFullPath(($repoRootOutput | Select-Object -First 1).Trim())
if (-not [IO.Path]::IsPathRooted($EvidenceDir)) {
    $EvidenceDir = Join-Path $repoRoot $EvidenceDir
}
$resolvedEvidenceDir = [IO.Path]::GetFullPath($EvidenceDir)
$receiptPath = Join-Path $resolvedEvidenceDir "verification.json"
if (-not (Test-Path -LiteralPath $receiptPath)) {
    throw "Verification receipt not found. Run invoke-musfit-pr-gate.ps1 first: $receiptPath"
}

$receipt = Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if ($receipt.schemaVersion -ne 1 -or $receipt.status -ne "passed") {
    throw "Unsupported or unsuccessful verification receipt"
}
$targetPackagePattern = [regex]::Escape($receipt.device.packageName)

$headOutput = & git -C $repoRoot rev-parse HEAD 2>&1
Assert-LastExitCode "Resolve current HEAD"
$currentHead = ($headOutput | Select-Object -First 1).Trim()
if ($currentHead -ne $receipt.headSha) {
    throw "Current HEAD $currentHead does not match verified HEAD $($receipt.headSha)"
}

$worktreeChanges = @(& git -C $repoRoot status --porcelain --untracked-files=all 2>&1)
Assert-LastExitCode "Inspect worktree state"
if ($worktreeChanges.Count -gt 0) {
    throw "Worktree changes appeared after verification; rerun the gate from a clean PR head"
}

if (-not $DeviceSerial) {
    $DeviceSerial = $receipt.device.serial
}
if ($DeviceSerial -notmatch '^emulator-\d+$') {
    throw "Only Android emulator serials are allowed, not: $DeviceSerial"
}

$envScript = Join-Path $repoRoot "scripts\android\android-env.ps1"
if (-not (Test-Path -LiteralPath $envScript)) {
    throw "MusFit Android environment helper is missing: $envScript"
}
. $envScript

$deviceState = (& adb -s $DeviceSerial get-state 2>&1 | Select-Object -First 1).Trim()
Assert-LastExitCode "Read emulator state"
if ($deviceState -ne "device") {
    throw "Emulator is not ready: $DeviceSerial ($deviceState)"
}

$avdOutput = @(& adb -s $DeviceSerial emu avd name 2>&1)
Assert-LastExitCode "Read emulator AVD name"
$actualAvdName = $avdOutput |
    ForEach-Object { $_.ToString().Trim() } |
    Where-Object { $_ -and $_ -ne "OK" } |
    Select-Object -First 1
if ($actualAvdName -ne "MusFit_API36" -or $receipt.device.avdName -ne "MusFit_API36") {
    throw "Evidence must use the dedicated MusFit_API36 emulator, not '$actualAvdName'"
}

if ($Theme -ne "System") {
    $uiModeOutput = @(& adb -s $DeviceSerial shell cmd uimode night 2>&1)
    Assert-LastExitCode "Read emulator UI mode"
    $expectedMode = if ($Theme -eq "Dark") { "yes" } else { "no" }
    if (($uiModeOutput -join "`n") -notmatch "Night mode:\s+$expectedMode") {
        throw "Emulator is not in expected $Theme mode. Set the theme, restart MusFit, navigate to the target state, then capture."
    }
}

$initialActivityState = @(& adb -s $DeviceSerial shell dumpsys activity activities 2>&1)
Assert-LastExitCode "Inspect foreground activity before capture"
if (($initialActivityState -join "`n") -notmatch "(mResumedActivity|topResumedActivity).*$targetPackagePattern") {
    throw "MusFit must already be foregrounded on the exact target state before capture"
}

$screenshotDir = Join-Path $resolvedEvidenceDir "screenshots"
New-Item -ItemType Directory -Path $screenshotDir -Force | Out-Null
$pngPath = Join-Path $screenshotDir "$Name.png"
$xmlPath = Join-Path $screenshotDir "$Name.xml"
$metadataPath = Join-Path $screenshotDir "$Name.json"
foreach ($candidate in @($pngPath, $xmlPath, $metadataPath)) {
    if (Test-Path -LiteralPath $candidate) {
        throw "Evidence name already exists; choose a new ordered name: $candidate"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmssfff"
$devicePng = "/sdcard/musfit-pr-$timestamp.png"
$deviceXml = "/sdcard/musfit-pr-$timestamp.xml"

try {
    $deadline = [DateTime]::UtcNow.AddSeconds($WaitSeconds)
    $uiXml = $null
    $lastDump = ""
    do {
        $dumpOutput = @(& adb -s $DeviceSerial shell uiautomator dump $deviceXml 2>&1)
        $dumpExit = $LASTEXITCODE
        $lastDump = $dumpOutput -join "`n"
        if ($dumpExit -eq 0 -and $lastDump -notmatch 'null root node|ERROR') {
            $uiOutput = @(& adb -s $DeviceSerial shell cat $deviceXml 2>&1)
            if ($LASTEXITCODE -eq 0) {
                $uiXml = $uiOutput -join "`n"
                $missing = @($RequireText | Where-Object {
                    $uiXml.IndexOf($_, [StringComparison]::OrdinalIgnoreCase) -lt 0
                })
                if ($missing.Count -eq 0) {
                    break
                }
            }
        }
        $uiXml = $null
        Start-Sleep -Milliseconds 750
    } while ([DateTime]::UtcNow -lt $deadline)

    if (-not $uiXml) {
        $required = if ($RequireText.Count -gt 0) { $RequireText -join ', ' } else { '<none>' }
        throw "UI did not stabilize with required text [$required]. Last dump output: $lastDump"
    }

    $activityState = @(& adb -s $DeviceSerial shell dumpsys activity activities 2>&1)
    Assert-LastExitCode "Inspect foreground activity"
    if (($activityState -join "`n") -notmatch "(mResumedActivity|topResumedActivity).*$targetPackagePattern") {
        throw "MusFit is not the resumed foreground activity"
    }

    $crashBuffer = @(& adb -s $DeviceSerial logcat -b crash -d 2>&1)
    Assert-LastExitCode "Inspect crash buffer"
    if (($crashBuffer -join "`n") -match "Process:\s+$targetPackagePattern|$targetPackagePattern.*FATAL EXCEPTION") {
        throw "MusFit crash-buffer evidence exists; fix the crash before capturing PR evidence"
    }

    & adb -s $DeviceSerial shell screencap -p $devicePng | Out-Null
    Assert-LastExitCode "Capture emulator screenshot"
    & adb -s $DeviceSerial pull $devicePng $pngPath | Out-Host
    Assert-LastExitCode "Pull emulator screenshot"
    & adb -s $DeviceSerial pull $deviceXml $xmlPath | Out-Host
    Assert-LastExitCode "Pull emulator UI tree"
} finally {
    & adb -s $DeviceSerial shell rm -f $devicePng $deviceXml 2>$null | Out-Null
}

$pngBytes = [IO.File]::ReadAllBytes($pngPath)
$pngSignature = @(137, 80, 78, 71, 13, 10, 26, 10)
if ($pngBytes.Length -le $pngSignature.Count) {
    throw "Screenshot is empty or truncated: $pngPath"
}
for ($i = 0; $i -lt $pngSignature.Count; $i++) {
    if ($pngBytes[$i] -ne $pngSignature[$i]) {
        throw "Screenshot does not have a valid PNG signature: $pngPath"
    }
}

$pngHash = (Get-FileHash -LiteralPath $pngPath -Algorithm SHA256).Hash.ToLowerInvariant()
$metadata = [ordered]@{
    schemaVersion = 1
    name = $Name
    caption = $Caption
    theme = $Theme.ToLowerInvariant()
    requiredText = @($RequireText)
    deviceSerial = $DeviceSerial
    headSha = $receipt.headSha
    capturedAtUtc = [DateTime]::UtcNow.ToString("o")
    image = "screenshots/$Name.png"
    uiTree = "screenshots/$Name.xml"
    sha256 = $pngHash
}
$metadataJson = $metadata | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText($metadataPath, $metadataJson, (New-Object Text.UTF8Encoding($false)))

Write-Host "Captured emulator evidence: $pngPath"
Write-Host "Caption: $Caption"
Write-Host "Inspect this PNG visually before publication."
