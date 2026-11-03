# Proposal

## Why

The E2E suite only runs after a merge (on every push to `main`), so a red run is discovered on `main`
instead of blocking the pull request that caused it — it happened on 2026-09-30, 10-07 and 10-28.
The fast PR gate from TUR-20 is in place and the suite has been stable since TUR-45, so the suite can
now gate the merge itself (TUR-21).

## What Changes

- Every pull request to `main` runs the Android E2E suite on every push, reusing
  `.github/workflows/android-e2e.yml` from `pr-check.yml`.
- A cheap first job skips the suite when **every** changed file is on an ignore list of paths that
  cannot affect the Android app or its emulators: `*.md` anywhere, `openspec/`, `docs/`, `store/`,
  `.claude/`, `firebase/hosting/`, `app/iosApp/`, `LICENSE`. It is an exclusion
  list, so anything else — `.github/`, `buildSrc/`, `gradle/`, the rest of `firebase/` — runs it.
- One job with a fixed name, `pr-e2e`, reports the result: green when the suite passed or was
  skipped, red otherwise. It is the check branch protection requires.
- Runs of one pull request share a concurrency group, so a new push cancels the obsolete run; parallel
  pull requests no longer queue behind each other.
- The `push: main` run stays as a free safety net for two pull requests that pass apart and fail
  together; `strict` (branch up to date) stays off.
- The retry and the known-flaky allowlist behave exactly as they do today.
- `CLAUDE.md` and the workflow headers stop saying the E2E suite is not part of the PR gate.

## Capabilities

### New Capabilities
- `pull-request-checks`: what a pull request to `main` must pass before it can merge — the checks it
  runs, when the E2E suite is skipped, and the stable check names branch protection relies on.

### Modified Capabilities
<!-- None: no existing spec covers CI. -->

## Impact

- `.github/workflows/pr-check.yml`: three new jobs (path decision, the suite, `pr-e2e`).
- `.github/workflows/android-e2e.yml`: its concurrency group gains the pull request number; the
  `push`, `workflow_dispatch` and `workflow_call` triggers are unchanged.
- A new ignore-list file and a small script that decides, with its test run by `pr-checks`.
- `CLAUDE.md` (*Pull requests*, *E2E tests*).
- Branch protection is the developer's: once this is merged, add `pr-e2e` to the required checks.
- `release.yml` is untouched (TUR-48 is changing it). Fork pull requests get no
  `GOOGLE_SERVICES_JSON_BASE64`, so their E2E run would fail; TUR-22's `demo-` project removes that need.
- Every pull request that touches the app waits ~30 minutes (longer with the retry, within the job's 45-minute limit) before it can merge.
