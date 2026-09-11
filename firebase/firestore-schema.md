# Firestore schema — source of truth

This document is the **authoritative description of the Firestore data model** for Turnia.
Keep it in sync with the code and with [`firestore.rules`](./firestore.rules) — whenever the
database shape changes, update this file first.

## Location

The `(default)` database lives in **`europe-southwest1`** (Madrid), fixed when it was created and not
changeable afterwards — moving it would mean a new database and a migration.

Three things have to name that same region, and only one of them is optional:

| What | Where | Why |
|------|-------|-----|
| The database | — | Set at creation. Immutable. |
| `onEventPutOnSwap` | `setGlobalOptions` in [`functions/src/index.ts`](./functions/src/index.ts) | **Forced**: a Firestore trigger must be deployed in the database's region. |
| The callables | the same `setGlobalOptions` | A choice, but they read Firestore on every request. |

The **client has to name it too**, for the callables only: a v2 callable's URL contains its region
(`https://europe-southwest1-<project>.cloudfunctions.net/...`), so `Firebase.functions(region)` in
`CoreModule` is what keeps the app from calling `us-central1`, which is the SDK's default and where the
functions used to live. Firestore needs nothing: it always talks to `firestore.googleapis.com` and the
routing follows the project and database id, which is why its client API has no region parameter.

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
(`users/{uid}/private/preferences.groupEventTypeColors`), and all their events of that type share it.
`personalEventType` carries its own `color`.

A **group** does carry one (`groups/{groupId}.color`): unlike a type's, it is the admin's pick and
every member sees the same accent. A group saved before the field existed has none, and the client
derives its accent from the group id instead.

---

## `users/{uid}`

The **public** profile: readable by **any signed-in user**. That is the point — a search result has
to render for someone who has never shared anything with the searcher, and before this document was
public the only way to do that was to keep a second copy of the name on the reservation. There is
one copy now, here. Everything a stranger must not see lives under `private` (below).

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Display name. |
| `username` | string | Handle, `a-z0-9._`, 3-20 chars. Auto-generated from the name on sign-up (`jorgeg482`); the user can change it. Kept unique by the `usernames` collection. |
| `animalIconId` | string \| null | The `animal_icon_*` drawable suffix the user picked (`"duck"`). Absent until they pick one. |
| `backgroundColor` | string \| null | Hex behind the icon. Absent until they pick one. |
| `calendarSharedWith` | string[] | UIDs this user grants read access to **their** calendar. Written only by the owner. |
| `updateAt` | timestamp | Server timestamp of the last write. The marker on `usernames/{username}` is a copy of it; a reader compares the two to tell whether their cached profile is current. |

> **Neither half of the avatar is stored until the user picks it.** Both fields are absent on a
> profile that has never opened the picker, and the client renders the same default for all of them
> — the first colour of the palette behind a default animal. The default lives in the client, not in
> the document, so changing it re-skins every account that never picked instead of only the ones
> created afterwards.

> The avatar is written **straight from the client**, unlike the name. A rename goes through the
> `updateProfile` Cloud Function because the name is copied into every group the user belongs to and
> copies need a keeper; the avatar is copied nowhere, so there is nothing to chase. Both writes move
> `updateAt` here **and** on the reservation, in one commit.

> `calendarSharedWith` has to stay on this document: the security rules read it to authorize the
> very access it grants, and the "calendars shared with me" list is a query over it. Now that the
> document is public, so is the grant list — anyone signed in can see who you share your calendar
> with, and resolve those uids to names through this same collection. That is a deliberate trade,
> made when the profile was opened up: the alternative was a separate grant collection, two reads
> per row on the People screen, and a rule that does an `exists()` per event query.

### `users/{uid}/private/account`

Readable and writable **only by the owner**.

| Field | Type | Description |
|-------|------|-------------|
| `email` | string | Account email. |
| `fcmTokens` | string[] | FCM device tokens for push, one per device the account is signed in on. |
| `notificationsEnabled` | boolean | The in-app notification switch. Absent means enabled. |
| `updateAt` | timestamp | Server timestamp of the last write. What a reader compares the `private` sync marker against. |

