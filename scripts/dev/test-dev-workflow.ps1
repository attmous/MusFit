param(
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path

function Get-RepoPath([string] $RelativePath) {
    Join-Path $repoRoot $RelativePath
}

function Assert-FileExists([string] $RelativePath) {
    $path = Get-RepoPath $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Expected file to exist: $RelativePath"
    }
}

function Get-FileText([string] $RelativePath) {
    Assert-FileExists $RelativePath
    Get-Content -LiteralPath (Get-RepoPath $RelativePath) -Raw
}

function Assert-FileContains([string] $RelativePath, [string] $Pattern) {
    $text = Get-FileText $RelativePath
    if ($text -notmatch $Pattern) {
        throw "Expected $RelativePath to contain pattern: $Pattern"
    }
}

function Assert-FileDoesNotContain([string] $RelativePath, [string] $Pattern) {
    $text = Get-FileText $RelativePath
    if ($text -match $Pattern) {
        throw "Expected $RelativePath not to contain pattern: $Pattern"
    }
}

function Assert-PowerShellParses([string] $RelativePath) {
    Assert-FileExists $RelativePath
    $tokens = $null
    $errors = $null
    [Management.Automation.Language.Parser]::ParseFile(
        (Get-RepoPath $RelativePath),
        [ref] $tokens,
        [ref] $errors
    ) | Out-Null
    if ($errors.Count -gt 0) {
        $messages = @($errors | ForEach-Object { $_.Message }) -join '; '
        throw "PowerShell parse errors in ${RelativePath}: $messages"
    }
}

function Assert-Equal([string] $Label, $Expected, $Actual) {
    if ($Expected -ne $Actual) {
        throw "$Label mismatch. Expected '$Expected', got '$Actual'."
    }
}

function Get-DelimitedBlock(
    [string] $Text,
    [int] $OpenIndex,
    [char] $OpenCharacter,
    [char] $CloseCharacter,
    [string] $Label
) {
    if ($OpenIndex -lt 0 -or $OpenIndex -ge $Text.Length -or $Text[$OpenIndex] -ne $OpenCharacter) {
        throw "Could not find the opening $OpenCharacter for $Label."
    }

    $depth = 0
    for ($index = $OpenIndex; $index -lt $Text.Length; $index++) {
        if ($Text[$index] -eq $OpenCharacter) {
            $depth++
        } elseif ($Text[$index] -eq $CloseCharacter) {
            $depth--
            if ($depth -eq 0) {
                return [pscustomobject]@{
                    Content = $Text.Substring($OpenIndex + 1, $index - $OpenIndex - 1)
                    EndIndex = $index
                }
            }
        }
    }

    throw "Could not find the closing $CloseCharacter for $Label."
}

function Assert-DatabaseSchemaContract([int] $SourceVersion, [int[]] $SchemaVersions) {
    if ($SchemaVersions.Count -eq 0) {
        throw "No exported Room schema files were found."
    }

    $latestSchemaVersion = ($SchemaVersions | Measure-Object -Maximum).Maximum
    Assert-Equal "Room source/exported schema version" $SourceVersion $latestSchemaVersion

    $missingVersions = 1..$SourceVersion | Where-Object { $_ -notin $SchemaVersions }
    if ($missingVersions.Count -gt 0) {
        throw "Exported Room schemas are not contiguous. Missing: $($missingVersions -join ', ')"
    }
}

function Get-DatabaseVersion {
    $databaseText = Get-FileText "app/src/main/java/com/musfit/data/local/MusFitDatabase.kt"
    $match = [regex]::Match($databaseText, '(?m)^\s*version\s*=\s*(\d+)\s*,?\s*$')
    if (-not $match.Success) {
        throw "Could not derive the Room version from MusFitDatabase.kt."
    }
    [int] $match.Groups[1].Value
}

function Get-ExportedSchemaVersions([string] $SchemaDirectory) {
    $schemaFiles = @(
        Get-ChildItem -LiteralPath $SchemaDirectory -Filter "*.json" -File |
            Where-Object { $_.BaseName -match '^\d+$' } |
            Sort-Object { [int] $_.BaseName }
    )

    $versions = foreach ($schemaFile in $schemaFiles) {
        $fileVersion = [int] $schemaFile.BaseName
        try {
            $schema = Get-Content -LiteralPath $schemaFile.FullName -Raw | ConvertFrom-Json
        } catch {
            throw "Could not parse exported Room schema $($schemaFile.Name): $($_.Exception.Message)"
        }

        if ($null -eq $schema.database -or $null -eq $schema.database.version) {
            throw "Exported Room schema $($schemaFile.Name) does not declare database.version."
        }

        $declaredVersion = [int] $schema.database.version
        Assert-Equal "Exported Room schema filename/content version for $($schemaFile.Name)" $fileVersion $declaredVersion
        $fileVersion
    }

    @($versions | Sort-Object -Unique)
}

