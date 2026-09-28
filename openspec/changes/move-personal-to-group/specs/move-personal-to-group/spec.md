# Spec Delta

## Purpose

Lets a user turn their personal shifts into shifts of a group they belong to, one event or every
event of a personal type at once, so that colleagues see them and they can be offered for swap.

## ADDED Requirements

### Requirement: A personal typed event offers "Move to a group"
On the user's own calendar, the day sheet row of a personal typed event SHALL offer "Move to a
group". It SHALL NOT be offered on a personal one-off event, on a group event, or on a colleague's
shared calendar. It SHALL be offered only when the user is a member of at least one group, not
revoked, that has at least one event type.

#### Scenario: A member sees the action
- **WHEN** a member of a group opens a day holding one of their personal typed events
- **THEN** that event's row offers "Move to a group"

#### Scenario: A user in no group
- **WHEN** a user who is in no group, or only revoked from groups, opens a day holding a personal typed event
- **THEN** the row offers no "Move to a group"

#### Scenario: A one-off event
- **WHEN** the user opens a day holding a personal one-off event
- **THEN** its row offers no "Move to a group"

### Requirement: The user picks a group and one of its event types
Choosing "Move to a group" SHALL let the user pick one of their groups where they are not revoked
and that has at least one event type. It SHALL then let them pick one of that group's event types.
When the user belongs to exactly one eligible group, the group step SHALL be skipped. Backing out of
either step SHALL move nothing.

#### Scenario: Two groups
- **WHEN** a member of "Urgencias" and "Planta" chooses "Move to a group"
- **THEN** both groups are offered, and picking "Urgencias" offers that group's event types

#### Scenario: A revoked group is not offered
- **WHEN** the user is a member of "Urgencias" and revoked from "Planta"
- **THEN** only "Urgencias" can be picked

#### Scenario: Cancelled
- **WHEN** the user closes the type picker without picking a type
- **THEN** no event is moved and nothing is written

### Requirement: The user moves one event or all events of the type
When the personal type has at least one other event dated on or after the start of the move window,
the user SHALL choose between moving only this event and moving all of them. The move window starts
one calendar month before today. When there is no other such event, only this event SHALL be moved,
with no choice offered.

"All" SHALL move every non-deleted event of that personal type dated on or after the start of the
window, the tapped one included. Events of that type dated before the window SHALL stay personal and
unchanged.

#### Scenario: A type with several events
- **WHEN** the personal type "Mañana" has events on 3, 10 and 17 October and the user moves the one on 10 October
- **THEN** the user is asked whether to move only this one or all of them

#### Scenario: Moving only one
- **WHEN** the user chooses "Only this one"
- **THEN** only the event on 10 October becomes a group event; the events on 3 and 17 October stay personal

#### Scenario: Old events are left behind
- **WHEN** today is 28 October and the type also has an event on 15 September, and the user moves all
- **THEN** the event on 15 September stays personal and every event from 28 September onward is moved

#### Scenario: The only event of its type
- **WHEN** the tapped event is the only one of its type dated within the window
- **THEN** it is moved with no choice offered, and the personal type is not deleted

### Requirement: A moved event becomes a group event the user holds
Each moved event SHALL become a group event of the picked group and type on the same date. The user
SHALL be both its creator and its assignee. It SHALL NOT be offered for swap and SHALL have an empty
history. The personal event SHALL be deleted in the same commit, so the day never shows both, and
other devices of the user and viewers of their shared calendar SHALL stop showing the personal
event. The notes of a moved event SHALL become the user's private note on the new group event (see
`group-event-notes`) and SHALL NOT be written to the group event.

#### Scenario: One event moved
- **WHEN** the user moves their personal "Mañana" on 10 October into the type "Morning" of "Urgencias"
- **THEN** a "Morning" shift of "Urgencias" on 10 October, held by the user and not on swap, shows on their calendar and on the group's calendar
- **THEN** the personal "Mañana" on 10 October no longer shows on any of their devices

#### Scenario: Notes follow privately
- **WHEN** a moved event carried the note "Cover for Ana"
- **THEN** the user sees "Cover for Ana" on the new group shift, and the group event document holds no notes

### Requirement: Days that already have a shift in the group are skipped
An event SHALL NOT be moved to a date on which the user already holds a non-deleted event of the
picked group. Such an event SHALL stay personal and unchanged. When the user moved all, the result
SHALL report how many events were moved and how many were skipped. When moving only one event and
its day is taken, nothing SHALL be written and the user SHALL be told that they already have a shift
in that group on that day.

#### Scenario: Two days taken
- **WHEN** the user moves 14 events and already holds a "Urgencias" shift on 2 of those days
- **THEN** 12 events are moved, the 2 stay personal, and the user is told "12 moved, 2 already had a shift"

#### Scenario: Moving one onto a taken day
- **WHEN** the user moves only one event and already holds a "Urgencias" shift that day
- **THEN** nothing is moved and the user is told they already have a shift in that group that day

#### Scenario: A shift the user gave away does not count
- **WHEN** the user created a "Urgencias" shift on a day but a colleague took it
- **THEN** that day is not skipped

### Requirement: Moving all deletes the personal type
After every event in the window of a "move all" has been moved or skipped, the personal type SHALL be
deleted. A deleted type SHALL NOT be offered in the add pane or listed in "My shifts", and no new
event can be created with it. Events of the type that stayed personal — those older than the window
and those skipped — SHALL keep rendering with the type's name and color. Moving only one event SHALL
NOT delete the type.

#### Scenario: The type disappears
- **WHEN** the user moves all events of "Mañana"
- **THEN** "Mañana" is no longer in the add pane or in "My shifts"

#### Scenario: Old events keep rendering
- **WHEN** the user pages back to a month before the window after moving all of "Mañana"
- **THEN** the personal "Mañana" events there still show with its name and color

#### Scenario: A skipped event keeps rendering
- **WHEN** an event of "Mañana" was skipped because the day already had a group shift
- **THEN** it still shows as a personal "Mañana" on that day

### Requirement: A failed move leaves nothing half-done that cannot be finished
A move that fails SHALL tell the user it failed. An event SHALL never exist both as the personal event
and as the group event after a commit. When a move of many events fails part-way, the events already
moved SHALL stay moved, the personal type SHALL NOT be deleted, and repeating "move all" SHALL move
the remaining ones.

#### Scenario: Offline failure on a single move
- **WHEN** the single move's write is rejected by the server
- **THEN** the user is told the move failed and the personal event shows again on its day

#### Scenario: Retrying a partial move
- **WHEN** a "move all" of 300 events fails after moving the first 150, and the user runs "move all" again
- **THEN** the remaining events are moved, no event is duplicated in the group, and the type is then deleted
