# Proposal

## Why

The shift setup creates personal types, so a user who later joins or creates a group ends up with
their shifts on the wrong side: personal events cannot be offered for swap and colleagues cannot see
them. Recreating them one by one in the group is enough friction to keep people out of groups. Group
events also cannot carry notes today, since the document is read by every member, so moving a shift
would lose its notes.

## What Changes

- **Move to a group.** The detail of a personal typed event offers "Move to a group". The user picks
  a group they are a member of (not revoked) and one of that group's event types.
- **One or all.** If the personal type is used by other events, the user chooses between moving only
  this one and moving all of them.
  - "All" moves every event of that type dated from one month ago onward, which is the retention
    window.
  - Days where the user already has an event in that group are skipped and reported at the end
    ("12 moved, 2 already had a shift").
- **Personal type becomes deleted.** After "move all", the personal type enters a **deleted** state.
  - It no longer appears in the add pane or in "My shifts" and cannot be used again.
  - Events older than the window stay personal and keep rendering with it, so a user browsing two
    months back still sees those shifts.
  - This is the single path whether or not old events remain.
  - It adds an `isDeleted` flag to `personalEventTypes`.
- **Client-only, atomic.** A move is one batched write:
  - group events created with `ownerId == assigneeId == auth.uid`;
  - the personal events deleted;
  - the personal type marked deleted;
  - the sync markers updated.

  No Cloud Function is needed.
- **Private notes on group events.** A new owner-only collection,
  `users/{uid}/groupEventExtras/{eventId}`, holds `{ notes, groupId, yearMonth }`.
  - It is read with a single query over the visible months, and only events that have notes cost a read.
  - Group event details gain viewing and editing of the user's own note.
  - A moved event's notes go there.
  - Nothing private is written to the shared group event document.
- **Retention.** The scheduled cleanup also purges `groupEventExtras` older than the window.
- **Analytics.** An event for a completed move, with the count of events moved and whether it was
  "all".

## Capabilities

### New Capabilities
- `move-personal-to-group`: moving one or all events of a personal type into a group type, the window,
  conflict skipping, and the deleted state of the personal type.
- `group-event-notes`: private per-user notes on group events, stored under the user.

### Modified Capabilities
- `analytics-events`: adds the move event and adds `group_event_notes_saved` (or extends
  `event_notes_saved`) for notes on group events.

## Impact

- **Firestore**:
  - `personalEventTypes.isDeleted`;
  - new collection `users/{uid}/groupEventExtras`, with rules, a `yearMonth` index and sync marker;
  - updates to `firestore-schema.md`, `firestore.rules`, `firestore.indexes.json`,
    `firestore-usage.md` (a move costs about 2N writes) and the test fixtures.
- **Cloud Functions**:
  - the retention cleanup purges `groupEventExtras`;
  - `getSharedCalendar` is unaffected, since notes never sit on group events.
- **Core**:
  - `PersonalEventRepository` and `GroupRepository`, for the batched move;
  - a new extras datasource with usage tracking;
  - the local cache, which must hold deleted types and group notes.
- **UI**:
  - personal event detail (the Move action and its sheets);
  - group event detail (notes);
  - the add pane and "My shifts", which filter out deleted types;
  - strings in all five languages.
- **E2E**: new flows for moving and for group notes; fixtures gain the new fields.
