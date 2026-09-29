# Turnia

Kotlin Multiplatform (Compose Multiplatform, Android/iOS) application for managing and swapping shifts in the healthcare sector. The backend is 100% Firebase (KMP client, no custom server). The central goal is **full traceability of chained shift changes** (A→B→C) that today are handled over WhatsApp, causing confusion.

## The problem

Shift changes are currently handled over WhatsApp, which causes confusion and errors, especially when **chained changes** happen:

> A swaps their shift with B. Later, B swaps that same shift (originally A's) with C.

Without a single source of truth it is easy to lose track of who actually covers each shift. Turnia solves this by keeping full traceability of every change and a state that is always consistent.

## Goals

- Single source of truth about who covers each shift at any given moment.
- Full traceability of the chain of changes (auditable, append-only history).
- Conflict prevention (double assignments, changes over already-reassigned shifts).
- Notifications to the parties involved on every relevant change.
- **Monetization** via ads (free tier) and premium subscriptions (see *Monetization*).
- **Bounded backend storage**: Firebase only keeps recent events; older ones are purged and live on-device (see *Data retention & local cache*).

## Domain concepts

Everything on a calendar is an **event** (there is no separate "shift" term).

| Concept | Description |
|---------|-------------|
| **User** | Healthcare professional (nurse/doctor); can belong to several groups. |
| **Group** | A team an admin creates; defines its own group event types. Members see the group's events. |
| **Group event type** | An event template of a group (name, description, optional start/end time). Carries a **default color**, fixed when the type is created; each user may override it with one of their own. |
| **Group event** | Stored under its group (`groups/{groupId}/events`); has an `ownerId` (creator) and an `assigneeId` (who performs it). Can be offered for swap. |
| **Personal event type** | A template a user defines for themselves (name, color, optional description/times). |
| **Personal event** | Belongs to one user and no group. Sealed: a **personal typed event** (`PersonalTypedEvent`), an instance of a personal event type on a date, or a personal one-off event. |
| **Personal one-off event** | A personal event with no type: it carries its own name, color, notes and a start and end date-time, or is **all day** and only its dates count. It may span several months. |
| **Swap offer** | The assignee offers their event; another member can take it. |
| **Transfer** | A member takes an event offered for swap; it moves to the new assignee and is logged (A→B). |
| **Shared calendar** | A user can grant another user full read access to their calendar (across groups). |
| **Change chain** | Ordered, append-only history of transfers over the same event. |

## Business rules

- An **admin** creates a group and defines the group's group event types.
- A group is **created with at least one group event type**: a group nobody can add a shift to is
  not finished, so the form will not save until one exists. A type created before the group has
  nowhere to be written — types live on the group's own document — so it waits in
  `GroupRepository.pendingEventTypes` and the group is created with the whole lot in a single write,
  never existing without them. The rule is about the moment of creation: an existing group is not stopped from
  deleting its last type.
- Each group has a **single invitation**. Anyone with the code can **request** to join; a group **admin must accept** the request.
- **Auto-approve** (whoever knows the code walks straight in) is decided when the group is created,
  alongside the code itself, and means the same thing before and after saving.
- **Any member can invite** with the share button on the group's screen, which sends the invitation
  link — code included. Only an admin sees the code itself, regenerates it or sets auto-approve;
  the rest of the group never sees the Invitación section.
- A user can put a group event **up for swap**; another member can take it (it moves to the taker).
- A user can **delete their own** event; an **admin** can delete any group event. Deleting removes it (there is no cancelled state).
- Someone who **took** a shift cannot delete it — it was never theirs — but can **give it back** with the
  same X: `returnEvent` hands it to whoever held it before them, **offered for swap again**, and tells them
  with a push. Holders form a stack (A → B → C: C gives it back to B, B to A), and a shift whose previous
  holder has left the group cannot be given back.
- **Personal events** can carry notes (on the event); group event docs are shared with all members, so they hold no private notes.
  A user's note on a group event lives under them instead, in `users/{uid}/groupEventExtras/{eventId}`,
  readable by nobody else — not the group, not whoever their calendar is shared with. It stays with the
  event whoever holds it, and clearing it writes `notes = null` so the delta sync sees it go.
- **A personal shift can be moved to a group.** From its row on the user's own calendar, into one of
  the event types of a group they are a member of (not revoked): **only this one**, or **all** of its
  type dated from `RetentionWindow.start` (a month ago) onward. A day where they already hold a shift of
  that group is skipped and reported. The group event keeps the personal event's id and becomes theirs
  (`ownerId == assigneeId`, not on swap), the personal event is soft-deleted in the same commit, and its
  notes become their private note on it. Moving all then **deletes the personal type**: it leaves the
  add pane and *My shifts*, while the older and skipped events keep rendering with it. It is client-only,
  in commits of 150 events with the type deleted in the last one, so a move that fails part-way is
  finished by running it again.
- A user can define their own **personal event types** and add **personal events** (no group), each colored by its type.
  The interface calls them **shifts** ("My shifts", "turnos"); the code keeps `PersonalEventType`.
- **A new user sets up their shifts before anything else.** After sign-in, an account with no
  personal type, no group and no invitation on the way lands on *What shifts do you work?*: Morning,
  Afternoon and Night selected, Morning & afternoon and 24h duty offered, times editable, and a
  shift of their own added inline. Confirming creates **personal types only**, never a group, in
  one write and in the language on screen. It can be skipped, and the skip is kept **on the
  device** (`shift_setup_settled` in DataStore), never on the account. An empty cache proves
  nothing, so the decision takes the server's word — at most two `limit(1)` reads, once per device
  — and without an answer the setup waits for the next launch. The add pane lists the user's
  shifts first and a one-off event last; with no shifts it offers the same setup.
- A **personal one-off event** (`users/{uid}/personalOneOffEvents`) is filed under every month it
  spans, not only the one it starts in: it stores `yearMonthStart` and `yearMonthEnd`, a month's query
  matches `yearMonthStart <= month <= yearMonthEnd`, and every write marks **all** those months in
  `users/{uid}/sync/updates.personalOneOffEvents`. An edit that moves it also marks the months it
  leaves, which is why `update` takes the event as it was before — a device showing only those
  months would otherwise keep it on its old date. Deleting it sets `isDeleted` instead of removing
  the document, so the delta sync can tell other devices it is gone.
- **Colors**: a group event type carries a **default color**, the one whoever created the type
  picked, which is what every member sees until they choose otherwise — without it a shift would
  have no background at all. It is **fixed at creation and never changes again**: an admin who
  changes the color later changes only their own, like any other member. Everyone's own color for
  a type lives in `users/{uid}/private/preferences.groupEventTypeColors`, and theirs wins outright: the shift is painted
  in one colour, theirs. A type saved before the default color existed falls back to one derived
  from its id — the same for everyone in the group. Personal event types carry their own color and
  have no second one. A **group** also carries its own color, with the same id-derived fallback.
  The screen shows **one** color picker either way: the user picks a color and the app decides where
  it goes — onto the type as its default when the type is being created, onto that user's own
  override every time after.
- **A user has a name before they get in.** Apple, and sometimes Google, sign a user in without one;
  a modal then asks for it over whatever screen the session reached, and cancelling signs them out.
  It only asks once the profile has been read — `User.needsName` — so an offline launch that has
  only heard from Auth is not mistaken for a nameless account.
- **Avatar**: a user picks an **animal icon** and a **background colour** for themselves; both live
  on their public profile, so anyone who can find them sees the same one. Neither is stored until
  it is picked: a profile that has never opened the picker renders the client's default — the first
  colour of the palette behind a default animal — and the default lives in the client, not in the
  document, so changing it re-skins everyone who never picked. Unlike the name, the avatar is
  **copied nowhere**: it is read from the profile wherever a person is shown, so changing it needs
  no fan-out and no Cloud Function.
- A user can belong to **several groups** and can invite another user to view **their entire calendar** (crossing groups).
- Whoever a calendar is shared with can **hide** it: it leaves "Shared with me" for a *Hidden* chip,
  by swipe or long press, and comes back the same way. It is their view only — the owner's grant
  stands and the owner is never told. The list lives in `users/{uid}/private/preferences.hiddenSharedCalendars`
  and survives a revoke and a new grant, so a hidden owner who shares again sends no push.
- A user can **leave** a group, and an **admin** can **remove** a member. Either way, if they still hold events
  there they become **revoked**: moved from `memberUids` to `revokedUids`, dropped from `members`, and left
  able to see only their own events and only the event types those events use. They can no longer create
  events in that group. Someone with no events is simply removed, with no `revokedUids` entry.
- The **last admin** of a group with other members cannot leave it, and an admin cannot remove another admin.
- A user can **delete their account** from their profile. It is **anonymized, not erased**, by the `deleteAccount`
  Cloud Function: the uid stays wherever it is referenced (`memberUids`, events, `history`), so no chain
  loses a link, and what goes is everything that says who it was — name and username on the profile and
  on every group's copy, avatar, username reservation, and everything under `users/{uid}/**`. A blank
  name renders as a former member. A group the account is alone in is deleted, pending join requests
  are withdrawn, and the Auth user is deleted last. The last-admin rule applies: the only admin of a
  group with other members is refused.
- The **app language** follows the device unless the user picks English, Spanish, French, German or Italian in *Preferences*.
  The pick lives where the OS keeps it — Android's per-app language (through `AppCompatDelegate`,
  which is why `MainActivity` is an `AppCompatActivity`) and iOS's `AppleLanguages` — so a push drawn
  with the app closed is in the same language.

