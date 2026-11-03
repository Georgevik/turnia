# Tasks

## 1. Guard against a repeated save

- [x] 1.1 Add a reproducing test to `GroupDetailViewModelTest.kt`: calling `onSave()` twice in a
      row (new-group creation, `groupId == null`) leaves exactly one `createGroup` call recorded by
      the fake `GroupRepository`. Verify it fails against the current code before the fix (red).
- [x] 1.2 Move `updateSuccess { it.copy(saving = true) }` out of the `viewModelScope.launch` block
      in `GroupDetailViewModel.onSave()` to before it, and add an explicit
      `if (state.saving) return` guard at the top of the function, mirroring
      `ShiftSetupViewModel.confirm()`. Verify the test from 1.1 now passes.
- [x] 1.3 Add the same reproducing test for the edit path (`groupId != null`): calling `onSave()`
      twice in a row on an already-loaded group leaves exactly one `updateGroup` call. Verify it
      passes.
- [x] 1.4 Add a test that a save can be retried after a failure: `onSave()` fails once (fake
      repository returns failure), `saving` and the guard both clear, and a second `onSave()` call
      does reach the repository. Verify it passes.

## 2. Regression check

- [x] 2.1 Run the full `GroupDetailViewModelTest` suite and verify all existing tests still pass
      (loading, member removal, leave/delete, join requests are untouched by this change).
- [x] 2.2 Manually verify on a fresh account (emulator or device): open "Create a group", fill the
      name, and rapid-tap the create button several times. Verify only one group is created (check
      Firestore emulator data or the Groups tab afterward) and the invite screen shows normally.
