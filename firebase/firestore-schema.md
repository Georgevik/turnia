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
  (`groups/{groupId}/events/{eventId}`), typed by a `groupEventType` the admin defines.
  Can be put up for swap and transferred between members (traceable). Single source of truth — there is no
  separate mirror.
- **Personal event** — stored under a single user, typed by a `personalEventType` the user defines. Not
  shareable/swappable; just shown on the calendar.

## Colors

`groupEventType` has **no color**: each user picks the color per group event type
(`users/{uid}.groupEventTypeColors`), and all their events of that type share it.
`personalEventType` carries its own `color`.

---

## `users/{uid}`

The **public** profile: every user this one shares their calendar with can read this whole document,
so it carries nothing but the name and the grant list. Everything else lives under `private` (below).

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Display name. |
| `username` | string | Handle, `a-z0-9._`, 3-20 chars. Auto-generated from the name on sign-up (`jorgeg482`); the user can change it. Kept unique by the `usernames` collection. |
| `calendarSharedWith` | string[] | UIDs this user grants read access to **their** calendar. Written only by the owner. |
| `groupEventTypeColors` | map&lt;string,string&gt; | Color per group event type, keyed by `"{groupId}_{groupEventTypeId}"` → hex. |

> `calendarSharedWith` has to stay on the public document: the security rules read it to authorize the
> very access it grants, and the "calendars shared with me" list is a query over it. The cost is that a
> user you share with can see who else you share with.

### `users/{uid}/private/account`

Readable and writable **only by the owner**.

| Field | Type | Description |
|-------|------|-------------|
| `email` | string | Account email. |
| `fcmTokens` | string[] | FCM device tokens for push. |

### `users/{uid}/private/subscription`

Premium entitlement. Readable only by the owner; **the client can never write it** — the document is
created and updated solely by the subscription-verification Cloud Function after validating a store
receipt. An absent document means the free tier.

| Field | Type | Description |
|-------|------|-------------|

| Field | Type | Description |
|-------|------|-------------|
| `tier` | string | `free` \| `premium`. |
| `plan` | string \| null | `monthly` ([redacted]) \| `annual` ([redacted]); `null` when free. |
| `platform` | string \| null | `play` \| `appstore` — where it was purchased. |
| `expiresAt` | timestamp \| null | Current period end; entitlement is active while now &lt; `expiresAt`. |
| `updatedAt` | timestamp | Last time the server updated this from a store notification. |

**Access**: `users/{uid}` is readable by the owner and by UIDs in `calendarSharedWith`, and writable only by
the owner. `users/{uid}/private/**` is readable and writable only by the owner, except
`private/subscription`, which the owner may read but never write.

**Listing the calendars shared with me** — the grant lives on the **granter's** document, so the list is a
query over `users` filtered by `calendarSharedWith array-contains {myUid}`, not a field of my own document.
`array-contains` is covered by the automatic single-field index, and the rule on `users/{uid}` allows exactly
the documents that query returns, so nothing else is readable through it.

**A single source of truth for a grant** — A may read B's calendar if and only if `A ∈ users/B.calendarSharedWith`.
Only B writes it, on their own document; `getSharedCalendar` and the security rules both check that one list.
There is no mirrored list on the reader's side to drift out of sync with it.

---

## `usernames/{username}`

The reservation that makes a username unique, and the only way to find a user you cannot read yet.
The **document id is the username**; `username` repeats it as a field because a document id cannot be
prefix-queried.

| Field | Type | Description |
|-------|------|-------------|
| `username` | string | Same as the document id. |
| `uid` | string | The user who reserved it. |
| `name` | string | Display name, so a search result can be rendered without reading `users/{uid}`. |

**Access**: readable by any signed-in user — that is the point, since `users/{uid}` is not. `create`
only when `uid == auth.uid`; `update` and `delete` only by the uid already in the document.

**Uniqueness** comes from Firestore itself: a write to a document that does not exist is a `create`, and
one to an existing document is an `update`. A second claimant therefore lands on `update`, where the rule
demands they already own it, and is denied. No transaction needed.

**Search** is a prefix query: `username >= q` and `username <= q + '\uf8ff'`, minimum 3 characters, capped
at 20 results. Firestore has **no substring or full-text search** — `jorge` finds `jorgeg482`, but `geg`
finds nothing. Matching the middle of a handle, or searching by display name, needs either an n-gram field,
a Cloud Function, or an external search index.

**Resolving a uid** — `usernames` is also queried by `uid in [...]`, because `users/{uid}` is unreadable
unless that person shares their calendar back: it is the only way to put a name to someone *you* granted
access to. `uid` is covered by the automatic single-field index.

