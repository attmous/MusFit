# MusFit Structure Review — 2026-07-11

**Purpose.** A forward-looking review of MusFit's component and module structure:
is the current shape the right one for where the product is going, what should be
split or combined, and how does it compare to the official Android architecture
and modularization guidance. The output is a decision set and work packages for a
Coordinator agent.

**Relation to the 2026-07-10 audit.** This review builds on, and does not
re-derive, the [full app architecture audit](app-architecture-audit-2026-07-10.md)
and its [remediation backlog](architecture-remediation-backlog-2026-07-10.md).
Where the audit already owns a finding, this review references its package ID
(`W1-…`/`W4-MOD-…`) instead of duplicating it. New items introduced here carry
`STRUCT-` IDs. The audit was point-in-time evidence-gathering; this review is the
long-term-features lens on the same structure, and it amends the audit's target
topology in four places (§6).

Snapshot: branch `claude/musfit-structure-review-0c7ea7`, DB schema 35,
127 main Kotlin files (~43k lines), 1 Gradle module.

---

## 1. Executive verdicts

Direct answers to the review questions:

**Is the current structure the best one?** The *internal* structure (layered
packages, pure domain, repository interfaces, state-driven UI) is good and worth
keeping. The *module* structure — a single `:app` — is no longer the best fit:
it cannot enforce the boundaries the docs promise, it gives every change a
whole-app compile/kapt blast radius, and it blocks future surfaces (widgets,
benchmarks, a possible Wear client) that need reusable core libraries. The
single-module decision was right for the MVP; the codebase has outgrown it.

**Split or combine?** Split — but not yet, and not finer than the coarse
topology in §6. Nothing should be combined at the Gradle level; at the package
level the AI-coach code should be *consolidated* (it is currently smeared across
five packages) before any module extraction.

**Best structure according to features?** Not quite. The four bottom-nav
miniapps (Today, Food, Training, Profile) map cleanly onto feature modules, but
the fifth de-facto subsystem — the **AI coach** — has no home: its engine is in
`domain/coach`, its client in `data/remote/coach`, its repositories in
`data/repository`, its UI in `ui/today` and `ui/profile`, and its DI in
`core/di`. Given the coach is the product's stated north star, this is the
biggest feature-alignment gap (§4.2).

**Best structure according to Android documentation?** The layering follows the
official architecture guide (UI → data, pure domain, unidirectional state). The
module story does not yet follow the official modularization guide, which
recommends by-feature + by-layer hybrid modularization once an app reaches this
size — but the same guide warns against premature or over-fine modularization,
and the audit's staged plan (clean leaks → conventions → coarse modules) is the
textbook-correct path. This review endorses that plan with amendments (§6) and a
firm ceiling on module count (§8).

**When?** Module extraction stays in Wave 4 as the backlog orders it. What this
review adds for *now* (before Wave 4) are cheap, behavior-preserving package
moves and contract decisions (§7) that make the eventual extraction mechanical
and stop today's active development (coach, Food) from deepening the leaks.

---

## 2. Current structure snapshot

### 2.1 Gradle level

One production module `:app` (`settings.gradle.kts` includes only `:app`).
232 resolved runtime dependencies, all sharing one compile boundary. kapt (Room,
Hilt) on the critical path. No benchmark/baseline-profile modules yet.

### 2.2 Package level (main source, by size)

