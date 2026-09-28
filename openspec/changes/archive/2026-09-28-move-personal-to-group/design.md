# Design

## Context

See proposal.md for the motivation. What the code already has, and what this design builds on:

- **There is no event detail screen.** Every event is a `DayEventRow` inside `DayDetailSheet`, driven by
  `DayDetailSheetViewModel`. Notes are edited there through `NotesDialog`, and only for personal
  events: `saveNotes` returns early unless `source == PERSONAL`.
- **Personal types already soft-delete.** `PersonalEventType.isDeleted` exists.
  `PersonalEventTypesFirestore.delete` writes `isDeleted = true`, `updateAt` and the types marker in
  one batch. `getMyEventTypes()` (the add pane, "My shifts") filters deleted types, and `getEvents`
  joins against every type, deleted ones included, so their events keep rendering.
- **Every delete is soft.** `PersonalEventFirestore.delete` and `GroupEventFirestore` delete by
  `isDeleted = true` + `updateAt`, so the per-month delta sync (`updateAt >` the newest cached
  document) carries the deletion to other devices. A hard delete would never reach them.
- **Each datasource commits its own batch**, always with the same shape: write, add the sync marker
  through `UserSyncFirestore.write…(batch, …)` / `GroupSyncFirestore.writeEvents(batch, …)`, commit,
  `trackWrite`, then `PendingWrite.committed()`. Nothing yet spans two datasources in one commit.
- **No conflict check exists** for a user holding two shifts of a group on one day. `addEvent` only
  refuses a revoked group.
- **The retention window has no shared constant.** It appears only as `purgedBefore()` in
  `SharedCalendarRepositoryImpl`. Purging is out of scope and belongs to a separate change.
- The "local cache" is the Firestore SDK's persistent cache, set to an unlimited size.
- The group event create rule is `isMember(groupId) && ownerId == assigneeId == auth.uid`. The
  personal collections allow any owner write. `users/{uid}/sync` refuses only `subscription` and
  `groupEvents`.

## Goals / Non-Goals

**Goals:**
- A move is atomic per commit: no commit leaves an event both personal and in the group.
- A move that fails part-way can simply be run again, with no duplicates and no clean-up.
- A group event note costs nothing to read for a month where the user wrote none, and nothing at
  all when nothing changed.

**Non-Goals:**
- Moving a group event back to personal, or between groups.
- Moving one-off events.
- Any Cloud Function, and any purge of old data (a separate change).
- Showing group event notes to viewers of a shared calendar.

## Decisions

### 1. The move lives in `PersonalEventRepository`, over a new `PersonalEventMoveFirestore`

`PersonalEventRepository` gains:

- `suspend fun moveCandidates(event: PersonalTypedEvent): Outcome<List<PersonalTypedEvent>, MoveError>`
  returns the non-deleted events of the event's type dated on or after the window start. The UI uses
  it to decide whether to ask "only this one or all".
- `suspend fun moveToGroup(events: List<PersonalTypedEvent>, target: GroupEventType, deleteType: Boolean): Outcome<MoveResult, MoveError>`.
  `MoveResult(moved, skipped)`, and `MoveError` is `DayTaken | Failed`.

A new datasource, `PersonalEventMoveFirestore`, owns the multi-path batch. It depends on
`UserSyncFirestore` and `GroupSyncFirestore` for the markers, and reuses the existing document
classes (`GroupEventDocument`, the personal event fields, `GroupEventExtrasDocument`).

*Alternative:* give every datasource a `batch`-accepting variant of its writes and compose them in
the repository. That is more churn for a single caller, and it spreads one commit's invariants
across four files. Rejected.

### 2. The group event reuses the personal event's id

The new document is `groups/{g}/events/{personalEventId}`. This makes the move idempotent at the
document level. Each chunk re-derives its events from the server, and events already moved are
soft-deleted, so they are never picked again. A retry after a commit whose acknowledgement was lost
therefore neither duplicates a shift nor overwrites one a colleague has since taken.

*Alternative:* fresh ids plus a `movedTo` field on the personal event. That is an extra field and
still needs the same "skip already deleted" logic. Rejected.

### 3. Chunked commits, with the type deleted in the last one

A batch holds at most 500 writes. Per event there are up to three: the group event `set`, the
personal event soft delete, and a notes document when the event has notes. Events are committed in
chunks of **150**, and each chunk also writes:

- one merged `set` on `users/{uid}/sync/updates` carrying the `personalEvents` and
  `groupEventExtras` months of the chunk;
- one on `groups/{g}/sync/updates` carrying its `events` months.

This needs a multi-month `GroupSyncFirestore.writeEvents(batch, groupId, months: Set<YearMonth>)`,
and a `writePersonalEvents` variant taking a set, like `writePersonalOneOffEvents`.

When `deleteType` is set, the **last** chunk also soft-deletes the personal type and moves
`personalEventTypesUpdatedAt`. A failure before it leaves the type alive, so "move all" is still
offered and finishes the job (spec: *A failed move leaves nothing half-done*). A move of one event
is a single chunk.

`trackWrite(TAG, "move", documents = n)` counts each commit's documents.

### 4. Taken days come from one server query per month batch

Before writing, the datasource reads the dates the user holds in the target group:
`groups/{g}/events` where `assigneeId == uid`, `yearMonth in [months]`. The `in` list is limited to
30 values, so it is split into slices of 30. The existing `(assigneeId, yearMonth, updateAt)` index
covers this query. The read uses `Source.SERVER`, tracked as `heldDates(SERVER)`. A cache read could
miss a shift added on another device, and a stale answer here creates a double assignment, which is
precisely what the product exists to prevent. Deleted documents are ignored. Only
`assigneeId == uid` counts, so a shift the user created but gave away does not block the day.