**Renaming** is claim-then-release: reserve the new document, point `users/{uid}.username` at it, then
delete the old one. A release that fails leaves a stale reservation, which only blocks that one username.

---

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

### `users/{uid}/sync/updates`

A single document (`updates`) holding **when each part of the user's calendar last changed**. A reader —
the owner, or a user the calendar is shared with — reads this one small document and compares it against
what it already cached to decide whether it has to query the server at all.

| Field | Type | Description |
|-------|------|-------------|
| `personalEventsUpdatedAt` | timestamp \| null | Last write to `personalEvents` (server timestamp). |
| `personalEventTypesUpdatedAt` | timestamp \| null | Last write to `personalEventTypes` (server timestamp). |

Every timestamp is written with a **server timestamp**, so readers on other devices compare against the same
clock. A missing document (or field) means that part has never been written. Each writer merges **only its
own field**, so the two timestamps never overwrite each other.

**Access**: written by the owner; read by the owner and by UIDs in `calendarSharedWith`.

### `users/{uid}/revokedGroups/{groupId}`

What is left of a group the user was removed from. They can no longer read `groups/{groupId}` — it holds
the member roster and the invitation code, and Firestore has no field-level access — so the little they
still need to render their leftover events is copied here, on a document only they can read.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | The group's name at the moment access was revoked. |
| `groupEventTypes` | array&lt;map&gt; | **Only the types their remaining events actually use**, same shape as the group's own (`id`, `name`, `acronym`, `description`, `startTime`, `endTime`). |
| `revokedAt` | timestamp | When access was revoked. |

**Deliberately a frozen snapshot, not a live copy.** Nothing keeps it in step with the group afterwards:
a departed member should not go on tracking a roster they left, and a type renamed after they went is not
a change they are entitled to see. It exists only so their own past shifts still render with a name.

A user with no leftover events gets no document here — and no `revokedUids` entry either. There is nothing
left of them in the group, so there is nothing to keep.

**Access**: read by the owner; **never written by any client** — only `leaveGroup` / `removeMember` create
it, and the rejoin path deletes it.

---

## `groups/{groupId}`

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Group name. |
| `memberUids` | string[] | Every member. The **single source of truth** for membership — written only by `acceptJoinRequest`, `leaveGroup` and `removeMember`. |
| `revokedUids` | string[] | Former members who still hold events here. They read **only the events assigned to them** and never the group document itself. A uid is in `memberUids` **xor** `revokedUids`, never both; someone who left with no events is in neither. Written only by `leaveGroup` / `removeMember`, and cleared when they rejoin. |
| `members` | map&lt;uid, {`name`, `username`}&gt; | Who those uids are — **`memberUids` only**; a revoked uid is dropped from here, so the calendar renders their leftover shifts without a name and the UI labels them as a former member. Denormalized on purpose: a calendar shows who covers each shift constantly, and `users/{uid}` is unreadable between group mates, so the alternative is a query against `usernames` every time. Here it costs **no read at all** — it arrives with the group. |
| `adminUids` | string[] | UIDs with admin role. |
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

| `updateAt` | timestamp \| null | Last change, written in the same commit as the group's sync marker. |

**`invitation`** map — **unique per group**. Anyone with the code can request access; a group admin must accept.

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | Invitation code embedded in the join link. |
| `active` | boolean | Whether the code can currently be used to request access. |
| `autoApprove` | boolean | `true`: whoever knows the code is added to the group by `requestToJoinGroup` on the spot. `false`: it lands in `joinRequests` and an admin decides. |
| `membersCanSeeCode` | boolean | Whether the app shows the code to plain members. **A house rule, not a boundary**: the whole group document is readable by every member, so a member who reads Firestore directly reads the code too (see *No private fields on shared docs*). |
| `expiresAt` | timestamp \| null | Expiration; `null` = no expiry. |

**Access**: readable by the UIDs in `memberUids` — read straight off the document, with no lookup, which is
what lets **"my groups" be one query**: `memberUids array-contains {myUid}` returns exactly the documents the
rule allows, and each one already carries its event types. `create` by any signed-in user, who becomes the
sole member and admin; `update`/`delete` by admins, with `memberUids`, `adminUids` and `revokedUids`
immutable from the client — only `acceptJoinRequest` grows membership, with an `arrayUnion` so two admins
accepting at once do not overwrite each other, and only `leaveGroup` / `removeMember` withdraw it.

**A revoked user cannot read this document at all.** It carries the member roster and the invitation code,
and Firestore has no field-level access, so the alternative to locking them out is handing every removed
member a live feed of the group. What they need instead — the group's name and the event types their own
events use — is snapshotted for them in `users/{uid}/revokedGroups/{groupId}`.

