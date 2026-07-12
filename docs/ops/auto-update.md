# Development Update And Publication Status

Normal CI publishes no production artifact. The separate, manually dispatched,
protected `Production release` workflow implements the W1-REL-04 Option A lane:
Google owns the permanent app-signing key, CI signs only the Play upload AAB,
and Obtainium receives only the universal APK returned by Google Play.

## Current CI Flow

```text
pull request or push to master/main
  -> GitHub Actions verify
       -> workflow contract
       -> enabled-variant/task-matrix contract
       -> internalDebug unit + lint + APK + instrumentation APK
       -> productionRelease unit + lint + unsigned APK/AAB
       -> musfit-internal-debug-apk workflow artifact (7-day retention)
  -> no GitHub Release or Obtainium publication

manual Production release dispatch for an exact verified master commit
  -> production-release environment approval
  -> full gate and one optimized AAB build
  -> upload-key signature and unserved Play internal draft
  -> Google-signed universal APK download and certificate verification
  -> immutable APK/AAB/checksum/metadata candidate
  -> exact verified Play version completed for internal testers
  -> promotion without rebuilding to GitHub Release / Obtainium
```

Relevant source:

- `.github/workflows/android.yml`
- `app/build.gradle.kts`
- `app/keystore/musfit.debug.keystore`

The internal lane uses application id `com.musfit.internal` and the committed
public development key. It can install beside `com.musfit`; it cannot overwrite
the production identity. Its APK is debuggable, unminified, and contains
internal-only LAN/developer defaults. The workflow artifact exists only for
bounded verification and is not a production release.

The `productionRelease` variant uses application id `com.musfit`, is
non-debuggable, leaves developer Hermes fields blank, omits internal LAN/debug
network resources, and produces unsigned APK/AAB outputs during normal CI. The
release workflow signs an exact AAB copy with the replaceable upload key; the
locally built APK is never promoted.

The legacy exported seed receiver tracked as SEC-001 has been removed.
Deterministic development seeding installs the separate
`com.musfit.internal.test` instrumentation APK only on the named dedicated
emulator and targets `com.musfit.internal`; the target APK exposes no seed
component or action.

## Publication boundary

Existing `com.musfit` developer installations are signed with the public debug
certificate. A secure production key cannot update those installs, and the
public key must never become a production key. W1-REL-02 uses the approved
encrypted export/import path documented in
[`signing-install-migration.md`](signing-install-migration.md). W1-SEC-02/03 harden the network and credential boundaries,
W1-REL-03 enables shrinking, and W1-REL-04 owns secret-backed signing and exact
artifact promotion.

Outside the protected release workflow:

- do not create a GitHub Release from `internalDebug` or unsigned production
  outputs;
- do not point Obtainium at the seven-day Actions artifact;
- do not reuse `app/keystore/musfit.debug.keystore` for production;
- do not install an ephemeral verification-signed production APK over an
  existing `com.musfit` user-data install;
- keep upload keys and store credentials outside the repository; the Google-managed
  production app-signing private key never enters MusFit CI.

See [`production-release.md`](production-release.md) for protected environment,
certificate, Play API, exact-artifact, and rollback procedures.

## Local Verification Only

The production APK may be signed with a freshly generated ephemeral key under
the gitignored `verification/` directory solely for clean disposable-emulator
install evidence. That key and signed copy must not be committed, reused, or
published. The canonical developer seed path remains:

```powershell
.\scripts\android\install-seed-musfit.ps1 -Reset -DeviceSerial emulator-5554
```

This installs and seeds `com.musfit.internal` through instrumentation; it never
resets or seeds a physical device.