| Area | Packages | ~Lines | Notes |
| --- | --- | ---: | --- |
| Food UI | `ui/food` (13 files) | ~15,100 | `FoodViewModel.kt` 6,872; `FoodModalSheets.kt` 2,909; `FoodScreen.kt` 2,312 |
| Training UI | `ui/training` (12 files) | ~9,600 | `TrainingRoutineContent.kt` 2,554; `TrainingViewModel.kt` 1,750 |
| Data: repositories | `data/repository` (13 files) | ~8,400 | `FoodRepository.kt` 2,609; `TrainingRepository.kt` 1,869 |
| Data: Room | `data/local` (24 files) | ~2,600 | 36 entities, 9 DAOs; schema v35 |
| Profile UI | `ui/profile` (9 files) | ~3,400 | incl. accounts, auth UI, AI-coach settings |
| Today UI | `ui/today` (6 files) | ~2,050 | incl. coach feed + coach chat |
| DI | `core/di` (5 files) | ~1,180 | `DatabaseModule.kt` 976 (mostly migrations) |
| Domain | `domain/*` (9 sub-packages, 22 files) | ~1,600 | pure; calculators + models |
| Health Connect | `integrations/healthconnect` (6 files) | ~840 | gateway + manager + mappers |
| Remote | `data/remote/{food,auth,coach}` | ~570 | OFF, GitHub OAuth, Hermes client |
| Shared UI | `ui/components`, `ui/theme`, `ui/` root | ~1,600 | charts, scaffold, theme tokens, nav |

Top-level navigation: `Today`, `Food`, `Training`, `Profile` tabs plus scanner
routes and three Profile sub-routes (settings, training progress, nutrition
trends).

### 2.3 Subsystems not yet reflected in the handoff docs

Two subsystems have grown past what `CLAUDE.md`/`AGENTS.md` describe (drift is
ARCH-003 / `W3-DOC-01`, but the Coordinator should know it now):

- **AI coach** — `CoachEngine` (deterministic feed), `HermesCoachClient`
  (OpenAI-compatible chat), `AiCoachRepository` / `AiCoachChatRepository` /
  `CoachRepository`, Room chat history per account+provider, Keystore-backed key
  store, coach feed + chat UI in Today, settings UI in Profile,
  `AiCoachConfigModule` with debug Hermes defaults.
- **External identity** — Google (Credential Manager) and GitHub (device flow)
  sign-in linking to local accounts: `data/remote/auth`, `ExternalAuthRepository`,
  `ui/profile/GoogleSignIn.kt`.

Also: DB is v35 (docs say 28/30/32 in different places), and `FoodViewModel`
grew from a documented ~4,500 to 6,872 lines in roughly a week — the growth rate
matters for prioritization (§4.1).

---

## 3. Long-term feature outlook → structural requirements

The point of this review: structure should be chosen for the features the
product is heading toward, not just today's code. Sources: `AGENTS.md`, the
Food system doc's deferred scope, the Hermes coach doc, and the stated product
north star (BYO-AI coach: local model / own API key / agent-bridge setups;
meal-photo logging; sleep import; Today coach feed; floating chat).

| Future feature | Structural requirement it creates |
| --- | --- |
| **BYO-AI coach** (multiple providers: local Hermes, user API key, agent bridge) | A provider-agnostic AI client boundary (ports + N adapters), separate from any single feature's UI. Credential store behind it. Today's `HermesCoachClient` is one adapter hard-wired as the only path. |
| **Floating coach chat over all tabs** | Coach UI anchored at the app root/scaffold level, not inside `ui/today`. Coach must not import feature ViewModels. |
| **Coach reads (later: acts on) app data** | A neutral, versioned **coach context snapshot** contract. Today `CoachChatViewModel` injects six repositories directly — every new feature grows the coach's dependency fan-in, and a future "coach can log a meal" needs typed *action* ports, not repository access. |
| **AI meal-photo logging** | Camera capture pipeline shared by barcode scan, label OCR, and photo capture; pluggable analyzers. Currently two scanner screens duplicate the CameraX session lifecycle (audit UI-005). |
| **Real nutrition-label OCR** (Slice 5 completion) | Parser stays pure domain (already is); scanner isolation (`W4-MOD-I2`) keeps ML Kit's 60 MB native payload contained and swappable. |
| **Sleep import, HC deepening** (deletion, reconciliation) | Neutral health ports independent of Room (`W3-BOUNDARY-01C`), already planned. |
| **Multi-account correctness** | `accountId` ownership on all personal tables (`W3-OWN-*`) **must precede** data-layer module extraction — moving `:core:database` first would mean re-touching a frozen module for every ownership migration. |
| **Home-screen widgets (Glance), possible Wear client** | The strongest *long-term* argument for `:core:database`/`:core:data`/`:core:model`: a widget or Wear process needs "read today's calories/water" without compiling four feature UIs and ML Kit. Impossible in a single module. |
| **Play-store / signed distribution** (`W1-REL-*`) | `:benchmark` + `:baselineprofile` modules (`W2-PERF-01`) — these are the first new Gradle modules regardless of feature modularization. |
| **Tablet/adaptive layouts, i18n** (`W5-*`) | `:core:designsystem` as the single owner of tokens/components/chart primitives so goldens and RTL fixes happen once. |

