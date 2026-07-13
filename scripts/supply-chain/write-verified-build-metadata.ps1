param(
    [Parameter(Mandatory = $true)][string] $CommitSha,
    [string] $OutputDirectory = "build/verified-artifacts",
    [string] $Apk = "app/build/outputs/apk/production/release/app-production-release-unsigned.apk",
    [string] $Aab = "app/build/outputs/bundle/productionRelease/app-production-release.aab",
    [string] $Sbom = "build/reports/cyclonedx/bom.json"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
if ($CommitSha -notmatch '^[0-9a-f]{40}$') { throw "CommitSha must be a full lowercase Git SHA." }

function Resolve-Input([string] $Path) {
    $candidate = if ([IO.Path]::IsPathRooted($Path)) { $Path } else { Join-Path $repoRoot $Path }
    $item = Get-Item -LiteralPath $candidate -ErrorAction Stop
    if (-not $item.PSIsContainer -and $item.Length -gt 0) { return $item }
    throw "Supply-chain input must be a non-empty file: $candidate"
}

$inputs = @(
    [pscustomobject]@{ Type = "apk"; Item = Resolve-Input $Apk; Name = "musfit-production-unsigned.apk" },
    [pscustomobject]@{ Type = "aab"; Item = Resolve-Input $Aab; Name = "musfit-production-unsigned.aab" },
    [pscustomobject]@{ Type = "cyclonedx"; Item = Resolve-Input $Sbom; Name = "musfit.cdx.json" }
)

$output = if ([IO.Path]::IsPathRooted($OutputDirectory)) { $OutputDirectory } else { Join-Path $repoRoot $OutputDirectory }
New-Item -ItemType Directory -Path $output -Force | Out-Null

$artifacts = foreach ($input in $inputs) {
    $destination = Join-Path $output $input.Name
    Copy-Item -LiteralPath $input.Item.FullName -Destination $destination -Force
    $copied = Get-Item -LiteralPath $destination
    [ordered]@{
        name = $input.Name
        type = $input.Type
        bytes = $copied.Length
        sha256 = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
    }
}

$metadataPath = Join-Path $output "build-metadata.json"
[ordered]@{
    schemaVersion = 1
    commitSha = $CommitSha
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
    artifacts = $artifacts
} | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $metadataPath -Encoding utf8

$checksumNames = @($inputs.Name) + "build-metadata.json"
$checksumLines = foreach ($name in $checksumNames | Sort-Object) {
    $hash = (Get-FileHash -LiteralPath (Join-Path $output $name) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $name"
}
$checksumLines | Set-Content -LiteralPath (Join-Path $output "SHA256SUMS") -Encoding ascii
Write-Host "Verified build metadata prepared for $CommitSha in $output"
