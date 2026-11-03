# Design

## Context

See `proposal.md` — *Why*. What matters for the approach is the shape every event reader shares:

```
  subscribe
     |
     v
  CACHE query (ONE SHOT)        <- the only time the cache is ever read
     |  emit
     v
  listen: sync/updates          <- the only live thing in the pipeline
     |
     v
  staleMonths empty? --yes--> settled? --yes--> emit NOTHING
     |
     no
     v
  SERVER delta query            <- the only thing that can change the output
```

The emitted list is a closure variable (`known`) frozen at subscription time, and the only operation
that mutates it is a `Source.SERVER` query. Both reported failures follow from that one fact:

- **Nothing appears offline.** The local write lands in the cache, and the marker's pending write
  does wake the listener — `trackedSnapshots` subscribes with `includeMetadataChanges = true` — but
  the month's marker is written as `Timestamp.ServerTimestamp`, which reads back unresolved from the
  cache, so `staleEventMonths` skips the month and the reader emits nothing. There is no cache-read
  step left for it to reach.
- **The calendar blanks.** If a month is genuinely behind while offline, the reader goes to the
  server, `get(Source.SERVER)` fails rather than falling back, and the terminal
  `.catch { emit(emptyList()) }` collapses the whole window — not just that month.

The project already states the correct rule for the other direction, for
`users/{uid}/private/preferences` (`firebase/firestore-schema.md`):

> An **unresolved** marker counts as settled — a pick made on this device is in the cache before the
> server acknowledges it, which is exactly when the cache is the most current thing there is.

`UserPrivateFirestore.currentPreferences` earns that by reading the cache on *every* marker
emission, going to the server only when the marker proves the cache is behind. The event readers
agree on the rule and have nothing to apply it to. This change gives them the missing step.

Six readers share the shape. Five are written by the client and need both corrections:
`PersonalEventFirestore`, `GroupEventFirestore`, `PersonalOneOffEventFirestore`,
`GroupEventExtrasFirestore`, `PersonalEventTypesFirestore`. `RevokedGroupFirestore` needs only the
second: `users/{uid}/revokedGroups` is written exclusively by `firebase/functions/src/membership.ts`,
so no local write of it is ever pending.

## Goals / Non-Goals

**Goals:**

- One rule, stated once and repeated identically in every reader: *when the server cannot answer,
  read the cache; never let a server read it could not make empty the calendar.*
- No change in billable reads, no new listener, no change to what is stored.
- No change above the data layer — not in a repository, a ViewModel, a UiState or a composable.

