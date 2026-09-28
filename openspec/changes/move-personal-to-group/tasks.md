# Tasks

## 1. Shared vocabulary

- [x] 1.1 Add `RetentionWindow.start(today)` (today minus one month) in core domain. Switch `SharedCalendarRepositoryImpl.purgedBefore()` to it. Verify with a unit test covering 31 March → 28/29 February and 15 October → 15 September, and that `SharedCalendarRepositoryImpl` tests still pass.
- [x] 1.2 Add `AnalyticsEvent.PersonalEventsMoved(scope, eventCount, skippedCount)` (`personal_events_moved`, params `scope` = `one`|`all`, `event_count`, `skipped_count`) and `AnalyticsEvent.GroupEventNotesSaved` (`group_event_notes_saved`). Verify with an `AnalyticsEvent` test asserting the names and parameter values.
- [x] 1.3 Update CLAUDE.md:
  - add `scope`, `event_count` and `skipped_count` to the event-scoped parameters to register;
  - add business rules for moving personal events to a group (window, skipped days, deleted type) and for private group event notes;
  - reword the "group event docs … hold no private notes" rule to point at `groupEventExtras`.

  Verify the text agrees with the specs.

## 2. Firestore schema, rules and indexes

- [x] 2.1 Add the `groupEventExtras` match to `firebase/firestore.rules`: read and write only when `request.auth.uid == uid`. Verify with the emulator that the owner can read and write, and that a `calendarSharedWith` viewer and another member are denied.
- [x] 2.2 Add the `groupEventExtras (yearMonth ASC, updateAt ASC)` and `personalEvents (typeId ASC, yearMonth ASC)` indexes to `firebase/firestore.indexes.json`. Verify the file parses and the emulator accepts both queries.
- [ ] 2.3 Update `firebase/firestore-schema.md`:
  - add a `users/{uid}/groupEventExtras/{eventId}` section: fields, why clearing keeps the document, access;
  - add the `groupEventExtras` per-month marker to `sync/updates`;
  - fix the `personalEventTypes` table: add `acronym`, `isDeleted` (deleted after a move of all its events; its events keep rendering) and `updateAt`, and remove the duplicated `color` row and the pasted group-colour note;
  - list `groupEventExtras` among what a purge must cover in *Data retention*;
  - update the invariant about notes on group events.

  Verify by reviewing the diff against design.md.

## 3. Group event notes in core

- [ ] 3.1 Add `GroupEventExtrasDocument` and `GroupEventExtrasFirestore`:
  - `get(uid, months)` uses the cache-first, marker-gated delta pattern of `PersonalEventFirestore.get`, reporting `extras(CACHE)` / `extras(SERVER)` through `trackData`;
  - `set(uid, groupId, eventId, yearMonth, notes)` writes the note and `UserSyncFirestore.writeGroupEventExtras(batch, uid, months)` in one batch, then `trackWrite(TAG, "setNote")`.

  Add the `groupEventExtras` field to `UserSyncDocument`. Verify with a unit test over a fake Firestore, or with an emulator test that saving and clearing round-trips and moves the marker.
- [ ] 3.2 Add `GroupRepository.getMyEventNotes(date, monthDelta)` and `saveEventNote(event, notes): Outcome<Unit, Unit>`. The latter trims the note, stores a blank one as `null`, and logs `GroupEventNotesSaved` only on success. Implement them in `GroupRepositoryImpl`, the demo repository and the test fakes. Verify with unit tests: a blank note stored as `null`, a failure logs nothing, a success logs once.
- [ ] 3.3 Add a row for group event notes to `firebase/firestore-usage.md`: 0 reads when unchanged, a read only per changed note, and 2 writes per save. Verify by reading the `FirestoreAudit` lines in a debug run.

## 4. Group event notes in the UI

- [ ] 4.1 Combine `getMyEventNotes` into `MyCalendarViewModel` and into the group-calendar mode of `ExternalCalendarViewModel`, filling `DayEventUi.notes` and `notesEditable = true` for group rows. Leave the shared-calendar mode untouched. Verify with ViewModel tests that group rows carry the note, and that shared-calendar rows carry none and are not editable.
- [ ] 4.2 Route `GROUP` rows in `DayDetailSheetViewModel.saveNotes` to `saveEventNote`, reusing `noteError`. Verify with a ViewModel test that a failure sets `noteError` and a success clears it.
- [ ] 4.3 Add the E2E flow `GroupFlowsTest.addPrivateNoteToAGroupShift`: Alice adds a note to a `urgencias` shift, the document appears at `users/alice/groupEventExtras/{id}`, the group event document has no `notes`, and `group_event_notes_saved` is logged. Verify it passes on the emulator.