> This document is read once per session — to decide whether the device should register for push — so
> it follows the same rule as the events: the cache answers, and the `private` field of
> `users/{uid}/sync/updates` says whether anything moved since. Every write here therefore commits the
> document and that marker **in the same batch** and stamps `updateAt`; a write that skipped either
> would leave the document looking permanently older than the marker and cost a server read on every
> launch from then on.
>
> `fcmTokens` is only ever written with `arrayUnion` / `arrayRemove`: a phone and a tablet signed into
> the same account both belong in it, and a write of the whole list would erase whichever device
> saved last. The client adds its token when a session starts and removes it on sign-out; the server
> drops the ones FCM reports as unregistered when it tries to send.
>
> `notificationsEnabled` is the switch in Settings, and it is per **account**, not per device: it says
> the user does not want to be interrupted, which is not a statement about which phone was in hand.
> Turning it off also takes that device's token out of `fcmTokens`, so nothing is sent rather than
> sent and discarded — the flag is what the client reads on the next launch to know not to register
> again. Nothing on the server reads it; an empty `fcmTokens` is already the whole story there.
> It is not the **system** permission either, which only the OS can answer for: a user who denied
> notifications to the app sees this switch on and still gets nothing.

### `users/{uid}/private/preferences`

What the user has chosen for themselves. Readable and writable **only by the owner**.

| Field | Type | Description |
|-------|------|-------------|
| `groupEventTypeColors` | map&lt;string,string&gt; | Color per group event type, keyed by `"{groupId}_{groupEventTypeId}"` → hex. |
| `updateAt` | timestamp | Server timestamp of the last write. What a reader compares the `preferences` marker against. |

> It has **its own marker**, `preferences` on `users/{uid}/sync/updates`, written in the same commit
> as the document. That is what lets every group screen read the colours from the cache instead of
> holding a listener on them: the sync document is already listened to, so the marker costs nothing,
> and only another of this user's devices moving it costs a read. An **unresolved** marker counts as
> settled — a pick made on this device is in the cache before the server acknowledges it, which is
> exactly when the cache is the most current thing there is, so the colour repaints immediately and
> offline.
>
> It could not share the old `private` marker: `account` is read once per session through that one,
> and a colour pick moving it would make `account` look stale and cost a server read of it on every
> launch. That is one of the two reasons the marker was split per document.

> It used to live on `users/{uid}`. It moved when that document became world-readable: what colour
> somebody paints their own shifts is nobody else's business. `getSharedCalendar` still renders with
> the owner's picks — it reads them here, with admin privileges.

### `users/{uid}/private/joinRequests`

The groups this user has asked to join and not yet seen an answer from. Written by
`requestToJoinGroup`; the owner removes an id once their app has shown the outcome.

| Field | Type | Description |
|-------|------|-------------|
| `groupIds` | string[] | Groups with a request from this user whose answer they have not acknowledged. |
| `updateAt` | timestamp | Server timestamp of the last write. What a reader compares the `private` sync marker against. |

> **This list is the only way a requester can find their own requests.** The rules let them read
> `groups/{groupId}/joinRequests/{uid}` by document id and by nothing else: a `collectionGroup` list
> cannot express "the document whose id is my uid", and the admin branch of the same rule needs a
> `get()` that a collection-group query cannot evaluate. So the pointer has to be stored, and it has
> to be stored where the requester can read it without belonging to the group.

> Because the requester reads this list through the `private` marker on `users/{uid}/sync/updates`,
> **every writer has to move that marker** — the Cloud Functions included. `requestToJoinGroup` and
> `deleteGroup` go through `writeJoinRequestPointer` for exactly that reason: it writes the id, the
> `updateAt` and the marker in one go, and a pointer written without the marker is one the requester
> never sees.

