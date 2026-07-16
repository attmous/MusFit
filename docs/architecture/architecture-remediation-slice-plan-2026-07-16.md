# MusFit Architecture Remediation Slice Plan - 2026-07-16

## Purpose and authority

This document is the active execution plan for the remaining packages in the
[architecture remediation backlog](architecture-remediation-backlog-2026-07-10.md).
It replaces the historical rule that every package ID must have its own pull
request. It does not retire, rename, combine, or weaken any package acceptance
contract.

The source-of-truth order is:

1. current source and tests on `origin/master`;
2. this document for slice composition, PR granularity, and execution order;
3. the backlog for every package's scope, findings, dependencies, tests, risks,
   and acceptance criteria;
4. the historical audit for evidence that still reproduces on current source.

Every package ID remains independently traceable in commits, PR evidence, and
the completion ledger. A slice is complete only when every package in it meets
its original acceptance contract.

## Verified planning snapshot

Planning was reconciled on 2026-07-16 against `origin/master` commit
`0b4921f93cb6edf8c3d9733a0b4342cac215efea` and live GitHub PR state.

- Waves 1 and 2 are complete.
- Wave 3 is complete through `W3-RESTORE-02` (PR #144).
- PRs #145, #146, and #148 are CI/migration repairs, not additional completed
  remediation packages.
- There were no open PRs at the planning snapshot.
- 53 defined package IDs remain: 5 in Wave 3, 23 in Wave 4, and 25 in Wave 5.
- `W5-PERF-01` may expand into more than one implementation PR only if current
  measurements identify unrelated hot spots that cannot be reviewed safely as
  one slice.

Revalidate this snapshot against current `origin/master` and live PR state
before starting or resuming work. Current source and newly merged tests win.

## Replacement PR policy

Use one branch, worktree, and PR per slice below rather than one PR per package.

- Keep one focused commit per package ID where practical. The PR may add small
  integration commits, but those commits must name the slice and their purpose.
- Implement the packages inside a slice in their listed dependency order.
- Run focused red/green checks for each package before starting the next package
  in that slice.
- Run the canonical full gate, package-specific checks, seeded emulator flow,
  `git diff --check`, and intended-file review once against the complete slice.
- The PR body must include a separate acceptance checklist and evidence block
  for every package ID in the slice.
- Do not absorb unrelated cleanup, dependency upgrades, or product work.
- Split a slice only when current evidence shows that its combined diff is not
  safely reviewable, it crosses an exclusive ownership boundary unexpectedly,
  or one package is externally blocked while the others can still land.
- Never combine two active slices that own `build-root`, `ci`, `db-schema`,
  `app-nav`, or the same feature ViewModel/repository family.

The current Android workflow runs for pull requests and pushes to `master` or
`main`, not ordinary branch pushes. To avoid wasting CI:

1. make and verify the package commits locally;
2. push the slice branch for backup if desired;
3. open the PR only after the complete slice is locally green;
4. avoid incremental PR pushes unless they fix review or CI findings;
5. merge as soon as required CI is green and GitHub reports the exact head as
   mergeable, under the user's standing approval to merge verified PRs;
6. fetch and verify the merge commit in `origin/master` before starting a
   dependent slice.

Recheck workflow triggers before relying on the branch-push behavior because CI
configuration can change.

## Remaining slice sequence

The 53 remaining package IDs are grouped into 20 slice PRs. Nineteen slices
contain multiple packages. The Training restoration contract remains a
singleton because process restoration, Room-owned active workouts, and
screen-scoped rest timers form one high-risk behavior boundary.

| Slice | Package IDs | Dependency and ownership gate |
| --- | --- | --- |
| `S01-TRAINING-RESTORE` | `W3-RESTORE-03` | Start first; owns `training-ui`; verify process death and timer shutdown before any Training state or navigation work. |
| `S02-W3-ARCH-CLOSE` | `W3-BOUNDARY-01A`, `W3-BOUNDARY-01B`, `W3-BOUNDARY-01C`, `W3-DOC-01` | After S01; finish Food/Profile/Health boundaries, architecture tests, and then source-derived documentation. |
| `S03-FOOD-STATE-FOUNDATION` | `W4-STATE-F1`, `W4-STATE-F2A` | After S02; owns `food-ui`; land reducers before diary/tracker state slicing. |
| `S04-FOOD-STATE-COMPLETE` | `W4-STATE-F2B`, `W4-STATE-F2C` | After S03; serialize Add/database before editor/planning work where `FoodViewModel` overlaps. |
| `S05-TRAINING-STATE` | `W4-STATE-T1A`, `W4-STATE-T1B` | After S01 and S02; owns `training-ui`; preserve Room-owned workouts and screen-scoped timers. May run alongside S03/S04 only in a separate worktree. |
| `S06-NAV-ROOT-PROFILE` | `W4-NAV-01`, `W4-NAV-F3` | After S02; exclusive `app-nav`; establish root parity before Profile/Today entries. |
| `S07-NAV-FOOD-TRAINING` | `W4-NAV-F1`, `W4-NAV-F2` | After S04, S05, and S06; serialize shared entry-registry edits; preserve scanner results and Training back behavior. |
| `S08-BUILD-CORE` | `W4-MOD-01`, `W4-MOD-C1`, `W4-MOD-C2`, `W4-MOD-C3` | After S02; exclusive `build-root`; add conventions first, then model, design system, and testing modules. |
| `S09-DATA-MODULES` | `W4-MOD-D1`, `W4-MOD-D2`, `W4-MOD-D3` | After S08; exclusive `db-schema`/`build-root`; fixed database, network, then data order with no schema or wire behavior changes. |
| `S10-INTEGRATION-MODULES` | `W4-MOD-I1`, `W4-MOD-I2` | After S07 and S09; serialize `build-root`; Health and scanner adapters depend only on neutral ports/models. |
| `S11-FEATURE-MODULES-FT` | `W4-MOD-F1`, `W4-MOD-F2` | After S10; exclusive app composition; move coarse Food and Training modules without per-screen modules. |
| `S12-FEATURE-MODULES-PT` | `W4-MOD-F3`, `W4-MOD-F4` | After S11; exclusive app composition; move Profile and Today and reverify all cross-feature actions. |
| `S13-ROOT-ADAPTIVE-BASE` | `W5-INSETS-01`, `W5-A11Y-01`, `W5-ADAPT-01` | After S12; exclusive `app-nav`/shared UI; insets before root semantics, then stable adaptive navigation. |
| `S14-FOOD-ADAPTIVE` | `W5-LIFE-F1`, `W5-LAZY-F1`, `W5-LAZY-F2`, `W5-A11Y-F1`, `W5-ADAPT-F2` | After S13; owns Food UI; lifecycle first, then measured lazy rendering, accessibility, and Food adaptive scene. |
| `S15-TRAINING-ADAPTIVE` | `W5-LIFE-F2`, `W5-IMAGE-01`, `W5-A11Y-F2`, `W5-A11Y-CHART-01`, `W5-ADAPT-F1` | After S13; owns Training UI; verify one image loader, chart alternatives, and compact/full-screen workout behavior. |
| `S16-PROFILE-TODAY-UX` | `W5-LIFE-F3`, `W5-LIFE-F4`, `W5-A11Y-F3` | After S13; owns Profile/Today UI. May run alongside S14/S15 in a separate worktree. |
| `S17-I18N-BASE-FT` | `W5-I18N-01`, `W5-I18N-F1`, `W5-I18N-F2` | After S14, S15, and S16; establish typed UI text before Food and Training migration. |
| `S18-I18N-PT` | `W5-I18N-F3`, `W5-I18N-F4` | After S17; migrate Profile and Today/app/scanner text while keeping route/database/wire keys stable. |
| `S19-SIZE-DISTRIBUTION` | `W5-SIZE-01`, `W5-SIZE-02` | After S18; exclusive shared icons, `build-root`, `ci`, and release workflow; verify ABI artifacts, checksums, provenance, installs, and size budgets. |
| `S20-MEASURED-CLOSEOUT` | `W5-PERF-01`, `W5-PLAT-01` | Final slice after S19; optimize only evidence-backed hot spots, then record the experimental API watchlist and adoption gates. |

## Concurrency

Use at most three implementation agents, but prefer one or two while the
bundled plan is new.

- S03/S04 and S05 may proceed in separate worktrees after S02, but S04 must
  remain behind S03.
- S06 and S08 may proceed independently after S02 because `app-nav` and
  `build-root` ownership are disjoint.
- S14, S15, and S16 may proceed concurrently after S13 when their feature file
  ownership remains disjoint.
- All other slices follow the table order. Never parallelize two slices that
  share an exclusive key or app composition.
- Merge completed dependencies and refresh `origin/master` before creating the
  worktree for a dependent slice.

## Slice verification contract

Before Gradle or adb commands:

```powershell
. .\scripts\android\android-env.ps1
```

At minimum, every complete slice must run:

```powershell
.\scripts\dev\verify-musfit.ps1 -Preset Full
.\scripts\android\install-seed-musfit.ps1 -Reset
git diff --check
```

Also run the union of every included package's focused, instrumentation,
screenshot, migration, security, release, query-count, benchmark, and device
acceptance checks from the backlog. A passing full gate cannot replace a
missing package-specific acceptance check.

The PR body must report:

- all package and finding IDs;
- current base commit and slice head commit;
- root cause/baseline for each package;
- implementation boundary and files changed per package;
- focused red/green evidence per package;
- full verification and seeded-emulator evidence;
- measurable performance/query/package evidence where applicable;
- schema, API, compatibility, security, privacy, and migration risk;
- rollback procedure for the complete slice and any package-specific rollback;
- dependencies satisfied and slices newly unblocked.

## Device safety

Use `MusFit_API36` / `emulator-5554` for reset/seeding, destructive database
tests, account deletion, Health test records, permission changes, scanner loops,
and migration verification.

The Pixel 8 Pro `38241FDJG00BLY` remains non-destructive evidence only unless
the user explicitly authorizes a destructive action. Do not reset or seed it,
mutate Food/workout data, export/delete Health records, change permissions, or
install a signing-transition build without explicit approval.

Never restore the obsolete exported seed broadcast receiver. Preserve an
unrelated untracked `gradle/gradle-daemon-jvm.properties` file if present.

## Immediate handoff

Start with `S01-TRAINING-RESTORE` from current `origin/master`. A historical
`codex/w3-restore-03` worktree may exist at commit `c80e685`; do not continue it
without rebasing or recreating it from current `origin/master` and revalidating
all current Training source and tests.

After S01 is locally and remotely verified, merge it immediately when GitHub
reports the exact PR head green and mergeable. Verify containment in
`origin/master`, then start S02.

The program is complete only when all 53 package IDs above are merged or are
explicitly retired with current evidence and user approval.
