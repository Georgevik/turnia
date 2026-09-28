# Tasks

## 1. Core: writing types in bulk and the first-event signal

- [x] 1.1 Add `PersonalEventTypesFirestore.setAll(uid, types)`: one batch with every type document, one `writePersonalEventTypes` marker, `trackWrite(TAG, "setEventTypes")` per document after the commit. Verify with a unit test in `core/src/commonTest` against the existing Firestore fakes, or by the T1 document assertions if no fake covers batches.
- [x] 1.2 Add `PersonalEventRepository.createEventTypes(types): Outcome<Unit, Unit>`, which logs `personal_event_type_created` once per type only after the commit succeeds. Verify with a unit test: success logs N events, failure logs none.
- [x] 1.3 Add `AnalyticsEvent.OnboardShiftShown`, `OnboardShiftSkipped`, `OnboardShiftCompleted` and `FirstEventAdded`, with the parameters `via`, `interacted`, `type_count`, `custom_type_count` and `kind`, as the `analytics-events` delta specifies. Verify that the build compiles and a unit test checks the parameter maps.
- [x] 1.4 Log `FirstEventAdded(kind)` in `SharePromptRepositoryImpl.eventAdded` when the new count is exactly 1. Verify with a unit test: count 0→1 logs `typed` or `one_off`; 1→2 logs nothing.
- [x] 1.5 Update `firebase/firestore-usage.md` with the setup's N + 1 writes and the two bounded `limit(1)` server reads. Verify that the doc lists both under the shift setup.

## 2. Core: `ShiftSetupRepository`

- [x] 2.1 Add `isShiftSetupSettled` / `setShiftSetupSettled` to `AppConfigRepository` and `AppConfigRepositoryImpl`, over the existing DataStore file, and to the test fake `FixedAppConfigRepository`. Verify with a unit test that the flag round-trips.
- [x] 2.2 Add the `hasAnyType(SERVER)` and `hasAnyGroup(SERVER)` `limit(1)` server reads in the owning datasources, tracked with `.trackData(TAG, …)`. Verify with the FirestoreAudit log line in a debug run of T1.
- [x] 2.3 Implement `ShiftSetupRepositoryImpl` (`due`, `hintPending`, `shown`, `skipped`, `complete`, `hintShown`) following design D1/D2, and register it in Koin. Verify with unit tests covering these cases:
  - settled flag set: false, no reads;
  - pending invitation: false;
  - cached types: false, and the flag is set;
  - server empty on both: true;
  - server finds a type or a group: false, and the flag is set;
  - server throws: false, and the flag is not set;
  - `complete` failure: no `onboard_shift_completed`, no hint.

## 3. UI: the setup screen

- [x] 3.1 Add the `ShiftPreset` enum (names and acronyms as `StringResource`, default times, `selectedByDefault`, fixed palette color) and every string, including the preset names and acronyms from design D4, in `values`, `values-es`, `values-fr`, `values-de` and `values-it`. Verify that the build compiles and the five `strings.xml` files contain every new key.
- [x] 3.2 Build `ShiftSetupViewModel` and its UiState: rows, selection, editable times, the inline custom form with required name and acronym, `interacted`, `userMessage` on a failed write, and confirm disabled with nothing selected. Log `shown` from `init`. Verify with unit tests for toggle, time edit, custom add validation and the `interacted` transitions.
- [x] 3.3 Build `ShiftSetupScreen` with Skip, the preset rows, "Add another shift" and Confirm, adding the `TestTags` from design D8. Resolve names in the current language on confirm. Verify with a Compose preview, and later with T1 and T2.
- [ ] 3.4 Add `RootRoute.ShiftSetupKey(via)` and its serializer. Push it from `MainScreen` when `due` is true and `needsName` is false; pop it on skip, confirm or back, with back from `Onboarding` counting as a skip. Show the calendar hint from `hintPending` in `MyCalendarScreen` and call `hintShown()`. Verify with T1, T3 and T4.

## 4. UI: add pane, wording and "Save as shift"

- [ ] 4.1 Reorder `DayDetailAddEvent` in Full mode: "MY SHIFTS" first, groups next, and an "Other event…" row last that expands `OneOffEventForm`. Add `NoShiftsPrompt`, which pushes `ShiftSetupKey(AddPane)`, and make `CalendarViewer.sheetDate` survive the push. Verify with T6 and T7.
- [ ] 4.2 Rename the user-facing strings for personal event types to shifts in all five languages: the add pane title, settings entry, screen title, create action, empty state and delete dialog. Verify with a grep showing no remaining "My events" or "personal events" text in `values*/strings.xml` for these keys.
- [ ] 4.3 Add `EventTypeDetailData.NewPersonalFrom(name, color, startTime?, endTime?)`, seed `EventTypeDetailViewModel` from it, and add the "Save as shift" action to the edit mode of an existing one-off, with times only when the one-off is not all day. Verify with T8.
- [ ] 4.4 Confirm that Night (22:00–08:00) and 24h (08:00–08:00) render as overnight wherever a type's times are shown, and fix the rendering if not. Verify with T2's assertion.

## 5. E2E flow tests

- [ ] 5.1 Add user `nuevo` to `firebase/test/fixtures/base.json`, with a profile, an account and a username, and no types or groups. Add `shiftSetupSettled` (default `true`) and `relaunch()` to `E2eRule`. Verify that the existing suite still passes unchanged apart from the renamed strings.
- [ ] 5.2 Update the robots and tests affected by the rename and the moved one-off field: `CalendarRobot.addOneOff` / `editOneOff` go through "Other event…", and `CalendarFlowsTest` uses "My shifts" / "New shift". Verify that `CalendarFlowsTest` and `SharePromptFlowsTest` pass.
- [ ] 5.3 Add `ShiftSetupRobot` and `ShiftSetupFlowsTest` with paths T1–T5 and T10 from design D10: setup show, skip and bypass. Verify that they pass on the emulator via `connectedDebugAndroidTest`.
- [ ] 5.4 Add paths T6–T9 to `ShiftSetupFlowsTest`: add pane empty state, order, "Save as shift" and `first_event_added`. Verify that they pass on the emulator.
- [ ] 5.5 Update the E2E path count and list in `CLAUDE.md` and `firebase/test/README.md`. Verify that the documented number matches the `@Test` count.

## 6. Documentation and release checks

- [ ] 6.1 Document the shift setup rule in `CLAUDE.md` *Business rules*, covering when it shows, device-only skip and personal types only, and the new analytics names and parameters in *Analytics*, including the GA registration list. Verify by review.
- [ ] 6.2 Run the full E2E suite and the core unit tests together. Verify with a green `connectedDebugAndroidTest` and `:core:allTests`.
