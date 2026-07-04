param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function Assert-FileExists([string] $RelativePath) {
    $path = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Expected file to exist: $RelativePath"
    }
}

function Assert-FileContains([string] $RelativePath, [string] $Pattern) {
    $path = Join-Path $repoRoot $RelativePath
    Assert-FileExists $RelativePath
    $text = Get-Content -LiteralPath $path -Raw
    if ($text -notmatch $Pattern) {
        throw "Expected $RelativePath to contain pattern: $Pattern"
    }
}

function Assert-FileDoesNotContain([string] $RelativePath, [string] $Pattern) {
    $path = Join-Path $repoRoot $RelativePath
    Assert-FileExists $RelativePath
    $text = Get-Content -LiteralPath $path -Raw
    if ($text -match $Pattern) {
        throw "Expected $RelativePath not to contain pattern: $Pattern"
    }
}

$liveDocs = @(
    "AGENTS.md",
    "CLAUDE.md",
    "README.md",
    "docs\architecture\README.md"
)

foreach ($doc in $liveDocs) {
    Assert-FileContains $doc "scripts\\android\\android-env\.ps1"
    Assert-FileDoesNotContain $doc "\\.superpowers\\sdd\\android-env\.ps1"
}

Assert-FileContains "README.md" "schema v30"
Assert-FileContains "CLAUDE.md" "version 30"
Assert-FileContains "docs\architecture\README.md" "schema version 30"
Assert-FileContains "AGENTS.md" "Tier 1b.*DONE"

Assert-FileContains "scripts\android\android-env.ps1" "JAVA_HOME"
Assert-FileContains "scripts\android\install-seed-musfit.ps1" "EvidenceDir"
Assert-FileContains "scripts\android\install-seed-musfit.ps1" "Assert-LastExitCode"
Assert-FileContains "scripts\android\install-seed-musfit.ps1" "pm clear"
Assert-FileContains "scripts\android\install-seed-musfit.ps1" "Wait-ForUiDump"

Assert-FileContains "scripts\dev\clean-generated.ps1" "Remove-Item"
Assert-FileContains "scripts\dev\verify-musfit.ps1" "RetryOnGeneratedOutputIssue"
Assert-FileContains "scripts\dev\new-task-branch.ps1" "origin/master"
Assert-FileContains "scripts\dev\new-task-branch.ps1" "DryRun"

Assert-FileContains ".github\workflows\android.yml" "concurrency:"
Assert-FileContains ".github\workflows\android.yml" "permissions:"
Assert-FileContains ".github\workflows\android.yml" "testDebugUnitTest lintDebug assembleDebug"
Assert-FileContains ".github\pull_request_template.md" "Verification"
Assert-FileContains ".gitignore" "verification/"

Write-Host "Development workflow checks passed."
