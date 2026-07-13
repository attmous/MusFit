param(
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path
$windows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
$gradleWrapper = Join-Path $repoRoot $(if ($windows) { "gradlew.bat" } else { "gradlew" })

if ($windows) {
    . (Join-Path $repoRoot "scripts/android/android-env.ps1")
}

function Invoke-GradleCapture([string[]] $Arguments) {
    Push-Location $repoRoot
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = @(& $gradleWrapper @Arguments 2>&1)
        [pscustomobject]@{
            ExitCode = $LASTEXITCODE
            Output = @($output | ForEach-Object { $_.ToString() })
        }
    } finally {
        $ErrorActionPreference = $previousPreference
        Pop-Location
    }
}

Push-Location $repoRoot
try {
    $shallow = (& git rev-parse --is-shallow-repository).Trim()
    if ($LASTEXITCODE -ne 0 -or $shallow -cne "false") {
        throw "Version-code contract requires a non-shallow Git checkout; got '$shallow'."
    }
    $commitCountText = (& git rev-list --count HEAD).Trim()
    $commitCount = 0
    if (-not [int]::TryParse($commitCountText, [ref] $commitCount) -or $commitCount -le 0) {
        throw "Git returned an invalid commit count: '$commitCountText'."
    }
} finally {
    Pop-Location
}

$cacheArguments = @(
    "verifyReleaseVariantMatrix",
    "--no-daemon",
    "--console=plain"
)
$first = Invoke-GradleCapture $cacheArguments
$first.Output | ForEach-Object { Write-Host $_ }
if ($first.ExitCode -ne 0) {
    throw "First configuration-cache probe failed with exit code $($first.ExitCode)."
}

$second = Invoke-GradleCapture $cacheArguments
$second.Output | ForEach-Object { Write-Host $_ }
if ($second.ExitCode -ne 0) {
    throw "Second configuration-cache probe failed with exit code $($second.ExitCode)."
}
if (($second.Output -join "`n") -notmatch "Configuration cache entry reused\.") {
    throw "Second configuration-cache probe did not reuse the entry."
}

if ($SelfTest) {
    $missingGit = "musfit-git-must-not-exist-$PID"
    $failure = Invoke-GradleCapture @(
        "help",
        "-Pmusfit.gitExecutable=$missingGit",
        "--no-configuration-cache",
        "--no-daemon",
        "--console=plain"
    )
    $failure.Output | ForEach-Object { Write-Host $_ }
    if ($failure.ExitCode -eq 0) {
        throw "Version-code derivation did not fail when Git was unavailable."
    }
    $failureText = $failure.Output -join "`n"
    if ($failureText -notmatch "Could not execute '$([regex]::Escape($missingGit))' while deriving versionCode\.") {
        throw "Version-code failure did not contain the fail-closed diagnostic."
    }
    $global:LASTEXITCODE = 0
    Write-Host "Deliberate missing-Git version-code self-test passed."
}

Write-Host "Configuration cache reused cleanly; Git commit count is $commitCount."
