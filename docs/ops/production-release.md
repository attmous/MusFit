# Production release operations

W1-REL-04 uses Google Play App Signing **Option A**: a Google-managed app-signing key
is the permanent `com.musfit` identity. Google owns and protects its private
key. MusFit CI holds only a
replaceable upload key and authenticates to Google with short-lived workload
identity. No Play service-account JSON or production app-signing private key is
stored in GitHub.

## Artifact trust chain

```text
verified commit contained in origin/master
  -> successful push-triggered Android workflow for that exact SHA
  -> protected production-release environment
  -> full gate rerun and one optimized production AAB build
  -> copy signed with the replaceable Play upload key
  -> upload to a Play internal-track draft (not served)
  -> generatedapks.list selects the approved Google certificate
  -> generatedapks.download returns Google's signed universal APK
  -> APK/AAB certificate, package, manifest, commit and checksums verified
  -> one immutable candidate artifact
  -> second Play edit completes that exact verified internal version
  -> promotion job downloads that candidate without rebuilding
  -> GitHub Release exposes APK, AAB, SHA256SUMS and release-metadata.json
```

The GitHub Release APK is the Google-signed universal APK and is the only file
Obtainium may install. The locally built production APK and the upload-key-signed
AAB are never mislabeled as the app-signing artifact. The legacy migration
bridge and internal APK are never release assets.

## Required protected environment

Create a GitHub environment named `production-release`. Require an explicit
reviewer for deployment. Configure only these values:

| Kind | Name | Purpose |
| --- | --- | --- |
| Secret | `MUSFIT_UPLOAD_KEYSTORE_B64` | Base64 PKCS12 upload keystore |
| Secret | `MUSFIT_UPLOAD_STORE_PASSWORD` | Upload-keystore password |
| Secret | `MUSFIT_UPLOAD_KEY_PASSWORD` | Upload-key password |
| Variable | `MUSFIT_UPLOAD_KEY_ALIAS` | Nonsecret upload alias |
| Variable | `MUSFIT_UPLOAD_CERT_SHA256` | Approved upload certificate |
| Variable | `MUSFIT_PLAY_APP_SIGNING_CERT_SHA256` | Google-managed app-signing certificate |
| Variable | `PLAY_WORKLOAD_IDENTITY_PROVIDER` | Google workload identity provider resource |
| Variable | `PLAY_SERVICE_ACCOUNT` | Service account authorized in Play Console |

The workload identity provider must restrict the GitHub subject to this
repository and the production release workflow/environment. The Play service
account gets only the release permission required for `com.musfit`; do not grant
account administration, financial, or user-management permissions.

Back up the upload keystore outside GitHub using an account separate from the
Play Console owner. It is replaceable in Play Console, but losing every copy
still interrupts uploads until reset. Never commit it, place it in
`local.properties`, or attach it to PR evidence.

## First publication

1. Create `com.musfit` in Play Console as an app, free, English (United States).
   Turn off Play automatic protection for this app: Obtainium installs the
   verified Google-signed universal APK outside Play, and that supported
   sideload path must not be redirected back to the Play Store.
2. Accept the Play terms personally; an automation account must not attest to
   program-policy or export-law declarations for the owner.
3. Create the first internal release and allow Google to generate the app-signing
   key. Do not provide the public development key or the upload key as the
   permanent app-signing key.
4. Record Google's app-signing SHA-256 and the upload certificate SHA-256 in the
   protected environment variables.
5. Authorize the workload-identity service account in Play Console.
6. Wait for the exact master commit's `Android` push workflow to pass.
7. Dispatch **Production release** with the full commit SHA and the expected tag
   `v0.1.0.<versionCode>`.
8. Approve the protected environment only after checking the SHA, workflow run,
   internal track, and expected certificates.
9. Install the downloaded candidate on clean API 28 and API 37 test emulators.
   For a migrated user, follow `signing-install-migration.md`; do not overwrite a
   debug-signed data install with a different certificate.

The first package is restricted in code to Play's `internal` track. Expanding
to alpha, beta, or production is a separate reviewed change after the internal
artifact, store declarations, data-safety form, tester access, and migration
instructions are accepted.

## Verification and rollback

The candidate verifier requires:

- a valid AAB signature matching the upload certificate;
- a valid APK signature matching Google's app-signing certificate;
- distinct upload and app-signing certificates;
- package `com.musfit`, non-debuggable manifest, no local-network permission;
- release metadata bound to the exact verified commit;
- matching APK/AAB hashes in both `release-metadata.json` and `SHA256SUMS`.

The first Play edit always commits the candidate as `draft`, which is not served
to testers. If download, certificate, manifest, version, or checksum verification
fails, leave that version draft and do not run the completion edit or create a
GitHub Release. Delete or supersede the draft in Play Console after preserving
the failure evidence.

The upload edit reads the current internal track and preserves all existing
completed releases. It refuses to add a candidate while any draft already
exists or when the candidate version is already present. Completion accepts
only a single-version draft whose version code exactly matches the verified
candidate; it never replaces or mutates another release.

Only after verification and immutable candidate upload does a second Play edit
change that exact version from `draft` to `completed`. If completion succeeds but
GitHub promotion fails, do not rebuild or re-upload: download the retained
`musfit-production-candidate-<commit>` artifact, rerun
`verify-release-candidate.ps1`, and create the missing tag/release from those
exact files. Alternatively halt the internal release in a new Play edit until
promotion is repaired. Never swap a release asset in place; corrections use a
new version code and tag.
