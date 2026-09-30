# Tasks

## 1. Setup

- [x] 1.1 Create branch `features/enable-app-check-enforcement` off `main` and verify it is checked out (`git status`)

## 2. Ship the crash-safety fix

- [ ] 2.1 Add a unit test asserting `GroupEventFirestore.get()` emits an empty list instead of throwing when the underlying Firestore query fails, and verify it fails without the existing `.catch` fix (temporarily revert it locally to confirm the test catches the regression, then restore it) and passes with the fix in place
- [ ] 2.2 Add the equivalent unit test for `PersonalEventFirestore.get()` and verify it passes
- [ ] 2.3 Add the equivalent unit test for `PersonalOneOffEventFirestore.get()` and verify it passes
- [ ] 2.4 Commit the crash-safety fix (already in the working tree) together with the three new tests, and verify `./gradlew :core:compileKotlinMetadata` and the new tests pass
- [ ] 2.5 Release the fix to Android (Play internal testing track, via the normal `bundleRelease` + release pipeline) and verify the new build version is visible in the Play Console internal testing track
- [ ] 2.6 Release the fix to iOS (TestFlight, via the normal release pipeline) and verify the new build is visible in TestFlight
- [ ] 2.7 Verify at least one tester (may be the developer's own device) has installed the new build on each platform, confirming the Play/TestFlight distribution is not stale

## 3. Enforce Cloud Functions

- [x] 3.1 Commit the `enforceAppCheck: true` change already made to the 11 `onCall` functions in `firebase/functions/src/{account,events,membership,invitations,users,sharedCalendar}.ts`
- [x] 3.2 Deploy the functions (`firebase deploy --only functions`) and verify the deploy completes without error
- [x] 3.3 From a genuine app build on Android, exercise at least one representative flow per function family (e.g. `requestToJoinGroup`, `takeEvent`, `returnEvent`, `updateProfile`, `getSharedCalendar`, `leaveGroup`) and verify each still succeeds
- [x] 3.4 Repeat 3.3 on iOS and verify each still succeeds
- [x] 3.5 Confirm rejection behavior: invoke one function from a client without a valid App Check token (e.g. a stale pre-fix build still installed on a spare device, or a direct unauthenticated call) and verify it is rejected
- [x] 3.6 Monitor Cloud Functions logs for 24–48h for unexpected 401/403 responses from real client traffic, and verify none are found before proceeding to section 4 (if any are found, redeploy without `enforceAppCheck: true` and investigate before retrying this section)

## 4. Enforce Cloud Firestore

- [ ] 4.1 Confirm the crash-safety fix build from section 2 is the one active on test devices, and verify by checking the app's version/build number on device against the release from task 2.5/2.6
- [ ] 4.2 Enable Cloud Firestore App Check enforcement in Firebase Console → App Check → APIs
- [ ] 4.3 On the crash-safety-fix build, open the calendar (own, group, and a colleague's) on Android and verify no crash occurs and Crashlytics shows no new `PERMISSION_DENIED`-uncaught-exception crashes
- [ ] 4.4 Repeat 4.3 on iOS and verify the same
- [ ] 4.5 Monitor Crashlytics and the App Check console's Firestore metrics for 24–48h, and verify no legitimate-traffic rejections before proceeding to section 5 (if found, revert Firestore to monitoring-only in Console and investigate before retrying this section)

## 5. Enforce Authentication

- [ ] 5.1 Confirm a tester has the current Google Play internal-testing-track build installed (not sideloaded), and verify by checking the install source on device (`adb shell dumpsys package <package> | grep installerPackageName` should show `com.android.vending`)
- [ ] 5.2 On that Play-installed build, sign in with Google and verify it succeeds with no "Firebase App Check token is invalid" error
- [ ] 5.3 On the iOS TestFlight build, sign in with Google and verify it succeeds with no App Check error
- [ ] 5.4 On the iOS TestFlight build, sign in with Apple and verify it succeeds with no App Check error
- [ ] 5.5 Enable Authentication App Check enforcement in Firebase Console → App Check → APIs
- [ ] 5.6 Repeat 5.2–5.4 after enabling enforcement and verify sign-in still succeeds on all three paths
- [ ] 5.7 Monitor Crashlytics and the App Check console's Authentication metrics for 24–48h, and verify no legitimate sign-in rejections (if found, revert Authentication to monitoring-only in Console and investigate before retrying this section)

## 6. Documentation and wrap-up

- [ ] 6.1 Update the App Check paragraph in `CLAUDE.md` to state that Cloud Functions, Firestore, and Authentication are all enforced, removing the now-stale "Firestore and Authentication are enforced (since 2026-09-29)" wording and describing the actual final date
- [ ] 6.2 Commit the `CLAUDE.md` update
- [ ] 6.3 Open a PR from `features/enable-app-check-enforcement` to `main` summarizing the staged rollout and linking the monitoring results from sections 3–5, and verify the PR description lists the final enforcement state of all three services