function Assert-RegisteredSequentialMigrations([int] $SourceVersion) {
    if ($SourceVersion -le 1) {
        return
    }

    $moduleText = Get-FileText "app/src/main/java/com/musfit/core/di/DatabaseModule.kt"
    $callMatch = [regex]::Match($moduleText, '\.addMigrations\s*\(')
    if (-not $callMatch.Success) {
        throw "Could not find the Room addMigrations registration."
    }

    $openIndex = $moduleText.IndexOf('(', $callMatch.Index)
    $registration = Get-DelimitedBlock $moduleText $openIndex '(' ')' "Room addMigrations registration"
    $registrationText = [regex]::Replace($registration.Content, '(?s)/\*.*?\*/', '')
    $registrationText = [regex]::Replace($registrationText, '(?m)//.*$', '')
    $registeredMigrations = @(
        [regex]::Matches($registrationText, '\bMIGRATION_\d+_\d+\b') |
            ForEach-Object { $_.Value } |
            Sort-Object -Unique
    )

    $missingMigrations = @(
        for ($fromVersion = 1; $fromVersion -lt $SourceVersion; $fromVersion++) {
            $expectedMigration = "MIGRATION_$($fromVersion)_$($fromVersion + 1)"
            if ($expectedMigration -cnotin $registeredMigrations) {
                $expectedMigration
            }
        }
    )
    if ($missingMigrations.Count -gt 0) {
        throw "Sequential Room migrations are not all registered in addMigrations(...). Missing: $($missingMigrations -join ', ')"
    }
}

function Get-AppDestinations {
    $destinationText = Get-FileText "app/src/main/java/com/musfit/ui/AppDestination.kt"
    $enumMatch = [regex]::Match($destinationText, '\benum\s+class\s+AppDestination\b')
    if (-not $enumMatch.Success) {
        throw "Could not find the AppDestination enum."
    }

    $bodyOpenIndex = $destinationText.IndexOf('{', $enumMatch.Index + $enumMatch.Length)
    $enumBlock = Get-DelimitedBlock $destinationText $bodyOpenIndex '{' '}' "AppDestination enum"
    $entriesText = @($enumBlock.Content -split ';', 2)[0]
    $entryMatches = [regex]::Matches($entriesText, '(?m)^[ \t]*([A-Z][A-Za-z0-9_]*)\s*\(')
    if ($entryMatches.Count -eq 0) {
        throw "No AppDestination entries were derived from source."
    }

    @($entryMatches | ForEach-Object {
        $entryName = $_.Groups[1].Value
        $argumentsOpenIndex = $entriesText.IndexOf('(', $_.Index)
        $arguments = Get-DelimitedBlock $entriesText $argumentsOpenIndex '(' ')' "AppDestination.$entryName arguments"
        $routeMatch = [regex]::Match($arguments.Content, '\broute\s*=\s*"([^"]+)"')
        if (-not $routeMatch.Success) {
            throw "Could not derive the route for AppDestination.$entryName."
        }

        [pscustomobject]@{
            Name = $entryName
            Route = $routeMatch.Groups[1].Value
        }
    })
}

function Assert-DestinationSummary([string] $RelativePath, [object[]] $Destinations) {
    $text = Get-FileText $RelativePath
    $summaryMatch = [regex]::Match(
        $text,
        '(?is)\bTop-level\s+(?:app\s+)?destinations(?:\s+are)?\s*:?\s*(.{1,300}?)\.'
    )
    if (-not $summaryMatch.Success) {
        throw "Expected $RelativePath to contain a top-level destination summary."
    }

    $documentedNames = @($summaryMatch.Groups[1].Value -split ',' | ForEach-Object {
        (($_ -replace '[`*_]', '') -replace '\s+', ' ').Trim()
    })
    $expectedNames = @($Destinations.Name)
    Assert-Equal "Top-level destination count in $RelativePath" $expectedNames.Count $documentedNames.Count
    Assert-Equal "Top-level destination set/order in $RelativePath" ($expectedNames -join '|') ($documentedNames -join '|')
}

function Normalize-MarkdownTableCell([string] $Cell) {
    (($Cell -replace '^[\s`*_]+|[\s`*_]+$', '') -replace '\s+', ' ').Trim()
}

function Assert-DestinationTable([string] $RelativePath, [object[]] $Destinations) {
    $lines = @((Get-FileText $RelativePath) -split "`r?`n")
    $headerIndex = -1
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $cells = @($lines[$index] -split '\|')
        if ($cells.Count -ge 4 -and
            (Normalize-MarkdownTableCell $cells[1]) -eq "Destination" -and
            (Normalize-MarkdownTableCell $cells[2]) -eq "Route") {
            $headerIndex = $index
            break
        }
    }
    if ($headerIndex -lt 0) {
        throw "Could not find the top-level Destination/Route table in $RelativePath."
    }

    $rows = @()
    for ($index = $headerIndex + 2; $index -lt $lines.Count; $index++) {
        if (-not $lines[$index].TrimStart().StartsWith('|')) {
            break
        }
        $cells = @($lines[$index] -split '\|')
        if ($cells.Count -lt 4) {
            break
        }
        $rows += [pscustomobject]@{
            Name = Normalize-MarkdownTableCell $cells[1]
            Route = Normalize-MarkdownTableCell $cells[2]
        }
    }

    Assert-Equal "Top-level destination table row count in $RelativePath" $Destinations.Count $rows.Count
    for ($index = 0; $index -lt $Destinations.Count; $index++) {
        Assert-Equal "Destination name at row $($index + 1) in $RelativePath" $Destinations[$index].Name $rows[$index].Name
        Assert-Equal "Destination route at row $($index + 1) in $RelativePath" $Destinations[$index].Route $rows[$index].Route
    }
}

