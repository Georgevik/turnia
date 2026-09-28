# Tasks

## 1. Remote Config and analytics vocabulary

- [x] 1.1 Add `RemoteKey.TEAM_PROMPT_ENABLED` (`teamPromptEnabled`, default `false`) and `TEAM_PROMPT_THRESHOLD` (`teamPromptThreshold`, `Long`, default `5`) to `RemoteConfigService` (defaults and `getFlags`). Add `teamPromptEnabled`, `teamPromptThreshold` and `teamPromptActive` (`enabled && threshold >= 1`) to `FeatureFlags`. Verify with a unit test on `FeatureFlags.teamPromptActive` covering off, on and threshold 0.
- [x] 1.2 Add `TeamPromptChoice` (`Create`/`Join`/`Dismissed` → `create`/`join`/`dismissed`), `AnalyticsEvent.OnboardTeamShown` (`onboard_team_shown`) and `AnalyticsEvent.OnboardTeamAnswered(choice)` (`onboard_team_answered`, param `choice`). Verify it compiles and an `AnalyticsEvent` test asserts the names and the param value.
- [x] 1.3 Update CLAUDE.md: add `choice` to the event-scoped parameters to register, document `onboard_team_*` under *Analytics*, and add a *Growth: the team prompt* note (Remote Config keys, once per device, precedence over the share prompt). Verify the text reads consistently with the specs.

## 2. Team prompt state in core

