# Design

## Context

See `proposal.md` (*Why*). Two facts about how the app talks to Firebase shape the contract and
what it asks of the code.

| Mechanism | Used by | With no connection |
|---|---|---|
| Callable Cloud Function | take, give back, join, accept, reject, leave, remove, delete group, name and username, delete account, the shared-calendar read, the profile photo (TUR-56) | Fails on its own. With no network it fails quickly. With a network that does not answer it takes up to the SDK's ~70 s timeout. The message is generic today. |
| Direct Firestore write | events, notes, swap offers, personal types, the shift setup, type colours, avatar, preferences, sharing and stopping sharing, creating and editing a group, the commits of a move to a group (after its two server reads) | Lands in the local cache at once and is queued. `commit()` keeps suspending until the server acknowledges it, so it **never fails offline**. A screen that waits on it spins until reconnect, and the write still lands then. |

Event writes already meet the contract: the day sheet launches them and waits on nothing
(`offline-event-visibility`). Every screen that awaits a commit before closing does not.

## Goals / Non-Goals

**Goals:**

- One rule that classifies every action, so a new action can be placed without a new decision.
- A map from every requirement of `offline-actions` to whether it holds today, and if not, who owns
  it.

**Non-Goals:**

- How the inheriting issues meet the contract: gating a Firestore write on a connection, the
  timeout mechanism, how the *No connection* error is modelled, how a rejection is surfaced. Those
  belong to TUR-37, TUR-38, TUR-39, TUR-40 and TUR-41.
- Restating `offline-event-visibility`. The new capability refers to it.

## Decisions

### D1. The rule: who decides the outcome

Three clauses, in this order:

1. **Writes to the calendar are local.** That means adding, editing and deleting events of any kind,
   notes and swap offers, whoever else can see them. This is the calendar `offline-event-visibility`
   already makes offline-first. A clash with someone else's change is resolved by the server, and
   the user is told when it refuses (TUR-37).
2. **Writes to what only the user owns are local:** their types, their preferences, their avatar.
3. **Whatever the server or another person decides needs a connection:**
   - a Cloud Function or transaction (take, join, leave);
   - uniqueness (username);
   - a read only the server can answer (the days a move would collide with);
   - the group document;
   - who can see the user's calendar.

   It also covers an action that is meaningless until it reaches the server: a group's invitation
   code.

**Who can see the calendar** is in the third clause by the developer's decision on stopping sharing.
`calendarSharedWith` is the user's own document, but `getSharedCalendar` checks it on someone else's
behalf, so a queued change would say *done* while it was not. Sharing falls in the same clause, so
the rule has no exception.

The calendar clause comes first because a group event is a shared document too: read by every
member, and written by its holder and by admins. Without the clause, the third would wrongly claim
swap offers and an admin's delete.

*Alternative considered:* classifying by mechanism (callable = server, direct write = local).
Rejected: editing a group is a direct write, yet it is a full-document `set` that the rules
reject if `memberUids`, `adminUids` or `revokedUids` moved in the meantime. That would be a silent
loss on reconnect. Mechanism is how the code works today, not what the user can rely on.

### D2. A spec ahead of the code

