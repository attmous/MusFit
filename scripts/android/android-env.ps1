$ErrorActionPreference = "Stop"

$sdkCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    (Join-Path $env:LOCALAPPDATA "Android\Sdk")
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

if (-not $sdkCandidates) {
    throw "Android SDK not found. Install Android Studio or set ANDROID_HOME to the SDK path."
}

$sdk = (Resolve-Path -LiteralPath $sdkCandidates[0]).Path
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

$toolPaths = @(
    (Join-Path $sdk "platform-tools"),
    (Join-Path $sdk "emulator"),
    (Join-Path $sdk "cmdline-tools\latest\bin")
) | Where-Object { Test-Path -LiteralPath $_ }

$currentPath = $env:PATH -split [System.IO.Path]::PathSeparator
foreach ($path in $toolPaths) {
    if ($currentPath -notcontains $path) {
        $env:PATH = "$path$([System.IO.Path]::PathSeparator)$env:PATH"
    }
}

Write-Host "Android SDK: $sdk"