> Unlike `subscription` it stays under the owner-writable wildcard, because both sides write it: the
> server adds an id, the owner removes it. Nothing is at stake in the array — it holds ids to
> documents that are each guarded by their own rule — and `requestToJoinGroup` is still the only
> thing that can create the request an id points at.

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

**Access**: `users/{uid}` is readable by **any signed-in user** and writable only by the owner, who
may not change `name` or `username` — only `updateProfile` may. `users/{uid}/private/**` is readable
and writable only by the owner, except `private/subscription`, which the owner may read but never
write. `private/joinRequests` is written by both the owner and the server, and the wildcard covers
that: the server adds a pointer, the owner removes it.

**Listing the calendars shared with me** — the grant lives on the **granter's** document, so the list is a
query over `users` filtered by `calendarSharedWith array-contains {myUid}`, not a field of my own document.
`array-contains` is covered by the automatic single-field index. The query returns whole profiles, so
the names and avatars in that list arrive with it and cost no second read.

**A single source of truth for a grant** — A may read B's calendar if and only if `A ∈ users/B.calendarSharedWith`.
Only B writes it, on their own document; `getSharedCalendar` and the security rules both check that one list.
There is no mirrored list on the reader's side to drift out of sync with it.

---

## `usernames/{username}`

The reservation that makes a username unique, and the index a prefix search runs over. The
**document id is the username**; `username` repeats it as a field because a document id cannot be
prefix-queried.

| Field | Type | Description |
|-------|------|-------------|
| `username` | string | Same as the document id. |
| `uid` | string | The user who reserved it. |
| `updateAt` | timestamp | Server timestamp of the last profile write by that user. The marker a searcher compares their cached `users/{uid}` against. |

**It no longer carries the name.** It used to, because `users/{uid}` was unreadable to a stranger and
a search result had to render from something. Now the profile is public and the name lives in one
place; what the reservation carries instead is the marker below.

**Access**: readable by any signed-in user. `create` only when `uid == auth.uid` and the field
matches the document id; `delete` never — releasing a reservation is `updateProfile`'s alone.
`update` is allowed to the owning uid and **only for `updateAt`**: the two fields that make the
document a lock are frozen, and a client changing the marker cannot quietly take over a handle.

**Uniqueness** comes from Firestore itself: a write to a document that does not exist is a `create`,
and one to an existing document is an `update`. A second claimant therefore lands on `update`, where
the rule demands they already own it, and is denied. No transaction needed.

**Search** is a prefix query: `username >= q` and `username <= q + '\uf8ff'`, minimum 3 characters,
capped at 20 results. Firestore has **no substring or full-text search** — `jorge` finds
`jorgeg482`, but `geg` finds nothing. Matching the middle of a handle, or searching by display name,
needs either an n-gram field, a Cloud Function, or an external search index.

**The marker is what makes the second search free.** A hit gives the searcher a uid, and the profile
behind it is one more read. `updateAt` moves on every write to that user's profile — a rename
through `updateProfile`, an avatar written by the client — and it arrives inside the search query
the client was already paying for. So the profile read consults the cache first and only asks the
server when the cached copy's own `updateAt` is older. Searching the same prefix twice costs the
query and nothing more.

> The marker only pays where it arrives free. Resolving a **known list** of uids — the People
> screen's "shared by me", a group's members — reads `users/{uid}` directly: fetching a marker per
> uid would cost exactly the read it was meant to save. Those reads are cache-first without a
> marker, so a new avatar shows up there on the next forced refresh rather than instantly.

> A profile write and its marker must land in the **same commit**. Written apart, the marker is
> always the later of the two, every reader thinks their cache is behind, and no cache ever settles.

**Renaming** is claim-then-release: reserve the new document, point `users/{uid}.username` at it,
then delete the old one. A release that fails leaves a stale reservation, which only blocks that one
username.

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
| `color` | string \| null | The type's **default** colour, fixed when the type is created and never written again; what every member sees until they pick their own. `null` in types created before it existed, which fall back to a colour derived from the `id`. |

