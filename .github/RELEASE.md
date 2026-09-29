# Releasing Turnia

The **Release** workflow ([`workflows/release.yml`](workflows/release.yml)) builds the app and sends
it to testers: the Android app to Play's **internal testing** track, the iPhone app to **TestFlight**.
It does not publish to the public. Moving a tested build to production is still done by hand, in
Play Console and App Store Connect.

## Before you release

Tick each one off before running the workflow:

1. **The backend is up to date.** If this release needs new or changed server code (Cloud
   Functions, Firestore rules or indexes), deploy it first, from the `firebase/` folder:
   `firebase deploy --only functions,firestore`. The app must never reach users before the server
   it talks to.
2. **The invitation page is up to date.** If anything in `firebase/hosting/` changed:
   `firebase deploy --only hosting`.
3. **Invitation links open the app** *(once, before the first production release)*.
   `firebase/hosting/.well-known/assetlinks.json` must list the SHA-256 of the Play App Signing key
   (Play Console → *App integrity*). Without it, Android users who tap an invitation link see the web
   page instead of the app.
4. **The release notes say what you want.** They are the files in `.github/whatsnew/`, one per
   language, 500 characters at most each. Keep them short and plain.

## Running a release

1. Open GitHub → **Actions** → **Release** → **Run workflow**.
2. Leave **Use workflow from** on `main`. Any other branch is refused.
3. Choose what kind of **release** it is. The workflow works out the new version from the last one:

   | Release | When | Example |
   |---------|------|---------|
   | `hotfix` | Only fixes something that was broken | 1.2.3 → 1.2.4 |
   | `minor` | New features or changes | 1.2.3 → 1.3.0 |
   | `major` | A big change, such as a redesign | 1.2.3 → 2.0.0 |

   The version it chose is shown at the top of the run's summary page.
4. Choose which apps to send: `both`, `android` or `ios`.
5. Leave **Run the Android E2E suite** ticked (see *Skipping the tests* below).
6. Click **Run workflow**. Recent releases have taken 10–20 minutes.

When it finishes green, the builds are with testers and the release is **tagged** (see *Tags*).

## Changing the release notes after a release

The notes in `.github/whatsnew/` go to both stores with every release: to Play with the upload, and
to TestFlight as *What to Test* a few minutes later, once Apple has processed the build.

To change them for a build that is already out, edit the files on `main`, then open GitHub →
**Actions** → **Release notes** → **Run workflow**. Leave the build number empty to update the last
release. On Play the notes go with the release when it is promoted to production. For the App Store,
the public *What's New* text is still typed in App Store Connect when the version is submitted.

## If a run fails partway

Open the failed run and click **Re-run failed jobs**, never **Re-run all jobs**.

Each build gets a number that the stores accept only once. If one app was already uploaded,
*Re-run all jobs* tries to send it again with the same number, the store refuses it, and the run
fails even when everything else works. *Re-run failed jobs* repeats only what failed.

## Skipping the tests

Before uploading, the workflow runs the automated tests on an Android emulator. A test that fails is
retried once, against the same emulators, before the run is failed for real — a test still failing
after that retry is treated as a genuine failure, and nothing is uploaded.

Unticking **Run the Android E2E suite** skips them, and the builds are uploaded untested. The run's
summary page then shows a **Tests skipped** warning. Only do this when the tests fail for a reason
that has nothing to do with the app (the emulator itself misbehaving, for example) and the release
cannot wait. Check the build by hand before promoting it.

The test report is attached to every run as `android-e2e-report`, and each failure ends with what was
on screen at that moment.

## Tags

Once every chosen app has been uploaded, the workflow tags the code it built, such as `v1.2.3-247`:
the version name, then the build number. The tag's message says which apps went out. A tag lets
anyone find exactly the code behind a build, for example when looking into a crash report.

A run that failed gets no tag. The *Re-run failed jobs* that completes it adds the tag.

## Version and build numbers

- The **version name** (`1.2.3`) is what users see. The workflow takes the last release's tag and
  raises the number that matches the kind of release you chose. The value in the code only applies to
  builds made on a developer's machine.