function Assert-DocumentedRoomTables([string] $RelativePath, [string[]] $ExpectedTables) {
    $lines = @((Get-FileText $RelativePath) -split "`r?`n")
    $headerIndex = -1
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match '\|\s*Persisted tables\s*\|') {
            $headerIndex = $index
            break
        }
    }
    if ($headerIndex -lt 0) {
        throw "Could not find the persisted-tables map in $RelativePath."
    }

    $documentedTables = @()
    for ($index = $headerIndex + 2; $index -lt $lines.Count; $index++) {
        if (-not $lines[$index].TrimStart().StartsWith('|')) {
            break
        }
        $cells = @($lines[$index] -split '\|')
        if ($cells.Count -lt 6) {
            break
        }
        $documentedTables += @([regex]::Matches($cells[3], '`([^`]+)`') | ForEach-Object {
            $_.Groups[1].Value
        })
    }

    $expected = @($ExpectedTables | Sort-Object -Unique)
    $actual = @($documentedTables | Sort-Object -Unique)
    Assert-Equal "Documented Room table count in $RelativePath" $expected.Count $actual.Count
    Assert-Equal "Documented Room table set in $RelativePath" ($expected -join '|') ($actual -join '|')
}

$workflowDocs = @(
    "AGENTS.md",
    "CLAUDE.md",
    "README.md",
    "docs/architecture/README.md"
)

$liveArchitectureDocs = @(
    "AGENTS.md",
    "CLAUDE.md",
    "README.md",
    "docs/architecture/README.md",
    "docs/architecture/data-models.md",
    "docs/architecture/screen-contracts.md",
    "docs/architecture/food-system.md"
)

foreach ($doc in $workflowDocs) {
    Assert-FileContains $doc 'scripts[\\/]android[\\/]android-env\.ps1'
    Assert-FileDoesNotContain $doc '\.superpowers[\\/]sdd[\\/]android-env\.ps1'
}

foreach ($doc in $liveArchitectureDocs) {
    Assert-FileDoesNotContain $doc 'AppDestination\.Health|route\s*=\s*"health"|\|\s*Health\s*\|\s*`health`'
    Assert-FileDoesNotContain $doc '(?i)\bschema\s+(?:version\s+|v)\d+\b|MusFitDatabase[^\r\n]{0,60}\bversion\s+\d+\b|(?m)^\s*-\s*Version:\s*\d+\s*$'
}

foreach ($doc in @("AGENTS.md", "README.md", "docs/ops/auto-update.md")) {
    Assert-FileDoesNotContain $doc '(?i)seed[- ]receiver[^\r\n]{0,100}(?:remains open|still exports)|still exports[^\r\n]{0,100}seed receiver|SEC-001[^\r\n]{0,100}remains open'
}
Assert-FileContains "AGENTS.md" '(?is)legacy exported seed receiver.{0,120}has been removed.{0,220}instrumentation APK'
Assert-FileContains "README.md" '(?is)legacy exported seed receiver has been removed.{0,220}instrumentation APK'
Assert-FileContains "docs/ops/auto-update.md" '(?is)legacy exported seed receiver tracked as SEC-001 has been removed.{0,220}instrumentation APK'
Assert-FileContains "docs/architecture/app-architecture-audit-2026-07-10.md" '(?is)Post-audit status.{0,500}SEC-001 was resolved.{0,500}DATA-001 was resolved'
Assert-FileContains "docs/architecture/architecture-remediation-backlog-2026-07-10.md" '(?is)W1-SEC-01.{0,120}Completed by PR #79'
Assert-FileContains "CLAUDE.md" "app/src/androidTest/java/com/musfit/"
Assert-FileContains "docs/architecture/README.md" "app/src/androidTest/java/com/musfit/"
Assert-FileDoesNotContain "AGENTS.md" '(?m)^\.\\scripts\\dev\\verify-musfit\.ps1[^\r\n]*-InstallSeed[^\r\n]*-DeviceSerial'

$databaseVersion = Get-DatabaseVersion
Assert-FileContains "app/src/main/java/com/musfit/data/local/MusFitDatabase.kt" "MUSFIT_DATABASE_VERSION\s*=\s*$databaseVersion"
$schemaDirectory = Get-RepoPath "app/schemas/com.musfit.data.local.MusFitDatabase"
if (-not (Test-Path -LiteralPath $schemaDirectory -PathType Container)) {
    throw "Expected exported Room schema directory: $schemaDirectory"
}
$schemaVersions = @(Get-ExportedSchemaVersions $schemaDirectory)
Assert-DatabaseSchemaContract $databaseVersion $schemaVersions
Assert-RegisteredSequentialMigrations $databaseVersion

$latestSchemaPath = Join-Path $schemaDirectory "$databaseVersion.json"
$latestSchema = Get-Content -LiteralPath $latestSchemaPath -Raw | ConvertFrom-Json
foreach ($entity in $latestSchema.database.entities) {
    $tableToken = "$([char] 96)$($entity.tableName)$([char] 96)"
    Assert-FileContains "docs/architecture/data-models.md" ([regex]::Escape($tableToken))
}
Assert-DocumentedRoomTables "docs/architecture/data-models.md" @($latestSchema.database.entities.tableName)

