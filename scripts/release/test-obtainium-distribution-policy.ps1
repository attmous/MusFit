param(
    [string] $MetadataPath,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
$expectedAbis = @("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

function Assert-DistributionMetadata([object] $Metadata) {
    if ([int] $Metadata.schemaVersion -ne 2) {
        throw "Release metadata schemaVersion must be 2."
    }
    $distribution = $Metadata.distribution
    if ($null -eq $distribution) { throw "Release metadata is missing distribution policy." }
    if ($distribution.obtainiumMode -cne "google-play-universal") {
        throw "Obtainium must use the Google Play universal APK."
    }
    if ($distribution.obtainiumApkFile -cne "musfit-universal.apk" -or
        $Metadata.apkFile -cne "musfit-universal.apk") {
        throw "Release metadata must bind Obtainium to musfit-universal.apk."
    }
    if ([bool] $distribution.abiSpecificApksPublished) {
        throw "ABI-specific APKs cannot be published with the Google-managed app-signing key."
    }
    if ($distribution.signingCustody -cne "google-managed-app-signing") {
        throw "Release metadata must preserve Google-managed app-signing custody."
    }
    if ($distribution.measuredException -cne "W5-SIZE-02-GOOGLE-MANAGED-SIGNING") {
        throw "Release metadata must identify the approved W5-SIZE-02 exception."
    }
    $actualAbis = @($distribution.supportedAbis)
    if ($actualAbis.Count -ne $expectedAbis.Count -or
        (Compare-Object -ReferenceObject $expectedAbis -DifferenceObject $actualAbis).Count -ne 0) {
        throw "Universal APK ABI support must remain arm64-v8a, armeabi-v7a, x86, and x86_64."
    }
}

function New-Fixture([bool] $AbiSpecific = $false) {
    [pscustomobject]@{
        schemaVersion = 2
        apkFile = "musfit-universal.apk"
        distribution = [pscustomobject]@{
            obtainiumMode = "google-play-universal"
            obtainiumApkFile = "musfit-universal.apk"
            abiSpecificApksPublished = $AbiSpecific
            supportedAbis = $expectedAbis
            signingCustody = "google-managed-app-signing"
            measuredException = "W5-SIZE-02-GOOGLE-MANAGED-SIGNING"
        }
    }
}

if ($SelfTest) {
    Assert-DistributionMetadata (New-Fixture)
    $badAbiPublicationRejected = $false
    try { Assert-DistributionMetadata (New-Fixture -AbiSpecific $true) } catch { $badAbiPublicationRejected = $true }
    if (-not $badAbiPublicationRejected) { throw "ABI-specific publication self-test failed." }

    $badCustody = New-Fixture
    $badCustody.distribution.signingCustody = "upload-key"
    $badCustodyRejected = $false
    try { Assert-DistributionMetadata $badCustody } catch { $badCustodyRejected = $true }
    if (-not $badCustodyRejected) { throw "Signing-custody self-test failed." }

    Write-Host "Obtainium distribution policy self-test passed."
    return
}

if ([string]::IsNullOrWhiteSpace($MetadataPath)) { throw "MetadataPath is required." }
$metadata = Get-Content -LiteralPath (Resolve-Path -LiteralPath $MetadataPath) -Raw | ConvertFrom-Json
Assert-DistributionMetadata $metadata

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$workflow = Get-Content -LiteralPath (Join-Path $repoRoot ".github\workflows\release.yml") -Raw
if ($workflow -match '(?i)musfit-(?:arm64-v8a|armeabi-v7a|x86|x86_64)\.apk') {
    throw "The release workflow must not publish upload-key-signed ABI APKs."
}
if ($workflow -notmatch 'musfit-universal\.apk') {
    throw "The release workflow is missing the Google-signed universal APK."
}

Write-Host "Obtainium distribution policy verified: Google-signed universal APK for four supported ABIs."