Candidates come from `users/{uid}/personalEvents` where `typeId == t` and `yearMonth >= windowStartMonth`,
also from the server (`candidates(SERVER)`). The client then drops `isDeleted` documents and those
dated before the window start. This needs a new `(typeId, yearMonth)` index.

*Alternative:* serve both from the cache, as the calendar does. That is cheaper, but it can miss
events on other devices and double-book a day. Rejected for a one-off action whose cost scales with
what it writes anyway.

### 5. One retention constant

`RetentionWindow` (core domain) exposes `start(today) = today.minus(1, MONTH)`.
`SharedCalendarRepositoryImpl.purgedBefore()` switches to it, so the move and the shared calendar
cannot disagree, and the future purge change has one place to edit.

### 6. Group event notes: `users/{uid}/groupEventExtras/{eventId}`

The document holds `{ groupId, yearMonth, notes, updateAt }`.

- Clearing a note writes `notes = null` and keeps the document. Like a soft delete, this is what
  lets another device's `updateAt >` query see that the note went away.
- A new per-month marker, `groupEventExtras`, goes in `users/{uid}/sync/updates` and is written in
  the same commit as the note.
- The reader, `GroupEventExtrasFirestore.get(uid, months)`, is a copy of the `PersonalEventFirestore.get`
  pattern:
  1. a cache query per month;
  2. the marker compared with the newest cached `updateAt`;
  3. a server `updateAt >` query only for stale months.
- A month with no notes has no documents and no marker, so it costs nothing. The query needs a
  `(yearMonth, updateAt)` index.
- Rules: read and write only when `request.auth.uid == uid`. It is not `ownerOrShared`, because
  notes are private even from shared-calendar viewers. No group lookup is needed.

The domain side is `GroupRepository.getMyEventNotes(date, monthDelta): Flow<Map<EventId, String>>`
and `saveEventNote(event: GroupEvent, notes: String?): Outcome<Unit, Unit>`. The latter trims the
note, turns a blank one into `null` and logs `group_event_notes_saved`. The keys are event ids, so
a note follows its event whoever holds it.

`MyCalendarViewModel` and the group-calendar mode of `ExternalCalendarViewModel` combine these notes
with the group events and fill `DayEventUi.notes` / `notesEditable = true`. The shared-calendar mode
never reads them. `DayDetailSheetViewModel.saveNotes` routes `GROUP` rows to `saveEventNote` and
reuses the existing `noteError` state.

*Alternative:* a subcollection per group (`groups/{g}/events/{e}/notes/{uid}`). That needs rules
that look up membership, costs a query per group per month, and is readable only while the user is
a member. Rejected.

### 7. The Move flow: its own ViewModel in a bottom sheet

`DayEventRow` gains an `onMove` callback. It is set only for `PERSONAL` typed rows on the user's own
calendar, and only when an eligible group exists. The eligible groups come from the add pane's
`getGroups()`, which is already collected, with revoked groups and groups without types dropped.

The callback opens `MoveToGroupSheet`, backed by `MoveToGroupViewModel` with the steps
`PickGroup → PickType → PickScope → Moving → Done`:

- `PickGroup` is skipped when only one group is eligible. The rows reuse `SwapGroupFilterSheet`'s
  group row look.
- The type list reuses the add pane's type chip.
- `PickScope` is shown only when `moveCandidates` returns more than the tapped event.
- The outcome becomes UiState:
  - `Done(moved, skipped)`, rendered with plural strings;
  - `userMessage` for `DayTaken` or `Failed`.

  None of it is sent as a one-shot event.

A move is not counted by `SharePromptRepository`, and the repository does not log the per-event
created and deleted events (see the analytics spec).

## Risks / Trade-offs

- **Rules `get()` budget in a large batch.** Each group event create evaluates `isMember(groupId)`,
  which calls `get(groups/{g})`. Firestore allows 20 document accesses per batched write, and
  repeated gets of the same document are served once, but this has never been exercised with 150
  creates. → Mitigation: an emulator E2E test commits a full chunk. If the rules refuse it, the
  chunk size drops, and the constant lives in one place.
- **Partial move.** Commits are atomic per chunk, not across a whole "move all". → The id reuse, the
  server re-query and the type being deleted last make re-running "move all" finish it (Decisions 2
  and 3).
- **Notes become more private.** A personal event's notes are readable by shared-calendar viewers.
  Once moved they are not. → This is accepted and stated in the group-event-notes spec: group notes
  are private by design.
- **Cost of a big move.** About 2N writes plus N notes, and N + M server reads. A type with a year of
  future shifts is roughly 700 writes. → It is a deliberate, rare action. The numbers go into
  `firestore-usage.md`.
- **A race with another device.** A shift added in the group between the taken-days read and the
  commit can still double-book that day. → The window is milliseconds, and the product already
  accepts that `create` has no uniqueness rule. This matches today's add path, which has no check at
  all.
- **Viewers of an older shared-calendar cache** may briefly show both copies until their next delta.
  → Both markers (`personalEvents` and the server-stamped `groupEvents`) move, so their next sync
  corrects it.

## Migration Plan

1. Deploy `firestore.rules` (the `groupEventExtras` match) and `firestore.indexes.json`
   (`groupEventExtras (yearMonth, updateAt)` and `personalEvents (typeId, yearMonth)`) before
   shipping the client. Older clients never touch either.
2. Ship the client. No data backfill is needed: the collection starts empty, and a missing
   `groupEventExtras` marker means "never written".
3. Rollback: an older client ignores `groupEventExtras` and moved events are ordinary group events,
   so nothing needs undoing.
