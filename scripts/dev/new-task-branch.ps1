param(
    [Parameter(Mandatory = $true)]
    [string] $TaskName,
    [string] $BaseRef = "origin/master",
    [switch] $AllowDirty,
    [switch] $DryRun
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function ConvertTo-BranchSlug([string] $Value) {
    $slug = $Value.ToLowerInvariant() -replace "[^a-z0-9]+", "-"
    $slug = $slug.Trim("-")
    if ([string]::IsNullOrWhiteSpace($slug)) {
        throw "TaskName must contain at least one letter or digit."
    }
    return $slug
}

Push-Location $repoRoot
try {
    $dirty = & git status --porcelain
    if ($dirty -and -not $AllowDirty) {
        throw "Working tree has uncommitted changes. Commit/stash them or rerun with -AllowDirty."
    }

    & git fetch origin
    if ($LASTEXITCODE -ne 0) {
        throw "git fetch origin failed with exit code $LASTEXITCODE"
    }

    $branch = "codex/$(ConvertTo-BranchSlug $TaskName)"
    if ($DryRun) {
        Write-Host "Would create branch $branch from $BaseRef"
        return
    }

    & git switch -c $branch $BaseRef
    if ($LASTEXITCODE -ne 0) {
        throw "git switch failed with exit code $LASTEXITCODE"
    }

    Write-Host "Created branch $branch from $BaseRef"
} finally {
    Pop-Location
}
