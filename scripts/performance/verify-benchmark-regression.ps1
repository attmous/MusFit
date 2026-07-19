param(
    [string] $ResultsPath,
    [string] $BaselinePath = "benchmark/baselines/approved-api37.json",
    [string] $ReportDirectory = "build/reports/performance",
    [ValidateRange(0.1, 100.0)]
    [double] $ThresholdPercent = 10.0,
    [string] $BenchmarkId,
    [switch] $WriteBaseline,
    [switch] $ReportOnly,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path
$benchmarkSelectorSpecified = $PSBoundParameters.ContainsKey("BenchmarkId")

if ($benchmarkSelectorSpecified -and [string]::IsNullOrWhiteSpace($BenchmarkId)) {
    throw "BenchmarkId must be a non-empty fully qualified benchmark ID."
}
if ($benchmarkSelectorSpecified -and $WriteBaseline) {
    throw "BenchmarkId cannot be combined with WriteBaseline because baseline approval must cover the full suite."
}

function Resolve-RepoPath([string] $Path) {
    if ([IO.Path]::IsPathRooted($Path)) {
        return [IO.Path]::GetFullPath($Path)
    }
    [IO.Path]::GetFullPath((Join-Path $repoRoot $Path))
}

function Get-Percentile([double[]] $Values, [double] $Percentile) {
    if ($Values.Count -eq 0) {
        throw "Cannot calculate a percentile from an empty value set."
    }
    $sorted = @($Values | Sort-Object)
    if ($sorted.Count -eq 1) {
        return [double] $sorted[0]
    }
    $position = ($sorted.Count - 1) * $Percentile
    $lower = [Math]::Floor($position)
    $upper = [Math]::Ceiling($position)
    if ($lower -eq $upper) {
        return [double] $sorted[$lower]
    }
    $weight = $position - $lower
    [double] $sorted[$lower] + (([double] $sorted[$upper] - [double] $sorted[$lower]) * $weight)
}

function Get-Statistics([double[]] $Values) {
    $sorted = @($Values | Sort-Object)
    [ordered]@{
        minimum = [Math]::Round([double] $sorted[0], 4)
        median = [Math]::Round((Get-Percentile $sorted 0.5), 4)
        p90 = [Math]::Round((Get-Percentile $sorted 0.9), 4)
        maximum = [Math]::Round([double] $sorted[-1], 4)
        sampleCount = $sorted.Count
    }
}

function Get-MetricValues($Metric, [switch] $Sampled) {
    if ($Sampled) {
        return [double[]] @($Metric.runs | ForEach-Object { @($_) } | ForEach-Object { [double] $_ })
    }
    [double[]] @($Metric.runs | ForEach-Object { [double] $_ })
}

function Get-GateStatistic([string] $MetricName) {
    switch ($MetricName) {
        "timeToInitialDisplayMs" { "p90" }
        "frameDurationCpuMs" { "p90" }
        "frameOverrunMs" { "p90" }
        "memoryHeapSizeMaxKb" { "maximum" }
        "memoryRssAnonMaxKb" { "maximum" }
        "trainingExerciseImagePssKb" { "maximum" }
        default { $null }
    }
}

function Get-BenchmarkDocuments([string] $Path) {
    $resolved = Resolve-RepoPath $Path
    if (Test-Path -LiteralPath $resolved -PathType Leaf) {
        $files = @(Get-Item -LiteralPath $resolved)
    } elseif (Test-Path -LiteralPath $resolved -PathType Container) {
        $files = @(Get-ChildItem -LiteralPath $resolved -Recurse -Filter "*benchmarkData.json" -File)
    } else {
        throw "Benchmark results path does not exist: $resolved"
    }
    if ($files.Count -eq 0) {
        throw "No *benchmarkData.json files found under: $resolved"
    }

    @($files | ForEach-Object {
        try {
            $document = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        } catch {
            throw "Could not parse benchmark JSON $($_.FullName): $($_.Exception.Message)"
        }
        if ($null -eq $document.context.build.version.sdk -or @($document.benchmarks).Count -eq 0) {
            throw "Benchmark JSON is missing device context or benchmark results: $($_.FullName)"
        }
        [pscustomobject]@{ File = $_.FullName; Document = $document }
    })
}

function ConvertTo-PerformanceReport([object[]] $Documents) {
    $devices = @()
    $measurements = @()
    foreach ($item in $Documents) {
        $document = $item.Document
        $sdk = [int] $document.context.build.version.sdk
        $model = [string] $document.context.build.model
        $fingerprint = [string] $document.context.build.fingerprint
        $deviceKey = "api$($sdk)-$model"
        $devices += [ordered]@{
            key = $deviceKey
            sdk = $sdk
            model = $model
            fingerprint = $fingerprint
            cpuLocked = [bool] $document.context.cpuLocked
            source = [IO.Path]::GetFileName($item.File)
        }

        foreach ($benchmark in $document.benchmarks) {
            $benchmarkId = "$($benchmark.className).$($benchmark.name)"
            foreach ($property in $benchmark.metrics.PSObject.Properties) {
                $values = Get-MetricValues $property.Value
                $stats = Get-Statistics $values
                $measurements += [ordered]@{
                    key = "$deviceKey|$benchmarkId|$($property.Name)"
                    device = $deviceKey
                    benchmark = $benchmarkId
                    metric = $property.Name
                    statistics = $stats
                    gateStatistic = Get-GateStatistic $property.Name
                }
            }
            foreach ($property in $benchmark.sampledMetrics.PSObject.Properties) {
                $values = Get-MetricValues $property.Value -Sampled
                $stats = Get-Statistics $values
                $measurements += [ordered]@{
                    key = "$deviceKey|$benchmarkId|$($property.Name)"
                    device = $deviceKey
                    benchmark = $benchmarkId
                    metric = $property.Name
                    statistics = $stats
                    gateStatistic = Get-GateStatistic $property.Name
                }
            }
        }
    }

    [ordered]@{
        schemaVersion = 1
        generatedAtUtc = [DateTime]::UtcNow.ToString("o")
        devices = @($devices | Sort-Object { $_.key } -Unique)
        measurements = @($measurements | Sort-Object { $_.key })
    }
}

function Get-ApprovedMeasurements($Report) {
    @($Report.measurements | Where-Object { $null -ne $_.gateStatistic } | ForEach-Object {
        $statistic = [string] $_.gateStatistic
        [ordered]@{
            key = $_.key
            statistic = $statistic
            approvedValue = [double] $_.statistics.$statistic
        }
    })
}

function Test-Regression([double] $Approved, [double] $Current, [double] $Threshold) {
    $Current -gt ($Approved * (1.0 + ($Threshold / 100.0)))
}

function Get-ApprovedBenchmarkId($Measurement) {
    $key = [string] $Measurement.key
    [string[]] $parts = $key.Split([char] '|')
    if (
        $parts.Count -ne 3 -or
        [string]::IsNullOrWhiteSpace($parts[0]) -or
        [string]::IsNullOrWhiteSpace($parts[1]) -or
        [string]::IsNullOrWhiteSpace($parts[2])
    ) {
        throw "Malformed approved benchmark measurement key '$key'. Expected '<device>|<fully-qualified-benchmark-id>|<metric>'."
    }
    $parts[1]
}

function Get-BenchmarkScope(
    $Report,
    $ApprovedBaseline,
    [string] $ExactBenchmarkId,
    [switch] $SelectorSpecified
) {
    $approvedDescriptors = @($ApprovedBaseline.measurements | ForEach-Object {
        [pscustomobject]@{
            Measurement = $_
            Benchmark = Get-ApprovedBenchmarkId $_
        }
    })

    $duplicateApprovedKeys = @(
        $approvedDescriptors |
            Group-Object { [string] $_.Measurement.key } |
            Where-Object { $_.Count -gt 1 } |
            ForEach-Object { $_.Name }
    )
    if ($duplicateApprovedKeys.Count -gt 0) {
        throw "Approved benchmark baseline contains duplicate measurement keys: $($duplicateApprovedKeys -join '; ')"
    }

    if (-not $SelectorSpecified) {
        return [pscustomobject]@{
            CurrentMeasurements = @($Report.measurements)
            ApprovedMeasurements = @($approvedDescriptors | ForEach-Object { $_.Measurement })
        }
    }

    $currentGatedIds = @(
        $Report.measurements |
            Where-Object { $null -ne $_.gateStatistic } |
            ForEach-Object { [string] $_.benchmark } |
            Sort-Object -CaseSensitive -Unique
    )
    if ($currentGatedIds -cnotcontains $ExactBenchmarkId) {
        throw "BenchmarkId '$ExactBenchmarkId' did not exactly match a current gated benchmark ID. Available exact IDs: $($currentGatedIds -join '; ')"
    }

    $approvedIds = @(
        $approvedDescriptors |
            ForEach-Object { [string] $_.Benchmark } |
            Sort-Object -CaseSensitive -Unique
    )
    if ($approvedIds -cnotcontains $ExactBenchmarkId) {
        throw "BenchmarkId '$ExactBenchmarkId' did not exactly match an approved benchmark ID. Available exact IDs: $($approvedIds -join '; ')"
    }

    [pscustomobject]@{
        CurrentMeasurements = @(
            $Report.measurements | Where-Object { ([string] $_.benchmark) -ceq $ExactBenchmarkId }
        )
        ApprovedMeasurements = @(
            $approvedDescriptors |
                Where-Object { ([string] $_.Benchmark) -ceq $ExactBenchmarkId } |
                ForEach-Object { $_.Measurement }
        )
    }
}

function Compare-BenchmarkMeasurements(
    [object[]] $CurrentMeasurements,
    [object[]] $ApprovedMeasurements,
    [string[]] $ApprovedDeviceKeys,
    [double] $Threshold
) {
    $approvedKeys = @($ApprovedMeasurements | ForEach-Object { [string] $_.key })
    $unapprovedMeasurements = @(
        $CurrentMeasurements | Where-Object {
            $null -ne $_.gateStatistic -and
            $ApprovedDeviceKeys -ccontains ([string] $_.device) -and
            $approvedKeys -cnotcontains ([string] $_.key)
        }
    )
    if ($unapprovedMeasurements.Count -gt 0) {
        $unapprovedKeys = @($unapprovedMeasurements | ForEach-Object { $_.key }) -join "; "
        throw "Current gated measurements are missing from the approved baseline: $unapprovedKeys"
    }

    $comparisons = @()
    $failures = @()
    foreach ($approved in $ApprovedMeasurements) {
        $current = @($CurrentMeasurements | Where-Object { $_.key -ceq $approved.key })
        if ($current.Count -ne 1) {
            throw "Expected exactly one current measurement for approved key '$($approved.key)', found $($current.Count)."
        }
        $statistic = [string] $approved.statistic
        $currentValue = [double] $current[0].statistics.$statistic
        $approvedValue = [double] $approved.approvedValue
        $changePercent = if ($approvedValue -eq 0.0) { 0.0 } else { (($currentValue / $approvedValue) - 1.0) * 100.0 }
        $regressed = Test-Regression $approvedValue $currentValue $Threshold
        $comparison = [ordered]@{
            key = $approved.key
            statistic = $statistic
            approvedValue = $approvedValue
            currentValue = $currentValue
            changePercent = [Math]::Round($changePercent, 2)
            thresholdPercent = $Threshold
            status = if ($regressed) { "regression" } else { "pass" }
        }
        $comparisons += $comparison
        if ($regressed) { $failures += $comparison }
    }

    [pscustomobject]@{
        Comparisons = @($comparisons)
        Failures = @($failures)
    }
}

function Assert-SelfTestThrows(
    [string] $Label,
    [scriptblock] $Action,
    [string] $ExpectedMessagePattern
) {
    try {
        & $Action
    } catch {
        if ($_.Exception.Message -notmatch $ExpectedMessagePattern) {
            throw "$Label produced an unexpected error: $($_.Exception.Message)"
        }
        return
    }
    throw "$Label did not reject the deliberately invalid fixture."
}

function Invoke-RegressionSelfTest {
    if (-not (Test-Regression 100.0 110.01 10.0)) {
        throw "Regression self-test failed to reject a deliberate >10% increase."
    }
    if (Test-Regression 100.0 110.0 10.0) {
        throw "Regression self-test incorrectly rejected the exact 10% boundary."
    }

    $deviceKey = "api37-Pixel_2"
    $targetId = "com.musfit.benchmark.MusFitJourneyBenchmark.trainingExerciseImageBrowse100Items"
    $otherId = "com.musfit.benchmark.MusFitJourneyBenchmark.trainingJourney"
    $targetKey = "$deviceKey|$targetId|frameDurationCpuMs"
    $otherKey = "$deviceKey|$otherId|frameDurationCpuMs"
    $targetMeasurement = [pscustomobject]@{
        key = $targetKey
        device = $deviceKey
        benchmark = $targetId
        gateStatistic = "p90"
        statistics = [pscustomobject]@{ p90 = 100.0 }
    }
    $otherMeasurement = [pscustomobject]@{
        key = $otherKey
        device = $deviceKey
        benchmark = $otherId
        gateStatistic = "p90"
        statistics = [pscustomobject]@{ p90 = 100.0 }
    }
    $targetedReportFixture = [pscustomobject]@{
        measurements = @($targetMeasurement)
    }
    $fullReportFixture = [pscustomobject]@{
        measurements = @($targetMeasurement, $otherMeasurement)
    }
    $approvedTarget = [pscustomobject]@{
        key = $targetKey
        statistic = "p90"
        approvedValue = 100.0
    }
    $approvedOther = [pscustomobject]@{
        key = $otherKey
        statistic = "p90"
        approvedValue = 100.0
    }
    $fullApprovedFixture = [pscustomobject]@{
        devices = @([pscustomobject]@{ key = $deviceKey })
        measurements = @($approvedTarget, $approvedOther)
    }
    $targetOnlyApprovedFixture = [pscustomobject]@{
        devices = @([pscustomobject]@{ key = $deviceKey })
        measurements = @($approvedTarget)
    }

    $targetScope = Get-BenchmarkScope `
        -Report $targetedReportFixture `
        -ApprovedBaseline $fullApprovedFixture `
        -ExactBenchmarkId $targetId `
        -SelectorSpecified
    if (
        @($targetScope.CurrentMeasurements).Count -ne 1 -or
        ([string] $targetScope.CurrentMeasurements[0].benchmark) -cne $targetId -or
        @($targetScope.ApprovedMeasurements).Count -ne 1
    ) {
        throw "Exact BenchmarkId self-test did not isolate the requested full benchmark ID."
    }

    $prefixId = $targetId.Substring(0, $targetId.LastIndexOf('.'))
    Assert-SelfTestThrows `
        -Label "BenchmarkId prefix self-test" `
        -ExpectedMessagePattern "did not exactly match a current gated benchmark ID" `
        -Action {
            Get-BenchmarkScope `
                -Report $targetedReportFixture `
                -ApprovedBaseline $fullApprovedFixture `
                -ExactBenchmarkId $prefixId `
                -SelectorSpecified | Out-Null
        }
    Assert-SelfTestThrows `
        -Label "Unknown BenchmarkId self-test" `
        -ExpectedMessagePattern "did not exactly match a current gated benchmark ID" `
        -Action {
            Get-BenchmarkScope `
                -Report $targetedReportFixture `
                -ApprovedBaseline $fullApprovedFixture `
                -ExactBenchmarkId "com.musfit.benchmark.UnknownBenchmark.unknown" `
                -SelectorSpecified | Out-Null
        }

    $malformedBaseline = [pscustomobject]@{
        measurements = @([pscustomobject]@{
            key = "$deviceKey|$targetId"
            statistic = "p90"
            approvedValue = 100.0
        })
    }
    Assert-SelfTestThrows `
        -Label "Malformed approved key self-test" `
        -ExpectedMessagePattern "Malformed approved benchmark measurement key" `
        -Action {
            Get-BenchmarkScope -Report $targetedReportFixture -ApprovedBaseline $malformedBaseline | Out-Null
        }

    Assert-SelfTestThrows `
        -Label "Default full-suite missing-key self-test" `
        -ExpectedMessagePattern "Expected exactly one current measurement for approved key" `
        -Action {
            $fullScope = Get-BenchmarkScope -Report $targetedReportFixture -ApprovedBaseline $fullApprovedFixture
            Compare-BenchmarkMeasurements `
                -CurrentMeasurements $fullScope.CurrentMeasurements `
                -ApprovedMeasurements $fullScope.ApprovedMeasurements `
                -ApprovedDeviceKeys @($deviceKey) `
                -Threshold 10.0 | Out-Null
        }

    Assert-SelfTestThrows `
        -Label "Unapproved current key self-test" `
        -ExpectedMessagePattern "Current gated measurements are missing from the approved baseline" `
        -Action {
            $unapprovedScope = Get-BenchmarkScope `
                -Report $fullReportFixture `
                -ApprovedBaseline $targetOnlyApprovedFixture
            Compare-BenchmarkMeasurements `
                -CurrentMeasurements $unapprovedScope.CurrentMeasurements `
                -ApprovedMeasurements $unapprovedScope.ApprovedMeasurements `
                -ApprovedDeviceKeys @($deviceKey) `
                -Threshold 10.0 | Out-Null
        }

    $targetedResult = Compare-BenchmarkMeasurements `
        -CurrentMeasurements $targetScope.CurrentMeasurements `
        -ApprovedMeasurements $targetScope.ApprovedMeasurements `
        -ApprovedDeviceKeys @($deviceKey) `
        -Threshold 10.0
    if (@($targetedResult.Comparisons).Count -ne 1 -or @($targetedResult.Failures).Count -ne 0) {
        throw "Targeted subset self-test did not compare exactly one passing approved measurement."
    }

    Write-Host "Benchmark selector, strictness, and deliberate >10% regression self-tests passed."
}

if ($SelfTest) {
    Invoke-RegressionSelfTest
}
if ([string]::IsNullOrWhiteSpace($ResultsPath)) {
    if ($SelfTest -and -not $WriteBaseline) {
        return
    }
    throw "ResultsPath is required unless running the built-in SelfTest only."
}

$documents = @(Get-BenchmarkDocuments $ResultsPath)
$report = ConvertTo-PerformanceReport $documents
$baseline = Resolve-RepoPath $BaselinePath
$reportDir = Resolve-RepoPath $ReportDirectory
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

if ($WriteBaseline) {
    $baselineParent = Split-Path -Parent $baseline
    New-Item -ItemType Directory -Path $baselineParent -Force | Out-Null
    $approved = [ordered]@{
        schemaVersion = 1
        thresholdPercent = $ThresholdPercent
        devices = $report.devices
        measurements = @(Get-ApprovedMeasurements $report)
    }
    $approved | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $baseline -Encoding utf8
    Write-Host "Approved benchmark baseline written: $baseline"
}

if (-not (Test-Path -LiteralPath $baseline -PathType Leaf)) {
    throw "Approved benchmark baseline does not exist: $baseline"
}
$approvedBaseline = Get-Content -LiteralPath $baseline -Raw | ConvertFrom-Json
if ([int] $approvedBaseline.schemaVersion -ne 1) {
    throw "Unsupported benchmark baseline schemaVersion: $($approvedBaseline.schemaVersion)"
}

$scope = Get-BenchmarkScope `
    -Report $report `
    -ApprovedBaseline $approvedBaseline `
    -ExactBenchmarkId $BenchmarkId `
    -SelectorSpecified:$benchmarkSelectorSpecified
$report.measurements = @($scope.CurrentMeasurements)
if ($benchmarkSelectorSpecified) {
    $report["benchmarkId"] = $BenchmarkId
}
$approvedDeviceKeys = @($approvedBaseline.devices | ForEach-Object { [string] $_.key })
$comparisonResult = Compare-BenchmarkMeasurements `
    -CurrentMeasurements $scope.CurrentMeasurements `
    -ApprovedMeasurements $scope.ApprovedMeasurements `
    -ApprovedDeviceKeys $approvedDeviceKeys `
    -Threshold $ThresholdPercent
$comparisons = @($comparisonResult.Comparisons)
$failures = @($comparisonResult.Failures)

$report.comparisons = $comparisons
$report.thresholdPercent = $ThresholdPercent
$report.status = if ($failures.Count -eq 0) { "pass" } else { "regression" }
$jsonReport = Join-Path $reportDir "benchmark-report.json"
$markdownReport = Join-Path $reportDir "benchmark-summary.md"
$report | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $jsonReport -Encoding utf8

$lines = @(
    "# MusFit benchmark report",
    "",
    "Status: **$($report.status)**",
    "",
    "Regression threshold: $ThresholdPercent%"
)
if ($benchmarkSelectorSpecified) {
    $lines += ""
    $lines += "Exact benchmark ID: ``$BenchmarkId``"
}
$lines += ""
$lines += "| Benchmark metric | Statistic | Approved | Current | Change | Result |"
$lines += "| --- | ---: | ---: | ---: | ---: | --- |"
foreach ($comparison in $comparisons) {
    $lines += "| $($comparison.key) | $($comparison.statistic) | $($comparison.approvedValue) | $($comparison.currentValue) | $($comparison.changePercent)% | $($comparison.status) |"
}
$lines | Set-Content -LiteralPath $markdownReport -Encoding utf8

Write-Host "Benchmark report: $jsonReport"
Write-Host "Benchmark summary: $markdownReport"
if ($failures.Count -gt 0) {
    $failureText = @($failures | ForEach-Object { "$($_.key)=$($_.changePercent)%" }) -join "; "
    if ($ReportOnly) {
        Write-Warning "Benchmark regression threshold exceeded: $failureText"
        if ($env:GITHUB_ACTIONS -eq "true") {
            Write-Host "::warning title=MusFit benchmark regression::$failureText"
        }
        Write-Host "Benchmark regression was reported without failing this noisy hosted-emulator run."
        return
    }
    throw "Benchmark regression threshold exceeded: $failureText"
}
Write-Host "Benchmark regression gate passed ($($comparisons.Count) approved measurements)."