- A run that fails leaves no tag, so running it again gives the same version, not the next one.
- It needs at least one earlier release tag to count from. If none exists, the run stops at once and
  says so. An engineer then creates one by hand, such as `git tag v1.2.3-250 <commit> && git push origin v1.2.3-250`.
- The **build number** is set automatically: the run's number plus 100, the same for both apps.
  The stores refuse a build number they have already seen, and this one never repeats. The +100 keeps
  it past the builds uploaded by hand before this workflow existed.

---

## One-time setup (for engineers)

The workflow reads these from Repository → Settings → Secrets and variables → **Actions**.

### Android

| Secret | What it is |
|--------|------------|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | The upload keystore, base64: `base64 -i upload.jks \| pbcopy` |
| `ANDROID_UPLOAD_STORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `ANDROID_UPLOAD_KEY_ALIAS` | `keyAlias` from `keystore.properties` |
| `ANDROID_UPLOAD_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON key of a Google Cloud service account invited in Play Console |
| `GOOGLE_SERVICES_JSON_BASE64` | `app/androidApp/google-services.json`, base64: `base64 -i google-services.json \| pbcopy` |

`google-services.json` is not in git (see `app/androidApp/google-services.json.example`); every
workflow that builds the app decodes it from this secret first.

The service account: Google Cloud console → IAM → Service accounts → create one and add a JSON key.
Then Play Console → **Users and permissions** → invite its email, with *Release apps to testing
tracks* for Turnia. Play only accepts API uploads once the app has had one bundle uploaded by hand.

### iOS

| Secret | What it is |
|--------|------------|
| `ASC_KEY_ID` | Key ID of an App Store Connect API key |
| `ASC_ISSUER_ID` | Issuer ID shown above the keys list |
| `ASC_KEY_P8` | The contents of the downloaded `AuthKey_XXXX.p8` |
| `IOS_DIST_CERT_P12_BASE64` | The distribution certificate + private key, exported as `.p12`, base64: `base64 -i dist.p12 \| pbcopy` |
| `IOS_DIST_CERT_PASSWORD` | The password chosen when exporting that `.p12` |
| `GOOGLE_SERVICE_INFO_PLIST_BASE64` | `app/iosApp/iosApp/GoogleService-Info.plist`, base64: `base64 -i GoogleService-Info.plist \| pbcopy` |

`GoogleService-Info.plist` is not in git (see the `.example` in the same folder); the `ios` job
decodes it from this secret first.

App Store Connect → **Users and Access** → **Integrations** → **App Store Connect API** → generate
a team key with the **Admin** role. Admin is what lets Xcode create or renew the provisioning
profile on the runner (cloud signing), so no profile is stored anywhere. The `.p8` can only be
downloaded once.

The certificate is different: it **is** stored, as a secret, and reused on every run instead of
being minted fresh each time. A GitHub Actions macOS runner starts with an empty keychain, so
without a stored certificate Xcode had to create a brand new "Apple Distribution" certificate on
every single release — and Apple caps how many an account can have, so releases eventually started
failing with "Choose a certificate to revoke. Your account has reached the maximum number of
certificates."

To create it once (Keychain Access on a Mac with access to the team):

1. Xcode → Settings → Accounts → select the team → **Manage Certificates** → **+** → **Apple
   Distribution**. This creates one certificate and installs it in the login keychain.
2. Keychain Access → *login* keychain → *My Certificates* → find the new "Apple Distribution:
   Geoviksoft..." certificate → right-click → **Export...** → save as `dist.p12`, choosing a
   password when prompted.
3. `base64 -i dist.p12 | pbcopy` and paste as the `IOS_DIST_CERT_P12_BASE64` secret; the password
   chosen above becomes `IOS_DIST_CERT_PASSWORD`.
4. Delete the local `dist.p12` once both secrets are saved.

If this certificate is ever revoked or expires, repeat the steps above and update both secrets —
nothing else in the workflow changes.

### Build machines

The iOS build runs on the `macos-26` runner with Xcode 26.6, pinned so that an update from Apple or
GitHub cannot change a release unannounced. To move to a newer Xcode, change both in `release.yml`
and run a release to check it.

The tag job pushes with the workflow's own token (`contents: write`). If the repository ever protects
tags, allow GitHub Actions to create `v*` tags.
