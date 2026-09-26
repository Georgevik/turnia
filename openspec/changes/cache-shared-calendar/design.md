# Design

## Context

Why the change is needed: see `proposal.md`. What it must do: see `specs/shared-calendar/spec.md`.

This is how things stand today:

- **The callable.** `getSharedCalendar` (`functions/src/sharedCalendar.ts`) authorizes on
  `users/{owner}.calendarSharedWith`. It then re-reads everything in `[from, to]`:
  - the owner's groups (`memberUids` and `revokedUids`);
  - each group's events with `assigneeId == owner`, filtered by date;
  - personal events, one-offs and all personal types.

  It skips `isDeleted` documents, so a deletion is invisible to it.
- **Group events** carry `yearMonth`, `updateAt` and soft deletes. Transfers change `assigneeId` in
  place, so a shift the owner lost stops matching the function's `assigneeId == owner` filter
  silently.
- **Group markers.** Each group has `groups/{g}/sync/updates.events[YYYY-MM]`, readable by members
  (and revoked users) only. A viewer outside the group cannot see it.
- **User markers.** `users/{owner}/sync/updates` is readable through `ownerOrShared`, so a viewer can
  already listen to it. The client writes `personalEvents: map<YYYY-MM,{updatedAt}>`,
  `personalOneOffEvents: map<YYYY-MM,{updatedAt}>` and `personalEventTypesUpdatedAt`
  (`UserSyncDocument`). Nothing there tracks the owner's group shifts.
- **The listener.** `UserSyncFirestore.observe(uid)` is a shared, tracked listener with a 10-minute
  keep-alive (`SharedListeners`). It already handles a missing document.
- **Indexes.** These exist already:
  - `events (yearMonth, updateAt)`
  - `personalEvents (yearMonth, updateAt)`
  - `personalOneOffEvents (yearMonthStart, yearMonthEnd, updateAt)`
- **Triggers.** `onEventPutOnSwap` is already an `onDocumentWritten` on
  `groups/{groupId}/events/{eventId}`. `functions/src/sync.ts` is the one place markers are written.
- **Local storage.** There is one Preferences DataStore (`turnia.preferences_pb`), bound unqualified.
  Core has no mocking library and no Koin qualifiers.
- **E2E.** Every test runs in its own process with cleared app data, so a cache never leaks between
  tests.

## Goals / Non-Goals

**Goals:**
- While a colleague's calendar is open, their changes appear without any action from the viewer.
- On reopening with nothing changed: zero function calls, and at most one marker read.
- A catch-up costs one call that returns only the changed documents.
- Old months keep their history on the device after the server purge.
- Every claim above is verified end to end, through the audit counters and the cache file. There
  are no unit tests.

**Non-Goals:**
- No background listening for calendars that are not open. The user chose to listen only while
  the calendar is open.
- No direct client reads of the owner's personal collections. Everything still goes through the one
  callable (see Decision 4).
- No pruning of the cache file or of old marker keys.
- No change to how the group calendar or the owner's own calendar sync.

## Decisions

### 1. The repository takes a month and returns a Flow

`sharedCalendar(ownerId: UserId, month: YearMonth): Flow<Outcome<SharedCalendar, SharedCalendarError>>`.

A `Flow` is what lets the result be live: it emits the cache, then emits again after every
catch-up.

A month is what makes entries reusable. The window is `month.firstDay − 14 .. month.lastDay + 14`:
at most 59 days, which spans three `yearMonth`s. That is why the repository watches the markers of
`month − 1`, `month` and `month + 1`.

`ExternalCalendarViewModel.monthDate` becomes a `YearMonth` `StateFlow`, which already drops an
equal value: kotlinx.coroutines refuses `distinctUntilChanged()` on a `StateFlow` at compile time. The group branch keeps calling `getEventsByGroup(…, month.firstDay, monthDelta = 2)`.

- *Rejected:* caching by `from`/`to`. The range moves with the day, so entries would never repeat.

### 2. A server-maintained `groupEvents` marker on the owner's sync doc

The field is `users/{uid}/sync/updates.groupEvents: map<YYYY-MM, {updatedAt}>`, the same shape as
`personalEvents`.

A new `onDocumentWritten("groups/{groupId}/events/{eventId}")` trigger,
`onGroupEventWrittenMarkHolders`, runs these steps:

1. If `after` is missing, it returns. That is a hard delete, by the retention cleanup or
   `deleteGroup`, and neither may move a marker (see Decision 5).
