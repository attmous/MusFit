param(
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function Get-RepoPath([string] $RelativePath) {
    Join-Path $repoRoot $RelativePath
}

function Assert-FileExists([string] $RelativePath) {
    if (-not (Test-Path -LiteralPath (Get-RepoPath $RelativePath) -PathType Leaf)) {
        throw "Expected file does not exist: $RelativePath"
    }
}

function Assert-FileContains([string] $RelativePath, [string] $Pattern) {
    Assert-FileExists $RelativePath
    $content = Get-Content -LiteralPath (Get-RepoPath $RelativePath) -Raw
    if ($content -notmatch $Pattern) {
        throw "Expected $RelativePath to match: $Pattern"
    }
}

function Get-BaselineCounts([string] $Path, [string] $NodeName, [string] $IdAttribute) {
    [xml] $document = Get-Content -LiteralPath $Path -Raw
    $counts = @{}
    foreach ($node in $document.SelectNodes("//$NodeName")) {
        $id = [string] $node.GetAttribute($IdAttribute)
        if ([string]::IsNullOrWhiteSpace($id)) {
            throw "Baseline node '$NodeName' is missing '$IdAttribute': $Path"
        }
        if (-not $counts.ContainsKey($id)) { $counts[$id] = 0 }
        $counts[$id]++
    }
    return $counts
}

function Get-DetektBaselineCounts([string] $Path) {
    [xml] $document = Get-Content -LiteralPath $Path -Raw
    $counts = @{}
    foreach ($node in $document.SelectNodes("//ID")) {
        $signature = [string] $node.InnerText
        $id = $signature.Split(':', 2)[0]
        if ([string]::IsNullOrWhiteSpace($id) -or $id -eq $signature) {
            throw "Detekt baseline ID is malformed: $signature"
        }
        if (-not $counts.ContainsKey($id)) { $counts[$id] = 0 }
        $counts[$id]++
    }
    return $counts
}

function Assert-OwnedDebt(
    [object[]] $Entries,
    [hashtable] $ActualCounts,
    [string] $Tool,
    [datetime] $Today
) {
    $toolEntries = @($Entries | Where-Object tool -eq $Tool)
    $registeredCounts = @{}
    foreach ($entry in $toolEntries) {
        if ([string]::IsNullOrWhiteSpace([string] $entry.id)) {
            throw "$Tool debt entry is missing id."
        }
        if ([string]::IsNullOrWhiteSpace([string] $entry.owner)) {
            throw "$Tool debt '$($entry.id)' is missing owner."
        }
        if ([string]::IsNullOrWhiteSpace([string] $entry.rationale)) {
            throw "$Tool debt '$($entry.id)' is missing rationale."
        }
        $expiry = [datetime]::ParseExact([string] $entry.expiresOn, "yyyy-MM-dd", $null)
        if ($expiry.Date -lt $Today.Date) {
            throw "$Tool debt '$($entry.id)' expired on $($entry.expiresOn)."
        }
        $count = [int] $entry.occurrences
        if ($count -le 0) {
            throw "$Tool debt '$($entry.id)' must own at least one occurrence."
        }
        if ($registeredCounts.ContainsKey([string] $entry.id)) {
            throw "Duplicate $Tool debt entry: $($entry.id)"
        }
        $registeredCounts[[string] $entry.id] = $count
    }

    foreach ($id in $ActualCounts.Keys) {
        if (-not $registeredCounts.ContainsKey($id)) {
            throw "Unowned $Tool baseline debt: $id ($($ActualCounts[$id]) occurrences)."
        }
        if ($registeredCounts[$id] -ne $ActualCounts[$id]) {
            throw "$Tool debt count drift for '$id': registered $($registeredCounts[$id]), baseline $($ActualCounts[$id])."
        }
    }
    foreach ($id in $registeredCounts.Keys) {
        if (-not $ActualCounts.ContainsKey($id)) {
            throw "Registered $Tool debt no longer exists in the baseline: $id"
        }
    }
}

Assert-FileExists "config/static-analysis-debt.json"
Assert-FileExists "config/detekt.yml"
Assert-FileExists "config/detekt-baseline.xml"
Assert-FileExists "app/lint-baseline.xml"
Assert-FileExists "docs/testing/static-quality.md"
Assert-FileContains "gradle/libs.versions.toml" '(?m)^spotless\s*=\s*"8\.8\.0"\s*$'
Assert-FileContains "gradle/libs.versions.toml" '(?m)^detekt\s*=\s*"1\.23\.8"\s*$'
Assert-FileContains "build.gradle.kts" 'ratchetFrom\("origin/master"\)'
Assert-FileContains "build.gradle.kts" 'ktlint\("1\.8\.0"\)'
Assert-FileContains "build.gradle.kts" 'config/detekt-baseline\.xml'
Assert-FileContains "app/build.gradle.kts" 'warningsAsErrors\s*=\s*true'
Assert-FileContains "app/build.gradle.kts" 'baseline\s*=\s*file\("lint-baseline\.xml"\)'
Assert-FileContains "scripts/dev/verify-musfit.ps1" 'test-static-quality\.ps1'
Assert-FileContains "scripts/dev/verify-musfit.ps1" 'spotlessCheck'
Assert-FileContains "scripts/dev/verify-musfit.ps1" '(?m)^\s*"detekt",?\s*$'
Assert-FileContains ".github/workflows/android.yml" 'spotlessCheck detekt'

$debt = Get-Content -LiteralPath (Get-RepoPath "config/static-analysis-debt.json") -Raw | ConvertFrom-Json
if ([int] $debt.schemaVersion -ne 1) {
    throw "Static-analysis debt schemaVersion must be 1."
}

$lintCounts = Get-BaselineCounts (Get-RepoPath "app/lint-baseline.xml") "issue" "id"
$detektCounts = Get-DetektBaselineCounts (Get-RepoPath "config/detekt-baseline.xml")
$forbiddenLintDebt = @(
    "ApplySharedPref",
    "ConstantLocale",
    "CredentialManagerMisuse",
    "InsecureBaseConfiguration"
)
foreach ($id in $forbiddenLintDebt) {
    if ($lintCounts.ContainsKey($id)) {
        throw "Security/correctness lint cannot be baselined: $id"
    }
}

Assert-OwnedDebt @($debt.entries) $lintCounts "android-lint" (Get-Date)
Assert-OwnedDebt @($debt.entries) $detektCounts "detekt" (Get-Date)

if ($SelfTest) {
    $fixture = @(
        [pscustomobject]@{
            tool = "fixture"
            id = "OwnedRule"
            occurrences = 2
            owner = "mobile-platform"
            expiresOn = "2099-12-31"
            rationale = "Fixture"
        }
    )
    Assert-OwnedDebt $fixture @{ OwnedRule = 2 } "fixture" ([datetime] "2099-01-01")
    $caught = $false
    try {
        Assert-OwnedDebt $fixture @{ OwnedRule = 3 } "fixture" ([datetime] "2099-01-01")
    } catch {
        $caught = $_.Exception.Message -match "count drift"
    }
    if (-not $caught) {
        throw "Self-test failed to reject debt count drift."
    }
}

Write-Host "Static-quality policy passed: $($lintCounts.Count) lint IDs and $($detektCounts.Count) detekt IDs are owned."