## Monetization — pricing and ads/premium business rules redacted from this repository's history; see CLAUDE.local.md.
## Growth: the share prompt

- **A sheet asks the user to share Turnia at milestones of events added.** Every new event counts
  — a shift of a type or a one-off, never an edit — and each milestone shows the sheet once, shared
  or dismissed. Several milestones passed at once make one sheet, for the highest.
- **Remote Config drives it, and it ships off:** `sharePromptEnabled` (Boolean, `false`) and
  `sharePromptMilestones` (a JSON array in a string, e.g. `"[10, 100, 200]"`; anything that does
  not parse means no milestones). The console keeps `sharePromptEnabled` off by default and turns
  it on per app with a condition — iOS first, Android once the app is on Google Play.
- **Nothing touches Firestore.** The count and the last milestone shown — its value, so the array
  can be edited safely — live in the device's DataStore (`SharePromptRepository`). A milestone is
  spent only once its sheet is on screen.
- **The count is per device, not per account.** Another account signed in on the same phone
  inherits it, and the demo mode counts in the same file.
- **It shows on the user's own calendar and on group calendars**, where events are added — never
  on a colleague's, which the user only reads. A prompt due elsewhere waits until one of those is
  open.
- **The event that reached the milestone picks the message**: a typed event speaks to coworkers, a
  one-off to friends.
- **The link is the landing page with `utm_*` tags** (`ShareLink`), never `/join/`, which would open
  an installed app. The page forwards the tags into Google Play's `referrer`, so Analytics attributes
  an Android install on its own; iOS only gives downloads per campaign.
- **Analytics:** `share_prompt_shown`, `share_prompt_shared` and `share_prompt_dismissed` with
  `audience` and `milestone`, and `sign_up` with its `method` when an account is created.

## Growth: the team prompt and group invites

