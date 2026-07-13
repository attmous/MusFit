param(
    [Parameter(Mandatory = $true)][string] $CommitSha,
    [string] $ArtifactDirectory = "build/verified-artifacts"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
if ($CommitSha -notmatch '^[0-9a-f]{40}$') { throw "CommitSha must be a full lowercase Git SHA." }
$dir = if ([IO.Path]::IsPathRooted($ArtifactDirectory)) { $ArtifactDirectory } else { Join-Path $repoRoot $ArtifactDirectory }

$expectedNames = @("build-metadata.json", "musfit-production-unsigned.aab", "musfit-production-unsigned.apk", "musfit.cdx.json")
$checksumsPath = Join-Path $dir "SHA256SUMS"
$metadataPath = Join-Path $dir "build-metadata.json"
foreach ($path in @($checksumsPath, $metadataPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing supply-chain artifact: $path" }
}

$entries = @{}
foreach ($line in Get-Content -LiteralPath $checksumsPath) {
    if ($line -notmatch '^([0-9a-f]{64})  ([A-Za-z0-9._-]+)$') { throw "Malformed SHA256SUMS line: $line" }
    if ($entries.ContainsKey($Matches[2])) { throw "Duplicate SHA256SUMS entry: $($Matches[2])" }
    $entries[$Matches[2]] = $Matches[1]
}
$actualSet = @($entries.Keys | Sort-Object) -join "`n"
$expectedSet = @($expectedNames | Sort-Object) -join "`n"
if ($actualSet -cne $expectedSet) {
    throw "SHA256SUMS does not contain the exact verified artifact set."
}
foreach ($name in $expectedNames) {
    $path = Join-Path $dir $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing checksummed artifact: $name" }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -cne $entries[$name]) { throw "SHA-256 mismatch for $name" }
}

$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
if ($metadata.schemaVersion -ne 1 -or $metadata.commitSha -cne $CommitSha) { throw "Build metadata is not bound to $CommitSha" }
if (@($metadata.artifacts).Count -ne 3) { throw "Build metadata must describe exactly three artifacts." }
foreach ($artifact in @($metadata.artifacts)) {
    $path = Join-Path $dir $artifact.name
    if ($artifact.name -notin $expectedNames -or $artifact.name -ceq "build-metadata.json") { throw "Unexpected metadata artifact: $($artifact.name)" }
    $item = Get-Item -LiteralPath $path
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($item.Length -ne $artifact.bytes -or $hash -cne $artifact.sha256) { throw "Metadata mismatch for $($artifact.name)" }
}

$sbom = Get-Content -LiteralPath (Join-Path $dir "musfit.cdx.json") -Raw | ConvertFrom-Json
if ($sbom.bomFormat -cne "CycloneDX" -or $sbom.specVersion -cne "1.6" -or @($sbom.components).Count -lt 1) {
    throw "CycloneDX 1.6 SBOM is missing or empty."
}
Write-Host "Verified build artifact set is intact and bound to $CommitSha."
