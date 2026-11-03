# Proposal

## Why

An onboarding hardening audit (triggered by the confirmed `fix-shift-setup-duplicate-confirm`
race) found the same class of bug in `GroupDetailViewModel.onSave()` — the group creation/edit
screen reached directly from the team prompt's "Create a group" choice. Every other write-triggering
action audited in the onboarding-adjacent surface (`ShiftSetupViewModel.confirm()`/`skip()`,
`CreateAccountViewModel.onSubmit()`, `SignInViewModel.onEmailSubmit()`,
`CompleteNameViewModel.onContinue()`, `GroupsViewModel.requestToJoin()`,
`TeamPromptViewModel.answered()`) sets its own busy flag with a synchronous `_uiState.update{}`
call before calling `viewModelScope.launch`, so a second tap reads the flag already set and is a
no-op. `onSave()` is the one exception: it sets `saving = true` *inside* the launched coroutine,
after `viewModelScope.launch` has already been called, with no `if (state.saving) return` guard at
all. The button's `enabled` state in `GroupDetailScreen.kt` depends on `saving` already being
`true` by the time a second tap is processed — an assumption about coroutine dispatch timing that
is unverified across platforms, and the same category of assumption that let the shift-setup bug
reach production on iOS. If it is exploitable here, the result is worse than duplicate event
types: a second, fully separate group document, each with its own invitation code and member list.

## What Changes

- `GroupDetailViewModel.onSave()` gets the same synchronous re-entrancy guard every sibling action
  already has: the busy flag is set before the write is launched, and a tap that lands while a save
  is already in flight is a no-op.
- This is the first OpenSpec-tracked behavior for group creation/edit, so it introduces a new
  `group-management` capability scoped to this one requirement — not a full retroactive spec of
  everything `GroupDetailViewModel` does.

## Capabilities

### New Capabilities

- `group-management`: creating and saving a group (the form reached from Groups' "+" and from the
  team prompt's "Create a group"), scoped here to the single requirement that a repeated
  confirm/save cannot create or save a group twice.

### Modified Capabilities

None.

## Impact

- `app/shared/src/commonMain/kotlin/com/geoviksoft/turnia/ui/group/detail/GroupDetailViewModel.kt`
  (`onSave()`)
- Test coverage: `app/shared/src/commonTest/kotlin/com/geoviksoft/turnia/ui/group/detail/GroupDetailViewModelTest.kt`
- Out of scope: `closeGroup()` (backs `onLeaveGroup()`/`onDeleteGroup()`) and `answerRequest()`
  (backs accept/reject on a join request) share the same structural shape — their busy/result state
  is also set inside their `launch` block, with no pre-check — but a repeated leave, delete, accept
  or reject is far lower-impact (the second call fails or is a no-op against Firestore/the Cloud
  Function rather than creating a duplicate entity) and is not fixed by this change. Flagged here so
  it isn't mistaken for a verified-safe pattern; a follow-up change can cover it if warranted.
- No backend/Firestore changes; no cleanup for any group already duplicated by this bug before the
  fix ships (out of scope, same as the sibling shift-setup change).
