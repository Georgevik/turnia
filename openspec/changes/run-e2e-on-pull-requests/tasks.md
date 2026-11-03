# Tasks

## 1. The skip decision

- [x] 1.1 Add `.github/e2e-ignored-paths.txt` with the ignore list from the spec (one `fnmatch` pattern per line, a header saying `*` also crosses `/`) and verify it holds exactly the paths the explore settled
- [x] 1.2 Add `.github/scripts/e2e-needed.py`, reading changed paths on stdin and writing `run=true|false` to `$GITHUB_OUTPUT` (and to stdout), and verify `printf 'CLAUDE.md\ncore/x.kt\n' | python3 .github/scripts/e2e-needed.py .github/e2e-ignored-paths.txt` prints `run=true`
- [x] 1.3 Add `.github/scripts/test_e2e_needed.py` covering every skip scenario of the spec plus the empty list, and verify `python3 .github/scripts/test_e2e_needed.py` passes on Python 3.9
- [x] 1.4 Run that test in the `pr-checks` job of `pr-check.yml` and verify the job's step list shows it next to the production guard's test

## 2. The E2E jobs in the PR gate

- [x] 2.1 Append `-pr-<number>` to `android-e2e.yml`'s concurrency group when `github.event.pull_request.number` is set, update its header comment, and verify the `push`, `workflow_dispatch` and release groups are unchanged
- [x] 2.2 Add `e2e-paths` (merge-commit diff with `fetch-depth: 2`, `run=true` outside `pull_request`), `e2e` (`uses: ./.github/workflows/android-e2e.yml`, `secrets: inherit`, `if` on its output) and `pr-e2e` (`if: always()`, green only when `e2e-paths` succeeded and `e2e` is `success` or `skipped`) to `pr-check.yml`, update its header comment, and verify with `actionlint` or a YAML parse that the workflow is valid
- [x] 2.3 Update `CLAUDE.md` (*Pull requests*: the gate now includes `pr-e2e` and when it skips; *E2E tests*: pull requests run the suite) and verify no doc still says the E2E suite is not part of the PR gate (`grep -rn "not part of it" CLAUDE.md .github`)

## 3. Verification on GitHub

- [ ] 3.1 Open the pull request and verify `pr-e2e` runs the suite on it (it touches `.github/`) and ends green
- [ ] 3.2 Push a second commit while the suite is running and verify the first run is cancelled
- [ ] 3.3 Push a throwaway branch that changes only a `.md` file to a draft pull request, verify `e2e` is skipped and `pr-e2e` is green within minutes, then close it and delete the branch

## Workflow follow-up

- Hand the developer the branch protection step: *Settings → Branches → `main` → Edit → Require status checks to pass → add `pr-e2e` → Save*.
- Tell the director that `docs/agents/workers.md` can list `pr-e2e` among the checks to wait for, and drop the manual `gh workflow run android-e2e.yml` before *ready to test*.
- Archive the change as the last commit of the PR, once the developer accepts it.
