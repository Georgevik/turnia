# Design

## Context

See proposal.md — Why. The constraint that shapes everything: the Firebase Analytics SDK starts on
its own when the process starts (Android through its content provider, iOS as soon as
`FirebaseApp.configure()` runs), before Koin, before `TurniaApplication.onCreate` has done anything,
and it logs `first_open` right away. So a check that runs in app code can only stop the leak if
collection is already off when the SDK starts.

What the repo already has:
- `AnalyticsImpl` (core, GitLive) sends the custom events; the E2E `e2eModule` replaces it with
  `RecordingAnalytics`, so tests can assert on events and none of them leave.
- Android debug has its own Firebase app (`com.geoviksoft.turnia.debug`), and E2E runs that same
  debug APK, from `android-e2e.yml` and from the Release workflow's `android-e2e` job.
- iOS has one bundle id and one `GoogleService-Info.plist` for both configurations. Its
  `IS_ANALYTICS_ENABLED=false` is a legacy key the SDK does not read.
- `Config.xcconfig` already sets values per configuration (`ADMOB_APP_ID[config=Debug]`), and
  `Info.plist` reads them with `$(…)`.
- `androidApp` pulls native Firebase libraries through the BOM (`firebase-messaging-android`,
  `firebase-crashlytics`); the native Analytics library is not on its compile classpath yet.

## Goals / Non-Goals

**Goals:**
- Stop the traffic on the device, before it is sent, not just filter it out of reports.
- Have no `isDebug` branch in shared or app code: the build type decides through configuration.

**Non-Goals:**
- Changing `AnalyticsImpl`, the `Analytics` interface or the E2E recorder.
- Consent Mode or tying analytics to the ad-consent flow (the release default-off below is the
  shape that would take, but it is not built here).
- A GA4 "developer traffic" filter; it can still be added in the console, and needs no code.

## Decisions

### 1. Debug: `deactivated`, not `enabled=false`

`src/debug/AndroidManifest.xml` sets `firebase_analytics_collection_deactivated = true`; iOS sets
`FIREBASE_ANALYTICS_COLLECTION_DEACTIVATED` to `YES` in Debug and `NO` in Release.

`deactivated` is permanent: `setAnalyticsCollectionEnabled(true)` cannot override it. That is what
lets decision 2 call `enable` unconditionally on non-Test-Lab devices without checking the build
type — in debug the call is a no-op. `enabled=false` was rejected because it would make debug
depend on that runtime code never running.

The debug manifest covers all of E2E for free, because the suite installs the debug APK. An
androidTest-only switch was rejected: the androidTest manifest merges into the test APK, not the
app under test, and the user does not want debug analytics at all anyway.

### 2. Release: off by default, enabled at launch unless on Test Lab

The main `AndroidManifest.xml` sets `firebase_analytics_collection_enabled = false`.
`TurniaApplication.onCreate` then calls `FirebaseAnalytics.setAnalyticsCollectionEnabled(true)`
unless `Settings.System.getString(contentResolver, "firebase.test.lab") == "true"` — the documented
marker Test Lab sets on its devices, and the devices the pre-launch report runs on.

```
                      manifest                     onCreate                result
debug (dev, E2E)      deactivated = true           enable -> ignored       never collects
release on Test Lab   enabled = false              not enabled             never collects
release, real user    enabled = false              enable                  collects as today
```

- **Every launch, not once.** The runtime setting is persisted by the SDK, so a single enable would
  also stick; calling it on every launch keeps the decision in one place and costs a settings read.
  The Test Lab branch never writes `false`, since the manifest default already is `false` and a
  Test Lab device is wiped between runs.
- **Its own step in `onCreate`**, not inside `installFirebase()`: `TurniaTestApplication` overrides
  `installFirebase()` without calling super, and App Check has nothing to do with analytics.
- **Native API in `androidApp`**, through a new `firebase-analytics-android`
  (`com.google.firebase:firebase-analytics`) alias on the BOM, as `firebase-messaging-android`
  already is. Rejected: adding `setCollectionEnabled` to the shared `Analytics` interface — Koin
  starts after the point the call has to happen, and iOS has nothing to call it with.
- **Nothing is lost on a real device.** While collection is off the SDK stores nothing; once enabled
  it logs `first_open` for that install, so a store install still reports its first open.

### 3. iOS: one xcconfig variable, read by `Info.plist`

`Config.xcconfig` gets `ANALYTICS_COLLECTION_DEACTIVATED[config=Debug] = YES` and
`[config=Release] = NO`, and `Info.plist` gets
`FIREBASE_ANALYTICS_COLLECTION_DEACTIVATED = $(ANALYTICS_COLLECTION_DEACTIVATED)`, exactly like the
AdMob ids. No Test Lab check on iOS: the pre-launch report crawls Android uploads only.

`$(…)` substitution only produces a string, and the key is documented as a Boolean. The SDK reads
it with `boolValue`, which accepts `"YES"`, but this is verified in the console log rather than
assumed (see Risks). The legacy `IS_ANALYTICS_ENABLED` in `GoogleService-Info.plist` is left alone:
that file is downloaded from the console and overwritten on refresh.

## Risks / Trade-offs

- [The iOS SDK ignores a string `"YES"`] → Check the Xcode console for Analytics reporting
  collection as deactivated on a Debug run. If it does not, replace the substitution with a Debug-only
  Run Script phase that writes a real `<true/>` into the built `Info.plist` with `PlistBuddy`.
- [Pre-launch robots that do not run on Test Lab] → Google documents the pre-launch report as
  running on Test Lab; anything outside it would still count. Accepted.
- [A release build no longer reports until `onCreate` runs] → Only the time between process start
  and `onCreate`; nothing is lost, since `first_open` is logged once collection is enabled.
- [Someone later reads `BuildConfig.DEBUG` and enables collection "for testing"] → Harmless:
  `deactivated` wins. Checking analytics by hand means a release build with DebugView.
- [Past data stays polluted] → Out of scope; the Android `.debug` stream can be excluded in
  reports, iOS history cannot.

## Migration Plan

Ships with the next release; nothing to migrate. Rollback is removing the manifest and plist keys
and the `onCreate` call. The effect shows in GA as a drop in `first_open` equal to the E2E runs
(about 37 per run) and the pre-launch crawls.