> A member may override it with their own in `users/{uid}/private/preferences.groupEventTypeColors`, keyed
> `"{groupId}_{typeId}"`, and theirs wins. The member's colour never goes on this document:
> `groups/{groupId}` is read by the whole group, and a personal preference has no business there.

**Access**: written by the owner; read by the owner and by UIDs in `calendarSharedWith`.

### `users/{uid}/personalEvents/{eventId}`

An instance of a personal event type on a date. Notes live **on the event**, not on the type.

| Field | Type | Description |
|-------|------|-------------|
| `personalEventTypeId` | string | References `personalEventTypes/{typeId}`. |
| `date` | string | `YYYY-MM-DD`. |
| `notes` | string \| null | Free-text notes for this event, written by the owner from the day sheet. Blank is stored as `null`. |

**Access**: written by the owner; read by the owner and by UIDs in `calendarSharedWith`.

### `users/{uid}/sync/updates`

A single document (`updates`) holding **when each part of the user's calendar last changed**. A reader —
the owner, or a user the calendar is shared with — reads this one small document and compares it against
what it already cached to decide whether it has to query the server at all.

| Field | Type | Description |
|-------|------|-------------|
| `personalEventsUpdatedAt` | timestamp \| null | Last write to `personalEvents` (server timestamp). |
| `personalEventTypesUpdatedAt` | timestamp \| null | Last write to `personalEventTypes` (server timestamp). |
| `revokedGroups` | timestamp \| null | Last write to `revokedGroups` — a revocation or a rejoin. Moved **only** by `leaveGroup` / `removeMember` / the rejoin path, never by a client. |
| `account` | timestamp \| null | Last write to `private/account`. |
| `joinRequests` | timestamp \| null | Last write to `private/joinRequests`, **or** to a `groups/{g}/joinRequests/{uid}` this user owns — answering a request changes no field of the pointer list, and this is the only thing that tells the requester to look again. |
| `preferences` | timestamp \| null | Last write to `private/preferences`. |
| `private` | timestamp \| null | **Legacy, read-only.** The single marker `account` and `joinRequests` used to share. |

Every timestamp is written with a **server timestamp**, so readers on other devices compare against the same
clock. A missing document (or field) means that part has never been written. Each writer merges **only its
own field**, so the timestamps never overwrite each other.

**One marker per document, not one per subcollection.** `account` and `joinRequests` shared a single
`private` field, so registering a push token invalidated the join-request cache and answering a request
invalidated the account cache — a needless server read each way. They are separate now, and clients read
`account ?? private` / `joinRequests ?? private` so an install that predates the split is not told its
cache is fine when it is not. `private` is never written again; drop the fallback once no account can
still be carrying only the old field.

`account` and `joinRequests` are read once per session — `account`, to decide whether the device should
register for push, and `joinRequests`, to show a requester the answer to their request. Both are written
from **both sides**: the owner's app and the Cloud Functions. So every writer has to move the matching
marker, the server included — `writeJoinRequestPointer` in `functions/src/users.ts` exists to make that
impossible to forget. A document written without moving its marker is one the reader never sees.

`preferences` is read differently: not once per session but continuously, by every group screen. Its
marker is what lets those reads come from the **cache** — a listener on the document itself would bill it
again on every re-attach, whereas the sync document is already listened to and the marker rides along for
free. The rule the reader follows is *cache unless the marker says otherwise*, and an **unresolved**
marker settles rather than forcing a read: a pick made on this device is in the cache before the server
acknowledges it, so the colour repaints straight away, offline included. Only another of the user's
devices leaves the cache genuinely behind, and only that costs a read.

`private/subscription` is covered by no marker: only the receipt-verification Cloud Function writes that
document, and gating it behind a marker the client also moves would keep an expired subscription looking
valid.

**Access**: written by the owner; read by the owner and by UIDs in `calendarSharedWith`.

