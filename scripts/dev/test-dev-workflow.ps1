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
Assert-FileContains "app/build.gradle.kts" 'verifyReleaseVariantMatrix'
Assert-FileDoesNotContain "app/src/main/AndroidManifest.xml" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileContains "app/src/internal/AndroidManifest.xml" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileContains "app/src/internal/java/com/musfit/ui/permissions/LocalNetworkPermission.kt" "android\.permission\.ACCESS_LOCAL_NETWORK"
Assert-FileDoesNotContain "app/src/production/java/com/musfit/ui/permissions/LocalNetworkPermission.kt" "android\.permission\.ACCESS_LOCAL_NETWORK"

Assert-FileContains "scripts/dev/clean-generated.ps1" "Remove-Item"
Assert-FileContains "scripts/dev/verify-musfit.ps1" "RetryOnGeneratedOutputIssue"
Assert-FileContains "scripts/dev/verify-musfit.ps1" '(?s)"Full"\s*\{.{0,300}"verifyReleaseVariantMatrix".{0,300}"testInternalDebugUnitTest".{0,200}"testProductionReleaseUnitTest".{0,500}"assembleInternalDebugAndroidTest".{0,300}"bundleProductionRelease"'
Assert-FileContains "scripts/dev/verify-musfit.ps1" '(?s)if\s*\(\$InstallSeed\)\s*\{.{0,500}"assembleInternalDebug".{0,200}"assembleInternalDebugAndroidTest"'
Assert-FileContains "scripts/dev/new-task-branch.ps1" "origin/master"
Assert-FileContains "scripts/dev/new-task-branch.ps1" "DryRun"

Assert-FileContains ".github/workflows/android.yml" "concurrency:"
Assert-FileContains ".github/workflows/android.yml" "permissions:"
Assert-FileContains ".github/workflows/android.yml" "test-dev-workflow\.ps1"
Assert-FileContains ".github/workflows/android.yml" "verifyReleaseVariantMatrix testInternalDebugUnitTest testProductionReleaseUnitTest lintInternalDebug lintProductionRelease assembleInternalDebug assembleInternalDebugAndroidTest assembleProductionRelease bundleProductionRelease"
Assert-FileContains ".github/workflows/android.yml" "app/build/outputs/apk/internal/debug/app-internal-debug\.apk"
Assert-FileContains ".github/workflows/android.yml" "if-no-files-found:\s*error"
Assert-FileDoesNotContain ".github/workflows/android.yml" "app-internal-debug-androidTest\.apk|outputs/apk/androidTest|softprops/action-gh-release|Publish GitHub Release"
Assert-FileContains ".github/pull_request_template.md" "Verification"
Assert-FileContains ".github/pull_request_template.md" '\$musfit-pr-emulator-evidence'
Assert-FileContains ".gitignore" "verification/"

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
Assert-FileContains "$prEvidenceSkillRoot/scripts/invoke-musfit-pr-gate.ps1" 'com\.musfit\.internal'
Assert-FileContains "$prEvidenceSkillRoot/scripts/capture-emulator-evidence.ps1" 'receipt\.device\.packageName'

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
}

Write-Host "Development workflow checks passed (Room $databaseVersion; destinations: $destinationSummary; bottom bar: $bottomBarComponent)."
