# Design

## Context

See proposal.md for the motivation. The pieces this change builds on:

- **Sign-in routing.** `SplashViewModel` sends an authenticated session straight to
  `RootRoute.MainKey`. `RootScreen` overlays `CompleteNameDialog` while `User.needsName`, and
  `HandleJoinGroupDeeplink` pops everything above Main when an invitation code arrives.
- **Personal event types.**
  - One document per type in `users/{uid}/personalEventTypes`, written by `PersonalEventTypesFirestore.set`
    in a batch with `UserSyncFirestore.writePersonalEventTypes`.
  - `PersonalEventType` already has `isDeleted`, which the add pane filters out.
  - The types flow (`observe`) emits the **cache first**. On a fresh install that first emission is
    empty even for an account that has types.
- **The add pane.** `DayDetailAddEvent` renders the one-off field first, then personal chips, then
  groups. `NoTypesPrompt` exists but is only used in `DayAddMode.GroupOnly`.
- **Device-wide DataStore settings.**
  - `AppConfigRepository` holds the onboarding flag. The E2E suite replaces it with
    `FixedAppConfigRepository`, seeded by `E2eRule(onboardingSeen = …)`.
  - `SharePromptRepositoryImpl` counts events added on the device (`share_prompt_events_added`) and
    is called from `DayDetailSheetViewModel` after each successful add.
- **One-off to type prefill.** `EventTypeDetailScreen` / `EventTypeDetailData.NewPersonal` create a
  personal type. The one-off form already has a name, a color and a start and end time.

## Goals / Non-Goals

**Goals:**
- Decide "show the setup" from the backend's answer, at most two reads, and only for accounts that
  could qualify.
- Keep every existing E2E path untouched by default, and cover the new behavior with flow tests
  against the emulators.

**Non-Goals:**
- Any change to the pre-sign-in `OnboardingScreen` tour.
- Group creation, invitations or the team prompt (`group-invite-nudges`).
- Converting a one-off into a typed event. "Save as shift" only creates the type.

## Decisions

### D1. `ShiftSetupRepository` in core, modelled on `SharePromptRepository`

A new repository owns the decision and the reports:

```
interface ShiftSetupRepository {
    val due: StateFlow<Boolean>          // show the setup after sign-in
    val hintPending: StateFlow<Boolean>  // "tap a day to add a shift", held until shown
    suspend fun shown(via: ShiftSetupVia)
    suspend fun skipped(via: ShiftSetupVia, interacted: Boolean)
    suspend fun complete(types: List<PersonalEventType>, via, interacted, customCount): Outcome<Unit, Unit>
    fun hintShown()
}
```

Why a repository:
- Analytics are logged in the repository, after success, per CLAUDE.md.
- `due` and `hintPending` are state, never a `Channel`, so a cold start or rotation cannot lose them.

`complete` delegates the write to a new `PersonalEventRepository.createEventTypes(types)`. That
method logs `personal_event_type_created` per type once the commit succeeds. Only then does `complete`
log `onboard_shift_completed` and raise `hintPending`.

*Alternative considered:* putting the logic in `MainViewModel`. Rejected, because the decision needs
the invitation repository, the user's groups and types, and DataStore; that belongs below the UI and
must be unit-testable without Compose.

### D2. The decision: cache first, then one bounded server check, remembered once settled

The `due` flow is evaluated once per signed-in session:

1. If the device flag `shift_setup_settled` is set, or `InvitationLinkRepository.pendingCode` is
   non-null, the result is `false`, with no read.
2. If the cached types or cached groups are non-empty, the result is `false` and the flag is set.
3. Otherwise:
   - it runs `personalEventTypes.limit(1)` with `Source.SERVER`;
   - it runs `groups where memberUids array-contains uid limit(1)` with `Source.SERVER`;
   - each is at most one read, tracked as `hasAnyType(SERVER)` / `hasAnyGroup(SERVER)`.

   If either finds a document, the result is `false` and the flag is set. If both are empty, the
   result is `true`. If either throws, for example offline, the result is `false` and the flag is not
   set, so the next launch decides again.

The flag is set by skipping, by completing, by the server showing the account already has
something, and by an invitation code arriving while the setup is owed: whoever comes through an
invitation joins a group that brings its own types, and without it the setup would reappear the
moment the join sheet takes the code. So the reads happen at most once per device for an account that settles.

The flag lives in **`AppConfigRepository`** (`isShiftSetupSettled` / `setShiftSetupSettled`), next to
the onboarding flag in the same DataStore file.
- This makes `FixedAppConfigRepository` the E2E seam, exactly like `onboardingSeen`.
- `E2eRule` gains `shiftSetupSettled: Boolean = true`, so all current tests keep landing on the
  calendar.

