# Tasks

## 1. The contract in `CLAUDE.md`

- [x] 1.1 Add offline-first to *Goals* as one bullet: what the user does to their own data works
  with no connection, and what needs the server says so. Verify that it sits with the other goals
  and links to the new section.
- [x] 1.2 Add an *Offline-first contract* section right after *Data retention & local cache*. It
  holds:
  - the rule (D1) and its one exception;
  - the table of actions: local at once, needs a connection, reading;
  - what *needs a connection* looks like: button enabled, *No connection* at once or within 15 s
    (photo 30 s), input kept, never queued;
  - signing out with no warning;
  - pointers to `openspec/specs/offline-actions` and `offline-event-visibility`;
  - the line that a new action is not finished until it has a row.

  Verify that every action named in `specs/offline-actions/spec.md` appears in the table, and that
  nothing from `CLAUDE.local.md` is written there.
- [x] 1.3 Read the rest of `CLAUDE.md` for statements the contract contradicts (*Business rules*,
  *Error handling & UI state*, *Sensitive points*) and align them. Verify with a search for
  "offline" and "connection" that no remaining sentence disagrees with the new section.

## 2. Ownership before archive

- [ ] 2.1 Check that every unmet row of `design.md`'s compliance map still names an open owner
  issue (TUR-36 to TUR-41, TUR-46, TUR-62 to TUR-64), and update any the director has since
  renamed or merged. Verify each with `gh issue view`.
- [ ] 2.2 Run `openspec validate offline-first-contract --strict` and verify it reports the change
  as valid.

## Workflow follow-up

- Open the PR as `TUR-34 Define the offline-first contract per action`.
- Archive the change as the PR's last commit once the developer accepts it, so `offline-actions`
  reaches `openspec/specs`.