Conclusion from the table: every major future feature pulls in the same
direction — coarse feature + core modules with neutral contracts — and two of
them (BYO-AI, widgets/Wear) are *impossible to do cleanly* in a single module.
None of them requires finer granularity than the §6 topology.

---

## 4. Assessment of the current structure

### 4.1 What is right (keep)

- **Layering and dependency direction.** UI → ViewModel → repository interface →
  DAO/remote; `domain/` verified pure. This matches the official architecture
  guide and makes the eventual module extraction mostly `git mv`.
- **Repository interfaces + Hilt `@Binds`** — a real seam; fakes keep ViewModel
  tests device-free.
- **Migration discipline** — v1→35 all registered and exported, no destructive
  fallback.
- **Single-ViewModel state machines per miniapp** — the Food doc's "do not split
  per sheet" rule is correct; splitting by sheet would break the unified state
  machine and the single-fake test model. (Slicing by *destination lifetime* per
  `W4-STATE-F2` is a different, correct operation.)
- **Package-inside-layer feature grouping** (`ui/food`, `domain/training`, …) —
  the internal layout already mirrors the target module topology.

### 4.2 What is wrong or aging out

1. **Single module can't enforce anything** (ARCH-001). `internal` is app-wide;
   the documented rules ("UI never imports remote DTOs") are only prose, and the
   audit found the violations to prove it (ARCH-002: Food UI exposes OFF DTOs,
   Profile imports a Food-UI extension, HC gateway imports Room entities). With
   agent-driven parallel development, compile-time boundaries are worth more
   here than in a typical solo project — a module boundary is a rule an agent
   *cannot* drift past, unlike a CLAUDE.md instruction.
2. **The AI coach has no architectural home.** Code in 5+ packages, DI fan-in of
   six repositories in `CoachChatViewModel`, provider hard-wired. This is the
   north-star feature and currently the least-structured subsystem. Every week
   of coach work in the current shape deepens the future extraction cost.
3. **Food's weight is still compounding.** `FoodViewModel` +2,300 lines in ~a
   week post-audit. The Food doc's Tier-1 extraction ("presentation calculators
   out of the ViewModel") is marked *deferred*; at this growth rate it should be
   re-prioritized ahead of new Food features (it is `W4-STATE-F1`, but nothing
   in it actually depends on Waves 1–3 — see §7).
4. **Cross-feature progress/trends surfaces have ambiguous ownership.**
   `NutritionTrends*` lives in `ui/food` but is routed from Profile;
   `TrainingProgress*` similarly. Fine in one module; a blocking cycle the day
   `:feature:food` and `:feature:profile` exist. Ownership must be decided
   before Wave 4 (`W4-NAV-F3` touches this; STRUCT-04 makes the decision
   explicit).
5. **Doc drift is a structural hazard for agents** (ARCH-003) — schema version,
   file sizes, tab names, and the coach subsystem are all stale in the handoff
   docs that agents read first.

### 4.3 Against the official Android guidance, concretely

| Guidance (developer.android.com) | MusFit today | Verdict |
| --- | --- | --- |
| App architecture guide: UI/data layers, optional domain layer, UDF, single source of truth | Followed | Keep |
| Modularization guide: split by feature + layer once scale hurts; `:app` as composition root; feature modules never depend on each other; core modules for shared infra | Single module | Adopt via §6, in Wave 4 |
| Modularization guide: beware over-modularization (build complexity > benefit for small teams) | n/a | Enforce a module ceiling (§8) |
| Convention plugins / build-logic for multi-module consistency | None | `W4-MOD-01`, required before third module |
| Baseline Profiles + Macrobenchmark as separate modules | None | `W2-PERF-01` |
| KSP over kapt | kapt | `W2-BUILD-04`, do **before** module explosion (kapt cost multiplies per module) |

