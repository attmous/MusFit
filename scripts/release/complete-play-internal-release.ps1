param(
    [long] $VersionCode,
    [string] $PackageName = "com.musfit",
    [switch] $SelfTest
)

$ErrorActionPreference = "Stop"
function Select-DraftRelease([object[]] $Releases, [long] $ExpectedVersionCode) {
    @($Releases | Where-Object {
        $_.status -ceq "draft" -and
        @($_.versionCodes).Count -eq 1 -and
        [long]$_.versionCodes[0] -eq $ExpectedVersionCode
    })
}

if ($SelfTest) {
    $fixture = @(
        [pscustomobject]@{ status = "completed"; versionCodes = @("10") },
        [pscustomobject]@{ status = "draft"; versionCodes = @("11") }
    )
    if (@(Select-DraftRelease $fixture 11).Count -ne 1) { throw "Draft completion selection self-test failed." }
    if (@(Select-DraftRelease $fixture 10).Count -ne 0) { throw "Completed release rejection self-test failed." }
    $multi = @([pscustomobject]@{ status = "draft"; versionCodes = @("11", "12") })
    if (@(Select-DraftRelease $multi 11).Count -ne 0) { throw "Multi-version draft rejection self-test failed." }
    Write-Host "Play internal completion self-test passed."
    return
}

if ($VersionCode -le 0) { throw "VersionCode must be positive." }
if ($PackageName -cne "com.musfit") { throw "W1-REL-04 may complete only com.musfit." }
if (-not $env:MUSFIT_PLAY_ACCESS_TOKEN) { throw "MUSFIT_PLAY_ACCESS_TOKEN is required." }

$headers = @{ Authorization = "Bearer $($env:MUSFIT_PLAY_ACCESS_TOKEN)" }
$api = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PackageName"
$edit = Invoke-RestMethod -Method Post -Uri "$api/edits" -Headers $headers -ContentType "application/json" -Body "{}"
if ([string]::IsNullOrWhiteSpace($edit.id)) { throw "Google Play did not return a completion edit id." }
$track = Invoke-RestMethod -Method Get -Uri "$api/edits/$($edit.id)/tracks/internal" -Headers $headers
$target = @(Select-DraftRelease @($track.releases) $VersionCode)
if ($target.Count -ne 1) { throw "Internal track must contain exactly one release for version $VersionCode." }
$target[0].status = "completed"
$body = @{ track = "internal"; releases = @($track.releases) } | ConvertTo-Json -Depth 10
Invoke-RestMethod -Method Put -Uri "$api/edits/$($edit.id)/tracks/internal" -Headers $headers `
    -ContentType "application/json" -Body $body | Out-Null
Invoke-RestMethod -Method Post -Uri "$api/edits/$($edit.id):commit" -Headers $headers `
    -ContentType "application/json" -Body "{}" | Out-Null
Write-Host "Verified Play internal release completed for version $VersionCode."