### `users/{uid}/revokedGroups/{groupId}`

What is left of a group the user was removed from. They can no longer read `groups/{groupId}` — it holds
the member roster and the invitation code, and Firestore has no field-level access — so the little they
still need to render their leftover events is copied here, on a document only they can read.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | The group's name at the moment access was revoked. |
| `color` | string \| null | The group's accent at that same moment. |
| `groupEventTypes` | array&lt;map&gt; | **Only the types their remaining events actually use**, same shape as the group's own (`id`, `name`, `acronym`, `description`, `startTime`, `endTime`). |
| `revokedAt` | timestamp | When access was revoked. |
| `isDeleted` | boolean | Soft delete — set when they rejoin. |
| `updateAt` | timestamp | Last change, written in the same commit as `users/{uid}/sync/updates.revokedGroups`. |

**Deliberately a frozen snapshot, not a live copy.** Nothing keeps it in step with the group afterwards:
a departed member should not go on tracking a roster they left, and a type renamed after they went is not
a change they are entitled to see. It exists only so their own past shifts still render with a name.

A user with no leftover events gets no document here — and no `revokedUids` entry either. There is nothing
left of them in the group, so there is nothing to keep.

**Rejoining soft-deletes it**, like everywhere else: a removed document carries no timestamp and appears in
no `updateAt >` query, so a reader that already cached the snapshot would go on offering a group they belong
to again. `revokedUids` on the group is the existence test for the tombstone — the rejoin paths already hold
the group document, and a uid is in there exactly when the snapshot was written — so a plain join leaves
nothing behind.

**Queries**: read cache-first, then `updateAt >` the newest cached document, and only when
`users/{uid}/sync/updates.revokedGroups` is newer than that. Being revoked is rare; a live listener would
spend a read per document to learn nothing almost every time.

**Access**: read by the owner; **never written by any client** — only `leaveGroup` / `removeMember` create
it, and the rejoin path soft-deletes it.

---

## `groups/{groupId}`

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Group name. |
| `color` | string \| null | Accent color (`#RRGGBB`), picked by an admin and shared by every member. Null on groups saved before the field existed. |
| `memberUids` | string[] | Every member. The **single source of truth** for membership — written only by `acceptJoinRequest`, `leaveGroup` and `removeMember`. |
| `revokedUids` | string[] | Former members who still hold events here. They read **only the events assigned to them** and never the group document itself. A uid is in `memberUids` **xor** `revokedUids`, never both; someone who left with no events is in neither. Written only by `leaveGroup` / `removeMember`, and cleared when they rejoin. |
| `members` | map&lt;uid, {`name`, `username`}&gt; | Who those uids are — **`memberUids` only**; a revoked uid is dropped from here, so the calendar renders their leftover shifts without a name and the UI labels them as a former member. Denormalized on purpose: a calendar shows who covers each shift constantly, so the alternative is a read of `users/{uid}` per member every time it renders. Here it costs **no read at all** — it arrives with the group. The **avatar is deliberately not copied here**: it is shown on the member list and the join requests, not on every calendar row, so it is read from the profile on demand and a new one needs no fan-out. |
| `adminUids` | string[] | UIDs with admin role. |
| `groupEventTypes` | array&lt;map&gt; | Event type templates — see below. |
| `invitation` | map | The group's single invitation — see below. |

**`groupEventTypes[]`** — each element:

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Stable ID referenced by `event.groupEventTypeId`. |
| `name` | string | e.g. "Morning", "Night", "On-call". |
| `description` | string \| null | Optional details. |
| `startTime` | string \| null | `HH:mm` or `null`. |
| `endTime` | string \| null | `HH:mm` or `null`. |
| `color` | string \| null | The type's **default** colour, fixed when the type is created and never written again; what every member sees until they pick their own. `null` in types created before it existed, which fall back to a colour derived from the `id`. |

