# Proposal

## Why

A release reaches TestFlight from CI, but the App Store side is still done by hand: someone creates
the version in App Store Connect, picks the build, and types the public *What's New* in every
language. After a release, the only thing left by hand should be pressing *Submit for Review*.

## What Changes

- Once the iOS build has finished processing, CI finds or creates the App Store version for the
  release's version name, writes `whatsNew` on each of its existing localizations from
  `.github/whatsnew`, and attaches the build. It reuses `.github/scripts/release-notes.py`, which
  already resolves the build, waits for it and maps the locales for TestFlight.
- CI never creates a review submission, never submits and never releases: the version it prepares
  is visible only inside App Store Connect.
- A version that is already submitted, in review or waiting for release is never touched; the step
  warns and leaves it alone.
- The manual **Release notes** workflow does the same for an existing build, so editing the notes
  after a release updates the App Store version too, and gains a dry run that only reports.
- The build declares `ITSAppUsesNonExemptEncryption = NO`, so it no longer stops on the export
  compliance question, and the App Store step fails clearly if a build is still missing it.
- The App Store version always carries the release's version name, the same one Android's build gets:
  CI checks the build against it and never derives a name of its own.
- The notes are reused from one release to the next on purpose, with no check that they changed. For
  now they become a generic "improvements and bug fixes" text in every language, replacing the
  feature text written for 1.8.1.
- `.github/RELEASE.md` describes the new flow: the public text comes from `.github/whatsnew`, not
  from typing it in App Store Connect.

## Capabilities

### New Capabilities
- `release-pipeline`: what the release workflows guarantee about the builds and store records they
  produce — the App Store version CI prepares, its version name, its notes, and what CI must never do
  to a store.

### Modified Capabilities

None.

## Impact

- `.github/scripts/release-notes.py`: a new `appstore` subcommand, a wait for processing, a dry-run
  flag, and a stdlib-only unit test for its decisions (`.github/scripts/test_release_notes.py`).
- `.github/workflows/release-notes.yml`: an App Store step after TestFlight, a `dryRun` input, a
  longer timeout.
- `.github/workflows/release.yml`: passes the release's version name to the App Store step; the
  header comment.
- `.github/whatsnew/*`: the generic text in the five languages.
- `app/iosApp/iosApp/Info.plist`: the export compliance declaration (touches `ios`).
- `.github/RELEASE.md`.
- No app code, Firestore, rules or Functions change. `pr-check.yml` and `android-e2e.yml` are not
  touched (TUR-21 is on them).