- [x] 2.1 Change `SharePromptRepository.eventAdded(kind)` to return the new count, or 0 when the write failed. Update `SharePromptRepositoryImpl`, the demo repository and the fakes. Verify that `SharePromptRepositoryImplTest` still passes, with an added assertion on the returned count.
- [x] 2.2 Create `TeamPromptRepository` (domain) and `TeamPromptRepositoryImpl` over the shared DataStore. It holds `pending: StateFlow<Boolean>`, `eventsAdded(count)`, `shown()` and `answered(choice): Boolean`, and a `team_prompt_settled` key. `eventsAdded` does nothing when the flag is inactive, the count is below the threshold or the prompt is settled. Otherwise it calls `GroupFirestore.hasAnyGroup(uid, Source.SERVER)`: a group → settle; none → `pending = true`; a failure → nothing. Register it in `DataModule`, and add a demo implementation that is never pending.
- [x] 2.3 Log `OnboardTeamShown` in `shown()` only when it flips `team_prompt_settled`. Log `OnboardTeamAnswered` in `answered()` only when it compare-and-clears `pending`.
- [x] 2.4 Unit-test `TeamPromptRepositoryImpl` with a fake DataStore and a fake group source. Cover:
  - below the threshold;
  - at the threshold with no group → pending;
  - with a group → settled, and no read on the next event;
  - server failure → not pending, retried on the next event;
  - flag off;
  - `shown` twice logs once;
  - a double `answered` logs once.

  Verify with `./gradlew :core:allTests` (or the project's core test task) passing.
- [x] 2.5 Confirm the `hasAnyGroup` server read is reported through `trackData` under an operation name that can be told apart. Add the team prompt's cost (at most one `limit(1)` read per event past the threshold, until shown or settled) to `firebase/firestore-usage.md`. Verify by reading the audit line in a debug run, or by reviewing the diff.
- [x] 2.6 In `DayDetailSheetViewModel`, pass the count returned by `sharePromptRepository.eventAdded(kind)` to `teamPromptRepository.eventsAdded(count)`, through a small `EventAddedPrompts` collaborator so it can be tested without the ViewModel's four repositories. Verify with `EventAddedPromptsTest` that adding an event calls both in order.

## 3. "I have a code" hand-off

- [x] 3.1 Add `joinSheetRequested: StateFlow<Boolean>`, `requestJoinSheet()` and `joinSheetOpened()` to `InvitationLinkRepository` and its implementation, and to the test/demo fakes. Verify with a unit test of the flag's set and clear.
- [x] 3.2 Make `MainViewModel`/`MainScreen` bring up the Groups tab when `joinSheetRequested` is true. Make `GroupsViewModel`/`GroupsScreen` open the join sheet with an empty field once loaded, then call `joinSheetOpened()`. Verify manually: calling `requestJoinSheet()` from the Calendar tab lands on Groups with the sheet open, and rotating does not reopen it after it is closed.

## 4. Team prompt UI and precedence

- [x] 4.1 Add strings in `values/`, `values-es/`, `values-fr/`, `values-de/` and `values-it/`: the team prompt's title, body, "Create a group", "I have a code" and "Not now". Verify every key exists in all five files.
- [x] 4.2 Create `TeamPromptViewModel` and `TeamPromptSheet`, modelled on `SharePromptSheet`:
  - `LaunchedEffect` → `shown()`;
  - dismissing the sheet → `answered(Dismissed)`;
  - the buttons → `answered(Create/Join)`, followed by `navigator.goTo(MainRoute.GroupDetail(null))` or `invitationLinkRepository.requestJoinSheet()` only when `answered` returned true.

  Add a Compose preview.
- [x] 4.3 Replace `SharePromptHost` in `CalendarViewer` with a `CalendarPromptHost(allowTeamPrompt)` that follows design Decision 3:
  - the team prompt wins when it is pending and allowed;
  - a `remember` "team prompt shown this visit" flag, hoisted into `CalendarViewer`, suppresses the share prompt until the calendar leaves composition.

  `MyCalendarScreen` passes `allowTeamPrompt = true`, and group calendars pass `false`. Verify manually in the demo or debug build with both flags forced due: only the team sheet shows; after answering, no share sheet shows until a tab switch and return.
- [x] 4.4 Add E2E `TeamPromptFlowsTest` with a `TeamPromptRobot`, with the Remote Config flag forced on and a low threshold through the test modules. Cover:
  - reaching the threshold shows the prompt and logs `onboard_team_shown`;
  - "I have a code" opens the join sheet and logs `onboard_team_answered` with `choice = join`;
  - "Create a group" opens the new-group form;
  - with a milestone due on the same event, only the team prompt shows.

  Verify that the suite passes on the emulator.

## 5. Proposed types in the new-group form

- [x] 5.1 Add `ShiftPreset.groupDefaults` (Morning, Afternoon, Night, MorningAfternoon). Verify with a unit test of its order and content.
- [x] 5.2 Add `GroupRepository.removePendingEventType(id)` to the implementation and the demo repository. Verify with a unit test.
- [x] 5.3 Add `GroupDetailViewModel.proposeTypes(texts: Map<ShiftPreset, Pair<String, String>>)`. It seeds each preset once as a pending `GroupEventType`: preset times, preset color as `defaultColor`, `swappable = true`, a new id. A ViewModel flag keeps it from seeding twice, and it does nothing for an existing group. Add `onRemoveType(id)`. Verify with `GroupDetailViewModel` tests:
  - seeding yields four rows;
  - calling it twice still yields four;
  - removing one leaves three;
  - removing all makes `onSave` a no-op;
  - saving passes exactly the listed types to `createGroup`.
- [ ] 5.4 In `GroupDetailScreen`, for a new group, resolve the preset texts and call `proposeTypes` from a `LaunchedEffect`. Add a remove affordance on type rows while `isNew`. Verify manually in Spanish that the form shows Mañana (M), Tarde (T), Noche (N) and Mañana y tarde (MT), that a row can be edited through `EventTypeDetail` and removed, and that Save is disabled with none left.

## 6. Invite step after creating

- [ ] 6.1 Extract the invitation share text and action from `ShareInvitationAction` into one shared helper that the top bar, the invite step and the card all use. Verify that the top-bar share still logs `group_invite_shared` (existing E2E in `GroupFlowsTest`).
- [ ] 6.2 On a successful create, set `created = true` on `GroupDetailUi.Success`, not `isSaved`, with the form's `groupId` set to the saved group so that `canPassOnCode` holds. Add `onCreatedDone()`, which sets `isSaved`. Verify with ViewModel tests:
  - a create success sets `created` and not `isSaved`;
  - a create failure sets neither;
  - an update of an existing group sets `isSaved` directly.
- [ ] 6.3 Add `GroupCreatedContent`: title "Your group is ready", body inviting colleagues, a primary share button and a "Done" button. System back does the same as Done. Add strings in all five languages and a preview. Verify manually that sharing opens the share sheet with the link, and that Done returns to the Groups tab.

## 7. Single-member card

- [ ] 7.1 Add `isAlone` (`!isNew && members.size == 1`) to `GroupDetailUi.Success`, recomputed in `show()`. Verify with ViewModel tests: one member → true; an emission with two members → false; a new group → false.
- [ ] 7.2 Add the "Only you here" card to `GroupDetailContent` with the shared invite helper, plus strings in all five languages and a preview. Verify manually that a one-member group shows it and that accepting a join request removes it without leaving the screen.

## 8. E2E and integration

- [ ] 8.1 Update `GroupsRobot.createGroup` and `GroupFlowsTest` for the four proposed types and the invite step. Add assertions that a group created by name alone has the four types, and that sharing from the invite step logs `group_invite_shared`. Update the fixtures only if a seeded flow depends on the old form. Verify that the whole E2E suite passes on the emulator.
- [ ] 8.2 Run `openspec validate group-invite-nudges --strict` and the full unit test suites (`core` and `app/shared`), and build the Android debug app. Verify all three are green.
