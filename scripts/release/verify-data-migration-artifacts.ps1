param(
    [switch] $SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $repoRoot "scripts\android\android-env.ps1")

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat assembleLegacyMigrationRelease assembleProductionRelease --no-daemon --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Migration artifact build failed with exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

$bridgeApk = Join-Path $repoRoot "app\build\outputs\apk\legacyMigration\release\app-legacyMigration-release.apk"
$productionApk = Join-Path $repoRoot "app\build\outputs\apk\production\release\app-production-release-unsigned.apk"
foreach ($artifact in @($bridgeApk, $productionApk)) {
    if (-not (Test-Path -LiteralPath $artifact -PathType Leaf)) { throw "Missing migration artifact: $artifact" }
}

$buildTools = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_SDK_ROOT "build-tools") -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
if (-not $buildTools) { throw "Android build-tools are not installed." }
$windows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
$apksigner = Join-Path $buildTools.FullName $(if ($windows) { "apksigner.bat" } else { "apksigner" })
$aapt2 = Join-Path $buildTools.FullName $(if ($windows) { "aapt2.exe" } else { "aapt2" })

$signature = & $apksigner verify --verbose --print-certs $bridgeApk 2>&1
if ($LASTEXITCODE -ne 0) { throw "Legacy migration bridge signature verification failed.`n$($signature -join "`n")" }
$bridgeDigestMatch = [regex]::Match(($signature -join "`n"), "certificate SHA-256 digest:\s*([0-9a-fA-F]+)")
if (-not $bridgeDigestMatch.Success) { throw "Could not read bridge signing certificate digest." }
$bridgeDigest = $bridgeDigestMatch.Groups[1].Value.ToLowerInvariant()

$keytool = Join-Path $env:JAVA_HOME $(if ($windows) { "bin\keytool.exe" } else { "bin/keytool" })
$keyDetails = & $keytool -list -v `
    -keystore (Join-Path $repoRoot "app\keystore\musfit.debug.keystore") `
    -storepass android -alias androiddebugkey 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not inspect the legacy public development key." }
$keyDigestMatch = [regex]::Match(($keyDetails -join "`n"), "SHA256:\s*([0-9A-Fa-f:]+)")
if (-not $keyDigestMatch.Success) { throw "Could not read the legacy key certificate digest." }
$expectedDigest = $keyDigestMatch.Groups[1].Value.Replace(":", "").ToLowerInvariant()
if ($bridgeDigest -cne $expectedDigest) { throw "Bridge is not signed by the legacy public development certificate." }

function Assert-Badging([string] $Path, [string] $Label) {
    $badging = & $aapt2 dump badging $Path 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Could not inspect $Label APK.`n$($badging -join "`n")" }
    $text = $badging -join "`n"
    if ($text -notmatch "package:\s+name='com\.musfit'") { throw "$Label must use com.musfit." }
    if ($text -match "application-debuggable") { throw "$Label must be non-debuggable." }
    if ($text -match "android\.permission\.ACCESS_LOCAL_NETWORK") { throw "$Label must not request local-network access." }
}

function Assert-MergedLauncher([string] $Variant, [string] $ExpectedLauncher) {
    $taskVariant = $Variant.Substring(0, 1).ToUpperInvariant() + $Variant.Substring(1)
    $manifestPath = Join-Path $repoRoot "app\build\intermediates\merged_manifest\$Variant\process${taskVariant}MainManifest\AndroidManifest.xml"
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) { throw "Missing merged manifest: $manifestPath" }
    [xml] $manifest = Get-Content -LiteralPath $manifestPath -Raw
    $android = "http://schemas.android.com/apk/res/android"
    $namespaces = New-Object System.Xml.XmlNamespaceManager($manifest.NameTable)
    $namespaces.AddNamespace("android", $android)
    $enabledLaunchers = @($manifest.SelectNodes(
        '//activity-alias[@android:enabled="true"][intent-filter/action[@android:name="android.intent.action.MAIN"]]',
        $namespaces
    ))
    if ($enabledLaunchers.Count -ne 1) { throw "$Variant must have exactly one enabled MAIN launcher alias." }
    if ($enabledLaunchers[0].GetAttribute("name", $android) -cne $ExpectedLauncher) {
        throw "$Variant launcher must be $ExpectedLauncher."
    }
}

Assert-Badging $bridgeApk "Legacy migration bridge"
Assert-Badging $productionApk "Production-shaped app"
Assert-MergedLauncher "legacyMigrationRelease" "com.musfit.LegacyMigrationLauncher"
Assert-MergedLauncher "productionRelease" "com.musfit.MainLauncher"

$bridgeHash = (Get-FileHash -LiteralPath $bridgeApk -Algorithm SHA256).Hash.ToLowerInvariant()
$productionHash = (Get-FileHash -LiteralPath $productionApk -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "Migration artifact verification passed."
Write-Host "Bridge certificate SHA-256: $bridgeDigest"
Write-Host "Bridge APK SHA-256: $bridgeHash"
Write-Host "Production APK SHA-256: $productionHash"