---

## 5. Split / combine decision table

Component-level and module-level decisions in one place.

| Current unit | Decision | Target (Wave 4+) | Rationale |
| --- | --- | --- | --- |
| `:app` (whole) | **Split** (Wave 4, coarse only) | §6 topology | ARCH-001; future widgets/Wear/benchmarks |
| `ui/food` | Keep together; slice state by destination lifetime | `:feature:food` | `W4-STATE-F1/F2`; do NOT split per sheet |
| `FoodViewModel` internals | **Split now** (pure calculators out) | stays in feature | 6,872 lines and compounding; no Wave dependency |
| `ui/training` | Keep together | `:feature:training` | `W4-STATE-T1` for state slices |
| `ui/today` | Keep, minus coach chat | `:feature:today` | coach feed cards can stay (Today renders them) |
| `ui/profile` | Keep, minus AI-coach settings ownership decision | `:feature:profile` | |
| Coach code (5 packages) | **Consolidate now** (packages), split later | `:core:ai` + coach UI initially inside `:feature:today` | STRUCT-01/02; north-star feature needs a boundary before it grows further |
| `domain/*` | Keep as-is | `:core:model` | already pure; includes chart scalers (`domain/charts`, `TrendChartScaler`) |
| `data/repository` | Keep interfaces broad for now | `:core:data` | revisit `FoodRepository` 40-method interface only when extraction forces it (agrees with Food doc) |
| `data/local` | Keep | `:core:database` | after `W3-OWN-*` ownership migrations land |
| `data/remote/{food,auth,coach}` | Keep; coach client moves with `:core:ai` | `:core:network` (OFF, GitHub), `:core:ai` (Hermes/providers) | |
| `core/di` modules | **Dissolve into target modules** | each module owns its DI | `DatabaseModule`→`:core:database`, `NetworkModule`→`:core:network`, `AiCoachConfigModule`→`:core:ai`, `RepositoryModule`→`:core:data` |
| `integrations/healthconnect` | Keep | `:integration:healthconnect` | after `W3-BOUNDARY-01C` neutral ports |
| Scanner screens (barcode + OCR) | **Combine the camera session lifecycle now** | `:integration:scanner` (scoped as capture + pluggable analyzers) | STRUCT-03; also fixes UI-005; enables meal-photo capture |
| `ui/components`, `ui/theme`, chart composables | Keep | `:core:designsystem` | |
| External auth (GitHub/Google) | Keep small, inside profile/data | no dedicated module | ~240 lines; a module would be over-modularization |
| Fakes/fixtures in tests | Keep; formalize later | `:core:testing` | `W4-MOD-C3` |

Nothing at the Gradle level should be **combined** — there is only one module.
The two package-level combines are the coach consolidation and the shared
camera-session owner.

---

## 6. Target topology — endorsed with four amendments

The audit's staged topology (`:app`, 4 feature modules, 6 core modules,
2 integration modules, benchmark + baselineprofile) is endorsed. Amendments:

**A1 — add `:core:ai`.** Provider-agnostic coach/AI runtime: chat-client port,
provider adapters (Hermes today; OpenAI-compatible key-based and agent-bridge
later), model/config types, the encrypted key store, and the **coach context
snapshot port** (see STRUCT-02). Depends on `:core:model` only. Rationale: the
BYO-AI north star means N providers behind one port; leaving this inside
`:core:data`/`:core:network` couples the most-active future area to the most
frozen infrastructure modules.

**A2 — coach UI starts inside `:feature:today`, not a fifth feature module.**
The feed and the chat sheet are launched from Today. When the floating-chat
(over all tabs) milestone lands, coach UI graduates to the app root or a
`:feature:coach` — decide then, not now. Creating the module today would be
speculative.

**A3 — scope `:integration:scanner` as *capture + analyzers*, not "the two
scanner screens".** One shared CameraX session owner (also the UI-005 fix),
with barcode, OCR-text, and (future) photo-capture analyzers as plugins. Meal
photos then reuse capture and hand the image to `:core:ai` without a new
integration module.

