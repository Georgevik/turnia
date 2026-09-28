# Design

## Context

See proposal.md for the motivation. The pieces this change builds on already exist:

- `SharePromptRepositoryImpl` counts events added in DataStore (`share_prompt_events_added`), and
  `DayDetailSheetViewModel` is the one caller of `eventAdded(kind)`. Its `pending` is held as state
  and only spent (`shown`) once the sheet is on screen. `SharePromptHost` is hosted by
  `CalendarViewer` when `showSharePrompt` is true, and only while no day sheet is open. It is used on
  the user's own calendar and on group calendars.
- `RemoteConfigService` maps keys into `FeatureFlags`, with defaults set blocking in `init`.
- `GroupFirestore.hasAnyGroup(userId, source)` is the `limit(1)` membership check that the shift
  setup already uses to decide from the server rather than an empty cache.
- `ShiftPreset` (app layer) holds the five presets as `StringResource`s, times and colors.
  `ShiftSetupScreen` resolves their text in the composable and hands it to the ViewModel.
- The new-group form is `GroupDetailViewModel` with a null `groupId`. Its types wait in
  `GroupRepository.pendingEventTypes`, and are edited through `EventTypeDetail` by id. There is no
  way to remove a pending type yet. After a successful save, `isSaved` makes the screen `goBack()`.
- `InvitationLinkRepository.pendingCode` is how a code reaches the join sheet. `MainScreen` switches
  to the Groups tab when it is non-null, and `GroupsScreen` opens the sheet and calls `codeHandled()`.

## Goals / Non-Goals

**Goals:**
- One place decides which of the two calendar prompts is shown, so they can never stack.
- The membership check costs a bounded number of `limit(1)` reads per device, then none.
- The proposed types go through the same pending-type path as types the user adds by hand, so
  editing, saving and analytics need no special case.

**Non-Goals:**
- The team prompt on group calendars or a colleague's calendar.
- Any Firestore field, rule or Cloud Function change.
- Changing the shift setup's own presets or behaviour.

## Decisions

### 1. `TeamPromptRepository` in core, fed by the share prompt's count

A new `TeamPromptRepository` (`core/domain/repository`), with `TeamPromptRepositoryImpl` over the
same DataStore file, exposes:

- `pending: StateFlow<Boolean>`;
- `suspend fun eventsAdded(count: Int)`;
- `suspend fun shown()`;
- `fun answered(choice: TeamPromptChoice): Boolean`.

Its own DataStore key is `team_prompt_settled` (boolean). It is set when the prompt reaches the screen,
or when the server says the user already belongs to a group.

`SharePromptRepository.eventAdded` returns the new count, and `DayDetailSheetViewModel` passes it on
to `teamPromptRepository.eventsAdded(count)`. The team prompt does not keep a count of its own: the
spec requires the share prompt's count, and two counters would drift the first time one write failed.

*Alternative:* have `TeamPromptRepositoryImpl` read `share_prompt_events_added` from DataStore
directly. That couples it to another repository's private key, so it was rejected.

### 2. Membership from the server, settled when a group is found

When the count reaches the threshold and the prompt is not settled, `eventsAdded` calls
`hasAnyGroup(uid, Source.SERVER)`:

- If the user has a group, it settles for good. The spec says a group member never sees the prompt,
  so the check never runs again.
- If the user has none, `pending` becomes `true`.
- If the call fails, nothing changes, and the next event added tries again.

So a device spends at most one read per event added past the threshold, and only until the prompt
is shown or settled. Revoked groups live under `revokedUids`, which `hasAnyGroup` (a `memberUids`
query) does not match. That already gives the spec's "revoked does not count".

*Alternative:* reuse the `getGroups()` listener that the Calendar tab already collects, at no read
cost. It was rejected because its first emission can be an empty cache on a fresh install, which is
exactly the false positive the spec forbids.

The read must be reported with `.trackData(TAG, "hasAnyGroup(SERVER)")` like every other read, and
`firestore-usage.md` gains the line.

### 3. A single `CalendarPromptHost` decides precedence

`CalendarViewer` stops hosting `SharePromptHost` directly. It hosts `CalendarPromptHost(allowTeamPrompt)`,
where `allowTeamPrompt` is `true` only from `MyCalendarScreen`. The host:

- shows the team prompt when team `pending` is true and team prompts are allowed there;
- otherwise shows the share prompt, unless a team prompt was already shown during this visit.

The "this visit" flag is a `remember { mutableStateOf(false) }` in the host. It is set once the team
sheet is on screen. It survives recomposition and dies when the calendar leaves composition, such as
a tab switch or a navigation away, which gives "the next time a calendar opens" with no stored state.
A rotation keeps it only if it is `rememberSaveable`, so it is `rememberSaveable`. Otherwise
rotating right after answering would pop the share prompt at once.

The share prompt's milestone is not spent while it is suppressed, because `shown()` is only called by
its sheet. Its existing semantics carry the waiting for free.

*Alternative:* have the repositories arbitrate, so the team prompt clears or blocks the share prompt's
`pending`. That is rejected because precedence is about what is on this screen right now. On a group
calendar the share prompt must still show while a team prompt waits for the user's own calendar.

### 4. "I have a code" rides on `InvitationLinkRepository`

`InvitationLinkRepository` gains `joinSheetRequested: StateFlow<Boolean>`, with `requestJoinSheet()`
and `joinSheetOpened()`. It is state, not an event, like `pendingCode`:
- `MainScreen` brings up the Groups tab when either the flag or `pendingCode` is set;
- `GroupsScreen` opens the sheet with its field empty, once its groups have loaded, and then calls
  `joinSheetOpened()`.

