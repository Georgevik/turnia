# Proposal

## Why

A colleague's calendar has no cache at all. Every month visited calls the `getSharedCalendar` Cloud
Function again, and each call re-reads every document in a three-month range. The calendar opens with
**two** calls instead of one. After the app is killed, the same walk (People → Adriana → October →
November) repaints every month from grey and empty. This was reproduced on a Samsung with the debug
build: `FirestoreAudit` logged 4 calls for that walk.

It also never updates on its own. A swap made while the calendar is open does not show until the
viewer changes month.

There are two causes:

1. `SharedCalendarRepositoryImpl` hands the function's answer straight through and keeps nothing. A
   group's calendar is served from Firestore's disk cache, but this one is served by a callable and
   gets no such cache.
2. `ExternalCalendarViewModel.monthDate` starts at *today*, e.g. the 26th, and `CalendarViewer` then
   reports the 1st of the same month. `flatMapLatest` sees two different dates and fires twice.
   The range it asks for (`date ± 1 month`) also depends on the day, so no answer could be reused
   even if one were kept.

The goal is a calendar that is **live while it is open** and costs only the documents that actually
changed. The app follows the owner's change markers, compares them with what the cache already has,
and asks the server only for the gap.

## What Changes

- **BREAKING (internal API)**: `SharedCalendarRepository.getSharedCalendar(ownerId, from, to)` is
  replaced by `sharedCalendar(ownerId, month): Flow<Outcome<SharedCalendar, SharedCalendarError>>`.
  The repository derives a fixed window per month:
  `[first day − 14 days, last day + 14 days]`, which is ≤ 92 days, the function's cap.
- `ExternalCalendarViewModel` reduces its date to a month with `distinctUntilChanged()`, so two dates
  in the same month make one request. `DemoSharedCalendarRepository` follows the new API.
- **A new server-maintained marker for the owner's group shifts**:
  `users/{uid}/sync/updates.groupEvents: map<YYYY-MM, {updatedAt}>`. A new Firestore trigger on
  `groups/{g}/events/{e}` stamps it for the previous and the new assignee, and for both months when
  the date moves.
  - It is needed because the markers that exist today do not work for this viewer. The group-side
    marker (`groups/{g}/sync/updates.events`) is readable by members only. The viewer, who is outside
    the group, can already read `users/{owner}/sync/updates`, since the rules allow `ownerOrShared`.
  - The rules make the new field server-only.
- **While a colleague's calendar is open, the app listens** to `users/{owner}/sync/updates`. It
  compares these markers against what the cached month last saw:
  - `groupEvents[month]`
  - `personalEvents[month]`
  - `personalOneOffEvents[month]`
  - `personalEventTypesUpdatedAt`

  A month is caught up only when one of them moved. The listener is `UserSyncFirestore.observe`'s,
  so it detaches 10 minutes after the screen closes.
- **`getSharedCalendar` gains an optional `since` cursor.**
  - With it, the function returns only documents whose `updateAt` is newer. Soft-deleted events come
    back, and so do shifts that left the owner (transferred or handed back), listed as removals by
    id.
  - Without it, the function answers as today. The client then merges the gap into its cache.
- **A persistent cache** for each month's merged response, stored as JSON in a Preferences DataStore
  of its own (`turnia.shared_calendars.preferences_pb`). Each entry is keyed `viewerUid|ownerUid|yyyy-MM` and
  holds three things: the response, the `updateAt` cursor, and the marker values it was caught up
  to. The domain model is not cached: `SharedCalendarMapper` maps on every read.
- **The cache is merged, never overwritten.** A document the retention job purged never shows up in
  a gap, so old months keep their history on the device, as CLAUDE.md requires. There is no need
  for a separate freezing rule.
- `SharedCalendarError.NotShared` wipes that owner's entries for that viewer.
- `preferencesFilePath()` becomes `preferencesFilePath(fileName)` on Android and iOS.
- `FirestoreAudit` gains a read-only `callsTo(name)` so the E2E suite can assert on function calls.
- Docs:
  - `firebase/firestore-schema.md`: the new marker, the trigger and the `since` parameter.
  - `firebase/firestore-usage.md`: the new row and the trigger's write cost.
  - Corrected KDoc on `SharedCalendar`, on `getSharedCalendar` ("Nothing is stored") and on
    `ExternalCalendarViewModel.events`.

## Capabilities

### New Capabilities
- `shared-calendar`: how a colleague's calendar is requested per month, cached on the device, kept
  live through change markers while open, caught up by gap reads, and invalidated.

### Modified Capabilities
- None. There are no existing specs.

## Impact

- **core**:
  - `SharedCalendarRepository`, `SharedCalendarRepositoryImpl`, and a new `SharedCalendarCache`
  - `SharedCalendarFunction` and its request/response DTOs, for `since` and the removals
  - `UserSyncFirestore.observeShared(ownerId)`, the owner-marker listener, reported under its own
    audit key `sharedSync(snapshots)`
  - `PreferencesDataStore*` (common, Android, iOS)
  - `DataModule`, which needs its first Koin qualifier
  - `FirestoreAudit`, which gains read-only counters, and KDoc on `SharedCalendar`
- **app/shared**: `ExternalCalendarViewModel`, `DemoSharedCalendarRepository`.
- **firebase**:
  - `functions/src/sharedCalendar.ts`, for `since` and the removals
  - a new trigger that stamps `groupEvents`
  - `functions/src/sync.ts`, which gains a `markUserGroupEventsUpdated` helper
  - `firestore.rules`, to make `groupEvents` server-only
  - `firestore-schema.md`, which also gets a fix for existing drift: it documents
    `personalEventsUpdatedAt` as one timestamp, but the client writes `personalEvents` as a
    per-month map
  - `firestore-usage.md`
  - fixtures only if they seed `sync/updates`
  - The existing indexes already cover the gap queries: `events (yearMonth, updateAt)`,
    `personalEvents (yearMonth, updateAt)` and `personalOneOffEvents (yearMonthStart, yearMonthEnd,
    updateAt)`.
- **Verification is E2E only**: `SharedCalendarFlowsTest.kt` proves the call and read counts and the
  cache contents, plus a way for `CalendarRobot` to page backwards. No unit tests are added.
- **Cost**:
  - Opening a cached calendar costs 1 listener read, and nothing if it re-attached within 30 min.
  - A change while it is open costs 1 marker read plus one gap call, which returns only the changed
    documents.
  - Every group event write gains 1 trigger invocation plus 1–2 marker writes, on the server.
- **Deploy order**: rules, then functions, then the client. An older client ignores the new marker
  and the new parameter.