$destinations = Get-AppDestinations
$destinationSummary = ($destinations.Name -join ", ")
Assert-DestinationSummary "AGENTS.md" $destinations
Assert-DestinationSummary "CLAUDE.md" $destinations
Assert-DestinationTable "docs/architecture/README.md" $destinations
Assert-DestinationTable "docs/architecture/screen-contracts.md" $destinations

$appNavText = Get-FileText "app/src/main/java/com/musfit/ui/AppNavGraph.kt"
$bottomBarMatch = [regex]::Match($appNavText, 'bottomBar\s*=\s*\{\s*([A-Za-z][A-Za-z0-9_]*)\s*\(')
if (-not $bottomBarMatch.Success) {
    throw "Could not derive the bottom-navigation component from AppNavGraph.kt."
}
$bottomBarComponent = $bottomBarMatch.Groups[1].Value
Assert-FileContains "CLAUDE.md" ([regex]::Escape($bottomBarComponent))
Assert-FileContains "docs/architecture/README.md" ([regex]::Escape($bottomBarComponent))

$typeText = Get-FileText "app/src/main/java/com/musfit/ui/theme/Type.kt"
$fontMatch = [regex]::Match($typeText, '\bR\.font\.([a-z0-9_]+)_regular\b')
if (-not $fontMatch.Success) {
    throw "Could not derive the app font family from the regular font resource in Type.kt."
}
$fontLabel = @($fontMatch.Groups[1].Value -split '_' | ForEach-Object {
    $_.Substring(0, 1).ToUpperInvariant() + $_.Substring(1)
}) -join ' '
foreach ($fontDoc in @(
    "docs/architecture/README.md",
    "docs/design/material-3-expressive.md",
    "docs/design/musfit-design-system.md"
)) {
    Assert-FileContains $fontDoc ([regex]::Escape($fontLabel))
    Assert-FileDoesNotContain $fontDoc "Google Sans"
}

Assert-FileContains "scripts/android/android-env.ps1" "JAVA_HOME"
Assert-FileContains "scripts/android/android-env.ps1" '(?s)if\s*\(\$env:LOCALAPPDATA\)\s*\{[^}]*MusFitToolchain[\\/]jdk-17'
Assert-FileContains "scripts/android/android-env.ps1" '(?s)if\s*\(\$env:LOCALAPPDATA\)\s*\{[^}]*Android[\\/]Sdk'
Assert-FileContains "scripts/dev/verify-musfit.ps1" '(?s)if\s*\(\$windows\)\s*\{\s*"gradlew\.bat"\s*\}\s*else\s*\{\s*"gradlew"\s*\}'
Assert-FileContains "scripts/dev/verify-musfit.ps1" '&\s*\$gradleWrapper\s+@Arguments'
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "EvidenceDir"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "Assert-LastExitCode"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "pm clear"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "Wait-ForUiDump"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" '(?s)if\s*\(\s*\$DeviceSerial\s+-notmatch\s+[''"]\^emulator-\\d\+\$[''"]\s*\)\s*\{\s*throw\s+[''"]Refusing to seed or reset non-emulator'
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "emu avd name"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "Refusing to seed emulator"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "assembleInternalDebugAndroidTest"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "am instrument"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" "com\.musfit\.internal\.test"
Assert-FileContains "scripts/android/install-seed-musfit.ps1" 'com\.musfit\.internal/com\.musfit\.MainActivity'
Assert-FileDoesNotContain "scripts/android/install-seed-musfit.ps1" "am broadcast|shell monkey|MusFitDebugSeedReceiver|com\.musfit\.debug\.SEED_TEST_DATA"
Assert-FileDoesNotContain "app/src/internal/AndroidManifest.xml" "<receiver|com\.musfit\.debug\.SEED_TEST_DATA"
Assert-FileExists "app/src/androidTest/java/com/musfit/debug/MusFitDebugSeedInstrumentationTest.kt"
Assert-FileContains "app/build.gradle.kts" 'create\("internal"\)'
Assert-FileContains "app/build.gradle.kts" 'applicationIdSuffix\s*=\s*"\.internal"'
Assert-FileContains "app/build.gradle.kts" 'create\("production"\)'
Assert-FileContains "app/build.gradle.kts" 'create\("legacyMigration"\)'
Assert-FileContains "app/build.gradle.kts" '(?s)create\("legacyMigration"\).{0,500}signingConfig\s*=\s*signingConfigs\.getByName\("debug"\)'
Assert-FileContains "app/build.gradle.kts" 'DATA_TRANSFER_MODE.*legacy-export'
Assert-FileContains "app/build.gradle.kts" 'verifyReleaseVariantMatrix'
Assert-FileContains "app/build.gradle.kts" '(?s)release\s*\{.{0,300}isMinifyEnabled\s*=\s*true.{0,200}isShrinkResources\s*=\s*true.{0,300}proguard-android-optimize\.txt.{0,200}proguard-rules\.pro'
Assert-FileExists "app/proguard-rules.pro"
Assert-FileExists "app/proguard-production-reports.pro"
Assert-FileExists "app/proguard-legacy-migration-reports.pro"
Assert-FileContains "app/build.gradle.kts" '(?s)create\("production"\).{0,300}proguardFiles\("proguard-production-reports\.pro"\)'
Assert-FileContains "app/proguard-production-reports.pro" '-printusage\s+build/outputs/r8Reports/productionRelease/usage\.txt'
Assert-FileContains "app/proguard-production-reports.pro" '-printseeds\s+build/outputs/r8Reports/productionRelease/seeds\.txt'
Assert-FileContains "app/proguard-production-reports.pro" '-printconfiguration\s+build/outputs/r8Reports/productionRelease/configuration\.txt'
Assert-FileContains "app/build.gradle.kts" '(?s)create\("legacyMigration"\).{0,500}proguardFiles\("proguard-legacy-migration-reports\.pro"\)'
Assert-FileContains "app/proguard-legacy-migration-reports.pro" '-printusage\s+build/outputs/r8Reports/legacyMigrationRelease/usage\.txt'
Assert-FileContains "app/proguard-legacy-migration-reports.pro" '-printseeds\s+build/outputs/r8Reports/legacyMigrationRelease/seeds\.txt'
Assert-FileContains "app/proguard-legacy-migration-reports.pro" '-printconfiguration\s+build/outputs/r8Reports/legacyMigrationRelease/configuration\.txt'
Assert-FileContains "app/build.gradle.kts" '(?s)minifyProductionReleaseWithR8.{0,200}productionRelease.{0,500}minifyLegacyMigrationReleaseWithR8.{0,200}legacyMigrationRelease.{0,600}doFirst.{0,500}outputs/r8Reports/\$variantName.{0,300}mkdirs\(\)'
Assert-FileDoesNotContain "gradle.properties" 'android\.enableR8\.fullMode\s*=\s*false'
Assert-FileDoesNotContain "app/src/main/AndroidManifest.xml" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileContains "app/src/internal/AndroidManifest.xml" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileContains "app/src/internal/java/com/musfit/ui/permissions/LocalNetworkPermission.kt" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileDoesNotContain "app/src/production/java/com/musfit/ui/permissions/LocalNetworkPermission.kt" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileDoesNotContain "app/src/legacyMigration/java/com/musfit/ui/permissions/LocalNetworkPermission.kt" "android\.permission\.ACCESS_LOCAL_NETWORK"
foreach ($relativeVariantFile in @(
    "data/remote/coach/AiCoachEndpointPolicyConfig.kt",
    "ui/permissions/LocalNetworkPermission.kt",
    "ui/profile/AiCoachVariantCopy.kt"
)) {
    $productionVariantFile = "app/src/production/java/com/musfit/$relativeVariantFile"
    $migrationVariantFile = "app/src/legacyMigration/java/com/musfit/$relativeVariantFile"
    Assert-FileExists $migrationVariantFile
    $productionVariantText = (Get-FileText $productionVariantFile) -replace "`r`n", "`n"
    $migrationVariantText = (Get-FileText $migrationVariantFile) -replace "`r`n", "`n"
    if ($productionVariantText -cne $migrationVariantText) {
        throw "Legacy migration policy must exactly match production: $relativeVariantFile"
    }
}
Assert-FileContains "app/src/main/AndroidManifest.xml" 'android:enabled="\$\{mainLauncherEnabled\}"'
Assert-FileContains "app/src/main/AndroidManifest.xml" 'android:enabled="\$\{legacyMigrationLauncherEnabled\}"'

