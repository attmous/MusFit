# MusFit Agent Handoff

## Purpose And Authority

This is the repository-level execution contract for coding agents. Keep it
stable and operational; do not turn it into a feature changelog or copy volatile
schema versions, file sizes, or roadmap tables into it.

Follow the current user request. For factual repository truth, use this order:

1. current source, build files, helper scripts, and CI;
2. the latest applicable audit or remediation package, revalidated against
   current `origin/master`;
3. living architecture and feature docs;
4. historical plans/specs, which explain intent but are not current status.

If a living doc conflicts with source, source wins and the doc should be fixed
in the same PR. The current repo-wide engineering handoff is:

- [`docs/architecture/app-architecture-audit-2026-07-10.md`](docs/architecture/app-architecture-audit-2026-07-10.md)
- [`docs/architecture/architecture-remediation-backlog-2026-07-10.md`](docs/architecture/architecture-remediation-backlog-2026-07-10.md)

The audit is a dated snapshot. Re-check a finding before implementing it.

## Product Boundary

MusFit is an Android-only, single-module (`:app`) fitness and nutrition tracker.
The production application id is `com.musfit`; the side-by-side developer
variant is `com.musfit.internal`. Top-level destinations are: Today, Food,
Training, Profile.

- Kotlin, Jetpack Compose Material 3, Hilt, Room, coroutines, and Flow.
- Local-first storage. Local accounts and optional Google/GitHub identity links
  exist, but there is no MusFit cloud-sync backend.
- The coach supports opt-in, user-configured OpenAI-compatible endpoints and
  local agents such as Hermes. MusFit does not operate a hosted AI backend.
- Open Food Facts, CameraX/ML Kit, and Health Connect are external boundaries.
- Do not add analytics, subscriptions, social features, new identity providers,
  or a MusFit-operated backend unless explicitly requested.
- Keep UI dense, clean, practical, and original. Lifesum/Hevy may inspire
  information architecture, but do not copy their assets or exact layouts.

Do not assume Food is the active priority. Scope work from the current request
and current backlog. Food is the largest mature feature; its feature reference
is [`docs/architecture/food-system.md`](docs/architecture/food-system.md).
Historical plans under `docs/superpowers/` are not the active backlog.

## Known Sharp Edges

Read the linked audit before broad changes. In particular:

- DATA-001 was remediated for Food and Training/AI in PRs #80-81. Preserve that
  invariant: relationship-bearing parents use `@Upsert` or targeted `@Update`;
  any retained `REPLACE` must be relationship-free, intentional, and tested.
- Account/provider UI exists, but most personal-data tables are not yet fully
  account-isolated. Do not assume switching accounts isolates all data.
- Food nutrition/hydration export permissions and rationale are synchronized by
  the shared Health permission inventory. Keep manifest, requested access, and
  rationale changes atomic.
- The legacy exported seed receiver (SEC-001) has been removed. Internal
  seeding now runs through a separately installed instrumentation APK that CI
  must not distribute; keep all target manifests free of seed components and
  actions.
- Seed/reset tooling is for the named dedicated AVD only and rejects physical or
  mismatched-emulator serials. Never seed, clear, or reset a phone or user-data
  device.
- CI verifies `internalDebug` and unsigned `productionRelease` outputs and keeps
  only the internal APK as a short-lived workflow artifact. GitHub Release and
  Obtainium publication are suspended until the remaining Wave 1 release gates
  land; no current artifact is production- or Play-ready.
- Hermes/API bearer credentials are runtime-only and live in the account-keyed,
  Android-Keystore-backed AI secret store. Build configuration may provide only
  nonsecret internal endpoint/model defaults; it has no API-key field or
  fallback. A stale Room `apiKeyStored` flag is reconciled against the runtime
  store before the UI or a connection treats the key as present.
- Treat any real Hermes key compiled by a pre-SEC-003 build as compromised:
  rotate/revoke it outside the repository, remove the obsolete local property,
  and clear stale generated output with `scripts/dev/clean-generated.ps1`.
- AI coach endpoint policy is enforced at settings save and again before request
  dispatch. Production accepts HTTPS only. Internal HTTP is limited to exact
  `localhost`, literal IPv4 loopback/RFC1918, and literal IPv6 loopback/ULA;
  hostnames, link-local addresses, redirects, and default-network fallback are
  rejected. Keep LAN permission and broad platform cleartext opt-in internal;
  the pure request policy is the CIDR gate because Android XML cannot express it.
- Never commit OAuth client secrets, AI keys, gateway tokens, or other secrets.
  Provider client ids are configuration, not bearer secrets, but keep
  environment-specific values in the existing local configuration path unless a
  task changes that policy explicitly.

## Build And Verification

On Windows PowerShell, initialize the checked-in toolchain environment before
direct Gradle or adb work:

```powershell
. .\scripts\android\android-env.ps1
```

Run the executable workflow contract after changing live docs, scripts, or CI:

```powershell
.\scripts\dev\test-dev-workflow.ps1 -SelfTest
```

The standard variant gate is:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Full -RetryOnGeneratedOutputIssue
```

It runs unit tests, lint, and assembly for `internalDebug` and the non-debuggable
`productionRelease`, builds the internal instrumentation APK, and builds the
unsigned production AAB. The installable developer APK is written to
`app/build/outputs/apk/internal/debug/app-internal-debug.apk`; CI retains only
that target APK briefly, never the instrumentation or production artifacts.

Focused Food verification:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Food
```

For another focused test class, use `-Tests` or invoke Gradle directly with
`--tests`. If generated files under `app/build` are stale or locked, run:

```powershell
.\scripts\dev\clean-generated.ps1
```

Then rerun the same failed command. Do not treat cleanup as a product-code fix.

