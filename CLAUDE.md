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
| **Personal event** | An instance of a personal event type on a date; belongs to no group. |
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
- Both invitation settings — **auto-approve** (whoever knows the code walks straight in) and whether
  **members can see the code** — are decided when the group is created, alongside the code itself,
  and mean the same thing before and after saving.
- A user can put a group event **up for swap**; another member can take it (it moves to the taker).
- A user can **delete their own** event; an **admin** can delete any group event. Deleting removes it (there is no cancelled state).
- **Personal events** can carry notes (on the event); group event docs are shared with all members, so they hold no private notes.
- A user can define their own **personal event types** and add **personal events** (no group), each colored by its type.
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
- **Avatar**: a user picks an **animal icon** and a **background colour** for themselves; both live
  on their public profile, so anyone who can find them sees the same one. Neither is stored until
  it is picked: a profile that has never opened the picker renders the client's default — the first
  colour of the palette behind a default animal — and the default lives in the client, not in the
  document, so changing it re-skins everyone who never picked. Unlike the name, the avatar is
  **copied nowhere**: it is read from the profile wherever a person is shown, so changing it needs
  no fan-out and no Cloud Function.
- A user can belong to **several groups** and can invite another user to view **their entire calendar** (crossing groups).
- A user can **leave** a group, and an **admin** can **remove** a member. Either way, if they still hold events
  there they become **revoked**: moved from `memberUids` to `revokedUids`, dropped from `members`, and left
  able to see only their own events and only the event types those events use. They can no longer create
  events in that group. Someone with no events is simply removed, with no `revokedUids` entry.
- The **last admin** of a group with other members cannot leave it, and an admin cannot remove another admin.

## Monetization — pricing and ads/premium business rules redacted from this repository's history; see CLAUDE.local.md.
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
  `composeResources` — Spanish belongs there, never in the source.

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
- **AdMob** — ads for free-tier users (banner / interstitial / rewarded). Hidden for premium users.
- **In-app subscriptions** — **Google Play Billing** (Android) and **StoreKit / App Store** (iOS) for premium plans.
- **Firebase** — backend, no custom server:
  - **Firestore** — data.
  - **Firebase Auth** — authentication.
  - **Cloud Functions (TypeScript)** — join requests, taking events, push, shared-calendar aggregation,
    **subscription receipt verification** (Play RTDN / App Store Server Notifications) and the
    **scheduled retention cleanup** (see *Data retention & local cache*).
  - **FCM** — push notifications.
  - **Crashlytics** — crash reporting. Auto-initialized with the rest of Firebase; nothing calls it
    to start it. It has no GitLive wrapper, so only the **Android** `Logger` reports through it
    (breadcrumbs for every line, a non-fatal for every error carrying a throwable) — on iOS it
    catches crashes on its own and hears nothing from shared code.
  - **Cloud Scheduler** — triggers the periodic retention cleanup of old events.
- **GitLive Firebase Kotlin SDK** (`dev.gitlive:firebase-*`) — Firebase access from `commonMain`.
- **Native FCM per platform** — push reception uses the native SDK on each platform (iOS involves APNs, `AppDelegate` and permissions).

## Firestore data model

See [firebase/firestore-schema.md](firebase/firestore-schema.md) — the single source of truth for collections, fields, enums, access rules and invariants.

## Firestore usage tracking

Firestore bills **per document**: one read for every document the server returns, one write for every document sent to it. The local cache is the only lever we have on that bill, so its effect has to be measurable — a read served from cache is free, and we count those apart to see the caching working.

**Every Firestore call must report itself** through [`FirestoreUsageMetrics.kt`](core/src/commonMain/kotlin/com/geoviksoft/turnia/core/data/datasource/firestore/analytics/FirestoreUsageMetrics.kt). A new datasource, or a new query in an existing one, is not finished until it does.

| Call | How to report it |
|------|------------------|
| `.get()` on a query | chain `.trackData(TAG, "operation")` onto the snapshot |
| `.get()` on a document | chain `.trackData(TAG, "operation")` — a document that does not exist still costs a read |
| `set` / `updateFields` / `delete` | `trackWrite(TAG, "operation")` on the line **after** the call |
| `httpsCallable(NAME)` on a Cloud Function | `trackFunction(NAME)` on the line **before** the call |