Assert-FileContains "scripts/dev/clean-generated.ps1" "Remove-Item"
Assert-FileContains "scripts/dev/verify-musfit.ps1" "RetryOnGeneratedOutputIssue"
Assert-FileContains "scripts/dev/verify-musfit.ps1" '(?s)"Full"\s*\{.{0,300}"verifyReleaseVariantMatrix".{0,300}"testInternalDebugUnitTest".{0,200}"testProductionReleaseUnitTest".{0,300}"testLegacyMigrationReleaseUnitTest".{0,500}"assembleInternalDebugAndroidTest".{0,300}"assembleLegacyMigrationRelease".{0,300}"bundleProductionRelease"'
Assert-FileContains "scripts/dev/verify-musfit.ps1" '(?s)if\s*\(\$InstallSeed\)\s*\{.{0,500}"assembleInternalDebug".{0,200}"assembleInternalDebugAndroidTest"'
Assert-FileContains "scripts/dev/new-task-branch.ps1" "origin/master"
Assert-FileContains "scripts/dev/new-task-branch.ps1" "DryRun"

Assert-FileContains ".github/workflows/android.yml" "concurrency:"
Assert-FileContains ".github/workflows/android.yml" "permissions:"
Assert-FileContains ".github/workflows/android.yml" "test-dev-workflow\.ps1"
Assert-FileContains ".github/workflows/android.yml" "verifyReleaseVariantMatrix testInternalDebugUnitTest testLegacyMigrationReleaseUnitTest testProductionReleaseUnitTest lintInternalDebug lintLegacyMigrationRelease lintProductionRelease assembleInternalDebug assembleInternalDebugAndroidTest assembleLegacyMigrationRelease assembleProductionRelease bundleProductionRelease"
Assert-FileContains ".github/workflows/android.yml" "app/build/outputs/apk/internal/debug/app-internal-debug\.apk"
Assert-FileContains ".github/workflows/android.yml" 'app/build/outputs/mapping/productionRelease/mapping\.txt'
Assert-FileContains ".github/workflows/android.yml" 'app/build/outputs/r8Reports/productionRelease/usage\.txt'
Assert-FileContains ".github/workflows/android.yml" 'app/build/outputs/r8Reports/productionRelease/seeds\.txt'
Assert-FileContains ".github/workflows/android.yml" 'app/build/outputs/r8Reports/productionRelease/configuration\.txt'
Assert-FileContains ".github/workflows/android.yml" 'app/build/outputs/mapping/legacyMigrationRelease/mapping\.txt'
Assert-FileContains ".github/workflows/android.yml" 'app/build/outputs/r8Reports/legacyMigrationRelease/usage\.txt'
Assert-FileContains ".github/workflows/android.yml" 'verify-r8-artifacts\.ps1'
Assert-FileExists "scripts/release/verify-r8-artifacts.ps1"
Assert-PowerShellParses "scripts/release/verify-r8-artifacts.ps1"
Assert-FileContains ".github/workflows/android.yml" "verify-data-migration-artifacts\.ps1 -SkipBuild"
Assert-FileExists "scripts/release/verify-data-migration-artifacts.ps1"
Assert-PowerShellParses "scripts/release/verify-data-migration-artifacts.ps1"
Assert-FileContains "scripts/release/verify-data-migration-artifacts.ps1" '(?s)if\s*\(\$windows\)\s*\{.{0,300}android-env\.ps1.{0,300}\}\s*elseif\s*\(-not\s+\$env:ANDROID_SDK_ROOT\s+-and\s+\$env:ANDROID_HOME\)'
Assert-FileContains "scripts/release/verify-data-migration-artifacts.ps1" '\$gradle\s*=\s*if\s*\(\$windows\)\s*\{\s*"\.\\gradlew\.bat"\s*\}\s*else\s*\{\s*"\./gradlew"\s*\}'
Assert-FileContains ".github/workflows/android.yml" "if-no-files-found:\s*error"
Assert-FileDoesNotContain ".github/workflows/android.yml" "app-internal-debug-androidTest\.apk|outputs/apk/androidTest|app-legacyMigration-release\.apk|softprops/action-gh-release|Publish GitHub Release"

