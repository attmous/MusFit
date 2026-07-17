$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$catalog = Get-Content -LiteralPath (Join-Path $repoRoot "gradle\libs.versions.toml") -Raw
$rootBuild = Get-Content -LiteralPath (Join-Path $repoRoot "build.gradle.kts") -Raw
$appBuild = Get-Content -LiteralPath (Join-Path $repoRoot "app\build.gradle.kts") -Raw
$databaseBuild = Get-Content -LiteralPath (Join-Path $repoRoot "core\database\build.gradle.kts") -Raw

function Assert-Match([string] $Label, [string] $Content, [string] $Pattern) {
    if ($Content -notmatch $Pattern) {
        throw "$Label is missing required KSP contract: $Pattern"
    }
}

function Assert-NoMatch([string] $Label, [string] $Content, [string] $Pattern) {
    if ($Content -match $Pattern) {
        throw "$Label still contains legacy kapt surface matching: $Pattern"
    }
}

Assert-Match "Version catalog" $catalog '(?m)^ksp\s*=\s*"2\.3\.10"\s*$'
Assert-Match "Version catalog" $catalog 'id\s*=\s*"com\.google\.devtools\.ksp"'
Assert-NoMatch "Version catalog" $catalog 'com\.android\.legacy-kapt'
Assert-Match "Root build" $rootBuild 'alias\(libs\.plugins\.ksp\)\s+apply false'
Assert-NoMatch "Root build" $rootBuild 'android\.legacy\.kapt'
Assert-Match "App build" $appBuild 'alias\(libs\.plugins\.ksp\)'
Assert-Match "App build" $appBuild 'ksp\s*\(libs\.hilt\.compiler\)'
Assert-Match "Database build" $databaseBuild 'ksp\s*\(libs\.androidx\.room\.compiler\)'
Assert-Match "Database build" $databaseBuild 'arg\("room\.schemaLocation",\s*rootProject\.file\("app/schemas"\)\.path\)'
Assert-NoMatch "App build" $appBuild '(?m)^\s*kapt\s*\{'
Assert-NoMatch "App build" $appBuild 'kapt\s*\('
Assert-NoMatch "App build" $appBuild 'android\.legacy\.kapt'

. (Join-Path $repoRoot "scripts\android\android-env.ps1")
$gradleWrapper = Join-Path $repoRoot $(if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) { "gradlew.bat" } else { "gradlew" })
$taskOutput = & $gradleWrapper :app:tasks --all --no-daemon --console=plain 2>&1
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    $taskOutput | ForEach-Object { Write-Host $_ }
    throw "Could not inspect the app task graph (exit $exitCode)."
}
$tasks = $taskOutput -join "`n"
Assert-Match "App task graph" $tasks '(?m)^kspInternalDebugKotlin\b'
Assert-NoMatch "App task graph" $tasks '(?m)^kapt(?:GenerateStubs)?\w*'

$databaseTaskOutput = & $gradleWrapper :core:database:tasks --all --no-daemon --console=plain 2>&1
if ($LASTEXITCODE -ne 0) {
    $databaseTaskOutput | ForEach-Object { Write-Host $_ }
    throw "Could not inspect the database task graph (exit $LASTEXITCODE)."
}
$databaseTasks = $databaseTaskOutput -join "`n"
Assert-Match "Database task graph" $databaseTasks '(?m)^kspDebugKotlin\b'
Assert-NoMatch "Database task graph" $databaseTasks '(?m)^kapt(?:GenerateStubs)?\w*'

Write-Host "KSP migration checks passed with no kapt tasks."
