param([switch] $SelfTest)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$previewPattern = '(?i)(?:^|[._-])(?:alpha|beta|rc|preview|snapshot|dev|eap|m\d+)\d*(?:$|[._+-])'

function Get-Section([string] $Text, [string] $Name) {
    $match = [regex]::Match($Text, "(?ms)^\[$([regex]::Escape($Name))\]\s*\r?\n(?<body>.*?)(?=^\[|\z)")
    if (-not $match.Success) { throw "Missing [$Name] section in version catalog." }
    $match.Groups['body'].Value
}

function Get-CatalogEntries([string] $Text, [string] $Section) {
    $body = Get-Section $Text $Section
    @([regex]::Matches($body, '(?m)^(?<key>[A-Za-z0-9_.-]+)\s*=\s*(?<value>.+?)\s*$') | ForEach-Object {
        [pscustomobject]@{ Key = $_.Groups['key'].Value; Value = $_.Groups['value'].Value }
    })
}

function Get-CatalogVersions([string] $Text) {
    @((Get-CatalogEntries $Text 'versions') | ForEach-Object {
        $valueMatch = [regex]::Match($_.Value, '^"(?<version>[^"]+)"$')
        if (-not $valueMatch.Success) { throw "Version '$($_.Key)' must be an exact quoted value." }
        [pscustomobject]@{ Key = $_.Key; Version = $valueMatch.Groups['version'].Value }
    })
}

function Assert-PreviewRegistry([string] $CatalogText, [object] $Registry, [datetime] $Today) {
    if ([int] $Registry.schemaVersion -ne 1) { throw "Unsupported preview exception schema." }
    $versions = @(Get-CatalogVersions $CatalogText)
    $previews = @($versions | Where-Object { $_.Version -match $previewPattern })
    $exceptions = @($Registry.exceptions)

    $duplicateKeys = @($exceptions | Group-Object versionKey | Where-Object Count -gt 1)
    if ($duplicateKeys.Count -gt 0) { throw "Duplicate preview exception: $($duplicateKeys[0].Name)" }

    foreach ($preview in $previews) {
        $exception = @($exceptions | Where-Object versionKey -CEQ $preview.Key)
        if ($exception.Count -ne 1) {
            throw "Unregistered preview version: $($preview.Key)=$($preview.Version)"
        }
        if ($exception[0].version -cne $preview.Version) {
            throw "Preview exception version mismatch for $($preview.Key)."
        }
    }

    foreach ($exception in $exceptions) {
        $catalogVersion = @($versions | Where-Object Key -CEQ $exception.versionKey)
        if ($catalogVersion.Count -ne 1 -or $catalogVersion[0].Version -notmatch $previewPattern) {
            throw "Stale preview exception: $($exception.versionKey)"
        }
        if ([string]::IsNullOrWhiteSpace($exception.owner) -or $exception.owner -notmatch '^@') {
            throw "Preview exception owner must be an accountable handle: $($exception.versionKey)"
        }
        if (@($exception.coordinates).Count -eq 0) {
            throw "Preview exception must list coordinates: $($exception.versionKey)"
        }
        if ([string]::IsNullOrWhiteSpace($exception.rationale) -or $exception.rationale.Length -lt 80) {
            throw "Preview exception rationale is incomplete: $($exception.versionKey)"
        }
        if ([string]::IsNullOrWhiteSpace($exception.exitCriteria) -or $exception.exitCriteria.Length -lt 60) {
            throw "Preview exception exit criteria are incomplete: $($exception.versionKey)"
        }
        $expiry = [datetime]::ParseExact($exception.expiresOn, 'yyyy-MM-dd', [Globalization.CultureInfo]::InvariantCulture)
        if ($expiry.Date -lt $Today.Date) {
            throw "Preview exception expired: $($exception.versionKey) on $($exception.expiresOn)"
        }
        if ($expiry.Date -gt $Today.Date.AddDays(180)) {
            throw "Preview exception expiry is too distant: $($exception.versionKey)"
        }
    }
}