### Seeded Emulator

Create the standard AVD only when it is missing:

```powershell
.\scripts\android\setup-musfit-emulator.ps1
```

Install, reset, seed, launch, and capture evidence on the dedicated emulator:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset None -InstallSeed -ResetSeed -EvidenceDir verification\musfit-emulator
```

Use `MusFit_API36` / `emulator-5554` as the normal seeded baseline. UI-visible
changes require workflow-specific verification plus reviewed screenshot and
UI-tree evidence. Documentation-only changes do not require an emulator; record
why the device step is not applicable.

For every PR whose diff can change Android runtime functionality or design,
read and complete
[the repository evidence skill](.agents/skills/musfit-pr-emulator-evidence/SKILL.md).
Completion includes running `publish-pr-evidence.ps1`; local `verification/`
files do not satisfy this requirement. Before handoff or merge, confirm that the
marker-based evidence comment and `MusFit emulator evidence` status verify the
exact current PR head SHA. A new commit invalidates the evidence. Skip only when
the diff is exclusively documentation, tests, CI, repository metadata, or
non-runtime tooling, and record the reason in the PR.

### Physical Device

Discover the current serial instead of trusting an old handoff value, and always
qualify adb commands when more than one device may be connected:

```powershell
adb devices -l
adb -s <serial> install -r app\build\outputs\apk\internal\debug\app-internal-debug.apk
adb -s <serial> shell am start -W -n com.musfit.internal/com.musfit.MainActivity
adb -s <serial> shell dumpsys window | Select-String 'mCurrentFocus|mFocusedApp'
```

Do not use the seed/reset helper on a physical device.

## Architecture And Test Rules

The intended dependency direction is Compose UI -> ViewModel -> repository
boundary -> DAO/remote/integration boundary. The current code has documented
cross-feature and integration leaks, so treat this as a target direction rather
than a claim that every file already complies.

- Prefer existing public contracts where they are sound; do not reproduce a
  pattern identified as defective by the audit.
- Keep domain calculators free of Android, Compose, Room, and Retrofit imports.
- ViewModels expose immutable observable state, but the implementation may be a
  private `MutableStateFlow`, combined repository flows with `stateIn`, or both.
- Use TDD for behavior changes where practical: demonstrate the failure, make
  the smallest fix, and keep the regression test.
- ViewModel tests use plain JUnit with hand-written fakes and coroutine test
  dispatchers; keep them free of Robolectric unless Android behavior is required.
- Repository/DAO query tests generally use Robolectric with an in-memory Room
  database. Migration tests are separate and must exercise the relevant schema
  transition.
- For Room changes, derive the current version from
  `app/src/main/java/com/musfit/data/local/MusFitDatabase.kt`, add and register
  the next migration in `core/di/DatabaseModule.kt`, and commit the exported
  schema JSON. There is no destructive-migration fallback.
- For Food work, read `food-system.md` for the feature map and state conventions,
  but let the July architecture audit/backlog supersede older refactor advice
  when they conflict.
- Preserve local-first behavior and explicit user review for nutrition-label OCR
  and Food AI drafts. Food voice/photo logging remain shells; the global coach
  chat is implemented and currently read-only with respect to app data.

## Branch, PR, And Task Flow

New work uses a scoped branch and PR. Because Codex often runs in detached
worktrees while another checkout owns `master`, inspect ownership before
switching or publishing:

```powershell
git status --short --branch
git worktree list --porcelain
```

Use `scripts/dev/new-task-branch.ps1` when it fits; it starts from current
`origin/master` and refuses a dirty checkout.

For each task:

1. Fetch `origin`, confirm a clean checkout/worktree, and create a scoped branch.
2. Read current source, tests, and applicable architecture/feature docs. For
   remediation work, also read the exact audit finding/package and keep one
   remediation package per PR unless the user explicitly changes that scope.
3. Add or adjust focused tests first for behavior changes where practical.
4. Implement the smallest coherent change without widening into unrelated
   feature areas.
5. Run focused checks, the workflow contract when applicable, and the standard
   variant gate.
6. For runtime functionality/design changes, complete and publish the repository
   evidence skill for the exact committed PR head. For non-runtime-only changes,
   record why the workflow is N/A.
7. Run `git diff --check` and review the final diff for secrets, generated files,
   accidental scope, and stale documentation.
8. Commit intentionally, push the branch, and open a draft PR with commands,
   evidence, risks, compatibility decisions, and rollback notes.
9. Do not push directly to `origin/master`. Do not merge a runtime PR until the
   current-head `MusFit emulator evidence` status passes. Merge only after all
   required checks and user approval, unless the user explicitly requests an
   emergency exception.

## CI And Reference Map

`.github/workflows/android.yml` runs the workflow contract and internal plus
production-shaped verification gate for PRs and pushes to `master`/`main`. It
retains `musfit-internal-debug-apk` for seven days and publishes no GitHub
Release. Production signing, install migration, optimization, and publication
remain separate Wave 1 packages.

Use these living references instead of duplicating their detail here:

- App map: [`docs/architecture/README.md`](docs/architecture/README.md)
- Screen/navigation contracts: [`docs/architecture/screen-contracts.md`](docs/architecture/screen-contracts.md)
- Data models: [`docs/architecture/data-models.md`](docs/architecture/data-models.md)
- Food feature map: [`docs/architecture/food-system.md`](docs/architecture/food-system.md)
- Coach/Hermes boundary: [`docs/architecture/hermes-coach-ai.md`](docs/architecture/hermes-coach-ai.md)
- Design system: [`docs/design/musfit-design-system.md`](docs/design/musfit-design-system.md)
- Auto-update flow: [`docs/ops/auto-update.md`](docs/ops/auto-update.md)