`TAG` is the reporting class's own log tag, and `operation` names the **call inside it**: usage is counted per class *and per query*, so the audit says who spent the reads, how many, and through which of that class's calls — the only one of the three you can act on. Function calls are the exception: they are counted per callable, which is already both.

The operation is a **key**, so it has to be short and stable. `"events(SERVER)"` is a good name — the source is worth splitting out, since it is exactly the cache/server distinction the audit exists to show. Anything built from an id (`"group-$groupId"`) is not: it would give every group its own line and drown the summary. Where one private helper serves several public writers — `UserSyncFirestore.write`, which backs `writePersonalEvents`, `writePersonalEventTypes` and `writePrivate` — the name is passed in by the caller, because the helper is not the thing you would go and change.

Two rules that are easy to get wrong:

- **`trackWrite` goes after the write, never before.** Inside `outcomeCatching { }` that means a call which threw never gets counted — a write rejected by the security rules is not billed, and counting it hides real failures behind plausible numbers.
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
  can add to it.
- A transfer changes `assigneeId` in place, so the event never moves and the history never has to be copied
  forward: the chain is simply the entries of that one event.
- It records only the swap lifecycle: `put_on_swap` and `transferred` (with `fromUid`→`toUid`).
- Each entry points to `parentEventId`, so the full chain A→B→C can be reconstructed.
- **Taking an event offered for swap** runs in a `takeEvent` transaction that checks `onSwap == true` before moving it, to prevent double assignment.

## Permissions (Security Rules)

- **read** `groups/{g}/events`: only members of the group.
- **create** event: the member for themselves — `ownerId == assigneeId == auth.uid`.
- **update** event: the assignee or an admin, with `ownerId`, `assigneeId` and `history` immutable from the
  client (`takeEvent` is the only writer that reassigns or appends to the chain).
- **delete** event: only the creator while they still hold it (`ownerId == assigneeId == auth.uid`).
- A group's calendar is one query over its own `events` collection, filtered by `yearMonth`.
- Membership is a field of the group: `isMember(g) = auth.uid in groups/{g}.memberUids`, and reading the group
  itself needs no lookup at all.
- **read** a revoked user's events: `isRevoked(g) && resource.data.assigneeId == auth.uid`. On a `list`
  that term is what forces their query to carry the matching `assigneeId ==` filter — the restriction is
  the rule, not the client's good behaviour. A revoked user cannot read `groups/{g}` at all, since it
  carries the member roster and the invitation code and Firestore hides no fields.

## Sensitive points (do not overlook)

1. **No private fields on shared docs** — Firestore does not hide individual fields: if you can read the document, you read all of it. A group event doc is readable by every group member, so never put private data (notes, etc.) on it. `users/{uid}` is readable by **any signed-in user** — that is what lets a search result render a stranger's name and avatar — so it holds only the name, the username, the avatar and `calendarSharedWith`; email, FCM tokens, entitlement and the user's own colour picks live in `users/{uid}/private/**`. Personal events are readable only by the owner and their shared users, so their `notes` may live on the doc.
2. **Viewing another user's full calendar (crosses groups)** — group events live under group members, so a user **outside** the group cannot read them directly. The cross-group shared calendar is served **on demand** by the `getSharedCalendar` Cloud Function. A grant has a **single source of truth**: A may read B only if `A ∈ users/B.calendarSharedWith`, a list only B writes. The function checks it, then aggregates the owner's group + personal events for a bounded date range (admin privileges, no stored copy).
3. **A username is a reservation, not a field** — `usernames/{username}` is a public collection keyed by the handle: the lock that makes a handle unique, and the index a prefix search runs over (a document id cannot be prefix-queried, so the handle is repeated as a field). Uniqueness is enforced by Firestore's create-vs-update distinction in the rules, so **always claim the reservation before writing `users/{uid}.username`**, and release the previous one after. Search there is prefix-only; Firestore has no full-text search.

   It carries **no name**: the profile it points at is public, so the name lives in one place. What it carries instead is `updateAt`, a copy of the profile's own — the marker a searcher compares their cached `users/{uid}` against, arriving free inside the query they already paid for, so the second search of a prefix costs nothing beyond it. **A profile write and its marker must land in the same commit**, or the marker is always the later of the two and no cache ever settles. The marker only pays where it arrives free: resolving a known list of uids reads `users/{uid}` directly, since fetching a marker per uid would cost exactly the read it was meant to save.