2. It builds the set of affected pairs (uid, yearMonth): `after.assigneeId`/`after.yearMonth`, plus
   `before.assigneeId`/`before.yearMonth` when a `before` exists.
3. If nothing a viewer renders changed between `before` and `after`, it returns without writing.
   The fields that count are `assigneeId`, `date`, `yearMonth`, `groupEventTypeId`, `onSwap`,
   `isDeleted` and `history`. This also means a write of the trigger's own does not make it fire
   again.
4. It writes one batch: for each distinct uid, a merge-set of
   `groupEvents.<yyyy-MM>.updatedAt = serverTimestamp()` through a new
   `markUserGroupEventsUpdated` in `sync.ts`.

A separate trigger rather than an extension of `onEventPutOnSwap`: one trigger that both notifies
and stamps would fail as a unit, and the push path returns early for most writes.

The **rules** make `groupEvents` server-only, the same way `subscription` is: a create may not
carry it and an update may not touch it. The owner cannot move it either. Their own app never reads
it, and a client able to hold it back could hide changes from viewers.

- *Rejected:* opening `groups/{g}/sync/updates` to shared viewers. The rules cannot express "the
  viewer is in the calendarSharedWith of some member of this group".
- *Rejected:* having the client stamp the assignee's marker in its event batch. An admin editing a
  member's shift, and `takeEvent` moving it to someone else, both write markers of users the writer
  does not own.

**This breaks the same-commit rule, on purpose.** The trigger commits after the event, so the marker
is always later than the event's `updateAt`. CLAUDE.md warns that such a pair never settles when the
reader compares the two directly. Decision 3 is what keeps it from looping: the marker is never
compared with `updateAt`.

### 3. Two independent clocks per cached month

Each cache entry is `CachedSharedCalendar` and holds three values:

- **`response`**: the merged `SharedCalendarResponse`.
- **`cursor: Timestamp?`**: the newest `updateAt` the server has shown this entry, across every
  document in every answer, removals included. This is what goes out as `since`. It only ever
  comes from document timestamps, so it is on the same clock as the query it bounds.
- **`seen: Map<String, Timestamp?>`**: the relevant marker values, as the listener delivered them
  when the catch-up started. The keys are `groupEvents/yyyy-MM`, `personalEvents/yyyy-MM`,
  `personalOneOffEvents/yyyy-MM` and `personalEventTypes`.

A month is behind when any relevant marker in the latest snapshot is newer than its entry in `seen`,
or missing from `seen` while present in the snapshot. A marker is only ever compared with an earlier
reading of itself, so the trigger's lag does no harm.

The race is safe:
- A write that lands after `seen` was captured moves a marker again, and that triggers the next
  catch-up.
- A write that lands before the gap query but after the snapshot is fetched now and again next time,
  and merging is idempotent.

### 4. `getSharedCalendar` accepts `since` and reports removals

The request is `{ ownerUid, from, to, since?: ISO-8601 }`, and the response gains:

- `removedGroupEventIds: string[]`, as `groupId/eventId`
- `removedPersonalEventIds: string[]`
- `removedPersonalOneOffEventIds: string[]`
- `cursor: string | null`, the maximum `updateAt` among the documents it looked at

With `since`, the function queries as follows:

- **Group events**, per owner group: `yearMonth in [the window's months]` and `updateAt > since`,
  covered by the `(yearMonth, updateAt)` index. Each document is sorted into one of three outcomes:
  - Owner is the assignee, not deleted, and its date is inside `[from, to]`: an **upsert**, returned
    exactly as today.
  - Otherwise, but the owner appears in its holders (`ownerId`, `history.fromUid` or
    `history.toUid`): a **removal** by id. This covers taken, handed back, deleted or moved out of
    the window.
  - Neither: **dropped**. The owner never held it, so the viewer cannot have it cached, and ids of
    unrelated shifts do not leak.
- **Personal events**: `yearMonth in [...]` and `updateAt > since`. Deleted ones become removals.
- **One-offs**: the existing overlap query, plus `updateAt > since`, covered by
  `(yearMonthStart, yearMonthEnd, updateAt)`. Deleted ones, and ones no longer in the window,
  become removals.
- **Lookups** (types, colours, group names, holder names) are returned whole on every call, as
  today. They are what make a type rename show. They are also small: already one read per group,
  and paid for by the group queries anyway.

