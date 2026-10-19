# Proposal

## Why

An event added with no connection does not appear on the calendar. The write itself is already
offline-first — Firestore's persistent cache holds it and uploads it on reconnect — but the calendar
never looks at the cache again after the first moment it subscribes, so the user is left with no
evidence their shift was added until they are back online.

The same reader has a second, worse failure: while offline, a month the cache is genuinely behind on
sends the reader to the server, the server read fails because there is no connection, and the whole
flow collapses to an empty list. One unsynced month blanks every event in the open window.

## What Changes

The event readers keep their marker-gated design — the cache is still read, the server is still only
asked when a marker proves the cache is behind — with two corrections to how they treat the moments
when the server cannot answer:

- **An unconfirmed marker sends the reader back to the cache, not to the server.** Today a marker
  emission that is unconfirmed (it carries this device's unsent writes, or it is answered from the
  cache because the device is offline) produces no new emission at all. It will instead re-read
  `Source.CACHE` for the open months and emit the merged result, which is where the local write
  already is. This is the rule `users/{uid}/private/preferences` already follows, and the reason a
  colour pick repaints offline while an added shift does not.
- **A server read that fails keeps what is known instead of emptying the calendar.** The reader falls
  back to the events it already holds rather than terminating the flow into `emptyList()`.

Scope of the readers touched: `PersonalEventFirestore`, `GroupEventFirestore`,
`PersonalOneOffEventFirestore`, `GroupEventExtrasFirestore` and `PersonalEventTypesFirestore` get
both corrections; `RevokedGroupFirestore` gets only the second, because nothing a client writes
reaches `users/{uid}/revokedGroups` — only the membership Cloud Functions do.

Non-goals, deliberately:

- **An event awaiting upload is indistinguishable from one the server has taken.** No badge, no
  greyed row, no "pending" state. The optimistic pattern is the whole point: the shift is on the
  calendar, and Firestore uploads it when the connection returns.
- **No optimistic layer in the repositories.** No second source of truth beside the cache, and no
  reconciliation of one against the other.
- **No live `.snapshots` listener on an event collection.** That would be instant for free on local
  writes but is billed its whole result set on every attach past 30 minutes, which is exactly the
  cost the marker design exists to avoid (see `firebase/firestore-usage.md`).
- **Writes keep resolving on the server's acknowledgement.** `batch.commit()` still suspends until
  the server commits, so analytics for an offline add still lands on reconnect, as
  `openspec/changes/archive/2026-09-27-add-analytics-events/design.md` already records.
- **Moving a personal event to a group stays server-dependent.** It reads `Source.SERVER` by design
  and must keep failing offline.

## Capabilities

### New Capabilities

- `offline-event-visibility`: what the calendar shows while the device cannot reach the server —
  an event added offline appears at once and is indistinguishable from a synced one, events already
  held are never replaced by an empty calendar, and the server is asked only when it can answer.

### Modified Capabilities

<!-- None. No existing requirement becomes false: the specs that mention offline behaviour
     (`shared-calendar`, `shift-setup`, `move-personal-to-group`, `group-event-notes`) describe
     paths this change does not touch — a Cloud Function call, a deliberate `Source.SERVER` read,
     or cross-device sync, all of which keep behaving as specified. -->

## Impact

**Code** — six datasources in
`core/src/commonMain/kotlin/com/geoviksoft/turnia/core/data/datasource/firestore/`:
`PersonalEventFirestore`, `GroupEventFirestore`, `PersonalOneOffEventFirestore`,
`GroupEventExtrasFirestore`, `PersonalEventTypesFirestore`, `RevokedGroupFirestore`. All six share
one copied flow shape, so the change is the same small edit repeated, not one abstraction.

`UserSyncFirestore.observeWithPendingWrites` and `GroupSyncFirestore` are the sources the readers
switch to; `observeWithPendingWrites` already exists and is already used by the preferences reader.
`GroupSyncFirestore` exposes no such method yet and needs one.

**Firestore cost** — unchanged in money. The new path reads only `Source.CACHE`, which is free; it
appears in the usage audit as `ReadCache`, so the extra cache reads are visible without being
billed. No new listener, no new server query. `firebase/firestore-usage.md` gains a line on the
offline path.

**No schema, rules or Cloud Function changes.** Nothing about what is stored changes — this is
entirely about when a reader consults the cache it already has.

**Tests** — GitLive exposes `FirebaseFirestore.disableNetwork()` / `enableNetwork()` in
`commonMain`, so the E2E suite can cut the connection without stopping the Firebase emulators. That
is the lever for covering both failures end to end.
