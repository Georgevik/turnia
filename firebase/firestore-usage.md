# Firestore usage

What a Turnia user costs on Firebase, how that was measured, and what keeps it low. Read it with
[firestore-schema.md](firestore-schema.md), which explains the sync markers every number here depends on.

Measured on 13 Sep 2026 on an Android emulator (debug build) with one account in 3 groups and 19
personal events, before and after the read-optimisation branch, using `FirestoreAudit` logs. The
functions and rules from that branch were deployed to `turnia-23ebc` for the second run.

## The model in one paragraph

Firestore bills one read per document the server returns, one write per document it stores, and one
invocation per callable. Turnia keeps a **sync document** per user (`users/{uid}/sync/updates`) and per
group (`groups/{g}/sync/updates`) holding a timestamp for everything the app caches. The app keeps a
**listener on each sync document** and reads anything else **from the local cache** unless its marker
says the server moved on. A healthy session therefore costs only the listener attaches, plus a read for
each thing somebody else actually changed.

## What each action costs

Server reads, as billed. "Attach" costs are upper bounds: a listener that re-attaches within 30 minutes
of its last listen pays nothing.

| Action | Before | After | What remains |
|---|---:|---:|---|
| Cold start (> 30 min since last) | 11 | **1 + G** | User sync listener, one sync listener per group (G). |
| Cold start (< 30 min since last) | 4 | **0** | — |
| Page through months | 0 | 0 | Events come from the cache. |
| Open a group calendar | 0 | 0 | — |
| Open group info (admin) | 4–5 | **0** | Requests and types follow the sync listener. |
| People tab, first time per 30 min | 2 | **1** | `calendarsSharedWithMe` listener attach (empty result = 1). |
| Open a colleague's calendar, first time per month | — | 1 read + **1 call** | `sharedSync(snapshots)` attach on the owner's `sync/updates` (0 within 30 min of the last), and one `getSharedCalendar` for the month. Was 1 call per month visited and **2** on opening. |
| Page back to, or reopen, a colleague's month already seen | — | **0** | Served from the device's cache, after the app was killed too, until the owner's markers move. |
| The colleague changes a shift while their calendar is open | — | 1 read + 1 call | 1 read for their marker; `getSharedCalendar` with `since` returns only the changed documents — but to see shifts that left the colleague it reads every changed shift in their groups, not only theirs, so a cursor older than a day asks for the month whole instead. Server: `onGroupEventWrittenMarkHolders` adds 1 invocation and 1 write per holder (at most 2) to **every** group event write that changes what a calendar shows. Each holder also pays for the stamp (next row). |
| A shift of yours is created, changed, taken, handed to you or given back | — | **+1 read per device** | New with `groupEvents`: the trigger stamps your own `sync/updates`, which every one of your signed-in devices already listens to, so each pays 1 read for it, whether or not anybody shares your calendar. Nothing else follows: the app has no use for its own `groupEvents`, so no other read or call is made. A take or a hand-back stamps two users, so both pay. |
| Background, or another tab, with a colleague's calendar on screen | — | **0** | The calendar lets go of the owner's markers within 35 s (5 s for the screen, 30 s keep-alive). Coming back costs 1 re-attach read (free on Firestore's bill within 30 min) and 1 call only if something moved. |
| Hide or show a shared calendar | — | 0 reads | 2 writes: `private/preferences` and its marker. The list is filtered on the device from the query already listened to; your other devices pay 1 preferences read. |
| Somebody shares their calendar with you | — | — | Server: `onCalendarShared` reads each new grantee's `private/preferences` (1 read each) to stay silent for a calendar they hid. |
| Groups / Swaps / Settings tabs | 0–1 | 0 | — |
| Save a group, a type or a colour | 0 reads | 0 reads | 2 writes: the document and its marker. |
| Open a day in my calendar | — | **0** | The day's one-off events arrive with the month, like its shifts: the sheet reads nothing of its own. |
| Add, edit or delete a one-off event | — | 0 reads | 2 writes: the event and the marker of every month it spans (an edit also marks the months it leaves). |
| Group event notes: open a month | — | **0** | `extras(CACHE)` per month, free. A server read (`extras(SERVER)`) only for a month whose `groupEventExtras` marker moved past the cache, and then only the notes that changed. A month where you wrote none has no marker and costs nothing. |
| Save or clear a note on a group shift | — | 0 reads | 2 writes: `groupEventExtras/{eventId}` and its month's marker. Your other devices pay 1 read for the marker and 1 for the note. |
| Move a personal shift to a group | — | **N + M**, server | `candidates(SERVER)`: one read per still-personal event of the type from the window's start (N, the tapped one included), asked for before offering "only this one or all". `heldDates(SERVER)`: one read per shift you hold in the group in those months (M), one query per 30 months — an empty answer still costs 1. Both skip the cache on purpose: a stale answer would double-book a day. |
| … and its writes | — | 0 reads | Per commit of up to 150 events: **2 per event** (the group shift and the personal event's soft delete), **+1 per event with a note** (`groupEventExtras`), and one marker each for your `personalEvents` months, the group's `events` months and, with notes, your `groupEventExtras` months. "Move all" adds **2** in its last commit: the type's soft delete and its marker. A year of future shifts is about 700 writes. Every member viewing the group pays the usual remote-change reads. |
| Shift setup: decide whether to show it | — | **0–2**, once per device | 0 once the device has settled it (skipped, completed, or the account had something), or when the cache already holds a type or a group. Otherwise `hasAnyType(SERVER)` and `hasAnyGroup(SERVER)`, each a `limit(1)` server read (an empty result still costs 1). A failed read settles nothing, so an offline launch tries again next time. |
| Shift setup: create the shifts | — | 0 reads | **N + 1 writes** in one commit: one per type (3 with the defaults) and the types marker. |
| Team prompt: decide whether it is due | — | **0–1** per event added | 0 while switched off, below the threshold, or once the device has settled it (shown, or the account had a group). Otherwise one `hasAnyGroup` read, audited as `teamPrompt(SERVER)`, a `limit(1)` read, per event added past the threshold. It settles for good at the first answer, since shown and in-a-group both settle it, so in practice it costs 1 read per device. A failed read settles nothing and the next event tries again. |
| Group screen: the "only you here" card | — | **0** | Derived from the members the group listener already brings. |
| Rename yourself (`updateProfile`) | — | 1 call | Server: profile + marker + reservation + 2 per group. Your other devices pay 1 profile read; every member pays 1 group read per shared group. |

**Remote changes** are the part that scales with team activity and is the same before and after: when
another member changes a shift in a month you are viewing, you pay 1 read for the sync document change
and 1 per changed event. On top of that, every change to a shift **you hold** now costs each of your
devices 1 read for the `groupEvents` stamp on your own sync document, even when you are not looking at it.

## Expected daily usage

A typical user: 4 app launches more than 30 minutes apart, 2 group-info opens, 1 People visit.

| | Reads / user / day | Free tier covers (50 000 reads/day) | Reads bill at 10 000 DAU |
|---|---:|---:|---:|
| Before | ~55 | ~900 DAU | ~$9.00 / month |
| After (3 groups) | **~17** | **~2 900 DAU** | **~$2.20 / month** |

Formula for planning: `reads/day ≈ launches × (1 + groups) + People visits + remote changes seen + devices × changes to your shifts`.

Assumptions: $0.06 per 100 000 reads (the upper end of Firestore's location pricing; check the
database's location), 30-day months, 50 000 free reads/day per project. Writes ($0.18 / 100 000) and
Cloud Function invocations are small next to reads for this app: an event save is 2 writes. What a
function reads and writes on the server does not appear in the client audit.

## What changed on the optimisation branch

| Fix | Saved |
|---|---|
| `getCached` no longer treats a cancelled coroutine as a cache miss | Spurious server reads on cancellation |
| Group detail follows the group instead of loading once and again on every resume | 2 reads per open, and every return to the screen |
| `FirestoreAudit` counts listener attaches (`trackedSnapshots`) | Makes the numbers above visible |
| One-shot sync reads answered by the live listener (`awaitConfirmed`) | 1 read per launch, 1 per group-info open |
| A missing preferences document is not re-read when no marker exists | 1 read per launch, 2 per group-info open |
| `subscription` marker, server-only by rule | 1 read per launch |
| Pending join requests listed from a server-kept map on the group sync doc | 1 read per group-info open |
| `profile` marker for the user's own document | 2 reads per session |
| Groups followed through a membership index instead of a `memberUids` listener | G reads per launch |
| Unbounded Firestore cache | Protects events older than the retention window from LRU eviction |
| A month a one-off event moved out of is remembered as checked | 1 empty read per sync change and per calendar page, for as long as the month stays in view |
| The day sheet takes its one-off events from the calendar instead of querying them | A second cache query and staleness check per open sheet |
| A colleague's calendar is cached per month on the device and caught up through the owner's markers (`groupEvents`) with `since` | 1 call per month visited, 1 on every reopen, a second call on every open |

## Keeping it this way

- **Every Firestore call reports itself** (see *Firestore usage tracking* in `CLAUDE.md`). A listener goes
  through `trackedSnapshots`; a plain `.snapshots` hides its attach cost.
- **Never add a `get()` next to a listener on the same document.** Use the listener's value
  (`awaitConfirmed`).
- **A document the app caches needs a marker, and every writer moves it in the same commit**, server
  functions included. A write that forgets the marker is invisible to other devices; a marker without
  a matching `updateAt` costs a read on every launch.
- **Prefer a marker-gated cache read over a query listener.** A query listener re-bills its whole result
  set on each re-attach after 30 minutes away.
- **Watch the real bill, not only the log.** Compare the audit against Cloud Monitoring's
  `firestore.googleapis.com/document/read_count` now and then; the audit's attach counts are upper bounds
  and it cannot see what functions spend.

## Deploy order and known limits

- **Rules and functions deploy before the client ships.** Without them, a joined group never appears for
  an indexed user and admins see no pending requests.
- `functions/lib/scripts/backfillJoinRequestMarkers.js` adds requests that were already pending before
  the pending map existed. It needs application-default credentials
  (`gcloud auth application-default login`) and has **not been run yet** on `turnia-23ebc`.
- A user's first launch on the new client pays one `memberUids` listener attach to seed their group
  index; after that the index is authoritative.
- Other members' cached profiles (avatars) are never refreshed: avatars are copied nowhere by design, so
  there is no free signal, and a correct refresh would cost about one read per member.
- A group whose document is not cached shows a spinner rather than an error while offline.
