param(
    [string] $CommitSha,
    [string] $Repository = $env:GITHUB_REPOSITORY,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

function Select-VerifiedWorkflowRun(
    [object[]] $Runs,
    [string] $ExpectedSha,
    [string] $WorkflowName,
    [string] $WorkflowPath
) {
    @($Runs | Where-Object {
        $_.name -ceq $WorkflowName -and
        $_.path -ceq $WorkflowPath -and
        $_.head_sha -ceq $ExpectedSha -and
        $_.event -ceq "push" -and
        $_.status -ceq "completed" -and
        $_.conclusion -ceq "success"
    })
}

$requiredWorkflows = @(
    [pscustomobject]@{ Name = "Android"; Path = ".github/workflows/android.yml"; Output = "verification_run_id" },
    [pscustomobject]@{ Name = "Android device and UI"; Path = ".github/workflows/device-ui.yml"; Output = "device_verification_run_id" },
    [pscustomobject]@{ Name = "Android performance"; Path = ".github/workflows/performance.yml"; Output = "performance_verification_run_id" }
)

if ($SelfTest) {
    $sha = "0123456789abcdef0123456789abcdef01234567"
    $runs = @(
        [pscustomobject]@{ name = "Android"; path = ".github/workflows/android.yml"; head_sha = $sha; event = "pull_request"; status = "completed"; conclusion = "success" },
        [pscustomobject]@{ name = "Android"; path = ".github/workflows/android.yml"; head_sha = $sha; event = "push"; status = "completed"; conclusion = "failure" },
        [pscustomobject]@{ id = 1; name = "Android"; path = ".github/workflows/android.yml"; head_sha = $sha; event = "push"; status = "completed"; conclusion = "success" },
        [pscustomobject]@{ id = 2; name = "Android device and UI"; path = ".github/workflows/device-ui.yml"; head_sha = $sha; event = "push"; status = "completed"; conclusion = "success" },
        [pscustomobject]@{ id = 3; name = "Android performance"; path = ".github/workflows/performance.yml"; head_sha = $sha; event = "push"; status = "completed"; conclusion = "success" }
    )
    foreach ($required in $requiredWorkflows) {
        $selected = @(Select-VerifiedWorkflowRun $runs $sha $required.Name $required.Path)
        if ($selected.Count -ne 1) { throw "Verified-run selection self-test failed for $($required.Name)." }
        if (@(Select-VerifiedWorkflowRun $runs ("f" * 40) $required.Name $required.Path).Count -ne 0) {
            throw "Mismatched SHA self-test failed for $($required.Name)."
        }
    }
    Write-Host "Verified release commit self-test passed."
    return
}

if ($CommitSha -notmatch '^[0-9a-f]{40}$') { throw "CommitSha must be a full lowercase 40-character Git SHA." }
if ($Repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') { throw "Repository must be owner/name." }
if (-not $env:GITHUB_TOKEN) { throw "GITHUB_TOKEN is required to verify the successful Android workflow." }

& git fetch --no-tags origin master
if ($LASTEXITCODE -ne 0) { throw "Could not fetch origin/master." }
& git merge-base --is-ancestor $CommitSha origin/master
if ($LASTEXITCODE -ne 0) { throw "Release commit $CommitSha is not contained in origin/master." }

$headers = @{
    Accept = "application/vnd.github+json"
    Authorization = "Bearer $($env:GITHUB_TOKEN)"
    "X-GitHub-Api-Version" = "2022-11-28"
}
$uri = "https://api.github.com/repos/$Repository/actions/runs?head_sha=$CommitSha&status=success&event=push&per_page=50"
$response = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers
$selectedRuns = @{}
foreach ($required in $requiredWorkflows) {
    $selected = @(Select-VerifiedWorkflowRun @($response.workflow_runs) $CommitSha $required.Name $required.Path)
    if ($selected.Count -lt 1) {
        throw "No successful push-triggered '$($required.Name)' workflow verifies release commit $CommitSha."
    }
    $selectedRuns[$required.Name] = $selected[0]
}

if ($env:GITHUB_OUTPUT) {
    "verified_commit=$CommitSha" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8 -Append
    foreach ($required in $requiredWorkflows) {
        "$($required.Output)=$($selectedRuns[$required.Name].id)" |
            Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8 -Append
    }
}
$runSummary = $requiredWorkflows | ForEach-Object { "$($_.Name)=$($selectedRuns[$_.Name].id)" }
Write-Host "Release commit verified by exact-commit push workflows ($($runSummary -join '; ')): $CommitSha"
