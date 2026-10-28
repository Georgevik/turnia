# Tasks

## 1. Offline tests load `e4` before cutting the network

- [x] 1.1 In `OfflineFlowsTest.offline_deletingAnEvent_hidesItAtOnce`, call
  `calendar.awaitDayShows(world.day(6), "MN")` after `calendar.awaitMain()` and before
  `Network.offline { }`. Verify the test passes locally against the emulators with
  `./gradlew :app:androidApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.geoviksoft.turnia.e2e.flows.OfflineFlowsTest#offline_deletingAnEvent_hidesItAtOnce`
  on a day when `today + 6` is in the next month (true through Oct 31, 2026).
- [x] 1.2 In `OfflineFlowsTest.offline_writingANoteOnAGroupShift_showsItAtOnce`, call
  `calendar.awaitEvent("e4")` after `calendar.openDay(world.day(6))` and before `Network.offline { }`.
  Verify it passes with the same command, targeting this test.

## 2. `onNewToken` does not start the user session while signed out

- [x] 2.1 Before changing the app, confirm the race: run
  `CalendarFlowsTest#calendar_showsTheSeededMonth` locally several times (e.g. 10) and capture
  `adb logcat` for `FirebaseMessaging`, `UserRepository` and the auth state, to see `onNewToken`
  arriving before `E2eRule` signs in on at least one failing run. If it never reproduces locally,
  record that and rely on 2.3 for the evidence.
  *Result:* 10/10 passed locally — the local emulator never gets a token (Firebase Installations
  answers "service unavailable"), so `onNewToken` never runs. CI's per-test logcats show no such
  error. Reproduced deterministically instead by calling `TurniaMessagingService().onNewToken(…)` in
  `E2eRule.before()` ahead of the sign-in (temporary, not kept): the test failed with
  "A restored session is no login", exactly as on CI.
- [x] 2.2 In `TurniaMessagingService.onNewToken`, return when `Firebase.auth.currentUser` (GitLive)
  is `null`, before `users` is first read, keeping the `loggedUser` check after it. Verify
  `./gradlew :app:androidApp:assembleDebug` builds, and that `calendar_showsTheSeededMonth` and
  `AuthFlowsTest` (which asserts `login` and `sign_up`) pass locally.
- [x] 2.3 Run `calendar_showsTheSeededMonth` the same number of times as in 2.1 with the fix in place.
  Verify it passes every time. If it still fails, stop: the cause in `design.md` is wrong, and the
  change goes back to design rather than onto the known-flaky list.
  *Result:* 10/10 passed, and the forced early `onNewToken` from 2.1 passes too with the fix.

## 3. CI

- [ ] 3.1 Push the branch and run the `AndroidE2E` workflow on it from Actions. Verify the job is
  green with neither test needing the retry (no 🔁 next to either in the job summary).