# W1-REL-04: production publication is manual, environment-protected, and
# promotes the Google-signed universal APK without rebuilding the candidate.
Assert-FileExists ".github/workflows/release.yml"
Assert-FileContains ".github/workflows/release.yml" "workflow_dispatch:"
Assert-FileDoesNotContain ".github/workflows/release.yml" '(?m)^\s*(push|pull_request|release):'
Assert-FileContains ".github/workflows/release.yml" "environment:\s*production-release"
Assert-FileContains ".github/workflows/release.yml" "contents:\s*write"
Assert-FileContains ".github/workflows/release.yml" "id-token:\s*write"
Assert-FileContains ".github/workflows/release.yml" "assert-verified-release-commit\.ps1"
Assert-FileContains ".github/workflows/release.yml" "verify-musfit\.ps1 -Preset Full"
Assert-FileContains ".github/workflows/release.yml" "prepare-play-upload-bundle\.ps1"
Assert-FileContains ".github/workflows/release.yml" "invoke-play-option-a\.ps1"
Assert-FileDoesNotContain ".github/workflows/release.yml" 'git\s+ls-remote\s+--exit-code\s+--tags'
Assert-FileContains ".github/workflows/release.yml" '(?s)\$existingTag\s*=\s*@\(git\s+ls-remote\s+--tags.{0,300}\$existingTag\.Count\s+-ne\s+0'
Assert-FileContains ".github/workflows/release.yml" "complete-play-internal-release\.ps1"
Assert-FileContains ".github/workflows/release.yml" "verify-release-candidate\.ps1"
Assert-FileContains ".github/workflows/release.yml" "google-github-actions/auth@[0-9a-f]{40}\s+# v3"
Assert-FileDoesNotContain ".github/workflows/release.yml" 'uses:\s+[^\s]+@v[0-9]'
Assert-FileContains ".github/workflows/release.yml" '(?s)preflight:.{0,300}permissions:.{0,150}contents:\s*read.{0,150}actions:\s*read'
Assert-FileContains ".github/workflows/release.yml" '(?s)play-candidate:.{0,400}permissions:.{0,150}contents:\s*read.{0,150}id-token:\s*write'
Assert-FileContains ".github/workflows/release.yml" '(?s)promote:.{0,400}permissions:.{0,150}contents:\s*write'
Assert-FileContains ".github/workflows/release.yml" "workload_identity_provider"
Assert-FileDoesNotContain ".github/workflows/release.yml" "credentials_json|PLAY_SERVICE_ACCOUNT_JSON|musfit\.debug\.keystore|legacyMigration|internalDebug"
Assert-FileContains ".github/workflows/release.yml" "musfit-production-candidate"
Assert-FileContains ".github/workflows/release.yml" "musfit-universal\.apk"
Assert-FileContains ".github/workflows/release.yml" "musfit-play-upload\.aab"
Assert-FileContains ".github/workflows/release.yml" "SHA256SUMS"
Assert-FileContains ".github/workflows/release.yml" "release-metadata\.json"
Assert-FileContains ".github/workflows/release.yml" '(?s)invoke-play-option-a\.ps1.{0,3000}verify-release-candidate\.ps1.{0,3000}upload-artifact@[0-9a-f]{40}.{0,3000}complete-play-internal-release\.ps1'
Assert-FileContains ".github/workflows/release.yml" '(?s)promote:.{0,300}needs:.{0,100}play-candidate.{0,3000}download-artifact@[0-9a-f]{40}.{0,3000}verify-release-candidate\.ps1.{0,3000}gh release create'
Assert-FileContains ".github/workflows/release.yml" 'MUSFIT_PLAY_APP_SIGNING_CERT_SHA256'
Assert-FileContains ".github/workflows/release.yml" 'MUSFIT_UPLOAD_CERT_SHA256'
Assert-FileDoesNotContain ".github/workflows/release.yml" '(?m)^\s*run:\s+.*\$\{\{\s*inputs\.'
Assert-FileExists "scripts/release/assert-verified-release-commit.ps1"
Assert-FileExists "scripts/release/prepare-play-upload-bundle.ps1"
Assert-FileExists "scripts/release/invoke-play-option-a.ps1"
Assert-FileContains "scripts/release/invoke-play-option-a.ps1" ':download\?alt=media'
Assert-FileContains "scripts/release/invoke-play-option-a.ps1" 'Invoke-WebRequest.{0,300}-OutFile\s+\$apk\s+-PassThru'
Assert-FileExists "scripts/release/complete-play-internal-release.ps1"
Assert-FileExists "scripts/release/verify-release-candidate.ps1"
Assert-FileExists "scripts/release/write-release-candidate-metadata.ps1"
Assert-PowerShellParses "scripts/release/assert-verified-release-commit.ps1"
Assert-PowerShellParses "scripts/release/prepare-play-upload-bundle.ps1"
Assert-PowerShellParses "scripts/release/invoke-play-option-a.ps1"
Assert-PowerShellParses "scripts/release/complete-play-internal-release.ps1"
Assert-PowerShellParses "scripts/release/verify-release-candidate.ps1"
Assert-PowerShellParses "scripts/release/write-release-candidate-metadata.ps1"
Assert-FileContains ".gitignore" 'gha-creds-\*\.json'
Assert-FileContains "docs/ops/production-release.md" "Google-managed app-signing key"
Assert-FileContains "docs/ops/production-release.md" "generatedapks"
Assert-FileContains "docs/ops/production-release.md" "production-release"

