[CmdletBinding()]
param(
    [string] $Repository = $env:GITHUB_REPOSITORY,
    [ValidateRange(0, 2147483647)]
    [int] $PullRequest = 0,
    [string] $Token = $env:GITHUB_TOKEN,
    [string] $FixturePath = "",
    [switch] $SetCommitStatus,
    [switch] $SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$evidenceMarker = "<!-- musfit-emulator-evidence:v1 -->"
$statusContext = "MusFit emulator evidence"

function Get-PropertyValue([object] $Object, [string] $Name) {
    if ($null -eq $Object) {
        return $null
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    $property.Value
}

function ConvertTo-UrlPath([string] $Path) {
    (($Path -split '/') | ForEach-Object { [Uri]::EscapeDataString($_) }) -join '/'
}

function Test-RuntimePath([string] $Path) {
    $normalized = $Path -replace '\\', '/'

    if ($normalized -match '^app/src/(?:test|androidTest|testFixtures|[^/]+Test)/') {
        return $false
    }
    if ($normalized -match '^app/src/[^/]+/') {
        return $true
    }
    if ($normalized -match '^(?:app/)?build\.gradle(?:\.kts)?$') {
        return $true
    }
    if ($normalized -match '^(?:settings\.gradle(?:\.kts)?|gradle\.properties|gradle/libs\.versions\.toml|app/proguard-rules\.pro)$') {
        return $true
    }
    if ($normalized -match '^buildSrc/') {
        return $true
    }

    $false
}

function Invoke-GitHubApi(
    [ValidateSet("GET", "POST")]
    [string] $Method,
    [string] $Path,
    [string] $ApiToken,
    [object] $Body = $null
) {
    $headers = @{
        Accept = "application/vnd.github+json"
        Authorization = "Bearer $ApiToken"
        "X-GitHub-Api-Version" = "2022-11-28"
    }
    $arguments = @{
        Method = $Method
        Uri = "https://api.github.com$Path"
        Headers = $headers
    }
    if ($null -ne $Body) {
        $arguments.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
        $arguments.ContentType = "application/json"
    }
    Invoke-RestMethod @arguments
}

function Get-GitHubPages([string] $Path, [string] $ApiToken) {
    $items = @()
    for ($page = 1; ; $page++) {
        $separator = if ($Path.Contains('?')) { '&' } else { '?' }
        $rawResponse = Invoke-GitHubApi -Method GET -Path "$Path${separator}per_page=100&page=$page" -ApiToken $ApiToken
        $response = @()
        foreach ($item in $rawResponse) {
            $response += $item
            $items += $item
        }
        if ($response.Count -lt 100) {
            break
        }
    }
    foreach ($item in $items) {
        $item
    }
}

function Get-Sha256Hex([byte[]] $Bytes) {
    $algorithm = [Security.Cryptography.SHA256]::Create()
    try {
        ([BitConverter]::ToString($algorithm.ComputeHash($Bytes)) -replace '-', '').ToLowerInvariant()
    } finally {
        $algorithm.Dispose()
    }
}

function Test-ArtifactHash([string] $Path, [byte[]] $Bytes) {
    $match = [regex]::Match($Path, '-(?<prefix>[0-9a-f]{12})\.(?:png|json)$', [Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $match.Success) {
        return $false
    }
    (Get-Sha256Hex $Bytes).StartsWith($match.Groups['prefix'].Value, [StringComparison]::OrdinalIgnoreCase)
}

function Test-PngArtifact([string] $Path, [byte[]] $Bytes) {
    if (-not (Test-ArtifactHash $Path $Bytes)) {
        return $false
    }
    $signature = @(137, 80, 78, 71, 13, 10, 26, 10)
    if ($Bytes.Length -le $signature.Count) {
        return $false
    }
    for ($index = 0; $index -lt $signature.Count; $index++) {
        if ($Bytes[$index] -ne $signature[$index]) {
            return $false
        }
    }
    $true
}

function Test-ReceiptArtifact([string] $Path, [byte[]] $Bytes, [string] $HeadSha) {
    if (-not (Test-ArtifactHash $Path $Bytes)) {
        return $false
    }
    try {
        $receipt = [Text.Encoding]::UTF8.GetString($Bytes) | ConvertFrom-Json
    } catch {
        return $false
    }
    if (
        [int](Get-PropertyValue $receipt 'schemaVersion') -ne 1 -or
        [string](Get-PropertyValue $receipt 'status') -ne 'passed' -or
        [string](Get-PropertyValue $receipt 'headSha') -ne $HeadSha
    ) {
        return $false
    }
    $checks = @()
    foreach ($check in (Get-PropertyValue $receipt 'checks')) {
        $checks += $check
    }
    if ($checks.Count -eq 0 -or @($checks | Where-Object { [string](Get-PropertyValue $_ 'status') -ne 'passed' }).Count -gt 0) {
        return $false
    }
    $checkNames = @($checks | ForEach-Object { [string](Get-PropertyValue $_ 'name') })
    foreach ($requiredCheck in @('Full Gradle gate', 'Seeded emulator install', 'Foreground launch')) {
        if ($requiredCheck -notin $checkNames) {
            return $false
        }
    }
    $device = Get-PropertyValue $receipt 'device'
    if (
        $null -eq $device -or
        [string](Get-PropertyValue $device 'serial') -notmatch '^emulator-\d+$' -or
        [string](Get-PropertyValue $device 'avdName') -ne 'MusFit_API36' -or
        [string](Get-PropertyValue $device 'packageName') -ne 'com.musfit.internal' -or
        -not [bool](Get-PropertyValue $device 'seeded')
    ) {
        return $false
    }
    $true
}

function Find-ValidEvidenceComment(
    [string] $Repo,
    [int] $PrNumber,
    [string] $HeadSha,
    [object[]] $Comments,
    [scriptblock] $ReadArtifact,
    [scriptblock] $EvidenceCommitReachable
) {
    $repositoryPattern = [regex]::Escape($Repo)
    $headPattern = [regex]::Escape($HeadSha)
    $rootPattern = "https://github\.com/$repositoryPattern/blob/(?<commit>[0-9a-f]{40})/pr/$PrNumber/$headPattern/"
    $imagePattern = "$rootPattern(?<suffix>[^)\s]+\.png)\?raw=true"
    $receiptPattern = "$rootPattern(?<suffix>verification-[0-9a-f]{12}\.json)\)"
    $tick = [char] 96
    $verifiedText = "Verified PR head $tick$HeadSha$tick"
    $expectedPublisher = @($Repo -split '/', 2)[0]

    for ($index = $Comments.Count - 1; $index -ge 0; $index--) {
        $comment = $Comments[$index]
        $body = [string](Get-PropertyValue $comment "body")
        $commentUser = Get-PropertyValue $comment "user"
        $publisher = [string](Get-PropertyValue $commentUser "login")
        if (
            -not $body.Contains($evidenceMarker) -or
            -not $body.Contains($verifiedText) -or
            $publisher -ne $expectedPublisher
        ) {
            Write-Verbose "Ignoring comment without the exact marker, head, and repository-owner publisher."
            continue
        }

        $imageMatches = [regex]::Matches($body, $imagePattern, [Text.RegularExpressions.RegexOptions]::IgnoreCase)
        $receiptMatch = [regex]::Match($body, $receiptPattern, [Text.RegularExpressions.RegexOptions]::IgnoreCase)
        if ($imageMatches.Count -eq 0 -or -not $receiptMatch.Success) {
            Write-Verbose "Ignoring evidence comment without immutable screenshot and receipt links."
            continue
        }

        $evidenceCommit = $receiptMatch.Groups["commit"].Value.ToLowerInvariant()
        if (-not (& $EvidenceCommitReachable $evidenceCommit)) {
            Write-Verbose "Ignoring evidence commit that is not reachable from pr-evidence: $evidenceCommit"
            continue
        }
        $receiptPath = "pr/$PrNumber/$HeadSha/$($receiptMatch.Groups['suffix'].Value)"
        $receiptBytes = & $ReadArtifact $evidenceCommit $receiptPath
        if ($null -eq $receiptBytes -or -not (Test-ReceiptArtifact $receiptPath ([byte[]] $receiptBytes) $HeadSha)) {
            Write-Verbose "Ignoring evidence comment with a missing or invalid receipt: $receiptPath"
            continue
        }

        $imagePaths = @()
        $allImagesValid = $true
        foreach ($imageMatch in $imageMatches) {
            if ($imageMatch.Groups["commit"].Value.ToLowerInvariant() -ne $evidenceCommit) {
                Write-Verbose "Ignoring evidence comment that mixes immutable evidence commits."
                $allImagesValid = $false
                break
            }
            $imagePath = "pr/$PrNumber/$HeadSha/$($imageMatch.Groups['suffix'].Value)"
            $imageBytes = & $ReadArtifact $evidenceCommit $imagePath
            if ($null -eq $imageBytes -or -not (Test-PngArtifact $imagePath ([byte[]] $imageBytes))) {
                Write-Verbose "Ignoring evidence comment with a missing or invalid PNG: $imagePath"
                $allImagesValid = $false
                break
            }
            $imagePaths += $imagePath
        }
        if (-not $allImagesValid) {
            continue
        }

        return [pscustomobject]@{
            CommentUrl = [string](Get-PropertyValue $comment "html_url")
            EvidenceCommit = $evidenceCommit
            ReceiptPath = $receiptPath
            ImagePaths = @($imagePaths)
        }
    }

    $null
}

function Get-EvidenceEvaluation(
    [string] $Repo,
    [int] $PrNumber,
    [string] $HeadSha,
    [object[]] $Files,
    [object[]] $Comments,
    [scriptblock] $ReadArtifact,
    [scriptblock] $EvidenceCommitReachable,
    [string] $PullRequestUrl
) {
    $runtimeFiles = @($Files | ForEach-Object {
        $filename = [string](Get-PropertyValue $_ "filename")
        if ($filename) {
            $filename
        }
        $previousFilename = [string](Get-PropertyValue $_ "previous_filename")
        if ($previousFilename) {
            $previousFilename
        }
    } | Where-Object { Test-RuntimePath $_ } | Sort-Object -Unique)

    if ($runtimeFiles.Count -eq 0) {
        return [pscustomobject]@{
            Required = $false
            Passed = $true
            HeadSha = $HeadSha
            RuntimeFiles = @()
            Description = "Not required: no Android runtime changes"
            TargetUrl = $PullRequestUrl
            Evidence = $null
        }
    }

    $evidence = Find-ValidEvidenceComment `
        -Repo $Repo `
        -PrNumber $PrNumber `
        -HeadSha $HeadSha `
        -Comments $Comments `
        -ReadArtifact $ReadArtifact `
        -EvidenceCommitReachable $EvidenceCommitReachable

    if ($null -eq $evidence) {
        return [pscustomobject]@{
            Required = $true
            Passed = $false
            HeadSha = $HeadSha
            RuntimeFiles = @($runtimeFiles)
            Description = "Required exact-head emulator evidence is missing"
            TargetUrl = $PullRequestUrl
            Evidence = $null
        }
    }

    [pscustomobject]@{
        Required = $true
        Passed = $true
        HeadSha = $HeadSha
        RuntimeFiles = @($runtimeFiles)
        Description = "Exact-head emulator evidence is published"
        TargetUrl = $evidence.CommentUrl
        Evidence = $evidence
    }
}

function Set-GitHubCommitStatus(
    [string] $Repo,
    [string] $HeadSha,
    [bool] $Passed,
    [string] $Description,
    [string] $TargetUrl,
    [string] $ApiToken
) {
    $state = if ($Passed) { "success" } else { "failure" }
    Invoke-GitHubApi `
        -Method POST `
        -Path "/repos/$Repo/statuses/$HeadSha" `
        -ApiToken $ApiToken `
        -Body ([ordered]@{
            state = $state
            context = $statusContext
            description = $Description
            target_url = $TargetUrl
        }) | Out-Null
}

function Assert-SelfTest([bool] $Condition, [string] $Message) {
    if (-not $Condition) {
        throw "PR emulator evidence self-test failed: $Message"
    }
}

function Invoke-SelfTest {
    $head = "1111111111111111111111111111111111111111"
    $evidenceCommit = "2222222222222222222222222222222222222222"
    $repo = "example/MusFit"
    $pr = 123
    $tick = [char] 96
    $pngBytes = [byte[]] @(137, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3, 4)
    $imageHash = Get-Sha256Hex $pngBytes
    $imagePath = "pr/$pr/$head/01-today-$($imageHash.Substring(0, 12)).png"
    $receiptObject = [ordered]@{
        schemaVersion = 1
        status = "passed"
        headSha = $head
        checks = @(
            [ordered]@{ name = "Full Gradle gate"; status = "passed" },
            [ordered]@{ name = "Seeded emulator install"; status = "passed" },
            [ordered]@{ name = "Foreground launch"; status = "passed" }
        )
        device = [ordered]@{
            serial = "emulator-5554"
            avdName = "MusFit_API36"
            packageName = "com.musfit.internal"
            seeded = $true
        }
    }
    $receiptBytes = [Text.Encoding]::UTF8.GetBytes(($receiptObject | ConvertTo-Json -Depth 8 -Compress))
    $receiptHash = Get-Sha256Hex $receiptBytes
    $receiptPath = "pr/$pr/$head/verification-$($receiptHash.Substring(0, 12)).json"
    $validBody = @(
        $evidenceMarker,
        "Verified PR head $tick$head$tick on a seeded emulator.",
        "![Today](https://github.com/$repo/blob/$evidenceCommit/$imagePath`?raw=true)",
        "Evidence: [receipt](https://github.com/$repo/blob/$evidenceCommit/$receiptPath)"
    ) -join "`n"
    $artifacts = @{
        "${evidenceCommit}:$imagePath" = $pngBytes
        "${evidenceCommit}:$receiptPath" = $receiptBytes
    }
    $readArtifact = {
        param([string] $Commit, [string] $Path)
        $key = "${Commit}:$Path"
        if ($artifacts.ContainsKey($key)) {
            return ,([byte[]] $artifacts[$key])
        }
        $null
    }.GetNewClosure()
    $reachable = {
        param([string] $Commit)
        $Commit -eq $evidenceCommit
    }.GetNewClosure()
    $runtimeFiles = @([pscustomobject]@{ filename = "app/src/main/java/com/musfit/ui/TodayScreen.kt" })
    $validComment = [pscustomobject]@{
        body = $validBody
        author_association = "OWNER"
        user = [pscustomobject]@{ login = "example" }
        html_url = "https://github.com/$repo/pull/$pr#issuecomment-1"
    }

    Assert-SelfTest (Test-RuntimePath "app/src/internal/AndroidManifest.xml") "internal manifest must require evidence"
    Assert-SelfTest (Test-RuntimePath "app/build.gradle.kts") "runtime Gradle changes must require evidence"
    Assert-SelfTest (-not (Test-RuntimePath "app/src/test/java/com/musfit/ExampleTest.kt")) "unit tests must not require evidence"
    Assert-SelfTest (-not (Test-RuntimePath "docs/architecture/README.md")) "docs must not require evidence"

    $valid = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files $runtimeFiles -Comments @($validComment) -ReadArtifact $readArtifact -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest ($valid.Required -and $valid.Passed) "valid exact-head evidence must pass"
    Assert-SelfTest ($valid.Evidence.ImagePaths.Count -eq 1) "valid evidence must retain its screenshot"

    $staleComment = [pscustomobject]@{
        body = $validBody.Replace($head, "3333333333333333333333333333333333333333")
        author_association = "OWNER"
        user = [pscustomobject]@{ login = "example" }
        html_url = "https://github.com/$repo/pull/$pr#issuecomment-2"
    }
    $stale = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files $runtimeFiles -Comments @($staleComment) -ReadArtifact $readArtifact -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest ($stale.Required -and -not $stale.Passed) "stale-head evidence must fail"

    $untrustedComment = [pscustomobject]@{
        body = $validBody
        author_association = "NONE"
        user = [pscustomobject]@{ login = "stranger" }
        html_url = "https://github.com/$repo/pull/$pr#issuecomment-3"
    }
    $untrusted = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files $runtimeFiles -Comments @($untrustedComment) -ReadArtifact $readArtifact -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest (-not $untrusted.Passed) "untrusted comments must fail"

    $missingArtifacts = {
        param([string] $Commit, [string] $Path)
        $null
    }
    $missing = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files $runtimeFiles -Comments @($validComment) -ReadArtifact $missingArtifacts -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest (-not $missing.Passed) "missing immutable artifacts must fail"

    $tamperedArtifacts = @{}
    foreach ($key in $artifacts.Keys) {
        $tamperedArtifacts[$key] = $artifacts[$key]
    }
    $tamperedArtifacts["${evidenceCommit}:$imagePath"] = [byte[]] @(137, 80, 78, 71, 13, 10, 26, 10, 9, 9, 9)
    $readTamperedArtifact = {
        param([string] $Commit, [string] $Path)
        $key = "${Commit}:$Path"
        if ($tamperedArtifacts.ContainsKey($key)) {
            return ,([byte[]] $tamperedArtifacts[$key])
        }
        $null
    }.GetNewClosure()
    $tampered = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files $runtimeFiles -Comments @($validComment) -ReadArtifact $readTamperedArtifact -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest (-not $tampered.Passed) "tampered screenshot hashes must fail"

    $docs = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files @([pscustomobject]@{ filename = "docs/README.md" }) -Comments @() -ReadArtifact $missingArtifacts -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest (-not $docs.Required -and $docs.Passed) "non-runtime PRs must pass without evidence"

    $runtimeRename = Get-EvidenceEvaluation -Repo $repo -PrNumber $pr -HeadSha $head -Files @([pscustomobject]@{ filename = "docs/TodayScreen.kt"; previous_filename = "app/src/main/java/com/musfit/ui/TodayScreen.kt" }) -Comments @() -ReadArtifact $missingArtifacts -EvidenceCommitReachable $reachable -PullRequestUrl "https://github.com/$repo/pull/$pr"
    Assert-SelfTest ($runtimeRename.Required -and -not $runtimeRename.Passed) "runtime renames must require evidence"

    Write-Host "PR emulator evidence self-test passed."
}

if ($SelfTest) {
    Invoke-SelfTest
    return
}

if ($FixturePath) {
    if ($SetCommitStatus) {
        throw "Fixture evaluation cannot write a GitHub commit status"
    }
    $fixture = Get-Content -LiteralPath $FixturePath -Raw | ConvertFrom-Json
    $Repository = [string] $fixture.repository
    $PullRequest = [int] $fixture.pullRequest
    $headSha = [string] $fixture.headSha
    $pullRequestUrl = [string] $fixture.pullRequestUrl
    $fixtureArtifacts = @{}
    foreach ($artifact in @($fixture.artifacts)) {
        $fixtureArtifacts["$($artifact.commit):$($artifact.path)"] = [Convert]::FromBase64String([string] $artifact.contentBase64)
    }
    $readArtifact = {
        param([string] $Commit, [string] $Path)
        $key = "${Commit}:$Path"
        if ($fixtureArtifacts.ContainsKey($key)) {
            return ,([byte[]] $fixtureArtifacts[$key])
        }
        $null
    }.GetNewClosure()
    $reachableCommits = @($fixture.reachableEvidenceCommits | ForEach-Object { [string] $_ })
    $reachable = {
        param([string] $Commit)
        $Commit -in $reachableCommits
    }.GetNewClosure()
    $evaluation = Get-EvidenceEvaluation -Repo $Repository -PrNumber $PullRequest -HeadSha $headSha -Files @($fixture.files) -Comments @($fixture.comments) -ReadArtifact $readArtifact -EvidenceCommitReachable $reachable -PullRequestUrl $pullRequestUrl
} else {
    if (-not $Repository -or $PullRequest -lt 1 -or -not $Token) {
        throw "Repository, PullRequest, and Token are required for live evaluation"
    }

    $prData = Invoke-GitHubApi -Method GET -Path "/repos/$Repository/pulls/$PullRequest" -ApiToken $Token
    if ([string] $prData.state -ne "open" -and $SetCommitStatus) {
        Write-Host "PR #$PullRequest is not open; no evidence status update is required."
        return
    }
    $headSha = [string] $prData.head.sha
    $pullRequestUrl = [string] $prData.html_url
    $files = @(Get-GitHubPages -Path "/repos/$Repository/pulls/$PullRequest/files" -ApiToken $Token)
    if ($files.Count -ne [int] $prData.changed_files) {
        throw "GitHub changed-file pagination was incomplete. Expected $($prData.changed_files), got $($files.Count)."
    }
    $comments = @(Get-GitHubPages -Path "/repos/$Repository/issues/$PullRequest/comments" -ApiToken $Token)
    $invokeApi = ${function:Invoke-GitHubApi}
    $convertPath = ${function:ConvertTo-UrlPath}
    $readArtifact = {
        param([string] $Commit, [string] $Path)
        try {
            $encodedPath = & $convertPath $Path
            $artifact = & $invokeApi -Method GET -Path "/repos/$Repository/contents/$encodedPath`?ref=$Commit" -ApiToken $Token
            if ([string] $artifact.type -ne "file" -or -not [string] $artifact.sha) {
                return $null
            }
            $blob = & $invokeApi -Method GET -Path "/repos/$Repository/git/blobs/$($artifact.sha)" -ApiToken $Token
            if ([string] $blob.encoding -ne "base64") {
                return $null
            }
            return ,([Convert]::FromBase64String(([string] $blob.content -replace '\s', '')))
        } catch {
            $null
        }
    }.GetNewClosure()
    $reachable = {
        param([string] $Commit)
        try {
            $comparison = & $invokeApi -Method GET -Path "/repos/$Repository/compare/$Commit...pr-evidence" -ApiToken $Token
            Write-Verbose "Evidence reachability comparison status: $($comparison.status)"
            ([string] $comparison.status) -in @("ahead", "identical")
        } catch {
            Write-Verbose "Evidence reachability check failed: $($_.Exception.Message)"
            $false
        }
    }.GetNewClosure()
    $evaluation = Get-EvidenceEvaluation -Repo $Repository -PrNumber $PullRequest -HeadSha $headSha -Files $files -Comments $comments -ReadArtifact $readArtifact -EvidenceCommitReachable $reachable -PullRequestUrl $pullRequestUrl
}

if ($SetCommitStatus) {
    $latestPr = Invoke-GitHubApi -Method GET -Path "/repos/$Repository/pulls/$PullRequest" -ApiToken $Token
    if ([string] $latestPr.head.sha -ne $evaluation.HeadSha) {
        throw "The PR head changed during evidence evaluation; the synchronize event must evaluate the new head"
    }
    Set-GitHubCommitStatus `
        -Repo $Repository `
        -HeadSha $evaluation.HeadSha `
        -Passed $evaluation.Passed `
        -Description $evaluation.Description `
        -TargetUrl $evaluation.TargetUrl `
        -ApiToken $Token
}

if (-not $evaluation.Passed) {
    $runtimeSummary = ($evaluation.RuntimeFiles | Select-Object -First 5) -join ', '
    throw "$($evaluation.Description). Runtime files: $runtimeSummary. Run `$musfit-pr-emulator-evidence and publish its exact-head PR comment."
}

if ($evaluation.Required) {
    Write-Host "MusFit emulator evidence passed for PR #$PullRequest at $($evaluation.HeadSha)."
    Write-Host "Evidence comment: $($evaluation.TargetUrl)"
} else {
    Write-Host "MusFit emulator evidence is not required for PR #$PullRequest."
}
