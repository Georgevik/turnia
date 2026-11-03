# Design

## Context

- `pr-check.yml` is the PR gate: jobs `pr-android`, `pr-ios`, `pr-checks`, one concurrency group per
  pull request with `cancel-in-progress: true`. Branch protection requires those three names, with
  `strict: false`.
- `android-e2e.yml` is the suite: `push` to `main`, `workflow_dispatch`, and `workflow_call` from
  `release.yml` with `secrets: inherit`. Its concurrency group is
  `${{ github.workflow }}-android-e2e` (plus the run id for a manual dispatch), with
  `cancel-in-progress: false`. When it is called, `github.workflow` and `github.event` are the
  caller's.
- Every pull request so far comes from a branch of this repository, so repository secrets reach it.
- The decisions of the explore are in the `## Explore` comment of TUR-21; the requirements are in
  `specs/pull-request-checks/spec.md`.

## Goals / Non-Goals

**Goals:**
- One definition of the suite (`android-e2e.yml`), now also called from the PR gate.
- A required check that is always reported, whether the suite ran or was skipped.

**Non-Goals:**
- Branch protection itself (the developer's), `release.yml` (TUR-48), fork pull requests and the
  emulators' project id (TUR-22), new E2E flows (TUR-44).
- Making the suite faster or sharding it.

## Decisions

### The suite is called from `pr-check.yml`, not triggered by `pull_request` in `android-e2e.yml`

Three jobs are added to `pr-check.yml`:

```
 e2e-paths --(run?)--> e2e (uses android-e2e.yml) --+
     |                                              +--> pr-e2e  (if: always())
     +----------------------------------------------+
```

The PR gate stays one workflow, and the suite inherits its per-PR group with
`cancel-in-progress: true`, so a new push cancels the obsolete run without a second policy. A
`pull_request` trigger on `android-e2e.yml` would also work, but it would need its own concurrency
rules for a fourth event and would put a required check in a workflow whose other runs (`main`,
dispatch, release) must never be cancelled.

### `pr-e2e` is a small aggregator job

`pr-e2e` `needs: [e2e-paths, e2e]`, runs `if: always()`, and fails unless `e2e-paths` succeeded and
`e2e` is `success` or `skipped`. This is the required check. Requiring the suite's own job does not
work: a caller job that is skipped reports under its own name (`e2e`) instead of the name it reports
when it runs (`e2e / android-e2e`), so a skipped run would leave the required check waiting forever.
The `pr-` prefix keeps the existing rule that no other workflow can satisfy a required name.

### The skip decision is a script over the merge commit's diff

`e2e-paths` checks out the pull request's merge commit with `fetch-depth: 2` and lists
`git diff --name-only HEAD^1 HEAD`. The first parent is the tip of `main`, so the list is exactly
what the pull request would change, with no full-history fetch. The list goes to
`.github/scripts/e2e-needed.py`, which reads the patterns from `.github/e2e-ignored-paths.txt` and
writes `run=true|false` to `$GITHUB_OUTPUT`. It answers `run=true` when any file matches no pattern,
when the list is empty, or when the event is not a pull request (a manual dispatch of `PR check`).

Patterns use Python's `fnmatch`, where `*` also crosses `/`: `*.md` covers Markdown anywhere and
`openspec/*` the whole tree. The file says so, since it is the one thing a reader would get wrong.

Rejected: `paths-ignore` on the trigger (a workflow that never starts leaves a required check
pending), and `dorny/paths-filter` (a third-party action with read access to the token, for what is
twenty lines of Python we can test).

### The script is tested in `pr-checks`

`.github/scripts/test_e2e_needed.py` (standard library `unittest`, Python 3.9 compatible) covers the
spec's scenarios and runs in `pr-checks` next to the production guard's test.

### `android-e2e.yml`'s group gains the pull request number

Called from `PR check`, its group would be the single `PR check-android-e2e`, with
`cancel-in-progress: false`: every open pull request would queue behind the others. The group gets
`-pr-<number>` appended when `github.event.pull_request.number` is set. Runs on `main`, dispatches
and releases keep the groups they have today. The inner group keeps `cancel-in-progress: false`;
cancelling an obsolete run is the caller's group's job.

### `push: main` stays and `strict` stays off

Decided in the explore: the post-merge run is free, keeps the README badge meaningful, and catches
two pull requests that pass apart and fail together. Updating a branch before its merge is a push,
which re-runs the suite anyway.

## Risks / Trade-offs

- [A pull request that touches the app waits ~30 minutes, ~68 with the retry] → accepted in the
  explore; cancelling obsolete runs keeps the wait to the latest push.
- [The ignore list skips something that mattered] → it is an exclusion list, `.github/` always runs,
  and `main` still runs the suite after every merge.
- [A fork pull request has no `GOOGLE_SERVICES_JSON_BASE64`, so its suite fails] → none have been
  opened; TUR-22's `demo-` project removes the need for the secret.
- [GitHub's free concurrency limit queues runs when many pull requests push at once] → each run is
  one Ubuntu job; the limit is 20.

## Migration Plan

1. Merge the change. Its own pull request shows `pr-e2e` running (it touches `.github/`).
2. The developer adds the check: *Settings → Branches → `main` → Edit → Require status checks to pass
   → search `pr-e2e` → add it → Save*. The search only lists a check that has already run once.
3. Pull requests opened before the merge get `pr-e2e` once they are updated from `main`.

Rollback: remove `pr-e2e` from the required checks, then revert the change.