- **A sheet asks "Do you work with a team?" once the user has been using the app**: never on first
  launch, but once the share prompt's count of events added reaches `teamPromptThreshold` (a whole
  number, `5` by default). It ships off behind `teamPromptEnabled` (Boolean, `false`); a threshold
  below 1 also turns it off. It offers *Create a group*, *I have a code* (the Groups tab with the
  join sheet open and empty) and *Not now*.
- **Only for someone in no group, decided by the server.** A revoked group does not count. The
  check is one `limit(1)` read per event added past the threshold. It stops for good once the
  prompt has been shown, or once the server says the user has a group: someone who has been in
  a group already knows what it is for.
- **Once per device, remembered in DataStore** (`team_prompt_settled`), never in Firestore. It is
  spent only once the sheet is on screen, like a share prompt milestone.
- **Only on the user's own calendar, and it wins over the share prompt.** When both are due, the
  share prompt keeps its milestone and waits until a calendar is opened again, never straight after
  the team prompt closes. A prompt already on screen is never replaced: a team prompt that becomes
  due while a share prompt is up (its server read came back late) waits for the next visit.
- **A new group starts with Morning, Afternoon, Night and Morning & afternoon proposed**, in the
  language on screen when the form opens, so naming it is enough to create it. The proposed types
  can be edited or removed, and at least one type is still required.
- **Creating a group leads to "Your group is ready"**, with the invitation link's share button, and
  **a group with a single member shows a card asking for the team**. The card derives from the
  members the screen has already loaded, so it costs no read. Every share button sends the same
  link and text, and logs `group_invite_shared`.
- **Analytics:** `onboard_team_shown`, and `onboard_team_answered` with `choice` (`create` | `join` |
  `dismissed`).

## Analytics

- **Every name lives in `AnalyticsEvent` and `AnalyticsUserProperty`**, never written at a call
  site. The console groups by the literal string, so a renamed event starts a new series and
  orphans the old one: existing names are never changed.
- **An action is logged in its repository, once it has succeeded** — after the `Outcome` says so,
  never before the write. The repository is the one place every screen that reaches the action
  passes through. The app layer logs only what no repository sees: `screen_view`, the group
  invitation's share sheet, opening a colleague's calendar, and the `app_language` property.
- **Nothing that identifies a person**: no id, name, username, email, invitation code, notes or
  other free text in a parameter or a property. Every value is a boolean (sent as `"true"` /
  `"false"`), a small count or one of a fixed set of strings. The account is tied to its reports
  only by `setUser`.
- **`login` is a sign-in, not a launch**: it is logged only when a user follows a signed-out state
  in the same process, and only for an account that already existed — a new one logs `sign_up`.
- **User properties cost no listener**: `group_count` and `is_admin` ride on `getGroups()`, which the
  Calendar tab already collects, and are set only when they change.
- **Parameters and properties must be registered by hand** in the GA console (*Admin → Custom
  definitions*), or they show only in DebugView and BigQuery. Event-scoped: `screen_name`,
  `method`, `audience`, `milestone`, `auto_approve`, `type_count`, `all_day`, `multi_month`, `via`,
  `type`, `interacted`, `custom_type_count`, `kind`, `choice`, `scope`, `event_count`,
  `skipped_count`. User-scoped: `group_count`, `is_admin`, `app_language`. A new one is added there too.
- **The shift setup** logs `onboard_shift_shown`, `onboard_shift_skipped` and
  `onboard_shift_completed`, all with `via` (`onboarding` | `add_pane`); the last two carry
  `interacted` — whether the user touched the panel at all before leaving it — and completion adds
  `type_count` and `custom_type_count`. `first_event_added` with `kind` (`typed` | `one_off`) is
  the activation signal, once per device, on the share prompt's counter.
- **A move to a group** logs `personal_events_moved` once, with `scope` (`one` | `all`), `event_count`
  and `skipped_count` — never a `group_event_created` or `personal_event_deleted` per event, and it
  counts toward no prompt. A note on a group event logs `group_event_notes_saved`; `event_notes_saved`
  stays the personal one.
- The E2E suite swaps `Analytics` for `RecordingAnalytics`; assert through `awaitLogged` and
  `awaitUserProperty`, since a report follows the backend's answer, not the screen.

## Data retention & local cache

Firebase must **not** accumulate every past event forever. The backend keeps only a **recent window**; anything older is purged from Firestore and preserved **on the device**.

- **Retention window** — an event whose `date` is **older than 1 month** is eligible for deletion from Firestore.
- **Scheduled cleanup** — a **Cloud Scheduler**-triggered Cloud Function periodically deletes old group events, personal events and their `history` from Firestore. This is the only writer allowed to delete past events in bulk.
- **Local NoSQL cache** — events fetched from Firestore are stored in an **on-device NoSQL/document store**, in a **normalized** shape, so they can still be listed and shown after the server purge. The calendar reads from this local cache and only syncs the recent window from Firestore.
- **Traceability caveat** — because `history` older than the window is purged from Firebase, the full A→B→C chain for old events survives **only in the local cache** of the users who synced it. Retention is a deliberate trade-off against the append-only-forever history.

### Business rules — retention

- The client treats Firestore as the source of truth for the **recent window** and the local cache as the store of record for **older** events.
- Only the scheduled Cloud Function deletes past events in bulk; clients never mass-delete history.
- The local cache is normalized (events reference their types/users by id) to avoid duplication and allow rendering colors/types offline.

## Code style

- **Naming** — one word per concept, no synonyms. The vocabulary is fixed:

  | Word | Means | Never call it |
  |------|-------|---------------|
  | `EventType` | The **template** an event is created from. Sealed: `GroupEventType` \| `PersonalEventType`, `Template*` |
  | `Personal` | Belongs to one user, no group. | `Custom`, `Private`, `Own` |
  | `Group` | Belongs to a group. | `Shared`, `Team` |
  | `EventSource` | **Where an event comes from** (`GROUP` \| `PERSONAL`) — an axis, not a template. Field name: `source`. | `type` (that word is taken by `EventType`) |
  | `swap` | Offering an event so another member takes it. `GroupEvent.onSwap` = offered right now; `GroupEventType.swappable` = the type allows it at all. | `sale`, `trade`, `sell` (`onSale` is dead) |