### `groups/{groupId}/joinRequests/{uid}`

A pending request to join, created after validating the invitation code. Document ID is the requester's UID.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Requester's display name, copied so an admin can render the request without reading `users/{uid}`, which they cannot. |
| `username` | string | Requester's username, copied for the same reason. |
| `requestedAt` | timestamp | When the request was made. |

**Access**: created only by `requestToJoinGroup`; readable by admins and by the requester; deletable by an admin (reject) or the requester (cancel).

### `groups/{groupId}/events/{eventId}`

A group event, in **one collection per group** rather than one per member. `ownerId` is the creator
(immutable); `assigneeId` is the current performer, and a transfer changes that field instead of moving
the document.

| Field | Type | Description |
|-------|------|-------------|
| `ownerId` | string | Creator of the event (immutable). |
| `assigneeId` | string | Current performer / last taker. |
| `groupEventTypeId` | string | References `groups/{groupId}.groupEventTypes[].id`. |
| `date` | string | `YYYY-MM-DD`. |
| `yearMonth` | string | `YYYY-MM`, the partition the sync markers and the queries use. |
| `onSwap` | boolean | `true` = offered for others to take. |
| `isDeleted` | boolean | Soft delete — see below. |
| `updateAt` | timestamp \| null | Last change, written in the same commit as the month's sync marker. |
| `history` | array&lt;map&gt; | The swap chain, in order — see below. |

**Access**: readable by any member, and by a **revoked** user for the events where
`assigneeId == their uid` — on a `list` that term is what forces their query to carry the matching
`assigneeId ==` filter, so the constraint is the rule rather than the client's good manners.
`create` by the member for themselves (`ownerId == assigneeId == auth.uid`) and never by a revoked user;
`update` by the assignee or an admin, with `ownerId`, `assigneeId` and `history` immutable from the client.
**`delete` only by the creator while they still hold it** (`ownerId == assigneeId == auth.uid`) — once a shift
has been handed to someone else it is theirs to cover, and giving it back means putting it up for swap, not
deleting it. Taking is done by
the `takeEvent` Cloud Function, the only writer that changes `assigneeId` or appends to `history`.
A revoked user may still soft-delete an event they created *and* still hold; because deletes are soft
(below) that arrives as an `update`, and their branch of the rule is narrowed to the `isDeleted` and
`updateAt` keys alone.

**Deletes are soft.** A removed document is invisible to a "what changed since" query, so nothing would
carry a newer timestamp for the other members to notice the event is gone. The scheduled retention
cleanup is what removes them for real.

**Queries**: a group's calendar is one query over this collection — `yearMonth` in the visible months,
each month optionally bounded by `updateAt >` its own cursor. No collection-group query and no `groupId`
denormalisation.

### `groups/{groupId}/sync/updates`

When each month of the group's calendar last changed, so a member can tell whether their cache is behind
before reading any event.

| Field | Type | Description |
|-------|------|-------------|
| `events` | map&lt;`YYYY-MM`, {`updatedAt`: timestamp}&gt; | Per month, when it last changed. |
| `group` | timestamp \| null | When the group document last changed — its name, its invitation and above all its event types. |

One document answers both questions a calendar asks on opening: *have the types changed?* and *which
months have?* The read is debounced, so opening a group costs **one** read when nothing moved.

**Access**: read and write by any member — any member's event write moves the month every member reads —
and by a revoked user, who reads it to tell whether their cache is behind and writes it in the same batch
as a soft delete. The document holds nothing but timestamps.

A member joining, leaving or renaming themselves moves `group`, since that is the document their name
lives on.

**Written in the same commit as the event.** A batch resolves every server timestamp in it to a single
commit time, so a reader sees the event and its marker as equally old. Written apart, the marker is always
the later of the two and no cache ever looks current.

**`history[]`** — the swap chain, on the event and not in a subcollection. Documents of a subcollection are
never returned by a query on their parent, so rendering "who held this before" would cost a read per event;
here it travels with the event for free. Each entry:

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | `put_on_swap` \| `transferred`. |
| `actorUid` | string | Who performed the action. |
| `fromUid` | string \| null | Previous assignee (for `transferred`). |
| `toUid` | string \| null | New assignee (for `transferred`). |
| `timestamp` | timestamp | When it happened — a real timestamp, since Firestore forbids the server-timestamp sentinel inside an array. |

**Append-only, and more firmly than before**: the rules freeze the whole field for clients, so only
`takeEvent` can add to it. The subcollection it replaces let any member create entries.

---

## Enumerations

