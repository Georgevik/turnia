# Tasks

## 1. Analytics vocabulary and plumbing

- [x] 1.1 Add every new event from the spec to `AnalyticsEvent`: the parameters of `OneOffEventCreated`,
  `GroupCreated`, `InvitationOpened`, `NotificationOpened` and `Login`, with booleans and counts as
  GA-compatible values. Keep the existing names untouched. Verify with `./gradlew :core:compileKotlinMetadata`.
- [x] 1.2 Add the sealed `AnalyticsUserProperty` (`GroupCount`, `IsAdmin`, `AppLanguage`) and
  `Analytics.setUserProperty`. Implement it in `AnalyticsImpl` over `FirebaseAnalytics.setUserProperty`,
  in `DemoAnalytics` as a no-op, and in `RecordingAnalytics` by keeping the last value per name. Verify
  that `./gradlew :app:androidApp:assembleDebug :app:androidApp:assembleDebugAndroidTest` compiles.

## 2. Personal events and event types

- [x] 2.1 `PersonalEventRepositoryImpl`:
  - log `personal_event_created` / `personal_event_deleted` only when the datasource's `Outcome` succeeds;
  - log `event_notes_saved`;
  - log `one_off_event_created(all_day, multi_month)`, `one_off_event_updated` and
    `one_off_event_deleted` on success.

  Verify in `CalendarFlowsTest.createPersonalType_thenAddAnEventWithANote` and `deleteOwnEvent` that
  `RecordingAnalytics.named(...)` holds one of each, and in `SharePromptFlowsTest.editingAnEvent_doesNotCount`
  that an edit logs `one_off_event_updated` and no `one_off_event_created`.
- [x] 2.2 Add `isNew` to `PersonalEventRepository.saveEventType`, passed from `EventTypeDetailViewModel`
  (`NewPersonal` vs `EditPersonal`) and implemented in the demo repository. Log
  `personal_event_type_created` only when it is new. Verify that `CalendarFlowsTest.createPersonalType_thenAddAnEventWithANote`
  records exactly one.

## 3. Group events, types and the swap cycle

- [x] 3.1 `GroupRepositoryImpl`:
  - `addEvent` logs `group_event_created` only when the write succeeds;
  - `deleteEvent` logs `group_event_deleted` on success;
  - `saveEventType` logs `group_event_type_created` when the type is new;
  - `createGroup` logs one `group_event_type_created` per type, then `group_created(auto_approve, type_count)`.

  Verify that `GroupFlowsTest.createGroup_withItsFirstEventType` records `group_created` with
  `type_count = 1` and one `group_event_type_created`, and that `CalendarFlowsTest.addGroupEvent_fromTheDaySheet`
  records `group_event_created`.
- [x] 3.2 Log `swap_offered` / `swap_withdrawn` from `setOnSwap`, and `swap_taken` / `swap_returned`
  from `takeEvent` / `returnEvent` on success. Log `swap_take_lost` for `SwapError.TakenBySomeoneElse`
  only. Verify by asserting the events in the existing `SwapFlowsTest` cases that offer, take and
  give back a shift.

## 4. Group growth and churn

- [x] 4.1 Log `join_request_rejected`, `group_left`, `member_removed` and `group_deleted` on their
  callables' success in `GroupRepositoryImpl`. Verify that `./gradlew :core:compileKotlinMetadata`
  passes, and add a `GroupFlowsTest` assertion wherever an existing case already rejects, leaves,
  removes or deletes.
- [x] 4.2 In `InvitationLinkRepositoryImpl`, inject `Analytics` and log `invitation_opened` with
  `via = link` from `opened` and `via = install_referrer` from `referred`, only for a valid code.
  Update its Koin definition. Verify that `GroupFlowsTest.invitationLink_prefillsTheJoinSheet`
  records it with `via = link`.
- [x] 4.3 Give `ShareInvitationAction` an `onShared` callback into `GroupDetailViewModel`, which logs
  `group_invite_shared` after `sharer.share`. Verify with a `GroupFlowsTest` case that taps the share
  button and finds both a `RecordingTextSharer` entry and the event.

## 5. Shared calendars, notifications, account

- [x] 5.1 `UserRepositoryImpl`:
  - log `calendar_shared`, `calendar_share_revoked` and `shared_calendar_hidden` on success;
  - log `account_deleted` in `deleteAccount`'s `onSuccess`, before `auth.signOut()`.

  In `ExternalCalendarViewModel.init`, log `shared_calendar_viewed` for `ExternalCalendarData.Personal`
  only. Verify that `SharedCalendarFlowsTest.shareMyCalendar_byUsername`, `viewAColleaguesSharedCalendar`
  and `hideASharedCalendar_bySwipe` each record their event.
- [x] 5.2 In `NotificationRepositoryImpl`, inject `Analytics` and log `notification_opened(type)`
  for recognised types only. Update its Koin definition. Verify that `./gradlew :core:compileKotlinMetadata`
  passes and that an unrecognised `type` returns before logging.

## 6. Sign-in and user properties

- [x] 6.1 Move the provider mapping out of `UserProvisioner` into a shared `signInMethod(FirebaseUser)`.
  In `UserRepositoryImpl.init`, track whether a signed-out emission has been seen, and have
  `gatherUserInfo` log `login(method)` when the profile is found after one. Verify that
  `AuthFlowsTest.signInWithEmail_landsOnCalendar` records `login` with `method = email`, and that
  `createAccountWithEmail_provisionsTheProfile` records `sign_up` and no `login`.
- [x] 6.2 Report `group_count` and `is_admin` from an `onEach` in `GroupRepositoryImpl.getGroups()`,
  over non-revoked groups and deduplicated against the last value reported. Report `app_language`
  from `RootViewModel.init` and `PreferencesViewModel.onLanguageSelected` (`tag ?: "system"`).
  Verify that `GroupFlowsTest.createGroup_withItsFirstEventType` ends with `group_count = 1` and
  `is_admin = true` in `RecordingAnalytics`.

## 7. Documentation and console

- [x] 7.1 Add an *Analytics* section to `CLAUDE.md` covering where events are logged (repository
  after success, app layer only for what it alone sees), the no-PII rule, and that names live in
  `AnalyticsEvent` / `AnalyticsUserProperty`. Verify by reading it against design decisions 1, 2 and 6.
- [x] 7.2 Add to `.github/RELEASE.md`, or next to the Analytics section, the list of custom
  dimensions to register in the GA console:
  - event-scoped: `all_day`, `multi_month`, `auto_approve`, `type_count`, `via`, `type`, `method`;
  - user-scoped: `group_count`, `is_admin`, `app_language`.

  Verify that the list matches the parameters in `AnalyticsEvent`.

## 8. Integration

- [x] 8.1 Run the whole E2E suite (`./gradlew :app:androidApp:connectedDebugAndroidTest`) and confirm it
  passes with the new assertions. Then run `openspec validate add-analytics-events --strict`.
