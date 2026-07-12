param(
    [string] $SignedBundle,
    [string] $OutputDirectory,
    [string] $CommitSha,
    [string] $ExpectedAppSigningCertificateSha256,
    [string] $PackageName = "com.musfit",
    [string] $Track = "internal",
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

function Normalize-Certificate([string] $Value) {
    if ($null -eq $Value) { return "" }
    $Value.Replace(":", "").Replace("-", "").ToLowerInvariant()
}

function Select-UniversalApk([object[]] $GeneratedApks, [string] $ExpectedCertificate) {
    $expected = Normalize-Certificate $ExpectedCertificate
    @($GeneratedApks | Where-Object {
        (Normalize-Certificate $_.certificateSha256Hash) -ceq $expected -and
        -not [string]::IsNullOrWhiteSpace($_.generatedUniversalApk.downloadId)
    })
}

function Get-GeneratedApkMediaUri(
    [string] $Api,
    [long] $VersionCode,
    [string] $DownloadId
) {
    $encodedDownloadId = [uri]::EscapeDataString($DownloadId)
    "$Api/generatedApks/$VersionCode/downloads/${encodedDownloadId}:download?alt=media"
}

function Add-ExactDraftRelease([object[]] $ExistingReleases, [long] $VersionCode, [string] $Name) {
    if (@($ExistingReleases | Where-Object { $_.status -ceq "draft" }).Count -ne 0) {
        throw "Internal track already contains a draft; resolve it before creating another candidate."
    }
    if (@($ExistingReleases | Where-Object {
        @($_.versionCodes | ForEach-Object { [long]$_ }) -contains $VersionCode
    }).Count -ne 0) {
        throw "Internal track already contains version $VersionCode."
    }
    @($ExistingReleases) + @([pscustomobject]@{
        name = $Name
        status = "draft"
        versionCodes = @($VersionCode.ToString())
    })
}

if ($SelfTest) {
    $cert = "ab" * 32
    $fixture = @(
        [pscustomobject]@{ certificateSha256Hash = "cd" * 32; generatedUniversalApk = [pscustomobject]@{ downloadId = "wrong" } },
        [pscustomobject]@{ certificateSha256Hash = $cert; generatedUniversalApk = [pscustomobject]@{ downloadId = "expected" } }
    )
    $selected = @(Select-UniversalApk $fixture $cert)
    if ($selected.Count -ne 1 -or $selected[0].generatedUniversalApk.downloadId -cne "expected") {
        throw "Google-signed universal APK selection self-test failed."
    }
    if (@(Select-UniversalApk $fixture ("ef" * 32)).Count -ne 0) { throw "Certificate mismatch self-test failed." }
    $mediaUri = Get-GeneratedApkMediaUri "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.musfit" 10 "id/with+symbols"
    if ($mediaUri -cne "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.musfit/generatedApks/10/downloads/id%2Fwith%2Bsymbols:download?alt=media") {
        throw "Google Play media-download URI self-test failed."
    }
    $existing = @([pscustomobject]@{ name = "old"; status = "completed"; versionCodes = @("9") })
    $merged = @(Add-ExactDraftRelease $existing 10 "new")
    if ($merged.Count -ne 2 -or $merged[0].status -cne "completed" -or $merged[1].status -cne "draft") {
        throw "Existing-release preservation self-test failed."
    }
    $conflictRejected = $false
    try { Add-ExactDraftRelease @([pscustomobject]@{ status = "draft"; versionCodes = @("8") }) 10 "new" | Out-Null } catch { $conflictRejected = $true }
    if (-not $conflictRejected) { throw "Conflicting draft self-test failed." }
    Write-Host "Play Option A selection self-test passed."
    return
}

foreach ($required in @("SignedBundle", "OutputDirectory", "CommitSha", "ExpectedAppSigningCertificateSha256")) {
    if ([string]::IsNullOrWhiteSpace((Get-Variable -Name $required -ValueOnly))) { throw "$required is required." }
}
if ($CommitSha -notmatch '^[0-9a-f]{40}$') { throw "CommitSha must be a full lowercase Git SHA." }
if ($PackageName -cne "com.musfit") { throw "W1-REL-04 may publish only com.musfit." }
if ($Track -cne "internal") { throw "The first verified W1-REL-04 publication is restricted to Play's internal track." }
$expectedCert = Normalize-Certificate $ExpectedAppSigningCertificateSha256
if ($expectedCert -notmatch '^[0-9a-f]{64}$') { throw "Expected app-signing certificate SHA-256 must contain 64 hex characters." }
if (-not $env:MUSFIT_PLAY_ACCESS_TOKEN) { throw "MUSFIT_PLAY_ACCESS_TOKEN is required." }
$bundle = (Resolve-Path -LiteralPath $SignedBundle).Path
$output = [IO.Path]::GetFullPath($OutputDirectory)
[IO.Directory]::CreateDirectory($output) | Out-Null

$headers = @{ Authorization = "Bearer $($env:MUSFIT_PLAY_ACCESS_TOKEN)" }
$api = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PackageName"
$uploadApi = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PackageName"
$edit = Invoke-RestMethod -Method Post -Uri "$api/edits" -Headers $headers -ContentType "application/json" -Body "{}"
if ([string]::IsNullOrWhiteSpace($edit.id)) { throw "Google Play did not return an edit id." }

$uploaded = Invoke-RestMethod -Method Post -Uri "$uploadApi/edits/$($edit.id)/bundles?uploadType=media" `
    -Headers $headers -ContentType "application/octet-stream" -InFile $bundle
$versionCode = [long]$uploaded.versionCode
if ($versionCode -le 0) { throw "Google Play did not return a valid version code." }

$existingReleases = @()
try {
    $currentTrack = Invoke-RestMethod -Method Get -Uri "$api/edits/$($edit.id)/tracks/$Track" -Headers $headers
    $existingReleases = @($currentTrack.releases)
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -ne 404) { throw }
}
$releases = @(Add-ExactDraftRelease $existingReleases $versionCode "MusFit $versionCode ($($CommitSha.Substring(0, 12)))")
$trackBody = @{ track = $Track; releases = $releases } | ConvertTo-Json -Depth 10
Invoke-RestMethod -Method Put -Uri "$api/edits/$($edit.id)/tracks/$Track" -Headers $headers `
    -ContentType "application/json" -Body $trackBody | Out-Null
Invoke-RestMethod -Method Post -Uri "$api/edits/$($edit.id):commit" -Headers $headers `
    -ContentType "application/json" -Body "{}" | Out-Null

$selected = @()
for ($attempt = 1; $attempt -le 30 -and $selected.Count -eq 0; $attempt++) {
    try {
        $generated = Invoke-RestMethod -Method Get -Uri "$api/generatedApks/$versionCode" -Headers $headers
        $selected = @(Select-UniversalApk @($generated.generatedApks) $expectedCert)
    } catch {
        if ($attempt -eq 30) { throw }
    }
    if ($selected.Count -eq 0) { Start-Sleep -Seconds 10 }
}
if ($selected.Count -ne 1) { throw "Google Play did not expose exactly one universal APK with the approved app-signing certificate." }

$apk = Join-Path $output "musfit-universal.apk"
$downloadUri = Get-GeneratedApkMediaUri $api $versionCode $selected[0].generatedUniversalApk.downloadId
$downloadResponse = Invoke-WebRequest -Method Get -Uri $downloadUri -Headers $headers -OutFile $apk -PassThru
if (-not (Test-Path -LiteralPath $apk -PathType Leaf) -or (Get-Item -LiteralPath $apk).Length -le 0) {
    throw "Google Play universal APK download is missing or empty."
}
$downloadedBytes = (Get-Item -LiteralPath $apk).Length
$contentType = @($downloadResponse.Headers["Content-Type"])[0]
Write-Host "Downloaded Google Play universal APK: HTTP $([int]$downloadResponse.StatusCode), $contentType, $downloadedBytes bytes."

$aab = Join-Path $output "musfit-play-upload.aab"
Copy-Item -LiteralPath $bundle -Destination $aab -Force
& (Join-Path $PSScriptRoot "write-release-candidate-metadata.ps1") `
    -CandidateDirectory $output -CommitSha $CommitSha -VersionCode $versionCode `
    -AppSigningCertificateSha256 $expectedCert -Track $Track -ReleaseStatus draft

if ($env:GITHUB_OUTPUT) {
    "version_code=$versionCode" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8 -Append
}
Write-Host "Play internal draft committed and Google-signed universal APK downloaded for version $versionCode."
