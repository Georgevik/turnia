# Issue tracker

Work is tracked in GitHub Issues on `Georgevik/turnia`. Call an issue **TUR-N**, where N is its GitHub number. Its title starts with that name: `TUR-48 Prepare the App Store version from CI`. Never refer to work by a PR number. `Closes #N` and `Blocked by #N` keep the number, because GitHub links them.

The repository is public. Never write pricing, anything from `CLAUDE.local.md`, or the details of an open security hole in an issue, a comment or a PR.

## Creating an issue

1. Create the issue with `gh issue create`, using the body below and its labels.
2. Read its number, then retitle it straight away to `TUR-<N> <title>`.

The body starts with three lines, followed by two sections:

```
Priority: 12
Blocked by #36 (TUR-36)
Touches: e2e, ci

## Goal
What is true once it is done, in one or two sentences.

## Notes
What is already known: files, causes, constraints.
```

- **`Priority`**: the queue order. A lower number goes first.
- **`Blocked by`**: one line per blocking issue, or no line if nothing blocks it.
- **`Touches`**: the areas it changes. The director uses them to tell whether two tickets can run at once. They are:
  - `app`, `core`, `ios`, `e2e`, `ci`, `release`, `analytics`, `docs`, `harness`;
  - `firebase:rules`, `firebase:functions`, `firebase:emulators`.

## Labels

Each issue carries one **state** label, one **class** label, and any number of area labels: `bug`, `enhancement`, `harness`, `e2e`, `subscriptions`.

| State | Means |
|---|---|
| `explore` | It needs an explore with the developer first (`/opsx:explore`). |
| `proposed` | Its change or brief waits for the developer's approval. |
| `ready` | It is approved and can be applied without the developer. |
| `in-progress` | A worker is applying it. |
| `testing` | Its PR waits for the developer's test. |
| `blocked` | It waits for another issue or an outside input. |

| Class | Means |
|---|---|
| `trivial` | No OpenSpec change. A short brief in `## Proposal` is enough. |
| `medium` | An OpenSpec change, with no risky design. |
| `complex` | An OpenSpec change whose design needs the developer's eye: data model, rules, offline behaviour, money, security. |

The `worker` label means the director has a worker on the issue. Only the director adds or removes it.

## Comments

The comments record the decisions:
- **`## Explore`**: the questions settled with the developer, one line each.
- **`## Proposal`**: the OpenSpec change (`openspec/changes/<name>`) or, for a `trivial` issue, the brief. It also lists the points the developer has to confirm.
- **`## Approved`**: the developer's approval, quoting their words.

## Changing another issue

When work on one issue changes another one in flight, don't edit that issue's scope. Open a **follow-up** issue with `Blocked by` pointing at the one in flight, and tell the director its name.
