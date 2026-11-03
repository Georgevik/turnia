# Tasks

## 1. The add pane: one event per visit

- [x] 1.1 Add a latch to `DayDetailSheetViewModel` guarding `addEventOfType()`: a `private var`
  set synchronously before anything is launched, following `ShiftSetupViewModel.confirming`, so a
  second call returns without writing. Move `adRepository.actionPerformed()` inside the guard so it
  fires once per accepted add, not once per tap. Verify by `./gradlew :app:shared:compileKotlin`
  (or the IDE) building clean.
- [x] 1.2 Add a `sheetClosed()` (or equivalently named) function on the view model that clears the
  latch, and call it from the existing `DisposableEffect(Unit) { onDispose { … } }` in
  `DayDetailSheet` alongside the one-off form cancel. Verify the call compiles and sits in that same
  `onDispose`, so the reset shares the path that already resets form state.
- [x] 1.3 Create `DayDetailAddEventTest` in `app/shared/src/commonTest/.../ui/components/daydetail/`,
  building the view model from the `Demo*` fakes the way `DayDetailNotesTest` does. Verify these
  pass, with no clock or virtual-time advance: three `addEventOfType` calls for the same type write
  one event; a second call for a *different* type after the first writes nothing; `countAdded`
  raises the prompt counter by one across three taps.
- [x] 1.4 Extend the same test with the reset: add, then `sheetClosed()`, then add again writes a
  second event. Verify it passes — this is the regression guard for the view model being keyed by
  date and outliving one opening of the sheet.
- [x] 1.5 Add `rapidRepeatedShiftTaps_addOnlyOneEvent` to `CalendarFlowsTest`, mirroring
  `rapidRepeatedCreateTaps_createOnlyOneGroup` in `GroupFlowsTest`: open a day, tap the shift chip
  five times guarded by a still-present check, then `awaitDayClosed()`. Verify by asserting
  `FirestoreRest`/`Documents` shows exactly one event document for that date and type, and that the
  `android…AndroidTest` run passes against the emulators.

## 2. The event type form: one type per save

- [x] 2.1 Add the re-entrancy guard to `EventTypeDetailViewModel.onSave()`: return early when a save
  is already in flight or has already succeeded, and set the busy flag synchronously **before**
  `viewModelScope.launch` rather than inside it, matching `GroupDetailViewModel.onSave()`. Verify it
  compiles and that the guard sits before the field validation's early return is bypassed — a blank
  name must still mark the field rather than latch the action.
- [x] 2.2 Stop the success branches of `savePersonal()` and `saveGroupType()` re-enabling the button:
  emit `isSaved = true` without flipping `saveButtonLoading` back to `false`, so the state change
  that triggers `navigator.goBack()` no longer makes the button live for the pop animation. Keep the
  failure branches clearing the flag so a rejected save stays retryable. Verify by reading
  `EventTypeDetailScreen`'s `enabled = !state.saveButtonLoading` and confirming no success path
  leaves it enabled.
- [x] 2.3 Guard the no-group-yet path in `saveGroupType()`, which writes the pending type and sets
  `isSaved` synchronously with no busy flag at all. Verify a repeated save leaves one type waiting:
  `PendingEventTypes.set` dedupes by id, so the guard must stop a fresh `EventTypeId(createId())`
  being minted per tap.
- [x] 2.4 Create `EventTypeDetailViewModelTest` in
  `app/shared/src/commonTest/.../ui/main/eventtypes/detail/`, using the `Demo*` fakes as
  `GroupDetailViewModelTest` does. Verify these pass, clock-free: two `onSave()` calls on a new
  personal type create one type; a save after success creates nothing more; two saves on a new
  group type leave one type pending; two saves of an *edit* update one type and create none.
- [x] 2.5 Extend that test with the retry path: a first save that the fake rejects leaves the form
  open with an error and the action available, and a second save then succeeds with exactly one type
  created. Verify it passes — this is the guard against the latch bricking the button.
- [x] 2.6 Add `rapidRepeatedSaveTaps_createOnlyOneShift` to the E2E suite along
  `createPersonalType_thenAddAnEventWithANote` in `CalendarFlowsTest`: fill *New shift*, tap Save
  five times with the same still-present guard, then assert `Documents` holds exactly one
  `users/alice/personalEventTypes` document with that name. Verify the run passes against the
  emulators.

## 3. Integration verification

- [x] 3.1 Run the full shared unit suite and verify it passes with no regression in
  `ShiftSetupViewModelTest`, `GroupDetailViewModelTest`, `DayDetailNotesTest` or
  `EventAddedPromptsTest` — the prompt-counter change in 1.1 is the one most likely to disturb an
  existing expectation.
- [x] 3.2 Run the Android E2E suite and verify it passes, including the two new tests and the
  existing `addGroupEvent_fromTheDaySheet` and `createPersonalType_thenAddAnEventWithANote`, which
  exercise the same two actions at normal speed and must be unaffected.
- [x] 3.3 Verify by hand on a device that the share prompt still appears at its milestone after
  normal single-tap adds, and that a failed save (airplane mode) still shows its error and accepts a
  retry on both the add pane and the shift form.
