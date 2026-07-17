param(
    [string[]] $ReportPath = @(),
    [string] $BaseRef = "HEAD^",
    [string] $ConfigPath = "config/coverage-policy.json",
    [string] $OutputDirectory = "build/reports/coverage-policy",
    [switch] $SkipChangedCode,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path

function Test-AnyPattern([string] $Path, [object[]] $Patterns) {
    foreach ($pattern in @($Patterns)) {
        if ($Path -match [string] $pattern) {
            return $true
        }
    }
    return $false
}

function Test-BusinessPath([string] $Path, [object] $Policy) {
    (Test-AnyPattern $Path @($Policy.businessPathPatterns)) -and
        -not (Test-AnyPattern $Path @($Policy.excludedPathPatterns))
}

function Get-RepoRelativePath([string] $Path) {
    $fullPath = [IO.Path]::GetFullPath($Path)
    $prefix = $repoRoot.TrimEnd([char[]] @('\', '/')) + [IO.Path]::DirectorySeparatorChar
    if ($fullPath.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath.Substring($prefix.Length).Replace([char] 92, [char] 47)
    }
    return $fullPath.Replace([char] 92, [char] 47)
}

function Get-CoverageSourceRoot([string] $ReportPath) {
    $relativeReportPath = Get-RepoRelativePath $ReportPath
    if ($relativeReportPath -match '^core/([^/]+)/build/') {
        $module = $Matches[1]
        $sourceLanguage = if ($module -eq "model") { "kotlin" } else { "java" }
        return "core/$module/src/main/$sourceLanguage"
    }
    return "app/src/main/java"
}

function Get-AggregatedLineMap([string[]] $Paths, [object] $Policy) {
    $lineMap = @{}
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Coverage report does not exist: $path"
        }
        [xml] $report = Get-Content -LiteralPath $path -Raw
        $sourceRoot = Get-CoverageSourceRoot $path
        foreach ($package in @($report.report.package)) {
            $packagePath = [string] $package.name
            foreach ($sourceFile in @($package.sourcefile)) {
                $repoPath = "$sourceRoot/$packagePath/$($sourceFile.name)"
                if (-not (Test-BusinessPath $repoPath $Policy)) {
                    continue
                }
                foreach ($line in @($sourceFile.line)) {
                    if ([int] $line.mi + [int] $line.ci -eq 0) {
                        continue
                    }
                    $lineNumber = [int] $line.nr
                    $key = "${repoPath}:${lineNumber}"
                    $covered = [int] $line.ci -gt 0
                    if (-not $lineMap.ContainsKey($key)) {
                        $lineMap[$key] = [pscustomobject]@{
                            Path = $repoPath
                            Line = $lineNumber
                            Covered = $covered
                        }
                    } elseif ($covered) {
                        $lineMap[$key].Covered = $true
                    }
                }
            }
        }
    }
    return $lineMap
}

function Get-CoverageStats([object[]] $Lines) {
    $all = @($Lines | Where-Object { $null -ne $_ })
    $covered = @($all | Where-Object Covered).Count
    $total = $all.Count
    $ratio = if ($total -eq 0) { 1.0 } else { $covered / [double] $total }
    [pscustomobject]@{
        Covered = $covered
        Missed = $total - $covered
        Total = $total
        Ratio = $ratio
    }
}

function Assert-MinimumRatio([string] $Label, [object] $Stats, [double] $Minimum) {
    if ($Stats.Total -gt 0 -and $Stats.Ratio + 0.000000001 -lt $Minimum) {
        throw "$Label line coverage is $([math]::Round(100 * $Stats.Ratio, 2))% " +
            "($($Stats.Covered)/$($Stats.Total)); required $([math]::Round(100 * $Minimum, 2))%."
    }
}

function Get-ChangedExecutableLines([hashtable] $LineMap, [string] $ComparisonRef) {
    $changed = @{}
    $currentPath = $null
    $diff = & git -C $repoRoot diff --unified=0 --no-color "$ComparisonRef...HEAD" -- "app/src/main/java" "core/*/src/main/kotlin" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Could not derive changed coverage lines from '$ComparisonRef...HEAD': $($diff -join [Environment]::NewLine)"
    }
    foreach ($line in @($diff)) {
        if ($line -match '^\+\+\+ b/(.+)$') {
            $currentPath = $Matches[1].Replace([char] 92, [char] 47)
            continue
        }
        if ($null -eq $currentPath -or $line -notmatch '^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@') {
            continue
        }
        $start = [int] $Matches[1]
        $count = if ([string]::IsNullOrWhiteSpace($Matches[2])) { 1 } else { [int] $Matches[2] }
        for ($offset = 0; $offset -lt $count; $offset++) {
            $key = "${currentPath}:$($start + $offset)"
            if ($LineMap.ContainsKey($key)) {
                $changed[$key] = $LineMap[$key]
            }
        }
    }
    return @($changed.Values)
}

