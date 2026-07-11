---
name: musfit-pr-emulator-evidence
description: Verify and document MusFit pull requests that change Android functionality or design by running the repository's complete local gate, installing deterministic seed data on the MusFit emulator, exercising the affected workflow, capturing reviewed screenshot and UI-tree evidence, waiting for PR checks, and upserting an evidence comment. Use whenever Codex creates, updates, reviews, or prepares to merge a MusFit PR whose diff changes runtime Kotlin, Compose UI, Android resources, manifest behavior, or build configuration; skip docs-only, test-only, CI-only, and non-runtime tooling changes.
---

# MusFit PR Emulator Evidence

Use this repo-scoped skill from the MusFit repository root. Reuse the repository's Android, verification, and GitHub helpers rather than depending on a user-level skill.

## Non-negotiable evidence rules

- Test the exact committed PR head. Do not publish evidence from a dirty worktree or a different SHA.
- Use `MusFit_API36` and an `emulator-*` serial. Never substitute the physical phone.
- Reset to deterministic seed data. Never upload personal data, credentials, notifications, provider tokens, or phone screenshots.
- Exercise the changed workflow; the automatic post-launch screenshot is bootstrap evidence, not sufficient PR proof.
- Capture the success state for each changed user workflow. Capture both light and dark mode for design changes.
- Inspect every selected PNG with the image viewer before publishing. Reject transitional, blank, clipped, keyboard-covered, secret-bearing, or wrong-screen captures.
- Treat any new commit as invalidating all prior test and screenshot evidence.
- Do not merge when a required scenario cannot be verified. State the limitation on the PR instead.

## 1. Decide whether the skill applies

Compare the PR branch with its base and inspect the actual diff.

Run this workflow when production behavior can change, including changes under:

- `app/src/main/java/`
- `app/src/main/res/`
- `app/src/main/AndroidManifest.xml`
- runtime Gradle or version-catalog configuration

Skip it only when the diff is exclusively documentation, tests, CI, repository metadata, or non-runtime tooling. Record the skip reason in the PR verification section.

Derive concrete scenarios from the diff, changed tests, PR description, and the relevant architecture document. Each scenario must name an action and an observable expected result.

## 2. Prepare the exact PR head

Commit the intended changes on the scoped branch, push it, and create or update the draft PR. Preserve unrelated user changes. Work from a clean worktree dedicated to the PR if unrelated tracked changes prevent a clean build.

Confirm that local `HEAD` equals the PR head SHA before testing:

```powershell
$pr = 123
$localHead = (git rev-parse HEAD).Trim()
$remoteHead = (gh pr view $pr --json headRefOid --jq .headRefOid).Trim()
if ($localHead -ne $remoteHead) { throw "Local HEAD does not match PR head" }
```

## 3. Run every local gate and reset the emulator

Resolve this skill directory, create a unique gitignored evidence directory, and run the gate helper:

```powershell
$repoRoot = (git rev-parse --show-toplevel).Trim()
$skillRoot = Join-Path $repoRoot ".agents\skills\musfit-pr-emulator-evidence"
$shortHead = (git rev-parse --short=12 HEAD).Trim()
$evidenceDir = "verification\pr-evidence\$shortHead-$(Get-Date -Format yyyyMMdd-HHmmss)"
& "$skillRoot\scripts\invoke-musfit-pr-gate.ps1" -OutputDir $evidenceDir
```

The helper must pass all of these before it writes `verification.json`:

1. `scripts/dev/verify-musfit.ps1 -Preset Full -RetryOnGeneratedOutputIssue`
2. Seeded debug install/reset on the emulator
3. Explicit `MainActivity` launch and foreground confirmation

When the same PR also changes repository workflow helpers, pass `-IncludeWorkflowContract` to run `scripts/dev/test-dev-workflow.ps1`. Do not make unrelated functionality/design PRs inherit stale documentation-contract failures.

