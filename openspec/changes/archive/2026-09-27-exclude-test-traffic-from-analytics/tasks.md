# Tasks

## 1. Android: debug never collects

- [x] 1.1 Add `firebase_analytics_collection_deactivated = true` as `<meta-data>` in `app/androidApp/src/debug/AndroidManifest.xml`, with a one-line comment on why (E2E clears data per test, so every test is a new install); verify `./gradlew :app:androidApp:processDebugMainManifest` succeeds and the merged manifest under `build/intermediates/merged_manifests/debug` carries the key
- [x] 1.2 Install the debug build, enable SDK logging with `adb shell setprop log.tag.FA VERBOSE` and `adb shell setprop log.tag.FA-SVC VERBOSE`, open the app and verify `adb logcat -s FA FA-SVC` reports collection deactivated and uploads no events

## 2. Android: release is off until the app decides

- [x] 2.1 Add a `firebase-analytics-android = { module = "com.google.firebase:firebase-analytics" }` alias to `gradle/libs.versions.toml` and `implementation(libs.firebase.analytics.android)` to `app/androidApp/build.gradle.kts` next to `firebase-messaging-android`; verify `./gradlew :app:androidApp:dependencies --configuration releaseRuntimeClasspath` resolves it to the BOM's version with no conflict
- [x] 2.2 Add `firebase_analytics_collection_enabled = false` as `<meta-data>` in `app/androidApp/src/main/AndroidManifest.xml`, commented with the reason (the SDK starts before `onCreate`); verify the merged release manifest carries it and the debug one carries both keys
- [x] 2.3 In `TurniaApplication.onCreate`, as its own step outside `installFirebase()`, call `Firebase.analytics.setAnalyticsCollectionEnabled(true)` unless `Settings.System.getString(contentResolver, "firebase.test.lab") == "true"`; verify `./gradlew :app:androidApp:assembleDebug :app:androidApp:assembleRelease` builds and task 1.2 still shows collection deactivated on debug
- [x] 2.4 Build a release APK signed with the upload key from `keystore.properties`, install it and verify with the `FA` logcat tags that events upload on a normal launch and appear in GA's Realtime / DebugView (`adb shell setprop debug.firebase.analytics.app com.geoviksoft.turnia`)
- [x] 2.5 On the same install, clear its data, run `adb shell settings put system firebase.test.lab true`, launch again and verify the `FA` logs show no events and no `first_open`; then `adb shell settings delete system firebase.test.lab` and verify a relaunch reports again

## 3. iOS: Debug never collects

- [x] 3.1 Add `ANALYTICS_COLLECTION_DEACTIVATED[config=Debug] = YES` and `ANALYTICS_COLLECTION_DEACTIVATED[config=Release] = NO` to `app/iosApp/Configuration/Config.xcconfig`, commented like the AdMob ids, and `FIREBASE_ANALYTICS_COLLECTION_DEACTIVATED` = `$(ANALYTICS_COLLECTION_DEACTIVATED)` in `app/iosApp/iosApp/Info.plist`; verify the built Debug and Release `Info.plist` (`plutil -p` on the `.app`) carry `YES` and `NO`
- [x] 3.2 Run the Debug configuration with the `-FIRDebugEnabled` launch argument and verify the Xcode console reports Analytics collection as deactivated; if the string value is not honoured, replace 3.1's plist entry with a Debug-only Run Script phase that writes a Boolean `true` with `PlistBuddy`, and verify again
- [x] 3.3 Run a Release build (or an archive on a device) with `-FIRDebugEnabled` and verify the console shows events being logged and they appear in DebugView

## 4. Documentation and integration

- [x] 4.1 Add a line under *Tech stack → Firebase* in `CLAUDE.md` saying debug builds (and so E2E) never report to Analytics and release builds skip Test Lab devices (Play's pre-launch report); verify it names the manifest keys and the xcconfig variable as they were implemented
- [x] 4.2 Run the E2E suite (`./gradlew :app:androidApp:connectedDebugAndroidTest` with the emulators) and verify it passes unchanged, including the tests that assert on `RecordingAnalytics`
- [ ] 4.3 After the next internal-track upload, verify in GA that no `first_open` arrives from the pre-launch report's device models and none from the `.debug` stream after the change's first E2E run
