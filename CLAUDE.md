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
| **Group event type** | An event template of a group (name, description, optional start/end time). **No color** — each user colors it themselves. |
| **Group event** | Stored under the member who performs it (`{uid} == assigneeId`); has an `ownerId` (creator). Can be offered for swap. |
| **Personal event type** | A template a user defines for themselves (name, color, optional description/times). |
| **Personal event** | An instance of a personal event type on a date; belongs to no group. |
| **Swap offer** | The assignee offers their event; another member can take it. |
| **Transfer** | A member takes an event offered for swap; it moves to the new assignee and is logged (A→B). |
| **Shared calendar** | A user can grant another user full read access to their calendar (across groups). |
| **Change chain** | Ordered, append-only history of transfers over the same event. |

## Business rules

- An **admin** creates a group and defines the group's group event types.
- Each group has a **single invitation**. Anyone with the code can **request** to join; a group **admin must accept** the request.
- A user can put a group event **up for swap**; another member can take it (it moves to the taker).
- A user can **delete their own** event; an **admin** can delete any group event. Deleting removes it (there is no cancelled state).
- **Personal events** can carry notes (on the event); group event docs are shared with all members, so they hold no private notes.
- A user can define their own **personal event types** and add **personal events** (no group), each colored by its type.
- **Colors**: a group event type has no color; each user picks a color per group event type, shared by all their events of that type. Personal event types carry their own color.
- A user can belong to **several groups** and can invite another user to view **their entire calendar** (crossing groups).

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

- **Comments** — do **not** add a comment to every file, function or header. Comments belong only on **non-obvious, non-logic** code (a business rule, a workaround, a subtle invariant, a "why"). A comment that restates what the code already says is redundant — omit it.

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

Use [`Outcome`](core/src/commonMain/kotlin/com/georgevik/turnia/core/system/Outcome.kt) for anything that can fail with a **known** domain error.

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
  - **Cloud Scheduler** — triggers the periodic retention cleanup of old events.
- **GitLive Firebase Kotlin SDK** (`dev.gitlive:firebase-*`) — Firebase access from `commonMain`.
- **Native FCM per platform** — push reception uses the native SDK on each platform (iOS involves APNs, `AppDelegate` and permissions).

## Firestore data model

See [firebase/firestore-schema.md](firebase/firestore-schema.md) — the single source of truth for collections, fields, enums, access rules and invariants.

## Traceability

- The `history` is **append-only** and lives alongside the event under the current assignee
  (`groups/{groupId}/members/{uid}/event/{eventId}/history`).
- On a transfer the `takeEvent` Cloud Function moves the event to the new assignee and **copies the history
  forward**, so the current holder's subcollection always has the full chain.
- It records only the swap lifecycle: `put_on_swap` and `transferred` (with `fromUid`→`toUid`).
- Each entry points to `parentEventId`, so the full chain A→B→C can be reconstructed.
- **Taking an event offered for swap** runs in a `takeEvent` transaction that checks `onSwap == true` before moving it, to prevent double assignment.

## Permissions (Security Rules)

- **read** `.../event`: only members of the group.
- **create** event: the member for themselves — `{uid} == auth.uid` and `ownerId == assigneeId == auth.uid`.
- **update / delete** event: the assignee (`{uid}`) or an admin. (Taking is a cross-member move via `takeEvent`.)
- Group-wide reads are a collection-group query on `event` filtered by `groupId` and bounded to a **≤ 3-month `date` range**.
- Helpers: `isMember(g) = exists(members/uid)`; `isAdmin(g) = get(members/uid).role == 'admin'`.

## Sensitive points (do not overlook)

1. **No private fields on shared docs** — Firestore does not hide individual fields: if you can read the document, you read all of it. A group event doc is readable by every group member, so never put private data (notes, etc.) on it. `users/{uid}` is readable by everyone the user shares their calendar with, so it holds only the name and `calendarSharedWith`; email, FCM tokens and entitlement live in `users/{uid}/private/**`. Personal events are readable only by the owner and their shared users, so their `notes` may live on the doc.
2. **Viewing another user's full calendar (crosses groups)** — group events live under group members, so a user **outside** the group cannot read them directly. The cross-group shared calendar is served **on demand** by the `getSharedCalendar` Cloud Function. A grant has a **single source of truth**: A may read B only if `A ∈ users/B.calendarSharedWith`, a list only B writes. The function checks it, then aggregates the owner's group + personal events for a bounded date range (admin privileges, no stored copy).
3. **Joining a group is two steps via Cloud Functions** — `requestToJoinGroup` validates the code/expiration and creates a `joinRequests` doc; `acceptJoinRequest` (admin only) moves it to `members`. Do **not** let the client write directly to `members`.
4. **Taking / push are server-only** — `takeEvent` performs the cross-member move (verifying `onSwap` in a transaction) and copies history forward; **push** is sent only from Cloud Functions, never from the client.
5. **Premium entitlement is server-verified** — the client may *request* a purchase but must never mark itself premium. Only a Cloud Function that validated the store receipt (Play RTDN / App Store Server Notifications) writes `users/{uid}/private/subscription`; security rules forbid the client from setting it. Ad-hiding and premium gating must read the server-verified state, not a local flag.
6. **Retention is destructive & only server-side** — the scheduled cleanup Cloud Function is the *only* thing that bulk-deletes events older than the 1-month window (events + `history`); clients must not. Before purging, the data must already be in each user's local cache, or old events (and their traceability chain) are lost. Sync into the local NoSQL cache before, not after, relying on the purge.

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

## License

To be defined.