If generated output causes a known build failure, allow the repository helper's single cleanup-and-retry path. Do not bypass real test, lint, build, install, seed, launch, or crash failures.

## 4. Exercise and capture affected workflows

Clear assumptions before tapping: inspect the UI tree, find current bounds, then navigate. Prefer semantic text and UI-tree bounds over fixed coordinates. Keep logcat clear from the gate so any subsequent MusFit crash remains attributable to the scenarios.

Capture each stable expected state with a descriptive, ordered name:

```powershell
& "$skillRoot\scripts\capture-emulator-evidence.ps1" `
  -EvidenceDir $evidenceDir `
  -Name "01-food-entry-saved" `
  -Caption "A saved food entry appears in Lunch with updated calories" `
  -RequireText "Lunch", "Calories"
```

For a design change, capture the relevant surface in both modes:

```powershell
$serial = (Get-Content "$evidenceDir\verification.json" -Raw | ConvertFrom-Json).device.serial
adb -s $serial shell cmd uimode night no
adb -s $serial shell am force-stop com.musfit
adb -s $serial shell am start -W -n com.musfit/.MainActivity
# Navigate to the changed surface, then capture it.
& "$skillRoot\scripts\capture-emulator-evidence.ps1" `
  -EvidenceDir $evidenceDir -Name "02-food-light" `
  -Caption "Food diary in light mode" -Theme Light -RequireText "Food"

adb -s $serial shell cmd uimode night yes
adb -s $serial shell am force-stop com.musfit
adb -s $serial shell am start -W -n com.musfit/.MainActivity
# Navigate to the same surface again, then capture it.
& "$skillRoot\scripts\capture-emulator-evidence.ps1" `
  -EvidenceDir $evidenceDir -Name "03-food-dark" `
  -Caption "Food diary in dark mode" -Theme Dark -RequireText "Food"
```

The capture helper is observational: it never launches, restarts, taps, or changes the theme because doing so could destroy the exact navigated state under test. It refuses non-emulator devices, stale receipts, background-app states, theme mismatches, missing required text, MusFit crash-buffer entries, corrupt PNGs, and duplicate names. It writes PNG/XML/metadata locally, but only reviewed PNGs and the sanitized verification receipt are published.

Open every PNG with the image viewer. Repeat a capture rather than publishing a weak image. Restore the emulator theme when finished:

```powershell
$serial = (Get-Content "$evidenceDir\verification.json" -Raw | ConvertFrom-Json).device.serial
adb -s $serial shell cmd uimode night auto
```

## 5. Confirm GitHub checks and publish one evidence comment

Wait for the PR checks on the same head:

```powershell
gh pr checks $pr --watch --fail-fast --interval 10
```

After visually reviewing all captures, publish them:

```powershell
& "$skillRoot\scripts\publish-pr-evidence.ps1" `
  -PullRequest $pr `
  -EvidenceDir $evidenceDir `
  -ConfirmVisualReview
```

The publisher:

- revalidates the receipt and PR head SHA;
- uploads selected PNGs to the separate `pr-evidence` branch through GitHub's Git Data API;
- keeps screenshots out of the feature branch and PR diff;
- uses immutable commit-pinned image URLs;
- creates or updates one marker-based PR comment instead of spamming comments;
- includes exact local commands, emulator identity, scenario captions, and the evidence commit.

The repository is public, so anything on `pr-evidence` is public. Stop before publishing if a screenshot contains non-seeded or sensitive content.

Use `-DryRun -PullRequest 0` to render `pr-comment-preview.md` locally without reading or changing GitHub state.

## 6. Revalidate before merge

Immediately before merge, query the PR head and compare it with `verification.json`. If the SHA changed, rerun the entire gate, reset/seed, scenarios, visual inspection, PR checks, and publication. Do not reuse screenshots across commits.

Report the verified SHA, commands, scenarios, screenshot count, PR comment URL, and any limitation in the final handoff.
