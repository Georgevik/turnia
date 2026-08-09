# Firestore schema — source of truth

This document is the **authoritative description of the Firestore data model** for Turnia.
Keep it in sync with the code and with [`firestore.rules`](./firestore.rules) — whenever the
database shape changes, update this file first.

## Conventions

- **IDs**: `{uid}` is the Firebase Auth UID. `{groupId}`, `{eventId}`, `{typeId}`, `{historyId}` are
  document IDs (auto-generated unless noted).
- **Types**: Firestore native types — `string`, `number`, `boolean`, `timestamp`, `array`, `map`, `null`.
- **Dates**: a `date` is a date-only `string` in `YYYY-MM-DD`. Instants (`createdAt`, `joinedAt`,
  `requestedAt`, `timestamp`, `expiresAt`, `updatedAt`) are Firestore `timestamp`.
- **Times**: `startTime` / `endTime` are `string` in `HH:mm` (24h) and **nullable** (`null` = no fixed time / all-day).
- **Colors**: `string` hex `#RRGGBB`.
- Access is enforced server-side by [`firestore.rules`](./firestore.rules); the "Access" notes below summarize it.

## Events

Everything on a calendar is an **event** (there is no separate "shift" term). Two kinds:

- **Group event** — stored under the group member who currently performs it
  (`groups/{groupId}/members/{uid}/event/{eventId}`), typed by a `groupEventType` the admin defines.
  Can be put on sale and transferred between members (traceable). Single source of truth — there is no
  separate mirror.
- **Personal event** — stored under a single user, typed by a `personalEventType` the user defines. Not
  shareable/tradeable; just shown on the calendar.

## Colors

`groupEventType` has **no color**: each user picks the color per group event type
(`users/{uid}.groupEventTypeColors`), and all their events of that type share it.
`personalEventType` carries its own `color`.

---

## `users/{uid}`

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Display name. |
| `email` | string | Account email. |
| `fcmTokens` | string[] | FCM device tokens for push. |
| `calendarSharedWith` | string[] | UIDs this user grants read access to **their** calendar. Written only by the owner. |
| `calendarsSharedWithMe` | string[] | UIDs **whose** calendars this user may read. Written by those granters (each adds/removes only themselves). |
| `groupEventTypeColors` | map&lt;string,string&gt; | Color per group event type, keyed by `"{groupId}_{groupEventTypeId}"` → hex. |

**Access**: readable by the owner and by UIDs in `calendarSharedWith`. The whole doc is writable only by the owner,
**except** `calendarsSharedWithMe`, which another user may update by adding/removing **only their own uid**.

