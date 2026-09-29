# Proposal

## Why

Firestore and Authentication App Check enforcement were switched on directly in the Firebase Console on 2026-09-29 with no staged rollout. This crashed the Android app in production — three Firestore listener flows had no error handling, so an App Check rejection propagated as an uncaught exception — and it blocked Google Sign-In outright on the device used to verify the rollout, because that device's build was sideloaded (`adb install`) rather than installed through Google Play, and Play Integrity attestation fails by design for a non-Play install. Both services were rolled back to monitoring-only the same day. Cloud Functions has never had App Check enforcement wired in code at all, so its console metrics are empty. Turnia needs a documented, staged rollout — with the right build distribution and verification gates at each step — so enforcement can be turned on for good without repeating this incident.

## What Changes

- Define a staged rollout order for App Check enforcement — Cloud Functions, then Firestore, then Authentication — with an explicit verification gate before advancing to the next service, replacing the ad hoc "flip the toggle in Console" approach used on 2026-09-29.
- Require that Authentication enforcement is verified against a **Play Store–distributed build** (internal testing track at minimum) on a real device, not a locally sideloaded APK, since Play Integrity cannot attest a non-Play install and this failure mode is invisible in App Check's monitoring metrics.
- Treat the crash-safety fix already made in `GroupEventFirestore.kt`, `PersonalEventFirestore.kt`, and `PersonalOneOffEventFirestore.kt` (each of their listener flows now degrades to an empty list and logs instead of propagating an uncaught exception on `PERMISSION_DENIED`) as a **release prerequisite**: it must ship to both stores before Firestore or Authentication are re-enforced.
- Deploy the already-written Cloud Functions change (`enforceAppCheck: true` added to all 11 `onCall` functions in `firebase/functions/src/{account,events,membership,invitations,users,sharedCalendar}.ts`) as its own discrete, verified step — Cloud Functions has no monitoring-only mode, so enabling it is immediate enforcement on deploy, unlike the Firestore/Authentication console toggle.
- Define a monitoring window and explicit rollback procedure per service (mirroring the rollback already performed today), so a bad flip is reverted deliberately instead of discovered by a support report.
- Update the App Check paragraph in `CLAUDE.md` to describe the actual, current enforcement state once the rollout finishes, instead of a snapshot that goes stale the moment the state changes (as happened today, when the doc said "enforced" after the Console had already been reverted).

## Capabilities

### New Capabilities

- `app-check-enforcement`: defines the staged rollout order, per-step verification gates (including the Play-distribution requirement for Authentication), monitoring windows, and rollback procedure for turning on Firebase App Check enforcement (Cloud Functions, Firestore, Authentication) in production.

### Modified Capabilities

None — no existing spec in `openspec/specs/` covers Firebase backend security configuration or App Check.

## Impact

- `firebase/functions/src/*.ts` — deploy of the already-coded `enforceAppCheck: true` change (11 `onCall` functions).
- `core/src/commonMain/kotlin/com/geoviksoft/turnia/core/data/datasource/firestore/{GroupEventFirestore,PersonalEventFirestore,PersonalOneOffEventFirestore}.kt` — crash-safety fixes that gate Firestore/Auth re-enforcement; already in the working tree, need a release to reach real devices.
- Firebase Console App Check configuration (Cloud Functions, Cloud Firestore, Authentication APIs) — the enforcement toggles this rollout sequences and verifies.
- Google Play Console (internal testing track) — required distribution channel for a build that can produce a valid Play Integrity verdict; sideloaded builds cannot be used for Authentication verification.
- `CLAUDE.md` — the App Check paragraph, updated to the final state once the rollout completes.
- Android release process and TestFlight — both are verification touchpoints per stage of the rollout.
