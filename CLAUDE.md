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

## Domain concepts

Everything on a calendar is an **event** (there is no separate "shift" term).

| Concept | Description |
|---------|-------------|
| **User** | Healthcare professional (nurse/doctor); can belong to several groups. |
| **Group** | A team an admin creates; defines its own group event types. Members see the group's events. |
| **Group event type** | An event template of a group (name, description, optional start/end time). **No color** — each user colors it themselves. |
| **Group event** | Stored under the member who performs it (`{uid} == assigneeId`); has an `ownerId` (creator). Can be on sale. |
| **Personal event type** | A template a user defines for themselves (name, color, optional description/times). |
| **Personal event** | An instance of a personal event type on a date; belongs to no group. |
| **On sale** | The assignee offers their event; another member can take it. |
| **Transfer** | A member takes an on-sale event; it moves to the new assignee and is logged (A→B). |
| **Shared calendar** | A user can grant another user full read access to their calendar (across groups). |
| **Change chain** | Ordered, append-only history of transfers over the same event. |

## Business rules

- An **admin** creates a group and defines the group's group event types.
- Each group has a **single invitation**. Anyone with the code can **request** to join; a group **admin must accept** the request.
- A user can put a group event **on sale**; another member can take it (it moves to the taker).
- A user can **delete their own** event; an **admin** can delete any group event. Deleting removes it (there is no cancelled state).
- **Personal events** can carry notes (on the event); group event docs are shared with all members, so they hold no private notes.
- A user can define their own **personal event types** and add **personal events** (no group), each colored by its type.
- **Colors**: a group event type has no color; each user picks a color per group event type, shared by all their events of that type. Personal event types carry their own color.
- A user can belong to **several groups** and can invite another user to view **their entire calendar** (crossing groups).

## Tech stack

- **Kotlin Multiplatform (KMP)** — shared business logic.
- **Compose Multiplatform** — shared UI (Android / iOS).
- **Coroutines + Flow** — asynchrony and reactive state.
- **Koin** — dependency injection (DI).
- **Navigation 3 (Nav3)** — shared, back-stack-based navigation.
- **kotlinx.serialization** — serialization (navigation keys, DTOs).
- **Firebase** — backend, no custom server:
  - **Firestore** — data.
  - **Firebase Auth** — authentication.
  - **Cloud Functions (TypeScript)** — join requests, taking events, push, shared-calendar aggregation.
  - **FCM** — push notifications.
- **GitLive Firebase Kotlin SDK** (`dev.gitlive:firebase-*`) — Firebase access from `commonMain`.
- **Native FCM per platform** — push reception uses the native SDK on each platform (iOS involves APNs, `AppDelegate` and permissions).

## Firestore data model

See [firebase/firestore-schema.md](firebase/firestore-schema.md) — the single source of truth for collections, fields, enums, access rules and invariants.

## Traceability

- The `history` is **append-only** and lives alongside the event under the current assignee
  (`groups/{groupId}/members/{uid}/event/{eventId}/history`).
- On a transfer the `takeEvent` Cloud Function moves the event to the new assignee and **copies the history
  forward**, so the current holder's subcollection always has the full chain.
- It records only the tradeable lifecycle: `put_on_sale` and `transferred` (with `fromUid`→`toUid`).
- Each entry points to `parentEventId`, so the full chain A→B→C can be reconstructed.
- **Taking an on-sale event** runs in a `takeEvent` transaction that checks `onSale == true` before moving it, to prevent double assignment.

## Permissions (Security Rules)

- **read** `.../event`: only members of the group.
- **create** event: the member for themselves — `{uid} == auth.uid` and `ownerId == assigneeId == auth.uid`.
- **update / delete** event: the assignee (`{uid}`) or an admin. (Taking is a cross-member move via `takeEvent`.)
- Group-wide reads are a collection-group query on `event` filtered by `groupId` and bounded to a **≤ 3-month `date` range**.
- Helpers: `isMember(g) = exists(members/uid)`; `isAdmin(g) = get(members/uid).role == 'admin'`.

## Sensitive points (do not overlook)

1. **No private fields on shared docs** — Firestore does not hide individual fields: if you can read the document, you read all of it. A group event doc is readable by every group member, so never put private data (notes, etc.) on it. Personal events are readable only by the owner and their shared users, so their `notes` may live on the doc.
2. **Viewing another user's full calendar (crosses groups)** — group events live under group members, so a user **outside** the group cannot read them directly. The cross-group shared calendar is served **on demand** by the `getSharedCalendar` Cloud Function. Sharing is **double-verified**: A may read B only if `B ∈ users/A.calendarsSharedWithMe` **and** `A ∈ users/B.calendarSharedWith`. The function checks both, then aggregates the owner's group + personal events for a bounded date range (admin privileges, no stored copy).
3. **Joining a group is two steps via Cloud Functions** — `requestToJoinGroup` validates the code/expiration and creates a `joinRequests` doc; `acceptJoinRequest` (admin only) moves it to `members`. Do **not** let the client write directly to `members`.
4. **Taking / push are server-only** — `takeEvent` performs the cross-member move (verifying `onSale` in a transaction) and copies history forward; **push** is sent only from Cloud Functions, never from the client.

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
    └── functions/      # Cloud Functions (TypeScript): join requests, taking events, push
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
