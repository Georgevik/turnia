# Proposal

## Why

A user on iOS 1.11.0 tapped the shift setup's confirm button repeatedly after it appeared
unresponsive, and on relaunch the account had many duplicate "Morning"/"Afternoon" personal event
types. Root cause, confirmed with a reproducing unit test: `ShiftSetupUi.canConfirm` only checks
`selectedCount > 0 && !saving`. After a successful `confirm()`, `saving` flips back to `false`
(which is what the user saw as the "Crear" label reappearing) before the screen actually leaves
composition — `NavDisplay` animates the exit over ~200ms, during which the confirm button stays
enabled. Any tap landing in that window re-runs `confirm()`, which mints fresh random ids
(`EventTypeId(createUuid())`) and writes a brand-new batch of personal event types with no check
for what was already created. Each repeat tap really does succeed, so duplicates accumulate for
real. The same race was already identified and fixed for `skip()` (a `skipping` guard with a
comment explaining the case) but never applied to `confirm()`.

## What Changes

- Confirming the shift setup a second time while the screen is finishing its close (already
  succeeded, or still saving) must not create a second batch of event types.
- `ShiftSetupViewModel.confirm()` gets the same re-entrancy guard `skip()` already has, so a
  tap that lands after success (or while a write is in flight) is a no-op instead of a new write.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shift-setup`: the "Confirming creates the selected shifts as personal event types" requirement
  gains behavior for a repeated confirm — a second confirm while saving, or after the setup has
  already succeeded and is closing, must not create a second set of personal event types.

## Impact

- `app/shared/src/commonMain/kotlin/com/geoviksoft/turnia/ui/shiftsetup/ShiftSetupViewModel.kt`
  (`confirm()`)
- `app/shared/src/commonMain/kotlin/com/geoviksoft/turnia/ui/shiftsetup/model/ShiftSetupUi.kt`
  (`canConfirm`)
- Test coverage: `app/shared/src/commonTest/kotlin/com/geoviksoft/turnia/ui/shiftsetup/ShiftSetupViewModelTest.kt`
- No backend/Firestore changes; no data migration for existing accounts already affected by the
  bug (out of scope — a user who already has duplicates is not cleaned up by this change).
