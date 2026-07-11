[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateRange(0, 2147483647)]
    [int] $PullRequest,
    [Parameter(Mandatory = $true)]
    [string] $EvidenceDir,
    [string] $Repository = "",
    [switch] $ConfirmVisualReview,
    [switch] $DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$EvidenceBranch = "pr-evidence"

function Assert-LastExitCode([string] $Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

function Invoke-GhJson {
    param(
        [Parameter(Mandatory = $true)]
        [string[]] $Arguments,
        [object] $InputObject = $null
    )

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        if ($null -eq $InputObject) {
            $output = @(& gh @Arguments 2>&1)
        } else {
            $json = $InputObject | ConvertTo-Json -Depth 20 -Compress
            $output = @($json | & gh @Arguments 2>&1)
        }
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }

    if ($exitCode -ne 0) {
        throw "gh $($Arguments -join ' ') failed with exit code $exitCode`n$($output -join "`n")"
    }

    $text = $output -join "`n"
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }
    return $text | ConvertFrom-Json
}

function ConvertTo-UrlPath([string] $Path) {
    return (($Path -split '/') | ForEach-Object { [Uri]::EscapeDataString($_) }) -join '/'
}

function Get-OptionalProperty([object] $Object, [string] $Name) {
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    $property.Value
}

function Assert-NonEvidencePrChecksPassed([int] $PrNumber, [string] $Repo) {
    $status = Invoke-GhJson -Arguments @(
        "pr", "view", $PrNumber.ToString(),
        "--repo", $Repo,
        "--json", "statusCheckRollup"
    )
    $allChecks = @()
    foreach ($check in (Get-OptionalProperty $status "statusCheckRollup")) {
        $allChecks += $check
    }
    $checked = 0
    $blocking = @()
    foreach ($check in $allChecks) {
        $type = [string](Get-OptionalProperty $check "__typename")
        $name = if ($type -eq "StatusContext") {
            [string](Get-OptionalProperty $check "context")
        } else {
            [string](Get-OptionalProperty $check "name")
        }
        if ($name -eq "MusFit emulator evidence") {
            continue
        }
        # The evidence workflow's own evaluate run stays FAILURE until this very
        # publication lands, so treating it as a prerequisite would deadlock every
        # runtime PR. It is part of the evidence loop, not a gate for it.
        $workflowName = [string](Get-OptionalProperty $check "workflowName")
        if ($type -ne "StatusContext" -and $workflowName -eq "PR emulator evidence") {
            continue
        }

        $checked++
        if ($type -eq "StatusContext") {
            $state = [string](Get-OptionalProperty $check "state")
            if ($state -ne "SUCCESS") {
                $blocking += "$name ($state)"
            }
            continue
        }

        $checkStatus = [string](Get-OptionalProperty $check "status")
        $conclusion = [string](Get-OptionalProperty $check "conclusion")
        if (
            $checkStatus -ne "COMPLETED" -or
            $conclusion -notin @("SUCCESS", "SKIPPED", "NEUTRAL")
        ) {
            $blocking += "$name ($checkStatus/$conclusion)"
        }
    }

    if ($checked -eq 0) {
        throw "No non-evidence GitHub PR checks were found; wait for CI before publishing evidence"
    }
    if ($blocking.Count -gt 0) {
        throw "Non-evidence GitHub PR checks are not all passing: $($blocking -join ', ')"
    }
}

if (-not $ConfirmVisualReview) {
    throw "Inspect every selected PNG, then rerun with -ConfirmVisualReview"
}
if (-not $DryRun -and $PullRequest -lt 1) {
    throw "A real pull request number is required unless -DryRun is used"
}
$repoRootOutput = & git rev-parse --show-toplevel 2>&1
Assert-LastExitCode "Resolve repository root"
$repoRoot = [IO.Path]::GetFullPath(($repoRootOutput | Select-Object -First 1).Trim())
if (-not [IO.Path]::IsPathRooted($EvidenceDir)) {
    $EvidenceDir = Join-Path $repoRoot $EvidenceDir
}
$resolvedEvidenceDir = [IO.Path]::GetFullPath($EvidenceDir)
$receiptPath = Join-Path $resolvedEvidenceDir "verification.json"
if (-not (Test-Path -LiteralPath $receiptPath)) {
    throw "Verification receipt not found: $receiptPath"
}