function Assert-CatalogClosure([string] $CatalogText) {
    $gradleText = Get-ChildItem -LiteralPath $repoRoot -Recurse -Filter '*.gradle.kts' -File |
        Where-Object FullName -NotMatch '[\\/]build[\\/]' |
        ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw } |
        Out-String

    foreach ($entry in Get-CatalogEntries $CatalogText 'libraries') {
        $accessor = 'libs.' + ($entry.Key -replace '[-_.]+', '.')
        if ($gradleText -notmatch [regex]::Escape($accessor)) {
            throw "Unused version-catalog library alias: $($entry.Key)"
        }
    }
    foreach ($entry in Get-CatalogEntries $CatalogText 'plugins') {
        $accessor = 'libs.plugins.' + ($entry.Key -replace '[-_.]+', '.')
        if ($gradleText -notmatch [regex]::Escape($accessor)) {
            throw "Unused version-catalog plugin alias: $($entry.Key)"
        }
    }
}

function Assert-UpdateAutomation([string] $DependabotText) {
    foreach ($pattern in @(
        'package-ecosystem:\s*"gradle"',
        'target-branch:\s*"master"',
        'dependency-type:\s*"direct"',
        'version-update:semver-major',
        'dependency-name:\s*"androidx\.credentials:\*"',
        'dependency-name:\s*"androidx\.benchmark:\*"',
        'dependency-name:\s*"androidx\.baselineprofile"',
        '(?m)^\s{6}android-toolchain:',
        '(?m)^\s{6}app-runtime:',
        '(?m)^\s{6}test-tooling:'
    )) {
        if ($DependabotText -notmatch $pattern) { throw "Dependabot policy is missing: $pattern" }
    }

    $androidWorkflow = Get-Content -LiteralPath (Join-Path $repoRoot '.github\workflows\android.yml') -Raw
    foreach ($pattern in @('(?m)^\s*pull_request:', 'testProductionReleaseUnitTest', 'lintProductionRelease', 'bundleProductionRelease')) {
        if ($androidWorkflow -notmatch $pattern) { throw "Dependency PR release gate is missing: $pattern" }
    }
    $performanceWorkflow = Get-Content -LiteralPath (Join-Path $repoRoot '.github\workflows\performance.yml') -Raw
    if ($performanceWorkflow -notmatch 'gradle/libs\.versions\.toml') {
        throw "Dependency catalog PRs must trigger the performance guard."
    }
}

$catalogPath = Join-Path $repoRoot 'gradle\libs.versions.toml'
$registryPath = Join-Path $repoRoot 'config\dependency-preview-exceptions.json'
$dependabotPath = Join-Path $repoRoot '.github\dependabot.yml'
$catalog = Get-Content -LiteralPath $catalogPath -Raw
$registry = Get-Content -LiteralPath $registryPath -Raw | ConvertFrom-Json
$dependabot = Get-Content -LiteralPath $dependabotPath -Raw

Assert-PreviewRegistry $catalog $registry ([datetime]::UtcNow)
Assert-CatalogClosure $catalog
Assert-UpdateAutomation $dependabot

if ($SelfTest) {
    $rogueCatalog = $catalog -replace '(?m)^\[versions\]\s*$', "[versions]`nroguePreview = `"1.0.0-beta01`""
    $rogueRejected = $false
    try { Assert-PreviewRegistry $rogueCatalog $registry ([datetime]::UtcNow) } catch {
        if ($_.Exception.Message -notmatch 'Unregistered preview version: roguePreview') { throw }
        $rogueRejected = $true
    }
    if (-not $rogueRejected) { throw "Unregistered preview self-test did not fail." }

    $expiredRegistry = $registry | ConvertTo-Json -Depth 8 | ConvertFrom-Json
    $expiredRegistry.exceptions[0].expiresOn = ([datetime]::UtcNow.Date.AddDays(-1)).ToString('yyyy-MM-dd')
    $expiryRejected = $false
    try { Assert-PreviewRegistry $catalog $expiredRegistry ([datetime]::UtcNow) } catch {
        if ($_.Exception.Message -notmatch 'Preview exception expired') { throw }
        $expiryRejected = $true
    }
    if (-not $expiryRejected) { throw "Expired preview self-test did not fail." }
}

$previewCount = @((Get-CatalogVersions $catalog) | Where-Object Version -Match $previewPattern).Count
$libraryCount = @(Get-CatalogEntries $catalog 'libraries').Count
$pluginCount = @(Get-CatalogEntries $catalog 'plugins').Count
Write-Host "Dependency governance checks passed ($previewCount registered previews; $libraryCount library aliases; $pluginCount plugin aliases)."
