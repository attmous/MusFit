## Summary

-

## Verification

- [ ] `.\scripts\dev\test-dev-workflow.ps1 -SelfTest` (when live docs, scripts, or CI changed)
- [ ] `.\scripts\dev\verify-musfit.ps1 -Preset Full -RetryOnGeneratedOutputIssue`
- [ ] Seeded emulator verified, or marked N/A with a reason for non-runtime changes
- [ ] UI-visible changes include reviewed screenshot and UI-tree evidence when applicable
- [ ] `$musfit-pr-emulator-evidence` published its marker-based comment and passing status for the current PR head SHA, or the non-runtime skip reason is documented below

## Notes

-