### Offered for swap

An event is either offered for swap or not — no multi-value state and no "deleted" state (a deleted event is removed).
Represented by the boolean `event.onSwap`.

- `onSwap: false → true` — the current assignee offers the event.
- `onSwap: true → false` — the offer is withdrawn, or a member takes it (moving the event to the new assignee).

### History event type

`history.type`:

| Value | Emitted when | `fromUid` / `toUid` |
|-------|--------------|---------------------|
| `put_on_swap` | The assignee puts the event up for swap. | — |
| `transferred` | A member takes the event; it moves to the new assignee. | `from` = previous assignee, `to` = new assignee |

### Member role

`member.role`: `admin` | `member`.

### Subscription tier

`users/{uid}.subscription.tier`: `free` | `premium`. `plan`: `monthly` | `annual` | `null`. Premium is active
while `now < subscription.expiresAt`. Free-tier users are shown AdMob ads; premium users are not.

---

## Data retention

Firestore keeps only a **recent window** of events; older events are purged and preserved in each user's on-device cache.

- **Purge threshold**: any event with `date` older than **1 month** (relative to the cleanup run) is eligible for deletion.
- **What is deleted**: matching group events and personal events **and their `history` subcollection**.
- **Who deletes**: a **Cloud Scheduler**-triggered Cloud Function (admin privileges). Clients never bulk-delete past events.
- **Where old data survives**: the client's **local NoSQL cache** (normalized), populated as events are synced from Firestore.
  Once purged from Firestore, old events (and their A→B→C chain) exist only in that local cache.

---

## Invariants (do not break)

- **A group event lives in exactly one place**: `groups/{groupId}/events/{eventId}` — one collection per group,
  no mirror, and a transfer changes `assigneeId` instead of moving the document.
- **Membership lives on the group document** (`memberUids`), so a group's calendar and its event types are
  reachable in one query and the rules need no lookup to authorize a member.
- **A uid is in `memberUids` xor `revokedUids`, never both**, and `members` mirrors `memberUids` alone.
  Someone removed with no events left behind is in neither list.
- **A revoked user reads only events where `assigneeId` is their own uid, and never the group document.**
  Their event types come from the `users/{uid}/revokedGroups/{groupId}` snapshot, not from the group.
- **Withdrawing membership is a Cloud Function**: `leaveGroup` (yourself) or `removeMember` (an admin).
  The client never writes `memberUids`, `members` or `revokedUids`.
- **History is append-only** and lives alongside the event; on transfer `takeEvent` copies it forward to the new assignee.
- **A deleted event is deleted** — there is no cancelled/deleted state.
- **Group event docs are readable by every group member** — never put private data (e.g. notes) on them.
  Personal events are private to the owner and their shared users, so their `notes` live on the event doc.
- **Joining a group is two steps**: `requestToJoinGroup` then `acceptJoinRequest` (admin). The client never writes `memberUids`.
- **Taking an event offered for swap** is a `takeEvent` Cloud Function that verifies `onSwap == true` in a transaction and moves the event (a cross-member write).
- **Push** is sent only from Cloud Functions, never from the client.
- **Colors**: `groupEventType` has no color (user's `groupEventTypeColors` decides it); `personalEventType` carries its own.
- **Group-wide event queries are bounded to a ≤ 3-month `date` range** (collection-group on `event`, filtered by `groupId`).
- **Cross-group shared calendars** are served on demand by the `getSharedCalendar` Cloud Function
  (collection-group on `event` filtered by `assigneeId` + date range); nothing is mirrored.
- **A username is unique and reserved**: `usernames/{username}` holds it; claim the reservation *before*
  writing `users/{uid}.username`, and release the old one after.
- **A grant lives in one place**: `users/{owner}.calendarSharedWith`, written only by the owner. No mirrored list.
- **`users/{uid}` is public to everyone you share with** — the name and the grant list, nothing else; email,
  FCM tokens and entitlement live under `users/{uid}/private/**`.
- **Sync timestamps are bumped on every personal write**: a write to `personalEvents` / `personalEventTypes` must also
  merge the matching field of `users/{uid}/sync/updates`, or readers keep serving a stale cache.
- **`subscription` is server-only**: only the subscription-verification Cloud Function writes `users/{uid}/private/subscription`; the client can never set itself premium.
- **Firestore holds only recent events**: events with `date` older than 1 month are purged by the scheduled cleanup function; older events live only in the client's local NoSQL cache. History is append-only *within the retention window*, not forever in Firebase.

See the domain overview in the root [`CLAUDE.md`](../CLAUDE.md) and the enforcement in [`firestore.rules`](./firestore.rules).
