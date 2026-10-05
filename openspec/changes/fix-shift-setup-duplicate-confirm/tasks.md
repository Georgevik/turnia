# Tasks

## 1. Guard against a repeated confirm

- [x] 1.1 Add a reproducing test to `ShiftSetupViewModelTest.kt`: calling `confirm()` twice in a
      row leaves exactly one entry in `repository.completed` (mirrors
      `skippingTwiceBeforeTheScreenClosesReportsOnce`, which guards the equivalent race for
      `skip()`). Verify it fails against the current code before the fix (red).
- [x] 1.2 Add a `confirming` re-entrancy flag to `ShiftSetupViewModel.confirm()`, set at entry and
      cleared only on failure, matching the shape of `skip()`'s existing `skipping` flag. Verify
      the test from 1.1 now passes.
- [x] 1.3 Change `ShiftSetupUi.canConfirm` to also require `!closed`
      (`selectedCount > 0 && !saving && !closed`). Verify with a unit test that `canConfirm` is
      `false` once `closed` is `true`, even with a selection and `saving == false`.
- [x] 1.4 Add a test covering a confirm tap arriving after a successful confirm (state already
      `closed`): verify it creates no second write and `repository.completed` still holds exactly
      one entry. Verify the full `ShiftSetupViewModelTest` suite passes.

## 2. Regression check

- [x] 2.1 Run the full `shift-setup` test suite (`ShiftSetupViewModelTest`,
      `ShiftSetupRepositoryImplTest`, `ShiftPresetTest`) and verify all tests pass, confirming the
      existing skip-race protection and the other shift-setup scenarios are unaffected.
- [x] 2.2 Manually verify on a fresh account (emulator or device): open the shift setup, tap
      confirm, and rapid-tap the button several more times immediately after. Verify only one set
      of personal event types is created (check Firestore emulator data or the calendar/add pane
      afterward) and the setup closes onto the calendar with the hint, as before.
