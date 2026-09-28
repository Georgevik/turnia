# Spec Delta

## ADDED Requirements

### Requirement: The shift setup reports how it is used
The system SHALL log:
- `onboard_shift_shown` each time the shift setup is displayed. A rotation or recomposition of the
  same display SHALL NOT log it again.
- `onboard_shift_skipped` when the user skips it.
- `onboard_shift_completed` once the types it creates have been written.

All three SHALL carry `via`:
- `onboarding` when the setup was opened after sign-in;
- `add_pane` when it was opened from the add pane's empty state.

`onboard_shift_skipped` and `onboard_shift_completed` SHALL carry `interacted`. It is `true` when,
before skipping or confirming, the user did any of the following, and `false` otherwise:
- selected or deselected a shift;
- edited a time;
- added a shift of their own.

`onboard_shift_completed` SHALL also carry:
- `type_count`, the number of types created;
- `custom_type_count`, how many of them the user added inline.

Each type created SHALL also log `personal_event_type_created`. A failed write SHALL log neither
`onboard_shift_completed` nor `personal_event_type_created`.

#### Scenario: Confirming the defaults without touching anything
- **WHEN** the setup opens after sign-in and the user confirms it untouched
- **THEN** `onboard_shift_shown` is logged with `via = onboarding`
- **THEN** `onboard_shift_completed` is logged with `via = onboarding`, `interacted = false`,
  `type_count = 3` and `custom_type_count = 0`
- **THEN** `personal_event_type_created` is logged three times

#### Scenario: Toggling then skipping
- **WHEN** the user deselects Night and then skips
- **THEN** `onboard_shift_skipped` is logged with `interacted = true` and no type is created

#### Scenario: Adding a custom shift
- **WHEN** the user deselects Afternoon and Night, adds one shift of their own and confirms
- **THEN** `onboard_shift_completed` is logged with `interacted = true`, `type_count = 2` and
  `custom_type_count = 1`

#### Scenario: Shown once per display
- **WHEN** the setup is displayed and the device is rotated
- **THEN** `onboard_shift_shown` has been logged once

### Requirement: The first event added on a device reports its kind
The system SHALL log `first_event_added` when the first event is added on the device, with `kind`:
- `typed` for an event of a personal or group event type;
- `one_off` for a personal one-off event.

It SHALL be logged at most once per device, and only once the event has been written. The count
SHALL be the same device-wide one the share prompt uses. A device that had already added events
before this change SHALL NOT log it.

#### Scenario: First event is a shift
- **WHEN** a new user adds their first event and it is a shift
- **THEN** `first_event_added` is logged with `kind = typed`

#### Scenario: Only the first
- **WHEN** the user adds a second event
- **THEN** `first_event_added` is not logged again
