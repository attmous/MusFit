param(
    [ValidateSet("Full", "Unit", "Food", "Assemble", "None")]
    [string] $Preset = "Full",
    [string[]] $Tests = @(),
    [switch] $InstallSeed,
    [switch] $ResetSeed,
    [switch] $Headless,
    [switch] $NoLaunch,
    [switch] $RetryOnGeneratedOutputIssue,
    [string] $DeviceSerial = "",
    [string] $EvidenceDir = "verification\musfit-emulator"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$windows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
$gradleWrapper = Join-Path $repoRoot $(if ($windows) { "gradlew.bat" } else { "gradlew" })

function Invoke-Gradle([string[]] $Arguments) {
    Push-Location $repoRoot
    try {
        Write-Host "Running: $gradleWrapper $($Arguments -join ' ')"
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $output = & $gradleWrapper @Arguments 2>&1
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        $exitCode = $LASTEXITCODE
        $output | ForEach-Object { Write-Host $_ }

        if ($exitCode -eq 0) {
            return
        }

        $joinedOutput = $output -join "`n"
        $isGeneratedOutputIssue =
            $joinedOutput -match "AccessDeniedException|Cannot snapshot|not a regular file" -and
            $joinedOutput -match "app[\\/]+build"

        if ($RetryOnGeneratedOutputIssue -and $isGeneratedOutputIssue) {
            Write-Host "Generated-output failure detected; cleaning app\build and retrying once."
            & (Join-Path $repoRoot "scripts\dev\clean-generated.ps1")
            if ($LASTEXITCODE -ne 0) {
                throw "Generated-output cleanup failed with exit code $LASTEXITCODE"
            }

            $previousErrorActionPreference = $ErrorActionPreference
            $ErrorActionPreference = "Continue"
            try {
                $output = & $gradleWrapper @Arguments 2>&1
            } finally {
                $ErrorActionPreference = $previousErrorActionPreference
            }
            $exitCode = $LASTEXITCODE
            $output | ForEach-Object { Write-Host $_ }
        }

        if ($exitCode -ne 0) {
            throw "Gradle failed with exit code $exitCode"
        }
    } finally {
        Pop-Location
    }
}

. (Join-Path $repoRoot "scripts\android\android-env.ps1")

$gradleArgs = @()
if ($Tests.Count -gt 0) {
    $gradleArgs = @("testInternalDebugUnitTest")
    foreach ($test in $Tests) {
        $gradleArgs += @("--tests", $test)
    }
} else {
    switch ($Preset) {
        "Full" {
            $gradleArgs = @(
                "spotlessCheck",
                "detekt",
                "verifyReleaseVariantMatrix",
                "testInternalDebugUnitTest",
                "testProductionReleaseUnitTest",
                "testLegacyMigrationReleaseUnitTest",
                "lintInternalDebug",
                "lintProductionRelease",
                "lintLegacyMigrationRelease",
                "assembleInternalDebug",
                "assembleInternalDebugAndroidTest",
                "assembleProductionRelease",
                "assembleLegacyMigrationRelease",
                "bundleProductionRelease"
            )
        }
        "Unit" { $gradleArgs = @("testInternalDebugUnitTest", "testProductionReleaseUnitTest", "testLegacyMigrationReleaseUnitTest") }
        "Food" {
            $gradleArgs = @(
                "testInternalDebugUnitTest",
                "--tests", "com.musfit.ui.food.FoodViewModelTest",
                "--tests", "com.musfit.data.repository.LocalFoodRepositoryTest"
            )
        }
        "Assemble" {
            $gradleArgs = @(
                "assembleInternalDebug",
                "assembleProductionRelease",
                "assembleLegacyMigrationRelease",
                "bundleProductionRelease"
            )
        }
        "None" { $gradleArgs = @() }
    }
}

if ($InstallSeed) {
    $missingSeedBuildTasks = @(
        "assembleInternalDebug",
        "assembleInternalDebugAndroidTest"
    ) | Where-Object { $_ -notin $gradleArgs }
    $gradleArgs = @($missingSeedBuildTasks) + $gradleArgs
}

if ($Tests.Count -eq 0 -and $Preset -eq "Full") {
    Write-Host "Running source-derived development workflow contract."
    & (Join-Path $repoRoot "scripts\dev\test-dev-workflow.ps1") -SelfTest
    Write-Host "Verifying that unused WorkManager/Hilt Work does not return."
    & (Join-Path $repoRoot "scripts\dev\test-no-unused-workmanager.ps1")
    Write-Host "Verifying KSP-only Room/Hilt processing."
    & (Join-Path $repoRoot "scripts\dev\test-ksp-migration.ps1")
    Write-Host "Verifying immutable supply-chain policy and tamper fixtures."
    & (Join-Path $repoRoot "scripts\supply-chain\test-supply-chain.ps1") -SelfTest
    Write-Host "Verifying stable-first dependency governance and catalog ownership."
    & (Join-Path $repoRoot "scripts\dependencies\test-dependency-governance.ps1") -SelfTest
    Write-Host "Verifying static-quality baselines, ownership, and expiry."
    & (Join-Path $repoRoot "scripts\quality\test-static-quality.ps1") -SelfTest
    Write-Host "Verifying actionable coverage policy and aggregation fixtures."
    & (Join-Path $repoRoot "scripts\coverage\test-coverage-policy.ps1") -SelfTest
}

if ($gradleArgs.Count -gt 0) {
    $gradleArgs += @("--no-daemon", "--console=plain")
    Invoke-Gradle $gradleArgs
}

if ($Tests.Count -eq 0 -and $Preset -eq "Full") {
    Write-Host "Generating the verified CycloneDX SBOM in an isolated Gradle graph."
    Invoke-Gradle @("cyclonedxBom", "--no-daemon", "--console=plain")
}

if ($Tests.Count -eq 0 -and $Preset -eq "Full") {
    & (Join-Path $repoRoot "scripts\dev\test-no-unused-workmanager.ps1") -RequireReleaseArtifact
}

if ($InstallSeed) {
    $installParams = @{}
    if ($DeviceSerial) { $installParams.DeviceSerial = $DeviceSerial }
    if ($ResetSeed) { $installParams.Reset = $true }
    if ($Headless) { $installParams.Headless = $true }
    if ($NoLaunch) { $installParams.NoLaunch = $true }
    if ($gradleArgs.Count -gt 0) { $installParams.SkipBuild = $true }
    if ($EvidenceDir) { $installParams.EvidenceDir = (Join-Path $repoRoot $EvidenceDir) }

    $displayArgs = $installParams.GetEnumerator() |
        Sort-Object Name |
        ForEach-Object {
            if ($_.Value -is [bool] -and $_.Value) {
                "-$($_.Name)"
            } else {
                "-$($_.Name) $($_.Value)"
            }
        }
    Write-Host "Running: .\scripts\android\install-seed-musfit.ps1 $($displayArgs -join ' ')"
    & (Join-Path $repoRoot "scripts\android\install-seed-musfit.ps1") @installParams
    if ($LASTEXITCODE -ne 0) {
        throw "Install/seed failed with exit code $LASTEXITCODE"
    }
}

Write-Host "MusFit verification command completed."
