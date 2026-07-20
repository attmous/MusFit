param(
    [Parameter(Mandatory)] [string] $CandidateDirectory,
    [Parameter(Mandatory)] [string] $CommitSha,
    [Parameter(Mandatory)] [long] $VersionCode,
    [Parameter(Mandatory)] [string] $AppSigningCertificateSha256,
    [string] $Track = "internal",
    [ValidateSet("draft", "completed")] [string] $ReleaseStatus = "completed"
)

$ErrorActionPreference = "Stop"
if ($CommitSha -notmatch '^[0-9a-f]{40}$') { throw "CommitSha must be a full lowercase Git SHA." }
if ($VersionCode -le 0) { throw "VersionCode must be positive." }
$certificate = $AppSigningCertificateSha256.Replace(":", "").Replace("-", "").ToLowerInvariant()
if ($certificate -notmatch '^[0-9a-f]{64}$') { throw "App-signing certificate SHA-256 must contain 64 hex characters." }
$output = (Resolve-Path -LiteralPath $CandidateDirectory).Path
$apk = Join-Path $output "musfit-universal.apk"
$aab = Join-Path $output "musfit-play-upload.aab"
foreach ($artifact in @($apk, $aab)) {
    if (-not (Test-Path -LiteralPath $artifact -PathType Leaf) -or (Get-Item -LiteralPath $artifact).Length -le 0) {
        throw "Missing or empty candidate artifact: $artifact"
    }
}

$metadata = [ordered]@{
    schemaVersion = 2
    packageName = "com.musfit"
    track = $Track
    releaseStatus = $ReleaseStatus
    commitSha = $CommitSha
    versionCode = $VersionCode
    appSigningCertificateSha256 = $certificate
    apkFile = "musfit-universal.apk"
    aabFile = "musfit-play-upload.aab"
    apkSha256 = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    aabSha256 = (Get-FileHash -LiteralPath $aab -Algorithm SHA256).Hash.ToLowerInvariant()
    distribution = [ordered]@{
        obtainiumMode = "google-play-universal"
        obtainiumApkFile = "musfit-universal.apk"
        abiSpecificApksPublished = $false
        supportedAbis = @("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        signingCustody = "google-managed-app-signing"
        measuredException = "W5-SIZE-02-GOOGLE-MANAGED-SIGNING"
    }
}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $output "release-metadata.json") -Encoding utf8
& (Join-Path $PSScriptRoot "test-obtainium-distribution-policy.ps1") `
    -MetadataPath (Join-Path $output "release-metadata.json")
@(
    "$($metadata.apkSha256)  musfit-universal.apk"
    "$($metadata.aabSha256)  musfit-play-upload.aab"
) | Set-Content -LiteralPath (Join-Path $output "SHA256SUMS") -Encoding ascii
Write-Host "Release candidate metadata written for version $VersionCode and commit $CommitSha."