Assert-FileExists ".github/workflows/pr-emulator-evidence.yml"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "pull_request_target:"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "issue_comment:"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "statuses:\s*write"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "github\.event\.repository\.default_branch"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "github\.event\.comment\.user\.login == github\.repository_owner"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "check-pr-emulator-evidence\.ps1"
Assert-FileContains ".github/workflows/pr-emulator-evidence.yml" "persist-credentials:\s*false"
Assert-FileDoesNotContain ".github/workflows/pr-emulator-evidence.yml" "github\.event\.pull_request\.head\.(?:ref|sha)"
Assert-FileContains ".github/pull_request_template.md" "Verification"
Assert-FileContains ".github/pull_request_template.md" '\$musfit-pr-emulator-evidence'
Assert-FileContains ".github/pull_request_template.md" "current PR head SHA"
Assert-FileContains ".github/pull_request_template.md" "non-runtime skip reason"
Assert-FileContains ".gitignore" "verification/"
Assert-FileContains ".gitignore" '(?m)^/\.kotlin/\r?$'

Assert-FileContains "AGENTS.md" '\.agents[\\/]skills[\\/]musfit-pr-emulator-evidence[\\/]SKILL\.md'
Assert-FileContains "AGENTS.md" 'publish-pr-evidence\.ps1'
Assert-FileContains "AGENTS.md" 'local `verification/`\s+files do not satisfy'
Assert-FileContains "AGENTS.md" 'exact current PR head SHA'
Assert-FileContains "AGENTS.md" 'MusFit emulator evidence'
Assert-FileContains "CLAUDE.md" '\.claude[\\/]skills[\\/]musfit-pr-emulator-evidence[\\/]SKILL\.md'
Assert-FileContains "CLAUDE.md" '/musfit-pr-emulator-evidence'
Assert-FileContains "CLAUDE.md" 'publish-pr-evidence\.ps1'
Assert-FileContains "CLAUDE.md" 'exact current PR head SHA'

$prEvidenceSkillRoot = ".agents/skills/musfit-pr-emulator-evidence"
foreach ($skillFile in @(
    "SKILL.md",
    "agents/openai.yaml",
    "scripts/invoke-musfit-pr-gate.ps1",
    "scripts/capture-emulator-evidence.ps1",
    "scripts/publish-pr-evidence.ps1"
)) {
    Assert-FileExists "$prEvidenceSkillRoot/$skillFile"
}
Assert-FileContains "$prEvidenceSkillRoot/SKILL.md" '(?m)^name: musfit-pr-emulator-evidence\r?$'
Assert-FileContains "$prEvidenceSkillRoot/SKILL.md" 'git rev-parse --show-toplevel'
Assert-FileDoesNotContain "$prEvidenceSkillRoot/SKILL.md" 'Use whenever Codex'
Assert-FileContains "$prEvidenceSkillRoot/SKILL.md" 'Local captures are not PR evidence'
Assert-FileContains "$prEvidenceSkillRoot/SKILL.md" 'MusFit emulator evidence'
Assert-FileContains "$prEvidenceSkillRoot/scripts/invoke-musfit-pr-gate.ps1" 'com\.musfit\.internal'
Assert-FileContains "$prEvidenceSkillRoot/scripts/capture-emulator-evidence.ps1" 'receipt\.device\.packageName'
Assert-FileContains "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1" 'Assert-NonEvidencePrChecksPassed'
Assert-FileContains "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1" 'workflowName.*PR emulator evidence'
Assert-FileContains "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1" '<!-- musfit-emulator-evidence:v1 -->'
Assert-FileContains "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1" 'Verified PR head'
Assert-FileContains "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1" 'com\.musfit\.internal'
Assert-FileDoesNotContain "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1" '\[string\]\s+\$EvidenceBranch'

