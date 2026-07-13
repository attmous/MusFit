# Static-quality ratchet

W2-QUALITY-01 makes formatting and static analysis part of the normal MusFit
verification graph without changing runtime artifacts.

## Gates

- Spotless 8.8.0 with ktlint 1.8.0 checks changed Kotlin, Gradle Kotlin, and
  repository text files relative to `origin/master`.
- Detekt 1.23.8 analyzes production Kotlin source and fails findings not present
  in `config/detekt-baseline.xml`.
- Android lint treats warnings as errors for internal, migration, and production
  variants. Existing non-security/non-correctness debt is pinned in
  `app/lint-baseline.xml`.
- `scripts/quality/test-static-quality.ps1 -SelfTest` requires every baseline
  ID and occurrence to have an owner, rationale, and non-expired removal date in
  `config/static-analysis-debt.json`. Count drift fails closed.

Security and correctness findings are fixed or narrowly suppressed only when
the warning conflicts with an intentional, tested contract. They are never
admitted to the lint baseline. Dependency-version warnings remain governed by
the stable-first Dependabot policy and are removed through separate dependency
PRs.

## Local commands

```powershell
. .\scripts\android\android-env.ps1
.\scripts\quality\test-static-quality.ps1 -SelfTest
.\gradlew.bat spotlessCheck detekt lintInternalDebug lintLegacyMigrationRelease lintProductionRelease --no-daemon --console=plain
```

To remove debt, fix the finding, regenerate only the affected baseline, update
the matching occurrence count or remove its register entry, and run the full
gate. Extending an expiry requires a reviewed rationale change in the same PR.
