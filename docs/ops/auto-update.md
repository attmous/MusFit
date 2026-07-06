# Auto-updating the app on your phone (no USB)

Goal: every push to `master` produces a build that lands on the dev phone automatically,
with no USB cable and no manual "install" tap beyond the one-time setup below.

## How it works

```
push to master
   └─ GitHub Actions (.github/workflows/android.yml)
        ├─ verify job   → tests + lint + assembleDebug → uploads app-debug.apk
        └─ publish job  → creates a GitHub Release "build-<n>" with musfit-<n>.apk
                              │
                              ▼
        Obtainium on the phone polls the repo's Releases,
        sees a higher versionCode, downloads and installs it.
```

Two things make this reliable:

- **Stable signing.** Every build (local *and* CI) is signed with the committed keystore
  at [`app/keystore/musfit.debug.keystore`](../../app/keystore/musfit.debug.keystore) — a
  copy of the standard Android debug key. Android only lets an update install over an
  existing app if the signature matches, so a single shared key is mandatory. Because it's
  the *same* debug key this machine already flashes with, the first OTA update installs over
  your current app with **no uninstall and no data loss**. The credentials are the public
  Android debug defaults (`storepass`/`keypass` = `android`, alias `androiddebugkey`); it's a
  debug-only key, safe to commit.
- **Monotonic versionCode.** `versionCode` is the git commit count (see `gitCommitCount()` in
  [`app/build.gradle.kts`](../../app/build.gradle.kts)). Every merge to master raises it, and
  Android only installs an APK whose `versionCode` is *higher* than what's installed. CI checks
  out with `fetch-depth: 0` so the count is real (a shallow clone would make it 1).

The distributed build is the **debug** variant (what you already flash). It's larger and
unminified but functionally identical — no ProGuard/R8 rules to maintain. To switch to a
release variant later, add a `release` buildType that reuses the same `signingConfig` and have
the `publish` job build/attach `assembleRelease` instead.

## One-time phone setup (Obtainium)

1. Install **Obtainium** (FOSS): from [GitHub Releases](https://github.com/ImranR98/Obtainium/releases)
   or F-Droid / the IzzyOnDroid repo. Grant it permission to install unknown apps when prompted.
2. In Obtainium → **Add App**, paste the MusFit repo URL, e.g.
   `https://github.com/attmous/MusFit` (use the actual repo URL).
3. In the app's Obtainium settings:
   - Source: GitHub.
   - Enable **"Attempt to filter APKs by CPU architecture"** off (single universal APK).
   - Optionally enable **background update checking** and set an interval (e.g. every 6h).
   - If the repo is private, add a GitHub Personal Access Token (repo:read) in
     Obtainium → Settings → Source-specific → GitHub.
4. Tap the app in Obtainium → it detects `build-<n>` → **Install**. From then on Obtainium
   checks on its schedule and installs new master builds automatically.

> Fully silent (no install tap) is only possible if Obtainium is granted "device owner" via
> shizuku/root. Without that, Android shows a one-tap install confirmation per update — still
> no USB, no cable, no dev flashing.

## Trigger a build manually

- Push to `master` (normal path), **or**
- GitHub → Actions → **Android** → *Run workflow* (`workflow_dispatch`) — note this only
  publishes a Release when run on the `master` ref.

## Gotchas

- **Don't re-generate the keystore.** Replacing `app/keystore/musfit.debug.keystore` with a
  different key breaks updates for every phone already on the old key (forces uninstall).
- **Building from a second machine is fine** — Gradle uses the committed keystore, not that
  machine's `~/.android/debug.keystore`.
- **Downgrades won't auto-install.** If you flash an older local build over a newer OTA one,
  its lower versionCode means Obtainium won't "update" backward; use `adb install -r -d` if you
  really need to.
