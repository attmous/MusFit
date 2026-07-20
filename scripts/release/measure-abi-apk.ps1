param(
    [ValidateSet("arm64-v8a", "armeabi-v7a", "x86", "x86_64")]
    [string] $Abi = "arm64-v8a",
    [string] $OutputDirectory = "verification/size-distribution",
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
$maxArm64Bytes = 60MB

function Get-ProbeFileName([string] $TargetAbi) {
    "musfit-$TargetAbi-unsigned-size-probe.apk"
}

if ($SelfTest) {
    if ((Get-ProbeFileName "arm64-v8a") -cne "musfit-arm64-v8a-unsigned-size-probe.apk") {
        throw "ABI probe filename self-test failed."
    }
    if ($maxArm64Bytes -ne 62914560) { throw "Arm64 size budget self-test failed." }
    Write-Host "ABI APK measurement self-test passed."
    return
}

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) {
    . (Join-Path $repoRoot "scripts\android\android-env.ps1")
}
if (-not $env:ANDROID_SDK_ROOT -or -not $env:JAVA_HOME) {
    throw "Android SDK and JAVA_HOME are required."
}

$gradle = Join-Path $repoRoot $(if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) { "gradlew.bat" } else { "gradlew" })
& $gradle :app:clean :app:assembleProductionRelease "-Pandroid.injected.build.abi=$Abi" --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Production $Abi APK build failed." }

$intermediate = Join-Path $repoRoot "app\build\intermediates\apk\production\release\app-production-release-unsigned.apk"
if (-not (Test-Path -LiteralPath $intermediate -PathType Leaf)) {
    throw "AGP did not produce the expected $Abi APK size probe."
}

$buildTools = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_SDK_ROOT "build-tools") -Directory |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $buildTools) { throw "Android build-tools are not installed." }
$aapt2 = Join-Path $buildTools.FullName $(if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) { "aapt2.exe" } else { "aapt2" })
$badging = & $aapt2 dump badging $intermediate 2>&1
if ($LASTEXITCODE -ne 0) { throw "Could not inspect the $Abi APK size probe." }
$nativeCode = [regex]::Match(($badging -join "`n"), "native-code:\s+(.+)")
if (-not $nativeCode.Success -or $nativeCode.Groups[1].Value.Trim() -cne "'$Abi'") {
    throw "APK size probe does not contain exactly the requested ABI $Abi."
}

$output = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDirectory))
[IO.Directory]::CreateDirectory($output) | Out-Null
$artifact = Join-Path $output (Get-ProbeFileName $Abi)
Copy-Item -LiteralPath $intermediate -Destination $artifact -Force
$bytes = (Get-Item -LiteralPath $artifact).Length
if ($Abi -ceq "arm64-v8a" -and $bytes -gt $maxArm64Bytes) {
    throw "Arm64 APK size probe exceeds 60 MiB: $bytes bytes."
}

$commitSha = (& git -C $repoRoot rev-parse HEAD).Trim()
$metadata = [ordered]@{
    schemaVersion = 1
    purpose = "unsigned-size-probe-not-for-publication"
    commitSha = $commitSha
    abi = $Abi
    apkFile = [IO.Path]::GetFileName($artifact)
    apkBytes = $bytes
    apkSha256 = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash.ToLowerInvariant()
    arm64BudgetBytes = $maxArm64Bytes
}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $output "abi-size-probe.json") -Encoding utf8
Write-Host "Unsigned $Abi size probe: $bytes bytes ($([math]::Round($bytes / 1MB, 2)) MiB)."
Write-Host "This probe is not a release asset and must never be published as Google-signed."
