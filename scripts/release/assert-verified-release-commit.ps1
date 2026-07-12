param(
    [string] $CommitSha,
    [string] $Repository = $env:GITHUB_REPOSITORY,
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"

function Select-VerifiedAndroidRun([object[]] $Runs, [string] $ExpectedSha) {
    @($Runs | Where-Object {
        $_.name -ceq "Android" -and
        $_.path -ceq ".github/workflows/android.yml" -and
        $_.head_sha -ceq $ExpectedSha -and
        $_.event -ceq "push" -and
        $_.status -ceq "completed" -and
        $_.conclusion -ceq "success"
    })
}

if ($SelfTest) {
    $sha = "0123456789abcdef0123456789abcdef01234567"
    $runs = @(
        [pscustomobject]@{ name = "Android"; path = ".github/workflows/android.yml"; head_sha = $sha; event = "pull_request"; status = "completed"; conclusion = "success" },
        [pscustomobject]@{ name = "Android"; path = ".github/workflows/android.yml"; head_sha = $sha; event = "push"; status = "completed"; conclusion = "failure" },
        [pscustomobject]@{ name = "Android"; path = ".github/workflows/android.yml"; head_sha = $sha; event = "push"; status = "completed"; conclusion = "success" }
    )
    $selected = @(Select-VerifiedAndroidRun $runs $sha)
    if ($selected.Count -ne 1) { throw "Verified-run selection self-test failed." }
    if (@(Select-VerifiedAndroidRun $runs ("f" * 40)).Count -ne 0) { throw "Mismatched SHA self-test failed." }
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
$selected = @(Select-VerifiedAndroidRun @($response.workflow_runs) $CommitSha)
if ($selected.Count -lt 1) {
    throw "No successful push-triggered Android workflow verifies release commit $CommitSha."
}

if ($env:GITHUB_OUTPUT) {
    "verified_commit=$CommitSha" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8 -Append
    "verification_run_id=$($selected[0].id)" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Encoding utf8 -Append
}
Write-Host "Release commit verified by Android workflow run $($selected[0].id): $CommitSha"
