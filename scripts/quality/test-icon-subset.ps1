param(
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function Get-RepoPath([string] $RelativePath) {
    Join-Path $repoRoot ($RelativePath.Replace("/", [IO.Path]::DirectorySeparatorChar))
}

function Get-SourceText {
    $roots = @("app", "core", "feature", "integration")
    @(
        $roots |
            ForEach-Object { Get-ChildItem -LiteralPath (Get-RepoPath $_) -Recurse -Filter "*.kt" -File } |
            ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }
    ) -join "`n"
}

function Assert-UniqueIconKeys([string[]] $Keys) {
    $duplicates = @($Keys | Group-Object | Where-Object Count -ne 1)
    if ($duplicates.Count -ne 0) {
        throw "Material icon subset contains duplicate style/name entries."
    }
}

$catalog = Get-Content -LiteralPath (Get-RepoPath "gradle/libs.versions.toml") -Raw
if ($catalog -match 'material-icons-extended') {
    throw "The version catalog still owns material-icons-extended."
}

$buildText = @(
    Get-ChildItem -LiteralPath $repoRoot -Recurse -Filter "*.gradle.kts" -File |
        Where-Object { $_.FullName -notmatch '[\\/]build[\\/]' } |
        ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw }
) -join "`n"
if ($buildText -match 'material\.icons\.extended|material-icons-extended') {
    throw "A Gradle build still depends on material-icons-extended."
}
$verificationMetadata = Get-Content -LiteralPath (Get-RepoPath "gradle/verification-metadata.xml") -Raw
if ($verificationMetadata -match '<component[^>]+name="material-icons-extended(?:-android|-desktop)?"') {
    throw "Dependency verification still owns material-icons-extended artifacts."
}

$manifestPath = Get-RepoPath "config/material-icon-subset.json"
if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Missing maintained icon manifest: $manifestPath"
}
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
if ([int] $manifest.schemaVersion -ne 1) { throw "Unsupported material icon subset schemaVersion." }
if ([string] $manifest.upstream.module -cne "androidx.compose.material:material-icons-extended-android") {
    throw "Material icon subset must identify its upstream module."
}

$icons = @($manifest.icons)
if ($icons.Count -eq 0) { throw "Material icon subset must register at least one vector." }
$keys = @($icons | ForEach-Object { "$($_.style)/$($_.name)" })
Assert-UniqueIconKeys $keys

$sourceRoot = Get-RepoPath "core/designsystem/src/main/kotlin/com/musfit/ui/icons/generated"
$sourceFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter "*.kt" -File)
if ($sourceFiles.Count -ne $icons.Count) {
    throw "Maintained icon source count mismatch: expected $($icons.Count), found $($sourceFiles.Count)."
}

$allSource = Get-SourceText
foreach ($icon in $icons) {
    $stylePath = ([string] $icon.style).Replace("/", [IO.Path]::DirectorySeparatorChar)
    $stylePackage = ([string] $icon.style).Replace("/", ".")
    $sourcePath = Join-Path (Join-Path $sourceRoot $stylePath) "$($icon.name).kt"
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Missing maintained vector source: $sourcePath"
    }
    $source = Get-Content -LiteralPath $sourcePath -Raw
    $package = "package com.musfit.ui.icons.$stylePackage"
    if (-not $source.Contains($package)) { throw "Maintained vector has the wrong package: $sourcePath" }
    if ($source -notmatch 'Copyright 2025 The Android Open Source Project' -or
        $source -notmatch 'Licensed under the Apache License, Version 2\.0') {
        throw "Maintained vector must retain its upstream Apache license header: $sourcePath"
    }

    $oldImport = "import androidx.compose.material.icons.$stylePackage.$($icon.name)"
    if ($allSource.Contains($oldImport)) { throw "Extended icon import remains: $oldImport" }
    $newImport = "import com.musfit.ui.icons.$stylePackage.$($icon.name)"
    if (-not $allSource.Contains($newImport)) { throw "Registered icon is not imported: $newImport" }
}

if ($SelfTest) {
    $duplicateRejected = $false
    try { Assert-UniqueIconKeys @("filled/Add", "filled/Add") } catch { $duplicateRejected = $true }
    if (-not $duplicateRejected) { throw "Icon subset duplicate self-test failed." }
    Write-Host "Material icon subset self-test passed."
}

Write-Host "Material icon subset policy passed ($($icons.Count) maintained vectors)."