*Alternatives considered:*
- The sync marker `personalEventTypesUpdatedAt == null`: accounts whose types predate the markers
  would read as empty.
- A field in `users/{uid}/private/preferences`: the user ruled out writing this to Firebase.

### D3. Routing: a root route pushed above Main, after the name

- New `RootRoute.ShiftSetupKey(via: ShiftSetupVia)`.
- `MainScreen` collects `due` and pushes `ShiftSetupKey(Onboarding)` when all of these hold:
  - `due` is true;
  - the user no longer `needsName`;
  - the key is not already on the stack.
- The add pane's empty state pushes `ShiftSetupKey(AddPane)`.
- Closing, whether by skip, confirm or back, pops the route.
  - Back from `Onboarding` counts as a skip.
  - Back from `AddPane` is just a close, with no skip report and no flag.
- `HandleJoinGroupDeeplink` already pops anything above Main, so an invitation arriving mid-setup wins.

Opening the setup from the add pane closes the day sheet first, as every other way out of the sheet
does: a modal sheet left open under another screen comes back hidden while still holding its day.
Confirming there raises the same "tap a day" hint as after sign-in, and T6 reopens the day.

### D4. Presets live in the UI layer; core receives plain types

- `ShiftPreset` is an enum in `app/shared`, holding:
  - name and acronym `StringResource`s;
  - default times;
  - `selectedByDefault`;
  - a fixed color.
- The setup ViewModel keeps `StringResource`s in its UiState, never resolved text.
- On confirm, the screen resolves the names in the current language and passes them to the
  ViewModel, which builds `PersonalEventType`s. That is when the language is frozen into the data.
- Core never sees a Compose resource.

Preset colors come from the same palette the personal type color picker offers (`EventTypeDetailViewModel`'s
`colors`). They are five distinct, fixed entries. A custom shift takes the first palette color not
used by any shift in the list.

Names and acronyms per language:

| | en | es | fr | de | it |
|-|----|----|----|----|----|
| Morning | Morning · M | Mañana · M | Matin · M | Früh · F | Mattina · M |
| Afternoon | Afternoon · A | Tarde · T | Après-midi · A | Spät · S | Pomeriggio · P |
| Night | Night · N | Noche · N | Nuit · N | Nacht · N | Notte · N |
| Morning & afternoon | Morning & afternoon · MA | Mañana y tarde · MT | Matin et après-midi · MA | Früh und Spät · FS | Mattina e pomeriggio · MP |
| 24h duty | 24h duty · 24H | Guardia 24h · 24H | Garde 24h · 24H | 24-h-Dienst · 24H | Turno 24h · 24H |

An end time at or before the start time already renders as overnight wherever a type's times are
shown. T2 checks that for the Night and 24h presets, and changes the rendering only if it does not.

### D5. One atomic write

`PersonalEventTypesFirestore.setAll(uid, types)` does all of this in one batch:
- `batch.set`s every type document;
- adds one `writePersonalEventTypes` marker;
- calls `trackWrite(TAG, "setEventTypes")` once per document after the commit;
- calls the marker's `committed()`.

That is N + 1 writes, three presets being 4. `firestore-usage.md` records it.

### D6. `interacted` is ViewModel state

The setup UiState has `interacted: Boolean`. It is set to true by any of:
- a preset toggle;
- a time edit;
- a custom add, or opening the custom form.

It is never reset, and is read when skipping or confirming.

`onboard_shift_shown` is logged from the ViewModel's `init` through `repository.shown(via)`.
- A ViewModel survives rotation, so one display logs once.
- A new display is a new ViewModel.

### D7. `first_event_added` rides on the share prompt's counter

`SharePromptRepositoryImpl.eventAdded(kind)` already increments the device count after a successful
add. When the new count is exactly 1, it logs `FirstEventAdded(kind)`: `EventKind.Typed` becomes
`typed` and `OneOff` becomes `one_off`. Devices whose count is already above 0 never log it, which is
what the spec asks.

*Alternative considered:* a separate counter. Rejected, because two counters of the same thing
drift apart.

### D8. Add pane layout

`DayDetailAddEvent` in Full mode renders, in order:
1. The "MY SHIFTS" section. It holds the type chips plus the Add chip, or, with no types, the
   `NoShiftsPrompt` empty state with a "Create my shifts" button.
2. The group sections, unchanged.
3. An "Other event…" text row. Tapping it expands `OneOffEventForm` exactly as the one-off field does
   today.

