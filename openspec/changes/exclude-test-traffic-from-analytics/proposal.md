# Proposal

## Why

Google Analytics counts traffic that no real user produced, so installs, active users and funnels
are inflated. The E2E suite is the worst source: it runs the debug build with the app's data
cleared before every test, so the Firebase SDK logs a fresh `first_open`, `session_start` and
`screen_view` per test — on every standalone run and on every release, whose `android-e2e` job runs
the same debug build. Development runs on Android and iOS add more, iOS ones straight into the
production stream (Debug and Release share one bundle id and one `GoogleService-Info.plist`), and
every upload to Play's internal track is crawled by the pre-launch report's robots on Test Lab
devices, which report as real users of the release build.

The custom events are already kept out of E2E (the suite replaces `Analytics` with a recorder); what
leaks is the SDK's own automatic collection, which starts with the process, before any app code.

## What Changes

- **Debug builds never collect analytics**, on either platform, and nothing can turn it back on at
  runtime. This covers development devices, simulators and the whole E2E suite, locally and on CI.
- **Release builds start with collection off** and the app turns it on at launch, unless it is
  running on a Firebase Test Lab device — which is where Play's pre-launch report runs. Nothing is
  collected before that decision, not even the first launch's `first_open`.
- Real users of release builds are unaffected: the same automatic and custom events are collected
  as today.
- The app's own analytics calls do not change; under a disabled collection the SDK drops them.

## Capabilities

### New Capabilities
- `analytics-collection`: which builds and devices report to Google Analytics, and the guarantee
  that test and development traffic never reaches it.

### Modified Capabilities

## Impact

- **Android**: the debug and main `AndroidManifest.xml` (collection flags), `TurniaApplication`
  (the Test Lab check at launch), and a native `firebase-analytics` dependency on `androidApp`
  through the existing Firebase BOM, as `firebase-messaging` and `firebase-crashlytics` already are.
- **iOS**: `Info.plist` and `Configuration/Config.xcconfig` (a per-configuration flag, following the
  AdMob ids already set with `[config=Debug]` / `[config=Release]`).
- **Not affected**: `AnalyticsImpl`, the E2E `RecordingAnalytics`, Crashlytics, Firestore, the CI
  workflows.
- **Out of scope**: TestFlight and internal testers, App Review, and cleaning data already in GA.
  Past Android debug traffic can still be excluded by leaving out the `.debug` stream; past iOS
  debug traffic cannot be told apart.
- **Docs**: `CLAUDE.md` gains a line on which builds report to Analytics.