**Double check** — a user A may read B's calendar only if **both** hold: `B ∈ users/A.calendarsSharedWithMe`
**and** `A ∈ users/B.calendarSharedWith`. To grant, the owner B writes A into `users/B.calendarSharedWith`
(own doc) and adds B to `users/A.calendarsSharedWithMe` (a self-add into A's doc). `getSharedCalendar` enforces both.

### `users/{uid}/personalEventTypes/{typeId}`

Reusable personal event templates the user defines.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Type name. |
| `color` | string | Hex `#RRGGBB`. |
| `description` | string \| null | Optional details. |
| `startTime` | string \| null | `HH:mm` or `null`. |
| `endTime` | string \| null | `HH:mm` or `null`. |

**Access**: written by the owner; read by the owner and by UIDs in `calendarSharedWith`.

### `users/{uid}/personalEvents/{eventId}`

An instance of a personal event type on a date. Notes live **on the event**, not on the type.

| Field | Type | Description |
|-------|------|-------------|
| `personalEventTypeId` | string | References `personalEventTypes/{typeId}`. |
| `date` | string | `YYYY-MM-DD`. |
| `notes` | string \| null | Free-text notes for this event. |

**Access**: written by the owner; read by the owner and by UIDs in `calendarSharedWith`.

---

## `groups/{groupId}`

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Group name. |
| `adminUids` | string[] | UIDs with admin role (denormalized; source of truth for role is the `members` doc). |
| `groupEventTypes` | array&lt;map&gt; | Event type templates — see below. |
| `invitation` | map | The group's single invitation — see below. |

**`groupEventTypes[]`** — each element (no color):

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Stable ID referenced by `event.groupEventTypeId`. |
| `name` | string | e.g. "Morning", "Night", "On-call". |
| `description` | string \| null | Optional details. |
| `startTime` | string \| null | `HH:mm` or `null`. |
| `endTime` | string \| null | `HH:mm` or `null`. |

**`invitation`** map — **unique per group**. Anyone with the code can request access; a group admin must accept.

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | Invitation code embedded in the join link. |
| `active` | boolean | Whether the code can currently be used to request access. |
| `expiresAt` | timestamp \| null | Expiration; `null` = no expiry. |

**Access**: readable by members; `create` by any signed-in user (becomes admin); `update`/`delete` by admins.

### `groups/{groupId}/members/{uid}`

| Field | Type | Description |
|-------|------|-------------|
| `role` | string | `admin` \| `member`. |
| `joinedAt` | timestamp | When they joined. |

**Access**: readable by members; written by admins or by the `acceptJoinRequest` Cloud Function.

### `groups/{groupId}/joinRequests/{uid}`

A pending request to join, created after validating the invitation code. Document ID is the requester's UID.

| Field | Type | Description |
|-------|------|-------------|
| `requestedAt` | timestamp | When the request was made. |

**Access**: created only by `requestToJoinGroup`; readable by admins and by the requester; deletable by an admin (reject) or the requester (cancel).

### `groups/{groupId}/members/{uid}/event/{eventId}`

A group event, stored under the member who currently performs it (`{uid}` = `assigneeId`). This is the
single source of truth for group events. `ownerId` is the creator (immutable); `assigneeId` is the current
performer / last taker (equals the path `{uid}`). On a transfer the event moves to the new assignee's
subcollection.

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | string | Owning group (denormalized, for collection-group queries). |
| `ownerId` | string | Creator of the event (immutable). |
| `assigneeId` | string | Current performer / last taker (equals `{uid}`). |
| `groupEventTypeId` | string | References `groups/{groupId}.groupEventTypes[].id`. |
| `date` | string | `YYYY-MM-DD`. |
| `onSale` | boolean | `true` = offered for others to take. |
| `createdAt` | timestamp | Creation time. |

**Access**: readable by any member of the group; `create` by the member for themselves
(`{uid} == auth.uid` and `ownerId == assigneeId == auth.uid`); `update`/`delete` by the assignee (`{uid}`) or an admin.
Taking is a cross-member move done by the `takeEvent` Cloud Function (see below) — clients don't write another member's subcollection.

**Queries**
- All events of a group (all members): a **collection-group query** on `event` filtered by `groupId ==` and a
  **`date` range of at most 3 months**.
- A user's events in a group: read the subcollection `groups/{groupId}/members/{uid}/event` directly.

### `groups/{groupId}/members/{uid}/event/{eventId}/history/{historyId}`

**Append-only** log of the event's tradeable lifecycle (put on sale, transfers), stored alongside the event
under the current assignee. On a transfer the `takeEvent` function copies it forward to the new assignee, so
the current holder always has the full chain. Each entry links to the previous via `parentEventId`.

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | `put_on_sale` \| `transferred`. |
| `actorUid` | string | Who performed the action. |
| `fromUid` | string \| null | Previous assignee (for `transferred`). |
| `toUid` | string \| null | New assignee (for `transferred`). |
| `timestamp` | timestamp | When it happened. |
| `parentEventId` | string \| null | Previous history entry on this event; `null` for the first. |

**Access**: readable by members; `create` by the assignee (`{uid}`) or the `takeEvent` function; **`update`/`delete` always denied** (append-only).

---

## Enumerations

### On sale

An event is either on sale or not — no multi-value state and no "deleted" state (a deleted event is removed).
Represented by the boolean `event.onSale`.

- `onSale: false → true` — the current assignee offers the event.
- `onSale: true → false` — the offer is withdrawn, or a member takes it (moving the event to the new assignee).

### History event type

`history.type`:

| Value | Emitted when | `fromUid` / `toUid` |
|-------|--------------|---------------------|
| `put_on_sale` | The assignee puts the event on sale. | — |
| `transferred` | A member takes the event; it moves to the new assignee. | `from` = previous assignee, `to` = new assignee |

### Member role

`member.role`: `admin` | `member`.

---

## Invariants (do not break)

- **A group event lives in exactly one place**: `groups/{groupId}/members/{assigneeId}/event/{eventId}` — no mirror.
- **History is append-only** and lives alongside the event; on transfer `takeEvent` copies it forward to the new assignee.
- **A deleted event is deleted** — there is no cancelled/deleted state.
- **Group event docs are readable by every group member** — never put private data (e.g. notes) on them.
  Personal events are private to the owner and their shared users, so their `notes` live on the event doc.
- **Joining a group is two steps**: `requestToJoinGroup` then `acceptJoinRequest` (admin). The client never writes `members` on join.
- **Taking an on-sale event** is a `takeEvent` Cloud Function that verifies `onSale == true` in a transaction and moves the event (a cross-member write).
- **Push** is sent only from Cloud Functions, never from the client.
- **Colors**: `groupEventType` has no color (user's `groupEventTypeColors` decides it); `personalEventType` carries its own.
- **Group-wide event queries are bounded to a ≤ 3-month `date` range** (collection-group on `event`, filtered by `groupId`).
- **Cross-group shared calendars** are served on demand by the `getSharedCalendar` Cloud Function
  (collection-group on `event` filtered by `assigneeId` + date range); nothing is mirrored.
- **Calendar sharing needs both sides**: `B ∈ users/A.calendarsSharedWithMe` and `A ∈ users/B.calendarSharedWith`.

See the domain overview in the root [`CLAUDE.md`](../CLAUDE.md) and the enforcement in [`firestore.rules`](./firestore.rules).
