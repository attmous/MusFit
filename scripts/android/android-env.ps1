$ErrorActionPreference = "Stop"

function Test-Java17Home([string] $Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }

    $java = Join-Path $Path "bin\java.exe"
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        return $false
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $versionOutput = & $java -version 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    return ($exitCode -eq 0 -and (($versionOutput -join "`n") -match 'version "17\.'))
}

$jdkCandidates = @(
    $env:JAVA_HOME,
    $env:MUSFIT_JAVA_HOME
)
if ($env:LOCALAPPDATA) {
    $jdkCandidates += Join-Path $env:LOCALAPPDATA "MusFitToolchain\jdk-17"
}
$jdkCandidates = $jdkCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

$programFilesCandidates = @()
if ($env:ProgramFiles) {
    $programFilesCandidates += Get-ChildItem -LiteralPath $env:ProgramFiles -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match "^(Eclipse Adoptium|Java)$" } |
        ForEach-Object {
            Get-ChildItem -LiteralPath $_.FullName -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match "jdk-17" } |
                Select-Object -ExpandProperty FullName
        }
}

$jdkCandidates += $programFilesCandidates
$jdk = $jdkCandidates | Where-Object { Test-Java17Home $_ } | Select-Object -First 1

if ($jdk) {
    $jdk = (Resolve-Path -LiteralPath $jdk).Path
    $env:JAVA_HOME = $jdk
    $javaBin = Join-Path $jdk "bin"
    $currentPath = $env:PATH -split [System.IO.Path]::PathSeparator
    if ($currentPath -notcontains $javaBin) {
        $env:PATH = "$javaBin$([System.IO.Path]::PathSeparator)$env:PATH"
    }
} else {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $versionOutput = & java -version 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0 -or (($versionOutput -join "`n") -notmatch 'version "17\.')) {
        throw "JDK 17 not found. Set JAVA_HOME or MUSFIT_JAVA_HOME to a JDK 17 installation."
    }
}

$sdkCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT
)
if ($env:LOCALAPPDATA) {
    $sdkCandidates += Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
$sdkCandidates = $sdkCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

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
Write-Host "Java home: $env:JAVA_HOME"
