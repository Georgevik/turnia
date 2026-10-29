# Proposal

## Why

The Android E2E job on `main` is red since PR #15 (run 40657377744), which blocks every release:
two tests fail on the first attempt and again on the retry, and neither is on the known-flaky list.
One is a new test that races the calendar's month loading and loses on a slow runner when
`today + 6` falls in the next month; the other is an older flake the retry used to hide (run
40657250987, before PR #15, failed it once too).

## What Changes

- `OfflineFlowsTest#offline_deletingAnEvent_hidesItAtOnce` waits until `e4` is on screen before it
  cuts the network. Today it goes offline straight after `awaitMain()`; when `today + 6` is in the
  next month (the last days of any month), that month may not have arrived from the server yet, the
  cache has no `e4`, and the row's "Delete event" action never appears. A fast local machine wins
  the race; the CI runner lost it twice in a row.
- `OfflineFlowsTest#offline_aMonthTheDeviceNeverSynced_keepsTheRestOfTheCalendar` gets the same
  wait. It waits online only for `day(2)`, a personal event, which says nothing about group events:
  they load separately, and on the branch's first CI run they had not arrived when the network was
  cut, so the relaunched calendar never showed `e4` (it passed on the retry).
- `offline_writingANoteOnAGroupShift_showsItAtOnce` is left as it is: it opens `today + 6` online
  before going offline, which gives the month time to load, and it has never failed on CI.
- `TurniaMessagingService.onNewToken` returns before touching `UserRepository` when nobody is
  signed in. With the orchestrator clearing app data before each test, FCM issues a new token at
  process start; reading `users.loggedUser` there creates `UserRepositoryImpl`, whose auth listener
  can then see "signed out" before `E2eRule` signs alice in, and logs `login` for what the test treats
  as a restored session. That makes `CalendarFlowsTest#calendar_showsTheSeededMonth` fail at random.
- `CalendarRobot.showMonthOf` pages back as well as forward. Seeded dates a few days apart straddle a
  month boundary at the end of every month; on Oct 29 `reconnecting_uploadsEveryOfflineAdd_exactlyOnce`
  went from `day(3)` (Nov 1) back to `day(1)` (Oct 30) and the forward-only robot refused.
- Every offline test that adds a shift waits, online, for a seeded personal shift (`day(2)`, "CUR")
  and a seeded group shift (`day(3)`, "MN") to show first. The add pane offers a type only once
  its type has reached the cache; a negative check such as "`day(1)` shows no MN" passes before
  anything has loaded, which is how `offline_addingAGroupShift_showsItAtOnce` went offline with the
  group document still missing and never found the "MN" chip (run 40919916714).
- No production behavior changes: a token that arrives while signed out was already dropped, and a
  real sign-in still logs `login` once the UI starts the repository.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. `analytics-events` already requires that a restored session logs no `login`; this change makes
the suite test that reliably. The change sets `skip_specs: true`.

## Impact

- `app/androidApp/src/androidTest/.../e2e/flows/OfflineFlowsTest.kt` — offline tests wait for what
  they need to be cached before going offline.
- `app/androidApp/src/androidTest/.../e2e/robots/CalendarRobot.kt` — pages in both directions.
- `app/androidApp/src/main/kotlin/com/geoviksoft/turnia/TurniaMessagingService.kt` — early return
  on `Firebase.auth.currentUser == null`.
- CI: the Android E2E job, and with it the Release workflow, goes green again. No Firestore,
  fixture, rules or Cloud Functions changes.