This change writes the contract and nothing else. On archive, `openspec/specs/offline-actions`
holds requirements the code does not meet yet. That is the point of TUR-34 ("so code and tests
have something to obey"), and the seven issues blocked on it each implement part of it.

*Alternative considered:* writing each issue's part into its own change as it lands. Rejected:
nobody could tell an unhandled action from an undecided one, which is today's problem.

**Compliance map** (as read on `main` at 2610cda):

| Requirement | Today | Owner |
|---|---|---|
| Event actions work locally at once | Met | — |
| Shift types and the shift setup work locally at once | Types and setup hang until reconnect | TUR-39 |
| … own colour for a group type shows at once | Shows only once the server acknowledges it | TUR-64 |
| Profile and preferences work locally at once | Avatar save hangs with a spinner; notifications, hiding and language are met | TUR-63 |
| Signing out works at once offline | Hangs on the push-token write | TUR-38 |
| An offline sign-out stops the account's notifications once back online | Not met: offline, the token removal would wait in the signed-out account's queue | TUR-38 |
| A local change is reported only when the server rejects it | Rejections are lost in silence | TUR-37 |
| Group and membership actions need a connection | Creating or editing a group hangs, and the write lands on reconnect; callables fail with a generic error | TUR-39, TUR-41 |
| Shift transfer and move actions need a connection | Generic error. A move's commits are queued if the connection drops after its server reads, and then run on reconnect | TUR-41; the move's commits: *owner pending* |
| Account, sharing and people actions need a connection | Sharing and stopping sharing are queued writes. Searching people offline returns cached or empty results with no error. The rest fail with a generic error | TUR-62 (stopping); sharing and search: *owner pending*; TUR-41 |
| An action that needs a connection says so | No *No connection* error; callables can take ~70 s | TUR-41 |
| … keeps what the user entered | Mostly met; to verify per screen with the error | TUR-41 |
| … never runs later on its own | Callables met; creating or editing a group is queued | TUR-39 |
| A screen shows what the device holds | Calendars met; the launch can block on consent | TUR-40 |
| A screen with nothing cached says *No connection* | Generic errors or empty states | TUR-41 |
| (testing the flows: process death, iOS) | — | TUR-36, TUR-46 |

The four gaps the explore found were filed by the director, all blocked by TUR-34:
- TUR-64: one's own type colour;
- TUR-63: the avatar save, also blocked by TUR-56;
- TUR-62: stopping sharing;
- screens with nothing cached: folded into TUR-41.

### D3. A new capability beside `offline-event-visibility`

`offline-event-visibility` holds precise cache and marker requirements for the calendar: at once,
indistinguishable, exactly once, never emptied, no billable read. The new capability is the
cross-action table and refers to it for events.

*Alternative considered:* growing `offline-event-visibility` into the whole contract. Rejected: its
requirements are about the calendar's cache, and mixing group creation and sign-out into it would
make both harder to read and test.

### D4. Readable bounds

- **"At once"** is read as **within two seconds**, so a test can assert it. It is the developer's
  "al momento", with room for a device to do the work.
- **15 s** is the bound for a network that does not answer, chosen with the developer.
- **30 s** for the photo upload is TUR-56's own decision (`openspec/changes/profile-photo`, D6), and
  is recorded rather than re-decided. If that change moves it, this spec follows.

### D5. Signing out relies on Firestore's per-account queue

Firestore keeps unacknowledged writes on the device, keyed by the signed-in account, and resumes
them when that account signs in again. Signing out offline therefore loses nothing as long as:
- the same account comes back on the same device;
- the app's data is not cleared first.

The developer chose no warning, so the 2026-10-30 decision holds without an exception: pending is
never shown.

The same queue would hold the push-token removal that sign-out writes first. Left there, the
signed-out account's notifications would keep reaching the device, and whoever signs in next, until
that account came back. The contract therefore requires those notifications to stop once the device
is back online. How is TUR-38's call; invalidating this device's token with FCM is one way that does
not depend on the queue.

### D6. Reads: no staleness marker

A screen shows what the device holds, with no "may be out of date" marker. A global offline
indicator was considered and rejected with the developer: it brushes against the decision that
pending is never shown, and it adds noise for a user who mostly reads their own calendar. Only the
part of a screen with **nothing** to show says *No connection*.

### D7. `CLAUDE.md` placement

The table gets its own section, *Offline-first contract*, right after *Data retention & local
cache*. Offline-first also becomes one line of *Goals*. In flight:
- TUR-21 (PR #58) edits *E2E tests* and *Pull requests*;
- TUR-56 does not touch `CLAUDE.md` so far.

Neither overlaps these sections.

## Risks / Trade-offs

- **[The main spec states behaviour the app does not have yet]** → The compliance map names an owner
  issue for every unmet requirement. A requirement with no owner must not reach `openspec/specs`.
- **[Unsent changes are stranded if the user signs out offline and then clears the app's data, or
  only ever signs in elsewhere]** → Accepted by the developer, in exchange for never showing
  pending (D5).
- **[Withdrawing an offer offline races with a colleague taking it]** → `takeEvent` has already
  cleared `onSwap` and moved `assigneeId`. A member's withdrawal is rejected on reconnect by the
  assignee check of the update rule, and is reported under the rejection requirement (TUR-37). An
  admin's passes that check, but writes `onSwap = false` over `false`, so it changes nothing.
- **[An admin's offline delete of a shift taken meanwhile is accepted, not rejected]** → A group
  event's delete is a soft delete through `update`, which the rules allow for any admin. An admin who
  deletes, offline, a shift a colleague took meanwhile removes the colleague's shift on reconnect
  with no rejection to report. The same delete made online would show the new holder first. →
  Reported to the director for TUR-37. The rejection scenario is written for a member who is not an
  admin.
- **[Admins cannot fix a group's types offline]** → Rare, and the alternative is a silent loss on
  reconnect (D1).
- **[A new action is added without a class]** → The `CLAUDE.md` section states the rule, and an
  action is not finished until its row exists.

## Migration Plan

None: documentation only. Rolling back means reverting the `CLAUDE.md` section and the spec.