function Invoke-PolicySelfTest {
    $tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("musfit-coverage-" + [guid]::NewGuid().ToString("N"))
    [IO.Directory]::CreateDirectory($tempRoot) | Out-Null
    try {
        $reportA = Join-Path $tempRoot "a.xml"
        $reportB = Join-Path $tempRoot "b.xml"
        $xmlA = '<report name="a"><package name="com/musfit/domain/test"><sourcefile name="Calculator.kt"><line nr="10" mi="0" ci="1"/><line nr="11" mi="1" ci="0"/></sourcefile></package></report>'
        $xmlB = '<report name="b"><package name="com/musfit/domain/test"><sourcefile name="Calculator.kt"><line nr="10" mi="1" ci="0"/><line nr="11" mi="0" ci="1"/></sourcefile></package></report>'
        [IO.File]::WriteAllText($reportA, $xmlA)
        [IO.File]::WriteAllText($reportB, $xmlB)
        $policy = [pscustomobject]@{
            businessPathPatterns = @('^app/src/main/java/com/musfit/domain/')
            criticalPathPatterns = @('^app/src/main/java/com/musfit/domain/')
            excludedPathPatterns = @()
        }
        $map = Get-AggregatedLineMap @($reportA, $reportB) $policy
        $stats = Get-CoverageStats @($map.Values)
        if ($stats.Covered -ne 2 -or $stats.Total -ne 2) {
            throw "Self-test failed to merge unit/instrumented coverage: $($stats.Covered)/$($stats.Total)."
        }
        $coreReportFixture = Join-Path $repoRoot "core/model/build/reports/jacoco/test/jacocoTestReport.xml"
        if ((Get-CoverageSourceRoot $coreReportFixture) -ne "core/model/src/main/kotlin") {
            throw "Self-test failed to map a core module JaCoCo report to its Kotlin source root."
        }
        $emptyStats = Get-CoverageStats @($null)
        if ($emptyStats.Covered -ne 0 -or $emptyStats.Total -ne 0 -or $emptyStats.Ratio -ne 1.0) {
            throw "Self-test failed to normalize an empty changed-line result."
        }
        $caught = $false
        try {
            Assert-MinimumRatio "Fixture" ([pscustomobject]@{ Total = 2; Covered = 1; Ratio = 0.5 }) 0.8
        } catch {
            $caught = $_.Exception.Message -match 'required 80'
        }
        if (-not $caught) {
            throw "Self-test failed to reject a changed-code coverage regression."
        }
    } finally {
        if ($tempRoot.StartsWith([IO.Path]::GetTempPath(), [StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    Write-Host "Coverage aggregation and threshold self-tests passed."
}

if ($SelfTest) {
    Invoke-PolicySelfTest
    return
}

$resolvedConfigPath = if ([IO.Path]::IsPathRooted($ConfigPath)) { $ConfigPath } else { Join-Path $repoRoot $ConfigPath }
if (-not (Test-Path -LiteralPath $resolvedConfigPath -PathType Leaf)) {
    throw "Coverage policy does not exist: $resolvedConfigPath"
}
$policy = Get-Content -LiteralPath $resolvedConfigPath -Raw | ConvertFrom-Json
if ([int] $policy.schemaVersion -ne 1) {
    throw "Coverage policy schemaVersion must be 1."
}

if ($ReportPath.Count -eq 0) {
    $ReportPath = @(
        Get-ChildItem -LiteralPath (Join-Path $repoRoot "app/build/reports/coverage") -Recurse -Filter "report.xml" -File |
            Select-Object -ExpandProperty FullName
        Get-ChildItem -LiteralPath (Join-Path $repoRoot "core/model/build/reports/jacoco") -Recurse -Filter "*.xml" -File |
            Select-Object -ExpandProperty FullName
        Get-ChildItem -LiteralPath (Join-Path $repoRoot "core/network/build/reports/coverage") -Recurse -Filter "report.xml" -File |
            Select-Object -ExpandProperty FullName
        Get-ChildItem -LiteralPath (Join-Path $repoRoot "core/data/build/reports/coverage") -Recurse -Filter "report.xml" -File |
            Select-Object -ExpandProperty FullName
    )
}
$resolvedReports = @(
    $ReportPath | ForEach-Object {
        if ([IO.Path]::IsPathRooted($_)) { $_ } else { Join-Path $repoRoot $_ }
    } | Sort-Object -Unique
)
if ($resolvedReports.Count -eq 0) {
    throw "No JaCoCo report XML files were supplied or discovered."
}

$lineMap = Get-AggregatedLineMap $resolvedReports $policy
$overall = Get-CoverageStats @($lineMap.Values)
$critical = Get-CoverageStats @($lineMap.Values | Where-Object { Test-AnyPattern $_.Path @($policy.criticalPathPatterns) })
Assert-MinimumRatio "Overall eligible business logic" $overall ([double] $policy.thresholds.overallLineRatioBaseline)

$changedBusiness = [pscustomobject]@{ Covered = 0; Missed = 0; Total = 0; Ratio = 1.0 }
$changedCritical = [pscustomobject]@{ Covered = 0; Missed = 0; Total = 0; Ratio = 1.0 }
if (-not $SkipChangedCode) {
    $changedLines = Get-ChangedExecutableLines $lineMap $BaseRef
    $changedBusiness = Get-CoverageStats $changedLines
    $changedCritical = Get-CoverageStats @($changedLines | Where-Object { Test-AnyPattern $_.Path @($policy.criticalPathPatterns) })
    Assert-MinimumRatio "Changed business logic" $changedBusiness ([double] $policy.thresholds.changedBusinessLineRatio)
    Assert-MinimumRatio "Changed critical domain/repository logic" $changedCritical ([double] $policy.thresholds.changedCriticalLineRatio)
}

$resolvedOutputDirectory = if ([IO.Path]::IsPathRooted($OutputDirectory)) { $OutputDirectory } else { Join-Path $repoRoot $OutputDirectory }
[IO.Directory]::CreateDirectory($resolvedOutputDirectory) | Out-Null
$summary = [ordered]@{
    schemaVersion = 1
    baselineCommit = [string] $policy.baselineCommit
    comparisonRef = if ($SkipChangedCode) { $null } else { $BaseRef }
    reports = @($resolvedReports | ForEach-Object { Get-RepoRelativePath $_ })
    overall = $overall
    critical = $critical
    changedBusiness = $changedBusiness
    changedCritical = $changedCritical
    thresholds = $policy.thresholds
}
$summary | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $resolvedOutputDirectory "summary.json") -Encoding utf8
@(
    "# MusFit coverage summary"
    ""
    "| Metric | Covered | Total | Ratio | Gate |"
    "| --- | ---: | ---: | ---: | ---: |"
    "| Overall eligible business logic | $($overall.Covered) | $($overall.Total) | $([math]::Round(100 * $overall.Ratio, 2))% | >= $([math]::Round(100 * [double] $policy.thresholds.overallLineRatioBaseline, 2))% |"
    "| Critical domain/repository (visibility) | $($critical.Covered) | $($critical.Total) | $([math]::Round(100 * $critical.Ratio, 2))% | changed lines gated below |"
    "| Changed business logic | $($changedBusiness.Covered) | $($changedBusiness.Total) | $([math]::Round(100 * $changedBusiness.Ratio, 2))% | >= $([math]::Round(100 * [double] $policy.thresholds.changedBusinessLineRatio, 0))% |"
    "| Changed critical domain/repository | $($changedCritical.Covered) | $($changedCritical.Total) | $([math]::Round(100 * $changedCritical.Ratio, 2))% | >= $([math]::Round(100 * [double] $policy.thresholds.changedCriticalLineRatio, 0))% |"
) | Set-Content -LiteralPath (Join-Path $resolvedOutputDirectory "summary.md") -Encoding utf8

Write-Host "Coverage policy passed: overall $($overall.Covered)/$($overall.Total) ($([math]::Round(100 * $overall.Ratio, 2))%); changed $($changedBusiness.Covered)/$($changedBusiness.Total); changed critical $($changedCritical.Covered)/$($changedCritical.Total)."
