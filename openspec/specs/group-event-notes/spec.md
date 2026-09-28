# group-event-notes Specification

## Purpose

Lets a user keep private notes on group events. Group event documents are read by every member, so
the notes are stored under the user and are never visible to anyone else.

## Requirements

### Requirement: A user can keep a private note on a group event
The day sheet row of a group event SHALL show the user's own note on it, and SHALL let the user add,
edit and clear that note. This applies on the user's own calendar and on a group's calendar, to any
group event of a group they are a member of or revoked from that is shown to them. A blank note
SHALL be stored as no note. The note SHALL NOT be offered on a colleague's shared calendar.

#### Scenario: Adding a note to a group shift
- **WHEN** the user opens a day holding a "Urgencias" shift, taps "Add note", writes "Parking B" and saves
- **THEN** the shift shows "Parking B" to that user

#### Scenario: Clearing a note
- **WHEN** the user edits the note to blank and saves
- **THEN** the shift shows no note and offers "Add note" again

#### Scenario: A colleague's shared calendar
- **WHEN** the user opens a calendar someone shared with them
- **THEN** group events there show no note and offer no "Add note"

### Requirement: A group event note is private to its author
A note on a group event SHALL be readable and writable by its author only. It SHALL NOT be written to
the group event document, SHALL NOT be shown to other members of the group, and SHALL NOT be shown to
users the author's calendar is shared with. Two members SHALL be able to keep different notes on the
same group event.

#### Scenario: Another member opens the same shift
- **WHEN** Alice has a note on a "Urgencias" shift and Bruno, another member, opens that day
- **THEN** Bruno sees no note from Alice

#### Scenario: Shared-calendar viewer
- **WHEN** Alice shares her calendar with Carla and Carla opens it
- **THEN** Carla does not see Alice's group event notes

#### Scenario: Two notes on one shift
- **WHEN** Alice and Bruno each save a note on the same shift
- **THEN** each sees only their own

### Requirement: A note stays with the event, not with who holds it
A note SHALL stay attached to its group event when the event is taken by someone else or given back.
It SHALL remain visible to its author wherever that event is still shown to them. A note on an event
that has been deleted SHALL NOT be shown.

#### Scenario: The shift is taken
- **WHEN** Alice has a note on her shift, offers it for swap and Bruno takes it
- **THEN** Alice still sees her note on that shift on the group's calendar, and Bruno does not see it

### Requirement: Group event notes sync across the author's devices
A note saved on one device SHALL appear on the author's other signed-in devices, and a note cleared on
one SHALL disappear from the others, without the user forcing a refresh. A month in which the user
has no group event notes SHALL cost no document read beyond checking that nothing changed.

#### Scenario: Another device
- **WHEN** the user saves a note on a group shift on their phone and then opens that month on their tablet
- **THEN** the tablet shows the note

#### Scenario: Cleared elsewhere
- **WHEN** the user clears that note on the tablet
- **THEN** the phone stops showing it

### Requirement: A failed note save is reported
When saving a group event note fails, the user SHALL be told it was not saved, and the row SHALL keep
showing the note as it was before the edit.

#### Scenario: Save rejected
- **WHEN** the note write is rejected
- **THEN** the user sees that the note was not saved and the previous note is shown
