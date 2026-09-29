# Design

## Context

See proposal.md - Why for the 2026-09-29 incident this rollout is designed to prevent from repeating.

Relevant current state:
- Firestore and Authentication App Check are currently in **monitoring-only** mode (rolled back today). Cloud Functions has no App Check wiring deployed at all.
- The crash-safety fix (`.catch` added to the three Firestore listener flows) is in the working tree, uncommitted, undeployed.
- The Cloud Functions `enforceAppCheck: true` change (11 `onCall` functions) is in the working tree, uncommitted, undeployed.
- Firestore and RTDB/Authentication support a genuine "monitor without blocking" console mode with traffic-verified metrics before you flip to enforced. **Cloud Functions does not** — `enforceAppCheck: true` on an `onCall` function is binary and takes effect immediately on deploy, so there is no equivalent metrics-first phase for it. Today's incident happened on the two services that *did* have a metrics-first mode; the console showed 9%/24% "verified" for Firestore/Auth as enforcement was switched on, and that number was misread as acceptable when it actually reflected a device that could not attest at all (see next point).
- Play Integrity (the Android attestation provider App Check uses in release builds) only produces a valid verdict for an app installed through Google Play. A build installed via `adb install` — the normal way to get a locally built release APK onto a test device — always fails attestation, and this failure is indistinguishable in App Check's console metrics from a genuinely malicious or outdated client. This is the root cause of both incidents today (Firestore listener crash and Google Sign-In failure): the verification device could never have passed, regardless of how carefully the console metrics were read.

## Goals / Non-Goals

**Goals:**
- Make it structurally impossible to repeat the exact failure from today: enforcing Authentication against a device/build combination that cannot produce a valid App Check token.
- Give Cloud Functions — which has no monitoring-only phase — a deploy-time verification step that substitutes for the metrics window Firestore/Auth get for free.
- Keep the rollout auditable: each step's gate, verification result, and (if needed) rollback is recorded, not just performed ad hoc in the Console.

**Non-Goals:**
- This does not change what App Check protects against or how the app integrates with it (provider factories, debug tokens, etc.) — that integration already exists and is correct; today's incident was a rollout-process failure, not an integration bug.
- This does not cover App Check for services Turnia does not use (Realtime Database, Storage) — out of scope, not enabled anywhere in this project.
- This does not design the eventual production Play release itself (that follows the existing `.github/RELEASE.md` pipeline) — it only requires that Authentication verification happen against a Play-distributed build, whichever track that is.

## Decisions

**Order: Cloud Functions → Firestore → Authentication.**
This reverses the "Functions first, Auth last" ordering from the original ad hoc plan on the reasoning that Functions has no monitoring phase and its blast radius is scoped to 11 specific operations (join/take/return/delete/etc.), each independently testable end-to-end right after deploy. Firestore is enforced next because its monitoring-only mode is genuinely useful once the crash-safety fix has shipped (a bad flip degrades to an empty calendar instead of crashing). Authentication is last and gated hardest, because a bad flip there locks users out of the app entirely, and because today proved its failure mode (Play Integrity on a non-Play install) is invisible in monitoring metrics — the only way to catch it is the explicit Play-distributed-build check, not more time spent watching the console.

**Play-distributed build is a hard gate for Authentication, not a recommendation.**
Alternative considered: keep watching Console metrics longer before flipping Authentication. Rejected — today's incident is direct evidence that metrics alone (9%/24% "verified") do not catch this failure mode, because the unverified traffic looked like generic noise rather than a systematic attestation failure. Only an explicit end-to-end sign-in test on a Play-installed build catches it.

**Crash-safety fix ships before Firestore/Auth are re-enforced, not concurrently.**
Alternative considered: enforce Firestore/Auth and ship the fix in parallel, racing to get the fix out before real users hit the crash. Rejected — the whole point of today's incident is that this race was already lost once. Sequencing removes the race.

**Cloud Functions verification is manual end-to-end calls, not new automated monitoring.**
Alternative considered: add the manual non-blocking `request.app` logging discussed earlier in the incident (log-only mode before enforcing). Rejected per the decision already made during the incident: given the app is only in internal testing/TestFlight and all current clients already send App Check tokens, the extra monitoring-only code path was judged not worth building for a rollout this narrow (11 functions, small user base). Verification instead means: deploy, then manually exercise each of the 11 functions from a real client on both platforms and confirm success, plus one deliberate call without a valid token (e.g., an old cached build, if available) to confirm it is rejected.

## Risks / Trade-offs

- **[Risk]** No automated regression test exists for the crash-safety fix, so a future refactor could reintroduce an uncaught-exception listener flow. → **Mitigation**: tasks.md includes adding a unit test asserting `GroupEventFirestore.get()` (and the other two) emit an empty list instead of throwing when the underlying Firestore call fails, following the pattern already testable in `GroupSyncFirestore`/`GroupFirestore`.
- **[Risk]** "Play-distributed build" verification depends on the internal testing track already being populated with testers who can install and test promptly; if the track is stale, this step stalls the whole rollout. → **Mitigation**: tasks.md includes confirming at least one tester (can be the developer's own Play-linked device) has the current internal testing build before starting the Authentication step.
- **[Risk]** Cloud Functions enforcement has no rollback via a Console toggle — reverting means redeploying without `enforceAppCheck: true`. → **Mitigation**: keep the pre-enforcement Cloud Functions commit easy to redeploy; document the redeploy command in tasks.md as the explicit rollback for that step.
- **[Trade-off]** Sequencing three independent flips end-to-end (each with its own monitoring window) is slower than flipping all three at once, as happened today. This is accepted deliberately: today's approach caused a production incident in under ten minutes of enforcement.

## Migration Plan

1. Ship the crash-safety fix (already coded) to both platforms via the normal release pipeline.
2. Deploy the Cloud Functions `enforceAppCheck: true` change; verify each of the 11 functions manually; observe Cloud Functions logs for unexpected 401s over the monitoring window.
3. Enable Cloud Firestore enforcement in the Console; observe Crashlytics and Firestore console metrics over the monitoring window.
4. Confirm a tester has the current build via the Google Play internal testing track; verify Google and Apple sign-in succeed on that Play-installed build.
5. Enable Authentication enforcement in the Console; observe Crashlytics and Authentication console metrics over the monitoring window.
6. Update `CLAUDE.md`'s App Check paragraph to the final state.

**Rollback**: at any step, if the monitoring window surfaces legitimate traffic being rejected, revert that step (Console toggle back to monitoring-only for Firestore/Auth; redeploy Cloud Functions without `enforceAppCheck: true`) before investigating further, and do not proceed to the next step until the cause is understood and re-verified.

## Open Questions

- Exact duration of each monitoring window (proposed: 24–48h, matching the original incident-day plan) — can be decided at task time without changing the spec or approach.
