param(
    [long] $BaselineApkBytes = 135503830,
    [long] $BaselineAabBytes = 49700255,
    [long] $MaximumAabBytes = 41943040
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$apk = Join-Path $repoRoot "app/build/outputs/apk/production/release/app-production-release-unsigned.apk"
$aab = Join-Path $repoRoot "app/build/outputs/bundle/productionRelease/app-production-release.aab"

foreach ($artifact in @($apk, $aab)) {
    if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) {
        throw "Missing optimized production artifact: $artifact"
    }
}

$apkBytes = (Get-Item -LiteralPath $apk).Length
$aabBytes = (Get-Item -LiteralPath $aab).Length
if ($apkBytes -ge $BaselineApkBytes) {
    throw "Optimized APK did not shrink: $apkBytes bytes versus $BaselineApkBytes baseline."
}
if ($aabBytes -ge $BaselineAabBytes) {
    throw "Optimized AAB did not shrink: $aabBytes bytes versus $BaselineAabBytes baseline."
}
if ($aabBytes -gt $MaximumAabBytes) {
    throw "Optimized AAB exceeds the W1-REL-03 40 MiB budget: $aabBytes bytes."
}

$variants = @("productionRelease", "legacyMigrationRelease")
foreach ($variant in $variants) {
    $mapping = Join-Path $repoRoot "app/build/outputs/mapping/$variant/mapping.txt"
    if (-not (Test-Path -LiteralPath $mapping -PathType Leaf) -or (Get-Item -LiteralPath $mapping).Length -le 0) {
        throw "Missing or empty R8 mapping for $variant."
    }
    foreach ($report in @("usage.txt", "seeds.txt", "configuration.txt")) {
        $path = Join-Path $repoRoot "app/build/outputs/r8Reports/$variant/$report"
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Missing R8 report for ${variant}: $report"
        }
        if ((Get-Item -LiteralPath $path).Length -le 0) {
            throw "Empty R8 report for ${variant}: $report"
        }
    }
}

$apkDelta = $BaselineApkBytes - $apkBytes
$aabDelta = $BaselineAabBytes - $aabBytes
$apkPercent = [math]::Round(($apkDelta / [double] $BaselineApkBytes) * 100, 2)
$aabPercent = [math]::Round(($aabDelta / [double] $BaselineAabBytes) * 100, 2)

Write-Host "R8 artifact verification passed."
Write-Host "Production APK: $apkBytes bytes (-$apkDelta, -$apkPercent%)."
Write-Host "Production AAB: $aabBytes bytes (-$aabDelta, -$aabPercent%)."
