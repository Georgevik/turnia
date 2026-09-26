# Tasks

Verification is E2E only: no unit tests are added. Tasks before group 7 are checked by building and
deploying to the emulators. Their behaviour is proven by task 7.2.

## 1. Backend: the `groupEvents` marker

- [x] 1.1 Add `markUserGroupEventsUpdated(db, writer, uid, yearMonth)` to `functions/src/sync.ts`. It
  merges `groupEvents.<yyyy-MM>.updatedAt = serverTimestamp()`. Verify that `npm run build` in
  `firebase/functions` passes.
- [x] 1.2 Add the `onGroupEventWrittenMarkHolders` trigger on `groups/{groupId}/events/{eventId}` and
  export it from `index.ts`. It:
  - skips hard deletes (no `after`);
  - skips writes that change none of `assigneeId`, `date`, `yearMonth`, `groupEventTypeId`,
    `onSwap`, `isDeleted` or `history`;
  - stamps the before and after (assignee, yearMonth) pairs in one batch.

  Verify that `npm run build` passes and that the Functions emulator loads the trigger. Behaviour
  is proven by 7.2's live steps.
- [x] 1.3 Make `groupEvents` server-only in `firestore.rules` under `users/{uid}/sync/{docId}`, the
  same way as `subscription` (create and update). Verify that the emulator loads the rules. The
  denial is proven by 7.2's last step.
- [x] 1.4 Document the work in `firebase/firestore-schema.md`:
  - the `groupEvents` marker, who writes it, and why it trails its event;
  - the trigger;
  - correct the `personalEventsUpdatedAt` row to the per-month `personalEvents` map the client
    actually writes.

  Verify that the schema matches `UserSyncDocument` and `sync.ts`.

## 2. Backend: gap reads in `getSharedCalendar`

- [x] 2.1 Accept an optional `since` (ISO-8601) in `functions/src/sharedCalendar.ts`.
  - **With `since`:** query group events by `yearMonth in` the window's months and `updateAt > since`.
    Personal events use the same filter. One-offs use the overlap filter plus `updateAt > since`.
    Classify each document as an upsert, a removal (the owner is among its holders but it is no
    longer theirs, not deleted, or not in range) or dropped.
  - **Always:** return `cursor`, the maximum `updateAt` seen, and the `removed*Ids` lists. The
    lookups stay whole.
  - Update the KDoc ("Nothing is stored").

  Verify that `npm run build` passes. Behaviour is proven by 7.2's live steps.
- [x] 2.2 Confirm the gap queries run on the existing indexes in `firestore.indexes.json`, with no
  "index required" error in the emulator log during 7.2. Add an index only if one is missing.
- [x] 2.3 Check whether `firebase/test/fixtures` seeds `sync/updates` for users. If it does, add
  `groupEvents` where the seeded events need it. Verify that the fixture loader still passes.

## 3. Core: storage and audit plumbing

- [x] 3.1 Generalise `expect fun Scope.preferencesFilePath(fileName: String)` in common, Android and iOS.
  - Keep `PREFERENCES_FILE` for the settings store and add `SHARED_CALENDARS_FILE =
    "turnia.shared_calendars.preferences_pb"`.
  - Update `DataModule` to `preferencesFilePath(PREFERENCES_FILE)` and add
    `single(named(SHARED_CALENDARS_STORE))`.

  Verify that `./gradlew :core:compileKotlinIosSimulatorArm64 :core:compileAndroidMain`
  passes and that onboarding still reads from the old file.
- [x] 3.2 Add read-only `FirestoreAudit.callsTo(name): Int` and `FirestoreAudit.usage(tag, operation):
  FirestoreUsage`. Verify that core compiles. They are used by 7.2.
- [x] 3.3 Add `UserSyncFirestore.observeShared(ownerId)`: the same shared, tracked listener as
  `observe`, reported under the operation key `sharedSync(snapshots)`. Verify that core compiles and
  that the audit summary shows the new key once 7.2 runs.

## 4. Core: the cache and the gap merge

- [x] 4.1 Extend the DTOs in `SharedCalendarRequest.kt`:
  - `since: String? = null` on the request;
  - `cursor`, `removedGroupEventIds`, `removedPersonalEventIds` and `removedPersonalOneOffEventIds`
    on `SharedCalendarResponse`, with defaults so old answers still decode.

  `SharedCalendarFunction.getSharedCalendar` takes `since`. Verify that core compiles.
