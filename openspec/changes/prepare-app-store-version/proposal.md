# Proposal

## Why

A release reaches TestFlight from CI, but the App Store side is still done by hand: someone creates
the version in App Store Connect, picks the build, and types the public *What's New* in every
language. That hand step is also why the notes go stale — `.github/whatsnew` was last edited for
1.8.1, and every release since has shipped those words, because nothing notices that they did not
change. After a release, the only thing left by hand should be pressing *Submit for Review*.

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
- The Release workflow refuses to run when `.github/whatsnew` has not changed since the last release
  tag, unless the run explicitly says the notes are reused.
- `.github/RELEASE.md` describes the new flow: the public text comes from `.github/whatsnew`, not
  from typing it in App Store Connect.

## Capabilities

### New Capabilities
- `release-pipeline`: what the release workflows guarantee about the builds and store records they
  produce — release notes freshness, the App Store version CI prepares, and what CI must never do to
  a store.

### Modified Capabilities

None.

## Impact

- `.github/scripts/release-notes.py`: a new `appstore` subcommand, a wait for processing, a dry-run
  flag, and a stdlib-only unit test for its decisions (`.github/scripts/test_release_notes.py`).
- `.github/workflows/release-notes.yml`: an App Store step after TestFlight, a `dryRun` input, a
  longer timeout.
- `.github/workflows/release.yml`: the notes guard in `validate`, a `reuseNotes` input, the header
  comment.
- `app/iosApp/iosApp/Info.plist`: the export compliance declaration (touches `ios`).
- `.github/RELEASE.md`.
- No app code, Firestore, rules or Functions change. `pr-check.yml` and `android-e2e.yml` are not
  touched (TUR-21 is on them).
