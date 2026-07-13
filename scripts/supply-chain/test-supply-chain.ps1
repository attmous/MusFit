param([switch] $SelfTest)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path

function Assert-PinnedActions([string] $Text) {
    $matches = [regex]::Matches($Text, '(?m)^\s*uses:\s*([^\s@]+)@([^\s#]+)')
    if ($matches.Count -eq 0) { throw "No GitHub Actions references were found." }
    foreach ($match in $matches) {
        if ($match.Groups[2].Value -notmatch '^[0-9a-f]{40}$') {
            throw "Mutable or invalid action reference: $($match.Value.Trim())"
        }
    }
}

function Assert-Sha256([string] $Path, [string] $Expected) {
    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -cne $Expected) { throw "Dependency checksum mismatch: $Path" }
}

$workflowText = Get-ChildItem -LiteralPath (Join-Path $repoRoot ".github\workflows") -Filter "*.yml" -File |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw } | Out-String
Assert-PinnedActions $workflowText

$wrapper = Get-Content -LiteralPath (Join-Path $repoRoot "gradle\wrapper\gradle-wrapper.properties") -Raw
if ($wrapper -notmatch '(?m)^distributionSha256Sum=[0-9a-f]{64}\r?$') { throw "Gradle distribution SHA-256 is not pinned." }

$verificationPath = Join-Path $repoRoot "gradle\verification-metadata.xml"
if (-not (Test-Path -LiteralPath $verificationPath -PathType Leaf)) { throw "Gradle verification metadata is missing." }
[xml]$verification = Get-Content -LiteralPath $verificationPath -Raw
$namespace = New-Object Xml.XmlNamespaceManager($verification.NameTable)
$namespace.AddNamespace("v", "https://schema.gradle.org/dependency-verification")
if ($verification.SelectNodes("//v:component", $namespace).Count -lt 500) { throw "Dependency verification component set is unexpectedly small." }
if ($verification.SelectNodes("//v:sha256", $namespace).Count -lt 1000) { throw "Dependency verification checksum set is unexpectedly small." }

$catalog = Get-Content -LiteralPath (Join-Path $repoRoot "gradle\libs.versions.toml") -Raw
if ($catalog -notmatch 'cyclonedx\s*=\s*"3\.2\.4"' -or $catalog -notmatch 'id\s*=\s*"org\.cyclonedx\.bom"') {
    throw "CycloneDX 3.2.4 is not pinned in the version catalog."
}
if ($workflowText -notmatch 'actions/attest@[0-9a-f]{40}' -or $workflowText -notmatch 'musfit\.cdx\.json') {
    throw "Signed provenance/SBOM publication is not wired into CI."
}

if ($SelfTest) {
    $mutableRejected = $false
    try { Assert-PinnedActions "    uses: actions/checkout@v4" } catch { $mutableRejected = $true }
    if (-not $mutableRejected) { throw "Mutable action reference self-test did not fail." }

    $temp = Join-Path ([IO.Path]::GetTempPath()) ("musfit-supply-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $temp | Out-Null
    try {
        $dependency = Join-Path $temp "dependency-fixture.jar"
        Set-Content -LiteralPath $dependency -Value "trusted dependency fixture" -Encoding ascii
        $dependencyHash = (Get-FileHash -LiteralPath $dependency -Algorithm SHA256).Hash.ToLowerInvariant()
        Assert-Sha256 $dependency $dependencyHash
        Add-Content -LiteralPath $dependency -Value "tampered dependency"
        $dependencyRejected = $false
        try { Assert-Sha256 $dependency $dependencyHash } catch { $dependencyRejected = $true }
        if (-not $dependencyRejected) { throw "Tampered dependency self-test did not fail." }

        $apk = Join-Path $temp "input.apk"
        $aab = Join-Path $temp "input.aab"
        $sbom = Join-Path $temp "bom.json"
        Set-Content -LiteralPath $apk -Value "apk fixture" -Encoding ascii
        Set-Content -LiteralPath $aab -Value "aab fixture" -Encoding ascii
        '{"bomFormat":"CycloneDX","specVersion":"1.6","components":[{"name":"fixture"}]}' |
            Set-Content -LiteralPath $sbom -Encoding utf8
        $sha = "a" * 40
        & (Join-Path $PSScriptRoot "write-verified-build-metadata.ps1") -CommitSha $sha -OutputDirectory $temp -Apk $apk -Aab $aab -Sbom $sbom
        & (Join-Path $PSScriptRoot "verify-verified-build-metadata.ps1") -CommitSha $sha -ArtifactDirectory $temp
        Add-Content -LiteralPath (Join-Path $temp "musfit-production-unsigned.apk") -Value "tampered"
        $tamperRejected = $false
        try {
            & (Join-Path $PSScriptRoot "verify-verified-build-metadata.ps1") -CommitSha $sha -ArtifactDirectory $temp
        } catch { $tamperRejected = $true }
        if (-not $tamperRejected) { throw "Tampered artifact self-test did not fail." }
    } finally {
        if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
    }
}

Write-Host "Supply-chain source and tamper checks passed."