> A member may override it with their own in `users/{uid}/private/preferences.groupEventTypeColors`, keyed
> `"{groupId}_{typeId}"`, and theirs wins. The member's colour never goes on this document:
> `groups/{groupId}` is read by the whole group, and a personal preference has no business there.

| `updateAt` | timestamp \| null | Last change, written in the same commit as the group's sync marker. |

**`invitation`** map — **unique per group**. Anyone with the code can request access; a group admin must accept.

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | Invitation code embedded in the join link. The app shows it to admins only. **A house rule, not a boundary**: the whole group document is readable by every member, so a member who reads Firestore directly reads the code too (see *No private fields on shared docs*). |
| `active` | boolean | Whether the code can currently be used to request access. |
| `autoApprove` | boolean | `true`: whoever knows the code is added to the group by `requestToJoinGroup` on the spot. `false`: it lands in `joinRequests` and an admin decides. |
| `expiresAt` | timestamp \| null | Expiration; `null` = no expiry. |

**Access**: readable by the UIDs in `memberUids` — read straight off the document, with no lookup, which is
what lets **"my groups" be one query**: `memberUids array-contains {myUid}` returns exactly the documents the
rule allows, and each one already carries its event types. `create` by any signed-in user, who becomes the
sole member and admin; `update`/`delete` by admins, with `memberUids`, `adminUids` and `revokedUids`
immutable from the client — only `acceptJoinRequest` grows membership, with an `arrayUnion` so two admins
accepting at once do not overwrite each other, and only `leaveGroup` / `removeMember` withdraw it.
**`delete` is denied to every client**: removing this document does not cascade, so a client delete would
leave `events`, `sync` and `joinRequests` orphaned — unreachable, since every rule guarding them looks the
group up first, and still billed. The `deleteGroup` function is the only deleter, and it refuses until the
admin is the last member standing.

**A revoked user cannot read this document at all.** It carries the member roster and the invitation code,
and Firestore has no field-level access, so the alternative to locking them out is handing every removed
member a live feed of the group. What they need instead — the group's name and the event types their own
events use — is snapshotted for them in `users/{uid}/revokedGroups/{groupId}`.

### `groups/{groupId}/joinRequests/{uid}`

A request to join, created after validating the invitation code. Document ID is the requester's UID.

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Requester's display name, copied so an admin can render the request without reading `users/{uid}`, which they cannot. |
| `username` | string | Requester's username, copied for the same reason. |
| `groupName` | string | The group's name, copied for the mirror-image reason: a requester who was rejected never became a member, so `groups/{groupId}` is unreadable to them and there is no group to name. |
| `status` | string | `pending` \| `accepted` \| `rejected`. |
| `requestedAt` | timestamp | When the request was made. |
| `respondedAt` | timestamp \| null | When an admin answered; `null` while pending. |

**An answered request stays behind as a receipt.** Deleting it on acceptance would be the tidier
write, but it is the only thing the requester can read to learn what happened — they cannot query for
it, and a rejection sends no push — so it survives until their app acknowledges it and deletes both
the request and its pointer in `users/{uid}/private/joinRequests`. The cost is a small document per
unacknowledged outcome; `deleteGroup` sweeps them with the rest of the group.

`requestToJoinGroup` writes with `set` rather than `create`, so asking again after a rejection flips
that receipt back to `pending`.

**Queries**: an admin's approval list is `status == "pending"` — a single-field equality filter with no
`orderBy`, covered by the automatic index. Without it the answered receipts would show up as work.

**Access**: created only by `requestToJoinGroup` and answered only by `acceptJoinRequest` /
`rejectJoinRequest` — `status` is frozen against every client. Readable by admins and by the
requester; **deletable only by the requester**, to cancel a pending request or acknowledge an answered
one. An admin deleting one would answer it without leaving an answer behind.

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
`update` by the assignee or an admin, with `ownerId`, `assigneeId` and `history` immutable from the client —
and `onSwap` movable by the **assignee alone**, since offering a shift is not an admin's call. The type's
`swappable` flag is not checked by the rules (it sits inside the group's `groupEventTypes` array, which they
cannot search by id); the client enforces that one.
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
| `type` | string | `transferred` — the only value written. |
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
| `transferred` | A member takes the event; it moves to the new assignee. | `from` = previous assignee, `to` = new assignee |

