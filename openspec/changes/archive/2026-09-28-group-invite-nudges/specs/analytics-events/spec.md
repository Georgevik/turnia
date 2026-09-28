# Spec Delta

## ADDED Requirements

### Requirement: The team prompt reports how it is answered
The system SHALL log `onboard_team_shown` the first time the team prompt is displayed on the device.
A rotation or recomposition of the same display SHALL NOT log it again.

It SHALL log `onboard_team_answered` once when the user answers it, with `choice`:
- `create` when they chose Create a group;
- `join` when they chose I have a code;
- `dismissed` when they chose Not now or closed the prompt any other way.

A share of the invitation link from the step shown after creating a group, or from the
single-member card, SHALL log `group_invite_shared`, as a share from the group's screen does.

#### Scenario: Prompt shown and dismissed
- **WHEN** the team prompt appears and the user swipes it away
- **THEN** `onboard_team_shown` is logged once and `onboard_team_answered` is logged with `choice = dismissed`

#### Scenario: Choosing to join
- **WHEN** the user answers I have a code
- **THEN** `onboard_team_answered` is logged with `choice = join`

#### Scenario: A double tap
- **WHEN** the user taps Create a group twice before the prompt closes
- **THEN** `onboard_team_answered` is logged once

#### Scenario: Inviting after creating
- **WHEN** the user shares the invitation link from the "Your group is ready" step
- **THEN** `group_invite_shared` is logged