$receipt = Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if ($receipt.schemaVersion -ne 1 -or $receipt.status -ne "passed") {
    throw "Unsupported or unsuccessful verification receipt"
}

$headOutput = & git -C $repoRoot rev-parse HEAD 2>&1
Assert-LastExitCode "Resolve current HEAD"
$currentHead = ($headOutput | Select-Object -First 1).Trim()
if ($currentHead -ne $receipt.headSha) {
    throw "Current HEAD $currentHead does not match verified HEAD $($receipt.headSha)"
}

$worktreeChanges = @(& git -C $repoRoot status --porcelain --untracked-files=all 2>&1)
Assert-LastExitCode "Inspect worktree state"
if ($worktreeChanges.Count -gt 0) {
    throw "Worktree changes appeared after verification; rerun the complete evidence workflow from a clean PR head"
}

$receiptChecks = @()
foreach ($check in $receipt.checks) {
    $receiptChecks += $check
    if ($check.status -ne "passed") {
        throw "Verification check did not pass: $($check.name)"
    }
}
if ($receiptChecks.Count -eq 0) {
    throw "Verification receipt does not contain any checks"
}
$receiptCheckNames = @($receiptChecks | ForEach-Object { [string] $_.name })
foreach ($requiredCheck in @("Full Gradle gate", "Seeded emulator install", "Foreground launch")) {
    if ($requiredCheck -notin $receiptCheckNames) {
        throw "Verification receipt is missing required check: $requiredCheck"
    }
}
if (
    $receipt.device.serial -notmatch '^emulator-\d+$' -or
    $receipt.device.avdName -ne "MusFit_API36" -or
    $receipt.device.packageName -ne "com.musfit.internal" -or
    -not $receipt.device.seeded
) {
    throw "Evidence receipt is not from the seeded MusFit_API36 emulator"
}

$metadataFiles = @(Get-ChildItem -LiteralPath (Join-Path $resolvedEvidenceDir "screenshots") -Filter "*.json" -File -ErrorAction SilentlyContinue | Sort-Object Name)
if ($metadataFiles.Count -eq 0) {
    throw "No workflow screenshots were captured"
}

$screenshots = @()
foreach ($metadataFile in $metadataFiles) {
    $metadata = Get-Content -LiteralPath $metadataFile.FullName -Raw | ConvertFrom-Json
    if ($metadata.schemaVersion -ne 1 -or $metadata.headSha -ne $receipt.headSha) {
        throw "Stale or unsupported screenshot metadata: $($metadataFile.FullName)"
    }
    if ($metadata.deviceSerial -ne $receipt.device.serial) {
        throw "Screenshot device does not match the verification receipt: $($metadataFile.FullName)"
    }
    $pngPath = Join-Path $resolvedEvidenceDir ($metadata.image -replace '/', [IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path -LiteralPath $pngPath)) {
        throw "Screenshot file is missing: $pngPath"
    }
    $actualHash = (Get-FileHash -LiteralPath $pngPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $metadata.sha256) {
        throw "Screenshot changed after capture: $pngPath"
    }
    $screenshots += [pscustomobject]@{
        metadata = $metadata
        pngPath = $pngPath
        sha256 = $actualHash
    }
}

if ($DryRun -and $PullRequest -eq 0) {
    if (-not $Repository) {
        $Repository = "local/dry-run"
    }
    $prData = [pscustomobject]@{
        headRefOid = $receipt.headSha
        url = "https://github.com/$Repository/pull/0"
        state = "OPEN"
    }
    $repoData = [pscustomobject]@{
        nameWithOwner = $Repository
        url = "https://github.com/$Repository"
        defaultBranchRef = [pscustomobject]@{ name = "master" }
    }
} else {
    $repoArguments = @("repo", "view", "--json", "nameWithOwner,url,defaultBranchRef")
    if ($Repository) {
        $repoArguments += $Repository
    }
    $repoData = Invoke-GhJson -Arguments $repoArguments
    $Repository = $repoData.nameWithOwner
    $prData = Invoke-GhJson -Arguments @(
        "pr", "view", $PullRequest.ToString(),
        "--repo", $Repository,
        "--json", "headRefOid,url,state"
    )
}