`TestTags` gains:
- `addPaneShifts`;
- `addPaneOtherEvent`;
- `addPaneEmptyShifts`;
- tags for each setup row: `shiftSetupRow(preset)`, `shiftSetupCustomRow(index)`, and the toggle and
  time fields.

These let the robots assert order by bounds and reach rows whose text repeats.

### D9. "Save as shift"

- `EventTypeDetailData` gains `NewPersonalFrom(name, color, startTime?, endTime?)`, which is
  serializable, as nav keys must be.
- `EventTypeDetailViewModel` seeds its personal form from it.
- The one-off form, in edit mode of an existing one-off, shows a "Save as shift" action that pushes
  it.
- Times are passed only when the one-off is not all day, taken from its start and end time of day.

### D10. Flow tests

A new class, `ShiftSetupFlowsTest`, with a `ShiftSetupRobot`.

Fixtures:
- A new user `nuevo` in `firebase/test/fixtures/base.json`, with a profile and an account, no
  personal types and no groups.
- The group-member path uses `bruno`, who is in groups and has no personal types.

| # | Path | Signed in / rule | Asserts |
|---|------|------------------|---------|
| T1 | New account lands on setup; confirm defaults | signed out, `shiftSetupSettled = false`, create account | 3 docs in `personalEventTypes` with localized names, acronyms and times; `onboard_shift_shown(via=onboarding)`; `onboard_shift_completed(interacted=false, type_count=3, custom_type_count=0)`; 3× `personal_event_type_created`; the calendar hint is shown |
| T2 | Edit a time, deselect, add a custom shift | `nuevo`, settled = false | Morning 07:00–14:00; Night absent; custom doc with no times; `interacted=true`, `custom_type_count=1`; Night and 24h render as overnight |
| T3 | Skip is remembered | `nuevo`, settled = false | No type docs; `onboard_shift_skipped(interacted=false)`; after `world.relaunch()` the calendar shows without the setup |
| T4 | Account with types never sees it | `alice`, settled = false | Calendar shown; no `onboard_shift_shown` |
| T5 | Group member never sees it | `bruno`, settled = false | Same as T4 |
| T6 | Empty add pane opens setup | `nuevo`, settled = true | The empty state is shown; "Create my shifts" opens setup; `shown(via=add_pane)`; confirm returns to the calendar with the hint; reopening the day lists M/A/N; tapping M adds the event; `first_event_added(kind=typed)` |
| T7 | Shifts first, one-off last | `alice` | The shift chips' top is above "Other event…"; "Other event…" opens the one-off form |
| T8 | Save a one-off as a shift | `alice` | Add a timed one-off, edit it, "Save as shift", acronym "G", save; the type doc has the one-off's name, color and times; the one-off doc is unchanged |
| T9 | First event is a one-off | `nuevo`, settled = true | `first_event_added(kind=one_off)` once; a second add logs nothing |
| T10 | Invitation link bypasses setup | signed out, launched with a `/join/` intent, create account | The join sheet is shown; no `onboard_shift_shown` |

`E2eRule` gains `shiftSetupSettled` and `relaunch()`, which closes the scenario and launches it again.

Existing tests that match the renamed strings or the moved one-off field change with them:
- `CalendarFlowsTest.createPersonalType_…`: "My shifts" and "New shift";
- `CalendarRobot.addOneOff` and `editOneOff`: through "Other event…".

The offline path, where the server check fails and nothing is shown, cannot be driven reliably on
the emulator. It is covered by a unit test of `ShiftSetupRepositoryImpl` with a failing fake
datasource, in `core/src/commonTest`.

## Risks / Trade-offs

- **Two server reads on a fresh install for accounts that had nothing cached.** These are bounded to
  `limit(1)` each and stop once the flag settles. → Documented in `firestore-usage.md`.
- **A skip is per device.** A user who skipped on their phone sees the setup on their tablet. →
  Accepted by the user; it only shows while the account has nothing.
- **Renamed strings break robots that match English text.** → The same change updates the robots, and
  T7 guards the new order.
- **Acronyms in fr/de/it are a first pass.** → They are plain strings and can be changed without
  touching specs or code.
- **`first_event_added` is per device.** An existing user on a new phone logs it once. → Acceptable
  for an activation ratio; noted in the analytics docs.

## Migration Plan

No data migration.
- Existing installs have no `shift_setup_settled` flag.
- Existing users with types or groups settle at step 2, from cache, with no read.
- Before release, register `interacted`, `custom_type_count` and `kind` in the GA console. `via` and
  `type_count` are already registered.
- Rollback is shipping a build without the route. Flags left in DataStore are inert.
