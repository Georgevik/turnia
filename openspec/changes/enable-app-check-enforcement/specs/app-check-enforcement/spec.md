# Spec Delta

## Purpose

Defines the staged rollout order, per-step verification gates, monitoring windows, and rollback procedure required to enable Firebase App Check enforcement (Cloud Functions, Cloud Firestore, Authentication) in production without breaking legitimate client traffic.

## ADDED Requirements

### Requirement: Staged enforcement order
App Check enforcement SHALL be enabled one service at a time, in the order Cloud Functions, then Cloud Firestore, then Authentication. A service SHALL NOT be enforced until the previous service in the order has passed its verification gate.

#### Scenario: Firestore enforcement blocked until Functions is verified
- **WHEN** Cloud Functions enforcement has not yet passed its verification gate
- **THEN** Cloud Firestore enforcement SHALL NOT be enabled in the Firebase Console

#### Scenario: Authentication enforcement blocked until Firestore is verified
- **WHEN** Cloud Firestore enforcement has not yet passed its verification gate
- **THEN** Authentication enforcement SHALL NOT be enabled in the Firebase Console

### Requirement: Crash-safety release prerequisite
Firestore and Authentication enforcement SHALL NOT be enabled until a build containing the Firestore listener crash-safety fix (graceful degradation to an empty result on `PERMISSION_DENIED` in `GroupEventFirestore`, `PersonalEventFirestore`, and `PersonalOneOffEventFirestore`) has reached a released build on both the Google Play track used for verification and TestFlight.

#### Scenario: Enforcement attempted before the fix ships
- **WHEN** the crash-safety fix has not yet reached a released build on both platforms
- **THEN** Firestore and Authentication enforcement SHALL remain in monitoring-only mode

### Requirement: Authentication verification requires a Play-distributed build
Before Authentication enforcement is enabled, sign-in SHALL be verified end-to-end on an Android device running a build installed through the Google Play internal testing track (or a higher track), not a sideloaded build, because Play Integrity cannot attest an install that did not go through Google Play.

#### Scenario: Sideloaded build cannot be used to verify Authentication
- **WHEN** the only available Android build was installed via `adb install` rather than Google Play
- **THEN** that build SHALL NOT be treated as sufficient verification to enable Authentication enforcement

#### Scenario: Play Integrity attestation succeeds on a Play-installed build
- **WHEN** a build installed through the Google Play internal testing track signs in with Google
- **THEN** the sign-in SHALL succeed without a "Firebase App Check token is invalid" error

### Requirement: Cloud Functions enforcement verification
Cloud Functions App Check enforcement SHALL be verified, after `enforceAppCheck: true` is deployed, by confirming that each of the 11 `onCall` functions still succeeds when called from a genuine app build on both platforms, and rejects calls that carry no valid App Check token.

#### Scenario: Genuine client call succeeds after Functions enforcement
- **WHEN** a genuine app build with a valid App Check token calls any of the 11 `onCall` functions after enforcement is deployed
- **THEN** the call SHALL succeed as it did before enforcement was enabled

#### Scenario: Unattested caller is rejected
- **WHEN** a caller without a valid App Check token invokes one of the 11 `onCall` functions after enforcement is deployed
- **THEN** the function SHALL reject the call

### Requirement: Per-step monitoring window and rollback
Each enforcement step SHALL be observed for a defined monitoring window after being enabled, and SHALL be reverted to monitoring-only if legitimate traffic is rejected during that window.

#### Scenario: Legitimate failures observed during the monitoring window
- **WHEN** Crashlytics or Cloud Functions logs show a legitimate, up-to-date client being rejected after an enforcement step is enabled
- **THEN** that service's enforcement SHALL be reverted to monitoring-only before the rollout proceeds further

#### Scenario: No legitimate failures observed
- **WHEN** the monitoring window for a step elapses with no legitimate traffic rejected
- **THEN** the rollout SHALL proceed to the next service in the staged order

### Requirement: Documentation reflects actual enforcement state
`CLAUDE.md`'s App Check section SHALL describe the actual, current enforcement state of Cloud Functions, Firestore, and Authentication, and SHALL be updated in the same change whenever that state changes.

#### Scenario: Documentation updated after a state change
- **WHEN** an enforcement toggle is changed in the Firebase Console, enabled or rolled back
- **THEN** `CLAUDE.md`'s App Check section SHALL be updated to match in the same change
