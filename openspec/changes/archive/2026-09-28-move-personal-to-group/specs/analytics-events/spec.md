# Spec Delta

## ADDED Requirements

### Requirement: Moving personal events to a group is reported
The system SHALL log `personal_events_moved` once per completed move, after its writes have
succeeded. It SHALL carry:
- `scope`: `one` when the user moved only the tapped event, `all` when they moved every event of the type;
- `event_count`: the number of events moved;
- `skipped_count`: the number of events left personal because the day already had a shift in the group.

A move SHALL NOT log `personal_event_deleted`, `group_event_created` or `personal_event_created` for
the events it moves, and SHALL NOT count as an event added for the share prompt or for
`first_event_added`. A move that writes nothing, because it failed or its only event was skipped,
SHALL NOT log `personal_events_moved`.

#### Scenario: Moving all with two skipped
- **WHEN** the user moves all events of a type and 12 are moved while 2 are skipped
- **THEN** `personal_events_moved` is logged once with `scope = all`, `event_count = 12` and `skipped_count = 2`
- **THEN** no `group_event_created` and no `personal_event_deleted` are logged

#### Scenario: Moving one
- **WHEN** the user moves only the tapped event and it succeeds
- **THEN** `personal_events_moved` is logged with `scope = one`, `event_count = 1` and `skipped_count = 0`

#### Scenario: A single move onto a taken day
- **WHEN** the user moves one event onto a day that already has a shift in the group
- **THEN** no `personal_events_moved` is logged

### Requirement: Notes on group events are reported
The system SHALL log `group_event_notes_saved` when the user saves their note on a group event,
including when they clear it, with no parameter holding the notes. `event_notes_saved` SHALL keep
meaning a note saved on a personal event. A moved event's notes becoming a group event note SHALL NOT
log `group_event_notes_saved`.

#### Scenario: A note on a group shift
- **WHEN** the user saves "Parking B" as their note on a group shift
- **THEN** `group_event_notes_saved` is logged and `event_notes_saved` is not

#### Scenario: A failed save
- **WHEN** saving a group event note fails
- **THEN** no `group_event_notes_saved` is logged