4. **Joining a group is two steps via Cloud Functions** — `requestToJoinGroup` validates the code/expiration and creates a `joinRequests` doc; `acceptJoinRequest` / `rejectJoinRequest` (admin only) answer it by writing `status`, and accepting also adds the uid to `members`. Do **not** let the client write directly to `members`, and do not let it answer a request: `status` is frozen by the rules. An answered request is **kept** as a receipt, because it is the only thing the requester can read to learn the outcome — a `collectionGroup` query over `joinRequests` cannot satisfy a rule that authorizes by document id, which is why `requestToJoinGroup` also writes a pointer into `users/{uid}/private/joinRequests`. Their app deletes the request and the pointer once it has shown the answer.
5. **Taking / push are server-only** — `takeEvent` reassigns the event in a transaction that verifies `onSwap` first; **push** is sent only from Cloud Functions, never from the client. The notification wording is written server-side, because a push has to render while the app is not running; the data payload carries a `type` for routing the tap. `users/{uid}/private/account.fcmTokens` is one entry per device, touched only through `arrayUnion` / `arrayRemove`, and the server prunes the tokens FCM reports as unregistered.

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
    └── functions/      # Cloud Functions (TypeScript): join requests, taking events, push, subscription verification, retention cleanup
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

## Pending: the paid Apple Developer Program

Two features are written and shipped but cannot work yet, both for the same reason: the project is
signed with a **Personal Team**, which cannot sign either capability. Xcode does not even list them
under *Signing & Capabilities → + Capability*, and writing the entitlement by hand only breaks the
build — Xcode fails to generate a profile. Both are blocked on an Apple Developer Program
membership; neither has a workaround.

- **Push (FCM)** — `aps-environment` is missing from
  [iosApp.entitlements](app/iosApp/iosApp/iosApp.entitlements). Without it iOS receives nothing.
- **Sign in with Apple** — the button is on the sign-in screen and calls `rememberAppleAuthState`,
  but every attempt fails until the setup below is complete. App Store guideline 4.8 requires it
  once an app offers third-party sign-in, and Turnia already offers Google, so this is release
  blocking rather than optional.

### Sign in with Apple — what is left

The Apple provider is already enabled in Firebase Authentication. What remains, in order:

1. **App ID** — developer.apple.com → *Identifiers* → `com.geoviksoft.turnia.Turnia` → tick
   **Sign In with Apple**. This is what makes the capability appear in Xcode.
2. **Services ID** — *Identifiers* → **+** → *Services IDs*. Its identifier must differ from the
   bundle id (e.g. `com.geoviksoft.turnia.signin`). Configure it with the App ID above, the domain
   `turnia-23ebc.firebaseapp.com`, and the return URL
   `https://turnia-23ebc.firebaseapp.com/__/auth/handler`. Android needs this: there the flow is
   Firebase's browser OAuth, not the native sheet.
3. **Key** — *Keys* → **+** → tick *Sign in with Apple* → register → download the `.p8`. It can be
   downloaded **once**; a lost key has to be replaced. Note the Key ID.
4. **Firebase** — Authentication → Sign-in method → Apple: the Services ID, Apple Team ID
   `83GQ2T4N4H`, the Key ID, and the private key. The same key is what lets Firebase **revoke Apple
   tokens**, which Apple requires of any app that lets a user delete their account.
5. **Xcode** — *+ Capability* → *Sign in with Apple*, which writes
   `com.apple.developer.applesignin` into the entitlements.

Steps 2 to 4 are not optional here: without them the button would work on iOS and fail on Android.

## License

To be defined.
