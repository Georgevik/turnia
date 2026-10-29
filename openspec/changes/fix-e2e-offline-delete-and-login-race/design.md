# Design

## Context

See proposal.md - Why.

- **E2E process model.** The suite runs under the test orchestrator with `clearPackageData`, so every
  test gets a fresh process with empty app data. `TurniaTestApplication` starts Koin in `onCreate`,
  before `E2eRule.before()` seeds the emulators and signs the user in with
  `Firebase.auth.signInWithEmailAndPassword`. Only then is `MainActivity` launched.
- **When `login` is logged.** `UserRepositoryImpl.init` collects `auth.authStateChanged`; a user
  that follows a `null` in the same process counts as a sign-in and logs `AnalyticsEvent.Login`
  (`UserRepositoryImpl.kt:121`). The repository is a Koin `single`, created on first injection.
- **The early injection.** `TurniaMessagingService` injects `UserRepository` lazily and reads
  `users.loggedUser` in `onNewToken`. On a fresh install (every test, here) the CI image
  (`google_apis`, which ships Play services) issues a token shortly after process start. If that
  lands before `before()` finishes signing in, the repository is created signed-out, sees `null`
  and then alice, and logs `login`.
- **Offline month loading.** The calendar fetches a month from Firestore when it is shown or paged
  next to; offline, an unfetched month is served from an empty cache. `e4` is seeded on
  `today + 6`, which is in the next month from the 25th or 26th onward.

## Goals / Non-Goals

**Goals:**
- Make both failing tests pass on every calendar day, not only most of the month.
- Remove the start-up path that creates `UserRepository` while nobody is signed in.

**Non-Goals:**
- Changing when or how `login`, `sign_up` or the FCM token registration happen in production.
- Auditing every E2E test for month-boundary sensitivity by hand. The robot handles both directions
  instead, which covers every test that reaches a date through it.
- `offline_writingANoteOnAGroupShift_showsItAtOnce` keeps its current shape: it opens the day
  online first and has never failed on CI.
- Adding either test to `.github/known-flaky-e2e-tests.txt`. Both have a cause that can be fixed.

## Decisions

**1. Wait for `e4` online, in the test, rather than prefetch in the app.**
`offline_deletingAnEvent_hidesItAtOnce` calls `calendar.awaitDayShows(world.day(6), "MN")` before
`Network.offline { }`; `offline_aMonthTheDeviceNeverSynced_keepsTheRestOfTheCalendar` makes the
same call before `Network.goOffline()`, next to its existing wait for `day(2)` — that one is a
personal event, and group events load separately. Both tests are about what
happens to an event the device *already has* when it goes offline, so loading it first is the
precondition they meant to state.
The tests that add a shift offline share `awaitSeedHeld()`: a seeded personal shift on `+2` and a
group shift on `+3` must show before the network goes off. A shift renders only once its type is
known, so the two prove the personal types, the group document and both event readers have reached
the cache. A negative check ("`+1` shows no MN") proves nothing, since it holds before anything loads.
*Alternative:* pin the fixture date so `+6` never crosses a month. Rejected: the fixtures are
relative to today on purpose, and the month boundary is a case the calendar has to handle anyway.

**2. Let the robot page backward.**
`CalendarRobot.showMonthOf` taps "Previous month" when the target is earlier than the month shown.
The forward-only rule made the result depend on the calendar day: a test reading `+1` after `+3`
passes on most days and fails on the two days where they fall in different months.
*Alternative:* reorder each test so it only ever reads later dates. Rejected: it has to be redone for
every new test, and the order a test reads in is part of what it checks.

**3. Guard `onNewToken` on `Firebase.auth.currentUser`, in the app, rather than in the harness.**
`onNewToken` returns when `Firebase.auth.currentUser` is `null`, before the lazy `users` is
resolved. The repository then starts only once the UI asks for it, by which time the test has signed
in, and the first emission it sees is alice: a restored session, as `analytics-events` requires.
Production is unchanged: with nobody signed in, `loggedUser` was already `null` and the method
already returned. It also stops a background token refresh from starting a Firestore-backed listener
for nobody.
*Alternatives:*
- Clear `RecordingAnalytics` after sign-in in `E2eRule`. Rejected: it hides the event instead of
  preventing it, and would equally hide a real `login` bug.
- Replace `TurniaMessagingService` in the test manifest. Rejected: the app path is the actual
  problem, and the test would then no longer exercise the real service.
- Inject `FirebaseAuth` through Koin. Rejected: it is not registered as a Koin definition today
  (`DataModule` passes `Firebase.auth` in directly), and adding one for this check is more code
  than the check.

## Risks / Trade-offs

- [The `login` cause cannot be observed locally: the local emulator never obtains an FCM token, and
  CI's per-test logcat starts after the process has already started] → Reproduced by forcing the
  CI ordering (calling `onNewToken` before the sign-in): it fails without the fix and passes with
  it. Task 3.1 confirms on CI.
- [Another start-up path could create `UserRepository` before sign-in] → A search found only
  `TurniaMessagingService` on Android; `PushTokenBridge` is iOS-only and the E2E suite is Android.
- [A token issued while signed out is never registered] → Unchanged from today, and
  `gatherUserInfo` registers the current token on every sign-in.