Without `since`, the function behaves exactly as today, plus `cursor`. That keeps older clients
working.

The client merges in `SharedCalendarCache`:
1. Drop the removals by id.
2. Upsert by id.
3. Replace the lookups.
4. Set `cursor` to the maximum of the old cursor and the answer's.

- *Rejected:* reading the owner's personal collections straight from Firestore. The rules would
  allow it, but that is a second source path and a second set of listeners, while the callable
  already has to exist for group shifts. Group shifts are also where the changes are.

### 5. Merging replaces the freezing rule

The retention cleanup hard-deletes, and a hard-deleted document is never in an `updateAt > since`
answer. A purge therefore never removes anything from the cache, and old months keep their history
on the device without any special case. The trigger skips hard deletes, so a purge does not even
wake a viewer up.

### 6. Repository flow

`sharedCalendar(owner, month)` is built like this:

```
channelFlow {
  entry = cache.read(key)
  if (entry != null) send(Success(map(entry.response)))

  markers(owner)                        // UserSyncFirestore.observe(owner), relevant keys only
    .map { it.relevantTo(month) }
    .distinctUntilChanged()
    .collectLatest { snapshot ->
      entry = cache.read(key)
      if (entry != null && !entry.isBehind(snapshot)) return@collectLatest
      when (val answer = fetch(owner, window, since = entry?.cursor)) {
        Success -> entry = cache.merge(key, answer, seen = snapshot); send(Success(map(entry.response)))
        Failure(NotShared) -> cache.removeOwner(viewer, owner); send(answer)
        Failure -> if (entry == null) send(answer)   // otherwise keep what is on screen
      }
    }
}
```

- A marker listener that fails already falls back to an empty document (`UserSyncFirestore`). With
  an entry, that means "not behind" and nothing is fetched. With no entry, the month is fetched
  whole.
- A missing viewer uid (signed out mid-flight) fetches without caching.
- Dependencies are passed as constructor functions, not new interfaces, which matches the other
  `*Function` classes and keeps the repository free of Firebase types:
  - `fetch` is bound to `SharedCalendarFunction::getSharedCalendar`.
  - `markers: (UserId) -> Flow<Synced<UserSyncDocument>>` is bound to `UserSyncFirestore::observeShared`.
    It keeps the `confirmed` flag: the first decision waits for the server (3 s, then the cached
    snapshot), or a Firestore-cache snapshot followed by the server one would cost two calls.
    It is the same shared, tracked listener as `observe`, reported under its own operation key
    `sharedSync(snapshots)`, so the audit tells the owner-marker reads apart from the viewer's own
    sync reads.
  - `viewerId: () -> UserId?` is bound to `FirebaseAuth.currentUser`.

On a month change the ViewModel sets `loading = true` and the first emission clears it. With a cache,
that is the DataStore read before any network call.

### 7. Storage

- **File:** `SharedCalendarCache` wraps its own Preferences DataStore, `turnia.shared_calendars.preferences_pb`,
  bound under `named(SHARED_CALENDARS_STORE)`. This is the codebase's first Koin qualifier. The
  existing unqualified binding is untouched.
- **Path:** `expect fun Scope.preferencesFilePath(fileName: String)`, with Android and iOS resolving
  it in today's directory.
- **Key:** `"$viewerUid|$ownerUid|yyyy-MM"`. Uids never contain `|`, and `removeOwner` is a prefix
  match in one `edit`.
- **Value:** JSON through a lenient `Json { ignoreUnknownKeys = true }`. An undecodable entry reads
  as missing, which costs one full fetch.
- **Why not the main settings file:** a separate file keeps this growing blob out of the small file
  read on every launch.
- **Why the raw response:** only the raw response is cached, not the domain model, so the mapper
  stays the one translation from the wire format to the domain.

### 8. Verification is E2E only

There are no unit tests. `FirestoreAudit` exposes its counters read-only:
- `callsTo(name): Int`, the calls under a callable's key;
- `usage(tag, operation): FirestoreUsage`, one operation's reads and writes.

The test also reads the qualified cache DataStore through Koin, to check the entries themselves.

The new test in `SharedCalendarFlowsTest` signs in as alice with fixture `base`. Every count is a
difference from the previous step:

