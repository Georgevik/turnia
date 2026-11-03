# Tasks

## 1. Script groundwork

- [ ] 1.1 In `.github/scripts/release-notes.py`, move the `jwt`, `requests` and `google.*` imports into the functions that use them and extract `find_app` / `find_build` from `testflight`; verify `python3 -I -c "import importlib.util, sys; s=importlib.util.spec_from_file_location('rn', '.github/scripts/release-notes.py'); m=importlib.util.module_from_spec(s); s.loader.exec_module(m)"` succeeds with no packages installed, and `python3 .github/scripts/release-notes.py` still prints the usage
- [ ] 1.2 Add `--dry-run` to every subcommand (Play deletes its edit instead of committing; TestFlight prints `would …` instead of writing); verify by reading the diff that every `post`/`patch`/`put`/`commit` sits behind the flag

## 2. App Store version

- [ ] 2.1 Add the pure `plan_version(versions, name)` and `plan_notes(locales, notes)` of design §4–5, with version-name ordering, and `.github/scripts/test_release_notes.py` (plain `unittest`) covering create, use, rename, skip-in-flight, skip-done, skip-older-build, unknown state, own notes, en-GB fallback and unused files; verify `python3 .github/scripts/test_release_notes.py` passes with no packages installed
- [ ] 2.2 Add the `appstore <buildNumber>` subcommand: find the build, read its version name from `preReleaseVersion`, wait for `processingState == VALID`, fail on unanswered `usesNonExemptEncryption`, apply the version plan, attach the build, write `whatsNew` on existing localizations, print one summary line per action; confirm each endpoint and state list against Apple's API reference and verify by reading that no call reaches `reviewSubmissions`, `appStoreVersionSubmissions` or `appStoreVersionReleaseRequests`
- [ ] 2.3 In `.github/workflows/release-notes.yml`, add the *Prepare the App Store version* step after *Update TestFlight* (same `ios` condition and secrets), the `dryRun` input on `workflow_dispatch` and `workflow_call` passed to every step, and `timeout-minutes: 75`; update the header comments of `release-notes.yml` and the `testflight-notes` job in `release.yml`; verify with `actionlint` (or `python3 -c "import yaml…"` if it is not installed) that both files parse

## 3. Export compliance

- [ ] 3.1 Add `ITSAppUsesNonExemptEncryption` = `false` to `app/iosApp/iosApp/Info.plist` with a comment on why it is exempt and when to revisit it; verify `plutil -lint app/iosApp/iosApp/Info.plist` passes and an iOS build (`xcodebuild … build` for the simulator, or the `pr-ios` check) still succeeds

## 4. Notes guard

- [ ] 4.1 In `release.yml`, add the `reuseNotes` boolean input (default `false`) and, in `validate` after *Resolve the version*, fetch the last tag at depth 1 and fail on `git diff --quiet <last> HEAD -- .github/whatsnew` unless `reuseNotes`, which instead warns and adds a *Release notes reused* summary block; verify the shell locally on this branch (`git diff --quiet v1.11.2-207 HEAD -- .github/whatsnew` exits 0, i.e. unchanged) and that the file parses

## 5. Documentation

- [ ] 5.1 Update `.github/RELEASE.md`: *Before you release* says the guard fails on unchanged notes and when to tick *Reuse notes*; the notes section says the App Store version, build and *What's New* are prepared by CI and only *Submit for Review* is left; the release notes workflow's dry run; the export compliance declaration; the new unit test; verify every input and step name it mentions matches the workflows

## 6. Integration

- [ ] 6.1 Run `python3 .github/scripts/test_release_notes.py` and `python3 .claude/hooks/test_guard_production.py`, and check `pr-android`, `pr-ios` and `pr-checks` are green on the PR

## Workflow follow-up

- The developer runs the real check (an agent cannot dispatch store workflows): *Release notes* from the PR branch with `platforms: ios` and *Dry run* ticked, then without it once satisfied, and the notes guard on the next release.
- Archive the change as the last commit of the PR once the developer accepts it.