**A4 — chart placement split by nature.** Pure scalers (`domain/charts`,
`domain/training/TrendChartScaler`) → `:core:model`; rendering composables
(`ui/components/charts`, `ExerciseTrendChart`) → `:core:designsystem`. The
audit implies this; making it explicit avoids a `:core:charts` micro-module.

Resulting ceiling: **17 modules** (`:app`, 4 features, 7 core incl. `:core:ai`,
2 integrations, benchmark, baselineprofile, build-logic). That is the maximum,
not a goal — see §8.

Dependency rules (compile-enforced once modules exist, architecture-test-enforced
before):

- feature → core, designsystem, data; **never** feature → feature.
- `:core:ai` → `:core:model` only (network client types allowed); **never** →
  `:core:data`/`:core:database` — coach context arrives via the snapshot port
  implemented in `:core:data` and injected at the `:app` composition root.
- integration → `:core:model` only; never Room entities (ARCH-002).
- `:app` = composition root + manifest only.

---

## 7. Work packages for the Coordinator

### 7.1 Adopted as-is from the remediation backlog (do not re-plan)

Sequencing authority remains the backlog. Structure-relevant chain:
`W3-BOUNDARY-01A/B/C` (leaks) → `W4-MOD-01` (conventions) →
`W4-MOD-C1/C2/C3` (model/designsystem/testing) → `W4-MOD-D1/D2/D3`
(database/network/data) → `W4-MOD-I1/I2` (integrations) → `W4-MOD-F1–F4`
(features). Preconditions from other waves: `W1-DATA-01/02`, `W2-BUILD-04`
(KSP), `W2-PERF-01`, `W3-OWN-01/02/03` before `D1`.

### 7.2 New packages introduced by this review

Each is one agent / one PR, standard handoff contract from the backlog
(branch from `origin/master`, failing test first where behavior changes,
`verify-musfit.ps1 -Preset Full`, seeded-emulator check when UI-visible).

**STRUCT-01 — Consolidate the AI-coach code into coherent seams.** *(S–M;
can run now; keys: `today-ui`, `profile-ui`, AI data files.)*
Behavior-preserving package moves: `ui/coach/` for chat UI + coach settings UI
(`CoachChatViewModel`, chat sheet composables, `AiCoachSettingsUi`),
`data/coach/` (or clearly-named `data/repository` files) for
`CoachRepository`/`AiCoachRepository`/`AiCoachChatRepository` +
`HermesCoachClient`. No Gradle change, no behavior change. Acceptance: full
gate green; imports of coach types from feature packages unchanged or reduced;
docs updated in the same PR.

**STRUCT-02 — Define the coach context-snapshot and action ports.** *(M; design
+ implementation; after STRUCT-01; before further coach features.)*
Replace `CoachChatViewModel`'s six-repository fan-in with a single
`CoachContextProvider` port returning a versioned, neutral snapshot (food,
hydration, training, health, profile, goals) — implemented in the data layer,
injected via DI. Define (but may stub) the typed action port for future
"coach logs a meal" work so it lands behind a contract, not repository access.
Acceptance: `CoachChatViewModel` depends on coach repositories + provider port
only; snapshot content byte-equivalent to today's prompt context (pin with a
test); fakes shrink accordingly.

**STRUCT-03 — Shared camera session owner for scanners.** *(M; merges with
`W3-CAMERA-01` — same files, same key; implement as one package.)* One
lifecycle-safe CameraX session component consumed by barcode and OCR screens;
analyzer as a parameter. Explicitly design the analyzer seam so a photo-capture
analyzer can be added without touching the session owner. Acceptance: UI-005
acceptance criteria + both scanners behavior-identical.

**STRUCT-04 — Decide ownership of cross-feature progress surfaces.** *(S;
decision + doc + optional move; before `W4-MOD-F1`.)* `NutritionTrends*` and
`TrainingProgress*` are routed from Profile but live in feature packages.
Recommended decision: they belong to their *data* feature (Food/Training), and
Profile reaches them via typed navigation actions (`W4-NAV-F3` mechanism), never
imports. Record as an ADR; move files only if the decision requires it.

