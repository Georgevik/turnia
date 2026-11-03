# Design

## Context

See proposal.md - Why. `GroupDetailViewModel.onSave()` sets `saving = true` inside the coroutine it
launches rather than before launching it, unlike every sibling action already audited in this
session. `GroupDetailScreen.kt`'s save button only disables via `enabled = ... && !state.saving &&
...` (line ~541), so the fix has to make `saving` true before a second tap can be processed, not
only eventually.

## Goals / Non-Goals

**Goals:**
- A repeated save request while one is already in flight creates or updates nothing a second time.
- Match the exact shape already used by `ShiftSetupViewModel.confirm()`/`skip()` and the other
  audited actions, rather than a bespoke mechanism.

**Non-Goals:**
- `closeGroup()` (leave/delete) and `answerRequest()` (accept/reject a join request) are not
  touched — see proposal.md - Impact for why they're deliberately deferred.
- No change to `GroupRepository.createGroup`/`updateGroup` or any Firestore write. As with the
  shift-setup fix, each individual write the user triggers is correct; the bug is that the UI could
  let the user trigger it twice.
- No retroactive OpenSpec coverage of the rest of `GroupDetailViewModel` (loading, members,
  requests, leave/delete). The `group-management` capability starts scoped to this one requirement;
  widening it is a separate decision.

## Decisions

**Move `updateSuccess { it.copy(saving = true) }` out of the `viewModelScope.launch` block, to the
top of `onSave()`, and add `if (state.saving) return` as an explicit early-return guard**, mirroring
`ShiftSetupViewModel.confirm()`'s `if (!state.canConfirm) return` / `CompleteNameViewModel.onContinue()`'s
`if (state.saving) return`. Both changes are needed together: the explicit guard makes the
precondition legible at the top of the function (matching the sibling flows' shape), and moving the
flag-set out of `launch` is what actually closes the race regardless of dispatcher timing, since the
guard and the flag it reads are now both evaluated synchronously on the same call.

Alternative considered: only add the `if (state.saving) return` guard, leaving the flag update
inside `launch`. Rejected — the guard alone still depends on the first `launch`'s body having
already run far enough to set `saving = true` before a second call reads `_uiState.value`, which is
exactly the dispatcher-timing assumption this change exists to remove.

## Risks / Trade-offs

- [Risk] `closeGroup()`/`answerRequest()` keep the same structural shape this change removes from
  `onSave()`, so the codebase is left inconsistent. → Mitigation: explicitly called out in
  proposal.md so it reads as a deliberate scope decision, not an oversight; low impact because a
  repeated leave/delete/accept/reject fails or no-ops against the backend rather than duplicating an
  entity.
- [Risk] Groups already duplicated by this bug before the fix ships stay duplicated. → Mitigation:
  out of scope, same trade-off already accepted for `fix-shift-setup-duplicate-confirm`.
