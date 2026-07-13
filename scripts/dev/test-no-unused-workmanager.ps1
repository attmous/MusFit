param(
    [switch] $RequireReleaseArtifact
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function Assert-NoMatch(
    [string] $Label,
    [string] $Content,
    [string] $Pattern
) {
    if ($Content -match $Pattern) {
        throw "$Label still contains unused WorkManager/Hilt Work surface matching: $Pattern"
    }
}

$catalog = Get-Content -LiteralPath (Join-Path $repoRoot "gradle\libs.versions.toml") -Raw
$appBuild = Get-Content -LiteralPath (Join-Path $repoRoot "app\build.gradle.kts") -Raw

Assert-NoMatch "Version catalog" $catalog '(?m)^work\s*='
Assert-NoMatch "Version catalog" $catalog 'androidx\.work:'
Assert-NoMatch "Version catalog" $catalog 'androidx\.hilt:hilt-work'
Assert-NoMatch "Version catalog" $catalog 'androidx\.hilt:hilt-compiler'
Assert-NoMatch "App build" $appBuild 'libs\.androidx\.work'
Assert-NoMatch "App build" $appBuild 'libs\.androidx\.hilt\.work'
Assert-NoMatch "App build" $appBuild 'libs\.androidx\.hilt\.compiler'

$runtimeSource = Get-ChildItem -LiteralPath (Join-Path $repoRoot "app\src") -Recurse -File |
    Where-Object { $_.Extension -in @(".kt", ".java", ".xml") } |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw } |
    Out-String
Assert-NoMatch "App source" $runtimeSource 'androidx\.work\.|WorkManager|HiltWorker|CoroutineWorker|ListenableWorker'

if ($RequireReleaseArtifact) {
    $apk = Get-ChildItem -LiteralPath (Join-Path $repoRoot "app\build\outputs\apk\production\release") -Filter "app-production-release*.apk" -File |
        Select-Object -First 1
    if (-not $apk) {
        throw "Production release APK is required but was not found."
    }

    $apkanalyzer = if ($env:ANDROID_SDK_ROOT) {
        Join-Path $env:ANDROID_SDK_ROOT $(if ($IsWindows -or $env:OS -eq "Windows_NT") {
            "cmdline-tools\latest\bin\apkanalyzer.bat"
        } else {
            "cmdline-tools/latest/bin/apkanalyzer"
        })
    } else {
        "apkanalyzer"
    }

    $manifest = & $apkanalyzer manifest print $apk.FullName 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "apkanalyzer could not inspect the production manifest."
    }
    Assert-NoMatch "Production manifest" $manifest 'androidx\.work\.|WorkManagerInitializer'

    $packages = & $apkanalyzer dex packages $apk.FullName 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) {
        throw "apkanalyzer could not inspect production DEX packages."
    }
    Assert-NoMatch "Production DEX" $packages '(?m)^\s*[PCMF]\s+d\s+\d+\s+\d+\s+androidx\.work(?:\.|$)'
}

Write-Host "Unused WorkManager/Hilt Work checks passed."