| Step | `getSharedCalendar` calls | `sharedSync(snapshots)` server reads | Cache |
|---|---|---|---|
| Open Bruno's calendar, `MN` shows on `world.day(4)` | 1 | ≤ 1 | entry `alice\|bruno\|yyyy-MM` exists |
| Next month | +1 | +0 | a second entry |
| Back | +0 | +0 | — |
| Leave and reopen | +0 | +0 | the days come from the entry |
| `FirestoreRest` flips `e2.onSwap` | +1 | +1 | the cursor moves forward; the other events are kept |
| Wait 5 s | +0 | +0 | no loop: a marker that trails its event settles |
| `FirestoreRest` gives `e2` to carla | +1 | +1 | `MN` disappears; `e2` is gone from the entry |
| App in the background 40 s; the server withdraws `e3` from swap | +0 | +0 | — |
| Back to the foreground | +1 (with `since`) | +1 (re-attach) | `e3` updated |
| Calendar tab for 40 s; the server offers `e3` again | +0 | +0 | — |
| Back to the People tab | +1 (with `since`) | not asserted | `e3` updated |
| Alice writes `groupEvents` on her own sync doc | — | — | the rules deny it; her other markers still write |

The live steps depend on the Functions emulator running the new trigger. The suite already relies on
it for `takeEvent`.

`CalendarRobot` gains `showPreviousMonth()`, using the `Previous month` content description.

What the E2E cannot see:
- **The function's own reads.** They happen on the server, so neither the client audit nor the
  emulator counts them. The test proves the gap indirectly: after a one-event change the cache keeps
  the untouched events and holds exactly the changed one. What the function reads is bounded by the
  `since` filter by design, not measured.
- **A process death.** The test cannot kill its own process. Because the repository keeps no memory
  cache, "Leave and reopen" is served from the DataStore file. Survival across a real force-stop is
  checked by hand (task 8.2).

## Risks / Trade-offs

- **[The marker trails its event]** → The two-clock design (Decision 3) exists so the marker is never
  compared with `updateAt`. The E2E "Wait 5 s" step pins it: a marker later than every `updateAt` must settle
  after one catch-up, not loop.
- **[The trigger costs something on every group event write]** → Each write gains 1 invocation plus
  at most 2 marker writes, server-side. Writes that change nothing a viewer renders return early,
  and the cost goes into `firestore-usage.md`.
- **[`personalEvents` markers move for the owner's own edits in any month]** → Only the months in the
  shown window count, so an edit elsewhere wakes nobody.
- **[The listener outlives the screen]** → By 35 seconds at most. `ExternalCalendarViewModel` exposes
  its state with `stateIn(WhileSubscribed(5 s))` and the screen collects it with
  `collectAsStateWithLifecycle()`, so the repository flow stops 5 s after the screen stops being
  visible. The owner-marker listener (`observeShared`) then has its own 30-second keep-alive, apart
  from the user's own sync listener, which keeps its 10 minutes. This matters because the
  ViewModel outlives the screen twice over: in the background, and behind another tab, whose back
  stack keeps the entry and its ViewModel (`MainNavigationState`). Firestore does not bill a re-attach
  within 30 minutes of the last listen, so the short keep-alive costs nothing extra; the audit still
  counts it as one attach, an upper bound.
- **[Existing shifts have no marker yet]** → No backfill is needed. Every cache starts empty with this
  release, so a first view fetches the month whole. The first write after deploy creates the marker,
  and a marker present in the snapshot but missing from `seen` reads as behind.
- **[A late edit to an old month]** → Now caught: the marker moves and the gap returns it. This is
  better than the earlier freeze proposal.
- **[The cache file and marker maps only grow]** → A few KB per month per colleague, and one key per
  month on the sync doc. Pruning is left for later.
- **[The response shape changes]** → Lenient decoding plus a full refetch on an unreadable entry.
- **[Schema doc drift]** → `firestore-schema.md` describes `personalEventsUpdatedAt` as one timestamp
  while the client writes a per-month `personalEvents` map. This change corrects the doc to match
  the code. The code is not changed.

## Migration Plan

Deploy in this order, all from `firebase/`:

1. `firebase deploy --only firestore:rules`. This makes `groupEvents` server-only.
2. `firebase deploy --only functions`. This ships the new trigger and `getSharedCalendar` with
   `since` and `cursor`.

   Without `since`, the answer is unchanged apart from the extra `cursor` field, so clients already
   in the field keep working.
3. Release the client. Its cache file starts empty.

To roll back, revert the client first. The trigger and the extra response fields are harmless to
older clients and can stay.
