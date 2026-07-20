param(
    [string] $SourceJar
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$upstreamVersion = "1.7.8"

if ([string]::IsNullOrWhiteSpace($SourceJar)) {
    $userProfile = [Environment]::GetFolderPath([Environment+SpecialFolder]::UserProfile)
    $gradleCache = Join-Path $userProfile ".gradle/caches/modules-2/files-2.1/androidx.compose.material/material-icons-extended-android/$upstreamVersion"
    $candidates = @(Get-ChildItem -LiteralPath $gradleCache -Recurse -Filter "material-icons-extended-android-*-sources.jar" -File)
    if ($candidates.Count -ne 1) {
        throw "Expected exactly one cached material-icons-extended source jar for $upstreamVersion; pass -SourceJar explicitly."
    }
    $SourceJar = $candidates[0].FullName
}
$sourceJarPath = (Resolve-Path -LiteralPath $SourceJar).Path

$archiveEntries = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
& jar tf $sourceJarPath | ForEach-Object { [void] $archiveEntries.Add($_) }
if ($LASTEXITCODE -ne 0) { throw "Could not list the upstream material icon source jar." }

$importPattern = '^import (?:androidx\.compose\.material\.icons|com\.musfit\.ui\.icons)\.(?<auto>automirrored\.)?(?<style>filled|outlined|rounded|sharp|twotone)\.(?<name>[A-Za-z0-9_]+)$'
$registered = @{}
@("app", "core", "feature", "integration") |
    ForEach-Object { Get-ChildItem -LiteralPath (Join-Path $repoRoot $_) -Recurse -Filter "*.kt" -File } |
    ForEach-Object {
        foreach ($line in Get-Content -LiteralPath $_.FullName) {
            if ($line -notmatch $importPattern) { continue }
            $style = if ($Matches.auto) { "automirrored/$($Matches.style)" } else { $Matches.style }
            $entry = "commonMain/androidx/compose/material/icons/$style/$($Matches.name).kt"
            if ($archiveEntries.Contains($entry)) {
                $registered["$style/$($Matches.name)"] = [ordered]@{ style = $style; name = $Matches.name }
            }
        }
    }

$icons = @($registered.GetEnumerator() | Sort-Object Name | ForEach-Object Value)
if ($icons.Count -eq 0) { throw "No used extended material icons were discovered." }

$manifest = [ordered]@{
    schemaVersion = 1
    upstream = [ordered]@{
        module = "androidx.compose.material:material-icons-extended-android"
        version = $upstreamVersion
        license = "Apache-2.0"
    }
    icons = $icons
}
$manifestPath = Join-Path $repoRoot "config/material-icon-subset.json"
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding utf8

$targetRoot = Join-Path $repoRoot "core/designsystem/src/main/kotlin/com/musfit/ui/icons/generated"
[IO.Directory]::CreateDirectory($targetRoot) | Out-Null
Get-ChildItem -LiteralPath $targetRoot -Recurse -Filter "*.kt" -File -ErrorAction SilentlyContinue |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("musfit-material-icons-" + [guid]::NewGuid().ToString("N"))
[IO.Directory]::CreateDirectory($tempRoot) | Out-Null
try {
    Push-Location $tempRoot
    try {
        foreach ($icon in $icons) {
            $entry = "commonMain/androidx/compose/material/icons/$($icon.style)/$($icon.name).kt"
            & jar xf $sourceJarPath $entry
            if ($LASTEXITCODE -ne 0) { throw "Could not extract upstream icon source: $entry" }
        }
    } finally {
        Pop-Location
    }

    foreach ($icon in $icons) {
        $stylePath = ([string] $icon.style).Replace("/", [IO.Path]::DirectorySeparatorChar)
        $stylePackage = ([string] $icon.style).Replace("/", ".")
        $entryPath = Join-Path $tempRoot "commonMain/androidx/compose/material/icons/$stylePath/$($icon.name).kt"
        $targetDirectory = Join-Path $targetRoot $stylePath
        [IO.Directory]::CreateDirectory($targetDirectory) | Out-Null
        $target = Join-Path $targetDirectory "$($icon.name).kt"
        $text = [IO.File]::ReadAllText($entryPath, [Text.UTF8Encoding]::new($false, $true))
        $text = $text.Replace(
            "package androidx.compose.material.icons.$stylePackage",
            "package com.musfit.ui.icons.$stylePackage"
        )
        Set-Content -LiteralPath $target -Value $text -Encoding utf8 -NoNewline
    }

    @("app", "core", "feature", "integration") |
        ForEach-Object { Get-ChildItem -LiteralPath (Join-Path $repoRoot $_) -Recurse -Filter "*.kt" -File } |
        ForEach-Object {
            $text = [IO.File]::ReadAllText($_.FullName, [Text.UTF8Encoding]::new($false, $true))
            $updated = $text
            foreach ($icon in $icons) {
                $stylePackage = ([string] $icon.style).Replace("/", ".")
                $updated = $updated.Replace(
                    "import androidx.compose.material.icons.$stylePackage.$($icon.name)",
                    "import com.musfit.ui.icons.$stylePackage.$($icon.name)"
                )
            }
            if ($updated -cne $text) {
                [IO.File]::WriteAllText($_.FullName, $updated, [Text.UTF8Encoding]::new($false))
            }
        }
} finally {
    $resolvedTemp = [IO.Path]::GetFullPath($tempRoot)
    $expectedTempPrefix = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if (-not $resolvedTemp.StartsWith($expectedTempPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a generated icon directory outside the temp root: $resolvedTemp"
    }
    Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
}

Write-Host "Updated $($icons.Count) maintained Material icon vectors from $sourceJarPath."
