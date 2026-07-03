param(
    [string] $RelativePath = "app\build",
    [switch] $SkipGradleStop,
    [switch] $WhatIf
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$targetPath = Join-Path $repoRoot $RelativePath
$target = Resolve-Path -LiteralPath $targetPath -ErrorAction SilentlyContinue

if (-not $target) {
    Write-Host "Generated output does not exist: $RelativePath"
    exit 0
}

$resolvedTarget = $target.Path
if (-not $resolvedTarget.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to remove outside workspace: $resolvedTarget"
}

if (-not $SkipGradleStop) {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat --stop
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle stop failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
    Start-Sleep -Seconds 3
}

if ($WhatIf) {
    Write-Host "Would remove generated output: $resolvedTarget"
    exit 0
}

Remove-Item -LiteralPath $resolvedTarget -Recurse -Force -ErrorAction Stop
Write-Host "Removed generated output: $resolvedTarget"