if ($prData.state -ne "OPEN") {
    throw "PR #$PullRequest is not open"
}
if ($prData.headRefOid -ne $receipt.headSha) {
    throw "Verified head $($receipt.headSha) does not match PR head $($prData.headRefOid). Rerun all evidence."
}

if (-not $DryRun) {
    Assert-NonEvidencePrChecksPassed -PrNumber $PullRequest -Repo $Repository
}

$remoteRoot = "pr/$PullRequest/$($receipt.headSha)"
$uploadItems = @()
$receiptHash = (Get-FileHash -LiteralPath $receiptPath -Algorithm SHA256).Hash.ToLowerInvariant()
$uploadItems += [pscustomobject]@{
    localPath = $receiptPath
    remotePath = "$remoteRoot/verification-$($receiptHash.Substring(0, 12)).json"
    kind = "receipt"
    metadata = $null
}
foreach ($screenshot in $screenshots) {
    $uploadItems += [pscustomobject]@{
        localPath = $screenshot.pngPath
        remotePath = "$remoteRoot/$($screenshot.metadata.name)-$($screenshot.sha256.Substring(0, 12)).png"
        kind = "image"
        metadata = $screenshot.metadata
    }
}

if ($DryRun) {
    $evidenceCommit = $receipt.headSha
} else {
    & gh auth status | Out-Host
    Assert-LastExitCode "Check GitHub authentication"

    $matchingRefs = @(Invoke-GhJson -Arguments @(
        "api", "repos/$Repository/git/matching-refs/heads/$EvidenceBranch"
    ))
    $exactRef = $matchingRefs | Where-Object { $_.ref -eq "refs/heads/$EvidenceBranch" } | Select-Object -First 1
    if ($exactRef) {
        $parentCommit = $exactRef.object.sha
    } else {
        $defaultRef = Invoke-GhJson -Arguments @(
            "api", "repos/$Repository/git/ref/heads/$($repoData.defaultBranchRef.name)"
        )
        $parentCommit = $defaultRef.object.sha
        Invoke-GhJson `
            -Arguments @("api", "--method", "POST", "repos/$Repository/git/refs", "--input", "-") `
            -InputObject ([ordered]@{
                ref = "refs/heads/$EvidenceBranch"
                sha = $parentCommit
            }) | Out-Null
    }

    $parentData = Invoke-GhJson -Arguments @("api", "repos/$Repository/git/commits/$parentCommit")
    $treeEntries = @()
    foreach ($item in $uploadItems) {
        $content = [Convert]::ToBase64String([IO.File]::ReadAllBytes($item.localPath))
        $blob = Invoke-GhJson `
            -Arguments @("api", "--method", "POST", "repos/$Repository/git/blobs", "--input", "-") `
            -InputObject ([ordered]@{
                content = $content
                encoding = "base64"
            })
        $treeEntries += [ordered]@{
            path = $item.remotePath
            mode = "100644"
            type = "blob"
            sha = $blob.sha
        }
    }

    $tree = Invoke-GhJson `
        -Arguments @("api", "--method", "POST", "repos/$Repository/git/trees", "--input", "-") `
        -InputObject ([ordered]@{
            base_tree = $parentData.tree.sha
            tree = $treeEntries
        })
    $commit = Invoke-GhJson `
        -Arguments @("api", "--method", "POST", "repos/$Repository/git/commits", "--input", "-") `
        -InputObject ([ordered]@{
            message = "Add emulator evidence for PR #$PullRequest ($($receipt.headSha.Substring(0, 12)))"
            tree = $tree.sha
            parents = @($parentCommit)
        })
    Invoke-GhJson `
        -Arguments @("api", "--method", "PATCH", "repos/$Repository/git/refs/heads/$EvidenceBranch", "--input", "-") `
        -InputObject ([ordered]@{
            sha = $commit.sha
            force = $false
        }) | Out-Null
    $evidenceCommit = $commit.sha
}

