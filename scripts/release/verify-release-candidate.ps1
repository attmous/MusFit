param(
    [Parameter(Mandatory)] [string] $CandidateDirectory,
    [Parameter(Mandatory)] [string] $ExpectedCommitSha,
    [Parameter(Mandatory)] [string] $ExpectedAppSigningCertificateSha256,
    [Parameter(Mandatory)] [string] $ExpectedUploadCertificateSha256
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$windows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
if ($windows) {
    . (Join-Path $repoRoot "scripts\android\android-env.ps1")
} elseif (-not $env:ANDROID_SDK_ROOT -and $env:ANDROID_HOME) {
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
}
if (-not $env:ANDROID_SDK_ROOT -or -not $env:JAVA_HOME) { throw "Android SDK and JAVA_HOME are required." }

function Normalize-Certificate([string] $Value) {
    $Value.Replace(":", "").Replace("-", "").ToLowerInvariant()
}

$dir = (Resolve-Path -LiteralPath $CandidateDirectory).Path
$apk = Join-Path $dir "musfit-universal.apk"
$aab = Join-Path $dir "musfit-play-upload.aab"
$metadataPath = Join-Path $dir "release-metadata.json"
$checksumsPath = Join-Path $dir "SHA256SUMS"
foreach ($artifact in @($apk, $aab, $metadataPath, $checksumsPath)) {
    if (-not (Test-Path -LiteralPath $artifact -PathType Leaf) -or (Get-Item -LiteralPath $artifact).Length -le 0) {
        throw "Missing or empty release candidate artifact: $artifact"
    }
}

$buildTools = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_SDK_ROOT "build-tools") -Directory |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $buildTools) { throw "Android build-tools are not installed." }
$apksigner = Join-Path $buildTools.FullName $(if ($windows) { "apksigner.bat" } else { "apksigner" })
$aapt2 = Join-Path $buildTools.FullName $(if ($windows) { "aapt2.exe" } else { "aapt2" })
$jarsigner = Join-Path $env:JAVA_HOME $(if ($windows) { "bin\jarsigner.exe" } else { "bin/jarsigner" })
$keytool = Join-Path $env:JAVA_HOME $(if ($windows) { "bin\keytool.exe" } else { "bin/keytool" })