- [x] 4.2 Add `SharedCalendarCache` in `data/sharedcalendar/` over the qualified DataStore. It has:
  - the key `viewer|owner|yyyy-MM`;
  - the entry `CachedSharedCalendar(response, cursor, seen)`, stored as lenient JSON;
  - the operations `read`, `merge(key, gap, seen)` and `removeOwner(viewer, owner)`.

  `merge` drops removals, upserts by id, replaces the lookups and keeps the maximum cursor. An
  undecodable value reads as missing. Verify that core compiles. Behaviour is proven by 7.2's cache
  checks.

## 5. Core: the repository

- [x] 5.1 Change `SharedCalendarRepository` to `sharedCalendar(ownerId, month: YearMonth): Flow<Outcome<…>>`.
  - Implement design Decision 6 in `SharedCalendarRepositoryImpl`, taking `fetch`, `markers`
    (`UserSyncFirestore::observeShared`), `viewerId`, `cache` and `mapper` in its constructor.
  - The window is `firstDay − 14 .. lastDay + 14`.
  - The relevant markers are `groupEvents`, `personalEvents` and `personalOneOffEvents` for months
    m−1..m+1, plus `personalEventTypesUpdatedAt`.
  - Add `groupEvents` to `UserSyncDocument`.
  - Wire it in `DataModule`.

  Verify that the module compiles.
- [x] 5.2 Rewrite the KDoc on `SharedCalendar` ("Read whole and not stored") to describe the
  per-month device cache and the gap catch-up. Verify it is in English and says why, not what.

## 6. App: ViewModel and demo

- [x] 6.1 Make `monthDate` a `YearMonth` in `ExternalCalendarViewModel`, reduced with
  `distinctUntilChanged()`.
  - `onMonthChanged(date)` maps the date to its month.
  - The personal branch collects `sharedCalendarRepository.sharedCalendar`.
  - `loading` is set on a month change and cleared on the first emission.
  - A `Failure` sets `userMessage` and emits empty only when nothing is on screen.
  - Rewrite the `events` KDoc ("a colleague's cannot").

  Verify that `./gradlew :app:shared:compileDebugKotlinAndroid` passes.
- [x] 6.2 Update `DemoSharedCalendarRepository` to the new API: `flowOf` the month's window. Verify
  the demo build compiles and shows a colleague's shifts.

## 7. E2E: the proof

- [x] 7.1 Add `showPreviousMonth()` to `CalendarRobot`. It uses the `Previous month` content
  description and updates `shown`. Verify it is used by 7.2.
- [x] 7.2 Add a test to `SharedCalendarFlowsTest`, signed in as alice with fixture `base`. It runs
  design Decision 8's table step by step, asserting each count as a difference from the previous
  step:
  - `FirestoreAudit.callsTo("getSharedCalendar")`;
  - server reads under `usage("UserSyncFirestore", "sharedSync(snapshots)")`;
  - the entries in the cache DataStore, read through Koin: the key exists, the cursor moves, the
    untouched events are kept, and `e2` is removed after the transfer.

  It ends by checking that the rules deny alice's own write of `groupEvents`. Verify that
  `./gradlew :app:androidApp:connectedDebugAndroidTest` passes with no regression in the existing
  shared-calendar tests.

## 8. Docs and integration checks

- [x] 8.1 Update `firebase/firestore-usage.md`:
  - add the row "Open a colleague's calendar": 0 calls when cached and unchanged, at most 1 marker
    read per 30 min, and 1 gap call per change while open, returning only the changed docs;
  - add the trigger's cost per group event write (1 invocation plus at most 2 marker writes).

  Verify the numbers match what 7.2 asserts.
- [ ] 8.2 Check on a device:
  1. Install the debug build.
  2. Walk People → a colleague → next month → the month after.
  3. `adb shell am force-stop com.geoviksoft.turnia.debug`.
  4. Repeat the walk while running `adb logcat -s FirestoreAudit`.

  Verify that no `getSharedCalendar - FUNCTION CALL` line appears and the days paint at once. Then
  have the colleague change a shift while the calendar is open, and verify exactly one call and
  that the change shows.
- [x] 8.3 Commit as a single one-sentence commit with no attribution. Verify with `git log -1`.