## 5. The move in core

- [ ] 5.1 Add `MoveResult(moved, skipped)` and `MoveError` (`DayTaken`, `Failed`) to the domain. Add `PersonalEventRepository.moveCandidates(event)` and `moveToGroup(events, target, deleteType)`. Verify it compiles across the repository, the demo and the fakes.
- [ ] 5.2 Implement `PersonalEventMoveFirestore`:
  - read the candidates from the server (`typeId ==`, `yearMonth >=` the window month; drop deleted events and those before the window);
  - read the held dates from the server (`assigneeId == uid`, `yearMonth in` slices of 30; ignore deleted events);
  - commit chunks of 150 events. Each chunk: a group event at the personal event's id with owner and assignee set to the user, not on swap and with an empty history; the personal event soft-deleted; a notes document when the event has notes; one user sync marker set (`personalEvents` and `groupEventExtras` months); one group sync marker set (`events` months). In the last chunk, when `deleteType` is set, also the personal type soft-deleted plus `personalEventTypesUpdatedAt`.

  Add the multi-month `GroupSyncFirestore.writeEvents` and `UserSyncFirestore.writePersonalEvents` variants. Report everything with `trackData` / `trackWrite` as named in design.md. Verify with an emulator test that a 150-event chunk commits under the security rules.
- [ ] 5.3 Implement `moveToGroup` in `PersonalEventRepositoryImpl`:
  - a single event on a taken day → `DayTaken`, with nothing written;
  - any commit failure → `Failed`, with the type kept;
  - log `PersonalEventsMoved` only after every chunk succeeds, and nothing else per event;
  - no share-prompt count.

  Implement the demo version. Verify with unit tests covering: a single move; "all" with skips; a taken single day; a failure part-way, then a retry that moves the rest with no duplicate and deletes the type; and one analytics event per move.
- [ ] 5.4 Add the move's cost to `firebase/firestore-usage.md`: N + M server reads, and about 2N writes plus notes plus 2 per chunk plus 2 for the type. Verify against the `FirestoreAudit` output of the emulator test in 5.2.

## 6. The move in the UI

- [ ] 6.1 Add strings in `values/`, `values-es/`, `values-fr/`, `values-de/` and `values-it/`:
  - "Move to a group", the group and type picker titles, "Only this one" and "All of them";
  - the plural "%d moved" / "%d already had a shift";
  - "You already have a shift in this group that day", and "Couldn't move the shift".

  Verify every key exists in all five files.
- [ ] 6.2 Add `onMove` to `DayEventRow` and wire it in `DayDetailSheet`, only for personal typed rows on the user's own calendar and only when an eligible group exists (not revoked, with at least one type). Add a `TestTags` tag for the action. Verify with a Compose preview, and with a ViewModel test that one-off, group and shared-calendar rows get no move.
- [ ] 6.3 Create `MoveToGroupViewModel` and `MoveToGroupSheet`:
  - steps `PickGroup → PickType → PickScope → Moving → Done`;
  - the group step is skipped with a single group;
  - the scope step appears only when `moveCandidates` returns more than one event;
  - the outcome goes into UiState: `Done` or a `userMessage`, with `userMessageShown()`.

  Register it in Koin. Verify with ViewModel tests for each branch (single group, scope skipped, `DayTaken`, `Failed`, `Done` counts) and with a Compose preview of each step.
- [ ] 6.4 Extend `firebase/test/fixtures/base.json`:
  - two more `curso` events for Alice, one of them on a day she already holds a `urgencias` shift;
  - one `curso` event dated before the window, with a note.

  Update any E2E assertion that counts Alice's personal events. Verify the existing suite still passes.
- [ ] 6.5 Add E2E flows in a new `MoveToGroupFlowsTest`, with a `MoveToGroupRobot`:
  - move one event: the group event exists at the same id, the personal event is `isDeleted`, and `personal_events_moved` is logged with `scope = one`;
  - move all: the counts on screen, the skipped day still personal, the old event still personal, `curso` `isDeleted` and gone from "My shifts", and a moved note in `groupEventExtras`.

  Verify both pass on the emulator.

## 7. Integration

- [ ] 7.1 Run `./gradlew :core:allTests`, the shared unit tests and `./gradlew :app:androidApp:assembleDebug`, then the full E2E suite with `connectedDebugAndroidTest`. Verify all of them are green.
- [ ] 7.2 Build the iOS app in Xcode and check the move sheet and group notes on a simulator. Verify manually.
- [ ] 7.3 Take before and after screenshots of the day sheet row, the move sheet steps, the result and a group shift with a note, for the UI review. Verify they are saved under the change's `screenshots/`.