**STRUCT-05 — Amend the target topology docs.** *(S; docs-only; now.)* Fold the
§6 amendments (A1–A4) into the audit's topology section or as an ADR referenced
by it, so Wave-4 agents extract against one authoritative picture. Include the
module ceiling and dependency rules.

**STRUCT-06 — Re-prioritize Food pure-calculator extraction.** *(M; equals
`W4-STATE-F1` scope; this review's change is only its position.)* Nothing in
`W4-STATE-F1` technically depends on Waves 1–3: it moves pure functions
(`buildDailyInsights`, `buildDayRating`, progress accumulation, weekly score)
into a `ui/food` presentation-calculator file with direct unit tests. Pull it
forward to run alongside Wave-1/2 work under the `food-ui` key, because
`FoodViewModel` is growing ~300 lines/day and every new Food feature written
before extraction raises the cost. Acceptance: as `W4-STATE-F1`.

### 7.3 Explicit non-goals (do NOT hand these out)

- No per-screen or per-sheet modules; no `:feature:coach` yet (A2).
- No `FoodViewModel` split into per-sheet ViewModels (Food doc rule stands).
- No `FoodRepository` interface split until module extraction forces a seam.
- No KMP/multiplatform restructuring; Android-only stands.
- No new module before `W4-MOD-01` conventions exist (backlog rule; the only
  exceptions are `:benchmark`/`:baselineprofile` per `W2-PERF-01`).
- No dedicated auth module (~240 lines does not justify one).

### 7.4 Suggested insertion into the wave plan

| When | Structure work |
| --- | --- |
| Now (parallel with Wave 1) | STRUCT-01, STRUCT-05, STRUCT-06 (`food-ui` key), STRUCT-04 decision |
| Wave 2–3 | STRUCT-02 (after STRUCT-01), STRUCT-03 (with `W3-CAMERA-01`), boundary packages as planned |
| Wave 4 | Module extraction per backlog order, against the amended topology; `:core:ai` extracted alongside `W4-MOD-D2/D3` (it has the same "depends inward only" shape) |

---

## 8. Risks and guardrails

- **Over-modularization is the main failure mode** for a one-owner project:
  each module adds Gradle config, API surface decisions, and version-catalog
  churn. Guardrails: 17-module ceiling (§6), no module without a named
  future consumer, conventions plugin first (`W4-MOD-01`), and `W2-BUILD-04`
  (KSP) *before* the module count multiplies annotation-processing cost.
- **Windows/OneDrive environment**: more modules = more `build/` directories;
  the known OneDrive sync-lock problem on worktree `app/build` dirs multiplies
  per module. Keep worktrees outside OneDrive (already the practice) and extend
  `clean-generated.ps1` to all modules when they exist. Long nested module paths
  also eat into MAX_PATH headroom for Robolectric DB files — keep module
  directory names short.
- **Extraction before ownership migrations** would freeze `:core:database`
  and then immediately re-open it for `W3-OWN-*` schema changes. The backlog's
  ordering (ownership before `D1`) must not be reshuffled by a coordinator
  optimizing for parallelism.
- **Coach work continuing in the old shape** is the quiet risk: coach is the
  active feature direction, and each new coach capability built on direct
  repository injection makes STRUCT-02 harder. STRUCT-01/02 are cheap now and
  expensive later.
- **Doc drift** (ARCH-003): every STRUCT package must update the docs it
  invalidates in the same PR; the Coordinator should reject structure PRs that
  don't.

## 9. Definition of done for the structure track

The structure track (as distinct from the whole remediation program) is done
when: the amended topology exists with an acyclic, test-enforced graph; `:app`
is composition-only; coach code sits behind `:core:ai` + the context/action
ports; scanners share one capture session with pluggable analyzers; a leaf
change in one feature no longer recompiles the others; and a hypothetical
Glance widget could be built against `:core:data`/`:core:model` without
touching any feature module.
