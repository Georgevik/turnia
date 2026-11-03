# Spec Delta

## Purpose

Defines what a pull request to `main` must pass before it can merge: the checks it runs, when the
Android E2E suite may be skipped, and the stable check names that branch protection relies on.

## ADDED Requirements

### Requirement: Pull requests run the E2E suite
Every pull request to `main` SHALL run the Android E2E suite on every push to it, unless every file it changes is on the E2E ignore list. The run SHALL test the pull request merged into `main`, with the same retry and known-flaky allowlist as every other E2E run.

#### Scenario: A pull request touching the app
- **WHEN** a pull request to `main` changes a file under `core/`
- **THEN** the Android E2E suite runs against it

#### Scenario: A failure retried once
- **WHEN** a test fails on the first attempt and passes on the retry
- **THEN** the pull request's E2E result is green and the summary marks that test as retried

### Requirement: Documentation-only pull requests skip the suite
The E2E suite SHALL be skipped when, and only when, every changed file matches the ignore list: `*.md` anywhere, `openspec/`, `docs/`, `store/`, `.claude/`, `firebase/hosting/`, `app/iosApp/`, `.github/whatsnew/` and `LICENSE`. A moved file counts by both its old and its new path. Any other file, or a change whose files cannot be listed, SHALL run the suite.

#### Scenario: Only Markdown and OpenSpec files
- **WHEN** a pull request changes only `CLAUDE.md` and files under `openspec/`
- **THEN** the E2E suite does not run and the E2E check is green

#### Scenario: Only the iOS app
- **WHEN** a pull request changes only files under `app/iosApp/`
- **THEN** the E2E suite does not run and the E2E check is green

#### Scenario: Only the store release notes
- **WHEN** a pull request changes only files under `.github/whatsnew/`
- **THEN** the E2E suite does not run and the E2E check is green

#### Scenario: Documentation plus one workflow file
- **WHEN** a pull request changes `README.md` and `.github/workflows/android-e2e.yml`
- **THEN** the E2E suite runs

#### Scenario: App code moved into an ignored path
- **WHEN** a pull request only moves a file from `core/` to `docs/`
- **THEN** the E2E suite runs

#### Scenario: Firebase outside hosting
- **WHEN** a pull request changes only `firebase/firestore.rules`
- **THEN** the E2E suite runs

### Requirement: One stable E2E check
A pull request SHALL report its E2E result through a single check named `pr-e2e`, present on every pull request to `main`. It SHALL be green when the suite passed or was skipped by the ignore list, and red when the suite failed or the skip decision failed. A run cancelled by a newer push SHALL NOT report a red `pr-e2e` on the commit it made obsolete.

#### Scenario: Suite skipped
- **WHEN** the ignore list skips the suite
- **THEN** `pr-e2e` completes green, so a merge requiring it is not left waiting

#### Scenario: Suite failed
- **WHEN** a test still fails after the retry and is not on the known-flaky allowlist
- **THEN** `pr-e2e` is red and a merge requiring it is blocked

### Requirement: A new push supersedes the pull request's previous run
A new push to a pull request SHALL cancel that pull request's E2E run still in progress. Runs of different pull requests SHALL NOT wait for each other, and SHALL NOT wait for or cancel a run on `main`, a manual run or a release.

#### Scenario: Two pushes in a row
- **WHEN** a second commit is pushed to a pull request while its E2E run is in progress
- **THEN** the first run is cancelled and a new one starts for the second commit

#### Scenario: Two pull requests at once
- **WHEN** two pull requests both start an E2E run
- **THEN** both run at the same time

### Requirement: Main keeps its post-merge E2E run
Every push to `main` SHALL keep running the Android E2E suite after the merge, as a safety net for pull requests that pass apart and fail together.

#### Scenario: A merge to main
- **WHEN** a pull request is merged into `main`
- **THEN** the Android E2E suite runs on the resulting commit of `main`