| `revoked` | A former member who still holds events in the group, so they keep read access to **their own**. Field: `groups/{g}.revokedUids`; domain flag: `Group.isRevoked`. | `removed`, `kicked`, `banned`, `inactive`, `archived` |

- **Comments are written in English — all of them, with no exceptions.** KDoc, block comments,
  inline comments and `TODO`s alike, in Kotlin and in the Cloud Functions' TypeScript. The code,
  its identifiers and this document are in English, so a comment in any other language forces the
  reader to switch language mid-file. User-facing strings are a different matter and stay in
  `composeResources`, never in the source: English in `values/` (the default, so any other device
  language falls back to it), with translations in `values-es/`, `values-fr/`, `values-de/` and `values-it/`. A new string goes into all five.

- **Comments** — do **not** add a comment to every file, function or header. Comments belong only on **non-obvious, non-logic** code (a business rule, a workaround, a subtle invariant, a "why"). A comment that restates what the code already says is redundant — omit it.

- **Firestore calls** — every read and every write reports itself to the usage audit. See *[Firestore usage tracking](#firestore-usage-tracking)*.

## Error handling & UI state

These follow the [official Android architecture guidance](https://developer.android.com/topic/architecture/recommendations), with one deliberate deviation (`Outcome`, below).

### Errors are state, never one-off events

**Do not send events from the ViewModel to the UI** (official strength: *Strongly recommended*). Never surface an error through a `Channel`, `SharedFlow` or any other one-shot stream: when the producer (the ViewModel) outlives the consumer (the Compose UI), delivery is not guaranteed and the user can silently miss the message.

Every failure the user must see becomes a **field on the UiState**, and the UI notifies the ViewModel once it has been shown:

```kotlin
data class Success(
    val form: EventTypeForm,
    val userMessage: StringResource? = null,
) : EventTypeDetailUi
```

```kotlin
fun userMessageShown() = _uiState.update { /* … */ copy(userMessage = null) }
```

```kotlin
state.userMessage?.let { message ->
    val text = stringResource(message)
    LaunchedEffect(message) {
        snackbarHostState.showSnackbar(text)
        viewModel.userMessageShown()
    }
}
```

The governing principle: *UI state is a faithful representation of what is displayed on screen at every point in time* — either the message is displayed or it isn't.

### The ViewModel never builds display text

A UiState carries a **`StringResource`**, or a semantic error type the UI maps to one — never a resolved `String`.

- `stringResource()` is `@Composable` and cannot be called from a ViewModel.
- A ViewModel survives configuration changes **including locale changes**. Text resolved inside it freezes at the language in effect when it was produced; a `StringResource` re-resolves on recomposition.
- It keeps ViewModels unit-testable with no resource context.

`core` must never depend on Compose resources: repositories return **semantic error types**, and the UI layer maps those to a `StringResource`.

### Terminal vs. transient failures

| Kind | Where it goes |
|------|---------------|
| The screen cannot render at all (group not found) | Its own UiState variant, e.g. `Error(val error: …)` |
| An action failed but the screen is still usable (colour didn't save) | A `userMessage` field on the existing `Success` state |

### `Outcome<T, E>` instead of `Result<T>`

Use [`Outcome`](core/src/commonMain/kotlin/com/geoviksoft/turnia/core/system/Outcome.kt) for anything that can fail with a **known** domain error.

`kotlin.Result` constrains failures to `Throwable`: it forces domain errors to be modelled as exceptions, allocates stack traces nothing reads, and lets `getOrNull()` collapse the reason for a failure into `null`. `Outcome` keeps the error as a sealed type or enum, so a `when` over it is checked by the compiler.

```kotlin
suspend fun getGroup(idGroup: String): Outcome<Group, GroupError>
```

Build one with `value.toSuccess()` / `error.toFailure()` — both work on any receiver, and `Outcome` is covariant in `T` and `E`, so the narrow type they infer widens to the declared return type on its own.

- **Exceptions stop at the data layer.** Firebase/GitLive genuinely throw — wrap those calls in `outcomeCatching { }` (or bridge with `Result.toOutcome { }`) inside the repository and map to a domain error there.
- A `Throwable` must never reach a ViewModel or a UiState.

> This deviates from the official guidance, which makes exceptions the primary mechanism and presents `Result` only as an alternative. We prefer typed errors because the UI layer has to map every failure to a message exhaustively, and an open `Throwable` hierarchy gives the compiler nothing to check.

## Tech stack

- **Kotlin Multiplatform (KMP)** — shared business logic.
- **Compose Multiplatform** — shared UI (Android / iOS).
- **Coroutines + Flow** — asynchrony and reactive state.
- **Koin** — dependency injection (DI).
- **Navigation 3 (Nav3)** — shared, back-stack-based navigation.
- **kotlinx.serialization** — serialization (navigation keys, DTOs).
- **Local NoSQL cache** — on-device document store (candidate: Realm Kotlin) holding events fetched from
  Firestore in a **normalized** shape, so past events stay viewable after Firebase purges them (see *Data retention & local cache*).
- **DataStore (KMP, Preferences)** — on-device key/value settings that exist before any account, such as
  whether onboarding was seen. Read and written only through a repository: `AppConfigRepositoryImpl`
  (onboarding) and `SharePromptRepositoryImpl` (the share prompt's count), over one shared file.
- **AdMob** — ads for free-tier users (banner / interstitial / rewarded). Hidden for premium users.
- **In-app subscriptions** — **Google Play Billing** (Android) and **StoreKit / App Store** (iOS) for premium plans.
- **Firebase** — backend, no custom server:
  - **Firestore** — data.
  - **Firebase Auth** — authentication.
  - **Cloud Functions (TypeScript)** — join requests, taking events, push, shared-calendar aggregation,
    account deletion (anonymization),
    **subscription receipt verification** (Play RTDN / App Store Server Notifications) and the
    **scheduled retention cleanup** (see *Data retention & local cache*).
  - **FCM** — push notifications.
  - **Crashlytics** — crash reporting. Auto-initialized with the rest of Firebase; nothing calls it
    to start it. It has no GitLive wrapper, so only the **Android** `Logger` reports through it
    (breadcrumbs for every line, a non-fatal for every error carrying a throwable) — on iOS it
    catches crashes on its own and hears nothing from shared code.
  - **Analytics** — only real users of release builds report to it. **Debug builds never do**, and
    nothing at runtime can switch them on: `firebase_analytics_collection_deactivated` in the debug
    `AndroidManifest.xml`, `ANALYTICS_COLLECTION_DEACTIVATED` in `Config.xcconfig` on iOS. That
    covers the E2E suite, whose tests each clear the app's data and would otherwise arrive as a
    new install. **A release build starts with collection off** (`firebase_analytics_collection_enabled`
    in the main manifest), because the SDK logs `first_open` before any app code runs;
    `TurniaApplication` turns it on unless the device is on **Test Lab**, where Play's pre-launch
    report crawls every upload.
  - **App Check** — proves requests come from the genuine app. Installed in platform code before
    Firebase is touched (`TurniaApplication`, `iOSApp.init`); the native SDKs under GitLive then
    attach the token on their own. Android release builds attest with **Play Integrity**, iOS
    release builds with **App Attest**; debug builds use a **debug token**, printed on first launch,
    that has to be registered in the console once per device. **Not enforced yet**: Firestore and
    Authentication were briefly enforced on 2026-09-29 and reverted to monitoring-only the same day
    after it crashed the app and blocked Google Sign-In on a sideloaded test build — see
    `openspec/changes/enable-app-check-enforcement` for the staged rollout that replaces that ad hoc
    attempt. Functions has no metrics at all yet: `enforceAppCheck: true` has been added to its
    `onCall` functions but not deployed, since Cloud Functions has no monitoring-only mode — deploying
    it is immediate enforcement. Enforcing before a release that attests is in users' hands locks
    every older build out.
  - **Cloud Scheduler** — triggers the periodic retention cleanup of old events.
- **GitLive Firebase Kotlin SDK** (`dev.gitlive:firebase-*`) — Firebase access from `commonMain`.
- **Native FCM per platform** — push reception uses the native SDK on each platform (iOS involves APNs, `AppDelegate` and permissions).

## Firestore data model

See [firebase/firestore-schema.md](firebase/firestore-schema.md) — the single source of truth for collections, fields, enums, access rules and invariants.

## Firestore usage tracking

Firestore bills **per document**: one read for every document the server returns, one write for every document sent to it. The local cache is the only lever we have on that bill, so its effect has to be measurable — a read served from cache is free, and we count those apart to see the caching working.

What a user is expected to cost, per action and per day, is in [firebase/firestore-usage.md](firebase/firestore-usage.md) — update it when a change moves those numbers.

**Every Firestore call must report itself** through [`FirestoreUsageMetrics.kt`](core/src/commonMain/kotlin/com/geoviksoft/turnia/core/data/datasource/firestore/analytics/FirestoreUsageMetrics.kt). A new datasource, or a new query in an existing one, is not finished until it does.

| Call | How to report it |
|------|------------------|
| `.get()` on a query | chain `.trackData(TAG, "operation")` onto the snapshot |
| `.get()` on a document | chain `.trackData(TAG, "operation")` — a document that does not exist still costs a read |
| a listener (document or query) | subscribe through `.trackedSnapshots(TAG, "operation")` instead of `.snapshots` — never `.snapshots` + `trackData` |
| `set` / `updateFields` / `delete` | `trackWrite(TAG, "operation")` on the line **after** the call |
| `httpsCallable(NAME)` on a Cloud Function | `trackFunction(NAME)` on the line **before** the call |

`TAG` is the reporting class's own log tag, and `operation` names the **call inside it**: usage is counted per class *and per query*, so the audit says who spent the reads, how many, and through which of that class's calls — the only one of the three you can act on. Function calls are the exception: they are counted per callable, which is already both.

The operation is a **key**, so it has to be short and stable. `"events(SERVER)"` is a good name — the source is worth splitting out, since it is exactly the cache/server distinction the audit exists to show. Anything built from an id (`"group-$groupId"`) is not: it would give every group its own line and drown the summary. Where one private helper serves several public writers — `UserSyncFirestore.write`, which backs `writePersonalEvents`, `writePersonalEventTypes` and `writePrivate` — the name is passed in by the caller, because the helper is not the thing you would go and change.

Two rules that are easy to get wrong:

- **`trackWrite` goes after the write, never before.** Inside `outcomeCatching { }` that means a call which threw never gets counted — a write rejected by the security rules is not billed, and counting it hides real failures behind plausible numbers.
- **A listener is billed when it attaches, even if nothing changed.** Without metadata changes the SDK
  raises no event when the server only confirms the cache, so `trackData` on `.snapshots` never saw
  it. `trackedSnapshots` listens with metadata changes and counts the result set on every attach —
  an upper bound, since a re-attach within 30 minutes of the last listen is free — then only the
  documents a remote change touches. Its extra metadata-only emissions repeat the same value, so
  follow it with `distinctUntilChanged()`.
- **A write has no cache variant.** It is billed even offline; the charge simply lands when the device syncs. Only reads can be free.

- **`trackFunction` goes *before* the call**, which is the opposite rule and has the opposite reason: a callable is billed the moment it reaches Google, refusals included, so counting it afterwards would hide exactly the failures worth seeing. What a function then spends on its own reads and writes never reaches this audit — it happens server-side with admin privileges — so one `Calls: 1` can stand for a dozen documents.

- **`trackFunction` takes the callable's name, not `TAG`.** It is the only reporter that does not key on the calling class: what costs money is the function, and `GroupMembershipFunction` alone calls four of them.

The audit logs under the `FirestoreAudit` tag. A billed call — a server read or any write — prints the running totals and then the breakdown per class; a cache hit only counts, at `debug`, so the noisy line is the one that costs money.

The two bills are printed apart, because they are priced in different units — Firestore per document,
a callable per invocation — and a class only appears in the block where it actually spends:

Under Firestore each class then breaks down into its own calls, ordered by what they cost the server.
Functions do not break down: the callable's name is already the whole answer.

```
FirestoreAudit: GroupFunction - FUNCTION CALL
Firestore  Read Server: 14 ReadCache: 61 Writes: 3
    PersonalEventFirestore -> Read Server: 11 ReadCache: 58 Writes: 0
        · events(SERVER) -> Read Server: 11 ReadCache: 0 Writes: 0
        · events(CACHE) -> Read Server: 0 ReadCache: 58 Writes: 0
    UserPathFirestore -> Read Server: 3 ReadCache: 3 Writes: 2
        · fetchProfile -> Read Server: 3 ReadCache: 0 Writes: 0
        · userDoc(snapshots) -> Read Server: 0 ReadCache: 3 Writes: 0
        · updateTypeColor -> Read Server: 0 ReadCache: 0 Writes: 2
    UsernameFirestore -> Read Server: 0 ReadCache: 0 Writes: 1
        · claim -> Read Server: 0 ReadCache: 0 Writes: 1
Functions  Calls: 3
    deleteGroup -> Calls: 1
    requestToJoinGroup -> Calls: 2
```

## Traceability

- The `history` is **append-only** and lives **on the event document**, as an array: a subcollection would cost
  a read per event to show the chain, and the array is frozen for clients by the rules, so only `takeEvent`
  and `returnEvent` can add to it.
- A transfer changes `assigneeId` in place, so the event never moves and the history never has to be copied
  forward: the chain is simply the entries of that one event.
- It records two things: `transferred` (a member took it) and `returned` (the holder gave it back), both with
  `fromUid`→`toUid`. Offering a shift writes no entry — the `onSwap` flag already says so, and a client cannot
  append to a frozen array anyway.
- A hand-back is appended, never an erasure: the chain keeps saying the taker had it. The app draws the chain
  **forward only** — a hand-back is just the next step (A → B ↷ A), marked only by a red arrow that curves forward, since
  the shift is then offered again like any other. `returned` still matters underneath: it is what tells who the next hand-back goes to.
- Each entry points to `parentEventId`, so the full chain A→B→C can be reconstructed.
- **Taking an event offered for swap** runs in a `takeEvent` transaction that checks `onSwap == true` before moving it, to prevent double assignment.
- **Two members taking the same shift is resolved in arrival order, and that is all "FIFO" means here.**
  `tx.get` locks the event, so both takers read `onSwap: true`; Firestore aborts the later commit and
  re-runs it, and the re-run finds the flag cleared and answers `TakeEventNotOnSwap` — which the client
  shows as *somebody got there first*. There is deliberately **no queue**: nobody holds a place, an
  earlier tap from a device that was offline longer wins nothing, and a waiting list would cost a
  claims subcollection, a trigger and rules to protect them for a race that settles in milliseconds.

## Invitation links

- A group's code is shared as `https://turnia.club/join/CODE` ([`InvitationLink`](core/src/commonMain/kotlin/com/geoviksoft/turnia/core/domain/model/InvitationLink.kt)):
  an https link because it is the only kind that still leads somewhere without the app. The page
  behind it (`firebase/hosting/index.html`, the site's own landing page) shows the code, points at the store, and opens an
  installed app through `turnia://join/CODE`.
- **Host, scheme and path live in [`InvitationLinkConfig`](core/src/commonMain/kotlin/com/geoviksoft/turnia/core/domain/model/InvitationLinkConfig.kt).**
  Files outside Kotlin repeat them and must change with it: `AndroidManifest.xml`, `Info.plist`, the
  entitlements, and `firebase/hosting` (`index.html`, `apple-app-site-association`, the rewrite in
  `firebase.json`).
- An opened link is **state, not an event**, exactly like a tapped notification:
  `InvitationLinkRepository` holds the code — through a cold start, or until the user has signed in —
  `MainScreen` brings the Groups tab up, and the Groups tab puts it in the join sheet and marks it
  handled. The code only prefills the sheet; joining still goes through `requestToJoinGroup`.
- **The code survives an install on Android only**: the page hands Play `referrer=code=CODE`, and
  `InstallReferrer` reads it once on first launch. iOS has no equivalent, so there the page tells the
  user to paste the code.
- `assetlinks.json` lists the **debug** signing key only. Before a release, add the SHA-256 of the
  Play App Signing key (Play Console → *App integrity*), or Android opens the page instead of the app.
- The landing page is deployed on its own: `firebase deploy --only hosting`. `turnia.club` is a
  custom domain on the project's Hosting site (Console → Hosting → *Add custom domain*, then the DNS
  records it asks for at the registrar). The apps only verify against it, so links do not open the
  app until the domain serves `/.well-known/assetlinks.json` and `apple-app-site-association` over
  https.

## Permissions (Security Rules)

- **read** `groups/{g}/events`: only members of the group.
- **create** event: the member for themselves — `ownerId == assigneeId == auth.uid`.
- **update** event: the assignee or an admin, with `ownerId`, `assigneeId` and `history` immutable from the
  client (`takeEvent` and `returnEvent` are the only writers that reassign or append to the chain). `onSwap` is narrower
  still — **only the assignee** may move it, because offering a shift is a decision for whoever covers
  it, not for an admin. Whether the shift's *type* allows swapping is **not** enforced here: the flag
  lives inside the group's `groupEventTypes` array and the rules cannot search it by id, so that one
  stays a product rule the client applies.
- **delete** event: only the creator while they still hold it (`ownerId == assigneeId == auth.uid`).
- A group's calendar is one query over its own `events` collection, filtered by `yearMonth`.
- Membership is a field of the group: `isMember(g) = auth.uid in groups/{g}.memberUids`, and reading the group
  itself needs no lookup at all.
- **read** a revoked user's events: `isRevoked(g) && resource.data.assigneeId == auth.uid`. On a `list`
  that term is what forces their query to carry the matching `assigneeId ==` filter — the restriction is
  the rule, not the client's good behaviour. A revoked user cannot read `groups/{g}` at all, since it
  carries the member roster and the invitation code and Firestore hides no fields.

## Sensitive points (do not overlook)

1. **No private fields on shared docs** — Firestore does not hide individual fields: if you can read the document, you read all of it. A group event doc is readable by every group member, so never put private data (notes, etc.) on it — a member's note on one goes in their own `users/{uid}/groupEventExtras`. `users/{uid}` is readable by **any signed-in user** — that is what lets a search result render a stranger's name and avatar — so it holds only the name, the username, the avatar, `calendarSharedWith` and `showAds`; email, FCM tokens, entitlement and the user's own colour picks live in `users/{uid}/private/**`. Personal events are readable only by the owner and their shared users, so their `notes` may live on the doc.
2. **Viewing another user's full calendar (crosses groups)** — group events live under group members, so a user **outside** the group cannot read them directly. The cross-group shared calendar is served **on demand** by the `getSharedCalendar` Cloud Function. A grant has a **single source of truth**: A may read B only if `A ∈ users/B.calendarSharedWith`, a list only B writes. The function checks it, then aggregates the owner's group + personal events for a bounded date range (admin privileges, no stored copy).
3. **A username is a reservation, not a field** — `usernames/{username}` is a public collection keyed by the handle: the lock that makes a handle unique, and the index a prefix search runs over (a document id cannot be prefix-queried, so the handle is repeated as a field). Uniqueness is enforced by Firestore's create-vs-update distinction in the rules, so **always claim the reservation before writing `users/{uid}.username`**, and release the previous one after. Search there is prefix-only; Firestore has no full-text search.

   It carries **no name**: the profile it points at is public, so the name lives in one place. What it carries instead is `updateAt`, a copy of the profile's own — the marker a searcher compares their cached `users/{uid}` against, arriving free inside the query they already paid for, so the second search of a prefix costs nothing beyond it. **A profile write and its marker must land in the same commit**, or the marker is always the later of the two and no cache ever settles. The marker only pays where it arrives free: resolving a known list of uids reads `users/{uid}` directly, since fetching a marker per uid would cost exactly the read it was meant to save.
4. **Joining a group is two steps via Cloud Functions** — `requestToJoinGroup` validates the code/expiration and creates a `joinRequests` doc; `acceptJoinRequest` / `rejectJoinRequest` (admin only) answer it by writing `status`, and accepting also adds the uid to `members`. Do **not** let the client write directly to `members`, and do not let it answer a request: `status` is frozen by the rules. An answered request is **kept** as a receipt, because it is the only thing the requester can read to learn the outcome — a `collectionGroup` query over `joinRequests` cannot satisfy a rule that authorizes by document id, which is why `requestToJoinGroup` also writes a pointer into `users/{uid}/private/joinRequests`. Their app deletes the request and the pointer once it has shown the answer.
5. **Taking / push are server-only** — `takeEvent` reassigns the event in a transaction that verifies `onSwap` first; **push** is sent only from Cloud Functions, never from the client. The notification wording is **not** written server-side: a push has to render while the app is not running, so the server sends a localization key and its arguments (`bodyLocKey` on Android, `loc-key` on iOS) and the operating system fills in the words from the app's own `res/values(-es|-fr|-de|-it)/strings.xml` and `{en,es,fr,de,it}.lproj/Localizable.strings`. That keeps the server in English whatever the app speaks; a new push needs its key, with the same arguments, in all ten files. The data payload carries a `type` for routing the tap. `users/{uid}/private/account.fcmTokens` is one entry per device, touched only through `arrayUnion` / `arrayRemove`, and the server prunes the tokens FCM reports as unregistered.

   **A tapped notification is state, not an event.** The platform delivers the tap whenever it likes — on a cold start, long before the UI that has to act on it exists — so `NotificationRepository` holds the `PushDestination` until a screen says it has navigated, exactly as a `userMessage` is held until it has been shown. A `Channel` would drop precisely the cold-start case. The destinations are split by the back stack that owns them: `GroupDetail` is `RootScreen`'s, the tabs are `MainScreen`'s, and each consumes only its own.
6. **Denormalized names have a keeper** — a group carries its members' names so the calendar costs no read to show them, and `onUserRenamed` is the only thing keeping those copies true. A document and the sync marker that gates it must be written in the **same commit**, or the marker is always the later of the two and no cache ever settles.

   **One marker per document, not one per subcollection.** `users/{uid}/sync/updates` carries `account`, `joinRequests` and `preferences` separately; they used to share a single `private` field, so registering a push token invalidated the join-request cache and answering a request invalidated the account cache. A marker named after a subcollection invites exactly that, because the documents under it are almost never read on the same schedule.
7. **Premium entitlement is server-verified** — the client may *request* a purchase but must never mark itself premium. Only a Cloud Function that validated the store receipt (Play RTDN / App Store Server Notifications) writes `users/{uid}/private/subscription`; security rules forbid the client from setting it. Ad-hiding and premium gating must read the server-verified state, not a local flag.
8. **Retention is destructive & only server-side** — the scheduled cleanup Cloud Function is the *only* thing that bulk-deletes events older than the 1-month window (events + `history`); clients must not. Before purging, the data must already be in each user's local cache, or old events (and their traceability chain) are lost. Sync into the local NoSQL cache before, not after, relying on the purge.

## Project structure

```
turnia/
├── app/
│   ├── androidApp/     # Android entry point
│   ├── shared/         # Shared Compose Multiplatform UI (commonMain / androidMain / iosMain)
│   └── iosApp/         # iOS entry point (Xcode)
├── core/               # Shared domain + business logic (KMP)
└── firebase/           # Firebase project: config + Cloud Functions (deployed separately)
    ├── firebase.json · .firebaserc · firestore.rules · firestore.indexes.json · firestore-schema.md
    ├── functions/      # Cloud Functions (TypeScript): join requests, taking events, push, subscription verification, retention cleanup
    └── hosting/        # Firebase Hosting: the invitation-link landing page + Android/iOS link verification files
```

> `firebase/` holds everything Firebase. `firebase/functions/` is a standalone Node.js/TypeScript project (Firebase CLI, deployed with `firebase deploy`). It runs on Google's servers, not inside the KMP app, and is not part of the Gradle build. Run all `firebase` CLI commands from the `firebase/` directory (where `firebase.json` lives). It exists only to hold backend logic the client must not do itself (see *Sensitive points*).

## Getting started

### Requirements

- JDK 17+
- Android Studio (with the KMP plugin) / IntelliJ IDEA
- Xcode (to build the iOS app, macOS only)

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

### E2E tests

The 50 critical happy paths run on an Android emulator against the Firebase emulators, seeded from
[`firebase/test/fixtures`](firebase/test/README.md) — see that README to run them. They live in
`app/androidApp/src/androidTest/.../e2e`: `flows/` holds the tests, `robots/` drives the screens,
`infra/` seeds the emulators and points the app at them (`TurniaTestApplication`).

- `connected…AndroidTest` starts the Firebase emulators when nothing serves their ports, and stops
  them at the end of the build (`buildSrc/src/main/kotlin/FirebaseEmulators.kt`), so a test
  run from the IDE needs no emulator started by hand.
- Robots find nodes by their **English** text; where the text is not unique on screen they use the
  tags in `TestTags` (`app/shared/.../ui/system/TestTags.kt`). Renaming a tag or an English string a
  robot matches breaks the suite.
- A change to a Firestore field is not finished until the fixtures change with it.
- The suite's CI job lives in `.github/workflows/android-e2e.yml`, runnable on its own from Actions.
  The Release workflow calls it as its `android-e2e` job, and both uploads wait for it. Its
  `runTests` checkbox (on by default) can skip it.

### Android release build

Release builds are minified and obfuscated by R8, and signed with the **upload key** from
`keystore.properties` at the project root (git-ignored; copy
[`keystore.properties.example`](keystore.properties.example)) or the `TURNIA_UPLOAD_*` environment
variables on CI. Without either the build still runs and produces an unsigned APK.

- Build the bundle for Play with `./gradlew :app:androidApp:bundleRelease`. It also uploads R8's
  mapping file to Crashlytics, which is what turns obfuscated crash reports back into readable ones.
- Play App Signing re-signs the app with Google's key, so two things need **that** key's
  fingerprints, from Play Console → *App integrity*: its SHA-1 in the Firebase project settings, or
  Google Sign-In fails in production, and its SHA-256 in `firebase/hosting/.well-known/assetlinks.json`,
  or invitation links open the web page instead of the app.

### Release pipeline

A manual GitHub Actions workflow builds both apps and uploads them — Android to Play's internal
track, iOS to TestFlight. The version name is computed from the last release tag and the kind of
release chosen when it is run (hotfix, minor or major), and the build number is set by the
workflow, on both platforms. It runs from `main` only, and once every chosen upload succeeds it
tags the commit `v<versionName>-<buildNumber>`. Neither app is uploaded unless the Android E2E suite
passes first.
Secrets and setup: [.github/RELEASE.md](.github/RELEASE.md).

## iOS signing & Apple setup

The app is signed by the paid team `83GQ2T4N4H` with automatic signing, so Xcode keeps the App ID's
capabilities in step with [iosApp.entitlements](app/iosApp/iosApp/iosApp.entitlements). What lives
outside the repo, and breaks silently if it is lost or changed:

- **Push** — an APNs `.p8` key is uploaded in Firebase → Project settings → *Cloud Messaging*.
  Without it FCM accepts the send and iOS receives nothing.
- **Sign in with Apple** — Firebase Authentication → Apple holds the Services ID
  `com.geoviksoft.turnia.signin` (return URL `https://turnia-23ebc.firebaseapp.com/__/auth/handler`),
  the Key ID and its `.p8`. Android needs the Services ID, since there the flow is Firebase's browser
  OAuth; the key is also what lets Firebase **revoke Apple tokens**, which Apple requires of an app
  that lets a user delete their account. App Store guideline 4.8 makes the button mandatory, because
  Turnia offers Google sign-in.
- **Universal Links** — `apple-app-site-association` in `firebase/hosting` names the app by
  `TEAM_ID.bundleId`: a change of team or bundle id has to be redeployed there.
- **App Check** — App Attest is registered for the iOS app in the console. Enforcement waits until
  an iOS release is out and its traffic shows as verified.

## License

To be defined.
