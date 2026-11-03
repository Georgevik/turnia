# release-pipeline Specification

## Purpose
Defines what the release workflows guarantee about the builds and store records they produce: which
release notes ship, how far CI prepares each store, and what CI must never do to a store.

## Requirements

### Requirement: CI prepares the App Store version of an iOS release
After an iOS build is uploaded and has finished processing, CI SHALL make sure an App Store version
exists for the build's version name, with that build attached and its *What's New* taken from
`.github/whatsnew`. Nothing else SHALL be left by hand before *Submit for Review*. Running it again
for the same build SHALL leave the same result.

#### Scenario: No version exists yet
- **WHEN** a release uploads build 210 of version 1.12.0 and App Store Connect has no 1.12.0 version
- **THEN** CI creates the 1.12.0 iOS version, attaches build 210 and writes *What's New* on its localizations

#### Scenario: The version already exists
- **WHEN** a release uploads build 211 of version 1.12.0 and an editable 1.12.0 version holds build 210
- **THEN** CI attaches build 211 to that version and rewrites its *What's New*, creating no second version

#### Scenario: An unsubmitted version has an older name
- **WHEN** a release uploads version 1.12.0 and the only editable version is a 1.11.3 that was never submitted
- **THEN** CI renames that version to 1.12.0 and prepares it, since App Store Connect allows one editable version

#### Scenario: The editable version is newer than the build
- **WHEN** the step runs for a build of 1.11.2 while the editable version is 1.12.0
- **THEN** it changes nothing and warns that the build is older than the version being prepared

#### Scenario: Re-running the step
- **WHEN** the App Store step runs twice for the same build
- **THEN** the second run ends in the same state as the first, without error

### Requirement: CI never submits, releases or touches a version in flight
CI SHALL NOT create a review submission, submit a version for review, release a version, or change
any version that is submitted, in review, approved, waiting for release or on sale. Every change it
makes SHALL be visible only inside App Store Connect.

#### Scenario: A version is waiting for review
- **WHEN** the App Store step runs while version 1.11.3 is waiting for review
- **THEN** it changes nothing on any version, warns that a version is in flight, and the run's summary says so

#### Scenario: The prepared version stays private
- **WHEN** the App Store step has finished
- **THEN** no review submission exists that CI created, and the version is still in a state the developer can edit and submit

### Requirement: What's New covers every localization of the version
CI SHALL write *What's New* on every existing localization of the version: from its own
`.github/whatsnew` file when there is one, otherwise from the English notes, with a warning naming it.
It SHALL NOT add a localization, which would add a store language without its listing. A notes file
with no matching localization SHALL be reported as a warning.

#### Scenario: Every language has notes
- **WHEN** the version has localizations en-GB, es-ES, fr-FR, de-DE and it, and each has a notes file
- **THEN** each localization's *What's New* is its own file's text

#### Scenario: A localization has no notes file
- **WHEN** the version also has an en-US localization and there is no `whatsnew-en-US`
- **THEN** en-US gets the en-GB text and the run warns that en-US used the fallback

#### Scenario: A notes file has no localization
- **WHEN** a notes file exists for a language the App Store listing does not have
- **THEN** no localization is created and the run warns that those notes were not used

### Requirement: An iOS build declares its export compliance
Every iOS build SHALL carry the export compliance declaration, so App Store Connect does not hold it
on the encryption question. The App Store step SHALL fail with a message naming the cause when a
build still has no answer to it.

#### Scenario: A release build is processed
- **WHEN** a release build finishes processing in App Store Connect
- **THEN** it is not marked *Missing Compliance* and can be tested and attached without a manual answer

#### Scenario: A build without the declaration
- **WHEN** the App Store step finds a processed build whose encryption question is unanswered
- **THEN** it fails, saying the build is missing its export compliance declaration, and attaches nothing

### Requirement: The App Store version carries the release's version name
The App Store version CI prepares SHALL be named with the version name the Release workflow computed,
which is also Android's `versionName` for that release. CI SHALL NOT derive or invent another name,
and SHALL fail without touching App Store Connect when the uploaded build's version differs from it.

#### Scenario: Both apps ship the same name
- **WHEN** a release computes version 1.12.0 and uploads both apps
- **THEN** Play's release has versionName 1.12.0 and the App Store version CI prepares is named 1.12.0

#### Scenario: A renamed version follows the release
- **WHEN** an unsubmitted 1.11.3 is the editable version and the release computes 1.12.0
- **THEN** the version is renamed to exactly 1.12.0, the name Android got

#### Scenario: The build does not match the release
- **WHEN** the release computed 1.12.0 but the processed build reports version 1.11.3
- **THEN** the step fails, naming both versions, and changes nothing in App Store Connect

### Requirement: Release notes are reused until someone edits them
A release SHALL ship whatever `.github/whatsnew` holds, whether or not it changed since the last
release, without failing or warning about it.

#### Scenario: Notes unchanged since the last release
- **WHEN** the Release workflow runs and no file in `.github/whatsnew` changed since the last release tag
- **THEN** the release proceeds and every store gets the same notes as the previous release

### Requirement: Release notes can be re-applied to an existing build
Running the **Release notes** workflow for a build SHALL update its Play notes, its TestFlight notes
and its App Store version as a release would, and SHALL offer a dry run that reports what it would
change without writing to any store.

#### Scenario: Notes edited after a release
- **WHEN** the notes are edited on `main` and the Release notes workflow runs with the build number empty
- **THEN** the last release's Play notes, TestFlight notes and App Store *What's New* all carry the new text

#### Scenario: Dry run
- **WHEN** the Release notes workflow runs with the dry run ticked
- **THEN** its summary lists the version, build and localizations it would change, and no store is written

#### Scenario: The summary shows the notes
- **WHEN** the Release notes workflow runs, from a release or by hand, with or without the dry run
- **THEN** its summary shows the text of every notes file, by language, with its length
