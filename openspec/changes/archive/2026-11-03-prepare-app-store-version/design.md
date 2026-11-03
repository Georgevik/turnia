# Design

## Context

- `release.yml` uploads the iOS build in its `ios` job, then calls `release-notes.yml` as the
  `testflight-notes` job, which runs `.github/scripts/release-notes.py testflight <buildNumber>`.
  That script already authenticates with the App Store Connect API key (`ASC_KEY_ID`,
  `ASC_ISSUER_ID`, `ASC_KEY_P8`, team key with the Admin role), finds the app by bundle id, polls
  until the build appears, and maps Play's language codes to App Store Connect's (`it-IT` → `it`).
- The `tag` job depends on `validate`, `android` and `ios` only, so a failure in the notes job
  never stops a release from being tagged; *Re-run failed jobs* repeats just that job.
- `validate` resolves the last release tag (`v<x.y.z>-<build>`) from the remote; the checkout is
  shallow.
- The iOS target generates its `Info.plist` (`GENERATE_INFOPLIST_FILE = YES`) on top of
  `iosApp/Info.plist`, which has no `ITSAppUsesNonExemptEncryption`.
- No test runs `.github/scripts` today; `.claude/hooks/test_guard_production.py` is the precedent
  for a plain `unittest` file run by hand.

## Goals / Non-Goals

**Goals:**
- One idempotent step that brings the App Store version to "ready to submit" from a build number.
- The decisions it takes (which version, create/rename/skip, which text per localization) are pure
  functions with unit tests, separate from the HTTP calls.

**Non-Goals:**
- Screenshots, description, keywords, pricing, phased release or any other listing metadata.
- Submitting for review or releasing, through any API (`reviewSubmissions`, `appStoreVersionSubmissions`,
  `appStoreVersionReleaseRequests`) — the script never calls those endpoints.
- `pr-check.yml` and `android-e2e.yml` (TUR-21). The new unit test is run by hand, not in CI.

## Decisions

### 1. A new `appstore` subcommand in `release-notes.py`, run by `release-notes.yml`

`release-notes.py appstore <buildNumber> [--dry-run]`, as a step right after *Update TestFlight* in
`release-notes.yml` under the same `ios` condition. It shares the session, the app lookup, the build
lookup and the locale mapping with `testflight`, which become helpers (`find_app`, `find_build`).
Running it from `release-notes.yml` means a manual *Release notes* run prepares the App Store too,
which is what replaces typing the public text by hand.

*Alternative:* a separate job in `release.yml`. Rejected: it would duplicate the Python setup and the
manual workflow would not reach it.

### 2. The version name is the release's, checked against the build

`validate` already computes one version name for both apps: Gradle gets it as `versionName`, Xcode as
`MARKETING_VERSION`. `release.yml` passes it on to `release-notes.yml` (a new optional `versionName`
input on `workflow_call`), and the script receives it as `--version-name`. It then reads the build's
own `CFBundleShortVersionString` from its `preReleaseVersion` relationship
(`GET /builds/{id}?include=preReleaseVersion`) and fails, before any write, if the two differ. The App
Store version is always named with that one value, so iOS and Android stay on the same name; a
renamed editable version (§4) takes it too.

A manual *Release notes* run only knows a build number, so without `--version-name` the build's own
version is used: it is the name that release computed, because the archive was stamped with it.

*Alternative:* derive the name from the build only. Rejected as the sole source: the explicit check is
what turns "the two apps go together" from an accident of the archive step into something the run
proves.

### 3. Wait for processing, then check compliance

`testflight` only waits for the build to appear. The App Store step also waits for
`processingState == VALID` (polling every minute, up to 30 minutes), fails on `FAILED`/`INVALID`, and
then fails if `usesNonExemptEncryption` is still `null` — the build would sit in *Missing
Compliance* and could not be submitted. The job's `timeout-minutes` goes from 40 to 75 so both waits
fit.

### 4. Choosing the version: a pure `plan_version(versions, name)`

The script lists the app's iOS `appStoreVersions` and reads each one's state (`appVersionState`,
falling back to the deprecated `appStoreState`). States fall in three groups:

| Group | States |
|---|---|
| editable | `PREPARE_FOR_SUBMISSION`, `DEVELOPER_REJECTED`, `REJECTED`, `INVALID_BINARY` |
| in flight | `METADATA_REJECTED` (its build stays and it is resubmitted as it is), `WAITING_FOR_REVIEW`, `IN_REVIEW`, `READY_FOR_REVIEW`, `PENDING_DEVELOPER_RELEASE`, `PENDING_APPLE_RELEASE`, `PROCESSING_FOR_DISTRIBUTION`, `WAITING_FOR_EXPORT_COMPLIANCE`, `ACCEPTED` |
| done | anything else (`READY_FOR_DISTRIBUTION`, `READY_FOR_SALE`, `REPLACED_WITH_NEW_VERSION`, removed…) |

