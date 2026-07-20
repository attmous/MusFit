# R8 release optimization

W1-REL-03 enables full-mode R8 and optimized resource shrinking for every
release build type. MusFit uses `proguard-android-optimize.txt`, dependency
consumer rules, and no app-wide keep rules.

## Measured baseline and result

Measured from `origin/master` at `85e1e5de93e014ca3e66d555e17a418f5a0ee3ec`:

| Artifact | Unminified baseline | Optimized result | Reduction |
| --- | ---: | ---: | ---: |
| Universal production APK | 135,503,830 bytes | 73,117,915 bytes | 62,385,915 bytes (46.04%) |
| Production AAB | 49,700,255 bytes | 39,873,912 bytes | 9,826,343 bytes (19.77%) |

The AAB is below the W1-REL-03 40 MiB budget. ABI-specific Obtainium packaging
remains a later size package; this change does not alter ABI delivery.

## Extended-icon ownership follow-up

W5-SIZE-01 remeasured the optimized artifacts before changing icon ownership.
The historical audit counted 66 imports before feature growth; the current
source uses 88 distinct extended Material vectors. The maintained subset keeps
only those 88 Apache-licensed upstream sources in `:core:designsystem`, and the
`material-icons-extended` dependency is no longer part of the build graph.

Measured from the S19 base `d3c4fe2f8c7fb9a1cefd6a9a0b7f84eaa9fe101e`
with the same production R8 tasks and toolchain:

| Artifact | Extended dependency | Maintained subset | Change |
| --- | ---: | ---: | ---: |
| Universal production APK | 74,550,401 bytes | 74,550,185 bytes | -216 bytes (-0.00029%) |
| Production AAB | 40,673,505 bytes | 40,674,587 bytes | +1,082 bytes (+0.00266%) |

The final-artifact change is neutral because full-mode R8 had already removed
the unused extended-icon bytecode. The accepted benefit is bounded source and
dependency ownership rather than a material download-size reduction; the AAB
remains 1,268,453 bytes below the 40 MiB budget. Roborazzi verifies the existing
icon states, and the checked-in manifest plus workflow policy prevent unused or
unregistered vectors and the extended dependency from returning.

## Configuration analysis

R8 keep-radius analysis covered 190,712 live classes, fields, and methods:

- optimization score: 97.39%;
- obfuscation score: 97.58%;
- shrinking score: 97.62%;
- global disabling rules: none.

The largest retained surfaces are consumer/default rules for bundled ML Kit
text/barcode models, enum contracts, Android views, and Health Connect proto
fields. MusFit does not duplicate or broaden those rules. Six small subsumed
rules also originate in default or dependency configurations, so this package
does not override them.

The API 37 minified smoke reproduced Moshi constructing the reflected
`OpenAiChatCompletionResponse` model as an abstract, vertically merged type
when Profile initialized the coach client. The app rules therefore retain only
the 15 exact reflected coach, Open Food Facts, and GitHub DTOs. A contract test
rejects package-wide MusFit keeps and global optimization disable rules.

## Runtime measurements

Cold-start measurements use the last five of seven launches on clean API 28
and API 37 test emulators. The optimized APK was installed over the unminified
baseline using the same disposable test certificate before measurement.

| Device | Baseline P90 | Optimized P90 | Change |
| --- | ---: | ---: | ---: |
| API 28 | 1,085 ms | 795 ms | -26.73% |
| API 37 | 1,210 ms | 785 ms | -35.12% |

Both devices completed the production-shaped Today, Food, Training, and
Profile smoke without a crash after the narrow Moshi rule. API 37 also imported
and restored the canonical 1,681-row seeded archive in the minified build;
Today restored 1,443 of 2,450 kcal, Food restored its four-item breakfast, and
Training restored the in-progress Full Body A workout. No physical device was
used.

## Retained reports

CI retains AGP's `mapping.txt` plus `usage.txt`, `seeds.txt`, and
`configuration.txt` from the dedicated `outputs/r8Reports` tree for both
`productionRelease` and `legacyMigrationRelease` for 30 days. Never publish
those reports with public release artifacts; operators use the exact build's
mapping privately for crash deobfuscation and rollback analysis.

Run after the release variants are built:

```powershell
.\scripts\release\verify-r8-artifacts.ps1
```

Any release-only failure must be reproduced before adding a keep rule. Add the
narrowest class/member rule possible and rerun the minified critical-flow and
size gates.
