# Experimental adaptive API watchlist

Status date: 2026-07-20. This is the living W5-PLAT-01 watchlist for
PLAT-001. It records emerging Compose layout APIs without adding a production
dependency, opt-in, feature flag, or API usage.

MusFit currently uses Compose BOM `2026.06.01`, which resolves Compose UI and
Foundation to stable `1.11.4`. The app has `minSdk 28`, `targetSdk 37`, and
`compileSdk 37`. AndroidX currently lists `1.11.4` as the stable Compose
UI/Foundation release and `1.12.0-beta02` as the beta release. A stable
artifact version does not make an API stable: the current Android guides still
label MediaQuery, non-lazy Grid, and FlexBox experimental.

Primary references:

- [AndroidX versions](https://developer.android.com/jetpack/androidx/versions)
- [Compose Foundation release notes](https://developer.android.com/jetpack/androidx/releases/compose-foundation)
- [MediaQuery guide](https://developer.android.com/develop/adaptive-apps/guides/mediaquery)
- [Grid guide](https://developer.android.com/develop/adaptive-apps/guides/grid)
- [FlexBox guide](https://developer.android.com/develop/adaptive-apps/guides/flexbox)

## Current decision

| Candidate | Current API status | MusFit scenario review | Decision |
| --- | --- | --- | --- |
| MediaQuery | Experimental Compose UI API; requires `ComposeUiFlags.isMediaQueryIntegrationEnabled`. | Stable window-size/adaptive navigation and pane scenes cover current phone, tablet, foldable, and resizable-window decisions. No current screen needs pointer precision, keyboard kind, device capability, viewing distance, or a new posture branch. | Defer. Do not enable the integration flag or call `mediaQuery`/`derivedMediaQuery` in production. |
| Non-lazy Grid | Experimental Foundation Layout API; it composes every child and explicitly does not support lazy loading. | MusFit's large Food and Training collections already require lazy lists/grids. Small Today, Profile, Training-history, and filter layouts are covered by stable `Row`/`Column`, existing measured grids, and `LazyVerticalGrid`; no required two-dimensional span is missing. | Defer. Do not replace a lazy collection or a working bounded layout for API novelty. |
| FlexBox | Experimental Foundation Layout API with `@ExperimentalFlexBoxApi`; it is intended for a small item count and does not support lazy loading. | Food's current `FlowRow` uses are bounded chip/action groups and have reviewed large-font/RTL coverage. No screen currently requires grow, shrink, basis, reordering, or multi-line space distribution that stable layouts cannot express. | Defer. Keep stable `FlowRow`/`FlowColumn` or lazy containers until a concrete layout contract proves insufficient. |

## Adoption gates

Every gate below is mandatory for a future proposal. A proposal that cannot
fill in one item remains watchlist-only.

1. **Stable API proof.** Link the stable AndroidX release notes and reference
   docs showing that the exact API and required types have left experimental
   status. A stable library version alone is insufficient. The proposal must
   require no experimental annotation, global integration flag, beta/RC/alpha
   artifact, or suppressed opt-in.
2. **Unmet user scenario.** Name one screen, window/device state, and user task
   whose accepted contract is not met by the current stable implementation.
   Include a reproducible screenshot, UI-tree, issue, or failing test. Code
   brevity and similarity to CSS are not user scenarios.
3. **Stable-alternative comparison.** Document why current window-size classes,
   Material adaptive/navigation scenes, `Row`/`Column`, `FlowRow`/`FlowColumn`,
   and lazy lists/grids cannot meet that scenario. Preserve compact behavior
   and existing Navigation 3 restoration/back semantics.
4. **Compatibility proof.** Confirm the resolved dependency graph and API's
   documented minimum SDK, then pass the scenario on MusFit's API 28 and API 37
   managed devices. Test compact, medium, expanded, split-screen, and applicable
   fold/posture changes without raising `minSdk 28` or weakening target 37
   behavior.
5. **Behavior and accessibility coverage.** Add focused state/layout tests,
   Compose semantics assertions, and reviewed screenshots for light/dark,
   LTR/RTL, 1.5x font scale, and the relevant window/device states. Visual and
   semantic traversal order must remain aligned, especially if Grid placement
   or FlexBox ordering can differ from composition order.
6. **Measured benefit.** Capture the current stable implementation and exact
   candidate on the same controlled device. Name the metric before the trial.
   Require at least a 10% improvement in the selected median/P90 performance
   metric or a quantified elimination of the declared layout failure, with no
   regression in critical journeys, memory budgets, or lazy behavior.
7. **ADR and rollback.** Add an ADR that records the selected API/artifact,
   alternatives, dependency and binary-size impact, test/benchmark evidence,
   owner, review date, and the exact stable implementation to restore. The
   rollback must be a bounded code/dependency change, not a rewrite.

## Candidate-specific evidence

### MediaQuery

A proposal must identify a capability beyond the current window-size and
Material adaptive scene inputs. It must dynamically test every queried value
that affects UI, including changes while the activity remains alive. Width or
height branching must demonstrate why stable window-size classes are
insufficient and must avoid high-frequency recomposition. Preview/test
injection must cover the queried `UiMediaScope` values. Enabling a process-wide
Compose UI flag is prohibited until the stable API no longer requires it.

### Non-lazy Grid

A proposal must prove that the child set is small and bounded and that the
layout needs true two-dimensional tracks, spans, or alignment that the current
stable implementation cannot provide. It must record maximum child count,
intrinsic-measure behavior, composition count, and frame/memory comparison.
Large Food/Training catalogs remain on lazy containers. Any nested lazy content
or flexible track requires an explicit regression test for measurement and
scroll behavior.

### FlexBox

A proposal must prove that the bounded item group needs grow, shrink, basis,
wrap distribution, or ordering semantics beyond stable `FlowRow`/`FlowColumn`.
It must test overflow, minimum intrinsic sizes, RTL, font scaling, and
composition-versus-visual traversal order. Large or unbounded collections
remain lazy. Ordering must not create a visual order that conflicts with TalkBack
or keyboard focus.

## Review cadence

Recheck this file only when AndroidX publishes a new stable Compose UI or
Foundation line, an applicable MusFit layout issue is accepted, or an official
API status changes. Update the status date and primary links from current
official sources. Until all adoption gates pass, the correct action is no
production change.