**Non-Goals** (beyond the proposal's):

- Extracting the six copied flows into one shared abstraction. See *Decisions*.
- Making a write resolve before the server acknowledges it. `batch.commit()` keeps suspending until
  the commit lands, so analytics for an offline add still fires on reconnect.
- Surfacing `hasPendingWrites` anywhere above the datasource.

## Decisions

### The cache path is triggered by an unconfirmed marker, not by an unresolved timestamp

`Synced.confirmed` is already `!isFromCache && !hasPendingWrites`. `confirmed == false` therefore
covers both situations in which the server cannot help:

| State | `isFromCache` | `hasPendingWrites` | Means |
|-------|---------------|--------------------|-------|
| Offline, answered from cache | true | — | no server read is possible |
| Online, this device's write unsent | false | true | the cache is ahead of the server |

Both mean the same thing for a reader: the cache is the most current thing there is. So the branch
is one condition, and it fixes both reported failures at once — the pending write becomes visible,
and the offline reader never attempts the server query that blanks the calendar.

**Alternative considered:** detect the months whose marker key is present but whose timestamp
resolves to null, and re-read only those. Rejected because it does not cover the offline-from-cache
case at all, so the blank calendar would survive it.

**Measured** (tasks 1.1/1.2, a temporary probe on `observeWithPendingWrites` while adding an event
with the connection cut). The four emissions of one add-offline-then-reconnect cycle:

| # | `confirmed` | marker `updatedAt` | What it is | `observe()` today |
|---|-------------|--------------------|------------|-------------------|
| 0 | `true` | `Timestamp(…894)` | steady state, online | passed through |
| 1 | `false` | `Timestamp(…894)`, unchanged | the connection dropping | **swallowed** (value identical) |
| 2 | `false` | `null` | this device's pending write | passed, then discarded as not stale |
| 3 | `true` | `Timestamp(…902)` | resolved on reconnect | passed; marker moved, delta runs |

Three things this settles:

- An unresolved `ServerTimestamp` deserializes to **`null`**, not a client estimate. So a pending
  month can never look *stale*, and the risk of a needless server read while a write is in flight
  does not exist.
- The marker listener **does** emit on the local pending write (row 2). Nothing is missing at the
  Firebase end; the reader simply has no cache-read step to act on it.
- Row 1 is the blank-calendar trigger, and it is distinguishable from row 0 by `confirmed` **alone**.
  That is precisely what `observe()`'s outer `distinctUntilChanged()` removes, and why the fix cannot
  be built on the marker value.

### The cache path re-queries; it does not patch the in-memory list

On an unconfirmed emission the reader runs its existing month query with `Source.CACHE` and merges
the result into `known`, exactly as the server path does.

The alternative — having the writer hand the reader the document it just wrote — would introduce a
second source of truth beside the cache and would not work uniformly: several writes are
`updateFields` rather than a whole document, and `GroupEventExtrasFirestore.set` is handed a caller's
batch, so no single place holds the written state. Re-querying costs nothing billable and reuses the
code that already exists.

### Merging by id needs no special case for the resolved write

A pending document read from the cache carries `updateAt = null`, and `updatedByMonth()` drops nulls
when computing a month's cursor. So a pending write does not advance the cursor:

```
offline add      cache: {eventA: null}       cursor for month: T0 (unchanged)
                 marker: month -> unresolved
                 -> unconfirmed -> CACHE re-read -> eventA shown

reconnect        marker: month -> T1 (> T0)
                 -> confirmed, stale -> SERVER delta (updateAt > T0)
                 -> returns eventA with updateAt = T1
                 -> merge by id replaces the pending copy
```

The resolved document replaces the pending one by id, so it appears exactly once and the cursor
advances only on confirmed data. No reconciliation logic, and no risk of a duplicate.

### A failed server delta falls back, inside the flow

The server query is wrapped where `known` is still in scope — inside the `mapNotNull`, not in the
terminal `.catch`. On failure the reader re-reads those months from the cache and merges; if that
fails too it emits `known` unchanged. The terminal `.catch { emit(emptyList()) }` stays as a
backstop for the genuinely unexpected, but stops being reachable for an offline or transient
failure. `GroupEventExtrasFirestore`, which has no terminal catch at all today, gains the same inner
fallback so a failed note read cannot take the calendar flow down with it.

### Nothing about pending writes is built; the flag is only un-discarded

Firebase supplies everything this change needs, and the app's own code is what drops it:

| Firebase supplies | What the app does with it today |
|-------------------|---------------------------------|
| The write, in the local cache, immediately | Never re-reads the cache after subscribing |
| `hasPendingWrites` on the snapshot | `observe()` drops it with `.map { it.value }` |
| `isFromCache` on the snapshot | The same |
| A snapshot emission on the local write | Arrives, finds nothing stale, emits nothing |

`Synced.isConfirmed` is just `!isFromCache && !hasPendingWrites` — pure Firebase metadata. So the
only change either sync datasource needs is visibility: `GroupSyncFirestore.synced` is **already**
`Flow<Synced<GroupSyncDocument>>` and merely `private`, with `observe()` the one public door out of
the class, dropping the flag on the way through. Widening it costs no new listener — `synced()` is
the same shared listener `observe()` already rides on.

Exposed as `observeWithPendingWrites` for symmetry with `UserSyncFirestore`, which has that exact
accessor already, added for the preferences reader that needed the same flag. That name is a
preference, not a requirement; `UserSyncFirestore` itself needs no change at all.

### The filter `observe()` applied moves onto the reader's output

`observe()`'s `.map { it.value }` is not dead weight, and understanding what it was for is what keeps
a switching reader correct. There are two layers of `distinctUntilChanged`:

```
snapshots()                  distinctUntilChanged() on Synced(value, confirmed)
                             -> drops emissions where BOTH are unchanged
   |
   v
synced()   Flow<Synced<T>>   <- the pending-aware view taps here
   |
   v
observe()  .map { it.value }.distinctUntilChanged() on the value alone
                             -> ALSO drops emissions where only `confirmed` changed
```

The outer pair collapses confirmation-only transitions, of which three occur and the first on every
attach: the cache's answer being confirmed by the server, the connection dropping back to the cache,
and this device's own write resolving to the same value.

**It was never a cost guard.** `staleEventMonths` depends only on the marker value and `known`, so
the same value with the same `known` yields the same answer: a confirmation-only transition cannot
flip not-stale into stale, and so cannot cause a read. The outer filter is pure noise suppression —
it spared a reader from waking for an emission that said nothing new *about the data*. Correct, until
a reader starts caring whether the server has spoken, at which point it is filtering out the signal.

So a reader on the pending-aware view inherits those emissions and must dispatch them itself
(`confirmed == false` → cache path; `confirmed == true` → the existing stale check), and the filter
moves **one layer down**, onto the reader's emitted list rather than onto the marker. It has to move
rather than be removed: the decision now depends on `confirmed`, so `confirmed` cannot be filtered
out before the decision is made. Filter the answer, not the question.

That output filter is cheap insurance rather than load-bearing: `StateFlow` already conflates equal
values, and `CalendarViewer`'s `remember(eventsByDate)` compares by `equals`, so a structurally equal
list recomputes nothing. Two layers above already absorb a repeat.

### The marker listener must not collapse its unconfirmed emissions

Found while implementing: `UserSyncFirestore.snapshots` and `GroupSyncFirestore.snapshots` ended in
`distinctUntilChanged()` over the whole `Synced` pair, and that silently broke the second of two
offline writes to the same month. An unresolved server timestamp reads as `null` both times, so the
pair is *identical* between the two writes, the emission is collapsed, the reader never re-reads the
cache, and the second event is not painted on the grid.

**It was never a matter of losing a write.** Both events are in the cache and both reach the server
on reconnect, with or without this filter — which is also what the original behaviour did, and the
only reason the bug was invisible before: a reader that emitted nothing on an unconfirmed marker
did not care whether it was woken once or twice. The filter cost the *second add its visibility
while offline*, nothing more, and only when emission 3 is byte-identical to emission 2 — the same
month, the same kind of event, no other write touching `sync/updates` in between, and a flatly
dropped connection with no `isFromCache` churn to break the equality.

Both inner filters are therefore removed. Nothing regresses, because every consumer already dedupes
at the level it cares about and the inner filter was subsumed by each of them:

| Consumer | What it dedupes |
|----------|-----------------|
| `observe` | `.map { it.value }.distinctUntilChanged()` — the document, exactly as before |
| a marker-gated reader | `distinctUntilChanged()` on the list it emits |
| `get` / `awaitConfirmed` | takes `first { it.confirmed }`, indifferent to repeats |

The cost of the extra emissions is a cache read each, which is free and billed to nobody. This also
means the fix cannot be built on the marker's *value* changing — only on its `confirmed` flag and on
re-reading the cache, which is what the first decision above already settled.

### The types query had to stop filtering on `updateAt`

Also found while implementing. `PersonalEventTypesFirestore.queryEventTypes` read
`updateAt > Timestamp(0, 0)` when given no cursor, and Firestore matches no comparison against
`null` — so a type this device had just written was excluded from its own cache read. The chip never
appeared in the add pane, and because `PersonalEventRepositoryImpl.getEvents` drops an event whose
type is absent from the map, an event of a shift created offline rendered nowhere at all.

A null cursor now means no filter rather than `> 0`. The events readers never had this problem: their
base clause is `yearMonth equalTo`, which matches whatever `updateAt` holds. `RevokedGroupFirestore`
still has the `> 0` form, left alone deliberately — only Cloud Functions write there, so no local
write of it is ever pending.

### The two views coexist on one listener, as they already do in production

`SharedListeners.shared` keys on the id and `shareIn`s the result, and both accessors reach it
through the same `synced()`. One listener instance, one attach, one bill, and two projections that
cannot disagree.

`observe()` is unchanged and keeps its callers: `GroupJoinRequestFirestore`, `GroupFirestore` (twice),
`UserPrivateFirestore`'s join requests, and `RevokedGroupFirestore`. The mixed arrangement is already
live on the same document through the same listener — `UserPrivateFirestore` reads preferences through
`observeWithPendingWrites` and join requests through `observe()`. This change widens an existing
pattern rather than introducing a mode.

Billable reads are unchanged either way:

| Sequence | Today | After |
|----------|-------|-------|
| Attach, server agrees with the cache | stale check once, no read | stale check twice, no read |
| Offline add, then reconnect | one server delta on resolve | the same one delta |
| Connection drops with the calendar open | swallowed, no read | cache path, no read |

What is added is in-memory comparison on emissions that were previously discarded. In the third row
that discarded emission is the blank-calendar fix.

### Each reader is edited in place; no shared abstraction

The six flows look alike but differ in ways a single helper would have to branch on: a per-month
marker map (events, one-offs, extras) versus one timestamp for the whole collection (types, revoked
groups); three different visibility filters (`areNotDeleted`, `visible`, `withNotes`); a month range
derived from dates versus a month collection passed in; and `PersonalOneOffEventFirestore`'s
`checkedMarkers` bookkeeping, which none of the others have. A generic version would carry more
conditionals than the duplication it removes, and the change the user asked for is a small one.

Revisit if a seventh reader of this shape appears, or if these flows need to change together again.

## Risks / Trade-offs

- **[~~An unresolved `ServerTimestamp` may deserialize as a client estimate, not null~~]** →
  **Closed, measured as `null`.** See the table under *Decisions*. A pending month therefore cannot
  look stale, and the needless-read case this risk described does not arise. The stale check still
  sits behind the `confirmed` branch, so the question cannot reopen by accident.

- **[While a write of this device is unsent, a teammate's change is not fetched]** → The reader takes
  the cache path and skips the stale check for that emission. The delay is bounded by the write's
  round-trip: when it resolves, the marker emits again as confirmed and the stale check runs then.
  Nothing is lost permanently, and this is the same bargain the preferences reader already makes.

- **[A write the security rules will refuse looks successful until reconnect]** → It silently
  disappears from the calendar when the server rejects it. This is pre-existing — the write already
  resolves only on acknowledgement — and the *indistinguishable* decision accepts it: the alternative
  is telling every offline user their shift might not be saved. Worth a follow-up change if rejections
  turn out to happen in practice; out of scope here.

- **[The fallback hides a real server failure behind a stale calendar]** → Every failed read is still
  logged as an error and still appears in the usage audit, so the signal is not lost. A stale
  calendar is strictly better for the user than an empty one.

- **[Re-reading the cache on every unconfirmed emission could churn the UI]** → The marker listener
  is already `distinctUntilChanged` on the `Synced` pair, so metadata-only repeats never reach the
  reader. Adding `distinctUntilChanged()` to each reader's output makes an identical list a no-op for
  recomposition as well.

- **[Cache reads grow in the usage audit without the bill growing]** → Intended, and the reason the
  audit counts `ReadCache` separately. `firebase/firestore-usage.md` gains a line so the new numbers
  are not read as a regression.

## Verification

Everything here is verified by the instrumented suite — `OfflineFlowsTest`, 13 tests — and not by
unit tests, because these six datasources take `FirebaseFirestore` concretely and the project has no
fake for it (`core/src/commonTest` holds one file, `SyncedTest`, over pure helpers). There is no seam
to inject a throwing or canned query through, and building one was out of scope for a change this
size.

`Network` (`e2e/infra/Network.kt`) is the lever: it resolves the app's own Firestore from Koin and
toggles `disableNetwork()` / `enableNetwork()`, leaving the emulators up, so *offline* is tested apart
from *no backend*. Its own test asserts the cut actually cuts, because every other test here would
otherwise pass for the wrong reason.

Both fixes were confirmed to fail before them and pass after, by reverting the reader and re-running:

| Behaviour | Test | Fails without the fix |
|-----------|------|-----------------------|
| An offline add appears at once | `offline_addingAShift_showsItAtOnce` | yes, at `awaitDayShows` |
| The calendar is never emptied | `offline_aMonthTheDeviceNeverSynced_keepsTheRestOfTheCalendar` | yes |

The audit assertion is machine-checked rather than eyeballed: `offline_addingAShift_costsNoServerRead`
reads `FirestoreAudit.usage(...)` directly and asserts server reads do not move while cache reads do.

**What is not directly verified** is the fallback of *A failed server delta falls back, inside the
flow*. Its trigger is `confirmed == true` together with a server read that fails, and
`disableNetwork()` can never produce it: offline always yields `confirmed == false`, which takes the
cache path and never reaches the server query. It stays in as defence in depth for a transient or
rules-denied read, and the user-facing outcome it guards — a stale calendar rather than an empty one
— is covered through the cache path instead. Closing that gap needs either a Firebase fake or a pure
decision function extracted from these flows; both were weighed and declined for this change.

## Migration Plan

Client-only. No Firestore rules, Cloud Function, index or schema change, so there is no deploy
ordering to respect and no backfill: the change ships in a normal app release.

Rollback is reverting the client. An older build reading a cache a newer build wrote behaves exactly
as it does today — nothing about what is stored changes, only when a reader consults it.

## Open Questions

- ~~Does `disableNetwork()` in the instrumented suite reproduce the *closed before reconnecting*
  case?~~ **Resolved: yes, within one process.** `E2eRule.relaunch()` already closes the activity and
  launches it again without clearing the app's data, so the reader re-subscribes with the Firestore
  cache and its queued mutations intact — which is what the scenario is about. It is not a true
  process death; surviving that is Firestore's own persistent-cache guarantee, not this change's,
  and the suite clears app data between tests so it cannot be staged there anyway.
