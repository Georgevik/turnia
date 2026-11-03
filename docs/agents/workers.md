# Workers of the director

A **worker** is a Claude session that the director (`/director`) launches with `aoe` in the group `turnia/workers`, to carry one issue through one step: explore, propose or apply. Each worker has its own worktree and its own branch, `features/tur-<N>-<slug>`. Your name, the director's name and your issue come in your system prompt. Read `docs/agents/issue-tracker.md` before you start. Talk to the developer in Spanish; code, comments, commits, OpenSpec artifacts and PRs are in English.

## Your step

| Issue state | What you do | You end at |
|---|---|---|
| `explore` | `/opsx:explore` with the developer, in your session. Ask the questions one at a time, each with your recommendation. | A `## Explore` comment and the issue in `proposed` |
| `proposed` | `/opsx:propose` (`medium`, `complex`), or a brief (`trivial`) | A `## Proposal` comment, the developer's `## Approved`, and the issue in `ready` |
| `ready` | `/opsx:apply`, or the brief | A PR and the issue in `testing` |

A step can roll into the next one in the same session when the developer is there for it. An explore whose outcome is trivial may go straight to the brief and the apply once the developer approves it.

**An important decision is never improvised.** If one comes up that the change does not settle, stop. A technical decision goes to the director; a product decision goes to the developer, in your session. Then update the change.

## Applying

- One commit per task: one simple sentence, with no body and no attribution. Never rewrite a pushed branch, so no rebase, amend or force push.
- Every user-visible change ships with its E2E flow (`app/androidApp/src/androidTest/.../e2e`). A change to a Firestore field ships with its fixtures (`firebase/test/fixtures`).
- Before you report **ready to test**:
  - run the unit tests and the E2E tests that cover your change;
  - review your own PR with `/code-review high` and fix what survives;
  - make sure the `pr-android`, `pr-ios`, `pr-checks` and `pr-e2e` checks are green on GitHub.
- The PR title is `TUR-<N> <summary>`. The body has exactly three sections: **What changes?**, **Why?** and **How to test it**. *How to test it* is a list of concrete steps, each with an action and its expected result. After them come `Closes #<N>` and `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- Archive the OpenSpec change as the last commit of the PR, once the developer accepts it.

## The emulators

The Firebase emulators and the E2E suite use fixed ports, so only one checkout on this Mac can run them at a time. **Ask the director for the emulator turn** before any `connected…AndroidTest` or `firebase emulators:*` run, and tell it when you are done. With the turn, run only the tests you need. One test is run like this:

```
./gradlew :app:androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.geoviksoft.turnia.e2e.flows.<Class>#<method>
```

The emulators, Android emulators and processes of other checkouts belong to whoever started them. Never stop them.

CI is free and does not take the emulator turn: every push to your PR runs the whole suite, reported as `pr-e2e`, so open the PR early when you need it. Never dispatch a workflow.

## Never

- Merge, push to `main`, deploy, release, or touch a store, Firebase or GA console. `.claude/hooks/guard_production.py` blocks most of these anyway.
- Change repository settings or branch protection.
- Write anything from `CLAUDE.local.md`, pricing or an open security hole into the repository, an issue or a PR.

## Reporting to the director

Every link must open with one click: either a full URL or an absolute local path.

Send the director a message with `SendMessage`, at these moments:
- **Waiting for the developer**: an explore question, an approval, a product decision or a test. Ask the developer in your session, and tell the director what you are waiting for in one line.
- **Technical question**: the director answers it.
- **Emulator turn**: ask for it, and say when you give it back.
- **Another issue changed or created**: give its name.
- **Ready to test**: give the PR URL, the checks and their results, and what to test.
- **Stuck**: say what you need.

A message to the director can be held for approval and expire. If it expires, don't resend it in a loop: leave the report as the last thing in your session, because the director reads it there.

## The developer's verdict

You never merge. When the developer accepts the PR, the director tells you:
1. If `main` has moved, merge `origin/main` into your branch with a normal merge.
2. Archive the change.
3. Push, and wait for the checks to go green.
4. Report **ready to merge**, with the head SHA.

The director merges the PR. Changes the developer asks for come to you in your session. Treat each one like a review finding, then report **ready to test** again. A change that turns out to be a new feature goes to the director instead.
