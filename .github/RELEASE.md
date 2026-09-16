# Release pipeline

[`workflows/release.yml`](workflows/release.yml) builds both apps in release and uploads them:
Android to Play's **internal testing** track, iOS to **TestFlight**. Run it from
GitHub → **Actions** → **Release** → **Run workflow**, typing the version name and choosing `both`,
`android` or `ios`.

Promoting a build to production is still done by hand, in Play Console and App Store Connect.

Before either app is built for upload, the `android-e2e` job runs the E2E suite on an Android
emulator against the Firebase emulators ([firebase/test/README.md](../firebase/test/README.md)). It
runs whatever platforms were chosen, needs no secrets, and a failure stops both uploads. Its report
is attached to the run as the `android-e2e-report` artifact, and every failure message ends with
what was on screen at that moment.

## Versions

- The **version name** users see (`1.04`) is typed when the workflow is run, and set as
  `-Pturnia.versionName` on Android and `MARKETING_VERSION` on iOS. It must be up to three
  period-separated integers, which is all App Store Connect accepts; the first job checks it before
  anything is built. The values in the repo only apply to local builds.
- The build number is the workflow's run number plus `BUILD_NUMBER_OFFSET` (100), passed as
  `-Pturnia.versionCode` on Android and `CURRENT_PROJECT_VERSION` on iOS. Both stores refuse a
  build number they have already seen, and this one never repeats. The offset keeps it past every
  build uploaded by hand before the pipeline existed.

## Secrets

Repository → Settings → Secrets and variables → **Actions**.

### Android

| Secret | What it is |
|--------|------------|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | The upload keystore, base64: `base64 -i upload.jks \| pbcopy` |
| `ANDROID_UPLOAD_STORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `ANDROID_UPLOAD_KEY_ALIAS` | `keyAlias` from `keystore.properties` |
| `ANDROID_UPLOAD_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON key of a Google Cloud service account invited in Play Console |

The service account: Google Cloud console → IAM → Service accounts → create one and add a JSON key.
Then Play Console → **Users and permissions** → invite its email, with *Release apps to testing
tracks* for Turnia. Play only accepts API uploads once the app has had one bundle uploaded by hand.

### iOS

| Secret | What it is |
|--------|------------|
| `ASC_KEY_ID` | Key ID of an App Store Connect API key |
| `ASC_ISSUER_ID` | Issuer ID shown above the keys list |
| `ASC_KEY_P8` | The contents of the downloaded `AuthKey_XXXX.p8` |

App Store Connect → **Users and Access** → **Integrations** → **App Store Connect API** → generate
a team key with the **Admin** role. Admin is what lets Xcode create the distribution certificate
and profile on the runner (cloud signing), so no certificate or profile is stored anywhere. The
`.p8` can only be downloaded once.
