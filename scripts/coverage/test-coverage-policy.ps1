param(
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path

function Assert-FileContains([string] $RelativePath, [string] $Pattern) {
    $path = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Expected coverage contract file does not exist: $RelativePath"
    }
    $text = Get-Content -LiteralPath $path -Raw
    if ($text -notmatch $Pattern) {
        throw "Expected $RelativePath to contain coverage contract: $Pattern"
    }
}

Assert-FileContains "app/build.gradle.kts" '(?m)^\s*jacoco\s*$'
Assert-FileContains "app/build.gradle.kts" 'toolVersion\s*=\s*"0\.8\.14"'
Assert-FileContains "app/build.gradle.kts" 'enableUnitTestCoverage\s*=\s*true'
Assert-FileContains "app/build.gradle.kts" 'enableAndroidTestCoverage\s*=\s*true'
Assert-FileContains "app/build.gradle.kts" 'isIncludeNoLocationClasses\s*=\s*true'
Assert-FileContains "app/build.gradle.kts" 'jdk\.internal\.\*'
Assert-FileContains ".github/workflows/android.yml" 'createInternalDebugUnitTestCoverageReport'
Assert-FileContains ".github/workflows/android.yml" ':core:model:jacocoTestReport'
Assert-FileContains ".github/workflows/android.yml" 'verify-coverage\.ps1'
Assert-FileContains ".github/workflows/android.yml" 'musfit-coverage-'
Assert-FileContains ".github/workflows/device-ui.yml" 'createManagedDeviceInternalDebugAndroidTestCoverageReport'
Assert-FileContains ".github/workflows/device-ui.yml" 'testInstrumentationRunnerArguments\.clearPackageData=false'
Assert-FileContains "docs/testing/coverage.md" '57\.8316%'
Assert-FileContains "docs/testing/coverage.md" '10 minutes'
Assert-FileContains "scripts/dev/verify-musfit.ps1" 'test-coverage-policy\.ps1'
Assert-FileContains "config/coverage-policy.json" '\^core/model/src/main/kotlin/com/musfit/domain/'
Assert-FileContains "scripts/coverage/verify-coverage.ps1" 'core/\(\[\^/\]\+\)/build/'

$policyPath = Join-Path $repoRoot "config/coverage-policy.json"
$policy = Get-Content -LiteralPath $policyPath -Raw | ConvertFrom-Json
if ([int] $policy.schemaVersion -ne 1) {
    throw "Coverage policy schemaVersion must be 1."
}
if ([double] $policy.thresholds.changedBusinessLineRatio -lt 0.8) {
    throw "Changed business coverage cannot fall below 80%."
}
if ([double] $policy.thresholds.changedCriticalLineRatio -lt 0.9) {
    throw "Changed critical coverage cannot fall below 90%."
}
$baselineRatio = [int] $policy.baselineCounts.covered / [double] [int] $policy.baselineCounts.total
if ([math]::Abs($baselineRatio - [double] $policy.thresholds.overallLineRatioBaseline) -gt 0.000001) {
    throw "Coverage baseline counts and ratio have drifted."
}
if ([string] $policy.baselineCommit -notmatch '^[0-9a-f]{40}$') {
    throw "Coverage baseline commit must be a full Git SHA."
}

if ($SelfTest) {
    & (Join-Path $repoRoot "scripts/coverage/verify-coverage.ps1") -SelfTest
}

Write-Host "Coverage policy contract passed."
