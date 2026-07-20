param(
    [string] $Task = "",
    [string[]] $GradleArguments = @(),
    [string] $ResultsDirectory = "",
    [ValidateRange(0, 2)]
    [int] $Api37InfrastructureRetries = 0,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path
$windows = [Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT
$gradleWrapper = Join-Path $repoRoot $(if ($windows) { "gradlew.bat" } else { "gradlew" })
$processCrashPattern = "Instrumentation run failed due to Process crashed"
$zeroTestSuitePattern = '<testsuites\s+[^>]*tests="0"'
$bluetoothHardwareErrorPattern = "on_hardware_error: Hardware Error Event with code 0x42"
$bluetoothAbortPattern = "Fatal signal 6 .*\(gd_stack_thread\).*\(droid\.bluetooth\)"
$appAbortPattern = "Fatal signal \d+ .*\(com\.musfit\.internal(?:\.test)?\)"

function Resolve-RepoChildPath([string] $RelativePath) {
    if ([string]::IsNullOrWhiteSpace($RelativePath) -or [IO.Path]::IsPathRooted($RelativePath)) {
        throw "Expected a non-empty repository-relative path, got: '$RelativePath'"
    }

    $fullPath = [IO.Path]::GetFullPath((Join-Path $repoRoot $RelativePath))
    $repoPrefix = $repoRoot.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) +
        [IO.Path]::DirectorySeparatorChar
    if (-not $fullPath.StartsWith($repoPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path must stay inside the repository: $RelativePath"
    }
    $fullPath
}

function Test-FileSetContains(
    [IO.FileInfo[]] $Files,
    [string] $Pattern,
    [switch] $SimpleMatch
) {
    foreach ($file in $Files) {
        $match = if ($SimpleMatch) {
            Select-String -LiteralPath $file.FullName -Pattern $Pattern -SimpleMatch -Quiet
        } else {
            Select-String -LiteralPath $file.FullName -Pattern $Pattern -Quiet
        }
        if ($match) {
            return $true
        }
    }
    $false
}

function Test-Api37RetryableInfrastructureCrash([string] $ResultPath) {
    if (-not (Test-Path -LiteralPath $ResultPath -PathType Container)) {
        return $false
    }

    $reports = @(Get-ChildItem -LiteralPath $ResultPath -Recurse -File -Filter "*.xml")
    $logs = @(Get-ChildItem -LiteralPath $ResultPath -Recurse -File -Filter "logcat-*.txt")
    if ($reports.Count -eq 0 -or
        -not (Test-FileSetContains $reports $processCrashPattern -SimpleMatch)) {
        return $false
    }

    if ($logs.Count -gt 0 -and (Test-FileSetContains $logs $appAbortPattern)) {
        return $false
    }

    $zeroTestsStarted = Test-FileSetContains $reports $zeroTestSuitePattern
    $bluetoothServiceAborted =
        $logs.Count -gt 0 -and
        (Test-FileSetContains $logs $bluetoothHardwareErrorPattern -SimpleMatch) -and
        (Test-FileSetContains $logs $bluetoothAbortPattern)
    $zeroTestsStarted -or $bluetoothServiceAborted
}

function Invoke-SelfTest {
    $fixtureRoot = Join-Path ([IO.Path]::GetTempPath()) "musfit-managed-device-retry-$([guid]::NewGuid())"
    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
    try {
        $reportPath = Join-Path $fixtureRoot "TEST-device.xml"
        $logPath = Join-Path $fixtureRoot "logcat-test.txt"

        Set-Content -LiteralPath $reportPath -Value $processCrashPattern
        Set-Content -LiteralPath $logPath -Value @(
            $bluetoothHardwareErrorPattern
            "F libc: Fatal signal 6 (SIGABRT) in tid 1353 (gd_stack_thread), pid 1191 (droid.bluetooth)"
        )
        if (-not (Test-Api37RetryableInfrastructureCrash $fixtureRoot)) {
            throw "Self-test did not recognize the exact API 37 Bluetooth infrastructure crash."
        }

        Set-Content -LiteralPath $reportPath -Value @(
            '<testsuites tests="0" failures="0">'
            "  <system-err>$processCrashPattern</system-err>"
            "</testsuites>"
        )
        Remove-Item -LiteralPath $logPath
        if (-not (Test-Api37RetryableInfrastructureCrash $fixtureRoot)) {
            throw "Self-test did not recognize the zero-test API 37 process crash."
        }

        Set-Content -LiteralPath $logPath -Value "F libc: Fatal signal 11 (SIGSEGV) in tid 1, pid 2 (com.musfit.internal)"
        if (Test-Api37RetryableInfrastructureCrash $fixtureRoot) {
            throw "Self-test misclassified an app process crash as retryable infrastructure."
        }

        Set-Content -LiteralPath $reportPath -Value "AssertionFailedError"
        Set-Content -LiteralPath $logPath -Value @(
            $bluetoothHardwareErrorPattern
            "F libc: Fatal signal 6 (SIGABRT) in tid 1353 (gd_stack_thread), pid 1191 (droid.bluetooth)"
        )
        if (Test-Api37RetryableInfrastructureCrash $fixtureRoot) {
            throw "Self-test misclassified an assertion failure as retryable infrastructure."
        }
    } finally {
        if (Test-Path -LiteralPath $fixtureRoot) {
            Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
        }
    }

    Write-Host "Managed-device infrastructure retry self-test passed."
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Task)) {
    throw "Task is required."
}
if ([string]::IsNullOrWhiteSpace($ResultsDirectory)) {
    throw "ResultsDirectory is required."
}

$resultPath = Resolve-RepoChildPath $ResultsDirectory
$retryArchiveRoot = Resolve-RepoChildPath "app/build/outputs/managed-device-infra-retries"

function Invoke-GradleAttempt {
    if (Test-Path -LiteralPath $resultPath) {
        Remove-Item -LiteralPath $resultPath -Recurse -Force
    }

    Push-Location $repoRoot
    try {
        Write-Host "Running: $gradleWrapper $Task $($GradleArguments -join ' ') --no-daemon --console=plain"
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            & $gradleWrapper $Task @GradleArguments --no-daemon --console=plain 2>&1 |
                ForEach-Object { Write-Host $_ }
            $exitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        $exitCode
    } finally {
        Pop-Location
    }
}

$maximumAttempts = 1 + $Api37InfrastructureRetries
for ($attempt = 1; $attempt -le $maximumAttempts; $attempt++) {
    $exitCode = Invoke-GradleAttempt
    if ($exitCode -eq 0) {
        if ($attempt -gt 1) {
            Write-Host "Managed-device task passed on infrastructure attempt $attempt of $maximumAttempts."
        }
        exit 0
    }

    if ($attempt -eq $maximumAttempts -or
        -not (Test-Api37RetryableInfrastructureCrash $resultPath)) {
        throw "Managed-device task $Task failed on attempt $attempt of $maximumAttempts with exit code $exitCode; failure is not eligible for another infrastructure retry."
    }

    $archivePath = Join-Path $retryArchiveRoot "$(Split-Path -Leaf $resultPath)-attempt-$attempt-$([DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ'))"
    New-Item -ItemType Directory -Path $retryArchiveRoot -Force | Out-Null
    Copy-Item -LiteralPath $resultPath -Destination $archivePath -Recurse
    Write-Warning "API 37 infrastructure process crashed before completion; retained attempt $attempt diagnostics and retrying on a fresh managed device."
}
