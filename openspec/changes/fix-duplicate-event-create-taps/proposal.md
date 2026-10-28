# Proposal

## Why

`fix-shift-setup-duplicate-confirm` fixed a confirmed production bug: a user whose write looked
unresponsive tapped the button again, and because the screen was still composed and still enabled
while it animated away, the second tap minted fresh ids and wrote a second, fully real copy.
`fix-group-create-duplicate-confirm` then audited the onboarding-adjacent surface and fixed the one
sibling it found. Neither audit reached the two surfaces a new user actually touches *next*, and both
are still unguarded today:

- **`DayDetailSheetViewModel.addEventOfType()`** has no guard of any kind — no busy flag, no state
  check — and `EventTypeChip` has no disabled state. `DayDetailSheet` calls it and then `onClose(true)`,
  which is `CalendarViewer.dismissSheet()`: an animated `sheetState.hide()` that only clears
  `sheetDate` once the slide-down completes, so the chip stays composed and clickable throughout.
  Each tap mints a new `EventId(Uuid.random())`, and nothing downstream dedupes — `addEvent` calls
  `set()` with whatever id it is handed, so a different id is a different document. Tapping a shift
  three times puts three identical events on the day. This is the surface the shift setup sends every
  new user to with its "tap a day to add a shift" hint.

- **`EventTypeDetailViewModel.onSave()`** is the exact shape the sibling proposal flagged as the one
  dangerous exception: no re-entrancy guard, and `saveButtonLoading = true` set *inside*
  `viewModelScope.launch` rather than before it. Worse, its success branch emits
  `saveButtonLoading = false` and `isSaved = true` together — the button is re-enabled by the very
  state change that triggers `navigator.goBack()`, so it is enabled for the whole Nav3 pop animation.
  The group path with no group yet is worse still: it writes the pending type and sets `isSaved`
  synchronously, never setting a busy flag at all.

The harm is confined to creation. The edit paths reuse `key.typeId`, so a repeated save overwrites
one document and is idempotent. The create paths mint a fresh id per call, so each extra tap is a new
entity. For a group's first type this compounds: `PendingEventTypes.set` dedupes by id, but a fresh id
per tap defeats it, and the duplicates are then written into the group in its single creation write —
a group created from the team prompt can start life with "Morning" listed twice.

Duplicate taps also inflate `countAdded`, which feeds the share prompt's counter, so a fat-fingered
add can burn a share milestone the user never earned.

## What Changes

- Adding a shift from the add pane SHALL create exactly one event however many times the chip is
  tapped, including taps that land while the sheet is animating closed.
- Saving a new event type SHALL create exactly one type however many times the save button is
  tapped, including taps that land while the write is in flight and taps that land after it
  succeeded, while the screen animates away.
- Both get the re-entrancy idiom the two shipped fixes already use: a flag set **synchronously
  before** the write is launched, latched so a success never re-enables the action, and reset only
  on failure so a failed write stays retryable.
- Each gets a clock-free unit test in the shape of the existing `confirmingTwiceCreatesOnlyOnce`.
- No new shared primitive and no app-wide change. A time-window debounce or throttle was considered
  and rejected as the primary fix; `design.md` records why, so it is not relitigated.

## Capabilities

### New Capabilities

- `event-type-management`: creating and saving an event type — the form reached from *My shifts*,
  from the add pane's create action, from "Save as shift", and from a group's type list. Scoped here
  to the single requirement that a repeated save cannot create two types, following the precedent of
  `group-management`, which `fix-group-create-duplicate-confirm` introduced scoped to one
  requirement rather than retroactively specifying everything its view model does.

### Modified Capabilities

- `event-add-pane`: the "Shifts come first, one-off events last" requirement states that tapping a
  shift adds it with a single tap. It gains the behavior for repeated taps — several taps on the same
  shift, including taps during the sheet's closing animation, add exactly one event.

## Impact

- `app/shared/src/commonMain/kotlin/com/geoviksoft/turnia/ui/components/daydetail/DayDetailSheetViewModel.kt`
  (`addEventOfType()`)
- `app/shared/src/commonMain/kotlin/com/geoviksoft/turnia/ui/main/eventtypes/detail/EventTypeDetailViewModel.kt`
  (`onSave()`, and the success branch of `savePersonal()` / `saveGroupType()` that re-enables the
  button while navigating away)
- Test coverage: `DayDetailSheetViewModelTest` and `EventTypeDetailViewModelTest`
- No backend, Firestore, security-rule or fixture changes. No Cloud Function changes.
- No cleanup of duplicates already created by this bug, matching the scope of both sibling changes.
- Out of scope, recorded in `design.md` as a follow-up: no client-side guard covers a second device
  or an offline queue flush. If "a day holds at most one event of a given type" is a real domain
  rule — `move-personal-to-group` already skips a day that holds a shift of the target group, so one
  may exist implicitly — it belongs in the repository, not in button state.
- Out of scope: `removeEvent`, `takeEvent`, `returnEvent`, `setOnSwap` and `saveNotes` on
  `DayDetailSheetViewModel` are also unguarded, but each is idempotent or server-checked against a
  known event id, so a repeated call cannot create a second entity. Flagged so they are not mistaken
  for verified-safe, as the sibling change did for `closeGroup()` and `answerRequest()`.