$marker = "<!-- musfit-emulator-evidence:v1 -->"
$tick = [char]96
$lines = @(
    $marker,
    "## MusFit emulator evidence",
    "",
    "Verified PR head $tick$($receipt.headSha)$tick on $tick$($receipt.device.avdName)$tick ($tick$($receipt.device.serial)$tick) with deterministic seed data.",
    "",
    "### Verification"
)
foreach ($check in @($receipt.checks)) {
    $lines += "- [PASS] **$($check.name):** $tick$($check.command)$tick"
}
if ($DryRun) {
    $lines += "- [DRY RUN] **GitHub PR checks:** not queried"
} else {
    $lines += "- [PASS] **GitHub PR checks:** passed before evidence publication"
}
$lines += @("", "### Scenarios")
foreach ($screenshot in $screenshots) {
    $themeLabel = if ($screenshot.metadata.theme -eq "system") { "system theme" } else { "$($screenshot.metadata.theme) mode" }
    $lines += "- **$($screenshot.metadata.caption)** ($themeLabel)"
}
$lines += ""

foreach ($item in @($uploadItems | Where-Object { $_.kind -eq "image" })) {
    $encodedPath = ConvertTo-UrlPath $item.remotePath
    $imageUrl = if ($DryRun) {
        "https://example.invalid/$Repository/blob/$evidenceCommit/${encodedPath}?raw=true"
    } else {
        "https://github.com/$Repository/blob/$evidenceCommit/${encodedPath}?raw=true"
    }
    $alt = ($item.metadata.caption -replace '[\[\]]', '')
    $lines += "### $($item.metadata.caption)"
    $lines += ""
    $lines += "![$alt]($imageUrl)"
    $lines += ""
}

$receiptItem = $uploadItems | Where-Object { $_.kind -eq "receipt" } | Select-Object -First 1
$receiptUrl = if ($DryRun) {
    "https://example.invalid/$Repository/blob/$evidenceCommit/$(ConvertTo-UrlPath $receiptItem.remotePath)"
} else {
    "https://github.com/$Repository/blob/$evidenceCommit/$(ConvertTo-UrlPath $receiptItem.remotePath)"
}
$commitUrl = if ($DryRun) {
    "https://example.invalid/$Repository/commit/$evidenceCommit"
} else {
    "https://github.com/$Repository/commit/$evidenceCommit"
}
$lines += @(
    "Evidence: [receipt]($receiptUrl) | [immutable evidence commit]($commitUrl)",
    "",
    "_Generated by $tick`$musfit-pr-emulator-evidence$tick. A new PR commit invalidates this evidence._"
)
$commentBody = $lines -join "`n"

if ($DryRun) {
    $previewPath = Join-Path $resolvedEvidenceDir "pr-comment-preview.md"
    [IO.File]::WriteAllText($previewPath, $commentBody, (New-Object Text.UTF8Encoding($false)))
    Write-Host "Dry run complete; no GitHub state changed."
    Write-Host "PR comment preview: $previewPath"
    return
}

$latestPr = Invoke-GhJson -Arguments @(
    "pr", "view", $PullRequest.ToString(),
    "--repo", $Repository,
    "--json", "headRefOid,state"
)
if ($latestPr.state -ne "OPEN" -or $latestPr.headRefOid -ne $receipt.headSha) {
    throw "The PR head changed while evidence was uploading. Rerun the complete gate and capture workflow."
}

Assert-NonEvidencePrChecksPassed -PrNumber $PullRequest -Repo $Repository

$viewer = Invoke-GhJson -Arguments @("api", "user")
$commentPages = Invoke-GhJson -Arguments @(
    "api", "--paginate", "--slurp",
    "repos/$Repository/issues/$PullRequest/comments?per_page=100"
)
$comments = @()
foreach ($page in $commentPages) {
    foreach ($entry in $page) {
        $comments += $entry
    }
}
$existing = $comments |
    Where-Object { $_.user.login -eq $viewer.login -and $_.body -like "*$marker*" } |
    Select-Object -Last 1

if ($existing) {
    $comment = Invoke-GhJson `
        -Arguments @("api", "--method", "PATCH", "repos/$Repository/issues/comments/$($existing.id)", "--input", "-") `
        -InputObject ([ordered]@{ body = $commentBody })
    Write-Host "Updated MusFit emulator evidence comment: $($comment.html_url)"
} else {
    $comment = Invoke-GhJson `
        -Arguments @("api", "--method", "POST", "repos/$Repository/issues/$PullRequest/comments", "--input", "-") `
        -InputObject ([ordered]@{ body = $commentBody })
    Write-Host "Created MusFit emulator evidence comment: $($comment.html_url)"
}

Write-Host "Evidence branch: $EvidenceBranch"
Write-Host "Evidence commit: $evidenceCommit"
