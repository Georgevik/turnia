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
| Groups / Swaps / Settings tabs | 0–1 | 0 | — |
| Save a group, a type or a colour | 0 reads | 0 reads | 2 writes: the document and its marker. |
| Rename yourself (`updateProfile`) | — | 1 call | Server: profile + marker + reservation + 2 per group. Your other devices pay 1 profile read; every member pays 1 group read per shared group. |

**Remote changes** are the part that scales with team activity and is the same before and after: when
another member changes a shift in a month you are viewing, you pay 1 read for the sync document change
and 1 per changed event.

## Expected daily usage

A typical user: 4 app launches more than 30 minutes apart, 2 group-info opens, 1 People visit.

| | Reads / user / day | Free tier covers (50 000 reads/day) | Reads bill at 10 000 DAU |
|---|---:|---:|---:|
| Before | ~55 | ~900 DAU | ~$9.00 / month |
| After (3 groups) | **~17** | **~2 900 DAU** | **~$2.20 / month** |

Formula for planning: `reads/day ≈ launches × (1 + groups) + People visits + remote changes seen`.

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
