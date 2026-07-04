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

function Invoke-Gradle([string[]] $Arguments) {
    Push-Location $repoRoot
    try {
        Write-Host "Running: .\gradlew.bat $($Arguments -join ' ')"
        $output = & .\gradlew.bat @Arguments 2>&1
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

            $output = & .\gradlew.bat @Arguments 2>&1
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
    $gradleArgs = @("testDebugUnitTest")
    foreach ($test in $Tests) {
        $gradleArgs += @("--tests", $test)
    }
} else {
    switch ($Preset) {
        "Full" { $gradleArgs = @("testDebugUnitTest", "lintDebug", "assembleDebug") }
        "Unit" { $gradleArgs = @("testDebugUnitTest") }
        "Food" {
            $gradleArgs = @(
                "testDebugUnitTest",
                "--tests", "com.musfit.ui.food.FoodViewModelTest",
                "--tests", "com.musfit.data.repository.LocalFoodRepositoryTest"
            )
        }
        "Assemble" { $gradleArgs = @("assembleDebug") }
        "None" { $gradleArgs = @() }
    }
}

if ($gradleArgs.Count -gt 0) {
    $gradleArgs += @("--no-daemon", "--console=plain")
    Invoke-Gradle $gradleArgs
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
