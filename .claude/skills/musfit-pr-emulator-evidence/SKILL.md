---
name: musfit-pr-emulator-evidence
description: Verify and publish emulator screenshot evidence for MusFit pull requests that change Android runtime functionality or design. Use whenever Claude creates, updates, reviews, hands off, or prepares to merge a PR with runtime Kotlin, Compose UI, Android resources, manifest behavior, or runtime build-configuration changes; skip docs-only, test-only, CI-only, and non-runtime tooling changes.
---

# MusFit PR Emulator Evidence

Read [`../../../.agents/skills/musfit-pr-emulator-evidence/SKILL.md`](../../../.agents/skills/musfit-pr-emulator-evidence/SKILL.md) in full and follow it as the canonical repository workflow.

Do not stop after creating local files under `verification/`. Complete the publisher step and confirm that the marker-based PR comment and `MusFit emulator evidence` status verify the exact current PR head SHA.