The lists match Apple's `AppVersionState` (and the deprecated `AppStoreVersionState` for the
fallback); an unknown state is treated as **in flight**, so a surprise never leads to a write. Version
names compare numerically with trailing zeros dropped, so `1.8` and `1.8.0` are the same version.

`plan_version` returns one action:
1. A version with this name exists: editable → **use** it; otherwise → **skip** ("already submitted
   or on sale").
2. Any version is in flight → **skip** ("version X is in flight").
3. An editable version with another name exists: lower than this build's → **rename** it
   (`PATCH versionString`), since App Store Connect allows a single editable version per platform;
   higher → **skip** ("build is older than the version being prepared").
4. Otherwise → **create** (`POST /appStoreVersions`, platform `IOS`, `versionString`, release type
   left at the app's default).

A skip is a `::warning::` and exit 0: it is the expected outcome when the notes workflow is rerun on
a live build. Every other failure is an error.

*Alternative for 3:* fail instead of renaming. Rejected as the default because the editable version
is by definition unsubmitted, and renaming is exactly what the developer would do in the console —
listed in the Proposal for confirmation.

### 5. Attach, then write the notes

`PATCH /appStoreVersions/{id}/relationships/build` with the build, then `whatsNew` on each existing
`appStoreVersionLocalizations` entry. App Store Connect copies a new version's localizations from
the previous one, so they exist without the script creating any. An app's first version takes no
*What's New* (App Store Connect refuses it), so the notes are skipped while no version has been released. A pure `plan_notes(locales, notes)`
maps each localization to its text (own file → en-GB fallback with a warning) and reports unused
files. The script never `POST`s a localization.

### 6. Export compliance declared in `Info.plist`

`<key>ITSAppUsesNonExemptEncryption</key><false/>` in `iosApp/Info.plist`, with a comment saying why.
Turnia only uses encryption the OS provides (HTTPS through Firebase, Google and Apple sign-in, AdMob),
which is exempt.

*Alternative:* the script answers `usesNonExemptEncryption = false` on each build through the API.
Rejected: it is a legal declaration, so it belongs in reviewed source, and the plist also covers a
build archived by hand in Xcode.

### 7. Generic notes, reused on purpose

There is no check that `.github/whatsnew` changed between releases: the developer decided the notes
are, for now, a generic text about improvements and bug fixes, the same for every release. The five
files are rewritten to that text, each in its language and well under Play's 500 characters, and
RELEASE.md says to edit them only when a release deserves specific notes.

*Alternative:* fail or warn on unchanged notes. Rejected by the developer: with generic notes,
unchanged is the normal case.

### 8. Dry run everywhere

`--dry-run` makes every subcommand read as usual and print `would …` instead of writing: Play opens
an edit and deletes it instead of committing, TestFlight and the App Store skip their `PATCH`/`POST`.
`release-notes.yml` gets a `dryRun` boolean input (default `false`) on both `workflow_dispatch` and
`workflow_call`; `release.yml` never sets it.

### 9. Long waits and failures stay visible

An App Store Connect token lives 15 minutes and the waits last up to an hour, so the session mints a
token per request. The job runs bash with `pipefail`, `PYTHONUNBUFFERED=1` and `2>&1 | tee`, so a
failing script fails its step and its message reaches the log and summary as it happens. The App
Store step runs even when TestFlight's notes failed, since it does not depend on them. A manual run
with no build number takes the version name from the last release tag, so the version-name check
applies there too, and a job-level concurrency group keeps two runs from planning the same version.

### 10. Pure logic testable with the standard library

`jwt`, `requests` and `google.*` are imported inside the functions that use them, so
`.github/scripts/test_release_notes.py` (plain `unittest`) imports the module with nothing installed
and covers `plan_version`, `plan_notes` and version-name ordering.

## Risks / Trade-offs

- [A state list drifts from Apple's API] → unknown states count as in flight, so the step skips
  instead of writing; the warning names the state.
- [Processing takes longer than 30 minutes] → the step fails with a clear message; *Re-run failed
  jobs* or a manual *Release notes* run finishes it, since it is idempotent.
- [Generic notes say nothing about a release with real news] → RELEASE.md says to edit the files
  when a release deserves it; the next release then simply ships them.
- [The plist declaration is wrong if strong non-exempt crypto is ever added] → the comment on the key
  says to revisit it then; the developer confirms the declaration now.
- [The Admin API key can do more than this needs] → unchanged from today; the script's endpoints are
  listed and none of them submits or releases.

## Migration Plan

Nothing to migrate. The first real check is a dry run of *Release notes* on the last build, then the
next release: the App Store version appears in App Store Connect with the build, the release's version
name and the generic notes. Rollback is reverting the PR; versions it created stay editable in App Store
Connect.
