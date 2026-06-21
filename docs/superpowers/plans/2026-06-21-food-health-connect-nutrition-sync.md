# Slice 19: Health Connect Nutrition Sync

## Goal
Add a user-controlled Food Health Connect sync path that writes logged meals as Nutrition records and daily water as Hydration records. Health Connect unavailability or permission gaps must not crash Food.

## Scope
- Persist a Food sync setting and last sync status in Room.
- Extend the existing Health Connect gateway with Food-specific permissions and export DTOs.
- Map logged meal totals and water totals to Health Connect Nutrition/Hydration records.
- Add compact controls in the Food diary for enable, permission request handoff, and manual sync.

## Tests First
- Add ViewModel tests for refreshing sync state, toggling sync, successful sync messaging, and non-blocking sync errors.
- Add HealthConnectManager tests for Food permission exposure and nutrition/hydration writes.
- Add repository coverage for persisted toggle and last failure/success state.

## Implementation Notes
- Export logged entries only; planned meals remain local planning data.
- Use existing Food diary totals so nutrition math stays centralized.
- Request/write Nutrition and Hydration permissions only from the Food surface.
- Keep reads of external nutrition out of MVP unless the existing gateway can support them cleanly.

## Verification
Run focused Food tests first, then `testDebugUnitTest lintDebug assembleDebug` before committing and pushing.
