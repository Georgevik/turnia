# Turnia

Shift management and swapping for healthcare teams, on Android and iOS.

Shift swaps are usually arranged over WhatsApp, and chained swaps (A → B → C) make it easy to lose
track of who actually covers a shift. Turnia keeps a single source of truth for every shift and an
append-only history of every transfer, so the chain can always be traced.

- **Groups** with their own event types, invitation links and join requests.
- **Swaps**: offer a shift, let a colleague take it, and give it back if needed.
- **Shared calendars** between colleagues, across groups.
- **Personal events** alongside group shifts.
- **Push notifications** for everything that affects your schedule.
- Free with **AdMob** banners (behind Google's consent message in the EEA), with premium
  subscriptions planned.

## Tech stack

- **Kotlin Multiplatform** and **Compose Multiplatform**: shared logic and UI for Android and iOS.
- **Firebase** as the whole backend (Firestore, Auth, Cloud Functions, FCM, Crashlytics, App Check,
  Remote Config, Hosting), used from shared code through the GitLive SDK.
- **Koin**, **Navigation 3**, **Coroutines/Flow**, **kotlinx.serialization**.
- **Google Mobile Ads** and the **User Messaging Platform** for ads and consent.

## Project structure

```
turnia/
├── app/
│   ├── androidApp/   Android entry point
│   ├── shared/       Shared Compose UI (commonMain / androidMain / iosMain)
│   └── iosApp/       iOS entry point (Xcode project, Swift bridges for the Google SDKs)
├── core/             Domain and data layer shared by both apps
├── firebase/         Firestore rules and indexes, Cloud Functions (TypeScript), Hosting
├── store/            Store listing texts and screenshot generation
└── .github/          Release pipeline
```

## Getting started

### Requirements

- JDK 17 or newer
- Android Studio with the Kotlin Multiplatform plugin
- Xcode, to build the iOS app (macOS only)
- Node.js and the Firebase CLI, only to work on the backend

### Run the apps

- **Android:** `./gradlew :app:androidApp:assembleDebug`, or the run configuration in Android
  Studio.
- **iOS:** open [`app/iosApp`](app/iosApp) in Xcode and run the `iosApp` scheme. Xcode builds the
  shared Kotlin framework through Gradle in a build phase.

Debug builds use Google's test ad units and a Firebase App Check debug token. The token is printed
on first launch and has to be registered once per device in the Firebase console.

### Tests

```bash
./gradlew :core:allTests
```

### Backend

Run Firebase commands from [`firebase/`](firebase), where `firebase.json` lives:

```bash
cd firebase/functions && npm install && npm run build
cd firebase && firebase deploy --only functions,firestore:rules,firestore:indexes
cd firebase && firebase deploy --only hosting
```

## Releasing

Releases go out through a manual GitHub Actions workflow: **Actions → Release → Run workflow**,
typing the version name (e.g. `1.04`). It uploads Android to Play's internal testing track and iOS to
TestFlight, with the same version name on both and a build number it sets itself. Promoting a build
to production is done in each store's console.

The secrets the workflow needs, and how to create them, are in [.github/RELEASE.md](.github/RELEASE.md).

To build a signed Android bundle locally, copy
[`keystore.properties.example`](keystore.properties.example) to `keystore.properties` and run
`./gradlew :app:androidApp:bundleRelease`.

## Documentation

- [CLAUDE.md](CLAUDE.md): domain concepts, business rules, architecture and code conventions.
- [firebase/firestore-schema.md](firebase/firestore-schema.md): collections, fields, security
  rules and invariants.
- [firebase/firestore-usage.md](firebase/firestore-usage.md): expected Firestore cost per action and
  per user.
- [.github/RELEASE.md](.github/RELEASE.md): release pipeline setup.

## License

To be defined.