`pendingCode` keeps its meaning, "an invitation is waiting", which the splash and the shift setup
rely on, so none of their callers change.

*Alternatives:*
- Set `pendingCode = ""`. Rejected: an empty string meaning "open the sheet" is a hidden convention
  that the next reader would break, and the shift setup would read it as an invitation.
- Replace `pendingCode` with `PendingJoin(code: String?)`. Rejected: it touches every link, referrer
  and setup caller for a flag that only two screens read.

"Create a group" is a plain `navigator.goTo(MainRoute.GroupDetail(null))` from the Calendar tab. It
needs no hand-off, because the navigation happens on the screen that holds the prompt.

### 5. Proposed types are seeded as pending types by the screen

The type names are `StringResource`s and the ViewModel may not resolve text. So `GroupDetailScreen`,
for a new group, resolves the four presets' names and acronyms and calls
`viewModel.proposeTypes(texts)` once, from a `LaunchedEffect`. The ViewModel then:

- builds a `GroupEventType` from each one (preset times, preset color as `defaultColor`, `swappable = true`);
- passes each to `setPendingEventType`.

After that, they are ordinary pending types. `EventTypeDetail` edits them by id, `createGroup`
writes them, and `group_event_type_created` is logged per type as today.

The call is idempotent per ViewModel: a `proposed` flag lives in the ViewModel, so recomposition or
rotation does not seed twice. The `init` that clears leftover pending types runs first.

The four presets are `ShiftPreset.Morning`, `Afternoon`, `Night` and `MorningAfternoon`, selected by a
`ShiftPreset.groupDefaults` list next to the enum. They stay in the app layer, not core as the
proposal's Impact suggested, because they carry `StringResource`s and Compose colors.

Removing a type needs `GroupRepository.removePendingEventType(id)`, plus a remove affordance on the
type row. The affordance is shown only while the group is new: existing groups keep today's behaviour.

*Alternative:* resolve the preset names at save time, as the shift setup does. That is rejected
because the types must be editable through `EventTypeDetail` before saving, and that screen reads them
from `pendingEventTypes` by id.

### 6. The invite step is a state of `GroupDetail`, not a route

On a successful create, the ViewModel sets `created = true` instead of `isSaved`. The form already
holds the saved group's id and code: `loadedGroup = saved`, and the form's `groupId` is updated so
that `canPassOnCode` is true. The screen then swaps its content for a `GroupCreatedContent`, which
has:

- a title and body;
- a primary share button using the same text and link as `ShareInvitationAction`;
- a "Done" text button that sets `isSaved`, which triggers today's `goBack()`.

System back does the same as "Done". Sharing calls the existing `onInvitationShared()`.

*Alternative:* a new `MainRoute.GroupCreated`. That is rejected because `Navigator` has no replace:
back would land on an empty creation form. A state keeps the back stack as it is today.

The share button's text and click are extracted from `ShareInvitationAction` into one
`rememberInvitationShare(form)` helper. The top-bar action, the invite step and the single-member
card all use it, so the three can never send different texts.

### 7. The single-member card derives from `members`

`GroupDetailUi.Success` gains `val isAlone: Boolean`, which is `!isNew && members.size == 1`. It is
recomputed in `show()` on every group emission, so an accepted request removes the card through the
listener that is already there. The card uses the invitation share helper, so it follows
`canPassOnCode`. Any member can invite, so it shows for non-admins too, although a lone member is
almost always the admin.

### 8. Remote Config and analytics

- `RemoteKey.TEAM_PROMPT_ENABLED` (`teamPromptEnabled`, default `false`) and
  `TEAM_PROMPT_THRESHOLD` (`teamPromptThreshold`, read as `Long`, default `5`).
- `FeatureFlags.teamPromptActive` is `enabled && threshold >= 1`.
- `AnalyticsEvent.OnboardTeamShown` and `OnboardTeamAnswered(choice)`, logged in
  `TeamPromptRepositoryImpl.shown()`/`answered()`. `shown()` logs only the first time: it is
  guarded by the settled flag, like `SharePromptRepositoryImpl.shown`. `answered` compares and clears
  `pending`, so a double tap logs once.
- `TeamPromptChoice` is an enum `Create | Join | Dismissed` with its string value. `choice` is added
  to CLAUDE.md's list of parameters to register.

## Risks / Trade-offs

- **The team prompt settles if the user has a group at the threshold, even if they leave it later.**
  → Accepted by the spec: someone who has been in a group already knows what groups are for.
- **A user offline for every event past the threshold never gets the prompt.** → The check reruns
  on every event added, and each costs one read at most.
- **Proposed type names freeze in the language the form opened in.** → They are data, like the shift
  setup's types, and the user can rename them. This matches the spec.
- **The share prompt is suppressed for the whole calendar visit after the team prompt.** A user who
  never leaves the calendar tab sees it only on the next launch. → Intended: two sheets back to back
  are exactly what the precedence rule avoids.
- **The E2E group creation flow breaks.** The group now gets four more types, and an invite step
  follows the save. → Update `GroupsRobot.createGroup` to finish the invite step, and adjust
  assertions that count types.

## Migration Plan

- Ship with `teamPromptEnabled` off. The other three nudges have no switch: they are form and screen
  changes.
- Register `choice` in GA's custom definitions before turning the prompt on.
- Rollback: turn `teamPromptEnabled` off. No data needs undoing, since nothing is written to Firestore.
