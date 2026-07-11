# Development Auto-Update With Obtainium

The current development lane publishes a debug APK after a successful push to
`master`. Obtainium can detect that GitHub Release without USB. Standard Android
still asks the user to confirm installation unless the device has separately
configured privileged/device-owner support.

This is a developer convenience, not a production release channel.

## Current Flow

```text
push to master
  -> GitHub Actions verify
       -> workflow contract
       -> unit tests + lint + target/instrumentation APK compilation
       -> musfit-debug-apk workflow artifact
  -> GitHub Actions publish
       -> build-<versionCode> GitHub Release
       -> musfit-<versionCode>.apk
  -> Obtainium detects the newer release and offers it for installation
```

Relevant source:

- `.github/workflows/android.yml`
- `app/build.gradle.kts`
- `app/keystore/musfit.debug.keystore`

The development update lane works because:

- Every debug build in this lane uses the same committed public development
  signing key, so a CI APK can update a locally installed debug APK without an
  uninstall.
- `versionCode` comes from the full Git commit count. CI uses `fetch-depth: 0`,
  so each merge to `master` advances it monotonically.

The APK is debuggable, unminified, signed with a public key, and includes
debug-only behavior. It is not functionally or operationally equivalent to a
production build. Do not reuse the debug signing configuration for a store or
production release; private signing, install migration, shrinking, and verified
APK/AAB publication are tracked in the architecture remediation backlog.

The legacy exported seed receiver tracked as SEC-001 has been removed. The APK
published by this lane contains no seed component or seed action. Deterministic
development seeding is separate: `install-seed-musfit.ps1` installs the
instrumentation APK only on the named dedicated emulator and invokes the Android
test runner. The public signing key, debuggability, and lack of a
production-shaped, signed distribution lane still make this a developer channel
rather than a production-safe one.

## One-Time Obtainium Setup

1. Install [Obtainium](https://github.com/ImranR98/Obtainium/releases) and grant
   its requested unknown-app install permission.
2. Add the MusFit GitHub repository URL.
3. Select GitHub as the source. The current artifact is one universal APK, so
   architecture filtering is unnecessary.
4. If the repository is private, configure a read-only GitHub token in
   Obtainium's source settings.
5. Install the detected `build-<n>` release. Later checks can discover newer
   builds automatically, but installation normally still requires confirmation.

Fully silent installation requires separate privileged/device-owner, Shizuku,
or root configuration. That is outside this repository's update workflow.

## Triggering And Artifacts

- A push to `master` runs verification and, if successful, creates the GitHub
  Release consumed by Obtainium.
- `workflow_dispatch` runs verification and uploads the workflow artifact, but
  the current `publish` condition is push-only; manual dispatch does not create a
  GitHub Release.
- Pull requests and pushes to `main` run the debug verification path but do not
  publish the Obtainium release.

## Operational Notes

- Do not replace `app/keystore/musfit.debug.keystore` casually. A different key
  cannot update phones installed with the old key without an uninstall or an
  explicit signing-key migration.
- A full Git history is required for the real commit-count `versionCode`.
- Downgrades do not install as normal updates. Use an explicit adb downgrade only
  on a development device and only after considering schema compatibility.
- Treat the GitHub Release and workflow artifact as debug developer artifacts,
  not Play-ready or production-safe binaries.
