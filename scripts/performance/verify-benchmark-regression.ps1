param(
    [Parameter(Mandatory = $true)]
    [string] $ResultsPath,
    [string] $BaselinePath = "benchmark/baselines/approved-api37.json",
    [string] $ReportDirectory = "build/reports/performance",
    [ValidateRange(0.1, 100.0)]
    [double] $ThresholdPercent = 10.0,
    [switch] $WriteBaseline,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path

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

$comparisons = @()
$failures = @()
foreach ($approved in $approvedBaseline.measurements) {
    $current = @($report.measurements | Where-Object { $_.key -ceq $approved.key })
    if ($current.Count -ne 1) {
        throw "Expected exactly one current measurement for approved key '$($approved.key)', found $($current.Count)."
    }
    $statistic = [string] $approved.statistic
    $currentValue = [double] $current[0].statistics.$statistic
    $approvedValue = [double] $approved.approvedValue
    $changePercent = if ($approvedValue -eq 0.0) { 0.0 } else { (($currentValue / $approvedValue) - 1.0) * 100.0 }
    $regressed = Test-Regression $approvedValue $currentValue $ThresholdPercent
    $comparison = [ordered]@{
        key = $approved.key
        statistic = $statistic
        approvedValue = $approvedValue
        currentValue = $currentValue
        changePercent = [Math]::Round($changePercent, 2)
        thresholdPercent = $ThresholdPercent
        status = if ($regressed) { "regression" } else { "pass" }
    }
    $comparisons += $comparison
    if ($regressed) { $failures += $comparison }
}

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
    "Regression threshold: $ThresholdPercent%",
    "",
    "| Benchmark metric | Statistic | Approved | Current | Change | Result |",
    "| --- | ---: | ---: | ---: | ---: | --- |"
)
foreach ($comparison in $comparisons) {
    $lines += "| $($comparison.key) | $($comparison.statistic) | $($comparison.approvedValue) | $($comparison.currentValue) | $($comparison.changePercent)% | $($comparison.status) |"
}
$lines | Set-Content -LiteralPath $markdownReport -Encoding utf8

if ($SelfTest) {
    if (-not (Test-Regression 100.0 110.01 10.0)) {
        throw "Regression self-test failed to reject a deliberate >10% increase."
    }
    if (Test-Regression 100.0 110.0 10.0) {
        throw "Regression self-test incorrectly rejected the exact 10% boundary."
    }
    Write-Host "Deliberate >10% regression self-test passed."
}

Write-Host "Benchmark report: $jsonReport"
Write-Host "Benchmark summary: $markdownReport"
if ($failures.Count -gt 0) {
    $failureText = @($failures | ForEach-Object { "$($_.key)=$($_.changePercent)%" }) -join "; "
    throw "Benchmark regression threshold exceeded: $failureText"
}
Write-Host "Benchmark regression gate passed ($($comparisons.Count) approved measurements)."
