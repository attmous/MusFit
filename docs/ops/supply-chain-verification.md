# Supply-chain verification and provenance

MusFit fails closed on build inputs before producing release evidence. The
controls apply to every module and every CI/release workflow.

## Immutable inputs

- `gradle/wrapper/gradle-wrapper.properties` pins the official Gradle 9.4.1
  binary distribution SHA-256.
- `gradle/verification-metadata.xml` enables Gradle's global dependency
  verification with SHA-256 entries for plugin, metadata, direct, and
  transitive artifacts. A missing or mismatched artifact fails resolution.
- Every GitHub Action reference is a full 40-character commit SHA. The adjacent
  version comment is informational; automation executes only the immutable SHA.
- `gradle/actions/setup-gradle` validates the committed wrapper JAR before each
  Gradle invocation.

Dependency updates must run the complete normal verification first. Then update
metadata explicitly and review every changed coordinate and checksum:

```powershell
. .\scripts\android\android-env.ps1
.\gradlew.bat --write-verification-metadata sha256 cyclonedxBom --no-daemon --console=plain
.\scripts\dev\verify-musfit.ps1 -Preset Full
```

Never use permissive or `off` dependency-verification mode in CI or release
workflows. Never accept a checksum only to make a failed build green; verify the
coordinate/version and its authoritative repository first.

## SBOM and exact-commit artifact set

CycloneDX Gradle plugin 3.2.4 emits an aggregate CycloneDX 1.6 JSON SBOM. The
Android workflow copies the unsigned production APK, production AAB, SBOM, and
commit-bound metadata into one artifact set. `SHA256SUMS` covers every file in
that set, and the verifier requires the exact filename set, byte sizes, hashes,
SBOM format, and full commit SHA.

The aggregate SBOM task runs in its own Gradle invocation. CycloneDX resolves
all module configurations while constructing the aggregate and therefore must
not mutate the same task graph after Android variants have been consumed.

The source contract includes two deliberate negative fixtures:

- a mutable action reference such as `actions/checkout@v4` is rejected;
- a dependency fixture whose bytes no longer match its trusted SHA-256 is rejected;
- changing one byte of a checksummed artifact is rejected.

## Signed attestations

On default-branch pushes, the provenance job downloads and reverifies the exact
artifact set produced by the successful Android job. GitHub's official
`actions/attest` action uses short-lived OIDC/Sigstore credentials to publish:

- SLSA build provenance for the checksummed artifact set;
- an SBOM attestation using `musfit.cdx.json`.

The production release promotion job separately attests the final Google-signed
universal APK and upload-signed AAB from the already-verified candidate
`SHA256SUMS`. Promotion still downloads that immutable candidate and never
rebuilds it.

Verify a published artifact locally with GitHub CLI:

```powershell
gh attestation verify .\musfit-universal.apk --repo attmous/MusFit
```

Attestation permissions exist only on default-branch provenance and protected
release jobs. Pull requests can generate and verify checksums/SBOMs but cannot
mint signed repository attestations.
