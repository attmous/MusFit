[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $OutputDir,
    [string] $DeviceSerial = "",
    [switch] $IncludeWorkflowContract,
    [switch] $Headless
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-LastExitCode([string] $Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

function Invoke-LoggedPowerShell(
    [string] $PowerShellExe,
    [string] $ScriptPath,
    [string[]] $Arguments,
    [string] $LogPath,
    [string] $Label
) {
    $command = @(
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy", "Bypass",
        "-File", $ScriptPath
    ) + $Arguments

    Write-Host "Running $Label"
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $rawOutput = @(& $PowerShellExe @command 2>&1)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    $rawOutput |
        ForEach-Object { $_.ToString() } |
        Tee-Object -FilePath $LogPath |
        ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "$Label failed with exit code $exitCode. See $LogPath"
    }
}

$repoRootOutput = & git rev-parse --show-toplevel 2>&1
Assert-LastExitCode "Resolve repository root"
$repoRoot = [IO.Path]::GetFullPath(($repoRootOutput | Select-Object -First 1).Trim())

$worktreeChanges = @(& git -C $repoRoot status --porcelain --untracked-files=all 2>&1)
Assert-LastExitCode "Inspect worktree state"
if ($worktreeChanges.Count -gt 0) {
    throw "Worktree changes would make the build differ from the PR head. Use a clean PR worktree first.`n$($worktreeChanges -join "`n")"
}

if (-not [IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir = Join-Path $repoRoot $OutputDir
}
$resolvedOutputDir = [IO.Path]::GetFullPath($OutputDir)
$verificationRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot "verification"))
$verificationPrefix = $verificationRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
if (-not $resolvedOutputDir.StartsWith($verificationPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "OutputDir must be a child of the repository's gitignored verification directory: $verificationRoot"
}

if (Test-Path -LiteralPath $resolvedOutputDir) {
    $existing = @(Get-ChildItem -LiteralPath $resolvedOutputDir -Force)
    if ($existing.Count -gt 0) {
        throw "Evidence directory is not empty; use a new timestamped directory: $resolvedOutputDir"
    }
} else {
    New-Item -ItemType Directory -Path $resolvedOutputDir -Force | Out-Null
}

$headShaOutput = & git -C $repoRoot rev-parse HEAD 2>&1
Assert-LastExitCode "Resolve HEAD"
$headSha = ($headShaOutput | Select-Object -First 1).Trim()
$startedAt = [DateTime]::UtcNow
$targetPackage = "com.musfit.internal"
$mainActivity = "com.musfit.MainActivity"
$mainComponent = "com.musfit.internal/com.musfit.MainActivity"

$powerShellExe = Join-Path $PSHOME "powershell.exe"
if (-not (Test-Path -LiteralPath $powerShellExe)) {
    throw "Windows PowerShell executable not found: $powerShellExe"
}

$verifyScript = Join-Path $repoRoot "scripts\dev\verify-musfit.ps1"
$installScript = Join-Path $repoRoot "scripts\android\install-seed-musfit.ps1"
$envScript = Join-Path $repoRoot "scripts\android\android-env.ps1"
$requiredScripts = @($verifyScript, $installScript, $envScript)
if ($IncludeWorkflowContract) {
    $requiredScripts += (Join-Path $repoRoot "scripts\dev\test-dev-workflow.ps1")
}
foreach ($requiredScript in $requiredScripts) {
    if (-not (Test-Path -LiteralPath $requiredScript)) {
        throw "Required MusFit helper is missing: $requiredScript"
    }
}

$fullLog = Join-Path $resolvedOutputDir "full-verification.log"
$emulatorLog = Join-Path $resolvedOutputDir "emulator-install.log"
$bootstrapDir = Join-Path $resolvedOutputDir "bootstrap"
New-Item -ItemType Directory -Path $bootstrapDir -Force | Out-Null

$receiptChecks = @()
if ($IncludeWorkflowContract) {
    $workflowScript = Join-Path $repoRoot "scripts\dev\test-dev-workflow.ps1"
    $workflowLog = Join-Path $resolvedOutputDir "workflow-contract.log"
    Invoke-LoggedPowerShell `
        -PowerShellExe $powerShellExe `
        -ScriptPath $workflowScript `
        -Arguments @("-SelfTest") `
        -LogPath $workflowLog `
        -Label "development workflow contract"
    $receiptChecks += [ordered]@{
        name = "Development workflow contract"
        command = ".\scripts\dev\test-dev-workflow.ps1 -SelfTest"
        status = "passed"
        localLog = "workflow-contract.log"
    }
}

Invoke-LoggedPowerShell `
    -PowerShellExe $powerShellExe `
    -ScriptPath $verifyScript `
    -Arguments @("-Preset", "Full", "-RetryOnGeneratedOutputIssue") `
    -LogPath $fullLog `
    -Label "full MusFit verification"
$receiptChecks += [ordered]@{
    name = "Full Gradle gate"
    command = ".\scripts\dev\verify-musfit.ps1 -Preset Full -RetryOnGeneratedOutputIssue"
    status = "passed"
    localLog = "full-verification.log"
}

if ($DeviceSerial -and $DeviceSerial -notmatch '^emulator-\d+$') {
    throw "Only Android emulator serials are allowed, not: $DeviceSerial"
}

$installArguments = @("-Reset", "-SkipBuild", "-EvidenceDir", $bootstrapDir)
if ($DeviceSerial) {
    $installArguments += @("-DeviceSerial", $DeviceSerial)
}
if ($Headless) {
    $installArguments += "-Headless"
}

Invoke-LoggedPowerShell `
    -PowerShellExe $powerShellExe `
    -ScriptPath $installScript `
    -Arguments $installArguments `
    -LogPath $emulatorLog `
    -Label "seeded emulator install"
$receiptChecks += [ordered]@{
    name = "Seeded emulator install"
    command = ".\scripts\android\install-seed-musfit.ps1 -Reset -SkipBuild -EvidenceDir <bootstrap>"
    status = "passed"
    localLog = "emulator-install.log"
}

. $envScript

if (-not $DeviceSerial) {
    $deviceLines = @(& adb devices 2>&1)
    Assert-LastExitCode "List Android devices"
    foreach ($deviceLine in $deviceLines) {
        if ($deviceLine -notmatch '^(emulator-\d+)\s+device$') {
            continue
        }
        $candidateSerial = $Matches[1]
        $candidateAvd = @(& adb -s $candidateSerial emu avd name 2>&1) |
            ForEach-Object { $_.ToString().Trim() } |
            Where-Object { $_ -and $_ -ne "OK" } |
            Select-Object -First 1
        if ($LASTEXITCODE -eq 0 -and $candidateAvd -eq "MusFit_API36") {
            $DeviceSerial = $candidateSerial
            break
        }
    }
}
if (-not $DeviceSerial -or $DeviceSerial -notmatch '^emulator-\d+$') {
    throw "The seeded install did not leave a usable emulator device"
}

& adb -s $DeviceSerial shell am start -W -n $mainComponent | Out-Host
Assert-LastExitCode "Launch MusFit MainActivity"
Start-Sleep -Seconds 2

$activityState = @(& adb -s $DeviceSerial shell dumpsys activity activities 2>&1)
Assert-LastExitCode "Inspect foreground activity"
if (($activityState -join "`n") -notmatch "(mResumedActivity|topResumedActivity).*$([regex]::Escape($targetPackage))") {
    throw "MusFit is not the resumed foreground activity on $DeviceSerial"
}

& adb -s $DeviceSerial logcat -c
Assert-LastExitCode "Clear emulator logcat"
$receiptChecks += [ordered]@{
    name = "Foreground launch"
    command = "adb -s $DeviceSerial shell am start -W -n $mainComponent"
    status = "passed"
    localLog = $null
}

$avdOutput = @(& adb -s $DeviceSerial emu avd name 2>&1)
Assert-LastExitCode "Read emulator AVD name"
$avdName = $avdOutput |
    Where-Object { $_ -and $_.Trim() -ne "OK" } |
    Select-Object -First 1
if (-not $avdName) {
    $avdName = "unknown"
}
$avdName = $avdName.Trim()
if ($avdName -ne "MusFit_API36") {
    throw "Refusing evidence from AVD '$avdName'; expected the dedicated MusFit_API36 emulator"
}

$completedAt = [DateTime]::UtcNow
$receipt = [ordered]@{
    schemaVersion = 1
    status = "passed"
    headSha = $headSha
    startedAtUtc = $startedAt.ToString("o")
    completedAtUtc = $completedAt.ToString("o")
    checks = @($receiptChecks)
    device = [ordered]@{
        serial = $DeviceSerial
        avdName = $avdName
        packageName = $targetPackage
        activity = $mainActivity
        seeded = $true
    }
}

$receiptPath = Join-Path $resolvedOutputDir "verification.json"
$receiptJson = $receipt | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($receiptPath, $receiptJson, (New-Object Text.UTF8Encoding($false)))

Write-Host "MusFit PR gate passed for $headSha"
Write-Host "Evidence directory: $resolvedOutputDir"
Write-Host "Verification receipt: $receiptPath"