$apkSignature = & $apksigner verify --verbose --print-certs $apk 2>&1
if ($LASTEXITCODE -ne 0) { throw "Google-signed universal APK signature verification failed.`n$($apkSignature -join "`n")" }
$apkCertMatch = [regex]::Match(($apkSignature -join "`n"), 'certificate SHA-256 digest:\s*([0-9A-Fa-f:]+)')
if (-not $apkCertMatch.Success) { throw "Could not read universal APK signing certificate." }
$apkCert = Normalize-Certificate $apkCertMatch.Groups[1].Value
$expectedAppCert = Normalize-Certificate $ExpectedAppSigningCertificateSha256
if ($apkCert -cne $expectedAppCert) { throw "Universal APK is not signed by the approved Google-managed app-signing key." }

$aabVerification = & $jarsigner -verify $aab 2>&1
if ($LASTEXITCODE -ne 0 -or ($aabVerification -join "`n") -notmatch '(?m)^jar verified\.$') {
    throw "Play upload AAB cryptographic signature verification failed."
}
$aabCertificate = & $keytool -printcert -jarfile $aab 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not inspect Play upload AAB certificate." }
$aabCertMatch = [regex]::Match(($aabCertificate -join "`n"), 'SHA256:\s*([0-9A-Fa-f:]+)')
if (-not $aabCertMatch.Success) { throw "Could not read Play upload AAB certificate." }
$aabCert = Normalize-Certificate $aabCertMatch.Groups[1].Value
$expectedUploadCert = Normalize-Certificate $ExpectedUploadCertificateSha256
if ($aabCert -cne $expectedUploadCert) { throw "AAB is not signed by the approved upload key." }
if ($aabCert -ceq $apkCert) { throw "Option A requires distinct upload and Google-managed app-signing keys." }

$badging = & $aapt2 dump badging $apk 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not inspect universal APK manifest.`n$($badging -join "`n")" }
$badgingText = $badging -join "`n"
if ($badgingText -notmatch "package:\s+name='com\.musfit'") { throw "Universal APK package must be com.musfit." }
if ($badgingText -match "application-debuggable") { throw "Universal APK must be non-debuggable." }
if ($badgingText -match "android\.permission\.ACCESS_LOCAL_NETWORK") { throw "Universal APK must not request local-network access." }
$manifestTree = & $aapt2 dump xmltree $apk --file AndroidManifest.xml 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not inspect compiled universal APK manifest.`n$($manifestTree -join "`n")" }
$manifestText = $manifestTree -join "`n"
if ($manifestText -notmatch 'allowBackup[^\r\n]*=false') { throw "Universal APK must set allowBackup=false." }
if ($manifestText -notmatch 'usesCleartextTraffic[^\r\n]*=false') { throw "Universal APK must set usesCleartextTraffic=false." }
if ($manifestText -match 'networkSecurityConfig') { throw "Universal APK must not declare a production networkSecurityConfig override." }
if ($manifestText -match 'ACCESS_LOCAL_NETWORK|DebugSeedReceiver|MUSFIT_SEED') { throw "Universal APK contains an internal network or seed surface." }
if ($manifestText -notmatch '(?s)name[^\r\n]*="com\.musfit\.ui\.transfer\.DataTransferActivity".{0,250}exported[^\r\n]*=false') {
    throw "Production DataTransferActivity must remain non-exported."
}
if ($manifestText -notmatch '(?s)name[^\r\n]*="com\.musfit\.LegacyMigrationLauncher".{0,250}enabled[^\r\n]*=false') {
    throw "Legacy migration launcher must be disabled in production."
}
if ($manifestText -notmatch '(?s)name[^\r\n]*="com\.musfit\.MainLauncher".{0,250}enabled[^\r\n]*=true') {
    throw "Production MainLauncher must be enabled."
}
$versionMatch = [regex]::Match($badgingText, "package:.*versionCode='([0-9]+)'")
if (-not $versionMatch.Success) { throw "Could not read universal APK version code." }
$apkVersionCode = [long]$versionMatch.Groups[1].Value

$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
& (Join-Path $PSScriptRoot "test-obtainium-distribution-policy.ps1") -MetadataPath $metadataPath
if ($ExpectedCommitSha -notmatch '^[0-9a-f]{40}$' -or $metadata.commitSha -cne $ExpectedCommitSha) {
    throw "Release metadata is not bound to the expected commit."
}
if ((Normalize-Certificate $metadata.appSigningCertificateSha256) -cne $expectedAppCert) {
    throw "Release metadata app-signing certificate mismatch."
}
if ([long]$metadata.versionCode -ne $apkVersionCode) { throw "Release metadata version code does not match the universal APK." }
$expectedHashes = @{
    "musfit-universal.apk" = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    "musfit-play-upload.aab" = (Get-FileHash -LiteralPath $aab -Algorithm SHA256).Hash.ToLowerInvariant()
}
$checksumLines = @(Get-Content -LiteralPath $checksumsPath)
foreach ($name in $expectedHashes.Keys) {
    $matchingHashes = @($checksumLines | ForEach-Object {
        $match = [regex]::Match($_, "^([0-9a-f]{64})\s\s$([regex]::Escape($name))$")
        if ($match.Success) { $match.Groups[1].Value }
    })
    if ($matchingHashes.Count -ne 1 -or $matchingHashes[0] -cne $expectedHashes[$name]) {
        throw "SHA256SUMS mismatch for $name."
    }
}
if ($metadata.apkSha256 -cne $expectedHashes["musfit-universal.apk"] -or
    $metadata.aabSha256 -cne $expectedHashes["musfit-play-upload.aab"]) {
    throw "Release metadata checksum mismatch."
}

Write-Host "Release candidate verified for commit $ExpectedCommitSha."
Write-Host "Google app-signing certificate: $apkCert"
Write-Host "Play upload certificate: $aabCert"
Write-Host "Universal APK SHA-256: $($expectedHashes['musfit-universal.apk'])"
Write-Host "Play upload AAB SHA-256: $($expectedHashes['musfit-play-upload.aab'])"
