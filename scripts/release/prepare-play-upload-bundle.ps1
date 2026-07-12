param(
    [Parameter(Mandatory)] [string] $InputAab,
    [Parameter(Mandatory)] [string] $OutputAab,
    [Parameter(Mandatory)] [string] $Keystore,
    [Parameter(Mandatory)] [string] $KeyAlias,
    [Parameter(Mandatory)] [string] $ExpectedUploadCertificateSha256
)

$ErrorActionPreference = "Stop"
$windows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
foreach ($name in @("MUSFIT_UPLOAD_STORE_PASSWORD", "MUSFIT_UPLOAD_KEY_PASSWORD")) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "$name must be provided as a protected environment secret."
    }
}
if (-not $env:JAVA_HOME) { throw "JAVA_HOME is required." }

$input = (Resolve-Path -LiteralPath $InputAab).Path
$store = (Resolve-Path -LiteralPath $Keystore).Path
if ($store -match '(?i)musfit\.debug\.keystore') { throw "The public development key cannot sign a Play upload bundle." }
$output = [IO.Path]::GetFullPath($OutputAab)
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($output)) | Out-Null

$jarsigner = Join-Path $env:JAVA_HOME $(if ($windows) { "bin\jarsigner.exe" } else { "bin/jarsigner" })
$keytool = Join-Path $env:JAVA_HOME $(if ($windows) { "bin\keytool.exe" } else { "bin/keytool" })
& $jarsigner -keystore $store -storepass:env MUSFIT_UPLOAD_STORE_PASSWORD -keypass:env MUSFIT_UPLOAD_KEY_PASSWORD `
    -sigalg SHA256withRSA -digestalg SHA-256 -signedjar $output $input $KeyAlias
if ($LASTEXITCODE -ne 0) { throw "Play upload AAB signing failed." }
$verification = & $jarsigner -verify $output 2>&1
if ($LASTEXITCODE -ne 0 -or ($verification -join "`n") -notmatch '(?m)^jar verified\.$') {
    throw "Signed Play upload AAB cryptographic verification failed."
}

$certificate = & $keytool -printcert -jarfile $output 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not inspect the signed Play upload AAB." }
$match = [regex]::Match(($certificate -join "`n"), 'SHA256:\s*([0-9A-Fa-f:]+)')
if (-not $match.Success) { throw "Could not read the Play upload certificate SHA-256." }
$actual = $match.Groups[1].Value.Replace(":", "").ToLowerInvariant()
$expected = $ExpectedUploadCertificateSha256.Replace(":", "").ToLowerInvariant()
if ($expected -notmatch '^[0-9a-f]{64}$') { throw "Expected upload certificate SHA-256 must contain 64 hex characters." }
if ($actual -cne $expected) { throw "Play upload AAB certificate does not match MUSFIT_UPLOAD_CERT_SHA256." }

Write-Host "Play upload AAB signed and verified with upload certificate $actual."