$claudePrEvidenceSkillRoot = ".claude/skills/musfit-pr-emulator-evidence"
Assert-FileExists "$claudePrEvidenceSkillRoot/SKILL.md"
Assert-FileContains "$claudePrEvidenceSkillRoot/SKILL.md" '(?m)^name: musfit-pr-emulator-evidence\r?$'
Assert-FileContains "$claudePrEvidenceSkillRoot/SKILL.md" '\.\./\.\./\.\./\.agents/skills/musfit-pr-emulator-evidence/SKILL\.md'
Assert-FileContains "$claudePrEvidenceSkillRoot/SKILL.md" 'canonical repository workflow'
Assert-FileContains "$claudePrEvidenceSkillRoot/SKILL.md" 'marker-based PR comment'

Assert-FileExists "scripts/dev/check-pr-emulator-evidence.ps1"
Assert-FileContains "scripts/dev/check-pr-emulator-evidence.ps1" 'MusFit emulator evidence'
Assert-FileContains "scripts/dev/check-pr-emulator-evidence.ps1" '<!-- musfit-emulator-evidence:v1 -->'
Assert-FileContains "scripts/dev/check-pr-emulator-evidence.ps1" 'pr-evidence'
Assert-FileContains "scripts/dev/check-pr-emulator-evidence.ps1" 'Test-PngArtifact'
Assert-FileContains "scripts/dev/check-pr-emulator-evidence.ps1" 'Test-ReceiptArtifact'
Assert-FileContains "scripts/dev/check-pr-emulator-evidence.ps1" 'git/blobs/'
Assert-PowerShellParses "scripts/dev/check-pr-emulator-evidence.ps1"
Assert-PowerShellParses "$prEvidenceSkillRoot/scripts/publish-pr-evidence.ps1"

Assert-FileExists "app/src/testInternalDebug/java/com/musfit/ui/MusFitComposeSemanticsTest.kt"
Assert-FileExists "app/src/test/resources/robolectric.properties"
Assert-FileExists "docs/testing/compose-testing.md"
Assert-FileContains "gradle/libs.versions.toml" 'androidx-compose-ui-test-junit4'
Assert-FileContains "gradle/libs.versions.toml" 'androidx-compose-ui-test-manifest'
Assert-FileContains "app/build.gradle.kts" 'debugImplementation\(libs\.androidx\.compose\.ui\.test\.manifest\)'
Assert-FileContains "app/build.gradle.kts" 'testImplementation\(libs\.androidx\.compose\.ui\.test\.junit4\)'
Assert-FileContains "app/build.gradle.kts" 'processLegacyMigrationReleaseMainManifest'
Assert-FileContains "app/src/testInternalDebug/java/com/musfit/ui/MusFitComposeSemanticsTest.kt" 'junit4\.v2\.createComposeRule'
Assert-FileContains "app/src/testInternalDebug/java/com/musfit/ui/MusFitComposeSemanticsTest.kt" 'StateRestorationTester'
Assert-FileContains "app/src/test/resources/robolectric.properties" '(?m)^sdk=35\r?$'
Assert-FileContains "docs/testing/compose-testing.md" 'Managed-device journey layer'
Assert-FileExists "app/src/androidTest/java/com/musfit/ui/MusFitCriticalJourneyInstrumentationTest.kt"
Assert-FileContains "app/build.gradle.kts" 'execution\s*=\s*"ANDROIDX_TEST_ORCHESTRATOR"'
Assert-FileContains "app/build.gradle.kts" 'create\("criticalJourneysApi28And37"\)'
Assert-FileContains ".github/workflows/android.yml" 'criticalJourneysApi28And37GroupInternalDebugAndroidTest'
Assert-FileContains ".github/workflows/android.yml" 'MusFitCriticalJourneyInstrumentationTest'
Assert-FileContains ".github/workflows/android.yml" 'managed_device_android_test_additional_output'
Assert-FileContains ".github/workflows/android.yml" 'Enable KVM for managed devices'

if ($SelfTest) {
    $mismatchDetected = $false
    $expectedMismatchMessage =
        "Room source/exported schema version mismatch. Expected '$databaseVersion', got '$($databaseVersion - 1)'."
    try {
        $deliberatelyIncompleteSchemas = @($schemaVersions | Where-Object { $_ -ne $databaseVersion })
        Assert-DatabaseSchemaContract $databaseVersion $deliberatelyIncompleteSchemas
    } catch {
        if ($_.Exception.Message -cne $expectedMismatchMessage) {
            throw
        }
        $mismatchDetected = $true
    }

    if (-not $mismatchDetected) {
        throw "Workflow self-test failed to detect a deliberate source/schema mismatch."
    }
    Write-Host "Deliberate mismatch self-test passed."

    & (Get-RepoPath "scripts/dev/check-pr-emulator-evidence.ps1") -SelfTest
}

Write-Host "Development workflow checks passed (Room $databaseVersion; destinations: $destinationSummary; bottom bar: $bottomBarComponent)."