Putting an event up for swap writes **no** history entry: `onSwap` already records it, and the array is
frozen against clients, so only a function could append one — a write per offer for nothing new.

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
- **Joining a group is two steps**: `requestToJoinGroup` then `acceptJoinRequest` / `rejectJoinRequest` (admin). The client never writes `memberUids`, and never answers a request either — `status` is server-only.
- **A requester finds their own requests through `users/{uid}/private/joinRequests`**, never a query: the rules authorize `groups/{g}/joinRequests/{uid}` by document id, which no collection-group list can filter on.
- **A group is deleted server-side, and only when empty**: `deleteGroup` (admin-only) refuses while anybody
  else is still a member, then `recursiveDelete`s the document with its subcollections and tombstones the
  snapshot of every revoked user who was still holding one. Firestore does not cascade, so a client delete
  would orphan them.
- **Taking an event offered for swap** is a `takeEvent` Cloud Function that verifies `onSwap == true` in a transaction and moves the event (a cross-member write).
- **Push** is sent only from Cloud Functions, never from the client. The visible text is written by the
  server — a notification has to render while the app is not running — and every message carries a
  `type` in its data payload so a tap can be routed:

  | `type` | Sent to | Tapping it opens |
  |--------|---------|------------------|
  | `join_requested` | the group's admins | that group (`groupId` travels with it) |
  | `join_accepted` | the requester | the Groups tab |
  | `calendar_shared` | whoever was granted access | the People tab |
  | `event_on_swap` | the other group members | the Swap tab |
  | `event_taken` | the member who offered it | the Swap tab |

  A `type` with no destination still opens the app; it just does not move it anywhere, which is also
  what an older client does with a `type` it has never heard of.
- **Colors**: `groupEventType` has no color (the user's `private/preferences.groupEventTypeColors` decides it); `personalEventType` and `group` carry their own — a group's is the admin's pick and is the same for every member.
- **Group-wide event queries are bounded to a ≤ 3-month `date` range** (collection-group on `event`, filtered by `groupId`).
- **Cross-group shared calendars** are served on demand by the `getSharedCalendar` Cloud Function
  (collection-group on `event` filtered by `assigneeId` + date range); nothing is mirrored.
- **A username is unique and reserved**: `usernames/{username}` holds it; claim the reservation *before*
  writing `users/{uid}.username`, and release the old one after.
- **A grant lives in one place**: `users/{owner}.calendarSharedWith`, written only by the owner. No mirrored list.
- **`users/{uid}` is public to every signed-in user** — the name, the handle, the avatar and the
  grant list; email, FCM tokens, entitlement and the user's own colour picks live under
  `users/{uid}/private/**`.
- **A profile write and its marker travel together**: any write to `users/{uid}` must also move
  `usernames/{username}.updateAt` in the **same commit**, or no reader's cache ever settles.
- **The name has a keeper, the avatar does not**: `name` and `username` are copied into every group,
  so only `updateProfile` may change them; the avatar is copied nowhere and the client writes it.
- **Sync timestamps are bumped on every personal write**: a write to `personalEvents` / `personalEventTypes` must also
  merge the matching field of `users/{uid}/sync/updates`, or readers keep serving a stale cache.
- **`subscription` is server-only**: only the subscription-verification Cloud Function writes `users/{uid}/private/subscription`; the client can never set itself premium.
- **Firestore holds only recent events**: events with `date` older than 1 month are purged by the scheduled cleanup function; older events live only in the client's local NoSQL cache. History is append-only *within the retention window*, not forever in Firebase.

See the domain overview in the root [`CLAUDE.md`](../CLAUDE.md) and the enforcement in [`firestore.rules`](./firestore.rules).
