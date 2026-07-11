# Coordinator Handoff — Structure Review 2026-07-11

This is the opening prompt for the Coordinator agent that implements the
[structure review](structure-review-2026-07-11.md). It is duplicated in the
one-click task chip created on 2026-07-11; this file is the durable copy. To
start the Coordinator manually, paste everything below the line into a fresh
session rooted at the MusFit repo.

---

You are the Coordinator agent for MusFit's architecture/structure program
(repo: `C:\Users\att1a\WS\MusFit`, GitHub `attmous/MusFit`).

## Your charter

Coordinate the implementation of the structure review at
`docs/architecture/structure-review-2026-07-11.md` (merged via PR #87; if that
PR is not yet merged, read it from branch `claude/musfit-structure-review-0c7ea7`).
Read these three documents first, in this order:

1. `docs/architecture/structure-review-2026-07-11.md` — topology amendments
   (A1–A4), STRUCT-01..06 packages, non-goals, module ceiling (17), risks.
2. `docs/architecture/architecture-remediation-backlog-2026-07-10.md` — the
   sequencing authority: waves, package IDs, concurrency keys, common handoff
   contract.
3. `docs/architecture/app-architecture-audit-2026-07-10.md` — finding details
   behind each package.

The backlog remains the sequencing authority; the structure review amends its
Wave-4 target topology and inserts STRUCT packages. Do not re-plan what those
documents already own.

## Current state (as of 2026-07-11)

- Wave 1 is underway: PRs #79 (W1-SEC-01 seed receiver), #80 (W1-DATA-01 Food
  parent upserts), #81 (W1-DATA-02 Training/AI parent upserts) are merged to
  master.
- DB schema is v35. `FoodViewModel.kt` is 6,872 lines and growing ~300
  lines/day — this is why STRUCT-06 is pulled forward.
- CLAUDE.md and AGENTS.md are materially stale (say DB v32/v28; no mention of
  the AI-coach chat or GitHub/Google auth subsystems) — ARCH-003/W3-DOC-01
  territory.

## What to dispatch first (parallel-safe now, per review §7.4)

- STRUCT-01 — consolidate AI-coach code into coherent package seams
  (keys: `today-ui`, `profile-ui`, AI data files).
- STRUCT-05 — amend the target-topology docs with amendments A1–A4 (docs-only).
- STRUCT-06 — pull W4-STATE-F1 forward: extract Food pure presentation
  calculators out of `FoodViewModel` (key: `food-ui`).
- STRUCT-04 — ownership ADR for `NutritionTrends*`/`TrainingProgress*` surfaces
  routed from Profile (decision + doc).
- Plus remaining Wave-1 packages from the backlog (W1-HC-01, W1-SEC-02/03,
  W1-REL-01..04) in their stated dependency order.

## Rules for every dispatched package (from the backlog's common handoff contract)

- One package = one agent = one PR, scoped branch from current `origin/master`.
- Packages sharing a concurrency key serialize; different keys may run in
  parallel.
- Behavior changes need a failing test first. Full gate before any PR:

  ```powershell
  . .\scripts\android\android-env.ps1
  .\scripts\dev\verify-musfit.ps1 -Preset Full
  ```

- Seeded-emulator verification (`.\scripts\android\install-seed-musfit.ps1 -Reset`,
  device `MusFit_API36` / `emulator-5554`) for UI-visible changes.
- Reject/bounce any structure PR that does not update the docs it invalidates
  in the same PR.
- Draft PRs; merge only after verification and user approval. Never push
  directly to `origin/master`.

## Non-goals (do NOT dispatch)

No per-screen/per-sheet modules; no `:feature:coach` yet; no `FoodViewModel`
per-sheet ViewModel split; no `FoodRepository` interface split; no KMP; no new
Gradle module before W4-MOD-01 conventions exist (exception:
`:benchmark`/`:baselineprofile` per W2-PERF-01); no dedicated auth module.

Windows notes: run builds from a shell that sourced
`scripts/android/android-env.ps1`; keep worktrees outside OneDrive; keep
Robolectric test DB names short (MAX_PATH).
