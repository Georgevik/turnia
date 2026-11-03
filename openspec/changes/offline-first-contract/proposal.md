# Proposal

## Why

Offline-first was never stated as a goal, so every screen improvised. Event writes became local and
instant, while saving a group, a type, the shift setup or the avatar spins until the connection returns.
Taking a shift offline shows a generic error, and signing out hangs. Seven issues (TUR-36 to TUR-41, TUR-46)
wait on one answer: what each action does with no connection. Without it, neither the code nor the
tests have anything to obey.

## What Changes

- A new capability states the offline contract for **every** user action. Each one either **works
  locally at once** or **needs a connection** and says so. Reading follows its own rule: show what
  the device holds.
- The rule that decides the class has three clauses:
  - writes to the calendar are local, whoever else can see them;
  - writes to what only the user owns are local;
  - whatever the server or another person decides needs a connection. That includes the group
    document and who can see the user's calendar, which the server checks on someone else's behalf.
- What *needs a connection* looks like:
  - the button stays enabled;
  - with no network, *No connection* (*Sin conexión*) shows at once;
  - with a network that does not answer, the same message shows after a bounded wait;
  - what the user typed is kept;
  - nothing runs later on its own.
- Settled during the explore, beyond the two decisions of 2026-10-30:
  - editing an existing group (name, colour, invitation code, auto-approve, its event types) needs a
    connection, like creating it;
  - stopping sharing one's calendar needs a connection, like sharing it;
  - a screen with nothing cached says *No connection*, never an empty state or an endless loader;
  - signing out works at once offline, with no warning about unsent changes.
- `CLAUDE.md` gains an *Offline-first contract* section with the table, and offline-first joins
  its *Goals*.
- No code changes in this change. The contract is ahead of the code on purpose. Each requirement that is
  not yet met is owned by an issue named in `design.md`.

## Capabilities

### New Capabilities

- `offline-actions`: what every user action does with no connection — which work locally at once,
  which need a connection and how they say so, and what a screen shows when it cannot reach the
  server.

### Modified Capabilities

None. `offline-event-visibility` keeps owning how events, notes and personal types behave once
written offline, and the new capability refers to it rather than restating it. No existing
requirement contradicts the contract:
- `shared-calendar` reports a failure with nothing cached "as before", and the new capability says
  how that failure reads when the cause is no connection;
- `move-personal-to-group` and `event-type-management` already require a failed save to tell the
  user and keep their input.

## Impact

- **Docs:** `CLAUDE.md` (a new section after *Data retention & local cache*, one line in *Goals*),
  `openspec/specs/offline-actions` on archive.
- **Code:** none in this change. The issues that inherit the contract:
  - TUR-37: tell the user when the server rejects an offline change;
  - TUR-38: sign out at once;
  - TUR-39: personal types and the shift setup finish at once; creating or editing a group needs a
    connection;
  - TUR-40: an offline launch reaches the calendar;
  - TUR-41: the *No connection* error, the bounded wait, input kept;
  - TUR-36 and TUR-46: the offline flows to test.
- **Gaps the explore found**, filed by the director:
  - TUR-63: the avatar save hangs;
  - TUR-64: one's own colour for a group type shows only once the server confirms it;
  - TUR-62: stopping sharing is a queued write today;
  - TUR-41 (folded in): screens with nothing cached show generic errors.
- **Gaps the PR's self-review found**, filed by the director:
  - TUR-62 (extended): sharing is queued, like stopping sharing;
  - TUR-66: a move to a group can run by itself on reconnect;
  - TUR-41 (folded in): searching people offline returns cached or empty results.
- **In flight:** TUR-56 (profile photo) is recorded as its own change decides. Uploading and removing
  a photo need a connection and keep their 30 s upload timeout.
